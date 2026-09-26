package com.manh.partnerbridge.education.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.manh.partnerbridge.education.application.port.out.ItemQueryPort;
import com.manh.partnerbridge.education.domain.exception.ResourceNotFoundException;
import com.manh.partnerbridge.education.domain.model.Item;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetItemServiceTest {
    private final ItemQueryPort port = mock(ItemQueryPort.class);
    private final GetItemService service = new GetItemService(port);
    @Test void returnsItemFromPort() {
        when(port.findById("1")).thenReturn(Optional.of(new Item("1", "One")));
        assertEquals("One", service.getItem("1").name());
    }
    @Test void translatesEmptyResultToDomainException() {
        when(port.findById("missing")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getItem("missing"));
    }
}
