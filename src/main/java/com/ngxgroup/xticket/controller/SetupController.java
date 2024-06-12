package com.ngxgroup.xticket.controller;

import com.ngxgroup.xticket.constant.ResponseCodes;
import com.ngxgroup.xticket.payload.XTicketPayload;
import com.ngxgroup.xticket.service.XTicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
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
    private String alertMessage = "";
    private String alertMessageType = "";

    @GetMapping("/ticket/group")
    public String ticketGroup(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("recordCount", xticketService.fetchTicketGroup().getData().size());
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup());
        model.addAttribute("profilePayload", profileDetails);
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
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "ticketgroup";
    }

    @GetMapping("/ticket/group/edit")
    public String ticketGroup(@RequestParam("seid") String id, Model model, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        XTicketPayload response = xticketService.fetchTicketGroup(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "error";
            return "redirect:/setup/ticket/group/list";
        }
        model.addAttribute("ticketPayload", response);
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("recordCount", xticketService.fetchTicketGroup().getData().size());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "ticketgroup";
    }

    @GetMapping("/ticket/group/list")
    public String ticketGroupList(Model model, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        XTicketPayload response = xticketService.fetchTicketGroup();
        model.addAttribute("dataList", response.getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", messageSource.getMessage("appMessages.ticket.record", new Object[]{response.getData().size()}, Locale.ENGLISH));
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "ticketgrouplist";
    }

    @GetMapping("/ticket/group/delete")
    public String deleteTicketGroup(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.deleteTicketGroup(seid, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = "success";
        return "redirect:/setup/ticket/group/list";
    }

    @GetMapping("/ticket/type")
    public String ticketType(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup().getData());
        model.addAttribute("ticketSla", xticketService.fetchTicketSla().getData());
        model.addAttribute("recordCount", xticketService.fetchTicketType().getData().size());
        model.addAttribute("profilePayload", profileDetails);
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
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup().getData());
        model.addAttribute("ticketSla", xticketService.fetchTicketSla().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "tickettype";
    }

    @GetMapping("/ticket/type/edit")
    public String ticketType(@RequestParam("seid") String id, Model model, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        XTicketPayload response = xticketService.fetchTicketType(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "error";
            return "redirect:/setup/ticket/type/list";
        }
        model.addAttribute("ticketPayload", response);
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("recordCount", xticketService.fetchTicketType().getData().size());
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup().getData());
        model.addAttribute("ticketSla", xticketService.fetchTicketSla().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "tickettype";
    }

    @GetMapping("/ticket/type/list")
    public String ticketTypeList(Model model, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        XTicketPayload response = xticketService.fetchTicketType();
        model.addAttribute("dataList", response.getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("ticketGroup", xticketService.fetchTicketGroup().getData());
        model.addAttribute("ticketSla", xticketService.fetchTicketSla().getData());
        model.addAttribute("alertMessage", messageSource.getMessage("appMessages.ticket.record", new Object[]{response.getData().size()}, Locale.ENGLISH));
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "tickettypelist";
    }

    @GetMapping("/ticket/type/delete")
    public String deleteTicketType(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.deleteTicketType(seid, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = "success";
        return "redirect:/setup/ticket/type/list";
    }

    @GetMapping("/ticket/agent")
    public String ticketAgent(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("userList", xticketService.fetchInternalAppUsers());
        model.addAttribute("roleList", null);
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "ticketagent";
    }

    @PostMapping("/ticket/agent/permission")
    public String ticketAgentPermission(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("userList", xticketService.fetchInternalAppUsers());
        model.addAttribute("roleList", xticketService.fetchAgentTicketTypes(requestPayload.getEmail()).getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "ticketagent";
    }

    @PostMapping("/ticket/agent/create")
    public String ticketAgent(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.createTicketAgent(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/setup/ticket/agent";
        }
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("userList", xticketService.fetchInternalAppUsers());
        model.addAttribute("roleList", xticketService.fetchAgentTicketTypes(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "ticketagent";
    }

    @GetMapping("/ticket/agent/list")
    public String ticketAgentList(Model model, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        XTicketPayload response = xticketService.fetchTicketAgent();
        model.addAttribute("dataList", response.getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", messageSource.getMessage("appMessages.ticket.record", new Object[]{response.getData().size()}, Locale.ENGLISH));
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "ticketagentlist";
    }

    @GetMapping("/ticket/sla")
    public String ticketSla(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("recordCount", xticketService.fetchTicketSla().getData().size());
        model.addAttribute("ticketSla", xticketService.fetchTicketSla());
        model.addAttribute("profilePayload", profileDetails);
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
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "ticketsla";
    }

    @GetMapping("/ticket/sla/edit")
    public String ticketSla(@RequestParam("seid") String id, Model model, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        XTicketPayload response = xticketService.fetchTicketSla(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "error";
            return "redirect:/setup/ticket/sla/list";
        }
        model.addAttribute("ticketPayload", response);
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("recordCount", xticketService.fetchTicketSla().getData().size());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "ticketsla";
    }

    @GetMapping("/ticket/sla/list")
    public String ticketSlaList(Model model, Principal principal) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        XTicketPayload response = xticketService.fetchTicketSla();
        model.addAttribute("dataList", response.getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", messageSource.getMessage("appMessages.ticket.record", new Object[]{response.getData().size()}, Locale.ENGLISH));
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "ticketslalist";
    }

    @GetMapping("/ticket/sla/delete")
    public String deleteTicketSla(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.deleteTicketSla(seid, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = "success";
        return "redirect:/setup/ticket/sla/list";
    }

    private void resetAlertMessage() {
        alertMessage = "";
        alertMessageType = "";
    }
}
