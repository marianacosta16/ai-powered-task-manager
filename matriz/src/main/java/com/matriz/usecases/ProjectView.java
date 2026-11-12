package com.matriz.usecases;

import java.time.LocalDate;
import java.time.LocalTime;

import com.matriz.access.UserInfo;

/** Project of the contract. */
public record ProjectView(long id, String name, String phase, String status, UserInfo manager, String description,
        LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime, String timezone) {
}
