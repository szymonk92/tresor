package io.tresor.core.blockchain;

import io.tresor.core.secretsharing.DeploymentOrchestrator.ChainAdapter;
import io.tresor.core.secretsharing.SecretHolderService.*;
import io.tresor.core.secretsharing.ShamirSecretSharing;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Self-custody blockchain adapter using local file storage.
 *
 * This adapter stores shares as encrypted files on the local filesystem.
 * Unlike blockchain deployments, self-custody shares:
 * - Cost $0 to deploy
 * - Are available instantly (no block height checks)
 * - Require user to maintain backup
 * - Support rollback (file deletion)
 *
 * File format:
 * ```
 * ~/.tresor/shares/
 *   ├── share-abc123-unlock-2035-01-15.bin
 *   └── share-def456-unlock-2040-06-20.bin
 * ```
 *
 * Security considerations:
 * - Files are stored with restricted permissions (owner read/write only)
 * - Shares should be encrypted at rest (TODO: implement encryption)
 * - Users should backup this directory
 * - Consider encrypting with user's master key
 *
 * Usage:
 * ```java
 * SelfCustodyAdapter adapter = new SelfCustodyAdapter("/path/to/storage");
 * ShareDeployment deployment = adapter.deploy(share, unlockDate, config);
 *
 * // Later: Retrieve
 * ShamirSecretSharing.Share retrieved = adapter.retrieveShare(deployment.getIdentifier());
 * ```
 */
@Slf4j
public class SelfCustodyAdapter implements ChainAdapter {

    private final Path storageDirectory;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Create adapter with default storage directory.
     */
    public SelfCustodyAdapter() {
        this(getDefaultStorageDirectory());
    }

    /**
     * Create adapter with custom storage directory.
     */
    public SelfCustodyAdapter(String storagePath) {
        this(Paths.get(storagePath));
    }

    /**
     * Create adapter with custom storage directory.
     */
    public SelfCustodyAdapter(Path storagePath) {
        this.storageDirectory = storagePath;
        initializeStorage();
    }

    @Override
    public ShareDeployment deploy(
        ShamirSecretSharing.Share share,
        LocalDateTime unlockDate,
        DeploymentConfig config
    ) throws Exception {

        log.debug("Self-custody deploying share {} (unlock: {})", share.getX(), unlockDate);

        // Generate unique file ID
        String fileId = "share-" + UUID.randomUUID().toString().substring(0, 8);

        // Create filename with unlock date
        String filename = String.format("%s-unlock-%s.bin",
            fileId, unlockDate.format(DATE_FORMAT));

        Path filePath = storageDirectory.resolve(filename);

        // Serialize share to file
        writeShareToFile(share, unlockDate, filePath);

        // Set restrictive permissions (owner read/write only)
        setFilePermissions(filePath);

        ShareDeployment deployment = ShareDeployment.builder()
            .shareNumber(share.getX())
            .blockchain(Blockchain.SELF_CUSTODY)
            .identifier(filePath.toString())
            .unlockBlockHeight(0) // Not applicable for self-custody
            .costUsd(0.0) // Free!
            .status(DeploymentStatus.CONFIRMED)
            .build();

        log.info("✅ Self-custody share deployed: {} (file: {})", share.getX(), filename);

        return deployment;
    }

    @Override
    public boolean supportsRollback() {
        return true; // Can delete files
    }

    @Override
    public void rollback(ShareDeployment deployment) throws Exception {
        log.warn("Rolling back self-custody share: {}", deployment.getIdentifier());

        Path filePath = Paths.get(deployment.getIdentifier());

        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("✅ Deleted self-custody share: {}", filePath.getFileName());
        } else {
            log.warn("Self-custody share not found for rollback: {}", filePath);
        }
    }

    /**
     * Retrieve a deployed share from file storage.
     *
     * @param identifier File path from ShareDeployment
     * @return The retrieved share
     * @throws Exception if file not found or deserialization fails
     */
    public ShamirSecretSharing.Share retrieveShare(String identifier) throws Exception {
        Path filePath = Paths.get(identifier);

        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Self-custody share not found: " + identifier);
        }

        ShareFileData data = readShareFromFile(filePath);

        log.info("✅ Retrieved self-custody share from: {}", filePath.getFileName());

        return data.share;
    }

    /**
     * Check if share has reached unlock date.
     *
     * For self-custody, we check the file's embedded unlock date.
     */
    public boolean isUnlocked(String identifier) {
        try {
            Path filePath = Paths.get(identifier);

            if (!Files.exists(filePath)) {
                return false;
            }

            ShareFileData data = readShareFromFile(filePath);

            return LocalDateTime.now().isAfter(data.unlockDate) ||
                   LocalDateTime.now().isEqual(data.unlockDate);

        } catch (Exception e) {
            log.error("Failed to check unlock status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * List all self-custody shares in storage directory.
     */
    public java.util.List<ShareFileInfo> listShares() throws IOException {
        java.util.List<ShareFileInfo> shares = new java.util.ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
            storageDirectory, "share-*.bin")) {

            for (Path file : stream) {
                try {
                    ShareFileData data = readShareFromFile(file);

                    ShareFileInfo info = new ShareFileInfo(
                        file.toString(),
                        data.share.getX(),
                        data.unlockDate,
                        Files.size(file),
                        isUnlocked(file.toString())
                    );

                    shares.add(info);

                } catch (Exception e) {
                    log.error("Failed to read share file {}: {}", file, e.getMessage());
                }
            }
        }

        return shares;
    }

    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================

    private void initializeStorage() {
        try {
            if (!Files.exists(storageDirectory)) {
                Files.createDirectories(storageDirectory);
                log.info("Created self-custody storage directory: {}", storageDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize self-custody storage", e);
        }
    }

    private void writeShareToFile(
        ShamirSecretSharing.Share share,
        LocalDateTime unlockDate,
        Path filePath
    ) throws IOException {

        try (DataOutputStream out = new DataOutputStream(
            new BufferedOutputStream(Files.newOutputStream(filePath)))) {

            // Write file format version (2 = includes threshold and totalShares)
            out.writeInt(2);

            // Write unlock date
            out.writeLong(unlockDate.toEpochSecond(ZoneOffset.UTC));

            // Write share data
            out.writeInt(share.getX()); // Share index
            out.writeInt(share.getPrime().bitLength()); // Prime bit length

            byte[] primeBytes = share.getPrime().toByteArray();
            out.writeInt(primeBytes.length);
            out.write(primeBytes);

            byte[] yBytes = share.getY().toByteArray();
            out.writeInt(yBytes.length);
            out.write(yBytes);

            // Write threshold and totalShares (new in version 2)
            out.writeInt(share.getThreshold());
            out.writeInt(share.getTotalShares());
        }
    }

    private ShareFileData readShareFromFile(Path filePath) throws IOException {
        try (DataInputStream in = new DataInputStream(
            new BufferedInputStream(Files.newInputStream(filePath)))) {

            // Read file format version
            int version = in.readInt();
            if (version != 1 && version != 2) {
                throw new IOException("Unsupported file format version: " + version);
            }

            // Read unlock date
            long unlockTimestamp = in.readLong();
            LocalDateTime unlockDate = LocalDateTime.ofEpochSecond(
                unlockTimestamp, 0, ZoneOffset.UTC
            );

            // Read share data
            int x = in.readInt();
            int primeBitLength = in.readInt();

            int primeLength = in.readInt();
            byte[] primeBytes = new byte[primeLength];
            in.readFully(primeBytes);

            int yLength = in.readInt();
            byte[] yBytes = new byte[yLength];
            in.readFully(yBytes);

            java.math.BigInteger prime = new java.math.BigInteger(primeBytes);
            java.math.BigInteger y = new java.math.BigInteger(yBytes);

            // Read threshold and totalShares (if version 2)
            int threshold = 3;  // Default for version 1
            int totalShares = 5;  // Default for version 1
            if (version >= 2) {
                threshold = in.readInt();
                totalShares = in.readInt();
            }

            ShamirSecretSharing.Share share = new ShamirSecretSharing.Share(x, y, prime, threshold, totalShares);

            return new ShareFileData(share, unlockDate);
        }
    }

    private void setFilePermissions(Path filePath) {
        try {
            // On Unix-like systems, set to owner read/write only (600)
            if (filePath.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                Files.setPosixFilePermissions(filePath,
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            }
        } catch (Exception e) {
            log.warn("Failed to set restrictive permissions on {}: {}",
                filePath, e.getMessage());
        }
    }

    private static Path getDefaultStorageDirectory() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".tresor", "shares");
    }

    // ========================================
    // VALUE OBJECTS
    // ========================================

    private static class ShareFileData {
        final ShamirSecretSharing.Share share;
        final LocalDateTime unlockDate;

        ShareFileData(ShamirSecretSharing.Share share, LocalDateTime unlockDate) {
            this.share = share;
            this.unlockDate = unlockDate;
        }
    }

    public static class ShareFileInfo {
        public final String filePath;
        public final int shareNumber;
        public final LocalDateTime unlockDate;
        public final long fileSizeBytes;
        public final boolean unlocked;

        ShareFileInfo(
            String filePath,
            int shareNumber,
            LocalDateTime unlockDate,
            long fileSizeBytes,
            boolean unlocked
        ) {
            this.filePath = filePath;
            this.shareNumber = shareNumber;
            this.unlockDate = unlockDate;
            this.fileSizeBytes = fileSizeBytes;
            this.unlocked = unlocked;
        }

        @Override
        public String toString() {
            return String.format("Share #%d (unlock: %s, size: %d bytes, %s)",
                shareNumber,
                unlockDate.format(DATE_FORMAT),
                fileSizeBytes,
                unlocked ? "UNLOCKED" : "LOCKED");
        }
    }
}
