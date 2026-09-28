package com.example.product_api.repository;

import com.example.product_api.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUser_Id(Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CartItem c join fetch c.product where c.user.id = :userId order by c.product.id")
    List<CartItem> findByUser_IdForUpdate(@Param("userId") Long userId);
    Optional<CartItem> findByIdAndUser_Id(Long id, Long userId);
    Optional<CartItem> findByUser_IdAndProduct_Id(Long userId, Long productId);
    void deleteByUser_Id(Long userId);
}