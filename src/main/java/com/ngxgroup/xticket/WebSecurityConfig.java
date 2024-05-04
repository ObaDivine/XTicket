package com.ngxgroup.xticket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 *
 * @author Brian A. Okon - okon.brian@gmail.com
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(true)
                .and()
                .sessionFixation().newSession();

        http.authorizeRequests()
                .antMatchers("/", "/signin/**", "/signup/**", "/forgot-password/**", "/logout/**").permitAll()
                .antMatchers("/css/**", "/images/**", "/js/**", "/font/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .formLogin().loginPage("/")
                .and()
                .logout().logoutSuccessUrl("/logout")
                .deleteCookies("JSESSIONID")
                .and()
                .csrf().disable();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
