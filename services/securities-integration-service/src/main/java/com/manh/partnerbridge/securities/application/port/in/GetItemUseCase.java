package com.manh.partnerbridge.securities.application.port.in;

import com.manh.partnerbridge.securities.domain.model.Item;

public interface GetItemUseCase { Item getItem(String itemId); }
