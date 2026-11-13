package com.matriz.access;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/** Public data (id, name, role) of users, for the modules that only hold their identifiers. */
@Service
public class UserDirectory {

    private final NamedParameterJdbcTemplate jdbc;

    public UserDirectory(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Everyone, by identifier (the candidates for a team are chosen among them). */
    public List<UserInfo> findAll() {
        return jdbc.getJdbcOperations().query("SELECT id, name, role FROM app_user ORDER BY id",
                (rs, i) -> new UserInfo(rs.getLong("id"), rs.getString("name"), rs.getString("role")));
    }

    /**
     * Reads a person and locks their row until the end of the transaction. Whoever changes the
     * projects of this person (R11) takes the lock first, so two requests about the same person
     * are done one after the other and the second sees what the first did.
     */
    public Optional<UserInfo> findForUpdate(long id) {
        return jdbc.getJdbcOperations().query("SELECT id, name, role FROM app_user WHERE id = ? FOR UPDATE",
                (rs, i) -> new UserInfo(rs.getLong("id"), rs.getString("name"), rs.getString("role")), id)
                .stream().findFirst();
    }

    public Map<Long, UserInfo> findByIds(Collection<Long> ids) {
        Map<Long, UserInfo> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        jdbc.query("SELECT id, name, role FROM app_user WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", ids),
                rs -> {
                    result.put(rs.getLong("id"),
                            new UserInfo(rs.getLong("id"), rs.getString("name"), rs.getString("role")));
                });
        return result;
    }
}
