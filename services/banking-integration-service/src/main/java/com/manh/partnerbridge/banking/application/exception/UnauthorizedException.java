package com.manh.partnerbridge.banking.application.exception;

import com.manh.partnerbridge.banking.domain.exception.CommonErrorCode;

public final class UnauthorizedException extends ApplicationException {
    public UnauthorizedException() { super(CommonErrorCode.UNAUTHORIZED, null); }
}
