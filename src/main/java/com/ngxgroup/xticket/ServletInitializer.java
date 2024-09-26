package com.ngxgroup.xticket;

import com.ngxgroup.xticket.model.AppRoles;
import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.model.Department;
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
    @Value("${xticket.default.entitycode}")
    private String defaultEntityCode;
    @Value("${xticket.default.entityname}")
    private String defaultEntityName;
    @Value("${xticket.default.departmentcode}")
    private String defaultDepartmentCode;
    @Value("${xticket.default.departmentname}")
    private String defaultDepartmentName;
    @Value("${tipro.holiday}")
    private String[] publicHolidays;
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
        appRoles.put("ADD_DEPARTMENT", "Add Department");
        appRoles.put("DELETE_DEPARTMENT", "Delete Department");
        appRoles.put("UPDATE_DEPARTMENT", "Update Department");
        appRoles.put("LIST_DEPARTMENT", "List Department");
        appRoles.put("KNOWLEDGE_BASE", "View Knowledge Base Documentation");
        appRoles.put("KNOWLEDGE_BASE_SETUP", "Setup Knowledge Base Documentation");
        appRoles.put("RAISE_TICKET", "Raise Tickets");
        appRoles.put("REASSIGN_TICKET", "Reassign Tickets");
        appRoles.put("REPORT", "View Reports accross entities");
        appRoles.put("MANAGEMENT_REPORT", "View Management Reports");
        appRoles.put("TICKET_AGENT", "Set user as a ticket agent");
        appRoles.put("ADD_AUTOMATED_TICKET", "Add Automated Tickets");
        appRoles.put("DELETE_AUTOMATED_TICKET", "Delete Automated Tickets");
        appRoles.put("UPDATE_AUTOMATED_TICKET", "Update Automated Tickets");
        appRoles.put("LIST_AUTOMATED_TICKET", "List Automated Tickets");

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
                newGroupRole.setAppRole(addTicketStatus);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles updateTicketStatus = xticketRepository.getRoleUsingRoleName("UPDATE_TICKET_STATUS");
            if (updateTicketStatus != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(updateTicketStatus);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles deleteTicketStatus = xticketRepository.getRoleUsingRoleName("DELETE_TICKET_STATUS");
            if (deleteTicketStatus != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(deleteTicketStatus);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles listTicketStatus = xticketRepository.getRoleUsingRoleName("LIST_TICKET_STATUS");
            if (listTicketStatus != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(listTicketStatus);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles addServiceUnit = xticketRepository.getRoleUsingRoleName("ADD_SERVICE_UNIT");
            if (addServiceUnit != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(addServiceUnit);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles updateServiceUnit = xticketRepository.getRoleUsingRoleName("UPDATE_SERVICE_UNIT");
            if (updateServiceUnit != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(updateServiceUnit);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles deleteServiceUnit = xticketRepository.getRoleUsingRoleName("DELETE_SERVICE_UNIT");
            if (deleteServiceUnit != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(deleteServiceUnit);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles listServiceUnit = xticketRepository.getRoleUsingRoleName("LIST_SERVICE_UNIT");
            if (listServiceUnit != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(listServiceUnit);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles addEntity = xticketRepository.getRoleUsingRoleName("ADD_ENTITY");
            if (addEntity != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(addEntity);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles updateEntity = xticketRepository.getRoleUsingRoleName("UPDATE_ENTITY");
            if (updateEntity != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(updateEntity);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles deleteEntity = xticketRepository.getRoleUsingRoleName("DELETE_ENTITY");
            if (deleteEntity != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(deleteEntity);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles listEntity = xticketRepository.getRoleUsingRoleName("LIST_ENTITY");
            if (listEntity != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(listEntity);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles addDepartment = xticketRepository.getRoleUsingRoleName("ADD_DEPARTMENT");
            if (addDepartment != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(addDepartment);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles updateDepartment = xticketRepository.getRoleUsingRoleName("UPDATE_DEPARTMENT");
            if (updateDepartment != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(updateDepartment);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles deleteDepartment = xticketRepository.getRoleUsingRoleName("DELETE_DEPARTMENT");
            if (deleteDepartment != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(deleteDepartment);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles listDepartment = xticketRepository.getRoleUsingRoleName("LIST_DEPARTMENT");
            if (listDepartment != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(listDepartment);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles addAutomatedTicket = xticketRepository.getRoleUsingRoleName("ADD_AUTOMATED_TICKET");
            if (addAutomatedTicket != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(addAutomatedTicket);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles updateAutomatedTicket = xticketRepository.getRoleUsingRoleName("UPDATE_AUTOMATED_TICKET");
            if (updateAutomatedTicket != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(updateAutomatedTicket);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles deleteAutomatedTicket = xticketRepository.getRoleUsingRoleName("DELETE_AUTOMATED_TICKET");
            if (deleteAutomatedTicket != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(deleteAutomatedTicket);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles listAutomatedTicket = xticketRepository.getRoleUsingRoleName("LIST_AUTOMATED_TICKET");
            if (listAutomatedTicket != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(listAutomatedTicket);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(saGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles reassignTicket = xticketRepository.getRoleUsingRoleName("REASSIGN_TICKET");
            if (reassignTicket != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(reassignTicket);
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

        //Create the agent role group
        RoleGroups agentGroup = xticketRepository.getRoleGroupUsingGroupName("AGENT");
        if (agentGroup == null) {
            RoleGroups newGroup = new RoleGroups();
            newGroup.setCreatedAt(LocalDateTime.now());
            newGroup.setGroupName("AGENT");
            agentGroup = xticketRepository.createRoleGroup(newGroup);
        }

        //Check the group roles
        List<GroupRoles> agentGroupRoles = xticketRepository.getGroupRolesUsingRoleGroup(agentGroup);
        if (agentGroupRoles == null) {
            AppRoles dashboard = xticketRepository.getRoleUsingRoleName("DASHBOARD");
            if (dashboard != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(dashboard);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(agentGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles knowledge = xticketRepository.getRoleUsingRoleName("KNOWLEDGE_BASE");
            if (knowledge != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(knowledge);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(agentGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles knowledgeBase = xticketRepository.getRoleUsingRoleName("KNOWLEDGE_BASE_SETUP");
            if (knowledgeBase != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(knowledgeBase);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(agentGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles ticket = xticketRepository.getRoleUsingRoleName("RAISE_TICKET");
            if (ticket != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(ticket);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(agentGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles ticketAgent = xticketRepository.getRoleUsingRoleName("TICKET_AGENT");
            if (ticketAgent != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(ticketAgent);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(agentGroup);
                xticketRepository.createGroupRoles(newGroupRole);
            }

            AppRoles report = xticketRepository.getRoleUsingRoleName("REPORT");
            if (report != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(report);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(agentGroup);
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
        if (publicHolidays.length != 0) {
            for (String hols : publicHolidays) {
                PublicHolidays holiday = xticketRepository.getPublicHoliday(LocalDate.parse(hols));
                if (holiday == null) {
                    PublicHolidays newHoliday = new PublicHolidays();
                    newHoliday.setCreatedAt(LocalDateTime.now());
                    newHoliday.setCreatedBy(SYSTEM_USER);
                    newHoliday.setHoliday(LocalDate.parse(hols));
                    xticketRepository.createPublicHoliday(newHoliday);
                }
            }
        }
        //Create the entities
        Entities defaultEntity = xticketRepository.getEntitiesUsingCode(defaultEntityCode);
        if (defaultEntity == null) {
            Entities newEntity = new Entities();
            newEntity.setCreatedAt(LocalDateTime.now());
            newEntity.setCreatedBy(SYSTEM_USER);
            newEntity.setEntityCode(defaultEntityCode);
            newEntity.setEntityName(defaultEntityName);
            newEntity.setStatus(ENABLE_STATUS);
            xticketRepository.createEntities(newEntity);
        }

        //Create the department
        Department department = xticketRepository.getDepartmentUsingCode(defaultDepartmentCode);
        if (department == null) {
            Department newDepartment = new Department();
            newDepartment.setCreatedAt(LocalDateTime.now());
            newDepartment.setCreatedBy(SYSTEM_USER);
            newDepartment.setDepartmentCode(defaultDepartmentCode);
            newDepartment.setDepartmentName(defaultDepartmentName);
            newDepartment.setEntity(defaultEntity);
            newDepartment.setStatus(ENABLE_STATUS);
            xticketRepository.createDepartment(newDepartment);
        }
    }
}
