// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title TimeLockSecretHolder
 * @dev Smart contract for storing time-locked secret shares.
 *
 * This contract enables decentralized time-lock encryption by:
 * 1. Storing encrypted secret shares
 * 2. Locking them until a specific block number
 * 3. Allowing retrieval after unlock time
 * 4. Providing verifiable unlock status
 *
 * Use case: Part of Shamir's Secret Sharing scheme for 50-year messages
 */
contract TimeLockSecretHolder {

    // ========================================
    // STRUCTS
    // ========================================

    struct LockedSecret {
        bytes32 secretId;           // Unique identifier for the secret
        bytes encryptedShare;       // The encrypted share data
        uint256 unlockBlock;        // Block number when share unlocks
        address owner;              // Who can claim the share
        uint256 createdAt;          // Block number when created
        bool retrieved;             // Whether share has been claimed
    }

    // ========================================
    // STATE VARIABLES
    // ========================================

    // secretId => LockedSecret
    mapping(bytes32 => LockedSecret) public secrets;

    // owner => array of secretIds
    mapping(address => bytes32[]) public ownerSecrets;

    // Total number of secrets stored
    uint256 public totalSecrets;

    // ========================================
    // EVENTS
    // ========================================

    event SecretLocked(
        bytes32 indexed secretId,
        address indexed owner,
        uint256 unlockBlock,
        uint256 shareSize
    );

    event SecretRetrieved(
        bytes32 indexed secretId,
        address indexed owner,
        uint256 blockNumber
    );

    event SecretUpdated(
        bytes32 indexed secretId,
        uint256 newUnlockBlock
    );

    // ========================================
    // MODIFIERS
    // ========================================

    modifier onlyOwner(bytes32 secretId) {
        require(
            secrets[secretId].owner == msg.sender,
            "Not authorized: only owner can access"
        );
        _;
    }

    modifier secretExists(bytes32 secretId) {
        require(
            secrets[secretId].owner != address(0),
            "Secret does not exist"
        );
        _;
    }

    modifier notYetRetrieved(bytes32 secretId) {
        require(
            !secrets[secretId].retrieved,
            "Secret already retrieved"
        );
        _;
    }

    // ========================================
    // PUBLIC FUNCTIONS
    // ========================================

    /**
     * @dev Store a time-locked secret share.
     * @param secretId Unique identifier (hash of message ID + share number)
     * @param encryptedShare The encrypted share data
     * @param unlockBlock Block number when share becomes available
     */
    function lockShare(
        bytes32 secretId,
        bytes memory encryptedShare,
        uint256 unlockBlock
    ) external payable {
        require(
            secrets[secretId].owner == address(0),
            "Secret ID already exists"
        );
        require(
            unlockBlock > block.number,
            "Unlock block must be in the future"
        );
        require(
            encryptedShare.length > 0,
            "Share data cannot be empty"
        );
        require(
            encryptedShare.length <= 10240, // 10 KB max
            "Share too large (max 10 KB)"
        );

        // Store the secret
        secrets[secretId] = LockedSecret({
            secretId: secretId,
            encryptedShare: encryptedShare,
            unlockBlock: unlockBlock,
            owner: msg.sender,
            createdAt: block.number,
            retrieved: false
        });

        // Track owner's secrets
        ownerSecrets[msg.sender].push(secretId);

        totalSecrets++;

        emit SecretLocked(
            secretId,
            msg.sender,
            unlockBlock,
            encryptedShare.length
        );
    }

    /**
     * @dev Retrieve a time-locked share after unlock time.
     * @param secretId The secret identifier
     * @return The encrypted share data
     */
    function retrieveShare(bytes32 secretId)
        external
        view
        secretExists(secretId)
        onlyOwner(secretId)
        returns (bytes memory)
    {
        LockedSecret memory secret = secrets[secretId];

        require(
            block.number >= secret.unlockBlock,
            "Not yet unlocked"
        );

        return secret.encryptedShare;
    }

    /**
     * @dev Mark a share as retrieved (for tracking).
     * @param secretId The secret identifier
     */
    function markRetrieved(bytes32 secretId)
        external
        secretExists(secretId)
        onlyOwner(secretId)
        notYetRetrieved(secretId)
    {
        require(
            block.number >= secrets[secretId].unlockBlock,
            "Not yet unlocked"
        );

        secrets[secretId].retrieved = true;

        emit SecretRetrieved(secretId, msg.sender, block.number);
    }

    /**
     * @dev Check if a secret is unlocked.
     * @param secretId The secret identifier
     * @return Whether the secret can be retrieved
     */
    function isUnlocked(bytes32 secretId)
        external
        view
        secretExists(secretId)
        returns (bool)
    {
        return block.number >= secrets[secretId].unlockBlock;
    }

    /**
     * @dev Get unlock status and metadata.
     * @param secretId The secret identifier
     * @return unlocked Whether share is available
     * @return blocksRemaining Blocks until unlock (0 if already unlocked)
     * @return shareSize Size of encrypted share
     * @return retrieved Whether share has been claimed
     */
    function getUnlockStatus(bytes32 secretId)
        external
        view
        secretExists(secretId)
        returns (
            bool unlocked,
            uint256 blocksRemaining,
            uint256 shareSize,
            bool retrieved
        )
    {
        LockedSecret memory secret = secrets[secretId];

        unlocked = block.number >= secret.unlockBlock;
        blocksRemaining = unlocked ? 0 : secret.unlockBlock - block.number;
        shareSize = secret.encryptedShare.length;
        retrieved = secret.retrieved;
    }

    /**
     * @dev Get all secret IDs for an owner.
     * @param owner The owner address
     * @return Array of secret IDs
     */
    function getOwnerSecrets(address owner)
        external
        view
        returns (bytes32[] memory)
    {
        return ownerSecrets[owner];
    }

    /**
     * @dev Update unlock time (only before current unlock time).
     * Allows extending lock duration if needed.
     * @param secretId The secret identifier
     * @param newUnlockBlock New unlock block number
     */
    function updateUnlockTime(bytes32 secretId, uint256 newUnlockBlock)
        external
        secretExists(secretId)
        onlyOwner(secretId)
        notYetRetrieved(secretId)
    {
        require(
            block.number < secrets[secretId].unlockBlock,
            "Already unlocked"
        );
        require(
            newUnlockBlock > block.number,
            "New unlock must be in future"
        );

        secrets[secretId].unlockBlock = newUnlockBlock;

        emit SecretUpdated(secretId, newUnlockBlock);
    }

    /**
     * @dev Get complete secret details (owner only).
     * @param secretId The secret identifier
     * @return The complete LockedSecret struct
     */
    function getSecretDetails(bytes32 secretId)
        external
        view
        secretExists(secretId)
        onlyOwner(secretId)
        returns (LockedSecret memory)
    {
        return secrets[secretId];
    }

    /**
     * @dev Calculate estimated unlock time based on average block time.
     * @param secretId The secret identifier
     * @param avgBlockTimeSeconds Average block time (e.g., 12 for Ethereum)
     * @return Estimated Unix timestamp of unlock
     */
    function estimateUnlockTime(bytes32 secretId, uint256 avgBlockTimeSeconds)
        external
        view
        secretExists(secretId)
        returns (uint256)
    {
        LockedSecret memory secret = secrets[secretId];

        if (block.number >= secret.unlockBlock) {
            return block.timestamp; // Already unlocked
        }

        uint256 blocksRemaining = secret.unlockBlock - block.number;
        uint256 secondsRemaining = blocksRemaining * avgBlockTimeSeconds;

        return block.timestamp + secondsRemaining;
    }

    // ========================================
    // EMERGENCY FUNCTIONS
    // ========================================

    /**
     * @dev Emergency cancel (only before unlock time).
     * Allows owner to delete secret before it unlocks.
     * @param secretId The secret identifier
     */
    function emergencyCancel(bytes32 secretId)
        external
        secretExists(secretId)
        onlyOwner(secretId)
        notYetRetrieved(secretId)
    {
        require(
            block.number < secrets[secretId].unlockBlock,
            "Already unlocked - cannot cancel"
        );

        delete secrets[secretId];
        totalSecrets--;

        // Note: We don't remove from ownerSecrets array to save gas
        // The mapping will return empty data for deleted secrets
    }
}
