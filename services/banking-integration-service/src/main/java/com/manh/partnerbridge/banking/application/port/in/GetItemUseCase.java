package com.manh.partnerbridge.banking.application.port.in;

import com.manh.partnerbridge.banking.domain.model.Item;

public interface GetItemUseCase { Item getItem(String itemId); }
