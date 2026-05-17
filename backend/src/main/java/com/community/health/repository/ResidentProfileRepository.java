package com.community.health.repository;

import com.community.health.model.ResidentProfile;
import com.community.health.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ResidentProfileRepository extends JpaRepository<ResidentProfile, Long> {

    Optional<ResidentProfile> findByArchiveNo(String archiveNo);
    
    Optional<ResidentProfile> findByIdCard(String idCard);

    Optional<ResidentProfile> findByUser(UserAccount user);

    List<ResidentProfile> findByNameContainingOrArchiveNoContaining(String name, String archiveNo);

    @Query("SELECT r FROM ResidentProfile r WHERE " +
           "(:keyword IS NULL OR r.name LIKE %:keyword% OR r.archiveNo LIKE %:keyword%) " +
           "ORDER BY r.id DESC")
    List<ResidentProfile> searchByKeyword(@org.springframework.data.repository.query.Param("keyword") String keyword);

    @Query("select r.gender, count(r.id) from ResidentProfile r group by r.gender")
    List<Object[]> countByGender();
}
