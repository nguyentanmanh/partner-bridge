package com.manh.partnerbridge.banking.adapter.in.rest;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.manh.partnerbridge.banking.application.port.in.GetItemUseCase;
import com.manh.partnerbridge.banking.domain.model.Item;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ItemControllerTest {
    @Test void delegatesOnlyToInboundPort() throws Exception {
        GetItemUseCase useCase = mock(GetItemUseCase.class);
        when(useCase.getItem("item-1")).thenReturn(new Item("item-1", "Sample"));
        MockMvcBuilders.standaloneSetup(new ItemController(useCase)).build().perform(get("/api/v1/items/item-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("item-1"));
        verify(useCase).getItem("item-1");
    }
}
