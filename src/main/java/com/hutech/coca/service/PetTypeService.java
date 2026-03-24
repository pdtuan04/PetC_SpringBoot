package com.hutech.coca.service;

import com.hutech.coca.dto.PetTypeRequest;
import com.hutech.coca.dto.PetTypeResponse;
import com.hutech.coca.model.PetType;
import com.hutech.coca.repository.IPetTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetTypeService {

    private final IPetTypeRepository petTypeRepository;

    public List<PetTypeResponse> getActivePetTypes() {
        return petTypeRepository.findByIsActiveTrue().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public Map<String, Object> getPaginatedList(String search, String sortBy, String sortDir, int pageNumber, int pageSize) {
        Sort.Direction direction = "Descending".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProperty = "Name".equalsIgnoreCase(sortBy) ? "name" : "id";
        
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by(direction, sortProperty));
        Page<PetType> page = petTypeRepository.searchPetTypes(search, pageable);

        List<PetTypeResponse> items = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("totalCount", page.getTotalElements());
        
        return data;
    }

    public PetTypeResponse getPetTypeById(Long id) {
        PetType petType = petTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại thú cưng"));
        return mapToResponse(petType);
    }

    public PetTypeResponse createPetType(PetTypeRequest request) {
        PetType petType = new PetType();
        petType.setName(request.getName());
        petType.setActive(request.isActive());
        petType = petTypeRepository.save(petType);
        return mapToResponse(petType);
    }

    public PetTypeResponse updatePetType(PetTypeRequest request) {
        PetType petType = petTypeRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại thú cưng"));
        petType.setName(request.getName());
        petType.setActive(request.isActive());
        petType = petTypeRepository.save(petType);
        return mapToResponse(petType);
    }

    public boolean softDeletePetType(Long id) {
        PetType petType = petTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại thú cưng"));
        petType.setActive(false);
        petTypeRepository.save(petType);
        return true;
    }

    public boolean activePetType(Long id) {
        PetType petType = petTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại thú cưng"));
        petType.setActive(true);
        petTypeRepository.save(petType);
        return true;
    }

    public boolean deletePetType(Long id) {
        if (!petTypeRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy loại thú cưng");
        }
        petTypeRepository.deleteById(id);
        return true;
    }

    private PetTypeResponse mapToResponse(PetType petType) {
        PetTypeResponse dto = new PetTypeResponse();
        dto.setId(petType.getId());
        dto.setName(petType.getName());
        dto.setActive(petType.isActive());
        dto.setCreateAt(petType.getCreateAt());
        return dto;
    }

    @Transactional
    public PetTypeResponse createPetType(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Tên loại thú cưng là bắt buộc");
        }

        PetType petType = new PetType();
        petType.setName(name.trim());
       petType.setActive(true);
petType.setDeleted(false);

        PetType saved = petTypeRepository.save(petType);

        PetTypeResponse dto = new PetTypeResponse();
        dto.setId(saved.getId());
        dto.setName(saved.getName());
        return dto;
    }
}