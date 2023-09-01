package com.ngxgroup.xpolicy.controller;

import com.ngxgroup.xpolicy.service.GenericService;
import com.google.gson.Gson;
import com.ngxgroup.xpolicy.constant.ResponseCodes;
import com.ngxgroup.xpolicy.model.AppUser;
import com.ngxgroup.xpolicy.model.Department;
import com.ngxgroup.xpolicy.model.Division;
import com.ngxgroup.xpolicy.model.GroupRoles;
import com.ngxgroup.xpolicy.model.Policy;
import com.ngxgroup.xpolicy.model.PolicyReview;
import com.ngxgroup.xpolicy.model.PolicyTemp;
import com.ngxgroup.xpolicy.payload.LoginPayload;
import com.ngxgroup.xpolicy.payload.XPolicyPayload;
import com.ngxgroup.xpolicy.service.XPolicyService;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Brian A. Okon okon.brian@gmail.com
 */
@Controller
public class HomeController {

    @Autowired
    GenericService genericService;
    @Autowired
    XPolicyService xpolicyService;
    @Autowired
    MessageSource messageSource;
    @Autowired
    Gson gson;
    @Value("${xpolicy.download.policy.base.dir}")
    private String policyBaseDir;
    private static final Logger LOGGER = Logger.getLogger(HomeController.class.getName());
    private String alertMessage = "";

    @GetMapping("/")
    public String home(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        model.addAttribute("loginPayload", new LoginPayload());
        model.addAttribute("xpolicyPayload", new XPolicyPayload());
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "signin";
    }

    @PostMapping("/account/signin")
    public String login(@ModelAttribute("loginPayload") LoginPayload requestPayload, HttpSession session, HttpServletRequest httpRequest, Model model) {
        XPolicyPayload response = xpolicyService.processSignin(requestPayload);
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            List<SimpleGrantedAuthority> newAuthorities = new ArrayList<>();
            List<GroupRoles> roles = xpolicyService.getUserRoles(requestPayload.getEmail());
            if (roles == null) {
                requestPayload.setOtp1("");
                requestPayload.setOtp2("");
                requestPayload.setOtp3("");
                requestPayload.setOtp4("");
                requestPayload.setOtp5("");
                requestPayload.setOtp6("");
                model.addAttribute("loginPayload", requestPayload);
                model.addAttribute("xpolicyPayload", new XPolicyPayload());
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
        requestPayload.setOtp1("");
        requestPayload.setOtp2("");
        requestPayload.setOtp3("");
        requestPayload.setOtp4("");
        requestPayload.setOtp5("");
        requestPayload.setOtp6("");
        model.addAttribute("loginPayload", requestPayload);
        model.addAttribute("xpolicyPayload", new XPolicyPayload());
        model.addAttribute("alertMessage", response.getResponseMessage());
        model.addAttribute("alertMessageType", "error");
        resetAlertMessage();
        return "signin";
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
        int[] stats = xpolicyService.fetchStatistics();
        model.addAttribute("policyStats", stats[1]);
        model.addAttribute("sopStats", stats[2]);
        model.addAttribute("charterStats", stats[3]);
        model.addAttribute("companyStats", stats[0]);
        model.addAttribute("frameworkStats", stats[4]);
        model.addAttribute("procedureStats", stats[5]);
        model.addAttribute("formStats", stats[6]);
        model.addAttribute("standardStats", stats[7]);
        model.addAttribute("scheduleStats", stats[8]);
        model.addAttribute("xpolicyPayload", new XPolicyPayload());
        model.addAttribute("companyList", xpolicyService.getCompanyList());
        model.addAttribute("mustReadPolicy", xpolicyService.getMustReadPolicy());
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("notification", xpolicyService.getNotifications(principal.getName()));
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "dashboard";
    }

    @PostMapping("/policy/")
    public String policy(@ModelAttribute("xpolicyPayload") XPolicyPayload xpolicyPayload, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal, Model model) {
        List<Policy> response = xpolicyService.processPolicy(xpolicyPayload, principal.getName());
        model.addAttribute("xpolicyPayload", new XPolicyPayload());
        model.addAttribute("policyList", response);
        model.addAttribute("companyList", xpolicyService.getCompanyList());
        model.addAttribute("policyType", xpolicyPayload.getPolicyType());
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "policy";
    }

    @GetMapping("/policy/details/{id}")
    @ResponseBody
    public Policy policyDetails(@PathVariable("id") String id, Model model, HttpServletRequest request) {
        Policy response = xpolicyService.processPolicyUsingId(id);
        return response;
    }

    @GetMapping("/division/fetch/{company}")
    @ResponseBody
    public List<Division> companyDivision(@PathVariable("company") String company, Model model, HttpServletRequest request) {
        List<Division> response = xpolicyService.processCompanyDivisionUsingId(company);
        return response;
    }

    @GetMapping("/department/fetch/{division}")
    @ResponseBody
    public List<Department> divisionDepartment(@PathVariable("division") String division, Model model, HttpServletRequest request) {
        List<Department> response = xpolicyService.processDivisionDepartmentsUsingId(division);
        return response;
    }

    @PostMapping("/account/qrcode")
    public String qrCode(@ModelAttribute("xpolicyPayload") XPolicyPayload requestPayload, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Exception {
        XPolicyPayload response = xpolicyService.generateQRCode(requestPayload);
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            requestPayload.setQrCodeImage(response.getQrCodeImage());
            model.addAttribute("xpolicyPayload", requestPayload);
            resetAlertMessage();
            return "authenticator";
        }
        alertMessage = response.getResponseMessage();
        return "redirect:/";
    }

    @GetMapping("/terms")
    public String terms(HttpServletRequest request, HttpServletResponse response, Principal principal, Model model) {
        model.addAttribute("xpolicyPayload", new XPolicyPayload());
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "terms";
    }

    @GetMapping("/privacy")
    public String privacy(HttpServletRequest request, HttpServletResponse response, Principal principal, Model model) {
        model.addAttribute("xpolicyPayload", new XPolicyPayload());
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "privacy";
    }

    @GetMapping("/policy/download/{fileName:.+}")
    public void downloadPolicy(@PathVariable("fileName") String fileName, Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) throws IOException {
        String destinationDirectory = policyBaseDir + "/";
        File file = new File(destinationDirectory + fileName + ".pdf");
        if (file.exists()) {
            //get the mimetype
            String mimeType = URLConnection.guessContentTypeFromName(file.getName());
            if (mimeType == null) {
                //unknown mimetype so set the mimetype to application/octet-stream
                mimeType = "application/octet-stream";
            }

            httpResponse.setContentType(mimeType);
            httpResponse.setHeader("Content-Disposition", String.format("inline; filename=\"" + file.getName() + "\""));
            httpResponse.setContentLength((int) file.length());

            InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
            FileCopyUtils.copy(inputStream, httpResponse.getOutputStream());
        }

        //Update policy read
        xpolicyService.processPolicyRead(fileName, principal.getName());
    }

    @GetMapping("/admin/policy")
    public String managePolicy(Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        List<Policy> response = xpolicyService.getPolicies();
        model.addAttribute("xpolicyPayload", new XPolicyPayload());
        model.addAttribute("policyList", response);
        model.addAttribute("companyList", xpolicyService.getCompanyList());
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "managepolicy";
    }

    @GetMapping("/admin/policy/add")
    public String addPolicy(Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        List<Policy> response = xpolicyService.getPolicies();
        model.addAttribute("xpolicyPayload", new XPolicyPayload());
        model.addAttribute("policyList", response);
        model.addAttribute("companyList", xpolicyService.getCompanyList());
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("refererPage", "Policy Update");
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "addpolicy";
    }

    @PostMapping("/admin/policy/add/")
    public String managePolicy(@ModelAttribute("xpolicyPayload") XPolicyPayload requestPayload, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model, Principal principal) throws Exception {
        XPolicyPayload response = xpolicyService.processCreatePolicy(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            return "redirect:/admin/policy/add";
        }
        List<Policy> policyList = xpolicyService.getPolicies();
        model.addAttribute("xpolicyPayload", requestPayload);
        model.addAttribute("policyList", policyList);
        model.addAttribute("companyList", xpolicyService.getCompanyList());
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("alertMessage", response.getResponseMessage());
        return "addpolicy";
    }

    @GetMapping("/admin/policy/edit/{id}")
    public String editPolicy(@PathVariable("id") String id, Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        XPolicyPayload response = xpolicyService.getPolicyUsingId(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            return "redirect:/admin/policy";
        }
        model.addAttribute("xpolicyPayload", response);
        model.addAttribute("companyList", xpolicyService.getCompanyList());
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("companyId", response.getCompany());
        model.addAttribute("divisionId", response.getDivision());
        model.addAttribute("departmentId", response.getDepartment());
        model.addAttribute("refererPage", "Policy Update");
        return "addpolicy";
    }

    @GetMapping("/admin/policy/delete/{id}")
    public String deletePolicy(@PathVariable("id") String id, Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        XPolicyPayload response = xpolicyService.processDeletePolicy(id, principal.getName());
        alertMessage = response.getResponseMessage();
        return "redirect:/admin/policy";
    }

    @GetMapping("/admin/policy/pending")
    public String pendingPolicy(Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        List<PolicyTemp> response = xpolicyService.getPendingPolicies();
        model.addAttribute("xpolicyPayload", new XPolicyPayload());
        model.addAttribute("policyList", response);
        model.addAttribute("companyList", xpolicyService.getCompanyList());
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "pendingpolicy";
    }

    @GetMapping("/admin/policy/approve/{id}")
    public String approvePolicy(@PathVariable("id") String id, Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        XPolicyPayload response = xpolicyService.processApprovePolicy(id, principal.getName());
        alertMessage = response.getResponseMessage();
        return "redirect:/admin/policy/pending";
    }

    @GetMapping("/admin/policy/decline/{id}")
    public String declinePolicy(@PathVariable("id") String id, Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        XPolicyPayload response = xpolicyService.processDeclinePolicy(id, principal.getName());
        alertMessage = response.getResponseMessage();
        return "redirect:/admin/policy/pending";
    }

    @GetMapping("/admin/policy/review")
    public String managePolicyReview(Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        List<PolicyReview> response = xpolicyService.getPolicyAllReview();
        model.addAttribute("xpolicyPayload", new XPolicyPayload());
        model.addAttribute("policyList", response);
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "managepolicyreview";
    }

    @GetMapping("/admin/policy/review/add")
    public String addPolicyReview(Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        List<Policy> response = xpolicyService.getPolicies();
        model.addAttribute("xpolicyPayload", new XPolicyPayload());
        model.addAttribute("policyList", response);
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "addpolicyreview";
    }

    @GetMapping("/admin/policy/review/{id}")
    public String reviewPolicy(@PathVariable("id") String id, Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        XPolicyPayload response = xpolicyService.getPolicyUsingId(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            return "redirect:/admin/policy";
        }
        List<Policy> policyList = xpolicyService.getPolicies();
        model.addAttribute("policyList", policyList);
        model.addAttribute("xpolicyPayload", response);
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("refererPage", "Policy Update");
        return "addpolicyreview";
    }

    @PostMapping("/admin/policy/review/add/")
    public String reviewPolicy(@ModelAttribute("xpolicyPayload") XPolicyPayload requestPayload, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model, Principal principal) throws Exception {
        XPolicyPayload response = xpolicyService.processReviewPolicy(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            return "redirect:/admin/policy/review/add";
        }
        List<Policy> policyList = xpolicyService.getPolicies();
        model.addAttribute("xpolicyPayload", requestPayload);
        model.addAttribute("policyList", policyList);
        model.addAttribute("companyList", xpolicyService.getCompanyList());
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("alertMessage", response.getResponseMessage());
        return "addpolicyreview";
    }

    @GetMapping("/admin/users")
    public String manageUsers(Model model, HttpServletRequest httpRequest, HttpServletResponse httResponse, Principal principal) {
        List<AppUser> response = xpolicyService.getAppUsers();
        model.addAttribute("userList", response);
        model.addAttribute("xpolicyPayload", new XPolicyPayload());
        model.addAttribute("companyList", xpolicyService.getCompanyList());
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "manageuser";
    }

    @GetMapping("/admin/users/add")
    public String addUsers(Model model, HttpServletRequest httpRequest, HttpServletResponse httResponse, Principal principal) {
        List<AppUser> response = xpolicyService.getAppUsers();
        model.addAttribute("userList", response);
        model.addAttribute("xpolicyPayload", new XPolicyPayload());
        model.addAttribute("companyList", xpolicyService.getCompanyList());
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("alertMessage", alertMessage);
        model.addAttribute("refererPage", "User Update");
        resetAlertMessage();
        return "adduser";
    }

    @PostMapping("/admin/users/add/")
    public String manageUsers(@ModelAttribute("xpolicyPayload") XPolicyPayload requestPayload, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model, Principal principal) throws Exception {
        XPolicyPayload response = xpolicyService.processCreateUser(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            return "redirect:/admin/users";
        }
        List<AppUser> userList = xpolicyService.getUsers();
        model.addAttribute("xpolicyPayload", requestPayload);
        model.addAttribute("userList", userList);
        model.addAttribute("companyList", xpolicyService.getCompanyList());
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("alertMessage", response.getResponseMessage());
        return "adduser";
    }

    @GetMapping("/admin/users/edit/{id}")
    public String editUsers(@PathVariable("id") String id, Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        XPolicyPayload response = xpolicyService.getAppUserUsingId(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            return "redirect:/admin/users";
        }
        model.addAttribute("xpolicyPayload", response);
        model.addAttribute("companyList", xpolicyService.getCompanyList());
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("refererPage", "User Update");
        return "adduser";
    }

    @GetMapping("/admin/users/delete/{id}")
    public String deleteUsers(@PathVariable("id") String id, Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        XPolicyPayload response = xpolicyService.processDeleteUser(id, principal.getName());
        alertMessage = response.getResponseMessage();
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/roles")
    public String addRoles(Model model, HttpServletRequest httpRequest, HttpServletResponse httResponse, Principal principal) {
        model.addAttribute("xpolicyPayload", new XPolicyPayload());
        model.addAttribute("permissionPayload", new XPolicyPayload());
        model.addAttribute("roleList", xpolicyService.getRoleGroupList());
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("groupRolesPayload", null);
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "addroles";
    }

    @PostMapping("/admin/roles/")
    public String addRoles(@ModelAttribute("xpolicyPayload") XPolicyPayload requestPayload, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model, Principal principal) throws Exception {
        XPolicyPayload response = xpolicyService.processCreateRoleGroup(requestPayload, principal.getName());
        if (response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            return "redirect:/admin/roles";
        }
        model.addAttribute("xpolicyPayload", requestPayload);
        model.addAttribute("roleList", xpolicyService.getRoleGroupList());
        model.addAttribute("permissionPayload", requestPayload);
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("groupRolesPayload", null);
        model.addAttribute("alertMessage", response.getResponseMessage());
        return "addroles";
    }

    @GetMapping("/admin/roles/edit/{id}")
    public String editRoles(@PathVariable("id") String id, Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        XPolicyPayload response = xpolicyService.getRoleGroupUsingId(id);
        if (!response.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            alertMessage = response.getResponseMessage();
            return "redirect:/admin/roles";
        }
        model.addAttribute("xpolicyPayload", response);
        model.addAttribute("permissionPayload", response);
        model.addAttribute("roleList", xpolicyService.getRoleGroupList());
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("groupRolesPayload", null);
        model.addAttribute("refererPage", "User Update");
        return "addroles";
    }

    @GetMapping("/admin/roles/delete/{id}")
    public String deleteRoles(@PathVariable("id") String id, Model model, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Principal principal) {
        XPolicyPayload response = xpolicyService.processDeleteRoleGroup(id, principal.getName());
        alertMessage = response.getResponseMessage();
        return "redirect:/admin/roles";
    }

    @PostMapping("/admin/roles/permission")
    public String fetchRolePermission(@ModelAttribute("xpolicyPayload") XPolicyPayload requestPayload, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model, Principal principal) throws Exception {
        Object response = xpolicyService.fetchGroupRecord(requestPayload.getGroupName());
        if (response instanceof String) {
            alertMessage = (String) response;
            return "redirect:/admin/roles";
        }
        List<XPolicyPayload> groupRoles = (List<XPolicyPayload>) response;
        model.addAttribute("xpolicyPayload", requestPayload);
        model.addAttribute("permissionPayload", requestPayload);
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("roleList", xpolicyService.getRoleGroupList());
        model.addAttribute("groupRolesPayload", groupRoles);
        return "addroles";
    }

    @PostMapping("/admin/roles/permission/")
    public String processGrouRolesAdd(@ModelAttribute("xpolicyPayload") XPolicyPayload requestPayload, Model model, Principal principal) {
        String response = xpolicyService.updateGroupRoles(requestPayload, principal.getName());
        if (response.equalsIgnoreCase(messageSource.getMessage("appMessages.success.roles", new Object[0], Locale.ENGLISH))) {
            alertMessage = response;
            return "redirect:/admin/roles";
        }

        model.addAttribute("xpolicyPayload", requestPayload);
        model.addAttribute("permissionPayload", requestPayload);
        model.addAttribute("roleList", xpolicyService.getRoleGroupList());
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("groupRolesPayload", null);
        model.addAttribute("alertMessage", response);
        return "addroles";
    }

    @GetMapping("/admin/report")
    public String manageReport(Model model, HttpServletRequest request, HttpServletResponse response, Principal principal) {
        int[] stats = xpolicyService.fetchStatistics();
        model.addAttribute("policyStats", stats[1]);
        model.addAttribute("sopStats", stats[2]);
        model.addAttribute("charterStats", stats[3]);
        model.addAttribute("companyStats", stats[0]);
        model.addAttribute("frameworkStats", stats[4]);
        model.addAttribute("procedureStats", stats[5]);
        model.addAttribute("formStats", stats[6]);
        model.addAttribute("standardStats", stats[7]);
        model.addAttribute("scheduleStats", stats[8]);
        model.addAttribute("loginPayload", new LoginPayload());
        model.addAttribute("xpolicyPayload", new XPolicyPayload());
        model.addAttribute("companyList", xpolicyService.getCompanyList());
        model.addAttribute("policyList", xpolicyService.getPolicies());
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "report";
    }

    @PostMapping("/admin/report/")
    public String report(@ModelAttribute("xpolicyPayload") XPolicyPayload requestPayload, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model, Principal principal) throws Exception {
        XPolicyPayload response = xpolicyService.generateReport(requestPayload);
        if (response.getReportPage().equalsIgnoreCase("Policy")) {
            if (response.getPolicies() == null) {
                int[] stats = xpolicyService.fetchStatistics();
                model.addAttribute("policyStats", stats[1]);
                model.addAttribute("sopStats", stats[2]);
                model.addAttribute("charterStats", stats[3]);
                model.addAttribute("companyStats", stats[0]);
                model.addAttribute("frameworkStats", stats[4]);
                model.addAttribute("procedureStats", stats[5]);
                model.addAttribute("formStats", stats[6]);
                model.addAttribute("standardStats", stats[7]);
                model.addAttribute("scheduleStats", stats[8]);
                model.addAttribute("loginPayload", new LoginPayload());
                model.addAttribute("xpolicyPayload", new XPolicyPayload());
                model.addAttribute("companyList", xpolicyService.getCompanyList());
                model.addAttribute("policyList", xpolicyService.getPolicies());
                model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
                model.addAttribute("alertMessage", response.getResponseMessage());
                resetAlertMessage();
                return "report";
            }
            model.addAttribute("policyList", response.getPolicies());
            model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
            model.addAttribute("alertMessage", alertMessage);
            resetAlertMessage();
            return "policyreport";
        }

        if (response.getReportPage().equalsIgnoreCase("PolicyRead")) {
            if (response.getPolicyRead() == null) {
                int[] stats = xpolicyService.fetchStatistics();
                model.addAttribute("policyStats", stats[1]);
                model.addAttribute("sopStats", stats[2]);
                model.addAttribute("charterStats", stats[3]);
                model.addAttribute("companyStats", stats[0]);
                model.addAttribute("frameworkStats", stats[4]);
                model.addAttribute("procedureStats", stats[5]);
                model.addAttribute("formStats", stats[6]);
                model.addAttribute("standardStats", stats[7]);
                model.addAttribute("scheduleStats", stats[8]);
                model.addAttribute("loginPayload", new LoginPayload());
                model.addAttribute("xpolicyPayload", new XPolicyPayload());
                model.addAttribute("companyList", xpolicyService.getCompanyList());
                model.addAttribute("policyList", xpolicyService.getPolicies());
                model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
                model.addAttribute("alertMessage", response.getResponseMessage());
                resetAlertMessage();
                return "report";
            }
            model.addAttribute("policyList", response.getPolicyRead());
            model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
            model.addAttribute("alertMessage", alertMessage);
            resetAlertMessage();
            return "policyreadreport";
        }

        if (response.getReportPage().equalsIgnoreCase("User")) {
            if (response.getAppUsers() == null) {
                int[] stats = xpolicyService.fetchStatistics();
                model.addAttribute("policyStats", stats[1]);
                model.addAttribute("sopStats", stats[2]);
                model.addAttribute("charterStats", stats[3]);
                model.addAttribute("companyStats", stats[0]);
                model.addAttribute("frameworkStats", stats[4]);
                model.addAttribute("procedureStats", stats[5]);
                model.addAttribute("formStats", stats[6]);
                model.addAttribute("standardStats", stats[7]);
                model.addAttribute("scheduleStats", stats[8]);
                model.addAttribute("loginPayload", new LoginPayload());
                model.addAttribute("xpolicyPayload", new XPolicyPayload());
                model.addAttribute("companyList", xpolicyService.getCompanyList());
                model.addAttribute("policyList", xpolicyService.getPolicies());
                model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
                model.addAttribute("alertMessage", response.getResponseMessage());
                resetAlertMessage();
                return "report";
            }
            model.addAttribute("userList", response.getAppUsers());
            model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
            model.addAttribute("alertMessage", alertMessage);
            resetAlertMessage();
            return "userreport";
        }

        if (response.getReportPage().equalsIgnoreCase("UserActivity")) {
            if (response.getUserActivity() == null) {
                int[] stats = xpolicyService.fetchStatistics();
                model.addAttribute("policyStats", stats[1]);
                model.addAttribute("sopStats", stats[2]);
                model.addAttribute("charterStats", stats[3]);
                model.addAttribute("companyStats", stats[0]);
                model.addAttribute("frameworkStats", stats[4]);
                model.addAttribute("procedureStats", stats[5]);
                model.addAttribute("formStats", stats[6]);
                model.addAttribute("standardStats", stats[7]);
                model.addAttribute("scheduleStats", stats[8]);
                model.addAttribute("loginPayload", new LoginPayload());
                model.addAttribute("xpolicyPayload", new XPolicyPayload());
                model.addAttribute("companyList", xpolicyService.getCompanyList());
                model.addAttribute("policyList", xpolicyService.getPolicies());
                model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
                model.addAttribute("alertMessage", response.getResponseMessage());
                resetAlertMessage();
                return "report";
            }
            model.addAttribute("activityList", response.getUserActivity());
            model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
            model.addAttribute("alertMessage", alertMessage);
            resetAlertMessage();
            return "useractivityreport";
        }
        model.addAttribute("activityList", response.getUserActivity());
        model.addAttribute("principalName", xpolicyService.getPrincipalName(principal.getName()));
        model.addAttribute("alertMessage", alertMessage);
        resetAlertMessage();
        return "useractivityreport";

    }

    @GetMapping("/tfa")
    public String generateTwoFactorDetails() {
        XPolicyPayload response = xpolicyService.generateTwoFactorDetails();
        alertMessage = response.getResponseMessage();
        return "redirect:/";
    }

    private void resetAlertMessage() {
        alertMessage = "";
    }
}
