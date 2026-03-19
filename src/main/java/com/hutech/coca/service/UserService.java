package com.hutech.coca.service;
import com.hutech.coca.Role;
import com.hutech.coca.model.User;
import com.hutech.coca.repository.IRoleRepository;
import com.hutech.coca.repository.IUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class UserService implements UserDetailsService {

    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private IRoleRepository roleRepository;

    // Lưu người dùng mới vào cơ sở dữ liệu sau khi mã hóa mật khẩu.
    public void save(@NotNull User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }else {

        }
        user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
        userRepository.save(user);
    }

    // Gán vai trò mặc định cho người dùng dựa trên tên người dùng.
    public void setDefaultRole(String username) {
        userRepository.findByUsername(username).ifPresentOrElse(
                user -> {
                    if (user.getRoles() == null) {
                        user.setRoles(new HashSet<>());
                    }

                    var role = roleRepository.findRoleById(Role.USER.value);
                    if (role != null) {
                        user.getRoles().add(role);
                        userRepository.save(user);
                    } else {
                        log.error("Role USER không tồn tại trong Database!");
                    }
                },
                () -> { throw new UsernameNotFoundException("User not found"); }
        );
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws
            UsernameNotFoundException {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getAuthorities())
                .accountExpired(!user.isAccountNonExpired())
                .accountLocked(!user.isAccountNonLocked())
                .credentialsExpired(!user.isCredentialsNonExpired())
                .disabled(!user.isEnabled())
                .build();
    }

    // Tìm kiếm người dùng dựa trên tên đăng nhập.
    public Optional<User> findByUsername(String username) throws
            UsernameNotFoundException {
        return userRepository.findByUsername(username);
    }
}

