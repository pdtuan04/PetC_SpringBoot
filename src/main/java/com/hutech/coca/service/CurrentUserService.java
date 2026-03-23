package com.hutech.coca.service;

import com.hutech.coca.model.User;
import com.hutech.coca.repository.IUserRepository;
import com.hutech.coca.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final JwtUtils jwtUtils;
    private final IUserRepository userRepository;

    public User getCurrentUser(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authorizationHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            throw new RuntimeException("Invalid or expired token");
        }

        String username = jwtUtils.getUsernameFromToken(token);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
