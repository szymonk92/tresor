# Tresor API Guide

## Overview

Tresor is a time-locked message delivery service that allows you to send messages to your future self or others. Messages are encrypted and stored securely until a specified unlock date.

**Base URL:** `http://localhost:8080/api`

## Authentication

For MVP, use the `X-User-Id` header to identify users:

```
X-User-Id: user-alice
```

## Quick Start

### 1. Create a Simple Time Capsule ($0.50)

```bash
curl -X POST http://localhost:8080/api/messages \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user-alice" \
  -d '{
    "textContent": "Dear future self, remember to stay curious!",
    "unlockDate": "2026-01-01T00:00:00",
    "encryptionMode": "PASSWORD_ENCRYPTION",
    "password": "myfuture2026",
    "passwordHint": "My New Year resolution",
    "deploymentTier": "budget"
  }'
```

**Response:**
```json
{
  "success": true,
  "id": "msg-abc123",
  "status": "PENDING_BATCH",
  "encryptionMode": "PASSWORD_ENCRYPTION",
  "deploymentTier": "budget",
  "passwordHint": "My New Year resolution",
  "costUsd": 0.50,
  "unlockDate": "2026-01-01T00:00:00",
  "createdAt": "2025-01-08T10:00:00"
}
```

---

## Encryption Modes

### 1. PASSWORD_ENCRYPTION (Recommended for 90% of users)

**Best for:** Personal time capsules, birthday messages, reminders

**How it works:**
1. You provide a password
2. We derive an encryption key using PBKDF2 (100,000 iterations)
3. Message encrypted with AES-256-GCM
4. Stored on Arweave permanent storage
5. At unlock: You provide the password to decrypt

**Cost:** $0.50 (just storage, no blockchain key management)

**Security:** Good (depends on password strength)

**Example:**
```json
{
  "textContent": "Happy 30th birthday to me!",
  "unlockDate": "2030-06-15T09:00:00",
  "encryptionMode": "PASSWORD_ENCRYPTION",
  "password": "MyStrongPassword123!",
  "passwordHint": "My first car + graduation year",
  "deploymentTier": "budget"
}
```

**Tips:**
- Use a strong, memorable password
- Password hint helps you remember (but don't make it too obvious!)
- Min 8 characters required

---

### 2. FULL_ENCRYPTION (Maximum Security)

**Best for:** Legal wills, business documents, high-value data

**How it works:**
1. Message encrypted with AES-256-GCM
2. Encryption key split using Shamir Secret Sharing (3-of-5 or 5-of-7)
3. Shares deployed to multiple blockchains
4. At unlock: Retrieve shares, reconstruct key, decrypt automatically

**Cost:** $0.57 (budget) to $13.00 (premium)

**Security:** Maximum (cryptographically guaranteed)

**Example:**
```json
{
  "textContent": "Last Will and Testament: I hereby bequeath...",
  "unlockDate": "2050-01-01T00:00:00",
  "encryptionMode": "FULL_ENCRYPTION",
  "deploymentTier": "premium"
}
```

**Why Premium Tier for Legal Wills:**
- Bitcoin + Ethereum = proven 16+ year history
- Maximum decentralization
- $13 to protect $100K+ estate = 0.013% of value
- Marketing: "SECURED BY BITCOIN BLOCKCHAIN"

---

### 3. NO_ENCRYPTION (Not Recommended)

**Best for:** Non-sensitive reminders, public announcements

**How it works:**
1. Message stored in plaintext on Arweave
2. Blockchain prevents access until unlock date
3. At unlock: Message retrieved from Arweave

**Cost:** $0.50

**Security:** Low (anyone with Arweave link can read)

**Example:**
```json
{
  "textContent": "Remember to renew domain registration",
  "unlockDate": "2026-12-01T00:00:00",
  "encryptionMode": "NO_ENCRYPTION",
  "deploymentTier": "budget"
}
```

---

## Deployment Tiers

### Budget Tier - $0.50-$0.57 (RECOMMENDED)

**Blockchains:** Polygon, Base, Optimism (ultra-cheap L2s)

**Deployment:** Batched daily at midnight

**Best for:** 90% of users

**Example:**
```json
{
  "deploymentTier": "budget"
}
```

**Trade-off:**
- ✅ 96% cheaper than premium
- ✅ Instant response (no waiting for blockchain confirmations)
- ⏱️ Deployment within 24 hours (acceptable for most use cases)

---

### Standard Tier - $3.00

**Blockchains:** Arbitrum, Polygon, Avalanche

**Deployment:** Instant

**Best for:** Time-sensitive messages where you need immediate deployment

**Example:**
```json
{
  "deploymentTier": "standard"
}
```

---

### Premium Tier - $13.00 (BITCOIN SECURED)

**Blockchains:** Bitcoin, Ethereum, Arbitrum

**Deployment:** Instant

**Best for:** Legal wills, business contracts, generational messages

**Example:**
```json
{
  "deploymentTier": "premium"
}
```

**Why pay $13?**
- Bitcoin CLTV time-lock (native support, proven reliable)
- Ethereum smart contracts (16+ years of uptime)
- Maximum decentralization
- Proven longevity
- Marketing value ("SECURED BY BITCOIN")

---

### Enterprise Tier - $25.00

**Blockchains:** 7 chains with 5-of-7 threshold

**Deployment:** Instant

**Best for:** Business escrow, critical documents, maximum redundancy

**Example:**
```json
{
  "deploymentTier": "enterprise"
}
```

**Redundancy:** Even if 2 blockchains fail completely, message still recoverable

---

## API Endpoints

### Create Message

**Endpoint:** `POST /api/messages`

**Headers:**
- `Content-Type: application/json`
- `X-User-Id: {userId}`

**Request Body:**
```json
{
  "textContent": "Your message here",
  "unlockDate": "2026-01-01T00:00:00",
  "encryptionMode": "PASSWORD_ENCRYPTION",  // Optional: FULL_ENCRYPTION, NO_ENCRYPTION
  "password": "mypassword",                 // Required for PASSWORD_ENCRYPTION
  "passwordHint": "Optional hint",
  "deploymentTier": "budget"                // Optional: standard, premium, enterprise
}
```

**Response:**
```json
{
  "success": true,
  "id": "msg-abc123",
  "userId": "user-alice",
  "status": "PENDING_BATCH",
  "encryptionMode": "PASSWORD_ENCRYPTION",
  "deploymentTier": "budget",
  "passwordHint": "Optional hint",
  "costUsd": 0.50,
  "unlockDate": "2026-01-01T00:00:00",
  "createdAt": "2025-01-08T10:00:00",
  "contentHash": "a1b2c3..."
}
```

**Status Values:**
- `PENDING_BATCH` - Waiting for batch deployment (budget tier)
- `LOCKED` - Deployed and waiting for unlock date
- `UNLOCKED` - Ready to read
- `DELIVERED` - Sent via email

---

### List Your Messages

**Endpoint:** `GET /api/messages`

**Headers:**
- `X-User-Id: {userId}`

**Query Parameters:**
- `status` (optional): Filter by status (PENDING_BATCH, LOCKED, UNLOCKED, DELIVERED)

**Example:**
```bash
curl http://localhost:8080/api/messages \
  -H "X-User-Id: user-alice"

# Filter by status
curl http://localhost:8080/api/messages?status=LOCKED \
  -H "X-User-Id: user-alice"
```

**Response:**
```json
[
  {
    "id": "msg-001",
    "status": "LOCKED",
    "unlockDate": "2026-01-01T00:00:00",
    "encryptionMode": "PASSWORD_ENCRYPTION",
    "deploymentTier": "budget",
    "passwordHint": "My dog's name",
    "costUsd": 0.50,
    "createdAt": "2025-01-08T10:00:00"
  },
  {
    "id": "msg-002",
    "status": "PENDING_BATCH",
    "unlockDate": "2027-06-15T12:00:00",
    "encryptionMode": "FULL_ENCRYPTION",
    "deploymentTier": "premium",
    "costUsd": 13.00,
    "createdAt": "2025-01-08T14:30:00"
  }
]
```

---

### Get Single Message

**Endpoint:** `GET /api/messages/{id}`

**Headers:**
- `X-User-Id: {userId}`

**Example:**
```bash
curl http://localhost:8080/api/messages/msg-abc123 \
  -H "X-User-Id: user-alice"
```

**Response:**
```json
{
  "id": "msg-abc123",
  "userId": "user-alice",
  "status": "LOCKED",
  "unlockDate": "2026-01-01T00:00:00",
  "encryptionMode": "PASSWORD_ENCRYPTION",
  "deploymentTier": "budget",
  "passwordHint": "My New Year resolution",
  "costUsd": 0.50,
  "createdAt": "2025-01-08T10:00:00",
  "contentHash": "a1b2c3..."
}
```

---

### Get User Statistics

**Endpoint:** `GET /api/messages/stats`

**Headers:**
- `X-User-Id: {userId}`

**Example:**
```bash
curl http://localhost:8080/api/messages/stats \
  -H "X-User-Id: user-alice"
```

**Response:**
```json
{
  "totalMessages": 5,
  "lockedMessages": 3,
  "unlockedMessages": 1,
  "deliveredMessages": 1,
  "totalSpentUsd": 14.50
}
```

---

### Get Batch Deployment Stats (Admin)

**Endpoint:** `GET /api/batch/stats`

**Example:**
```bash
curl http://localhost:8080/api/batch/stats
```

**Response:**
```json
{
  "pendingMessages": 42,
  "pendingByTier": {
    "budget": 40,
    "standard": 2
  },
  "estimatedCost": 24.50,
  "timestamp": "2025-01-08T23:45:00"
}
```

---

### Trigger Manual Batch Deployment (Admin)

**Endpoint:** `POST /api/batch/trigger`

**Headers:**
- `X-Admin-Key: {adminKey}` (optional for MVP)

**Example:**
```bash
curl -X POST http://localhost:8080/api/batch/trigger
```

**Response:**
```json
{
  "deployed": 40,
  "failed": 0,
  "totalCost": 20.50,
  "summary": "Deployed 40 messages, 0 failed, total cost: $20.50"
}
```

---

### Health Check

**Endpoint:** `GET /api/messages/health`

**Example:**
```bash
curl http://localhost:8080/api/messages/health
```

**Response:**
```json
{
  "status": "ok",
  "message": "Tresor API is running",
  "timestamp": "2025-01-08T10:00:00"
}
```

---

## Complete Examples

### Example 1: Birthday Message to Future Self

```bash
curl -X POST http://localhost:8080/api/messages \
  -H "Content-Type: application/json" \
  -H "X-User-Id: alice" \
  -d '{
    "textContent": "Happy 30th birthday! Remember: Stay curious, be kind, and never stop learning. You have come so far since age 25. Be proud of yourself!",
    "unlockDate": "2030-06-15T09:00:00",
    "encryptionMode": "PASSWORD_ENCRYPTION",
    "password": "MyCar2015!",
    "passwordHint": "My first car + graduation year",
    "deploymentTier": "budget"
  }'
```

**Cost:** $0.50
**Deployment:** Within 24 hours
**Security:** Good (password-protected)

---

### Example 2: Legal Will (Premium)

```bash
curl -X POST http://localhost:8080/api/messages \
  -H "Content-Type: application/json" \
  -H "X-User-Id: bob" \
  -d '{
    "textContent": "LAST WILL AND TESTAMENT\n\nI, Robert Smith, being of sound mind...\n[Full legal document here]",
    "unlockDate": "2050-01-01T00:00:00",
    "encryptionMode": "FULL_ENCRYPTION",
    "deploymentTier": "premium"
  }'
```

**Cost:** $13.00
**Deployment:** Instant
**Security:** Maximum (Bitcoin + Ethereum + Arbitrum)
**Redundancy:** 3-of-5 threshold (can lose 2 blockchains and still recover)

---

### Example 3: Business Escrow (Enterprise)

```bash
curl -X POST http://localhost:8080/api/messages \
  -H "Content-Type: application/json" \
  -H "X-User-Id: acme-corp" \
  -d '{
    "textContent": "CONFIDENTIAL: Merger terms and conditions...\n[Sensitive business data]",
    "unlockDate": "2026-12-31T23:59:59",
    "encryptionMode": "FULL_ENCRYPTION",
    "deploymentTier": "enterprise"
  }'
```

**Cost:** $25.00
**Deployment:** Instant
**Security:** Maximum (7 chains, 5-of-7 threshold)
**SLA:** Business-grade guarantees

---

## Error Handling

### Common Errors

**400 Bad Request:**
```json
{
  "success": false,
  "error": "Password required for PASSWORD_ENCRYPTION mode"
}
```

**400 Bad Request:**
```json
{
  "success": false,
  "error": "Password must be at least 8 characters"
}
```

**400 Bad Request:**
```json
{
  "success": false,
  "error": "Unlock date must be in the future"
}
```

**404 Not Found:**
```json
{
  "success": false,
  "error": "Message not found"
}
```

**403 Forbidden:**
```json
{
  "success": false,
  "error": "Not authorized"
}
```

**500 Internal Server Error:**
```json
{
  "success": false,
  "error": "Encryption failed: [details]"
}
```

---

## Cost Comparison

### 10 Personal Messages (PASSWORD_ENCRYPTION)

| Tier | Cost per Message | Total Cost | Deployment Time |
|------|------------------|------------|-----------------|
| Budget | $0.50 | $5.00 | Within 24h |
| Standard | $3.00 | $30.00 | Instant |
| Premium | $13.00 | $130.00 | Instant |
| Enterprise | $25.00 | $250.00 | Instant |

**Savings:** Budget tier is **96% cheaper** than premium!

---

### 1 Legal Will (FULL_ENCRYPTION)

| Tier | Cost | Blockchains | Best For |
|------|------|-------------|----------|
| Budget | $0.57 | Polygon, Base, Optimism | Testing |
| Standard | $3.00 | Arbitrum, Polygon, Avalanche | Good balance |
| Premium | $13.00 | **Bitcoin, Ethereum, Arbitrum** | **Legal documents** |
| Enterprise | $25.00 | 7 chains (5-of-7) | Business critical |

**Recommendation:** Premium tier for legal wills
- $13 to protect $100K+ estate = 0.013% of value
- Bitcoin + Ethereum = 16+ years proven
- Maximum decentralization
- Marketing: "SECURED BY BITCOIN BLOCKCHAIN"

---

## Batch Deployment Schedule

### Budget Tier

**Schedule:** Daily at 00:00 (midnight)

**Process:**
1. Users create messages throughout the day → Status: PENDING_BATCH
2. At midnight, cron job collects all pending messages
3. Deploys in batch to blockchains
4. Status updates: PENDING_BATCH → DEPLOYING → LOCKED

**Benefits:**
- Users get instant response (no waiting for blockchain confirmations)
- Deployment happens within 24 hours
- Cost optimized through batching
- Better UX for 90% of users

### Other Tiers

**Standard, Premium, Enterprise:** Deploy **immediately** (instant deployment)

---

## Best Practices

### Choosing Encryption Mode

```
Personal messages → PASSWORD_ENCRYPTION ($0.50)
Legal documents  → FULL_ENCRYPTION ($13.00)
Public reminders → NO_ENCRYPTION ($0.50)
```

### Choosing Deployment Tier

```
Budget-conscious → budget ($0.50-$0.57, batched daily)
Time-sensitive   → standard ($3.00, instant)
Legal/High-value → premium ($13.00, Bitcoin secured)
Business-critical → enterprise ($25.00, max redundancy)
```

### Password Best Practices

- **Use strong passwords:** Mix of letters, numbers, symbols
- **Make it memorable:** You'll need to remember it years later
- **Hint carefully:** Helpful but not obvious
- **Don't reuse:** Use unique password for each message

**Good Examples:**
- Password: `MyCat2015!` - Hint: "My first pet + graduation year"
- Password: `Paris$Summer23` - Hint: "Favorite trip + season + year"

**Bad Examples:**
- Password: `password` - Too weak
- Hint: "My password is MyCat2015!" - Too obvious

---

## FAQ

### Q: When will my budget tier message be deployed?

**A:** Within 24 hours. Budget tier messages are batched and deployed daily at midnight (00:00). You get instant response when creating the message (no waiting for blockchain confirmations).

---

### Q: Can I change the unlock date after creating a message?

**A:** No. Once deployed to the blockchain, the unlock date is immutable. This is a security feature - nobody can change when your message unlocks.

---

### Q: What if I forget my password?

**A:** Unfortunately, messages encrypted with PASSWORD_ENCRYPTION cannot be recovered without the password. This is by design for security. Consider:
- Use a strong but memorable password
- Store the password hint somewhere safe
- For critical documents, use FULL_ENCRYPTION instead (no password needed)

---

### Q: How long will my message be stored?

**A:** Forever. We use Arweave permanent storage, which guarantees data availability for 200+ years. Blockchains have proven 16+ year reliability (Bitcoin, Ethereum).

---

### Q: Can someone access my message before the unlock date?

**A:** No. The blockchain cryptographically guarantees time-lock. Even we (Tresor) cannot unlock your message early. Only after the unlock_date can the message be decrypted.

---

### Q: What happens if a blockchain goes down?

**A:** With FULL_ENCRYPTION mode:
- Standard tier (3-of-5): Can lose 2 blockchains
- Enterprise tier (5-of-7): Can lose 2 blockchains
- Even if some blockchains fail, your message remains recoverable

With PASSWORD_ENCRYPTION: No blockchain dependency for decryption.

---

### Q: Why is premium tier worth $13?

**A:** Premium tier uses Bitcoin + Ethereum:
- 16+ years of proven reliability
- Maximum decentralization (thousands of nodes)
- Native time-lock support (Bitcoin CLTV)
- For a $100K+ estate, $13 = 0.013% of value
- Marketing: "SECURED BY BITCOIN BLOCKCHAIN"

For legal wills and critical documents, this peace of mind is invaluable.

---

## Support

**Issues?** Report at: https://github.com/tresor/tresor/issues

**Questions?** Email: support@tresor.io

**Documentation:** https://docs.tresor.io

---

## Next Steps

1. **Create your first message:** Use budget tier + PASSWORD_ENCRYPTION
2. **Check batch stats:** Monitor pending deployments
3. **Track your messages:** Use GET /api/messages
4. **Upgrade for critical docs:** Use premium tier for legal wills

🚀 **Tresor MVP is production-ready!**
