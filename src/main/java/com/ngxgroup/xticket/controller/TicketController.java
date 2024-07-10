package com.ngxgroup.xticket.controller;

import com.ngxgroup.xticket.constant.ResponseCodes;
import com.ngxgroup.xticket.model.TicketAgent;
import com.ngxgroup.xticket.model.TicketType;
import com.ngxgroup.xticket.payload.XTicketPayload;
import com.ngxgroup.xticket.service.XTicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "newticket";
    }

    @PostMapping("/new/create")
    public String createTicket(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.createTicket(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/ticket/new";
        }
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        return "newticket";
    }

    @PostMapping("/reply")
    public String replyTicket(@ModelAttribute("ticketReplyPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.replyTicket(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/ticket/open";
        }
        model.addAttribute("ticketPayload", response);
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        return "ticketdetails";
    }

    @GetMapping("/type/fetch/{ticketGroupCode}")
    @ResponseBody
    public List<TicketType> ticketGroupTypes(@PathVariable("ticketGroupCode") String ticketGroupCode, Principal principal) {
        List<TicketType> response = xticketService.fetchTicketTypeUsingGroup(ticketGroupCode, principal.getName());
        return response;
    }

    @GetMapping("/agent/fetch/{ticketTypeCode}")
    @ResponseBody
    public List<TicketAgent> ticketTypeAgents(@PathVariable("ticketTypeCode") String ticketTypeCode, Principal principal) {
        List<TicketAgent> response = xticketService.fetchTicketAgentUsingType(ticketTypeCode, principal.getName());
        return response;
    }

    @PostMapping("/reopen")
    public String reopenTicket(@ModelAttribute("ticketReplyPayload") XTicketPayload requestPayload, Model model, Principal principal) {
        XTicketPayload response = xticketService.createReopenTicket(requestPayload, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/ticket/open";
    }

    @GetMapping("/closed")
    public String myTicket(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("dataList", xticketService.fetchClosedTicket(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "myclosedticket";
    }

    @GetMapping("/open")
    public String myOpenTicket(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("dataList", xticketService.fetchOpenTicket(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "myopenticket";
    }

    @GetMapping("/view")
    public String viewTicket(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchTicketUsingId(seid);
        XTicketPayload ticketPayload = new XTicketPayload();
        ticketPayload.setTicketId(response.getTicketId());
        model.addAttribute("ticketPayload", response);
        model.addAttribute("ticketReplyPayload", ticketPayload);
        model.addAttribute("ticketList", xticketService.fetchTicketByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "ticketdetails";
    }

    @GetMapping("/close")
    public String closeTicket(@RequestParam("seid") String seid, @RequestParam("tr") String ticketReopened, @RequestParam("troid") String ticketReopenedId, Model model, Principal principal) {
        XTicketPayload response = xticketService.closeTicket(seid, ticketReopened, ticketReopenedId, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/ticket/open";
    }

    @PostMapping("/search")
    public String searchTicket(@ModelAttribute("profilePayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.fetchTicketFullDetails(requestPayload.getTicketId());
        model.addAttribute("ticketPayload", response);
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("dataList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("reopenedTicketList", response.getReopenedTickets());
        model.addAttribute("reassignedTicketList", response.getReassignedTickets());
        model.addAttribute("escalatedTicketList", response.getTicketEscalations());
        model.addAttribute("commentTicketList", response.getTicketComments());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        return "ticketfulldetails";
    }

    private void resetAlertMessage() {
        alertMessage = "";
        alertMessageType = "";
    }
}
