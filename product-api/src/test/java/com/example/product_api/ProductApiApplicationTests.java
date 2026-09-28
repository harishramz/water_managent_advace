package com.example.product_api;

import com.example.product_api.model.Product;
import com.example.product_api.model.User;
import com.example.product_api.repository.ProductRepository;
import com.example.product_api.repository.UserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductApiApplicationTests {

	@Autowired private MockMvc mvc;
	@Autowired private ObjectMapper mapper;
	@Autowired private ProductRepository products;
	@Autowired private UserRepository users;

	@BeforeEach
	void clearTables() {
		products.deleteAll();
		users.deleteAll();
	}

	@Test
	void customerCanRegisterCheckoutAndTrackAnOrderWithoutExposingAdminOperations() throws Exception {
		mvc.perform(post("/api/products").contentType("application/json")
				.content("""
						{"name":"Unprivileged water","price":10,"stockQuantity":1}
						"""))
			.andExpect(status().isForbidden());

		MvcResult registration = mvc.perform(post("/api/auth/register").contentType("application/json")
				.content("""
						{"name":"Test Customer","email":"WATER.TEST@example.test","password":"test-water-password"}
						"""))
			.andExpect(status().isOk()).andReturn();
		assertThat(registration.getResponse().getContentAsString()).contains("registered successfully");

		MvcResult login = mvc.perform(post("/api/auth/login").contentType("application/json")
				.content("""
						{"email":"water.test@example.test","password":"test-water-password"}
						"""))
			.andExpect(status().isOk()).andReturn();
		MockHttpSession customerSession = (MockHttpSession) login.getRequest().getSession(false);
		assertThat(customerSession).isNotNull();
		assertThat(users.findByEmailIgnoreCase("water.test@example.test").orElseThrow().getPassword())
				.startsWith("$2");

		User admin = new User("water-admin@example.test", new BCryptPasswordEncoder().encode("test-admin-password"));
		admin.setRole("ADMIN");
		admin.setStatus("ACTIVE");
		users.save(admin);
		MvcResult adminLogin = mvc.perform(post("/api/auth/login").contentType("application/json")
				.content("""
						{"email":"water-admin@example.test","password":"test-admin-password"}
						"""))
			.andExpect(status().isOk()).andReturn();
		MockHttpSession adminSession = (MockHttpSession) adminLogin.getRequest().getSession(false);

		MvcResult createdProduct = mvc.perform(post("/api/products").session(adminSession).contentType("application/json")
				.content("""
						{"name":"20L Spring Water","description":"Test product","waterType":"Mineral","capacity":"20L","brand":"Bisleri","price":100,"stockQuantity":5}
						"""))
			.andExpect(status().isOk()).andReturn();
		long productId = mapper.readTree(createdProduct.getResponse().getContentAsString()).get("id").asLong();
		JsonNode brands = mapper.readTree(mvc.perform(get("/api/products/brands")).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString());
		assertThat(brands.get(0).get("name").asText()).isEqualTo("Bisleri");
		assertThat(brands.get(0).get("productCount").asInt()).isEqualTo(1);
		mvc.perform(get("/api/products").param("brand", "Bisleri")).andExpect(status().isOk());

		MvcResult savedAddress = mvc.perform(post("/api/addresses").session(customerSession).contentType("application/json")
				.content("""
						{"fullName":"Test Customer","phone":"9876543210","addressLine":"1 Test Road","area":"Central","city":"Water City","state":"Test State","pincode":"123456","addressType":"HOME","defaultAddress":true}
						"""))
			.andExpect(status().isOk()).andReturn();
		long addressId = mapper.readTree(savedAddress.getResponse().getContentAsString()).get("id").asLong();

		MvcResult cartResult = mvc.perform(post("/api/cart/items").session(customerSession).contentType("application/json")
				.content(mapper.writeValueAsString(Map.of("productId", productId, "quantity", 2))))
			.andExpect(status().isOk()).andReturn();
		JsonNode cart = mapper.readTree(cartResult.getResponse().getContentAsString());
		assertThat(cart.get("itemCount").asInt()).isEqualTo(2);
		assertThat(cart.get("items").get(0).get("brand").asText()).isEqualTo("Bisleri");

		MvcResult placed = mvc.perform(post("/api/orders").session(customerSession).contentType("application/json")
				.content(mapper.writeValueAsString(Map.of("addressId", addressId, "paymentMethod", "CASH_ON_DELIVERY"))))
			.andExpect(status().isOk()).andReturn();
		JsonNode order = mapper.readTree(placed.getResponse().getContentAsString());
		long orderId = order.get("id").asLong();
		assertThat(order.get("totalAmount").asDouble()).isEqualTo(230.0);
		assertThat(products.findById(productId).orElseThrow().getStockQuantity()).isEqualTo(3);

		mvc.perform(get("/api/orders/" + orderId).session(customerSession)).andExpect(status().isOk());
		mvc.perform(get("/api/notifications").session(customerSession)).andExpect(status().isOk());
		mvc.perform(post("/api/cart/items").session(customerSession).contentType("application/json")
				.content(mapper.writeValueAsString(Map.of("productId", productId, "quantity", 4))))
			.andExpect(status().isBadRequest());
		mvc.perform(post("/api/orders/" + orderId + "/cancel").session(customerSession))
			.andExpect(status().isOk());
		assertThat(products.findById(productId).orElseThrow().getStockQuantity()).isEqualTo(5);
		mvc.perform(get("/api/admin/dashboard").session(adminSession)).andExpect(status().isOk());
		mvc.perform(get("/api/admin/reports").session(adminSession)).andExpect(status().isOk());
		mvc.perform(get("/api/admin/orders").session(customerSession)).andExpect(status().isForbidden());
	}

}
