package com.example.product_api.repository;

import com.example.product_api.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUser_IdOrderByDefaultAddressDescIdDesc(Long userId);
    Optional<Address> findByIdAndUser_Id(Long id, Long userId);
    void deleteByUser_IdAndDefaultAddressTrue(Long userId);
    void deleteByUser_IdAndDefaultAddressTrueAndIdNot(Long userId, Long id);
    List<Address> findByUser_IdAndDefaultAddressTrue(Long userId);
}