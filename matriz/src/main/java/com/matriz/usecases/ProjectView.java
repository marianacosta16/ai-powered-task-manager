package com.matriz.usecases;

import java.time.LocalDate;
import java.time.LocalTime;

import com.matriz.access.UserInfo;

/** Project of the contract. */
public record ProjectView(long id, String name, String phase, String status, UserInfo manager, String description,
        LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime, String timezone) {

    static ProjectView of(com.matriz.projects.ProjectRecord p, UserInfo manager) {
        return new ProjectView(p.id(), p.name(), p.phase(), p.status(), manager, p.description(), p.startDate(),
                p.startTime(), p.endDate(), p.endTime(), p.timezone());
    }
}
