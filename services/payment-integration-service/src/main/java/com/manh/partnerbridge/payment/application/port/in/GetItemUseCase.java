package com.manh.partnerbridge.payment.application.port.in;

import com.manh.partnerbridge.payment.domain.model.Item;

public interface GetItemUseCase {
    Item getItem(String itemId);
}
