package com.manh.partnerbridge.banking.application.port.out;

import com.manh.partnerbridge.banking.domain.model.Item;
import java.util.Optional;

public interface ItemQueryPort { Optional<Item> findById(String itemId); }
