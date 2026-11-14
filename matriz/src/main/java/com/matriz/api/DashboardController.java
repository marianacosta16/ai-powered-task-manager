package com.matriz.api;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.matriz.access.AuthenticatedUser;
import com.matriz.usecases.DashboardProjectView;
import com.matriz.usecases.DashboardUseCases;

/** getDashboard. The user comes from the session. */
@RestController
class DashboardController {

    private final DashboardUseCases dashboard;

    DashboardController(DashboardUseCases dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/dashboard")
    List<DashboardProjectView> getDashboard(@AuthenticationPrincipal AuthenticatedUser user) {
        return dashboard.getDashboard(user);
    }
}
