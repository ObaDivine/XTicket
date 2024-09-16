package com.ngxgroup.xticket.service;

import com.ngxgroup.xticket.payload.XTicketPayload;
import com.google.gson.Gson;
import com.ngxgroup.xticket.constant.ResponseCodes;
import com.ngxgroup.xticket.model.AppRoles;
import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.model.AuditLog;
import com.ngxgroup.xticket.model.AutomatedTicket;
import com.ngxgroup.xticket.model.ContactUs;
import com.ngxgroup.xticket.model.Department;
import com.ngxgroup.xticket.model.DocumentUpload;
import com.ngxgroup.xticket.model.Entities;
import com.ngxgroup.xticket.model.GroupRoles;
import com.ngxgroup.xticket.model.KnowledgeBase;
import com.ngxgroup.xticket.model.KnowledgeBaseCategory;
import com.ngxgroup.xticket.model.PublicHolidays;
import com.ngxgroup.xticket.model.RoleGroups;
import com.ngxgroup.xticket.model.ServiceUnit;
import com.ngxgroup.xticket.model.TicketAgent;
import com.ngxgroup.xticket.model.TicketComment;
import com.ngxgroup.xticket.model.TicketEscalations;
import com.ngxgroup.xticket.model.TicketGroup;
import com.ngxgroup.xticket.model.TicketReassign;
import com.ngxgroup.xticket.model.TicketReopened;
import com.ngxgroup.xticket.model.TicketSla;
import com.ngxgroup.xticket.model.TicketStatus;
import com.ngxgroup.xticket.model.TicketStatusChange;
import com.ngxgroup.xticket.model.TicketType;
import com.ngxgroup.xticket.model.Tickets;
import com.ngxgroup.xticket.payload.LogPayload;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Hashtable;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import com.ngxgroup.xticket.repository.XTicketRepository;
import jakarta.servlet.ServletContext;
import java.io.File;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author briano
 */
@Service
public class XTicketServiceImpl implements XTicketService {

    @Autowired
    ServletContext servletContext;
    @Autowired
    MessageSource messageSource;
    @Autowired
    XTicketRepository xticketRepository;
    @Autowired
    GenericService genericService;
    @Autowired
    Gson gson;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Value("${xticket.password.retry.count}")
    private String passwordRetryCount;
    @Value("${xticket.password.change.days}")
    private int passwordChangeDays;
    @Value("${xticket.password.reset.time}")
    private String passwordResetTime;
    @Value("${xticket.default.email.domain}")
    private String companyEmailDomain;
    @Value("${xticket.adauth.domains}")
    private String adAuthDomain;
    @Value("${xticket.company.name}")
    private String companyName;
    @Value("${xticket.company.email}")
    private String companyEmail;
    @Value("${xticket.host}")
    private String host;
    @Value("${xticket.default.password}")
    private String defaultPassword;
    static final Logger logger = Logger.getLogger(XTicketServiceImpl.class.getName());
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    DateTimeFormatter shortDtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    DateTimeFormatter timeDtf = DateTimeFormatter.ofPattern("HH:mm:ss");
    @Value("${xticket.password.pattern}")
    private String passwordPattern;
    @Value("${xticket.office.open}")
    private int officeOpenHour;
    @Value("${xticket.office.close}")
    private int officeCloseHour;
    @Value("${xticket.slaexpiry.timeleft}")
    private int slaExpiryTimeLeft;
    @Value("${xticket.email.contactus}")
    private String contactUsEmail;
    @Value("${xticket.default.entitycode}")
    private String defaultEntityCode;
    @Value("${xticket.default.entityname}")
    private String defaultEntityName;
    @Value("${xticket.default.departmentcode}")
    private String defaultDepartmentCode;
    @Value("${xticket.default.departmentname}")
    private String defaultDepartmentName;

    @Override
    public XTicketPayload signin(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        var log = new LogPayload();
        try {
            var appUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
            if (appUser == null) {
                String message = messageSource.getMessage("appMessages.user.notexist", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Login");
                log.setUsername(requestPayload.getEmail());
                logger.log(Level.INFO, gson.toJson(log));
                //Log the error
                response.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                response.setResponseMessage(message);
                return response;
            }

            //Check if user is disable
            if (appUser.isLocked() || !appUser.isActivated()) {
                String message = messageSource.getMessage("appMessages.user.disabled", new Object[0], Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Login");
                log.setUsername(requestPayload.getEmail());
                logger.log(Level.INFO, gson.toJson(log));
                response.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                response.setResponseMessage(message);
                return response;
            }

            //Check the reset time
            if (LocalDateTime.now().isBefore(appUser.getResetTime())) {
                String message = messageSource.getMessage("appMessages.reset.on", new Object[]{dtf.format(appUser.getResetTime())}, Locale.ENGLISH);
                response.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                response.setResponseMessage(message);
                return response;
            }

            //Check if the user needs to change password
            if (LocalDate.now().isAfter(appUser.getPasswordChangeDate()) && !appUser.isInternal()) {
                String message = messageSource.getMessage("appMessages.password.expire", new Object[0], Locale.ENGLISH);
                response.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                response.setResponseMessage(message);
                return response;
            }

            //Check if the user is internal or external. Fetch all the domain for AD authentication
            boolean useADAuth = false;
            String[] adDomains = adAuthDomain.split(",");
            if (adDomains != null) {
                for (String s : adDomains) {
                    if (appUser.getEmail().contains(s)) {
                        useADAuth = true;
                    }
                }
            }

            //Uset AD Authentication
            if (useADAuth) {
                String adResponse = authenticateADUser(requestPayload.getEmail(), requestPayload.getPassword(), requestPayload.getEmail().split("@")[1], "");
                if (!adResponse.equalsIgnoreCase("success")) {
                    //Check the fail count
                    if (appUser.getLoginFailCount() == Integer.parseInt(passwordRetryCount)) {
                        appUser.setLoginFailCount(appUser.getLoginFailCount() + 1);
                        appUser.setResetTime(LocalDateTime.now().plusMinutes(Integer.parseInt(passwordResetTime)));
                        xticketRepository.updateAppUser(appUser);

                        String message = messageSource.getMessage("appMessages.user.multiple.attempt", new Object[]{requestPayload.getEmail().trim()}, Locale.ENGLISH);
                        response.setResponseCode(ResponseCodes.MULTIPLE_ATTEMPT.getResponseCode());
                        response.setResponseMessage(message);
                        return response;
                    }

                    //Login failed. Set fail count
                    appUser.setLoginFailCount(appUser.getLoginFailCount() + 1);
                    xticketRepository.updateAppUser(appUser);

                    //Determine the error from the AD. Error code 49 is invalid credentials
                    String message = "";
                    if (adResponse.contains("error code 49")) {
                        message = messageSource.getMessage("appMessages.login.failed", new Object[0], Locale.ENGLISH);
                    } else {
                        message = messageSource.getMessage("appMessages.connection.failed", new Object[]{adResponse}, Locale.ENGLISH);
                    }
                    response.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                    response.setResponseMessage(message);

                    //Log the response
                    genericService.logResponse(requestPayload.getEmail(), requestPayload.getEmail(), "Login", "Login", "Failed Login", "", "");
                    return response;
                }

                //Clear failed login
                appUser.setLoginFailCount(0);
                appUser.setLastLogin(LocalDateTime.now());
                appUser.setOnline(true);
                xticketRepository.updateAppUser(appUser);

                String message = messageSource.getMessage("appMessages.success.signin", new Object[0], Locale.ENGLISH);
                response.setResponseMessage(message);
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setAgent(appUser.isAgent());
                //Log the response
                genericService.logResponse(requestPayload.getEmail(), requestPayload.getEmail(), "Login", "Login", "Successful Login", "", "");
                return response;
            }

            //Check authentication
            Boolean passwordMatch = passwordEncoder.matches(requestPayload.getPassword(), appUser.getPassword());
            if (!passwordMatch) {
                //Check the fail count
                if (appUser.getLoginFailCount() == Integer.parseInt(passwordRetryCount)) {
                    appUser.setLoginFailCount(appUser.getLoginFailCount() + 1);
                    appUser.setResetTime(LocalDateTime.now().plusMinutes(Integer.parseInt(passwordResetTime)));
                    xticketRepository.updateAppUser(appUser);

                    String message = messageSource.getMessage("appMessages.user.multiple.attempt", new Object[]{requestPayload.getEmail().trim()}, Locale.ENGLISH);
                    response.setResponseCode(ResponseCodes.MULTIPLE_ATTEMPT.getResponseCode());
                    response.setResponseMessage(message);
                    return response;
                }

                //Login failed. Set fail count
                appUser.setLoginFailCount(appUser.getLoginFailCount() + 1);
                xticketRepository.updateAppUser(appUser);

                String message = messageSource.getMessage("appMessages.login.failed", new Object[0], Locale.ENGLISH);
                response.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                response.setResponseMessage(message);
                //Log the response
                genericService.logResponse(requestPayload.getEmail(), requestPayload.getEmail(), "Login", "Login", "Failed Login", "", "");
                return response;
            }

            //Clear failed login
            appUser.setLoginFailCount(0);
            appUser.setLastLogin(LocalDateTime.now());
            appUser.setOnline(true);
            xticketRepository.updateAppUser(appUser);

            String message = messageSource.getMessage("appMessages.success.signin", new Object[0], Locale.ENGLISH);
            response.setResponseMessage(message);
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setAgent(appUser.isAgent());
            //Log the response
            genericService.logResponse(requestPayload.getEmail(), requestPayload.getEmail(), "Login", "Login", "Successful Login", "", "");
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            return response;
        }
    }

    public String authenticateADUser(String username, String password, String domainName, String serverName) throws NamingException {
        var log = new LogPayload();
        try {
            Hashtable<Object, Object> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://" + domainName + ":389");
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.REFERRAL, "follow");
            env.put(Context.SECURITY_PRINCIPAL, username);
            env.put(Context.SECURITY_CREDENTIALS, password);

            // attempt to authenticate
            DirContext ctx = new InitialDirContext(env);
            ctx.close();
            return "Success";
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Login");
            log.setUsername(username);
            logger.log(Level.INFO, gson.toJson(log));
            return ex.getMessage();
        }
    }

    @Override
    public XTicketPayload signup(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            //Check if the username (Email) already exist
            var appUserByEmail = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
            if (appUserByEmail != null) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.exist", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH));
                return response;
            }

            //Check if the user is registered with the mobile number
            var appUserByMobile = xticketRepository.getAppUserUsingMobileNumber(requestPayload.getMobileNumber());
            if (appUserByMobile != null) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.exist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                return response;
            }

            //Check if the user is internal and set the password. Fetch all the domain for AD authentication
            boolean useADAuth = false;
            String[] adDomains = adAuthDomain.split(",");
            if (adDomains != null) {
                for (String s : adDomains) {
                    if (requestPayload.getEmail().contains(s)) {
                        useADAuth = true;
                    }
                }
            }

            if (useADAuth) {
                requestPayload.setPassword(defaultPassword);
                requestPayload.setConfirmPassword(defaultPassword);
            }

            //Check if the password and confirm password
            if (!requestPayload.getPassword().equalsIgnoreCase(requestPayload.getConfirmPassword())) {
                response.setResponseCode(ResponseCodes.PASSWORD_PIN_MISMATCH.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.password.mismatch", new Object[0], Locale.ENGLISH));
                return response;
            }

            //Check the password complexity. This is for external users
            Pattern pattern = Pattern.compile(passwordPattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(requestPayload.getPassword());
            if (!matcher.matches()) {
                response.setResponseCode(ResponseCodes.PASSWORD_PIN_MISMATCH.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.password.complexity", new Object[0], Locale.ENGLISH));
                return response;
            }

            //Fetch the default role group for initial setup
            RoleGroups roleGroup = xticketRepository.getRoleGroupUsingGroupName("DEFAULT");

            //Get the entity
            Entities entity = null;
            Department department = null;
            if (!useADAuth) {
                //This is external. Set the entity to the default
                department = xticketRepository.getDepartmentUsingCode(defaultDepartmentCode);
                entity = xticketRepository.getEntitiesUsingCode(defaultEntityCode);
            } else {
                department = xticketRepository.getDepartmentUsingCode(requestPayload.getDepartmentCode());
                entity = department.getEntity();
            }

            //Create new Record
            AppUser newUser = new AppUser();
            String activationId = UUID.randomUUID().toString().replaceAll("-", "");
            newUser.setActivationId(activationId);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setCreatedBy(requestPayload.getEmail());
            newUser.setDepartment(department);
            newUser.setEmail(requestPayload.getEmail());
            newUser.setEntity(entity);
            newUser.setActivated(false);
            newUser.setGender(requestPayload.getGender());
            newUser.setInternal(requestPayload.getEmail().contains(companyEmailDomain));
            newUser.setLastLogin(null);
            newUser.setLastName(requestPayload.getLastName());
            newUser.setLocked(true);
            newUser.setLoginFailCount(0);
            newUser.setMobileNumber(requestPayload.getMobileNumber());
            newUser.setOtherName(requestPayload.getOtherName());
            newUser.setPassword(passwordEncoder.encode(useADAuth ? defaultPassword : requestPayload.getPassword()));
            newUser.setPasswordChangeDate(LocalDate.now().plusDays(passwordChangeDays));
            newUser.setResetTime(LocalDateTime.now().minusMinutes(1));
            newUser.setRole(roleGroup == null ? null : roleGroup);
            newUser.setAgent(false);
            newUser.setUpdatedAt(LocalDateTime.now());
            newUser.setUpdatedBy(requestPayload.getEmail());
            xticketRepository.createAppUser(newUser);

            //Send profile activation email
            XTicketPayload mailPayload = new XTicketPayload();
            mailPayload.setRecipientEmail(requestPayload.getEmail());
            mailPayload.setEmailSubject("Profile Activation");
            mailPayload.setCarbonCopyEmail("");
            String message = "<h4>Dear " + requestPayload.getLastName() + "</h4>\n"
                    + "<p>We appreciate your interest in " + companyName + " X-TICKET, our award-winning ticketing platform and help desk.</p>\n"
                    + "<p>To activate your profile, please click on the activation button below</p>\n"
                    + "<p><a href=\"" + host + "/xticket/signup/activate?id=" + activationId + "\"><button type='button' style='display:inline-block;background-color:#ee4f1e;padding:5px; width:200px; color:#ffffff; text-align:center; border-radius:20px; border-color:white;'>Activate Profile</button></a></p>"
                    + "<p>For support and enquiries, email: " + companyEmail + ".</p>\n"
                    + "<p>Once again, thanks for choosing " + companyName + "!.</p>\n"
                    + "<p>Best wishes,</p>"
                    + "<p>" + companyName + "</p>";

            mailPayload.setEmailBody(message);
            genericService.sendEmail(mailPayload, requestPayload.getEmail());
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.user", new Object[0], Locale.ENGLISH));

            //Log the response
            StringBuilder newValue = new StringBuilder();
            newValue.append("Last Name:").append(requestPayload.getLastName()).append("Other Names:").append(requestPayload.getOtherName())
                    .append("Gender:").append(requestPayload.getGender()).append("Email:").append(requestPayload.getEmail())
                    .append("Mobile Number:").append(requestPayload.getMobileNumber());
            genericService.logResponse(requestPayload.getEmail(), newUser.getId(), "Sign Up", "Sign Up", "Successful Sign Up", "", newValue.toString());
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            return response;
        }
    }

    @Override
    public XTicketPayload signUpActivation(String activationId) {
        var response = new XTicketPayload();
        try {
            //Check if a user exist using the id
            var appUser = xticketRepository.getAppUserUsingActivationId(activationId);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{activationId}, Locale.ENGLISH));
                return response;
            }

            //Check if the user is activated already
            if (appUser.isActivated()) {
                response.setResponseCode(ResponseCodes.CUSTOMER_BOARDED.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.activated", new Object[0], Locale.ENGLISH));
                return response;
            }

            //Update the user profile
            appUser.setActivated(true);
            appUser.setLocked(false);
            xticketRepository.updateAppUser(appUser);
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.activated", new Object[0], Locale.ENGLISH));

            //Log the response
            genericService.logResponse(appUser.getEmail(), activationId, "Sign Up", "Activation", "Successful Profile Activation", "", "");
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            return response;
        }
    }

    @Override
    public XTicketPayload fetchProfile(String principal) {
        var response = new XTicketPayload();
        try {
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            BeanUtils.copyProperties(appUser, response);
            response.setPasswordChangeDate(appUser.getPasswordChangeDate().toString());
            response.setCreatedAt(dtf.format(appUser.getCreatedAt()));
            response.setCreatedBy(appUser.getCreatedBy());
            response.setResetTime(appUser.getResetTime().toString());
            String userType = "";
            if (!appUser.isInternal()) {
                userType = "User";
            } else {
                if (appUser.isAgent()) {
                    userType = "Agent";
                } else {
                    if (appUser.getRole().getGroupName().equalsIgnoreCase("DEFAULT")) {
                        userType = "User";
                    } else {
                        userType = "Admin";
                    }
                }
            }
            response.setUserType(userType);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            return response;
        }
    }

    @Override
    public XTicketPayload changePassword(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        var log = new LogPayload();
        try {
            //Check if user exist
            var appUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
            if (appUser == null) {
                String message = messageSource.getMessage("appMessages.user.notexist", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Change Password");
                log.setUsername(requestPayload.getEmail());
                logger.log(Level.INFO, gson.toJson(log));
                //Log the error
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(message);
                return response;
            }

            //Check if user is disable
            if (appUser.isLocked() || !appUser.isActivated()) {
                String message = messageSource.getMessage("appMessages.user.disabled", new Object[0], Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Change Password");
                log.setUsername(requestPayload.getEmail());
                logger.log(Level.INFO, gson.toJson(log));
                response.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                response.setResponseMessage(message);
                return response;
            }

            //Check authentication
            Boolean passwordMatch = passwordEncoder.matches(requestPayload.getPassword(), appUser.getPassword());
            if (Boolean.FALSE.equals(passwordMatch)) {
                //Check the fail count
                if (appUser.getLoginFailCount() == Integer.parseInt(passwordRetryCount)) {
                    appUser.setLoginFailCount(appUser.getLoginFailCount() + 1);
                    appUser.setResetTime(LocalDateTime.now().plusMinutes(Integer.parseInt(passwordResetTime)));
                    xticketRepository.updateAppUser(appUser);

                    String message = messageSource.getMessage("appMessages.user.multiple.attempt", new Object[]{requestPayload.getEmail().trim()}, Locale.ENGLISH);
                    response.setResponseCode(ResponseCodes.MULTIPLE_ATTEMPT.getResponseCode());
                    response.setResponseMessage(message);
                    return response;
                }

                //Login failed. Set fail count
                appUser.setLoginFailCount(appUser.getLoginFailCount() + 1);
                xticketRepository.updateAppUser(appUser);

                String message = messageSource.getMessage("appMessages.password.invalid", new Object[0], Locale.ENGLISH);
                response.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                response.setResponseMessage(message);
                return response;
            }

            //Check if the password and confirm password
            if (!requestPayload.getNewPassword().equalsIgnoreCase(requestPayload.getConfirmPassword())) {
                response.setResponseCode(ResponseCodes.PASSWORD_PIN_MISMATCH.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.password.mismatch", new Object[0], Locale.ENGLISH));
                return response;
            }

            //Check the password complexity. This is for external users
            Pattern pattern = Pattern.compile(passwordPattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(requestPayload.getNewPassword());
            if (!matcher.matches()) {
                response.setResponseCode(ResponseCodes.PASSWORD_PIN_MISMATCH.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.password.complexity", new Object[0], Locale.ENGLISH));
                return response;
            }

            appUser.setPassword(passwordEncoder.encode(requestPayload.getNewPassword()));
            appUser.setPasswordChangeDate(LocalDate.now().plusDays(passwordChangeDays));
            xticketRepository.updateAppUser(appUser);
            BeanUtils.copyProperties(appUser, response);
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.password", new Object[0], Locale.ENGLISH));

            //Log the response
            genericService.logResponse(requestPayload.getEmail(), appUser.getId(), "Change Password", "Change Password", "Change Password", "", "");
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            return response;
        }
    }

    @Override
    public XTicketPayload forgotPassword(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        var log = new LogPayload();
        try {
            //Check if user exist
            var appUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
            if (appUser == null) {
                String message = messageSource.getMessage("appMessages.user.notexist", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Change Password");
                log.setUsername(requestPayload.getEmail());
                logger.log(Level.INFO, gson.toJson(log));
                //Log the error
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(message);
                return response;
            }

            //Check if user is disable
            if (appUser.isLocked() || !appUser.isActivated()) {
                String message = messageSource.getMessage("appMessages.user.disabled", new Object[0], Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Change Password");
                log.setUsername(requestPayload.getEmail());
                logger.log(Level.INFO, gson.toJson(log));
                response.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                response.setResponseMessage(message);
                return response;
            }

            //Send password reset email
            XTicketPayload mailPayload = new XTicketPayload();
            mailPayload.setRecipientEmail(requestPayload.getEmail());
            mailPayload.setEmailSubject("Password Change");
            mailPayload.setCarbonCopyEmail("");
            String message = "<h4>Dear " + requestPayload.getLastName() + "</h4>\n"
                    + "<p>We appreciate your interest in " + companyName + " X-TICKET, our award-winning ticketing platform and help desk.</p>\n"
                    + "<p>To activate your profile, please click on the activation button below</p>\n"
                    + "<p><a href=\"" + host + "/xticket/signup/activate?id=" + "ID" + "\"><button type='button' style='display:inline-block;background-color:#ee4f1e;padding:5px; width:200px; color:#ffffff; text-align:center; border-radius:20px; border-color:white;'>Activate Profile</button></a></p>"
                    + "<p>For support and enquiries, email: " + companyEmail + ".</p>\n"
                    + "<p>Once again, thanks for choosing " + companyName + "!.</p>\n"
                    + "<p>Best wishes,</p>"
                    + "<p>" + companyName + "</p>";

            mailPayload.setEmailBody(message);
            genericService.sendEmail(mailPayload, requestPayload.getEmail());
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.forgotpassword", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH));

            //Log the response
            genericService.logResponse(requestPayload.getEmail(), appUser.getId(), "Forgot Password", "Forgot Password", "Forgot Password", "", "");
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            return response;
        }
    }

    @Override
    public List<AppUser> fetchAppUsers() {
        return xticketRepository.getUsers();
    }

    @Override
    public List<AppUser> fetchInternalAppUsers() {
        return xticketRepository.getInternalAppUsers();
    }

    @Override
    public void userOnline(String principal, boolean userOnline) {
        //Check if the user is valid. Update the online status
        var appUser = xticketRepository.getAppUserUsingEmail(principal);
        if (appUser != null) {
            appUser.setOnline(userOnline);
            xticketRepository.updateAppUser(appUser);
        }
    }

    @Override
    public XTicketPayload updateAppUser(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the selected user for update is valid
            AppUser selectedUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
            if (selectedUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if new value is provided for certain update times
            if (requestPayload.getAction().equalsIgnoreCase("Email") || requestPayload.getAction().equalsIgnoreCase("Mobile")) {
                if (requestPayload.getNewValue().equalsIgnoreCase("")) {
                    response.setResponseCode(ResponseCodes.INPUT_MISSING.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.invalid.newvalue", new Object[0], Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check if valid email is provided
                if (requestPayload.getAction().equalsIgnoreCase("Email")) {
                    if (!requestPayload.getNewValue().matches("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$")) {
                        response.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                        response.setResponseMessage(messageSource.getMessage("appMessages.invalid.newvalue", new Object[0], Locale.ENGLISH));
                        response.setData(null);
                        return response;
                    }
                }

                //Check if valid email is provided
                if (requestPayload.getAction().equalsIgnoreCase("Mobile")) {
                    if (!requestPayload.getNewValue().matches("[0-9]{11}")) {
                        response.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                        response.setResponseMessage(messageSource.getMessage("appMessages.invalid.newvalue", new Object[0], Locale.ENGLISH));
                        response.setData(null);
                        return response;
                    }
                }
            }

            //Check if the request is to update the user role
            RoleGroups roleGroup = null;
            if (requestPayload.getAction().equalsIgnoreCase("Role")) {
                roleGroup = xticketRepository.getRoleGroupUsingId(Long.parseLong(requestPayload.getRolesToUpdate()));
                if (roleGroup == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.roles.notexist", new Object[]{" Id " + requestPayload.getRolesToUpdate()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }
            }

            //Check if the request is to change department
            Department department = null;
            if (requestPayload.getAction().equalsIgnoreCase("Department")) {
                department = xticketRepository.getDepartmentUsingId(Long.parseLong(requestPayload.getDepartmentCode()));
                if (roleGroup == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.roles.notexist", new Object[]{" Id " + requestPayload.getDepartmentCode()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }
            }

            //Check if the request is to change entity
            Entities entity = null;
            if (requestPayload.getAction().equalsIgnoreCase("Role")) {
                entity = xticketRepository.getEntitiesUsingId(Long.parseLong(requestPayload.getEntityCode()));
                if (roleGroup == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.roles.notexist", new Object[]{" Id " + requestPayload.getEntityCode()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }
            }

            String oldValue = "";
            String newValue = "";
            switch (requestPayload.getAction()) {
                case "Activate" -> {
                    oldValue = String.valueOf(selectedUser.isActivated());
                    selectedUser.setActivated(true);
                    xticketRepository.updateAppUser(selectedUser);
                    newValue = String.valueOf(true);
                    break;
                }
                case "AgentAdd" -> {
                    oldValue = String.valueOf(selectedUser.isAgent());
                    selectedUser.setAgent(true);
                    xticketRepository.updateAppUser(selectedUser);
                    newValue = String.valueOf(true);
                    break;
                }
                case "Role" -> {
                    oldValue = selectedUser.getRole().getGroupName();
                    selectedUser.setRole(roleGroup);
                    xticketRepository.updateAppUser(selectedUser);
                    newValue = roleGroup.getGroupName();
                    break;
                }
                case "Deactivate" -> {
                    oldValue = String.valueOf(selectedUser.isActivated());
                    selectedUser.setActivated(false);
                    xticketRepository.updateAppUser(selectedUser);
                    newValue = String.valueOf(false);
                    break;
                }
                case "Lock" -> {
                    oldValue = String.valueOf(selectedUser.isLocked());
                    selectedUser.setLocked(true);
                    xticketRepository.updateAppUser(selectedUser);
                    newValue = String.valueOf(true);
                    break;
                }
                case "Unlock" -> {
                    oldValue = String.valueOf(selectedUser.isLocked());
                    selectedUser.setLocked(false);
                    xticketRepository.updateAppUser(selectedUser);
                    newValue = String.valueOf(false);
                    break;
                }
                case "AgentRemove" -> {
                    oldValue = String.valueOf(selectedUser.isAgent());
                    selectedUser.setAgent(false);
                    xticketRepository.updateAppUser(selectedUser);
                    newValue = String.valueOf(false);
                    break;
                }
                case "FailedLogin" -> {
                    oldValue = String.valueOf(selectedUser.getLoginFailCount());
                    selectedUser.setLoginFailCount(0);
                    xticketRepository.updateAppUser(selectedUser);
                    newValue = "0";
                    break;
                }
                case "LogoutTime" -> {
                    oldValue = String.valueOf(selectedUser.getResetTime());
                    selectedUser.setResetTime(LocalDateTime.now().minusMinutes(1));
                    xticketRepository.updateAppUser(selectedUser);
                    newValue = String.valueOf(LocalDateTime.now().minusMinutes(1));
                    break;
                }
                case "Email" -> {
                    oldValue = selectedUser.getEmail();
                    selectedUser.setEmail(requestPayload.getNewValue());
                    xticketRepository.updateAppUser(selectedUser);
                    newValue = requestPayload.getNewValue();
                    break;
                }
                case "Mobile" -> {
                    oldValue = selectedUser.getMobileNumber();
                    selectedUser.setMobileNumber(requestPayload.getNewValue());
                    xticketRepository.updateAppUser(selectedUser);
                    newValue = requestPayload.getNewValue();
                    break;
                }
                case "PasswordChange" -> {
                    oldValue = String.valueOf(selectedUser.getPasswordChangeDate());
                    selectedUser.setPasswordChangeDate(LocalDate.now().plusDays(passwordChangeDays));
                    xticketRepository.updateAppUser(selectedUser);
                    newValue = String.valueOf(LocalDate.now().plusDays(passwordChangeDays));
                    break;
                }
                case "Department" -> {
                    oldValue = String.valueOf(selectedUser.getDepartment());
                    selectedUser.setDepartment(department);
                    xticketRepository.updateAppUser(selectedUser);
                    newValue = String.valueOf(LocalDate.now().plusDays(passwordChangeDays));
                    break;
                }
                case "Entity" -> {
                    oldValue = String.valueOf(selectedUser.getEntity());
                    selectedUser.setEntity(entity);
                    xticketRepository.updateAppUser(selectedUser);
                    newValue = String.valueOf(LocalDate.now().plusDays(passwordChangeDays));
                    break;
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.user.updated", new Object[0], Locale.ENGLISH));
            response.setData(null);

            //Log the response
            genericService.logResponse(principal, requestPayload.getEmail(), "Update", "User", "Update User Record. " + requestPayload.getAction(), oldValue, newValue);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    /**
     * Roles *
     */
    @Override
    public List<RoleGroups> fetchRoleGroup() {
        return xticketRepository.getRoleGroupList();
    }

    @Override
    public XTicketPayload fetchRoleGroup(String id) {
        var response = new XTicketPayload();
        try {
            //Fetch the role group
            RoleGroups roleGroup = xticketRepository.getRoleGroupUsingId(Long.valueOf(id));
            if (roleGroup == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            response.setGroupName(roleGroup.getGroupName());
            response.setId(roleGroup.getId().intValue());
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public List<AppRoles> fetchAppRoles() {
        return xticketRepository.getAppRoles();
    }

    @Override
    public XTicketPayload createRoleGroup(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the request is to create new role group or update
            if (requestPayload.getId() == 0) {
                //Check if the group role exist already
                RoleGroups roleGroup = xticketRepository.getRoleGroupUsingGroupName(requestPayload.getGroupName());
                if (roleGroup != null) {
                    response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.roles.exist", new Object[]{"Group Name", requestPayload.getGroupName()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Create the role group
                RoleGroups newRoleGroup = new RoleGroups();
                newRoleGroup.setCreatedAt(LocalDateTime.now());
                newRoleGroup.setGroupName(requestPayload.getGroupName());
                xticketRepository.createRoleGroup(newRoleGroup);

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.success.roles", new Object[0], Locale.ENGLISH));
                response.setData(null);

                //Log the response
                genericService.logResponse(principal, requestPayload.getId(), "Create", "Role", "Create Role Group " + requestPayload.getGroupName(), "", requestPayload.getGroupName());
                return response;
            }

            //This is an update request
            RoleGroups oldRoleGroup = xticketRepository.getRoleGroupUsingId(Long.valueOf(requestPayload.getId()));
            if (oldRoleGroup == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{requestPayload.getId()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the group role exist already
            RoleGroups roleGroup = xticketRepository.getRoleGroupUsingGroupName(requestPayload.getGroupName());
            if (roleGroup != null && !Objects.equals(roleGroup.getId(), oldRoleGroup.getId())) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.roles.exist", new Object[]{"Group Name", requestPayload.getGroupName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            String oldValue = oldRoleGroup.getGroupName();
            oldRoleGroup.setGroupName(requestPayload.getGroupName());
            xticketRepository.updateRoleGroup(oldRoleGroup);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.roles", new Object[]{1}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            genericService.logResponse(principal, requestPayload.getId(), "Update", "Role", "Update Role Group " + requestPayload.getGroupName(), oldValue, requestPayload.getGroupName());
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload deleteRoleGroup(String id, String principal) {
        var response = new XTicketPayload();
        try {
            //Check the username of the requester
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the group role exist
            RoleGroups oldRoleGroup = xticketRepository.getRoleGroupUsingId(Long.valueOf(id));
            if (oldRoleGroup == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the role group is in use
            List<AppUser> appUserWithRole = xticketRepository.getAppUserUsingRoleGroup(oldRoleGroup);
            if (appUserWithRole != null) {
                response.setResponseCode(ResponseCodes.IN_USE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.roles.inuse", new Object[]{oldRoleGroup.getGroupName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //The role group is free to be deleted
            xticketRepository.deleteRoleGroups(oldRoleGroup);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.roles.deleted", new Object[]{oldRoleGroup.getGroupName()}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            genericService.logResponse(principal, id, "Delete", "Role", "Delete Role Group " + oldRoleGroup.getGroupName(), oldRoleGroup.getGroupName(), "");
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchGroupRoles(String groupName) {
        var response = new XTicketPayload();
        try {
            RoleGroups roleGroup = xticketRepository.getRoleGroupUsingGroupName(groupName);
            if (roleGroup == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{groupName}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            List<XTicketPayload> data = new ArrayList<>();
            List<String> userRoles = new ArrayList<>();
            List<String> userNoRoles = new ArrayList<>();

            //Fecth the Group details
            List<GroupRoles> groupRoles = xticketRepository.getGroupRolesUsingRoleGroup(roleGroup);
            if (groupRoles != null) {
                for (GroupRoles r : groupRoles) {
                    userRoles.add(r.getAppRole().getRoleName());
                }
            }
            //Get all the app roles 
            List<AppRoles> allRoles = xticketRepository.getAppRoles();
            if (allRoles != null) {
                for (AppRoles r : allRoles) {
                    if (!userRoles.contains(r.getRoleName())) {
                        userNoRoles.add(r.getRoleName());
                    }
                }
            }

            for (String r : userRoles) {
                XTicketPayload newRole = new XTicketPayload();
                newRole.setRoleName(r);
                newRole.setRoleExist("true");
                data.add(newRole);
            }

            for (String r : userNoRoles) {
                XTicketPayload newRole = new XTicketPayload();
                newRole.setRoleName(r);
                newRole.setRoleExist("false");
                data.add(newRole);
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.roles", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload updateGroupRoles(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            //Check if the group role exist already
            RoleGroups roleGroup = xticketRepository.getRoleGroupUsingGroupName(requestPayload.getGroupName());
            if (roleGroup == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{requestPayload.getRoleName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            String[] roles = requestPayload.getRolesToUpdate().split(",");
            if (roles.length > 0) {
                List<GroupRoles> currentRoles = xticketRepository.getGroupRolesUsingRoleGroup(roleGroup);
                if (currentRoles != null) {
                    for (GroupRoles rol : currentRoles) {
                        xticketRepository.deteleGroupRoles(rol);
                    }
                }
                for (String rol : roles) {
                    GroupRoles newRole = new GroupRoles();
                    newRole.setCreatedAt(LocalDateTime.now());
                    newRole.setRoleGroup(roleGroup);
                    AppRoles appRole = xticketRepository.getRoleUsingRoleName(rol);
                    newRole.setAppRole(appRole);
                    xticketRepository.createGroupRoles(newRole);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.roles", new Object[]{1000}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public List<GroupRoles> fetchUserRoles(String principal) {
        try {
            //Fetch the user
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                return null;
            }

            //Fetch group roles belonging to the user group
            List<GroupRoles> groupRoles = xticketRepository.getGroupRolesUsingRoleGroup(appUser.getRole());
            if (groupRoles == null) {
                return null;
            }
            return groupRoles;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Ticket Group *
     */
    @Override
    public XTicketPayload fetchTicketGroup() {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<TicketGroup> ticketGroup = xticketRepository.getTicketGroup();
            if (ticketGroup != null) {
                for (TicketGroup t : ticketGroup) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(dtf.format(t.getCreatedAt()));
                    payload.setCreatedBy(t.getCreatedBy());
                    payload.setId(t.getId().intValue());
                    data.add(payload);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketGroup(String id) {
        var response = new XTicketPayload();
        try {
            TicketGroup ticketGroup = xticketRepository.getTicketGroupUsingId(Long.parseLong(id));
            if (ticketGroup == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            BeanUtils.copyProperties(ticketGroup, response);
            response.setCreatedAt(ticketGroup.getCreatedAt().toLocalDate().toString());
            response.setId(ticketGroup.getId().intValue());

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload createTicketGroup(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the request is for update or create new
            if (requestPayload.getId() == 0) {
                //Check if the user is valid
                var appUser = xticketRepository.getAppUserUsingEmail(principal);
                if (appUser == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check if the ticket group exist using code
                TicketGroup groupByCode = xticketRepository.getTicketGroupUsingCode(requestPayload.getTicketGroupCode());
                if (groupByCode != null) {
                    response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Ticket Group", "Code", requestPayload.getTicketGroupCode()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check the ticket group using the name
                TicketGroup groupByName = xticketRepository.getTicketGroupUsingName(requestPayload.getTicketGroupName());
                if (groupByName != null) {
                    response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Ticket Group", "Name", requestPayload.getTicketGroupName()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                TicketGroup newTicketGroup = new TicketGroup();
                newTicketGroup.setCreatedAt(LocalDateTime.now());
                newTicketGroup.setCreatedBy(principal);
                newTicketGroup.setStatus(requestPayload.getStatus());
                newTicketGroup.setTicketGroupCode(requestPayload.getTicketGroupCode());
                newTicketGroup.setTicketGroupName(requestPayload.getTicketGroupName());
                xticketRepository.createTicketGroup(newTicketGroup);

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket Group", "Created"}, Locale.ENGLISH));
                response.setData(null);

                //Log the response
                StringBuilder newValue = new StringBuilder();
                newValue.append("Ticket Group Code:").append(requestPayload.getTicketGroupCode()).append(", ")
                        .append("Ticket Group Name:").append(requestPayload.getTicketGroupName());
                genericService.logResponse(principal, requestPayload.getId(), "Create", "Ticket Group", "Create Ticket Group. " + requestPayload.getTicketGroupName(), "", newValue.toString());
                return response;
            }

            //This is an update request
            TicketGroup ticketGroup = xticketRepository.getTicketGroupUsingId(Long.valueOf(requestPayload.getId()));
            if (ticketGroup == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Ticket Group", "Id", requestPayload.getId()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the ticket group exist using code
            TicketGroup groupByCode = xticketRepository.getTicketGroupUsingCode(requestPayload.getTicketGroupCode());
            if (groupByCode != null && !Objects.equals(ticketGroup.getId(), groupByCode.getId())) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Ticket Group", "Code", requestPayload.getTicketGroupCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check the ticket group using the name
            TicketGroup groupByName = xticketRepository.getTicketGroupUsingName(requestPayload.getTicketGroupName());
            if (groupByName != null && !Objects.equals(ticketGroup.getId(), groupByName.getId())) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Ticket Group", "Name", requestPayload.getTicketGroupName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            StringBuilder oldValue = new StringBuilder();
            oldValue.append("Ticket Group Code:").append(groupByName.getTicketGroupCode()).append(", ")
                    .append("Ticket Group Name:").append(groupByName.getTicketGroupName());
            ticketGroup.setStatus(requestPayload.getStatus());
            ticketGroup.setTicketGroupCode(requestPayload.getTicketGroupCode());
            ticketGroup.setTicketGroupName(requestPayload.getTicketGroupName());
            xticketRepository.updateTicketGroup(ticketGroup);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket Group", "Updated"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            StringBuilder newValue = new StringBuilder();
            newValue.append("Ticket Group Code:").append(requestPayload.getTicketGroupCode()).append(", ")
                    .append("Ticket Group Name:").append(requestPayload.getTicketGroupName());
            genericService.logResponse(principal, requestPayload.getId(), "Update", "Ticket Group", "Update Ticket Group. " + requestPayload.getTicketGroupName(), oldValue.toString(), newValue.toString());
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload deleteTicketGroup(String id, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the ticket group by Id is valid
            TicketGroup ticketGroup = xticketRepository.getTicketGroupUsingId(Long.valueOf(id));
            if (ticketGroup == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Ticket Group", "Id", id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the ticket group is in use
            Tickets ticket = xticketRepository.getTicketUsingTicketGroup(ticketGroup);
            if (ticket != null) {
                response.setResponseCode(ResponseCodes.IN_USE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.inuse", new Object[]{"Ticket Group", ticketGroup.getTicketGroupName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the ticket group is in use
            List<TicketType> ticketByType = xticketRepository.getTicketTypeUsingTicketGroup(ticketGroup);
            if (ticketByType != null) {
                response.setResponseCode(ResponseCodes.IN_USE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.inuse", new Object[]{"Ticket Group", ticketGroup.getTicketGroupName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            xticketRepository.deleteTicketGroup(ticketGroup);
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket Group" + ticketGroup.getTicketGroupName(), "Deleted"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            genericService.logResponse(principal, ticketGroup.getId(), "Delete", "Ticket Group", "Delete Ticket Group. " + ticketGroup.getTicketGroupName(), ticketGroup.getTicketGroupName(), "");
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    /**
     * Ticket Type *
     */
    @Override
    public XTicketPayload fetchTicketType() {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<TicketType> ticketType = xticketRepository.getTicketType();
            if (ticketType != null) {
                for (TicketType t : ticketType) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(dtf.format(t.getCreatedAt()));
                    payload.setCreatedBy(t.getCreatedBy());
                    payload.setId(t.getId().intValue());
                    payload.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    payload.setTicketGroupCode(t.getTicketGroup().getTicketGroupCode());
                    payload.setServiceUnitCode(t.getServiceUnit().getServiceUnitCode());
                    payload.setServiceUnitName(t.getServiceUnit().getServiceUnitName());
                    payload.setInitialSla(t.getSla().getTicketSlaPeriod() == 'D' ? t.getSla().getTicketSla() + " Day(s)"
                            : t.getSla().getTicketSlaPeriod() == 'M' ? t.getSla().getTicketSla() + " Minute(s)" : t.getSla().getTicketSla() + " Hour(s)");
                    data.add(payload);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketType(String id) {
        var response = new XTicketPayload();
        try {
            TicketType ticketType = xticketRepository.getTicketTypeUsingId(Long.parseLong(id));
            if (ticketType == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            BeanUtils.copyProperties(ticketType, response);
            response.setCreatedAt(ticketType.getCreatedAt().toLocalDate().toString());
            response.setId(ticketType.getId().intValue());
            response.setTicketGroupName(ticketType.getTicketGroup().getTicketGroupName());
            response.setTicketGroupCode(ticketType.getTicketGroup().getTicketGroupCode());
            response.setServiceUnitCode(ticketType.getServiceUnit().getServiceUnitCode());
            response.setServiceUnitName(ticketType.getServiceUnit().getServiceUnitName());
            response.setTicketSlaName(ticketType.getSla().getTicketSlaName());

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload createTicketType(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the request is for new or update
            if (requestPayload.getId() == 0) {
                //Check if the ticket type exist using code
                TicketType typeByCode = xticketRepository.getTicketTypeUsingCode(requestPayload.getTicketTypeCode());
                if (typeByCode != null) {
                    response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Ticket Type", "Code", requestPayload.getTicketTypeCode()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check if ticket exist using name
                TicketType typeByName = xticketRepository.getTicketTypeUsingName(requestPayload.getTicketTypeName());
                if (typeByName != null) {
                    response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Ticket Type", "Name", requestPayload.getTicketTypeName()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check if the ticket group is valid
                TicketGroup ticketGroup = xticketRepository.getTicketGroupUsingCode(requestPayload.getTicketGroupCode());
                if (ticketGroup == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Ticket Group", "Code", requestPayload.getTicketGroupCode()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check if the ticket sla is valid
                TicketSla ticketSla = xticketRepository.getTicketSlaUsingName(requestPayload.getTicketSlaName());
                if (ticketSla == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Ticket SLA", "Name", requestPayload.getTicketSlaName()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check if the service unit is valid
                ServiceUnit serviceUnit = xticketRepository.getServiceUnitUsingCode(requestPayload.getServiceUnitCode());
                if (serviceUnit == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Service Unit", "Code", requestPayload.getServiceUnitCode()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                TicketType newTicketType = new TicketType();
                newTicketType.setCreatedAt(LocalDateTime.now());
                newTicketType.setCreatedBy(principal);
                newTicketType.setTicketTypeCode(requestPayload.getTicketTypeCode());
                newTicketType.setTicketTypeName(requestPayload.getTicketTypeName());
                newTicketType.setEscalationEmails(requestPayload.getEscalationEmails());
                newTicketType.setInternal(requestPayload.isInternal());
                newTicketType.setEmailEscalationIndex(0);
                newTicketType.setSla(ticketSla);
                newTicketType.setStatus(requestPayload.getStatus());
                newTicketType.setServiceUnit(serviceUnit);
                newTicketType.setTicketGroup(ticketGroup);
                newTicketType.setRequireChangeRequestForm(requestPayload.isRequireChangeRequestForm());
                newTicketType.setRequireServiceRequestForm(requestPayload.isRequireServiceRequestForm());
                newTicketType.setAutomated(requestPayload.isAutomated());
                xticketRepository.createTicketType(newTicketType);

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket Type", "Created"}, Locale.ENGLISH));
                response.setData(null);

                //Log the response
                StringBuilder newValue = new StringBuilder();
                newValue.append("Ticket Type Code:").append(requestPayload.getTicketTypeCode()).append(", ")
                        .append("Ticket Type Name:").append(requestPayload.getTicketTypeName()).append(", ")
                        .append("Escalation Emails:").append(requestPayload.getEscalationEmails()).append(", ")
                        .append("SLA:").append(ticketSla.getTicketSlaName()).append(", ")
                        .append("Service Unit:").append(serviceUnit.getServiceUnitName()).append(", ")
                        .append("Ticket Group:").append(ticketGroup.getTicketGroupName()).append(", ")
                        .append("Require Service Request Form:").append(requestPayload.isRequireServiceRequestForm()).append(", ")
                        .append("Require Change Request Form:").append(requestPayload.isRequireChangeRequestForm()).append(", ")
                        .append("Automated Ticket:").append(requestPayload.isAutomated());
                genericService.logResponse(principal, requestPayload.getId(), "Create", "Ticket Type", "Create Ticket Type. " + requestPayload.getTicketTypeName(), "", newValue.toString());
                return response;
            }

            //This is an update request
            TicketType ticketType = xticketRepository.getTicketTypeUsingId(Long.valueOf(requestPayload.getId()));
            if (ticketType == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Ticket Type", "Id", requestPayload.getId()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the ticket type exist using code
            TicketType typeByCode = xticketRepository.getTicketTypeUsingCode(requestPayload.getTicketTypeCode());
            if (typeByCode != null && !Objects.equals(ticketType.getId(), typeByCode.getId())) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Ticket Type", "Code", requestPayload.getTicketTypeCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if ticket exist using name
            TicketType typeByName = xticketRepository.getTicketTypeUsingName(requestPayload.getTicketTypeName());
            if (typeByName != null && !Objects.equals(ticketType.getId(), typeByName.getId())) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Ticket Type", "Name", requestPayload.getTicketTypeName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the ticket group is valid
            TicketGroup ticketGroup = xticketRepository.getTicketGroupUsingCode(requestPayload.getTicketGroupCode());
            if (ticketGroup == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Ticket Group", "Code", requestPayload.getTicketGroupCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the ticket sla is valid
            TicketSla ticketSla = xticketRepository.getTicketSlaUsingName(requestPayload.getTicketSlaName());
            if (ticketSla == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Ticket SLA", "Name", requestPayload.getTicketSlaName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the service unit is valid
            ServiceUnit serviceUnit = xticketRepository.getServiceUnitUsingCode(requestPayload.getServiceUnitCode());
            if (serviceUnit == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Service Unit", "Code", requestPayload.getServiceUnitCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            StringBuilder oldValue = new StringBuilder();
            oldValue.append("Ticket Type Code:").append(ticketType.getTicketTypeCode()).append(", ")
                    .append("Ticket Type Name:").append(ticketType.getTicketTypeName()).append(", ")
                    .append("Escalation Emails:").append(ticketType.getEscalationEmails()).append(", ")
                    .append("SLA:").append(ticketSla.getTicketSlaName()).append(", ")
                    .append("Service Unit:").append(serviceUnit.getServiceUnitName()).append(", ")
                    .append("Ticket Group:").append(ticketGroup.getTicketGroupName()).append(", ")
                    .append("Require Service Request Form:").append(ticketType.isRequireServiceRequestForm()).append(", ")
                    .append("Require Change Request Form:").append(ticketType.isRequireChangeRequestForm()).append(", ")
                    .append("Automated Ticket:").append(ticketType.isAutomated());

            ticketType.setCreatedAt(LocalDateTime.now());
            ticketType.setCreatedBy(principal);
            ticketType.setStatus(requestPayload.getStatus());
            ticketType.setTicketTypeCode(requestPayload.getTicketTypeCode());
            ticketType.setTicketTypeName(requestPayload.getTicketTypeName());
            ticketType.setEscalationEmails(requestPayload.getEscalationEmails());
            ticketType.setInternal(requestPayload.isInternal());
            ticketType.setEmailEscalationIndex(0);
            ticketType.setSla(ticketSla);
            ticketType.setStatus(requestPayload.getStatus());
            ticketType.setServiceUnit(serviceUnit);
            ticketType.setTicketGroup(ticketGroup);
            ticketType.setRequireChangeRequestForm(requestPayload.isRequireChangeRequestForm());
            ticketType.setRequireServiceRequestForm(requestPayload.isRequireServiceRequestForm());
            ticketType.setAutomated(requestPayload.isAutomated());
            xticketRepository.updateTicketType(ticketType);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket Type", "Updated"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            StringBuilder newValue = new StringBuilder();
            newValue.append("Ticket Type Code:").append(requestPayload.getTicketTypeCode()).append(", ")
                    .append("Ticket Type Name:").append(requestPayload.getTicketTypeName()).append(", ")
                    .append("Escalation Emails:").append(requestPayload.getEscalationEmails()).append(", ")
                    .append("SLA:").append(ticketSla.getTicketSlaName()).append(", ")
                    .append("Service Unit:").append(serviceUnit.getServiceUnitName()).append(", ")
                    .append("Ticket Group:").append(ticketGroup.getTicketGroupName()).append(", ")
                    .append("Require Service Request Form:").append(requestPayload.isRequireServiceRequestForm()).append(", ")
                    .append("Require Change Request Form:").append(requestPayload.isRequireChangeRequestForm()).append(", ")
                    .append("Automated Ticket:").append(requestPayload.isAutomated());
            genericService.logResponse(principal, requestPayload.getId(), "Update", "Ticket Type", "Update Ticket Type. " + requestPayload.getTicketTypeName(), oldValue.toString(), newValue.toString());
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload deleteTicketType(String id, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the ticket type by Id is valid
            TicketType ticketType = xticketRepository.getTicketTypeUsingId(Long.valueOf(id));
            if (ticketType == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Ticket Type", "Id", id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the ticket type is in use
            Tickets ticket = xticketRepository.getTicketUsingTicketType(ticketType);
            if (ticket != null) {
                response.setResponseCode(ResponseCodes.IN_USE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.inuse", new Object[]{"Ticket Type", ticketType.getTicketTypeName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            xticketRepository.deleteTicketType(ticketType);
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket Type" + ticketType.getTicketTypeName(), "Deleted"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            genericService.logResponse(principal, id, "Delete", "Ticket Type", "Delete Ticket Type. " + ticketType.getTicketTypeName(), ticketType.getTicketTypeName(), "");
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public List<TicketType> fetchTicketTypeUsingGroup(String ticketGroupCode, String principal) {
        var appUser = xticketRepository.getAppUserUsingEmail(principal);
        if (appUser == null) {
            return new ArrayList<>();
        }

        TicketGroup ticketGroup = xticketRepository.getTicketGroupUsingCode(ticketGroupCode);
        if (ticketGroup == null) {
            return new ArrayList<>();
        } else {
            List<TicketType> ticketTypes = new ArrayList<>();
            //Check if the user is an agent
            if (appUser.isAgent()) {
                ticketTypes = xticketRepository.getTicketTypeUsingTicketGroup(ticketGroup);
            } else {
                ticketTypes = xticketRepository.getTicketTypeUsingTicketGroup(ticketGroup, appUser.isInternal());
            }

            if (ticketTypes == null) {
                return new ArrayList<>();
            }
            return ticketTypes;
        }
    }

    /**
     * Ticket SLA *
     */
    @Override
    public XTicketPayload fetchTicketSla() {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<TicketSla> ticketSla = xticketRepository.getTicketSla();
            if (ticketSla != null) {
                for (TicketSla t : ticketSla) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(dtf.format(t.getCreatedAt()));
                    payload.setCreatedBy(t.getCreatedBy());
                    payload.setId(t.getId().intValue());
                    data.add(payload);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketSla(String id) {
        var response = new XTicketPayload();
        try {
            TicketSla ticketSla = xticketRepository.getTicketSlaUsingId(Long.parseLong(id));
            if (ticketSla == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            BeanUtils.copyProperties(ticketSla, response);
            response.setCreatedAt(ticketSla.getCreatedAt().toLocalDate().toString());
            response.setId(ticketSla.getId().intValue());

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload createTicketSla(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the request is for update or create new
            if (requestPayload.getId() == 0) {
                //Check if the user is valid
                var appUser = xticketRepository.getAppUserUsingEmail(principal);
                if (appUser == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check the ticket sla using the name
                TicketSla slaByName = xticketRepository.getTicketSlaUsingName(requestPayload.getTicketSlaName());
                if (slaByName != null) {
                    response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Ticket SLA", "Name", requestPayload.getTicketSlaName()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                TicketSla newTicketSla = new TicketSla();
                newTicketSla.setCreatedAt(LocalDateTime.now());
                newTicketSla.setCreatedBy(principal);
                newTicketSla.setTicketSla(requestPayload.getTicketSla());
                newTicketSla.setTicketSlaPeriod(requestPayload.getTicketSlaPeriod());
                newTicketSla.setTicketSlaName(requestPayload.getTicketSlaName());
                newTicketSla.setPriority(requestPayload.getPriority());
                xticketRepository.createTicketSla(newTicketSla);

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket SLA", "Created"}, Locale.ENGLISH));
                response.setData(null);

                //Log the response
                StringBuilder newValue = new StringBuilder();
                newValue.append("SLA:").append(requestPayload.getTicketSla()).append("SLA Name:").append(requestPayload.getTicketSlaName())
                        .append("SLA Period:").append(requestPayload.getTicketSlaPeriod());
                genericService.logResponse(principal, requestPayload.getId(), "Create", "SLA", "Create SLA Record. " + requestPayload.getTicketSlaName(), "", newValue.toString());
                return response;
            }

            //This is an update request
            TicketSla ticketSla = xticketRepository.getTicketSlaUsingId(Long.valueOf(requestPayload.getId()));
            if (ticketSla == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Ticket SLA", "Id", requestPayload.getId()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check the ticket sla using the name
            TicketSla slaByName = xticketRepository.getTicketSlaUsingName(requestPayload.getTicketSlaName());
            if (slaByName != null && !Objects.equals(ticketSla.getId(), slaByName.getId())) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Ticket SLA", "Name", requestPayload.getTicketSlaName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            StringBuilder oldValue = new StringBuilder();
            oldValue.append("SLA:").append(ticketSla.getTicketSla()).append("SLA Name:").append(ticketSla.getTicketSlaName())
                    .append("SLA Period:").append(ticketSla.getTicketSlaPeriod());

            ticketSla.setTicketSla(requestPayload.getTicketSla());
            ticketSla.setTicketSlaPeriod(requestPayload.getTicketSlaPeriod());
            ticketSla.setTicketSlaName(requestPayload.getTicketSlaName());
            xticketRepository.updateTicketSla(ticketSla);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket SLA", "Updated"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            StringBuilder newValue = new StringBuilder();
            newValue.append("SLA:").append(requestPayload.getTicketSla()).append("SLA Name:").append(requestPayload.getTicketSlaName())
                    .append("SLA Period:").append(requestPayload.getTicketSlaPeriod());
            genericService.logResponse(principal, requestPayload.getId(), "Update", "SLA", "Update SLA Record. " + requestPayload.getTicketSlaName(), oldValue.toString(), newValue.toString());
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload deleteTicketSla(String id, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the ticket group by Id is valid
            TicketSla ticketSla = xticketRepository.getTicketSlaUsingId(Long.valueOf(id));
            if (ticketSla == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Ticket SLA", "Id", id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the ticket sla is in use
            List<TicketType> ticketByType = xticketRepository.getTicketTypeUsingTicketSla(ticketSla);
            if (ticketByType != null) {
                response.setResponseCode(ResponseCodes.IN_USE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.inuse", new Object[]{"Ticket SLA", ticketSla.getTicketSlaName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            xticketRepository.deleteTicketSla(ticketSla);
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket SLA" + ticketSla.getTicketSlaName(), "Deleted"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            genericService.logResponse(principal, id, "Delete", "SLA", "Delete SLA Record. " + ticketSla.getTicketSlaName(), ticketSla.getTicketSlaName(), "");
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    /**
     * Ticket Agent *
     */
    @Override
    public XTicketPayload fetchTicketAgent() {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<AppUser> ticketAgent = xticketRepository.getAgentAppUsers();
            if (ticketAgent != null) {
                for (AppUser t : ticketAgent) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(dtf.format(t.getCreatedAt()));
                    payload.setCreatedBy(t.getLastName() + " " + t.getOtherName());
                    payload.setLastLogin(t.getLastLogin() == null ? "" : dtf.format(t.getLastLogin()));
                    payload.setId(t.getId().intValue());
                    data.add(payload);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload createTicketAgent(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the user is valid. This is the user to be made an agent
            AppUser userAgent = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
            if (userAgent == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            List<String> roles = Arrays.asList(requestPayload.getRolesToUpdate().split(","));
            if (!roles.isEmpty()) {
                List<TicketAgent> currentRoles = xticketRepository.getTicketAgent(userAgent);
                if (currentRoles != null) {
                    for (TicketAgent rol : currentRoles) {
                        //Check if the current role is found in the new roles
                        if (!roles.contains(rol.getTicketType().getTicketTypeName())) {
                            //Check if a ticket has been created with this agent id
                            Tickets agentTicket = xticketRepository.getTicketUsingAgent(rol);
                            if (agentTicket == null) {
                                xticketRepository.deleteTicketAgent(rol);
                            } else {
                                rol.setInUse(false);
                                xticketRepository.updateTicketAgent(rol);
                            }
                        }
                    }
                }
                for (String rol : roles) {
                    //Check if the roles exist in the db or not
                    TicketType newTicketType = xticketRepository.getTicketTypeUsingName(rol);
                    List<TicketAgent> agentTicket = xticketRepository.getTicketAgentUsingTicketType(userAgent, newTicketType);

                    if (agentTicket == null) {
                        TicketAgent newTicket = new TicketAgent();
                        newTicket.setCreatedAt(LocalDateTime.now());
                        newTicket.setAgent(userAgent);
                        newTicket.setCreatedBy(principal);
                        newTicket.setInUse(true);
                        TicketType ticketType = xticketRepository.getTicketTypeUsingName(rol);
                        newTicket.setTicketType(ticketType);
                        xticketRepository.createTicketAgent(newTicket);
                    }
                }

                //Update the user as agent if not already
                if (!userAgent.isAgent()) {
                    userAgent.setAgent(true);
                    xticketRepository.updateAppUser(userAgent);
                }

                //Update the user role to that of an agent
                RoleGroups agentGroup = xticketRepository.getRoleGroupUsingGroupName("AGENT");
                if (agentGroup != null) {
                    userAgent.setRole(agentGroup);
                    xticketRepository.updateAppUser(userAgent);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.roles", new Object[0], Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchAgentTicketTypes(String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            List<XTicketPayload> data = new ArrayList<>();
            List<String> userRoles = new ArrayList<>();
            List<String> userNoRoles = new ArrayList<>();

            //Fecth the Group details
            List<TicketAgent> userTicketTypes = xticketRepository.getTicketAgent(appUser);
            if (userTicketTypes != null) {
                for (TicketAgent r : userTicketTypes) {
                    userRoles.add(r.getTicketType().getTicketTypeName());
                }
            }

            //Get all the app roles 
            List<TicketType> allTicketType = xticketRepository.getTicketType();
            if (allTicketType != null) {
                for (TicketType r : allTicketType) {
                    if (!userRoles.contains(r.getTicketTypeName())) {
                        userNoRoles.add(r.getTicketTypeName());
                    }
                }
            }

            for (String r : userRoles) {
                XTicketPayload newRole = new XTicketPayload();
                newRole.setTicketTypeName(r);
                newRole.setRoleExist("true");
                data.add(newRole);
            }

            for (String r : userNoRoles) {
                XTicketPayload newRole = new XTicketPayload();
                newRole.setTicketTypeName(r);
                newRole.setRoleExist("false");
                data.add(newRole);
            }
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload createTicket(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            TicketType ticketType = xticketRepository.getTicketTypeUsingCode(requestPayload.getTicketTypeCode());
            if (ticketType == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{requestPayload.getTicketTypeCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Generate a random ticket id
            String ticketId = UUID.randomUUID().toString().substring(0, 18).replaceAll("-", "");

            //Determine the agent to assign the ticket to
            TicketAgent ticketAgent = getAgentToAssignTicket(ticketType);
            if (ticketAgent == null) {
                response.setResponseCode(ResponseCodes.INPUT_MISSING.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.noagent", new Object[]{ticketType.getTicketTypeName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Fetch the open ticket status
            TicketStatus ticketStatus = xticketRepository.getTicketStatusUsingCode("OPEN");
            if (ticketStatus == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"OPEN"}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            Tickets newTicket = new Tickets();
            newTicket.setAttachedFile(!requestPayload.getUploadedFiles().isEmpty());
            newTicket.setAgentNotifiedOfExpiry(false);
            newTicket.setClosedAt(null);
            newTicket.setClosedBy(null);
            newTicket.setCreatedAt(LocalDateTime.now());
            newTicket.setCreatedBy(appUser);
            newTicket.setEscalated(false);
            newTicket.setEscalatedAt(null);
            newTicket.setEscalationIndex(0);
            newTicket.setEntity(appUser.getEntity());
            newTicket.setFileIndex(1);
            newTicket.setInternal(ticketType.isInternal());
            newTicket.setMessage(requestPayload.getMessage());
            newTicket.setSubject(requestPayload.getSubject());
            newTicket.setTicketAgent(ticketAgent);
            newTicket.setTicketGroup(ticketType.getTicketGroup());
            newTicket.setTicketStatus(ticketStatus);
            newTicket.setTicketId(ticketId);
            newTicket.setTicketReopen(false);
            newTicket.setTicketReassign(false);
            newTicket.setTicketType(ticketType);
            newTicket.setSla(String.valueOf(ticketType.getSla().getTicketSla()) + String.valueOf(ticketType.getSla().getTicketSlaPeriod()));
            newTicket.setSlaExpiry(getSlaExpiryDate(ticketType));
            newTicket.setTicketLocked(false);
            newTicket.setTicketSource("Web");
            newTicket.setPriority(ticketType.getSla().getPriority());
            Tickets createTicket = xticketRepository.createTicket(newTicket);

            int fileIndex = 1;
            if (!requestPayload.getUploadedFiles().isEmpty()) {
                for (MultipartFile f : requestPayload.getUploadedFiles()) {
                    String fileExt = FilenameUtils.getExtension(f.getOriginalFilename());
                    String newFileName = genericService.generateFileName();
                    DocumentUpload newDoc = new DocumentUpload();
                    newDoc.setCreatedAt(LocalDateTime.now());
                    newDoc.setDocLink(host + "/xticket/document" + "/" + newFileName + fileIndex + "." + fileExt);
                    newDoc.setTicket(createTicket);
                    newDoc.setNewFileName(newFileName + "." + fileExt);
                    newDoc.setOriginalFileName(f.getOriginalFilename());
                    newDoc.setUploadBy(appUser);
                    xticketRepository.createDocumentUpload(newDoc);
                    //Copy the file to destination
                    String path = servletContext.getRealPath("/") + "WEB-INF/classes/document" + "/" + newFileName + fileIndex + "." + fileExt;
                    File newFile = new File(path);
                    FileCopyUtils.copy(f.getBytes(), newFile);
                    fileIndex++;
                }
            }

            //Update file index
            createTicket.setFileIndex(fileIndex);
            xticketRepository.updateTicket(createTicket);

            //Persist Ticket Comment
            TicketComment newComment = new TicketComment();
            newComment.setComment(requestPayload.getMessage());
            newComment.setCommentFrom(appUser);
            newComment.setCreatedAt(LocalDateTime.now());
            newComment.setTicket(createTicket);
            xticketRepository.createTicketComment(newComment);

            //Send notification to ticket agents
            XTicketPayload mailPayload = new XTicketPayload();
            mailPayload.setRecipientEmail(ticketAgent.getAgent().getEmail());
            mailPayload.setEmailSubject("Ticket Request Notification");
            mailPayload.setCarbonCopyEmail("");
            LocalDateTime slaExpiry = getSlaExpiryDate(ticketType);
            String slaTime = timeDtf.format(slaExpiry.toLocalTime());
            String slaDate = slaExpiry.getMonth().toString() + " " + slaExpiry.getDayOfMonth() + ", " + slaExpiry.getYear();
            String message = "<h4>Dear " + ticketAgent.getAgent().getLastName() + ",</h4>\n"
                    + "<p>A <b>" + ticketType.getTicketTypeName() + "</b> ticket with an ID <b>" + ticketId
                    + "</b> has been generated by <b>" + appUser.getLastName() + ", " + appUser.getOtherName() + "</b> with a priority <b>"
                    + ticketType.getSla().getPriority() + ".</b></p>"
                    + "<p>The ticket is set to expire by <b>" + slaTime + "</b> on <b>" + slaDate + "</b></p>"
                    + "<p>To view the ticket details or take action, kindly login into NGX X-Ticket by <a href=\"" + host + "/xticket" + "\">clicking here</a></p>"
                    + "<p>For support and enquiries, email: " + companyEmail + ".</p>\n"
                    + "<p>Best wishes,</p>"
                    + "<p>" + companyName + "</p>";
            mailPayload.setEmailBody(message);
            genericService.sendEmail(mailPayload, principal);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{ticketType.getTicketTypeName() + " Ticket", "Created"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            StringBuilder newValue = new StringBuilder();
            newValue.append("Ticket Group:").append(ticketType.getTicketGroup().getTicketGroupName()).append(", ")
                    .append("Ticket Type:").append(ticketType.getTicketTypeName()).append(", ")
                    .append("SLA:").append(ticketType.getSla().getTicketSla()).append(String.valueOf(ticketType.getSla().getTicketSlaPeriod())).append(", ")
                    .append("Priority:").append(ticketType.getSla().getPriority()).append(", ")
                    .append("Ticket Agent:").append(ticketAgent.getAgent().getLastName()).append(", ").append(ticketAgent.getAgent().getOtherName());
            genericService.logResponse(principal, createTicket.getId(), "Create", "Ticket", "Create Ticket Record. " + requestPayload.getSubject(), "", newValue.toString());
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload replyTicket(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check the ticket group
            Tickets ticket = xticketRepository.getTicketUsingTicketId(requestPayload.getTicketId());
            if (ticket == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{requestPayload.getTicketId()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            int fileIndex = ticket.getFileIndex();
            if (!requestPayload.getUploadedFiles().isEmpty()) {
                String newFileName = ticket.getTicketId().replace("-", "");
                for (MultipartFile f : requestPayload.getUploadedFiles()) {
                    String fileExt = FilenameUtils.getExtension(f.getOriginalFilename());
                    DocumentUpload newDoc = new DocumentUpload();
                    newDoc.setCreatedAt(LocalDateTime.now());
                    newDoc.setDocLink(host + "/xticket/document" + "/" + newFileName + fileIndex + "." + fileExt);
                    newDoc.setTicket(ticket);
                    newDoc.setNewFileName(newFileName + "." + fileExt);
                    newDoc.setOriginalFileName(f.getOriginalFilename());
                    newDoc.setUploadBy(appUser);
                    xticketRepository.createDocumentUpload(newDoc);
                    //Copy the file to destination
                    String path = servletContext.getRealPath("/") + "WEB-INF/classes/document" + "/" + newFileName + fileIndex + "." + fileExt;
                    File newFile = new File(path);
                    FileCopyUtils.copy(f.getBytes(), newFile);
                    fileIndex++;
                }
            }

            //Update file index
            ticket.setFileIndex(fileIndex);
            xticketRepository.updateTicket(ticket);

            //Persist Ticket Comment
            TicketComment newComment = new TicketComment();
            newComment.setComment(requestPayload.getMessage());
            newComment.setCommentFrom(appUser);
            newComment.setCreatedAt(LocalDateTime.now());
            newComment.setTicket(ticket);
            xticketRepository.createTicketComment(newComment);

            //Send notification to ticket agent
            XTicketPayload mailPayload = new XTicketPayload();
            mailPayload.setRecipientEmail(ticket.getTicketAgent().getAgent().getEmail());
            mailPayload.setEmailSubject("Ticket Reply Notification");
            mailPayload.setCarbonCopyEmail("");
            String slaTime = timeDtf.format(ticket.getSlaExpiry().toLocalTime());
            String slaDate = ticket.getSlaExpiry().getMonth().toString() + " " + ticket.getSlaExpiry().getDayOfMonth() + ", " + ticket.getSlaExpiry().getYear();
            String message = "<h4>Dear " + ticket.getTicketAgent().getAgent().getLastName() + ",</h4>\n"
                    + "<p>A reply to <b>" + ticket.getTicketType().getTicketTypeName() + "</b> ticket with an ID <b>" + ticket.getTicketId()
                    + "</b> has been generated by <b>" + appUser.getLastName() + ", " + appUser.getOtherName() + "</b> with a priority <b>"
                    + ticket.getPriority() + ".</b></p>"
                    + "<p>The ticket is set to expire by <b>" + slaTime + "</b> on <b>" + slaDate + "</b></p>"
                    + "<p>To view the ticket details or take action, kindly login into NGX X-Ticket by <a href=\"" + host + "/xticket" + "\">clicking here</a></p>"
                    + "<p>For support and enquiries, email: " + companyEmail + ".</p>\n"
                    + "<p>Best wishes,</p>"
                    + "<p>" + companyName + "</p>";
            mailPayload.setEmailBody(message);
            genericService.sendEmail(mailPayload, principal);

            //Check if the status is changed
            if (!requestPayload.getTicketStatusCode().equalsIgnoreCase("")) {
                TicketStatus ticketStatus = xticketRepository.getTicketStatusUsingCode(requestPayload.getTicketStatusCode());
                //Check if the SLA is to be extended
                if (ticketStatus.isPauseSLA()) {
                    ticket.setTicketPause(LocalDateTime.now());
                } else {
                    //Check time elapsed between when it was paused and now
                    int timeElapsed = Duration.between(ticket.getTicketPause(), LocalDate.now()).toMinutesPart();
                    ticket.setSlaExpiry(ticket.getSlaExpiry().plusMinutes(timeElapsed));
                    ticket.setTicketPause(null);
                }
                ticket.setTicketStatus(ticketStatus);
                xticketRepository.updateTicketStatus(ticketStatus);

                //Log the status change record
                TicketStatusChange newStatus = new TicketStatusChange();
                newStatus.setChangedAt(LocalDateTime.now());
                newStatus.setChangedBy(appUser);
                newStatus.setPriority(ticket.getPriority());
                newStatus.setReasonForChange(requestPayload.getMessage());
                newStatus.setSla(ticket.getSla());
                newStatus.setTicket(ticket);
                newStatus.setTicketAgent(ticket.getTicketAgent());
                newStatus.setTicketStatus(ticketStatus);
                xticketRepository.createTicketStatusChange(newStatus);
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{ticket.getTicketType().getTicketTypeName() + " Ticket", "Replied"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            genericService.logResponse(principal, ticket.getTicketId(), "create", "Ticket Comment", "Ticket Comment By Requester", "", requestPayload.getMessage());
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload agentReplyTicket(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check the ticket using its id
            Tickets ticket = xticketRepository.getTicketUsingTicketId(requestPayload.getTicketId());
            if (ticket == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{requestPayload.getTicketId()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            int fileIndex = ticket.getFileIndex();
            if (!requestPayload.getUploadedFiles().isEmpty()) {
                String newFileName = ticket.getTicketId().replace("-", "");
                for (MultipartFile f : requestPayload.getUploadedFiles()) {
                    String fileExt = FilenameUtils.getExtension(f.getOriginalFilename());
                    DocumentUpload newDoc = new DocumentUpload();
                    newDoc.setCreatedAt(LocalDateTime.now());
                    newDoc.setDocLink(host + "/xticket/document" + "/" + newFileName + fileIndex + "." + fileExt);
                    newDoc.setTicket(ticket);
                    newDoc.setNewFileName(newFileName + "." + fileExt);
                    newDoc.setOriginalFileName(f.getOriginalFilename());
                    newDoc.setUploadBy(appUser);
                    xticketRepository.createDocumentUpload(newDoc);
                    //Copy the file to destination
                    //Copy the file to destination
                    String path = servletContext.getRealPath("/") + "WEB-INF/classes/document" + "/" + newFileName + fileIndex + "." + fileExt;
                    File newFile = new File(path);
                    FileCopyUtils.copy(f.getBytes(), newFile);
                    fileIndex++;
                }
            }

            //Update file index
            ticket.setFileIndex(fileIndex);
            xticketRepository.updateTicket(ticket);

            //Persist Ticket Comment
            TicketComment newComment = new TicketComment();
            newComment.setComment(requestPayload.getMessage());
            newComment.setCommentFrom(appUser);
            newComment.setCreatedAt(LocalDateTime.now());
            newComment.setTicket(ticket);
            xticketRepository.createTicketComment(newComment);

            //Send notification to the client
            XTicketPayload mailPayload = new XTicketPayload();
            mailPayload.setRecipientEmail(ticket.getCreatedBy().getEmail());
            mailPayload.setEmailSubject("Ticket Reply Notification");
            mailPayload.setCarbonCopyEmail("");
            String slaTime = timeDtf.format(ticket.getSlaExpiry().toLocalTime());
            String slaDate = ticket.getSlaExpiry().getMonth().toString() + " " + ticket.getSlaExpiry().getDayOfMonth() + ", " + ticket.getSlaExpiry().getYear();
            String message = "<h4>Dear " + ticket.getCreatedBy().getLastName() + ",</h4>\n"
                    + "<p>A reply to <b>" + ticket.getTicketType().getTicketTypeName() + "</b> ticket with an ID <b>" + ticket.getTicketId() + "</b> and priority <b>"
                    + ticket.getTicketType().getSla().getPriority() + "</b>"
                    + " initiated by <b>" + appUser.getLastName() + ", " + appUser.getOtherName() + "</b>.</p>"
                    + "<p>The ticket is set to expire by <b>" + slaTime + "</b> on <b>" + slaDate + "</b></p>"
                    + "<p>To view the ticket details or take action, kindly login into NGX X-Ticket by <a href=\"" + host + "/xticket" + "\">clicking here</a></p>"
                    + "<p>For support and enquiries, email: " + companyEmail + ".</p>\n"
                    + "<p>Best wishes,</p>"
                    + "<p>" + companyName + "</p>";
            mailPayload.setEmailBody(message);
            genericService.sendEmail(mailPayload, principal);

            //Check if the status is changed
            if (!requestPayload.getTicketStatusCode().equalsIgnoreCase("")) {
                TicketStatus ticketStatus = xticketRepository.getTicketStatusUsingCode(requestPayload.getTicketStatusCode());
                //Check if the SLA is to be extended
                if (ticketStatus.isPauseSLA()) {
                    ticket.setTicketPause(LocalDateTime.now());
                } else {
                    //Check time elapsed between when it was paused and now
                    int timeElapsed = Duration.between(ticket.getTicketPause(), LocalDateTime.now()).toMinutesPart();
                    ticket.setSlaExpiry(ticket.getSlaExpiry().plusMinutes(timeElapsed));
                    ticket.setTicketPause(null);
                }
                ticket.setTicketStatus(ticketStatus);
                xticketRepository.updateTicketStatus(ticketStatus);

                //Log the status change record
                TicketStatusChange newStatus = new TicketStatusChange();
                newStatus.setChangedAt(LocalDateTime.now());
                newStatus.setChangedBy(appUser);
                newStatus.setPriority(ticket.getPriority());
                newStatus.setReasonForChange(requestPayload.getMessage());
                newStatus.setSla(ticket.getSla());
                newStatus.setTicket(ticket);
                newStatus.setTicketAgent(ticket.getTicketAgent());
                newStatus.setTicketStatus(ticketStatus);
                xticketRepository.createTicketStatusChange(newStatus);
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{ticket.getTicketType().getTicketTypeName() + " Ticket", "Replied"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            genericService.logResponse(principal, ticket.getTicketId(), "Create", "Ticket Comment", "Ticket Comment By Agent", "", requestPayload.getMessage());
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchOpenTicket(String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Fetch all the tickets created by the user
            TicketStatus closedStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
            List<Tickets> tickets = xticketRepository.getOpenTicketsByUser(appUser, closedStatus);
            if (tickets != null) {
                List<XTicketPayload> ticketList = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setPriority(t.getPriority());
                    TicketReopened reopenedTicket = xticketRepository.getMostRecentTicketReopenedUsingTicket(t);
                    newTicket.setReopenedId(reopenedTicket == null ? 0 : reopenedTicket.getId().intValue());
                    newTicket.setEmail(t.getCreatedBy().getEmail());
                    ticketList.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{tickets.size()}, Locale.ENGLISH));
                response.setData(ticketList);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{principal}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchClosedTicket(String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Fetch all the tickets created by the user
            TicketStatus closedStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
            List<Tickets> tickets = xticketRepository.getTicketClosedByAgent(appUser, closedStatus);
            if (tickets != null) {
                List<XTicketPayload> ticketList = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));
                    //Check if the ticket was reopened
                    LocalDateTime closedDate;
                    String closedBy;
                    if (t.isTicketReopen()) {
                        //Get the last reopened record
                        TicketReopened reopenedTicket = xticketRepository.getMostRecentTicketReopenedUsingTicket(t);
                        closedDate = reopenedTicket.getClosedAt();
                        closedBy = reopenedTicket.getClosedBy().getLastName() + ", " + reopenedTicket.getClosedBy().getOtherName();
                    } else {
                        closedDate = t.getClosedAt();
                        closedBy = t.getClosedBy().getLastName() + ", " + t.getClosedBy().getOtherName();
                    }
                    newTicket.setClosedAt(dtf.format(closedDate));
                    newTicket.setClosedBy(closedBy);
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setPriority(t.getPriority());
                    ticketList.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{ticketList.size()}, Locale.ENGLISH));
                response.setData(ticketList);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{principal}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchClosedTicket(String principal, String transType) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Fetch all the tickets created by the user
            TicketStatus closedStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
            List<Tickets> tickets = xticketRepository.getClosedTicketsByUser(appUser, closedStatus);
            if (tickets != null) {
                if (!transType.equalsIgnoreCase("all") && !transType.equalsIgnoreCase("")) {
                    tickets = tickets.stream().filter(t -> t.getTicketGroup().getTicketGroupName().equalsIgnoreCase(transType)).collect(Collectors.toList());
                }

                List<XTicketPayload> ticketList = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));
                    //Check if the ticket was reopened
                    LocalDateTime closedDate;
                    String closedBy;
                    if (t.isTicketReopen()) {
                        //Get the last reopened record
                        TicketReopened reopenedTicket = xticketRepository.getMostRecentTicketReopenedUsingTicket(t);
                        closedDate = reopenedTicket.getClosedAt();
                        closedBy = reopenedTicket.getClosedBy().getLastName() + ", " + reopenedTicket.getClosedBy().getOtherName();
                    } else {
                        closedDate = t.getClosedAt();
                        closedBy = t.getClosedBy().getLastName() + ", " + t.getClosedBy().getOtherName();
                    }
                    newTicket.setClosedAt(dtf.format(closedDate));
                    newTicket.setClosedBy(closedBy);
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setPriority(t.getPriority());
                    ticketList.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{ticketList.size()}, Locale.ENGLISH));
                response.setData(ticketList);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{principal}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketUsingId(String id) {
        var response = new XTicketPayload();
        try {
            Tickets ticket = xticketRepository.getTicketUsingId(Long.parseLong(id));
            if (ticket == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            BeanUtils.copyProperties(ticket, response);
            response.setCreatedAt(dtf.format(ticket.getCreatedAt()));
            response.setId(ticket.getId().intValue());
            response.setTicketGroupName(ticket.getTicketGroup().getTicketGroupName());
            response.setTicketTypeName(ticket.getTicketType().getTicketTypeName());
            response.setPriority(ticket.getPriority());
            response.setSlaExpiry(dtf.format(ticket.getSlaExpiry()));
            response.setTicketAgent(ticket.getTicketAgent().getAgent().getLastName() + ", " + ticket.getTicketAgent().getAgent().getOtherName());
            response.setStatus(ticket.getTicketStatus().getTicketStatusName());
            TicketReopened reopenedTicket = xticketRepository.getMostRecentTicketReopenedUsingTicket(ticket);
            response.setReopenedId(!ticket.isTicketReopen() || reopenedTicket == null ? 0 : reopenedTicket.getId().intValue());
            response.setStatus(ticket.getTicketStatus().getTicketStatusName());

            //Fetch ticket responses
            List<TicketComment> comments = xticketRepository.getTicketCommentUsingTicket(ticket);
            List<XTicketPayload> data = new ArrayList<>();
            if (comments != null) {
                for (TicketComment c : comments) {
                    XTicketPayload comment = new XTicketPayload();
                    comment.setMessage(c.getComment());
                    comment.setMessageFrom(c.getCommentFrom().getLastName() + ", " + c.getCommentFrom().getOtherName());
                    comment.setCreatedAt(shortDtf.format(c.getCreatedAt()));
                    comment.setReply(!Objects.equals(c.getCommentFrom(), c.getTicket().getCreatedBy()));
                    data.add(comment);
                }
            }

            //Get the list of all supporting documents
            List<XTicketPayload> documentData = new ArrayList<>();
            List<DocumentUpload> supportingDocuments = xticketRepository.getDocumentUploadUsingTicket(ticket);
            if (supportingDocuments != null) {
                for (DocumentUpload a : supportingDocuments) {
                    XTicketPayload payload = new XTicketPayload();
                    payload.setDocumentLink(a.getDocLink());
                    payload.setOriginalFileName(a.getOriginalFileName());
                    payload.setCreatedAt(dtf.format(a.getCreatedAt()));
                    documentData.add(payload);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            response.setUploadDocuments(documentData);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketByUser(String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }
            List<Tickets> tickets = xticketRepository.getTicketsByUser(appUser);
            if (tickets != null) {
                List<XTicketPayload> ticketList = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setServiceUnitName(t.getTicketType().getServiceUnit().getServiceUnitName());
                    newTicket.setPriority(t.getPriority());
                    newTicket.setMessage(t.getMessage());
                    ticketList.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{ticketList.size()}, Locale.ENGLISH));
                response.setData(ticketList);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{principal}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketGroupStatisticsByUser(String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Fetch the ticket groups
            List<TicketGroup> ticketGroup = xticketRepository.getTicketGroup();
            List<XTicketPayload> data = new ArrayList<>();
            if (ticketGroup != null) {
                for (TicketGroup t : ticketGroup) {
                    XTicketPayload ticket = new XTicketPayload();
                    int count = xticketRepository.getTicketGroupByUser(appUser, t);
                    ticket.setValue(count);
                    ticket.setName(t.getTicketGroupName());
                    data.add(ticket);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketStatusStatisticsByUser(String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            List<XTicketPayload> data = new ArrayList<>();
            //Fetch the ticket for the user
            TicketStatus closedStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
            List<Tickets> closedTickets = xticketRepository.getClosedTicketsByUser(appUser, closedStatus);
            List<Tickets> openTickets = xticketRepository.getOpenTicketsByUser(appUser, closedStatus); //All status other than closed

            XTicketPayload closedTicket = new XTicketPayload();
            closedTicket.setValue(closedTickets == null ? 0 : closedTickets.size());
            closedTicket.setName("Closed");
            data.add(closedTicket);

            XTicketPayload openTicket = new XTicketPayload();
            openTicket.setValue(openTickets == null ? 0 : openTickets.size());
            openTicket.setName("Open");
            data.add(openTicket);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchOpenTicketGroupStatisticsForAgent(String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Fetch the ticket groups
            List<TicketGroup> ticketGroup = xticketRepository.getTicketGroup();
            List<XTicketPayload> data = new ArrayList<>();
            TicketStatus openStatus = xticketRepository.getTicketStatusUsingCode("OPEN");
            if (ticketGroup != null) {
                for (TicketGroup t : ticketGroup) {
                    XTicketPayload ticket = new XTicketPayload();
                    int count = xticketRepository.getTicketOpenForAgentByGroup(appUser, openStatus, t);
                    ticket.setValue(count);
                    ticket.setName(t.getTicketGroupName());
                    data.add(ticket);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchOpenTicketAboutToViolateSlaForAgent(String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Fetch the ticket for the user
            TicketStatus openStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
            List<Tickets> openTickets = xticketRepository.getOpenAgentTickets(appUser, openStatus);

            if (openTickets != null) {
                //Filters tickets with 5 mins or less to SLA expiry
                openTickets = openTickets.stream().filter(t -> Duration.between(LocalDateTime.now(), t.getSlaExpiry()).toMinutes() <= slaExpiryTimeLeft).collect(Collectors.toList());
                XTicketPayload openTicket = new XTicketPayload();
                openTicket.setValue(openTickets.size());
                openTicket.setName("About to Violate Sla");
            } else {
                XTicketPayload openTicket = new XTicketPayload();
                openTicket.setValue(0);
                openTicket.setName("About to Violate Sla");
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchOpenTicketWithCriticalSlaForAgent(String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Fetch the ticket for the user
            TicketStatus openStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
            List<Tickets> openTickets = xticketRepository.getOpenAgentTickets(appUser, openStatus);

            if (openTickets != null) {
                //Filters tickets with 5 mins or less to SLA expiry
                openTickets = openTickets.stream().filter(t -> t.getTicketType().getSla().getPriority().equalsIgnoreCase("Critical")
                        || t.getTicketType().getSla().getPriority().equalsIgnoreCase("High")).collect(Collectors.toList());
                XTicketPayload openTicket = new XTicketPayload();
                openTicket.setValue(openTickets.size());
                openTicket.setName("Critical Sla Tickets");
            } else {
                XTicketPayload openTicket = new XTicketPayload();
                openTicket.setValue(0);
                openTicket.setName("Critical Sla Tickets");
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload closeTicket(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            Tickets ticket = xticketRepository.getTicketUsingId(requestPayload.getId());
            if (ticket == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{requestPayload.getId()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //This is a close request for initial ticket
            if (requestPayload.getAction().equalsIgnoreCase("f")) {
                //Fetch the closed ticket status
                TicketStatus closedStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
                //Update ticket
                ticket.setTicketStatus(closedStatus);
                ticket.setClosedAt(LocalDateTime.now());
                ticket.setClosedBy(appUser);
                ticket.setRating(requestPayload.getRating());
                ticket.setRatingComment(requestPayload.getComment());
                ticket.setResolution(requestPayload.getResolution());
                xticketRepository.updateTicket(ticket);

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{" ticket is ", "closed "}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //This is a request to close a reopened ticket
            TicketReopened ticketReopen = xticketRepository.getTicketReopenedUsingId(requestPayload.getReopenedId());
            if (ticketReopen == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{requestPayload.getReopenedId()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Update ticket with close status
            TicketStatus closedStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
            ticket.setTicketStatus(closedStatus);
            ticket.setTicketReopen(false);
            ticket.setResolution(requestPayload.getResolution());
            xticketRepository.updateTicket(ticket);

            //Update the reopened ticket record
            ticketReopen.setClosedAt(LocalDateTime.now());
            ticketReopen.setClosedBy(appUser);
            ticketReopen.setRating(requestPayload.getRating());
            ticketReopen.setRatingComment(requestPayload.getComment());
            xticketRepository.updateTicketReopen(ticketReopen);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{" ticket is ", "closed "}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            String newValue = "Rating:" + requestPayload.getRating() + ", " + "Rating Comment:" + requestPayload.getComment();
            genericService.logResponse(principal, ticket.getTicketId(), "Create", "Ticket Closure", "Ticket Closure By " + principal, "", newValue);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload createReopenTicket(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            Tickets ticket = xticketRepository.getTicketUsingId(requestPayload.getId());
            if (ticket == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{requestPayload.getId()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Determine the agent to assign the ticket to
            TicketAgent ticketAgent = getAgentToAssignTicket(ticket.getTicketType());
            if (ticketAgent == null) {
                response.setResponseCode(ResponseCodes.INPUT_MISSING.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.noagent", new Object[]{ticket.getTicketType().getTicketTypeName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            TicketReopened newReopenTicket = new TicketReopened();
            newReopenTicket.setClosedAt(null);
            newReopenTicket.setClosedBy(null);
            newReopenTicket.setReasonForReopening(requestPayload.getMessage());
            newReopenTicket.setReopenedAt(LocalDateTime.now());
            newReopenTicket.setReopenedBy(appUser);
            newReopenTicket.setTicket(ticket);
            newReopenTicket.setTicketAgent(ticketAgent);
            newReopenTicket.setSla(String.valueOf(ticket.getTicketType().getSla().getTicketSla()) + String.valueOf(ticket.getTicketType().getSla().getTicketSlaPeriod()));
            newReopenTicket.setSlaExpiry(getSlaExpiryDate(ticket.getTicketType()));
            newReopenTicket.setPriority(ticket.getTicketType().getSla().getPriority());
            xticketRepository.createTicketReopen(newReopenTicket);

            //Reopen the ticket and update the status to open
            TicketStatus openStatus = xticketRepository.getTicketStatusUsingCode("OPEN");
            ticket.setTicketStatus(openStatus);
            ticket.setTicketReopen(true);
            xticketRepository.updateTicket(ticket);

            //Persist Ticket Comment
            TicketComment newComment = new TicketComment();
            newComment.setComment(requestPayload.getMessage());
            newComment.setCommentFrom(appUser);
            newComment.setCreatedAt(LocalDateTime.now());
            newComment.setTicket(ticket);
            xticketRepository.createTicketComment(newComment);

            //Send notification to ticket agents
            XTicketPayload mailPayload = new XTicketPayload();
            mailPayload.setRecipientEmail(ticketAgent.getAgent().getEmail());
            mailPayload.setEmailSubject("Ticket Reply Notification");
            mailPayload.setCarbonCopyEmail("");
            LocalDateTime slaExpiry = getSlaExpiryDate(ticket.getTicketType());
            String slaTime = timeDtf.format(slaExpiry.toLocalTime());
            String slaDate = slaExpiry.getMonth().toString() + " " + slaExpiry.getDayOfMonth() + ", " + slaExpiry.getYear();
            String message = "<h4>Dear " + ticketAgent.getAgent().getLastName() + ",</h4>\n"
                    + "<p>A <b>" + ticket.getTicketType().getTicketTypeName() + "</b> ticket with an ID <b>" + ticket.getTicketId()
                    + "</b> has been reopened by <b>" + appUser.getLastName() + ", " + appUser.getOtherName() + "</b> with a priority <b>"
                    + ticket.getTicketType().getSla().getPriority() + ".</b></p>"
                    + "<p>The ticket is set to expire by <b>" + slaTime + "</b> on <b>" + slaDate + "</b></p>"
                    + "<p>To view the ticket details or take action, kindly login into NGX X-Ticket by <a href=\"" + host + "/xticket" + "\">clicking here</a></p>"
                    + "<p>For support and enquiries, email: " + companyEmail + ".</p>\n"
                    + "<p>Best wishes,</p>"
                    + "<p>" + companyName + "</p>";
            mailPayload.setEmailBody(message);
            genericService.sendEmail(mailPayload, principal);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            genericService.logResponse(principal, ticket.getTicketId(), "Create", "Reopen Ticket", "Ticket Reopened By " + principal, "", requestPayload.getMessage());
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchOpenTicket() {
        var response = new XTicketPayload();
        try {
            //Fetch all the open tickets
            TicketStatus openStatus = xticketRepository.getTicketStatusUsingCode("OPEN");
            List<Tickets> tickets = xticketRepository.getTicketsByStatus(openStatus);
            if (tickets != null) {
                List<XTicketPayload> ticketList = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));
                    newTicket.setCreatedBy(t.getCreatedBy().getLastName() + ", " + t.getCreatedBy().getOtherName());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setServiceUnitName(t.getTicketType().getServiceUnit().getServiceUnitName());
                    newTicket.setPriority(t.getPriority());
                    newTicket.setSlaExpiry(dtf.format(t.getSlaExpiry()));
                    TicketReopened reopenedTicket = xticketRepository.getMostRecentTicketReopenedUsingTicket(t);
                    newTicket.setReopenedId(reopenedTicket == null ? 0 : reopenedTicket.getId().intValue());
                    ticketList.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{ticketList.size()}, Locale.ENGLISH));
                response.setData(ticketList);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Open Ticket"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchOpenTicketForAgent(String principal, String transType) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the agent
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Fetch all the open tickets assigned to the agent
            TicketStatus openStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
            List<Tickets> agentTickets = xticketRepository.getOpenAgentTickets(appUser, openStatus);
            if (agentTickets != null) {
                //Check the trans type
                if (transType.equalsIgnoreCase("critical")) {
                    agentTickets = agentTickets.stream().filter(t -> t.getTicketType().getSla().getPriority().equalsIgnoreCase("Critical")
                            || t.getTicketType().getSla().getPriority().equalsIgnoreCase("High")).collect(Collectors.toList());

                }

                if (transType.equalsIgnoreCase("sla")) {
                    agentTickets = agentTickets.stream().filter(t -> Duration.between(LocalDateTime.now(), t.getSlaExpiry()).toMinutes() <= slaExpiryTimeLeft).collect(Collectors.toList());
                }

                if (!transType.equalsIgnoreCase("all") && !transType.equalsIgnoreCase("sla") && !transType.equalsIgnoreCase("critical") && !transType.equalsIgnoreCase("")) {
                    agentTickets = agentTickets.stream().filter(t -> t.getTicketGroup().getTicketGroupName().equalsIgnoreCase(transType)).collect(Collectors.toList());
                }

                List<XTicketPayload> data = new ArrayList<>();
                for (Tickets t : agentTickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));
                    newTicket.setCreatedBy(t.getCreatedBy().getLastName() + ", " + t.getCreatedBy().getOtherName());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setServiceUnitName(t.getTicketType().getServiceUnit().getServiceUnitName());
                    newTicket.setPriority(t.getPriority());
                    newTicket.setSlaExpiry(dtf.format(t.getSlaExpiry()));
                    TicketReopened reopenedTicket = xticketRepository.getMostRecentTicketReopenedUsingTicket(t);
                    newTicket.setReopenedId(reopenedTicket == null ? 0 : reopenedTicket.getId().intValue());
                    newTicket.setEmail(t.getCreatedBy().getEmail());
                    data.add(newTicket);
                }

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{agentTickets.size()}, Locale.ENGLISH));
                response.setData(data);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Open Ticket"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchClosedTicket() {
        var response = new XTicketPayload();
        try {
            //Fetch all the Closed tickets 
            TicketStatus closedStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
            List<Tickets> tickets = xticketRepository.getTicketsByStatus(closedStatus);
            if (tickets != null) {
                List<XTicketPayload> ticketList = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));

                    //Check if the ticket was reopened
                    //Check if the ticket was reopened
                    LocalDateTime closedDate;
                    String closedBy;
                    if (t.isTicketReopen()) {
                        //Get the last reopened record
                        TicketReopened reopenedTicket = xticketRepository.getMostRecentTicketReopenedUsingTicket(t);
                        closedDate = reopenedTicket.getClosedAt();
                        closedBy = reopenedTicket.getClosedBy().getLastName() + ", " + reopenedTicket.getClosedBy().getOtherName();
                    } else {
                        closedDate = t.getClosedAt();
                        closedBy = t.getClosedBy().getLastName() + ", " + t.getClosedBy().getOtherName();
                    }

                    newTicket.setClosedAt(dtf.format(closedDate));
                    newTicket.setClosedBy(closedBy);
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setServiceUnitName(t.getTicketType().getServiceUnit().getServiceUnitName());
                    newTicket.setPriority(t.getPriority());
                    ticketList.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{ticketList.size()}, Locale.ENGLISH));
                response.setData(ticketList);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Closed Ticket"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    private TicketAgent getAgentToAssignTicket(TicketType ticketType) {
        List<TicketAgent> ticketAgents = xticketRepository.getTicketAgentUsingTicketType(ticketType);
        String[] jobStats = new String[20];
        if (ticketAgents != null) {
            int i = 0;
            int availableAgents = 0;
            for (TicketAgent t : ticketAgents) {
                //Check if the agent is active
                if (!t.getAgent().isLocked() && t.getAgent().isAgent()) {
                    //Get the jobs currently assigned to the agent
                    TicketStatus openStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
                    List<Tickets> ticketsByAgent = xticketRepository.getOpenAgentTickets(t.getAgent(), openStatus);
                    jobStats[i] = String.valueOf(ticketsByAgent == null ? 0 : ticketsByAgent.size()) + "*" + String.valueOf(t.getId());
                    i++;
                    availableAgents++;
                }
            }

            //Check if agent is assigned. Returns 0 when no agent is identified or locked.
            if (availableAgents == 0) {
                return null;
            }

            //Loop through the array
            int minJob = Integer.parseInt(jobStats[0].split("\\*")[0]);
            String agentWithMinJob = "";
            for (String jobStat : jobStats) {
                //Check if there is any value for the array at the index
                if (jobStat != null && !jobStat.equalsIgnoreCase("")) {
                    int newMin = Integer.parseInt(jobStat.split("\\*")[0]);
                    agentWithMinJob = jobStat.split("\\*")[1];
                    if (newMin < minJob) {
                        minJob = newMin;
                        agentWithMinJob = jobStat.split("\\*")[1];
                    }
                }
            }
            //Determine the agent with the minimum job count
            return xticketRepository.getTicketAgentUsingId(Long.parseLong(agentWithMinJob));
        }
        return null;
    }

    @Override
    public XTicketPayload createTicketReassignment(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the agent
            var appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Get the details of the agent reassigned to
            AppUser newTicketAgent = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
            if (newTicketAgent == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Get the original ticket using Id
            Tickets ticket = xticketRepository.getTicketUsingId(requestPayload.getId());
            if (ticket == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{requestPayload.getId()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the ticket sla is valid
            TicketSla ticketSla = xticketRepository.getTicketSlaUsingName(ticket.getTicketType().getSla().getTicketSlaName());
            if (ticketSla == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Ticket SLA", "Name", requestPayload.getTicketSlaName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Determine the ticket type and agent
            TicketType ticketType = xticketRepository.getTicketTypeUsingCode(requestPayload.getTicketTypeCode());
            if (ticketType == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{requestPayload.getTicketTypeCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Determine the agent to assign the ticket to
            TicketAgent ticketAgent = getAgentToAssignTicket(ticketType);
            if (ticketAgent == null) {
                response.setResponseCode(ResponseCodes.INPUT_MISSING.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.noagent", new Object[]{ticketType.getTicketTypeName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if reassigned to self
            if (Objects.equals(ticketAgent, newTicketAgent)) {
                response.setResponseCode(ResponseCodes.SAME_ACCOUNT.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.sameagent", new Object[0], Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            LocalDateTime slaExpiryTime = ticket.getTicketType().getSla().getTicketSlaPeriod() == 'D'
                    ? LocalDateTime.now().plusDays(ticket.getTicketType().getSla().getTicketSla())
                    : ticket.getTicketType().getSla().getTicketSlaPeriod() == 'H'
                    ? LocalDateTime.now().plusHours(ticket.getTicketType().getSla().getTicketSla())
                    : LocalDateTime.now().plusMinutes(ticket.getTicketType().getSla().getTicketSla());

            //Reassign the ticket
            TicketReassign ticketReassign = new TicketReassign();
            ticketReassign.setInitialSla(ticket.getSlaExpiry());
            ticketReassign.setNewSla(slaExpiryTime);
            ticketReassign.setReasonForReassigning(requestPayload.getMessage());
            ticketReassign.setReassignedAt(LocalDateTime.now());
            ticketReassign.setReassignedBy(appUser);
            ticketReassign.setReassignedTo(newTicketAgent);
            ticketReassign.setTicket(ticket);
            xticketRepository.createTicketReassign(ticketReassign);

            //Update the ticket record and status
            TicketStatus openStatus = xticketRepository.getTicketStatusUsingCode("OPEN");
            ticket.setTicketStatus(openStatus);
            ticket.setTicketReassign(true);
            ticket.setSlaExpiry(slaExpiryTime);
            ticket.setTicketAgent(ticketAgent);
            xticketRepository.updateTicket(ticket);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            genericService.logResponse(principal, ticket.getTicketId(), "Create", "Ticket Reassignment", "Ticket Reassigned By " + principal, appUser.getLastName() + ", " + appUser.getOtherName(), newTicketAgent.getLastName() + ", " + newTicketAgent.getOtherName());
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketFullDetails(String ticketId) {
        var response = new XTicketPayload();
        try {
            //Fetch Ticket using the ticket id
            Tickets ticket = xticketRepository.getTicketUsingTicketId(ticketId);
            if (ticket == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{ticketId}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the ticket was reopened
            LocalDateTime closedDate;
            String closedBy;
            TicketStatus openTicketStatus = xticketRepository.getTicketStatusUsingCode("OPEN");
            if (ticket.isTicketReopen()) {
                //Get the last reopened record
                TicketReopened reopenedTicket = xticketRepository.getMostRecentTicketReopenedUsingTicket(ticket);
                closedDate = reopenedTicket.getClosedAt();
                closedBy = reopenedTicket.getClosedBy().getLastName() + ", " + reopenedTicket.getClosedBy().getOtherName();
            } else if (Objects.equals(ticket.getTicketStatus(), openTicketStatus)) {
                closedDate = null;
                closedBy = "";
            } else {
                closedDate = ticket.getClosedAt();
                closedBy = ticket.getClosedBy().getLastName() + ", " + ticket.getClosedBy().getOtherName();
            }

            //Add details of the ticket
            response.setClosedAt(closedDate == null ? null : dtf.format(closedDate));
            response.setClosedBy(closedBy);
            response.setCreatedBy(ticket.getCreatedBy().getLastName() + ", " + ticket.getCreatedBy().getOtherName());
            response.setCreatedAt(ticket.getCreatedAt() == null ? null : dtf.format(ticket.getCreatedAt()));
            response.setEscalated(ticket.isEscalated());
            response.setInternal(ticket.isInternal());
            response.setTicketId(ticketId);
            response.setTicketOpen(ticket.getTicketStatus().getTicketStatusCode().equalsIgnoreCase("OPEN"));
            response.setReopened(ticket.isTicketReopen());
            response.setTicketGroupName(ticket.getTicketGroup().getTicketGroupName());
            response.setTicketTypeName(ticket.getTicketType().getTicketTypeName());
            response.setServiceUnitName(ticket.getTicketType().getServiceUnit().getServiceUnitName());
            response.setPriority(ticket.getPriority());
            response.setSlaExpiry(ticket.getSlaExpiry() == null ? null : dtf.format(ticket.getSlaExpiry()));
            response.setTicketLocked(ticket.isTicketLocked());
            response.setSource(ticket.getTicketSource());
            response.setMessage(ticket.getMessage());
            response.setSubject(ticket.getSubject());
            response.setTicketReopened(ticket.isTicketReopen());
            response.setTicketReassigned(ticket.isTicketReassign());
            response.setFileIndex(ticket.getFileIndex());
            response.setRating(ticket.getRating());
            response.setComment(ticket.getRatingComment());

            //Fetch all the reopen record
            List<TicketReopened> reopenedTickets = xticketRepository.getTicketReopenedUsingTicket(ticket);
            List<XTicketPayload> reopenedTicketsRecord = new ArrayList<>();
            if (reopenedTickets != null) {
                for (TicketReopened rec : reopenedTickets) {
                    XTicketPayload reopen = new XTicketPayload();
                    reopen.setClosedAt(rec.getClosedAt() == null ? null : dtf.format(rec.getClosedAt()));
                    reopen.setMessage(rec.getReasonForReopening());
                    reopen.setReopenedAt(rec.getReopenedAt() == null ? null : dtf.format(rec.getReopenedAt()));
                    reopen.setReopenedBy(rec.getReopenedBy().getLastName() + ", " + rec.getReopenedBy().getOtherName());
                    reopen.setSlaExpiry(rec.getSlaExpiry() == null ? null : dtf.format(rec.getSlaExpiry()));
                    reopen.setPriority(rec.getPriority());
                    reopen.setTicketAgent(rec.getTicketAgent().getAgent().getLastName() + ", " + rec.getTicketAgent().getAgent().getOtherName());
                    reopen.setRating(rec.getRating());
                    reopen.setComment(rec.getRatingComment());
                    reopenedTicketsRecord.add(reopen);
                }
                response.setReopenedTickets(reopenedTicketsRecord);
            }

            //Fetch all the reassigned ticket record
            List<TicketReassign> reassignedTickets = xticketRepository.getTicketReassignedUsingTicket(ticket);
            List<XTicketPayload> reassignedTicketsRecord = new ArrayList<>();
            if (reassignedTickets != null) {
                for (TicketReassign rec : reassignedTickets) {
                    XTicketPayload reassign = new XTicketPayload();
                    reassign.setInitialSla(rec.getNewSla() == null ? null : dtf.format(rec.getInitialSla()));
                    reassign.setNewSla(rec.getNewSla() == null ? null : dtf.format(rec.getNewSla()));
                    reassign.setMessage(rec.getReasonForReassigning());
                    reassign.setReassignedAt(rec.getReassignedAt() == null ? null : dtf.format(rec.getReassignedAt()));
                    reassign.setReassignedBy(rec.getReassignedBy().getLastName() + ", " + rec.getReassignedBy().getOtherName());
                    reassign.setReassignedTo(rec.getReassignedTo().getLastName() + ", " + rec.getReassignedTo().getOtherName());
                    reassignedTicketsRecord.add(reassign);
                }
                response.setReassignedTickets(reassignedTicketsRecord);
            }

            //Fetch all the comment for ticket record
            List<TicketComment> ticketComments = xticketRepository.getTicketCommentUsingTicket(ticket);
            List<XTicketPayload> ticketCommentRecord = new ArrayList<>();
            if (ticketComments != null) {
                for (TicketComment rec : ticketComments) {
                    XTicketPayload comment = new XTicketPayload();
                    comment.setMessage(rec.getComment());
                    comment.setMessageFrom(rec.getCommentFrom().getLastName() + ", " + rec.getCommentFrom().getOtherName());
                    comment.setCreatedAt(rec.getCreatedAt() == null ? null : shortDtf.format(rec.getCreatedAt()));
                    comment.setReply(!Objects.equals(rec.getCommentFrom(), rec.getTicket().getCreatedBy()));
                    ticketCommentRecord.add(comment);
                }
                response.setTicketComments(ticketCommentRecord);
            }

            //Fetch all the escalation ticket record
            List<TicketEscalations> ticketEscalation = xticketRepository.getTicketEscalationUsingTicket(ticket);
            List<XTicketPayload> ticketEscalationRecord = new ArrayList<>();
            if (ticketEscalation != null) {
                for (TicketEscalations rec : ticketEscalation) {
                    XTicketPayload escalation = new XTicketPayload();
                    escalation.setSlaExpiry(rec.getSlaExpiresAt() == null ? null : dtf.format(rec.getSlaExpiresAt()));
                    escalation.setEscalationEmails(rec.getEscalatedTo());
                    ticketEscalationRecord.add(escalation);
                }
                response.setTicketEscalations(ticketEscalationRecord);
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public List<TicketAgent> fetchTicketAgentUsingType(String ticketTypeCode, String principal) {
        TicketType ticketType = xticketRepository.getTicketTypeUsingCode(ticketTypeCode);
        if (ticketType == null) {
            return new ArrayList<>();
        } else {
            List<TicketAgent> ticketAgents = xticketRepository.getTicketAgentUsingTicketType(ticketType);
            if (ticketAgents == null) {
                return new ArrayList<>();
            }
            return ticketAgents;
        }
    }

    @Override
    public XTicketPayload fetchOpenTicket(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            TicketStatus openStatus = xticketRepository.getTicketStatusUsingCode("OPEN");
            List<Tickets> tickets = xticketRepository.getTicketsByStatus(openStatus);
            if (tickets != null) {
                //Check if service unit is set in the filter
                if (!requestPayload.getServiceUnitCode().equalsIgnoreCase("")) {
                    ServiceUnit serviceUnit = xticketRepository.getServiceUnitUsingCode(requestPayload.getServiceUnitCode());
                    if (serviceUnit != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketType().getServiceUnit() == serviceUnit).collect(Collectors.toList());
                    }
                }

                //Check if ticket group is set in the filter
                if (!requestPayload.getTicketGroupCode().equalsIgnoreCase("")) {
                    TicketGroup ticketGroup = xticketRepository.getTicketGroupUsingCode(requestPayload.getTicketGroupCode());
                    if (ticketGroup != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketGroup() == ticketGroup).collect(Collectors.toList());
                    }
                }

                //Check if ticket type is set in the filter
                if (!requestPayload.getTicketTypeCode().equalsIgnoreCase("")) {
                    TicketType ticketType = xticketRepository.getTicketTypeUsingCode(requestPayload.getTicketTypeCode());
                    if (ticketType != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketType() == ticketType).collect(Collectors.toList());
                    }
                }

                //Check if user type is set 
                if (!requestPayload.getSource().equalsIgnoreCase("")) {
                    if (requestPayload.getSource().equalsIgnoreCase("Internal")) {
                        tickets = tickets.stream().filter(t -> t.isInternal() == true).collect(Collectors.toList());
                    } else {
                        tickets = tickets.stream().filter(t -> t.isInternal() == false).collect(Collectors.toList());
                    }
                }

                //Loop through the list and transform
                List<XTicketPayload> data = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));
                    newTicket.setCreatedBy(t.getCreatedBy().getLastName() + ", " + t.getCreatedBy().getOtherName());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setServiceUnitName(t.getTicketType().getServiceUnit().getServiceUnitName());
                    newTicket.setPriority(t.getPriority());
                    newTicket.setSlaExpiry(dtf.format(t.getSlaExpiry()));
                    newTicket.setTimeElapsed(getTimeElapsed(t.getSlaExpiry(), LocalDateTime.now()));
                    newTicket.setInternal(t.isInternal());
                    newTicket.setTicketAgent(t.getTicketAgent().getAgent().getLastName() + ", " + t.getTicketAgent().getAgent().getOtherName());
                    newTicket.setStatus(t.getTicketStatus().getTicketStatusName());
                    data.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
                response.setData(data);
                return response;
            }
            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Open Ticket"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchClosedTicket(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            TicketStatus closedStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
            List<Tickets> tickets = xticketRepository.getClosedTickets(LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()), closedStatus);
            if (tickets != null) {
                //Check if service unit is set in the filter
                if (!requestPayload.getServiceUnitCode().equalsIgnoreCase("")) {
                    ServiceUnit serviceUnit = xticketRepository.getServiceUnitUsingCode(requestPayload.getServiceUnitCode());
                    if (serviceUnit != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketType().getServiceUnit() == serviceUnit).collect(Collectors.toList());
                    }
                }

                //Check if ticket group is set in the filter
                if (!requestPayload.getTicketGroupCode().equalsIgnoreCase("")) {
                    TicketGroup ticketGroup = xticketRepository.getTicketGroupUsingCode(requestPayload.getTicketGroupCode());
                    if (ticketGroup != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketGroup() == ticketGroup).collect(Collectors.toList());
                    }
                }

                //Check if ticket type is set in the filter
                if (!requestPayload.getTicketTypeCode().equalsIgnoreCase("")) {
                    TicketType ticketType = xticketRepository.getTicketTypeUsingCode(requestPayload.getTicketTypeCode());
                    if (ticketType != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketType() == ticketType).collect(Collectors.toList());
                    }
                }

                //Check if user type is set 
                if (!requestPayload.getSource().equalsIgnoreCase("")) {
                    if (requestPayload.getSource().equalsIgnoreCase("Internal")) {
                        tickets = tickets.stream().filter(t -> t.isInternal() == true).collect(Collectors.toList());
                    } else {
                        tickets = tickets.stream().filter(t -> t.isInternal() == false).collect(Collectors.toList());
                    }
                }

                //Check if ticket agent filter is set
                if (!requestPayload.getEmail().equalsIgnoreCase("")) {
                    var appUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
                    if (appUser != null) {
                        List<TicketAgent> agentTickets = xticketRepository.getTicketAgent(appUser);
                        if (agentTickets != null) {
                            for (TicketAgent ag : agentTickets) {
                                tickets = tickets.stream().filter(t -> t.getTicketAgent() == ag).collect(Collectors.toList());
                            }
                        }
                    }
                }

                //Loop through the list and transform
                List<XTicketPayload> data = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));
                    newTicket.setCreatedBy(t.getCreatedBy().getLastName() + ", " + t.getCreatedBy().getOtherName());

                    //Check if the ticket was reopened
                    //Check if the ticket was reopened
                    LocalDateTime closedDate;
                    String closedBy;
                    if (t.isTicketReopen()) {
                        //Get the last reopened record
                        TicketReopened reopenedTicket = xticketRepository.getMostRecentTicketReopenedUsingTicket(t);
                        closedDate = reopenedTicket.getClosedAt();
                        closedBy = reopenedTicket.getClosedBy().getLastName() + ", " + reopenedTicket.getClosedBy().getOtherName();
                    } else {
                        closedDate = t.getClosedAt();
                        closedBy = t.getClosedBy().getLastName() + ", " + t.getClosedBy().getOtherName();
                    }

                    newTicket.setClosedAt(dtf.format(closedDate));
                    newTicket.setClosedBy(closedBy);
                    newTicket.setTicketId(t.getTicketId());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setServiceUnitName(t.getTicketType().getServiceUnit().getServiceUnitName());
                    newTicket.setPriority(t.getPriority());
                    newTicket.setReopened(t.isTicketReopen());
                    newTicket.setSlaViolated(t.isSlaViolated());
                    newTicket.setInitialSla(t.getSla());
                    data.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
                response.setData(data);
                return response;
            }
            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Closed Ticket"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchAllAppUsers() {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<AppUser> appUsers = xticketRepository.getUsers();
            if (appUsers != null) {
                for (AppUser t : appUsers) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(dtf.format(t.getCreatedAt()));
                    payload.setCreatedBy(t.getLastName() + " " + t.getOtherName());
                    payload.setLastLogin(t.getLastLogin() == null ? "" : dtf.format(t.getLastLogin()));
                    payload.setName(t.getLastName() + ", " + t.getOtherName());
                    payload.setEmail(t.getEmail());
                    payload.setMobileNumber(t.getMobileNumber());
                    payload.setRoleName(t.getRole().getGroupName());
                    payload.setInternal(t.isInternal());
                    data.add(payload);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
                response.setData(data);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Application Users"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketByWithinSla(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            TicketStatus closedStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
            List<Tickets> tickets = xticketRepository.getTicketsWithinSLA(LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()), closedStatus);
            if (tickets != null) {
                //Check if service unit is set in the filter
                if (!requestPayload.getServiceUnitCode().equalsIgnoreCase("")) {
                    ServiceUnit serviceUnit = xticketRepository.getServiceUnitUsingCode(requestPayload.getServiceUnitCode());
                    if (serviceUnit != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketType().getServiceUnit() == serviceUnit).collect(Collectors.toList());
                    }
                }

                //Check if ticket group is set in the filter
                if (!requestPayload.getTicketGroupCode().equalsIgnoreCase("")) {
                    TicketGroup ticketGroup = xticketRepository.getTicketGroupUsingCode(requestPayload.getTicketGroupCode());
                    if (ticketGroup != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketGroup() == ticketGroup).collect(Collectors.toList());
                    }
                }

                //Check if ticket type is set in the filter
                if (!requestPayload.getTicketTypeCode().equalsIgnoreCase("")) {
                    TicketType ticketType = xticketRepository.getTicketTypeUsingCode(requestPayload.getTicketTypeCode());
                    if (ticketType != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketType() == ticketType).collect(Collectors.toList());
                    }
                }

                //Check if user type is set 
                if (!requestPayload.getSource().equalsIgnoreCase("")) {
                    if (requestPayload.getSource().equalsIgnoreCase("Internal")) {
                        tickets = tickets.stream().filter(t -> t.isInternal() == true).collect(Collectors.toList());
                    } else {
                        tickets = tickets.stream().filter(t -> t.isInternal() == false).collect(Collectors.toList());
                    }
                }

                //Check if ticket agent filter is set
                if (!requestPayload.getEmail().equalsIgnoreCase("")) {
                    var appUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
                    if (appUser != null) {
                        List<TicketAgent> agentTickets = xticketRepository.getTicketAgent(appUser);
                        if (agentTickets != null) {
                            for (TicketAgent ag : agentTickets) {
                                tickets = tickets.stream().filter(t -> t.getTicketAgent() == ag).collect(Collectors.toList());
                            }
                        }
                    }
                }

                //Loop through the list and transform
                List<XTicketPayload> data = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));
                    newTicket.setCreatedBy(t.getCreatedBy().getLastName() + ", " + t.getCreatedBy().getOtherName());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setServiceUnitName(t.getTicketType().getServiceUnit().getServiceUnitName());
                    newTicket.setPriority(t.getPriority());
                    newTicket.setSlaExpiry(dtf.format(t.getSlaExpiry()));
                    newTicket.setInternal(t.isInternal());
                    newTicket.setInitialSla(t.getSla());
                    newTicket.setServiceUnitName(t.getTicketType().getServiceUnit().getServiceUnitName());
                    newTicket.setTicketAgent(t.getTicketAgent().getAgent().getLastName() + ", " + t.getTicketAgent().getAgent().getOtherName());
                    data.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
                response.setData(data);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Ticket Within SLA"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketByViolatedSla(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            List<Tickets> tickets = xticketRepository.getViolatedTickets(LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()));
            if (tickets != null) {
                //Check if service unit is set in the filter
                if (!requestPayload.getServiceUnitCode().equalsIgnoreCase("")) {
                    ServiceUnit serviceUnit = xticketRepository.getServiceUnitUsingCode(requestPayload.getServiceUnitCode());
                    if (serviceUnit != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketType().getServiceUnit() == serviceUnit).collect(Collectors.toList());
                    }
                }

                //Check if ticket group is set in the filter
                if (!requestPayload.getTicketGroupCode().equalsIgnoreCase("")) {
                    TicketGroup ticketGroup = xticketRepository.getTicketGroupUsingCode(requestPayload.getTicketGroupCode());
                    if (ticketGroup != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketGroup() == ticketGroup).collect(Collectors.toList());
                    }
                }

                //Check if ticket type is set in the filter
                if (!requestPayload.getTicketTypeCode().equalsIgnoreCase("")) {
                    TicketType ticketType = xticketRepository.getTicketTypeUsingCode(requestPayload.getTicketTypeCode());
                    if (ticketType != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketType() == ticketType).collect(Collectors.toList());
                    }
                }

                //Check if user type is set 
                if (!requestPayload.getSource().equalsIgnoreCase("")) {
                    if (requestPayload.getSource().equalsIgnoreCase("Internal")) {
                        tickets = tickets.stream().filter(t -> t.isInternal()).collect(Collectors.toList());
                    } else {
                        tickets = tickets.stream().filter(t -> !t.isInternal()).collect(Collectors.toList());
                    }
                }

                //Check if ticket agent filter is set
                if (!requestPayload.getEmail().equalsIgnoreCase("")) {
                    var appUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
                    if (appUser != null) {
                        List<TicketAgent> agentTickets = xticketRepository.getTicketAgent(appUser);
                        if (agentTickets != null) {
                            for (TicketAgent ag : agentTickets) {
                                tickets = tickets.stream().filter(t -> t.getTicketAgent() == ag).collect(Collectors.toList());
                            }
                        }
                    }
                }

                //Loop through the list and transform
                List<XTicketPayload> data = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));
                    newTicket.setCreatedBy(t.getCreatedBy().getLastName() + ", " + t.getCreatedBy().getOtherName());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setServiceUnitName(t.getTicketType().getServiceUnit().getServiceUnitName());
                    newTicket.setPriority(t.getPriority());
                    newTicket.setSlaExpiry(dtf.format(t.getSlaExpiry()));
                    newTicket.setInternal(t.isInternal());
                    newTicket.setInitialSla(t.getSla());
                    newTicket.setTicketAgent(t.getTicketAgentViolated().getAgent().getLastName() + ", " + t.getTicketAgentViolated().getAgent().getOtherName());
                    data.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
                response.setData(data);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Ticket With Violated SLA"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketAgent(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<TicketAgent> ticketAgents = xticketRepository.getTicketAgent();
            if (ticketAgents != null) {
                //Check the ticket type filter is set
                if (!requestPayload.getTicketTypeCode().equalsIgnoreCase("")) {
                    TicketType ticketType = xticketRepository.getTicketTypeUsingCode(requestPayload.getTicketTypeCode());
                    if (ticketType != null) {
                        ticketAgents = ticketAgents.stream().filter(t -> t.getTicketType() == ticketType).collect(Collectors.toList());
                    }

                    for (TicketAgent t : ticketAgents) {
                        XTicketPayload payload = new XTicketPayload();
                        BeanUtils.copyProperties(t, payload);

                        //Fetch the tickets closed by the agent
                        TicketStatus closedStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
                        List<Tickets> ticketsByAgent = xticketRepository.getTicketClosedByAgent(t.getAgent(), closedStatus);
                        payload.setCreatedAt(dtf.format(t.getCreatedAt()));
                        payload.setCreatedBy(t.getCreatedBy());
                        payload.setLastLogin(t.getAgent().getLastLogin() == null ? "" : t.getAgent().getLastLogin().format(dtf));
                        payload.setTicketTypeName(t.getTicketType().getTicketTypeName());
                        payload.setTicketCount(ticketsByAgent == null ? 0 : ticketsByAgent.size());
                        data.add(payload);
                    }

                    response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
                    response.setData(data);
                    return response;
                }

                //There are multiple tickets per agent. Group them
                List<AppUser> distinctTicketAgents = xticketRepository.getDistinctTicketAgent();
                for (AppUser t : distinctTicketAgents) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    //Create a list of all the ticket types for the agent
                    List<TicketAgent> ticketsByUser = xticketRepository.getTicketAgent(t);
                    StringBuilder strBuilder = new StringBuilder();
                    for (TicketAgent ta : ticketsByUser) {
                        strBuilder.append(ta.getTicketType().getTicketTypeName()).append(", ");
                    }

                    //Fetch the tickets closed by the agent
                    TicketStatus closedStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
                    List<Tickets> ticketsByAgent = xticketRepository.getTicketClosedByAgent(t, closedStatus);
                    payload.setCreatedAt(dtf.format(t.getCreatedAt()));
                    payload.setCreatedBy(t.getLastName() + " " + t.getOtherName());
                    payload.setLastLogin(t.getLastLogin() == null ? "" : t.getLastLogin().format(dtf));
                    payload.setTicketCount(ticketsByAgent == null ? 0 : ticketsByAgent.size());
                    payload.setTicketTypeName(strBuilder.toString());
                    data.add(payload);
                }

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
                response.setData(data);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Open Ticket"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketClosedByAgent(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            var appUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
            if (appUser != null) {
                TicketStatus closedStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
                List<Tickets> agentTickets = xticketRepository.getTicketClosedByAgent(appUser, LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()), closedStatus);
                if (agentTickets != null) {
                    List<XTicketPayload> data = new ArrayList<>();
                    for (Tickets t : agentTickets) {
                        XTicketPayload newTicket = new XTicketPayload();
                        BeanUtils.copyProperties(t, newTicket);
                        newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));
                        newTicket.setCreatedBy(t.getCreatedBy().getLastName() + ", " + t.getCreatedBy().getOtherName());

                        //Check if the ticket was reopened
                        LocalDateTime closedDate;
                        String closedBy;
                        if (t.isTicketReopen()) {
                            //Get the last reopened record
                            TicketReopened reopenedTicket = xticketRepository.getMostRecentTicketReopenedUsingTicket(t);
                            closedDate = reopenedTicket.getClosedAt();
                            closedBy = reopenedTicket.getClosedBy().getLastName() + ", " + reopenedTicket.getClosedBy().getOtherName();
                        } else {
                            closedDate = t.getClosedAt();
                            closedBy = t.getClosedBy().getLastName() + ", " + t.getClosedBy().getOtherName();
                        }

                        newTicket.setClosedAt(closedDate == null ? null : dtf.format(closedDate));
                        newTicket.setClosedBy(closedBy);
                        newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                        newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                        newTicket.setServiceUnitName(t.getTicketType().getServiceUnit().getServiceUnitName());
                        newTicket.setTicketId(t.getTicketId());
                        newTicket.setPriority(t.getPriority());
                        newTicket.setSlaExpiry(dtf.format(t.getSlaExpiry()));
                        newTicket.setInternal(t.isInternal());
                        newTicket.setSubject(t.getMessage());
                        newTicket.setSlaViolated(t.isSlaViolated());
                        newTicket.setTicketReassigned(t.isTicketReassign());
                        newTicket.setTicketReopened(t.isTicketReopen());
                        newTicket.setEscalated(t.isEscalated());
                        data.add(newTicket);
                    }
                    response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
                    response.setData(data);
                    return response;
                }
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Closed Tickets By Agent"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchReopenedTicket(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            List<TicketReopened> tickets = xticketRepository.getDistinctTicketReopened(LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()));
            if (tickets != null) {
                List<XTicketPayload> data = new ArrayList<>();
                for (TicketReopened t : tickets) {
                    XTicketPayload reopen = new XTicketPayload();
                    reopen.setCreatedAt(dtf.format(t.getTicket().getCreatedAt()));
                    reopen.setCreatedBy(t.getTicket().getCreatedBy().getLastName() + ", " + t.getTicket().getCreatedBy().getOtherName());
                    reopen.setClosedAt(t.getClosedAt() == null ? null : dtf.format(t.getClosedAt()));
                    reopen.setClosedBy(t.getClosedBy() == null ? null : t.getClosedBy().getLastName() + ", " + t.getClosedBy().getOtherName());
                    reopen.setMessage(t.getReasonForReopening());
                    reopen.setReopenedAt(t.getReopenedAt() == null ? null : dtf.format(t.getReopenedAt()));
                    reopen.setReopenedBy(t.getReopenedBy().getLastName() + ", " + t.getReopenedBy().getOtherName());
                    reopen.setTicketId(t.getTicket().getTicketId());
                    reopen.setTicketGroupName(t.getTicket().getTicketGroup().getTicketGroupName());
                    reopen.setTicketTypeName(t.getTicket().getTicketType().getTicketTypeName());
                    reopen.setServiceUnitName(t.getTicket().getTicketType().getServiceUnit().getServiceUnitName());
                    reopen.setPriority(t.getPriority());
                    reopen.setTicketAgent(t.getTicketAgent().getAgent().getLastName() + ", " + t.getTicketAgent().getAgent().getOtherName());
                    data.add(reopen);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
                response.setData(data);
                return response;
            }
            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Open Ticket"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchReassignedTicket(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            List<TicketReassign> tickets = xticketRepository.getDistinctTicketReassigned(LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()));
            if (tickets != null) {
                //Check if service unit is set in the filter
                if (!requestPayload.getServiceUnitCode().equalsIgnoreCase("")) {
                    ServiceUnit serviceUnit = xticketRepository.getServiceUnitUsingCode(requestPayload.getServiceUnitCode());
                    if (serviceUnit != null) {
                        tickets = tickets.stream().filter(t -> t.getTicket().getTicketType().getServiceUnit() == serviceUnit).collect(Collectors.toList());
                    }
                }

                //Check if ticket group is set in the filter
                if (!requestPayload.getTicketGroupCode().equalsIgnoreCase("")) {
                    TicketGroup ticketGroup = xticketRepository.getTicketGroupUsingCode(requestPayload.getTicketGroupCode());
                    if (ticketGroup != null) {
                        tickets = tickets.stream().filter(t -> t.getTicket().getTicketGroup() == ticketGroup).collect(Collectors.toList());
                    }
                }

                //Check if ticket type is set in the filter
                if (!requestPayload.getTicketTypeCode().equalsIgnoreCase("")) {
                    TicketType ticketType = xticketRepository.getTicketTypeUsingCode(requestPayload.getTicketTypeCode());
                    if (ticketType != null) {
                        tickets = tickets.stream().filter(t -> t.getTicket().getTicketType() == ticketType).collect(Collectors.toList());
                    }
                }

                //Check if ticket agent filter is set
                if (!requestPayload.getEmail().equalsIgnoreCase("")) {
                    var appUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
                    if (appUser != null) {
                        List<TicketAgent> agentTickets = xticketRepository.getTicketAgent(appUser);
                        if (agentTickets != null) {
                            for (TicketAgent ag : agentTickets) {
                                tickets = tickets.stream().filter(t -> t.getReassignedBy() == ag.getAgent()).collect(Collectors.toList());
                            }
                        }
                    }
                }

                List<XTicketPayload> data = new ArrayList<>();
                for (TicketReassign t : tickets) {
                    XTicketPayload reassign = new XTicketPayload();
                    reassign.setCreatedAt(dtf.format(t.getTicket().getCreatedAt()));
                    reassign.setCreatedBy(t.getTicket().getCreatedBy().getLastName() + ", " + t.getTicket().getCreatedBy().getOtherName());
                    reassign.setInitialSla(t.getNewSla() == null ? null : dtf.format(t.getInitialSla()));
                    reassign.setNewSla(t.getNewSla() == null ? null : dtf.format(t.getNewSla()));
                    reassign.setMessage(t.getReasonForReassigning());
                    reassign.setReassignedAt(t.getReassignedAt() == null ? null : dtf.format(t.getReassignedAt()));
                    reassign.setReassignedBy(t.getReassignedBy().getLastName() + ", " + t.getReassignedBy().getOtherName());
                    reassign.setReassignedTo(t.getReassignedTo().getLastName() + ", " + t.getReassignedTo().getOtherName());
                    reassign.setTicketGroupName(t.getTicket().getTicketGroup().getTicketGroupName());
                    reassign.setTicketTypeName(t.getTicket().getTicketType().getTicketTypeName());
                    reassign.setTicketGroupName(t.getTicket().getTicketGroup().getTicketGroupName());
                    reassign.setServiceUnitName(t.getTicket().getTicketType().getServiceUnit().getServiceUnitName());
                    reassign.setPriority(t.getTicket().getPriority());
                    reassign.setInternal(t.getTicket().isInternal());
                    reassign.setTicketId(t.getTicket().getTicketId());
                    data.add(reassign);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
                response.setData(data);
                return response;
            }
            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Reassigned Ticket"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    /**
     * Entity Transactions *
     */
    @Override
    public XTicketPayload fetchTicketByEntity(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            List<Tickets> tickets = xticketRepository.getViolatedTickets(LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()));
            if (tickets != null) {
                //Check if service unit is set in the filter
                if (!requestPayload.getServiceUnitCode().equalsIgnoreCase("")) {
                    ServiceUnit serviceUnit = xticketRepository.getServiceUnitUsingCode(requestPayload.getServiceUnitCode());
                    if (serviceUnit != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketType().getServiceUnit() == serviceUnit).collect(Collectors.toList());
                    }
                }

                //Check if ticket group is set in the filter
                if (!requestPayload.getTicketGroupCode().equalsIgnoreCase("")) {
                    TicketGroup ticketGroup = xticketRepository.getTicketGroupUsingCode(requestPayload.getTicketGroupCode());
                    if (ticketGroup != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketGroup() == ticketGroup).collect(Collectors.toList());
                    }
                }

                //Check if ticket type is set in the filter
                if (!requestPayload.getTicketTypeCode().equalsIgnoreCase("")) {
                    TicketType ticketType = xticketRepository.getTicketTypeUsingCode(requestPayload.getTicketTypeCode());
                    if (ticketType != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketType() == ticketType).collect(Collectors.toList());
                    }
                }

                //Check if user type is set 
                if (!requestPayload.getSource().equalsIgnoreCase("")) {
                    if (requestPayload.getSource().equalsIgnoreCase("Internal")) {
                        tickets = tickets.stream().filter(t -> t.isInternal() == true).collect(Collectors.toList());
                    } else {
                        tickets = tickets.stream().filter(t -> t.isInternal() == false).collect(Collectors.toList());
                    }
                }

                //Check if ticket agent filter is set
                if (!requestPayload.getEmail().equalsIgnoreCase("")) {
                    var appUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
                    if (appUser != null) {
                        List<TicketAgent> agentTickets = xticketRepository.getTicketAgent(appUser);
                        if (agentTickets != null) {
                            for (TicketAgent ag : agentTickets) {
                                tickets = tickets.stream().filter(t -> t.getTicketAgent() == ag).collect(Collectors.toList());
                            }
                        }
                    }
                }

                //Loop through the list and transform
                List<XTicketPayload> data = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));
                    newTicket.setCreatedBy(t.getCreatedBy().getLastName() + ", " + t.getCreatedBy().getOtherName());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setServiceUnitName(t.getTicketType().getServiceUnit().getServiceUnitName());
                    newTicket.setPriority(t.getPriority());
                    newTicket.setSlaExpiry(dtf.format(t.getSlaExpiry()));
                    newTicket.setInternal(t.isInternal());
                    newTicket.setInitialSla(t.getSla());
                    newTicket.setTicketAgent(t.getTicketAgentViolated().getAgent().getLastName() + ", " + t.getTicketAgentViolated().getAgent().getOtherName());
                    data.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
                response.setData(data);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Ticket With Violated SLA"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketByEntityToEntity(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            //Check if the entity exist using code
            Entities entityByCode = xticketRepository.getEntitiesUsingCode(requestPayload.getFromEntity());
            if (entityByCode == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Entity", "Code", requestPayload.getEntityCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Fetch the open ticket status
            TicketStatus ticketStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
            if (ticketStatus == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"CLOSED"}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            List<Tickets> tickets = xticketRepository.getClosedTickets(LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()), ticketStatus, entityByCode);
            if (tickets != null) {
                //Check if service unit is set in the filter
                if (!requestPayload.getToEntity().equalsIgnoreCase("")) {
                    Entities toEntity = xticketRepository.getEntitiesUsingCode(requestPayload.getToEntity());
                    if (toEntity != null) {
                        tickets = tickets.stream().filter(t -> t.getEntity() == toEntity).collect(Collectors.toList());
                    }
                }

                //Loop through the list and transform
                List<XTicketPayload> data = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));

                    //Check if the ticket was reopened
                    LocalDateTime closedDate;
                    String closedBy;
                    if (t.isTicketReopen()) {
                        //Get the last reopened record
                        TicketReopened reopenedTicket = xticketRepository.getMostRecentTicketReopenedUsingTicket(t);
                        closedDate = reopenedTicket.getClosedAt();
                        closedBy = reopenedTicket.getClosedBy().getLastName() + ", " + reopenedTicket.getClosedBy().getOtherName();
                    } else {
                        closedDate = t.getClosedAt();
                        closedBy = t.getClosedBy().getLastName() + ", " + t.getClosedBy().getOtherName();
                    }

                    newTicket.setClosedAt(dtf.format(closedDate));
                    newTicket.setClosedBy(closedBy);
                    newTicket.setFromEntity(t.getTicketAgent().getAgent().getEntity().getEntityName());
                    newTicket.setToEntity(t.getEntity().getEntityName());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setSlaExpiry(dtf.format(t.getSlaExpiry()));
                    newTicket.setSlaViolated(t.isSlaViolated());
                    newTicket.setRating(t.getRating());
                    newTicket.setInitialSla(t.getSla().replace("D", " Day(s)").replace("M", " Minute(s)").replace("H", " Hour(s)"));

                    //Fetch the count of the tickets reassigned
                    List<TicketReassign> ticketCount = xticketRepository.getTicketReassignedUsingTicket(t);
                    newTicket.setTicketReassignedCount(ticketCount == null ? 0 : ticketCount.size());

                    //Fetch the number of escalations
                    List<TicketEscalations> ticketEscalations = xticketRepository.getTicketEscalationUsingTicket(t);
                    newTicket.setTicketEscalationCount(ticketEscalations == null ? 0 : ticketEscalations.size());
                    data.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{tickets.size()}, Locale.ENGLISH));
                response.setData(data);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{" selected"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchEntity() {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<Entities> entity = xticketRepository.getEntities();
            if (entity != null) {
                for (Entities t : entity) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(dtf.format(t.getCreatedAt()));
                    payload.setCreatedBy(t.getCreatedBy());
                    payload.setId(t.getId().intValue());
                    data.add(payload);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchEntity(String id) {
        var response = new XTicketPayload();
        try {
            Entities entity = xticketRepository.getEntitiesUsingId(Long.parseLong(id));
            if (entity == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            BeanUtils.copyProperties(entity, response);
            response.setCreatedAt(entity.getCreatedAt().toLocalDate().toString());
            response.setId(entity.getId().intValue());

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload createEntity(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the request is for update or create new
            if (requestPayload.getId() == 0) {
                //Check if the user is valid
                var appUser = xticketRepository.getAppUserUsingEmail(principal);
                if (appUser == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check if the entity exist using code
                Entities entityByCode = xticketRepository.getEntitiesUsingCode(requestPayload.getEntityCode());
                if (entityByCode != null) {
                    response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Entity", "Code", requestPayload.getEntityCode()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check the entity using the name
                Entities entityByName = xticketRepository.getEntitiesUsingName(requestPayload.getEntityName());
                if (entityByName != null) {
                    response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Entity", "Name", requestPayload.getEntityName()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                Entities newEntity = new Entities();
                newEntity.setCreatedAt(LocalDateTime.now());
                newEntity.setCreatedBy(principal);
                newEntity.setStatus(requestPayload.getStatus());
                newEntity.setEntityCode(requestPayload.getEntityCode());
                newEntity.setEntityName(requestPayload.getEntityName());
                xticketRepository.createEntities(newEntity);

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Entity", "Created"}, Locale.ENGLISH));
                response.setData(null);

                StringBuilder newValue = new StringBuilder();
                newValue.append("Entity Code:").append(requestPayload.getEntityCode()).append(", ")
                        .append("Entity Name:").append(requestPayload.getEntityName());
                //Log the response
                genericService.logResponse(principal, newEntity.getId(), "Create", "Entity", "Create Entity " + requestPayload.getEntityName(), "", newValue.toString());
                return response;
            }

            //This is an update request
            Entities entity = xticketRepository.getEntitiesUsingId(Long.valueOf(requestPayload.getId()));
            if (entity == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Entity", "Id", requestPayload.getId()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the entity exist using code
            Entities entityByCode = xticketRepository.getEntitiesUsingCode(requestPayload.getEntityCode());
            if (entityByCode != null && !Objects.equals(entity.getId(), entityByCode.getId())) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Entity", "Code", requestPayload.getEntityCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check the entity using the name
            Entities entityByName = xticketRepository.getEntitiesUsingName(requestPayload.getEntityName());
            if (entityByName != null && !Objects.equals(entity.getId(), entityByName.getId())) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Entity", "Name", requestPayload.getEntityName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            StringBuilder oldValue = new StringBuilder();
            oldValue.append("Entity Code:").append(entity.getEntityCode()).append(", ")
                    .append("Entity Name:").append(entity.getEntityName());

            entity.setStatus(requestPayload.getStatus());
            entity.setEntityCode(requestPayload.getEntityCode());
            entity.setEntityName(requestPayload.getEntityName());
            xticketRepository.updateEntities(entity);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Entity", "Updated"}, Locale.ENGLISH));
            response.setData(null);

            StringBuilder newValue = new StringBuilder();
            newValue.append("Entity Code:").append(requestPayload.getEntityCode()).append(", ")
                    .append("Entity Name:").append(requestPayload.getEntityName());
            //Log the response
            genericService.logResponse(principal, entity.getId(), "Update", "Entity", "Update Entity " + requestPayload.getEntityName(), oldValue.toString(), newValue.toString());
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload deleteEntity(String id, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the service unit by Id is valid
            Entities entity = xticketRepository.getEntitiesUsingId(Long.valueOf(id));
            if (entity == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Entity", "Id", id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check users related to the entity
            List<AppUser> userByEntity = xticketRepository.getAppUserUsingEntity(entity);
            if (userByEntity != null) {
                response.setResponseCode(ResponseCodes.IN_USE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.inuse", new Object[]{"Entity", entity.getEntityName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the ticket type is in use
            List<ServiceUnit> serviceUnitByEntity = xticketRepository.getServiceUnitUsingEntity(entity);
            if (serviceUnitByEntity != null) {
                response.setResponseCode(ResponseCodes.IN_USE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.inuse", new Object[]{"Entity", entity.getEntityName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            xticketRepository.deleteEntities(entity);
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Entity" + entity.getEntityName(), "Deleted"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            genericService.logResponse(principal, entity.getId(), "Delete", "Entity", "Delete Entity " + entity.getEntityName(), entity.getEntityName(), "");
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    /**
     * Department Transactions *
     */
    @Override
    public XTicketPayload fetchTicketByDepartment(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            List<Tickets> tickets = xticketRepository.getViolatedTickets(LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()));
            if (tickets != null) {
                //Check if service unit is set in the filter
                if (!requestPayload.getServiceUnitCode().equalsIgnoreCase("")) {
                    ServiceUnit serviceUnit = xticketRepository.getServiceUnitUsingCode(requestPayload.getServiceUnitCode());
                    if (serviceUnit != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketType().getServiceUnit() == serviceUnit).collect(Collectors.toList());
                    }
                }

                //Check if ticket group is set in the filter
                if (!requestPayload.getTicketGroupCode().equalsIgnoreCase("")) {
                    TicketGroup ticketGroup = xticketRepository.getTicketGroupUsingCode(requestPayload.getTicketGroupCode());
                    if (ticketGroup != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketGroup() == ticketGroup).collect(Collectors.toList());
                    }
                }

                //Check if ticket type is set in the filter
                if (!requestPayload.getTicketTypeCode().equalsIgnoreCase("")) {
                    TicketType ticketType = xticketRepository.getTicketTypeUsingCode(requestPayload.getTicketTypeCode());
                    if (ticketType != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketType() == ticketType).collect(Collectors.toList());
                    }
                }

                //Check if user type is set 
                if (!requestPayload.getSource().equalsIgnoreCase("")) {
                    if (requestPayload.getSource().equalsIgnoreCase("Internal")) {
                        tickets = tickets.stream().filter(t -> t.isInternal() == true).collect(Collectors.toList());
                    } else {
                        tickets = tickets.stream().filter(t -> t.isInternal() == false).collect(Collectors.toList());
                    }
                }

                //Check if ticket agent filter is set
                if (!requestPayload.getEmail().equalsIgnoreCase("")) {
                    var appUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
                    if (appUser != null) {
                        List<TicketAgent> agentTickets = xticketRepository.getTicketAgent(appUser);
                        if (agentTickets != null) {
                            for (TicketAgent ag : agentTickets) {
                                tickets = tickets.stream().filter(t -> t.getTicketAgent() == ag).collect(Collectors.toList());
                            }
                        }
                    }
                }

                //Loop through the list and transform
                List<XTicketPayload> data = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));
                    newTicket.setCreatedBy(t.getCreatedBy().getLastName() + ", " + t.getCreatedBy().getOtherName());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setServiceUnitName(t.getTicketType().getServiceUnit().getServiceUnitName());
                    newTicket.setPriority(t.getPriority());
                    newTicket.setSlaExpiry(dtf.format(t.getSlaExpiry()));
                    newTicket.setInternal(t.isInternal());
                    newTicket.setInitialSla(t.getSla());
                    newTicket.setTicketAgent(t.getTicketAgentViolated().getAgent().getLastName() + ", " + t.getTicketAgentViolated().getAgent().getOtherName());
                    data.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
                response.setData(data);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Ticket With Violated SLA"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketByDepartmentToEntity(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            //Check if the entity exist using code
            Department departmentByCode = xticketRepository.getDepartmentUsingCode(requestPayload.getDepartmentCode());
            if (departmentByCode == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Department", "Code", requestPayload.getDepartmentCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Fetch the open ticket status
            TicketStatus ticketStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
            if (ticketStatus == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"CLOSED"}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            List<Tickets> tickets = xticketRepository.getClosedTickets(LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()), ticketStatus, departmentByCode);
            if (tickets != null) {
                //Check if entity is set in the filter
                if (!requestPayload.getToEntity().equalsIgnoreCase("")) {
                    Entities toEntity = xticketRepository.getEntitiesUsingCode(requestPayload.getToEntity());
                    if (toEntity != null) {
                        tickets = tickets.stream().filter(t -> t.getEntity() == toEntity).collect(Collectors.toList());
                    }
                }

                //Loop through the list and transform
                List<XTicketPayload> data = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));

                    //Check if the ticket was reopened
                    LocalDateTime closedDate;
                    String closedBy;
                    if (t.isTicketReopen()) {
                        //Get the last reopened record
                        TicketReopened reopenedTicket = xticketRepository.getMostRecentTicketReopenedUsingTicket(t);
                        closedDate = reopenedTicket.getClosedAt();
                        closedBy = reopenedTicket.getClosedBy().getLastName() + ", " + reopenedTicket.getClosedBy().getOtherName();
                    } else {
                        closedDate = t.getClosedAt();
                        closedBy = t.getClosedBy().getLastName() + ", " + t.getClosedBy().getOtherName();
                    }

                    newTicket.setClosedAt(dtf.format(closedDate));
                    newTicket.setClosedBy(closedBy);
                    newTicket.setFromEntity(t.getTicketAgent().getAgent().getEntity().getEntityName());
                    newTicket.setToEntity(t.getEntity().getEntityName());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setSlaExpiry(dtf.format(t.getSlaExpiry()));
                    newTicket.setSlaViolated(t.isSlaViolated());
                    newTicket.setRating(t.getRating());
                    newTicket.setInitialSla(t.getSla().replace("D", " Day(s)").replace("M", " Minute(s)").replace("H", " Hour(s)"));

                    //Fetch the count of the tickets reassigned
                    List<TicketReassign> ticketCount = xticketRepository.getTicketReassignedUsingTicket(t);
                    newTicket.setTicketReassignedCount(ticketCount == null ? 0 : ticketCount.size());

                    //Fetch the number of escalations
                    List<TicketEscalations> ticketEscalations = xticketRepository.getTicketEscalationUsingTicket(t);
                    newTicket.setTicketEscalationCount(ticketEscalations == null ? 0 : ticketEscalations.size());
                    data.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{tickets.size()}, Locale.ENGLISH));
                response.setData(data);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{" selected"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchDepartment() {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<Department> department = xticketRepository.getDepartment();
            if (department != null) {
                for (Department t : department) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(dtf.format(t.getCreatedAt()));
                    payload.setCreatedBy(t.getCreatedBy());
                    payload.setId(t.getId().intValue());
                    payload.setEntityName(t.getEntity().getEntityName());
                    payload.setEntityCode(t.getEntity().getEntityCode());
                    data.add(payload);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchDepartment(String id) {
        var response = new XTicketPayload();
        try {
            Department department = xticketRepository.getDepartmentUsingId(Long.parseLong(id));
            if (department == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            BeanUtils.copyProperties(department, response);
            response.setCreatedAt(department.getCreatedAt().toLocalDate().toString());
            response.setId(department.getId().intValue());
            response.setEntityName(department.getEntity().getEntityName());
            response.setEntityCode(department.getEntity().getEntityCode());

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload createDepartment(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the request is for update or create new
            if (requestPayload.getId() == 0) {
                //Check if the user is valid
                var appUser = xticketRepository.getAppUserUsingEmail(principal);
                if (appUser == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check if the department exist using code
                Department departmentByCode = xticketRepository.getDepartmentUsingCode(requestPayload.getDepartmentCode());
                if (departmentByCode != null) {
                    response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Department", "Code", requestPayload.getDepartmentCode()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check the deprtment using the name
                Department departmentByName = xticketRepository.getDepartmentUsingName(requestPayload.getDepartmentName());
                if (departmentByName != null) {
                    response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Department", "Name", requestPayload.getDepartmentName()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check the entity
                Entities entity = xticketRepository.getEntitiesUsingCode(requestPayload.getEntityCode());
                if (entity == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{requestPayload.getEntityCode()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                Department newDepartment = new Department();
                newDepartment.setCreatedAt(LocalDateTime.now());
                newDepartment.setCreatedBy(principal);
                newDepartment.setStatus(requestPayload.getStatus());
                newDepartment.setDepartmentCode(requestPayload.getDepartmentCode());
                newDepartment.setDepartmentName(requestPayload.getDepartmentName());
                newDepartment.setEntity(entity);
                xticketRepository.createDepartment(newDepartment);

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Department", "Created"}, Locale.ENGLISH));
                response.setData(null);

                StringBuilder newValue = new StringBuilder();
                newValue.append("Department Code:").append(requestPayload.getDepartmentCode()).append(", ")
                        .append("Department Name:").append(requestPayload.getDepartmentName())
                        .append("Entity Name:").append(requestPayload.getEntityName());
                //Log the response
                genericService.logResponse(principal, newDepartment.getId(), "Create", "Department", "Create Department " + requestPayload.getDepartmentName(), "", newValue.toString());
                return response;
            }

            //This is an update request
            Department department = xticketRepository.getDepartmentUsingId(Long.valueOf(requestPayload.getId()));
            if (department == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Department", "Id", requestPayload.getId()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the department exist using code
            Department departmentByCode = xticketRepository.getDepartmentUsingCode(requestPayload.getDepartmentCode());
            if (departmentByCode != null && !Objects.equals(department.getId(), departmentByCode.getId())) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Department", "Code", requestPayload.getDepartmentCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check the department using the name
            Department departmentByName = xticketRepository.getDepartmentUsingName(requestPayload.getDepartmentName());
            if (departmentByName != null && !Objects.equals(department.getId(), departmentByName.getId())) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Department", "Name", requestPayload.getDepartmentName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check the entity
            Entities entity = xticketRepository.getEntitiesUsingCode(requestPayload.getEntityCode());
            if (entity == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{requestPayload.getEntityCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            StringBuilder oldValue = new StringBuilder();
            oldValue.append("Department Code:").append(department.getDepartmentCode()).append(", ")
                    .append("Department Name:").append(department.getDepartmentName()).append(", ")
                    .append("Entity Name:").append(department.getEntity().getEntityName());

            department.setStatus(requestPayload.getStatus());
            department.setDepartmentCode(requestPayload.getDepartmentCode());
            department.setDepartmentName(requestPayload.getDepartmentName());
            department.setEntity(entity);
            xticketRepository.updateDepartment(department);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Department", "Updated"}, Locale.ENGLISH));
            response.setData(null);

            StringBuilder newValue = new StringBuilder();
            newValue.append("Department Code:").append(requestPayload.getDepartmentCode()).append(", ")
                    .append("Department Name:").append(requestPayload.getDepartmentName()).append(", ")
                    .append("Entity Name:").append(entity.getEntityName());
            //Log the response
            genericService.logResponse(principal, department.getId(), "Update", "Department", "Update Department " + requestPayload.getDepartmentName(), oldValue.toString(), newValue.toString());
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload deleteDepartment(String id, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the department by Id is valid
            Department department = xticketRepository.getDepartmentUsingId(Long.valueOf(id));
            if (department == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Department", "Id", id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check service units related to the department
            List<ServiceUnit> serviceUnitByDepartment = xticketRepository.getServiceUnitUsingDepartment(department);
            if (serviceUnitByDepartment != null) {
                response.setResponseCode(ResponseCodes.IN_USE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.inuse", new Object[]{"Entity", department.getDepartmentName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            xticketRepository.deleteDepartment(department);
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Department" + department.getDepartmentName(), "Deleted"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            genericService.logResponse(principal, department.getId(), "Delete", "Department", "Delete Department " + department.getDepartmentName(), department.getDepartmentName(), "");
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    /**
     * Service Unit Transactions *
     */
    @Override
    public XTicketPayload fetchTicketByServiceUnit(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            //Check if the service unit exist using code
            ServiceUnit serviceUnit = xticketRepository.getServiceUnitUsingCode(requestPayload.getServiceUnitCode());
            if (serviceUnit == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Service Unit", "Code", requestPayload.getServiceUnitCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            List<Tickets> tickets = xticketRepository.getTicketsByServiceUnit(LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()), serviceUnit);
            if (tickets != null) {
                //Check if ticket group is set in the filter
                if (!requestPayload.getTicketGroupCode().equalsIgnoreCase("")) {
                    TicketGroup ticketGroup = xticketRepository.getTicketGroupUsingCode(requestPayload.getTicketGroupCode());
                    if (ticketGroup != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketGroup() == ticketGroup).collect(Collectors.toList());
                    }
                }

                //Check if ticket type is set in the filter
                if (!requestPayload.getTicketTypeCode().equalsIgnoreCase("")) {
                    TicketType ticketType = xticketRepository.getTicketTypeUsingCode(requestPayload.getTicketTypeCode());
                    if (ticketType != null) {
                        tickets = tickets.stream().filter(t -> t.getTicketType() == ticketType).collect(Collectors.toList());
                    }
                }

                //Check if user type is set 
                if (!requestPayload.getSource().equalsIgnoreCase("")) {
                    if (requestPayload.getSource().equalsIgnoreCase("Internal")) {
                        tickets = tickets.stream().filter(t -> t.isInternal() == true).collect(Collectors.toList());
                    } else {
                        tickets = tickets.stream().filter(t -> t.isInternal() == false).collect(Collectors.toList());
                    }
                }

                //Check if ticket agent filter is set
                if (!requestPayload.getEmail().equalsIgnoreCase("")) {
                    var appUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
                    if (appUser != null) {
                        List<TicketAgent> agentTickets = xticketRepository.getTicketAgent(appUser);
                        if (agentTickets != null) {
                            for (TicketAgent ag : agentTickets) {
                                tickets = tickets.stream().filter(t -> t.getTicketAgent() == ag).collect(Collectors.toList());
                            }
                        }
                    }
                }

                //Loop through the list and transform
                List<XTicketPayload> data = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));
                    newTicket.setCreatedBy(t.getCreatedBy().getLastName() + ", " + t.getCreatedBy().getOtherName());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setServiceUnitName(t.getTicketType().getServiceUnit().getServiceUnitName());
                    newTicket.setPriority(t.getPriority());
                    newTicket.setSlaExpiry(dtf.format(t.getSlaExpiry()));
                    newTicket.setInternal(t.isInternal());
                    newTicket.setInitialSla(t.getSla().replace("D", " Day(s)").replace("M", " Minute(s)").replace("H", " Hour(s)"));
                    newTicket.setTicketAgent(t.getTicketAgent().getAgent().getLastName() + ", " + t.getTicketAgent().getAgent().getOtherName());
                    newTicket.setRating(t.getRating());

                    //Fetch the count of the tickets reassigned
                    List<TicketReassign> ticketCount = xticketRepository.getTicketReassignedUsingTicket(t);
                    newTicket.setTicketReassignedCount(ticketCount == null ? 0 : ticketCount.size());

                    //Fetch the number of escalations
                    List<TicketEscalations> ticketEscalations = xticketRepository.getTicketEscalationUsingTicket(t);
                    newTicket.setTicketEscalationCount(ticketEscalations == null ? 0 : ticketEscalations.size());
                    data.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
                response.setData(data);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Service Unit"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketByServiceUnitToEntity(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            //Check if the service unit exist using code
            ServiceUnit serviceUnit = xticketRepository.getServiceUnitUsingCode(requestPayload.getServiceUnitCode());
            if (serviceUnit == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Service Unit", "Code", requestPayload.getServiceUnitCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Fetch the open ticket status
            TicketStatus ticketStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
            if (ticketStatus == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"CLOSED"}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            List<Tickets> tickets = xticketRepository.getTicketsByServiceUnit(LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()), serviceUnit, ticketStatus);
            if (tickets != null) {
                //Check if ticket group is set in the filter
                if (!requestPayload.getToEntity().equalsIgnoreCase("")) {
                    Entities entity = xticketRepository.getEntitiesUsingCode(requestPayload.getToEntity());
                    if (entity != null) {
                        tickets = tickets.stream().filter(t -> t.getEntity() == entity).collect(Collectors.toList());
                    }
                }

                //Loop through the list and transform
                List<XTicketPayload> data = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));

                    //Check if the ticket was reopened
                    LocalDateTime closedDate;
                    String closedBy;
                    if (t.isTicketReopen()) {
                        //Get the last reopened record
                        TicketReopened reopenedTicket = xticketRepository.getMostRecentTicketReopenedUsingTicket(t);
                        closedDate = reopenedTicket.getClosedAt();
                        closedBy = reopenedTicket.getClosedBy().getLastName() + ", " + reopenedTicket.getClosedBy().getOtherName();
                    } else {
                        closedDate = t.getClosedAt();
                        closedBy = t.getClosedBy().getLastName() + ", " + t.getClosedBy().getOtherName();
                    }

                    newTicket.setClosedAt(dtf.format(closedDate));
                    newTicket.setClosedBy(closedBy);
                    newTicket.setServiceUnitName(t.getTicketAgent().getTicketType().getServiceUnit().getServiceUnitName());
                    newTicket.setEntityName(t.getEntity().getEntityName());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setSlaExpiry(dtf.format(t.getSlaExpiry()));
                    newTicket.setSlaViolated(t.isSlaViolated());
                    newTicket.setInitialSla(t.getSla().replace("D", " Day(s)").replace("M", " Minute(s)").replace("H", " Hour(s)"));
                    newTicket.setRating(t.getRating());

                    //Fetch the count of the tickets reassigned
                    List<TicketReassign> ticketCount = xticketRepository.getTicketReassignedUsingTicket(t);
                    newTicket.setTicketReassignedCount(ticketCount == null ? 0 : ticketCount.size());

                    //Fetch the number of escalations
                    List<TicketEscalations> ticketEscalations = xticketRepository.getTicketEscalationUsingTicket(t);
                    newTicket.setTicketEscalationCount(ticketEscalations == null ? 0 : ticketEscalations.size());
                    data.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{tickets.size()}, Locale.ENGLISH));
                response.setData(data);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Service Unit"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketByServiceRating(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            //Fetch the open ticket status
            TicketStatus ticketStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
            if (ticketStatus == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"CLOSED"}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            List<XTicketPayload> data = new ArrayList<>();
            if (!requestPayload.getEntityCode().equalsIgnoreCase("")) {
                Entities entity = xticketRepository.getEntitiesUsingCode(requestPayload.getEntityCode());
                if (entity != null) {
                    List<Department> departmentsInEntity = xticketRepository.getDepartmentUsingEntity(entity);
                    if (departmentsInEntity != null) {
                        for (Department d : departmentsInEntity) {
                            List<Tickets> tickets = xticketRepository.getClosedTickets(LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()), ticketStatus, d);
                            if (tickets != null) {
                                XTicketPayload newTicket = new XTicketPayload();
                                newTicket.setDepartmentName(d.getDepartmentName());
                                newTicket.setServiceUnitName("NA");
                                newTicket.setTicketCount(tickets.size());
                                newTicket.setFiveStar(tickets.stream().filter(f -> f.getRating() == 5).mapToInt(t -> t.getRating()).sum());
                                newTicket.setFourStar(tickets.stream().filter(f -> f.getRating() == 4).mapToInt(t -> t.getRating()).sum());
                                newTicket.setThreeStar(tickets.stream().filter(f -> f.getRating() == 3).mapToInt(t -> t.getRating()).sum());
                                newTicket.setTwoStar(tickets.stream().filter(f -> f.getRating() == 2).mapToInt(t -> t.getRating()).sum());
                                newTicket.setOneStar(tickets.stream().filter(f -> f.getRating() == 1).mapToInt(t -> t.getRating()).sum());
                                newTicket.setRating(tickets.stream().mapToInt(t -> t.getRating()).sum());
                                newTicket.setRatingAverage(tickets.stream().mapToInt(t -> t.getRating()).sum() / tickets.size());
                                newTicket.setEntityName(d.getEntity().getEntityName());
                                data.add(newTicket);
                            }
                        }

                        response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                        response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
                        response.setData(data);
                        return response;
                    }
                }

                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Entity"}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            if (!requestPayload.getDepartmentCode().equalsIgnoreCase("")) {
                Department department = xticketRepository.getDepartmentUsingCode(requestPayload.getDepartmentCode());
                if (department != null) {
                    //Get the service units in the department
                    List<ServiceUnit> serviceUnits = xticketRepository.getServiceUnit();
                    if (serviceUnits != null) {
                        for (ServiceUnit s : serviceUnits) {
                            List<Tickets> tickets = xticketRepository.getTicketsByServiceUnit(LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()), s, ticketStatus);
                            if (tickets != null) {
                                XTicketPayload newTicket = new XTicketPayload();
                                newTicket.setDepartmentName(s.getDepartment().getDepartmentName());
                                newTicket.setServiceUnitName(s.getServiceUnitName());
                                newTicket.setTicketCount(tickets.size());
                                newTicket.setFiveStar(tickets.stream().filter(f -> f.getRating() == 5).mapToInt(t -> t.getRating()).sum());
                                newTicket.setFourStar(tickets.stream().filter(f -> f.getRating() == 4).mapToInt(t -> t.getRating()).sum());
                                newTicket.setThreeStar(tickets.stream().filter(f -> f.getRating() == 3).mapToInt(t -> t.getRating()).sum());
                                newTicket.setTwoStar(tickets.stream().filter(f -> f.getRating() == 2).mapToInt(t -> t.getRating()).sum());
                                newTicket.setOneStar(tickets.stream().filter(f -> f.getRating() == 1).mapToInt(t -> t.getRating()).sum());
                                newTicket.setRating(tickets.stream().mapToInt(t -> t.getRating()).sum());
                                newTicket.setRatingAverage(tickets.stream().mapToInt(t -> t.getRating()).sum() / tickets.size());
                                newTicket.setEntityName(s.getDepartment().getEntity().getEntityName());
                                data.add(newTicket);
                            }
                        }
                        response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                        response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
                        response.setData(data);
                        return response;
                    }
                }

                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Department"}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"Empty Parameter"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchServiceUnit() {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<ServiceUnit> serviceUnit = xticketRepository.getServiceUnit();
            if (serviceUnit != null) {
                for (ServiceUnit t : serviceUnit) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(dtf.format(t.getCreatedAt()));
                    payload.setCreatedBy(t.getCreatedBy());
                    payload.setId(t.getId().intValue());
                    payload.setDepartmentName(t.getDepartment().getDepartmentName());
                    payload.setDepartmentCode(t.getDepartment().getDepartmentCode());
                    data.add(payload);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchServiceUnit(String id) {
        var response = new XTicketPayload();
        try {
            ServiceUnit serviceUnit = xticketRepository.getServiceUnitUsingId(Long.parseLong(id));
            if (serviceUnit == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            BeanUtils.copyProperties(serviceUnit, response);
            response.setCreatedAt(dtf.format(serviceUnit.getCreatedAt()));
            response.setId(serviceUnit.getId().intValue());
            response.setDepartmentName(serviceUnit.getDepartment().getDepartmentName());
            response.setDepartmentCode(serviceUnit.getDepartment().getDepartmentCode());

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload createServiceUnit(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the request is for update or create new
            if (requestPayload.getId() == 0) {
                //Check if the user is valid
                var appUser = xticketRepository.getAppUserUsingEmail(principal);
                if (appUser == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check if the entity exist using code
                Department departmentByCode = xticketRepository.getDepartmentUsingCode(requestPayload.getDepartmentCode());
                if (departmentByCode == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Department", "Code", requestPayload.getDepartmentCode()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check if the service unit exist using code
                ServiceUnit serviceUnitByCode = xticketRepository.getServiceUnitUsingCode(requestPayload.getServiceUnitCode());
                if (serviceUnitByCode != null) {
                    response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Service Unit", "Code", requestPayload.getServiceUnitCode()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check the service unit using the name
                ServiceUnit serviceUnitByName = xticketRepository.getServiceUnitUsingName(requestPayload.getServiceUnitName());
                if (serviceUnitByName != null) {
                    response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Service Unit", "Name", requestPayload.getServiceUnitName()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                ServiceUnit newServiceUnit = new ServiceUnit();
                newServiceUnit.setCreatedAt(LocalDateTime.now());
                newServiceUnit.setCreatedBy(principal);
                newServiceUnit.setDepartment(departmentByCode);
                newServiceUnit.setStatus(requestPayload.getStatus());
                newServiceUnit.setServiceUnitCode(requestPayload.getServiceUnitCode());
                newServiceUnit.setServiceUnitName(requestPayload.getServiceUnitName());
                xticketRepository.createServiceUnit(newServiceUnit);

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Service Unit", "Created"}, Locale.ENGLISH));
                response.setData(null);

                StringBuilder newValue = new StringBuilder();
                newValue.append("Service Unit Code:").append(requestPayload.getServiceUnitCode()).append(", ")
                        .append("Service Unit Name:").append(requestPayload.getServiceUnitName());
                //Log the response
                genericService.logResponse(principal, newServiceUnit.getId(), "Update", "Service Unit", "Update Entity " + requestPayload.getServiceUnitName(), "", newValue.toString());
                return response;
            }

            //This is an update request
            ServiceUnit serviceUnit = xticketRepository.getServiceUnitUsingId(Long.valueOf(requestPayload.getId()));
            if (serviceUnit == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Service Unit", "Id", requestPayload.getId()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the department exist using code
            Department departmentByCode = xticketRepository.getDepartmentUsingCode(requestPayload.getDepartmentCode());
            if (departmentByCode == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Department", "Code", requestPayload.getDepartmentCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the service unit exist using code
            ServiceUnit serviceUnitByCode = xticketRepository.getServiceUnitUsingCode(requestPayload.getServiceUnitCode());
            if (serviceUnitByCode != null && !Objects.equals(serviceUnit.getId(), serviceUnitByCode.getId())) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Service Unit", "Code", requestPayload.getServiceUnitCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check the service unit using the name
            ServiceUnit serviceUnitByName = xticketRepository.getServiceUnitUsingName(requestPayload.getServiceUnitName());
            if (serviceUnitByName != null && !Objects.equals(serviceUnit.getId(), serviceUnitByName.getId())) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Service Unit", "Name", requestPayload.getServiceUnitName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            StringBuilder oldValue = new StringBuilder();
            oldValue.append("Service Unit Code:").append(serviceUnit.getServiceUnitCode()).append(", ")
                    .append("Service Unit Name:").append(serviceUnit.getServiceUnitName()).append(", ")
                    .append("Department Name:").append(serviceUnit.getDepartment().getDepartmentName());

            serviceUnit.setDepartment(departmentByCode);
            serviceUnit.setStatus(requestPayload.getStatus());
            serviceUnit.setServiceUnitCode(requestPayload.getServiceUnitCode());
            serviceUnit.setServiceUnitName(requestPayload.getServiceUnitName());
            xticketRepository.updateServiceUnit(serviceUnit);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Service Unit", "Updated"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            StringBuilder newValue = new StringBuilder();
            newValue.append("Service Unit Code:").append(requestPayload.getServiceUnitCode()).append(", ")
                    .append("Service Unit Name:").append(requestPayload.getServiceUnitName()).append(", ")
                    .append("Department Name:").append(departmentByCode.getDepartmentName());
            //Log the response
            genericService.logResponse(principal, serviceUnit.getId(), "Update", "Service Unit", "Update Entity " + requestPayload.getServiceUnitName(), oldValue.toString(), newValue.toString());
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload deleteServiceUnit(String id, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the service unit by Id is valid
            ServiceUnit serviceUnit = xticketRepository.getServiceUnitUsingId(Long.valueOf(id));
            if (serviceUnit == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Service Unit", "Id", id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the ticket type is in use
            List<TicketType> ticketByType = xticketRepository.getTicketTypeUsingServiceUnit(serviceUnit);
            if (ticketByType != null) {
                response.setResponseCode(ResponseCodes.IN_USE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.inuse", new Object[]{"Service Unit", serviceUnit.getServiceUnitName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            xticketRepository.deleteServiceUnit(serviceUnit);
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Service Unit" + serviceUnit.getServiceUnitName(), "Deleted"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            genericService.logResponse(principal, serviceUnit.getId(), "Delete", "Service Unit", "Delete Service Unit " + serviceUnit.getServiceUnitName(), serviceUnit.getServiceUnitName(), "");
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    /**
     * Ticket Status *
     */
    @Override
    public XTicketPayload fetchTicketStatus() {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<TicketStatus> ticketStatus = xticketRepository.getTicketStatus();
            if (ticketStatus != null) {
                for (TicketStatus t : ticketStatus) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(dtf.format(t.getCreatedAt()));
                    payload.setCreatedBy(t.getCreatedBy());
                    payload.setId(t.getId().intValue());
                    data.add(payload);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketStatus(String id) {
        var response = new XTicketPayload();
        try {
            TicketStatus ticketStatus = xticketRepository.getTicketStatusUsingId(Long.parseLong(id));
            if (ticketStatus == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            BeanUtils.copyProperties(ticketStatus, response);
            response.setCreatedAt(ticketStatus.getCreatedAt().toLocalDate().toString());
            response.setId(ticketStatus.getId().intValue());

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketStatusForReply() {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<TicketStatus> ticketStatus = xticketRepository.getTicketStatus();
            if (ticketStatus != null) {
                for (TicketStatus t : ticketStatus) {
                    if (!t.getTicketStatusCode().equalsIgnoreCase("OPEN") && !t.getTicketStatusCode().equalsIgnoreCase("CLOSED")) {
                        XTicketPayload payload = new XTicketPayload();
                        BeanUtils.copyProperties(t, payload);
                        payload.setCreatedAt(dtf.format(t.getCreatedAt()));
                        payload.setCreatedBy(t.getCreatedBy());
                        payload.setId(t.getId().intValue());
                        data.add(payload);
                    }
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload createTicketStatus(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the request is for update or create new
            if (requestPayload.getId() == 0) {
                //Check if the user is valid
                var appUser = xticketRepository.getAppUserUsingEmail(principal);
                if (appUser == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check if the ticket status exist using code
                TicketStatus ticketStatusByCode = xticketRepository.getTicketStatusUsingCode(requestPayload.getTicketStatusCode());
                if (ticketStatusByCode != null) {
                    response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Ticket Status", "Code", requestPayload.getTicketStatusCode()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check the ticket group using the name
                TicketStatus ticketStatusByName = xticketRepository.getTicketStatusUsingName(requestPayload.getTicketStatusName());
                if (ticketStatusByName != null) {
                    response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Ticket Status", "Name", requestPayload.getTicketStatusName()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                TicketStatus newTicketStatus = new TicketStatus();
                newTicketStatus.setCreatedAt(LocalDateTime.now());
                newTicketStatus.setCreatedBy(principal);
                newTicketStatus.setStatus(requestPayload.getStatus());
                newTicketStatus.setTicketStatusCode(requestPayload.getTicketStatusCode());
                newTicketStatus.setTicketStatusName(requestPayload.getTicketStatusName());
                newTicketStatus.setPauseSLA(requestPayload.isPauseSLA());
                xticketRepository.createTicketStatus(newTicketStatus);

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket Status", "Created"}, Locale.ENGLISH));
                response.setData(null);

                StringBuilder newValue = new StringBuilder();
                newValue.append("Ticket Status Code:").append(requestPayload.getTicketStatusCode()).append(", ")
                        .append("Ticket Status Name:").append(requestPayload.getTicketStatusName());
                return response;
            }

            //This is an update request
            TicketStatus ticketStatus = xticketRepository.getTicketStatusUsingId(Long.valueOf(requestPayload.getId()));
            if (ticketStatus == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Ticket Status", "Id", requestPayload.getId()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the ticket status exist using code
            TicketStatus ticketStatusByCode = xticketRepository.getTicketStatusUsingCode(requestPayload.getTicketStatusCode());
            if (ticketStatusByCode != null && !Objects.equals(ticketStatus.getId(), ticketStatusByCode.getId())) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Ticket Status", "Code", requestPayload.getTicketStatusCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check the ticket status using the name
            TicketStatus ticketStatusByName = xticketRepository.getTicketStatusUsingName(requestPayload.getTicketStatusName());
            if (ticketStatusByName != null && !Objects.equals(ticketStatus.getId(), ticketStatusByName.getId())) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Ticket Status", "Name", requestPayload.getTicketStatusName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            StringBuilder oldValue = new StringBuilder();
            oldValue.append("Ticket Status Code:").append(ticketStatus.getTicketStatusCode()).append(", ")
                    .append("Ticket Status Name:").append(ticketStatus.getTicketStatusName());

            ticketStatus.setStatus(requestPayload.getStatus());
            ticketStatus.setTicketStatusCode(requestPayload.getTicketStatusCode());
            ticketStatus.setTicketStatusName(requestPayload.getTicketStatusName());
            xticketRepository.updateTicketStatus(ticketStatus);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket Status", "Updated"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            StringBuilder newValue = new StringBuilder();
            newValue.append("Ticket Status Code:").append(requestPayload.getTicketStatusCode()).append(", ")
                    .append("Ticket Status Name:").append(requestPayload.getTicketStatusName());
            //Log the response
            genericService.logResponse(principal, ticketStatus.getId(), "Update", "Ticket Status", "Update Ticket Status " + requestPayload.getTicketStatusName(), oldValue.toString(), newValue.toString());

            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload deleteTicketStatus(String id, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the ticket status by Id is valid
            TicketStatus ticketStatus = xticketRepository.getTicketStatusUsingId(Long.valueOf(id));
            if (ticketStatus == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Ticket Status", "Id", id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the ticket status is in use
            List<Tickets> ticketByStatus = xticketRepository.getTicketsUsingStatus(ticketStatus);
            if (ticketByStatus != null) {
                response.setResponseCode(ResponseCodes.IN_USE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.inuse", new Object[]{"Ticket Status", ticketStatus.getTicketStatusName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Status Open and Completed should not be deleted
            if (ticketStatus.getTicketStatusCode().equalsIgnoreCase("OPEN") || ticketStatus.getTicketStatusCode().equalsIgnoreCase("CLOSED")) {
                response.setResponseCode(ResponseCodes.IN_USE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.inuse", new Object[]{"Ticket Status", ticketStatus.getTicketStatusName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            xticketRepository.deleteTicketStatus(ticketStatus);
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket Status" + ticketStatus.getTicketStatusName(), "Deleted"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            genericService.logResponse(principal, ticketStatus.getId(), "Delete", "Ticket Status", "Delete Ticket Status " + ticketStatus.getTicketStatusName(), ticketStatus.getTicketStatusName(), "");
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    /**
     * Ticket Status *
     */
    @Override
    public XTicketPayload fetchAutomatedTicket() {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<AutomatedTicket> automatedTicket = xticketRepository.getAutomatedTicket();
            if (automatedTicket != null) {
                for (AutomatedTicket t : automatedTicket) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(dtf.format(t.getCreatedAt()));
                    payload.setCreatedBy(t.getServiceRequester());
                    payload.setId(t.getId().intValue());
                    payload.setTicketTypeCode(t.getTicketType().getTicketTypeCode());
                    payload.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    payload.setTicketAgent(t.getServiceProvider());
                    payload.setTicketSlaName(String.valueOf(t.getTicketType().getSla().getTicketSla()) + " " + t.getTicketType().getSla().getTicketSlaPeriod());
                    payload.setStartDate(String.valueOf(t.getStartDate()));
                    payload.setEndDate(String.valueOf(t.getEndDate()));
                    data.add(payload);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchAutomatedTicket(String id) {
        var response = new XTicketPayload();
        try {
            AutomatedTicket automatedTicket = xticketRepository.getAutomatedTicketUsingId(Long.parseLong(id));
            if (automatedTicket == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            BeanUtils.copyProperties(automatedTicket, response);
            response.setCreatedAt(automatedTicket.getCreatedAt().toLocalDate().toString());
            response.setId(automatedTicket.getId().intValue());
            response.setTicketTypeCode(automatedTicket.getTicketType().getTicketTypeCode());
            response.setTicketTypeName(automatedTicket.getTicketType().getTicketTypeName());
            response.setTicketAgent(automatedTicket.getServiceProvider());
            response.setTicketSlaName(String.valueOf(automatedTicket.getTicketType().getSla().getTicketSla()) + " " + automatedTicket.getTicketType().getSla().getTicketSlaPeriod());
            response.setStartDate(String.valueOf(automatedTicket.getStartDate()));
            response.setEndDate(String.valueOf(automatedTicket.getEndDate()));

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchAutomatedTicketType() {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<TicketType> ticketType = xticketRepository.getAutomatedTicketType();
            if (ticketType != null) {
                for (TicketType t : ticketType) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(dtf.format(t.getCreatedAt()));
                    payload.setCreatedBy(t.getCreatedBy());
                    payload.setId(t.getId().intValue());
                    payload.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    payload.setTicketGroupCode(t.getTicketGroup().getTicketGroupCode());
                    payload.setServiceUnitCode(t.getServiceUnit().getServiceUnitCode());
                    payload.setServiceUnitName(t.getServiceUnit().getServiceUnitName());
                    payload.setInitialSla(t.getSla().getTicketSlaPeriod() == 'D' ? t.getSla().getTicketSla() + " Day(s)"
                            : t.getSla().getTicketSlaPeriod() == 'M' ? t.getSla().getTicketSla() + " Minute(s)" : t.getSla().getTicketSla() + " Hour(s)");
                    data.add(payload);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchTicketByAutomation(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            //Check if the entity exist using code
            Entities entityByCode = xticketRepository.getEntitiesUsingCode(requestPayload.getFromEntity());
            if (entityByCode == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Entity", "Code", requestPayload.getEntityCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Fetch the open ticket status
            TicketStatus ticketStatus = xticketRepository.getTicketStatusUsingCode("CLOSED");
            if (ticketStatus == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{"CLOSED"}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            List<Tickets> tickets = xticketRepository.getClosedAutomatedTickets(LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()), ticketStatus);
            if (tickets != null) {
                //Check if service unit is set in the filter
                if (!requestPayload.getToEntity().equalsIgnoreCase("")) {
                    Entities toEntity = xticketRepository.getEntitiesUsingCode(requestPayload.getToEntity());
                    if (toEntity != null) {
                        tickets = tickets.stream().filter(t -> t.getEntity() == toEntity).collect(Collectors.toList());
                    }
                }

                //Loop through the list and transform
                List<XTicketPayload> data = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(dtf.format(t.getCreatedAt()));

                    //Check if the ticket was reopened
                    LocalDateTime closedDate;
                    String closedBy;
                    if (t.isTicketReopen()) {
                        //Get the last reopened record
                        TicketReopened reopenedTicket = xticketRepository.getMostRecentTicketReopenedUsingTicket(t);
                        closedDate = reopenedTicket.getClosedAt();
                        closedBy = reopenedTicket.getClosedBy().getLastName() + ", " + reopenedTicket.getClosedBy().getOtherName();
                    } else {
                        closedDate = t.getClosedAt();
                        closedBy = t.getClosedBy().getLastName() + ", " + t.getClosedBy().getOtherName();
                    }

                    newTicket.setClosedAt(dtf.format(closedDate));
                    newTicket.setClosedBy(closedBy);
                    newTicket.setFromEntity(t.getTicketAgent().getAgent().getEntity().getEntityName());
                    newTicket.setToEntity(t.getEntity().getEntityName());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setSlaExpiry(dtf.format(t.getSlaExpiry()));
                    newTicket.setSlaViolated(t.isSlaViolated());
                    newTicket.setRating(t.getRating());
                    newTicket.setInitialSla(t.getSla().replace("D", " Day(s)").replace("M", " Minute(s)").replace("H", " Hour(s)"));

                    //Fetch the count of the tickets reassigned
                    List<TicketReassign> ticketCount = xticketRepository.getTicketReassignedUsingTicket(t);
                    newTicket.setTicketReassignedCount(ticketCount == null ? 0 : ticketCount.size());

                    //Fetch the number of escalations
                    List<TicketEscalations> ticketEscalations = xticketRepository.getTicketEscalationUsingTicket(t);
                    newTicket.setTicketEscalationCount(ticketEscalations == null ? 0 : ticketEscalations.size());
                    data.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{tickets.size()}, Locale.ENGLISH));
                response.setData(data);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{" selected"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload createAutomatedTicket(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the request is for update or create new
            if (requestPayload.getId() == 0) {
                //Check if the user is valid
                var appUser = xticketRepository.getAppUserUsingEmail(principal);
                if (appUser == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check if the automated ticket exist using ticket type
                TicketType ticketType = xticketRepository.getTicketTypeUsingCode(requestPayload.getTicketTypeCode());
                if (ticketType == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Ticket Type", "Code", requestPayload.getTicketTypeCode()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check if the service requester is same as service provider
                if (requestPayload.getCreatedBy().equalsIgnoreCase(requestPayload.getTicketAgent())) {
                    response.setResponseCode(ResponseCodes.SAME_ACCOUNT.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.sameagent", new Object[0], Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Determine the agent to assign the ticket to
                TicketAgent ticketAgent = getAgentToAssignTicket(ticketType);
                if (ticketAgent == null) {
                    response.setResponseCode(ResponseCodes.INPUT_MISSING.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.noagent", new Object[]{ticketType.getTicketTypeName()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                LocalDate startDate = LocalDate.parse(requestPayload.getStartDate());
                LocalDate nextRun = null;

                //Check the start and end date
                if (startDate.isBefore(LocalDate.now())) {
                    response.setResponseCode(ResponseCodes.OUT_OF_RANGE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.invalid.date", new Object[]{"Start Date", " cannot be less than today"}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                if (!requestPayload.getEndDate().equalsIgnoreCase("")) {
                    LocalDate endDate = LocalDate.parse(requestPayload.getEndDate());
                    if (endDate.isBefore(LocalDate.now()) || endDate.isBefore(startDate)) {
                        response.setResponseCode(ResponseCodes.OUT_OF_RANGE.getResponseCode());
                        response.setResponseMessage(messageSource.getMessage("appMessages.invalid.date", new Object[]{"Start or End Date", " cannot be in the past"}, Locale.ENGLISH));
                        response.setData(null);
                        return response;
                    }
                }

                switch (requestPayload.getFrequency()) {
                    case "Daily" -> {
                        nextRun = startDate.plusDays(1);
                    }
                    case "Weekly" -> {
                        nextRun = startDate.plusWeeks(1);
                    }
                    case "Monthly" -> {
                        nextRun = startDate.plusMonths(1);
                    }
                    case "Annual" -> {
                        nextRun = startDate.plusYears(1);
                    }
                    case "BiAnnual" -> {
                        nextRun = startDate.plusDays(183);
                    }
                    case "Quarterly" -> {
                        nextRun = startDate.plusDays(90);
                    }
                }

                AutomatedTicket newAutomatedTicket = new AutomatedTicket();
                newAutomatedTicket.setCreatedAt(LocalDateTime.now());
                newAutomatedTicket.setCreatedBy(principal);
                newAutomatedTicket.setStatus(requestPayload.getStatus());
                newAutomatedTicket.setEndDate(requestPayload.getEndDate().equalsIgnoreCase("") ? null : LocalDate.parse(requestPayload.getEndDate()));
                newAutomatedTicket.setEscalationEmails(requestPayload.getEscalationEmails());
                newAutomatedTicket.setFrequency(requestPayload.getFrequency());
                newAutomatedTicket.setMessage(requestPayload.getMessage());
                newAutomatedTicket.setNextRun(nextRun);
                newAutomatedTicket.setServiceProvider(requestPayload.getTicketAgent());
                newAutomatedTicket.setServiceRequester(requestPayload.getCreatedBy());
                newAutomatedTicket.setStartDate(LocalDate.parse(requestPayload.getStartDate()));
                newAutomatedTicket.setSubject(requestPayload.getSubject());
                newAutomatedTicket.setTicketType(ticketType);
                xticketRepository.createAutomatedTicket(newAutomatedTicket);

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Automated Ticket", "Created"}, Locale.ENGLISH));
                response.setData(null);

                StringBuilder newValue = new StringBuilder();
                newValue.append("Ticket Type Name:").append(ticketType.getTicketTypeName()).append(", ")
                        .append("Service Requester:").append(requestPayload.getCreatedBy()).append(", ")
                        .append("Service Provider:").append(requestPayload.getTicketAgent()).append(", ")
                        .append("Start Date:").append(requestPayload.getStartDate()).append(", ")
                        .append("End Date:").append(requestPayload.getEndDate()).append(", ")
                        .append("Subject:").append(requestPayload.getSubject()).append(", ")
                        .append("Message:").append(requestPayload.getMessage()).append(", ")
                        .append("Escalation:").append(requestPayload.getEscalationEmails()).append(", ")
                        .append("Frequency:").append(requestPayload.getFrequency()).append(", ");
                return response;
            }

            //This is an update request
            AutomatedTicket automatedTicket = xticketRepository.getAutomatedTicketUsingId(Long.valueOf(requestPayload.getId()));
            if (automatedTicket == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Automated Ticket", "Id", requestPayload.getId()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the ticket type exist using code
            TicketType ticketType = xticketRepository.getTicketTypeUsingCode(requestPayload.getTicketTypeCode());
            if (ticketType != null && !Objects.equals(automatedTicket.getId(), ticketType.getId())) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Automated Ticket", "Code", requestPayload.getTicketTypeCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the service requester is same as service provider
            if (requestPayload.getCreatedBy().equalsIgnoreCase(requestPayload.getTicketAgent())) {
                response.setResponseCode(ResponseCodes.SAME_ACCOUNT.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.sameagent", new Object[0], Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Determine the agent to assign the ticket to
            TicketAgent ticketAgent = getAgentToAssignTicket(ticketType);
            if (ticketAgent == null) {
                response.setResponseCode(ResponseCodes.INPUT_MISSING.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.noagent", new Object[]{ticketType.getTicketTypeName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            StringBuilder oldValue = new StringBuilder();
            oldValue.append("Ticket Type Name:").append(automatedTicket.getTicketType().getTicketTypeName()).append(", ")
                    .append("Service Requester:").append(requestPayload.getCreatedBy()).append(", ")
                    .append("Service Provider:").append(requestPayload.getTicketAgent()).append(", ")
                    .append("Start Date:").append(requestPayload.getStartDate()).append(", ")
                    .append("End Date:").append(requestPayload.getEndDate()).append(", ")
                    .append("Subject:").append(requestPayload.getSubject()).append(", ")
                    .append("Message:").append(requestPayload.getMessage()).append(", ")
                    .append("Escalation:").append(requestPayload.getEscalationEmails()).append(", ")
                    .append("Frequency:").append(requestPayload.getFrequency()).append(", ");

            LocalDate startDate = LocalDate.parse(requestPayload.getStartDate());
            LocalDate nextRun = null;

            //Check the start and end date
            if (startDate.isBefore(LocalDate.now())) {
                response.setResponseCode(ResponseCodes.OUT_OF_RANGE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.invalid.date", new Object[]{"Start Date", " cannot be less than today"}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            if (!requestPayload.getEndDate().equalsIgnoreCase("")) {
                LocalDate endDate = LocalDate.parse(requestPayload.getEndDate());
                if (endDate.isBefore(LocalDate.now()) || endDate.isBefore(startDate)) {
                    response.setResponseCode(ResponseCodes.OUT_OF_RANGE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.invalid.date", new Object[]{"Start or End Date", " cannot be in the past"}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }
            }

            switch (requestPayload.getFrequency()) {
                case "Daily" -> {
                    nextRun = startDate.plusDays(1);
                }
                case "Weekly" -> {
                    nextRun = startDate.plusWeeks(1);
                }
                case "Monthly" -> {
                    nextRun = startDate.plusMonths(1);
                }
                case "Annual" -> {
                    nextRun = startDate.plusYears(1);
                }
                case "BiAnnual" -> {
                    nextRun = startDate.plusDays(183);
                }
                case "Quarterly" -> {
                    nextRun = startDate.plusDays(90);
                }
            }

            automatedTicket.setCreatedAt(LocalDateTime.now());
            automatedTicket.setCreatedBy(principal);
            automatedTicket.setStatus(requestPayload.getStatus());
            automatedTicket.setEndDate(requestPayload.getEndDate().equalsIgnoreCase("") ? null : LocalDate.parse(requestPayload.getEndDate()));
            automatedTicket.setEscalationEmails(requestPayload.getEscalationEmails());
            automatedTicket.setFrequency(requestPayload.getFrequency());
            automatedTicket.setMessage(requestPayload.getMessage());
            automatedTicket.setNextRun(nextRun);
            automatedTicket.setServiceProvider(requestPayload.getTicketAgent());
            automatedTicket.setServiceRequester(requestPayload.getCreatedBy());
            automatedTicket.setStartDate(LocalDate.parse(requestPayload.getStartDate()));
            automatedTicket.setSubject(requestPayload.getSubject());
            automatedTicket.setTicketType(ticketType);
            xticketRepository.updateAutomatedTicket(automatedTicket);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Automated Ticket", "Updated"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            StringBuilder newValue = new StringBuilder();
            newValue.append("Ticket Type Name:").append(ticketType.getTicketTypeName()).append(", ")
                    .append("Service Requester:").append(requestPayload.getCreatedBy()).append(", ")
                    .append("Service Provider:").append(requestPayload.getTicketAgent()).append(", ")
                    .append("Start Date:").append(requestPayload.getStartDate()).append(", ")
                    .append("End Date:").append(requestPayload.getEndDate()).append(", ")
                    .append("Subject:").append(requestPayload.getSubject()).append(", ")
                    .append("Message:").append(requestPayload.getMessage()).append(", ")
                    .append("Escalation:").append(requestPayload.getEscalationEmails()).append(", ")
                    .append("Frequency:").append(requestPayload.getFrequency()).append(", ");
            //Log the response
            genericService.logResponse(principal, automatedTicket.getId(), "Update", "Automated Ticket", "Update Automated Ticket " + requestPayload.getTicketTypeCode(), oldValue.toString(), newValue.toString());

            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload deleteAutomatedTicket(String id, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the automated ticket by Id is valid
            AutomatedTicket automatedTicket = xticketRepository.getAutomatedTicketUsingId(Long.valueOf(id));
            if (automatedTicket == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Automated Ticket", "Id", id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if an automated ticket of type is in use
            Tickets automatedTicketByType = xticketRepository.getAutomatedTicketUsingTicketType(automatedTicket.getTicketType());
            if (automatedTicketByType != null) {
                response.setResponseCode(ResponseCodes.IN_USE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.inuse", new Object[]{"Automated Ticket", automatedTicket.getTicketType().getTicketTypeName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            xticketRepository.deleteAutomatedTicket(automatedTicket);
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Automated Ticket" + automatedTicket.getTicketType().getTicketTypeName(), "Deleted"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            genericService.logResponse(principal, automatedTicket.getId(), "Delete", "Automated Ticket", "Delete Automated Ticket " + automatedTicket.getTicketType().getTicketTypeName(), automatedTicket.getTicketType().getTicketTypeName(), "");
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    private String getTimeElapsed(LocalDateTime startTime, LocalDateTime endTime) {

        Date d0 = Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant());
        Date d1 = Date.from(endTime.atZone(ZoneId.systemDefault()).toInstant());

        // Calculating the time difference in milliseconds  
        long timeDifference = d1.getTime() - d0.getTime();

        // Calculating the time difference in terms of minutes,   
        // hours, seconds, years, and days  
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeDifference) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(timeDifference) % 24;
        long days = TimeUnit.MILLISECONDS.toDays(timeDifference) % 365;
        return days + " days " + hours + " hours " + minutes + " minutes ";
    }

    private LocalDateTime getSlaExpiryDate(TicketType ticketType) {
        LocalDateTime slaExpiry = LocalDateTime.now();
        switch (ticketType.getSla().getTicketSlaPeriod()) {
            case 'D' -> {
                slaExpiry = LocalDateTime.now().plusDays(ticketType.getSla().getTicketSla());
            }
            case 'H' -> {
                slaExpiry = LocalDateTime.now().plusHours(ticketType.getSla().getTicketSla());
            }
            default -> {
                slaExpiry = LocalDateTime.now().plusMinutes(ticketType.getSla().getTicketSla());
            }
        }

        //Check if the sla expiry falls within workday and hours
        if (slaExpiry.getDayOfWeek() != DayOfWeek.SATURDAY && slaExpiry.getDayOfWeek() != DayOfWeek.SUNDAY) {
            //Check the time ticket is raised
            if (slaExpiry.getHour() >= officeOpenHour && slaExpiry.getHour() <= officeCloseHour) {
                //Check if day is public holiday
                if (!isPublicHoliday(slaExpiry.toLocalDate())) {
                    return slaExpiry;
                } else {
                    //Get the next working day
                    return nextWorkingDay(slaExpiry);
                }
            } else {
                //Check if the request is before 8am or after 5pm
                if (slaExpiry.getHour() < officeOpenHour) {
                    if (ticketType.getSla().getTicketSlaPeriod() == 'H') {
                        slaExpiry = slaExpiry.withHour(8 + ticketType.getSla().getTicketSla()).withMinute(0).withSecond(0);
                    }
                    if (ticketType.getSla().getTicketSlaPeriod() == 'M') {
                        slaExpiry = slaExpiry.withHour(8).withMinute(ticketType.getSla().getTicketSla()).withSecond(0);
                    }
                    return slaExpiry.withHour(8).withMinute(0).withSecond(0);
                } else {
                    //Check if the day is friday
                    if (slaExpiry.getDayOfWeek() == DayOfWeek.FRIDAY) {
                        //Outside official worktime. Move ticket to next day
                        LocalDateTime newSlaExpiry = slaExpiry.plusDays(3);
                        //Check if the sla is in minutes or hours
                        if (ticketType.getSla().getTicketSlaPeriod() == 'H') {
                            newSlaExpiry = newSlaExpiry.withHour(8 + ticketType.getSla().getTicketSla()).withMinute(0).withSecond(0);
                        }
                        if (ticketType.getSla().getTicketSlaPeriod() == 'M') {
                            newSlaExpiry = newSlaExpiry.withHour(8).withMinute(ticketType.getSla().getTicketSla()).withSecond(0);
                        }
                        //Check if day is public holiday
                        if (!isPublicHoliday(newSlaExpiry.toLocalDate())) {
                            return newSlaExpiry;
                        } else {
                            //Get the next working day
                            return nextWorkingDay(newSlaExpiry);
                        }
                    } else {
                        //Get the difference in time left
                        int hourDiff = Duration.between(LocalDateTime.now().withHour(17).withMinute(59), slaExpiry).toHoursPart();
                        int minDiff = Duration.between(LocalDateTime.now().withHour(17).withMinute(59), slaExpiry).toMinutesPart();
                        //Outside official worktime. Move ticket to next day
                        LocalDateTime newSlaExpiry = slaExpiry.plusDays(1);
                        //Check if the sla is in minutes or hours
                        if (ticketType.getSla().getTicketSlaPeriod() == 'H') {
                            newSlaExpiry = newSlaExpiry.withHour(8 + (ticketType.getSla().getTicketSla() - hourDiff)).withMinute(minDiff).withSecond(0);
                        }
                        if (ticketType.getSla().getTicketSlaPeriod() == 'M') {
                            newSlaExpiry = newSlaExpiry.withHour(8).withMinute((ticketType.getSla().getTicketSla() - minDiff)).withSecond(0);
                        }
                        //Check if day is public holiday
                        if (!isPublicHoliday(newSlaExpiry.toLocalDate())) {
                            return newSlaExpiry;
                        } else {
                            //Get the next working day
                            return nextWorkingDay(newSlaExpiry);
                        }
                    }
                }
            }
        }

        //This is a weekend. Move the SLA to next working day
        if (slaExpiry.getDayOfWeek() == DayOfWeek.SATURDAY) {
            LocalDateTime newTime = slaExpiry.plusDays(2).withHour(8).withMinute(0).withSecond(0);
            LocalDateTime newSlaExpiry = LocalDateTime.now();
            switch (ticketType.getSla().getTicketSlaPeriod()) {
                case 'D' -> {
                    newSlaExpiry = newTime.plusDays(ticketType.getSla().getTicketSla());
                }
                case 'H' -> {
                    newSlaExpiry = newTime.plusHours(ticketType.getSla().getTicketSla());
                }
                default -> {
                    newSlaExpiry = newTime.plusMinutes(ticketType.getSla().getTicketSla());
                }
            }

            //Check if public holiday
            if (!isPublicHoliday(newSlaExpiry.toLocalDate())) {
                return newSlaExpiry;
            } else {
                //Get the next working day
                return nextWorkingDay(newSlaExpiry);
            }
        } else if (slaExpiry.getDayOfWeek() == DayOfWeek.SUNDAY) {
            LocalDateTime newTime = slaExpiry.plusDays(1).withHour(8).withMinute(0).withSecond(0);
            LocalDateTime newSlaExpiry = LocalDateTime.now();
            switch (ticketType.getSla().getTicketSlaPeriod()) {
                case 'D' -> {
                    newSlaExpiry = newTime.plusDays(ticketType.getSla().getTicketSla());
                }
                case 'H' -> {
                    newSlaExpiry = newTime.plusHours(ticketType.getSla().getTicketSla());
                }
                default -> {
                    newSlaExpiry = newTime.plusMinutes(ticketType.getSla().getTicketSla());
                }
            }
            if (!isPublicHoliday(newSlaExpiry.toLocalDate())) {
                return newSlaExpiry;
            } else {
                //Get the next working day
                return nextWorkingDay(newSlaExpiry);
            }
        }

        return slaExpiry;
    }

    private boolean isPublicHoliday(LocalDate date) {
        List<PublicHolidays> publicHolidays = xticketRepository.getPublicHolidays();
        boolean isPublicHols = false;
        if (publicHolidays != null) {
            for (PublicHolidays p : publicHolidays) {
                if (date.equals(p.getHoliday())) {
                    isPublicHols = true;
                }
            }
        }
        return isPublicHols;
    }

    private LocalDateTime nextWorkingDay(LocalDateTime date) {
        LocalDateTime nextWorkDay = null;
        //Loop 5 times to cover long holidays
        for (int i = 0; i <= 5; i++) {
            LocalDateTime newDate = date.plusDays(1);
            boolean isPublicHols = isPublicHoliday(newDate.toLocalDate());
            boolean isWeekend = (newDate.getDayOfWeek() == DayOfWeek.SATURDAY || newDate.getDayOfWeek() == DayOfWeek.SUNDAY);
            if (!isPublicHols && !isWeekend) {
                nextWorkDay = newDate.withHour(8).withMinute(0).withSecond(0);
                break;
            }
        }
        return nextWorkDay;
    }

    @Override
    public XTicketPayload fetchKnowledgeBaseCategory() {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<KnowledgeBaseCategory> knowledgeBaseCategory = xticketRepository.getKnowledgeBaseCategory();
            if (knowledgeBaseCategory != null) {
                for (KnowledgeBaseCategory t : knowledgeBaseCategory) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(dtf.format(t.getCreatedAt()));
                    payload.setCreatedBy(t.getCreatedBy());
                    payload.setId(t.getId().intValue());
                    payload.setCategoryCode(t.getCategoryCode());
                    payload.setCategoryName(t.getCategoryName());
                    data.add(payload);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchKnowledgeBaseCategory(String id) {
        var response = new XTicketPayload();
        try {
            KnowledgeBaseCategory knowledgeBaseCategory = xticketRepository.getKnowledgeBaseCategoryUsingId(Long.parseLong(id));
            if (knowledgeBaseCategory == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            BeanUtils.copyProperties(knowledgeBaseCategory, response);
            response.setCreatedAt(knowledgeBaseCategory.getCreatedAt().toLocalDate().toString());
            response.setId(knowledgeBaseCategory.getId().intValue());
            response.setCategoryCode(knowledgeBaseCategory.getCategoryCode());
            response.setCategoryName(knowledgeBaseCategory.getCategoryName());

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload createKnowledgeBaseCategory(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the request is for update or create new
            if (requestPayload.getId() == 0) {
                //Check if the user is valid
                var appUser = xticketRepository.getAppUserUsingEmail(principal);
                if (appUser == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check if the knowledge base category exist using code
                KnowledgeBaseCategory knowledgeBaseCategoryByCode = xticketRepository.getKnowledgeBaseCategoryUsingCode(requestPayload.getCategoryCode());
                if (knowledgeBaseCategoryByCode != null) {
                    response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Knowledge Base Category", "Code", requestPayload.getCategoryCode()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check the knowledge base category using the name
                KnowledgeBaseCategory knowledgeBaseCategoryByName = xticketRepository.getKnowledgeBaseCategoryUsingName(requestPayload.getCategoryName());
                if (knowledgeBaseCategoryByName != null) {
                    response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Knowledge Base Category", "Name", requestPayload.getCategoryName()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                KnowledgeBaseCategory newCategory = new KnowledgeBaseCategory();
                newCategory.setCreatedAt(LocalDateTime.now());
                newCategory.setCreatedBy(principal);
                newCategory.setCategoryCode(requestPayload.getCategoryCode());
                newCategory.setCategoryName(requestPayload.getCategoryName());
                xticketRepository.createKnowledgeBaseCategory(newCategory);

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Knowledge Base Category", "Created"}, Locale.ENGLISH));
                response.setData(null);

                //Log the response
                StringBuilder newValue = new StringBuilder();
                newValue.append("Knowledge Base Category Code:").append(requestPayload.getCategoryCode()).append(", ")
                        .append("Knowledge Base Category Name:").append(requestPayload.getCategoryName());
                genericService.logResponse(principal, newCategory.getId(), "Create", "Knowledge Base Category", "Create Knowledge Base Category " + requestPayload.getCategoryName(), "", newValue.toString());
                return response;
            }

            //This is an update request
            KnowledgeBaseCategory knowledgeBaseCategory = xticketRepository.getKnowledgeBaseCategoryUsingId(Long.valueOf(requestPayload.getId()));
            if (knowledgeBaseCategory == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Knowledge Base Category", "Id", requestPayload.getId()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the knowledge base category exist using code
            KnowledgeBaseCategory categoryByCode = xticketRepository.getKnowledgeBaseCategoryUsingCode(requestPayload.getCategoryCode());
            if (categoryByCode != null && !Objects.equals(knowledgeBaseCategory.getId(), categoryByCode.getId())) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Knowledge Base Category", "Code", requestPayload.getCategoryCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check the knowledge base category using the name
            KnowledgeBaseCategory categoryByName = xticketRepository.getKnowledgeBaseCategoryUsingName(requestPayload.getCategoryName());
            if (categoryByName != null && !Objects.equals(knowledgeBaseCategory.getId(), categoryByName.getId())) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Knowledge Base Category", "Name", requestPayload.getCategoryName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            StringBuilder oldValue = new StringBuilder();
            oldValue.append("Knowledge Base Category Code:").append(knowledgeBaseCategory.getCategoryCode()).append(", ")
                    .append("Knowledge Base Category Name:").append(knowledgeBaseCategory.getCategoryName());

            knowledgeBaseCategory.setCategoryCode(requestPayload.getCategoryCode());
            knowledgeBaseCategory.setCategoryName(requestPayload.getCategoryName());
            xticketRepository.updateKnowledgeBaseCategory(knowledgeBaseCategory);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Knowledge Base Category", "Updated"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            StringBuilder newValue = new StringBuilder();
            newValue.append("Knowledge Base Category Code:").append(requestPayload.getCategoryCode()).append(", ")
                    .append("Knowledge Base Category Name:").append(requestPayload.getCategoryName());
            genericService.logResponse(principal, knowledgeBaseCategory.getId(), "Update", "Knowledge Base Category", "Update Knowledge Base Category " + requestPayload.getCategoryName(), oldValue.toString(), newValue.toString());
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload deleteKnowledgeBaseCategory(String id, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the knowledge base by Id is valid
            KnowledgeBaseCategory knowledgeBaseCategory = xticketRepository.getKnowledgeBaseCategoryUsingId(Long.valueOf(id));
            if (knowledgeBaseCategory == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Knowledge Base Category", "Id", id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the ticket status is in use
            List<KnowledgeBase> knowledgeBaseByCategory = xticketRepository.getKnowledgeBaseUsingCategory(knowledgeBaseCategory);
            if (knowledgeBaseByCategory != null) {
                response.setResponseCode(ResponseCodes.IN_USE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.inuse", new Object[]{"Knowledge Base Category", knowledgeBaseCategory.getCategoryName()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            xticketRepository.deleteKnowledgeBaseCategory(knowledgeBaseCategory);
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Knowledge Base Category" + knowledgeBaseCategory.getCategoryName(), "Deleted"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            genericService.logResponse(principal, knowledgeBaseCategory.getId(), "Delete", "Knowledge Base Category", "Delete Knowledge Base Category " + knowledgeBaseCategory.getCategoryName(), knowledgeBaseCategory.getCategoryName(), "");
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchKnowledgeBaseContent() {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<KnowledgeBase> knowledgeBase = xticketRepository.getKnowledgeBase();
            if (knowledgeBase != null) {
                for (KnowledgeBase t : knowledgeBase) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(dtf.format(t.getCreatedAt()));
                    payload.setCreatedBy(t.getCreatedBy());
                    payload.setId(t.getId().intValue());
                    payload.setKnowledgeBaseContent(t.getBody());
                    payload.setKnowledgeBaseHeader(t.getHeader());
                    payload.setDocumentLink(t.getVideoLink());
                    payload.setLatestArticle(t.isLatestArticle());
                    payload.setPopularArticle(t.isPopularArticle());
                    data.add(payload);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchKnowledgeBaseContentUsingCategory(String id) {
        var response = new XTicketPayload();
        try {
            KnowledgeBaseCategory knowledgeBaseCategory = xticketRepository.getKnowledgeBaseCategoryUsingId(Long.parseLong(id));
            if (knowledgeBaseCategory == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            List<KnowledgeBase> knowledgeBaseList = xticketRepository.getKnowledgeBaseUsingCategory(knowledgeBaseCategory);
            if (knowledgeBaseList == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            List<XTicketPayload> data = new ArrayList<>();
            for (KnowledgeBase k : knowledgeBaseList) {
                XTicketPayload knowledgeBasePayload = new XTicketPayload();
                knowledgeBasePayload.setId(k.getId().intValue());
                knowledgeBasePayload.setTag(k.getTag());
                knowledgeBasePayload.setKnowledgeBaseHeader(k.getHeader());
                knowledgeBasePayload.setKnowledgeBaseContent(k.getBody().substring(0, k.getBody().length() >= 251 ? 250 : k.getBody().length()));
                knowledgeBasePayload.setDocumentLink(k.getVideoLink());
                data.add(knowledgeBasePayload);
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
            response.setCategoryName(knowledgeBaseCategory.getCategoryName());
            response.setValue(knowledgeBaseList.size());
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchKnowledgeBaseContent(String id) {
        var response = new XTicketPayload();
        try {
            KnowledgeBase knowledgeBase = xticketRepository.getKnowledgeBaseUsingId(Long.parseLong(id));
            if (knowledgeBase == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            BeanUtils.copyProperties(knowledgeBase, response);
            response.setCreatedAt(knowledgeBase.getCreatedAt().toLocalDate().toString());
            response.setId(knowledgeBase.getId().intValue());
            response.setKnowledgeBaseContent(knowledgeBase.getBody());
            response.setKnowledgeBaseHeader(knowledgeBase.getHeader());
            response.setDocumentLink(knowledgeBase.getVideoLink());
            response.setCategoryCode(knowledgeBase.getKnowledgeBaseCategory().getCategoryCode());
            response.setCategoryName(knowledgeBase.getKnowledgeBaseCategory().getCategoryName());
            response.setLatestArticle(knowledgeBase.isLatestArticle());
            response.setPopularArticle(knowledgeBase.isPopularArticle());

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload createKnowledgeBaseContent(XTicketPayload requestPayload, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the request is for update or create new
            if (requestPayload.getId() == 0) {
                //Check if the user is valid
                var appUser = xticketRepository.getAppUserUsingEmail(principal);
                if (appUser == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check if the ticket status exist using code
                KnowledgeBase knowledgeBaseByHeader = xticketRepository.getKnowledgeBaseUsingTitle(requestPayload.getKnowledgeBaseHeader());
                if (knowledgeBaseByHeader != null) {
                    response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Knowledge Base", "Header", requestPayload.getKnowledgeBaseHeader()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check if the knowledge base category exist using code
                KnowledgeBaseCategory knowledgeBaseCategory = xticketRepository.getKnowledgeBaseCategoryUsingCode(requestPayload.getCategoryCode());
                if (knowledgeBaseCategory == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Knowledge Base Category", "Category", requestPayload.getCategoryCode()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                String path = "";
                if (!requestPayload.getUploadedFiles().isEmpty()) {
                    for (MultipartFile f : requestPayload.getUploadedFiles()) {
                        String fileExt = FilenameUtils.getExtension(f.getOriginalFilename());
                        String newFileName = genericService.generateFileName();
                        //Copy the file to destination
                        path = servletContext.getRealPath("/") + "WEB-INF/classes/document" + "/" + newFileName + "." + fileExt;
                        File newFile = new File(path);
                        FileCopyUtils.copy(f.getBytes(), newFile);
                    }
                }

                KnowledgeBase newKnowledgeBase = new KnowledgeBase();
                newKnowledgeBase.setCreatedAt(LocalDateTime.now());
                newKnowledgeBase.setCreatedBy(principal);
                newKnowledgeBase.setBody(requestPayload.getKnowledgeBaseContent());
                newKnowledgeBase.setHeader(requestPayload.getKnowledgeBaseHeader());
                newKnowledgeBase.setKnowledgeBaseCategory(knowledgeBaseCategory);
                newKnowledgeBase.setTag(requestPayload.getTag());
                newKnowledgeBase.setVideoLink(path);
                newKnowledgeBase.setLatestArticle(requestPayload.isLatestArticle());
                newKnowledgeBase.setPopularArticle(requestPayload.isPopularArticle());
                xticketRepository.createKnowledgeBase(newKnowledgeBase);

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Knowledge Base", "Created"}, Locale.ENGLISH));
                response.setData(null);

                //Log the response
                StringBuilder newValue = new StringBuilder();
                newValue.append("Knowledge Base Content:").append(requestPayload.getKnowledgeBaseHeader()).append(", ")
                        .append("Knowledge Base Category:").append(knowledgeBaseCategory.getCategoryName());
                genericService.logResponse(principal, newKnowledgeBase.getId(), "Create", "Knowledge Base Content", "Create Knowledge Base Content " + requestPayload.getKnowledgeBaseHeader(), "", newValue.toString());
                return response;
            }

            //This is an update request
            KnowledgeBase knowledgeBase = xticketRepository.getKnowledgeBaseUsingId(Long.valueOf(requestPayload.getId()));
            if (knowledgeBase == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Knowledge Base", "Id", requestPayload.getId()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the knowledge base exist using header
            KnowledgeBase knowledgeBaseByHeader = xticketRepository.getKnowledgeBaseUsingTitle(requestPayload.getKnowledgeBaseHeader());
            if (knowledgeBaseByHeader != null && !Objects.equals(knowledgeBase.getId(), knowledgeBaseByHeader.getId())) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Knowledge Base", "Header", requestPayload.getKnowledgeBaseHeader()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check if the knowledge base category exist using code
            KnowledgeBaseCategory knowledgeBaseCategory = xticketRepository.getKnowledgeBaseCategoryUsingCode(requestPayload.getCategoryCode());
            if (knowledgeBaseCategory == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Knowledge Base Category", "Category", requestPayload.getCategoryCode()}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            StringBuilder oldValue = new StringBuilder();
            oldValue.append("Knowledge Base Content:").append(knowledgeBase.getHeader()).append(", ")
                    .append("Knowledge Base Category:").append(knowledgeBase.getKnowledgeBaseCategory().getCategoryName());

            knowledgeBase.setBody(requestPayload.getKnowledgeBaseContent());
            knowledgeBase.setHeader(requestPayload.getKnowledgeBaseHeader());
            knowledgeBase.setKnowledgeBaseCategory(knowledgeBaseCategory);
            knowledgeBase.setTag(requestPayload.getTag());
            knowledgeBase.setVideoLink(requestPayload.getDocumentLink());
            knowledgeBase.setLatestArticle(requestPayload.isLatestArticle());
            knowledgeBase.setPopularArticle(requestPayload.isPopularArticle());
            xticketRepository.updateKnowledgeBase(knowledgeBase);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket Status", "Updated"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            StringBuilder newValue = new StringBuilder();
            newValue.append("Knowledge Base Content:").append(requestPayload.getKnowledgeBaseHeader()).append(", ")
                    .append("Knowledge Base Category:").append(knowledgeBaseCategory.getCategoryName());
            genericService.logResponse(principal, knowledgeBase.getId(), "Update", "Knowledge Base Content", "Update Knowledge Base Content " + requestPayload.getKnowledgeBaseHeader(), oldValue.toString(), newValue.toString());
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload deleteKnowledgeBaseContent(String id, String principal) {
        var response = new XTicketPayload();
        try {
            //Check if the knowledge base by Id is valid
            KnowledgeBase knowledgeBase = xticketRepository.getKnowledgeBaseUsingId(Long.valueOf(id));
            if (knowledgeBase == null) {
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.notexist", new Object[]{"Knowledge Base", "Id", id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            xticketRepository.deleteKnowledgeBase(knowledgeBase);
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Knowledge Base" + knowledgeBase.getHeader(), "Deleted"}, Locale.ENGLISH));
            response.setData(null);

            //Log the response
            genericService.logResponse(principal, knowledgeBase.getId(), "Delete", "Knowledge Base Content", "Delete Knowledge Base Content " + knowledgeBase.getHeader(), knowledgeBase.getHeader(), "");
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchKnowledgeBase() {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<KnowledgeBaseCategory> knowledgeBaseCategory = xticketRepository.getKnowledgeBaseCategory();
            if (knowledgeBaseCategory != null) {
                for (KnowledgeBaseCategory t : knowledgeBaseCategory) {
                    XTicketPayload payload = new XTicketPayload();
                    payload.setCategoryName(t.getCategoryName());
                    payload.setId(t.getId().intValue());

                    //Get the contents in this category
                    List<KnowledgeBase> knowledgeBaseList = xticketRepository.getKnowledgeBaseUsingCategory(t);
                    if (knowledgeBaseList != null) {
                        List<XTicketPayload> knowledgeBase = new ArrayList<>();
                        for (KnowledgeBase k : knowledgeBaseList) {
                            XTicketPayload knowledgeBasePayload = new XTicketPayload();
                            knowledgeBasePayload.setId(t.getId().intValue());
                            knowledgeBasePayload.setTag(k.getTag());
                            knowledgeBasePayload.setKnowledgeBaseHeader(k.getHeader());
                            knowledgeBasePayload.setKnowledgeBaseContent(k.getBody());
                            knowledgeBasePayload.setDocumentLink(k.getVideoLink());
                            knowledgeBase.add(knowledgeBasePayload);
                        }
                        payload.setKnowledgeBaseContentList(knowledgeBase);
                        payload.setValue(knowledgeBaseList.size());
                    }
                    data.add(payload);
                }

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
                response.setData(data);
                return response;
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchKnowledgeBasePopularArticle() {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<KnowledgeBase> knowledgeBase = xticketRepository.getKnowledgeBasePopularArticle();
            if (knowledgeBase != null) {
                for (KnowledgeBase t : knowledgeBase) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(dtf.format(t.getCreatedAt()));
                    payload.setCreatedBy(t.getCreatedBy());
                    payload.setId(t.getId().intValue());
                    payload.setKnowledgeBaseContent(t.getBody());
                    payload.setKnowledgeBaseHeader(t.getHeader());
                    payload.setDocumentLink(t.getVideoLink());
                    data.add(payload);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchKnowledgeBaseLatestArticle() {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<KnowledgeBase> knowledgeBase = xticketRepository.getKnowledgeBaseLatestArticle();
            if (knowledgeBase != null) {
                for (KnowledgeBase t : knowledgeBase) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(dtf.format(t.getCreatedAt()));
                    payload.setCreatedBy(t.getCreatedBy());
                    payload.setId(t.getId().intValue());
                    payload.setKnowledgeBaseContent(t.getBody());
                    payload.setKnowledgeBaseHeader(t.getHeader());
                    payload.setDocumentLink(t.getVideoLink());
                    data.add(payload);
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchKnowledgeBasePopularTag() {
        var response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<KnowledgeBase> knowledgeBase = xticketRepository.getKnowledgeBase();
            if (knowledgeBase != null) {
                for (KnowledgeBase t : knowledgeBase) {
                    String[] tags = t.getTag().split(",");
                    if (tags.length > 0) {
                        for (String k : tags) {
                            XTicketPayload payload = new XTicketPayload();
                            payload.setTag(k);
                            data.add(payload);
                        }
                    }
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload fetchAuditLog(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            List<AuditLog> auditLog = xticketRepository.getAuditLog(LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()));
            if (auditLog != null) {
                //Check if audit class is set in the filter
                if (!requestPayload.getAuditClass().equalsIgnoreCase("")) {
                    auditLog = auditLog.stream().filter(t -> t.getAuditClass().equalsIgnoreCase(requestPayload.getAuditClass())).collect(Collectors.toList());
                }

                //Check if audit category is set in the filter
                if (!requestPayload.getAuditCategory().equalsIgnoreCase("")) {
                    auditLog = auditLog.stream().filter(t -> t.getAuditCategory().equalsIgnoreCase(requestPayload.getAuditCategory())).collect(Collectors.toList());

                }

                //Check if ref no is set in the filter
                if (!requestPayload.getRefNo().equalsIgnoreCase("")) {
                    auditLog = auditLog.stream().filter(t -> t.getRefNo().equalsIgnoreCase(requestPayload.getRefNo())).collect(Collectors.toList());

                }

                //Check if username is set in the filter
                if (!requestPayload.getUsername().equalsIgnoreCase("")) {
                    auditLog = auditLog.stream().filter(t -> t.getUsername().equalsIgnoreCase(requestPayload.getUsername())).collect(Collectors.toList());

                }

                //Loop through the list and transform
                List<XTicketPayload> data = new ArrayList<>();
                for (AuditLog t : auditLog) {
                    XTicketPayload newLog = new XTicketPayload();
                    BeanUtils.copyProperties(t, newLog);
                    newLog.setCreatedAt(dtf.format(t.getCreatedAt()));
                    data.add(newLog);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{data.size()}, Locale.ENGLISH));
                response.setData(data);
                return response;
            }

            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{" selected"}, Locale.ENGLISH));
            response.setData(null);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload createContactUs(XTicketPayload requestPayload) {
        var response = new XTicketPayload();
        try {
            ContactUs newContact = new ContactUs();
            newContact.setCreatedAt(LocalDateTime.now());
            newContact.setEmail(requestPayload.getEmail());
            newContact.setFullName(requestPayload.getName());
            newContact.setMessage(requestPayload.getMessage());
            newContact.setMobileNumber(requestPayload.getMobileNumber());
            xticketRepository.createContactUs(newContact);

            //Send email to the contact us mail group
            XTicketPayload mailPayload = new XTicketPayload();
            mailPayload.setRecipientEmail(contactUsEmail);
            mailPayload.setEmailSubject("Contact Us");
            mailPayload.setCarbonCopyEmail("");
            String message = "<h4>To Whom It May Concern,</h4>\n"
                    + "<p>" + requestPayload.getName() + " sent a message \"" + requestPayload.getMessage() + "\"</p>"
                    + "<p>For feedback, kindly use the email  " + requestPayload.getEmail() + "  or call the mobile " + requestPayload.getMobileNumber() + "</p>"
                    + "<p>Best wishes,</p>"
                    + "<p>" + companyName + "</p>";

            mailPayload.setEmailBody(message);
            genericService.sendEmail(mailPayload, contactUsEmail);
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.contact", new Object[0], Locale.ENGLISH));
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

    @Override
    public XTicketPayload searchKnowledgeBaseContent(String searchKeyWord) {
        var response = new XTicketPayload();
        try {

            List<KnowledgeBase> knowledgeBaseList = xticketRepository.getKnowledgeBaseUsingTags(searchKeyWord);
            if (knowledgeBaseList == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{searchKeyWord}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            List<XTicketPayload> data = new ArrayList<>();
            for (KnowledgeBase k : knowledgeBaseList) {
                XTicketPayload knowledgeBasePayload = new XTicketPayload();
                knowledgeBasePayload.setId(k.getId().intValue());
                knowledgeBasePayload.setTag(k.getTag());
                knowledgeBasePayload.setKnowledgeBaseHeader(k.getHeader());
                knowledgeBasePayload.setKnowledgeBaseContent(k.getBody().substring(0, k.getBody().length() >= 251 ? 250 : k.getBody().length()));
                knowledgeBasePayload.setDocumentLink(k.getVideoLink());
                knowledgeBasePayload.setCategoryName(k.getKnowledgeBaseCategory().getCategoryName());
                data.add(knowledgeBasePayload);
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
            response.setValue(knowledgeBaseList.size());
            response.setData(data);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            response.setData(null);
            return response;
        }
    }

}
