package com.hutech.coca.service;
import com.hutech.coca.common.Roles;
import com.hutech.coca.dto.UpdateRoleRequest;
import com.hutech.coca.dto.UpdateProfileRequest;
import com.hutech.coca.dto.UserProfileResponse;
import com.hutech.coca.dto.UserSummaryResponse;
import com.hutech.coca.model.Role;
import com.hutech.coca.model.User;
import com.hutech.coca.repository.IRoleRepository;
import com.hutech.coca.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JobScheduler jobScheduler;
    private final EmailService emailService;
    @Transactional
    public void registerNewUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }
        if (userRepository.findByPhone(user.getPhone()).isPresent()) {
            throw new RuntimeException("SDT đã tồn tại!");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại!");
        }
        // Mã hóa mật khẩu
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Khởi tạo roles tránh NullPointerException
        if (user.getRoles() == null) user.setRoles(new HashSet<>());

        // Gán Role mặc định (USER)
        var defaultRole = roleRepository.findRoleById(Roles.USER.value);
        user.getRoles().add(defaultRole);

        userRepository.save(user);
        jobScheduler.enqueue(() -> emailService.sendWelcome(user.getEmail(),user.getUsername()));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng"));
    }
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    public UserSummaryResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với email này"));

        UserSummaryResponse dto = new UserSummaryResponse();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        return dto;
    }

    // Hàm phụ: Lấy tất cả Roles để Frontend hiển thị ra checkbox
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }
    @Transactional
    public void updateUserRoles(Long userId, UpdateRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (request.getRoleId() == null) {
            throw new RuntimeException("Vui lòng chọn một quyền cho người dùng.");
        }
        Role newRole = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Quyền không tồn tại."));
        Set<Role> singleRoleSet = new HashSet<>();
        singleRoleSet.add(newRole);
        user.setRoles(singleRoleSet);

        userRepository.save(user);
    }
    public List<Map<String, Object>> getAllRolesClean() {
        return roleRepository.findAll().stream().map(role -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", role.getId());
            map.put("name", role.getName());
            map.put("description", role.getDescription());
            return map;
        }).collect(Collectors.toList());
    }
    public List<UserSummaryResponse> getAllUsers() {
        List<User> users = userRepository.findAll();

        return users.stream().map(user -> {
            UserSummaryResponse dto = new UserSummaryResponse();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setPhone(user.getPhone());
            dto.setEmail(user.getEmail());

            // XỬ LÝ LẤY ROLES CHO FRONTEND HIỂN THỊ BẢNG
            if (user.getRoles() != null) {
                List<Map<String, Object>> roleList = user.getRoles().stream().map(r -> {
                    Map<String, Object> roleMap = new HashMap<>();
                    roleMap.put("id", r.getId());
                    roleMap.put("name", r.getName());
                    return roleMap;
                }).collect(Collectors.toList());
                dto.setRoles(roleList); // Set vào DTO
            }

            return dto;
        }).collect(Collectors.toList());
    }
    public UserProfileResponse toUserProfile(User user) {
        UserProfileResponse dto = new UserProfileResponse();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRewardPoints(user.getRewardPoints());
        return dto;
    }

    @Transactional
    public UserProfileResponse updateMyProfile(User currentUser, UpdateProfileRequest request) {
        String normalizedEmail = request.getEmail() == null ? null : request.getEmail().trim();
        String normalizedPhone = request.getPhone() == null ? null : request.getPhone().trim();

        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            throw new RuntimeException("Email không được để trống");
        }

        if (normalizedPhone == null || normalizedPhone.isEmpty()) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }

        userRepository.findByEmail(normalizedEmail)
                .filter(found -> !found.getId().equals(currentUser.getId()))
                .ifPresent(found -> {
                    throw new RuntimeException("Email đã tồn tại!");
                });

        userRepository.findByPhone(normalizedPhone)
                .filter(found -> !found.getId().equals(currentUser.getId()))
                .ifPresent(found -> {
                    throw new RuntimeException("SDT đã tồn tại!");
                });

        currentUser.setEmail(normalizedEmail);
        currentUser.setPhone(normalizedPhone);
        userRepository.save(currentUser);
        return toUserProfile(currentUser);
    }
}