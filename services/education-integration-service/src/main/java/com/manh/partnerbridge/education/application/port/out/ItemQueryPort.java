package com.manh.partnerbridge.education.application.port.out;

import com.manh.partnerbridge.education.domain.model.Item;
import java.util.Optional;

public interface ItemQueryPort { Optional<Item> findById(String itemId); }
