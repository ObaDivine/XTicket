package com.ngxgroup.xticket.service;

import com.ngxgroup.xticket.model.TicketGroup;
import com.ngxgroup.xticket.model.TicketType;
import com.ngxgroup.xticket.payload.XTicketPayload;
import java.util.List;

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

    /**
     * Ticket Group
     *
     * @return
     */
    List<TicketGroup> processFetchTicketGroup();

    /**
     * Ticket Type
     *
     * @return
     */
    List<TicketType> processFetchTicketType();

}
