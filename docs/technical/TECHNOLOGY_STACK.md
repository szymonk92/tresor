# Tresor - Technology Stack & Architecture

## Table of Contents
1. [Architecture Philosophy](#architecture-philosophy)
2. [Do We Need a Server?](#do-we-need-a-server)
3. [Technology Stack](#technology-stack)
4. [Where Does Data Live?](#where-does-data-live)
5. [Development Phases](#development-phases)
6. [Tech Stack Decisions](#tech-stack-decisions)

---

## Architecture Philosophy

### Core Principles

1. **Encryption Client-Side**: Users' devices encrypt messages, not our servers
2. **Decentralized Storage**: Encrypted data lives on blockchain (Arweave, IPFS), not our database
3. **Minimal Server**: Server only handles coordination, not sensitive data
4. **Open Source**: Community can run their own instances
5. **Self-Custody Option**: Users can operate without any server

**Visual Architecture:**
```
┌─────────────────────────────────────────────────────────────┐
│                     USER'S DEVICE                            │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Web App / Mobile PWA (React/Next.js)              │    │
│  │  - Message composition                              │    │
│  │  - CLIENT-SIDE ENCRYPTION ← happens here!          │    │
│  │  - Key generation                                   │    │
│  │  - File compression                                 │    │
│  └────────────────────────────────────────────────────┘    │
└───────────────────┬─────────────────────────────────────────┘
                    │
        ┌───────────┴────────────┐
        │                        │
        v                        v
┌───────────────┐        ┌──────────────────┐
│ ENCRYPTED     │        │ METADATA         │
│ DATA          │        │ (not encrypted)  │
│               │        │                  │
│ Arweave       │        │ Backend Server   │
│ IPFS          │        │ PostgreSQL DB    │
│ Filecoin      │        │                  │
│               │        │ - User profiles  │
│ (permanent)   │        │ - Delivery info  │
│               │        │ - Message status │
└───────────────┘        └──────────────────┘
        │                        │
        └───────────┬────────────┘
                    │
                    v
        ┌─────────────────────┐
        │ BLOCKCHAIN MONITOR  │
        │ (Node.js service)   │
        │                     │
        │ Watches Bitcoin     │
        │ block height        │
        │                     │
        │ When target reached:│
        │ - Decrypt message   │
        │ - Send notifications│
        └─────────────────────┘
```

---

## Do We Need a Server?

### Short Answer: YES, but minimal

**What the server DOES:**
1. ✅ User account management (email, password, profile)
2. ✅ Metadata storage (delivery addresses, message status)
3. ✅ Bitcoin blockchain monitoring
4. ✅ Notification delivery (email, SMS, push)
5. ✅ API for clients to interact with
6. ✅ Payment processing (Stripe integration)

**What the server DOES NOT DO:**
1. ❌ Encrypt messages (client does this)
2. ❌ Store encrypted message content (Arweave does this)
3. ❌ Hold encryption keys (client generates, then time-locks)
4. ❌ Decrypt messages early (mathematically impossible)

**Philosophy:** Server is **coordination layer**, not **trust layer**.

### Why We Need a Server

**Without a server:**
- ❌ No way to monitor Bitcoin blockchain 24/7
- ❌ No way to send notifications when messages unlock
- ❌ No way to update delivery addresses (would need blockchain transactions)
- ❌ No way to handle payments
- ❌ Poor UX (users would need to check manually if message unlocked)

**With a minimal server:**
- ✅ Monitors blockchain automatically
- ✅ Sends notifications
- ✅ Manages user accounts
- ✅ But doesn't hold sensitive data (encrypted messages are on Arweave)
- ✅ Can be open-sourced (community can run their own)

### Server Redundancy Strategy

**Multiple deployment options:**

1. **Tresor Official Server** (default)
   - Hosted by us
   - Most users will use this
   - SaaS model

2. **Community Servers**
   - Open-source code
   - Anyone can deploy
   - If official server dies, community continues

3. **Self-Hosted**
   - Advanced users can run their own instance
   - Full control
   - Same codebase

4. **No Server (Self-Custody)**
   - Download encrypted message
   - Use CLI tool to unlock
   - No server needed at all

**Result:** Not dependent on our single server!

---

## Technology Stack

### Frontend (Client)

#### Web Application

**Framework:** Next.js 14+ (React)

**Why Next.js:**
- ✅ Server-side rendering (SEO for marketing pages)
- ✅ API routes (can handle some backend logic)
- ✅ Static export (can deploy to CDN, no server required for frontend)
- ✅ Great developer experience
- ✅ TypeScript support
- ✅ Image optimization

**UI Library:** Tailwind CSS + shadcn/ui

**Why:**
- ✅ Beautiful, accessible components
- ✅ Dark mode support
- ✅ Highly customizable
- ✅ No runtime overhead (compiled to CSS)

**State Management:** Zustand or React Context

**Why:**
- ✅ Simple, lightweight
- ✅ TypeScript-friendly
- ✅ No boilerplate (unlike Redux)

**Encryption Library:** Web Crypto API + tweetnacl

**Why:**
- ✅ Built into browsers (no dependencies)
- ✅ Standard, secure implementations
- ✅ Fast performance
- ✅ tweetnacl for witness encryption primitives

**File Handling:** Browser File API + comlink for Web Workers

**Why:**
- ✅ Encrypt large files without blocking UI
- ✅ Web Workers keep app responsive
- ✅ Progress callbacks for UX

**Example code structure:**
```typescript
// crypto/encrypt.ts
import { encryptMessage, generateKey, witnessEncrypt } from './crypto';
import { uploadToArweave } from './storage';

export async function createTimeLockedMessage(
  content: string,
  files: File[],
  unlockDate: Date
) {
  // 1. Generate AES key (client-side)
  const contentKey = await generateKey();

  // 2. Encrypt content with AES (client-side)
  const encryptedContent = await encryptMessage(content, contentKey);
  const encryptedFiles = await Promise.all(
    files.map(f => encryptFile(f, contentKey))
  );

  // 3. Calculate Bitcoin block height
  const blockHeight = await calculateBlockHeight(unlockDate);

  // 4. Time-lock the key using witness encryption
  const timeLockedKey = await witnessEncrypt(contentKey, blockHeight);

  // 5. Package everything
  const message = {
    encryptedContent,
    encryptedFiles,
    timeLockedKey,
    metadata: {
      unlockDate,
      blockHeight,
      createdAt: new Date()
    }
  };

  // 6. Upload to Arweave (decentralized storage)
  const arweaveTxId = await uploadToArweave(message);

  // 7. Register with backend (just metadata)
  await fetch('/api/messages', {
    method: 'POST',
    body: JSON.stringify({
      arweaveTxId,
      unlockDate,
      blockHeight,
      size: getTotalSize(message)
    })
  });

  return { messageId: arweaveTxId };
}
```

#### Mobile Application

**Option 1:** Progressive Web App (PWA) - Recommended for MVP

**Why:**
- ✅ Same codebase as web (Next.js)
- ✅ Works offline
- ✅ Install to home screen
- ✅ Push notifications
- ✅ No app store approval needed
- ✅ Deploy updates instantly

**Option 2:** React Native (Future)

**Why:**
- ✅ Native performance
- ✅ Better biometric integration
- ✅ App store presence
- ⚠️ More complex, maintain two codebases

**Recommendation:** Start with PWA, consider React Native later if needed.

---

### Backend (Server)

#### API Server

**Framework:** Node.js + Express.js OR Bun + Hono

**Option A: Node.js + Express**
- ✅ Mature, stable
- ✅ Huge ecosystem
- ✅ Everyone knows it
- ❌ Slower than alternatives

**Option B: Bun + Hono** (Recommended)
- ✅ Much faster (3-4x)
- ✅ Built-in TypeScript
- ✅ Better developer experience
- ✅ Drop-in Node replacement
- ⚠️ Newer (less mature)

**My recommendation:** Bun + Hono for performance, but Node.js is safer choice for MVP.

**Example API structure:**
```typescript
// server/index.ts
import { Hono } from 'hono';
import { db } from './db';
import { arweave } from './storage';

const app = new Hono();

// Create message metadata (encrypted content already on Arweave)
app.post('/api/messages', async (c) => {
  const { arweaveTxId, unlockDate, blockHeight, size } = await c.req.json();
  const userId = c.get('userId'); // from auth middleware

  // Store metadata only (not encrypted content!)
  const message = await db.messages.create({
    userId,
    arweaveTxId,
    unlockDate,
    blockHeight,
    size,
    status: 'locked'
  });

  return c.json({ messageId: message.id });
});

// Update delivery settings
app.patch('/api/users/:id/delivery', async (c) => {
  const { email, phone } = await c.req.json();

  // This is NOT encrypted, so user can update it!
  await db.users.update({
    where: { id: c.req.param('id') },
    data: { email, phone }
  });

  return c.json({ success: true });
});

// Get user's messages (metadata only)
app.get('/api/messages', async (c) => {
  const userId = c.get('userId');

  const messages = await db.messages.findMany({
    where: { userId },
    select: {
      id: true,
      unlockDate: true,
      status: true,
      title: true, // User-provided title (optional)
      // NOT selecting encrypted content!
    }
  });

  return c.json({ messages });
});
```

#### Database

**Choice:** PostgreSQL (via Supabase or Neon)

**Why PostgreSQL:**
- ✅ Reliable, mature
- ✅ Good JSON support (for flexible metadata)
- ✅ Full-text search
- ✅ Strong consistency
- ✅ Excellent TypeScript tooling (Prisma)

**Why Supabase/Neon:**
- ✅ Managed (don't manage servers)
- ✅ Generous free tier
- ✅ Automatic backups
- ✅ Real-time subscriptions (nice for notifications)
- ✅ Built-in auth (can use or skip)

**Schema:**
```sql
-- Users table
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email TEXT UNIQUE NOT NULL,
  password_hash TEXT, -- Optional, if using password auth
  unique_id TEXT UNIQUE NOT NULL, -- tresor_user_12345
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Delivery settings (separate, updateable)
CREATE TABLE delivery_settings (
  user_id UUID REFERENCES users(id),
  type TEXT NOT NULL, -- 'email', 'sms', 'push'
  address TEXT NOT NULL, -- email address, phone number, or device token
  verified BOOLEAN DEFAULT FALSE,
  priority INTEGER DEFAULT 1,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  PRIMARY KEY (user_id, type, address)
);

-- Message metadata (NOT the encrypted content!)
CREATE TABLE messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id),
  arweave_tx_id TEXT UNIQUE NOT NULL, -- Points to encrypted content on Arweave
  title TEXT, -- Optional user-provided title
  unlock_date TIMESTAMP NOT NULL,
  bitcoin_block_target INTEGER NOT NULL,
  status TEXT NOT NULL DEFAULT 'locked', -- 'locked', 'unlocked', 'delivered'
  size_bytes INTEGER NOT NULL,
  created_at TIMESTAMP DEFAULT NOW(),
  unlocked_at TIMESTAMP,
  delivered_at TIMESTAMP
);

-- Notification log
CREATE TABLE notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  message_id UUID REFERENCES messages(id),
  type TEXT NOT NULL, -- 'email', 'sms', 'push'
  address TEXT NOT NULL,
  status TEXT NOT NULL, -- 'sent', 'failed', 'opened'
  sent_at TIMESTAMP DEFAULT NOW(),
  error_message TEXT
);

-- Emergency contacts
CREATE TABLE emergency_contacts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id),
  name TEXT NOT NULL,
  email TEXT NOT NULL,
  relationship TEXT,
  can_update_delivery BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT NOW()
);
```

**ORM:** Prisma

**Why:**
- ✅ Type-safe database queries
- ✅ Automatic migrations
- ✅ Great developer experience
- ✅ Works with PostgreSQL

---

### Blockchain Monitoring Service

**Technology:** Node.js / Bun + Bitcoin RPC client

**What it does:**
1. Connects to Bitcoin node (self-hosted or API)
2. Checks current block height every 10 minutes
3. Queries database for messages with `bitcoin_block_target <= current_height`
4. For each unlockable message:
   - Fetch encrypted content from Arweave
   - Use witness encryption to derive decryption key
   - Decrypt message
   - Update status to 'unlocked'
   - Trigger notification service

**Example:**
```typescript
// services/blockchain-monitor.ts
import { Client as BitcoinClient } from 'bitcoin-core';
import { db } from './db';
import { decryptMessage } from './crypto';
import { sendNotifications } from './notifications';

const bitcoin = new BitcoinClient({
  host: process.env.BITCOIN_RPC_HOST,
  port: 8332,
  username: process.env.BITCOIN_RPC_USER,
  password: process.env.BITCOIN_RPC_PASS
});

async function checkForUnlockableMessages() {
  // Get current Bitcoin block height
  const blockHeight = await bitcoin.getBlockCount();

  console.log(`Current Bitcoin block height: ${blockHeight}`);

  // Find messages that should be unlocked
  const messages = await db.messages.findMany({
    where: {
      status: 'locked',
      bitcoin_block_target: {
        lte: blockHeight
      }
    },
    include: {
      user: {
        include: {
          deliverySettings: true
        }
      }
    }
  });

  console.log(`Found ${messages.length} messages to unlock`);

  // Process each message
  for (const message of messages) {
    try {
      // 1. Fetch encrypted content from Arweave
      const encrypted = await fetchFromArweave(message.arweave_tx_id);

      // 2. Use witness encryption to derive key (now possible!)
      const decryptedKey = await witnessDecrypt(
        encrypted.timeLockedKey,
        blockHeight
      );

      // 3. Decrypt message content
      const content = await decryptMessage(encrypted, decryptedKey);

      // 4. Store decrypted content in user's inbox (encrypted at rest with user's key)
      await storeInUserInbox(message.user_id, content);

      // 5. Update message status
      await db.messages.update({
        where: { id: message.id },
        data: {
          status: 'unlocked',
          unlocked_at: new Date()
        }
      });

      // 6. Send notifications
      await sendNotifications(message);

      console.log(`✓ Unlocked message ${message.id}`);
    } catch (error) {
      console.error(`Failed to unlock message ${message.id}:`, error);
      // Log error but continue with other messages
    }
  }
}

// Run every 10 minutes
setInterval(checkForUnlockableMessages, 10 * 60 * 1000);

// Run on startup
checkForUnlockableMessages();
```

**Bitcoin Node Options:**

**Option A: Self-Hosted Bitcoin Node**
- ✅ Full control
- ✅ No API limits
- ❌ Expensive (~$100/month for VPS with storage)
- ❌ Requires maintenance

**Option B: Bitcoin RPC API (BlockCypher, Blockchain.com)**
- ✅ Easy, managed
- ✅ Free tier available
- ❌ Rate limits
- ❌ Dependency on third party

**Option C: Hybrid (Recommended)**
- Use API for MVP
- Run own node as we scale
- Multiple API providers for redundancy

---

### Decentralized Storage

#### Arweave (Primary)

**SDK:** `arweave-js`

**Example:**
```typescript
// services/arweave.ts
import Arweave from 'arweave';

const arweave = Arweave.init({
  host: 'arweave.net',
  port: 443,
  protocol: 'https'
});

export async function uploadToArweave(
  data: any,
  tags: { name: string; value: string }[]
) {
  // Create transaction
  const tx = await arweave.createTransaction({
    data: JSON.stringify(data)
  });

  // Add tags for categorization
  tags.forEach(tag => {
    tx.addTag(tag.name, tag.value);
  });

  // Sign transaction
  await arweave.transactions.sign(tx, wallet);

  // Upload
  await arweave.transactions.post(tx);

  // Return transaction ID (permanent reference)
  return tx.id;
}

export async function fetchFromArweave(txId: string) {
  const tx = await arweave.transactions.get(txId);
  const data = tx.get('data', { decode: true, string: true });
  return JSON.parse(data);
}

// Calculate storage cost
export async function calculateStorageCost(sizeBytes: number) {
  const price = await arweave.transactions.getPrice(sizeBytes);
  const arPrice = arweave.ar.winstonToAr(price);
  return parseFloat(arPrice);
}
```

**Cost Management:**
```typescript
// Before uploading, check cost
const sizeBytes = JSON.stringify(message).length;
const costAR = await calculateStorageCost(sizeBytes);
const costUSD = costAR * arweaveUsdPrice; // Get from API

if (costUSD > userTierLimit) {
  throw new Error(`Message too large. Cost: $${costUSD}, Limit: $${userTierLimit}`);
}

// Proceed with upload
const txId = await uploadToArweave(message, [
  { name: 'App', value: 'Tresor' },
  { name: 'Type', value: 'TimeCapsule' },
  { name: 'Version', value: '1.0' }
]);
```

#### IPFS (Secondary, for redundancy)

**SDK:** `ipfs-http-client` or `web3.storage`

**Why IPFS:**
- ✅ Content-addressed (hash of content = address)
- ✅ Fast retrieval
- ✅ Free if you run node, cheap if using service

**Why Web3.Storage (recommended IPFS service):**
- ✅ Free storage (backed by Filecoin)
- ✅ Simple API
- ✅ Automatic Filecoin deals

**Example:**
```typescript
import { Web3Storage } from 'web3.storage';

const client = new Web3Storage({ token: process.env.WEB3_STORAGE_TOKEN });

export async function uploadToIPFS(data: any) {
  const blob = new Blob([JSON.stringify(data)], { type: 'application/json' });
  const file = new File([blob], 'message.json');

  const cid = await client.put([file]);
  return cid; // Content identifier
}

export async function fetchFromIPFS(cid: string) {
  const res = await client.get(cid);
  const files = await res.files();
  const content = await files[0].text();
  return JSON.parse(content);
}
```

---

### Authentication

**Option A: NextAuth.js** (Recommended)

**Why:**
- ✅ Easy OAuth (Google, Apple, Microsoft)
- ✅ Magic link email auth
- ✅ Built for Next.js
- ✅ Session management

**Example:**
```typescript
// app/api/auth/[...nextauth]/route.ts
import NextAuth from 'next-auth';
import GoogleProvider from 'next-auth/providers/google';
import EmailProvider from 'next-auth/providers/email';

export const authOptions = {
  providers: [
    GoogleProvider({
      clientId: process.env.GOOGLE_CLIENT_ID,
      clientSecret: process.env.GOOGLE_CLIENT_SECRET
    }),
    EmailProvider({
      server: process.env.EMAIL_SERVER,
      from: 'auth@tresor.io'
    })
  ],
  callbacks: {
    async session({ session, user }) {
      // Add user ID to session
      session.userId = user.id;
      return session;
    }
  }
};

export const handler = NextAuth(authOptions);
export { handler as GET, handler as POST };
```

**Option B: Supabase Auth**

If using Supabase for database, can use their auth:
- ✅ Built-in
- ✅ Email, OAuth, magic links
- ✅ Row-level security

---

### Notifications

**Email:** SendGrid or Resend

**Why SendGrid:**
- ✅ Reliable
- ✅ Free tier (100 emails/day)
- ✅ Templates
- ❌ Can be expensive at scale

**Why Resend (Recommended for MVP):**
- ✅ Modern, simple API
- ✅ React email templates
- ✅ Good free tier
- ✅ Better developer experience

**Example:**
```typescript
import { Resend } from 'resend';

const resend = new Resend(process.env.RESEND_API_KEY);

export async function sendUnlockNotification(
  email: string,
  message: Message
) {
  await resend.emails.send({
    from: 'notifications@tresor.io',
    to: email,
    subject: '🎉 You have a message from your past self!',
    react: UnlockNotificationEmail({ message }), // React component
  });
}
```

**SMS:** Twilio

**Push Notifications:** Firebase Cloud Messaging (FCM)

---

### Payment Processing

**Choice:** Stripe

**Why:**
- ✅ Industry standard
- ✅ Supports subscriptions
- ✅ Great API
- ✅ Built-in security

**Example:**
```typescript
import Stripe from 'stripe';

const stripe = new Stripe(process.env.STRIPE_SECRET_KEY);

// Create subscription
export async function createSubscription(
  userId: string,
  priceId: string
) {
  const customer = await stripe.customers.create({
    metadata: { userId }
  });

  const subscription = await stripe.subscriptions.create({
    customer: customer.id,
    items: [{ price: priceId }],
  });

  return subscription;
}
```

---

## Where Does Data Live?

### Data Flow Diagram

```
USER CREATES MESSAGE:
┌─────────────┐
│ User Device │
│ (Browser)   │
└──────┬──────┘
       │
       │ 1. Encrypt message (CLIENT-SIDE)
       │
       v
┌──────────────┐
│ Encrypted    │
│ Message      │
└──────┬───────┘
       │
       ├─────────────────────────────────┐
       │                                 │
       │ 2. Upload                       │ 3. Register
       │    encrypted                    │    metadata
       v                                 v
┌──────────────┐                  ┌──────────────┐
│ ARWEAVE      │                  │ OUR DATABASE │
│              │                  │ (PostgreSQL) │
│ Stores:      │                  │              │
│ - Encrypted  │                  │ Stores:      │
│   content    │                  │ - User email │
│ - Encrypted  │                  │ - Message ID │
│   files      │                  │ - Unlock date│
│ - Time-locked│                  │ - Status     │
│   key        │                  │ - Arweave ID │
│              │                  │              │
│ (Permanent,  │                  │ (Updateable, │
│  decentralized)│                │  traditional)│
└──────────────┘                  └──────────────┘
       │                                 │
       │                                 │
       │ 4. Monitor blockchain           │
       │                                 │
       └────────────┬────────────────────┘
                    │
                    v
         ┌────────────────────┐
         │ BLOCKCHAIN MONITOR │
         │ (Our Service)      │
         │                    │
         │ When unlocked:     │
         │ 1. Fetch from      │
         │    Arweave         │
         │ 2. Decrypt         │
         │ 3. Notify user     │
         └────────────────────┘
```

### Data Storage Summary

| Data Type | Where Stored | Why | Can Change? |
|-----------|-------------|-----|-------------|
| **Encrypted message content** | Arweave | Permanent, decentralized | ❌ No |
| **Encrypted files (photos/videos)** | Arweave | Permanent, decentralized | ❌ No |
| **Time-locked encryption key** | Arweave (with message) | Part of encrypted payload | ❌ No |
| **User account (email, password)** | PostgreSQL | Fast queries, authentication | ✅ Yes |
| **Delivery addresses** | PostgreSQL | Need to update as they change | ✅ Yes |
| **Message metadata (title, status, unlock date)** | PostgreSQL | Fast queries, monitoring | ⚠️ Some |
| **Payment records** | Stripe + PostgreSQL | Billing, subscriptions | ❌ No |
| **Notification logs** | PostgreSQL | Delivery tracking | ❌ No (append-only) |

**Key Insight:** Encrypted content is SEPARATE from delivery settings. This is what allows users to update their email/phone after creating a message!

---

## Development Phases

### Phase 1: MVP (2-3 months)

**Goal:** Prove the core concept works

**Features:**
- ✅ User signup (email + password)
- ✅ Create text-only messages (up to 100 KB)
- ✅ Client-side encryption (AES-256)
- ✅ Time-lock puzzles (simpler than witness encryption for MVP)
- ✅ Upload to Arweave
- ✅ Simple blockchain monitoring (check once/day)
- ✅ Email notifications when unlocked
- ✅ Web app only (no mobile)
- ✅ Free tier only (no payments yet)

**Tech Stack:**
- Frontend: Next.js + Tailwind + shadcn/ui
- Backend: Node.js + Express (keep it simple)
- Database: PostgreSQL (Supabase free tier)
- Storage: Arweave
- Auth: NextAuth.js (email/password)
- Blockchain: Bitcoin API (BlockCypher)
- Notifications: Resend (email only)

**Timeline:**
- Week 1-2: Setup, basic UI (message composer, dashboard)
- Week 3-4: Encryption implementation, Arweave integration
- Week 5-6: Backend API, database schema
- Week 7-8: Blockchain monitoring, notifications
- Week 9-10: Testing, bug fixes
- Week 11-12: Beta launch, user feedback

### Phase 2: Enhanced Features (3-4 months)

**Features:**
- ✅ Photo/video support (up to 10 MB)
- ✅ OAuth (Google, Apple sign-in)
- ✅ Payment integration (Stripe subscriptions)
- ✅ Multiple delivery methods (email + SMS)
- ✅ Self-custody download option
- ✅ Mobile PWA
- ✅ Witness encryption (replace time-lock puzzles)
- ✅ Emergency contacts

**Tech Stack Additions:**
- Payments: Stripe
- SMS: Twilio
- Mobile: Convert to PWA

### Phase 3: Scale & Decentralization (6+ months)

**Features:**
- ✅ IPFS/Filecoin redundancy
- ✅ Smart contract deployment (for metadata)
- ✅ Community-run monitoring nodes
- ✅ Open-source everything
- ✅ React Native mobile app
- ✅ Advanced features (video editing, transcription)
- ✅ API for third-party integrations
- ✅ Enterprise features

---

## Tech Stack Decisions: Summary

### Chosen Stack (MVP)

| Component | Technology | Why |
|-----------|-----------|-----|
| **Frontend** | Next.js 14 + TypeScript | Modern, fast, SEO-friendly |
| **UI** | Tailwind CSS + shadcn/ui | Beautiful, accessible, customizable |
| **Backend** | Node.js + Express | Simple, proven, everyone knows it |
| **Database** | PostgreSQL (Supabase) | Reliable, free tier, managed |
| **ORM** | Prisma | Type-safe, great DX |
| **Auth** | NextAuth.js | Easy OAuth + magic links |
| **Storage** | Arweave | Permanent, decentralized |
| **Blockchain** | Bitcoin (BlockCypher API) | Reference clock, proven |
| **Notifications** | Resend (email) | Modern, simple, affordable |
| **Payments** | Stripe | Industry standard |
| **Encryption** | Web Crypto API | Built-in, secure, fast |
| **Hosting** | Vercel (frontend) + Railway/Fly.io (backend) | Easy deploy, free tier, scales |

### Infrastructure Costs (MVP, estimated)

| Service | Cost/month | Notes |
|---------|-----------|-------|
| Vercel (frontend) | $0 | Free tier sufficient for MVP |
| Railway (backend) | $5-20 | Based on usage |
| Supabase (database) | $0 | Free tier: 500 MB DB, 2 GB storage |
| Arweave storage | Pay-per-use | ~$0.05 per message (user pays) |
| BlockCypher API | $0 | Free tier: 200 req/hr |
| Resend (email) | $0 | Free tier: 3000 emails/month |
| Domain | $12/year | tresor.io or similar |
| **Total** | **~$20-50/month** | Very affordable for MVP! |

**As we scale:**
- Self-host Bitcoin node: +$100/month
- Twilio SMS: +$0.0075 per SMS
- More backend capacity: +$50-200/month
- CDN for media: +$20-100/month

---

## Next Steps: Setting Up Development Environment

### 1. Repository Structure

```
tresor/
├── apps/
│   ├── web/              # Next.js frontend
│   ├── api/              # Backend API
│   └── monitor/          # Blockchain monitoring service
├── packages/
│   ├── crypto/           # Encryption library
│   ├── database/         # Prisma schema + migrations
│   ├── ui/               # Shared UI components
│   └── types/            # Shared TypeScript types
├── docs/                 # Documentation (what we're writing now!)
└── scripts/              # Deployment, utilities
```

**Using Turborepo (monorepo)** - keeps everything in one place, shares code easily.

### 2. Initial Setup Commands

```bash
# Create new Next.js app
npx create-next-app@latest tresor-web --typescript --tailwind --app

# Setup Prisma
npm install prisma @prisma/client
npx prisma init

# Install key dependencies
npm install arweave bitcoin-core stripe resend next-auth

# Install crypto utilities
npm install tweetnacl tweetnacl-util

# Development tools
npm install -D @types/node tsx nodemon
```

### 3. Environment Variables

```env
# .env.local

# Database
DATABASE_URL="postgresql://..."

# Arweave
ARWEAVE_WALLET_KEY="..."

# Bitcoin
BITCOIN_RPC_URL="https://api.blockcypher.com/v1/btc/main"
BITCOIN_RPC_KEY="..."

# Auth
NEXTAUTH_SECRET="..."
NEXTAUTH_URL="http://localhost:3000"

# Email
RESEND_API_KEY="..."

# Stripe
STRIPE_SECRET_KEY="..."
STRIPE_WEBHOOK_SECRET="..."

# App
NEXT_PUBLIC_APP_URL="http://localhost:3000"
```

---

## Questions for You

Before we start building:

1. **Start simple or full-featured?**
   - Option A: MVP with text-only, time-lock puzzles (2 months)
   - Option B: Full featured from start with videos, witness encryption (4+ months)

2. **Self-host or managed services?**
   - Option A: Use Supabase, Vercel, Railway (faster, easier)
   - Option B: Self-host everything (more control, more complex)

3. **Open source from day 1 or later?**
   - Option A: Open source immediately (builds trust, community)
   - Option B: Keep private initially (protect business model)

4. **Tech stack preferences?**
   - Happy with recommendations above?
   - Any strong opinions on specific technologies?

**My recommendation:**
- Start with MVP (text + photos, time-lock puzzles)
- Use managed services (Supabase, Vercel, etc.)
- Open source from day 1 (aligns with our "decentralized" philosophy)
- Next.js + Node.js + PostgreSQL stack (proven, reliable)

Should I create:
1. A setup guide to start coding?
2. Detailed API specification?
3. Database schema with Prisma?
4. Sample code for encryption flow?

What would be most helpful next?
