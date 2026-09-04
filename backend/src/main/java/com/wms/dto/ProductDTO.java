package com.wms.dto;

import java.time.Instant;

public record ProductDTO(Long id, String sku, String name, String spec, String unit, Instant createdAt) {
}
