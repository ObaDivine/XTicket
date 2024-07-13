package com.ngxgroup.xticket.controller;

import com.ngxgroup.xticket.constant.ResponseCodes;
import com.ngxgroup.xticket.payload.XTicketPayload;
import com.ngxgroup.xticket.service.XTicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
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
 * @author briano
 */
@Controller
@RequestMapping("/agent/ticket")
public class AgentController {

    @Autowired
    XTicketService xticketService;
    private String alertMessage = "";
    private String alertMessageType = "";

    @GetMapping("/open")
    @Secured("ROLE_TICKET_AGENT")
    public String openTicket(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("dataList", xticketService.fetchOpenTicketForAgent(principal.getName()).getData());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("userList", null);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "agentopenticket";
    }

    @GetMapping("/view")
    @Secured("ROLE_TICKET_AGENT")
    public String viewTicket(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchTicketUsingId(seid);
        XTicketPayload ticketPayload = new XTicketPayload();
        ticketPayload.setTicketId(response.getTicketId());
        model.addAttribute("ticketPayload", response);
        model.addAttribute("ticketReplyPayload", ticketPayload);
        model.addAttribute("ticketReassignPayload", ticketPayload);
        model.addAttribute("ticketList", xticketService.fetchTicketByUser(principal.getName()).getData());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("ticketStatusList", xticketService.fetchTicketStatusForReply().getData());
        model.addAttribute("userList", null);
        model.addAttribute("documentList", response.getUploadDocuments());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "agentticketdetails";
    }

    @PostMapping("/reassigned")
    public String createTicket(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.createTicketReassignment(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/agent/ticket/open";
        }
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("dataList", xticketService.fetchOpenTicketForAgent(principal.getName()).getData());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("userList", null);
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        return "agentopenticket";
    }

    @PostMapping("/reply")
    public String replyTicket(@ModelAttribute("ticketReplyPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.agentReplyTicket(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/agent/ticket/open";
        }
        model.addAttribute("ticketPayload", response);
        XTicketPayload ticketPayload = new XTicketPayload();
        ticketPayload.setTicketId(response.getTicketId());
        model.addAttribute("ticketReplyPayload", ticketPayload);
        model.addAttribute("ticketReassignPayload", ticketPayload);
        model.addAttribute("ticketList", xticketService.fetchTicketByUser(principal.getName()).getData());
        model.addAttribute("ticketGroupList", xticketService.fetchTicketGroup().getData());
        model.addAttribute("userList", null);
        return "agentticketdetails";
    }

    @GetMapping("/close")
    @Secured("ROLE_TICKET_AGENT")
    public String closeTicket(@RequestParam("seid") String seid, @RequestParam("tr") String ticketReopened, @RequestParam("troid") String ticketReopenedId, Model model, Principal principal) {
        XTicketPayload response = xticketService.closeTicket(seid, ticketReopened, ticketReopenedId, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/agent/ticket/open";
    }

    private void resetAlertMessage() {
        alertMessage = "";
        alertMessageType = "";
    }
}
