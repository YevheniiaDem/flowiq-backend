package com.flowiq.profile.repository;

import com.flowiq.profile.entity.FopProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FopProfileRepository extends JpaRepository<FopProfile, Long> {

    Optional<FopProfile> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);
}
