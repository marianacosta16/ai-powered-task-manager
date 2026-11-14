package com.matriz.views;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * Progress and calculated status of a project (US06; P5, P7, P16, P23, P26). Pure calculation: it reads
 * no data and no clock, so the limits (15 and 25 points, the end day) are tested with fixed instants.
 *
 * @param hasCountableTasks false when there are no tasks that count; the client shows "No tasks" (P16)
 * @param percent           done tasks divided by countable ones, two decimals; null without countable tasks
 * @param calculatedStatus  "Attention" or "Late", only for a project In progress with countable tasks; else null
 */
public record ProjectProgress(boolean hasCountableTasks, BigDecimal percent, String calculatedStatus) {

    public static final String ATTENTION = "Attention";
    public static final String LATE = "Late";

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ATTENTION_FROM = BigDecimal.valueOf(15);
    private static final BigDecimal LATE_FROM = BigDecimal.valueOf(25);

    /**
     * @param inProgress whether the status set by the manager is In progress: only then is a status calculated (P5)
     */
    public static ProjectProgress of(ProjectSchedule schedule, boolean inProgress, StatusCounts counts, Instant now) {
        int countable = counts.countable();
        if (countable == 0) {
            return new ProjectProgress(false, null, null);
        }
        BigDecimal actual = percent(counts.done(), countable);
        return new ProjectProgress(true, actual, inProgress ? calculatedStatus(schedule, actual, now) : null);
    }

    /** Both values are rounded to two decimals before the deviation is taken, so it is what is shown (P5). */
    private static String calculatedStatus(ProjectSchedule schedule, BigDecimal actual, Instant now) {
        if (!now.isBefore(schedule.end())) {
            return LATE; // the end passed (P5), whatever the progress
        }
        BigDecimal deviation = elapsedPercent(schedule, now).subtract(actual);
        if (deviation.compareTo(LATE_FROM) >= 0) {
            return LATE;
        }
        return deviation.compareTo(ATTENTION_FROM) >= 0 ? ATTENTION : null;
    }

    /** Time elapsed between the start and the end, as a percentage; 0 before the start and 100 after the end (P23). */
    static BigDecimal elapsedPercent(ProjectSchedule schedule, Instant now) {
        Duration total = Duration.between(schedule.start(), schedule.end());
        Duration elapsed = Duration.between(schedule.start(), now);
        if (elapsed.isNegative()) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (elapsed.compareTo(total) >= 0) {
            return HUNDRED.setScale(2);
        }
        return BigDecimal.valueOf(elapsed.toMillis()).multiply(HUNDRED)
                .divide(BigDecimal.valueOf(total.toMillis()), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(int part, int whole) {
        return BigDecimal.valueOf(part).multiply(HUNDRED).divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP);
    }
}
