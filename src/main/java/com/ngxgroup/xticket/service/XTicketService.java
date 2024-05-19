package com.ngxgroup.xticket.service;

import com.ngxgroup.xticket.model.AppRoles;
import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.model.RoleGroups;
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

    XTicketPayload processUpdateAppUser(XTicketPayload requestPayload, String principal);

    List<AppUser> processFetchInternalAppUsers();

    List<AppUser> processFetchAppUsers();

    void processUserOnline(String principal, boolean userOnline);

    /**
     * Roles
     *
     *
     * @return
     */
    List<RoleGroups> processFetchRoleGroup();

    XTicketPayload processFetchRoleGroup(String id);

    List<AppRoles> processFetchAppRoles();

    XTicketPayload processCreateRoleGroup(XTicketPayload requestPayload, String principal);

    XTicketPayload processDeleteRoleGroup(String id, String principal);

    XTicketPayload processFetchGroupRoles(String groupName);

    XTicketPayload processUpdateGroupRoles(XTicketPayload requestPayload);

    /**
     * Ticket Group
     *
     * @return
     */
    XTicketPayload processFetchTicketGroup();

    XTicketPayload processFetchTicketGroup(String id);

    XTicketPayload processCreateTicketGroup(XTicketPayload requestPayload, String principal);

    XTicketPayload processDeleteTicketGroup(String id, String principal);

    /**
     * Ticket Type
     *
     * @return
     */
    XTicketPayload processFetchTicketType();

    XTicketPayload processFetchTicketType(String id);

    XTicketPayload processCreateTicketType(XTicketPayload requestPayload, String principal);

    XTicketPayload processDeleteTicketType(String id, String principal);

    XTicketPayload processFetchAgentTicketTypes(String principal);

    List<TicketType> processFetchTicketTypeUsingGroup(String ticketGroupCode, String principal);

    /**
     * Ticket SLA
     *
     * @return
     */
    XTicketPayload processFetchTicketSla();

    XTicketPayload processFetchTicketSla(String id);

    XTicketPayload processCreateTicketSla(XTicketPayload requestPayload, String principal);

    XTicketPayload processDeleteTicketSla(String id, String principal);

    /**
     * Ticket Agent
     *
     * @return
     */
    XTicketPayload processFetchTicketAgent();

    XTicketPayload processCreateTicketAgent(XTicketPayload requestPayload, String principal);

    /**
     * Ticket **
     *
     * @param requestPayload
     * @param principal
     * @return
     */
    XTicketPayload processCreateTicket(XTicketPayload requestPayload, String principal);
}
