package com.hutech.coca.repository;

import com.hutech.coca.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByIsActiveTrue();
}
