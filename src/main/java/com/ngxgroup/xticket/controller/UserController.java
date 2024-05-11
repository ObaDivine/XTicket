package com.ngxgroup.xticket.controller;

import com.ngxgroup.xticket.constant.ResponseCodes;
import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.payload.XTicketPayload;
import com.ngxgroup.xticket.service.XTicketService;
import java.security.Principal;
import java.util.List;
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
 * @author briano
 */
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    XTicketService xticketService;
    @Autowired
    MessageSource messageSource;
    private String alertMessage = "";
    private String alertMessageType = "";

    @GetMapping("/")
    public String appUser(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        model.addAttribute("userPayload", new XTicketPayload());
        model.addAttribute("userList", xticketService.processFetchAppUsers());
        model.addAttribute("roleList", xticketService.processFetchRoleGroup());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "appuser";
    }

    @PostMapping("/update")
    public String updateUser(@ModelAttribute("userPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.processUpdateAppUser(requestPayload, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/user/";
    }

    @GetMapping("/list")
    public String appUserList(Model model, Principal principal) {
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        List<AppUser> response = xticketService.processFetchAppUsers();
        model.addAttribute("dataList", xticketService.processFetchAppUsers());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", messageSource.getMessage("appMessages.ticket.record", new Object[]{response.size()}, Locale.ENGLISH));
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "appuserlist";
    }

    @GetMapping("/roles")
    public String userRoles(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        model.addAttribute("rolePayload", new XTicketPayload());
        model.addAttribute("roleList", xticketService.processFetchRoleGroup());
        model.addAttribute("groupRolesPayload", null);
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "roles";
    }

    @PostMapping("/roles/group")
    public String createRoleGroup(@ModelAttribute("xticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.processCreateRoleGroup(requestPayload, principal.getName());
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/user/roles";
        }
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("rolePayload", requestPayload);
        model.addAttribute("roleList", xticketService.processFetchRoleGroup());
        model.addAttribute("groupRolesPayload", response.getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        return "roles";
    }

    @GetMapping("/roles/edit")
    public String editRoleGroup(@RequestParam("seid") String seid, Model model, Principal principal, HttpServletRequest httpRequest) {
        XTicketPayload response = xticketService.processFetchRoleGroup(seid);
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/user/roles";
        }
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("rolePayload", response);
        model.addAttribute("roleList", xticketService.processFetchRoleGroup());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "roles";
    }

    @GetMapping("/roles/delete")
    public String deleteRole(@RequestParam("seid") String seid, Model model, Principal principal, HttpServletRequest httpRequest) {
        XTicketPayload response = xticketService.processDeleteRoleGroup(seid, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = "success";
        return "redirect:/user/roles";
    }

    @PostMapping("/roles/fetch")
    public String fetchGroupRoles(@ModelAttribute("rolePayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        model.addAttribute("rolePayload", requestPayload);
        model.addAttribute("roleList", xticketService.processFetchRoleGroup());
        model.addAttribute("groupRolesPayload", xticketService.processFetchGroupRoles(requestPayload.getGroupName()).getData());
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", "");
        model.addAttribute("alertMessageType", "");
        return "roles";
    }

    @PostMapping("/roles/update")
    public String updateGroupRoles(@ModelAttribute("rolePayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.processUpdateGroupRoles(requestPayload);
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        model.addAttribute("rolePayload", requestPayload);
        model.addAttribute("roleList", xticketService.processFetchRoleGroup());
        model.addAttribute("groupRolesPayload", null);
        model.addAttribute("profilePayload", profileDetails);
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        return "roles";
    }

    private void resetAlertMessage() {
        alertMessage = "";
        alertMessageType = "";
    }
}
