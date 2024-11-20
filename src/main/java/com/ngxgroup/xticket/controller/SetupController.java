package com.ngxgroup.xticket.controller;

import com.ngxgroup.xticket.constant.ResponseCodes;
import com.ngxgroup.xticket.payload.XTicketPayload;
import com.ngxgroup.xticket.service.XTicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author bokon
 */
@Controller
@RequestMapping("/setup")
public class SetupController {

    @Autowired
    XTicketService xticketService;
    @Autowired
    MessageSource messageSource;
    @Value("${xticket.default.departmentcode}")
    private String defaultDepartmentCode;
    private String alertMessage = "";
    private String alertMessageType = "";

    @GetMapping("/ticket/group")
    @Secured("ROLE_ADD_TICKET_GROUP")
    public String ticketGroup(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("recordCount", xticketService.fetchTicketGroup().getData().size());
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "ticketgroup";
    }

    @PostMapping("/ticket/group/create")
    public String ticketGroup(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.createTicketGroup(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/setup/ticket/group";
        }
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "ticketgroup";
    }

    @GetMapping("/ticket/group/edit")
    @Secured("ROLE_UPDATE_TICKET_GROUP")
    public String ticketGroup(@RequestParam("seid") String id, Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchTicketGroup(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "error";
            return "redirect:/setup/ticket/group/list";
        }
        model.addAttribute("ticketPayload", response);
        model.addAttribute("recordCount", xticketService.fetchTicketGroup().getData().size());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "ticketgroup";
    }

    @GetMapping("/ticket/group/list")
    @Secured("ROLE_LIST_TICKET_GROUP")
    public String ticketGroupList(Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchTicketGroup();
        model.addAttribute("dataList", response.getData());
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "ticketgrouplist";
    }

    @GetMapping("/ticket/group/delete")
    @Secured("ROLE_DELETE_TICKET_GROUP")
    public String deleteTicketGroup(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.deleteTicketGroup(seid, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/setup/ticket/group/list";
    }

    @GetMapping("/ticket/type")
    @Secured("ROLE_ADD_TICKET_TYPE")
    public String ticketType(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("serviceUnit", xticketService.fetchServiceUnit().getData());
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup().getData());
        model.addAttribute("ticketSla", xticketService.fetchTicketSla().getData());
        model.addAttribute("recordCount", xticketService.fetchTicketType(true).getData().size());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "tickettype";
    }

    @PostMapping("/ticket/type/create")
    public String ticketType(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.createTicketType(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/setup/ticket/type";
        }
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("serviceUnit", xticketService.fetchServiceUnit().getData());
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup().getData());
        model.addAttribute("ticketSla", xticketService.fetchTicketSla().getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "tickettype";
    }

    @GetMapping("/ticket/type/edit")
    @Secured("ROLE_UPDATE_TICKET_TYPE")
    public String ticketType(@RequestParam("seid") String id, Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchTicketType(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "error";
            return "redirect:/setup/ticket/type/list";
        }
        model.addAttribute("ticketPayload", response);
        model.addAttribute("recordCount", xticketService.fetchTicketType(true).getData().size());
        model.addAttribute("serviceUnit", xticketService.fetchServiceUnit().getData());
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup().getData());
        model.addAttribute("ticketSla", xticketService.fetchTicketSla().getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "tickettype";
    }

    @GetMapping("/ticket/type/list")
    @Secured("ROLE_LIST_TICKET_TYPE")
    public String ticketTypeList(Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchTicketType(true);
        model.addAttribute("dataList", response.getData());
        model.addAttribute("serviceUnit", xticketService.fetchServiceUnit().getData());
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup().getData());
        model.addAttribute("ticketSla", xticketService.fetchTicketSla().getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "tickettypelist";
    }

    @GetMapping("/ticket/type/delete")
    @Secured("ROLE_DELETE_TICKET_TYPE")
    public String deleteTicketType(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.deleteTicketType(seid, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/setup/ticket/type/list";
    }

    @GetMapping("/ticket/agent")
    @Secured("ROLE_ADD_TICKET_AGENT")
    public String ticketAgent(@RequestParam("seid") String id, Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        XTicketPayload requestPayload = new XTicketPayload();
        XTicketPayload responsePayload = xticketService.fetchProfile(id);
        requestPayload.setEmail(responsePayload.getEmail());
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("userList", xticketService.fetchInternalAppUsers());
        model.addAttribute("roleList", null);
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "ticketagent";
    }

    @PostMapping("/ticket/agent/permission")
    public String ticketAgentPermission(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.fetchAgentTicketTypes(requestPayload.getEmail());
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("userList", xticketService.fetchInternalAppUsers());
        model.addAttribute("roleList", response.getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "ticketagent";
    }

    @PostMapping("/ticket/agent/create")
    public String ticketAgent(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.createTicketAgent(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/setup/ticket/agent?seid=";
        }
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("userList", xticketService.fetchInternalAppUsers());
        model.addAttribute("roleList", xticketService.fetchAgentTicketTypes(principal.getName()).getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "ticketagent";
    }

    @GetMapping("/ticket/agent/list")
    @Secured("ROLE_LIST_TICKET_AGENT")
    public String ticketAgentList(Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchTicketAgent();
        model.addAttribute("dataList", response.getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "ticketagentlist";
    }

    @GetMapping("/ticket/sla")
    @Secured("ROLE_ADD_TICKET_SLA")
    public String ticketSla(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("recordCount", xticketService.fetchTicketSla().getData().size());
        model.addAttribute("ticketSla", xticketService.fetchTicketSla());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "ticketsla";
    }

    @PostMapping("/ticket/sla/create")
    public String ticketSla(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.createTicketSla(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/setup/ticket/sla";
        }
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "ticketsla";
    }

    @GetMapping("/ticket/sla/edit")
    @Secured("ROLE_UPDATE_TICKET_SLA")
    public String ticketSla(@RequestParam("seid") String id, Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchTicketSla(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "error";
            return "redirect:/setup/ticket/sla/list";
        }
        model.addAttribute("ticketPayload", response);
        model.addAttribute("recordCount", xticketService.fetchTicketSla().getData().size());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "ticketsla";
    }

    @GetMapping("/ticket/sla/list")
    @Secured("ROLE_LIST_TICKET_SLA")
    public String ticketSlaList(Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchTicketSla();
        model.addAttribute("dataList", response.getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "ticketslalist";
    }

    @GetMapping("/ticket/sla/delete")
    @Secured("ROLE_DELETE_TICKET_SLA")
    public String deleteTicketSla(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.deleteTicketSla(seid, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/setup/ticket/sla/list";
    }

    @GetMapping("/service-unit")
    @Secured("ROLE_ADD_SERVICE_UNIT")
    public String serviceUnit(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("recordCount", xticketService.fetchServiceUnit().getData().size());
        model.addAttribute("departmentList", xticketService.fetchDepartment().getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "serviceunit";
    }

    @PostMapping("/service-unit/create")
    public String serviceUnit(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.createServiceUnit(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/setup/service-unit";
        }
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("departmentList", xticketService.fetchDepartment().getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "serviceunit";
    }

    @GetMapping("/service-unit/edit")
    @Secured("ROLE_UPDATE_SERVICE_UNIT")
    public String serviceUnit(@RequestParam("seid") String id, Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchServiceUnit(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "error";
            return "redirect:/setup/service-unit/list";
        }
        model.addAttribute("ticketPayload", response);
        model.addAttribute("recordCount", xticketService.fetchServiceUnit().getData().size());
        model.addAttribute("departmentList", xticketService.fetchDepartment().getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "serviceunit";
    }

    @GetMapping("/service-unit/list")
    @Secured("ROLE_LIST_SERVICE_UNIT")
    public String serviceUnit(Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchServiceUnit();
        model.addAttribute("dataList", response.getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "serviceunitlist";
    }

    @GetMapping("/service-unit/delete")
    @Secured("ROLE_DELETE_SERVICE_UNIT")
    public String deleteServiceUnit(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.deleteServiceUnit(seid, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/setup/service-unit/list";
    }

    @GetMapping("/ticket/status")
    @Secured("ROLE_ADD_TICKET_STATUS")
    public String ticketStatus(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("recordCount", xticketService.fetchTicketStatus().getData().size());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "ticketstatus";
    }

    @PostMapping("/ticket/status/create")
    public String ticketStatus(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.createTicketStatus(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/setup/ticket/status";
        }
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "ticketstatus";
    }

    @GetMapping("/ticket/status/edit")
    @Secured("ROLE_UPDATE_TICKET_STATUS")
    public String ticketStatus(@RequestParam("seid") String id, Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchTicketStatus(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "error";
            return "redirect:/setup/ticket/status/list";
        }
        model.addAttribute("ticketPayload", response);
        model.addAttribute("recordCount", xticketService.fetchTicketStatus().getData().size());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "ticketstatus";
    }

    @GetMapping("/ticket/status/list")
    @Secured("ROLE_LIST_TICKET_STATUS")
    public String ticketStatus(Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchTicketStatus();
        model.addAttribute("dataList", response.getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "ticketstatuslist";
    }

    @GetMapping("/ticket/status/delete")
    @Secured("ROLE_DELETE_TICKET_STATUS")
    public String deleteTicketStatus(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.deleteTicketStatus(seid, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/setup/ticket/status/list";
    }

    @GetMapping("/entity")
    @Secured("ROLE_ADD_ENTITY")
    public String entity(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("recordCount", xticketService.fetchEntity().getData().size());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "entity";
    }

    @PostMapping("/entity/create")
    public String entity(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.createEntity(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/setup/entity";
        }
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "entity";
    }

    @GetMapping("/entity/edit")
    @Secured("ROLE_UPDATE_ENTITY")
    public String entity(@RequestParam("seid") String id, Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchEntity(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "error";
            return "redirect:/setup/entity/list";
        }
        model.addAttribute("ticketPayload", response);
        model.addAttribute("recordCount", xticketService.fetchEntity().getData().size());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "entity";
    }

    @GetMapping("/entity/list")
    @Secured("ROLE_LIST_ENTITY")
    public String entity(Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchEntity();
        model.addAttribute("dataList", response.getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "entitylist";
    }

    @GetMapping("/entity/delete")
    @Secured("ROLE_DELETE_ENTITY")
    public String deleteEntity(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.deleteEntity(seid, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/setup/entity/list";
    }

    @GetMapping("/department")
    @Secured("ROLE_ADD_DEPARTMENT")
    public String department(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("recordCount", xticketService.fetchDepartment().getData().size());
        model.addAttribute("entityList", xticketService.fetchEntity().getData());
        model.addAttribute("defaultDepartmentCode", defaultDepartmentCode);
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "department";
    }

    @PostMapping("/department/create")
    public String department(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.createDepartment(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/setup/department";
        }
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("entityList", xticketService.fetchEntity().getData());
        model.addAttribute("defaultDepartmentCode", defaultDepartmentCode);
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "department";
    }

    @GetMapping("/department/edit")
    @Secured("ROLE_UPDATE_DEPARTMENT")
    public String department(@RequestParam("seid") String id, Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchDepartment(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "error";
            return "redirect:/setup/department/list";
        }
        model.addAttribute("ticketPayload", response);
        model.addAttribute("recordCount", xticketService.fetchDepartment().getData().size());
        model.addAttribute("entityList", xticketService.fetchEntity().getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "department";
    }

    @GetMapping("/department/list")
    @Secured("ROLE_LIST_DEPARTMENT")
    public String department(Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchDepartment();
        model.addAttribute("dataList", response.getData());
        model.addAttribute("entityList", xticketService.fetchEntity().getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "departmentlist";
    }

    @GetMapping("/department/delete")
    @Secured("ROLE_DELETE_DEPARTMENT")
    public String deleteDepartment(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.deleteDepartment(seid, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/setup/department/list";
    }

    @GetMapping("/ticket/automation")
    @Secured("ROLE_ADD_AUTOMATED_TICKET")
    public String automatedTicket(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("recordCount", xticketService.fetchAutomatedTicket().getData().size());
        model.addAttribute("userList", xticketService.fetchAppUsers());
        model.addAttribute("ticketAgentList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchAutomatedTicketType().getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "automatedticket";
    }

    @PostMapping("/ticket/automation/create")
    public String automatedTicket(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.createAutomatedTicket(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/setup/ticket/automation";
        }
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("recordCount", xticketService.fetchAutomatedTicket().getData().size());
        model.addAttribute("userList", xticketService.fetchAppUsers());
        model.addAttribute("ticketAgentList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchAutomatedTicketType().getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "automatedticket";
    }

    @GetMapping("/ticket/automation/edit")
    @Secured("ROLE_UPDATE_AUTOMATED_TICKET")
    public String automatedTicket(@RequestParam("seid") String id, Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchAutomatedTicket(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "error";
            return "redirect:/setup/ticket/automation/list";
        }
        model.addAttribute("ticketPayload", response);
        model.addAttribute("recordCount", xticketService.fetchAutomatedTicket().getData().size());
        model.addAttribute("userList", xticketService.fetchAppUsers());
        model.addAttribute("ticketAgentList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchAutomatedTicketType().getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "automatedticket";
    }

    @GetMapping("/ticket/automation/list")
    @Secured("ROLE_LIST_AUTOMATED_TICKET")
    public String automatedTicket(Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchAutomatedTicket();
        model.addAttribute("dataList", response.getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "automatedticketlist";
    }

    @GetMapping("/ticket/automation/delete")
    @Secured("ROLE_DELETE_AUTOMATED_TICKET")
    public String deleteAutomatedTicket(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.deleteAutomatedTicket(seid, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/setup/ticket/automation/list";
    }

    private void resetAlertMessage() {
        alertMessage = "";
        alertMessageType = "";
    }
}
