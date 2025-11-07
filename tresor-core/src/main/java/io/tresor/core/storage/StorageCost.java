package io.tresor.core.storage;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Represents the cost of storing a message.
 */
@Data
@Builder
public class StorageCost {

    /**
     * Cost in USD
     */
    private BigDecimal usdCost;

    /**
     * Cost in native currency of the storage provider
     * (e.g., AR for Arweave, FIL for Filecoin)
     */
    private BigDecimal nativeCost;

    /**
     * Native currency symbol
     */
    private String nativeCurrency;

    /**
     * Size in bytes
     */
    private long sizeBytes;

    /**
     * Whether this is a one-time cost or recurring
     */
    private boolean oneTime;

    /**
     * If recurring, the period (e.g., "month", "year")
     */
    private String period;
}
