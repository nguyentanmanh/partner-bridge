package com.manh.partnerbridge.digitalbanking.domain.exception;

public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException(ErrorCode errorCode) { super(errorCode); }
}
