package com.ngxgroup.xticket.controller;

import com.google.gson.Gson;
import com.ngxgroup.xticket.constant.ResponseCodes;
import com.ngxgroup.xticket.model.Entities;
import com.ngxgroup.xticket.payload.ChartPayload;
import com.ngxgroup.xticket.payload.XTicketPayload;
import com.ngxgroup.xticket.service.XTicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author bokon
 */
@Controller
@RequestMapping("/report")
public class ReportController {

    @Autowired
    XTicketService xticketService;
    @Autowired
    Gson gson;
    private String alertMessage = "";
    private String alertMessageType = "";

    @GetMapping("/ticket/open")
    @Secured("ROLE_REPORT")
    public String openTicket(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchTicketType(false).getData());
        model.addAttribute("openTicketList", null);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportopenticket";
    }

    @PostMapping("/ticket/open/process")
    public String openTicket(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.fetchOpenTicket(requestPayload);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchTicketType(false).getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportopenticket";
    }

    @GetMapping("/ticket/closed")
    @Secured("ROLE_REPORT")
    public String closedTicket(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchTicketType(false).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportclosedticket";
    }

    @PostMapping("/ticket/closed/process")
    public String closedTicket(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.fetchClosedTicket(requestPayload);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchTicketType(false).getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportclosedticket";
    }

    @GetMapping("/ticket/reopened")
    @Secured("ROLE_REPORT")
    public String reopened(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportreopenedtickets";
    }

    @PostMapping("/ticket/reopened/process")
    public String reopenedTicket(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.fetchReopenedTicket(requestPayload);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportreopenedtickets";
    }

    @GetMapping("/ticket/reassigned")
    @Secured("ROLE_REPORT")
    public String reassigned(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchTicketType(false).getData());
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportreassignedtickets";
    }

    @PostMapping("/ticket/reassigned/process")
    public String reassignedTicket(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.fetchReassignedTicket(requestPayload);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchTicketType(false).getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportreassignedtickets";
    }

    @GetMapping("/ticket/by-agents")
    @Secured("ROLE_REPORT")
    public String byAgents(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportticketsbyagents";
    }

    @PostMapping("/ticket/by-agents/process")
    public String byAgents(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.fetchTicketClosedByAgent(requestPayload);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportticketsbyagents";
    }

    @GetMapping("/ticket/sla")
    @Secured("ROLE_REPORT")
    public String sla(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchTicketType(false).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportticketsbysla";
    }

    @PostMapping("/ticket/sla/process")
    public String sla(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.fetchTicketByWithinSla(requestPayload);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchTicketType(false).getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportticketsbysla";
    }

    @GetMapping("/ticket/sla/violated")
    @Secured("ROLE_REPORT")
    public String violatedSla(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchTicketType(false).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportticketsbyviolatedsla";
    }

    @PostMapping("/ticket/sla/violated/process")
    public String violatedSla(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.fetchTicketByViolatedSla(requestPayload);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchTicketType(false).getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportticketsbyviolatedsla";
    }

    @GetMapping("/ticket/agents")
    @Secured("ROLE_REPORT")
    public String agents(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchTicketType(false).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportticketagents";
    }

    @PostMapping("/ticket/agents/process")
    public String agents(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.fetchTicketAgent(requestPayload);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchTicketType(false).getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportticketagents";
    }

    @GetMapping("/app-user")
    @Secured("ROLE_REPORT")
    public String appUsers(Model model, HttpServletRequest httRequest, HttpServletResponse httResponse, Principal principal) {
        XTicketPayload response = xticketService.fetchAllAppUsers();
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportappuser";
    }

    @GetMapping("/ticket/service-unit")
    @Secured("ROLE_MANAGEMENT_REPORT")
    public String serviceUnit(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchTicketType(false).getData());
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportticketsbyserviceunit";
    }

    @PostMapping("/ticket/service-unit/process")
    public String serviceUnit(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.fetchTicketByServiceUnit(requestPayload);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchTicketType(false).getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportticketsbyserviceunit";
    }

    @GetMapping("/ticket/service-unit-entity")
    @Secured("ROLE_MANAGEMENT_REPORT")
    public String serviceUnitToEntity(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("entityList", xticketService.fetchEntity().getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportticketsserviceunittoentity";
    }

    @PostMapping("/ticket/service-unit-entity/process")
    public String serviceUnitToEntity(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.fetchTicketByServiceUnitToEntity(requestPayload);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("entityList", xticketService.fetchEntity().getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("serviceEffectivenessChartData", generateServiceEffectivenessForServiceUnit(response, requestPayload));
        model.addAttribute("serviceHourChartData", generateServiceHoursForServiceUnit(response, requestPayload));
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportticketsserviceunittoentity";
    }

    @GetMapping("/ticket/department-entity")
    @Secured("ROLE_MANAGEMENT_REPORT")
    public String departmentToEntity(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup());
        model.addAttribute("departmentList", xticketService.fetchDepartment().getData());
        model.addAttribute("entityList", xticketService.fetchEntity().getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportticketsdepartmenttoentity";
    }

    @PostMapping("/ticket/department-entity/process")
    public String departmentToEntity(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.fetchTicketByDepartmentToEntity(requestPayload);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("departmentList", xticketService.fetchDepartment().getData());
        model.addAttribute("entityList", xticketService.fetchEntity().getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("serviceEffectivenessChartData", generateServiceEffectivenessForDepartment(response, requestPayload));
        model.addAttribute("serviceHourChartData", generateServiceHoursForDepartment(response, requestPayload));
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportticketsdepartmenttoentity";
    }

    @GetMapping("/ticket/service-rating")
    @Secured("ROLE_MANAGEMENT_REPORT")
    public String serviceRating(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("entityList", xticketService.fetchEntity().getData());
        model.addAttribute("departmentList", xticketService.fetchDepartment().getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportticketsservicerating";
    }

    @PostMapping("/ticket/service-rating/process")
    public String serviceRating(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.fetchTicketByServiceRating(requestPayload);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("entityList", xticketService.fetchEntity().getData());
        model.addAttribute("departmentList", xticketService.fetchDepartment().getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("serviceRatingChartData", generateServiceRatingChart(response.getData()));
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportticketsservicerating";
    }

    @GetMapping("/ticket/entity")
    @Secured("ROLE_MANAGEMENT_REPORT")
    public String entityToEntity(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("entityList", xticketService.fetchEntity().getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportticketsbyentity";
    }

    @PostMapping("/ticket/entity/process")
    public String entityToEntity(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.fetchTicketByEntityToEntity(requestPayload);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("entityList", xticketService.fetchEntity().getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("serviceEffectivenessChartData", generateServiceEffectivenessForServicePerformance(response, requestPayload));
        model.addAttribute("serviceHourChartData", generateServiceHoursForServicePerformance(response, requestPayload));
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportticketsbyentity";
    }

    @GetMapping("/ticket/entity-performance")
    @Secured("ROLE_MANAGEMENT_REPORT")
    public String entityPerformance(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("entityList", xticketService.fetchEntity().getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportticketsbyentityperformance";
    }

    @PostMapping("/ticket/entity-performance/process")
    public String entityPerformance(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        requestPayload.setServiceUnitCode("");
        requestPayload.setTicketGroupCode("");
        requestPayload.setTicketTypeCode("");
        requestPayload.setSource("");
        requestPayload.setEmail("");
        XTicketPayload response = xticketService.fetchClosedTicket(requestPayload);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("dataList", response.getData());
        model.addAttribute("serviceEffectivenessChartData", generateServiceEffectivenessForServicePerformance(response, requestPayload));
        model.addAttribute("serviceHourChartData", generateServiceHoursForServicePerformance(response, requestPayload));
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportticketsbyentityperformance";
    }

    @GetMapping("/ticket/automation")
    @Secured("ROLE_MANAGEMENT_REPORT")
    public String automatedTicket(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchTicketType(true).getData());
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportticketsbyautomation";
    }

    @PostMapping("/ticket/automation/process")
    public String automatedTicket(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.fetchTicketByAutomation(requestPayload);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("serviceUnitList", xticketService.fetchServiceUnit().getData());
        model.addAttribute("ticketTypeList", xticketService.fetchTicketType(true).getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportticketsbyautomation";
    }

    @GetMapping("/audit-log")
    @Secured("ROLE_MANAGEMENT_REPORT")
    public String auditLog(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("entityList", xticketService.fetchEntity().getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "auditlog";
    }

    @PostMapping("/audit-log/process")
    public String auditLog(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.fetchAuditLog(requestPayload);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("dataList", response.getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "auditlog";
    }

    private String generateServiceRatingChart(List<XTicketPayload> ticketList) {
        List<ChartPayload> data = new ArrayList<>();
        if (ticketList != null) {
            for (XTicketPayload t : ticketList) {
                ChartPayload chart = new ChartPayload();
                long[] series = new long[]{t.getOneStar(), t.getTwoStar(), t.getThreeStar(), t.getFourStar(), t.getFiveStar()};
                chart.setName(t.getServiceUnitName().equalsIgnoreCase("") ? t.getDepartmentName() : t.getServiceUnitName());
                chart.setType("bar");
                chart.setData(series);
                data.add(chart);
            }
        }
        return gson.toJson(data);
    }

    private String generateServiceEffectivenessForServicePerformance(XTicketPayload responseData, XTicketPayload requestPayload) {
        XTicketPayload response = xticketService.fetchServiceEffectivenessByEntity(responseData, requestPayload);
        List<ChartPayload> data = new ArrayList<>();
        if (response.getData() != null) {
            for (XTicketPayload t : response.getData()) {
                ChartPayload chart = new ChartPayload();
                chart.setName(t.getEntityName());
                chart.setType("bar");
                chart.setData(t.getSeries());
                data.add(chart);
            }
        }
        return gson.toJson(data);
    }

    private String generateServiceHoursForServicePerformance(XTicketPayload responseData, XTicketPayload requestPayload) {
        XTicketPayload response = xticketService.fetchServiceHoursByEntity(responseData, requestPayload);
        List<XTicketPayload> data = new ArrayList<>();
        if (response.getData() != null) {
            for (XTicketPayload t : response.getData()) {
                XTicketPayload chart = new XTicketPayload();
                chart.setValue(t.getValue());
                chart.setName(t.getName());
                data.add(chart);
            }
        }
        return gson.toJson(data);
    }

    private String generateServiceEffectivenessForDepartment(XTicketPayload responseData, XTicketPayload requestPayload) {
        XTicketPayload response = xticketService.fetchServiceEffectivenessByDepartment(responseData, requestPayload);
        List<ChartPayload> data = new ArrayList<>();
        if (response.getData() != null) {
            for (XTicketPayload t : response.getData()) {
                ChartPayload chart = new ChartPayload();
                chart.setName(t.getEntityName());
                chart.setType("bar");
                chart.setData(t.getSeries());
                data.add(chart);
            }
        }
        return gson.toJson(data);
    }

    private String generateServiceHoursForDepartment(XTicketPayload responseData, XTicketPayload requestPayload) {
        XTicketPayload response = xticketService.fetchServiceHoursByDepartment(responseData, requestPayload);
        List<XTicketPayload> data = new ArrayList<>();
        if (response.getData() != null) {
            for (XTicketPayload t : response.getData()) {
                XTicketPayload chart = new XTicketPayload();
                chart.setValue(t.getValue());
                chart.setName(t.getName());
                data.add(chart);
            }
        }
        return gson.toJson(data);
    }

    private String generateServiceEffectivenessForServiceUnit(XTicketPayload responseData, XTicketPayload requestPayload) {
        XTicketPayload response = xticketService.fetchServiceEffectivenessByServiceUnit(responseData, requestPayload);
        List<ChartPayload> data = new ArrayList<>();
        if (response.getData() != null) {
            for (XTicketPayload t : response.getData()) {
                ChartPayload chart = new ChartPayload();
                chart.setName(t.getEntityName());
                chart.setType("bar");
                chart.setData(t.getSeries());
                data.add(chart);
            }
        }
        return gson.toJson(data);
    }

    private String generateServiceHoursForServiceUnit(XTicketPayload responseData, XTicketPayload requestPayload) {
        XTicketPayload response = xticketService.fetchServiceHoursByServiceUnit(responseData, requestPayload);
        List<XTicketPayload> data = new ArrayList<>();
        if (response.getData() != null) {
            for (XTicketPayload t : response.getData()) {
                XTicketPayload chart = new XTicketPayload();
                chart.setValue(t.getValue());
                chart.setName(t.getName());
                data.add(chart);
            }
        }
        return gson.toJson(data);
    }

    private void resetAlertMessage() {
        alertMessage = "";
        alertMessageType = "";
    }
}
