package com.ngxgroup.xticket.controller;

import com.ngxgroup.xticket.constant.ResponseCodes;
import com.ngxgroup.xticket.model.GroupRoles;
import com.ngxgroup.xticket.payload.XTicketPayload;
import com.ngxgroup.xticket.payload.XTicketPayload;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import com.ngxgroup.xticket.service.XTicketService;

/**
 *
 * @author Brian A. Okon okon.brian@gmail.com
 */
@Controller
public class HomeController {

    @Autowired
    XTicketService xticketService;
    @Autowired
    MessageSource messageSource;
    private static final Logger LOGGER = Logger.getLogger(HomeController.class.getName());
    private String alertMessage = "";

    @GetMapping("/")
    public String signin(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("loginPayload", new XTicketPayload());
        model.addAttribute("xpolicyPayload", new XTicketPayload());
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "signin";
    }

    @PostMapping("/signin")
    public String signin(@ModelAttribute("signinPayload") XTicketPayload requestPayload, HttpSession session, HttpServletRequest httpRequest, Model model) {
        XTicketPayload response = xticketService.processSignin(requestPayload);
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            List<SimpleGrantedAuthority> newAuthorities = new ArrayList<>();
            List<GroupRoles> roles = new ArrayList<>();//xpolicyService.getUserRoles(requestPayload.getEmail());
            if (roles == null) {
                model.addAttribute("signinPayload", requestPayload);
                model.addAttribute("alertMessage", messageSource.getMessage("appMessages.role.notexist", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH));
                model.addAttribute("alertMessageType", "error");
                resetAlertMessage();
                return "signin";
            }

            for (GroupRoles userRole : roles) {
                newAuthorities.add(new SimpleGrantedAuthority("ROLE_" + userRole.getAppRole().getRoleName()));
            }
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(requestPayload.getEmail(), requestPayload.getPassword(), newAuthorities));
            resetAlertMessage();
            return "redirect:/dashboard";
        }

        model.addAttribute("signinPayload", requestPayload);
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "signin";
    }

    @GetMapping("/signup")
    public String signup(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("signupPayload", new XTicketPayload());
        model.addAttribute("xpolicyPayload", new XTicketPayload());
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "signup";
    }

    @PostMapping("/signup/")
    public String signup(@ModelAttribute("signupPayload") XTicketPayload requestPayload, HttpSession session, HttpServletRequest httpRequest, Model model) {
        XTicketPayload response = xticketService.processSignup(requestPayload);
        model.addAttribute("signupPayload", requestPayload);
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "signup";
    }

    @PostMapping("/account/logout")
    public String logoutPage(HttpServletRequest request, HttpServletResponse response, Principal principal, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        alertMessage = "Your session is terminated and you are logged out";
        return "redirect:/";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HttpSession httpSession, Principal principal, Model model) {
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "dashboard";
    }

    @GetMapping("/terms")
    public String terms(HttpServletRequest request, HttpServletResponse response, Principal principal, Model model) {
        model.addAttribute("xpolicyPayload", new XTicketPayload());
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "terms";
    }

    @GetMapping("/privacy")
    public String privacy(HttpServletRequest request, HttpServletResponse response, Principal principal, Model model) {
        model.addAttribute("xpolicyPayload", new XTicketPayload());
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "privacy";
    }

    private void resetAlertMessage() {
        alertMessage = "";
    }
}
