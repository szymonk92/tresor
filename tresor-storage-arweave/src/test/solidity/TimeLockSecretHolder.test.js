const { expect } = require("chai");
const { ethers } = require("hardhat");
const { time } = require("@nomicfoundation/hardhat-network-helpers");

describe("TimeLockSecretHolder", function () {
    let contract;
    let owner;
    let otherUser;
    let secretId;
    let encryptedShare;
    let unlockBlock;

    beforeEach(async function () {
        // Get signers
        [owner, otherUser] = await ethers.getSigners();

        // Deploy contract
        const TimeLockSecretHolder = await ethers.getContractFactory("TimeLockSecretHolder");
        contract = await TimeLockSecretHolder.deploy();
        await contract.waitForDeployment();

        // Prepare test data
        secretId = ethers.keccak256(ethers.toUtf8Bytes("test-secret-1"));
        encryptedShare = ethers.toUtf8Bytes("encrypted-share-data-12345");

        // Set unlock block 10 blocks in the future
        const currentBlock = await ethers.provider.getBlockNumber();
        unlockBlock = currentBlock + 10;
    });

    describe("Deployment", function () {
        it("Should deploy with zero total secrets", async function () {
            expect(await contract.totalSecrets()).to.equal(0);
        });
    });

    describe("lockShare", function () {
        it("Should successfully lock a share", async function () {
            await expect(contract.lockShare(secretId, encryptedShare, unlockBlock))
                .to.emit(contract, "SecretLocked")
                .withArgs(secretId, owner.address, unlockBlock, encryptedShare.length);

            expect(await contract.totalSecrets()).to.equal(1);
        });

        it("Should reject duplicate secret IDs", async function () {
            await contract.lockShare(secretId, encryptedShare, unlockBlock);

            await expect(
                contract.lockShare(secretId, encryptedShare, unlockBlock)
            ).to.be.revertedWith("Secret ID already exists");
        });

        it("Should reject unlock block in the past", async function () {
            const pastBlock = await ethers.provider.getBlockNumber();

            await expect(
                contract.lockShare(secretId, encryptedShare, pastBlock)
            ).to.be.revertedWith("Unlock block must be in the future");
        });

        it("Should reject empty share data", async function () {
            await expect(
                contract.lockShare(secretId, "0x", unlockBlock)
            ).to.be.revertedWith("Share data cannot be empty");
        });

        it("Should reject shares larger than 10 KB", async function () {
            // Create 11 KB of data
            const largeData = new Uint8Array(11 * 1024);
            largeData.fill(0xff);

            await expect(
                contract.lockShare(secretId, largeData, unlockBlock)
            ).to.be.revertedWith("Share too large (max 10 KB)");
        });

        it("Should add secret to owner's list", async function () {
            await contract.lockShare(secretId, encryptedShare, unlockBlock);

            const ownerSecrets = await contract.getOwnerSecrets(owner.address);
            expect(ownerSecrets.length).to.equal(1);
            expect(ownerSecrets[0]).to.equal(secretId);
        });
    });

    describe("retrieveShare", function () {
        beforeEach(async function () {
            await contract.lockShare(secretId, encryptedShare, unlockBlock);
        });

        it("Should retrieve share after unlock block", async function () {
            // Mine blocks until unlock
            await mineBlocks(10);

            const retrieved = await contract.retrieveShare(secretId);
            expect(retrieved).to.equal(ethers.hexlify(encryptedShare));
        });

        it("Should reject retrieval before unlock block", async function () {
            await expect(
                contract.retrieveShare(secretId)
            ).to.be.revertedWith("Not yet unlocked");
        });

        it("Should reject retrieval by non-owner", async function () {
            await mineBlocks(10);

            await expect(
                contract.connect(otherUser).retrieveShare(secretId)
            ).to.be.revertedWith("Not authorized: only owner can access");
        });

        it("Should reject retrieval of non-existent secret", async function () {
            const fakeId = ethers.keccak256(ethers.toUtf8Bytes("fake"));

            await expect(
                contract.retrieveShare(fakeId)
            ).to.be.revertedWith("Secret does not exist");
        });
    });

    describe("isUnlocked", function () {
        beforeEach(async function () {
            await contract.lockShare(secretId, encryptedShare, unlockBlock);
        });

        it("Should return false before unlock block", async function () {
            expect(await contract.isUnlocked(secretId)).to.be.false;
        });

        it("Should return true after unlock block", async function () {
            await mineBlocks(10);
            expect(await contract.isUnlocked(secretId)).to.be.true;
        });
    });

    describe("getUnlockStatus", function () {
        beforeEach(async function () {
            await contract.lockShare(secretId, encryptedShare, unlockBlock);
        });

        it("Should return correct status before unlock", async function () {
            const status = await contract.getUnlockStatus(secretId);

            expect(status.unlocked).to.be.false;
            expect(status.blocksRemaining).to.be.greaterThan(0);
            expect(status.shareSize).to.equal(encryptedShare.length);
            expect(status.retrieved).to.be.false;
        });

        it("Should return correct status after unlock", async function () {
            await mineBlocks(10);

            const status = await contract.getUnlockStatus(secretId);

            expect(status.unlocked).to.be.true;
            expect(status.blocksRemaining).to.equal(0);
            expect(status.shareSize).to.equal(encryptedShare.length);
        });
    });

    describe("markRetrieved", function () {
        beforeEach(async function () {
            await contract.lockShare(secretId, encryptedShare, unlockBlock);
            await mineBlocks(10);
        });

        it("Should mark secret as retrieved", async function () {
            await expect(contract.markRetrieved(secretId))
                .to.emit(contract, "SecretRetrieved")
                .withArgs(secretId, owner.address, await ethers.provider.getBlockNumber() + 1);

            const status = await contract.getUnlockStatus(secretId);
            expect(status.retrieved).to.be.true;
        });

        it("Should reject marking before unlock", async function () {
            // Lock a new secret
            const newId = ethers.keccak256(ethers.toUtf8Bytes("new-secret"));
            const futureBlock = (await ethers.provider.getBlockNumber()) + 100;
            await contract.lockShare(newId, encryptedShare, futureBlock);

            await expect(
                contract.markRetrieved(newId)
            ).to.be.revertedWith("Not yet unlocked");
        });

        it("Should reject marking twice", async function () {
            await contract.markRetrieved(secretId);

            await expect(
                contract.markRetrieved(secretId)
            ).to.be.revertedWith("Secret already retrieved");
        });
    });

    describe("updateUnlockTime", function () {
        beforeEach(async function () {
            await contract.lockShare(secretId, encryptedShare, unlockBlock);
        });

        it("Should update unlock time", async function () {
            const newUnlockBlock = unlockBlock + 100;

            await expect(contract.updateUnlockTime(secretId, newUnlockBlock))
                .to.emit(contract, "SecretUpdated")
                .withArgs(secretId, newUnlockBlock);

            const secret = await contract.getSecretDetails(secretId);
            expect(secret.unlockBlock).to.equal(newUnlockBlock);
        });

        it("Should reject update after unlock", async function () {
            await mineBlocks(10);

            await expect(
                contract.updateUnlockTime(secretId, unlockBlock + 100)
            ).to.be.revertedWith("Already unlocked");
        });

        it("Should reject update to past block", async function () {
            const pastBlock = await ethers.provider.getBlockNumber();

            await expect(
                contract.updateUnlockTime(secretId, pastBlock)
            ).to.be.revertedWith("New unlock must be in future");
        });

        it("Should reject update by non-owner", async function () {
            await expect(
                contract.connect(otherUser).updateUnlockTime(secretId, unlockBlock + 100)
            ).to.be.revertedWith("Not authorized: only owner can access");
        });
    });

    describe("emergencyCancel", function () {
        beforeEach(async function () {
            await contract.lockShare(secretId, encryptedShare, unlockBlock);
        });

        it("Should cancel secret before unlock", async function () {
            await contract.emergencyCancel(secretId);

            await expect(
                contract.isUnlocked(secretId)
            ).to.be.revertedWith("Secret does not exist");

            expect(await contract.totalSecrets()).to.equal(0);
        });

        it("Should reject cancel after unlock", async function () {
            await mineBlocks(10);

            await expect(
                contract.emergencyCancel(secretId)
            ).to.be.revertedWith("Already unlocked - cannot cancel");
        });

        it("Should reject cancel by non-owner", async function () {
            await expect(
                contract.connect(otherUser).emergencyCancel(secretId)
            ).to.be.revertedWith("Not authorized: only owner can access");
        });
    });

    describe("estimateUnlockTime", function () {
        beforeEach(async function () {
            await contract.lockShare(secretId, encryptedShare, unlockBlock);
        });

        it("Should estimate unlock time correctly", async function () {
            const avgBlockTime = 12; // Ethereum mainnet average
            const currentTime = await time.latest();
            const currentBlock = await ethers.provider.getBlockNumber();

            const estimate = await contract.estimateUnlockTime(secretId, avgBlockTime);

            const expectedTime = currentTime + ((unlockBlock - currentBlock) * avgBlockTime);

            // Allow 1 block time tolerance
            expect(estimate).to.be.closeTo(expectedTime, avgBlockTime);
        });

        it("Should return current time if already unlocked", async function () {
            await mineBlocks(10);

            const currentTime = await time.latest();
            const estimate = await contract.estimateUnlockTime(secretId, 12);

            expect(estimate).to.equal(currentTime);
        });
    });

    describe("Multiple shares", function () {
        // NOTE: This test involves multiple transactions and may take longer to execute
        it("Should handle multiple shares from same owner", async function () {
            this.timeout(10000); // Extend timeout to 10 seconds

            const shareIds = [];
            const shareCount = 5;

            // Lock 5 shares (simulating 3-of-5 Shamir sharing)
            for (let i = 0; i < shareCount; i++) {
                const id = ethers.keccak256(ethers.toUtf8Bytes(`share-${i}`));
                shareIds.push(id);

                await contract.lockShare(
                    id,
                    ethers.toUtf8Bytes(`share-data-${i}`),
                    unlockBlock
                );
            }

            expect(await contract.totalSecrets()).to.equal(shareCount);

            const ownerSecrets = await contract.getOwnerSecrets(owner.address);
            expect(ownerSecrets.length).to.equal(shareCount);

            // Mine to unlock
            await mineBlocks(10);

            // Retrieve first 3 shares (threshold)
            for (let i = 0; i < 3; i++) {
                const retrieved = await contract.retrieveShare(shareIds[i]);
                expect(retrieved).to.not.be.empty;
            }
        });
    });

    describe("Gas optimization", function () {
        it("Should estimate gas for typical operations", async function () {
            // Lock share
            const lockTx = await contract.lockShare(secretId, encryptedShare, unlockBlock);
            const lockReceipt = await lockTx.wait();
            console.log(`      ⛽ lockShare gas used: ${lockReceipt.gasUsed.toString()}`);

            // Mine blocks
            await mineBlocks(10);

            // Retrieve share
            const retrieveTx = await contract.retrieveShare.staticCall(secretId);
            console.log(`      ⛽ retrieveShare (read-only)`);

            // Check status
            const statusCall = await contract.isUnlocked.staticCall(secretId);
            console.log(`      ⛽ isUnlocked (read-only)`);

            // Mark retrieved
            const markTx = await contract.markRetrieved(secretId);
            const markReceipt = await markTx.wait();
            console.log(`      ⛽ markRetrieved gas used: ${markReceipt.gasUsed.toString()}`);
        });
    });

    // Helper function to mine blocks
    async function mineBlocks(count) {
        for (let i = 0; i < count; i++) {
            await ethers.provider.send("evm_mine");
        }
    }
});
