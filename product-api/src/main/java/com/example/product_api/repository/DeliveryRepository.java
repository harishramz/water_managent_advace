package com.example.product_api.repository;

import com.example.product_api.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByDeliveryPerson_IdOrderByAssignedAtAsc(Long userId);
    Optional<Delivery> findByIdAndDeliveryPerson_Id(Long id, Long userId);
    Optional<Delivery> findByOrder_Id(Long orderId);
}