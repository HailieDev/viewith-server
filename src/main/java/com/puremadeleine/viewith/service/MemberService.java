package com.puremadeleine.viewith.service;

import com.puremadeleine.viewith.aware.SpringProxyAware;
import com.puremadeleine.viewith.constants.NicknameConstants;
import com.puremadeleine.viewith.converter.CommonConverter;
import com.puremadeleine.viewith.domain.bookmark.BookmarkEntity;
import com.puremadeleine.viewith.domain.member.MemberEntity;
import com.puremadeleine.viewith.domain.review.ReviewEntity;
import com.puremadeleine.viewith.domain.venue.VenueEntity;
import com.puremadeleine.viewith.dto.client.KakaoUserInfoResDto;
import com.puremadeleine.viewith.dto.client.UpdateTokenResDto;
import com.puremadeleine.viewith.dto.common.SortType;
import com.puremadeleine.viewith.dto.member.BookmarkResDto;
import com.puremadeleine.viewith.dto.member.JoinResDto;
import com.puremadeleine.viewith.dto.member.MemberInfo;
import com.puremadeleine.viewith.dto.member.ProfileResDto;
import com.puremadeleine.viewith.dto.member.RefreshReqDto;
import com.puremadeleine.viewith.dto.member.RefreshResDto;
import com.puremadeleine.viewith.dto.member.ValidateNicknameResDto;
import com.puremadeleine.viewith.dto.review.ReviewWithSeatIdDto;
import com.puremadeleine.viewith.dto.review.request.ReviewListReqDto;
import com.puremadeleine.viewith.dto.review.response.ReviewListResDto;
import com.puremadeleine.viewith.exception.ViewithErrorCode;
import com.puremadeleine.viewith.exception.ViewithException;
import com.puremadeleine.viewith.provider.BookmarkProvider;
import com.puremadeleine.viewith.provider.MemberProvider;
import com.puremadeleine.viewith.provider.ReviewProvider;
import com.puremadeleine.viewith.provider.VenueProvider;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import static com.puremadeleine.viewith.constants.SeatConstants.UNSELECTED_STRING;
import static com.puremadeleine.viewith.converter.review.ReviewServiceConverter.toReviewListResDto;
import static com.puremadeleine.viewith.domain.member.MemberEntity.createAppleMember;
import static com.puremadeleine.viewith.domain.member.MemberEntity.createKakaoMember;
import static com.puremadeleine.viewith.dto.member.OAuthType.APPLE;
import static com.puremadeleine.viewith.dto.member.OAuthType.KAKAO;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MemberService extends SpringProxyAware<MemberService> {

    KakaoService kakaoService;
    AppleService appleService;
    JwtService jwtService;
    ImageService imageService;
    ProfileImageService profileImageService;
    MemberProvider memberProvider;
    BookmarkProvider bookmarkProvider;
    ReviewProvider reviewProvider;
    VenueProvider venueProvider;

    @Transactional
    public JoinResDto loginByKakao(String oauthAccessToken, String oauthRefreshToken) {
        // Kakao 인증 및 유저 정보 조회
        KakaoUserInfoResDto tokenInfo = kakaoService.getAccessTokenInfo(oauthAccessToken);

        // DB 정보 조회 및 handle
        Optional<MemberEntity> optionalMember = memberProvider.findMemberByKakaoId(tokenInfo.getId());
        MemberEntity member = optionalMember.orElseGet(() -> createAndSaveKakaoMember(tokenInfo.getId()));

        // token 생성
        MemberInfo memberInfo = MemberInfo.builder()
                .authType(KAKAO)
                .memberId(member.getId())
                .accessToken(oauthAccessToken)
                .refreshToken(oauthRefreshToken)
                .build();
        String accessToken = jwtService.makeAccessToken(memberInfo);
        String refreshToken = jwtService.makeRefreshToken(memberInfo);

        return JoinResDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .nickname(member.getNickname())
                .build();
    }

    public JoinResDto loginByApple(String authCode, String idToken) {
        // Apple 인증 및 유저 정보 조회
        String clientSub = appleService.validateAppleOAuthAndGetSub(idToken);
        UpdateTokenResDto tokenInfo = appleService.validateAuthCode(authCode);

        // userId로 정보 조회해서 저장하기
        String appleSub = appleService.validateAppleOAuthAndGetSub(tokenInfo.getIdToken());
        if (!StringUtils.equals(clientSub, appleSub)) {
            throw new ViewithException(ViewithErrorCode.INVALID_OAUTH_TOKEN);
        }

        // DB 정보 조회 및 handle
        Optional<MemberEntity> optionalMember = memberProvider.findMemberByAppleId(appleSub);
        MemberEntity member = optionalMember.orElseGet(() -> createAndSaveAppleMember(appleSub));

        // token 생성
        MemberInfo memberInfo = MemberInfo.builder()
                .authType(APPLE)
                .memberId(member.getId())
                .accessToken(tokenInfo.getAccessToken())
                .refreshToken(tokenInfo.getRefreshToken())
                .build();
        String accessToken = jwtService.makeAccessToken(memberInfo);
        String refreshToken = jwtService.makeRefreshToken(memberInfo);

        return JoinResDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .nickname(member.getNickname())
                .build();
    }

    public ProfileResDto getProfile(Long memberId) {
        MemberEntity member = memberProvider.findActiveMember(memberId)
                .orElseThrow(() -> new ViewithException(ViewithErrorCode.UNKNOWN_EXCEPTION));
        long bookmarksCnt = bookmarkProvider.countByMemberId(member.getId());
        long reviewsCnt = reviewProvider.countByMemberId(member.getId());

        return ProfileResDto.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .bookmarksCount(bookmarksCnt)
                .writtenReviewsCount(reviewsCnt)
                .profileImageUrl(member.getProfileImage().getImageUrl())
                .build();
    }

    public void putNickname(MemberInfo member, String nickname) {
        if (!StringUtils.equals(member.getNickname(), nickname)) {
            validateNickname(nickname);
            MemberEntity activeMember = memberProvider.getActiveMember(member.getMemberId());
            activeMember.updateNickname(nickname);
            memberProvider.saveMember(activeMember);
        }
    }

    public ValidateNicknameResDto validateNickname(String nickname) {
        if (!nickname.matches(NicknameConstants.PATTERN)) {
            throw new ViewithException(ViewithErrorCode.INVALID_NICKNAME_FORMAT);
        }

        if (!memberProvider.isNicknameUnique(nickname)) {
            throw new ViewithException(ViewithErrorCode.DUPLICATED_NICKNAME);
        }

        return ValidateNicknameResDto.builder().isValidated(true).build();
    }

    private MemberEntity createAndSaveKakaoMember(long oauthMemberId) {
        MemberEntity newMember = createKakaoMember(oauthMemberId, makeRandomNickname(), profileImageService.getRandom());
        return memberProvider.save(newMember);
    }

    private MemberEntity createAndSaveAppleMember(String oauthMemberId) {
        MemberEntity newMember = createAppleMember(oauthMemberId, makeRandomNickname(), profileImageService.getRandom());
        return memberProvider.save(newMember);
    }

    private String makeRandomNickname() {
        String nickname = makeNickname();
        while (!memberProvider.isNicknameUnique(nickname)) {
            nickname = makeNickname();
        }
        return nickname;
    }

    private static String makeNickname() {
        // 수식어(최대 4글자) + 띄어쓰기 + 동물(최대 5글자) + 랜덤 숫자
        String nickname = NicknameConstants.PREFIXES[getRandomNumber(NicknameConstants.PREFIXES.length)]
                + NicknameConstants.MIDDLE
                + NicknameConstants.ANIMALS[getRandomNumber(NicknameConstants.ANIMALS.length)]
                + getRandomNumber(9999);
        int max = Integer.parseInt(NicknameConstants.MAX);
        nickname = nickname.length() > max ? nickname.substring(0, max) : nickname;
        return nickname;
    }

    private static int getRandomNumber(int size) {
        return new Random().nextInt(size);
    }

    public void withdraw(MemberInfo memberInfo) {
        switch (memberInfo.getAuthType()) {
            case KAKAO -> getProxy().withdrawByKakao(memberInfo);
            case APPLE -> getProxy().withdrawByApple(memberInfo);
            default -> throw new ViewithException(ViewithErrorCode.INVALID_PARAM);
        }
    }

    @Transactional
    public void withdrawByApple(MemberInfo memberInfo) {
        memberProvider.delete(memberInfo.getMemberId());
        appleService.revoke(memberInfo.getRefreshToken());
    }

    @Transactional
    public void withdrawByKakao(MemberInfo memberInfo) {
        memberProvider.delete(memberInfo.getMemberId());
        kakaoService.unlink(memberInfo.getAccessToken());
    }

    public RefreshResDto refresh(RefreshReqDto refreshReqDto) {
        boolean isValidRefreshToken = jwtService.validateRefreshToken(refreshReqDto.getRefreshToken());

        if (!isValidRefreshToken) {
            throw new ViewithException(ViewithErrorCode.INVALID_TOKEN);
        }

        MemberInfo memberInfo = jwtService.getMemberInfoByRefreshToken(refreshReqDto.getRefreshToken());

        UpdateTokenResDto newTokenInfo = switch (memberInfo.getAuthType()) {
            case KAKAO -> kakaoService.updateAccessToken(memberInfo.getRefreshToken());
            case APPLE -> appleService.updateAccessToken(memberInfo.getRefreshToken());
            default -> throw new ViewithException(ViewithErrorCode.INVALID_PARAM);
        };

        MemberInfo newMember = MemberInfo.builder()
                .authType(memberInfo.getAuthType())
                .memberId(memberInfo.getMemberId())
                .accessToken(newTokenInfo.getAccessToken())
                .refreshToken(newTokenInfo.getRefreshToken())
                .build();

        String accessToken = jwtService.makeAccessToken(newMember);
        String refreshToken = jwtService.makeRefreshToken(newMember);

        return RefreshResDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public BookmarkResDto getBookmarks(MemberInfo memberInfo) {
        List<VenueEntity> venues = venueProvider.getVenues();
        Long memberId = memberInfo.getMemberId();
        List<BookmarkResDto.BookmarkDto> bookmarkDtos = venues.stream()
                .map(v -> getBookmarkDto(v, memberId))
                .toList();

        return BookmarkResDto.builder()
                .bookmarks(bookmarkDtos)
                .build();
    }

    private BookmarkResDto.BookmarkDto getBookmarkDto(VenueEntity v, Long memberId) {
        List<BookmarkEntity> bookmarks = bookmarkProvider.getBookmarksByVenueIdAndMemberId(v.getId(), memberId);
        List<Long> bookmarkSeatIds = bookmarks.stream()
                .map(b -> b.getSeat().getId())
                .toList();
        Map<Long, LocalDateTime> reviewLastCreateTimeBySeatId = getReviewLastCreateTimeBySeatIds(bookmarkSeatIds);

        List<BookmarkResDto.BookmarkFloorDto> bookmarkFloorDtos = mapToBookmarkFloorDto(bookmarks, reviewLastCreateTimeBySeatId);
        return BookmarkResDto.BookmarkDto.builder()
                .venueId(v.getId())
                .venueName(v.getName())
                .bookmarkFloors(bookmarkFloorDtos)
                .build();
    }

    private Map<Long, LocalDateTime> getReviewLastCreateTimeBySeatIds(List<Long> seatIds) {
        return reviewProvider.findTopReviewsBySeatIdsAndStatus(seatIds)
                .stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                ReviewWithSeatIdDto::getSeatId,
                                ReviewWithSeatIdDto::getCreateTime
                        ),
                        Map::copyOf  // 불변으로 변환
                ));
    }

    public List<BookmarkResDto.BookmarkFloorDto> mapToBookmarkFloorDto(List<BookmarkEntity> bookmarkEntities,
                                                                       Map<Long, LocalDateTime> reviewLastCreateTimeBySeatId) {
        return bookmarkEntities.stream()
                .collect(Collectors.groupingBy(entity -> entity.getSeat().getFloor())) // floor 기준 그룹화
                .entrySet()
                .stream()
                .map(entry -> BookmarkResDto.BookmarkFloorDto.builder()
                        .bookmarkFloor(entry.getKey()) // key = floor
                        .bookmarkSeats(entry.getValue().stream() // 해당 그룹의 각 엔티티를 BookmarkSeatDto로 변환
                                .map(entity -> {
                                    String section = entity.getSeat().getSection();
                                    String row = entity.getSeat().getSeatRow();

                                    LocalDateTime lastUpdateDate = reviewLastCreateTimeBySeatId.get(entity.getSeat().getId());
                                    return BookmarkResDto.BookmarkSeatDto.builder()
                                            .bookmarkId(entity.getId())
                                            .bookmarkSection(StringUtils.equals(section, UNSELECTED_STRING) ? null : section)
                                            .bookmarkRow(row == UNSELECTED_STRING ? null : row)
                                            .lastUpdateDate(CommonConverter.toNullableTimestamp(lastUpdateDate))
                                            .build();
                                })
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList()); // 최종 결과 리스트로 수집
    }

    public ReviewListResDto getMyReviews(MemberInfo memberInfo, ReviewListReqDto req, Boolean isSummary) {
        Long memberNo = memberInfo.getMemberId();
        Page<ReviewEntity> reviewList = (SortType.DEFAULT.equals(req.sortType()))
                ? reviewProvider.getMyReviewListPrioritizingMedia(memberNo, req)
                : reviewProvider.getMyReviewList(memberNo, req);

        List<Long> reviewIds = reviewList.getContent().stream().map(ReviewEntity::getId).toList();
        Map<Long, List<String>> reviewImageUrlMap = imageService.getReviewImageUrlMap(reviewIds);
        return toReviewListResDto(isSummary, reviewList, reviewImageUrlMap);
    }
}
