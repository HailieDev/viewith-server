package com.puremadeleine.viewith.service;

import com.puremadeleine.viewith.domain.bookmark.BookmarkEntity;
import com.puremadeleine.viewith.domain.member.MemberEntity;
import com.puremadeleine.viewith.domain.review.Block;
import com.puremadeleine.viewith.domain.venue.PerformanceEntity;
import com.puremadeleine.viewith.domain.venue.SeatEntity;
import com.puremadeleine.viewith.domain.venue.VenueEntity;
import com.puremadeleine.viewith.domain.venue.VenueStageEntity;
import com.puremadeleine.viewith.dto.member.MemberInfo;
import com.puremadeleine.viewith.dto.review.ReviewCntDto;
import com.puremadeleine.viewith.dto.venue.FloorRowDto;
import com.puremadeleine.viewith.dto.venue.VenueFilterResDto;
import com.puremadeleine.viewith.dto.venue.VenueListResDto;
import com.puremadeleine.viewith.dto.venue.VenueResDto;
import com.puremadeleine.viewith.dto.venue.VenueSearchListResDto;
import com.puremadeleine.viewith.dto.venue.VenueSeatResDto;
import com.puremadeleine.viewith.exception.ViewithErrorCode;
import com.puremadeleine.viewith.exception.ViewithException;
import com.puremadeleine.viewith.provider.BookmarkProvider;
import com.puremadeleine.viewith.provider.MemberProvider;
import com.puremadeleine.viewith.provider.PerformanceProvider;
import com.puremadeleine.viewith.provider.ReviewProvider;
import com.puremadeleine.viewith.provider.SeatProvider;
import com.puremadeleine.viewith.provider.VenueProvider;
import com.puremadeleine.viewith.provider.VenueStageProvider;
import io.jsonwebtoken.lang.Collections;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.puremadeleine.viewith.constants.SeatConstants.FLOOR;
import static com.puremadeleine.viewith.constants.SeatConstants.SEAT;
import static com.puremadeleine.viewith.constants.SeatConstants.SEPARATOR;
import static com.puremadeleine.viewith.constants.SeatConstants.UNSELECTED_STRING;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VenueService {

    VenueProvider venueProvider;
    VenueStageProvider venueStageProvider;
    SeatProvider seatProvider;
    PerformanceProvider performanceProvider;
    ReviewProvider reviewProvider;
    VenueServiceMapper venueServiceMapper;
    BookmarkProvider bookmarkProvider;
    MemberProvider memberProvider;

    public VenueListResDto getVenues(int performanceCnt) {
        List<VenueEntity> venues = venueProvider.getVenues();
        var performancesPerVenue = getTopPerformancesPerVenue(performanceCnt);
        var venueRes = venues.stream()
                .map(venue -> venueServiceMapper.toVenueResDto(venue, performancesPerVenue.getOrDefault(venue.getId(), List.of())))
                .toList();
        return VenueListResDto.builder()
                .venues(venueRes)
                .build();
    }

    public Map<Long, List<VenueListResDto.Performance>> getTopPerformancesPerVenue(int maxPerformances) {
        List<PerformanceEntity> performances = performanceProvider.findTopPerformancesPerVenue(maxPerformances);

        return performances.stream()
                .collect(Collectors.groupingBy(
                        performance -> performance.getVenue().getId(),
                        Collectors.mapping(
                                p -> venueServiceMapper.toPerformance(p),
                                Collectors.toList()
                        )
                ));
    }

    public VenueResDto getVenue(long venueId) {
        // 공연장, 무대 정보
        VenueEntity venueEntity = venueProvider.getVenue(venueId);
        List<VenueStageEntity> stageEntities = venueStageProvider.getVenueStages(venueId);
        var stages = venueServiceMapper.toStages(stageEntities);

        // 좌석 정보
        List<SeatEntity> seatEntities = seatProvider.getSeats(venueId);
        var sections = seatEntities.stream()
                .map(SeatEntity::getSection)
                .distinct()
                .toList();

        // 리뷰 정보
        Map<String, Long> cntByKey = getReviewCntBySectionKey(venueId);
        var reviewInfos = seatEntities.stream()
                .map(s -> makeSectionKey(s.getFloor(), s.getSection()))
                .distinct()
                .map(key -> VenueResDto.VenueReviewInfo.builder()
                        .sectionKey(key)
                        .reviewCnt(cntByKey.getOrDefault(key, 0L))
                        .build()
                )
                .toList();

        return venueServiceMapper.toVenueResDto(venueEntity, sections, stages, reviewInfos);
    }

    private Map<String, Long> getReviewCntBySectionKey(long venueId) {
        List<ReviewCntDto> reviewCountDtos = reviewProvider.countNormalReviewsByVenueAndSeat(venueId);
        return reviewCountDtos.stream()
                .collect(Collectors.toMap(
                        dto -> makeSectionKey(dto.getFloor(), dto.getSection()),
                        ReviewCntDto::getReviewCount
                ));
    }

    private String makeSectionKey(String floor, String section) {
        String prefix = FLOOR.equalsIgnoreCase(floor) ? FLOOR : SEAT;
        return StringUtils.join(prefix, SEPARATOR, section);
    }

    public VenueFilterResDto getVenueFilter(long venueId) {
        List<VenueFilterResDto.FilterInfoDto> seatInfos = convertToFilterInfoDto(seatProvider.getAllSeatsByVenueId(venueId));

        return VenueFilterResDto.builder()
                .seatInfos(seatInfos)
                .build();
    }

    @Transactional
    public void createBookmark(MemberInfo memberInfo, long seatId) {
        Optional<BookmarkEntity> bookmark = bookmarkProvider.findBookmark(memberInfo.getMemberId(), seatId);
        bookmark.ifPresent(b -> {
            throw new ViewithException(ViewithErrorCode.DUPLICATED_BOOKMARK);
        });

        MemberEntity member = memberProvider.getActiveMember(memberInfo.getMemberId());
        SeatEntity seat = seatProvider.getSeat(seatId);
        BookmarkEntity newBookmark = BookmarkEntity.createBookmark(member, seat);
        bookmarkProvider.save(newBookmark);
    }

    @Transactional
    public void deleteBookmark(MemberInfo memberInfo, long seatId) {
        BookmarkEntity bookmark = bookmarkProvider.getBookmark(memberInfo.getMemberId(), seatId);
        bookmarkProvider.deleteBookmark(bookmark);
    }

    @Transactional
    public void deleteBookmarks(MemberInfo memberInfo, List<Long> bookmarkIds) {
        List<BookmarkEntity> bookmarks = bookmarkProvider.findBookmarks(bookmarkIds);
        List<BookmarkEntity> memberBookmarks = bookmarks.stream()
                .filter(b -> b.getMember().getId().equals(memberInfo.getMemberId()))
                .toList();
        if (Collections.isEmpty(memberBookmarks)) {
            throw new ViewithException(ViewithErrorCode.NO_BOOKMARK);
        }

        bookmarkProvider.deleteBookmark(memberBookmarks);
    }

    public List<VenueFilterResDto.FilterInfoDto> convertToFilterInfoDto(List<FloorRowDto> floorRows) {
        return floorRows.stream()
                .collect(Collectors.groupingBy(
                        FloorRowDto::getFloor,
                        Collectors.mapping(FloorRowDto::getRow, Collectors.toList())
                ))
                .entrySet()
                .stream()
                .map(this::mapToFilterInfoDto)
                .toList();
    }

    private VenueFilterResDto.FilterInfoDto mapToFilterInfoDto(Map.Entry<String, List<String>> rowsByFloor) {
        return VenueFilterResDto.FilterInfoDto.builder()
                .floor(rowsByFloor.getKey())
                .rows(rowsByFloor.getValue()
                        .stream()
                        .filter(r -> !UNSELECTED_STRING.equals(r))
                        .toList())
                .build();
    }

    public VenueSearchListResDto searchVenue(String keyword) {
        List<VenueEntity> searchList = venueProvider.search(keyword);
        var venues = venueServiceMapper.toVenueSearchResDtos(searchList);
        return VenueSearchListResDto.builder().venues(venues).build();
    }

    public VenueSeatResDto getVenueSeatInfo(long venueId) {
        List<SeatEntity> seats = seatProvider.getSeats(venueId);
        Map<String, Map<String, List<SeatEntity>>> groupedSeats = groupSeats(seats);

        List<VenueSeatResDto.SeatInfoDto> seatInfos = groupedSeats.entrySet()
                .stream()
                .map(sectionGroup -> buildSeatInfo(sectionGroup.getKey(), sectionGroup.getValue()))
                .toList();

        return VenueSeatResDto.builder()
                .seatInfos(seatInfos)
                .build();
    }

    // <Section, <Row, SeatEntity>>
    private Map<String, Map<String, List<SeatEntity>>> groupSeats(List<SeatEntity> seats) {
        return seats.stream()
                .filter(seat -> !StringUtils.equals(UNSELECTED_STRING, seat.getSection()))
                .collect(Collectors.groupingBy(
                        SeatEntity::getSection,
                        Collectors.groupingBy(SeatEntity::getSeatRow)
                ));
    }

    private VenueSeatResDto.SeatInfoDto buildSeatInfo(String section, Map<String, List<SeatEntity>> rowMap) {
        List<VenueSeatResDto.RowInfoDto> rows = rowMap.entrySet()
                .stream()
                .filter(rowGroup -> !UNSELECTED_STRING.equals(rowGroup.getKey()))
                .map(rowGroup -> buildRowInfo(rowGroup.getKey(), rowGroup.getValue()))
                .collect(Collectors.toList());

        return VenueSeatResDto.SeatInfoDto.builder()
                .section(section)
                .rows(rows)
                .build();
    }

    private VenueSeatResDto.RowInfoDto buildRowInfo(String row, List<SeatEntity> seats) {
        List<VenueSeatResDto.ColumnInfoDto> columns = seats.stream()
                .filter(seat -> !UNSELECTED_STRING.equals(seat.getSeatColumn()))
                .filter(seat -> Block.NONE != seat.getBlock())
                .map(seat -> VenueSeatResDto.ColumnInfoDto.builder()
                        .column(seat.getSeatColumn())
                        .block(seat.getBlock())
                        .build())
                .toList();

        return VenueSeatResDto.RowInfoDto.builder()
                .row(row)
                .columns(columns)
                .build();
    }

    @Mapper(componentModel = "spring")
    public interface VenueServiceMapper {
        @Mapping(source = "venue.id", target = "venueId")
        @Mapping(source = "venue.name", target = "venueName")
        @Mapping(source = "venue.location", target = "venueLocation")
        VenueListResDto.VenueResDto toVenueResDto(VenueEntity venue, List<VenueListResDto.Performance> performances);

        VenueListResDto.Performance toPerformance(PerformanceEntity performance);

        @Mapping(source = "id", target = "stageId")
        VenueResDto.Stage toStage(VenueStageEntity stageEntity);

        List<VenueResDto.Stage> toStages(List<VenueStageEntity> stageEntities);

        @Mapping(source = "venueEntity.svgUrl", target = "venueUrl")
        VenueResDto toVenueResDto(VenueEntity venueEntity,
                                  List<String> sections,
                                  List<VenueResDto.Stage> stages,
                                  List<VenueResDto.VenueReviewInfo> venueReviewInfos);

        List<VenueSearchListResDto.VenueSearchResDto> toVenueSearchResDtos(List<VenueEntity> venues);

        @Mapping(source = "id", target = "venueId")
        @Mapping(source = "name", target = "venueName")
        @Mapping(source = "location", target = "venueLocation")
        VenueSearchListResDto.VenueSearchResDto toVenueSearchResDto(VenueEntity venue);
    }
}
