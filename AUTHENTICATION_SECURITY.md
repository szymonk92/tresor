# Tresor - Authentication & Security Architecture

## Table of Contents
1. [Authentication Strategy: Magic Link vs Password](#authentication-strategy)
2. [Auth Provider Comparison](#auth-provider-comparison)
3. [Recommended Solution](#recommended-solution)
4. [Security for Unlocked Messages](#security-for-unlocked-messages)
5. [Complete Authentication Flow](#complete-authentication-flow)
6. [Spring Boot 4.0 + Java 21 Setup](#spring-boot-4-setup)
7. [Implementation Guide](#implementation-guide)

---

## Authentication Strategy

### Why Magic Link is PERFECT for Tresor

**Your Instinct is 100% Correct!** Magic link is the RIGHT choice for this application.

#### The Unique Problem: Long Time Gaps

**Scenario:**
```
2025: User creates account, sends message to unlock in 2030
2025-2029: User doesn't touch the app (5 years!)
2030: Message unlocks, user gets email notification
2030: User clicks link... but they forgot their password!
```

**With Passwords:**
- ❌ User forgot password after 5 years
- ❌ Password reset flow adds friction
- ❌ User might not even remember they used this app
- ❌ Barrier to accessing their important message

**With Magic Link:**
- ✅ User clicks email notification link
- ✅ Automatically logged in (token in URL)
- ✅ Immediately sees their message
- ✅ Zero friction, perfect UX

#### Additional Benefits of Magic Link

1. **No Password Fatigue** 🧠
   - Users don't need to remember another password
   - No password reuse security issues
   - No "forgot password" flow needed

2. **Better Security** 🔐
   - No password database to breach
   - No weak passwords ("password123")
   - Email account is the security perimeter

3. **Simpler Implementation** 💻
   - No password hashing/salting
   - No password strength validation
   - No password reset emails

4. **Better for Infrequent Use** 📅
   - Users may only access once every few years
   - Magic link is perfect for this pattern

5. **Mobile-Friendly** 📱
   - Click link in email, automatically logged in
   - No typing passwords on mobile keyboards

### When Passwords Still Make Sense

**For frequent users:**
- Enterprise admins managing many messages
- Power users checking status regularly

**Solution:** Offer BOTH, default to magic link
- Magic link: Default, recommended
- Password: Optional, for users who prefer it
- OAuth: Google/Apple for convenience

---

## Auth Provider Comparison

### Option 1: Keycloak (Self-Hosted)

**What it is:**
- Open-source identity and access management
- Self-hosted (you run it)
- Enterprise-grade features

**Pros:**
- ✅ 100% open source (aligns with your values)
- ✅ Self-hosted = full control
- ✅ No vendor lock-in
- ✅ Supports magic links, passwords, OAuth, SAML
- ✅ User federation (can migrate users between instances)
- ✅ Multi-tenancy (for white-label later)
- ✅ Admin UI (manage users)
- ✅ Free (no per-user costs)

**Cons:**
- ❌ Complex to set up (steep learning curve)
- ❌ Requires dedicated infrastructure (RAM hungry: ~1-2 GB)
- ❌ Maintenance burden (updates, security patches)
- ❌ Overkill for MVP (too many features)
- ❌ Slower development (more integration work)

**Cost:**
- Free software
- Infrastructure: ~$20-50/month (dedicated server for Keycloak)
- Total: ~$20-50/month

**Verdict:** ⚠️ Great for enterprise, overkill for MVP

---

### Option 2: Supabase Auth (Managed)

**What it is:**
- Managed authentication service
- Built on PostgreSQL
- Part of Supabase (Postgres-as-a-Service)

**Pros:**
- ✅ Open source (can self-host if needed)
- ✅ Built-in magic links (primary auth method)
- ✅ OAuth providers (Google, Apple, GitHub, etc.)
- ✅ Very fast to integrate
- ✅ Generous free tier (50,000 MAU)
- ✅ Already uses PostgreSQL (same as our DB)
- ✅ Great developer experience
- ✅ Built-in email templates
- ✅ Row-level security (RLS) for database

**Cons:**
- ❌ Vendor dependency (hosted service)
- ❌ Paid tiers can get expensive at scale
- ❌ Less customizable than Keycloak
- ❌ Limited enterprise features (no SAML)

**Cost:**
- Free: 50,000 MAU
- Pro: $25/month (100,000 MAU)
- For most MVPs: FREE

**Verdict:** ✅ Excellent for MVP, can migrate later if needed

---

### Option 3: Auth0 / Okta (Enterprise Managed)

**What it is:**
- Enterprise-grade auth-as-a-service
- Owned by Okta

**Pros:**
- ✅ Enterprise credibility
- ✅ Excellent documentation
- ✅ Magic links supported
- ✅ All OAuth providers
- ✅ Advanced security features

**Cons:**
- ❌ Expensive! ($240/year minimum, $0.05 per MAU)
- ❌ Not open source
- ❌ Vendor lock-in
- ❌ Overkill for MVP

**Cost:**
- $240/year minimum
- $0.05 per MAU after 7,000 users
- At 10,000 users: ~$390/month

**Verdict:** ❌ Too expensive for MVP

---

### Option 4: Custom Implementation (Spring Security)

**What it is:**
- Build your own using Spring Security
- Store users in your PostgreSQL database
- Implement magic link yourself

**Pros:**
- ✅ Complete control
- ✅ No external dependencies
- ✅ No recurring costs
- ✅ Simple (just what you need)
- ✅ Perfect for learning
- ✅ Aligns with open-source model

**Cons:**
- ❌ More development time upfront
- ❌ You maintain security yourself
- ❌ Need to implement email sending
- ❌ Need to implement token management

**Cost:**
- Free (just your development time)

**Verdict:** ✅ Great option! Simple, transparent, no dependencies

---

### Option 5: Firebase Authentication

**What it is:**
- Google's auth service

**Pros:**
- ✅ Easy to integrate
- ✅ Free tier (generous)
- ✅ OAuth providers

**Cons:**
- ❌ Google dependency
- ❌ Not ideal for backend-only apps
- ❌ Designed for client SDKs

**Verdict:** ⚠️ Better for mobile apps, not ideal here

---

## Recommended Solution

### For MVP: Custom Implementation with Spring Security

**Why:**
1. **Simple & Transparent** - You understand exactly how it works
2. **No Dependencies** - No third-party service can go down
3. **Perfect for Open Source** - Users can audit security
4. **Cost: FREE** - No per-user fees
5. **Fast to Build** - Simpler than integrating Keycloak
6. **Aligns with Philosophy** - Self-hosted, open source

### For Scale (Later): Keycloak

**When to migrate:**
- 10,000+ users
- Need multi-tenancy (white-label)
- Need SAML for enterprise
- Need advanced user management

**Migration path:**
- Start with custom auth (PostgreSQL user table)
- Later: Export users to Keycloak
- User federation makes this easy

### Architecture: Custom Magic Link Authentication

**Flow:**
```
1. User enters email
   ↓
2. Backend generates secure token (UUID + expiry)
   ↓
3. Store token in database (expires in 15 minutes)
   ↓
4. Send email with magic link: tresor.io/auth/verify?token=abc123
   ↓
5. User clicks link
   ↓
6. Backend validates token (checks DB, not expired)
   ↓
7. If valid: Create JWT session token
   ↓
8. Set JWT in cookie (httpOnly, secure)
   ↓
9. User is logged in!
```

**Simple, secure, no external dependencies!**

---

## Security for Unlocked Messages

### The Problem

**Scenario:**
```
Message unlocks in 2030
   ↓
Blockchain monitor decrypts it (witness encryption)
   ↓
Where do we store the decrypted message?
   ↓
Can't store it in plaintext on our server!
```

**Why NOT plaintext:**
- ❌ Database breach exposes all unlocked messages
- ❌ Admins could read users' messages
- ❌ Violates zero-knowledge principle

### Solution: User-Encrypted Inbox

**Architecture:**

```
┌─────────────────────────────────────────────────────┐
│              BEFORE UNLOCK (2025-2030)              │
├─────────────────────────────────────────────────────┤
│                                                       │
│  Message on Arweave:                                 │
│  ┌─────────────────────────────────────────────┐   │
│  │ Encrypted with TIME-LOCK                     │   │
│  │ - Cannot be decrypted until 2030             │   │
│  │ - Witness encryption + Bitcoin block height  │   │
│  └─────────────────────────────────────────────┘   │
│                                                       │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│               AFTER UNLOCK (2030)                    │
├─────────────────────────────────────────────────────┤
│                                                       │
│  Step 1: Blockchain monitor detects unlock          │
│  Step 2: Derive decryption key from Bitcoin block   │
│  Step 3: Decrypt message (now we have plaintext)    │
│  Step 4: WHERE TO STORE IT?                          │
│                                                       │
│  ❌ BAD: Store plaintext in database                │
│     → Database breach exposes messages               │
│                                                       │
│  ✅ GOOD: Re-encrypt with USER'S KEY                │
│                                                       │
│  Message in database:                                │
│  ┌─────────────────────────────────────────────┐   │
│  │ Encrypted with USER'S PERSONAL KEY           │   │
│  │ - Derived from user's email (or password)    │   │
│  │ - Only user can decrypt                      │   │
│  │ - NOT time-locked anymore (always accessible)│   │
│  └─────────────────────────────────────────────┘   │
│                                                       │
└─────────────────────────────────────────────────────┘
```

### How User's Personal Key Works

**Option A: Derive from Email (Recommended for Magic Link)**

```java
// When user logs in with magic link:
public byte[] deriveUserKey(String email, String magicToken) {
    // 1. Combine email + long-lived secret
    String input = email + SECRET_PEPPER;

    // 2. Use PBKDF2 to derive encryption key
    byte[] userKey = PBKDF2.derive(
        input.getBytes(),
        SALT,
        100000, // iterations
        256 // key length
    );

    return userKey;
}

// When saving unlocked message:
public void saveUnlockedMessage(User user, byte[] plaintext) {
    // 1. Derive user's personal key
    byte[] userKey = deriveUserKey(user.getEmail(), SECRET_PEPPER);

    // 2. Encrypt message with user's key
    byte[] encrypted = AES.encrypt(plaintext, userKey);

    // 3. Store encrypted in database
    database.save(encrypted);
}

// When user views message:
public byte[] getUserMessage(User user, UUID messageId) {
    // 1. Fetch encrypted message from database
    byte[] encrypted = database.get(messageId);

    // 2. Derive user's personal key
    byte[] userKey = deriveUserKey(user.getEmail(), SECRET_PEPPER);

    // 3. Decrypt and return
    return AES.decrypt(encrypted, userKey);
}
```

**Key Points:**
- User's key is derived from their email (which doesn't change often)
- Key is NOT stored anywhere (derived on-demand)
- We use a long-lived SECRET_PEPPER (stored in environment variables)
- Even if database is breached, messages stay encrypted

**Option B: Derive from Password (If Using Passwords)**

```java
public byte[] deriveUserKey(String email, String password) {
    // Use Argon2 (better than PBKDF2 for passwords)
    byte[] userKey = Argon2.hash(
        password.getBytes(),
        email.getBytes(), // salt
        100000, // iterations
        256 // key length
    );

    return userKey;
}
```

**Challenge:** What if user changes email/password?
- Need to re-encrypt all their messages with new key
- Or: Store user's key encrypted with their credentials

**Option C: Store User's Key (Encrypted with Credentials)**

```java
// When user first signs up:
public void createUser(String email) {
    // 1. Generate random encryption key for this user
    byte[] userMasterKey = SecureRandom.generateBytes(32);

    // 2. Derive wrapping key from email
    byte[] wrappingKey = PBKDF2.derive(email + SECRET_PEPPER);

    // 3. Encrypt user's master key with wrapping key
    byte[] encryptedMasterKey = AES.encrypt(userMasterKey, wrappingKey);

    // 4. Store encrypted master key in database
    database.users.create({
        email: email,
        encryptedMasterKey: encryptedMasterKey
    });
}

// When user logs in:
public byte[] getUserMasterKey(User user) {
    // 1. Derive wrapping key from email
    byte[] wrappingKey = PBKDF2.derive(user.getEmail() + SECRET_PEPPER);

    // 2. Decrypt user's master key
    byte[] userMasterKey = AES.decrypt(user.getEncryptedMasterKey(), wrappingKey);

    return userMasterKey;
}
```

**Advantage:** If user changes email, just re-wrap the master key (no need to re-encrypt all messages)

### Recommended Approach

**For MVP:**
- Use **Option A** (derive from email)
- Simple, no extra database fields
- Works perfectly with magic link auth

**For Production:**
- Use **Option C** (store encrypted master key)
- More flexible (can change email without re-encrypting everything)
- Slightly more complex

### Database Schema for Unlocked Messages

```sql
CREATE TABLE unlocked_messages (
    id UUID PRIMARY KEY,
    message_id UUID REFERENCES messages(id),
    user_id UUID REFERENCES users(id),

    -- Encrypted content (using user's personal key)
    encrypted_content BYTEA NOT NULL,

    -- IV for AES-GCM
    encryption_iv BYTEA NOT NULL,

    -- Metadata (NOT encrypted)
    unlocked_at TIMESTAMP DEFAULT NOW(),
    content_type TEXT, -- 'text', 'image', 'audio', 'video'
    size_bytes INTEGER,

    -- Integrity check
    content_hash TEXT, -- SHA-256 of plaintext (for verification)

    UNIQUE(message_id)
);
```

---

## Complete Authentication Flow

### Magic Link Flow (Detailed)

#### Step 1: User Requests Magic Link

**Frontend:**
```typescript
// User enters email
async function requestMagicLink(email: string) {
    const response = await fetch('/api/auth/magic-link', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email })
    });

    if (response.ok) {
        // Show success message
        alert('Check your email for login link!');
    }
}
```

**Backend (Java):**
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/magic-link")
    public ResponseEntity<?> sendMagicLink(@RequestBody MagicLinkRequest request) {
        String email = request.getEmail();

        // 1. Find or create user
        User user = userService.findOrCreateByEmail(email);

        // 2. Generate secure token (UUID + timestamp)
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        // 3. Store token in database
        MagicLinkToken magicToken = new MagicLinkToken();
        magicToken.setToken(token);
        magicToken.setUser(user);
        magicToken.setExpiresAt(expiresAt);
        magicToken.setUsed(false);
        magicTokenRepository.save(magicToken);

        // 4. Send email with link
        String magicLink = appUrl + "/auth/verify?token=" + token;
        emailService.sendMagicLink(email, magicLink);

        return ResponseEntity.ok(Map.of("message", "Check your email!"));
    }
}
```

#### Step 2: User Clicks Magic Link

**Frontend:**
```typescript
// URL: /auth/verify?token=abc123
async function verifyMagicLink(token: string) {
    const response = await fetch(`/api/auth/verify?token=${token}`, {
        method: 'POST',
        credentials: 'include' // Important: include cookies
    });

    if (response.ok) {
        const { sessionToken } = await response.json();

        // Store in localStorage (or cookie is already set by backend)
        localStorage.setItem('sessionToken', sessionToken);

        // Redirect to dashboard
        window.location.href = '/dashboard';
    } else {
        alert('Invalid or expired link');
    }
}
```

**Backend (Java):**
```java
@PostMapping("/verify")
public ResponseEntity<?> verifyMagicLink(@RequestParam String token, HttpServletResponse response) {
    // 1. Find token in database
    Optional<MagicLinkToken> magicTokenOpt = magicTokenRepository.findByToken(token);

    if (magicTokenOpt.isEmpty()) {
        return ResponseEntity.status(401).body("Invalid token");
    }

    MagicLinkToken magicToken = magicTokenOpt.get();

    // 2. Check if expired
    if (magicToken.getExpiresAt().isBefore(LocalDateTime.now())) {
        return ResponseEntity.status(401).body("Token expired");
    }

    // 3. Check if already used
    if (magicToken.isUsed()) {
        return ResponseEntity.status(401).body("Token already used");
    }

    // 4. Mark as used
    magicToken.setUsed(true);
    magicTokenRepository.save(magicToken);

    // 5. Generate JWT session token
    User user = magicToken.getUser();
    String jwtToken = jwtService.generateToken(user);

    // 6. Set JWT in HTTP-only cookie
    Cookie cookie = new Cookie("session", jwtToken);
    cookie.setHttpOnly(true);
    cookie.setSecure(true); // HTTPS only
    cookie.setPath("/");
    cookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
    response.addCookie(cookie);

    // 7. Return success
    return ResponseEntity.ok(Map.of(
        "sessionToken", jwtToken,
        "user", UserResponse.from(user)
    ));
}
```

#### Step 3: Subsequent Requests (JWT Authentication)

**Frontend:**
```typescript
// All API requests include session cookie automatically
async function getMessages() {
    const response = await fetch('/api/messages', {
        credentials: 'include' // Sends session cookie
    });

    return response.json();
}
```

**Backend (Java):**
```java
// Spring Security filter validates JWT on every request

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Extract JWT from cookie
        String jwt = extractJwtFromCookie(request);

        if (jwt != null && jwtService.validateToken(jwt)) {
            // 2. Parse user from JWT
            String email = jwtService.getEmailFromToken(jwt);
            User user = userService.findByEmail(email);

            // 3. Set authentication in Spring Security context
            Authentication auth = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
```

### Database Schema for Auth

```sql
-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT UNIQUE NOT NULL,
    unique_id TEXT UNIQUE NOT NULL, -- tresor_user_12345

    -- User's encrypted master key (for encrypting unlocked messages)
    encrypted_master_key BYTEA,

    -- Optional password (if user prefers password auth)
    password_hash TEXT,

    created_at TIMESTAMP DEFAULT NOW(),
    last_login_at TIMESTAMP,

    -- Account status
    email_verified BOOLEAN DEFAULT FALSE,
    account_status TEXT DEFAULT 'active' -- 'active', 'suspended', 'deleted'
);

-- Magic link tokens
CREATE TABLE magic_link_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token TEXT UNIQUE NOT NULL,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,

    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT NOW(),

    -- Security
    ip_address TEXT,
    user_agent TEXT
);

-- Create index for fast token lookups
CREATE INDEX idx_magic_link_tokens_token ON magic_link_tokens(token) WHERE used = FALSE;
```

---

## Spring Boot 4.0 Setup

### Why Spring Boot 4.0 RC1?

**Spring Boot 4.0** (currently RC1, releasing soon):
- ✅ Based on Spring Framework 6.2
- ✅ Requires Java 21+ (perfect timing!)
- ✅ Virtual threads support (Project Loom)
- ✅ Better observability
- ✅ Improved security defaults
- ✅ Native compilation improvements (GraalVM)

**You're on the cutting edge!** 🚀

### Complete pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.0-RC1</version>
        <relativePath/>
    </parent>

    <groupId>io.tresor</groupId>
    <artifactId>tresor-backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>Tresor Backend</name>
    <description>Time-locked message delivery service</description>

    <properties>
        <java.version>21</java.version>
        <testcontainers.version>1.19.3</testcontainers.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>

        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.3</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Cryptography -->
        <dependency>
            <groupId>org.bouncycastle</groupId>
            <artifactId>bcprov-jdk18on</artifactId>
            <version>1.77</version>
        </dependency>

        <!-- Bitcoin -->
        <dependency>
            <groupId>org.bitcoinj</groupId>
            <artifactId>bitcoinj-core</artifactId>
            <version>0.16.2</version>
        </dependency>

        <!-- Utilities -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Testcontainers -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>${testcontainers.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <!-- Spring Milestones repo (for RC versions) -->
    <repositories>
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>

    <pluginRepositories>
        <pluginRepository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </pluginRepository>
    </pluginRepositories>
</project>
```

### Testcontainers Configuration

**Base Test Class:**
```java
package io.tresor;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("tresor_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }
}
```

**Example Integration Test:**
```java
package io.tresor.service;

import io.tresor.BaseIntegrationTest;
import io.tresor.entity.User;
import io.tresor.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCreateUserWithMagicLink() {
        // Given
        String email = "test@example.com";

        // When
        User user = userService.findOrCreateByEmail(email);

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getUniqueId()).startsWith("tresor_user_");

        // Verify in database
        User found = userRepository.findByEmail(email).orElseThrow();
        assertThat(found.getId()).isEqualTo(user.getId());
    }
}
```

---

## Implementation Guide

### Step 1: Create User Entity

```java
package io.tresor.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "unique_id", unique = true, nullable = false)
    private String uniqueId; // tresor_user_12345

    @Column(name = "encrypted_master_key")
    private byte[] encryptedMasterKey;

    @Column(name = "password_hash")
    private String passwordHash; // Optional

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "account_status")
    private String accountStatus = "active";

    @PrePersist
    public void prePersist() {
        if (uniqueId == null) {
            uniqueId = "tresor_user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
    }
}
```

### Step 2: Create Magic Link Token Entity

```java
package io.tresor.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "magic_link_tokens")
@Data
public class MagicLinkToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;
}
```

### Step 3: Create JWT Service

```java
package io.tresor.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.tresor.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationDays;

    public JwtService(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.expiration-days:30}") long expirationDays
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationDays = expirationDays;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(expirationDays, ChronoUnit.DAYS);

        return Jwts.builder()
            .subject(user.getEmail())
            .claims(Map.of(
                "userId", user.getId().toString(),
                "uniqueId", user.getUniqueId()
            ))
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(secretKey)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        return claims.getSubject();
    }
}
```

---

## Summary & Recommendations

### Authentication: Magic Link (Custom Implementation)

✅ **Recommended:** Build your own with Spring Security
- Simple, transparent, no dependencies
- Perfect for open source (auditable)
- Free (no per-user costs)
- Fast to implement

❌ **Not Recommended for MVP:** Keycloak
- Too complex for MVP
- Consider for enterprise features later

### Security for Unlocked Messages

✅ **Approach:** Re-encrypt with user's personal key
- Derive key from email + secret pepper
- Store encrypted in database
- Even database breach doesn't expose messages

### Technology Stack

✅ **Confirmed:**
- Java 21 LTS
- Spring Boot 4.0.0 RC1
- PostgreSQL
- Testcontainers
- BouncyCastle (crypto)
- BitcoinJ (blockchain)

### Next Steps

**What I can create next:**
1. Complete Spring Boot project (all entities, services, controllers)
2. Flyway database migrations
3. Complete authentication flow (working code)
4. Testcontainers integration tests
5. Docker setup (docker-compose for local dev)

**What would you like me to build first?**
