package com.ngxgroup.xticket.service;

import com.ngxgroup.xticket.payload.XTicketPayload;

/**
 *
 * @author briano
 */
public interface XTicketService {

    XTicketPayload processSignin(XTicketPayload requestPayload);

    XTicketPayload processSignup(XTicketPayload requestPayload);

    XTicketPayload processSignUpActivation(String id);

    XTicketPayload processFetchProfile(String principal);

    XTicketPayload processChangePassword(XTicketPayload requestPayload);
    
    XTicketPayload processForgotPassword(XTicketPayload requestPayload);

}
