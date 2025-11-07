# Smart Contract Tests

Comprehensive test suite for Tresor time-lock smart contracts.

## Running Tests

### Install Dependencies

```bash
cd tresor-storage-arweave/src/test/solidity
npm install
```

### Run Tests

```bash
# Run all tests
npm test

# Run with gas reporting
npm run test:gas

# Run with coverage report
npm run coverage

# Run specific test file
npx hardhat test TimeLockSecretHolder.test.js
```

## Test Coverage

The test suite covers:

1. **Deployment**
   - Initial state verification

2. **lockShare function**
   - ✅ Successful share locking
   - ✅ Reject duplicate IDs
   - ✅ Reject past unlock blocks
   - ✅ Reject empty data
   - ✅ Reject oversized shares (>10 KB)
   - ✅ Owner tracking

3. **retrieveShare function**
   - ✅ Successful retrieval after unlock
   - ✅ Reject early retrieval
   - ✅ Reject non-owner retrieval
   - ✅ Reject non-existent secrets

4. **isUnlocked function**
   - ✅ False before unlock
   - ✅ True after unlock

5. **getUnlockStatus function**
   - ✅ Correct status before unlock
   - ✅ Correct status after unlock
   - ✅ Blocks remaining calculation

6. **markRetrieved function**
   - ✅ Successful marking
   - ✅ Reject before unlock
   - ✅ Reject duplicate marking

7. **updateUnlockTime function**
   - ✅ Successful update
   - ✅ Reject after unlock
   - ✅ Reject past blocks
   - ✅ Reject non-owner updates

8. **emergencyCancel function**
   - ✅ Successful cancellation
   - ✅ Reject after unlock
   - ✅ Reject non-owner cancellation

9. **estimateUnlockTime function**
   - ✅ Correct time estimation
   - ✅ Current time if unlocked

10. **Multiple shares**
    - ✅ Handle 5 shares from same owner
    - ✅ Retrieve threshold (3-of-5)
    - ⚠️ **Long-running test** (annotated with timeout)

11. **Gas optimization**
    - ⛽ Gas usage reporting for all operations

## Test Annotations

Tests that may take longer than usual are annotated with:

```javascript
it("Should handle multiple shares from same owner", async function () {
    this.timeout(10000); // 10 second timeout
    // ... test code
});
```

## Gas Reporting

To see gas usage for each operation:

```bash
REPORT_GAS=true npm test
```

Sample output:
```
⛽ lockShare gas used: ~100,000
⛽ markRetrieved gas used: ~35,000
```

## Continuous Integration

These tests are designed to run in CI/CD pipelines:

```yaml
# .github/workflows/test.yml
- name: Install dependencies
  run: cd tresor-storage-arweave/src/test/solidity && npm install

- name: Run smart contract tests
  run: cd tresor-storage-arweave/src/test/solidity && npm test

- name: Check coverage
  run: cd tresor-storage-arweave/src/test/solidity && npm run coverage
```

## Test Networks

Tests run on Hardhat's built-in network by default (fast, no real ETH needed).

To test on actual networks:

```bash
# Sepolia testnet
npx hardhat test --network sepolia

# Arbitrum Sepolia
npx hardhat test --network arbitrumSepolia
```

Configure network RPC URLs in `hardhat.config.js` or use environment variables:

```bash
export SEPOLIA_RPC_URL="https://sepolia.infura.io/v3/YOUR_KEY"
export PRIVATE_KEY="your_private_key"
```

## Expected Test Duration

| Test Suite | Duration | Notes |
|------------|----------|-------|
| Full suite | ~30-60s | Local Hardhat network |
| Single test | ~2-5s | Individual test case |
| Coverage | ~2-3min | Includes instrumentation |
| Testnet | ~5-10min | Real network latency |

## Troubleshooting

**Tests timing out:**
```javascript
// Increase timeout for specific test
this.timeout(20000); // 20 seconds
```

**Out of gas errors:**
```javascript
// Check hardhat.config.js gas limits
blockGasLimit: 30000000
```

**Network errors:**
```bash
# Reset Hardhat network
npx hardhat clean
npx hardhat compile
```
