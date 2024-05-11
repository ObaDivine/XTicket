package com.ngxgroup.xticket;

import com.ngxgroup.xticket.model.AppRoles;
import com.ngxgroup.xticket.model.AppUser;
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
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class ServletInitializer extends SpringBootServletInitializer implements ApplicationRunner {

    @Autowired
    XTicketRepository xticketRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Value("${xticket.default.email.domain}")
    private String emailDomain;
    @Value("${xticket.password.default}")
    private String defaultPassword;
    @Value("${xticket.company.phone}")
    private String companyPhone;

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(XTicketApplication.class);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        //Add App Roles
        Map<String, String> appRoles = new HashMap<>();
        appRoles.put("DASHBOARD", "Application Dashboard");
        appRoles.put("UPDATE_ROLES", "Update Roles");
        appRoles.put("DELETE_ROLES", "Delete Roles");
        appRoles.put("ADD_ROLES", "Add Roles");
        appRoles.put("LIST_USER", "List System Users");
        appRoles.put("MANAGE_USER", "Manage Application Users");
        appRoles.put("ADD_TICKET_GROUP", "Add Ticket Group");
        appRoles.put("DELETE_TICKET_GROUP", "Delete Ticket Group");
        appRoles.put("UPDATE_TICKET_GROUP", "Update Ticket Group");
        appRoles.put("LIST_TICKET_GROUP", "List Ticket Type");
        appRoles.put("ADD_TICKET_TYPE", "Add Ticket Type");
        appRoles.put("DELETE_TICKET_TYPE", "Delete Ticket Type");
        appRoles.put("UPDATE_TICKET_TYPE", "Update Ticket Type");
        appRoles.put("LIST_TICKET_TYPE", "List Ticket Type");
        appRoles.put("ADD_TICKET_AGENT", "Add Ticket Agent");
        appRoles.put("DELETE_TICKET_AGENT", "Delete Ticket Agent");
        appRoles.put("UPDATE_TICKET_AGENT", "Update Ticket Agent");
        appRoles.put("LIST_TICKET_AGENT", "List Ticket Agent");
        appRoles.put("KNOWLEDGE_BASE", "View Knowledge Base Documentation");
        appRoles.put("RAISE_TICKET", "Raise Tickets");
        appRoles.put("REPORT", "View Reports accross entities");
        appRoles.put("TICKET_AGENT", "Set user as a ticket agent");

        for (Map.Entry<String, String> role : appRoles.entrySet()) {
            //Check if the role exist already
            AppRoles appRole = xticketRepository.getRoleUsingRoleName(role.getKey());
            if (appRole == null) {
                AppRoles newRole = new AppRoles();
                newRole.setRoleName(role.getKey());
                newRole.setRoleDesc(role.getValue());
                xticketRepository.createAppRole(newRole);
            }
        }

        //Create the System Admin role group
        RoleGroups saGroup = xticketRepository.getRoleGroupUsingGroupName("SA");
        if (saGroup == null) {
            RoleGroups newGroup = new RoleGroups();
            newGroup.setCreatedAt(LocalDateTime.now());
            newGroup.setGroupName("SA");
            saGroup = xticketRepository.createRoleGroup(newGroup);
        }

        //Check the group roles
        List<GroupRoles> saGroupRoles = xticketRepository.getGroupRolesUsingRoleGroup(saGroup);
        if (saGroupRoles == null) {
            AppRoles dashboard = xticketRepository.getRoleUsingRoleName("DASHBOARD");
            if (dashboard != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(dashboard);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles knowledge = xticketRepository.getRoleUsingRoleName("KNOWLEDGE_BASE");
            if (knowledge != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(knowledge);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles ticket = xticketRepository.getRoleUsingRoleName("RAISE_TICKET");
            if (ticket != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(ticket);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles addRole = xticketRepository.getRoleUsingRoleName("ADD_ROLES");
            if (addRole != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(addRole);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles updateRole = xticketRepository.getRoleUsingRoleName("UPDATE_ROLES");
            if (updateRole != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(updateRole);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles deleteRole = xticketRepository.getRoleUsingRoleName("DELETE_ROLES");
            if (deleteRole != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(deleteRole);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles listUser = xticketRepository.getRoleUsingRoleName("LIST_USER");
            if (listUser != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(listUser);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles manageUser = xticketRepository.getRoleUsingRoleName("MANAGE_USER");
            if (manageUser != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(manageUser);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles addTicketGroup = xticketRepository.getRoleUsingRoleName("ADD_TICKET_GROUP");
            if (addTicketGroup != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(addTicketGroup);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles updateTicketGroup = xticketRepository.getRoleUsingRoleName("UPDATE_TICKET_GROUP");
            if (updateTicketGroup != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(updateTicketGroup);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles deleteTicketGroup = xticketRepository.getRoleUsingRoleName("DELETE_TICKET_GROUP");
            if (deleteTicketGroup != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(deleteTicketGroup);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles listTicketGroup = xticketRepository.getRoleUsingRoleName("LIST_TICKET_GROUP");
            if (listTicketGroup != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(listTicketGroup);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles addTicketType = xticketRepository.getRoleUsingRoleName("ADD_TICKET_TYPE");
            if (addTicketType != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(addTicketType);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles updateTicketType = xticketRepository.getRoleUsingRoleName("UPDATE_TICKET_TYPE");
            if (updateTicketType != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(updateTicketType);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles deleteTicketType = xticketRepository.getRoleUsingRoleName("DELETE_TICKET_TYPE");
            if (deleteTicketType != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(deleteTicketType);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles listTicketType = xticketRepository.getRoleUsingRoleName("LIST_TICKET_TYPE");
            if (listTicketType != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(listTicketType);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles addTicketAgent = xticketRepository.getRoleUsingRoleName("ADD_TICKET_AGENT");
            if (addTicketAgent != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(addTicketAgent);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles updateTicketAgent = xticketRepository.getRoleUsingRoleName("UPDATE_TICKET_AGENT");
            if (updateTicketAgent != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(updateTicketAgent);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles deleteTicketAgent = xticketRepository.getRoleUsingRoleName("DELETE_TICKET_AGENT");
            if (deleteTicketAgent != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(deleteTicketAgent);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles listTicketAgent = xticketRepository.getRoleUsingRoleName("LIST_TICKET_AGENT");
            if (listTicketAgent != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(listTicketAgent);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles report = xticketRepository.getRoleUsingRoleName("REPORT");
            if (report != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(report);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles ticketAgent = xticketRepository.getRoleUsingRoleName("TICKET_AGENT");
            if (ticketAgent != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(ticketAgent);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }
        }

        //Create the default role group
        RoleGroups defaultGroup = xticketRepository.getRoleGroupUsingGroupName("DEFAULT");
        if (defaultGroup == null) {
            RoleGroups newGroup = new RoleGroups();
            newGroup.setCreatedAt(LocalDateTime.now());
            newGroup.setGroupName("DEFAULT");
            defaultGroup = xticketRepository.createRoleGroup(newGroup);
        }

        //Check the group roles
        List<GroupRoles> defaultGroupRoles = xticketRepository.getGroupRolesUsingRoleGroup(defaultGroup);
        if (defaultGroupRoles == null) {
            AppRoles dashboard = xticketRepository.getRoleUsingRoleName("DASHBOARD");
            if (dashboard != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(dashboard);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(defaultGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles knowledge = xticketRepository.getRoleUsingRoleName("KNOWLEDGE_BASE");
            if (knowledge != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(knowledge);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(defaultGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles ticket = xticketRepository.getRoleUsingRoleName("RAISE_TICKET");
            if (ticket != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(ticket);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(defaultGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }
        }

        //Add System Admin User
        AppUser sa = xticketRepository.getAppUserUsingEmail("sa@" + emailDomain);
        if (sa == null) {
            AppUser newSA = new AppUser();
            newSA.setActivated(true);
            newSA.setActivationId(UUID.randomUUID().toString().replace("-", ""));
            newSA.setAgent(false);
            newSA.setCreatedAt(LocalDateTime.now());
            newSA.setCreatedBy(null);
            newSA.setEmail("sa@" + emailDomain);
            newSA.setGender("Male");
            newSA.setInternal(true);
            newSA.setLastLogin(LocalDateTime.now());
            newSA.setLastName("System");
            newSA.setLocked(false);
            newSA.setLoginFailCount(0);
            newSA.setMobileNumber(companyPhone);
            newSA.setOnline(false);
            newSA.setOtherName("Admin");
            newSA.setPassword(passwordEncoder.encode(defaultPassword));
            newSA.setPasswordChangeDate(LocalDate.now().plusYears(50));
            newSA.setResetTime(LocalDateTime.now());
            newSA.setRole(saGroup);
            newSA.setUpdatedAt(LocalDateTime.now());
            newSA.setUpdatedBy(null);

            xticketRepository.createAppUser(newSA);
        }

    }

}
