package com.ngxgroup.xticket;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 *
 * @author bokon
 */
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    static final Logger logger = Logger.getLogger(CustomAccessDeniedHandler.class.getName());

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exc) throws IOException, ServletException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            logger.log(Level.INFO, "User: {0} attempted to access the protected URL: {1}", new Object[]{auth.getName(), request.getRequestURI()});
        }

        response.sendRedirect(request.getContextPath() + "/accessDenied");
    }
}
