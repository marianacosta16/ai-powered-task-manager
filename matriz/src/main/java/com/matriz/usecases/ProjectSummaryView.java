package com.matriz.usecases;

import com.matriz.access.UserInfo;

/** ProjectSummary of the contract. */
public record ProjectSummaryView(long id, String name, String phase, String status, UserInfo manager) {
}
