package com.puremadeleine.viewith.service;

import com.puremadeleine.viewith.domain.image.SourceType;
import com.puremadeleine.viewith.domain.member.MemberEntity;
import com.puremadeleine.viewith.domain.review.ReviewEntity;
import com.puremadeleine.viewith.domain.review.ReviewReportEntity;
import com.puremadeleine.viewith.domain.venue.PerformanceEntity;
import com.puremadeleine.viewith.domain.venue.SeatEntity;
import com.puremadeleine.viewith.domain.venue.VenueEntity;
import com.puremadeleine.viewith.dto.common.SortType;
import com.puremadeleine.viewith.dto.review.request.CreateReviewReqDto;
import com.puremadeleine.viewith.dto.review.request.ReportReviewReqDto;
import com.puremadeleine.viewith.dto.review.request.ReviewListReqDto;
import com.puremadeleine.viewith.dto.review.request.UpdateReviewReqDto;
import com.puremadeleine.viewith.dto.review.response.CreateReviewResDto;
import com.puremadeleine.viewith.dto.review.response.ReviewInfoResDto;
import com.puremadeleine.viewith.dto.review.response.ReviewListResDto;
import com.puremadeleine.viewith.provider.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.puremadeleine.viewith.converter.review.ReviewServiceConverter.toReviewInfoResDto;
import static com.puremadeleine.viewith.converter.review.ReviewServiceConverter.toReviewListResDto;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewService {

    ReviewProvider reviewProvider;
    VenueProvider venueProvider;
    PerformanceProvider performanceProvider;
    SeatProvider seatProvider;
    ReviewReportProvider reviewReportProvider;
    MemberProvider memberProvider;
    BookmarkProvider bookmarkProvider;
    ImageService imageService;

    @Transactional
    public CreateReviewResDto createReview(CreateReviewReqDto reqDto, List<MultipartFile> images, Long memberId) {
        MemberEntity activeMember = memberProvider.getActiveMember(memberId);
        VenueEntity venue = venueProvider.getVenue(reqDto.venueId());
        SeatEntity seat = seatProvider.getSeat(venue.getId(), reqDto.section(), reqDto.seatRow(), reqDto.seatColumn());

        PerformanceEntity performance = Optional.ofNullable(reqDto.performanceId())
            .map(performanceProvider::getPerformance)
            .orElse(null);

        ReviewEntity review = ReviewEntity.createReview(reqDto, venue, seat, performance, activeMember);
        ReviewEntity savedReview = reviewProvider.saveReview(review);
        imageService.saveImages(images, review.getId(), SourceType.REVIEW);

        return CreateReviewResDto.builder().reviewId(savedReview.getId()).build();
    }

    @Transactional
    public void updateReview(Long reviewId, UpdateReviewReqDto reqDto, Long memberId) {
        ReviewEntity review = reviewProvider.getNormalReview(reviewId);
        review.updateReview(reqDto, memberId);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long memberId) {
        ReviewEntity review = reviewProvider.getNormalReview(reviewId);
        review.deleteReview(memberId);
    }

    public ReviewInfoResDto getReviewInfo(Long reviewId, Long memberId) {
        ReviewEntity review = reviewProvider.getNormalReview(reviewId);
        boolean bookmarked = bookmarkProvider.isBookmarked(review.getSeat().getId(), memberId);
        List<String> imageUrls = imageService.getReviewImageUrlList(reviewId);
        return toReviewInfoResDto(review, bookmarked, imageUrls);
    }

    public ReviewListResDto getReviewList(ReviewListReqDto req, boolean isSummary) {
        Page<ReviewEntity> reviewList = (SortType.DEFAULT.equals(req.sortType()))
                ? reviewProvider.getReviewListPrioritizingMedia(req)
                : reviewProvider.getReviewList(req);
        List<Long> reviewIds = reviewList.getContent().stream().map(ReviewEntity::getId).toList();
        Map<Long, List<String>> reviewImageUrlMap = imageService.getReviewImageUrlMap(reviewIds);
        return toReviewListResDto(isSummary, reviewList, reviewImageUrlMap);
    }

    public void reportReview(Long reviewId, ReportReviewReqDto req, Long memberId) {
        ReviewEntity review = reviewProvider.getNormalReview(reviewId);
        review.reportReview(memberId);
        ReviewReportEntity reviewReport =
                ReviewReportEntity.createReviewReport(review, req.reportReason(), req.reportReasonDetail());
        reviewReportProvider.saveReviewReport(reviewReport);
    }
}