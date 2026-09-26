package com.manh.partnerbridge.digitalbanking.application.usecase;

import com.manh.partnerbridge.digitalbanking.application.port.in.GetItemUseCase;
import com.manh.partnerbridge.digitalbanking.application.port.out.ItemQueryPort;
import com.manh.partnerbridge.digitalbanking.domain.exception.ItemErrorCode;
import com.manh.partnerbridge.digitalbanking.domain.exception.ResourceNotFoundException;
import com.manh.partnerbridge.digitalbanking.domain.model.Item;

public final class GetItemService implements GetItemUseCase {
    private final ItemQueryPort itemQueryPort;
    public GetItemService(ItemQueryPort itemQueryPort) { this.itemQueryPort = itemQueryPort; }
    @Override public Item getItem(String itemId) {
        return itemQueryPort.findById(itemId).orElseThrow(() -> new ResourceNotFoundException(ItemErrorCode.NOT_FOUND));
    }
}
