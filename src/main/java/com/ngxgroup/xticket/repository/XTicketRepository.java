package com.ngxgroup.xticket.repository;

import com.ngxgroup.xticket.model.AppRoles;
import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.model.AuditLog;
import com.ngxgroup.xticket.model.AutomatedTicket;
import com.ngxgroup.xticket.model.ContactUs;
import com.ngxgroup.xticket.model.Department;
import com.ngxgroup.xticket.model.DocumentUpload;
import com.ngxgroup.xticket.model.Entities;
import com.ngxgroup.xticket.model.GroupRoles;
import com.ngxgroup.xticket.model.KnowledgeBase;
import com.ngxgroup.xticket.model.KnowledgeBaseCategory;
import com.ngxgroup.xticket.model.PushNotification;
import com.ngxgroup.xticket.model.PublicHolidays;
import com.ngxgroup.xticket.model.RoleGroups;
import com.ngxgroup.xticket.model.ServiceUnit;
import com.ngxgroup.xticket.model.TicketComment;
import com.ngxgroup.xticket.model.TicketEscalations;
import com.ngxgroup.xticket.model.TicketGroup;
import com.ngxgroup.xticket.model.TicketReopened;
import com.ngxgroup.xticket.model.TicketAgent;
import com.ngxgroup.xticket.model.TicketReassign;
import com.ngxgroup.xticket.model.TicketSla;
import com.ngxgroup.xticket.model.TicketStatus;
import com.ngxgroup.xticket.model.TicketStatusChange;
import com.ngxgroup.xticket.model.TicketType;
import com.ngxgroup.xticket.model.TicketUpload;
import com.ngxgroup.xticket.model.Tickets;
import java.time.LocalDate;
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

    List<AppUser> getInternalAppUsers();

    List<AppUser> getUsers();

    List<AppUser> getAgentAppUsers();

    List<AppUser> getAppUserUsingRoleGroup(RoleGroups roleGroup);

    List<AppUser> getAppUserUsingEntity(Entities entity);

    AppUser getAppUserUsingUserId(String id);

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

    Tickets getTicketUsingAgent(TicketAgent ticketAgent);

    List<Tickets> getTickets();

    List<Tickets> getTicketsByStatus(TicketStatus ticketStatus);

    List<Tickets> getClosedTicketsByUser(AppUser appUser, TicketStatus ticketStatus);

    List<Tickets> getClosedTickets(LocalDate startDate, LocalDate endDate, TicketStatus ticketStatus);

    List<Tickets> getClosedTickets(LocalDate startDate, LocalDate endDate, TicketStatus ticketStatus, Entities entity);

    List<Tickets> getClosedTickets(LocalDate startDate, LocalDate endDate, TicketStatus ticketStatus, Department department);

    List<Tickets> getTicketClosedByAgent(AppUser appUser, LocalDate startDate, LocalDate endDate, TicketStatus ticketStatus);

    List<Tickets> getClosedAutomatedTickets(LocalDate startDate, LocalDate endDate, TicketStatus ticketStatus);

    List<Tickets> getTicketClosedByAgent(AppUser appUser, TicketStatus ticketStatus);

    int getTicketOpenForAgentByGroup(AppUser appUser, TicketStatus ticketStatus, TicketGroup ticketGroup);

    List<Tickets> getViolatedTickets(LocalDate startDate, LocalDate endDate);

    List<Tickets> getTicketsWithinSLA(LocalDate startDate, LocalDate endDate, TicketStatus ticketStatus);

    List<Tickets> getTicketsByUser(AppUser appUser);

    List<Tickets> getOpenTicketsByUser(AppUser appUser, TicketStatus ticketStatus);

    List<Tickets> getOpenTicketsByType(TicketType ticketType, TicketStatus ticketStatus);

    List<Tickets> getOpenAgentTickets(AppUser appUser, TicketStatus ticketStatus);

    List<Tickets> getTicketsUsingStatus(TicketStatus ticketStatus);

    List<Tickets> getTicketsByServiceUnit(LocalDate startDate, LocalDate endDate, ServiceUnit serviceUnit);

    List<Tickets> getClosedTickets(LocalDate startDate, LocalDate endDate, TicketStatus ticketStatus, ServiceUnit serviceUnit);

    List<DocumentUpload> getDocumentUploadUsingTicket(Tickets ticket);

    DocumentUpload createDocumentUpload(DocumentUpload documentUpload);

    Tickets getTicketUsingTicketGroup(TicketGroup ticketGroup);

    Tickets getTicketUsingTicketType(TicketType ticketType);

    Tickets getTicketUsingTicketId(String ticketId);

    /**
     * Ticket Reopened
     *
     * @return *
     */
    List<TicketReopened> getTicketReopened();

    List<TicketReopened> getTicketReopenedUsingTicket(Tickets ticket);

    List<TicketReopened> getDistinctTicketReopened(LocalDate startDate, LocalDate endDate);

    TicketReopened createTicketReopen(TicketReopened ticketReopened);

    TicketReopened updateTicketReopen(TicketReopened ticketReopened);

    TicketReopened deleteTicketReopen(TicketReopened ticketReopened);

    TicketReopened getTicketReopenedUsingId(long id);

    TicketReopened getMostRecentTicketReopenedUsingTicket(Tickets ticket);

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

    List<TicketType> getNonAutomatedTicketType();

    TicketType getTicketTypeUsingId(long id);

    TicketType getTicketTypeUsingCode(String ticketTypeCode);

    TicketType getTicketTypeUsingName(String ticketTypeName);

    TicketType createTicketType(TicketType ticketType);

    TicketType updateTicketType(TicketType ticketType);

    TicketType deleteTicketType(TicketType ticketType);

    List<TicketType> getTicketTypeUsingTicketGroup(TicketGroup ticketGroup);

    List<TicketType> getTicketTypeUsingTicketGroup(TicketGroup ticketGroup, boolean userType);

    List<TicketType> getTicketTypeUsingTicketSla(TicketSla ticketSla);

    List<TicketType> getTicketTypeUsingServiceUnit(ServiceUnit serviceUnit);

    int getTicketGroupByUser(AppUser appUser, TicketGroup ticketGroup);

    /**
     * Ticket SLA
     *
     * @return *
     */
    List<TicketSla> getTicketSla();

    TicketSla getTicketSlaUsingId(long id);

    TicketSla getTicketSlaUsingName(String ticketSlaName);

    TicketSla createTicketSla(TicketSla ticketSla);

    TicketSla updateTicketSla(TicketSla ticketSla);

    TicketSla deleteTicketSla(TicketSla ticketSla);

    /**
     * Ticket Agent
     *
     * @return *
     */
    List<TicketAgent> getTicketAgent();

    List<AppUser> getDistinctTicketAgent();

    List<TicketAgent> getTicketAgent(AppUser ticketAgent);

    List<TicketAgent> getTicketAgentUsingTicketType(TicketType ticketType);

    List<TicketAgent> getTicketAgentUsingTicketType(AppUser appUser, TicketType ticketType);

    TicketAgent createTicketAgent(TicketAgent ticketAgent);

    TicketAgent updateTicketAgent(TicketAgent ticketAgent);

    TicketAgent deleteTicketAgent(TicketAgent ticketAgent);

    TicketAgent getTicketAgentUsingId(long id);

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

    /**
     * Ticket Comment
     *
     * @param ticketComment
     * @return *
     */
    TicketComment createTicketComment(TicketComment ticketComment);

    /**
     * Ticket Reassignment
     *
     * @param ticketReassign
     * @return *
     */
    TicketReassign createTicketReassign(TicketReassign ticketReassign);

    TicketReassign updateTicketReassign(TicketReassign ticketReassign);

    TicketReassign deleteTicketReassign(TicketReassign ticketReassign);

    List<TicketReassign> getTicketReassignedUsingTicket(Tickets ticket);

    List<TicketReassign> getDistinctTicketReassigned(LocalDate startDate, LocalDate endDate);

    /**
     * Entities
     *
     * @return *
     */
    List<Entities> getEntities();

    Entities getEntitiesUsingId(long id);

    Entities getEntitiesUsingCode(String entityCode);

    Entities getEntitiesUsingName(String entityName);

    Entities createEntities(Entities entity);

    Entities updateEntities(Entities entity);

    Entities deleteEntities(Entities entity);

    /**
     * Department
     *
     * @return *
     */
    List<Department> getDepartment();

    Department getDepartmentUsingId(long id);

    Department getDepartmentUsingCode(String entityCode);

    Department getDepartmentUsingName(String departmentName);

    Department createDepartment(Department department);

    Department updateDepartment(Department department);

    Department deleteDepartment(Department department);

    List<ServiceUnit> getServiceUnitUsingDepartment(Department department);

    List<Department> getDepartmentUsingEntity(Entities entity);

    /**
     * Service Unit
     *
     * @return *
     */
    List<ServiceUnit> getServiceUnit();

    ServiceUnit getServiceUnitUsingId(long id);

    ServiceUnit getServiceUnitUsingCode(String serviceUnitCode);

    ServiceUnit getServiceUnitUsingName(String serviceUnitName);

    ServiceUnit createServiceUnit(ServiceUnit serviceUnit);

    ServiceUnit updateServiceUnit(ServiceUnit serviceUnit);

    ServiceUnit deleteServiceUnit(ServiceUnit serviceUnit);

    List<ServiceUnit> getServiceUnitUsingEntity(Entities entity);

    /**
     * Ticket Status
     *
     * @return *
     */
    List<TicketStatus> getTicketStatus();

    TicketStatus getTicketStatusUsingId(long id);

    TicketStatus getTicketStatusUsingCode(String ticketStatusCode);

    TicketStatus getTicketStatusUsingName(String ticketStatusName);

    TicketStatus createTicketStatus(TicketStatus ticketStatus);

    TicketStatus updateTicketStatus(TicketStatus ticketStatus);

    TicketStatus deleteTicketStatus(TicketStatus ticketStatus);

    TicketStatusChange createTicketStatusChange(TicketStatusChange ticketStatusChange);

    List<PublicHolidays> getPublicHolidays();

    PublicHolidays getPublicHoliday(LocalDate holiday);

    PublicHolidays createPublicHoliday(PublicHolidays publicHoliday);

    PublicHolidays updatePublicHoliday(PublicHolidays publicHoliday);

    /**
     * Knowledge Base
     *
     * @return *
     */
    List<KnowledgeBaseCategory> getKnowledgeBaseCategory();

    List<KnowledgeBase> getKnowledgeBase();

    List<KnowledgeBase> getKnowledgeBaseUsingCategory(KnowledgeBaseCategory category);

    List<KnowledgeBase> getKnowledgeBasePopularArticle();

    List<KnowledgeBase> getKnowledgeBaseLatestArticle();

    List<KnowledgeBase> getKnowledgeBaseUsingTags(String tag);

    KnowledgeBase getKnowledgeBaseUsingId(long id);

    KnowledgeBaseCategory getKnowledgeBaseCategoryUsingId(long id);

    KnowledgeBaseCategory getKnowledgeBaseCategoryUsingCode(String categoryCode);

    KnowledgeBaseCategory getKnowledgeBaseCategoryUsingName(String categoryName);

    KnowledgeBase getKnowledgeBaseUsingTitle(String title);

    KnowledgeBase createKnowledgeBase(KnowledgeBase knowledgeBase);

    KnowledgeBase updateKnowledgeBase(KnowledgeBase knowledgeBase);

    KnowledgeBase deleteKnowledgeBase(KnowledgeBase knowledgeBase);

    KnowledgeBaseCategory createKnowledgeBaseCategory(KnowledgeBaseCategory knowledgeBaseCategory);

    KnowledgeBaseCategory updateKnowledgeBaseCategory(KnowledgeBaseCategory knowledgeBaseCategory);

    KnowledgeBaseCategory deleteKnowledgeBaseCategory(KnowledgeBaseCategory knowledgeBaseCategory);

    List<AuditLog> getAuditLog(LocalDate startDate, LocalDate endDate);

    ContactUs createContactUs(ContactUs contactUs);

    /**
     * Automated Ticket
     *
     * @return *
     */
    List<TicketType> getAutomatedTicketType();

    List<AutomatedTicket> getAutomatedTicket();

    List<AutomatedTicket> getActiveAutomatedTicket();

    AutomatedTicket getAutomatedTicketUsingId(long id);

    Tickets getAutomatedTicketUsingTicketType(TicketType ticketType);

    AutomatedTicket createAutomatedTicket(AutomatedTicket automatedTicket);

    AutomatedTicket updateAutomatedTicket(AutomatedTicket automatedTicket);

    AutomatedTicket deleteAutomatedTicket(AutomatedTicket automatedTicket);

    AutomatedTicket getAutomatedTicketUsingType(TicketType ticketType);

    /**
     * Push Notification
     *
     * @return *
     */
    List<PushNotification> getPushNotification();

    PushNotification getPushNotificationUsingId(long id);

    List<PushNotification> getPushNotificationBySendTo(String sendTo);

    List<PushNotification> getPushNotificationUsingBatchId(int batchId);

    PushNotification createPushNotification(PushNotification pushNotification);

    PushNotification updatePushNotification(PushNotification pushNotification);

    PushNotification deletePushNotification(PushNotification pushNotification);
}
