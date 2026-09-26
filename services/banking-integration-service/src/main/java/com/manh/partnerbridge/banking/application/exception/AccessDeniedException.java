package com.manh.partnerbridge.banking.application.exception;

import com.manh.partnerbridge.banking.domain.exception.CommonErrorCode;

public final class AccessDeniedException extends ApplicationException {
    public AccessDeniedException() { super(CommonErrorCode.ACCESS_DENIED, null); }
}
