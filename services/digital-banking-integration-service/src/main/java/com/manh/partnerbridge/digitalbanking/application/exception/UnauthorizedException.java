package com.manh.partnerbridge.digitalbanking.application.exception;

import com.manh.partnerbridge.digitalbanking.domain.exception.CommonErrorCode;

public final class UnauthorizedException extends ApplicationException {
    public UnauthorizedException() { super(CommonErrorCode.UNAUTHORIZED, null); }
}
