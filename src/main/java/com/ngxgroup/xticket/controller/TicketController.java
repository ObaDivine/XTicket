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
@RequestMapping("/ticket")
public class TicketController {

    @Autowired
    XTicketService xticketService;
    private String alertMessage = "";
    private String alertMessageType = "";

    @GetMapping("/new")
    public String newTicket(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.processFetchTicketGroup());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "newticket";
    }

    @GetMapping("/reopen")
    public String reopenTicket(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.processFetchTicketGroup());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reopenticket";
    }

    @GetMapping("/my")
    public String myTicket(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.processFetchTicketGroup());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "myticket";
    }

    @GetMapping("/open")
    public String openTicket(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.processFetchTicketGroup());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "openticket";
    }

    private void resetAlertMessage() {
        alertMessage = "";
        alertMessageType = "";
    }
}
