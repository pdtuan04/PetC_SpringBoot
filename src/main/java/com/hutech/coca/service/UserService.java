package com.hutech.coca.service;

import com.hutech.coca.Role;
import com.hutech.coca.model.User;
import com.hutech.coca.repository.IRoleRepository;
import com.hutech.coca.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;

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
        var defaultRole = roleRepository.findRoleById(Role.USER.value);
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
}