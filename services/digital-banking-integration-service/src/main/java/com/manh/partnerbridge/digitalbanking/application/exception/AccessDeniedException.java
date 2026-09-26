package com.manh.partnerbridge.digitalbanking.application.exception;

import com.manh.partnerbridge.digitalbanking.domain.exception.CommonErrorCode;

public final class AccessDeniedException extends ApplicationException {
    public AccessDeniedException() { super(CommonErrorCode.ACCESS_DENIED, null); }
}
