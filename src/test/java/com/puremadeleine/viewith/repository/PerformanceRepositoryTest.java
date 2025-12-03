package com.puremadeleine.viewith.repository;

import com.puremadeleine.viewith.config.jpa.JpaConfig;
import com.puremadeleine.viewith.domain.venue.PerformanceEntity;
import com.puremadeleine.viewith.domain.venue.VenueEntity;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

@ActiveProfiles("test")
@DataJpaTest
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PerformanceRepositoryTest {
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private PerformanceRepository performanceRepository;

    @Test
    void find() {
        // given
        VenueEntity venue = venueRepository.save(makeDummyVenueEntity());
        PerformanceEntity performance = performanceRepository.save(makePerformanceEntity(venue, LocalDate.now().minusDays(1), LocalDate.now()));

        // when
        Optional<PerformanceEntity> actual = performanceRepository.findById(performance.getId());

        // then
        assertThat(actual).isPresent();
        assertThat(actual.get()).usingRecursiveComparison().isEqualTo(performance);
        assertThat(actual.get().getVenue()).usingRecursiveComparison().isEqualTo(venue);
    }

    @Test
    void findTopPerformancesPerVenue() {
        // given
        VenueEntity venue = venueRepository.saveAndFlush(makeDummyVenueEntity());
        VenueEntity venue2 = venueRepository.saveAndFlush(makeDummyVenueEntity());
        VenueEntity venue3 = venueRepository.saveAndFlush(makeDummyVenueEntity());

        LocalDate fiveDaysAgo = LocalDate.now().minusDays(5);
        LocalDate threeDaysAgo = LocalDate.now().minusDays(3);
        LocalDate threeDaysLater = LocalDate.now().plusDays(3);

        // 미노출, 5일 전 시작하여 3일 전 끝남
        PerformanceEntity performance_venue = performanceRepository.saveAndFlush(makePerformanceEntity(venue, fiveDaysAgo, threeDaysAgo));
        // 노출, 5일 전 시작하여 3일 후 끝남
        PerformanceEntity performance2_venue = performanceRepository.saveAndFlush(makePerformanceEntity(venue, fiveDaysAgo, threeDaysLater));
        // 미노출, 3일 전 시작하여 3일 전 끝남
        PerformanceEntity performance3_venue = performanceRepository.saveAndFlush(makePerformanceEntity(venue, threeDaysAgo, threeDaysAgo));
        // 노출, 3일 전 시작하여 3일 후 끝남
        PerformanceEntity performance4_venue = performanceRepository.saveAndFlush(makePerformanceEntity(venue, threeDaysAgo, threeDaysLater));

        // 미노출, 5일 전 시작하여 3일 전 끝남
        PerformanceEntity performance_venue2 = performanceRepository.saveAndFlush(makePerformanceEntity(venue2, fiveDaysAgo, threeDaysAgo));
        // 노출, 5일 전 시작하여 3일 후 끝남
        PerformanceEntity performance2_venue2 = performanceRepository.saveAndFlush(makePerformanceEntity(venue2, fiveDaysAgo, threeDaysLater));
        // 미노출, 3일 전 시작하여 3일 전 끝남
        PerformanceEntity performance3_venue2 = performanceRepository.saveAndFlush(makePerformanceEntity(venue2, threeDaysAgo, threeDaysAgo));
        // 노출, 3일 전 시작하여 3일 후 끝남
        PerformanceEntity performance4_venue2 = performanceRepository.saveAndFlush(makePerformanceEntity(venue2, threeDaysAgo, threeDaysLater));

        // 미노출, 5일 전 시작하여 3일 전 끝남
        PerformanceEntity performance_venue3 = performanceRepository.saveAndFlush(makePerformanceEntity(venue3, fiveDaysAgo, threeDaysAgo));
        // 노출, 5일 전 시작하여 3일 후 끝남
        PerformanceEntity performance2_venue3 = performanceRepository.saveAndFlush(makePerformanceEntity(venue3, fiveDaysAgo, threeDaysLater));
        // 미노출, 3일 전 시작하여 3일 전 끝남
        PerformanceEntity performance3_venue3 = performanceRepository.saveAndFlush(makePerformanceEntity(venue3, threeDaysAgo, threeDaysAgo));
        // 노출, 3일 전 시작하여 3일 후 끝남
        PerformanceEntity performance4_venue3 = performanceRepository.saveAndFlush(makePerformanceEntity(venue3, threeDaysAgo, threeDaysLater));

        // when
        List<PerformanceEntity> actual = performanceRepository.findTopPerformancesPerVenue(3);

        // then
        assertThat(actual).isNotEmpty().hasSize(6);
        assertThat(actual).contains(performance2_venue, performance4_venue, performance2_venue2, performance4_venue2, performance2_venue3, performance4_venue3);

    }

    @Test
    void findTopPerformancesPerVenue_excludesNullOrEmptyArtist() {
        // given
        VenueEntity venue = venueRepository.saveAndFlush(makeDummyVenueEntity());
        LocalDate threeDaysLater = LocalDate.now().plusDays(3);

        // artist가 null인 공연
        PerformanceEntity performanceWithNullArtist = makePerformanceEntityWithArtist(venue, LocalDate.now(), threeDaysLater, null);
        performanceRepository.saveAndFlush(performanceWithNullArtist);

        // artist가 빈 문자열인 공연
        PerformanceEntity performanceWithEmptyArtist = makePerformanceEntityWithArtist(venue, LocalDate.now(), threeDaysLater, "");
        performanceRepository.saveAndFlush(performanceWithEmptyArtist);

        // artist가 정상적으로 있는 공연
        PerformanceEntity performanceWithValidArtist = makePerformanceEntityWithArtist(venue, LocalDate.now(), threeDaysLater, "아티스트");
        performanceRepository.saveAndFlush(performanceWithValidArtist);

        // when
        List<PerformanceEntity> actual = performanceRepository.findTopPerformancesPerVenue(5);

        // then
        assertThat(actual).hasSize(1);
        assertThat(actual.getFirst().getArtist()).isEqualTo(performanceWithValidArtist.getArtist());
        assertThat(actual).doesNotContain(performanceWithNullArtist, performanceWithEmptyArtist);
    }

    private PerformanceEntity makePerformanceEntity(VenueEntity venue, LocalDate startDate, LocalDate endDate) {
        return Instancio.of(PerformanceEntity.class)
                .ignore(field(PerformanceEntity::getId))
                .set(field(PerformanceEntity::getVenue), venue)
                .set(field(PerformanceEntity::getStartDate), startDate)
                .set(field(PerformanceEntity::getEndDate), endDate)
                .create();
    }

    private PerformanceEntity makePerformanceEntityWithArtist(VenueEntity venue, LocalDate startDate, LocalDate endDate, String artist) {
        return Instancio.of(PerformanceEntity.class)
                .ignore(field(PerformanceEntity::getId))
                .set(field(PerformanceEntity::getVenue), venue)
                .set(field(PerformanceEntity::getStartDate), startDate)
                .set(field(PerformanceEntity::getEndDate), endDate)
                .set(field(PerformanceEntity::getArtist), artist)
                .create();
    }

    private VenueEntity makeDummyVenueEntity() {
        return Instancio.of(VenueEntity.class)
                .ignore(field(VenueEntity::getId))
                .create();
    }
}