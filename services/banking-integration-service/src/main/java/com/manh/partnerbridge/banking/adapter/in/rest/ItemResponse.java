package com.manh.partnerbridge.banking.adapter.in.rest;

import com.manh.partnerbridge.banking.domain.model.Item;

public record ItemResponse(String id, String name) {
    static ItemResponse from(Item item) { return new ItemResponse(item.id(), item.name()); }
}
