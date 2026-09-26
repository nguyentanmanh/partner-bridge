package com.manh.partnerbridge.securities.domain.model;

import java.util.Objects;

public record Item(String id, String name) {
    public Item {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        Objects.requireNonNull(name, "name must not be null");
    }
}
