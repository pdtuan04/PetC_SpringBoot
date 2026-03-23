package com.hutech.coca.service;

import com.hutech.coca.dto.CreateStaffResponse;
import com.hutech.coca.dto.StaffRequest;
import com.hutech.coca.dto.StaffResponse;
import com.hutech.coca.dto.StaffSummaryResponse;
import com.hutech.coca.model.Role;
import com.hutech.coca.model.Staff;
import com.hutech.coca.model.User;
import com.hutech.coca.repository.IRoleRepository;
import com.hutech.coca.repository.IStaffRepository;
import com.hutech.coca.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final IStaffRepository staffRepository;
    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public CreateStaffResponse createStaff(StaffRequest request) {
        // Validate email uniqueness
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }

        // Validate phone uniqueness in User table
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            if (userRepository.findByPhone(request.getPhoneNumber()).isPresent()) {
                throw new RuntimeException("Số điện thoại đã được sử dụng bởi tài khoản khác");
            }
        }

        // Validate phone uniqueness in Staff table
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty() &&
            staffRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new RuntimeException("Số điện thoại đã tồn tại");
        }

        // Generate staff code
        String staffCode = generateStaffCode();

        // Generate username from email
        String username = generateUsername(request.getEmail());
        
        // Check username uniqueness
        int counter = 1;
        String originalUsername = username;
        while (userRepository.findByUsername(username).isPresent()) {
            username = originalUsername + counter;
            counter++;
        }

        // Generate temporary password
        String temporaryPassword = generateTemporaryPassword();

        // Create User account - Try to find STAFF role, if not found use USER role
        Role staffRole = roleRepository.findByName("STAFF")
                .orElseGet(() -> {
                    System.err.println("WARNING: Role STAFF not found, using USER role instead");
                    return roleRepository.findByName("USER")
                            .orElseThrow(() -> new RuntimeException("Role USER không tồn tại"));
                });

        Set<Role> roles = new HashSet<>();
        roles.add(staffRole);

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(temporaryPassword))
                .email(request.getEmail())
                .phone(request.getPhoneNumber())
                .roles(roles)
                .rewardPoints(0)
                .build();

        user = userRepository.save(user);

        // Create Staff
        Staff staff = Staff.builder()
                .staffCode(staffCode)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .dateOfBirth(request.getDateOfBirth())
                .hireDate(request.getHireDate() != null ? request.getHireDate() : LocalDate.now())
                .department(request.getDepartment())
                .position(request.getPosition())
                .specialization(request.getSpecialization())
                .profilePictureUrl(request.getProfilePictureUrl())
                .isActive(true)
                .user(user)
                .build();

        staff = staffRepository.save(staff);

        // Send welcome email
        try {
            emailService.sendStaffWelcomeEmail(
                request.getEmail(),
                request.getFullName(),
                username,
                temporaryPassword,
                staffCode
            );
            System.out.println("Email queued for sending to: " + request.getEmail());
        } catch (Exception e) {
            // Log error but don't fail the transaction
            System.err.println("Failed to queue welcome email: " + e.getMessage());
            e.printStackTrace();
        }

        return CreateStaffResponse.builder()
                .staffId(staff.getId())
                .staffCode(staffCode)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .username(username)
                .temporaryPassword(temporaryPassword)
                .mustChangePassword(true)
                .build();
    }

    public Page<StaffSummaryResponse> getAllStaff(int pageNumber, int pageSize, String search, 
                                                   String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("Descending") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sort);

        Page<Staff> staffPage;
        if (search != null && !search.trim().isEmpty()) {
            staffPage = staffRepository.searchStaff(search.trim(), pageable);
        } else {
            staffPage = staffRepository.findAll(pageable);
        }

        return staffPage.map(this::mapToSummaryResponse);
    }

    public StaffResponse getStaffById(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        
        return mapToResponse(staff);
    }

    @Transactional
    public StaffResponse updateStaff(Long id, StaffRequest request) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        // Check email uniqueness if changed
        if (!staff.getEmail().equals(request.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new RuntimeException("Email đã tồn tại trong hệ thống");
            }
            staff.setEmail(request.getEmail());
            staff.getUser().setEmail(request.getEmail());
        }

        // Check phone uniqueness if changed
        if (request.getPhoneNumber() != null && 
            !request.getPhoneNumber().equals(staff.getPhoneNumber())) {
            if (staffRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
                throw new RuntimeException("Số điện thoại đã tồn tại");
            }
            staff.setPhoneNumber(request.getPhoneNumber());
            staff.getUser().setPhone(request.getPhoneNumber());
        }

        staff.setFullName(request.getFullName());
        staff.setAddress(request.getAddress());
        staff.setDateOfBirth(request.getDateOfBirth());
        staff.setHireDate(request.getHireDate());
        staff.setDepartment(request.getDepartment());
        staff.setPosition(request.getPosition());
        staff.setSpecialization(request.getSpecialization());
        staff.setProfilePictureUrl(request.getProfilePictureUrl());

        staff = staffRepository.save(staff);
        userRepository.save(staff.getUser());

        return mapToResponse(staff);
    }

    @Transactional
    public void toggleActive(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        
        staff.setIsActive(!staff.getIsActive());
        staffRepository.save(staff);
    }

    @Transactional
    public void deleteStaff(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        
        // Soft delete
        staffRepository.delete(staff);
    }

    public long countActiveStaff() {
        return staffRepository.countActiveStaff();
    }

    // Helper methods
    private String generateStaffCode() {
        String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yy"));
        long count = staffRepository.count() + 1;
        return String.format("STF%s%04d", year, count);
    }

    private String generateUsername(String email) {
        return email.substring(0, email.indexOf("@")).toLowerCase();
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            password.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return password.toString();
    }

    private StaffSummaryResponse mapToSummaryResponse(Staff staff) {
        return StaffSummaryResponse.builder()
                .id(staff.getId())
                .staffCode(staff.getStaffCode())
                .fullName(staff.getFullName())
                .email(staff.getEmail())
                .phoneNumber(staff.getPhoneNumber())
                .department(staff.getDepartment())
                .position(staff.getPosition())
                .profilePictureUrl(staff.getProfilePictureUrl())
                .isActive(staff.getIsActive())
                .build();
    }

    private StaffResponse mapToResponse(Staff staff) {
        return StaffResponse.builder()
                .id(staff.getId())
                .staffCode(staff.getStaffCode())
                .fullName(staff.getFullName())
                .email(staff.getEmail())
                .phoneNumber(staff.getPhoneNumber())
                .address(staff.getAddress())
                .dateOfBirth(staff.getDateOfBirth())
                .hireDate(staff.getHireDate())
                .department(staff.getDepartment())
                .position(staff.getPosition())
                .specialization(staff.getSpecialization())
                .profilePictureUrl(staff.getProfilePictureUrl())
                .isActive(staff.getIsActive())
                .username(staff.getUser() != null ? staff.getUser().getUsername() : null)
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .build();
    }
}
