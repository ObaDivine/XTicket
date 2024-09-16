package com.ngxgroup.xticket.controller;

import com.ngxgroup.xticket.constant.ResponseCodes;
import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.payload.XTicketPayload;
import com.ngxgroup.xticket.service.XTicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
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
@RequestMapping("/user")
public class UserController {

    @Autowired
    XTicketService xticketService;
    @Autowired
    MessageSource messageSource;
    private String alertMessage = "";
    private String alertMessageType = "";

    @GetMapping("/")
    @Secured("ROLE_MANAGE_USER")
    public String appUser(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("userPayload", new XTicketPayload());
        model.addAttribute("userList", xticketService.fetchAppUsers());
        model.addAttribute("roleList", xticketService.fetchRoleGroup());
        model.addAttribute("departmentList", xticketService.fetchDepartment().getData());
        model.addAttribute("entityList", xticketService.fetchEntity().getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "appuser";
    }

    @PostMapping("/update")
    public String updateUser(@ModelAttribute("userPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.updateAppUser(requestPayload, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/user/";
    }

    @GetMapping("/list")
    @Secured("ROLE_LIST_USER")
    public String appUserList(Model model, Principal principal) {
        List<AppUser> response = xticketService.fetchAppUsers();
        model.addAttribute("dataList", xticketService.fetchAppUsers());
        model.addAttribute("alertMessage", messageSource.getMessage("appMessages.ticket.record", new Object[]{response.size()}, Locale.ENGLISH));
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "appuserlist";
    }

    @GetMapping("/roles")
    @Secured("ROLE_ADD_ROLES")
    public String userRoles(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("rolePayload", new XTicketPayload());
        model.addAttribute("roleList", xticketService.fetchRoleGroup());
        model.addAttribute("groupRolesPayload", null);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "roles";
    }

    @PostMapping("/roles/group")
    public String createRoleGroup(@ModelAttribute("xticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.createRoleGroup(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/user/roles";
        }
        model.addAttribute("rolePayload", requestPayload);
        model.addAttribute("roleList", xticketService.fetchRoleGroup());
        model.addAttribute("groupRolesPayload", response.getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        return "roles";
    }

    @GetMapping("/roles/edit")
    @Secured("ROLE_UPDATE_ROLES")
    public String editRoleGroup(@RequestParam("seid") String seid, Model model, Principal principal, HttpServletRequest httpRequest) {
        XTicketPayload response = xticketService.fetchRoleGroup(seid);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/user/roles";
        }
        model.addAttribute("rolePayload", response);
        model.addAttribute("roleList", xticketService.fetchRoleGroup());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "roles";
    }

    @GetMapping("/roles/delete")
    @Secured("ROLE_DELETE_ROLES")
    public String deleteRole(@RequestParam("seid") String seid, Model model, Principal principal, HttpServletRequest httpRequest) {
        XTicketPayload response = xticketService.deleteRoleGroup(seid, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = "success";
        return "redirect:/user/roles";
    }

    @PostMapping("/roles/fetch")
    public String fetchGroupRoles(@ModelAttribute("rolePayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        model.addAttribute("rolePayload", requestPayload);
        model.addAttribute("roleList", xticketService.fetchRoleGroup());
        model.addAttribute("groupRolesPayload", xticketService.fetchGroupRoles(requestPayload.getGroupName()).getData());
        model.addAttribute("alertMessage", "");
        model.addAttribute("alertMessageType", "");
        return "roles";
    }

    @PostMapping("/roles/update")
    public String updateGroupRoles(@ModelAttribute("rolePayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.updateGroupRoles(requestPayload);
        model.addAttribute("rolePayload", requestPayload);
        model.addAttribute("roleList", xticketService.fetchRoleGroup());
        model.addAttribute("groupRolesPayload", null);
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error");
        return "roles";
    }

    private void resetAlertMessage() {
        alertMessage = "";
        alertMessageType = "";
    }
}
