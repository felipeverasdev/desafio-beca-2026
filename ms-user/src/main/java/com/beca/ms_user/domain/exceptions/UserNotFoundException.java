package com.beca.ms_user.domain.exceptions;

public class UserNotFoundException extends RuntimeException {
    private static  final Long serialVersionUID = 1L;

    public UserNotFoundException(String message) {
        super(message);
    }
}
