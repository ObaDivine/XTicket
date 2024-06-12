package com.ngxgroup.xticket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 *
 * @author briano
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private static final String[] WHITE_LIST_URL = {"/", "/login/**", "/signup/**", "/forgot-password/**", "/logout", "/css/**", "/images/**", "/js/**", "/font/**", "/error/**", "/WEB-INF/**"};

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> {
            cors.disable();
        });

        http.csrf(csrf -> {
            csrf.disable();
        });

        http.sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(true)
                .expiredUrl("/"));

        http.authorizeHttpRequests(authorize -> {
            authorize.requestMatchers(WHITE_LIST_URL).permitAll();
            authorize.anyRequest().authenticated();
        });

        http.formLogin(formLogin -> {
            formLogin.loginPage("/");
            formLogin.permitAll();
        });

        http.securityContext(securityContext -> {
            securityContext.requireExplicitSave(true);
        });

        http.logout(authz -> authz
                .deleteCookies("JSESSIONID")
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
        );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
