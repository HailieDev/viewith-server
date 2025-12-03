package com.puremadeleine.viewith.repository;

import com.puremadeleine.viewith.domain.venue.PerformanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerformanceRepository extends JpaRepository<PerformanceEntity, Long> {

    List<PerformanceEntity> findByVenue_IdAndTitleContainingIgnoreCase(Long venueId, String keyword);

    @Query(value = """
                WITH RecentPerformance AS (
                    SELECT
                        p.performance_id AS performance_id,
                        p.kopis_id AS kopis_id,
                        p.title AS title,
                        p.artist AS artist,
                        p.start_date AS start_date,
                        p.end_date AS end_date,
                        p.venue_id AS venue_id,
                        p.image_url AS image_url,
                        ROW_NUMBER() OVER (
                            PARTITION BY p.venue_id ORDER BY
                            ABS(TIMESTAMPDIFF(SECOND, p.start_date, CURRENT_TIMESTAMP())) ASC
                        ) AS row_num
                    FROM tb_performance p
                    JOIN tb_venue v ON p.venue_id = v.venue_id
                    WHERE p.end_date >= CURRENT_TIMESTAMP() 
                      AND p.artist IS NOT NULL 
                      AND p.artist != ''
                )
                SELECT rp.performance_id, rp.kopis_id, rp.title, rp.artist, rp.start_date, rp.end_date, rp.venue_id, rp.image_url
                FROM RecentPerformance rp
                WHERE row_num <= :limit
            """, nativeQuery = true)
    List<PerformanceEntity> findTopPerformancesPerVenue(@Param("limit") int limit);

    @Query("SELECT p.kopisId FROM PerformanceEntity p WHERE p.kopisId IN :kopisIds")
    List<String> findByKopisIds(@Param("kopisIds") List<String> kopisIds);
}
