# Tresor - Messages to Your Future Self

> Time-locked message delivery using cryptographic guarantees and blockchain technology

## What is Tresor?

Tresor is an open-source application that allows you to send messages to yourself (or others) in the future, with **cryptographic guarantees** that they cannot be read until a specified date.

Unlike traditional "scheduled email" services, Tresor uses:
- **Bitcoin blockchain** as a time reference (witness encryption)
- **Decentralized storage** (Arweave) for permanence
- **Cryptographic time-locking** (messages are mathematically impossible to decrypt early)

## Key Features

- 🔐 **Cryptographically Time-Locked** - Messages cannot be decrypted before the unlock date (provably secure)
- ⛓️ **Blockchain-Based** - Uses Bitcoin block height as a tamper-proof clock
- 🌐 **Decentralized Storage** - Messages stored permanently on Arweave
- 📱 **Multimedia Support** - Text, images, audio (videos in Phase 2)
- 🔄 **Updateable Delivery** - Change your email/phone even after message is encrypted
- 👥 **Emergency Contacts** - Trusted contacts can help ensure delivery
- 💾 **Self-Custody Option** - Download encrypted messages for complete ownership
- 🔓 **Magic Link Auth** - No passwords to forget after 5+ years

## Project Status

**Current Phase:** MVP Implementation ✅
**Next Phase:** Production deployment (blockchain integrations, Arweave)

### Completed
- ✅ Core encryption (AES-256-GCM)
- ✅ Shamir Secret Sharing (3-of-5 threshold)
- ✅ Multi-chain time-lock service
- ✅ Mock blockchain adapters
- ✅ Self-custody adapter (file-based)
- ✅ Database models (JPA/Hibernate)
- ✅ REST API endpoints
- ✅ Security configuration
- ✅ End-to-end integration tests
- ✅ Application startup verified

## Deployment Tiers: Choose Your Security Level

Tresor offers **flexible deployment options** to match your needs and budget:

### 💰 Budget Tier - $0.57 (Recommended for 90% of users)
- **Blockchains**: Polygon + Base + Optimism (ultra-cheap L2s)
- **Deployment**: Within 24 hours (batched)
- **Perfect for**: Personal time capsules, birthday messages, reminders
- **Savings**: 96% cheaper than Premium tier

### ⚡ Standard Tier - $3.00
- **Blockchains**: Arbitrum + Polygon + Avalanche (fast L2s)
- **Deployment**: Instant
- **Perfect for**: Time-sensitive messages, important family communications
- **Savings**: 77% cheaper than Premium tier

### 🏆 Premium Tier - $13.00 **SECURED BY BITCOIN + ETHEREUM**
- **Blockchains**: Bitcoin + Ethereum + Arbitrum
- **Deployment**: Instant
- **Security**: Maximum (20+ years proven, highest decentralization)
- **Perfect for**: Legal wills, business documents, generational messages
- **Value**: "Secured by Bitcoin blockchain" - the gold standard

### 🏢 Enterprise Tier - $25.00
- **Blockchains**: 7 chains with 5-of-7 threshold
- **Redundancy**: Survives 2 blockchain failures
- **SLA**: Guaranteed uptime and delivery
- **Perfect for**: Business escrow, compliance, mission-critical data

**Why the flexibility?**
- Most users don't need Bitcoin's premium security (Budget tier is excellent)
- Power users get Bitcoin/Ethereum for maximum trust and longevity
- Business users get enterprise-grade redundancy
- **You choose what matters to you: cost or maximum security**

See [ARCHITECTURE.md](./ARCHITECTURE.md) for technical details and [COST_OPTIMIZATION.md](./COST_OPTIMIZATION.md) for full cost analysis.

## Quick Links

- [Architecture & Blockchain Flexibility](./ARCHITECTURE.md) ⭐ **NEW**
- [Cost Optimization Strategy](./COST_OPTIMIZATION.md) ⭐ **NEW**
- [Testing & Simplification](./TESTING_AND_SIMPLIFICATION.md) ⭐ **NEW**
- [Product Requirements Document (PRD)](./PRD.md)
- [Implementation Details](./IMPLEMENTATION_DETAILS.md)
- [Multimedia Analysis](./MULTIMEDIA_ANALYSIS.md)
- [Marketing & Business Model](./MARKETING_AND_BUSINESS_MODEL.md)
- [Technology Stack](./TECHNOLOGY_STACK.md)
- [Java Backend Stack](./JAVA_BACKEND_STACK.md)
- [Authentication & Security](./AUTHENTICATION_SECURITY.md)
- [Modular Architecture](./MODULAR_ARCHITECTURE.md)

---

## Architecture Summary

### Modular Design

Tresor is built as a **modular system** to maximize flexibility and reusability:

```
tresor/
├── tresor-core/              # Pure Java crypto library (the heart ❤️)
├── tresor-storage-arweave/   # Arweave storage implementation
├── tresor-auth/              # Authentication (OPTIONAL for self-hosters)
├── tresor-api/               # REST API (Spring Boot application)
└── tresor-cli/               # Command-line tool
```

**Why Modular?**
- ✅ Self-hosters can replace auth with Keycloak, LDAP, SSO
- ✅ Core library can be used in any Java project
- ✅ Better testing (test modules independently)
- ✅ Enterprise-friendly (pluggable components)

### Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Backend** | Java + Spring Boot | Java 21, Spring Boot 4.0.0-RC1 |
| **Frontend** | Next.js + TypeScript | Next.js 14 |
| **Database** | PostgreSQL | 16+ |
| **Cryptography** | BouncyCastle | 1.77 |
| **Blockchain** | BitcoinJ | 0.16.2 |
| **Storage** | Arweave | Latest |
| **Testing** | JUnit + Testcontainers | Latest |

---

## Key Design Decisions

### 1. Authentication: Magic Link (Not Passwords)

**Decision:** Magic link authentication by default
**Rationale:** Users may not access the app for 5+ years - they'll forget passwords!

**Flow:**
1. User enters email
2. Receive magic link
3. Click link → automatically logged in
4. JWT session (30 days)

**Optional:** Password auth available for users who prefer it

### 2. Open Source Business Model

**Decision:** 100% open source with SaaS revenue model
**License:** AGPL (backend), MIT (frontend/libraries)

**Revenue:**
- Free tier: 3 messages/year
- Personal: $5/year
- Family: $20/year
- Legacy: $50/year

**Why it works:**
- Only ~5% of users will self-host
- 95% will pay for convenience
- Self-hosters become advocates and contributors
- Examples: GitLab ($500M/year), Supabase ($100M valuation), Bitwarden (millions/year)

### 3. MVP Scope

**Phase 1 (3 months):**
- ✅ Text messages
- ✅ Images (JPEG, PNG)
- ✅ Audio (MP3, WAV)
- ✅ Magic link auth
- ✅ Email notifications
- ✅ Up to 10 MB per message

**Phase 2:**
- Videos (up to 100 MB)
- SMS notifications
- OAuth (Google, Apple)
- Witness encryption (upgrade from time-lock puzzles)

### 4. Security for Unlocked Messages

**Problem:** When a message unlocks, where do we store the decrypted content?

**Solution:** User-encrypted inbox
- Derive user's encryption key from email + secret pepper
- Re-encrypt unlocked message with user's personal key
- Store encrypted in database
- Even database breach doesn't expose messages

### 5. Modular Architecture

**Decision:** Separate core crypto from auth and API
**Rationale:** Enables self-hosters to use own auth, developers to use just the crypto library

**Modules:**
1. **tresor-core** - Pure Java library (NO Spring, NO auth)
2. **tresor-storage-arweave** - Storage implementation
3. **tresor-auth** - OPTIONAL authentication
4. **tresor-api** - Main Spring Boot application
5. **tresor-cli** - Command-line tool

---

## Use Cases

### Primary: Parent to Child

> "I'm recording a video message for my daughter every year on her birthday. On her 18th birthday, she'll receive 18 videos - a complete time capsule of her childhood from my perspective."

**Cost:** ~$45 one-time (18 videos × 100 MB)
**Value:** Priceless

### Secondary: Personal Growth

> "I'm setting 5-year goals and sending myself a message. When I receive it in 2030, I'll see if I achieved them - and I can't cheat by checking early!"

### Niche: Estate Planning

> "I'm leaving messages for my family to receive after I'm gone, on specific milestones - my daughter's wedding, grandson's graduation."

---

## Project Principles

### 1. Security First
- Client-side encryption (we never see plaintext)
- Cryptographic time-locking (mathematically impossible to decrypt early)
- Zero-knowledge architecture
- Open source for auditability

### 2. Long-Term Thinking
- Designed for 10-50 year time horizons
- Decentralized storage (survives company failure)
- Open source (community can maintain)
- Stable technology (Java 21 LTS)

### 3. User Ownership
- Self-custody option (download encrypted messages)
- Updateable delivery addresses
- Multi-channel notifications
- Transparent pricing

### 4. Open Source Done Right
- 100% code is open source
- Clear business model (SaaS convenience)
- Modular design (use what you need)
- Pluggable components (bring your own auth)

---

## Development Roadmap

### Phase 1: MVP (Months 1-3) - Current Phase

**Goal:** Prove core concept works

**Deliverables:**
- [ ] Maven multi-module project structure
- [ ] tresor-core: Encryption interfaces and implementation
- [ ] tresor-core: Time-lock encryption (puzzles for MVP)
- [ ] tresor-core: Bitcoin blockchain monitoring
- [ ] tresor-storage-arweave: Arweave integration
- [ ] tresor-auth: Magic link authentication
- [ ] tresor-api: REST API with basic endpoints
- [ ] Frontend: Next.js app with message composer
- [ ] Database: PostgreSQL schema with Flyway migrations
- [ ] Testing: Integration tests with Testcontainers
- [ ] Documentation: Setup guides, API docs

**Timeline:**
- Weeks 1-4: Core crypto implementation
- Weeks 5-8: Storage + blockchain monitoring
- Weeks 9-12: API + frontend + testing

### Phase 2: Enhanced Features (Months 4-6)

- [ ] Video support (up to 100 MB)
- [ ] Witness encryption (upgrade from puzzles)
- [ ] SMS notifications
- [ ] OAuth (Google, Apple)
- [ ] Payment integration (Stripe)
- [ ] Mobile PWA
- [ ] Self-custody download

### Phase 3: Scale & Decentralization (Months 7-12)

- [ ] IPFS/Filecoin redundancy
- [ ] Smart contract deployment
- [ ] Community-run monitoring nodes
- [ ] React Native mobile app
- [ ] Enterprise features (SAML, API)
- [ ] Open source community building

---

## Market Validation

### Proven Demand

**Spotify's "Playlist in a Bottle":**
- Millions of users loved it
- Discontinued in 2025 → users are upset
- Proves massive demand for time-capsule features

**Existing Competitors:**
- FutureMe.org (20+ years, millions of users)
- Cupaloy (parent-to-child messages)
- GoodTrust (end-of-life messages)

**Our Advantage:**
- Only service with cryptographic time-locking
- Only service with decentralized storage
- Only open source option
- Only one with multimedia + long-term guarantees

---

## Contributing

**Status:** Not yet accepting contributions (in initial development)

**Future:**
Once MVP is complete, we'll welcome:
- Code contributions (features, bug fixes)
- Documentation improvements
- Security audits
- Storage backend implementations
- Auth provider integrations

---

## License

This project is licensed under the **Apache License 2.0** - see the [LICENSE](LICENSE) file for details.

### Why Apache 2.0?

✅ **Open source** - Fully auditable, transparent code
✅ **Permissive** - Commercial use allowed
✅ **Attribution required** - Users must mention the technology
✅ **Patent grant** - Protection against patent trolls
✅ **Industry standard** - Used by Apache, Google, Microsoft

### Attribution Requirements

When using Tresor, you must acknowledge the cryptographic technologies:
- AES-256-GCM encryption (NIST FIPS 197)
- Shamir's Secret Sharing (Adi Shamir, 1979)
- Multi-chain time-lock encryption
- Bitcoin CLTV time-locks
- Ethereum/EVM smart contracts
- Arweave permanent storage

---

## Team

**Current:** Solo developer (with AI assistance)

**Future:** Open to co-founders and early contributors

---

## Documentation Index

### Product Documentation
- [PRD.md](./PRD.md) - Complete product requirements
- [IMPLEMENTATION_DETAILS.md](./IMPLEMENTATION_DETAILS.md) - UI mockups, architecture, costs
- [MULTIMEDIA_ANALYSIS.md](./MULTIMEDIA_ANALYSIS.md) - Why photos/videos/audio are essential
- [MARKETING_AND_BUSINESS_MODEL.md](./MARKETING_AND_BUSINESS_MODEL.md) - Go-to-market, pricing, revenue

### Technical Documentation
- [TECHNOLOGY_STACK.md](./TECHNOLOGY_STACK.md) - Full tech stack (original Node.js version)
- [JAVA_BACKEND_STACK.md](./JAVA_BACKEND_STACK.md) - Java + Spring Boot decision
- [AUTHENTICATION_SECURITY.md](./AUTHENTICATION_SECURITY.md) - Auth strategy, security for unlocked messages
- [MODULAR_ARCHITECTURE.md](./MODULAR_ARCHITECTURE.md) - Why and how to separate modules

### Development
- [PROJECT_DECISIONS.md](./PROJECT_DECISIONS.md) - All major decisions in one place (this file created next)

---

## Contact

**Repository:** [github.com/your-username/tresor](https://github.com/your-username/tresor) (to be created)
**Website:** tresor.io (to be created)
**Email:** contact@tresor.io (to be created)

---

## Acknowledgments

**Inspiration:**
- Spotify's Playlist in a Bottle
- FutureMe.org (pioneering future messages since 2002)
- Academic research on witness encryption and time-lock puzzles

**Technology:**
- Bitcoin community (for the blockchain)
- Arweave team (for permanent storage)
- BouncyCastle (for cryptography)
- Spring Boot team (for the framework)

---

**Last Updated:** 2025-01-07
**Version:** 0.1.0-SNAPSHOT (Pre-release)
