package com.wms.service;

import com.wms.common.BizException;
import com.wms.dto.UserCreateReq;
import com.wms.dto.UserDTO;
import com.wms.dto.UserUpdateReq;
import com.wms.entity.User;
import com.wms.entity.UserRole;
import com.wms.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<UserDTO> page(String username, String displayName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        String u = StringUtils.hasText(username) ? username : "";
        String d = StringUtils.hasText(displayName) ? displayName : "";
        if (!u.isEmpty() || !d.isEmpty()) {
            return userRepo.findByUsernameContainingOrDisplayNameContaining(u, d, pageable).map(this::toDTO);
        }
        return userRepo.findAll(pageable).map(this::toDTO);
    }

    public UserDTO getById(Long id) {
        return userRepo.findById(id).map(this::toDTO)
                .orElseThrow(() -> new BizException("用户不存在"));
    }

    @Transactional
    public UserDTO create(UserCreateReq req) {
        if (userRepo.existsByUsername(req.username())) {
            throw new BizException("用户名已存在");
        }
        UserRole role;
        try {
            role = UserRole.valueOf(req.role());
        } catch (IllegalArgumentException e) {
            throw new BizException("角色无效");
        }
        User user = new User(
                req.username(),
                passwordEncoder.encode(req.password()),
                req.displayName(),
                role
        );
        userRepo.save(user);
        return toDTO(user);
    }

    @Transactional
    public UserDTO update(Long id, UserUpdateReq req) {
        User user = userRepo.findById(id).orElseThrow(() -> new BizException("用户不存在"));
        if (StringUtils.hasText(req.displayName())) {
            user.setDisplayName(req.displayName());
        }
        if (StringUtils.hasText(req.role())) {
            try {
                user.setRole(UserRole.valueOf(req.role()));
            } catch (IllegalArgumentException e) {
                throw new BizException("角色无效");
            }
        }
        if (req.enabled() != null) {
            user.setEnabled(req.enabled());
        }
        if (StringUtils.hasText(req.password())) {
            user.setPassword(passwordEncoder.encode(req.password()));
        }
        userRepo.save(user);
        return toDTO(user);
    }

    @Transactional
    public void delete(Long id, String currentUsername) {
        User user = userRepo.findById(id).orElseThrow(() -> new BizException("用户不存在"));
        if (user.getUsername().equals(currentUsername)) {
            throw new BizException("不能删除当前登录用户");
        }
        userRepo.deleteById(id);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepo.findById(id).orElseThrow(() -> new BizException("用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    public User findByUsername(String username) {
        return userRepo.findByUsername(username).orElse(null);
    }

    private UserDTO toDTO(User u) {
        return new UserDTO(
                u.getId(),
                u.getUsername(),
                u.getDisplayName(),
                u.getRole() == null ? null : u.getRole().name(),
                u.isEnabled(),
                u.getCreatedAt()
        );
    }
}
