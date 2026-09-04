package com.wms.controller;

import com.wms.common.BizException;
import com.wms.common.Result;
import com.wms.config.JwtUtil;
import com.wms.dto.LoginReq;
import com.wms.dto.LoginRes;
import com.wms.dto.UserDTO;
import com.wms.entity.User;
import com.wms.repository.UserRepository;
import com.wms.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository,
                          UserService userService,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Result<LoginRes> login(@Valid @RequestBody LoginReq req) {
        User user = userRepository.findByUsername(req.username()).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            return Result.fail("用户名或密码错误");
        }
        if (!user.isEnabled()) {
            return Result.fail("账号已禁用");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        LoginRes res = new LoginRes(token, user.getId(), user.getUsername(), user.getDisplayName(), user.getRole().name());
        return Result.ok(res);
    }

    @GetMapping("/me")
    public Result<UserDTO> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BizException("未登录");
        }
        String username = (String) auth.getPrincipal();
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        UserDTO dto = new UserDTO(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getRole().name(), user.isEnabled(), user.getCreatedAt());
        return Result.ok(dto);
    }
}
