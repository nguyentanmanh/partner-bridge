package com.manh.partnerbridge.banking.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import com.manh.partnerbridge.banking.domain.model.Item;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InMemoryItemAdapterTest {
    @Test void findsConfiguredItemAndReturnsEmptyForUnknownId() {
        var adapter = new InMemoryItemAdapter(Map.of("x", new Item("x", "X")));
        assertEquals("X", adapter.findById("x").orElseThrow().name());
        assertTrue(adapter.findById("y").isEmpty());
    }
}
