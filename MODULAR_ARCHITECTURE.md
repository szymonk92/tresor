# Tresor - Modular Architecture Design

## Your Insight is PERFECT! 🎯

You're absolutely right! Authentication should be **optional and pluggable** for self-hosting. The core value is the **time-lock encryption + blockchain monitoring**, not auth.

---

## Table of Contents
1. [The Problem with Monolithic Architecture](#the-problem)
2. [How Other Open Source Projects Do It](#real-world-examples)
3. [Tresor's Modular Architecture](#tresors-modular-architecture)
4. [Maven Multi-Module Project Structure](#maven-multi-module-structure)
5. [Module Dependencies](#module-dependencies)
6. [Use Cases for Each Module](#use-cases)
7. [Implementation Guide](#implementation-guide)

---

## The Problem

### Monolithic Approach (What We Want to AVOID)

```
┌─────────────────────────────────────────┐
│         Tresor Backend                  │
│  (Everything tightly coupled)           │
│                                         │
│  - Authentication (Magic Link, JWT)     │
│  - Time-lock Encryption    ← CORE VALUE│
│  - Blockchain Monitoring   ← CORE VALUE│
│  - Arweave Storage        ← CORE VALUE│
│  - User Management                      │
│  - Email Notifications                  │
│  - Payment Processing                   │
│                                         │
│  Self-hosters must use ALL of this     │
│  Can't replace auth with their own     │
│  Can't use encryption in other projects│
└─────────────────────────────────────────┘
```

**Problems:**
- ❌ Self-hosters forced to use our auth (might have their own SSO)
- ❌ Can't reuse encryption library in other projects
- ❌ Can't run "headless" (API only)
- ❌ Harder to test (everything coupled)
- ❌ Less flexible for enterprise (they want SAML, LDAP, etc.)

### Modular Approach (What We SHOULD Do)

```
┌──────────────────────────────────────────────────────────┐
│                    Tresor Ecosystem                       │
└──────────────────────────────────────────────────────────┘

┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐
│  tresor-core    │  │  tresor-auth    │  │ tresor-api   │
│  (Library)      │  │  (Optional)     │  │ (Main App)   │
│                 │  │                 │  │              │
│ - Encryption    │  │ - Magic Link    │  │ - REST API   │
│ - Time-lock     │  │ - JWT           │  │ - Controllers│
│ - Blockchain    │  │ - OAuth         │  │ - Uses both  │
│ - Arweave       │  │ - User mgmt     │  │              │
│                 │  │                 │  │              │
│ Pure Java lib   │  │ Spring Boot     │  │ Spring Boot  │
│ No dependencies │  │ Optional module │  │ Full service │
└─────────────────┘  └─────────────────┘  └──────────────┘
        │                     │                    │
        │                     │                    │
        v                     v                    v
   ┌────────────────────────────────────────────────┐
   │         Use Cases                              │
   ├────────────────────────────────────────────────┤
   │ tresor-core: Use in any Java project          │
   │ tresor-auth: Optional auth for SaaS           │
   │ tresor-api: Full Tresor service               │
   └────────────────────────────────────────────────┘
```

**Benefits:**
- ✅ Self-hosters can replace auth (use Keycloak, Auth0, etc.)
- ✅ Core library reusable in other projects
- ✅ Can run headless (just core + custom frontend)
- ✅ Easier testing (test core independently)
- ✅ Flexibility for enterprise customers

---

## Real-World Examples

### How Other Open Source Projects Handle Modularity

#### 1. **Supabase** (Excellent Example!)

**Architecture:**
```
supabase/
├── postgres/           # Core database (can use standalone)
├── auth/              # Auth service (optional, separate)
├── storage/           # Storage service (optional, separate)
├── realtime/          # Realtime service (optional, separate)
└── api/               # API gateway (orchestrates everything)
```

**Key Insight:**
- Each service is **independent**
- Can use just Postgres without auth
- Can replace auth with your own
- Self-hosters pick which services they want

**Tresor parallel:**
- `tresor-core` = core encryption (like Postgres)
- `tresor-auth` = auth service (optional)
- `tresor-api` = API gateway

---

#### 2. **GitLab** (Modular Auth)

**Auth integration:**
```java
// GitLab supports multiple auth backends
- Built-in database auth
- LDAP
- SAML
- OAuth (Google, GitHub, etc.)
- Kerberos

// Self-hosters choose which to enable
gitlab.yml:
  ldap:
    enabled: true
    host: ldap.company.com
```

**Tresor parallel:**
- `tresor-api` has auth **interface**
- `tresor-auth` is one **implementation**
- Self-hosters can implement their own

---

#### 3. **Keycloak** (Pluggable Everything)

**Architecture:**
```
keycloak-core/         # Core identity management
keycloak-services/     # REST API
keycloak-themes/       # UI themes (pluggable)
keycloak-adapters/     # Integration with apps
```

**Extension points:**
- Custom user storage (LDAP, DB, API)
- Custom authentication flows
- Custom themes
- Custom protocols

**Tresor parallel:**
- Define **interfaces** for auth
- Provide **default implementation**
- Allow **custom implementations**

---

#### 4. **Spring Boot** (Starter-based Modularity)

**Module approach:**
```
spring-boot-starter-web       # Core web
spring-boot-starter-security  # Security (OPTIONAL!)
spring-boot-starter-data-jpa  # Database (OPTIONAL!)
spring-boot-starter-mail      # Email (OPTIONAL!)
```

**Key principle:**
- Core is minimal
- Add **starters** for features you need
- Each starter is independent

**Tresor parallel:**
```
tresor-core              # Core encryption (required)
tresor-starter-auth      # Auth starter (optional)
tresor-starter-storage   # Arweave starter (optional)
tresor-starter-monitor   # Blockchain monitor (optional)
```

---

#### 5. **HashiCorp Vault** (Backend Plugins)

**Auth backends:**
```
vault/
├── core/              # Core secrets engine
├── auth-github/       # GitHub auth (plugin)
├── auth-ldap/         # LDAP auth (plugin)
├── auth-kubernetes/   # K8s auth (plugin)
└── auth-userpass/     # Username/password (plugin)
```

**Self-hosters:**
- Use Vault core
- Choose auth backend (or write their own)
- Plug in what they need

---

## Tresor's Modular Architecture

### Module Breakdown

```
tresor/
├── tresor-core/                    # Core library (pure Java)
│   ├── Encryption
│   ├── Time-lock primitives
│   ├── Blockchain monitoring
│   ├── Storage interfaces
│   └── NO Spring, NO auth, NO HTTP
│
├── tresor-storage-arweave/         # Arweave implementation
│   └── Implements storage interface
│
├── tresor-auth/                    # Auth module (Spring Boot)
│   ├── Magic link implementation
│   ├── JWT implementation
│   ├── OAuth providers
│   └── User management
│
├── tresor-api/                     # REST API (Spring Boot)
│   ├── Controllers
│   ├── Uses tresor-core
│   ├── Uses tresor-auth (optional!)
│   └── Main application
│
└── tresor-cli/                     # CLI tool
    └── Uses tresor-core directly
```

---

### Module Responsibilities

#### Module 1: `tresor-core` (The Heart ❤️)

**Purpose:** Pure Java library for time-lock encryption

**Responsibilities:**
- ✅ Time-lock encryption (witness encryption, time-lock puzzles)
- ✅ Blockchain interaction (Bitcoin block height)
- ✅ Key derivation (PBKDF2, Argon2)
- ✅ Message encryption/decryption (AES-GCM)
- ✅ Storage interface (abstract, implemented by other modules)

**Dependencies:**
- BouncyCastle (crypto)
- BitcoinJ (blockchain)
- NO Spring
- NO HTTP
- NO Database

**Usage:**
```java
// Can be used in ANY Java project!
import io.tresor.core.TimeLockEncryption;

TimeLockEncryption encryption = new TimeLockEncryption();

// Encrypt message with time-lock
EncryptedMessage encrypted = encryption.encrypt(
    "Hello future!",
    LocalDateTime.of(2030, 1, 1)
);

// Save to Arweave (or any storage)
storage.save(encrypted);

// Later, when time comes...
if (encryption.canDecrypt(encrypted, currentBlockHeight)) {
    String message = encryption.decrypt(encrypted);
}
```

**Key Point:** This is a **pure library** - no web, no database, no auth. Just encryption.

---

#### Module 2: `tresor-storage-arweave`

**Purpose:** Arweave storage implementation

**Responsibilities:**
- ✅ Upload to Arweave
- ✅ Fetch from Arweave
- ✅ Cost calculation
- ✅ Implements `StorageService` interface from core

**Dependencies:**
- tresor-core (interface)
- Arweave Java SDK

**Usage:**
```java
import io.tresor.storage.arweave.ArweaveStorage;

StorageService storage = new ArweaveStorage(config);
String txId = storage.save(encryptedMessage);
```

**Benefit:** Self-hosters could implement `StorageService` for:
- S3
- IPFS only (cheaper)
- Local filesystem (testing)

---

#### Module 3: `tresor-auth` (Optional!)

**Purpose:** Authentication for the SaaS service

**Responsibilities:**
- ✅ Magic link generation/validation
- ✅ JWT creation/validation
- ✅ OAuth integration (Google, Apple)
- ✅ User management (CRUD)
- ✅ Session management

**Dependencies:**
- Spring Boot
- Spring Security
- JWT library
- PostgreSQL (for users table)

**Key Point:** This is **completely optional** for self-hosters!

**Interface-based design:**
```java
package io.tresor.auth;

// Interface that tresor-api depends on
public interface AuthService {
    User authenticate(String token);
    boolean hasPermission(User user, String resource, String action);
}

// tresor-auth provides implementation
public class MagicLinkAuthService implements AuthService {
    // Magic link implementation
}

// Self-hosters can provide their own
public class KeycloakAuthService implements AuthService {
    // Integrate with Keycloak
}

public class CustomSSOAuthService implements AuthService {
    // Integrate with company SSO
}
```

---

#### Module 4: `tresor-api` (The Application)

**Purpose:** REST API that orchestrates everything

**Responsibilities:**
- ✅ REST controllers
- ✅ Uses `tresor-core` for encryption
- ✅ Uses `tresor-auth` for auth (but auth is **pluggable**)
- ✅ Uses `tresor-storage-arweave` for storage
- ✅ Database for metadata
- ✅ Scheduling (blockchain monitor)

**Configuration:**
```yaml
# application.yml

tresor:
  auth:
    provider: magic-link  # or 'keycloak', 'oauth', 'custom'

  storage:
    provider: arweave     # or 'ipfs', 's3', 'local'

  blockchain:
    network: mainnet      # or 'testnet'
```

**Benefits:**
- Self-hosters can configure which modules to use
- Can disable auth completely (if using reverse proxy auth)
- Can swap storage backends

---

#### Module 5: `tresor-cli` (Bonus)

**Purpose:** Command-line tool for power users

**Responsibilities:**
- ✅ Encrypt messages locally
- ✅ Decrypt messages locally
- ✅ Upload to Arweave directly
- ✅ Check unlock status
- ✅ Uses `tresor-core` directly (no API needed!)

**Usage:**
```bash
# Encrypt a message
tresor encrypt \
  --message "Hello future!" \
  --unlock-date "2030-01-01" \
  --output message.tsr

# Upload to Arweave
tresor upload message.tsr

# Check if unlockable
tresor check message.tsr

# Decrypt (if unlocked)
tresor decrypt message.tsr
```

**Benefit:** Users can use Tresor without ANY server!

---

## Maven Multi-Module Structure

### Project Layout

```
tresor/
├── pom.xml                          # Parent POM
│
├── tresor-core/                     # Module 1: Core library
│   ├── pom.xml
│   └── src/
│       ├── main/java/
│       │   └── io/tresor/core/
│       │       ├── encryption/
│       │       │   ├── TimeLockEncryption.java
│       │       │   ├── WitnessEncryption.java
│       │       │   └── AESEncryption.java
│       │       ├── blockchain/
│       │       │   ├── BitcoinMonitor.java
│       │       │   └── BlockHeightCalculator.java
│       │       ├── storage/
│       │       │   └── StorageService.java (interface)
│       │       └── model/
│       │           ├── EncryptedMessage.java
│       │           └── TimeLockConfig.java
│       └── test/
│
├── tresor-storage-arweave/          # Module 2: Arweave storage
│   ├── pom.xml
│   └── src/
│       └── main/java/
│           └── io/tresor/storage/arweave/
│               └── ArweaveStorageService.java
│
├── tresor-auth/                     # Module 3: Auth (optional)
│   ├── pom.xml
│   └── src/
│       └── main/java/
│           └── io/tresor/auth/
│               ├── AuthService.java (interface)
│               ├── MagicLinkAuthService.java
│               ├── JwtService.java
│               └── entity/
│                   ├── User.java
│                   └── MagicLinkToken.java
│
├── tresor-api/                      # Module 4: REST API
│   ├── pom.xml
│   └── src/
│       └── main/java/
│           └── io/tresor/api/
│               ├── TresorApplication.java (main)
│               ├── controller/
│               │   ├── MessageController.java
│               │   └── AuthController.java
│               ├── service/
│               │   └── MessageService.java
│               └── config/
│                   ├── AuthConfig.java
│                   └── StorageConfig.java
│
└── tresor-cli/                      # Module 5: CLI tool
    ├── pom.xml
    └── src/
        └── main/java/
            └── io/tresor/cli/
                └── TresorCLI.java
```

---

### Parent pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.tresor</groupId>
    <artifactId>tresor-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>Tresor</name>
    <description>Time-locked message delivery system</description>

    <modules>
        <module>tresor-core</module>
        <module>tresor-storage-arweave</module>
        <module>tresor-auth</module>
        <module>tresor-api</module>
        <module>tresor-cli</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <spring-boot.version>4.0.0-RC1</spring-boot.version>
        <bouncycastle.version>1.77</bouncycastle.version>
        <bitcoinj.version>0.16.2</bitcoinj.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Internal modules -->
            <dependency>
                <groupId>io.tresor</groupId>
                <artifactId>tresor-core</artifactId>
                <version>${project.version}</version>
            </dependency>

            <dependency>
                <groupId>io.tresor</groupId>
                <artifactId>tresor-storage-arweave</artifactId>
                <version>${project.version}</version>
            </dependency>

            <dependency>
                <groupId>io.tresor</groupId>
                <artifactId>tresor-auth</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

---

### tresor-core/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.tresor</groupId>
        <artifactId>tresor-parent</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>tresor-core</artifactId>
    <name>Tresor Core</name>
    <description>Core encryption and time-lock library</description>

    <dependencies>
        <!-- Cryptography -->
        <dependency>
            <groupId>org.bouncycastle</groupId>
            <artifactId>bcprov-jdk18on</artifactId>
            <version>${bouncycastle.version}</version>
        </dependency>

        <!-- Bitcoin -->
        <dependency>
            <groupId>org.bitcoinj</groupId>
            <artifactId>bitcoinj-core</artifactId>
            <version>${bitcoinj.version}</version>
        </dependency>

        <!-- Utilities -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <!-- NOTE: NO Spring dependencies! Pure Java library -->
</project>
```

---

### tresor-api/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.tresor</groupId>
        <artifactId>tresor-parent</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>tresor-api</artifactId>
    <name>Tresor API</name>
    <description>REST API application</description>

    <dependencies>
        <!-- Internal modules -->
        <dependency>
            <groupId>io.tresor</groupId>
            <artifactId>tresor-core</artifactId>
        </dependency>

        <dependency>
            <groupId>io.tresor</groupId>
            <artifactId>tresor-storage-arweave</artifactId>
        </dependency>

        <!-- Auth is OPTIONAL! -->
        <dependency>
            <groupId>io.tresor</groupId>
            <artifactId>tresor-auth</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

---

## Module Dependencies

### Dependency Graph

```
tresor-cli
    └── tresor-core

tresor-api
    ├── tresor-core
    ├── tresor-storage-arweave
    └── tresor-auth (optional!)

tresor-storage-arweave
    └── tresor-core

tresor-auth
    └── (Spring Boot, no dependency on core)

tresor-core
    └── (BouncyCastle, BitcoinJ, no Spring!)
```

**Key Points:**
- `tresor-core` has NO dependencies on other modules
- `tresor-auth` is OPTIONAL for `tresor-api`
- Each module can be used independently

---

## Use Cases

### Use Case 1: Self-Hoster with Own Auth

**Scenario:** Company wants Tresor but uses Keycloak for SSO

**Solution:**
```java
// Don't use tresor-auth module
// Implement AuthService interface

@Service
public class KeycloakAuthService implements AuthService {

    @Autowired
    private KeycloakClient keycloak;

    @Override
    public User authenticate(String token) {
        // Validate token with Keycloak
        KeycloakUser keycloakUser = keycloak.validateToken(token);

        // Map to Tresor user
        return mapToUser(keycloakUser);
    }
}

// Configure in application.yml
tresor:
  auth:
    provider: keycloak
    keycloak:
      realm: company-realm
      url: https://keycloak.company.com
```

**Benefit:** Company uses their existing SSO, doesn't need tresor-auth!

---

### Use Case 2: Developer Using Core Library Only

**Scenario:** Developer wants time-lock encryption in their own app

**Solution:**
```xml
<!-- Just add one dependency -->
<dependency>
    <groupId>io.tresor</groupId>
    <artifactId>tresor-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

```java
// Use in any Java application
import io.tresor.core.TimeLockEncryption;

public class MyApp {
    public static void main(String[] args) {
        TimeLockEncryption encryption = new TimeLockEncryption();

        // Encrypt something for the future
        EncryptedMessage encrypted = encryption.encrypt(
            "Secret data",
            LocalDateTime.of(2026, 1, 1)
        );

        // Save wherever you want (DB, filesystem, S3, etc.)
        saveToMyStorage(encrypted);
    }
}
```

**Benefit:** No need to run Tresor server, just use the library!

---

### Use Case 3: Headless Mode (API Only)

**Scenario:** Enterprise wants to integrate Tresor into their existing system

**Solution:**
```yaml
# Run tresor-api without frontend
tresor:
  mode: headless
  auth:
    provider: oauth
    oauth:
      issuer: https://company.okta.com
  frontend:
    enabled: false
```

**Usage:**
```bash
# Just API endpoints
curl -X POST https://tresor-api.company.com/api/messages \
  -H "Authorization: Bearer $OKTA_TOKEN" \
  -d '{"content": "...", "unlockDate": "2030-01-01"}'
```

**Benefit:** Enterprise uses API only, builds their own UI!

---

### Use Case 4: CLI for Power Users

**Scenario:** User wants complete control, no cloud

**Solution:**
```bash
# Install CLI
brew install tresor-cli  # or download JAR

# Encrypt locally
tresor encrypt \
  --message "Hello future!" \
  --unlock-date "2030-01-01" \
  --output message.tsr

# Store on YOUR OWN Arweave wallet
tresor upload message.tsr --wallet ~/.arweave/wallet.json

# No server needed!
```

**Benefit:** Complete self-sovereignty!

---

## Implementation Guide

### Step 1: Start with tresor-core

**Create interface-first design:**

```java
// tresor-core/src/main/java/io/tresor/core/TimeLockService.java
package io.tresor.core;

import java.time.LocalDateTime;

public interface TimeLockService {
    /**
     * Encrypt message with time-lock
     */
    EncryptedMessage encrypt(String plaintext, LocalDateTime unlockDate);

    /**
     * Check if message can be decrypted
     */
    boolean canDecrypt(EncryptedMessage message, int currentBlockHeight);

    /**
     * Decrypt message (if unlocked)
     */
    String decrypt(EncryptedMessage message) throws EncryptionException;
}

// Implementation
public class WitnessTimeLockService implements TimeLockService {
    // Implementation using witness encryption
}

public class PuzzleTimeLockService implements TimeLockService {
    // Implementation using time-lock puzzles
}
```

**Benefit:** Users can choose implementation or provide their own!

---

### Step 2: Define Storage Interface

```java
// tresor-core/src/main/java/io/tresor/core/storage/StorageService.java
package io.tresor.core.storage;

public interface StorageService {
    /**
     * Save encrypted message
     * @return storage identifier (e.g., Arweave TX ID)
     */
    String save(EncryptedMessage message) throws StorageException;

    /**
     * Retrieve encrypted message
     */
    EncryptedMessage retrieve(String identifier) throws StorageException;

    /**
     * Calculate storage cost
     */
    StorageCost calculateCost(long sizeBytes);
}

// tresor-storage-arweave implements this
public class ArweaveStorageService implements StorageService {
    // Arweave-specific implementation
}

// Self-hosters can implement for S3, IPFS, etc.
public class S3StorageService implements StorageService {
    // S3 implementation
}
```

---

### Step 3: Make Auth Pluggable

```java
// tresor-api/src/main/java/io/tresor/api/auth/AuthService.java
package io.tresor.api.auth;

public interface AuthService {
    User authenticate(String credentials);
    boolean authorize(User user, String resource, String action);
}

// tresor-auth provides default implementation
@ConditionalOnProperty(name = "tresor.auth.provider", havingValue = "magic-link")
@Service
public class MagicLinkAuthService implements AuthService {
    // Magic link implementation
}

// Self-hosters provide their own
@ConditionalOnProperty(name = "tresor.auth.provider", havingValue = "keycloak")
@Service
public class KeycloakAuthService implements AuthService {
    // Keycloak implementation
}
```

---

## Summary & Recommendations

### Modular Architecture: YES! ✅

**Your instinct is 100% correct.**

**Modules:**
1. **tresor-core** - Pure Java crypto library (the valuable part!)
2. **tresor-storage-arweave** - Arweave storage impl
3. **tresor-auth** - Auth module (OPTIONAL for self-hosters)
4. **tresor-api** - REST API (uses all modules)
5. **tresor-cli** - CLI tool (uses core only)

**Benefits:**
- ✅ Self-hosters can replace auth with Keycloak, LDAP, SSO
- ✅ Core library reusable in other projects
- ✅ Better testing (test core independently)
- ✅ Enterprise-friendly (pluggable everything)
- ✅ More GitHub stars (developers use core library)

**Real-world validation:**
- Supabase does this (separate services)
- Spring Boot does this (optional starters)
- GitLab does this (pluggable auth)
- HashiCorp Vault does this (backend plugins)

### Next Steps

**I can create:**
1. Complete Maven multi-module project structure
2. tresor-core module (encryption library)
3. Interface definitions for pluggable components
4. Example implementations (Arweave storage, magic link auth)
5. Documentation for self-hosters (how to replace auth)

**What would you like me to build first?**
