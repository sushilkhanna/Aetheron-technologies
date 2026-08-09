package com.bikepooling.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;


public class UserPrincipal implements UserDetails {

    private final Long   userId;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long userId,
                         String role,
                         Collection<? extends GrantedAuthority> authorities) {
        this.userId      = userId;
        this.role        = role;
        this.authorities = authorities;
    }

    public Long   getUserId() { return userId; }
    public String getRole()   { return role;   }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String  getPassword()             { return null; }
    @Override public String  getUsername()             { return String.valueOf(userId); }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}