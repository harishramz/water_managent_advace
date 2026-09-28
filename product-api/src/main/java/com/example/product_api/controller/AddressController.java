package com.example.product_api.controller;

import com.example.product_api.model.Address;
import com.example.product_api.model.User;
import com.example.product_api.repository.AddressRepository;
import com.example.product_api.repository.UserRepository;
import com.example.product_api.repository.CustomerOrderRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {
    private final AddressRepository addresses;
    private final UserRepository users;
    private final CustomerOrderRepository orders;

    public AddressController(AddressRepository addresses, UserRepository users, CustomerOrderRepository orders) {
        this.addresses = addresses;
        this.users = users;
        this.orders = orders;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpSession session) {
        Long userId = customerId(session);
        if (userId == null) return unauthorized();
        return ResponseEntity.ok(addresses.findByUser_IdOrderByDefaultAddressDescIdDesc(userId).stream().map(this::view).toList());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody AddressRequest request, HttpSession session) {
        Long userId = customerId(session);
        if (userId == null) return unauthorized();
        if (!valid(request)) return ResponseEntity.badRequest().body(Map.of("message", "Complete the required address fields with a valid phone and pincode"));
        User user = users.findById(userId).orElseThrow();
        Address address = new Address();
        address.setUser(user);
        apply(address, request);
        if (request.defaultAddress()) {
            addresses.findByUser_IdAndDefaultAddressTrue(userId).forEach(existing -> existing.setDefaultAddress(false));
            address.setDefaultAddress(true);
        }
        return ResponseEntity.ok(view(addresses.save(address)));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody AddressRequest request, HttpSession session) {
        Long userId = customerId(session);
        if (userId == null) return unauthorized();
        if (!valid(request)) return ResponseEntity.badRequest().body(Map.of("message", "Complete the required address fields with a valid phone and pincode"));
        return addresses.findByIdAndUser_Id(id, userId).map(address -> {
            apply(address, request);
            if (request.defaultAddress()) {
                addresses.deleteByUser_IdAndDefaultAddressTrueAndIdNot(userId, id);
                    addresses.findByUser_IdAndDefaultAddressTrue(userId).stream()
                            .filter(existing -> !existing.getId().equals(id))
                            .forEach(existing -> existing.setDefaultAddress(false));
                address.setDefaultAddress(true);
            }
            return ResponseEntity.ok(view(addresses.save(address)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        Long userId = customerId(session);
        if (userId == null) return unauthorized();
        return addresses.findByIdAndUser_Id(id, userId).map(address -> {
            if (orders.existsByAddress_Id(id)) {
                return ResponseEntity.badRequest().body(Map.of("message", "This address is linked to an order and cannot be removed"));
            }
            if (address.isDefaultAddress()) address.setDefaultAddress(false);
            addresses.delete(address);
            return ResponseEntity.noContent().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Long customerId(HttpSession session) {
        Object id = session.getAttribute("userId");
        return "CUSTOMER".equals(session.getAttribute("role")) && id instanceof Long userId ? userId : null;
    }

    private boolean valid(AddressRequest request) {
        return request != null && present(request.fullName()) && request.phone() != null
                && request.phone().matches("[0-9+() -]{8,18}") && present(request.addressLine())
                && present(request.city()) && present(request.state())
                && request.pincode() != null && request.pincode().matches("[A-Za-z0-9 -]{4,10}");
    }

    private boolean present(String value) { return value != null && !value.isBlank(); }

    private void apply(Address address, AddressRequest request) {
        address.setFullName(request.fullName().trim());
        address.setPhone(request.phone().trim());
        address.setAddressLine(request.addressLine().trim());
        address.setArea(request.area());
        address.setCity(request.city().trim());
        address.setState(request.state().trim());
        address.setPincode(request.pincode().trim());
        address.setLandmark(request.landmark());
        address.setAddressType(request.addressType());
        address.setDefaultAddress(request.defaultAddress());
    }

    private Map<String, Object> view(Address address) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", address.getId());
        result.put("fullName", address.getFullName());
        result.put("phone", address.getPhone());
        result.put("addressLine", address.getAddressLine());
        result.put("area", address.getArea());
        result.put("city", address.getCity());
        result.put("state", address.getState());
        result.put("pincode", address.getPincode());
        result.put("landmark", address.getLandmark());
        result.put("addressType", address.getAddressType());
        result.put("defaultAddress", address.isDefaultAddress());
        return result;
    }

    private ResponseEntity<?> unauthorized() { return ResponseEntity.status(401).body(Map.of("message", "Please log in")); }

    public record AddressRequest(String fullName, String phone, String addressLine, String area,
                                 String city, String state, String pincode, String landmark,
                                 String addressType, boolean defaultAddress) {}
}