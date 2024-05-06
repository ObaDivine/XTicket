package com.ngxgroup.xticket.repository;

import com.ngxgroup.xticket.model.AppRoles;
import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.model.AuditLog;
import com.ngxgroup.xticket.model.GroupRoles;
import com.ngxgroup.xticket.model.Notification;
import com.ngxgroup.xticket.model.RoleGroups;
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

}
