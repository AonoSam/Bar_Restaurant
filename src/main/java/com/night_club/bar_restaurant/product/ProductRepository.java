package com.night_club.bar_restaurant.product;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory(ProductCategory category);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByCategoryAndStatus(ProductCategory category, ProductStatus status);

    List<Product> findByNameContainingIgnoreCase(String name);

    // PESSIMISTIC_WRITE takes a row-level lock (SELECT ... FOR UPDATE) so two
    // orders paid at nearly the same instant can't both read stock=1, both
    // decrement to 0, and oversell by one unit. Mirrors the select_for_update()
    // pattern used for Tronilla's loan repayment concurrency handling.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(Long id);
}