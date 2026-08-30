package com.morel.greenhouse.application.service;

public final class DefaultAvatarResolver {
    public static final String MALE_ADMIN = "/avatars/male_admin.png";
    public static final String FEMALE_ADMIN = "/avatars/female_admin.png";
    public static final String MALE_FARMER = "/avatars/male_farmer.jpg";
    public static final String FEMALE_FARMER = "/avatars/female_farmer.png";

    private DefaultAvatarResolver() {
    }

    public static String resolve(String roleCode, String gender) {
        boolean female = "FEMALE".equalsIgnoreCase(nullToEmpty(gender));
        if ("ADMIN".equalsIgnoreCase(nullToEmpty(roleCode))) {
            return female ? FEMALE_ADMIN : MALE_ADMIN;
        }
        return female ? FEMALE_FARMER : MALE_FARMER;
    }

    public static String defaultIfBlank(String avatarUrl, String roleCode, String gender) {
        return avatarUrl == null || avatarUrl.isBlank() ? resolve(roleCode, gender) : avatarUrl;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
