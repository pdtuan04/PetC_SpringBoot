package com.hutech.coca.service;

import com.hutech.coca.dto.CreatePetRequest;
import com.hutech.coca.dto.PetResponse;
import com.hutech.coca.model.Pet;
import com.hutech.coca.model.PetType;
import com.hutech.coca.model.User;
import com.hutech.coca.repository.IPetRepository;
import com.hutech.coca.repository.IPetTypeRepository;
import com.hutech.coca.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetService {

    private final IPetRepository petRepository;
    private final IUserRepository userRepository;
    private final IPetTypeRepository petTypeRepository;

    @Transactional
    public PetResponse createUserPet(Long ownerId, CreatePetRequest dto) {
        // Kiểm tra User và PetType có tồn tại không
        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        PetType petType = petTypeRepository.findById(dto.getPetTypeId())
                .orElseThrow(() -> new RuntimeException("Pet Type not found"));

        // Tạo Entity Pet mới
        Pet pet = new Pet();
        pet.setUser(user);
        pet.setPetType(petType);
        pet.setName(dto.getName());
        pet.setAge(dto.getAge());
        pet.setImageUrl(dto.getImageUrl());

        // Lưu vào DB
        petRepository.save(pet);

        // Map sang DTO trả về
        PetResponse response = new PetResponse();
        response.setId(pet.getId());
        response.setName(pet.getName());
        response.setAge(pet.getAge());
        response.setImageUrl(pet.getImageUrl());

        return response;
    }

    public PetResponse getPetById(Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("Pet not found"));

        PetResponse dto = new PetResponse();
        dto.setId(pet.getId());
        dto.setName(pet.getName());
        dto.setAge(pet.getAge());
        dto.setImageUrl(pet.getImageUrl());

        return dto;
    }

    public List<PetResponse> getUserPets(Long ownerId) {
        List<Pet> pets = petRepository.findByUserIdAndIsDeletedFalse(ownerId);

        return pets.stream().map(p -> {
            PetResponse dto = new PetResponse();
            dto.setId(p.getId());
            dto.setName(p.getName());
            dto.setAge(p.getAge());
            dto.setImageUrl(p.getImageUrl());
            return dto;
        }).collect(Collectors.toList());
    }
}