package com.puremadeleine.viewith.repository;

import com.puremadeleine.viewith.domain.image.SourceType;
import com.puremadeleine.viewith.domain.review.ReviewEntity;
import com.puremadeleine.viewith.domain.review.Status;
import com.puremadeleine.viewith.dto.common.SortType;
import com.puremadeleine.viewith.dto.review.request.ReviewListReqDto;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

import static com.puremadeleine.viewith.domain.image.QImageEntity.imageEntity;
import static com.puremadeleine.viewith.domain.review.QReviewEntity.reviewEntity;
import static com.puremadeleine.viewith.domain.venue.QSeatEntity.seatEntity;

@Repository
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ReviewCustomRepository {

    JPAQueryFactory queryFactory;

    public List<ReviewEntity> findReviewList(ReviewListReqDto req) {
        return queryFactory
                .select(reviewEntity)
                .from(reviewEntity)
                .join(reviewEntity.seat, seatEntity)
                .where(
                        reviewEntity.status.eq(Status.NORMAL),
                        venueEq(req.venueId()),
                        floorEq(req.floor()),
                        sectionEq(req.section()),
                        seatRowEq(req.seatRow()))
                .orderBy(
                        getOrder(req.sortType()),
                        reviewEntity.id.desc())
                .offset(getOffset(req))
                .limit(req.size())
                .fetch();
    }

    public List<ReviewEntity> findMyReviewList(Long memberNo, ReviewListReqDto req) {
        return queryFactory
                .select(reviewEntity)
                .from(reviewEntity)
                .join(reviewEntity.seat, seatEntity)
                .where(
                        memberIdEq(memberNo),
                        isNormal()
                )
                .orderBy(
                        getOrder(req.sortType()),
                        reviewEntity.id.desc()
                )
                .offset(getOffset(req))
                .limit(req.size())
                .fetch();
    }

    public List<ReviewEntity> findMyReviewListPrioritizingMedia(Long memberNo, ReviewListReqDto req) {
        return queryFactory
                .select(reviewEntity)
                .from(reviewEntity)
                .leftJoin(imageEntity)
                .on(reviewEntity.id.eq(imageEntity.sourceId))
                .where(
                        memberIdEq(memberNo),
                        isNormal()
                )
                .orderBy(
                        getPrioritizingMediaDesc(),
                        reviewEntity.id.desc()
                )
                .offset(getOffset(req))
                .limit(req.size())
                .fetch();
    }

    public List<ReviewEntity> findReviewListPrioritizingMedia(ReviewListReqDto req) {
        return queryFactory
                .select(reviewEntity)
                .from(reviewEntity)
                .join(reviewEntity.seat, seatEntity)
                .leftJoin(imageEntity)
                .on(reviewEntity.id.eq(imageEntity.sourceId))
                .where(
                        reviewEntity.status.eq(Status.NORMAL),
                        venueEq(req.venueId()),
                        floorEq(req.floor()),
                        sectionEq(req.section()),
                        seatRowEq(req.seatRow()))
                .orderBy(
                        getPrioritizingMediaDesc(),
                        reviewEntity.id.desc()
                )
                .offset(getOffset(req))
                .limit(req.size())
                .fetch();
    }

    private static OrderSpecifier<Integer> getPrioritizingMediaDesc() {
        return new CaseBuilder()
                .when(imageEntity.id.isNotNull()
                        .and(imageEntity.sourceType.eq(SourceType.REVIEW))
                ).then(1)
                .otherwise(0)
                .desc();
    }

    public int countReviewTotal(Long memberNo) {
        return queryFactory
                .select(reviewEntity.id)
                .from(reviewEntity)
                .where(
                        memberIdEq(memberNo),
                        isNormal()
                )
                .fetch().size();
    }

    public int countReviewTotal(ReviewListReqDto req) {
        return queryFactory
                .select(reviewEntity.id)
                .from(reviewEntity)
                .join(reviewEntity.seat, seatEntity)
                .where(
                        reviewEntity.status.eq(Status.NORMAL),
                        venueEq(req.venueId()),
                        floorEq(req.floor()),
                        sectionEq(req.section()),
                        seatRowEq(req.seatRow()))
                .fetch().size();
    }

    private int getOffset(ReviewListReqDto req) {
        return (req.page() - 1) * req.size();
    }

    private BooleanExpression venueEq(Long venueId) {
        if (Objects.isNull(venueId)) return null;
        return seatEntity.venue.id.eq(venueId);
    }

    private BooleanExpression floorEq(String floor) {
        if (Objects.isNull(floor)) return null;
        return seatEntity.floor.eq(floor);
    }

    private BooleanExpression sectionEq(String section) {
        if (Objects.isNull(section)) return null;
        return seatEntity.section.eq(section);
    }

    private BooleanExpression seatRowEq(String seatRow) {
        if (Objects.isNull(seatRow)) return null;
        return seatEntity.seatRow.eq(seatRow);
    }

    private BooleanExpression memberIdEq(Long memberId) {
        if (Objects.isNull(memberId)) return null;
        return reviewEntity.member.id.eq(memberId);
    }

    private BooleanExpression isNormal() {
        return reviewEntity.status.eq(Status.NORMAL);
    }

    private OrderSpecifier<?> getOrder(SortType sortType) {
        if (Objects.isNull(sortType)) return null;
        return switch (sortType) {
            case LATEST -> reviewEntity.createTime.desc();
            case RATING -> reviewEntity.rating.desc();
            default -> reviewEntity.id.asc();
        };
    }
}
