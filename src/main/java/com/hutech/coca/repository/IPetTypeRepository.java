package com.hutech.coca.repository;

import com.hutech.coca.model.Pet;
import com.hutech.coca.model.PetType;
import com.hutech.coca.model.Service;
import com.hutech.coca.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IPetTypeRepository extends JpaRepository<PetType, Long> {
    Optional<PetType> findById(Long id);
    List<PetType> findByIsActiveTrue();
}
