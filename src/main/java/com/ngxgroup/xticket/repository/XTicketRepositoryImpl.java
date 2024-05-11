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
import com.ngxgroup.xticket.model.TicketAgent;
import com.ngxgroup.xticket.model.TicketType;
import com.ngxgroup.xticket.model.TicketUpload;
import com.ngxgroup.xticket.model.Tickets;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
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
        List<AppUser> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public AppUser getAppUserUsingMobileNumber(String mobileNumber) {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.mobileNumber = :mobileNumber", AppUser.class)
                .setParameter("mobileNumber", mobileNumber);
        List<AppUser> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public AppUser getAppUserUsingActivationId(String activationId) {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.activationId = :activationId", AppUser.class)
                .setParameter("activationId", activationId);
        List<AppUser> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<AppUser> getAgentAppUsers() {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.agent = true", AppUser.class);
        List<AppUser> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<AppUser> getAppUserUsingRoleGroup(RoleGroups roleGroup) {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.role = :roleGroup", AppUser.class)
                .setParameter("roleGroup", roleGroup);
        List<AppUser> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
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
        List<Notification> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public AppRoles getRoleUsingRoleName(String roleName) {
        TypedQuery<AppRoles> query = em.createQuery("SELECT p FROM AppRoles p WHERE p.roleName = :roleName", AppRoles.class)
                .setParameter("roleName", roleName);
        List<AppRoles> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
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
        List<AppRoles> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<RoleGroups> getRoleGroupList() {
        TypedQuery<RoleGroups> query = em.createQuery("SELECT p FROM RoleGroups p", RoleGroups.class);
        List<RoleGroups> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public RoleGroups getRoleGroupUsingGroupName(String groupName) {
        TypedQuery<RoleGroups> query = em.createQuery("SELECT p FROM RoleGroups p WHERE p.groupName = :groupName", RoleGroups.class)
                .setParameter("groupName", groupName);
        List<RoleGroups> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
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
        List<RoleGroups> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<GroupRoles> getGroupRolesUsingRoleGroup(RoleGroups roleGroup) {
        TypedQuery<GroupRoles> query = em.createQuery("SELECT p FROM GroupRoles p WHERE p.roleGroup = :roleGroup", GroupRoles.class)
                .setParameter("roleGroup", roleGroup);
        List<GroupRoles> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
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
        List<AppUser> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
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
        List<AppUser> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public AppUser getAppUserUsingUserId(String userId) {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.userId = :userId", AppUser.class)
                .setParameter("userId", userId);
        List<AppUser> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<AppUser> getAppUsersForTFAFix() {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.twoFactorSecretKey = 'NA'", AppUser.class);
        List<AppUser> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<AppUser> getActiveUsers() {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.isEnabled = true", AppUser.class);
        List<AppUser> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<AppUser> getDisabledUsers() {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.isEnabled = false", AppUser.class);
        List<AppUser> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<AppUser> getInternalAppUsers() {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.internal = true", AppUser.class);
        List<AppUser> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<AuditLog> getAuditLogUsingDate(String startDate, String endDate) {
        TypedQuery<AuditLog> query = em.createQuery("SELECT c FROM AuditLog c WHERE c.createdAt >= '" + startDate + "'" + " AND c.createdAt <= '" + endDate + "'", AuditLog.class);
        List<AuditLog> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
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
    public Tickets getTicketUsingId(long id) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.id = :id", Tickets.class)
                .setParameter("id", id);
        List<Tickets> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<Tickets> getTickets() {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p", Tickets.class);
        List<Tickets> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public Tickets getTicketUsingTicketGroup(TicketGroup ticketGroup) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.ticketGroup = :ticketGroup", Tickets.class)
                .setParameter("ticketGroup", ticketGroup);
        List<Tickets> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Tickets getTicketUsingTicketType(TicketType ticketType) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.ticketType = :ticketType", Tickets.class)
                .setParameter("ticketType", ticketType);
        List<Tickets> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<Tickets> getOpenTickets() {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Tickets p WHERE p.ticketOpen = true", Tickets.class);
        List<Tickets> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketReopened> getTicketReopened() {
        TypedQuery<TicketReopened> query = em.createQuery("SELECT p FROM TicketReopened p", TicketReopened.class);
        List<TicketReopened> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketReopened> getTicketReopenedUsingTicket(Tickets ticket) {
        TypedQuery<TicketReopened> query = em.createQuery("SELECT p FROM TicketReopened p WHERE p.ticket = :ticket", TicketReopened.class)
                .setParameter("ticket", ticket);
        List<TicketReopened> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
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
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public TicketGroup getTicketGroupUsingId(long id) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM TicketGroup p WHERE p.id = :id", TicketGroup.class)
                .setParameter("id", id);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public TicketGroup getTicketGroupUsingCode(String ticketGroupCode) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM TicketGroup p WHERE p.ticketGroupCode = :ticketGroupCode", TicketGroup.class)
                .setParameter("ticketGroupCode", ticketGroupCode);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public TicketGroup getTicketGroupUsingName(String ticketGroupName) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM TicketGroup p WHERE p.ticketGroupName = :ticketGroupName", TicketGroup.class)
                .setParameter("ticketGroupName", ticketGroupName);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
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
        TypedQuery<TicketType> query = em.createQuery("SELECT p FROM TicketType p", TicketType.class);
        List<TicketType> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public TicketType getTicketTypeUsingId(long id) {
        TypedQuery<TicketType> query = em.createQuery("SELECT p FROM TicketType p WHERE p.id = :id", TicketType.class)
                .setParameter("id", id);
        List<TicketType> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public TicketType getTicketTypeUsingCode(String ticketTypeCode) {
        TypedQuery<TicketType> query = em.createQuery("SELECT p FROM TicketType p WHERE p.ticketTypeCode = :ticketTypeCode", TicketType.class)
                .setParameter("ticketTypeCode", ticketTypeCode);
        List<TicketType> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public TicketType getTicketTypeUsingName(String ticketTypeName) {
        TypedQuery<TicketType> query = em.createQuery("SELECT p FROM TicketType p WHERE p.ticketTypeName = :ticketTypeName", TicketType.class)
                .setParameter("ticketTypeName", ticketTypeName);
        List<TicketType> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
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
        em.remove(em.contains(ticketType) ? ticketType : ticketType);
        em.flush();
        return ticketType;
    }

    @Override
    public List<TicketType> getTicketTypeUsingTicketGroup(TicketGroup ticketGroup) {
        TypedQuery<TicketType> query = em.createQuery("SELECT p FROM TicketType p WHERE p.ticketGroup = :ticketGroup", TicketType.class)
                .setParameter("ticketGroup", ticketGroup);
        List<TicketType> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketType> getTicketTypeUsingTicketGroup(TicketGroup ticketGroup, boolean userType) {
        TypedQuery<TicketType> query = em.createQuery("SELECT p FROM TicketType p WHERE p.ticketGroup = :ticketGroup AND p.internal = :userType", TicketType.class)
                .setParameter("ticketGroup", ticketGroup)
                .setParameter("userType", userType);
        List<TicketType> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketAgent> getTicketAgent() {
        TypedQuery<TicketAgent> query = em.createQuery("SELECT p FROM TicketAgent p", TicketAgent.class);
        List<TicketAgent> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketAgent> getTicketAgent(AppUser ticketAgent) {
        TypedQuery<TicketAgent> query = em.createQuery("SELECT p FROM TicketAgent p WHERE p.agent = :ticketAgent", TicketAgent.class)
                .setParameter("ticketAgent", ticketAgent);
        List<TicketAgent> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketAgent> getTicketAgentUsingTicketType(TicketType ticketType) {
        TypedQuery<TicketAgent> query = em.createQuery("SELECT p FROM TicketAgent p WHERE p.ticketType = :ticketType", TicketAgent.class)
                .setParameter("ticketType", ticketType);
        List<TicketAgent> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
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
        em.remove(em.contains(ticketAgent) ? ticketAgent : ticketAgent);
        em.flush();
        return ticketAgent;
    }

    @Override
    public List<TicketEscalations> getTicketEscalation() {
        TypedQuery<TicketEscalations> query = em.createQuery("SELECT p FROM TicketEscalations p", TicketEscalations.class);
        List<TicketEscalations> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketEscalations> getTicketEscalationUsingTicket(Tickets ticket) {
        TypedQuery<TicketEscalations> query = em.createQuery("SELECT p FROM TicketEscalations p WHERE p.ticket = :ticket", TicketEscalations.class)
                .setParameter("ticket", ticket);
        List<TicketEscalations> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
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
        List<TicketComment> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketComment> getTicketCommentUsingTicket(Tickets ticket) {
        TypedQuery<TicketComment> query = em.createQuery("SELECT p FROM TicketComment p WHERE p.ticket = :ticket", TicketComment.class)
                .setParameter("ticket", ticket);
        List<TicketComment> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
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
        em.remove(em.contains(ticketComment) ? ticketComment : ticketComment);
        em.flush();
        return ticketComment;
    }

    @Override
    public List<TicketUpload> getTicketUpload() {
        TypedQuery<TicketUpload> query = em.createQuery("SELECT p FROM TicketUpload p", TicketUpload.class);
        List<TicketUpload> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketUpload> getTicketUploadUsingTicket(Tickets ticket) {
        TypedQuery<TicketUpload> query = em.createQuery("SELECT p FROM TicketUpload p WHERE p.ticket = :ticket", TicketUpload.class);
        List<TicketUpload> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
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
        em.remove(em.contains(ticketUpload) ? ticketUpload : ticketUpload);
        em.flush();
        return ticketUpload;
    }

}
