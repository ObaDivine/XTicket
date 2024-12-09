package com.ngxgroup.xticket.controller;

import com.google.gson.Gson;
import com.ngxgroup.xticket.constant.ResponseCodes;
import com.ngxgroup.xticket.model.GroupRoles;
import com.ngxgroup.xticket.payload.XTicketPayload;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
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
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author briano
 */
@Controller
public class HomeController implements ErrorController {

    @Autowired
    XTicketService xticketService;
    @Autowired
    MessageSource messageSource;
    @Autowired
    Gson gson;
    @Value("${xticket.adauth.domains}")
    private String adAuthDomains;
    private String alertMessage = "";
    private String alertMessageType = "";
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    @GetMapping("/")
    public String signin(Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        model.addAttribute("loginPayload", new XTicketPayload());
        model.addAttribute("passwordPayload", new XTicketPayload());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "signin";
    }

    @PostMapping("/login")
    public String signin(@ModelAttribute("signinPayload") XTicketPayload requestPayload, HttpSession httpSession, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) {
        //Set the session id
        requestPayload.setSessionId(httpSession.getId());
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

            SecurityContext context = securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(requestPayload.getEmail(), requestPayload.getPassword(), newAuthorities));
            securityContextHolderStrategy.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            //set the session details
            XTicketPayload profileDetails = xticketService.fetchProfile(requestPayload.getEmail());
            httpSession.setAttribute("fullName", profileDetails.getLastName() + ", " + profileDetails.getOtherName());
            httpSession.setAttribute("lastName", profileDetails.getLastName());
            httpSession.setAttribute("otherNames", profileDetails.getOtherName());
            httpSession.setAttribute("email", profileDetails.getEmail());
            httpSession.setAttribute("mobileNumber", profileDetails.getMobileNumber());
            httpSession.setAttribute("gender", profileDetails.getGender());
            httpSession.setAttribute("passwordChangeDate", profileDetails.getPasswordChangeDate());
            httpSession.setAttribute("createdAt", profileDetails.getCreatedAt());
            httpSession.setAttribute("createdBy", profileDetails.getCreatedBy());
            httpSession.setAttribute("resetTime", profileDetails.getPasswordChangeDate());
            httpSession.setAttribute("isAgent", profileDetails.isAgent());
            httpSession.setAttribute("isInternal", profileDetails.isInternal());
            httpSession.setAttribute("isLocked", profileDetails.isLocked());
            httpSession.setAttribute("isActivated", profileDetails.isActivated());
            httpSession.setAttribute("userType", profileDetails.getUserType());
            
            //Check if the user is an egent
            if (response.isAgent()) {
                return "redirect:/agent/dashboard";
            }
            return "redirect:/dashboard";
        }

        //Check if password has expired
        if (response.getResponseMessage().equalsIgnoreCase(messageSource.getMessage("appMessages.password.expire", new Object[0], Locale.ENGLISH))) {
            model.addAttribute("passwordPayload", requestPayload);
            model.addAttribute("alertMessage", response.getResponseMessage());
            model.addAttribute("alertMessageType", "error");
            resetAlertMessage();
            return "passwordexpire";
        }

        model.addAttribute("loginPayload", requestPayload);
        model.addAttribute("passwordPayload", new XTicketPayload());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "signin";
    }

    @GetMapping("/signup")
    public String signup(Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        XTicketPayload requestPayload = new XTicketPayload();
        requestPayload.setAdAuthDomains(adAuthDomains);
        model.addAttribute("signupPayload", requestPayload);
        model.addAttribute("profilePayload", new XTicketPayload());
        model.addAttribute("department", xticketService.fetchDepartment().getData());
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
        model.addAttribute("department", xticketService.fetchDepartment().getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "signup";
    }

    @GetMapping("/signup/activate")
    public String signupActivation(@RequestParam("id") String id, Model model, HttpServletRequest httpRequest, HttpSession httpSession) {
        XTicketPayload response = xticketService.signUpActivation(id);
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logoutPage(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            xticketService.userOnline(principal.getName(), false);
            new SecurityContextLogoutHandler().logout(httpRequest, httpResponse, auth);
        }
        alertMessage = "Your session is terminated and you are logged out";
        return "redirect:/";
    }

    @GetMapping("/about")
    public String about(Principal principal, Model model) {
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "about";
    }

    @GetMapping("/system")
    @Secured("ROLE_LIST_USER")
    public String systemInfo(HttpServletRequest httpRequest, Principal principal, Model model) {
        model.addAttribute("userConnections", xticketService.fetchSystemInfo("UserConnection").getData());
        model.addAttribute("systemResources", xticketService.fetchSystemInfo("SystemResources").getKeyValuePair());
        model.addAttribute("javaVirtualMachine", xticketService.fetchSystemInfo("JavaVirtualMachine").getKeyValuePair());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "systeminfo";
    }

    @GetMapping("/dashboard")
    @Secured("ROLE_DASHBOARD")
    public String dashboard(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HttpSession httpSession, Principal principal, Model model) {
        List<XTicketPayload> closedTickets = xticketService.fetchClosedTicket(principal.getName()).getData();
        List<XTicketPayload> openTickets = xticketService.fetchOpenTicket(principal.getName()).getData();
        model.addAttribute("closedTicketStat", closedTickets == null ? 0 : closedTickets.size());
        model.addAttribute("openTicketStat", openTickets == null ? 0 : openTickets.size());
        List<XTicketPayload> ticketByGroup = xticketService.fetchTicketGroupStatisticsByUser(principal.getName()).getData();
        List<XTicketPayload> ticketByStatus = xticketService.fetchTicketStatusStatisticsByUser(principal.getName()).getData();
        model.addAttribute("ticketList", ticketByGroup);
        model.addAttribute("ticketByGroupChartData", generateTicketByGroupChart(ticketByGroup));
        model.addAttribute("ticketByStatusChartData", generateTicketByGroupChart(ticketByStatus));
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "dashboard";
    }

    @GetMapping("/agent/dashboard")
    @Secured("ROLE_DASHBOARD")
    public String agentDashboard(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HttpSession httpSession, Principal principal, Model model) {
        List<XTicketPayload> closedTickets = xticketService.fetchClosedTicket(principal.getName()).getData();
        List<XTicketPayload> openTickets = xticketService.fetchOpenTicketForAgent(principal.getName(), "all").getData();
        XTicketPayload aboutToViolateSlaTickets = xticketService.fetchOpenTicketAboutToViolateSlaForAgent(principal.getName());
        XTicketPayload criticalSlaTickets = xticketService.fetchOpenTicketWithCriticalSlaForAgent(principal.getName());
        List<XTicketPayload> ticketByGroup = xticketService.fetchTicketGroupStatisticsByUser(principal.getName()).getData();
        List<XTicketPayload> ticketByStatus = xticketService.fetchTicketStatusStatisticsByUser(principal.getName()).getData();
        List<XTicketPayload> openTicketByGroup = xticketService.fetchOpenTicketGroupStatisticsForAgent(principal.getName()).getData();
        model.addAttribute("closedTicketStat", closedTickets == null ? 0 : closedTickets.size());
        model.addAttribute("agentOpenTicketStat", openTickets == null ? 0 : openTickets.size());
        model.addAttribute("aboutToViolateSlaTicketStat", aboutToViolateSlaTickets.getValue());
        model.addAttribute("criticalSlaTicketStat", criticalSlaTickets.getValue());
        model.addAttribute("ticketList", openTicketByGroup);
        model.addAttribute("ticketByGroupChartData", generateTicketByGroupChart(ticketByGroup));
        model.addAttribute("ticketByStatusChartData", generateTicketByGroupChart(ticketByStatus));
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "agentdashboard";
    }

    @GetMapping("/change-password")
    public String changePassword(Principal principal, Model model) {
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

    @PostMapping("/change-expired-password/")
    public String changeExpiredPassword(@ModelAttribute("passwordPayload") XTicketPayload requestPayload, HttpSession session, Model model, Principal principal, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
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
        model.addAttribute("passwordPayload", requestPayload);
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "passwordexpire";
    }

    @GetMapping("/profile")
    public String profile(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal, Model model) {
        model.addAttribute("profilePayload", new XTicketPayload());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "profile";
    }

    @GetMapping("/knowledge-base")
    @Secured("ROLE_KNOWLEDGE_BASE")
    public String knowledgeBase(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal, Model model) {
        XTicketPayload requestPayload = new XTicketPayload();
        requestPayload.setEmail(principal.getName());
        model.addAttribute("dataList", xticketService.fetchKnowledgeBase());
        model.addAttribute("popularArticleList", xticketService.fetchKnowledgeBasePopularArticle().getData());
        model.addAttribute("latestArticleList", xticketService.fetchKnowledgeBaseLatestArticle().getData());
        model.addAttribute("popularTagList", xticketService.fetchKnowledgeBasePopularTag().getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "knowledgebase";
    }

    @GetMapping("/knowledge-base/list")
    @Secured("ROLE_KNOWLEDGE_BASE")
    public String knowledgeBase(@RequestParam("seid") String seid, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal, Model model) {
        XTicketPayload requestPayload = new XTicketPayload();
        requestPayload.setEmail(principal.getName());
        model.addAttribute("dataList", xticketService.fetchKnowledgeBaseContentUsingCategory(seid));
        model.addAttribute("popularArticleList", xticketService.fetchKnowledgeBasePopularArticle().getData());
        model.addAttribute("latestArticleList", xticketService.fetchKnowledgeBaseLatestArticle().getData());
        model.addAttribute("popularTagList", xticketService.fetchKnowledgeBasePopularTag().getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "knowledgebaselist";
    }

    @GetMapping("/knowledge-base/list/category")
    @Secured("ROLE_KNOWLEDGE_BASE")
    public String knowledgeBaseContent(@RequestParam("seid") String seid, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal, Model model) {
        XTicketPayload requestPayload = new XTicketPayload();
        requestPayload.setEmail(principal.getName());
        model.addAttribute("dataList", xticketService.fetchKnowledgeBaseContent(seid));
        model.addAttribute("popularArticleList", xticketService.fetchKnowledgeBasePopularArticle().getData());
        model.addAttribute("latestArticleList", xticketService.fetchKnowledgeBaseLatestArticle().getData());
        model.addAttribute("popularTagList", xticketService.fetchKnowledgeBasePopularTag().getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "knowledgebasefullcontent";
    }

    @GetMapping("/knowledge-base/category")
    @Secured("ROLE_KNOWLEDGE_BASE_SETUP")
    public String knowledgeBaseCategory(Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("recordCount", xticketService.fetchKnowledgeBaseCategory().getData().size());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "knowledgebasecategory";
    }

    @PostMapping("/knowledge-base/category/create")
    public String knowledgeBaseCategory(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.createKnowledgeBaseCategory(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/knowledge-base/category";
        }
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "knowledgebasecategory";
    }

    @GetMapping("/knowledge-base/category/edit")
    @Secured("ROLE_KNOWLEDGE_BASE_SETUP")
    public String knowledgeBaseCategory(@RequestParam("seid") String id, Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchKnowledgeBaseCategory(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "error";
            return "redirect:/knowledge-base/category/list";
        }
        model.addAttribute("ticketPayload", response);
        model.addAttribute("recordCount", xticketService.fetchKnowledgeBaseCategory().getData().size());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "knowledgebasecategory";
    }

    @GetMapping("/knowledge-base/category/list")
    @Secured("ROLE_KNOWLEDGE_BASE_SETUP")
    public String knowledgeBaseCategory(Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchKnowledgeBaseCategory();
        model.addAttribute("dataList", response.getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "knowledgebasecategorylist";
    }

    @GetMapping("/knowledge-base/category/delete")
    @Secured("ROLE_KNOWLEDGE_BASE_SETUP")
    public String deleteKnowledgeBaseCategory(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.deleteKnowledgeBaseCategory(seid, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/knowledge-base/category/list";
    }

    @GetMapping("/knowledge-base/content")
    @Secured("ROLE_KNOWLEDGE_BASE_SETUP")
    public String knowledgeBaseContent(Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("recordCount", xticketService.fetchKnowledgeBaseContent().getData().size());
        model.addAttribute("knowledgeBaseCategoryList", xticketService.fetchKnowledgeBaseCategory().getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "knowledgebasecontent";
    }

    @PostMapping("/knowledge-base/content/create")
    public String knowledgeBaseContent(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.createKnowledgeBaseContent(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/knowledge-base/content";
        }
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("recordCount", xticketService.fetchKnowledgeBaseContent().getData().size());
        model.addAttribute("knowledgeBaseCategoryList", xticketService.fetchKnowledgeBaseCategory().getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "knowledgebasecontent";
    }

    @GetMapping("/knowledge-base/content/edit")
    @Secured("ROLE_KNOWLEDGE_BASE_SETUP")
    public String knowledgeBaseContent(@RequestParam("seid") String id, Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchKnowledgeBaseContent(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "error";
            return "redirect:/knowledge-base/content/list";
        }
        model.addAttribute("ticketPayload", response);
        model.addAttribute("knowledgeBaseCategoryList", xticketService.fetchKnowledgeBaseCategory().getData());
        model.addAttribute("recordCount", xticketService.fetchKnowledgeBaseContent().getData().size());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "knowledgebasecontent";
    }

    @GetMapping("/knowledge-base/content/list")
    @Secured("ROLE_KNOWLEDGE_BASE_SETUP")
    public String knowledgeBaseContent(Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchKnowledgeBaseContent();
        model.addAttribute("dataList", response.getData());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "knowledgebasecontentlist";
    }

    @GetMapping("/knowledge-base/content/delete")
    @Secured("ROLE_KNOWLEDGE_BASE_SETUP")
    public String deleteKnowledgeBaseContent(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.deleteKnowledgeBaseContent(seid, principal.getName());
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/knowledge-base/content/list";
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

    @RequestMapping("/error")
    public String handleError(HttpServletRequest httpRequest) {
        Object status = httpRequest.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "404";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                return "500";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                return "403";
            }
        }
        return "error";
    }

    @GetMapping("/contact-us")
    public String contactUs(Model model, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "contactus";
    }

    @PostMapping("/contact-us/create")
    public String contactUs(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.createContactUs(requestPayload);
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/contact-us";
    }

    @PostMapping("/knowledge-base/search")
    public String searchKnowledgeBase(@ModelAttribute("xticketPayload") XTicketPayload requestPayload, HttpSession httpSession, Principal principal, Model model) {
        XTicketPayload response = xticketService.searchKnowledgeBaseContent(requestPayload.getKeyword());
        model.addAttribute("dataList", response.getData());
        model.addAttribute("popularArticleList", xticketService.fetchKnowledgeBasePopularArticle().getData());
        model.addAttribute("latestArticleList", xticketService.fetchKnowledgeBaseLatestArticle().getData());
        model.addAttribute("popularTagList", xticketService.fetchKnowledgeBasePopularTag().getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "knowledgebasesearch";
    }

    @GetMapping("/notification")
    @Secured("ROLE_ADD_PUSH_NOTIFICATION")
    public String pushNotification(Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        model.addAttribute("ticketPayload", new XTicketPayload());
        model.addAttribute("recordCount", xticketService.fetchPushNotification().getData().size());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "notification";
    }

    @PostMapping("/notification/create")
    public String pushNotification(@ModelAttribute("ticketPayload") XTicketPayload requestPayload, HttpSession session, Principal principal, Model model) {
        XTicketPayload response = xticketService.createPushNotification(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "success";
            return "redirect:/notification";
        }
        model.addAttribute("ticketPayload", requestPayload);
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "notification";
    }

    @GetMapping("/notification/edit")
    @Secured("ROLE_UPDATE_PUSH_NOTIFICATION")
    public String pushNotification(@RequestParam("seid") String id, Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchPushNotification(id, false);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "error";
            return "redirect:/notification/list";
        }
        model.addAttribute("ticketPayload", response);
        model.addAttribute("recordCount", xticketService.fetchPushNotification().getData().size());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "notification";
    }

    @GetMapping("/notification/batch/edit")
    @Secured("ROLE_UPDATE_PUSH_NOTIFICATION")
    public String batchUpdatePushNotification(@RequestParam("seid") String id, Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchPushNotification(id, true);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            alertMessageType = "error";
            return "redirect:/notification/list";
        }
        model.addAttribute("ticketPayload", response);
        model.addAttribute("recordCount", xticketService.fetchPushNotification().getData().size());
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "success");
        resetAlertMessage();
        return "notification";
    }

    @GetMapping("/notification/list")
    @Secured("ROLE_LIST_PUSH_NOTIFICATION")
    public String pushNotification(Model model, Principal principal) {
        XTicketPayload response = xticketService.fetchPushNotification();
        model.addAttribute("dataList", response.getData());
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        model.addAttribute("notification", xticketService.fetchPushNotificationByUser(principal.getName()).getData());
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("alertMessageType", alertMessageType);
        resetAlertMessage();
        return "notificationlist";
    }

    @GetMapping("/notification/delete")
    @Secured("ROLE_DELETE_PUSH_NOTIFICATION")
    public String deletePushNotification(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.deletePushNotification(seid, principal.getName(), false);
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/notification/list";
    }

    @GetMapping("/notification/batch/delete")
    @Secured("ROLE_DELETE_PUSH_NOTIFICATION")
    public String batchDeletePushNotification(@RequestParam("seid") String seid, Model model, Principal principal) {
        XTicketPayload response = xticketService.deletePushNotification(seid, principal.getName(), true);
        alertMessage = response.getResponseMessage();
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        return "redirect:/notification/list";
    }

    @GetMapping("/notification/self/delete")
    @Secured("ROLE_DELETE_PUSH_NOTIFICATION")
    public String deletePushNotificationByUser(@RequestParam("seid") String seid, Model model, Principal principal, HttpServletRequest httpRequest) {
        XTicketPayload response = xticketService.deletePushNotification(seid, principal.getName(), false);
        alertMessage = "Notification Deleted";
        alertMessageType = response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) ? "success" : "error";
        String requestUri = httpRequest.getHeader("Referer");
        return "redirect:" + requestUri;
    }

    private void resetAlertMessage() {
        alertMessage = "";
        alertMessageType = "";
    }
}
