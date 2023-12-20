package com.ngxgroup.xpolicy.repository;

import com.ngxgroup.xpolicy.model.AppRoles;
import com.ngxgroup.xpolicy.model.AppUser;
import com.ngxgroup.xpolicy.model.AuditLog;
import com.ngxgroup.xpolicy.model.Company;
import com.ngxgroup.xpolicy.model.Department;
import com.ngxgroup.xpolicy.model.Division;
import com.ngxgroup.xpolicy.model.GroupRoles;
import com.ngxgroup.xpolicy.model.Notification;
import com.ngxgroup.xpolicy.model.Policy;
import com.ngxgroup.xpolicy.model.PolicyRead;
import com.ngxgroup.xpolicy.model.PolicyReview;
import com.ngxgroup.xpolicy.model.PolicyTemp;
import com.ngxgroup.xpolicy.model.PolicyType;
import com.ngxgroup.xpolicy.model.RoleGroups;
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
public class XPolicyRepositoryImpl implements XPolicyRepository {

    @PersistenceContext
    EntityManager em;

    @Override
    public List<Company> getCompanies() {
        TypedQuery<Company> query = em.createQuery("SELECT p FROM Company p", Company.class);
        List<Company> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public Company createCompany(Company company) {
        em.persist(company);
        em.flush();
        return company;
    }

    @Override
    public Company updateCompany(Company company) {
        em.merge(company);
        em.flush();
        return company;
    }

    @Override
    public Company deleteCompany(Company company) {
        em.remove(em.contains(company) ? company : em.merge(company));
        em.flush();
        return company;
    }

    @Override
    public Company getCompanyUsingCode(String companyCode) {
        TypedQuery<Company> query = em.createQuery("SELECT p FROM Company p WHERE p.companyCode = :companyCode", Company.class)
                .setParameter("companyCode", companyCode);
        List<Company> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Company getCompanyUsingId(long id) {
        TypedQuery<Company> query = em.createQuery("SELECT p FROM Company p WHERE p.id = :id", Company.class)
                .setParameter("id", id);
        List<Company> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<Division> getDivisionUsingCompany(Company company) {
        TypedQuery<Division> query = em.createQuery("SELECT p FROM Division p WHERE p.company = :company", Division.class)
                .setParameter("company", company);
        List<Division> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public Division createDivision(Division division) {
        em.persist(division);
        em.flush();
        return division;
    }

    @Override
    public Division updateDivision(Division division) {
        em.merge(division);
        em.flush();
        return division;
    }

    @Override
    public Division deleteDivision(Division division) {
        em.remove(em.contains(division) ? division : em.merge(division));
        em.flush();
        return division;
    }

    @Override
    public Division getDivisionUsingId(long id) {
        TypedQuery<Division> query = em.createQuery("SELECT p FROM Division p WHERE p.id = :id", Division.class)
                .setParameter("id", id);
        List<Division> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Division getDivisionUsingCode(String divisionCode) {
        TypedQuery<Division> query = em.createQuery("SELECT p FROM Division p WHERE p.divisionCode = :divisionCode", Division.class)
                .setParameter("divisionCode", divisionCode);
        List<Division> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<Department> getDepartmentUsingDivision(Division division) {
        TypedQuery<Department> query = em.createQuery("SELECT p FROM Department p WHERE p.division = :division", Department.class)
                .setParameter("division", division);
        List<Department> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
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
    public Department getDepartmentUsingId(long id) {
        TypedQuery<Department> query = em.createQuery("SELECT p FROM Department p WHERE p.id = :id", Department.class)
                .setParameter("id", id);
        List<Department> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Department getDepartmentUsingCode(String departmentCode) {
        TypedQuery<Department> query = em.createQuery("SELECT p FROM Department p WHERE p.departmentCode = :departmentCode", Department.class)
                .setParameter("departmentCode", departmentCode);
        List<Department> record = query.getResultList();
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
    public List<PolicyType> getPolicyTypes() {
        TypedQuery<PolicyType> query = em.createQuery("SELECT p FROM PolicyType p", PolicyType.class);
        List<PolicyType> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public PolicyType getPolicyTypeUsingCode(String policyTypeCode) {
        TypedQuery<PolicyType> query = em.createQuery("SELECT p FROM PolicyType p WHERE p.policyTypeCode = :policyTypeCode", PolicyType.class)
                .setParameter("policyTypeCode", policyTypeCode);
        List<PolicyType> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public PolicyType getPolicyTypeUsingId(long id) {
        TypedQuery<PolicyType> query = em.createQuery("SELECT p FROM PolicyType p WHERE p.id = :id", PolicyType.class)
                .setParameter("id", id);
        List<PolicyType> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public PolicyType createPolicyType(PolicyType policyType) {
        em.persist(policyType);
        em.flush();
        return policyType;
    }

    @Override
    public List<Policy> getPoliciesUsingType(PolicyType policyType) {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p WHERE p.policyType = :policyType", Policy.class)
                .setParameter("policyType", policyType);
        List<Policy> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<Policy> getPolicyUsingDepartmentAndType(Department department, PolicyType policyType) {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p WHERE p.department = :department AND p.policyType = :policyType", Policy.class)
                .setParameter("department", department)
                .setParameter("policyType", policyType);
        List<Policy> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<Policy> getPolicies(Company company, PolicyType policyType, int accessLevel) {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p WHERE p.company = :company AND p.policyType = :policyType AND p.accessLevel <= :accessLevel", Policy.class)
                .setParameter("company", company)
                .setParameter("policyType", policyType)
                .setParameter("accessLevel", accessLevel);
        List<Policy> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<Policy> getPolicies(Division division, PolicyType policyType, int accessLevel) {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p WHERE p.division = :division AND p.policyType = :policyType AND p.accessLevel <= :accessLevel", Policy.class)
                .setParameter("division", division)
                .setParameter("policyType", policyType)
                .setParameter("accessLevel", accessLevel);
        List<Policy> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<Policy> getPolicies(Department department, PolicyType policyType, int accessLevel) {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p WHERE p.department = :department AND p.policyType = :policyType AND p.accessLevel <= :accessLevel", Policy.class)
                .setParameter("department", department)
                .setParameter("policyType", policyType)
                .setParameter("accessLevel", accessLevel);
        List<Policy> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<Policy> getPolicies(Company company, Division division, PolicyType policyType, int accessLevel) {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p WHERE p.company = :company AND p.division = :division AND p.policyType = :policyType AND p.accessLevel <= :accessLevel", Policy.class)
                .setParameter("company", company)
                .setParameter("division", division)
                .setParameter("policyType", policyType)
                .setParameter("accessLevel", accessLevel);
        List<Policy> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<Policy> getPolicies(Company company, Department department, PolicyType policyType, int accessLevel) {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p WHERE p.company = :company AND p.department = :department AND p.policyType = :policyType AND p.accessLevel <= :accessLevel", Policy.class)
                .setParameter("company", company)
                .setParameter("department", department)
                .setParameter("policyType", policyType)
                .setParameter("accessLevel", accessLevel);
        List<Policy> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<Policy> getPolicies(Company company, Division division, Department department, PolicyType policyType, int accessLevel) {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p WHERE p.company = :company AND p.division = :division AND p.department = :department AND p.policyType = :policyType AND p.accessLevel <= :accessLevel", Policy.class)
                .setParameter("company", company)
                .setParameter("division", division)
                .setParameter("department", department)
                .setParameter("policyType", policyType)
                .setParameter("accessLevel", accessLevel);
        List<Policy> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<Policy> getPolicies(Division division, Department department, PolicyType policyType, int accessLevel) {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p WHERE p.division = :division AND p.department = :department AND p.policyType = :policyType AND p.accessLevel <= :accessLevel", Policy.class)
                .setParameter("division", division)
                .setParameter("department", department)
                .setParameter("policyType", policyType)
                .setParameter("accessLevel", accessLevel);
        List<Policy> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<Policy> getPolicies(PolicyType policyType) {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p WHERE p.policyType = :policyType", Policy.class)
                .setParameter("policyType", policyType);
        List<Policy> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<Policy> getPolicies(PolicyType policyType, int accessLevel) {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p WHERE p.policyType = :policyType AND p.accessLevel <= :accessLevel", Policy.class)
                .setParameter("policyType", policyType)
                .setParameter("accessLevel", accessLevel);
        List<Policy> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<PolicyTemp> getPendingPolicies() {
        TypedQuery<PolicyTemp> query = em.createQuery("SELECT p FROM PolicyTemp p", PolicyTemp.class);
        List<PolicyTemp> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public Policy getPolicyUsingId(long id) {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p WHERE p.id = :id", Policy.class)
                .setParameter("id", id);
        List<Policy> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Policy getPolicyUsingName(String policyName) {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p WHERE p.policyName = :policyName", Policy.class)
                .setParameter("policyName", policyName);
        List<Policy> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Policy getPolicyUsingCode(String policyCode) {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p WHERE p.policyCode = :policyCode", Policy.class)
                .setParameter("policyCode", policyCode);
        List<Policy> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public PolicyRead createPolicyRead(PolicyRead policyRead) {
        em.persist(policyRead);
        em.flush();
        return policyRead;
    }

    @Override
    public PolicyRead updatePolicyRead(PolicyRead policyRead) {
        em.merge(policyRead);
        em.flush();
        return policyRead;
    }

    @Override
    public PolicyRead deletePolicyRead(PolicyRead policyRead) {
        em.remove(em.contains(policyRead) ? policyRead : em.merge(policyRead));
        em.flush();
        return policyRead;
    }

    @Override
    public PolicyRead getPolicyReadUsingUserAndPolicy(AppUser appUser, Policy policy) {
        TypedQuery<PolicyRead> query = em.createQuery("SELECT p FROM PolicyRead p WHERE p.appUser = :appUser AND p.policy = :policy", PolicyRead.class)
                .setParameter("appUser", appUser)
                .setParameter("policy", policy);
        List<PolicyRead> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<Policy> getMustReadPolicy() {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p WHERE p.mustRead = true", Policy.class);
        List<Policy> record = query.getResultList();
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
    public List<Policy> getPolicies() {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p", Policy.class);
        List<Policy> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public Policy createPolicy(Policy policy) {
        em.persist(policy);
        em.flush();
        return policy;
    }

    @Override
    public Policy updatePolicy(Policy policy) {
        em.merge(policy);
        em.flush();
        return policy;
    }

    @Override
    public Policy deletePolicy(Policy policy) {
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
    public Policy getPolicyUsingDocumentId(String documentId) {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p WHERE p.policyDocumentId = :documentId", Policy.class)
                .setParameter("documentId", documentId);
        List<Policy> record = query.getResultList();
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
    public PolicyReview createPolicyReview(PolicyReview policyReview) {
        em.persist(policyReview);
        em.flush();
        return policyReview;
    }

    @Override
    public PolicyReview deletePolicyReview(PolicyReview policyReview) {
        em.remove(em.contains(policyReview) ? policyReview : em.merge(policyReview));
        em.flush();
        return policyReview;
    }

    @Override
    public List<PolicyReview> getAllPolicyReview() {
        TypedQuery<PolicyReview> query = em.createQuery("SELECT p FROM PolicyReview p", PolicyReview.class);
        List<PolicyReview> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<PolicyReview> getPolicyReview(String startDate, String endDate) {
        TypedQuery<PolicyReview> query = em.createQuery("SELECT p FROM PolicyReview p WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate", PolicyReview.class)
                .setParameter("startDate", Date.parse(startDate))
                .setParameter("endDate", Date.parse(endDate));
        List<PolicyReview> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<PolicyReview> getPolicyReviewUsingPolicy(Policy policy) {
        TypedQuery<PolicyReview> query = em.createQuery("SELECT p FROM PolicyReview p WHERE p.policy = :policy", PolicyReview.class)
                .setParameter("policy", policy);
        List<PolicyReview> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<Policy> getPolicyExpiringToday(int daysBeforeExpiry) {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p WHERE p.expiryDate <= :today AND p.expired = false", Policy.class)
                .setParameter("today", LocalDate.now().plusDays(daysBeforeExpiry));
        List<Policy> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<Policy> getExpiredPolicies() {
        TypedQuery<Policy> query = em.createQuery("SELECT p FROM Policy p WHERE p.expired = true", Policy.class);
        List<Policy> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<PolicyRead> getPolicyReadUsingPolicy(Policy policy) {
        TypedQuery<PolicyRead> query = em.createQuery("SELECT p FROM PolicyRead p WHERE p.policy = :policy", PolicyRead.class)
                .setParameter("policy", policy);
        List<PolicyRead> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public PolicyTemp createPolicyTemp(PolicyTemp policyTemp) {
        em.persist(em.contains(policyTemp) ? policyTemp : em.merge(policyTemp));
        em.flush();
        return policyTemp;
    }

    @Override
    public PolicyTemp deletePolicyTemp(PolicyTemp policyTemp) {
        em.remove(em.contains(policyTemp) ? policyTemp : em.merge(policyTemp));
        em.flush();
        return policyTemp;
    }

    @Override
    public PolicyTemp getPolicyTempUsingId(long id) {
        TypedQuery<PolicyTemp> query = em.createQuery("SELECT p FROM PolicyTemp p WHERE p.id = :id", PolicyTemp.class)
                .setParameter("id", id);
        List<PolicyTemp> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public PolicyTemp getPolicyTempUsingPolicy(Policy policy) {
        TypedQuery<PolicyTemp> query = em.createQuery("SELECT p FROM PolicyTemp p WHERE p.policy = :policy", PolicyTemp.class)
                .setParameter("policy", policy);
        List<PolicyTemp> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public PolicyTemp getPolicyTempUsingDocumentId(String documentId) {
        TypedQuery<PolicyTemp> query = em.createQuery("SELECT p FROM PolicyTemp p WHERE p.policyDocumentId = :documentId", PolicyTemp.class)
                .setParameter("documentId", documentId);
        List<PolicyTemp> record = query.getResultList();
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
    public List<PolicyTemp> getPendingPoliciesUpload() {
        TypedQuery<PolicyTemp> query = em.createQuery("SELECT p FROM PolicyTemp p WHERE p.actionType = 'NEW'", PolicyTemp.class);
        List<PolicyTemp> record = query.getResultList();
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
