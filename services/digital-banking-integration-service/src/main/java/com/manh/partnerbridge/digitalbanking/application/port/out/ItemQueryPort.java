package com.manh.partnerbridge.digitalbanking.application.port.out;

import com.manh.partnerbridge.digitalbanking.domain.model.Item;
import java.util.Optional;

public interface ItemQueryPort { Optional<Item> findById(String itemId); }
