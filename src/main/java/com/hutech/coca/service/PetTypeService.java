package com.hutech.coca.service;

import com.hutech.coca.dto.PetTypeResponse;
import com.hutech.coca.repository.IPetTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetTypeService {

    private final IPetTypeRepository petTypeRepository;

    // Lấy danh sách tất cả các loại thú cưng (Chó, Mèo,...)
    public List<PetTypeResponse> getActivePetTypes() {
        return petTypeRepository.findAll().stream().map(pt -> {
            PetTypeResponse dto = new PetTypeResponse();
            dto.setId(pt.getId());
            dto.setName(pt.getName());
            return dto;
        }).collect(Collectors.toList());
    }
}