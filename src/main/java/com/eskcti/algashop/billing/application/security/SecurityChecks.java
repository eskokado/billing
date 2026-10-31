package com.eskcti.algashop.billing.application.security;

import java.util.UUID;

public interface SecurityChecks {
    UUID getAuthenticatedUserId();

    boolean isAuthenticated();

    boolean isMachineAuthenticated();
}
