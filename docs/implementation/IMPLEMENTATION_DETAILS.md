# Tresor - Implementation Details & Architecture

## Table of Contents
1. [App Design & User Experience](#app-design--user-experience)
2. [What Gets Encrypted vs Not Encrypted](#what-gets-encrypted-vs-not-encrypted)
3. [Decentralized Infrastructure (No Server Downtime)](#decentralized-infrastructure)
4. [Message Delivery Mechanism](#message-delivery-mechanism)
5. [Cost Analysis](#cost-analysis)

---

## App Design & User Experience

### Visual Design Philosophy
- **Minimal & Timeless**: Clean interface that won't feel dated in 10 years
- **Calming Colors**: Soft blues, whites, muted tones (time/reflection theme)
- **Typography**: Clear, readable fonts (accessibility for all ages)
- **No Trendy UI**: Avoid design trends that will age poorly

### Main Screens & User Flows

#### 1. Home Screen / Dashboard

```
┌─────────────────────────────────────────────────────┐
│  Tresor                                    [@User]   │
├─────────────────────────────────────────────────────┤
│                                                       │
│         📝 Write to Your Future Self                 │
│                                                       │
│  ┌─────────────────────────────────────────────┐   │
│  │                                               │   │
│  │     [Compose New Message Button]             │   │
│  │                                               │   │
│  └─────────────────────────────────────────────┘   │
│                                                       │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                       │
│  📬 Your Messages                                    │
│                                                       │
│  ┌─ Pending Messages ─────────────────────────┐   │
│  │                                               │   │
│  │  🔒 [Locked icon]  Dec 25, 2025              │   │
│  │     "Christmas 2025"                          │   │
│  │     Unlocks in 354 days                       │   │
│  │                                               │   │
│  │  🔒 [Locked icon]  Jan 1, 2030               │   │
│  │     "New Decade Reflection"                   │   │
│  │     Unlocks in 1,824 days                     │   │
│  │                                               │   │
│  └─────────────────────────────────────────────┘   │
│                                                       │
│  ┌─ Received Messages ──────────────────────────┐  │
│  │                                               │   │
│  │  ✉️ [Envelope icon] Jan 7, 2025              │   │
│  │     "2024 Resolution Check-in"                │   │
│  │     From: You, 1 year ago                     │   │
│  │                                               │   │
│  └─────────────────────────────────────────────┘   │
│                                                       │
└─────────────────────────────────────────────────────┘
```

**Key Features**:
- Clear distinction between locked (pending) and unlocked (received) messages
- You can see METADATA (title, date, countdown) but NOT content
- Visual countdown creates anticipation
- Simple, uncluttered interface

---

#### 2. Message Composer

```
┌─────────────────────────────────────────────────────┐
│  ← Back          New Message          [Help ?]       │
├─────────────────────────────────────────────────────┤
│                                                       │
│  Message Title (optional)                            │
│  ┌─────────────────────────────────────────────┐   │
│  │ My 30th Birthday Message                     │   │
│  └─────────────────────────────────────────────┘   │
│                                                       │
│  When should this be delivered?                      │
│  ┌─────────────────────────────────────────────┐   │
│  │  📅  June 15, 2030    🕐  9:00 AM           │   │
│  └─────────────────────────────────────────────┘   │
│                                                       │
│  ⏱️  This is 1,825 days from now (5.0 years)        │
│  🔗  Bitcoin block: ~872,560 (approx.)              │
│                                                       │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                       │
│  Your Message                                        │
│  ┌─────────────────────────────────────────────┐   │
│  │ Dear future me,                               │   │
│  │                                               │   │
│  │ I'm writing this on my 25th birthday...      │   │
│  │                                               │   │
│  │ [Rich text editor]                            │   │
│  │                                               │   │
│  │                                               │   │
│  │                                               │   │
│  └─────────────────────────────────────────────┘   │
│                                                       │
│  📎 Attach files (photos, videos, documents)         │
│  [+ Add attachment]                                  │
│                                                       │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                       │
│  [Preview Message]      [Encrypt & Send →]          │
│                                                       │
└─────────────────────────────────────────────────────┘
```

**User Flow**:
1. User writes message (rich text, can include formatting)
2. Selects future date/time
3. System calculates Bitcoin block height and shows it (educational)
4. Optional: Add attachments (photos, videos, etc.)
5. Preview message before encryption
6. Click "Encrypt & Send" → Goes to confirmation screen

---

#### 3. Encryption Confirmation Screen

```
┌─────────────────────────────────────────────────────┐
│          ⚠️  Before You Lock This Message            │
├─────────────────────────────────────────────────────┤
│                                                       │
│  Important: Once encrypted, you CANNOT read this     │
│  message until June 15, 2030.                        │
│                                                       │
│  ✅ What you CAN do:                                 │
│    • Update where this message is delivered          │
│    • Cancel/delete this message anytime              │
│    • View when it will unlock                        │
│                                                       │
│  ❌ What you CANNOT do:                              │
│    • Read the message content early                  │
│    • Edit the message after encryption               │
│    • Change the delivery date                        │
│                                                       │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                       │
│  Delivery Settings                                   │
│                                                       │
│  Where should we send this when it unlocks?          │
│  ✉️ Email: john@example.com ✏️ [Change]             │
│  📱 SMS: +1-555-0123      ✏️ [Change]                │
│  📲 Push: Enabled          ✏️ [Settings]             │
│                                                       │
│  💡 Tip: You can update these addresses anytime      │
│  in your profile settings, even after encryption.    │
│                                                       │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                       │
│  Storage Cost: $0.01 (one-time)                      │
│  Message size: 850 KB                                │
│                                                       │
│  [← Go Back]           [✓ Confirm & Encrypt]         │
│                                                       │
└─────────────────────────────────────────────────────┘
```

**Key Points**:
- Clear warning about irreversibility
- Shows what can/cannot be changed later
- Confirms delivery settings (which ARE changeable)
- Transparent about costs
- Gives user a chance to review before committing

---

#### 4. Profile / Delivery Settings

```
┌─────────────────────────────────────────────────────┐
│  Profile & Delivery Settings                         │
├─────────────────────────────────────────────────────┤
│                                                       │
│  🆔 Your Unique ID                                   │
│  ┌─────────────────────────────────────────────┐   │
│  │ tresor_a7f3e9c2b1d4                          │   │
│  │ [Copy ID]                                     │   │
│  └─────────────────────────────────────────────┘   │
│                                                       │
│  This ID never changes. Use it to access your        │
│  messages even if you lose access to your email.     │
│                                                       │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                       │
│  📬 Delivery Methods (Unencrypted - You Can Update)  │
│                                                       │
│  Primary Email                                       │
│  ┌─────────────────────────────────────────────┐   │
│  │ john@example.com              [Verified ✓]   │   │
│  │ [Edit] [Remove]                               │   │
│  └─────────────────────────────────────────────┘   │
│                                                       │
│  + Add Another Email                                 │
│                                                       │
│  Primary Phone                                       │
│  ┌─────────────────────────────────────────────┐   │
│  │ +1-555-0123                   [Verified ✓]   │   │
│  │ [Edit] [Remove]                               │   │
│  └─────────────────────────────────────────────┘   │
│                                                       │
│  + Add Another Phone                                 │
│                                                       │
│  Push Notifications                                  │
│  ┌─────────────────────────────────────────────┐   │
│  │ Enabled on this device        [Active ✓]     │   │
│  └─────────────────────────────────────────────┘   │
│                                                       │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                       │
│  🛡️ Emergency Contacts (For Long-Term Messages)     │
│                                                       │
│  If you don't respond to messages for 10+ years,     │
│  these trusted contacts can help update your info.   │
│                                                       │
│  + Add Emergency Contact                             │
│                                                       │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                       │
│  📅 Proof of Life Reminder                           │
│  We'll ask you to confirm your contact info          │
│  annually: [On] Every January 1st                    │
│                                                       │
└─────────────────────────────────────────────────────┘
```

**Critical Feature**: Delivery settings are NOT encrypted, so you can update them even after messages are locked!

---

#### 5. Message Received Screen

```
┌─────────────────────────────────────────────────────┐
│  🎉 You Have a Message from Your Past Self!          │
├─────────────────────────────────────────────────────┤
│                                                       │
│  Sent: June 15, 2020                                 │
│  Received: June 15, 2025                             │
│  Time capsule: 5 years                               │
│                                                       │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                       │
│  "My 30th Birthday Message"                          │
│                                                       │
│  Dear future me,                                     │
│                                                       │
│  I'm writing this on my 25th birthday. I just        │
│  graduated from university and I'm starting my       │
│  first job next week. I'm nervous but excited...     │
│                                                       │
│  [Full message content displayed]                    │
│                                                       │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                       │
│  📎 Attachments (3)                                  │
│  🖼️ graduation.jpg [View]                            │
│  🖼️ first_apartment.jpg [View]                       │
│  📄 goals_2020.pdf [Download]                        │
│                                                       │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                       │
│  [💬 Reply to Past Self]  [Share Story]  [Archive]  │
│                                                       │
└─────────────────────────────────────────────────────┘
```

---

## What Gets Encrypted vs Not Encrypted

This is **critically important** for the architecture!

### ✅ ENCRYPTED (Time-Locked, Cannot Be Changed)

**Message Content**:
```javascript
{
  "content": "Dear future me, I'm writing this...",
  "attachments": [
    { "data": "<binary_photo_data>", "filename": "photo.jpg" },
    { "data": "<binary_video_data>", "filename": "video.mp4" }
  ],
  "created_at": "2025-01-07T10:30:00Z",
  "user_note": "Any private notes or tags"
}
```

**Why Encrypted**:
- This is the sensitive data you want time-locked
- Cannot be read by anyone (including us) until unlock date
- Prevents premature access
- Protects your privacy

**Encryption Method**:
- Bitcoin blockchain + witness encryption (primary)
- Time-lock puzzle (fallback)
- AES-256 for actual content, time-lock protects the decryption key

---

### ❌ NOT ENCRYPTED (Metadata, Can Be Updated)

**Message Metadata**:
```javascript
{
  "message_id": "msg_a7f3e9c2b1d4",
  "user_id": "tresor_user_12345",
  "unlock_date": "2030-06-15T09:00:00Z",
  "bitcoin_block_target": 872560,
  "title": "My 30th Birthday Message",  // Optional, user-visible
  "message_size_kb": 850,
  "created_date": "2025-01-07T10:30:00Z",
  "status": "locked" | "unlocked" | "delivered",
  "encrypted_payload_hash": "sha256_of_encrypted_content",
  "storage_location": {
    "arweave_tx_id": "arweave_transaction_id",
    "ipfs_cid": "QmXyz123...",
    "filecoin_deal_id": "filecoin_id"
  }
}
```

**Delivery Settings** (stored separately, updateable):
```javascript
{
  "message_id": "msg_a7f3e9c2b1d4",
  "user_id": "tresor_user_12345",
  "delivery_methods": [
    {
      "type": "email",
      "address": "john@example.com",
      "verified": true,
      "priority": 1
    },
    {
      "type": "email",
      "address": "john.work@company.com",
      "verified": true,
      "priority": 2
    },
    {
      "type": "sms",
      "phone": "+1-555-0123",
      "verified": true,
      "priority": 1
    },
    {
      "type": "push",
      "device_token": "firebase_token_xyz",
      "verified": true,
      "priority": 3
    }
  ],
  "emergency_contacts": [
    {
      "name": "Jane Doe",
      "relationship": "spouse",
      "email": "jane@example.com",
      "can_update_delivery": true
    }
  ],
  "last_updated": "2027-03-15T14:22:00Z"
}
```

**Why NOT Encrypted**:
- **Delivery addresses must be updateable**: You change email/phone over 5-10 years
- **Metadata helps with management**: See what messages you have pending
- **No sensitive content**: Just delivery info, not the actual message
- **System needs to read this**: To monitor blockchain and send notifications

---

### Data Architecture Diagram

```
┌───────────────────────────────────────────────────────────┐
│                         User Interface                     │
└─────────────────────┬─────────────────────────────────────┘
                      │
        ┌─────────────┴─────────────┐
        │                           │
        v                           v
┌───────────────┐          ┌───────────────────┐
│   ENCRYPTED   │          │   NOT ENCRYPTED   │
│   (Time-Lock) │          │   (Updateable)    │
├───────────────┤          ├───────────────────┤
│ • Content     │          │ • User ID         │
│ • Attachments │          │ • Message ID      │
│ • Private     │          │ • Unlock date     │
│   notes       │          │ • Title           │
│               │          │ • Status          │
│               │          │ • Delivery emails │
│               │          │ • Delivery phones │
│               │          │ • Storage refs    │
└───────┬───────┘          └────────┬──────────┘
        │                           │
        v                           v
┌────────────────┐         ┌─────────────────┐
│ Arweave        │         │ Traditional DB  │
│ (Permanent)    │         │ or Blockchain   │
│                │         │ (Updateable)    │
└────────────────┘         └─────────────────┘
```

**Key Insight**: By separating encrypted content from delivery metadata, we solve the "changed email" problem!

---

## Decentralized Infrastructure

### The Problem with Traditional Servers

Traditional app architecture:
```
Users → Company Servers → Database → Company owns everything
                ↓
        If company dies, app dies
        If servers down, app down
        If database corrupted, data lost
```

This fails for 5-10 year time horizons!

### Tresor's Decentralized Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Multiple Clients                      │
│  (Web app, Mobile PWA, CLI tool, Third-party apps)      │
└────────────────┬────────────────────────────────────────┘
                 │
                 v
┌────────────────────────────────────────────────────────┐
│              API Gateway / Load Balancer                │
│         (Multiple providers: Cloudflare, AWS, etc.)    │
└────────────────┬───────────────────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
        v                 v
┌───────────────┐  ┌──────────────────┐
│  Web2 Layer   │  │  Web3 Layer      │
│  (Monitoring) │  │  (Storage)       │
├───────────────┤  ├──────────────────┤
│ • Bitcoin     │  │ • Arweave        │
│   blockchain  │  │   (permanent)    │
│   monitoring  │  │ • Filecoin       │
│ • Notification│  │   (redundant)    │
│   service     │  │ • IPFS           │
│ • User DB     │  │   (distributed)  │
│   (metadata)  │  │                  │
│               │  │ Encrypted        │
│ Redundant     │  │ payloads         │
│ across 3+     │  │ stored here      │
│ providers     │  │                  │
└───────────────┘  └──────────────────┘
```

### How This Prevents Server Downtime

#### 1. Encrypted Messages: Permanent Decentralized Storage

**Arweave** (Primary):
- Blockchain-based permanent storage
- One-time payment, stored forever
- 200+ year design horizon
- Data replicated across thousands of nodes globally
- Economic model ensures miners keep data
- If Tresor company disappears, messages still exist on Arweave

**Filecoin** (Redundant):
- Decentralized storage network
- Proof-of-storage ensures data integrity
- Multiple copies across global miners
- Storage deals enforced by blockchain

**IPFS** (Content Addressing):
- Content-addressable storage (data identified by hash)
- Any node can serve the data
- No single point of failure

**Result**: Even if Tresor company completely disappears, your encrypted messages are safe on these networks!

#### 2. Metadata & Delivery Info: Redundant Databases

**Multiple Options**:

**Option A: Traditional Database (Replicated)**
- PostgreSQL replicated across 3+ cloud providers
- AWS RDS (US), Google Cloud SQL (EU), Azure (Asia)
- Automatic failover
- Hourly backups to decentralized storage

**Option B: Blockchain-Based**
- Store delivery metadata on Ethereum/Polygon
- Smart contract manages user profiles
- Immutable, decentralized, no server needed
- More expensive but truly permanent

**Option C: Hybrid** (Recommended)
- Day-to-day: Fast traditional DB (replicated)
- Nightly backup: Smart contract on blockchain
- If DB fails, reconstruct from blockchain

#### 3. Monitoring Service: Blockchain is the Truth

**Bitcoin Blockchain Monitoring**:
- We don't control Bitcoin, we just monitor it
- Multiple services watch for block heights:
  - Our own nodes (3+ geographically distributed)
  - Third-party blockchain APIs (BlockCypher, Blockchain.com, etc.)
  - Public Bitcoin nodes

**Decentralized Monitoring**:
```javascript
// Multiple independent monitors
const monitors = [
  { provider: "TresorNode1", location: "US" },
  { provider: "TresorNode2", location: "EU" },
  { provider: "TresorNode3", location: "Asia" },
  { provider: "BlockCypher API", external: true },
  { provider: "Blockchain.com API", external: true }
];

// Message unlocks when 3+ monitors agree block height reached
if (consensusReached(monitors, targetBlockHeight)) {
  unlockMessage();
  sendNotifications();
}
```

**Fail-safe**: Even if all our monitors go down, anyone can decrypt the message once Bitcoin reaches the block height (witness encryption property!)

#### 4. Notification Service: Multi-Provider

```
Message unlocked → Trigger notifications
                       ↓
        ┌──────────────┼──────────────┐
        v              v              v
    Email          SMS            Push

  SendGrid       Twilio        Firebase
  Mailgun        Vonage        OneSignal
  AWS SES        Plivo         Custom

  (Try all providers until one succeeds)
```

**Redundancy**: If SendGrid is down, try Mailgun, then AWS SES, etc.

#### 5. Open Source: Community Can Run It

**GitHub Repository**:
```
tresor/
├── client/          # Web app (React, open source)
├── server/          # Monitoring service (Node.js, open source)
├── smart-contracts/ # Ethereum contracts (Solidity, open source)
├── crypto/          # Encryption library (Rust, open source)
└── docs/            # Complete documentation
```

**If Tresor company dies**:
1. Code is open source on GitHub
2. Community can fork and run their own instances
3. Encrypted messages already on Arweave (permanent)
4. Anyone can monitor Bitcoin blockchain
5. Decryption happens client-side (in user's browser)

**Docker Deployment**:
```bash
# Anyone can run the monitoring service
docker pull tresor/monitor:latest
docker run -e BITCOIN_NODE=your_node -e ARWEAVE_GATEWAY=your_gateway tresor/monitor
```

### Survival Scenarios

| Scenario | Result |
|----------|--------|
| Tresor company bankrupt | Messages safe on Arweave, community runs monitors |
| AWS goes down | Failover to Google Cloud, then Azure |
| Arweave network fails | Filecoin and IPFS backups still exist |
| Bitcoin blockchain stops | Time-lock puzzle fallback activates |
| User loses password | Unique ID + email recovery |
| All email providers block us | SMS, push, web portal still work |
| Internet censorship | Tor hidden service, IPFS gateway |
| Company acquired by evil corp | Data already encrypted, they can't read it |

---

## Message Delivery Mechanism

### Step-by-Step: What Happens When Message Unlocks

#### Phase 1: Blockchain Reaches Target

```
Day 1: User sends message (target: block 872,560)
  ↓
Days 2-1825: Monitoring service checks Bitcoin blockchain daily
  ↓
Day 1825: Block 872,560 mined! ✓
```

#### Phase 2: Automatic Decryption

```
Monitor detects block height reached
  ↓
Retrieve encrypted message from Arweave
  ↓
Use witness encryption to derive decryption key
  (This is instant, mathematical property of Bitcoin block)
  ↓
Decrypt message content
  ↓
Store decrypted message in user's inbox (encrypted at rest with user's key)
  ↓
Update message status: "locked" → "unlocked"
```

#### Phase 3: Multi-Channel Notification

```
Message unlocked
  ↓
Retrieve user's current delivery settings (from updateable DB)
  ↓
Send notifications in parallel:

┌─────────────────┐
│  Email #1       │ → SendGrid → john@example.com
│  Priority: 1    │    ↓ (if fails)
└─────────────────┘    Mailgun → john@example.com
                       ↓ (if fails)
                       AWS SES → john@example.com

┌─────────────────┐
│  Email #2       │ → SendGrid → john.work@company.com
│  Priority: 2    │
└─────────────────┘

┌─────────────────┐
│  SMS            │ → Twilio → +1-555-0123
│  Priority: 1    │    ↓ (if fails)
└─────────────────┘    Vonage → +1-555-0123

┌─────────────────┐
│  Push           │ → Firebase → User's devices
│  Priority: 3    │
└─────────────────┘

┌─────────────────┐
│  Web Portal     │ → Always available at tresor.io/inbox
│  Always on      │    (User can login with Unique ID)
└─────────────────┘
```

#### Phase 4: Delivery Confirmation

**Email Example**:
```
From: messages@tresor.io
To: john@example.com
Subject: 🎉 You have a message from 5 years ago!

Hi John,

You have a new message from your past self!

Sent: January 7, 2020
Unlocked: January 7, 2025
Subject: "My 30th Birthday Message"

Click here to read your message:
https://tresor.io/messages/msg_a7f3e9c2b1d4

Or login to your account at https://tresor.io

Your unique ID: tresor_user_12345
(Save this ID - you can always access messages with it)

---
Tresor - Messages to Your Future Self
```

**SMS Example**:
```
Tresor: You have a message from 5 years ago! "My 30th Birthday Message" - Read it here: https://tresor.io/m/msg_a7f3e9c2b1d4
```

**Push Notification**:
```
📬 Tresor
You have a message from 5 years ago!
"My 30th Birthday Message"
[Tap to read]
```

#### Phase 5: Persistent Access

- Message stays in user's web portal inbox forever
- Can re-read anytime
- Can download content and attachments
- Can delete if desired
- Delivery receipts logged (when user opens it)

### Delivery Failure Handling

**What if all delivery methods fail?**

1. **Retry Strategy**:
   ```
   Attempt 1: Immediately
   Attempt 2: 1 hour later
   Attempt 3: 6 hours later
   Attempt 4: 24 hours later
   Attempt 5: 7 days later
   Attempt 6: 30 days later
   ```

2. **Emergency Contacts**:
   - After 30 days, notify emergency contacts
   - "John has a message waiting, but we can't reach him"
   - Emergency contact can help update delivery info

3. **Permanent Availability**:
   - Message always in web portal
   - User can check anytime with Unique ID
   - API available for third-party integrations

4. **Annual Proof-of-Life**:
   - System sends yearly reminder to verify contact info
   - "You have 3 messages pending for future delivery"
   - "Please confirm your email/phone are still correct"

### Changing Delivery Address (The Key Feature!)

**User updates email in 2027**:
```
1. User logs into Tresor
2. Goes to Profile → Delivery Settings
3. Changes email from john@oldcompany.com to john@newcompany.com
4. System sends verification email to new address
5. User confirms
6. ✓ Updated!

Now when message unlocks in 2030, it goes to the NEW email!
```

**Behind the scenes**:
```javascript
// Delivery settings are NOT in the encrypted payload
// They're in a separate, updateable database

UPDATE user_delivery_settings
SET primary_email = 'john@newcompany.com',
    verified = true,
    updated_at = '2027-03-15'
WHERE user_id = 'tresor_user_12345';

// The encrypted message on Arweave is unchanged
// Only the delivery metadata is updated
```

**This is why encryption architecture matters!**
- Encrypted: Message content (can't change)
- Not encrypted: Delivery addresses (can change!)

---

## Cost Analysis

### Encryption Costs (Computational)

**Good news: Encryption is virtually free!**

#### Client-Side Encryption
```
User's device does the encryption:
- AES-256 encryption: ~10 MB/second on modern CPU
- 1 MB message: ~0.1 seconds
- CPU cost: Negligible (user's device)
- Our cost: $0
```

#### Witness Encryption (Bitcoin-based)
```
One-time setup per message:
- Generate witness encryption parameters
- Computation time: ~1-5 seconds (depending on implementation)
- Can be done on user's device or our server
- CPU cost: Pennies (if done on server)
- Our cost per message: < $0.001
```

#### Time-Lock Puzzle (Fallback)
```
Generating puzzle:
- RSA key generation: ~0.1 seconds
- Sequential squaring computation: ~1 second
- CPU cost: Minimal
- Our cost per message: < $0.001
```

**Total encryption cost: < $0.01 per message**

### Storage Costs (Significant)

#### Arweave (Permanent Storage)

**Current pricing (January 2025)**:
- ~$25 per GB (one-time payment)
- ~$0.025 per MB
- ~$0.000025 per KB

**Example costs**:
```
Text-only message (50 KB):          $0.00125
Message with 2 photos (2 MB):       $0.05
Message with video (50 MB):         $1.25
Rich media message (500 MB):        $12.50
```

**Average user message**: ~100 KB (text + small photo)
**Cost**: ~$0.0025 per message

**Business model**:
- Free tier: We subsidize up to 1 MB/message
- Premium: User pays for large files
- One-time fee for very large messages

#### Filecoin (Redundant Backup)

**Current pricing**:
- ~$0.19 per TB per month = ~$0.000000019 per KB per month
- For 10-year storage: ~$0.0000023 per KB

**Much cheaper, but**:
- Requires ongoing storage deals
- Not "permanent" like Arweave
- Better for redundancy than primary storage

**Use case**: Automatic backup for extra reliability

#### IPFS (Free, but not permanent)

**Pricing**: Free if you run nodes, or ~$5/month for pinning services
**Use case**: Fast retrieval, content addressing
**Limitation**: Data can disappear if no one pins it

### Ongoing Operational Costs

#### Blockchain Monitoring
```
Bitcoin node operation:
- AWS EC2 t3.medium: ~$30/month
- Bandwidth: ~$10/month
- Or use public APIs: Free tier or ~$50/month

For redundancy (3 nodes): ~$150/month
Per message cost (amortized over 10,000 messages/month): $0.015
```

#### Notification Delivery
```
Email (SendGrid):
- Free tier: 100 emails/day
- Paid: $0.0001 per email
Cost per message: $0.0001

SMS (Twilio):
- $0.0075 per SMS
Cost per message: $0.0075

Push (Firebase):
- Free for most usage
Cost per message: $0

Average notification cost: ~$0.008 per message
```

#### Database & Hosting
```
PostgreSQL (managed):
- AWS RDS: ~$50/month
- Replication: +$50/month

Web hosting:
- Cloudflare: $20/month
- CDN: ~$10/month

Per message cost (10,000 messages/month): $0.013
```

### Total Cost per Message

| Component | Cost |
|-----------|------|
| Encryption (computation) | $0.001 |
| Arweave storage (100 KB) | $0.0025 |
| Filecoin backup (100 KB, 10 years) | $0.0002 |
| Blockchain monitoring (amortized) | $0.015 |
| Notifications (email + SMS) | $0.008 |
| Database & hosting (amortized) | $0.013 |
| **Total** | **$0.042** |

**~$0.04 per message** for a small (100 KB) message

**Larger messages**:
- 1 MB message: ~$0.065 (mostly storage)
- 10 MB message: ~$0.29 (mostly storage)
- 100 MB message: ~$2.54 (mostly storage)

### Pricing Strategy

**Free Tier**:
- 3 messages/year
- Up to 1 MB each
- Max 5 years in future
- **Our cost**: $0.195/year per user
- **Price**: Free (customer acquisition)

**Personal Tier** - $5/year:
- 50 messages/year
- Up to 10 MB each
- Max 10 years
- **Our cost**: ~$3.25/year per user (at 50 KB avg)
- **Margin**: ~$1.75/year (~35% margin)

**Legacy Tier** - $50/year:
- Unlimited messages
- Up to 100 MB each
- Max 50 years
- **Our cost**: ~$10-20/year (depending on usage)
- **Margin**: ~$30-40/year

**One-time Messages**:
- For very long durations (20+ years)
- User pays direct cost + margin
- $1 + $0.025/MB
- Example: 10 MB message = $1.25

### Cost Optimization Strategies

1. **Compression**:
   - GZIP compression before encryption
   - Typical 60-70% size reduction for text
   - Reduces storage costs significantly

2. **Deduplication**:
   - If user sends same photo in multiple messages
   - Store once, reference multiple times
   - Saves storage cost

3. **Tiered Storage**:
   - First 5 years: Arweave + Filecoin + IPFS
   - After 5 years: Arweave only (most reliable)
   - Reduces ongoing costs

4. **Bulk Deals**:
   - Negotiate Arweave/Filecoin bulk pricing
   - As we scale, per-GB costs decrease

5. **Open Source Infrastructure**:
   - Community-run nodes (like Mastodon)
   - Reduces our infrastructure costs
   - Users can self-host if desired

### Is It Expensive?

**Compared to alternatives**:

| Service | Cost per Message | Duration | Features |
|---------|-----------------|----------|----------|
| FutureMe | Free (ad-supported) | Any | Email only, no encryption |
| Physical time capsule | $50-200 (one-time) | Lifetime | Physical only, location-dependent |
| Safe deposit box | $50-200/year | Annual | Physical only, requires retrieval |
| **Tresor** | **$0.04-2.50** | **Up to 50+ years** | **Encrypted, digital, multi-channel** |

**Conclusion**: Very affordable for the value provided!

**The real cost is marketing/customer acquisition**, not infrastructure.

---

## Security Considerations

### What Could Go Wrong?

1. **User's device compromised during encryption**:
   - Malware could steal message before encryption
   - **Mitigation**: Warn users to encrypt on trusted devices
   - Consider hardware security module (HSM) integration for high-value messages

2. **Quantum computing breaks encryption**:
   - Future quantum computers might break RSA, elliptic curves
   - **Mitigation**: Post-quantum cryptography migration path
   - Re-encryption option for very long-term messages

3. **Bitcoin blockchain compromised**:
   - Extremely unlikely (massive hashrate)
   - **Mitigation**: Time-lock puzzle fallback doesn't depend on Bitcoin

4. **Arweave network fails**:
   - Economic model could fail over decades
   - **Mitigation**: Multi-provider redundancy, self-custody option

5. **Witness encryption vulnerability discovered**:
   - Still active research area
   - **Mitigation**: Regular security audits, ability to re-encrypt

### Security Best Practices

- Open source cryptographic implementation
- Regular third-party security audits
- Bug bounty program
- Responsible disclosure policy
- Clear security documentation
- User education about limitations

---

## Summary

### App Design
- **Clean, minimal UI** that won't feel dated
- **Clear distinction** between locked (metadata visible) and unlocked (content visible)
- **Educational elements** (Bitcoin block height, encryption status)
- **Mobile-first** Progressive Web App

### Encrypted vs Not Encrypted
- **Encrypted**: Message content, attachments, private notes
- **Not Encrypted**: Delivery addresses, unlock dates, titles, status
- **Key insight**: Separating these allows updating delivery info while keeping content locked

### No Server Downtime
- **Decentralized storage**: Arweave (permanent), Filecoin (redundant), IPFS (distributed)
- **Multiple monitoring nodes**: Geographic redundancy, third-party APIs
- **Open source**: Community can run instances if company fails
- **Blockchain doesn't go down**: Bitcoin is the "server"

### Message Delivery
- **Multi-channel**: Email (multiple), SMS (multiple), Push, Web portal
- **Retry strategy**: Multiple attempts over 30 days
- **Emergency contacts**: Backup notification method
- **Always accessible**: Web portal with Unique ID

### Costs
- **Encryption**: ~$0.001 per message (negligible)
- **Storage**: ~$0.0025 per 100 KB (significant for large files)
- **Operations**: ~$0.036 per message (monitoring, notifications, hosting)
- **Total**: ~$0.04 per small message, scales with file size
- **Affordable pricing**: $5/year for personal use covers costs + margin

### Business Viability
- Costs are low enough to offer free tier for growth
- Premium tiers have healthy margins (35%+)
- Primary expense will be marketing, not infrastructure
- Decentralized architecture provides long-term reliability needed for trust

---

## Next Steps

1. **Prototype the UI**: Create clickable mockups to test user experience
2. **Validate witness encryption**: Build proof-of-concept to confirm technical feasibility
3. **Cost modeling**: Run detailed financial projections with realistic user growth
4. **Legal review**: Ensure compliance with data protection laws, encryption regulations
5. **User research**: Interview potential users about willingness to pay, use cases
