package com.rawello.maparserver.security;

public final class SecurityRoles {
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_OWNER = "ROLE_OWNER";
    public static final String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";

    private SecurityRoles() {
    }

    public static boolean hasOwnerRole(String role) {
        return ROLE_OWNER.equals(role);
    }

    public static boolean hasUserRole(String role) {
        return ROLE_USER.equals(role);
    }

    public static boolean isAnonymous(String role) {
        return ROLE_ANONYMOUS.equals(role);
    }
}
