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
        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        PetType petType = petTypeRepository.findById(dto.getPetTypeId())
                .orElseThrow(() -> new RuntimeException("Pet Type not found"));

        Pet pet = new Pet();
        pet.setUser(user);
        pet.setPetType(petType);
        pet.setName(dto.getName());
        pet.setAge(dto.getAge());
        pet.setImageUrl(dto.getImageUrl());

        petRepository.save(pet);

        PetResponse response = new PetResponse();
        response.setId(pet.getId());
        response.setName(pet.getName());
        response.setAge(pet.getAge());
        response.setImageUrl(pet.getImageUrl());
        if (pet.getPetType() != null) {
            response.setPetTypeId(pet.getPetType().getId());
        }

        return response;
    }

    @Transactional
    public PetResponse updateUserPet(Long userId, Long petId, CreatePetRequest dto) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("Pet not found"));

        if (pet.getUser() == null || !pet.getUser().getId().equals(userId)) {
            throw new RuntimeException("Pet does not belong to this user");
        }

        PetType petType = petTypeRepository.findById(dto.getPetTypeId())
                .orElseThrow(() -> new RuntimeException("Pet Type not found"));

        pet.setName(dto.getName());
        pet.setAge(dto.getAge());
        pet.setImageUrl(dto.getImageUrl());
        pet.setPetType(petType);

        petRepository.save(pet);

        PetResponse response = new PetResponse();
        response.setId(pet.getId());
        response.setName(pet.getName());
        response.setAge(pet.getAge());
        response.setImageUrl(pet.getImageUrl());
        if (pet.getPetType() != null) {
            response.setPetTypeId(pet.getPetType().getId());
        }

        return response;
    }

    @Transactional
    public void deleteUserPet(Long userId, Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("Pet not found"));

        if (pet.getUser() == null || !pet.getUser().getId().equals(userId)) {
            throw new RuntimeException("Pet does not belong to this user");
        }

        // Soft delete via @SQLDelete on Pet entity
        petRepository.delete(pet);
    }

    public PetResponse getPetById(Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("Pet not found"));

        PetResponse dto = new PetResponse();
        dto.setId(pet.getId());
        dto.setName(pet.getName());
        dto.setAge(pet.getAge());
        dto.setImageUrl(pet.getImageUrl());
        if (pet.getPetType() != null) {
            dto.setPetTypeId(pet.getPetType().getId());
        }

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
            if (p.getPetType() != null) {
                dto.setPetTypeId(p.getPetType().getId());
            }
            return dto;
        }).collect(Collectors.toList());
    }
}