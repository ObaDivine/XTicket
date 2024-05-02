package com.ngxgroup.xticket.service;

import com.ngxgroup.xticket.payload.XTicketPayload;

/**
 *
 * @author briano
 */
public interface XTicketService {

    XTicketPayload processSignin(XTicketPayload requestPayload);

    XTicketPayload processSignup(XTicketPayload requestPayload);

    String processSignUpActivation(String id);

    String processDashboard(String username);

}
