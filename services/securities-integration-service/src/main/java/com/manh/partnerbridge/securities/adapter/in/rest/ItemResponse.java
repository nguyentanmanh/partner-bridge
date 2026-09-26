package com.manh.partnerbridge.securities.adapter.in.rest;

import com.manh.partnerbridge.securities.domain.model.Item;

public record ItemResponse(String id, String name) {
    static ItemResponse from(Item item) { return new ItemResponse(item.id(), item.name()); }
}
