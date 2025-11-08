# Tresor Flow Analysis: Deep Dive

## Critical Flows Examined

---

## Flow 1: Create Message (Happy Path)

```
User Journey:
┌─────────────────────────────────────────────────────────────┐
│ 1. User writes message                                      │
│ 2. Selects unlock date (e.g., 2035-01-01)                  │
│ 3. Enters delivery email/phone                              │
│ 4. Uploads photos/videos                                    │
│ 5. Reviews cost (~$7.54)                                    │
│ 6. Confirms & pays                                          │
│ 7. System encrypts & deploys                                │
│ 8. User downloads self-custody backup                       │
│ 9. Confirmation screen                                      │
└─────────────────────────────────────────────────────────────┘

Backend Flow:
┌─────────────────────────────────────────────────────────────┐
│ 1. Generate AES-256 key (K)                                 │
│ 2. Encrypt message with K → Ciphertext                      │
│ 3. Upload ciphertext to Arweave → Get TX_ID                │
│ 4. Split K into 5 shares (Shamir 3-of-5)                   │
│ 5. Deploy shares in parallel:                               │
│    ├─ Bitcoin CLTV transaction → BTC_TX                    │
│    ├─ Ethereum smart contract → ETH_TX                     │
│    ├─ Arbitrum smart contract → ARB_TX                     │
│    ├─ Generate self-custody file → JSON                    │
│    └─ Arweave backup share → AR_TX                         │
│ 6. Store metadata in PostgreSQL:                            │
│    ├─ Message ID                                            │
│    ├─ User ID                                               │
│    ├─ Unlock date                                           │
│    ├─ Delivery email/phone                                  │
│    ├─ Arweave TX IDs                                        │
│    └─ Blockchain TX IDs                                     │
│ 7. Return self-custody file to user                         │
│ 8. Send confirmation email                                  │
└─────────────────────────────────────────────────────────────┘

Time: ~2-5 minutes
Cost: ~$7.54
```

### 🚨 Problems Identified:

#### Problem 1.1: Partial Deployment Failure

**Scenario:**
```
✅ Arweave upload succeeds → $0.04 spent
✅ Bitcoin transaction succeeds → $7 spent
❌ Ethereum transaction FAILS (gas spike, network congestion)
❌ Arbitrum transaction NOT ATTEMPTED
```

**Current state:**
- $7.04 already spent
- Only 2 shares deployed (need 3!)
- Message is BROKEN

**Solution:**
```java
@Transactional(rollbackOn = ShareDeploymentException.class)
public DeploymentReceipt createMessage(Message msg) {
    // Phase 1: Upload encrypted message (can retry)
    String arweaveId = uploadWithRetry(encrypted, MAX_RETRIES);

    // Phase 2: Deploy shares with compensation logic
    List<Future<ShareDeployment>> deployments = new ArrayList<>();

    // Deploy all 5 in parallel
    deployments.add(executor.submit(() -> deployToBitcoin(share1)));
    deployments.add(executor.submit(() -> deployToEthereum(share2)));
    deployments.add(executor.submit(() -> deployToArbitrum(share3)));
    deployments.add(executor.submit(() -> createSelfCustody(share4)));
    deployments.add(executor.submit(() -> deployToArweaveBackup(share5)));

    // Wait for all deployments
    List<ShareDeployment> completed = new ArrayList<>();
    List<ShareDeployment> failed = new ArrayList<>();

    for (Future<ShareDeployment> future : deployments) {
        try {
            completed.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        } catch (Exception e) {
            failed.add(extractFailedDeployment(e));
        }
    }

    // Check threshold
    if (completed.size() < THRESHOLD) {
        // CRITICAL: Not enough shares deployed
        // Attempt emergency recovery
        return attemptRecovery(completed, failed);
    }

    // Success: At least 3 shares deployed
    return createReceipt(completed, failed);
}
```

**Recovery Strategy:**

```java
private DeploymentReceipt attemptRecovery(
    List<ShareDeployment> completed,
    List<ShareDeployment> failed
) {
    // Option 1: Retry failed deployments
    for (ShareDeployment failedShare : failed) {
        if (failedShare.isRetryable()) {
            ShareDeployment retried = retryDeployment(failedShare);
            if (retried.succeeded()) {
                completed.add(retried);
                if (completed.size() >= THRESHOLD) {
                    return createReceipt(completed, failed);
                }
            }
        }
    }

    // Option 2: Deploy to alternative blockchains
    if (completed.size() < THRESHOLD) {
        // Try Polygon (very cheap backup)
        ShareDeployment polygon = deployToPolygon(generateNewShare());
        if (polygon.succeeded()) {
            completed.add(polygon);
        }
    }

    // Option 3: Lower threshold temporarily (risky!)
    if (completed.size() == 2) {
        // Store as "degraded" state
        // User accepts 2-of-2 instead of 3-of-5
        return createDegradedReceipt(completed);
    }

    // Option 4: Full rollback
    throw new MessageCreationException(
        "Failed to deploy minimum shares. Refunding $" + calculateRefund()
    );
}
```

#### Problem 1.2: User Abandons Mid-Creation

**Scenario:**
```
✅ User writes message
✅ Uploads 5 photos
✅ System encrypts (10 MB total)
✅ Uploads to Arweave → $0.50 spent
❌ User closes browser tab
```

**Current state:**
- $0.50 spent on Arweave
- No shares deployed
- Orphaned data on Arweave (permanent!)

**Solution: Draft System**

```java
// Save drafts immediately
@AutoSave(intervalSeconds = 30)
public class MessageDraft {
    private String draftId;
    private String userId;
    private String content;
    private List<File> attachments;
    private DraftState state;

    enum DraftState {
        COMPOSING,           // User writing
        UPLOADING_FILES,     // Files uploading
        ENCRYPTING,          // Encryption in progress
        DEPLOYING_SHARES,    // Blockchain deployment
        AWAITING_PAYMENT,    // Payment pending
        ABANDONED            // User left
    }
}

// Abandoned draft cleanup job
@Scheduled(cron = "0 0 2 * * *") // 2 AM daily
public void cleanupAbandonedDrafts() {
    List<MessageDraft> abandoned = draftRepo.findByState(
        DraftState.ABANDONED
    ).filter(d -> d.getUpdatedAt().isBefore(LocalDateTime.now().minusDays(7)));

    for (MessageDraft draft : abandoned) {
        // Delete local attachments
        deleteFiles(draft.getAttachments());

        // Note: Cannot delete from Arweave (permanent!)
        // This is acceptable - user paid for it

        draftRepo.delete(draft);
    }
}
```

#### Problem 1.3: Payment Fails After Deployment

**Scenario:**
```
✅ Shares deployed to Bitcoin ($7)
✅ Shares deployed to Arbitrum ($0.50)
✅ Message on Arweave ($0.04)
❌ User's credit card DECLINED
```

**Current state:**
- $7.54 already spent (unrecoverable)
- User hasn't paid
- Company loses money

**Solution: Pre-authorization + Reserve Funds**

```java
public class PaymentFlow {

    public DeploymentReceipt createMessageWithPayment(Message msg, PaymentMethod pm) {
        // Step 1: Calculate total cost
        BigDecimal estimatedCost = calculateCost(msg);
        BigDecimal reserveAmount = estimatedCost.multiply(1.2); // 20% buffer

        // Step 2: Pre-authorize payment
        PaymentAuthorization auth = paymentService.authorize(pm, reserveAmount);
        if (!auth.isApproved()) {
            throw new PaymentException("Payment declined");
        }

        try {
            // Step 3: Deploy message
            DeploymentReceipt receipt = deployMessage(msg);

            // Step 4: Capture actual amount
            BigDecimal actualCost = receipt.getTotalCost();
            paymentService.capture(auth, actualCost);

            return receipt;

        } catch (Exception e) {
            // Step 5: Void authorization if deployment fails
            paymentService.voidAuthorization(auth);
            throw e;
        }
    }
}
```

**Alternative: Subscription Model**

```java
// User maintains balance
public class UserAccount {
    private BigDecimal balance;

    public void topUp(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public DeploymentReceipt createMessage(Message msg) {
        BigDecimal cost = calculateCost(msg);

        if (balance.compareTo(cost) < 0) {
            throw new InsufficientBalanceException(
                "Need $" + cost + ", have $" + balance
            );
        }

        // Deduct immediately
        this.balance = this.balance.subtract(cost);

        // Deploy message
        return deployMessage(msg);
    }
}
```

---

## Flow 2: Unlock Message (Happy Path)

```
User Journey:
┌─────────────────────────────────────────────────────────────┐
│ 1. User receives email: "Your message is ready!"           │
│ 2. Clicks magic link                                        │
│ 3. Authenticates (if needed)                                │
│ 4. Sees "Unlocking message..." (4 seconds)                 │
│ 5. Message appears with attachments                         │
│ 6. Can download, share, or re-lock                         │
└─────────────────────────────────────────────────────────────┘

Backend Flow:
┌─────────────────────────────────────────────────────────────┐
│ 1. Cron job checks: "Any messages unlocking today?"        │
│ 2. For each unlocked message:                               │
│    ├─ Check Bitcoin block height                           │
│    ├─ Check Ethereum block height                          │
│    ├─ Check Arbitrum block height                          │
│    └─ If ANY blockchain reached target → UNLOCKED          │
│ 3. Retrieve shares (parallel):                              │
│    ├─ Bitcoin: Fetch CLTV transaction                      │
│    ├─ Ethereum: Call smart contract                        │
│    └─ Arbitrum: Call smart contract                        │
│ 4. Reconstruct key (Shamir)                                │
│ 5. Download encrypted message from Arweave                 │
│ 6. Decrypt message                                          │
│ 7. Re-encrypt with user's personal key                     │
│ 8. Store in user's inbox (PostgreSQL)                      │
│ 9. Send notification email                                 │
└─────────────────────────────────────────────────────────────┘

Time: ~4 seconds (after triggered)
Cost: $0
```

### 🚨 Problems Identified:

#### Problem 2.1: Only 2 Shares Available

**Scenario:**
```
Check Bitcoin: ✅ Reached block 920,000 → Share 1 available
Check Ethereum: ❌ Network offline / congested
Check Arbitrum: ✅ Reached target → Share 3 available
Check Self-custody: ❌ User doesn't have it (lost phone)
Check Arweave: ❌ Gateway temporarily down

Available: 2 shares
Needed: 3 shares
Status: CANNOT UNLOCK
```

**Solution: Retry with Exponential Backoff**

```java
@Scheduled(cron = "0 */10 * * * *") // Every 10 minutes
public void checkUnlockableMessages() {
    List<Message> candidates = messageRepo.findByUnlockDateBefore(LocalDateTime.now())
        .filter(m -> m.getStatus() == MessageStatus.LOCKED);

    for (Message message : candidates) {
        try {
            UnlockResult result = attemptUnlock(message);

            if (result.isSuccess()) {
                message.setStatus(MessageStatus.UNLOCKED);
                notifyUser(message);
            } else if (result.getAvailableShares() >= THRESHOLD - 1) {
                // Close! Retry more frequently
                scheduleRetry(message, Duration.ofMinutes(5));
            } else {
                // Not enough shares yet - try again later
                scheduleRetry(message, Duration.ofHours(1));
            }

        } catch (Exception e) {
            log.error("Failed to unlock message {}: {}", message.getId(), e);
            scheduleRetry(message, Duration.ofHours(1));
        }
    }
}

private UnlockResult attemptUnlock(Message message) {
    List<Share> retrievedShares = new ArrayList<>();
    Map<Blockchain, Exception> failures = new HashMap<>();

    // Try each blockchain
    for (ShareDeployment deployment : message.getShareDeployments()) {
        try {
            Share share = retrieveShare(deployment);
            retrievedShares.add(share);

            // Early exit if we have enough
            if (retrievedShares.size() >= message.getThreshold()) {
                break;
            }

        } catch (Exception e) {
            failures.put(deployment.getBlockchain(), e);
            log.warn("Failed to retrieve share from {}: {}",
                deployment.getBlockchain(), e.getMessage());
        }
    }

    if (retrievedShares.size() >= message.getThreshold()) {
        // Success! Decrypt message
        byte[] key = shamirService.reconstruct(retrievedShares);
        byte[] plaintext = decrypt(message.getEncryptedPayload(), key);
        return UnlockResult.success(plaintext);
    } else {
        // Not enough shares
        return UnlockResult.failure(retrievedShares.size(), failures);
    }
}
```

#### Problem 2.2: Blockchain Fork

**Scenario:**
```
Expected: Bitcoin block 920,000 at 2035-01-01
Reality: Bitcoin had hard fork at block 915,000
  ├─ Bitcoin Core continues (block 920,000 reached)
  └─ Bitcoin Cash fork (only at block 905,000)

Question: Which chain do we monitor?
```

**Solution: Monitor Multiple Chains**

```java
public class BlockchainMonitor {

    private final List<BitcoinNode> bitcoinNodes = Arrays.asList(
        new BitcoinNode("https://bitcoin-core-node-1.com"),
        new BitcoinNode("https://bitcoin-core-node-2.com"),
        new BitcoinNode("https://bitcoin-abc-node.com") // Alternative implementation
    );

    public int getCurrentBitcoinBlockHeight() {
        Map<Integer, Integer> blockHeights = new HashMap<>();

        // Poll multiple nodes
        for (BitcoinNode node : bitcoinNodes) {
            try {
                int height = node.getBlockHeight();
                blockHeights.merge(height, 1, Integer::sum);
            } catch (Exception e) {
                log.warn("Bitcoin node {} unavailable", node);
            }
        }

        // Return consensus height (majority)
        return blockHeights.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElseThrow(() -> new BlockchainException("No consensus on block height"));
    }

    public boolean isShareUnlocked(ShareDeployment deployment) {
        // Accept if ANY major chain reached target
        // (User benefit - unlock on any surviving fork)
        for (BitcoinNode node : bitcoinNodes) {
            try {
                if (node.getBlockHeight() >= deployment.getUnlockBlock()) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
```

#### Problem 2.3: Race Condition on Unlock

**Scenario:**
```
10:00:00 - Cron job checks message A → Starts unlocking
10:00:01 - User clicks "Unlock now" → Starts unlocking (again!)
10:00:02 - Both processes retrieve shares
10:00:03 - Both processes decrypt message
10:00:04 - Database: Duplicate entry error
```

**Solution: Distributed Lock**

```java
public class UnlockService {

    @Autowired
    private RedissonClient redisson;

    public UnlockResult unlockMessage(String messageId) {
        // Acquire distributed lock
        RLock lock = redisson.getLock("unlock:" + messageId);

        try {
            // Try to acquire lock (wait max 10 seconds)
            boolean acquired = lock.tryLock(10, 60, TimeUnit.SECONDS);

            if (!acquired) {
                return UnlockResult.alreadyInProgress();
            }

            // Check if already unlocked
            Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));

            if (message.getStatus() == MessageStatus.UNLOCKED) {
                return UnlockResult.alreadyUnlocked(message);
            }

            // Perform unlock
            UnlockResult result = attemptUnlock(message);

            if (result.isSuccess()) {
                message.setStatus(MessageStatus.UNLOCKED);
                messageRepo.save(message);
            }

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnlockException("Interrupted while acquiring lock");
        } finally {
            // Always release lock
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

---

## Flow 3: Update Delivery Address

```
User Journey:
┌─────────────────────────────────────────────────────────────┐
│ 1. User logs in (after 5 years)                            │
│ 2. Goes to "My Messages"                                   │
│ 3. Sees: "Message unlocking in 5 years"                    │
│ 4. Clicks "Update delivery email"                          │
│ 5. Enters new email                                         │
│ 6. Verifies new email (magic link)                         │
│ 7. Confirmation: "Will deliver to new@email.com"           │
└─────────────────────────────────────────────────────────────┘
```

### 🚨 Problems Identified:

#### Problem 3.1: User Forgets They Have Messages

**Scenario:**
```
2025: User creates message for 2035
2030: User changes email, forgets about Tresor
2035: Message unlocks
      Sends to old@email.com (bounces!)
      User never receives message
```

**Solution: Multi-Channel Reminders**

```java
@Scheduled(cron = "0 0 9 * * MON") // Every Monday 9 AM
public void sendReminders() {
    LocalDateTime oneYearFromNow = LocalDateTime.now().plusYears(1);
    LocalDateTime oneMonthFromNow = LocalDateTime.now().plusMonths(1);

    // Messages unlocking in 1 year
    List<Message> soonMessages = messageRepo.findByUnlockDateBetween(
        oneYearFromNow.minusDays(7),
        oneYearFromNow.plusDays(7)
    );

    for (Message message : soonMessages) {
        sendReminderEmail(message.getUser(),
            "Your message unlocks in 1 year! Update your delivery address if needed.");
    }

    // Messages unlocking in 1 month
    List<Message> imminentMessages = messageRepo.findByUnlockDateBetween(
        oneMonthFromNow.minusDays(7),
        oneMonthFromNow.plusDays(7)
    );

    for (Message message : imminentMessages) {
        sendMultiChannelReminder(message);
    }
}

private void sendMultiChannelReminder(Message message) {
    User user = message.getUser();

    // Email
    if (user.getEmail() != null) {
        emailService.send(user.getEmail(), "Message unlocking soon!");
    }

    // SMS (if provided)
    if (user.getPhone() != null) {
        smsService.send(user.getPhone(), "Tresor: Your message unlocks in 1 month!");
    }

    // Push notification (if app installed)
    if (user.hasPushToken()) {
        pushService.send(user.getPushToken(), "Don't forget your future message!");
    }

    // Emergency contact (if configured)
    if (user.hasEmergencyContact()) {
        emailService.send(user.getEmergencyContact().getEmail(),
            user.getName() + " has a message unlocking soon. Please remind them!");
    }
}
```

#### Problem 3.2: Email Change Without Verification

**Scenario:**
```
Attacker:
1. Compromises user account
2. Changes delivery email to attacker@evil.com
3. Waits for message to unlock
4. Receives victim's private message

Victim:
- Never receives message
- May not notice until unlock date
```

**Solution: Verified Changes with Confirmation**

```java
public class DeliveryAddressUpdate {

    public void requestEmailChange(String userId, String newEmail) {
        User user = userRepo.findById(userId).orElseThrow();

        // Create pending change
        PendingEmailChange change = new PendingEmailChange();
        change.setUserId(userId);
        change.setOldEmail(user.getEmail());
        change.setNewEmail(newEmail);
        change.setToken(generateSecureToken());
        change.setExpiresAt(LocalDateTime.now().plusHours(24));

        pendingChangeRepo.save(change);

        // Send verification to NEW email
        sendVerificationEmail(newEmail, change.getToken());

        // Send NOTIFICATION to OLD email
        sendNotificationEmail(user.getEmail(),
            "Someone requested to change your delivery email. " +
            "If this wasn't you, secure your account immediately!");
    }

    public void confirmEmailChange(String token) {
        PendingEmailChange change = pendingChangeRepo.findByToken(token)
            .orElseThrow(() -> new InvalidTokenException());

        if (change.isExpired()) {
            throw new ExpiredTokenException();
        }

        // Update user email
        User user = userRepo.findById(change.getUserId()).orElseThrow();
        String oldEmail = user.getEmail();
        user.setEmail(change.getNewEmail());
        userRepo.save(user);

        // Update ALL future messages
        List<Message> futureMessages = messageRepo.findByUserIdAndStatusNot(
            change.getUserId(),
            MessageStatus.UNLOCKED
        );

        for (Message message : futureMessages) {
            message.setDeliveryEmail(change.getNewEmail());
        }
        messageRepo.saveAll(futureMessages);

        // Send confirmation to BOTH emails
        sendConfirmationEmail(change.getNewEmail(),
            "Your delivery address has been updated.");
        sendConfirmationEmail(oldEmail,
            "Your delivery address was changed to " + change.getNewEmail());

        // Log for audit
        auditLog.log("Email changed from " + oldEmail + " to " + change.getNewEmail());

        pendingChangeRepo.delete(change);
    }
}
```

---

## Flow 4: User Death / Inheritance

```
Scenario:
┌─────────────────────────────────────────────────────────────┐
│ 2025: Father creates message for son's 18th birthday       │
│ 2030: Father passes away                                   │
│ 2035: Message should still unlock                          │
│                                                             │
│ Questions:                                                  │
│ - Can son access it?                                       │
│ - Does account get deleted?                                │
│ - Can executor access it?                                  │
└─────────────────────────────────────────────────────────────┘
```

### 🚨 Problems Identified:

#### Problem 4.1: Account Deletion Deletes Messages

**Scenario:**
```
User dies → Family requests account deletion →
All messages deleted → Son never gets 18th birthday message
```

**Solution: Message Independence + Beneficiary System**

```java
@Entity
public class Message {
    @Id
    private String id;

    @ManyToOne
    private User creator; // Who created it

    private String beneficiaryEmail; // Who receives it (can be different!)

    private boolean independentDelivery; // Deliver even if creator's account deleted

    @ManyToOne
    private User emergencyContact; // Can claim message if creator dies

    private LocalDateTime unlockDate;

    private MessageStatus status;
}

public class MessageDeletionPolicy {

    public void deleteUserAccount(String userId) {
        User user = userRepo.findById(userId).orElseThrow();

        // Find all messages created by this user
        List<Message> messages = messageRepo.findByCreatorId(userId);

        for (Message message : messages) {
            if (message.isIndependentDelivery()) {
                // Detach from user account but keep message
                message.setCreator(null);
                message.setNote("Original creator account deleted");
                messageRepo.save(message);

                log.info("Message {} preserved after account deletion", message.getId());

            } else {
                // User chose to delete messages with account
                cancelMessage(message);
            }
        }

        // Delete user account
        userRepo.delete(user);
    }
}
```

**UI During Message Creation:**

```
┌─────────────────────────────────────────────────────────────┐
│ Delivery Options:                                           │
│                                                             │
│ Recipient email: son@email.com                             │
│                                                             │
│ ☑ Deliver even if I delete my account                      │
│                                                             │
│ Emergency contact (optional):                               │
│   wife@email.com                                           │
│   ℹ️  Can claim message if you pass away                   │
│                                                             │
│ ☐ Allow legal executor to access (requires verification)   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### Problem 4.2: Self-Custody Share Lost When User Dies

**Scenario:**
```
Father has self-custody share on his phone
Father dies
Phone is locked/wiped
Share 4 (self-custody) LOST
Only 4 shares remain (Bitcoin, Ethereum, Arbitrum, Arweave)
Still have 4, need 3 → Message unlocks ✅

BUT: What if 2 blockchains also fail?
```

**Solution: Hereditary Access Protocol**

```java
public class HereditaryAccess {

    @Entity
    public static class DeadManSwitch {
        private String messageId;
        private String userId;
        private int checkInIntervalDays; // e.g., 90 days
        private LocalDateTime lastCheckIn;
        private LocalDateTime nextCheckInDue;
        private int missedCheckIns;
        private boolean triggered;

        // Beneficiaries who get access if switch triggers
        @OneToMany
        private List<Beneficiary> beneficiaries;
    }

    @Scheduled(cron = "0 0 1 * * *") // Daily at 1 AM
    public void checkDeadManSwitches() {
        List<DeadManSwitch> overdue = switchRepo.findByNextCheckInDueBefore(
            LocalDateTime.now()
        ).filter(s -> !s.isTriggered());

        for (DeadManSwitch dms : overdue) {
            dms.setMissedCheckIns(dms.getMissedCheckIns() + 1);

            if (dms.getMissedCheckIns() == 1) {
                // First miss - send reminder
                User user = userRepo.findById(dms.getUserId()).orElseThrow();
                sendCheckInReminder(user);

            } else if (dms.getMissedCheckIns() == 3) {
                // Third miss - send urgent warning
                sendUrgentWarning(user, "If you don't check in within 7 days, " +
                    "your emergency contacts will be notified");

            } else if (dms.getMissedCheckIns() >= 4) {
                // Trigger dead man's switch
                triggerSwitch(dms);
            }

            switchRepo.save(dms);
        }
    }

    private void triggerSwitch(DeadManSwitch dms) {
        dms.setTriggered(true);

        Message message = messageRepo.findById(dms.getMessageId()).orElseThrow();

        // Grant access to beneficiaries
        for (Beneficiary beneficiary : dms.getBeneficiaries()) {
            // Generate special recovery key
            String recoveryKey = generateRecoveryKey(message, beneficiary);

            sendRecoveryEmail(beneficiary.getEmail(),
                "We believe " + message.getCreator().getName() + " may have passed away. " +
                "You are listed as a beneficiary. Use this recovery key to access messages: " +
                recoveryKey);
        }

        // Attempt to re-share self-custody share
        // Create new share and send to beneficiaries
        byte[] selfCustodyShare = retrieveSelfCustodyShare(message);
        if (selfCustodyShare != null) {
            for (Beneficiary beneficiary : dms.getBeneficiaries()) {
                sendEncryptedShare(beneficiary, selfCustodyShare);
            }
        }
    }
}
```

---

## Flow 5: Blockchain Migration

```
Scenario:
┌─────────────────────────────────────────────────────────────┐
│ 2025: Message created, shares on Bitcoin/Ethereum          │
│ 2030: Quantum computers threaten RSA/ECDSA                 │
│ 2032: Industry migrates to post-quantum cryptography       │
│ 2035: Message unlocks - are shares still secure?           │
└─────────────────────────────────────────────────────────────┘
```

### 🚨 Problems Identified:

#### Problem 5.1: Cryptographic Obsolescence

**Solution: Migration Protocol**

```java
public class CryptoMigration {

    @Scheduled(cron = "0 0 0 1 * *") // Monthly
    public void checkCryptoHealth() {
        // Check if current crypto is still secure
        CryptoHealthReport report = cryptoHealthService.analyze();

        if (report.isCritical()) {
            // Critical: RSA broken by quantum computers
            notifyAllUsers("URGENT: Security migration required");
            initiateEmergencyMigration();

        } else if (report.needsUpgrade()) {
            // Gradual upgrade recommended
            offerVoluntaryMigration();
        }
    }

    public void migrateMessage(String messageId) {
        Message message = messageRepo.findById(messageId).orElseThrow();

        if (message.getStatus() == MessageStatus.UNLOCKED) {
            // Already unlocked - no migration needed
            return;
        }

        // Step 1: Unlock message early (with user consent)
        byte[] key = earlyUnlock(message); // Retrieve shares before quantum attack

        // Step 2: Re-encrypt with post-quantum algorithm
        byte[] newKey = generatePostQuantumKey();
        byte[] newCiphertext = encryptPostQuantum(message.getPlaintext(), newKey);

        // Step 3: Re-share with new algorithm
        List<Share> newShares = shamirPQ.split(newKey, 5, 3); // Post-quantum Shamir

        // Step 4: Deploy to quantum-resistant blockchains
        deployToPQBlockchains(newShares, message.getUnlockDate());

        // Step 5: Update message record
        message.setCryptoVersion("post-quantum-v1");
        message.setMigrated(true);
        messageRepo.save(message);
    }
}
```

---

## Flow 6: Bulk Unlock (Thousands of Messages Same Day)

```
Scenario:
┌─────────────────────────────────────────────────────────────┐
│ New Year's Day 2030: 10,000 messages unlock                │
│ All created for "10 years from New Year 2020"              │
│                                                             │
│ System needs to:                                            │
│ - Check 10,000 × 5 = 50,000 blockchain transactions        │
│ - Retrieve 30,000 shares (3 per message)                   │
│ - Decrypt 10,000 messages                                  │
│ - Send 10,000 emails                                       │
│                                                             │
│ Time available: Should complete within hours, not days     │
└─────────────────────────────────────────────────────────────┘
```

### 🚨 Problems Identified:

#### Problem 6.1: Unlock Storm

**Solution: Rate Limiting + Queue System**

```java
@Service
public class UnlockOrchestrator {

    @Autowired
    private RabbitMQTemplate rabbitMQ;

    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void queueUnlockableMessages() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);

        // Find messages that became unlockable in last hour
        List<Message> unlockable = messageRepo.findByUnlockDateBetween(
            oneHourAgo, now
        ).filter(m -> m.getStatus() == MessageStatus.LOCKED);

        log.info("Found {} messages to unlock", unlockable.size());

        // Queue them for processing
        for (Message message : unlockable) {
            UnlockTask task = new UnlockTask(message.getId());
            rabbitMQ.convertAndSend("unlock-queue", task);
        }
    }

    // Multiple workers process unlock queue
    @RabbitListener(queues = "unlock-queue", concurrency = "10-50")
    public void processUnlockTask(UnlockTask task) {
        try {
            unlockService.unlockMessage(task.getMessageId());
        } catch (Exception e) {
            log.error("Failed to unlock {}: {}", task.getMessageId(), e);

            // Retry with backoff
            if (task.getRetryCount() < MAX_RETRIES) {
                task.incrementRetry();
                rabbitMQ.convertAndSend("unlock-retry-queue", task);
            } else {
                // Move to dead letter queue for manual intervention
                rabbitMQ.convertAndSend("unlock-failed-queue", task);
            }
        }
    }
}
```

---

## Summary: Critical Flows Fixed

| Flow | Problem | Solution |
|------|---------|----------|
| **Create** | Partial deployment | Retry + recovery + alternative chains |
| **Create** | User abandons | Draft system + cleanup jobs |
| **Create** | Payment fails | Pre-authorization + balance system |
| **Unlock** | Only 2 shares | Retry with backoff + fallback chains |
| **Unlock** | Blockchain fork | Monitor multiple chains, accept any |
| **Unlock** | Race condition | Distributed locks (Redis) |
| **Update** | User forgets | Multi-channel reminders (email/SMS/push) |
| **Update** | Unauthorized change | Verification + notification to old email |
| **Death** | Account deleted | Independent delivery flag |
| **Death** | Self-custody lost | Dead man's switch + beneficiaries |
| **Migration** | Crypto obsolete | Re-encrypt with post-quantum |
| **Bulk** | 10k unlocks same day | Queue system + horizontal scaling |

Should I dive deeper into any specific flow?
