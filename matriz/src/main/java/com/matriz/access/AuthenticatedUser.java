package com.matriz.access;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The user of the current session. It is the only source of identity: owners and authors are
 * taken from here, never from data sent by the client. The password hash is erased after login.
 */
public class AuthenticatedUser implements UserDetails, CredentialsContainer, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long id;
    private final String name;
    private final String username;
    private final Role role;
    private String password;

    public AuthenticatedUser(long id, String name, String username, String password, Role role) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Role role() {
        return role;
    }

    public UserInfo toInfo() {
        return new UserInfo(id, name, role.label());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }

    @Override
    public String toString() {
        return "AuthenticatedUser[id=" + id + ", username=" + username + ", role=" + role + "]";
    }
}
