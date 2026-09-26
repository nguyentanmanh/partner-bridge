package com.manh.partnerbridge.securities.domain.exception;

public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException(ErrorCode errorCode) { super(errorCode); }
}
