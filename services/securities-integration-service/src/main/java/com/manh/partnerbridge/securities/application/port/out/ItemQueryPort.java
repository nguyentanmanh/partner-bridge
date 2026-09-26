package com.manh.partnerbridge.securities.application.port.out;

import com.manh.partnerbridge.securities.domain.model.Item;
import java.util.Optional;

public interface ItemQueryPort { Optional<Item> findById(String itemId); }
