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
import org.springframework.web.bind.annotation.RequestParam;

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
    private String alertMessageType = "";

    @GetMapping("/")
    public String signin(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("loginPayload", new XTicketPayload());
        model.addAttribute("passwordPayload", new XTicketPayload());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
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
                model.addAttribute("loginPayload", requestPayload);
                model.addAttribute("passwordPayload", new XTicketPayload());
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

        model.addAttribute("loginPayload", requestPayload);
        model.addAttribute("passwordPayload", new XTicketPayload());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "signin";
    }

    @GetMapping("/signup")
    public String signup(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("signupPayload", new XTicketPayload());
        model.addAttribute("xticketPayload", new XTicketPayload());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "signup";
    }

    @PostMapping("/signup/")
    public String signup(@ModelAttribute("signupPayload") XTicketPayload requestPayload, HttpSession session, HttpServletRequest httpRequest, Model model) {
        XTicketPayload response = xticketService.processSignup(requestPayload);
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/";
        }
        model.addAttribute("signupPayload", requestPayload);
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "signup";
    }

    @GetMapping("/signup/activate")
    public String signupActivation(@RequestParam("id") String id, Model model, HttpServletRequest request, HttpSession httpSession) {
        XTicketPayload response = xticketService.processSignUpActivation(id);
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/";
    }

    @PostMapping("/logout")
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
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        model.addAttribute("xticketPayload", profileDetails);
        model.addAttribute("myTicketStat", 30);
        model.addAttribute("openTicketStat", 30);
        model.addAttribute("incidentTicketStat", 30);
        model.addAttribute("serviceTicketStat", 30);
        model.addAttribute("chageRequestTicketStat", 30);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "dashboard";
    }

    @GetMapping("/change-password")
    public String changePassword(Principal principal, Model model) {
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        model.addAttribute("xticketPayload", profileDetails);
        XTicketPayload passwordChangePayload = new XTicketPayload();
        passwordChangePayload.setEmail(principal.getName());
        model.addAttribute("passwordPayload", passwordChangePayload);
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "changepassword";
    }

    @PostMapping("/change-password/")
    public String changePassword(@ModelAttribute("passwordPayload") XTicketPayload requestPayload, HttpSession session, Model model, Principal principal, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        XTicketPayload response = xticketService.processChangePassword(requestPayload);
        //Check for locked account due to multiple invalid attempts
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.MULTIPLE_ATTEMPT.getResponseCode())) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                new SecurityContextLogoutHandler().logout(httpRequest, httpResponse, auth);
            }
            alertMessage = response.getResponseMessage();
            alertMessageType = "error";
            return "redirect:/";
        }

        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/";
        }
        XTicketPayload profileDetails = xticketService.processFetchProfile(principal.getName());
        model.addAttribute("xticketPayload", profileDetails);
        requestPayload.setEmail(principal.getName());
        model.addAttribute("passwordPayload", requestPayload);
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "changepassword";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@ModelAttribute("passwordPayload") XTicketPayload requestPayload, HttpSession session, Model model) {
        XTicketPayload response = xticketService.processForgotPassword(requestPayload);
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/";
    }

    @GetMapping("/terms")
    public String terms(HttpServletRequest request, HttpServletResponse response, Principal principal, Model model) {
        model.addAttribute("xticketPayload", new XTicketPayload());
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "terms";
    }

    @GetMapping("/privacy")
    public String privacy(HttpServletRequest request, HttpServletResponse response, Principal principal, Model model) {
        model.addAttribute("xticketPayload", new XTicketPayload());
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "privacy";
    }

    private void resetAlertMessage() {
        alertMessage = "";
        alertMessageType = "";
    }
}
