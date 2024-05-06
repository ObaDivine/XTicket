package com.ngxgroup.xticket.repository;

import com.ngxgroup.xticket.model.AppRoles;
import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.model.AuditLog;
import com.ngxgroup.xticket.model.Tickets;
import com.ngxgroup.xticket.model.TicketUpload;
import com.ngxgroup.xticket.model.TicketComment;
import com.ngxgroup.xticket.model.GroupRoles;
import com.ngxgroup.xticket.model.Notification;
import com.ngxgroup.xticket.model.TicketGroup;
import com.ngxgroup.xticket.model.TicketReopened;
import com.ngxgroup.xticket.model.TicketEscalations;
import com.ngxgroup.xticket.model.TicketTechnicians;
import com.ngxgroup.xticket.model.TicketType;
import com.ngxgroup.xticket.model.RoleGroups;
import java.time.LocalDate;
import java.util.Date;
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
    public List<Tickets> getCompanies() {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Company p", Tickets.class);
        List<Tickets> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public Tickets createCompany(Tickets company) {
        em.persist(company);
        em.flush();
        return company;
    }

    @Override
    public Tickets updateCompany(Tickets company) {
        em.merge(company);
        em.flush();
        return company;
    }

    @Override
    public Tickets deleteCompany(Tickets company) {
        em.remove(em.contains(company) ? company : em.merge(company));
        em.flush();
        return company;
    }

    @Override
    public Tickets getCompanyUsingCode(String companyCode) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Company p WHERE p.companyCode = :companyCode", Tickets.class)
                .setParameter("companyCode", companyCode);
        List<Tickets> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Tickets getCompanyUsingId(long id) {
        TypedQuery<Tickets> query = em.createQuery("SELECT p FROM Company p WHERE p.id = :id", Tickets.class)
                .setParameter("id", id);
        List<Tickets> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<TicketComment> getDivisionUsingCompany(Tickets company) {
        TypedQuery<TicketComment> query = em.createQuery("SELECT p FROM Division p WHERE p.company = :company", TicketComment.class)
                .setParameter("company", company);
        List<TicketComment> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public TicketComment createDivision(TicketComment division) {
        em.persist(division);
        em.flush();
        return division;
    }

    @Override
    public TicketComment updateDivision(TicketComment division) {
        em.merge(division);
        em.flush();
        return division;
    }

    @Override
    public TicketComment deleteDivision(TicketComment division) {
        em.remove(em.contains(division) ? division : em.merge(division));
        em.flush();
        return division;
    }

    @Override
    public TicketComment getDivisionUsingId(long id) {
        TypedQuery<TicketComment> query = em.createQuery("SELECT p FROM Division p WHERE p.id = :id", TicketComment.class)
                .setParameter("id", id);
        List<TicketComment> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public TicketComment getDivisionUsingCode(String divisionCode) {
        TypedQuery<TicketComment> query = em.createQuery("SELECT p FROM Division p WHERE p.divisionCode = :divisionCode", TicketComment.class)
                .setParameter("divisionCode", divisionCode);
        List<TicketComment> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<TicketUpload> getDepartmentUsingDivision(TicketComment division) {
        TypedQuery<TicketUpload> query = em.createQuery("SELECT p FROM Department p WHERE p.division = :division", TicketUpload.class)
                .setParameter("division", division);
        List<TicketUpload> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public TicketUpload createDepartment(TicketUpload department) {
        em.persist(department);
        em.flush();
        return department;
    }

    @Override
    public TicketUpload updateDepartment(TicketUpload department) {
        em.merge(department);
        em.flush();
        return department;
    }

    @Override
    public TicketUpload deleteDepartment(TicketUpload department) {
        em.remove(em.contains(department) ? department : em.merge(department));
        em.flush();
        return department;
    }

    @Override
    public TicketUpload getDepartmentUsingId(long id) {
        TypedQuery<TicketUpload> query = em.createQuery("SELECT p FROM Department p WHERE p.id = :id", TicketUpload.class)
                .setParameter("id", id);
        List<TicketUpload> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public TicketUpload getDepartmentUsingCode(String departmentCode) {
        TypedQuery<TicketUpload> query = em.createQuery("SELECT p FROM Department p WHERE p.departmentCode = :departmentCode", TicketUpload.class)
                .setParameter("departmentCode", departmentCode);
        List<TicketUpload> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public AppUser getAppUserUsingUsername(String email) {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.email = :email", AppUser.class)
                .setParameter("email", email);
        List<AppUser> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public AppUser updateAppUser(AppUser appUser) {
        em.merge(appUser);
        em.flush();
        return appUser;
    }

    @Override
    public List<TicketType> getPolicyTypes() {
        TypedQuery<TicketType> query = em.createQuery("SELECT p FROM PolicyType p", TicketType.class);
        List<TicketType> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public TicketType getPolicyTypeUsingCode(String policyTypeCode) {
        TypedQuery<TicketType> query = em.createQuery("SELECT p FROM PolicyType p WHERE p.policyTypeCode = :policyTypeCode", TicketType.class)
                .setParameter("policyTypeCode", policyTypeCode);
        List<TicketType> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public TicketType getPolicyTypeUsingId(long id) {
        TypedQuery<TicketType> query = em.createQuery("SELECT p FROM PolicyType p WHERE p.id = :id", TicketType.class)
                .setParameter("id", id);
        List<TicketType> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public TicketType createPolicyType(TicketType policyType) {
        em.persist(policyType);
        em.flush();
        return policyType;
    }

    @Override
    public List<TicketGroup> getPoliciesUsingType(TicketType policyType) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p WHERE p.policyType = :policyType", TicketGroup.class)
                .setParameter("policyType", policyType);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketGroup> getPolicyUsingDepartmentAndType(TicketUpload department, TicketType policyType) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p WHERE p.department = :department AND p.policyType = :policyType", TicketGroup.class)
                .setParameter("department", department)
                .setParameter("policyType", policyType);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketGroup> getPolicies(Tickets company, TicketType policyType, int accessLevel) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p WHERE p.company = :company AND p.policyType = :policyType AND p.accessLevel <= :accessLevel", TicketGroup.class)
                .setParameter("company", company)
                .setParameter("policyType", policyType)
                .setParameter("accessLevel", accessLevel);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketGroup> getPolicies(TicketComment division, TicketType policyType, int accessLevel) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p WHERE p.division = :division AND p.policyType = :policyType AND p.accessLevel <= :accessLevel", TicketGroup.class)
                .setParameter("division", division)
                .setParameter("policyType", policyType)
                .setParameter("accessLevel", accessLevel);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketGroup> getPolicies(TicketUpload department, TicketType policyType, int accessLevel) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p WHERE p.department = :department AND p.policyType = :policyType AND p.accessLevel <= :accessLevel", TicketGroup.class)
                .setParameter("department", department)
                .setParameter("policyType", policyType)
                .setParameter("accessLevel", accessLevel);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketGroup> getPolicies(Tickets company, TicketComment division, TicketType policyType, int accessLevel) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p WHERE p.company = :company AND p.division = :division AND p.policyType = :policyType AND p.accessLevel <= :accessLevel", TicketGroup.class)
                .setParameter("company", company)
                .setParameter("division", division)
                .setParameter("policyType", policyType)
                .setParameter("accessLevel", accessLevel);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketGroup> getPolicies(Tickets company, TicketUpload department, TicketType policyType, int accessLevel) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p WHERE p.company = :company AND p.department = :department AND p.policyType = :policyType AND p.accessLevel <= :accessLevel", TicketGroup.class)
                .setParameter("company", company)
                .setParameter("department", department)
                .setParameter("policyType", policyType)
                .setParameter("accessLevel", accessLevel);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketGroup> getPolicies(Tickets company, TicketComment division, TicketUpload department, TicketType policyType, int accessLevel) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p WHERE p.company = :company AND p.division = :division AND p.department = :department AND p.policyType = :policyType AND p.accessLevel <= :accessLevel", TicketGroup.class)
                .setParameter("company", company)
                .setParameter("division", division)
                .setParameter("department", department)
                .setParameter("policyType", policyType)
                .setParameter("accessLevel", accessLevel);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketGroup> getPolicies(TicketComment division, TicketUpload department, TicketType policyType, int accessLevel) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p WHERE p.division = :division AND p.department = :department AND p.policyType = :policyType AND p.accessLevel <= :accessLevel", TicketGroup.class)
                .setParameter("division", division)
                .setParameter("department", department)
                .setParameter("policyType", policyType)
                .setParameter("accessLevel", accessLevel);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketGroup> getPolicies(TicketType policyType) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p WHERE p.policyType = :policyType", TicketGroup.class)
                .setParameter("policyType", policyType);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketGroup> getPolicies(TicketType policyType, int accessLevel) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p WHERE p.policyType = :policyType AND p.accessLevel <= :accessLevel", TicketGroup.class)
                .setParameter("policyType", policyType)
                .setParameter("accessLevel", accessLevel);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketTechnicians> getPendingPolicies() {
        TypedQuery<TicketTechnicians> query = em.createQuery("SELECT p FROM PolicyTemp p", TicketTechnicians.class);
        List<TicketTechnicians> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public TicketGroup getPolicyUsingId(long id) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p WHERE p.id = :id", TicketGroup.class)
                .setParameter("id", id);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public TicketGroup getPolicyUsingName(String policyName) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p WHERE p.policyName = :policyName", TicketGroup.class)
                .setParameter("policyName", policyName);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public TicketGroup getPolicyUsingCode(String policyCode) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p WHERE p.policyCode = :policyCode", TicketGroup.class)
                .setParameter("policyCode", policyCode);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public TicketReopened createPolicyRead(TicketReopened policyRead) {
        em.persist(policyRead);
        em.flush();
        return policyRead;
    }

    @Override
    public TicketReopened updatePolicyRead(TicketReopened policyRead) {
        em.merge(policyRead);
        em.flush();
        return policyRead;
    }

    @Override
    public TicketReopened deletePolicyRead(TicketReopened policyRead) {
        em.remove(em.contains(policyRead) ? policyRead : em.merge(policyRead));
        em.flush();
        return policyRead;
    }

    @Override
    public TicketReopened getPolicyReadUsingUserAndPolicy(AppUser appUser, TicketGroup policy) {
        TypedQuery<TicketReopened> query = em.createQuery("SELECT p FROM PolicyRead p WHERE p.appUser = :appUser AND p.policy = :policy", TicketReopened.class)
                .setParameter("appUser", appUser)
                .setParameter("policy", policy);
        List<TicketReopened> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<TicketGroup> getMustReadPolicy() {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p WHERE p.mustRead = true", TicketGroup.class);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
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
    public List<TicketGroup> getPolicies() {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p", TicketGroup.class);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public TicketGroup createPolicy(TicketGroup policy) {
        em.persist(policy);
        em.flush();
        return policy;
    }

    @Override
    public TicketGroup updatePolicy(TicketGroup policy) {
        em.merge(policy);
        em.flush();
        return policy;
    }

    @Override
    public TicketGroup deletePolicy(TicketGroup policy) {
        em.remove(em.contains(policy) ? policy : em.merge(policy));
        em.flush();
        return policy;
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
    public TicketGroup getPolicyUsingDocumentId(String documentId) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p WHERE p.policyDocumentId = :documentId", TicketGroup.class)
                .setParameter("documentId", documentId);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
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
    public TicketEscalations createPolicyReview(TicketEscalations policyReview) {
        em.persist(policyReview);
        em.flush();
        return policyReview;
    }

    @Override
    public TicketEscalations deletePolicyReview(TicketEscalations policyReview) {
        em.remove(em.contains(policyReview) ? policyReview : em.merge(policyReview));
        em.flush();
        return policyReview;
    }

    @Override
    public List<TicketEscalations> getAllPolicyReview() {
        TypedQuery<TicketEscalations> query = em.createQuery("SELECT p FROM PolicyReview p", TicketEscalations.class);
        List<TicketEscalations> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketEscalations> getPolicyReview(String startDate, String endDate) {
        TypedQuery<TicketEscalations> query = em.createQuery("SELECT p FROM PolicyReview p WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate", TicketEscalations.class)
                .setParameter("startDate", Date.parse(startDate))
                .setParameter("endDate", Date.parse(endDate));
        List<TicketEscalations> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketEscalations> getPolicyReviewUsingPolicy(TicketGroup policy) {
        TypedQuery<TicketEscalations> query = em.createQuery("SELECT p FROM PolicyReview p WHERE p.policy = :policy", TicketEscalations.class)
                .setParameter("policy", policy);
        List<TicketEscalations> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketGroup> getPolicyExpiringToday(int daysBeforeExpiry) {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p WHERE p.expiryDate <= :today AND p.expired = false", TicketGroup.class)
                .setParameter("today", LocalDate.now().plusDays(daysBeforeExpiry));
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketGroup> getExpiredPolicies() {
        TypedQuery<TicketGroup> query = em.createQuery("SELECT p FROM Policy p WHERE p.expired = true", TicketGroup.class);
        List<TicketGroup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<TicketReopened> getPolicyReadUsingPolicy(TicketGroup policy) {
        TypedQuery<TicketReopened> query = em.createQuery("SELECT p FROM PolicyRead p WHERE p.policy = :policy", TicketReopened.class)
                .setParameter("policy", policy);
        List<TicketReopened> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public TicketTechnicians createPolicyTemp(TicketTechnicians policyTemp) {
        em.persist(em.contains(policyTemp) ? policyTemp : em.merge(policyTemp));
        em.flush();
        return policyTemp;
    }

    @Override
    public TicketTechnicians deletePolicyTemp(TicketTechnicians policyTemp) {
        em.remove(em.contains(policyTemp) ? policyTemp : em.merge(policyTemp));
        em.flush();
        return policyTemp;
    }

    @Override
    public TicketTechnicians getPolicyTempUsingId(long id) {
        TypedQuery<TicketTechnicians> query = em.createQuery("SELECT p FROM PolicyTemp p WHERE p.id = :id", TicketTechnicians.class)
                .setParameter("id", id);
        List<TicketTechnicians> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public TicketTechnicians getPolicyTempUsingPolicy(TicketGroup policy) {
        TypedQuery<TicketTechnicians> query = em.createQuery("SELECT p FROM PolicyTemp p WHERE p.policy = :policy", TicketTechnicians.class)
                .setParameter("policy", policy);
        List<TicketTechnicians> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public TicketTechnicians getPolicyTempUsingDocumentId(String documentId) {
        TypedQuery<TicketTechnicians> query = em.createQuery("SELECT p FROM PolicyTemp p WHERE p.policyDocumentId = :documentId", TicketTechnicians.class)
                .setParameter("documentId", documentId);
        List<TicketTechnicians> record = query.getResultList();
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
    public List<TicketTechnicians> getPendingPoliciesUpload() {
        TypedQuery<TicketTechnicians> query = em.createQuery("SELECT p FROM PolicyTemp p WHERE p.actionType = 'NEW'", TicketTechnicians.class);
        List<TicketTechnicians> record = query.getResultList();
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
    public List<AppUser> getPolicyChampions() {
        TypedQuery<AppUser> query = em.createQuery("SELECT p FROM AppUser p WHERE p.policyChampion = true", AppUser.class);
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
}
