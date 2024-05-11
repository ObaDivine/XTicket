package com.ngxgroup.xticket.controller;

import com.ngxgroup.xticket.constant.ResponseCodes;
import com.ngxgroup.xticket.payload.XTicketPayload;
import com.ngxgroup.xticket.service.XTicketService;
import java.security.Principal;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
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
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("recordCount", xticketService.processFetchTicketGroup().getData().size());
        model.addAttribute("ticketGroup", xticketService.processFetchTicketGroup());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "ticketgroup";
    }

    @PostMapping("/ticket/group/create")
    public String ticketGroup(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.processCreateTicketGroup(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/setup/ticket/group";
        }
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "ticketgroup";
    }

    @GetMapping("/ticket/group/edit")
    public String ticketGroup(@RequestParam("seid") String id, Model model, Principal principal) {
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        XTicketPayload response = xticketService.processFetchTicketGroup(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "error";
            return "redirect:/setup/ticket/group/list";
        }
        model.addAttribute("ticketPayload", response);
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("recordCount", xticketService.processFetchTicketGroup().getData().size());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "ticketgroup";
    }

    @GetMapping("/ticket/group/list")
    public String ticketGroupList(Model model, Principal principal) {
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        XTicketPayload response = xticketService.processFetchTicketGroup();
        model.addAttribute("dataList", response.getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", messageSource.getMessage("appMessages.ticket.record", new Object[]{response.getData().size()}, Locale.ENGLISH));
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "ticketgrouplist";
    }

    @GetMapping("/ticket/group/delete")
    public String deleteTicketGroup(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.processDeleteTicketGroup(seid, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = "success";
        return "redirect:/setup/ticket/group/list";
    }

    @GetMapping("/ticket/type")
    public String ticketType(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.processFetchTicketGroup().getData());
        model.addAttribute("recordCount", xticketService.processFetchTicketType().getData().size());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "tickettype";
    }

    @PostMapping("/ticket/type/create")
    public String ticketType(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.processCreateTicketType(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/setup/ticket/type";
        }
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("ticketGroup", xticketService.processFetchTicketGroup().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "tickettype";
    }

    @GetMapping("/ticket/type/edit")
    public String ticketType(@RequestParam("seid") String id, Model model, Principal principal) {
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        XTicketPayload response = xticketService.processFetchTicketType(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "error";
            return "redirect:/setup/ticket/type/list";
        }
        model.addAttribute("ticketPayload", response);
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("recordCount", xticketService.processFetchTicketType().getData().size());
        model.addAttribute("ticketGroup", xticketService.processFetchTicketGroup().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "tickettype";
    }

    @GetMapping("/ticket/type/list")
    public String ticketTypeList(Model model, Principal principal) {
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        XTicketPayload response = xticketService.processFetchTicketType();
        model.addAttribute("dataList", response.getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("ticketGroup", xticketService.processFetchTicketGroup().getData());
        model.addAttribute("alertMessage", messageSource.getMessage("appMessages.ticket.record", new Object[]{response.getData().size()}, Locale.ENGLISH));
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "tickettypelist";
    }

    @GetMapping("/ticket/type/delete")
    public String deleteTicketType(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.processDeleteTicketType(seid, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = "success";
        return "redirect:/setup/ticket/type/list";
    }

    @GetMapping("/ticket/agent")
    public String ticketAgent(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("userList", xticketService.processFetchInternalAppUsers());
        model.addAttribute("roleList", null);
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "ticketagent";
    }

    @PostMapping("/ticket/agent/permission")
    public String ticketAgentPermission(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("userList", xticketService.processFetchInternalAppUsers());
        model.addAttribute("roleList", xticketService.processFetchAgentTicketTypes(requestPayload.getEmail()).getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "ticketagent";
    }

    @PostMapping("/ticket/agent/create")
    public String ticketAgent(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.processCreateTicketAgent(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/setup/ticket/agent";
        }
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("userList", xticketService.processFetchInternalAppUsers());
        model.addAttribute("roleList", xticketService.processFetchAgentTicketTypes(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "ticketagent";
    }

    @GetMapping("/ticket/agent/list")
    public String ticketAgentList(Model model, Principal principal) {
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        XTicketPayload response = xticketService.processFetchTicketAgent();
        model.addAttribute("dataList", response.getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", messageSource.getMessage("appMessages.ticket.record", new Object[]{response.getData().size()}, Locale.ENGLISH));
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "ticketagentlist";
    }

    private void resetAlertMessage() {
        alertMessage = "";
        alertMessageType = "";
    }
}
