package com.matriz.access;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Loads the accounts of APP_USER (F10). Passwords are stored only as hashes (S5). */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbc;

    public AppUserDetailsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return jdbc.query("""
                SELECT id, name, username, password, role FROM app_user WHERE username = ?
                """, (rs, i) -> new AuthenticatedUser(rs.getLong("id"), rs.getString("name"),
                        rs.getString("username"), rs.getString("password"), Role.fromLabel(rs.getString("role"))),
                username).stream().findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }
}
