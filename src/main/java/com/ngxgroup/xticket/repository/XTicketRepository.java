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
import java.util.List;

/**
 *
 * @author bokon
 */
public interface XTicketRepository {

    List<Tickets> getCompanies();

    Tickets createCompany(Tickets company);

    Tickets updateCompany(Tickets company);

    Tickets deleteCompany(Tickets company);

    Tickets getCompanyUsingCode(String companyCode);

    Tickets getCompanyUsingId(long id);

    List<TicketComment> getDivisionUsingCompany(Tickets company);

    TicketComment createDivision(TicketComment division);

    TicketComment updateDivision(TicketComment division);

    TicketComment deleteDivision(TicketComment division);

    TicketComment getDivisionUsingId(long id);

    TicketComment getDivisionUsingCode(String divisionCode);

    List<TicketUpload> getDepartmentUsingDivision(TicketComment division);

    TicketUpload createDepartment(TicketUpload department);

    TicketUpload updateDepartment(TicketUpload department);

    TicketUpload deleteDepartment(TicketUpload department);

    TicketUpload getDepartmentUsingId(long id);

    TicketUpload getDepartmentUsingCode(String departmentCode);

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

    List<TicketType> getPolicyTypes();

    TicketType getPolicyTypeUsingCode(String policyTypeCode);

    TicketType getPolicyTypeUsingId(long id);

    TicketType createPolicyType(TicketType policyType);

    List<TicketGroup> getPoliciesUsingType(TicketType policyType);

    List<TicketGroup> getPolicyUsingDepartmentAndType(TicketUpload department, TicketType policyType);

    List<TicketGroup> getPolicies(Tickets company, TicketType policyType, int accessLevel);

    List<TicketGroup> getPolicies(TicketComment division, TicketType policyType, int accessLevel);

    List<TicketGroup> getPolicies(TicketUpload department, TicketType policyType, int accessLevel);

    List<TicketGroup> getPolicies(Tickets company, TicketComment division, TicketType policyType, int accessLevel);

    List<TicketGroup> getPolicies(Tickets company, TicketUpload department, TicketType policyType, int accessLevel);

    List<TicketGroup> getPolicies(Tickets company, TicketComment division, TicketUpload department, TicketType policyType, int accessLevel);

    List<TicketGroup> getPolicies(TicketComment division, TicketUpload department, TicketType policyType, int accessLevel);

    List<TicketGroup> getPolicies(TicketType policyType);

    List<TicketGroup> getPolicies(TicketType policyType, int accessLevel);

    List<TicketGroup> getPolicies();

    List<TicketGroup> getPolicyExpiringToday(int daysBeforeExpiry);

    TicketGroup getPolicyUsingId(long id);

    TicketGroup getPolicyUsingName(String policyName);

    TicketGroup getPolicyUsingCode(String policyCode);

    TicketGroup getPolicyUsingDocumentId(String documentId);

    TicketGroup createPolicy(TicketGroup policy);

    TicketGroup updatePolicy(TicketGroup policy);

    TicketGroup deletePolicy(TicketGroup policy);

    TicketReopened createPolicyRead(TicketReopened policyRead);

    TicketReopened updatePolicyRead(TicketReopened policyRead);

    TicketReopened deletePolicyRead(TicketReopened policyRead);

    TicketReopened getPolicyReadUsingUserAndPolicy(AppUser appUser, TicketGroup policy);

    List<TicketReopened> getPolicyReadUsingPolicy(TicketGroup policy);

    List<TicketGroup> getMustReadPolicy();

    List<TicketTechnicians> getPendingPolicies();

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

    TicketEscalations createPolicyReview(TicketEscalations policyReview);

    List<TicketEscalations> getAllPolicyReview();

    List<TicketEscalations> getPolicyReview(String startDate, String endDate);

    List<TicketEscalations> getPolicyReviewUsingPolicy(TicketGroup policy);

    TicketEscalations deletePolicyReview(TicketEscalations policyReview);

    List<TicketGroup> getExpiredPolicies();

    TicketTechnicians createPolicyTemp(TicketTechnicians policyTemp);

    TicketTechnicians deletePolicyTemp(TicketTechnicians policyTemp);

    TicketTechnicians getPolicyTempUsingId(long id);

    TicketTechnicians getPolicyTempUsingPolicy(TicketGroup policy);

    TicketTechnicians getPolicyTempUsingDocumentId(String documentId);

    List<TicketTechnicians> getPendingPoliciesUpload();

    List<AppUser> getAppUsersForTFAFix();

    List<AuditLog> getAuditLogUsingDate(String startDate, String enddate);

    AuditLog createAuditLog(AuditLog auditLog);

}
