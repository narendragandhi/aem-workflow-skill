package com.example.core.workflows;

class InvalidProposalException extends Exception {

    private static final long serialVersionUID = 1L;

    InvalidProposalException(String message) {
        super(message);
    }
}
