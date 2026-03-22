package com.hutech.coca.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.hutech.coca.model.Role;
import com.hutech.coca.model.User;
import com.hutech.coca.repository.IRoleRepository;
import com.hutech.coca.repository.IUserRepository;
import com.hutech.coca.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final JobScheduler jobScheduler;
    private final EmailService emailService;
    @Value("${GOOGLE_CLIENT_ID}")
    private String googleClientId;

    @Transactional
    public Map<String, Object> authenticateWithGoogle(String googleToken) throws Exception {
        // 1. Xác minh Token Google gửi lên có hợp lệ không
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(googleToken);
        if (idToken == null) {
            throw new RuntimeException("Token Google không hợp lệ hoặc đã hết hạn.");
        }

        // 2. Bóc tách thông tin từ Google
        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();

        // 3. Tìm hoặc tạo mới User trong Database
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(email); // Lấy thẳng email làm username
            newUser.setProvider("GOOGLE");
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Pass ngẫu nhiên
            // Không gán phone -> Sẽ lưu null trong DB
            newUser.setRewardPoints(0);

            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy Role USER"));
            newUser.getRoles().add(userRole);
            var result =userRepository.save(newUser);
            jobScheduler.enqueue(() -> emailService.sendWelcome(newUser.getEmail(),newUser.getUsername()));
            return result;
        });

        // Nếu user cũ từng đăng ký bằng form, nay login bằng Google thì update provider
        if (user.getProvider() == null) {
            user.setProvider("GOOGLE");
            user = userRepository.save(user);
        }

        // 4. Sinh JWT Token bằng JwtUtils
        String jwtToken = jwtUtils.generateToken(user.getUsername(), user.getAuthorities());

        // 5. Đóng gói dữ liệu trả về cho Controller
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("token", jwtToken);
        responseData.put("userId", user.getId());
        responseData.put("username", user.getUsername());
        responseData.put("email", user.getEmail());

        return responseData;
    }
}