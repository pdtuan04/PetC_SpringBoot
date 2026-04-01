package com.hutech.coca.repository;

import com.hutech.coca.model.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IPetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findByUserIdAndIsDeletedFalse(Long userId);
    List<Pet> findByIsDeletedFalse();

    @Query("SELECT p FROM Pet p WHERE p.id = :id")
    Optional<Pet> findByIdIgnoreDeleted(@Param("id") Long id);
}