package com.ngxgroup.xticket;

import com.ngxgroup.xticket.model.AppRoles;
import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.model.Entities;
import com.ngxgroup.xticket.model.GroupRoles;
import com.ngxgroup.xticket.model.PublicHolidays;
import com.ngxgroup.xticket.model.RoleGroups;
import com.ngxgroup.xticket.model.TicketStatus;
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
    static final String SYSTEM_USER = "System";
    static final String ENABLE_STATUS = "Enabled";

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
        appRoles.put("ADD_TICKET_SLA", "Add Ticket SLA");
        appRoles.put("DELETE_TICKET_SLA", "Delete Ticket SLA");
        appRoles.put("UPDATE_TICKET_SLA", "Update Ticket SLA");
        appRoles.put("LIST_TICKET_SLA", "List Ticket SLA");
        appRoles.put("ADD_TICKET_STATUS", "Add Ticket Status");
        appRoles.put("DELETE_TICKET_STATUS", "Delete Ticket Status");
        appRoles.put("UPDATE_TICKET_STATUS", "Update Ticket Status");
        appRoles.put("LIST_TICKET_STATUS", "List Ticket Status");
        appRoles.put("ADD_SERVICE_UNIT", "Add Service Unit");
        appRoles.put("DELETE_SERVICE_UNIT", "Delete Service Unit");
        appRoles.put("UPDATE_SERVICE_UNIT", "Update Service Unit");
        appRoles.put("LIST_SERVICE_UNIT", "List Service Unit");
        appRoles.put("ADD_ENTITY", "Add Entity");
        appRoles.put("DELETE_ENTITY", "Delete Entity");
        appRoles.put("UPDATE_ENTITY", "Update Entity");
        appRoles.put("LIST_ENTITY", "List Entity");
        appRoles.put("KNOWLEDGE_BASE", "View Knowledge Base Documentation");
        appRoles.put("KNOWLEDGE_BASE_SETUP", "Setup Knowledge Base Documentation");
        appRoles.put("RAISE_TICKET", "Raise Tickets");
        appRoles.put("REPORT", "View Reports accross entities");
        appRoles.put("MANAGEMENT_REPORT", "View Management Reports");
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

            AppRoles knowledgeBase = xticketRepository.getRoleUsingRoleName("KNOWLEDGE_BASE_SETUP");
            if (knowledgeBase != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(knowledgeBase);
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
            
            AppRoles managementReport = xticketRepository.getRoleUsingRoleName("MANAGEMENT_REPORT");
            if (managementReport != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(managementReport);
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

            AppRoles addTicketStatus = xticketRepository.getRoleUsingRoleName("ADD_TICKET_STATUS");
            if (addTicketStatus != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(addTicketAgent);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles updateTicketStatus = xticketRepository.getRoleUsingRoleName("UPDATE_TICKET_STATUS");
            if (updateTicketStatus != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(updateTicketAgent);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles deleteTicketStatus = xticketRepository.getRoleUsingRoleName("DELETE_TICKET_STATUS");
            if (deleteTicketStatus != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(deleteTicketAgent);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles listTicketStatus = xticketRepository.getRoleUsingRoleName("LIST_TICKET_STATUS");
            if (listTicketStatus != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(listTicketAgent);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles addServiceUnit = xticketRepository.getRoleUsingRoleName("ADD_SERVICE_UNIT");
            if (addServiceUnit != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(addTicketAgent);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles updateServiceUnit = xticketRepository.getRoleUsingRoleName("UPDATE_SERVICE_UNIT");
            if (updateServiceUnit != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(updateTicketAgent);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles deleteServiceUnit = xticketRepository.getRoleUsingRoleName("DELETE_SERVICE_UNIT");
            if (deleteServiceUnit != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(deleteTicketAgent);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles listServiceUnit = xticketRepository.getRoleUsingRoleName("LIST_SERVICE_UNIT");
            if (listServiceUnit != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(listTicketAgent);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles addEntity = xticketRepository.getRoleUsingRoleName("ADD_ENTITY");
            if (addEntity != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(addTicketAgent);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles updateEntity = xticketRepository.getRoleUsingRoleName("UPDATE_ENTITY");
            if (updateEntity != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(updateTicketAgent);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles deleteEntity = xticketRepository.getRoleUsingRoleName("DELETE_ENTITY");
            if (deleteEntity != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(deleteTicketAgent);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles listEntity = xticketRepository.getRoleUsingRoleName("LIST_ENTITY");
            if (listEntity != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(listTicketAgent);
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
            
            AppRoles knowledgeBase = xticketRepository.getRoleUsingRoleName("KNOWLEDGE_BASE_SETUP");
            if (knowledgeBase != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(knowledgeBase);
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
            newSA.setLastName(SYSTEM_USER);
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

        //Add Ticket Status
        TicketStatus openStatus = xticketRepository.getTicketStatusUsingCode("OPEN");
        if (openStatus == null) {
            TicketStatus newStatus = new TicketStatus();
            newStatus.setCreatedAt(LocalDateTime.now());
            newStatus.setCreatedBy(SYSTEM_USER);
            newStatus.setStatus(ENABLE_STATUS);
            newStatus.setTicketStatusCode("OPEN");
            newStatus.setTicketStatusName("Open");
            newStatus.setPauseSLA(false);
            xticketRepository.createTicketStatus(newStatus);
        }

        TicketStatus completedStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
        if (completedStatus == null) {
            TicketStatus newStatus = new TicketStatus();
            newStatus.setCreatedAt(LocalDateTime.now());
            newStatus.setCreatedBy(SYSTEM_USER);
            newStatus.setStatus(ENABLE_STATUS);
            newStatus.setTicketStatusCode("CLOSED");
            newStatus.setTicketStatusName("Closed");
            newStatus.setPauseSLA(false);
            xticketRepository.createTicketStatus(newStatus);
        }

        //Add known public holidays
        PublicHolidays newYear = xticketRepository.getPublicHoliday(LocalDate.parse("2024-01-01"));
        if (newYear == null) {
            PublicHolidays newHoliday = new PublicHolidays();
            newHoliday.setCreatedAt(LocalDateTime.now());
            newHoliday.setCreatedBy(SYSTEM_USER);
            newHoliday.setHoliday(LocalDate.parse("2024-01-01"));
            xticketRepository.createPublicHoliday(newHoliday);
        }

        PublicHolidays democracyDay = xticketRepository.getPublicHoliday(LocalDate.parse("2024-06-12"));
        if (democracyDay == null) {
            PublicHolidays newHoliday = new PublicHolidays();
            newHoliday.setCreatedAt(LocalDateTime.now());
            newHoliday.setCreatedBy(SYSTEM_USER);
            newHoliday.setHoliday(LocalDate.parse("2024-06-12"));
            xticketRepository.createPublicHoliday(newHoliday);
        }

        PublicHolidays independenceDay = xticketRepository.getPublicHoliday(LocalDate.parse("2024-10-10"));
        if (independenceDay == null) {
            PublicHolidays newHoliday = new PublicHolidays();
            newHoliday.setCreatedAt(LocalDateTime.now());
            newHoliday.setCreatedBy(SYSTEM_USER);
            newHoliday.setHoliday(LocalDate.parse("2024-10-10"));
            xticketRepository.createPublicHoliday(newHoliday);
        }

        PublicHolidays goodFriday = xticketRepository.getPublicHoliday(LocalDate.parse("2024-03-29"));
        if (goodFriday == null) {
            PublicHolidays newHoliday = new PublicHolidays();
            newHoliday.setCreatedAt(LocalDateTime.now());
            newHoliday.setCreatedBy(SYSTEM_USER);
            newHoliday.setHoliday(LocalDate.parse("2024-03-29"));
            xticketRepository.createPublicHoliday(newHoliday);
        }

        PublicHolidays easterMonday = xticketRepository.getPublicHoliday(LocalDate.parse("2024-04-01"));
        if (easterMonday == null) {
            PublicHolidays newHoliday = new PublicHolidays();
            newHoliday.setCreatedAt(LocalDateTime.now());
            newHoliday.setCreatedBy(SYSTEM_USER);
            newHoliday.setHoliday(LocalDate.parse("2024-04-01"));
            xticketRepository.createPublicHoliday(newHoliday);
        }

        PublicHolidays idElFitr = xticketRepository.getPublicHoliday(LocalDate.parse("2024-04-10"));
        if (idElFitr == null) {
            PublicHolidays newHoliday = new PublicHolidays();
            newHoliday.setCreatedAt(LocalDateTime.now());
            newHoliday.setCreatedBy(SYSTEM_USER);
            newHoliday.setHoliday(LocalDate.parse("2024-04-10"));
            xticketRepository.createPublicHoliday(newHoliday);
        }

        PublicHolidays idElFitrHol = xticketRepository.getPublicHoliday(LocalDate.parse("2024-04-11"));
        if (idElFitrHol == null) {
            PublicHolidays newHoliday = new PublicHolidays();
            newHoliday.setCreatedAt(LocalDateTime.now());
            newHoliday.setCreatedBy(SYSTEM_USER);
            newHoliday.setHoliday(LocalDate.parse("2024-04-11"));
            xticketRepository.createPublicHoliday(newHoliday);
        }

        PublicHolidays workersDay = xticketRepository.getPublicHoliday(LocalDate.parse("2024-05-01"));
        if (workersDay == null) {
            PublicHolidays newHoliday = new PublicHolidays();
            newHoliday.setCreatedAt(LocalDateTime.now());
            newHoliday.setCreatedBy(SYSTEM_USER);
            newHoliday.setHoliday(LocalDate.parse("2024-05-01"));
            xticketRepository.createPublicHoliday(newHoliday);
        }

        PublicHolidays idElKabir = xticketRepository.getPublicHoliday(LocalDate.parse("2024-06-16"));
        if (idElKabir == null) {
            PublicHolidays newHoliday = new PublicHolidays();
            newHoliday.setCreatedAt(LocalDateTime.now());
            newHoliday.setCreatedBy(SYSTEM_USER);
            newHoliday.setHoliday(LocalDate.parse("2024-06-16"));
            xticketRepository.createPublicHoliday(newHoliday);
        }

        PublicHolidays idElMalud = xticketRepository.getPublicHoliday(LocalDate.parse("2024-09-16"));
        if (idElMalud == null) {
            PublicHolidays newHoliday = new PublicHolidays();
            newHoliday.setCreatedAt(LocalDateTime.now());
            newHoliday.setCreatedBy(SYSTEM_USER);
            newHoliday.setHoliday(LocalDate.parse("2024-09-16"));
            xticketRepository.createPublicHoliday(newHoliday);
        }

        PublicHolidays xmas = xticketRepository.getPublicHoliday(LocalDate.parse("2024-12-25"));
        if (xmas == null) {
            PublicHolidays newHoliday = new PublicHolidays();
            newHoliday.setCreatedAt(LocalDateTime.now());
            newHoliday.setCreatedBy(SYSTEM_USER);
            newHoliday.setHoliday(LocalDate.parse("2024-12-25"));
            xticketRepository.createPublicHoliday(newHoliday);
        }

        PublicHolidays boxingDay = xticketRepository.getPublicHoliday(LocalDate.parse("2024-12-26"));
        if (boxingDay == null) {
            PublicHolidays newHoliday = new PublicHolidays();
            newHoliday.setCreatedAt(LocalDateTime.now());
            newHoliday.setCreatedBy(SYSTEM_USER);
            newHoliday.setHoliday(LocalDate.parse("2024-12-26"));
            xticketRepository.createPublicHoliday(newHoliday);
        }

        //Create the entities
        Entities groupEntity = xticketRepository.getEntitiesUsingCode("NGX");
        if (groupEntity == null) {
            Entities newEntity = new Entities();
            newEntity.setCreatedAt(LocalDateTime.now());
            newEntity.setCreatedBy(SYSTEM_USER);
            newEntity.setEntityCode("NGX");
            newEntity.setEntityName("Nigerian Exchange Group");
            newEntity.setStatus(ENABLE_STATUS);
            xticketRepository.createEntities(newEntity);
        }

        Entities limitedEntity = xticketRepository.getEntitiesUsingCode("NGXL");
        if (limitedEntity == null) {
            Entities newEntity = new Entities();
            newEntity.setCreatedAt(LocalDateTime.now());
            newEntity.setCreatedBy(SYSTEM_USER);
            newEntity.setEntityCode("NGXL");
            newEntity.setEntityName("Nigerian Exchange Limited");
            newEntity.setStatus(ENABLE_STATUS);
            xticketRepository.createEntities(newEntity);
        }

        Entities regulationEntity = xticketRepository.getEntitiesUsingCode("NREG");
        if (regulationEntity == null) {
            Entities newEntity = new Entities();
            newEntity.setCreatedAt(LocalDateTime.now());
            newEntity.setCreatedBy(SYSTEM_USER);
            newEntity.setEntityCode("NREG");
            newEntity.setEntityName("Nigerian Exchange Regulation");
            newEntity.setStatus(ENABLE_STATUS);
            xticketRepository.createEntities(newEntity);
        }

        Entities realEstateEntity = xticketRepository.getEntitiesUsingCode("NREL");
        if (realEstateEntity == null) {
            Entities newEntity = new Entities();
            newEntity.setCreatedAt(LocalDateTime.now());
            newEntity.setCreatedBy(SYSTEM_USER);
            newEntity.setEntityCode("NREL");
            newEntity.setEntityName("Nigerian Exchange Real Estate");
            newEntity.setStatus(ENABLE_STATUS);
            xticketRepository.createEntities(newEntity);
        }
    }
}
