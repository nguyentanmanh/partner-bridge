package com.manh.partnerbridge.payment.application.port.out;

import com.manh.partnerbridge.payment.domain.model.Item;

import java.util.Optional;

public interface ItemQueryPort {
    Optional<Item> findById(String itemId);
}
