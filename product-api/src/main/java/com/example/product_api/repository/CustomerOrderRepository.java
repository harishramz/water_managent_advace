package com.example.product_api.repository;

import com.example.product_api.model.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByUser_IdOrderByOrderDateDesc(Long userId);
    Optional<CustomerOrder> findByIdAndUser_Id(Long id, Long userId);
    boolean existsByAddress_Id(Long addressId);
}