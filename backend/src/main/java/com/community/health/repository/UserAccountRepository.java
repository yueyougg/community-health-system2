package com.community.health.repository;

import com.community.health.model.UserAccount;
import com.community.health.model.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    Page<UserAccount> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    Page<UserAccount> findByRole(Role role, Pageable pageable);

    Page<UserAccount> findByUsernameContainingIgnoreCaseAndRole(String username, Role role, Pageable pageable);

    @Query("select u from UserAccount u where u.role = :role and u.enabled = true and " +
            "u.id not in (select r.user.id from ResidentProfile r where r.user is not null)")
    List<UserAccount> findUnboundEnabledUsersByRole(Role role);
}