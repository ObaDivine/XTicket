package com.ngxgroup.xticket;

import com.ngxgroup.xticket.model.AppRoles;
import com.ngxgroup.xticket.model.GroupRoles;
import com.ngxgroup.xticket.model.RoleGroups;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import com.ngxgroup.xticket.repository.XTicketRepository;

//@Component
public class ServletInitializer extends SpringBootServletInitializer implements ApplicationRunner {

    @Autowired
    XTicketRepository xpolicyRepository;

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(XTicketApplication.class);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
               

        //Add App Roles
        Map<String, String> appRoles = new HashMap<>();
        appRoles.put("LIST_POLICY", "List Policies");
        appRoles.put("UPDATE_POLICY", "Update Policies");
        appRoles.put("DELETE_POLICY", "Delete Policies");
        appRoles.put("ADD_POLICY", "Add Policies");
        appRoles.put("APPROVE_POLICY", "Approve Policies");
        appRoles.put("REVIEW_POLICY", "Approve Policies");
        appRoles.put("MANAGE_USER", "Manage Application Users");
        appRoles.put("MANAGE_ROLES", "Manage Application Roles");
        appRoles.put("GENERATE_REPORT", "Generate Report");
        appRoles.put("POLICY", "View Policies accross entities");
        appRoles.put("SOP", "View Standard Operating Procedures accross entities");
        appRoles.put("CHARTER", "View Charters accross entities");
        appRoles.put("FORMS", "View Forms accross entities");
        appRoles.put("TEMPLATES", "View Templates accross entities");
        appRoles.put("COMMITTEES AND ASSOCIATION", "View Committees and Association accross entities");
        appRoles.put("SCHEDULE", "View Schedules accross entities");
        appRoles.put("REPORT", "View Reports accross entities");

        for (Map.Entry<String, String> role : appRoles.entrySet()) {
            //Check if the role exist already
            AppRoles appRole = xpolicyRepository.getRoleUsingRoleName(role.getKey());
            if (appRole == null) {
                AppRoles newRole = new AppRoles();
                newRole.setRoleName(role.getKey());
                newRole.setRoleDesc(role.getValue());
                xpolicyRepository.createAppRole(newRole);
            }
        }

        //Create the Admin Role Group
        RoleGroups adminRole = xpolicyRepository.getRoleGroupUsingGroupName("ADMIN");
        RoleGroups adminGroup = null;
        if (adminRole == null) {
            RoleGroups newGroup = new RoleGroups();
            newGroup.setCreatedAt(LocalDateTime.now());
            newGroup.setGroupName("ADMIN");
            adminGroup = xpolicyRepository.createRoleGroup(newGroup);
        }

        RoleGroups userRole = xpolicyRepository.getRoleGroupUsingGroupName("USER");
        RoleGroups userGroup = null;
        if (userRole == null) {
            RoleGroups newGroup = new RoleGroups();
            newGroup.setCreatedAt(LocalDateTime.now());
            newGroup.setGroupName("USER");
            userGroup = xpolicyRepository.createRoleGroup(newGroup);
        }

        RoleGroups policyUploadRole = xpolicyRepository.getRoleGroupUsingGroupName("POLICY UPLOAD");
        RoleGroups policyUploadGroup = null;
        if (policyUploadRole == null) {
            RoleGroups newGroup = new RoleGroups();
            newGroup.setCreatedAt(LocalDateTime.now());
            newGroup.setGroupName("POLICY UPLOAD");
            policyUploadGroup = xpolicyRepository.createRoleGroup(newGroup);
        }

        //Check the group roles
        List<GroupRoles> adminGroupRoles = xpolicyRepository.getGroupRolesUsingRoleGroup(adminGroup);
        if (adminGroupRoles == null) {
            AppRoles appRole = xpolicyRepository.getRoleUsingRoleName("MANAGE_POLICY");
            if (appRole != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRole);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleUser = xpolicyRepository.getRoleUsingRoleName("MANAGE_USER");
            if (appRoleUser != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleUser);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleReport = xpolicyRepository.getRoleUsingRoleName("GENERATE_REPORT");
            if (appRoleReport != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleReport);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRolePolicy = xpolicyRepository.getRoleUsingRoleName("POLICY");
            if (appRolePolicy != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRolePolicy);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleSOP = xpolicyRepository.getRoleUsingRoleName("SOP");
            if (appRoleSOP != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleSOP);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleStandard = xpolicyRepository.getRoleUsingRoleName("COMMITTEES AND ASSOCIATION");
            if (appRoleStandard != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleStandard);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleFramework = xpolicyRepository.getRoleUsingRoleName("TEMPLATES");
            if (appRoleFramework != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleFramework);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleSchedule = xpolicyRepository.getRoleUsingRoleName("SCHEDULE");
            if (appRoleSchedule != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleSchedule);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleCharter = xpolicyRepository.getRoleUsingRoleName("CHARTER");
            if (appRoleCharter != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleCharter);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleForm = xpolicyRepository.getRoleUsingRoleName("FORM");
            if (appRoleForm != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleForm);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleListPolicyList = xpolicyRepository.getRoleUsingRoleName("LIST_POLICY");
            if (appRoleListPolicyList != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleListPolicyList);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleListPolicyApprove = xpolicyRepository.getRoleUsingRoleName("APPROVE_POLICY");
            if (appRoleListPolicyApprove != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleListPolicyApprove);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleListPolicyDelete = xpolicyRepository.getRoleUsingRoleName("DELETE_POLICY");
            if (appRoleListPolicyDelete != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleListPolicyDelete);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
        }

        List<GroupRoles> userGroupRoles = xpolicyRepository.getGroupRolesUsingRoleGroup(userGroup);
        if (userGroupRoles == null) {
            AppRoles appRolePolicy = xpolicyRepository.getRoleUsingRoleName("POLICY");
            if (appRolePolicy != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRolePolicy);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(userGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleSOP = xpolicyRepository.getRoleUsingRoleName("SOP");
            if (appRoleSOP != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleSOP);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(userGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleStandard = xpolicyRepository.getRoleUsingRoleName("COMMITTEES AND ASSOCIATION");
            if (appRoleStandard != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleStandard);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(userGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleFramework = xpolicyRepository.getRoleUsingRoleName("TEMPLATES");
            if (appRoleFramework != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleFramework);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(userGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleSchedule = xpolicyRepository.getRoleUsingRoleName("SCHEDULE");
            if (appRoleSchedule != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleSchedule);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(userGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleCharter = xpolicyRepository.getRoleUsingRoleName("CHARTER");
            if (appRoleCharter != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleCharter);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(userGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleForm = xpolicyRepository.getRoleUsingRoleName("FORM");
            if (appRoleForm != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleForm);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(userGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
        }

        List<GroupRoles> policyUploadGroupRoles = xpolicyRepository.getGroupRolesUsingRoleGroup(policyUploadGroup);
        if (policyUploadGroupRoles == null) {
            AppRoles appRolePolicy = xpolicyRepository.getRoleUsingRoleName("POLICY");
            if (appRolePolicy != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRolePolicy);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleSOP = xpolicyRepository.getRoleUsingRoleName("SOP");
            if (appRoleSOP != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleSOP);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleStandard = xpolicyRepository.getRoleUsingRoleName("COMMITTEES AND ASSOCIATION");
            if (appRoleStandard != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleStandard);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleFramework = xpolicyRepository.getRoleUsingRoleName("TEMPLATES");
            if (appRoleFramework != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleFramework);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleSchedule = xpolicyRepository.getRoleUsingRoleName("SCHEDULE");
            if (appRoleSchedule != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleSchedule);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleCharter = xpolicyRepository.getRoleUsingRoleName("CHARTER");
            if (appRoleCharter != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleCharter);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleForm = xpolicyRepository.getRoleUsingRoleName("FORM");
            if (appRoleForm != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleForm);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleListPolicyList = xpolicyRepository.getRoleUsingRoleName("LIST_POLICY");
            if (appRoleListPolicyList != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleListPolicyList);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleListPolicyAdd = xpolicyRepository.getRoleUsingRoleName("ADD_POLICY");
            if (appRoleListPolicyAdd != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleListPolicyAdd);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleListPolicyDelete = xpolicyRepository.getRoleUsingRoleName("DELETE_POLICY");
            if (appRoleListPolicyDelete != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleListPolicyDelete);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleListPolicyUpdate = xpolicyRepository.getRoleUsingRoleName("UPDATE_POLICY");
            if (appRoleListPolicyUpdate != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleListPolicyUpdate);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
        }

    }

}
