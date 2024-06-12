package com.ngxgroup.xticket.controller;

import com.google.gson.Gson;
import com.ngxgroup.xticket.constant.ResponseCodes;
import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.model.GroupRoles;
import com.ngxgroup.xticket.payload.XTicketPayload;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Brian A. Okon okon.brian@gmail.com
 */
@Controller
public class HomeController {

//    @Autowired
//    AuthenticationManager authenticationManager;
    @Autowired
    XTicketService xticketService;
    @Autowired
    MessageSource messageSource;
    @Autowired
    Gson gson;
    @Value("${xticket.adauth.domains}")
    private String adAuthDomains;
    private static final Logger LOGGER = Logger.getLogger(HomeController.class.getName());
    private String alertMessage = "";
    private String alertMessageType = "";
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    @GetMapping("/")
    public String signin(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("loginPayload", new XTicketPayload());
        model.addAttribute("passwordPayload", new XTicketPayload());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "signin";
    }

    @PostMapping("/login")
    public String signin(@ModelAttribute("signinPayload") XTicketPayload requestPayload, HttpSession session, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) {
        XTicketPayload response = xticketService.signin(requestPayload);
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            List<SimpleGrantedAuthority> newAuthorities = new ArrayList<>();
            List<GroupRoles> roles = xticketService.fetchUserRoles(requestPayload.getEmail());
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

//            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(requestPayload.getEmail(), requestPayload.getPassword(), newAuthorities));
//            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(requestPayload.getEmail(), requestPayload.getPassword(), newAuthorities));
            
            SecurityContext context = securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(requestPayload.getEmail(), requestPayload.getPassword(), newAuthorities));
            securityContextHolderStrategy.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);
            //Check if the user is an egent
            if (response.isAgent()) {
                return "redirect:/agent/dashboard";
            }
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
        XTicketPayload requestPayload = new XTicketPayload();
        requestPayload.setAdAuthDomains(adAuthDomains);
        model.addAttribute("signupPayload", requestPayload);
        model.addAttribute("profilePayload", new XTicketPayload());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "signup";
    }

    @PostMapping("/signup/")
    public String signup(@ModelAttribute("signupPayload") XTicketPayload requestPayload, HttpSession session, HttpServletRequest httpRequest, Model model) {
        XTicketPayload response = xticketService.signup(requestPayload);
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
        XTicketPayload response = xticketService.signUpActivation(id);
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logoutPage(HttpServletRequest request, HttpServletResponse response, Principal principal, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            xticketService.userOnline(principal.getName(), false);
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        alertMessage = "Your session is terminated and you are logged out";
        return "redirect:/";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("profilePayload", profileDetails);
        List<XTicketPayload> closedTickets = xticketService.fetchClosedTicket(principal.getName()).getData();
        List<XTicketPayload> openTickets = xticketService.fetchOpenTicket(principal.getName()).getData();
        model.addAttribute("closedTicketStat", closedTickets == null ? 0 : closedTickets.size());
        model.addAttribute("openTicketStat", openTickets == null ? 0 : openTickets.size());
        List<XTicketPayload> ticketByGroup = xticketService.fetchTicketGroupStatisticsByUser(principal.getName()).getData();
        List<XTicketPayload> ticketByStatus = xticketService.fetchTicketStatusStatisticsByUser(principal.getName()).getData();
        model.addAttribute("ticketList", ticketByGroup);
        model.addAttribute("ticketByGroupChartData", generateTicketByGroupChart(ticketByGroup));
        model.addAttribute("ticketByStatusChartData", generateTicketByGroupChart(ticketByStatus));
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "dashboard";
    }

    @GetMapping("/agent/dashboard")
    public String agentDashboard(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("profilePayload", profileDetails);
        List<XTicketPayload> closedTickets = xticketService.fetchClosedTicket(principal.getName()).getData();
        List<XTicketPayload> openTickets = xticketService.fetchOpenTicket().getData();
        model.addAttribute("closedTicketStat", closedTickets == null ? 0 : closedTickets.size());
        model.addAttribute("agentOpenTicketStat", openTickets == null ? 0 : openTickets.size());
        List<XTicketPayload> ticketByGroup = xticketService.fetchTicketGroupStatisticsByUser(principal.getName()).getData();
        List<XTicketPayload> ticketByStatus = xticketService.fetchTicketStatusStatisticsByUser(principal.getName()).getData();
        model.addAttribute("ticketList", ticketByGroup);
        model.addAttribute("ticketByGroupChartData", generateTicketByGroupChart(ticketByGroup));
        model.addAttribute("ticketByStatusChartData", generateTicketByGroupChart(ticketByStatus));
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "agentdashboard";
    }

    @GetMapping("/change-password")
    public String changePassword(Principal principal, Model model) {
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("profilePayload", profileDetails);
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
        XTicketPayload response = xticketService.changePassword(requestPayload);
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
        XTicketPayload profileDetails = xticketService.fetchProfile(principal.getName());
        model.addAttribute("profilePayload", profileDetails);
        requestPayload.setEmail(principal.getName());
        model.addAttribute("passwordPayload", requestPayload);
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "changepassword";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@ModelAttribute("passwordPayload") XTicketPayload requestPayload, HttpSession session, Model model) {
        XTicketPayload response = xticketService.forgotPassword(requestPayload);
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/";
    }

    @GetMapping("/terms")
    public String terms(HttpServletRequest request, HttpServletResponse response, Principal principal, Model model) {
        model.addAttribute("profilePayload", new XTicketPayload());
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "terms";
    }

    @GetMapping("/privacy")
    public String privacy(HttpServletRequest request, HttpServletResponse response, Principal principal, Model model) {
        model.addAttribute("profilePayload", new XTicketPayload());
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "privacy";
    }

    private String generateTicketByGroupChart(List<XTicketPayload> ticketList) {
        List<XTicketPayload> data = new ArrayList<>();
        if (ticketList != null) {
            for (XTicketPayload t : ticketList) {
                XTicketPayload chart = new XTicketPayload();
                chart.setValue(t.getValue());
                chart.setName(t.getName());
                data.add(chart);
            }
        }
        return gson.toJson(data);
    }

    private boolean isLogin() {
        return SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                && !(SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken);
    }

    private void resetAlertMessage() {
        alertMessage = "";
        alertMessageType = "";
    }
}
