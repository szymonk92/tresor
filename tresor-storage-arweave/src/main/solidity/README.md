# Tresor Smart Contracts

This directory contains Solidity smart contracts for decentralized time-lock encryption.

## Contracts

### TimeLockSecretHolder.sol

Main contract for storing time-locked secret shares on Ethereum, Arbitrum, Polygon, or other EVM-compatible chains.

**Features:**
- Store encrypted secret shares locked until specific block height
- Retrieve shares after unlock time
- Query unlock status
- Update unlock time (before current unlock)
- Emergency cancel (before unlock)
- Event emissions for monitoring

**Gas Costs (estimated on Ethereum mainnet):**

| Operation | Gas Used | Cost @ 30 gwei | Cost @ 50 gwei |
|-----------|----------|----------------|----------------|
| lockShare (1 KB) | ~100,000 | ~$9 | ~$15 |
| retrieveShare | ~35,000 | ~$3 | ~$5 |
| isUnlocked | ~5,000 | ~$0.50 | ~$0.80 |
| getUnlockStatus | ~8,000 | ~$0.75 | ~$1.25 |

**Deployment:**

```bash
# Using Hardhat
npx hardhat compile
npx hardhat deploy --network arbitrum

# Using Foundry
forge build
forge create --rpc-url $RPC_URL --private-key $PRIVATE_KEY \
    src/TimeLockSecretHolder.sol:TimeLockSecretHolder
```

## Development

### Compilation

```bash
# Install dependencies
npm install --save-dev hardhat @nomicfoundation/hardhat-toolbox

# Compile
npx hardhat compile
```

### Testing

```bash
# Run tests
npx hardhat test

# With coverage
npx hardhat coverage
```

### Deployment Networks

**Mainnet:**
- Ethereum (high cost, high security)
- Arbitrum (low cost, fast)
- Polygon (very low cost)

**Testnet:**
- Sepolia (Ethereum testnet)
- Arbitrum Sepolia
- Polygon Mumbai

## Integration with Tresor

The smart contract is used by `SecretHolderService` to:

1. **Deploy shares**: Call `lockShare()` with encrypted share data
2. **Monitor status**: Poll `isUnlocked()` at regular intervals
3. **Retrieve shares**: Call `retrieveShare()` when unlocked
4. **Reconstruct key**: Combine with other shares using Shamir's Secret Sharing

**Example flow:**

```java
// 1. Split AES key into shares
List<Share> shares = shamirSplitting.split(aesKey, 5, 3);

// 2. Deploy to Ethereum
Web3j web3 = Web3j.build(new HttpService("https://mainnet.infura.io/..."));
TimeLockSecretHolder contract = TimeLockSecretHolder.load(contractAddress, web3, ...);

byte[] encryptedShare = shares.get(0).toBytes();
bytes32 secretId = keccak256(messageId + shareNumber);
uint256 unlockBlock = calculateTargetBlock(unlockDate);

contract.lockShare(secretId, encryptedShare, unlockBlock).send();

// 3. Later: Check if unlocked
boolean unlocked = contract.isUnlocked(secretId).send();

// 4. Retrieve share
if (unlocked) {
    byte[] retrievedShare = contract.retrieveShare(secretId).send();
    // Combine with other shares to reconstruct key
}
```

## Security Considerations

1. **Immutability**: Once deployed, shares cannot be modified (only metadata like unlock time)
2. **Access control**: Only owner (msg.sender) can retrieve their shares
3. **Time-lock enforcement**: Smart contract prevents early retrieval
4. **No private data on-chain**: Shares are encrypted before storage
5. **Gas limits**: Shares limited to 10 KB to prevent gas issues

## License

MIT License - See LICENSE file for details.
