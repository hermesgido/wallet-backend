package com.mwangahakika.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mwangahakika.backend.entity.TopUpRequest;
import com.mwangahakika.backend.enums.TopUpRequestStatus;

@Repository
public interface TopUpRequestRepository extends JpaRepository<TopUpRequest, Long> {

    List<TopUpRequest> findByUserId(Long userId);

    List<TopUpRequest> findByStatus(TopUpRequestStatus status);
    List<TopUpRequest> findByUserIdOrderByRequestedAtDesc(Long userId);
    List<TopUpRequest> findByStatusOrderByRequestedAtAsc(TopUpRequestStatus status);
}
