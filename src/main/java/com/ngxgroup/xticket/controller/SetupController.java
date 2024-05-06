package com.ngxgroup.xticket.controller;

import com.ngxgroup.xticket.payload.XTicketPayload;
import com.ngxgroup.xticket.service.XTicketService;
import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author bokon
 */
@Controller
@RequestMapping("/setup")
public class SetupController {

    @Autowired
    XTicketService xticketService;
    private String alertMessage = "";
    private String alertMessageType = "";

    @GetMapping("/ticket/group")
    public String ticketGroup(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.processFetchTicketGroup());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "ticketgroup";
    }

    @GetMapping("/ticket/type")
    public String ticketType(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.processFetchTicketGroup());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "tickettype";
    }

    @GetMapping("/ticket/technicians")
    public String ticketTechnicians(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.processFetchTicketGroup());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "tickettechnician";
    }

    @GetMapping("/user")
    public String systemUser(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.processFetchTicketGroup());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "adduser";
    }

    @GetMapping("/roles")
    public String userRoles(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.processFetchTicketGroup());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "addroles";
    }

    private void resetAlertMessage() {
        alertMessage = "";
        alertMessageType = "";
    }
}
