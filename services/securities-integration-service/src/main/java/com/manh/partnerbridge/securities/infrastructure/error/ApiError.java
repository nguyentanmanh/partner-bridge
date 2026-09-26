package com.manh.partnerbridge.securities.infrastructure.error;

import java.time.Instant;
import java.util.List;

public record ApiError(String code, String message, String traceId, String requestId,
                       Instant timestamp, List<ApiErrorDetail> details) { }
