package com.ljl.agent.security;

import com.ljl.agent.entity.User;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;

/**
 * 只接受项目明确支持的 role claim，防止非法角色形成权限提升。
 */
public class JwtAuthorityConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        String role = jwt.getClaimAsString("role");
        if (!User.ROLE_USER.equals(role) && !User.ROLE_ADMIN.equals(role)) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
