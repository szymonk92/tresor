package io.tresor.core.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Represents a plaintext message before encryption.
 */
@Data
@Builder
public class PlainMessage {

    /**
     * Text content of the message
     */
    private String textContent;

    /**
     * Email address for delivery (optional)
     */
    private String deliveryEmail;

    /**
     * Attached files (images, audio, video)
     */
    private List<AttachedFile> files;

    /**
     * Calculate total size of message
     */
    public long calculateSize() {
        long textSize = textContent != null ? textContent.getBytes().length : 0;
        long filesSize = files != null ?
                files.stream().mapToLong(f -> f.getData().length).sum() : 0;
        return textSize + filesSize;
    }

    @Data
    public static class AttachedFile {
        private String filename;
        private String contentType;
        private byte[] fileData;
        private long sizeBytes;

        public AttachedFile(String filename, String contentType, byte[] fileData, long sizeBytes) {
            this.filename = filename;
            this.contentType = contentType;
            this.fileData = fileData;
            this.sizeBytes = sizeBytes;
        }

        // Alias methods for compatibility
        public String getFileName() {
            return filename;
        }

        public byte[] getData() {
            return fileData;
        }
    }
}
