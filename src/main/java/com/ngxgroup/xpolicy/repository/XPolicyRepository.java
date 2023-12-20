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
import java.util.List;

/**
 *
 * @author bokon
 */
public interface XPolicyRepository {

    List<Company> getCompanies();

    Company createCompany(Company company);

    Company updateCompany(Company company);

    Company deleteCompany(Company company);

    Company getCompanyUsingCode(String companyCode);

    Company getCompanyUsingId(long id);

    List<Division> getDivisionUsingCompany(Company company);

    Division createDivision(Division division);

    Division updateDivision(Division division);

    Division deleteDivision(Division division);

    Division getDivisionUsingId(long id);

    Division getDivisionUsingCode(String divisionCode);

    List<Department> getDepartmentUsingDivision(Division division);

    Department createDepartment(Department department);

    Department updateDepartment(Department department);

    Department deleteDepartment(Department department);

    Department getDepartmentUsingId(long id);

    Department getDepartmentUsingCode(String departmentCode);

    AppUser getAppUserUsingUsername(String username);

    AppUser getAppUserUsingId(long id);

    AppUser updateAppUser(AppUser appUser);

    AppUser deleteAppUser(AppUser appUser);

    AppUser createAppUser(AppUser appUser);

    List<AppUser> getActiveUsers();

    List<AppUser> getDisabledUsers();

    List<AppUser> getPolicyChampions();

    List<AppUser> getUsers();

    AppUser getAppUserUsingUserId(String id);

    List<PolicyType> getPolicyTypes();

    PolicyType getPolicyTypeUsingCode(String policyTypeCode);

    PolicyType getPolicyTypeUsingId(long id);

    PolicyType createPolicyType(PolicyType policyType);

    List<Policy> getPoliciesUsingType(PolicyType policyType);

    List<Policy> getPolicyUsingDepartmentAndType(Department department, PolicyType policyType);

    List<Policy> getPolicies(Company company, PolicyType policyType, int accessLevel);

    List<Policy> getPolicies(Division division, PolicyType policyType, int accessLevel);

    List<Policy> getPolicies(Department department, PolicyType policyType, int accessLevel);

    List<Policy> getPolicies(Company company, Division division, PolicyType policyType, int accessLevel);

    List<Policy> getPolicies(Company company, Department department, PolicyType policyType, int accessLevel);

    List<Policy> getPolicies(Company company, Division division, Department department, PolicyType policyType, int accessLevel);

    List<Policy> getPolicies(Division division, Department department, PolicyType policyType, int accessLevel);

    List<Policy> getPolicies(PolicyType policyType);

    List<Policy> getPolicies(PolicyType policyType, int accessLevel);

    List<Policy> getPolicies();

    List<Policy> getPolicyExpiringToday(int daysBeforeExpiry);

    Policy getPolicyUsingId(long id);

    Policy getPolicyUsingName(String policyName);

    Policy getPolicyUsingCode(String policyCode);

    Policy getPolicyUsingDocumentId(String documentId);

    Policy createPolicy(Policy policy);

    Policy updatePolicy(Policy policy);

    Policy deletePolicy(Policy policy);

    PolicyRead createPolicyRead(PolicyRead policyRead);

    PolicyRead updatePolicyRead(PolicyRead policyRead);

    PolicyRead deletePolicyRead(PolicyRead policyRead);

    PolicyRead getPolicyReadUsingUserAndPolicy(AppUser appUser, Policy policy);

    List<PolicyRead> getPolicyReadUsingPolicy(Policy policy);

    List<Policy> getMustReadPolicy();

    List<PolicyTemp> getPendingPolicies();

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

    PolicyReview createPolicyReview(PolicyReview policyReview);

    List<PolicyReview> getAllPolicyReview();

    List<PolicyReview> getPolicyReview(String startDate, String endDate);

    List<PolicyReview> getPolicyReviewUsingPolicy(Policy policy);

    PolicyReview deletePolicyReview(PolicyReview policyReview);

    List<Policy> getExpiredPolicies();

    PolicyTemp createPolicyTemp(PolicyTemp policyTemp);

    PolicyTemp deletePolicyTemp(PolicyTemp policyTemp);

    PolicyTemp getPolicyTempUsingId(long id);

    PolicyTemp getPolicyTempUsingPolicy(Policy policy);

    PolicyTemp getPolicyTempUsingDocumentId(String documentId);

    List<PolicyTemp> getPendingPoliciesUpload();

    List<AppUser> getAppUsersForTFAFix();

    List<AuditLog> getAuditLogUsingDate(String startDate, String enddate);

    AuditLog createAuditLog(AuditLog auditLog);

}
