package com.manh.partnerbridge.education.application.exception;

import com.manh.partnerbridge.education.domain.exception.CommonErrorCode;

public final class UnauthorizedException extends ApplicationException {
    public UnauthorizedException() { super(CommonErrorCode.UNAUTHORIZED, null); }
}
