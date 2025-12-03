package com.puremadeleine.viewith.dto.member;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.extern.jackson.Jacksonized;

@Getter
@Value
@Jacksonized
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileResDto {
    Long memberId;
    String nickname;
    Long writtenReviewsCount;
    Long bookmarksCount;
    String profileImageUrl;
}
