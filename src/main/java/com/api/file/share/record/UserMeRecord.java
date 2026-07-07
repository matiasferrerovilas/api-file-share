package com.api.file.share.record;

public record UserMeRecord(
        Long id,
        String email,
        String givenName,
        String familyName,
        boolean isFirstLogin,
        String userType,
        boolean hasSeenTour
) { }
