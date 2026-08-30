package com.morel.greenhouse.application.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultAvatarResolverTest {

    @Test
    void resolveReturnsAdminAvatarByGender() {
        assertEquals(DefaultAvatarResolver.MALE_ADMIN, DefaultAvatarResolver.resolve("ADMIN", "MALE"));
        assertEquals(DefaultAvatarResolver.FEMALE_ADMIN, DefaultAvatarResolver.resolve("ADMIN", "FEMALE"));
    }

    @Test
    void resolveDefaultsUnknownRoleToFarmerAvatar() {
        assertEquals(DefaultAvatarResolver.MALE_FARMER, DefaultAvatarResolver.resolve(null, null));
        assertEquals(DefaultAvatarResolver.FEMALE_FARMER, DefaultAvatarResolver.resolve("FARMER", "female"));
    }

    @Test
    void defaultIfBlankKeepsCustomAvatarAndFillsBlankValue() {
        assertEquals("/avatars/custom.png", DefaultAvatarResolver.defaultIfBlank("/avatars/custom.png", "ADMIN", "FEMALE"));
        assertEquals(DefaultAvatarResolver.MALE_FARMER, DefaultAvatarResolver.defaultIfBlank(" ", "FARMER", "MALE"));
        assertEquals(DefaultAvatarResolver.FEMALE_ADMIN, DefaultAvatarResolver.defaultIfBlank(null, "ADMIN", "FEMALE"));
    }
}
