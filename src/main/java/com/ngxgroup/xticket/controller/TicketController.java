package com.ngxgroup.xticket.controller;

import com.ngxgroup.xticket.constant.ResponseCodes;
import com.ngxgroup.xticket.model.TicketType;
import com.ngxgroup.xticket.payload.XTicketPayload;
import com.ngxgroup.xticket.service.XTicketService;
import java.security.Principal;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.processFetchTicketGroup().getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "newticket";
    }

    @PostMapping("/new/create")
    public String createTicket(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.processCreateTicket(requestPayload, principal.getName());
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/ticket/new";
        }
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("ticketGroupList", xticketService.processFetchTicketGroup().getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        return "newticket";
    }
    
    @GetMapping("/type/fetch/{ticketGroupCode}")
    @ResponseBody
    public List<TicketType> ticketGroupTypes(@PathVariable("ticketGroupCode") String ticketGroupCode, Principal principal){
        List<TicketType> response = xticketService.processFetchTicketTypeUsingGroup(ticketGroupCode, principal.getName());
        return response;
    }

    @GetMapping("/reopen")
    public String reopenTicket(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.processFetchTicketGroup().getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "reopenticket";
    }

    @GetMapping("/my")
    public String myTicket(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.processFetchTicketGroup().getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "myticket";
    }

    @GetMapping("/open")
    public String openTicket(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroup", xticketService.processFetchTicketGroup().getData());
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
