package com.matriz.views;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * The start and the end of a project, in its time zone (P26). A start without a time counts from 00:00;
 * an end without a time counts the whole day, so it is the instant the next day begins.
 */
public record ProjectSchedule(LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime,
        String timezone) {

    public Instant start() {
        return ZonedDateTime.of(startDate, startTime == null ? LocalTime.MIDNIGHT : startTime, zone()).toInstant();
    }

    public Instant end() {
        ZonedDateTime end = endTime == null ? endDate.plusDays(1).atStartOfDay(zone())
                : ZonedDateTime.of(endDate, endTime, zone());
        return end.toInstant();
    }

    private ZoneId zone() {
        return ZoneId.of(timezone);
    }
}
