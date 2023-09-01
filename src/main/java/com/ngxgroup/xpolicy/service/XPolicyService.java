package com.ngxgroup.xpolicy.service;

import com.ngxgroup.xpolicy.model.AppUser;
import com.ngxgroup.xpolicy.model.Company;
import com.ngxgroup.xpolicy.model.Department;
import com.ngxgroup.xpolicy.model.Division;
import com.ngxgroup.xpolicy.model.GroupRoles;
import com.ngxgroup.xpolicy.model.Notification;
import com.ngxgroup.xpolicy.model.Policy;
import com.ngxgroup.xpolicy.model.PolicyReview;
import com.ngxgroup.xpolicy.model.PolicyTemp;
import com.ngxgroup.xpolicy.model.RoleGroups;
import com.ngxgroup.xpolicy.payload.LoginPayload;
import com.ngxgroup.xpolicy.payload.XPolicyPayload;
import java.util.List;

/**
 *
 * @author briano
 */
public interface XPolicyService {

    XPolicyPayload processSignin(LoginPayload requestPayload);

    String processSignUp(XPolicyPayload requestPayload);

    String validateTwoFactorAuthentication(XPolicyPayload requestPayload);

    String processSignUpActivation(String id);

    String processDashboard(String username);

    XPolicyPayload generateQRCode(XPolicyPayload requestPayload);

    List<Company> getCompanyList();

    List<Division> processCompanyDivisionUsingId(String company);

    List<Department> processDivisionDepartmentsUsingId(String division);

    int[] fetchStatistics();

    List<Policy> processPolicy(XPolicyPayload xpolicyPayload, String principal);

    List<Policy> getPolicies();

    Policy processPolicyUsingId(String id);

    void processPolicyRead(String fileName, String principal);

    List<Policy> getMustReadPolicy();

    List<PolicyTemp> getPendingPolicies();

    String getPrincipalName(String principal);

    List<Notification> getNotifications(String principal);

    List<GroupRoles> getUserRoles(String principal);

    XPolicyPayload processCreatePolicy(XPolicyPayload requestPayload, String principal);

    XPolicyPayload processDeletePolicy(String id, String principal);

    XPolicyPayload processApprovePolicy(String id, String principal);

    XPolicyPayload processDeclinePolicy(String id, String principal);

    List<AppUser> getUsers();

    XPolicyPayload getAppUserUsingId(String id);

    XPolicyPayload processCreateUser(XPolicyPayload requestPayload, String principal);

    XPolicyPayload processDeleteUser(String id, String principal);

    List<AppUser> getAppUsers();

    XPolicyPayload getPolicyUsingId(String id);

    XPolicyPayload processReviewPolicy(XPolicyPayload requestPayload, String principal);

    List<PolicyReview> getPolicyAllReview();

    XPolicyPayload generateReport(XPolicyPayload requestPayload);

    XPolicyPayload generateTwoFactorDetails();

    List<RoleGroups> getRoleGroupList();

    XPolicyPayload processCreateRoleGroup(XPolicyPayload requestPayload, String principal);

    XPolicyPayload getRoleGroupUsingId(String id);

    XPolicyPayload processDeleteRoleGroup(String id, String principal);
    
    Object fetchGroupRecord(String roleName);

    String updateGroupRoles(XPolicyPayload requestPayload, String principal);
}
