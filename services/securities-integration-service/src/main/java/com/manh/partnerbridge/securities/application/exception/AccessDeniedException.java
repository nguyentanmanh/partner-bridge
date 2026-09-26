package com.manh.partnerbridge.securities.application.exception;

import com.manh.partnerbridge.securities.domain.exception.CommonErrorCode;

public final class AccessDeniedException extends ApplicationException {
    public AccessDeniedException() { super(CommonErrorCode.ACCESS_DENIED, null); }
}
