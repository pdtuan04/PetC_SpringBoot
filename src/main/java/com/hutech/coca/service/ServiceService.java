package com.hutech.coca.service;

import com.hutech.coca.dto.ServiceResponse;
import com.hutech.coca.repository.IServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceService {

    private final IServiceRepository serviceRepository;

    public List<ServiceResponse> getAllActiveServices() {
        return serviceRepository.findByIsActiveTrue().stream().map(s -> {
            ServiceResponse dto = new ServiceResponse();
            dto.setId(s.getId());
            dto.setName(s.getName());
            dto.setDescription(s.getDescription());
            dto.setPrice(s.getPrice());
            dto.setDurationInMinutes(s.getDurationInMinutes());
            return dto;
        }).collect(Collectors.toList());
    }
}