package com.matriz.views;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

/**
 * The calculation of progress and calculated status with fixed instants (US06: scenarios 6.1 to 6.6 and 6.8
 * to 6.15; EC1, EC11, EC16, EC24; P5, P7, P16, P23, P26).
 */
class ProjectProgressTest {

    /** 2026-01-01 to 2026-01-31, both without a time: 31 days in Lisbon. */
    private static final ProjectSchedule JANUARY = new ProjectSchedule(LocalDate.of(2026, 1, 1), null,
            LocalDate.of(2026, 1, 31), null, "Europe/Lisbon");

    /** Half of the time has passed: 15.5 days of 31. */
    private static final Instant HALF_WAY = at("2026-01-16T12:00");

    @Test
    void progressIsDoneOverCountableWithoutCancelledAndDuplicates() { // 6.1
        StatusCounts counts = new StatusCounts(2, 2, 0, 0, 4, 1, 1);

        ProjectProgress progress = ProjectProgress.of(JANUARY, true, counts, at("2026-01-02T10:00"));

        assertThat(progress.hasCountableTasks()).isTrue();
        assertThat(progress.percent()).isEqualByComparingTo("50");
    }

    @Test
    void aProjectWithoutCountableTasksHasNoPercentAndNoCalculatedStatus() { // 6.2, 6.10, EC1, EC16
        for (StatusCounts counts : new StatusCounts[] { new StatusCounts(0, 0, 0, 0, 0, 0, 0),
                new StatusCounts(0, 0, 0, 0, 0, 3, 2) }) {
            ProjectProgress progress = ProjectProgress.of(JANUARY, true, counts, at("2026-02-02T10:00")); // past the end

            assertThat(progress.hasCountableTasks()).isFalse();
            assertThat(progress.percent()).isNull();
            assertThat(progress.calculatedStatus()).isNull();
        }
    }

    @Test
    void aDeviationOf15PointsIsAttentionAndOf14IsNot() { // 6.3, 6.4, EC11
        assertThat(statusWithDone(35)).isEqualTo("Attention"); // 50 - 35 = 15
        assertThat(statusWithDone(36)).isNull(); // 50 - 36 = 14
    }

    @Test
    void aDeviationOf25PointsIsLateAndOf24IsAttention() { // 6.5, EC11
        assertThat(statusWithDone(25)).isEqualTo("Late"); // 50 - 25 = 25
        assertThat(statusWithDone(26)).isEqualTo("Attention"); // 50 - 26 = 24
    }

    @Test
    void anEndDateThatPassedIsLateWhateverTheProgress() { // 6.6
        ProjectProgress progress = ProjectProgress.of(JANUARY, true, doneOutOf100(80), at("2026-02-02T10:00"));

        assertThat(progress.calculatedStatus()).isEqualTo("Late");
        assertThat(progress.percent()).isEqualByComparingTo("80");
    }

    @Test
    void onlyAProjectInProgressGetsACalculatedStatus() { // 6.8, 6.13, EC24, P5
        ProjectProgress suspended = ProjectProgress.of(JANUARY, false, doneOutOf100(10), at("2026-02-02T10:00"));

        assertThat(suspended.calculatedStatus()).isNull(); // suspended or not started, even long past the end
        assertThat(suspended.percent()).isEqualByComparingTo("10"); // the bar is still there
    }

    @Test
    void subtasksDoNotCountBecauseOnlyMainTaskCountsAreGiven() { // 6.11
        ProjectProgress progress = ProjectProgress.of(JANUARY, true, new StatusCounts(0, 1, 0, 0, 1, 0, 0), at("2026-01-02T00:00"));

        assertThat(progress.percent()).isEqualByComparingTo("50");
    }

    @Test
    void theProgressIsRoundedToTwoDecimals() { // 6.12
        ProjectProgress progress = ProjectProgress.of(JANUARY, true, new StatusCounts(2, 0, 0, 0, 1, 0, 0), at("2026-01-02T00:00"));

        assertThat(progress.percent()).isEqualTo(new BigDecimal("33.33"));
        assertThat(ProjectProgress.of(JANUARY, true, new StatusCounts(1, 0, 0, 0, 2, 0, 0), at("2026-01-02T00:00")).percent())
                .isEqualTo(new BigDecimal("66.67")); // half up, not down
    }

    @Test
    void theDeviationUsesTheRoundedValues() { // P5: 50 - 33.33 = 16.67
        ProjectProgress progress = ProjectProgress.of(JANUARY, true, new StatusCounts(2, 0, 0, 0, 1, 0, 0), HALF_WAY);

        assertThat(progress.calculatedStatus()).isEqualTo("Attention");
    }

    @Test
    void anEndWithATimeIsLateFromThatTime() { // 6.14
        ProjectSchedule withTime = new ProjectSchedule(LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 1, 31),
                LocalTime.of(18, 0), "Europe/Lisbon");

        assertThat(ProjectProgress.of(withTime, true, doneOutOf100(90), at("2026-01-31T19:00")).calculatedStatus()).isEqualTo("Late");
        assertThat(ProjectProgress.of(withTime, true, doneOutOf100(90), at("2026-01-31T17:59")).calculatedStatus()).isNull();
    }

    @Test
    void anEndWithoutATimeCountsTheWholeDay() { // 6.15
        assertThat(ProjectProgress.of(JANUARY, true, doneOutOf100(90), at("2026-01-31T23:00")).calculatedStatus()).isNull();
        assertThat(ProjectProgress.of(JANUARY, true, doneOutOf100(90), at("2026-02-01T00:00")).calculatedStatus()).isEqualTo("Late");
    }

    @Test
    void theElapsedTimeIsMeasuredInTheTimeZoneOfTheProject() { // P26
        ProjectSchedule newYork = new ProjectSchedule(LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 1, 31), null,
                "America/New_York");
        Instant lateInLisbon = Instant.parse("2026-02-01T00:30:00Z"); // 19:30 of 31 January in New York

        assertThat(ProjectProgress.of(newYork, true, doneOutOf100(90), lateInLisbon).calculatedStatus()).isNull();
        assertThat(ProjectProgress.of(newYork, true, doneOutOf100(90), Instant.parse("2026-02-01T05:00:00Z")).calculatedStatus())
                .isEqualTo("Late");
    }

    @Test
    void beforeTheStartNoTimeHasElapsed() { // P23
        assertThat(ProjectProgress.elapsedPercent(JANUARY, at("2025-12-01T00:00"))).isEqualByComparingTo("0");
        assertThat(ProjectProgress.elapsedPercent(JANUARY, HALF_WAY)).isEqualByComparingTo("50");
        assertThat(ProjectProgress.of(JANUARY, true, doneOutOf100(0), at("2025-12-01T00:00")).calculatedStatus()).isNull();
    }

    // ---- helpers ----

    private static String statusWithDone(int done) {
        return ProjectProgress.of(JANUARY, true, doneOutOf100(done), HALF_WAY).calculatedStatus();
    }

    private static StatusCounts doneOutOf100(int done) {
        return new StatusCounts(100 - done, 0, 0, 0, done, 0, 0);
    }

    /** An instant given as a wall-clock time in Lisbon, the zone of the schedules above. */
    private static Instant at(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(ZoneId.of("Europe/Lisbon")).toInstant();
    }
}
