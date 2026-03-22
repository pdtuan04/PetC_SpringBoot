package com.hutech.coca.repository;

import com.hutech.coca.model.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IPetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findByUserIdAndIsDeletedFalse(Long userId);
}