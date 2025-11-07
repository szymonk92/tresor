# Tresor - Java Backend Technology Stack

## Executive Summary

**Key Decisions:**
- ✅ **Backend: Java with Spring Boot** (excellent choice!)
- ✅ **MVP: Text + Images + Audio** (videos in Phase 2)
- ✅ **Open Source: YES** (with profitable business model)
- ✅ **Database: PostgreSQL** (confirmed)

---

## Table of Contents
1. [Why Java is Perfect for Tresor](#why-java-is-perfect-for-tresor)
2. [Open Source Monetization Model](#open-source-monetization-model)
3. [Updated Technology Stack](#updated-technology-stack)
4. [Java Backend Architecture](#java-backend-architecture)
5. [MVP Scope with Images & Audio](#mvp-scope-with-images--audio)

---

## Why Java is Perfect for Tresor

### Reasons Java is Excellent for This Project

1. **Enterprise Credibility** 🏢
   - Banks and financial institutions trust Java
   - Important for handling user payments and sensitive data
   - Better perception for enterprise clients

2. **Strong Type System** 🛡️
   - Compile-time error detection
   - Refactoring is safer
   - Less bugs in production
   - Clear contracts between services

3. **Excellent Concurrency** ⚡
   - Blockchain monitoring requires background jobs
   - Handling multiple message decryption in parallel
   - Java's concurrency primitives are mature and tested

4. **Mature Cryptography Libraries** 🔐
   - BouncyCastle: Industry-standard crypto library
   - Excellent for implementing witness encryption
   - Battle-tested security implementations

5. **Long-term Maintenance** 🕰️
   - Java has been around 25+ years, will be around 25+ more
   - For an app about "messages to the future," using stable tech makes sense
   - Easier to find developers (larger talent pool)

6. **Spring Boot Ecosystem** 🌱
   - Spring Security (authentication)
   - Spring Data JPA (database)
   - Spring Scheduler (blockchain monitoring)
   - Spring WebSocket (real-time notifications)
   - Massive community and documentation

7. **Better for Blockchain Integration** ⛓️
   - Web3j: Excellent Java library for blockchain
   - BitcoinJ: Native Bitcoin library
   - Better performance for cryptographic operations

### Java vs Node.js Comparison

| Aspect | Java + Spring Boot | Node.js + Express |
|--------|-------------------|-------------------|
| **Type Safety** | ✅ Excellent (compile-time) | ⚠️ Good (TypeScript, runtime) |
| **Performance** | ✅ High (JVM optimization) | ⚠️ Good (V8, single-threaded) |
| **Concurrency** | ✅ Excellent (threads, virtual threads) | ⚠️ Good (async, but single-threaded) |
| **Crypto Libraries** | ✅ BouncyCastle (mature) | ⚠️ crypto (good, less mature) |
| **Enterprise Adoption** | ✅ Very high | ⚠️ Medium |
| **Developer Pool** | ✅ Very large | ✅ Very large |
| **Learning Curve** | ⚠️ Steeper | ✅ Easier |
| **Startup Time** | ⚠️ Slower (~5 seconds) | ✅ Fast (~1 second) |
| **Memory Usage** | ⚠️ Higher (JVM) | ✅ Lower |
| **Blockchain SDKs** | ✅ Excellent (Web3j, BitcoinJ) | ⚠️ Good (web3.js) |

**Verdict:** Java is the RIGHT choice for Tresor's long-term stability and enterprise credibility.

---

## Open Source Monetization Model

### Your Question: "Would there be a way to make money on open source?"

**Answer: YES! Open source does NOT mean unprofitable.**

### Successful Open Source Companies (Making Millions)

| Company | Model | Revenue |
|---------|-------|---------|
| **GitLab** | Open core + Hosted SaaS | $500M+/year |
| **Supabase** | Fully open source + Hosted SaaS | $100M valuation |
| **Bitwarden** | Open source + Hosted premium | Profitable, growing |
| **Sentry** | Open source + Hosted SaaS | $100M+/year |
| **Plausible** | Open source + Hosted SaaS | $1M+/year (2 people!) |
| **Cal.com** | Open source + Hosted SaaS | $25M funding |
| **Discourse** | Open source + Hosted + Support | Profitable |

**The pattern:** Code is open source, but most people pay for the **hosted, managed service**.

---

### Tresor's Open Source Business Model

#### The "Open Source SaaS" Model (Recommended)

**What's Open Source:**
- ✅ **100% of the code** (frontend, backend, monitoring service)
- ✅ **Cryptographic implementation** (users can verify security)
- ✅ **Database schema** (transparency)
- ✅ **Deployment scripts** (anyone can run their own instance)

**What We Charge For:**
- ✅ **Hosted service at tresor.io** (convenience, reliability)
- ✅ **Managed infrastructure** (we handle servers, monitoring, updates)
- ✅ **Email/SMS notifications** (costs money to send)
- ✅ **Storage costs** (Arweave uploads)
- ✅ **Support** (help users, answer questions)
- ✅ **Uptime SLA** (guarantee 99.9% availability)
- ✅ **Automatic updates** (new features, security patches)

**Why People Will Pay (Instead of Self-Hosting):**

1. **Convenience** 🎯
   - Self-hosting requires technical knowledge
   - Most people don't want to manage servers
   - **Example:** WordPress is free, but millions pay for wordpress.com

2. **Reliability** 🛡️
   - We guarantee uptime
   - Professional monitoring
   - Automatic backups
   - **Example:** You can run your own email server, but you use Gmail

3. **Time = Money** ⏰
   - Setting up takes hours/days
   - Maintenance takes ongoing time
   - $5/month is cheaper than their time
   - **Example:** Linux is free, but people pay for managed Linux hosting

4. **Peace of Mind** 😌
   - We handle security updates
   - We monitor for issues
   - We ensure messages actually deliver
   - **Example:** You could host your own password manager, but Bitwarden charges $10/year

5. **Support** 🤝
   - Email support
   - Help setting up
   - Troubleshooting
   - **Example:** People pay for GitHub even though Git is free

---

### Revenue Model (Open Source + SaaS)

#### Pricing Strategy

| Tier | Price | Features | Target |
|------|-------|----------|--------|
| **Self-Hosted** | $0 | Full features, you run it | Tech enthusiasts (5% of users) |
| **Free (Hosted)** | $0 | 3 messages/year, 1 MB | Trial users (40% of users) |
| **Personal** | $5/year | 50 messages/year, 10 MB | Individuals (35% of users) |
| **Family** | $20/year | Unlimited, 100 MB | Parents (15% of users) |
| **Legacy** | $50/year | Unlimited, 1 GB | Power users (4% of users) |
| **Enterprise** | Custom | API, SLA, support | Companies (1% of users) |

#### Why Self-Hosted Users Don't Hurt Business

**Reality Check:**
- Only ~5% of users will actually self-host
- These users would likely not pay anyway (price-sensitive tech users)
- They become **advocates** (spread word about Tresor)
- They contribute **code** (improve the product)
- They verify **security** (build trust for paying users)

**Example: Bitwarden**
- Code is 100% open source
- Self-hosting is free
- But 95% of users pay for hosted version
- Revenue: Millions per year

**Net effect:** Open source INCREASES total users and revenue, doesn't decrease it.

---

### Additional Revenue Streams (Open Source Compatible)

1. **Enterprise Support** 💼
   - Paid SLA (99.99% uptime guarantee)
   - Dedicated support team
   - Custom features
   - On-premise deployment assistance
   - **Revenue:** $10k-100k per enterprise client

2. **White Label** 🏷️
   - Companies can rebrand Tresor for their use
   - Law firms: "ClientTimeCapsule"
   - Insurance: "FamilyLegacy"
   - We provide infrastructure, they provide branding
   - **Revenue:** $1k-10k per white label client

3. **API Access** 🔌
   - Third-party apps integrate Tresor
   - Estate planning software
   - Therapy apps (journal to future self)
   - Charge per API call
   - **Revenue:** $100-1k per integration

4. **Partnerships** 🤝
   - Estate planning attorneys refer clients
   - Life insurance companies bundle service
   - We pay referral fees / revenue share
   - **Revenue:** 10-20% of customer value

5. **Consulting** 🧑‍💼
   - Help enterprises deploy on-premise
   - Security audits for companies
   - Custom feature development
   - **Revenue:** $150-300/hour

---

### Open Source Licenses (How to Protect Business)

**Recommended: AGPL (Affero GPL)**

**What it means:**
- ✅ Anyone can use, modify, distribute the code
- ✅ BUT: If you run it as a service, you MUST share your modifications
- ✅ Prevents competitors from taking code and NOT sharing improvements
- ✅ Used by: MongoDB, Grafana, Supabase

**Example:** If Company X takes Tresor code, modifies it, and runs a competing service, they MUST open-source their changes.

**Why this is good:**
- Prevents "parasitic" competitors
- Forces competitors to contribute back
- Still allows commercial use

**Alternative: MIT License** (most permissive, but less protection)

**My recommendation:** **AGPL** for backend, **MIT** for frontend/libraries

---

## Updated Technology Stack

### Complete Stack with Java Backend

```
┌────────────────────────────────────────────────────┐
│                   FRONTEND                          │
│  Next.js 14 + TypeScript + Tailwind CSS           │
│  - Client-side encryption                          │
│  - Message composition                             │
│  - User dashboard                                  │
└────────────────┬───────────────────────────────────┘
                 │
                 │ REST API / WebSocket
                 │
┌────────────────▼───────────────────────────────────┐
│              JAVA BACKEND                          │
│  Spring Boot 3.2+ (Java 21 LTS)                   │
│                                                     │
│  ┌─────────────────────────────────────────────┐  │
│  │  Spring MVC (REST Controllers)              │  │
│  │  - User management                           │  │
│  │  - Message metadata CRUD                     │  │
│  │  - Delivery settings                         │  │
│  └─────────────────────────────────────────────┘  │
│                                                     │
│  ┌─────────────────────────────────────────────┐  │
│  │  Spring Security                             │  │
│  │  - JWT authentication                        │  │
│  │  - OAuth2 (Google, Apple)                    │  │
│  │  - Role-based access                         │  │
│  └─────────────────────────────────────────────┘  │
│                                                     │
│  ┌─────────────────────────────────────────────┐  │
│  │  Spring Scheduler                            │  │
│  │  - Blockchain monitoring                     │  │
│  │  - Message unlocking                         │  │
│  │  - Notification sending                      │  │
│  └─────────────────────────────────────────────┘  │
│                                                     │
│  ┌─────────────────────────────────────────────┐  │
│  │  Services                                    │  │
│  │  - ArweaveService (upload/download)         │  │
│  │  - BitcoinService (blockchain monitoring)   │  │
│  │  - EncryptionService (witness encryption)   │  │
│  │  - NotificationService (email/SMS)          │  │
│  │  - PaymentService (Stripe)                  │  │
│  └─────────────────────────────────────────────┘  │
└────────────────┬───────────────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
        v                 v
┌───────────────┐  ┌──────────────┐
│  PostgreSQL   │  │  Arweave     │
│  (Metadata)   │  │  (Encrypted) │
└───────────────┘  └──────────────┘
```

---

## Java Backend Architecture

### Tech Stack Details

#### Core Framework

**Spring Boot 3.2+**
- Latest stable version
- Spring Boot 3 = Spring 6 (modern)
- Built-in observability
- Native compilation support (GraalVM)

**Java 21 LTS**
- Latest long-term support version
- Virtual threads (excellent for I/O operations)
- Pattern matching (cleaner code)
- Record classes (less boilerplate)

#### Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- Spring Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- PostgreSQL Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>

    <!-- BouncyCastle (Cryptography) -->
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcprov-jdk18on</artifactId>
        <version>1.77</version>
    </dependency>

    <!-- BitcoinJ (Bitcoin blockchain) -->
    <dependency>
        <groupId>org.bitcoinj</groupId>
        <artifactId>bitcoinj-core</artifactId>
        <version>0.16.2</version>
    </dependency>

    <!-- Arweave Java SDK -->
    <dependency>
        <groupId>com.github.ArweaveTeam</groupId>
        <artifactId>arweave-java-client</artifactId>
        <version>1.1.0</version>
    </dependency>

    <!-- Stripe Java SDK -->
    <dependency>
        <groupId>com.stripe</groupId>
        <artifactId>stripe-java</artifactId>
        <version>24.0.0</version>
    </dependency>

    <!-- SendGrid for Email -->
    <dependency>
        <groupId>com.sendgrid</groupId>
        <artifactId>sendgrid-java</artifactId>
        <version>4.9.3</version>
    </dependency>

    <!-- Lombok (reduce boilerplate) -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>provided</scope>
    </dependency>

    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

### Project Structure

```
tresor-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── io/
│   │   │       └── tresor/
│   │   │           ├── TresorApplication.java
│   │   │           │
│   │   │           ├── config/
│   │   │           │   ├── SecurityConfig.java
│   │   │           │   ├── WebConfig.java
│   │   │           │   └── SchedulerConfig.java
│   │   │           │
│   │   │           ├── controller/
│   │   │           │   ├── AuthController.java
│   │   │           │   ├── MessageController.java
│   │   │           │   ├── UserController.java
│   │   │           │   └── DeliveryController.java
│   │   │           │
│   │   │           ├── service/
│   │   │           │   ├── UserService.java
│   │   │           │   ├── MessageService.java
│   │   │           │   ├── ArweaveService.java
│   │   │           │   ├── BitcoinService.java
│   │   │           │   ├── EncryptionService.java
│   │   │           │   ├── NotificationService.java
│   │   │           │   └── PaymentService.java
│   │   │           │
│   │   │           ├── repository/
│   │   │           │   ├── UserRepository.java
│   │   │           │   ├── MessageRepository.java
│   │   │           │   └── DeliverySettingRepository.java
│   │   │           │
│   │   │           ├── entity/
│   │   │           │   ├── User.java
│   │   │           │   ├── Message.java
│   │   │           │   └── DeliverySetting.java
│   │   │           │
│   │   │           ├── dto/
│   │   │           │   ├── CreateMessageRequest.java
│   │   │           │   └── MessageResponse.java
│   │   │           │
│   │   │           ├── scheduler/
│   │   │           │   └── BlockchainMonitor.java
│   │   │           │
│   │   │           └── exception/
│   │   │               └── GlobalExceptionHandler.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           ├── V1__create_users_table.sql
│   │           └── V2__create_messages_table.sql
│   │
│   └── test/
│       └── java/
│
├── pom.xml
├── Dockerfile
└── docker-compose.yml
```

---

## MVP Scope with Images & Audio

### Updated MVP Features

**Phase 1: MVP (3 months)** - What we'll build first

✅ **Content Types:**
- Text messages (up to 1 MB)
- Images (JPEG, PNG, up to 10 MB total)
- Audio (MP3, WAV, up to 10 MB total)
- **Videos in Phase 2**

✅ **Core Features:**
- User signup (email + password)
- Client-side encryption (AES-256)
- Time-lock encryption
- Upload to Arweave
- Blockchain monitoring
- Email notifications
- Update delivery settings

---

## Development Timeline

### Phase 1: MVP (3 months)

**Month 1: Foundation**
- Weeks 1-2: Project setup, database schema, auth
- Weeks 3-4: Frontend UI, client-side encryption

**Month 2: Core Features**
- Weeks 5-6: Arweave integration, backend API
- Weeks 7-8: Bitcoin integration, blockchain monitoring

**Month 3: Polish & Launch**
- Weeks 9-10: Notifications, testing
- Weeks 11-12: Bug fixes, documentation, beta launch

---

## Summary

### What We Decided

✅ **Backend: Java + Spring Boot**
✅ **MVP: Text + Images + Audio**
✅ **Open Source: YES** (with profitable SaaS model)
✅ **Database: PostgreSQL**

### Open Source Business Model

**The Plan:**
- 100% of code is open source (AGPL license)
- We run the hosted service at tresor.io
- 95% of users will pay for hosted convenience
- 5% self-host (but they advocate and contribute)

**Revenue:** Same pricing as before, sustainable and profitable!

**Infrastructure Costs:** ~$25-50/month for MVP

---

## What I Can Create Next

1. **Complete Spring Boot setup**
2. **Full Java backend code**
3. **API specification**
4. **Frontend integration guide**
5. **Deployment guide**

**What would be most helpful?**
