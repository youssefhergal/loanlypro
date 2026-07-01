package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
