package com.example.product_api.repository;

import com.example.product_api.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<Notification> findByIdAndUser_Id(Long id, Long userId);
}