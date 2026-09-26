package com.manh.partnerbridge.digitalbanking.adapter.in.rest;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.manh.partnerbridge.digitalbanking.bootstrap.Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Test void returnsEnglishNotFoundAndPropagatesRequestId() throws Exception {
        mvc.perform(get("/api/v1/items/missing").header("Accept-Language", "en").header("Request-ID", "REQ-123"))
                .andExpect(status().isNotFound()).andExpect(header().string("Request-ID", "REQ-123"))
                .andExpect(jsonPath("$.code").value("DBK-ITEM-001"))
                .andExpect(jsonPath("$.message").value("Item not found"))
                .andExpect(jsonPath("$.requestId").value("REQ-123"));
    }
    @Test void returnsVietnameseMessage() throws Exception {
        mvc.perform(get("/api/v1/items/missing").header("Accept-Language", "vi"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Không tìm thấy mặt hàng"));
    }
    @Test void unsupportedLanguageFallsBackToEnglishAndGeneratesRequestId() throws Exception {
        mvc.perform(get("/api/v1/items/missing").header("Accept-Language", "fr"))
                .andExpect(status().isNotFound()).andExpect(header().string("Request-ID", not(blankOrNullString())))
                .andExpect(jsonPath("$.message").value("Item not found"));
    }
}
