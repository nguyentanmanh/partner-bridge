package com.manh.partnerbridge.education.domain.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ItemTest {
    @Test void rejectsBlankId() { assertThrows(IllegalArgumentException.class, () -> new Item(" ", "name")); }
    @Test void exposesImmutableValues() { assertEquals("name", new Item("1", "name").name()); }
}
