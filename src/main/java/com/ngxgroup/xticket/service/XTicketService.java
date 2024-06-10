package com.ngxgroup.xticket.service;

import com.ngxgroup.xticket.model.AppRoles;
import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.model.RoleGroups;
import com.ngxgroup.xticket.model.TicketAgent;
import com.ngxgroup.xticket.model.TicketType;
import com.ngxgroup.xticket.payload.XTicketPayload;
import java.util.List;

/**
 *
 * @author briano
 */
public interface XTicketService {

    XTicketPayload signin(XTicketPayload requestPayload);

    XTicketPayload signup(XTicketPayload requestPayload);

    XTicketPayload signUpActivation(String id);

    XTicketPayload fetchProfile(String principal);

    XTicketPayload changePassword(XTicketPayload requestPayload);

    XTicketPayload forgotPassword(XTicketPayload requestPayload);

    XTicketPayload updateAppUser(XTicketPayload requestPayload, String principal);

    List<AppUser> fetchInternalAppUsers();

    List<AppUser> fetchAppUsers();

    XTicketPayload fetchAllAppUsers();

    void userOnline(String principal, boolean userOnline);

    /**
     * Roles
     *
     *
     * @return
     */
    List<RoleGroups> fetchRoleGroup();

    XTicketPayload fetchRoleGroup(String id);

    List<AppRoles> fetchAppRoles();

    XTicketPayload createRoleGroup(XTicketPayload requestPayload, String principal);

    XTicketPayload deleteRoleGroup(String id, String principal);

    XTicketPayload fetchGroupRoles(String groupName);

    XTicketPayload updateGroupRoles(XTicketPayload requestPayload);

    /**
     * Ticket Group
     *
     * @return
     */
    XTicketPayload fetchTicketGroup();

    XTicketPayload fetchTicketGroup(String id);

    XTicketPayload createTicketGroup(XTicketPayload requestPayload, String principal);

    XTicketPayload deleteTicketGroup(String id, String principal);

    XTicketPayload fetchTicketGroupStatisticsByUser(String principal);

    XTicketPayload fetchTicketStatusStatisticsByUser(String principal);

    /**
     * Ticket Type
     *
     * @return
     */
    XTicketPayload fetchTicketType();

    XTicketPayload fetchTicketType(String id);

    XTicketPayload createTicketType(XTicketPayload requestPayload, String principal);

    XTicketPayload deleteTicketType(String id, String principal);

    XTicketPayload fetchAgentTicketTypes(String principal);

    List<TicketType> fetchTicketTypeUsingGroup(String ticketGroupCode, String principal);

    List<TicketAgent> fetchTicketAgentUsingType(String ticketTypeCode, String principal);

    /**
     * Ticket SLA
     *
     * @return
     */
    XTicketPayload fetchTicketSla();

    XTicketPayload fetchTicketByViolatedSla(XTicketPayload requestPayload);

    XTicketPayload fetchTicketSla(String id);

    XTicketPayload createTicketSla(XTicketPayload requestPayload, String principal);

    XTicketPayload deleteTicketSla(String id, String principal);

    /**
     * Ticket Agent
     *
     * @return
     */
    XTicketPayload fetchTicketAgent();

    XTicketPayload fetchTicketAgent(XTicketPayload requestPayload);

    XTicketPayload fetchTicketClosedByAgent(XTicketPayload requestPayload);

    XTicketPayload createTicketAgent(XTicketPayload requestPayload, String principal);

    /**
     * Ticket **
     *
     * @param requestPayload
     * @param principal
     * @return
     */
    XTicketPayload createTicket(XTicketPayload requestPayload, String principal);

    XTicketPayload replyTicket(XTicketPayload requestPayload, String principal);

    XTicketPayload fetchTicketUsingId(String id);

    XTicketPayload fetchOpenTicket(String principal);

    XTicketPayload fetchOpenTicket();

    XTicketPayload fetchOpenTicket(XTicketPayload requestPayload);

    XTicketPayload fetchOpenTicketForAgent(String principal);

    XTicketPayload fetchClosedTicket(String principal);

    XTicketPayload fetchClosedTicket(XTicketPayload requestPayload);

    XTicketPayload fetchClosedTicket();

    XTicketPayload fetchTicketByUser(String principal);

    XTicketPayload closeTicket(String id, String ticketReopened, String ticketReopenedId, String principal);

    XTicketPayload createReopenTicket(XTicketPayload requestPayload, String principal);

    XTicketPayload fetchReopenedTicket(XTicketPayload requestPayload);

    XTicketPayload createTicketReassignment(XTicketPayload requestPayload, String principal);

    XTicketPayload fetchReassignedTicket(XTicketPayload requestPayload);

    XTicketPayload fetchTicketFullDetails(String ticketId);
}
