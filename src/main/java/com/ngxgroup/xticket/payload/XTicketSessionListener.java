package com.ngxgroup.xticket.payload;

import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.repository.XTicketRepository;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author bokon
 */
@WebListener
public class XTicketSessionListener implements HttpSessionListener {

    @Autowired
    XTicketRepository xticketRepository;

    @Override
    public void sessionCreated(HttpSessionEvent se) {

    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        String sessionId = se.getSession().getId();

        AppUser appUser = xticketRepository.getAppUserUsingSessionId(sessionId);
        if(appUser != null){
            appUser.setOnline(false);
            appUser.setSessionId("");
            xticketRepository.updateAppUser(appUser);
        }
    }
}
