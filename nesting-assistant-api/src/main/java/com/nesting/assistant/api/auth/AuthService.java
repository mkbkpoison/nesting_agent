package com.nesting.assistant.api.auth;

import com.nesting.assistant.common.util.IdGenerator;
import com.nesting.assistant.domain.entity.User;
import com.nesting.assistant.domain.repository.UserMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 认证服务
 * 注册、登录、Token管理
 */
@Slf4j
@Service
public class AuthService {

    private static final String TOKEN_KEY_PREFIX = "nesting:token:";
    private static final Duration TOKEN_TTL = Duration.ofDays(7);

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户注册
     */
    public Map<String, Object> register(String username, String password, String nickname) {
        // 检查用户名是否已存在
        User existing = userMapper.findByUsername(username);
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建用户
        String userId = "user_" + IdGenerator.uuid().substring(0, 12);
        User user = User.builder()
                .userId(userId)
                .username(username)
                .password(passwordEncoder.encode(password))
                .nickname(nickname != null && !nickname.isEmpty() ? nickname : username)
                .status("active")
                .build();
        userMapper.insert(user);

        // 生成Token
        String token = generateToken(userId);

        log.info("User registered: userId={}, username={}", userId, username);
        return Map.of(
                "userId", userId,
                "username", username,
                "nickname", user.getNickname(),
                "token", token
        );
    }

    /**
     * 用户登录
     */
    public Map<String, Object> login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!"active".equals(user.getStatus())) {
            throw new RuntimeException("账号已被禁用");
        }

        // 生成Token
        String token = generateToken(user.getUserId());

        log.info("User logged in: userId={}, username={}", user.getUserId(), username);
        return Map.of(
                "userId", user.getUserId(),
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "token", token
        );
    }

    /**
     * 验证Token并返回userId
     */
    public Optional<String> validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }
        String userId = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + token);
        return Optional.ofNullable(userId);
    }

    /**
     * 登出（删除Token）
     */
    public void logout(String token) {
        redisTemplate.delete(TOKEN_KEY_PREFIX + token);
        log.info("User logged out, token invalidated");
    }

    /**
     * 根据userId获取用户信息
     */
    public Optional<User> getUserById(String userId) {
        User user = userMapper.findByUserId(userId);
        return Optional.ofNullable(user);
    }

    private String generateToken(String userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token, userId, TOKEN_TTL);
        return token;
    }
}
