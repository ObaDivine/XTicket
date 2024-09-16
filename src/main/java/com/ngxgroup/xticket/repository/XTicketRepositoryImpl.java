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
import com.ngxgroup.xticket.model.Notification;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author bokon
 */
@Repository
@Transactional
public class XTicketRepositoryImpl implements XTicketRepository {

    @PersistenceContext
    EntityManager em;

    @Override
    public AppUser getAppUserUsingEmail(String email) {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.email = :email", AppUser.class)
                .setParameter("email", email);
        List<AppUser> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public AppUser getAppUserUsingMobileNumber(String mobileNumber) {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.mobileNumber = :mobileNumber", AppUser.class)
                .setParameter("mobileNumber", mobileNumber);
        List<AppUser> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public AppUser getAppUserUsingActivationId(String activationId) {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.activationId = :activationId", AppUser.class)
                .setParameter("activationId", activationId);
        List<AppUser> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public List<AppUser> getAgentAppUsers() {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.agent = true", AppUser.class);
        List<AppUser> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<AppUser> getAppUserUsingEntity(Entities entity) {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.entity = :entity", AppUser.class)
                .setParameter("entity", entity);
        List<AppUser> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<AppUser> getAppUserUsingRoleGroup(RoleGroups roleGroup) {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.role = :roleGroup", AppUser.class)
                .setParameter("roleGroup", roleGroup);
        List<AppUser> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public AppUser updateAppUser(AppUser appUser) {
        em.merge(appUser);
        em.flush();
        return appUser;
    }

    @Override
    public List<Notification> getNotifications(String principal) {
        TypedQuery<Notification> query = em.createQuery("SELECT p FROM Notification p WHERE p.sentTo = :principal OR p.sentTo = 'All'", Notification.class)
                .setParameter("principal", principal);
        List<Notification> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public AppRoles getRoleUsingRoleName(String roleName) {
        TypedQuery<AppRoles> query = em.createQuery("SELECT p FROM AppRoles p WHERE p.roleName = :roleName", AppRoles.class)
                .setParameter("roleName", roleName);
        List<AppRoles> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public AppRoles createAppRole(AppRoles appRole) {
        em.persist(appRole);
        em.flush();
        return appRole;
    }

    @Override
    public List<AppRoles> getAppRoles() {
        TypedQuery<AppRoles> query = em.createQuery("SELECT p FROM AppRoles p", AppRoles.class);
        List<AppRoles> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<RoleGroups> getRoleGroupList() {
        TypedQuery<RoleGroups> query = em.createQuery("SELECT p FROM RoleGroups p", RoleGroups.class);
        List<RoleGroups> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public RoleGroups getRoleGroupUsingGroupName(String groupName) {
        TypedQuery<RoleGroups> query = em.createQuery("SELECT p FROM RoleGroups p WHERE p.groupName = :groupName", RoleGroups.class)
                .setParameter("groupName", groupName);
        List<RoleGroups> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public RoleGroups createRoleGroup(RoleGroups roleGroup) {
        em.persist(roleGroup);
        em.flush();
        return roleGroup;
    }

    @Override
    public RoleGroups updateRoleGroup(RoleGroups roleGroup) {
        em.merge(roleGroup);
        em.flush();
        return roleGroup;
    }

    @Override
    public RoleGroups deleteRoleGroups(RoleGroups roleGroup) {
        em.remove(em.contains(roleGroup) ? roleGroup : em.merge(roleGroup));
        em.flush();
        return roleGroup;
    }

    @Override
    public RoleGroups getRoleGroupUsingId(long id) {
        TypedQuery<RoleGroups> query = em.createQuery("SELECT p FROM RoleGroups p WHERE p.id = :id", RoleGroups.class)
                .setParameter("id", id);
        List<RoleGroups> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public List<GroupRoles> getGroupRolesUsingRoleGroup(RoleGroups roleGroup) {
        TypedQuery<GroupRoles> query = em.createQuery("SELECT p FROM GroupRoles p WHERE p.roleGroup = :roleGroup", GroupRoles.class)
                .setParameter("roleGroup", roleGroup);
        List<GroupRoles> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public GroupRoles createGroupRoles(GroupRoles groupRoles) {
        em.persist(groupRoles);
        em.flush();
        return groupRoles;
    }

    @Override
    public AppUser getAppUserUsingId(long id) {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.id = :id", AppUser.class)
                .setParameter("id", id);
        List<AppUser> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public AppUser deleteAppUser(AppUser appUser) {
        em.remove(em.contains(appUser) ? appUser : em.merge(appUser));
        em.flush();
        return appUser;
    }

    @Override
    public AppUser createAppUser(AppUser appUser) {
        em.persist(appUser);
        em.flush();
        return appUser;
    }

    @Override
    public List<AppUser> getUsers() {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p", AppUser.class);
        List<AppUser> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public AppUser getAppUserUsingUserId(String userId) {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.userId = :userId", AppUser.class)
                .setParameter("userId", userId);
        List<AppUser> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public List<AppUser> getAppUsersForTFAFix() {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.twoFactorSecretKey = 'NA'", AppUser.class);
        List<AppUser> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<AppUser> getActiveUsers() {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.isEnabled = true", AppUser.class);
        List<AppUser> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<AppUser> getDisabledUsers() {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.isEnabled = false", AppUser.class);
        List<AppUser> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<AppUser> getInternalAppUsers() {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.internal = true", AppUser.class);
        List<AppUser> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<AuditLog> getAuditLogUsingDate(String startDate, String endDate) {
        TypedQuery<AuditLog> query = em.createQuery("SELECT c FROM AuditLog c WHERE c.createdAt >= '" + startDate + "'" + " AND c.createdAt <= '" + endDate + "'", AuditLog.class);
        List<AuditLog> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public AuditLog createAuditLog(AuditLog auditLog) {
        em.persist(auditLog);
        em.flush();
        return auditLog;
    }

    @Override
    public GroupRoles deteleGroupRoles(GroupRoles roleGroup) {
        em.remove(em.contains(roleGroup) ? roleGroup : em.merge(roleGroup));
        em.flush();
        return roleGroup;
    }

    @Override
    public Tickets createTicket(Tickets ticket) {
        em.persist(ticket);
        em.flush();
        return ticket;
    }

    @Override
    public Tickets updateTicket(Tickets ticket) {
        em.merge(ticket);
        em.flush();
        return ticket;
    }

    @Override
    public Tickets deleteTicket(Tickets ticket) {
        em.remove(em.contains(ticket) ? ticket : em.merge(ticket));
        em.flush();
        return ticket;
    }

    @Override
    public Tickets getTicketUsingTicketId(String ticketId) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.ticketId = :ticketId", Tickets.class)
                .setParameter("ticketId", ticketId);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public Tickets getTicketUsingId(long id) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.id = :id", Tickets.class)
                .setParameter("id", id);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public List<Tickets> getTickets() {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p", Tickets.class);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<Tickets> getTicketsUsingStatus(TicketStatus ticketStatus) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.ticketStatus = :ticketStatus", Tickets.class)
                .setParameter("ticketStatus", ticketStatus);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<Tickets> getOpenTicketsByType(TicketType ticketType, TicketStatus ticketStatus) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.ticketType = :ticketType AND p.ticketStatus = :ticketStatus", Tickets.class)
                .setParameter("ticketType", ticketType)
                .setParameter("ticketStatus", ticketStatus);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<Tickets> getOpenAgentTickets(AppUser appUser, TicketStatus ticketStatus) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.ticketAgent.agent = :appUser AND p.ticketStatus != :ticketStatus", Tickets.class)
                .setParameter("appUser", appUser)
                .setParameter("ticketStatus", ticketStatus);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public Tickets getTicketUsingAgent(TicketAgent ticketAgent) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.ticketAgent = :ticketAgent", Tickets.class)
                .setParameter("ticketAgent", ticketAgent);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public Tickets getTicketUsingTicketGroup(TicketGroup ticketGroup) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.ticketGroup = :ticketGroup", Tickets.class)
                .setParameter("ticketGroup", ticketGroup);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public Tickets getTicketUsingTicketType(TicketType ticketType) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.ticketType = :ticketType", Tickets.class)
                .setParameter("ticketType", ticketType);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public List<Tickets> getTicketsByStatus(TicketStatus ticketStatus) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.ticketStatus = :ticketStatus", Tickets.class)
                .setParameter("ticketStatus", ticketStatus);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<Tickets> getClosedTickets(LocalDate startDate, LocalDate endDate, TicketStatus ticketStatus) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.ticketStatus = :ticketStatus AND p.createdAt >= :startDate AND p.createdAt <= :endDate AND p.automated = false", Tickets.class)
                .setParameter("startDate", startDate.atStartOfDay())
                .setParameter("endDate", endDate.atTime(23, 59))
                .setParameter("ticketStatus", ticketStatus);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<Tickets> getClosedTickets(LocalDate startDate, LocalDate endDate, TicketStatus ticketStatus, Entities entity) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.ticketStatus = :ticketStatus AND p.createdAt >= :startDate AND p.createdAt <= :endDate AND p.closedBy.entity = :entity AND p.automated = false", Tickets.class)
                .setParameter("startDate", startDate.atStartOfDay())
                .setParameter("endDate", endDate.atTime(23, 59))
                .setParameter("ticketStatus", ticketStatus)
                .setParameter("entity", entity);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }
    
        @Override
    public List<Tickets> getClosedTickets(LocalDate startDate, LocalDate endDate, TicketStatus ticketStatus, Department department) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.ticketStatus = :ticketStatus AND p.createdAt >= :startDate AND p.createdAt <= :endDate AND p.closedBy.department = :department AND p.automated = false", Tickets.class)
                .setParameter("startDate", startDate.atStartOfDay())
                .setParameter("endDate", endDate.atTime(23, 59))
                .setParameter("ticketStatus", ticketStatus)
                .setParameter("department", department);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<Tickets> getClosedAutomatedTickets(LocalDate startDate, LocalDate endDate, TicketStatus ticketStatus) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.ticketStatus = :ticketStatus AND p.createdAt >= :startDate AND p.createdAt <= :endDate AND p.automated = true", Tickets.class)
                .setParameter("startDate", startDate.atStartOfDay())
                .setParameter("endDate", endDate.atTime(23, 59))
                .setParameter("ticketStatus", ticketStatus);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<Tickets> getTicketsByServiceUnit(LocalDate startDate, LocalDate endDate, ServiceUnit serviceUnit, TicketStatus ticketStatus) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate AND p.ticketType.serviceUnit = :serviceUnit AND p.ticketStatus = :ticketStatus", Tickets.class)
                .setParameter("startDate", startDate.atStartOfDay())
                .setParameter("endDate", endDate.atTime(23, 59))
                .setParameter("serviceUnit", serviceUnit)
                .setParameter("ticketStatus", ticketStatus);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<Tickets> getTicketClosedByAgent(AppUser appUser, LocalDate startDate, LocalDate endDate, TicketStatus ticketStatus) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.ticketStatus = :ticketStatus AND p.closedBy = :appUser AND p.closedAt >= :startDate AND p.closedAt <= :endDate", Tickets.class)
                .setParameter("appUser", appUser)
                .setParameter("startDate", startDate.atStartOfDay())
                .setParameter("endDate", endDate.atTime(23, 59))
                .setParameter("ticketStatus", ticketStatus);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<Tickets> getTicketClosedByAgent(AppUser appUser, TicketStatus ticketStatus) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.ticketStatus = :ticketStatus AND p.closedBy = :appUser", Tickets.class)
                .setParameter("appUser", appUser)
                .setParameter("ticketStatus", ticketStatus);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<Tickets> getViolatedTickets(LocalDate startDate, LocalDate endDate) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.slaViolated = true AND p.slaViolatedAt >= :startDate AND p.slaViolatedAt <= :endDate", Tickets.class)
                .setParameter("startDate", startDate.atStartOfDay())
                .setParameter("endDate", endDate.atTime(23, 59));
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<Tickets> getTicketsWithinSLA(LocalDate startDate, LocalDate endDate, TicketStatus ticketStatus) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.slaViolated = false AND p.ticketStatus = :ticketStatus AND p.closedAt >= :startDate AND p.closedAt <= :endDate", Tickets.class)
                .setParameter("startDate", startDate.atStartOfDay())
                .setParameter("endDate", endDate.atTime(23, 59))
                .setParameter("ticketStatus", ticketStatus);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<Tickets> getClosedTicketsByUser(AppUser appUser, TicketStatus ticketStatus) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.createdBy = :appUser AND p.ticketStatus = :ticketStatus", Tickets.class)
                .setParameter("appUser", appUser)
                .setParameter("ticketStatus", ticketStatus);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }
    
        @Override
    public List<Tickets> getOpenTicketsByUser(AppUser appUser, TicketStatus ticketStatus) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.createdBy = :appUser AND p.ticketStatus != :ticketStatus", Tickets.class)
                .setParameter("appUser", appUser)
                .setParameter("ticketStatus", ticketStatus);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<Tickets> getTicketsByUser(AppUser appUser) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.createdBy = :appUser", Tickets.class)
                .setParameter("appUser", appUser);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<Tickets> getTicketsByServiceUnit(LocalDate startDate, LocalDate endDate, ServiceUnit serviceUnit) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate AND p.ticketType.serviceUnit = :serviceUnit", Tickets.class)
                .setParameter("startDate", startDate.atStartOfDay())
                .setParameter("endDate", endDate.atTime(23, 59))
                .setParameter("serviceUnit", serviceUnit);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<TicketReopened> getTicketReopened() {
        TypedQuery<TicketReopened> query = em.createQuery("SELECT p FROM TicketReopened p", TicketReopened.class);
        List<TicketReopened> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<TicketReopened> getTicketReopenedUsingTicket(Tickets ticket) {
        TypedQuery<TicketReopened> query = em.createQuery("SELECT p FROM TicketReopened p WHERE p.ticket = :ticket", TicketReopened.class)
                .setParameter("ticket", ticket);
        List<TicketReopened> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<TicketReopened> getDistinctTicketReopened(LocalDate startDate, LocalDate endDate) {
        TypedQuery<TicketReopened> query = em.createQuery("SELECT DISTINCT(p) FROM TicketReopened p WHERE p.reopenedAt >= :startDate AND p.reopenedAt <= :endDate", TicketReopened.class)
                .setParameter("startDate", startDate.atStartOfDay())
                .setParameter("endDate", endDate.atTime(23, 59));
        List<TicketReopened> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public TicketReopened getTicketReopenedUsingId(long id) {
        TypedQuery<TicketReopened> query = em.createQuery("SELECT p FROM TicketReopened p WHERE p.id = :id", TicketReopened.class)
                .setParameter("id", id);
        List<TicketReopened> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public TicketReopened getMostRecentTicketReopenedUsingTicket(Tickets ticket) {
        TypedQuery<TicketReopened> query = em.createQuery("SELECT p FROM TicketReopened p WHERE p.ticket = :ticket ORDER BY p.id DESC", TicketReopened.class)
                .setParameter("ticket", ticket).setMaxResults(1);
        List<TicketReopened> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public TicketReopened createTicketReopen(TicketReopened ticketReopened) {
        em.persist(ticketReopened);
        em.flush();
        return ticketReopened;
    }

    @Override
    public TicketReopened updateTicketReopen(TicketReopened ticketReopened) {
        em.merge(ticketReopened);
        em.flush();
        return ticketReopened;
    }

    @Override
    public TicketReopened deleteTicketReopen(TicketReopened ticketReopened) {
        em.remove(em.contains(ticketReopened) ? ticketReopened : em.merge(ticketReopened));
        em.flush();
        return ticketReopened;
    }

    @Override
    public List<TicketGroup> getTicketGroup() {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM TicketGroup p", TicketGroup.class);
        List<TicketGroup> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public TicketGroup getTicketGroupUsingId(long id) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM TicketGroup p WHERE p.id = :id", TicketGroup.class)
                .setParameter("id", id);
        List<TicketGroup> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public TicketGroup getTicketGroupUsingCode(String ticketGroupCode) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM TicketGroup p WHERE p.ticketGroupCode = :ticketGroupCode", TicketGroup.class)
                .setParameter("ticketGroupCode", ticketGroupCode);
        List<TicketGroup> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public TicketGroup getTicketGroupUsingName(String ticketGroupName) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM TicketGroup p WHERE p.ticketGroupName = :ticketGroupName", TicketGroup.class)
                .setParameter("ticketGroupName", ticketGroupName);
        List<TicketGroup> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public TicketGroup createTicketGroup(TicketGroup ticketGroup) {
        em.persist(ticketGroup);
        em.flush();
        return ticketGroup;
    }

    @Override
    public TicketGroup updateTicketGroup(TicketGroup ticketGroup) {
        em.merge(ticketGroup);
        em.flush();
        return ticketGroup;
    }

    @Override
    public TicketGroup deleteTicketGroup(TicketGroup ticketGroup) {
        em.remove(em.contains(ticketGroup) ? ticketGroup : em.merge(ticketGroup));
        em.flush();
        return ticketGroup;
    }

    @Override
    public List<TicketType> getTicketType() {
        TypedQuery<TicketType> query = em.createQuery("SELECT p FROM TicketType p WHERE p.automated = false", TicketType.class);
        List<TicketType> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public TicketType getTicketTypeUsingId(long id) {
        TypedQuery<TicketType> query = em.createQuery("SELECT p FROM TicketType p WHERE p.id = :id", TicketType.class)
                .setParameter("id", id);
        List<TicketType> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public TicketType getTicketTypeUsingCode(String ticketTypeCode) {
        TypedQuery<TicketType> query = em.createQuery("SELECT p FROM TicketType p WHERE p.ticketTypeCode = :ticketTypeCode", TicketType.class)
                .setParameter("ticketTypeCode", ticketTypeCode);
        List<TicketType> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public TicketType getTicketTypeUsingName(String ticketTypeName) {
        TypedQuery<TicketType> query = em.createQuery("SELECT p FROM TicketType p WHERE p.ticketTypeName = :ticketTypeName", TicketType.class)
                .setParameter("ticketTypeName", ticketTypeName);
        List<TicketType> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public TicketType createTicketType(TicketType ticketType) {
        em.persist(ticketType);
        em.flush();
        return ticketType;
    }

    @Override
    public TicketType updateTicketType(TicketType ticketType) {
        em.merge(ticketType);
        em.flush();
        return ticketType;
    }

    @Override
    public TicketType deleteTicketType(TicketType ticketType) {
        em.remove(em.contains(ticketType) ? ticketType : em.merge(ticketType));
        em.flush();
        return ticketType;
    }

    @Override
    public List<TicketType> getTicketTypeUsingTicketGroup(TicketGroup ticketGroup) {
        TypedQuery<TicketType> query = em.createQuery("SELECT p FROM TicketType p WHERE p.ticketGroup = :ticketGroup AND p.automated = false", TicketType.class)
                .setParameter("ticketGroup", ticketGroup);
        List<TicketType> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<TicketType> getTicketTypeUsingTicketGroup(TicketGroup ticketGroup, boolean userType) {
        TypedQuery<TicketType> query = em.createQuery("SELECT p FROM TicketType p WHERE p.ticketGroup = :ticketGroup AND p.internal = :userType AND p.automated = false", TicketType.class)
                .setParameter("ticketGroup", ticketGroup)
                .setParameter("userType", userType);
        List<TicketType> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<TicketType> getTicketTypeUsingServiceUnit(ServiceUnit serviceUnit) {
        TypedQuery<TicketType> query = em.createQuery("SELECT p FROM TicketType p WHERE p.serviceUnit = :serviceUnit AND p.automated = false", TicketType.class)
                .setParameter("serviceUnit", serviceUnit);
        List<TicketType> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<TicketType> getTicketTypeUsingTicketSla(TicketSla ticketSla) {
        TypedQuery<TicketType> query = em.createQuery("SELECT p FROM TicketType p WHERE p.sla = :ticketSla AND p.automated = false", TicketType.class)
                .setParameter("ticketSla", ticketSla);
        List<TicketType> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<TicketSla> getTicketSla() {
        TypedQuery<TicketSla> query = em.createQuery("SELECT p FROM TicketSla p", TicketSla.class);
        List<TicketSla> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public TicketSla getTicketSlaUsingId(long id) {
        TypedQuery<TicketSla> query = em.createQuery("SELECT p FROM TicketSla p WHERE p.id = :id", TicketSla.class)
                .setParameter("id", id);
        List<TicketSla> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public TicketSla getTicketSlaUsingName(String ticketSlaName) {
        TypedQuery<TicketSla> query = em.createQuery("SELECT p FROM TicketSla p WHERE p.ticketSlaName = :ticketSlaName", TicketSla.class)
                .setParameter("ticketSlaName", ticketSlaName);
        List<TicketSla> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public TicketSla createTicketSla(TicketSla ticketSla) {
        em.persist(ticketSla);
        em.flush();
        return ticketSla;
    }

    @Override
    public TicketSla updateTicketSla(TicketSla ticketSla) {
        em.merge(ticketSla);
        em.flush();
        return ticketSla;
    }

    @Override
    public TicketSla deleteTicketSla(TicketSla ticketSla) {
        em.remove(em.contains(ticketSla) ? ticketSla : em.merge(ticketSla));
        em.flush();
        return ticketSla;
    }

    @Override
    public List<TicketAgent> getTicketAgent() {
        TypedQuery<TicketAgent> query = em.createQuery("SELECT p FROM TicketAgent p WHERE p.inUse = true", TicketAgent.class);
        List<TicketAgent> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<AppUser> getDistinctTicketAgent() {
        TypedQuery<AppUser> query = em.createQuery("SELECT distinct p.agent FROM TicketAgent p WHERE p.inUse = true", AppUser.class);
        List<AppUser> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<TicketAgent> getTicketAgent(AppUser ticketAgent) {
        TypedQuery<TicketAgent> query = em.createQuery("SELECT p FROM TicketAgent p WHERE p.agent = :ticketAgent AND p.inUse = true", TicketAgent.class)
                .setParameter("ticketAgent", ticketAgent);
        List<TicketAgent> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<TicketAgent> getTicketAgentUsingTicketType(TicketType ticketType) {
        TypedQuery<TicketAgent> query = em.createQuery("SELECT p FROM TicketAgent p WHERE p.ticketType = :ticketType AND p.inUse = true", TicketAgent.class)
                .setParameter("ticketType", ticketType);
        List<TicketAgent> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<TicketAgent> getTicketAgentUsingTicketType(AppUser appUser, TicketType ticketType) {
        TypedQuery<TicketAgent> query = em.createQuery("SELECT p FROM TicketAgent p WHERE p.ticketType = :ticketType AND p.agent = :appUser AND p.inUse = true", TicketAgent.class)
                .setParameter("ticketType", ticketType)
                .setParameter("appUser", appUser);
        List<TicketAgent> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public TicketAgent getTicketAgentUsingId(long id) {
        TypedQuery<TicketAgent> query = em.createQuery("SELECT p FROM TicketAgent p WHERE p.id = :id AND p.inUse = true", TicketAgent.class)
                .setParameter("id", id);
        List<TicketAgent> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public TicketAgent createTicketAgent(TicketAgent ticketAgent) {
        em.persist(ticketAgent);
        em.flush();
        return ticketAgent;
    }

    @Override
    public TicketAgent updateTicketAgent(TicketAgent ticketAgent) {
        em.merge(ticketAgent);
        em.flush();
        return ticketAgent;
    }

    @Override
    public TicketAgent deleteTicketAgent(TicketAgent ticketAgent) {
        em.remove(em.contains(ticketAgent) ? ticketAgent : em.merge(ticketAgent));
        em.flush();
        return ticketAgent;
    }

    @Override
    public List<TicketEscalations> getTicketEscalation() {
        TypedQuery<TicketEscalations> query = em.createQuery("SELECT p FROM TicketEscalations p", TicketEscalations.class);
        List<TicketEscalations> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<TicketEscalations> getTicketEscalationUsingTicket(Tickets ticket) {
        TypedQuery<TicketEscalations> query = em.createQuery("SELECT p FROM TicketEscalations p WHERE p.ticket = :ticket", TicketEscalations.class)
                .setParameter("ticket", ticket);
        List<TicketEscalations> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public TicketEscalations createTicketEscalation(TicketEscalations ticketEscalations) {
        em.persist(ticketEscalations);
        em.flush();
        return ticketEscalations;
    }

    @Override
    public TicketEscalations updateTicketEscalation(TicketEscalations ticketEscalations) {
        em.merge(ticketEscalations);
        em.flush();
        return ticketEscalations;
    }

    @Override
    public TicketEscalations deleteTicketEscalation(TicketEscalations ticketEscalations) {
        em.persist(em.contains(ticketEscalations) ? ticketEscalations : em.merge(ticketEscalations));
        em.flush();
        return ticketEscalations;
    }

    @Override
    public List<TicketComment> getTicketComment() {
        TypedQuery<TicketComment> query = em.createQuery("SELECT p FROM TicketComment p", TicketComment.class);
        List<TicketComment> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<TicketComment> getTicketCommentUsingTicket(Tickets ticket) {
        TypedQuery<TicketComment> query = em.createQuery("SELECT p FROM TicketComment p WHERE p.ticket = :ticket ORDER BY p.id DESC", TicketComment.class)
                .setParameter("ticket", ticket);
        List<TicketComment> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public TicketComment createTicketEscalation(TicketComment ticketComment) {
        em.persist(ticketComment);
        em.flush();
        return ticketComment;
    }

    @Override
    public TicketComment updateTicketEscalation(TicketComment ticketComment) {
        em.merge(ticketComment);
        em.flush();
        return ticketComment;
    }

    @Override
    public TicketComment deleteTicketEscalation(TicketComment ticketComment) {
        em.remove(em.contains(ticketComment) ? ticketComment : em.merge(ticketComment));
        em.flush();
        return ticketComment;
    }

    @Override
    public List<TicketUpload> getTicketUpload() {
        TypedQuery<TicketUpload> query = em.createQuery("SELECT p FROM TicketUpload p", TicketUpload.class);
        List<TicketUpload> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<TicketUpload> getTicketUploadUsingTicket(Tickets ticket) {
        TypedQuery<TicketUpload> query = em.createQuery("SELECT p FROM TicketUpload p WHERE p.ticket = :ticket", TicketUpload.class);
        List<TicketUpload> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public TicketUpload createTicketEscalation(TicketUpload ticketUpload) {
        em.persist(ticketUpload);
        em.flush();
        return ticketUpload;
    }

    @Override
    public TicketUpload updateTicketEscalation(TicketUpload ticketUpload) {
        em.merge(ticketUpload);
        em.flush();
        return ticketUpload;
    }

    @Override
    public TicketUpload deleteTicketEscalation(TicketUpload ticketUpload) {
        em.remove(em.contains(ticketUpload) ? ticketUpload : em.merge(ticketUpload));
        em.flush();
        return ticketUpload;
    }

    @Override
    public TicketComment createTicketComment(TicketComment ticketComment) {
        em.persist(ticketComment);
        em.flush();
        return ticketComment;
    }

    @Override
    public int getTicketGroupByUser(AppUser appUser, TicketGroup ticketGroup) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.createdBy = :appUser AND p.ticketGroup = :ticketGroup", Tickets.class)
                .setParameter("appUser", appUser)
                .setParameter("ticketGroup", ticketGroup);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return 0;
        }
        return recordset.size();
    }

    @Override
    public int getTicketOpenForAgentByGroup(AppUser appUser, TicketStatus ticketStatus, TicketGroup ticketGroup) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.ticketAgent.agent = :appUser AND p.ticketGroup = :ticketGroup AND p.ticketStatus = :ticketStatus", Tickets.class)
                .setParameter("appUser", appUser)
                .setParameter("ticketGroup", ticketGroup)
                .setParameter("ticketStatus", ticketStatus);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return 0;
        }
        return recordset.size();
    }

    @Override
    public TicketReassign createTicketReassign(TicketReassign ticketReassign) {
        em.persist(ticketReassign);
        em.flush();
        return ticketReassign;
    }

    @Override
    public TicketReassign updateTicketReassign(TicketReassign ticketReassign) {
        em.merge(ticketReassign);
        em.flush();
        return ticketReassign;
    }

    @Override
    public TicketReassign deleteTicketReassign(TicketReassign ticketReassign) {
        em.remove(em.contains(ticketReassign) ? ticketReassign : em.merge(ticketReassign));
        em.flush();
        return ticketReassign;
    }

    @Override
    public List<TicketReassign> getTicketReassignedUsingTicket(Tickets ticket) {
        TypedQuery<TicketReassign> query = em.createQuery("SELECT p FROM TicketReassign p WHERE p.ticket = :ticket", TicketReassign.class)
                .setParameter("ticket", ticket);
        List<TicketReassign> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<TicketReassign> getDistinctTicketReassigned(LocalDate startDate, LocalDate endDate) {
        TypedQuery<TicketReassign> query = em.createQuery("SELECT DISTINCT(p) FROM TicketReassign p WHERE p.reassignedAt >= :startDate AND p.reassignedAt <= :endDate", TicketReassign.class)
                .setParameter("startDate", startDate.atStartOfDay())
                .setParameter("endDate", endDate.atTime(23, 59));
        List<TicketReassign> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<ServiceUnit> getServiceUnit() {
        TypedQuery<ServiceUnit> query = em.createQuery("SELECT p FROM ServiceUnit p", ServiceUnit.class);
        List<ServiceUnit> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public ServiceUnit getServiceUnitUsingId(long id) {
        TypedQuery<ServiceUnit> query = em.createQuery("SELECT p FROM ServiceUnit p WHERE p.id = :id", ServiceUnit.class)
                .setParameter("id", id);
        List<ServiceUnit> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public ServiceUnit getServiceUnitUsingCode(String serviceUnitCode) {
        TypedQuery<ServiceUnit> query = em.createQuery("SELECT p FROM ServiceUnit p WHERE p.serviceUnitCode = :serviceUnitCode", ServiceUnit.class)
                .setParameter("serviceUnitCode", serviceUnitCode);
        List<ServiceUnit> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public ServiceUnit getServiceUnitUsingName(String serviceUnitName) {
        TypedQuery<ServiceUnit> query = em.createQuery("SELECT p FROM ServiceUnit p WHERE p.serviceUnitName = :serviceUnitName", ServiceUnit.class)
                .setParameter("serviceUnitName", serviceUnitName);
        List<ServiceUnit> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public List<ServiceUnit> getServiceUnitUsingEntity(Entities entity) {
        TypedQuery<ServiceUnit> query = em.createQuery("SELECT p FROM ServiceUnit p WHERE p.entity = :entity", ServiceUnit.class)
                .setParameter("entity", entity);
        List<ServiceUnit> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public ServiceUnit createServiceUnit(ServiceUnit serviceUnit) {
        em.persist(serviceUnit);
        em.flush();
        return serviceUnit;
    }

    @Override
    public ServiceUnit updateServiceUnit(ServiceUnit serviceUnit) {
        em.merge(serviceUnit);
        em.flush();
        return serviceUnit;
    }

    @Override
    public ServiceUnit deleteServiceUnit(ServiceUnit serviceUnit) {
        em.remove(em.contains(serviceUnit) ? serviceUnit : em.merge(serviceUnit));
        em.flush();
        return serviceUnit;
    }

    @Override
    public List<TicketStatus> getTicketStatus() {
        TypedQuery<TicketStatus> query = em.createQuery("SELECT p FROM TicketStatus p", TicketStatus.class);
        List<TicketStatus> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public TicketStatus getTicketStatusUsingId(long id) {
        TypedQuery<TicketStatus> query = em.createQuery("SELECT p FROM TicketStatus p WHERE p.id = :id", TicketStatus.class)
                .setParameter("id", id);
        List<TicketStatus> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public TicketStatus getTicketStatusUsingCode(String ticketStatusCode) {
        TypedQuery<TicketStatus> query = em.createQuery("SELECT p FROM TicketStatus p WHERE p.ticketStatusCode = :ticketStatusCode", TicketStatus.class)
                .setParameter("ticketStatusCode", ticketStatusCode);
        List<TicketStatus> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public TicketStatus getTicketStatusUsingName(String ticketStatusName) {
        TypedQuery<TicketStatus> query = em.createQuery("SELECT p FROM TicketStatus p WHERE p.ticketStatusName = :ticketStatusName", TicketStatus.class)
                .setParameter("ticketStatusName", ticketStatusName);
        List<TicketStatus> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public TicketStatus createTicketStatus(TicketStatus ticketStatus) {
        em.persist(ticketStatus);
        em.flush();
        return ticketStatus;
    }

    @Override
    public TicketStatus updateTicketStatus(TicketStatus ticketStatus) {
        em.merge(ticketStatus);
        em.flush();
        return ticketStatus;
    }

    @Override
    public TicketStatus deleteTicketStatus(TicketStatus ticketStatus) {
        em.remove(em.contains(ticketStatus) ? ticketStatus : em.merge(ticketStatus));
        em.flush();
        return ticketStatus;
    }

    @Override
    public TicketStatusChange createTicketStatusChange(TicketStatusChange ticketStatusChange) {
        em.persist(ticketStatusChange);
        em.flush();
        return ticketStatusChange;
    }

    @Override
    public List<PublicHolidays> getPublicHolidays() {
        TypedQuery<PublicHolidays> query = em.createQuery("SELECT p FROM PublicHolidays p", PublicHolidays.class);
        List<PublicHolidays> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public PublicHolidays getPublicHoliday(LocalDate holiday) {
        TypedQuery<PublicHolidays> query = em.createQuery("SELECT p FROM PublicHolidays p WHERE p.holiday = :holiday", PublicHolidays.class)
                .setParameter("holiday", holiday);
        List<PublicHolidays> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public PublicHolidays createPublicHoliday(PublicHolidays publicHoliday) {
        em.persist(publicHoliday);
        em.flush();
        return publicHoliday;
    }

    @Override
    public PublicHolidays updatePublicHoliday(PublicHolidays publicHoliday) {
        em.merge(publicHoliday);
        em.flush();
        return publicHoliday;
    }

    @Override
    public List<Entities> getEntities() {
        TypedQuery<Entities> query = em.createQuery("SELECT p FROM Entities p", Entities.class);
        List<Entities> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public Entities getEntitiesUsingId(long id) {
        TypedQuery<Entities> query = em.createQuery("SELECT p FROM Entities p  WHERE p.id = :id", Entities.class)
                .setParameter("id", id);
        List<Entities> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public Entities getEntitiesUsingCode(String entityCode) {
        TypedQuery<Entities> query = em.createQuery("SELECT p FROM Entities p  WHERE p.entityCode = :entityCode", Entities.class)
                .setParameter("entityCode", entityCode);
        List<Entities> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public Entities getEntitiesUsingName(String entityName) {
        TypedQuery<Entities> query = em.createQuery("SELECT p FROM Entities p  WHERE p.entityName = :entityName", Entities.class)
                .setParameter("entityName", entityName);
        List<Entities> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public Entities createEntities(Entities entity) {
        em.persist(entity);
        em.flush();
        return entity;
    }

    @Override
    public Entities updateEntities(Entities entity) {
        em.merge(entity);
        em.flush();
        return entity;
    }

    @Override
    public Entities deleteEntities(Entities entity) {
        em.remove(em.contains(entity) ? entity : em.merge(entity));
        em.flush();
        return entity;
    }

    @Override
    public List<Department> getDepartment() {
        TypedQuery<Department> query = em.createQuery("SELECT p FROM Department p", Department.class);
        List<Department> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public Department getDepartmentUsingId(long id) {
        TypedQuery<Department> query = em.createQuery("SELECT p FROM Department p  WHERE p.id = :id", Department.class)
                .setParameter("id", id);
        List<Department> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public Department getDepartmentUsingCode(String departmentCode) {
        TypedQuery<Department> query = em.createQuery("SELECT p FROM Department p  WHERE p.departmentCode = :departmentCode", Department.class)
                .setParameter("departmentCode", departmentCode);
        List<Department> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public Department getDepartmentUsingName(String departmentName) {
        TypedQuery<Department> query = em.createQuery("SELECT p FROM Department p  WHERE p.departmentName = :departmentName", Department.class)
                .setParameter("departmentName", departmentName);
        List<Department> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public Department createDepartment(Department department) {
        em.persist(department);
        em.flush();
        return department;
    }

    @Override
    public Department updateDepartment(Department department) {
        em.merge(department);
        em.flush();
        return department;
    }

    @Override
    public Department deleteDepartment(Department department) {
        em.remove(em.contains(department) ? department : em.merge(department));
        em.flush();
        return department;
    }

    @Override
    public List<ServiceUnit> getServiceUnitUsingDepartment(Department department) {
        TypedQuery<ServiceUnit> query = em.createQuery("SELECT p FROM ServiceUnit p WHERE p.department = :department", ServiceUnit.class)
                .setParameter("department", department);
        List<ServiceUnit> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }
    
        @Override
    public List<Department> getDepartmentUsingEntity(Entities entity) {
        TypedQuery<Department> query = em.createQuery("SELECT p FROM Department p WHERE p.entity = :entity", Department.class)
                .setParameter("entity", entity);
        List<Department> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<DocumentUpload> getDocumentUploadUsingTicket(Tickets ticket) {
        TypedQuery<DocumentUpload> query = em.createQuery("SELECT p FROM DocumentUpload p  WHERE p.ticket = :ticket", DocumentUpload.class)
                .setParameter("ticket", ticket);
        List<DocumentUpload> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public DocumentUpload createDocumentUpload(DocumentUpload documentUpload) {
        em.persist(documentUpload);
        em.flush();
        return documentUpload;
    }

    @Override
    public List<KnowledgeBaseCategory> getKnowledgeBaseCategory() {
        TypedQuery<KnowledgeBaseCategory> query = em.createQuery("SELECT p FROM KnowledgeBaseCategory p", KnowledgeBaseCategory.class);
        List<KnowledgeBaseCategory> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<KnowledgeBase> getKnowledgeBase() {
        TypedQuery<KnowledgeBase> query = em.createQuery("SELECT p FROM KnowledgeBase p", KnowledgeBase.class);
        List<KnowledgeBase> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<KnowledgeBase> getKnowledgeBaseUsingCategory(KnowledgeBaseCategory category) {
        TypedQuery<KnowledgeBase> query = em.createQuery("SELECT p FROM KnowledgeBase p WHERE p.knowledgeBaseCategory = :knowledgeBaseCategory", KnowledgeBase.class)
                .setParameter("knowledgeBaseCategory", category);
        List<KnowledgeBase> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public KnowledgeBase getKnowledgeBaseUsingId(long id) {
        TypedQuery<KnowledgeBase> query = em.createQuery("SELECT p FROM KnowledgeBase p WHERE p.id = :id", KnowledgeBase.class)
                .setParameter("id", id);
        List<KnowledgeBase> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public List<KnowledgeBase> getKnowledgeBaseUsingTags(String searchKeyWord) {
        StringBuilder strQuery = new StringBuilder();
        strQuery.append("SELECT t FROM KnowledgeBase t WHERE ");

        String[] searchStrings = searchKeyWord.split(",");
        int i = 1;
        for (String str : searchStrings) {
            if (i == searchStrings.length) {
                strQuery = strQuery.append("t.tag LIKE '%").append(str.trim()).append("%'");
            } else {
                strQuery = strQuery.append("t.tag LIKE '%").append(str.trim()).append("%' OR ");
            }
            i++;
        }
        TypedQuery<KnowledgeBase> query = em.createQuery(strQuery.toString(), KnowledgeBase.class);
        List<KnowledgeBase> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public KnowledgeBaseCategory getKnowledgeBaseCategoryUsingId(long id) {
        TypedQuery<KnowledgeBaseCategory> query = em.createQuery("SELECT p FROM KnowledgeBaseCategory p WHERE p.id = :id", KnowledgeBaseCategory.class)
                .setParameter("id", id);
        List<KnowledgeBaseCategory> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public KnowledgeBaseCategory getKnowledgeBaseCategoryUsingCode(String categoryCode) {
        TypedQuery<KnowledgeBaseCategory> query = em.createQuery("SELECT p FROM KnowledgeBaseCategory p WHERE p.categoryCode = :categoryCode", KnowledgeBaseCategory.class)
                .setParameter("categoryCode", categoryCode);
        List<KnowledgeBaseCategory> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public KnowledgeBaseCategory getKnowledgeBaseCategoryUsingName(String categoryName) {
        TypedQuery<KnowledgeBaseCategory> query = em.createQuery("SELECT p FROM KnowledgeBaseCategory p WHERE p.categoryName = :categoryName", KnowledgeBaseCategory.class)
                .setParameter("categoryName", categoryName);
        List<KnowledgeBaseCategory> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public KnowledgeBase getKnowledgeBaseUsingTitle(String title) {
        TypedQuery<KnowledgeBase> query = em.createQuery("SELECT p FROM KnowledgeBase p WHERE p.header = :title", KnowledgeBase.class)
                .setParameter("title", title);
        List<KnowledgeBase> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public List<KnowledgeBase> getKnowledgeBasePopularArticle() {
        TypedQuery<KnowledgeBase> query = em.createQuery("SELECT p FROM KnowledgeBase p WHERE p.popularArticle = true", KnowledgeBase.class);
        List<KnowledgeBase> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<KnowledgeBase> getKnowledgeBaseLatestArticle() {
        TypedQuery<KnowledgeBase> query = em.createQuery("SELECT p FROM KnowledgeBase p WHERE p.latestArticle = true", KnowledgeBase.class);
        List<KnowledgeBase> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public KnowledgeBase createKnowledgeBase(KnowledgeBase knowledgeBase) {
        em.persist(knowledgeBase);
        em.flush();
        return knowledgeBase;
    }

    @Override
    public KnowledgeBase updateKnowledgeBase(KnowledgeBase knowledgeBase) {
        em.merge(knowledgeBase);
        em.flush();
        return knowledgeBase;
    }

    @Override
    public KnowledgeBase deleteKnowledgeBase(KnowledgeBase knowledgeBase) {
        em.remove(em.contains(knowledgeBase) ? knowledgeBase : em.merge(knowledgeBase));
        em.flush();
        return knowledgeBase;
    }

    @Override
    public KnowledgeBaseCategory createKnowledgeBaseCategory(KnowledgeBaseCategory knowledgeBaseCategory) {
        em.persist(knowledgeBaseCategory);
        em.flush();
        return knowledgeBaseCategory;
    }

    @Override
    public KnowledgeBaseCategory updateKnowledgeBaseCategory(KnowledgeBaseCategory knowledgeBaseCategory) {
        em.merge(knowledgeBaseCategory);
        em.flush();
        return knowledgeBaseCategory;
    }

    @Override
    public KnowledgeBaseCategory deleteKnowledgeBaseCategory(KnowledgeBaseCategory knowledgeBaseCategory) {
        em.remove(em.contains(knowledgeBaseCategory) ? knowledgeBaseCategory : em.merge(knowledgeBaseCategory));
        em.flush();
        return knowledgeBaseCategory;
    }

    @Override
    public List<AuditLog> getAuditLog(LocalDate startDate, LocalDate endDate) {
        TypedQuery<AuditLog> query = em.createQuery("SELECT p FROM AuditLog p WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate", AuditLog.class)
                .setParameter("startDate", startDate.atStartOfDay())
                .setParameter("endDate", endDate.atTime(23, 59));
        List<AuditLog> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public ContactUs createContactUs(ContactUs contactUs) {
        em.persist(contactUs);
        em.flush();
        return contactUs;
    }

    @Override
    public List<TicketType> getAutomatedTicketType() {
        TypedQuery<TicketType> query = em.createQuery("SELECT p FROM TicketType p WHERE p.automated = true", TicketType.class);
        List<TicketType> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<AutomatedTicket> getAutomatedTicket() {
        TypedQuery<AutomatedTicket> query = em.createQuery("SELECT p FROM AutomatedTicket p", AutomatedTicket.class);
        List<AutomatedTicket> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public List<AutomatedTicket> getActiveAutomatedTicket() {
        TypedQuery<AutomatedTicket> query = em.createQuery("SELECT p FROM AutomatedTicket p WHERE p.status = 'Enabled'", AutomatedTicket.class);
        List<AutomatedTicket> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset;
    }

    @Override
    public AutomatedTicket getAutomatedTicketUsingId(long id) {
        TypedQuery<AutomatedTicket> query = em.createQuery("SELECT p FROM AutomatedTicket p WHERE p.id = :id", AutomatedTicket.class)
                .setParameter("id", id);
        List<AutomatedTicket> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public Tickets getAutomatedTicketUsingTicketType(TicketType ticketType) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM AutomatedTicket p WHERE p.ticketType = :ticketType", Tickets.class)
                .setParameter("ticketType", ticketType);
        List<Tickets> recordset = query.getResultList();
        if (recordset.isEmpty()) {
            return null;
        }
        return recordset.get(0);
    }

    @Override
    public AutomatedTicket createAutomatedTicket(AutomatedTicket automatedTicket) {
        em.persist(automatedTicket);
        em.flush();
        return automatedTicket;
    }

    @Override
    public AutomatedTicket updateAutomatedTicket(AutomatedTicket automatedTicket) {
        em.merge(automatedTicket);
        em.flush();
        return automatedTicket;
    }

    @Override
    public AutomatedTicket deleteAutomatedTicket(AutomatedTicket automatedTicket) {
        em.remove(em.contains(automatedTicket) ? automatedTicket : em.merge(automatedTicket));
        em.flush();
        return automatedTicket;
    }

}
