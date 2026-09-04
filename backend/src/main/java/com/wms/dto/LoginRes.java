package com.wms.dto;

public record LoginRes(String token, Long userId, String username, String displayName, String role) {
}
