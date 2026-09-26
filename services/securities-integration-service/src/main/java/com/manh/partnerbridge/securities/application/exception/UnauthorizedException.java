package com.manh.partnerbridge.securities.application.exception;

import com.manh.partnerbridge.securities.domain.exception.CommonErrorCode;

public final class UnauthorizedException extends ApplicationException {
    public UnauthorizedException() { super(CommonErrorCode.UNAUTHORIZED, null); }
}
