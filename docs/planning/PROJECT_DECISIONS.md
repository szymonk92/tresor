# Tresor - Project Decisions Summary

> A chronological record of all major decisions made during the design phase

## Decision Log

### Decision 1: Core Concept Validation ✅
**Date:** 2025-01-07
**Question:** Is Bitcoin-based time-lock encryption real and feasible?
**Decision:** YES - Use Bitcoin blockchain + witness encryption

**Research:**
- Found academic paper: "How to build time-lock encryption" (Liu, Garcia, Ryan, 2018)
- Published in peer-reviewed journal (Designs, Codes and Cryptography)
- Bitcoin blockchain serves as computational reference clock
- Witness encryption makes decryption instant once block height reached

**Validation:** Approach is academically sound and practically feasible

---

### Decision 2: Multimedia Support in MVP ✅
**Date:** 2025-01-07
**Question:** Text only, or include images/audio/video in MVP?
**Decision:** Include text + images + audio in MVP, videos in Phase 2

**Rationale:**
- Images are emotionally powerful and small (2-5 MB)
- Audio (voice messages) are unique and personal (5 MB for 5 minutes)
- Videos are large (100 MB+) and complex - defer to Phase 2
- Encryption works the same for any file type (just bytes)

**Costs:**
- Small photo (2 MB): $0.05 storage
- Audio (5 MB): $0.125 storage
- Affordable and manageable

---

### Decision 3: Backend Technology - Java ✅
**Date:** 2025-01-07
**Question:** Node.js vs Java for backend?
**Decision:** Java 21 + Spring Boot 4.0.0-RC1

**Rationale:**
- Enterprise credibility (banks trust Java)
- Strong type system (compile-time safety)
- Excellent concurrency (blockchain monitoring)
- Mature crypto libraries (BouncyCastle)
- Better blockchain integration (BitcoinJ)
- Long-term stability ("messages to the future" needs stable tech)

**Trade-offs:**
- Slower startup time vs Node.js
- Higher memory usage
- Worth it for security and stability

---

### Decision 4: Open Source Business Model ✅
**Date:** 2025-01-07
**Question:** Can we make money with open source code?
**Decision:** YES - 100% open source with SaaS revenue model

**Evidence:**
- GitLab: $500M+/year (open source)
- Supabase: $100M valuation (fully open source)
- Bitwarden: Millions/year (open source password manager)
- Plausible: $1M+/year with 2 people (open source analytics)

**Model:**
- Code is 100% open source
- Revenue from hosted service (convenience)
- Only ~5% will self-host
- ~95% pay for hosted version
- Self-hosters become advocates

**Pricing:**
- Free: 3 messages/year
- Personal: $5/year
- Family: $20/year
- Legacy: $50/year

---

### Decision 5: Authentication - Magic Link ✅
**Date:** 2025-01-07
**Question:** Magic link vs password authentication?
**Decision:** Magic link (default), password optional

**Rationale - The Critical Insight:**
```
2025: User creates account
2025-2030: User doesn't touch app (5 years!)
2030: Message unlocks
2030: User tries to login... forgot password! 😱
```

**With Magic Link:**
- User clicks email → instantly logged in
- No password to remember after 5 years
- Zero friction, perfect for infrequent use

**Additional Benefits:**
- No password database to breach
- No "forgot password" flow
- Better security (email is the perimeter)
- Mobile-friendly

---

### Decision 6: Auth Provider - Custom Spring Security ✅
**Date:** 2025-01-07
**Question:** Keycloak vs Supabase vs Auth0 vs Custom?
**Decision:** Custom implementation with Spring Security

**Comparison:**
| Provider | Verdict | Why |
|----------|---------|-----|
| Keycloak | ⚠️ Later | Too complex for MVP |
| Supabase | ✅ Good option | But vendor dependency |
| Auth0 | ❌ Too expensive | $240/year minimum |
| Custom | ✅ **CHOSEN** | Simple, free, transparent |

**Rationale:**
- Simple to implement (JWT + magic link tokens)
- No external dependencies (can't go down)
- Perfect for open source (users can audit)
- Free (no per-user costs)
- Migration path to Keycloak later if needed

---

### Decision 7: Security for Unlocked Messages ✅
**Date:** 2025-01-07
**Question:** How to store messages after they unlock?
**Decision:** Re-encrypt with user's personal key

**Problem:**
```
Message unlocks in 2030
   ↓
Blockchain monitor decrypts it
   ↓
We have plaintext... can't store it in DB!
```

**Solution:**
```
1. Derive user's encryption key from email + secret pepper
2. Re-encrypt unlocked message with user's key
3. Store encrypted in database
4. When user views: decrypt with derived key
```

**Benefits:**
- Zero-knowledge (we can't read messages)
- Database breach doesn't expose content
- Only user can decrypt
- Key is derived on-demand (not stored)

**Implementation:**
```java
byte[] userKey = PBKDF2.derive(
    user.getEmail() + SECRET_PEPPER,
    SALT,
    100000 iterations,
    256 bits
);
```

---

### Decision 8: Modular Architecture ✅
**Date:** 2025-01-07
**Question:** Should auth be separate from core encryption?
**Decision:** YES - Split into 5 independent modules

**User's Insight:**
> "Authentication is not necessary for self hosting, right? Can we modularize?"

**Answer:** Absolutely! This is how successful open source works.

**Modules:**
1. **tresor-core** - Pure Java crypto library (NO Spring, NO auth)
2. **tresor-storage-arweave** - Storage implementation
3. **tresor-auth** - OPTIONAL authentication
4. **tresor-api** - Main Spring Boot application
5. **tresor-cli** - Command-line tool

**Benefits:**
- Self-hosters can replace auth (Keycloak, LDAP, SSO)
- Core library reusable in other projects
- Better testing (modules independent)
- Enterprise-friendly (pluggable components)
- More adoption (developers use core library)

**Real-World Validation:**
- Supabase: postgres, auth, storage as separate services
- Spring Boot: Optional starters (security separate)
- GitLab: Pluggable auth (LDAP, SAML, OAuth)
- HashiCorp Vault: Pluggable auth backends

---

### Decision 9: Payment Model for Long-Term Messages ✅
**Date:** 2025-01-07
**Question:** If user pays $20/year but message is in 10 years, how to handle?
**Decision:** Hybrid model - subscription + one-time storage fee

**Problem:**
```
User pays $20 for 1 year
Creates message to unlock in 10 years
Stops paying after year 1
Should message still deliver?
```

**Solution:**
```
1. Subscription ($20/year) covers:
   - Message creation (while subscribed)
   - Premium features
   - Support

2. Storage cost baked into subscription:
   - Messages created during subscription are stored forever
   - Already paid for via subscription fee

3. If user stops paying:
   - ✅ Existing messages STILL DELIVER
   - ❌ Can't create new messages (unless pay-per-message)
   - ❌ Basic notifications only
```

**Example:**
```
Year 1-3: Pay $60 total ($20 × 3 years)
Create 15 messages (unlock in year 18)
Stop paying in year 4
Year 18: All 15 messages STILL DELIVER ✅
```

**Ethical and sustainable!**

---

### Decision 10: Self-Custody Option ✅
**Date:** 2025-01-07
**Question:** Can users download messages for self-custody?
**Decision:** YES - Downloadable .tsr files

**How it Works:**
```
1. User creates message
2. Encrypted locally (client-side)
3. Two copies:
   - Upload to Arweave (via our service)
   - Download .tsr file to user's device

4. User stores file:
   - External hard drive
   - Cloud storage (Google Drive, Dropbox)
   - Give to trusted contact

5. When unlock date arrives:
   - Upload to tresor.io/unlock, OR
   - Use tresor-cli unlock file.tsr, OR
   - Manual process (full docs provided)
```

**Benefits:**
- Complete user ownership
- Not dependent on Tresor existing
- Works even if company dies
- Builds trust

**File Format:**
```json
{
  "version": "1.0",
  "unlock_date": "2030-01-01",
  "bitcoin_block_target": 920000,
  "encrypted_payload": "...",
  "time_locked_key": "...",
  "integrity": {
    "sha256": "..."
  }
}
```

---

### Decision 11: Unlock Mechanism ✅
**Date:** 2025-01-07
**Question:** Password to decrypt, or purely time-based?
**Decision:** Hybrid - Time-lock (automatic) + Authentication (login)

**Two Separate Concepts:**

**1. Encryption (Time-Based, Automatic):**
```
Bitcoin reaches block 920,000
   ↓
Message AUTOMATICALLY becomes decryptable
   ↓
No password can decrypt it early (math!)
```

**2. Authentication (Prove Identity):**
```
Time-lock expired, message decrypted
   ↓
User needs to prove they're the owner
   ↓
Magic link / password / OAuth
   ↓
View message
```

**Analogy:**
- Time-lock = Safe with timer (opens Jan 1, 2030, no combo opens it early)
- Authentication = Proving you own the safe (when it opens)

**For Self-Custody:**
- NO authentication needed
- If you have the .tsr file, you can decrypt it (pure crypto)

---

### Decision 12: Spring Boot Version ✅
**Date:** 2025-01-07
**Question:** Which Spring Boot version?
**Decision:** Spring Boot 4.0.0-RC1 (cutting edge!)

**Why:**
- Requires Java 21+ (perfect timing)
- Virtual threads support (Project Loom)
- Better observability
- Improved security defaults
- We're building for the future anyway!

**Trade-offs:**
- RC version (not GA yet)
- Less mature (but close to release)
- Worth it for modern features

---

## Technical Decisions Summary

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| **Backend** | Java 21 + Spring Boot 4.0 | Enterprise credibility, stability |
| **Frontend** | Next.js 14 + TypeScript | Modern, fast, SEO-friendly |
| **Database** | PostgreSQL 16+ | Reliable, proven, JSON support |
| **Cryptography** | BouncyCastle | Industry standard |
| **Blockchain** | Bitcoin (BitcoinJ) | Most secure, predictable |
| **Storage** | Arweave | Permanent, decentralized |
| **Auth** | Magic link (custom) | Perfect for 5+ year gaps |
| **Testing** | JUnit + Testcontainers | Real database, reliable |
| **License** | AGPL (backend), MIT (libs) | Protect business, enable adoption |

---

## Architecture Principles

### 1. Modular Design
- Core crypto is separate from auth
- Each module has single responsibility
- Pluggable components (bring your own auth/storage)

### 2. Security First
- Client-side encryption (we never see plaintext)
- Zero-knowledge architecture
- Open source for auditability
- User-encrypted inbox for unlocked messages

### 3. Long-Term Thinking
- Decentralized storage (survives company failure)
- Stable technology (Java 21 LTS)
- Self-custody option
- Open source (community can maintain)

### 4. User Ownership
- Users own their data
- Self-custody downloads
- Updateable delivery addresses
- Multiple recovery methods

---

## Development Approach

### Phase 1: Foundation (Current)
1. Create Maven multi-module structure
2. Define core interfaces (contract-first)
3. Implement crypto (tresor-core)
4. Build storage integration (Arweave)
5. Add blockchain monitoring

### Phase 2: Application
1. Build REST API (tresor-api)
2. Implement auth (tresor-auth)
3. Create frontend (Next.js)
4. Integration testing (Testcontainers)

### Phase 3: Polish
1. Documentation
2. Security audit
3. Beta testing
4. Public launch

---

## Open Questions

### Q1: Witness Encryption vs Time-Lock Puzzles?
**Status:** To be decided during implementation
**Options:**
- Start with time-lock puzzles (simpler, proven)
- Upgrade to witness encryption in Phase 2
- Or implement witness encryption from start (more complex)

**Recommendation:** Start with puzzles for MVP, upgrade later

---

### Q2: Exact License?
**Status:** To be decided before public release
**Leading option:**
- AGPL for backend (prevents parasitic competitors)
- MIT for libraries (maximum adoption)
- MIT for frontend

---

### Q3: Self-Hosting Documentation Depth?
**Status:** To be created
**Need:**
- Complete setup guide
- How to replace auth with Keycloak
- How to implement custom storage
- Docker compose examples

---

## Next Steps (Immediate)

1. ✅ Create project summary (this document)
2. ⏳ Set up Maven multi-module structure
3. ⏳ Define core interfaces
4. ⏳ Implement crypto in tresor-core
5. ⏳ Implement Arweave storage
6. ⏳ Build blockchain monitoring

---

## References

All detailed documentation in:
- [PRD.md](./PRD.md)
- [IMPLEMENTATION_DETAILS.md](./IMPLEMENTATION_DETAILS.md)
- [MULTIMEDIA_ANALYSIS.md](./MULTIMEDIA_ANALYSIS.md)
- [MARKETING_AND_BUSINESS_MODEL.md](./MARKETING_AND_BUSINESS_MODEL.md)
- [TECHNOLOGY_STACK.md](./TECHNOLOGY_STACK.md)
- [JAVA_BACKEND_STACK.md](./JAVA_BACKEND_STACK.md)
- [AUTHENTICATION_SECURITY.md](./AUTHENTICATION_SECURITY.md)
- [MODULAR_ARCHITECTURE.md](./MODULAR_ARCHITECTURE.md)

---

**Last Updated:** 2025-01-07
**Status:** Architecture Complete, Implementation Starting
