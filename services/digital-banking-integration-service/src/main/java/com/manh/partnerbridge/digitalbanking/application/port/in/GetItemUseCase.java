package com.manh.partnerbridge.digitalbanking.application.port.in;

import com.manh.partnerbridge.digitalbanking.domain.model.Item;

public interface GetItemUseCase { Item getItem(String itemId); }
