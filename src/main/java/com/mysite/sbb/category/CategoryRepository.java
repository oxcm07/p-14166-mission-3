package com.mysite.sbb.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Optional<Category> findByCode(String code);

    List<Category> findAllByOrderByIdAsc();
}
