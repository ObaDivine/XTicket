package com.ngxgroup.xticket.controller;

import com.ngxgroup.xticket.constant.ResponseCodes;
import com.ngxgroup.xticket.payload.XTicketPayload;
import com.ngxgroup.xticket.service.XTicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
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
    private String alertMessage = "";
    private String alertMessageType = "";

    @GetMapping("/ticket/open")
    public String openTicket(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("openTicketList", null);
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportopenticket";
    }

    @PostMapping("/ticket/open/process")
    public String openTicket(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        XTicketPayload response = xticketService.fetchOpenTicket(requestPayload);
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportopenticket";
    }

    @GetMapping("/ticket/closed")
    public String closedTicket(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportclosedticket";
    }

    @PostMapping("/ticket/closed/process")
    public String closedTicket(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        XTicketPayload response = xticketService.fetchClosedTicket(requestPayload);
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportclosedticket";
    }

    @GetMapping("/ticket/reopened")
    public String reopened(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportreopenedtickets";
    }

    @PostMapping("/ticket/reopened/process")
    public String reopenedTicket(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        XTicketPayload response = xticketService.fetchReopenedTicket(requestPayload);
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportreopenedtickets";
    }

    @GetMapping("/ticket/reassigned")
    public String reassigned(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportreassignedtickets";
    }

    @PostMapping("/ticket/reassigned/process")
    public String reassignedTicket(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        XTicketPayload response = xticketService.fetchReassignedTicket(requestPayload);
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportreassignedtickets";
    }

    @GetMapping("/ticket/by-agents")
    public String byAgents(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup());
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportticketsbyagents";
    }

    @PostMapping("/ticket/by-agents/process")
    public String byAgents(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        XTicketPayload response = xticketService.fetchTicketClosedByAgent(requestPayload);
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportticketsbyagents";
    }

    @GetMapping("/ticket/sla")
    public String sla(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportticketsbysla";
    }

    @PostMapping("/ticket/sla/process")
    public String sla(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        XTicketPayload response = xticketService.fetchTicketByWithinSla(requestPayload);
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportticketsbysla";
    }

    @GetMapping("/ticket/sla/violated")
    public String violatedSla(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportticketsbyviolatedsla";
    }

    @PostMapping("/ticket/sla/violated/process")
    public String violatedSla(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        XTicketPayload response = xticketService.fetchTicketByViolatedSla(requestPayload);
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportticketsbyviolatedsla";
    }

    @GetMapping("/ticket/agents")
    public String agents(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportticketagents";
    }

    @PostMapping("/ticket/agents/process")
    public String agents(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        XTicketPayload response = xticketService.fetchTicketAgent(requestPayload);
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportticketagents";
    }

    @GetMapping("/app-user")
    public String appUsers(Model model, HttpServletRequest httRequest, HttpServletResponse httResponse, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        XTicketPayload response = xticketService.fetchAllAppUsers();
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportappuser";
    }

    @GetMapping("/ticket/service-unit")
    public String serviceUnit(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup());
        model.addAttribute("serviceUnitList",xticketService.fetchServiceUnit());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reportticketsbyserviceunit";
    }

    @PostMapping("/ticket/service-unit/process")
    public String serviceUnit(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        XTicketPayload response = xticketService.fetchTicketByServiceUnit(requestPayload);
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("userList", xticketService.fetchTicketAgent().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        resetAlertMessage();
        return "reportticketsbyserviceunit";
    }

    private void resetAlertMessage() {
        alertMessage = "";
        alertMessageType = "";
    }
}
