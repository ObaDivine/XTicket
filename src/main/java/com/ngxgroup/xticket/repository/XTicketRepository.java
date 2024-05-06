package com.ngxgroup.xticket.repository;

import com.ngxgroup.xticket.model.AppRoles;
import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.model.AuditLog;
import com.ngxgroup.xticket.model.GroupRoles;
import com.ngxgroup.xticket.model.Notification;
import com.ngxgroup.xticket.model.RoleGroups;
import com.ngxgroup.xticket.model.TicketComment;
import com.ngxgroup.xticket.model.TicketEscalations;
import com.ngxgroup.xticket.model.TicketGroup;
import com.ngxgroup.xticket.model.TicketReopened;
import com.ngxgroup.xticket.model.TicketTechnicians;
import com.ngxgroup.xticket.model.TicketType;
import com.ngxgroup.xticket.model.TicketUpload;
import com.ngxgroup.xticket.model.Tickets;
import java.util.List;

/**
 *
 * @author bokon
 */
public interface XTicketRepository {

    AppUser getAppUserUsingEmail(String email);

    AppUser getAppUserUsingMobileNumber(String mobileNumber);

    AppUser getAppUserUsingActivationId(String activationId);

    AppUser getAppUserUsingId(long id);

    AppUser updateAppUser(AppUser appUser);

    AppUser deleteAppUser(AppUser appUser);

    AppUser createAppUser(AppUser appUser);

    List<AppUser> getActiveUsers();

    List<AppUser> getDisabledUsers();

    List<AppUser> getPolicyChampions();

    List<AppUser> getUsers();

    AppUser getAppUserUsingUserId(String id);

    List<Notification> getNotifications(String principal);

    AppRoles getRoleUsingRoleName(String roleName);

    AppRoles createAppRole(AppRoles appRole);

    List<AppRoles> getAppRoles();

    RoleGroups getRoleGroupUsingGroupName(String groupName);

    RoleGroups createRoleGroup(RoleGroups roleGroup);

    RoleGroups updateRoleGroup(RoleGroups roleGroup);

    RoleGroups deleteRoleGroups(RoleGroups roleGroup);

    RoleGroups getRoleGroupUsingId(long id);

    List<RoleGroups> getRoleGroupList();

    List<GroupRoles> getGroupRolesUsingRoleGroup(RoleGroups roleGroup);

    GroupRoles createGroupRoles(GroupRoles groupRoles);

    GroupRoles deteleGroupRoles(GroupRoles groupRoles);

    List<AppUser> getAppUsersForTFAFix();

    List<AuditLog> getAuditLogUsingDate(String startDate, String enddate);

    AuditLog createAuditLog(AuditLog auditLog);

    /**
     * Tickets
     *
     * @param ticket
     * @return
     */
    Tickets createTicket(Tickets ticket);

    Tickets updateTicket(Tickets ticket);

    Tickets deleteTicket(Tickets ticket);

    Tickets getTicketUsingId(long id);

    List<Tickets> getTickets();

    List<Tickets> getOpenTickets();

    /**
     * Ticket Reopened
     *
     * @return *
     */
    List<TicketReopened> getTicketReopened();

    List<TicketReopened> getTicketReopenedUsingTicket(Tickets ticket);

    TicketReopened createTicketReopen(TicketReopened ticketReopened);

    TicketReopened updateTicketReopen(TicketReopened ticketReopened);

    TicketReopened deleteTicketReopen(TicketReopened ticketReopened);

    /**
     * Ticket Group
     *
     * @return *
     */
    List<TicketGroup> getTicketGroup();

    TicketGroup getTicketGroupUsingId(long id);

    TicketGroup getTicketGroupUsingCode(String ticketGroupCode);

    TicketGroup getTicketGroupUsingName(String ticketGroupName);

    TicketGroup createTicketGroup(TicketGroup ticketGroup);

    TicketGroup updateTicketGroup(TicketGroup ticketGroup);

    TicketGroup deleteTicketGroup(TicketGroup ticketGroup);

    /**
     * Ticket Type
     *
     * @return *
     */
    List<TicketType> getTicketType();

    TicketType getTicketTypeUsingId(long id);

    TicketType getTicketTypeUsingCode(String ticketTypeCode);

    TicketType getTicketTypeUsingName(String ticketTypeName);

    TicketType createTicketGroup(TicketType ticketType);

    TicketType updateTicketGroup(TicketType ticketType);

    TicketType deleteTicketGroup(TicketType ticketType);

    List<TicketType> getTicketTypeUsingGroup(TicketGroup ticketGroup);

    /**
     * Ticket Technician
     *
     * @return *
     */
    List<TicketTechnicians> getTicketTechnicians();

    TicketTechnicians createTicketTechnician(TicketTechnicians ticketTechnician);

    TicketTechnicians updateTicketTechnician(TicketTechnicians ticketTechnician);

    TicketTechnicians deleteTicketTechnician(TicketTechnicians ticketTechnician);

    /**
     * Ticket Escalation
     *
     * @return *
     */
    List<TicketEscalations> getTicketEscalation();

    List<TicketEscalations> getTicketEscalationUsingTicket(Tickets ticket);

    TicketEscalations createTicketEscalation(TicketEscalations ticketEscalations);

    TicketEscalations updateTicketEscalation(TicketEscalations ticketEscalations);

    TicketEscalations deleteTicketEscalation(TicketEscalations ticketEscalations);
    
     /**
     * Ticket Comments
     *
     * @return *
     */
    List<TicketComment> getTicketComment();

    List<TicketComment> getTicketCommentUsingTicket(Tickets ticket);

    TicketComment createTicketEscalation(TicketComment ticketComment);

    TicketComment updateTicketEscalation(TicketComment ticketComment);

    TicketComment deleteTicketEscalation(TicketComment ticketComment);

         /**
     * Ticket Upload
     *
     * @return *
     */
    List<TicketUpload> getTicketUpload();

    List<TicketUpload> getTicketUploadUsingTicket(Tickets ticket);

    TicketUpload createTicketEscalation(TicketUpload ticketUpload);

    TicketUpload updateTicketEscalation(TicketUpload ticketUpload);

    TicketUpload deleteTicketEscalation(TicketUpload ticketUpload);

}
