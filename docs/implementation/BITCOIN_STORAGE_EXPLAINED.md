# Bitcoin Storage Explained: How Tresor Uses Bitcoin

## What We Store in Bitcoin

### NOT Stored: The Entire Message ❌

```
❌ WRONG: Store your 10 MB video on Bitcoin
   - Bitcoin blocks are ~4 MB max
   - Would cost thousands of dollars
   - Not designed for data storage
```

### What We ACTUALLY Store: One Small Share ✅

```
✅ CORRECT: Store ONE secret share (~256 bytes)

Example:
Message: "Happy 18th birthday, son!" + 10 photos (10 MB total)
   ↓
Encrypt with AES-256 key (32 bytes)
   ↓
Message ciphertext → Arweave ($0.50)
   ↓
AES key → Split into 5 shares (Shamir)
   ↓
Share 1 (256 bytes) → Bitcoin ($7)
Share 2 (256 bytes) → Ethereum ($20)
Share 3 (256 bytes) → Arbitrum ($0.50)
Share 4 (256 bytes) → You keep it (free)
Share 5 (256 bytes) → Arweave backup ($0.04)
```

**What's in a share:** Just a big number (the Shamir share)
```
Share 1: x=1, y=123456789...987654321
  - x coordinate: 4 bytes
  - y coordinate: ~252 bytes
  - Total: ~256 bytes
```

---

## How Bitcoin Time-Lock Works (CLTV)

### The Bitcoin Transaction Structure

```
Bitcoin Transaction:
┌─────────────────────────────────────────────────┐
│ Input: Your bitcoins (e.g., 0.001 BTC = $70)  │
│                                                 │
│ Output 1: Time-locked script                   │
│   ├─ Value: 0.0001 BTC (~$7)                  │
│   ├─ Script: CLTV + data                       │
│   └─ Unlock: Block 920,000                     │
│                                                 │
│ Output 2: Change back to you                   │
│   └─ Value: 0.0009 BTC (~$63)                 │
│                                                 │
│ Fee: ~0.00001 BTC (~$0.70)                     │
└─────────────────────────────────────────────────┘

Total cost: ~$7 (fee + small UTXO)
```

### CLTV Script (CheckLockTimeVerify)

This is actual Bitcoin script language:

```bitcoin
OP_IF
  <unlock_block_height> OP_CHECKLOCKTIMEVERIFY OP_DROP
  <your_public_key> OP_CHECKSIG
OP_ELSE
  <share_data_hash> OP_EQUAL
OP_ENDIF
```

**What this means:**
1. **IF** current block >= 920,000:
   - You can claim the bitcoin back
   - The share data is now publicly readable
2. **ELSE**:
   - Share is locked
   - Nobody can access it

### Where the Share Data Lives

**Option A: OP_RETURN (most common)**

```
OP_RETURN <hex_encoded_share_data>
```

- Max size: 80 bytes per output (Bitcoin limit)
- For 256-byte share: Need 4 transactions OR compress data
- Cost: ~$7 per transaction
- **Immutable:** Once on blockchain, permanent

**Option B: P2SH (Pay-to-Script-Hash)**

```
Redeem script contains:
  - Share data hash
  - Time-lock condition
  - Unlock logic
```

- Can store hash, actual data stored off-chain (e.g., IPFS)
- Retrieve redeem script at unlock time
- More complex but cheaper

**Option C: Taproot (modern, best for privacy)**

```
Taproot script:
  - Looks like normal transaction
  - Share data in witness data
  - Time-lock in script path
```

- Max 400 KB witness data (plenty for shares)
- Better privacy (looks like regular transaction)
- Cheaper fees (~$3-5)

---

## Why $7 Cost?

### Cost Breakdown (2025 estimate)

```
Bitcoin transaction fee:
  - Size: ~250 bytes (typical)
  - Fee rate: 20 sat/vbyte (median)
  - Total: 250 × 20 = 5,000 satoshis
  - In USD: ~$3.50

Dust limit (minimum UTXO):
  - Must lock at least ~546 sats in output
  - Otherwise miners reject it ("dust")
  - In USD: ~$0.40

OP_RETURN data:
  - Extra size for 80 bytes data
  - Additional 80 vbytes
  - Cost: 80 × 20 = 1,600 sats = ~$1.10

Emergency fund (you get back):
  - Small amount locked in time-lock output
  - Reclaimable after unlock
  - ~5,000 sats = ~$3.50 (you get this back!)

TOTAL: ~$3.50 + $0.40 + $1.10 + $3.50 = $8.50
USER PAYS: ~$5.00 (rest is recoverable)
```

**Why I said ~$7:**
- Fees fluctuate (could be $3-$20 depending on network)
- Conservative estimate
- Covers worst-case gas spikes

---

## How Much Can We Store?

### Bitcoin Limitations

| Method | Max Size | Cost (estimate) | Retrievability |
|--------|----------|-----------------|----------------|
| **OP_RETURN** | 80 bytes | $3-5 | Easy (in transaction) |
| **Multiple OP_RETURN** | 320 bytes (4×) | $12-20 | Medium (4 TXs) |
| **P2SH redeem script** | ~520 bytes | $5-10 | Medium (need redeem script) |
| **Taproot witness** | 400 KB | $10-50 | Easy (in witness data) |

**For Tresor (256-byte shares):**
- **Best option:** Taproot witness
- **Cost:** ~$5-10
- **Storage:** 256 bytes fits easily
- **Time-lock:** Yes (via script path)

---

## Example: Real Bitcoin Transaction

### Creating a Time-Locked Share

```javascript
// Using BitcoinJS library

const bitcoin = require('bitcoinjs-lib');

// 1. Create time-lock script
const script = bitcoin.script.compile([
  bitcoin.opcodes.OP_IF,
    bitcoin.script.number.encode(920000), // Block height
    bitcoin.opcodes.OP_CHECKLOCKTIMEVERIFY,
    bitcoin.opcodes.OP_DROP,
    Buffer.from(userPublicKey, 'hex'),
    bitcoin.opcodes.OP_CHECKSIG,
  bitcoin.opcodes.OP_ELSE,
    Buffer.from(shareData, 'hex'), // The actual share
    bitcoin.opcodes.OP_DROP,
    bitcoin.opcodes.OP_TRUE,
  bitcoin.opcodes.OP_ENDIF
]);

// 2. Create P2WSH (Taproot) address
const p2wsh = bitcoin.payments.p2wsh({
  redeem: { output: script },
  network: bitcoin.networks.bitcoin
});

// 3. Create transaction
const tx = new bitcoin.TransactionBuilder();
tx.addInput(previousTxHash, 0); // Your coins
tx.addOutput(p2wsh.address, 10000); // Lock 10,000 sats
tx.sign(0, keyPair);

// 4. Broadcast
await broadcastTransaction(tx.build().toHex());
```

### Retrieving After Unlock (10 years later)

```javascript
// Check current block height
const currentBlock = await bitcoinClient.getBlockCount();

if (currentBlock >= 920000) {
  // Unlocked! Retrieve share data

  // Method 1: Read from transaction
  const tx = await bitcoinClient.getRawTransaction(txId, true);
  const shareData = extractShareFromScript(tx.vout[0].scriptPubKey);

  // Method 2: If in witness data
  const witnessData = tx.vin[0].txinwitness;
  const shareData = witnessData[1]; // Share is in witness

  console.log('Share retrieved:', shareData);
  return shareData;
}
```

---

## Cost Per Message vs Per Key

### Pricing Model

```
ONE MESSAGE:
  ├─ Message content (10 MB) → Arweave: $0.50
  ├─ AES key (32 bytes) split into 5 shares:
  │   ├─ Share 1 → Bitcoin: $7.00
  │   ├─ Share 2 → Ethereum: $20.00 (or skip)
  │   ├─ Share 3 → Arbitrum: $0.50
  │   ├─ Share 4 → Self-custody: FREE
  │   └─ Share 5 → Arweave: $0.04
  └─ TOTAL: $28.04 (with Ethereum) or $8.04 (without)

Cost breakdown:
  - Per message: $8-28 (one-time)
  - Per key: N/A (1 key per message)
  - Per share: ~$0-20 depending on blockchain
  - Per retrieval: $0 (reading blockchain is free)
```

**Recommended default:**
```
Bitcoin ($7) + Arbitrum ($0.50) + Self-custody (free) = $7.50 per message
```

---

## Storage Math

### Comparison: Bitcoin vs Other Solutions

```
BITCOIN:
  - Storage: 256 bytes per share
  - Cost: $7 one-time
  - Duration: Forever (blockchain is permanent)
  - $/byte/year: $7 ÷ 256 ÷ 50 years = $0.00055/byte/year

ARWEAVE:
  - Storage: 10 MB message
  - Cost: $0.50 one-time
  - Duration: 200+ years (permanent)
  - $/byte/year: $0.50 ÷ 10,000,000 ÷ 200 = $0.00000000025/byte/year

AWS S3:
  - Storage: 10 MB
  - Cost: $0.023/month = $0.28/year
  - Duration: As long as you pay
  - $/byte/year: $0.28 ÷ 10,000,000 = $0.000000028/byte/year

CONCLUSION:
  - Bitcoin: Expensive per byte, but PERMANENT and DECENTRALIZED
  - Arweave: Cheapest for large files + permanent
  - AWS: Cheapest short-term, but requires ongoing payment
```

---

## Why Not Store Everything on Bitcoin?

### The Problem

```
Example message: 10 MB

If we stored entire message on Bitcoin:
  - 10 MB = 10,000,000 bytes
  - Bitcoin fee: ~20 sat/vbyte
  - Cost: 10,000,000 × 20 = 200,000,000 sats
  - In USD: ~$140,000 💸💸💸

Plus:
  - Would fill multiple blocks
  - Miners would reject it
  - Takes hours to confirm
  - Permanent blockchain bloat
```

### Our Solution

```
Store SMALL share on Bitcoin (256 bytes):
  - 256 bytes × 20 sat/vbyte = 5,120 sats
  - In USD: ~$3.50 ✅

Store LARGE message on Arweave (10 MB):
  - 10 MB for $0.50 ✅

TOTAL: $4.00 instead of $140,000
SAVINGS: 99.997%
```

---

## How Retrieval Works

### Timeline

```
2025: Create message
  ├─ Encrypt with AES key
  ├─ Upload ciphertext to Arweave
  ├─ Split key into 5 shares
  ├─ Deploy Share 1 to Bitcoin block X
  ├─ Bitcoin TX confirmed in ~10 minutes
  └─ Share is now locked until block 920,000

... 10 YEARS PASS ...

2035: Bitcoin reaches block 920,000
  ├─ Tresor cron job checks: "Is 920,000 reached?"
  ├─ YES! Retrieve share from transaction:
  │   - Query Bitcoin node: getTransaction(txId)
  │   - Extract share from OP_RETURN or witness
  │   - Takes ~2 seconds
  ├─ Retrieve 2 more shares (Arbitrum, Self-custody)
  ├─ Reconstruct AES key (Shamir)
  ├─ Download ciphertext from Arweave
  ├─ Decrypt message
  └─ Deliver to user

Total unlock time: ~5 seconds
```

---

## Key Insights

### 1. **One Share Per Message (Not Entire Message)**
```
Message → 1 AES key → 5 shares → 1 share to Bitcoin
```

### 2. **$7 is One-Time (Not Recurring)**
```
Pay once: $7
Wait: 10 years
Retrieve: FREE
```

### 3. **Bitcoin Stores ~256 Bytes (Not Megabytes)**
```
Share size: 256 bytes (fits in OP_RETURN + extra TX)
Message size: Unlimited (stored on Arweave)
```

### 4. **Cost Scales with NUMBER of Messages (Not Size)**
```
1 message (1 KB): $7
1 message (10 MB): $7 (same!)
100 messages (1 KB each): $700 (100 × $7)
```

### 5. **You Can Reduce Bitcoin Costs**
```
Skip Bitcoin → Use only Arbitrum + Polygon: $0.51
Trade-off: Less decentralization, but still works
```

---

## Alternative: Skip Bitcoin Entirely?

### Option 1: Bitcoin + Arbitrum + Self-custody ($7.50)
✅ Maximum decentralization
✅ Bitcoin's 50-year track record
❌ Higher cost

### Option 2: Ethereum + Arbitrum + Self-custody ($20.50)
✅ Smart contract flexibility
✅ Proven ecosystem
❌ Expensive

### Option 3: Arbitrum + Polygon + Self-custody ($0.51)
✅ Very cheap
✅ Still decentralized (L2s)
❌ Less proven long-term

### Option 4: Self-custody + Arweave + Email ($0.54)
✅ Cheapest
✅ Still works
❌ Lower redundancy

**My recommendation:** Bitcoin + Arbitrum + Self-custody = $7.50
- Good balance of cost vs. reliability
- Bitcoin's longevity is worth the premium for 50-year product

---

## Summary

**Q: What do we store on Bitcoin?**
A: ONE secret share (256 bytes), not the whole message

**Q: Why $7?**
A: Transaction fee ($3.50) + OP_RETURN data ($1) + dust limit ($0.40) + buffer

**Q: Per message or per key?**
A: Per message (1 message = 1 key = 5 shares = 1 share on Bitcoin)

**Q: How much can we store?**
A: Up to 400 KB in Taproot, but we only need 256 bytes

**Q: Why not store everything on Bitcoin?**
A: Would cost $140,000 for 10 MB vs. $7.50 using our approach

**Q: Can we make it cheaper?**
A: Yes! Use Arbitrum + Polygon instead of Bitcoin ($0.51 total)
