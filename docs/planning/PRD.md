# Product Requirements Document: Tresor - Time-Locked Message Delivery System

## Executive Summary

Tresor is a future self-messaging application that allows users to send messages to themselves that are cryptographically guaranteed to be unreadable until a specified future date, with delivery mechanisms designed to persist across technological changes, decades into the future.

## Problem Statement

### Core Challenges

1. **Premature Access Prevention**: Traditional delayed messaging systems store messages in readable form with access controlled by authentication. This creates risks:
   - Messages can be accessed by compromised systems
   - Users may be tempted to retrieve messages early
   - Database breaches expose all future messages
   - No cryptographic guarantee of time-locking

2. **Long-term Delivery Guarantees**: Ensuring message delivery 1, 5, 10, or even 20 years in the future presents unique challenges:
   - Email addresses change or become obsolete
   - Phone numbers change
   - Companies and services shut down
   - Technology platforms evolve or disappear
   - User's digital identity evolves

## Research-Backed Solution

### Time-Lock Cryptography

Based on academic research, we have identified two complementary approaches to cryptographically "forget" messages:

#### Approach 1: Bitcoin Blockchain + Witness Encryption (Primary)

**Research Foundation**: "How to build time-lock encryption" by Liu, Garcia, and Ryan (2018)

**Mechanism**:
- Uses Bitcoin blockchain as a computational reference clock
- Bitcoin produces blocks approximately every 10 minutes (predictable, public, immutable)
- Messages are encrypted using witness encryption schemes tied to future blockchain heights
- Decryption becomes possible only when Bitcoin blockchain reaches the specified height
- No central authority required
- Decryption is instant once the deadline is reached

**Example**:
- Current Bitcoin block height: 820,000 (January 2025)
- User wants message delivered in 1 year
- Approximate blocks in 1 year: 52,560 (365 days × 144 blocks/day)
- Message locked until block 872,560
- When blockchain reaches this height, witness encryption allows instant decryption

**Advantages**:
- Provably secure: Message cannot be decrypted early even with supercomputers
- Decentralized: No dependency on any company or service
- Publicly verifiable: Anyone can verify the blockchain height
- Instant decryption: Once deadline reached, decryption is computational fast
- No interaction needed: User doesn't need to perform expensive computation

**Limitations**:
- Witness encryption is still evolving (requires SNARKs or similar constructions)
- Implementation complexity is high
- Bitcoin blockchain must continue operating (high probability over decades)

#### Approach 2: Time-Lock Puzzles (Fallback/Hybrid)

**Research Foundation**: Rivest, Shamir, Wagner (1996) - "Time-lock puzzles and timed-release crypto"

**Mechanism**:
- Based on sequential modular squaring in RSA groups
- Creates computational puzzle requiring specific amount of time to solve
- Cannot be parallelized effectively
- Puzzle difficulty calibrated to time delay desired

**Example**:
- Encrypt message with puzzle requiring 1 year of sequential computation
- To unlock early, attacker must perform ~1 year of computation on fastest CPU
- At deadline, decryption service performs computation and releases key

**Advantages**:
- Mature cryptographic primitive
- Well-understood security properties
- No dependency on external systems (like Bitcoin)

**Limitations**:
- Requires someone to actually compute the solution (computational cost)
- Vulnerable to dramatic increases in computing power
- Less precise timing (depends on hardware improvements)
- User or service must perform computation to decrypt

#### Recommended Hybrid Approach

- **Primary**: Bitcoin + Witness Encryption for messages up to 10 years
- **Fallback**: Time-lock puzzles for longer durations or if Bitcoin approach fails
- **Redundancy**: Store encrypted messages in multiple formats

### Long-term Delivery Mechanisms

#### 1. Decentralized Permanent Storage

**Arweave Integration**:
- One-time payment for permanent storage (~$25/GB)
- Encrypted messages stored on Arweave blockchain
- Data replication across global network
- Economic incentive model ensures long-term persistence
- No recurring storage fees

**Architecture**:
```
User Message → Time-lock Encryption → Arweave Permanent Storage
                                           ↓
                        Decryption Service monitors blockchain height
                                           ↓
                        Multi-channel notification when unlocked
```

#### 2. Multi-Identity Registry

**Concept**: Create a persistent, updateable identity registry that follows the user

**Components**:
- **Primary ID**: Immutable unique identifier (generated at signup)
- **Contact Methods**: User maintains multiple contact channels
  - Email addresses (multiple)
  - Phone numbers (multiple)
  - Blockchain addresses
  - Social media handles
  - Physical addresses
  - Emergency contacts

**Update Mechanisms**:
- User can update contact information anytime
- Annual "proof of life" reminders to verify/update contacts
- Trusted contacts can help update information if user loses access
- OAuth integration with major platforms (Google, Microsoft, Apple)

#### 3. Smart Notification System

**Multi-Channel Delivery**:
When message becomes decryptable:
1. **Primary Channels** (attempt all):
   - Email to all registered addresses
   - SMS to all registered phones
   - Push notifications (if app installed)
   - In-app notification

2. **Secondary Channels**:
   - Physical mail (if registered)
   - Emergency contact notification (for long delays)
   - Social media DMs (if authorized)

3. **Persistent Availability**:
   - Message always accessible via Primary ID
   - Web portal available indefinitely
   - API for third-party integrations
   - Blockchain-based proof of message existence

#### 4. Technology Change Adaptation

**Protocol Versioning**:
- Messages include protocol version
- Backward compatibility maintained indefinitely
- Decryption libraries archived permanently
- Open-source implementation ensures community maintenance

**Platform Independence**:
- Web-based interface (no app dependency)
- Progressive Web App (PWA) for offline access
- API for third-party clients
- Command-line tools for developers

**Service Continuity Plans**:
- Open-source codebase allows community forks
- Decentralized infrastructure (no single point of failure)
- Escrow arrangements for domain/service continuation
- Non-profit foundation governance (long-term stability)

## Technical Architecture

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│                        User Interface                        │
│  (Web Portal, Mobile PWA, API, Browser Extension)          │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                   Application Layer                          │
│  - Identity Management                                       │
│  - Message Composition                                       │
│  - Contact Registry                                          │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                 Cryptographic Layer                          │
│  - Witness Encryption (Bitcoin-based)                       │
│  - Time-lock Puzzle Generation (fallback)                   │
│  - Key Management                                            │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Storage Layer                             │
│  - Arweave (permanent storage)                              │
│  - Filecoin (redundancy)                                    │
│  - IPFS (content addressing)                                │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                  Monitoring Services                         │
│  - Bitcoin Blockchain Monitor                                │
│  - Decryption Service (when deadline reached)               │
│  - Notification Service                                      │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow

**Message Creation**:
1. User composes message in application
2. User selects target date/time
3. System calculates Bitcoin block height for target date
4. Message encrypted using witness encryption + block height
5. Encrypted payload stored on Arweave with metadata
6. User receives confirmation + message ID
7. Message added to monitoring queue

**Message Delivery**:
1. Monitoring service tracks Bitcoin blockchain
2. When target block height reached, decryption possible
3. System decrypts message automatically
4. Notification service attempts all registered contact methods
5. Message available in user's inbox (web portal)
6. Delivery confirmation logged

### Security Model

**Encryption Properties**:
- **Confidentiality**: Message content cryptographically hidden until deadline
- **Integrity**: Tampering detected via cryptographic signatures
- **Authenticity**: Sender verification via digital signatures
- **Time-lock**: Provably cannot be decrypted before deadline (assuming Bitcoin operates)

**Privacy Protections**:
- Zero-knowledge about message content (end-to-end encrypted)
- Metadata minimization
- Optional anonymous message creation (no identity required)
- GDPR-compliant data handling

**Threat Model**:
- **Trusted**: User's own device (during message creation)
- **Adversarial**:
  - Storage providers (cannot read encrypted messages)
  - Network observers (cannot infer message content)
  - Malicious insiders (no access to decryption keys)
  - Compromised servers (cannot decrypt early)
- **Assumptions**:
  - Bitcoin blockchain continues operating
  - Witness encryption is secure
  - User's device not compromised during encryption

## User Experience

### Core User Flows

#### 1. Create Message to Future Self

```
1. User signs up / logs in
   - Creates Primary ID (or connects existing)
   - Registers contact methods

2. Compose message
   - Rich text editor with media attachments
   - Select delivery date/time
   - Preview message

3. Review settings
   - See calculated Bitcoin block height
   - Review contact methods for delivery
   - Set message priority

4. Confirm and encrypt
   - Message encrypted locally (client-side)
   - Upload to Arweave
   - Receive confirmation + message ID

5. Message "forgotten"
   - User cannot access message content
   - Can view metadata (date, exists, etc.)
   - Can cancel/delete if desired (before deadline)
```

#### 2. Update Contact Information

```
1. Login to account
2. Navigate to "Profile" → "Contact Methods"
3. Add/edit/remove contact channels
4. Verify new contact methods (email verification, SMS code)
5. Set priority order for notifications
6. Annual reminder to review contacts
```

#### 3. Receive Future Message

```
1. Deadline reached (Bitcoin block height achieved)
2. System automatically decrypts message
3. Notifications sent via all channels:
   - Email: "You have a message from your past self!"
   - SMS: Link to retrieve message
   - Push notification (if app installed)

4. User accesses message:
   - Click link in notification, or
   - Login to web portal

5. View message
   - Read content
   - View attached media
   - See metadata (when written, from when)
   - Option to reply to past self (create new future message)
```

### User Interface Mockups (Conceptual)

**Home Screen**:
- "Send a message to your future self"
- Timeline view of pending messages (metadata only, not content)
- Received messages inbox

**Message Composer**:
- Clean, distraction-free writing interface
- Date picker with visual timeline
- Bitcoin block height calculator (educational)
- Estimated delivery window
- File/media attachments

**Profile/Settings**:
- Primary ID display
- Contact method management
- Message history (sent/received)
- Security settings
- Subscription/payment

## Business Model

### Revenue Streams

1. **Freemium Model**:
   - Free tier: 3 messages/year, up to 1MB each, max 5 years in future
   - Premium: Unlimited messages, larger files, longer durations, priority support

2. **Pricing Tiers**:
   - **Free**: $0/year - Basic features
   - **Personal**: $5/year - 50 messages/year, 10MB each, 10 years max
   - **Legacy**: $50/year - Unlimited, 100MB each, 50+ years, family features
   - **Enterprise**: Custom - Business continuity, team features, API access

3. **One-time Permanent Messages**:
   - Pay-per-message for very long durations (20+ years)
   - Covers Arweave storage costs + margin
   - Estimated $1-5 per message depending on size

4. **B2B Services**:
   - Time-capsule services for businesses
   - Legal document delivery (wills, trusts)
   - Corporate archives
   - API access for developers

### Cost Structure

**Storage Costs**:
- Arweave: ~$25/GB one-time (amortized across users)
- Average message size: 100KB (text)
- Storage cost per message: ~$0.0025

**Infrastructure**:
- Blockchain monitoring service
- Notification delivery (email/SMS)
- Web hosting and CDN
- Customer support

**Development**:
- Cryptographic library maintenance
- Protocol upgrades
- Security audits

## Go-to-Market Strategy

### Target Audiences

1. **Primary**: Individuals for personal reflection
   - Life transitions (graduation, marriage, career change)
   - Parents to children (messages for 18th birthday)
   - Personal growth and goal tracking

2. **Secondary**: Businesses and professionals
   - Legal professionals (estate planning)
   - Business continuity planning
   - Time-release contracts
   - Corporate time capsules

3. **Niche**: Cryptocurrency enthusiasts
   - Already understand blockchain concepts
   - Early adopters of cryptographic tech
   - Appreciate the Bitcoin-based innovation

### Launch Plan

**Phase 1: MVP (Months 1-6)**
- Core time-lock encryption (Bitcoin + witness encryption)
- Basic web interface
- Email notifications only
- Arweave storage integration
- Beta testing with 100 users

**Phase 2: Public Launch (Months 6-12)**
- Full multi-channel notifications
- Mobile PWA
- Identity registry with multiple contacts
- Free tier + premium subscriptions
- Marketing campaign targeting personal development community

**Phase 3: Scale (Year 2)**
- Enterprise features
- API for developers
- Partnerships (legal services, life coaching)
- International expansion
- Time-lock puzzle fallback implementation

**Phase 4: Long-term (Year 3+)**
- Advanced cryptographic features
- Integration with other blockchains
- Family/group features
- Physical mail integration for very long delays

## Technical Challenges and Mitigations

### Challenge 1: Witness Encryption Maturity

**Issue**: Witness encryption is still an active research area, practical implementations are experimental.

**Mitigations**:
- Partner with academic cryptography labs
- Implement using SNARKs (more mature)
- Have time-lock puzzle as proven fallback
- Regular security audits by experts
- Open-source implementation for peer review
- Gradual rollout: Start with shorter time locks (< 1 year)

### Challenge 2: Bitcoin Blockchain Continuity

**Issue**: System depends on Bitcoin blockchain continuing to operate decades into future.

**Mitigations**:
- Bitcoin is most secure blockchain (highest probability of longevity)
- 15+ years of proven operation
- Massive economic incentives to maintain
- Fallback: Allow re-encryption to alternative clock if Bitcoin fails
- Multi-blockchain support (Ethereum, other PoW chains)
- Time-lock puzzle fallback doesn't depend on blockchain

### Challenge 3: Storage Persistence

**Issue**: Even "permanent" storage services may fail over decades.

**Mitigations**:
- Multi-provider redundancy (Arweave + Filecoin + IPFS)
- Self-custody option (users can download encrypted message)
- Regular replication checks
- Economic analysis: Arweave's endowment model designed for 200+ years
- Open-source tools to migrate to new storage if needed

### Challenge 4: Contact Information Currency

**Issue**: Email/phone may become obsolete over decades.

**Mitigations**:
- Multiple contact methods
- Regular "proof of life" checks
- Trusted contact network (family can update info)
- Blockchain addresses (more persistent)
- Physical mail as last resort
- Message always accessible via Primary ID at web portal

### Challenge 5: Regulatory Compliance

**Issue**: Different jurisdictions may regulate encrypted communications, financial services, or data storage.

**Mitigations**:
- Legal structure as non-profit foundation (neutral)
- Compliance with data protection (GDPR, CCPA)
- No custody of funds (not a financial service)
- Terms of service clearly outline use cases
- Prohibit illegal content (encrypted, but policies still apply)
- Transparency reports

### Challenge 6: User Death or Incapacity

**Issue**: User may die before receiving long-term messages.

**Mitigations**:
- Emergency contact designation
- Legacy features: Messages transferred to heirs
- "Dead man's switch" integration
- Legal framework for digital inheritance
- Optional: Life insurance/proof of life integrations

## Success Metrics

### Key Performance Indicators (KPIs)

**User Acquisition**:
- Monthly active users (MAU)
- Conversion rate (free → paid)
- User acquisition cost (CAC)
- Viral coefficient (referrals)

**Engagement**:
- Messages sent per user per year
- Average time to future (days/years)
- Message retrieval rate (% delivered and opened)
- Return users (create multiple messages)

**Technical Performance**:
- Decryption success rate (% of messages successfully unlocked at deadline)
- Notification delivery rate (% reaching user)
- Storage integrity (% of messages verified intact)
- System uptime (99.9% target)

**Business**:
- Monthly recurring revenue (MRR)
- Customer lifetime value (LTV)
- LTV:CAC ratio (target > 3:1)
- Churn rate

**Long-term**:
- Oldest message successfully delivered
- Messages pending for 5+ years
- User testimonials and stories

## Competitive Analysis

### Direct Competitors

1. **FutureMe.org**
   - Established (2002), large user base
   - Simple email-based delivery
   - **Weakness**: No cryptographic time-locking, messages stored in plaintext
   - **Weakness**: Email-only delivery, no update mechanism for changed emails

2. **Letter to Future Self (various apps)**
   - Mobile apps, basic functionality
   - **Weakness**: Company-dependent, may shut down
   - **Weakness**: No cryptographic guarantees

3. **Traditional Time Capsules**
   - Physical, proven longevity
   - **Weakness**: Physical location required, not scalable
   - **Weakness**: No digital content

### Unique Value Propositions

**Tresor's Differentiators**:
1. **Cryptographic Guarantee**: Messages provably cannot be read early (vs. access control)
2. **Decentralized**: No single company dependency (vs. centralized services)
3. **Long-term Delivery**: Multi-channel, updateable contacts (vs. single email)
4. **Research-backed**: Academic cryptography, not just engineering (vs. ad-hoc solutions)
5. **Permanent Storage**: Blockchain-based persistence (vs. traditional databases)
6. **Open Source**: Community can maintain even if company disappears

## Roadmap

### Short-term (Months 1-6)

- [ ] Form technical advisory board (cryptographers, blockchain experts)
- [ ] Implement proof-of-concept witness encryption with Bitcoin
- [ ] Design and build MVP web interface
- [ ] Integrate Arweave storage
- [ ] Develop blockchain monitoring service
- [ ] Security audit (internal)
- [ ] Closed beta with 50-100 users
- [ ] Iterate based on feedback

### Medium-term (Months 6-18)

- [ ] Public launch (open beta)
- [ ] Implement time-lock puzzle fallback
- [ ] Multi-channel notification system
- [ ] Mobile PWA
- [ ] Identity registry v1
- [ ] Premium subscription launch
- [ ] External security audit
- [ ] Marketing campaign
- [ ] Reach 10,000 users

### Long-term (18+ months)

- [ ] Enterprise features and API
- [ ] International expansion
- [ ] Physical mail integration
- [ ] Family/group features
- [ ] Alternative blockchain support
- [ ] Non-profit foundation establishment
- [ ] Open-source community development
- [ ] Reach 100,000 users
- [ ] First messages delivered after 5+ years

## Validation of Bitcoin-based Approach

### Academic Validation

**Research Quality**: The Bitcoin + witness encryption approach is published in peer-reviewed cryptography journals (Designs, Codes and Cryptography, IACR) and is not just a blog post or whitepaper.

**Authors**: Jia Liu (University of Birmingham), Flavio Garcia (University of Birmingham), Mark Ryan (University of Birmingham) - respected academics in cryptography.

**Theoretical Soundness**: The construction:
- Proves security under standard cryptographic assumptions
- Uses Bitcoin blockchain as computational reference clock
- Combines with witness encryption for SAT (satisfiability problems)
- Achieves extractable security

### Practical Considerations

**Bitcoin as Clock**:
- Bitcoin block time: ~10 minutes (highly predictable over long periods)
- Difficulty adjustment ensures consistency
- Most secure blockchain (hashrate, decentralization)
- 15+ years of uninterrupted operation
- Strong economic incentives for continuation

**Witness Encryption Implementation**:
- Requires advanced cryptographic constructions (SNARKs)
- Still cutting-edge, not yet widely deployed
- Performance considerations for mobile devices
- Active research area with ongoing improvements

### Risk Assessment

**Low Risk** (6 months - 2 years):
- Bitcoin highly likely to continue
- Cryptographic assumptions well-tested
- Short time locks easier to predict

**Medium Risk** (2-10 years):
- Bitcoin still probable to continue
- Technology may improve (better witness encryption)
- User contact information main challenge

**High Risk** (10+ years):
- Bitcoin continuation highly likely but not certain
- Computing advances may weaken some assumptions
- Multiple fallback mechanisms essential
- Social/regulatory changes unpredictable

**Recommended Approach**:
- Use Bitcoin-based time-locking for messages up to 10 years
- Combine with time-lock puzzles for redundancy
- For messages beyond 10 years, use hybrid approach
- Allow users to "refresh" very long-term messages periodically

## Ethical Considerations

### User Wellbeing

**Potential Positive Impacts**:
- Personal growth and reflection
- Emotional connections across time
- Goal tracking and accountability
- Therapeutic applications (mental health)
- Family legacy and storytelling

**Potential Negative Impacts**:
- Anxiety about future messages
- Regret about past messages (cannot unsend after encryption)
- Grief if user dies before receiving message
- Relationship issues (messages from ex-partners, etc.)

**Mitigations**:
- Clear UI warnings before sending
- Cooling-off period (24 hours before encryption)
- Ability to cancel message before deadline
- Mental health resources in app
- Content warnings for potentially distressing messages

### Privacy and Security

**Privacy Commitments**:
- End-to-end encryption (we cannot read messages)
- Metadata minimization
- No selling of user data
- Transparent data practices
- User owns their data (can export/delete)

**Security Responsibilities**:
- Regular security audits
- Responsible disclosure program
- Incident response plan
- Clear communication about limitations
- No false promises about absolute security

### Content Moderation

**Challenge**: Messages are encrypted, so content moderation is impossible until decryption.

**Approach**:
- Terms of service prohibit illegal content
- Report mechanism for received messages
- Law enforcement cooperation (for court orders)
- No proactive scanning (would break time-locking)
- Clear that service is not liable for user content

## Conclusion

Tresor addresses the two fundamental challenges of future self-messaging through:

1. **Cryptographic Time-Locking**: Using Bitcoin blockchain + witness encryption (backed by academic research), we can provably ensure messages remain unreadable until a specified future date. This is not merely access control, but mathematical certainty.

2. **Decentralized Long-term Delivery**: Through permanent storage (Arweave), multi-channel notifications, updateable identity registries, and open-source infrastructure, we can maximize the probability of successful delivery even decades into the future, despite technological and personal changes.

The combination of cutting-edge cryptography with pragmatic delivery mechanisms creates a unique product that can genuinely deliver on the promise of "messages to your future self."

**Next Steps**:
1. Form technical advisory board
2. Validate witness encryption implementation feasibility
3. Develop MVP with 6-month max time locks
4. Conduct closed beta
5. Iterate and scale

**Open Questions for Discussion**:
1. Should we start with simpler time-lock puzzles before implementing witness encryption?
2. What's the right balance between features and simplicity for MVP?
3. How do we educate users about the cryptographic guarantees without overwhelming them?
4. Should we target individuals or businesses first?
5. What's the right pricing model for long-term sustainability?
6. Should we establish as for-profit or non-profit from the beginning?
