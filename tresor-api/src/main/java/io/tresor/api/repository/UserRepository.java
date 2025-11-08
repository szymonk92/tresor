package io.tresor.api.repository;

import io.tresor.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find user by email address.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if email exists.
     */
    boolean existsByEmail(String email);
}
