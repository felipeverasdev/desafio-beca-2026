package com.beca.ms_transaction.application.ports.out;

import java.util.UUID;

public interface UserValidationPort {
    boolean existsAndActive(UUID userId);
}

