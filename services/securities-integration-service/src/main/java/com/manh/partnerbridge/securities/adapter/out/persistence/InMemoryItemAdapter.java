package com.manh.partnerbridge.securities.adapter.out.persistence;

import com.manh.partnerbridge.securities.application.port.out.ItemQueryPort;
import com.manh.partnerbridge.securities.domain.model.Item;
import java.util.Map;
import java.util.Optional;

public final class InMemoryItemAdapter implements ItemQueryPort {
    private final Map<String, Item> items;
    public InMemoryItemAdapter() { this(Map.of("item-1", new Item("item-1", "Sample item"))); }
    public InMemoryItemAdapter(Map<String, Item> items) { this.items = Map.copyOf(items); }
    @Override public Optional<Item> findById(String itemId) { return Optional.ofNullable(items.get(itemId)); }
}
