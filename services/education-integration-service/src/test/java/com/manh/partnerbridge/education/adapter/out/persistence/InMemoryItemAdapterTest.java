package com.manh.partnerbridge.education.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import com.manh.partnerbridge.education.domain.model.Item;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InMemoryItemAdapterTest {
    @Test void findsConfiguredItemAndReturnsEmptyForUnknownId() {
        var adapter = new InMemoryItemAdapter(Map.of("x", new Item("x", "X")));
        assertEquals("X", adapter.findById("x").orElseThrow().name());
        assertTrue(adapter.findById("y").isEmpty());
    }
}
