package com.ticket.identity.application.usecase;

public interface AdminDeleteUserUseCase {
    void execute(Long id, String currentAdminUsername);
}
