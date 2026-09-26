package com.manh.partnerbridge.education.application.exception;

import com.manh.partnerbridge.education.domain.exception.CommonErrorCode;

public final class AccessDeniedException extends ApplicationException {
    public AccessDeniedException() { super(CommonErrorCode.ACCESS_DENIED, null); }
}
