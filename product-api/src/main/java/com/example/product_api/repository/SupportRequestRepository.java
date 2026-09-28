package com.example.product_api.repository;

import com.example.product_api.model.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
    List<SupportRequest> findByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<SupportRequest> findByIdAndUser_Id(Long id, Long userId);
}