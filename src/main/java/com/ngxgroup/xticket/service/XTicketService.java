package com.ngxgroup.xticket.service;

import com.ngxgroup.xticket.model.AppRoles;
import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.model.GroupRoles;
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

    List<GroupRoles> fetchUserRoles(String principal);

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
     * @param includeAutomatedTicket
     * @return
     */
    XTicketPayload fetchTicketType(boolean includeAutomatedTicket);

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

    XTicketPayload fetchTicketByWithinSla(XTicketPayload requestPayload);

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

    XTicketPayload agentReplyTicket(XTicketPayload requestPayload, String principal);

    XTicketPayload fetchTicketUsingId(String id);

    XTicketPayload fetchOpenTicket(String principal);

    XTicketPayload fetchOpenTicket();

    XTicketPayload fetchOpenTicket(XTicketPayload requestPayload);

    XTicketPayload fetchOpenTicketForAgent(String principal, String transType);

    XTicketPayload fetchOpenTicketGroupStatisticsForAgent(String principal);

    XTicketPayload fetchOpenTicketAboutToViolateSlaForAgent(String principal);

    XTicketPayload fetchOpenTicketWithCriticalSlaForAgent(String principal);

    XTicketPayload fetchClosedTicket(String principal);

    XTicketPayload fetchClosedTicket(String principal, String transType);

    XTicketPayload fetchClosedTicket(XTicketPayload requestPayload);

    XTicketPayload fetchClosedTicket();

    XTicketPayload fetchTicketByUser(String principal);

    XTicketPayload closeTicket(XTicketPayload requestPayload, String principal);

    XTicketPayload createReopenTicket(XTicketPayload requestPayload, String principal);

    XTicketPayload fetchReopenedTicket(XTicketPayload requestPayload);

    XTicketPayload createTicketReassignment(XTicketPayload requestPayload, String principal);

    XTicketPayload fetchReassignedTicket(XTicketPayload requestPayload);

    XTicketPayload fetchTicketFullDetails(String ticketId);

    /**
     * Entity Transactions
     *
     *
     * @param requestPayload
     * @return
     */

    XTicketPayload fetchTicketByEntityToEntity(XTicketPayload requestPayload);

    XTicketPayload fetchEntity();

    XTicketPayload fetchEntity(String id);

    XTicketPayload createEntity(XTicketPayload requestPayload, String principal);

    XTicketPayload deleteEntity(String id, String principal);

    XTicketPayload fetchServiceEffectivenessByEntity(XTicketPayload responseData, XTicketPayload requestPayload);

    XTicketPayload fetchServiceHoursByEntity(XTicketPayload responseData, XTicketPayload requestPayload);

    /**
     * Department Transactions
     *
     *
     * @param requestPayload
     * @return
     */

    XTicketPayload fetchTicketByDepartmentToEntity(XTicketPayload requestPayload);

    XTicketPayload fetchDepartment();

    XTicketPayload fetchDepartment(String id);

    XTicketPayload createDepartment(XTicketPayload requestPayload, String principal);

    XTicketPayload deleteDepartment(String id, String principal);

    XTicketPayload fetchServiceEffectivenessByDepartment(XTicketPayload responseData, XTicketPayload requestPayload);

    XTicketPayload fetchServiceHoursByDepartment(XTicketPayload responseData, XTicketPayload requestPayload);

    /**
     * Service Unit Transactions
     *
     *
     * @param requestPayload
     * @return
     */
    
    XTicketPayload fetchTicketByServiceUnitToEntity(XTicketPayload requestPayload);

    XTicketPayload fetchTicketByServiceRating(XTicketPayload requestPayload);

    XTicketPayload fetchServiceUnit();

    XTicketPayload fetchServiceUnit(String id);

    XTicketPayload createServiceUnit(XTicketPayload requestPayload, String principal);

    XTicketPayload deleteServiceUnit(String id, String principal);

    XTicketPayload fetchServiceEffectivenessByServiceUnit(XTicketPayload responseData, XTicketPayload requestPayload);

    XTicketPayload fetchServiceHoursByServiceUnit(XTicketPayload responseData, XTicketPayload requestPayload);

    /**
     * Ticket Status Transactions
     *
     *
     * @return
     */
    XTicketPayload fetchTicketStatus();

    XTicketPayload fetchTicketStatus(String id);

    XTicketPayload fetchTicketStatusForReply();

    XTicketPayload createTicketStatus(XTicketPayload requestPayload, String principal);

    XTicketPayload deleteTicketStatus(String id, String principal);

    /**
     * Ticket Automation Transactions
     *
     *
     * @return
     */
    XTicketPayload fetchAutomatedTicket();

    XTicketPayload fetchAutomatedTicket(String id);

    XTicketPayload fetchAutomatedTicketType();

    XTicketPayload fetchTicketByAutomation(XTicketPayload requestPayload);

    XTicketPayload createAutomatedTicket(XTicketPayload requestPayload, String principal);

    XTicketPayload deleteAutomatedTicket(String id, String principal);

    /**
     * Knowledge Base
     *
     *
     * @return
     */
    XTicketPayload fetchKnowledgeBaseCategory();

    XTicketPayload fetchKnowledgeBaseCategory(String id);

    XTicketPayload createKnowledgeBaseCategory(XTicketPayload requestPayload, String principal);

    XTicketPayload deleteKnowledgeBaseCategory(String id, String principal);

    XTicketPayload fetchKnowledgeBaseContent();

    XTicketPayload fetchKnowledgeBaseContentUsingCategory(String id);

    XTicketPayload fetchKnowledgeBase();

    XTicketPayload fetchKnowledgeBasePopularArticle();

    XTicketPayload fetchKnowledgeBaseLatestArticle();

    XTicketPayload fetchKnowledgeBasePopularTag();

    XTicketPayload fetchKnowledgeBaseContent(String id);

    XTicketPayload searchKnowledgeBaseContent(String searchKeyWord);

    XTicketPayload createKnowledgeBaseContent(XTicketPayload requestPayload, String principal);

    XTicketPayload deleteKnowledgeBaseContent(String id, String principal);

    /**
     * Audit Log
     *
     *
     * @param requestPayload
     * @return
     */
    XTicketPayload fetchAuditLog(XTicketPayload requestPayload);

    XTicketPayload createContactUs(XTicketPayload requestPayload);

}
