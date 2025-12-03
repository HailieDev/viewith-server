package com.puremadeleine.viewith.dto.venue;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VenueListResDto {
    List<VenueResDto> venues;

    @Getter
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class VenueResDto {
        long venueId;
        String venueName;
        String venueLocation;
        List<Performance> performances;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Performance {
        String artist;
        String imageUrl;
    }
}
