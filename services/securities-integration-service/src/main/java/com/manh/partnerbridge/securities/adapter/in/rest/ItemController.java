package com.manh.partnerbridge.securities.adapter.in.rest;

import com.manh.partnerbridge.securities.application.port.in.GetItemUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/items")
public class ItemController {
    private final GetItemUseCase getItemUseCase;
    public ItemController(GetItemUseCase getItemUseCase) { this.getItemUseCase = getItemUseCase; }
    @Operation(summary = "Get an item by id")
    @GetMapping("/{itemId}")
    public ResponseEntity<ItemResponse> getItem(
            @PathVariable @NotBlank(message = "{validation.item-id.not-blank}") String itemId) {
        return ResponseEntity.ok(ItemResponse.from(getItemUseCase.getItem(itemId)));
    }
}
