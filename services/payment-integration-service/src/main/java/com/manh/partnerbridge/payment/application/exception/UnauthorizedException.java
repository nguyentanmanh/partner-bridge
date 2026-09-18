package com.manh.partnerbridge.payment.application.exception;

import com.manh.partnerbridge.payment.domain.exception.CommonErrorCode;

public final class UnauthorizedException extends ApplicationException {
    public UnauthorizedException() {
        super(CommonErrorCode.UNAUTHORIZED, null);
    }
}
