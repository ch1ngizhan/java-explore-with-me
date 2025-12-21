package ru.practicum.category.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.category.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(@Param("name") String name);

    Optional<Category> findByNameAndIdNot(@Param("name") String name, @Param("id") Long id);

    @Query("SELECT c FROM Category c")
    List<Category> findAllList(Pageable pageable);
}
