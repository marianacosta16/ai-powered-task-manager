package com.matriz.usecases;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the fictional demo data (P21: 2 managers, 8 members; 4 projects) on startup,
 * only when there are no users yet, in a single transaction. Starting again never
 * deletes or duplicates anything.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final int MIN_PASSWORD_LENGTH = 8; // S5
    private static final String MANAGER = "Project manager";
    private static final String MEMBER = "Team member";

    private final JdbcTemplate jdbc;
    private final String demoPassword;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public DemoDataSeeder(JdbcTemplate jdbc, @Value("${matriz.demo.password:}") String demoPassword) {
        this.jdbc = jdbc;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional // run() calls seed() on this, so the transaction must start here too
    public void run(ApplicationArguments args) {
        if (demoPassword == null || demoPassword.isBlank()) {
            log.warn("DEMO_PASSWORD is not set: demo data not created.");
            return;
        }
        if (seed(demoPassword)) {
            log.info("Demo data created.");
        } else {
            log.info("Users already exist: demo data left untouched.");
        }
    }

    /** @return true if the data was created, false if users already existed. */
    @Transactional
    public boolean seed(String password) {
        // Serialises concurrent startups; released at the end of the transaction.
        jdbc.execute("SELECT pg_advisory_xact_lock(7301001)");
        Integer users = jdbc.queryForObject("SELECT count(*) FROM app_user", Integer.class);
        if (users != null && users > 0) {
            return false; // nothing to create, so the password is not used (and not checked)
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("The demo password must have at least " + MIN_PASSWORD_LENGTH
                    + " characters (S5).");
        }

        String hash = encoder.encode(password);
        for (String[] u : List.of(
                new String[] {"Marta", "marta", MANAGER},
                new String[] {"Pedro", "pedro", MANAGER},
                new String[] {"Ana", "ana", MEMBER},
                new String[] {"Rui", "rui", MEMBER},
                new String[] {"Luís", "luis", MEMBER},
                new String[] {"Sofia", "sofia", MEMBER},
                new String[] {"Carla", "carla", MEMBER},
                new String[] {"Inês", "ines", MEMBER},
                new String[] {"Tiago", "tiago", MEMBER},
                new String[] {"Diana", "diana", MEMBER})) {
            jdbc.update("INSERT INTO app_user (name, username, password, role) VALUES (?, ?, ?, ?)",
                    u[0], u[1], hash, u[2]);
        }

        long website = project("Website", "Company website redesign", "2026-10-01", "2027-02-28",
                "Execution", "In progress", "marta", "ana", "rui", "luis");
        long mobile = project("Mobile App", "First version of the mobile app", "2026-11-01", "2027-03-31",
                "Planning", "In progress", "marta", "ines", "tiago");
        long intranet = project("Intranet", "Internal news and documents", "2026-09-15", "2027-01-31",
                "Execution", "In progress", "pedro", "sofia", "carla");
        long oldSite = project("Old Site", "Shutdown of the previous website", "2026-01-05", "2026-06-30",
                "Evaluation", "Completed", "pedro", "diana");

        // Website
        long homepage = task(website, null, "Design homepage", "In progress", "2026-12-01", 4, 4, null, null, "ana");
        long wireframes = task(website, homepage, "Draft wireframes", "Done", "2026-11-20", 3, 3, null, null, "ana");
        task(website, homepage, "Pick colour palette", "To do", "2026-11-25", 2, 3, null, null, "rui");
        task(website, null, "Set up hosting", "Blocked", "2026-12-10", 3, 2, "Waiting for credentials", null, "luis");
        long contact = task(website, null, "Write contact page copy", "To do", "2027-01-15", 2, 3, null, null,
                "rui", "luis");
        task(website, null, "Contact page text", "Duplicate", "2027-01-15", 2, 3, null, contact, "rui");
        comment(wireframes, "ana", "First version shared with the team.");
        comment(homepage, "marta", "Please keep the layout simple.");

        // Mobile App
        long navigation = task(mobile, null, "Define app navigation", "In progress", "2026-12-05", 3, 4, null, null,
                "ines");
        task(mobile, null, "Prototype login screen", "To do", "2026-12-20", 4, 2, null, null, "tiago");
        task(mobile, null, "Choose push provider", "In review", "2026-12-15", 2, 3, null, null, "tiago", "ines");
        comment(navigation, "ines", "Three main tabs for now.");

        // Intranet
        task(intranet, null, "Migrate news section", "To do", "2026-12-18", 3, 3, null, null, "sofia");
        long permissions = task(intranet, null, "Review permissions", "In progress", "2026-11-30", 4, 3, null, null,
                "carla", "sofia");
        task(intranet, permissions, "List current roles", "Done", "2026-11-27", 3, 3, null, null, "carla");
        comment(permissions, "pedro", "Needed before the migration.");

        // Old Site (completed)
        task(oldSite, null, "Archive old pages", "Done", "2026-06-15", 2, 2, null, null, "diana");
        long shutdown = task(oldSite, null, "Shut down server", "Done", "2026-06-30", 3, 4, null, null, "diana");
        comment(shutdown, "diana", "Server switched off.");

        return true;
    }

    private long userId(String username) {
        return jdbc.queryForObject("SELECT id FROM app_user WHERE username = ?", Long.class, username);
    }

    private long project(String name, String description, String start, String end, String phase,
            String status, String manager, String... members) {
        long id = jdbc.queryForObject("""
                INSERT INTO project (name, description, start_date, end_date, phase, status, manager_id)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, name, description, LocalDate.parse(start), LocalDate.parse(end), phase, status,
                userId(manager));
        for (String m : members) {
            jdbc.update("INSERT INTO project_member (project_id, user_id) VALUES (?, ?)", id, userId(m));
        }
        return id;
    }

    private long task(long projectId, Long parentId, String title, String status, String deadline, int urgency,
            int importance, String blockReason, Long originalId, String... assignees) {
        long id = jdbc.queryForObject("""
                INSERT INTO task (project_id, parent_task_id, original_task_id, title, status, deadline,
                                  urgency, importance, block_reason)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, projectId, parentId, originalId, title, status, LocalDate.parse(deadline),
                urgency, importance, blockReason);
        for (String a : assignees) {
            jdbc.update("INSERT INTO task_assignee (task_id, user_id) VALUES (?, ?)", id, userId(a));
        }
        return id;
    }

    private void comment(long taskId, String author, String content) {
        jdbc.update("INSERT INTO task_comment (task_id, author_id, content) VALUES (?, ?, ?)",
                taskId, userId(author), content);
    }
}
