package io.tresor.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application for Tresor.
 *
 * Tresor: Time-locked message delivery service.
 *
 * Features:
 * - AES-256-GCM encryption for messages
 * - Shamir Secret Sharing (3-of-5 threshold)
 * - Multi-chain secret holders (Bitcoin, Ethereum, Arbitrum)
 * - Magic link authentication
 * - Email delivery on unlock
 *
 * Architecture:
 * - tresor-core: Encryption, secret sharing, blockchain adapters
 * - tresor-api: REST API, database, scheduling
 * - tresor-storage-arweave: Arweave permanent storage
 */
@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories("io.tresor.api.repository")
@EntityScan("io.tresor.api.model")
public class TresorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TresorApplication.class, args);
    }
}
