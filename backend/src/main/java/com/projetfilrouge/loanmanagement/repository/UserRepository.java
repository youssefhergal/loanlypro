package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            SELECT u FROM User u
            JOIN u.roles r
            WHERE r.name = 'ROLE_CONSEILLER'
            ORDER BY u.lastName, u.firstName
            """)
    List<User> findAllConseillers();

    @Query("""
            SELECT u FROM User u
            JOIN u.roles r
            WHERE r.name = 'ROLE_ADMIN'
            ORDER BY u.lastName, u.firstName
            """)
    List<User> findAllAdmins();

    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN u.roles r
            WHERE (:roleName IS NULL OR r.name = :roleName)
              AND (
                    :q IS NULL
                 OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            ORDER BY u.lastName, u.firstName
            """)
    Page<User> searchUsers(@Param("roleName") String roleName,
                           @Param("q") String q,
                           Pageable pageable);
}
