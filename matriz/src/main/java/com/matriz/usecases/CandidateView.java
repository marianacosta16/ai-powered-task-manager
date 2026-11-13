package com.matriz.usecases;

import com.matriz.access.UserInfo;

/** Candidate of the contract: someone who is not in the project yet, and whether they can be added (R11). */
public record CandidateView(UserInfo user, boolean available, String unavailableReason) {
}
