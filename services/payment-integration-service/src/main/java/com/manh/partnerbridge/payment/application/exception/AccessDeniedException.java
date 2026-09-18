package com.manh.partnerbridge.payment.application.exception;

import com.manh.partnerbridge.payment.domain.exception.CommonErrorCode;

public final class AccessDeniedException extends ApplicationException {
    public AccessDeniedException() {
        super(CommonErrorCode.ACCESS_DENIED, null);
    }
}
