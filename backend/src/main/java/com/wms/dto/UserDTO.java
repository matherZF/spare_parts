package com.wms.dto;

import java.time.Instant;

public record UserDTO(Long id, String username, String displayName, String role, Boolean enabled, Instant createdAt) {
}
