package com.ngxgroup.xticket.service;

import com.ngxgroup.xticket.payload.XTicketPayload;
import com.google.gson.Gson;
import com.ngxgroup.xticket.constant.ResponseCodes;
import com.ngxgroup.xticket.model.AppRoles;
import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.model.GroupRoles;
import com.ngxgroup.xticket.model.RoleGroups;
import com.ngxgroup.xticket.model.TicketAgent;
import com.ngxgroup.xticket.model.TicketComment;
import com.ngxgroup.xticket.model.TicketEscalations;
import com.ngxgroup.xticket.model.TicketGroup;
import com.ngxgroup.xticket.model.TicketReassign;
import com.ngxgroup.xticket.model.TicketReopened;
import com.ngxgroup.xticket.model.TicketSla;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
public class XTicketServiceImpl implements  XTicketService {

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
    @Value("${xticket.email.notification}")
    private String emailNotification;
    @Value("${xticket.default.email.domain}")
    private String companyEmailDomain;
    @Value("${xticket.adauth.domains}")
    private String adAuthDomain;
    @Value("${xticket.company.name}")
    private String companyName;
    @Value("${xticket.company.address}")
    private String companyAddress;
    @Value("${xticket.company.email}")
    private String companyEmail;
    @Value("${xticket.company.phone}")
    private String companyPhone;
    @Value("${xticket.company.rc}")
    private String companyRC;
    @Value("${xticket.company.logo}")
    private String companyLogo;
    @Value("${xticket.host}")
    private String host;
    static final Logger logger = Logger.getLogger(XTicketServiceImpl.class.getName());
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    DateTimeFormatter shortDtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    @Value("${xticket.password.pattern}")
    private String passwordPattern;

    @Override
    public XTicketPayload signin(XTicketPayload requestPayload) {
        XTicketPayload response = new XTicketPayload();
        LogPayload log = new LogPayload();
        try {
            AppUser appUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
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
                String message = messageSource.getMessage("appMessages.reset.on", new Object[]{LocalTime.now()}, Locale.ENGLISH);
                response.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                response.setResponseMessage(message);
                return response;
            }

            //Check if the user needs to change password
            if (LocalDate.now().isAfter(appUser.getPasswordChangeDate())) {
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
                if (adResponse.equals("success")) {
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
                    return response;
                }
            } else {
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
                    return response;
                }
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
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            return response;
        }
    }

    public String authenticateADUser(String username, String password, String domainName, String serverName) throws NamingException {
        LogPayload log = new LogPayload();
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
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if the username (Email) already exist
            AppUser appUserByEmail = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
            if (appUserByEmail != null) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.exist", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH));
                return response;
            }

            //Check if the user is registered with the mobile number
            AppUser appUserByMobile = xticketRepository.getAppUserUsingMobileNumber(requestPayload.getMobileNumber());
            if (appUserByMobile != null) {
                response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.exist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                return response;
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

            //Create new Record
            AppUser newUser = new AppUser();
            String activationId = UUID.randomUUID().toString().replaceAll("-", "");
            newUser.setActivationId(activationId);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setCreatedBy(requestPayload.getEmail());
            newUser.setEmail(requestPayload.getEmail());
            newUser.setActivated(false);
            newUser.setGender(requestPayload.getGender());
            newUser.setInternal(requestPayload.getEmail().contains(companyEmailDomain));
            newUser.setLastLogin(null);
            newUser.setLastName(requestPayload.getLastName());
            newUser.setLocked(true);
            newUser.setLoginFailCount(0);
            newUser.setMobileNumber(requestPayload.getMobileNumber());
            newUser.setOtherName(requestPayload.getOtherName());
            newUser.setPassword(passwordEncoder.encode(requestPayload.getPassword()));
            newUser.setPasswordChangeDate(LocalDate.now().plusDays(passwordChangeDays));
            newUser.setResetTime(LocalDateTime.now().minusMinutes(1));
            newUser.setRole(roleGroup == null ? null : roleGroup);
            newUser.setAgent(false);
            newUser.setUpdatedAt(LocalDateTime.now());
            newUser.setUpdatedBy(requestPayload.getEmail());
            xticketRepository.createAppUser(newUser);

            //Send profile activation email
            XTicketPayload emailPayload = new XTicketPayload();
            emailPayload.setRecipientEmail(requestPayload.getEmail());
            emailPayload.setEmailSubject("Profile Activation");
            String message = "<h4>Dear " + requestPayload.getLastName() + "</h4>\n"
                    + "<p>We appreciate your interest in " + companyName + " X-TICKET, our award-winning ticketing platform and help desk.</p>\n"
                    + "<p>To activate your profile, please click on the activation button below</p>\n"
                    + "<p><a href=\"" + host + "/xticket/signup/activate?id=" + activationId + "\"><button type='button' style='display:inline-block;background-color:#ee4f1e;padding:5px; width:200px; color:#ffffff; text-align:center; border-radius:20px; border-color:white;'>Activate Profile</button></a></p>"
                    + "<p>For support and enquiries, email: " + companyEmail + ".</p>\n"
                    + "<p>Once again, thanks for choosing " + companyName + "!.</p>\n"
                    + "<p>Best wishes,</p>"
                    + "<p>" + companyName + "</p>";

            emailPayload.setEmailBody(message);
            genericService.sendEmail(emailPayload, requestPayload.getEmail());
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.user", new Object[0], Locale.ENGLISH));
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            return response;
        }
    }

    @Override
    public XTicketPayload signUpActivation(String activationId) {
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if a user exist using the id
            AppUser appUser = xticketRepository.getAppUserUsingActivationId(activationId);
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
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            return response;
        }
    }

    @Override
    public XTicketPayload fetchProfile(String principal) {
        XTicketPayload response = new XTicketPayload();
        try {
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
            BeanUtils.copyProperties(appUser, response);
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            return response;
        }
    }

    @Override
    public XTicketPayload changePassword(XTicketPayload requestPayload) {
        XTicketPayload response = new XTicketPayload();
        LogPayload log = new LogPayload();
        try {
            //Check if user exist
            AppUser appUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
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

            appUser.setPassword(passwordEncoder.encode(requestPayload.getNewPassword()));
            appUser.setPasswordChangeDate(LocalDate.now().plusDays(passwordChangeDays));
            xticketRepository.updateAppUser(appUser);
            BeanUtils.copyProperties(appUser, response);
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.user", new Object[0], Locale.ENGLISH));
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            return response;
        }
    }

    @Override
    public XTicketPayload forgotPassword(XTicketPayload requestPayload) {
        XTicketPayload response = new XTicketPayload();
        LogPayload log = new LogPayload();
        try {
            //Check if user exist
            AppUser appUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
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

            //Send profile activation email
            XTicketPayload emailPayload = new XTicketPayload();
            emailPayload.setRecipientEmail(requestPayload.getEmail());
            emailPayload.setEmailSubject("Password Change");
            String message = "<h4>Dear " + requestPayload.getLastName() + "</h4>\n"
                    + "<p>We appreciate your interest in " + companyName + " X-TICKET, our award-winning ticketing platform and help desk.</p>\n"
                    + "<p>To activate your profile, please click on the activation button below</p>\n"
                    + "<p><a href=\"" + host + "/xticket/signup/activate?id=" + "ID" + "\"><button type='button' style='display:inline-block;background-color:#ee4f1e;padding:5px; width:200px; color:#ffffff; text-align:center; border-radius:20px; border-color:white;'>Activate Profile</button></a></p>"
                    + "<p>For support and enquiries, email: " + companyEmail + ".</p>\n"
                    + "<p>Once again, thanks for choosing " + companyName + "!.</p>\n"
                    + "<p>Best wishes,</p>"
                    + "<p>" + companyName + "</p>";

            emailPayload.setEmailBody(message);
            genericService.sendEmail(requestPayload, requestPayload.getEmail());
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.forgotpassword", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH));
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
        AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
        if (appUser != null) {
            appUser.setOnline(userOnline);
            xticketRepository.updateAppUser(appUser);
        }
    }

    @Override
    public XTicketPayload updateAppUser(XTicketPayload requestPayload, String principal) {
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if the user is valid
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
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

            switch (requestPayload.getAction()) {
                case "Activate": {
                    selectedUser.setActivated(true);
                    xticketRepository.updateAppUser(selectedUser);
                    break;
                }
                case "AgentAdd": {
                    selectedUser.setAgent(true);
                    xticketRepository.updateAppUser(selectedUser);
                    break;
                }
                case "Role": {
                    selectedUser.setRole(roleGroup);
                    xticketRepository.updateAppUser(selectedUser);
                    break;
                }
                case "Deactivate": {
                    selectedUser.setActivated(false);
                    xticketRepository.updateAppUser(selectedUser);
                    break;
                }
                case "Lock": {
                    selectedUser.setLocked(true);
                    xticketRepository.updateAppUser(selectedUser);
                    break;
                }
                case "Unlock": {
                    selectedUser.setLocked(false);
                    xticketRepository.updateAppUser(selectedUser);
                    break;
                }
                case "AgentRemove": {
                    selectedUser.setAgent(false);
                    xticketRepository.updateAppUser(selectedUser);
                    break;
                }
                case "FailedLogin": {
                    selectedUser.setLoginFailCount(0);
                    xticketRepository.updateAppUser(selectedUser);
                    break;
                }
                case "LogoutTime": {
                    selectedUser.setResetTime(LocalDateTime.now().minusMinutes(1));
                    xticketRepository.updateAppUser(selectedUser);
                    break;
                }
                case "Email": {
                    selectedUser.setEmail(requestPayload.getNewValue());
                    xticketRepository.updateAppUser(selectedUser);
                    break;
                }
                case "Mobile": {
                    selectedUser.setMobileNumber(requestPayload.getNewValue());
                    xticketRepository.updateAppUser(selectedUser);
                    break;
                }
                case "PasswordChange": {
                    selectedUser.setPasswordChangeDate(LocalDate.now().plusDays(passwordChangeDays));
                    xticketRepository.updateAppUser(selectedUser);
                    break;
                }
            }

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.user", new Object[0], Locale.ENGLISH));
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
     * Roles *
     */
    @Override
    public List<RoleGroups> fetchRoleGroup() {
        return xticketRepository.getRoleGroupList();
    }

    @Override
    public XTicketPayload fetchRoleGroup(String id) {
        XTicketPayload response = new XTicketPayload();
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
        XTicketPayload response = new XTicketPayload();
        try {
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
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

            oldRoleGroup.setGroupName(requestPayload.getGroupName());
            xticketRepository.updateRoleGroup(oldRoleGroup);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.roles", new Object[]{1}, Locale.ENGLISH));
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
    public XTicketPayload deleteRoleGroup(String id, String principal) {
        XTicketPayload response = new XTicketPayload();
        try {
            //Check the username of the requester
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
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
        XTicketPayload response = new XTicketPayload();
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
        XTicketPayload response = new XTicketPayload();
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
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
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
        XTicketPayload response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<TicketGroup> ticketGroup = xticketRepository.getTicketGroup();
            if (ticketGroup != null) {
                for (TicketGroup t : ticketGroup) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(t.getCreatedAt().toLocalDate().toString());
                    payload.setCreatedBy(t.getCreatedBy().getLastName() + " " + t.getCreatedBy().getOtherName());
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
        XTicketPayload response = new XTicketPayload();
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
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if the request is for update or create new
            if (requestPayload.getId() == 0) {
                //Check if the user is valid
                AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
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
                newTicketGroup.setCreatedBy(appUser);
                newTicketGroup.setStatus(requestPayload.getStatus());
                newTicketGroup.setTicketGroupCode(requestPayload.getTicketGroupCode());
                newTicketGroup.setTicketGroupName(requestPayload.getTicketGroupName());
                xticketRepository.createTicketGroup(newTicketGroup);

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket Group", "Created"}, Locale.ENGLISH));
                response.setData(null);
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

            ticketGroup.setStatus(requestPayload.getStatus());
            ticketGroup.setTicketGroupCode(requestPayload.getTicketGroupCode());
            ticketGroup.setTicketGroupName(requestPayload.getTicketGroupName());
            xticketRepository.updateTicketGroup(ticketGroup);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket Group", "Updated"}, Locale.ENGLISH));
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
    public XTicketPayload deleteTicketGroup(String id, String principal) {
        XTicketPayload response = new XTicketPayload();
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
            response.setResponseMessage(messageSource.getMessage("appMessages.success.notexist", new Object[]{"Ticket Group" + ticketGroup.getTicketGroupName(), "Deleted"}, Locale.ENGLISH));
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
     * Ticket Type *
     */
    @Override
    public XTicketPayload fetchTicketType() {
        XTicketPayload response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<TicketType> ticketType = xticketRepository.getTicketType();
            if (ticketType != null) {
                for (TicketType t : ticketType) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(t.getCreatedAt().toLocalDate().toString());
                    payload.setCreatedBy(t.getCreatedBy().getLastName() + " " + t.getCreatedBy().getOtherName());
                    payload.setId(t.getId().intValue());
                    payload.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    response.setTicketGroupCode(t.getTicketGroup().getTicketGroupCode());
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
        XTicketPayload response = new XTicketPayload();
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
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if the user is valid
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
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

                TicketType newTicketType = new TicketType();
                newTicketType.setCreatedAt(LocalDateTime.now());
                newTicketType.setCreatedBy(appUser);
                newTicketType.setTicketTypeCode(requestPayload.getTicketTypeCode());
                newTicketType.setTicketTypeName(requestPayload.getTicketTypeName());
                newTicketType.setEscalationEmails(requestPayload.getEscalationEmails());
                newTicketType.setInternal(requestPayload.isInternal());
                newTicketType.setEmailEscalationIndex(0);
                newTicketType.setSla(ticketSla);
                newTicketType.setStatus(requestPayload.getStatus());
                newTicketType.setTicketGroup(ticketGroup);
                newTicketType.setRequireChangeRequestForm(requestPayload.isRequireChangeRequestForm());
                newTicketType.setRequireServiceRequestForm(requestPayload.isRequireServiceRequestForm());
                xticketRepository.createTicketType(newTicketType);

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket Type", "Created"}, Locale.ENGLISH));
                response.setData(null);
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

            ticketType.setCreatedAt(LocalDateTime.now());
            ticketType.setCreatedBy(appUser);
            ticketType.setStatus(requestPayload.getStatus());
            ticketType.setTicketTypeCode(requestPayload.getTicketTypeCode());
            ticketType.setTicketTypeName(requestPayload.getTicketTypeName());
            ticketType.setEscalationEmails(requestPayload.getEscalationEmails());
            ticketType.setInternal(requestPayload.isInternal());
            ticketType.setEmailEscalationIndex(0);
            ticketType.setSla(ticketSla);
            ticketType.setStatus(requestPayload.getStatus());
            ticketType.setTicketGroup(ticketGroup);
            ticketType.setRequireChangeRequestForm(requestPayload.isRequireChangeRequestForm());
            ticketType.setRequireServiceRequestForm(requestPayload.isRequireServiceRequestForm());
            xticketRepository.updateTicketType(ticketType);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket Type", "Updated"}, Locale.ENGLISH));
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
    public XTicketPayload deleteTicketType(String id, String principal) {
        XTicketPayload response = new XTicketPayload();
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
            response.setResponseMessage(messageSource.getMessage("appMessages.success.notexist", new Object[]{"Ticket Type" + ticketType.getTicketTypeName(), "Deleted"}, Locale.ENGLISH));
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
    public List<TicketType> fetchTicketTypeUsingGroup(String ticketGroupCode, String principal) {
        boolean userType = false;
        AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
        if (appUser != null) {
            userType = appUser.isInternal();
        }

        TicketGroup ticketGroup = xticketRepository.getTicketGroupUsingCode(ticketGroupCode);
        if (ticketGroup == null) {
            return new ArrayList<>();
        } else {
            List<TicketType> ticketTypes = xticketRepository.getTicketTypeUsingTicketGroup(ticketGroup, userType);
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
        XTicketPayload response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<TicketSla> ticketSla = xticketRepository.getTicketSla();
            if (ticketSla != null) {
                for (TicketSla t : ticketSla) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(t.getCreatedAt().toLocalDate().toString());
                    payload.setCreatedBy(t.getCreatedBy().getLastName() + " " + t.getCreatedBy().getOtherName());
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
        XTicketPayload response = new XTicketPayload();
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
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if the request is for update or create new
            if (requestPayload.getId() == 0) {
                //Check if the user is valid
                AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
                if (appUser == null) {
                    response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                //Check the ticket group using the name
                TicketSla slaByName = xticketRepository.getTicketSlaUsingName(requestPayload.getTicketSlaName());
                if (slaByName != null) {
                    response.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.exist", new Object[]{"Ticket SLA", "Name", requestPayload.getTicketSlaName()}, Locale.ENGLISH));
                    response.setData(null);
                    return response;
                }

                TicketSla newTicketSla = new TicketSla();
                newTicketSla.setCreatedAt(LocalDateTime.now());
                newTicketSla.setCreatedBy(appUser);
                newTicketSla.setTicketSla(requestPayload.getTicketSla());
                newTicketSla.setTicketSlaPeriod(requestPayload.getTicketSlaPeriod());
                newTicketSla.setTicketSlaName(requestPayload.getTicketSlaName());
                xticketRepository.createTicketSla(newTicketSla);

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket SLA", "Created"}, Locale.ENGLISH));
                response.setData(null);
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

            ticketSla.setTicketSla(requestPayload.getTicketSla());
            ticketSla.setTicketSlaPeriod(requestPayload.getTicketSlaPeriod());
            ticketSla.setTicketSlaName(requestPayload.getTicketSlaName());
            xticketRepository.updateTicketSla(ticketSla);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{"Ticket SLA", "Updated"}, Locale.ENGLISH));
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
    public XTicketPayload deleteTicketSla(String id, String principal) {
        XTicketPayload response = new XTicketPayload();
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
            response.setResponseMessage(messageSource.getMessage("appMessages.success.notexist", new Object[]{"Ticket SLA" + ticketSla.getTicketSlaName(), "Deleted"}, Locale.ENGLISH));
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
     * Ticket Agent *
     */
    @Override
    public XTicketPayload fetchTicketAgent() {
        XTicketPayload response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<AppUser> ticketAgent = xticketRepository.getAgentAppUsers();
            if (ticketAgent != null) {
                for (AppUser t : ticketAgent) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(t.getCreatedAt().toLocalDate().toString());
                    payload.setCreatedBy(t.getLastName() + " " + t.getOtherName());
                    payload.setLastLogin(t.getLastLogin().format(dtf));
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
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
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

            String[] roles = requestPayload.getRolesToUpdate().split(",");
            if (roles.length > 0) {
                List<TicketAgent> currentRoles = xticketRepository.getTicketAgent(userAgent);
                if (currentRoles != null) {
                    for (TicketAgent rol : currentRoles) {
                        xticketRepository.deleteTicketAgent(rol);
                    }
                }
                for (String rol : roles) {
                    TicketAgent newTicket = new TicketAgent();
                    newTicket.setCreatedAt(LocalDateTime.now());
                    newTicket.setAgent(userAgent);
                    newTicket.setCreatedBy(appUser);
                    TicketType ticketType = xticketRepository.getTicketTypeUsingName(rol);
                    newTicket.setTicketType(ticketType);
                    xticketRepository.createTicketAgent(newTicket);
                }
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
    public XTicketPayload fetchAgentTicketTypes(String principal) {
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if the user is valid
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
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
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Check the ticket group
            TicketGroup ticketGroup = xticketRepository.getTicketGroupUsingCode(requestPayload.getTicketGroupCode());
            if (ticketGroup == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{requestPayload.getTicketGroupCode()}, Locale.ENGLISH));
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

            Tickets newTicket = new Tickets();
            newTicket.setAttachedFile(!requestPayload.getUploadedFiles().isEmpty());
            newTicket.setClosedAt(null);
            newTicket.setClosedBy(null);
            newTicket.setCreatedAt(LocalDateTime.now());
            newTicket.setCreatedBy(appUser);
            newTicket.setEscalated(false);
            newTicket.setFileIndex(1);
            newTicket.setInternal(ticketType.isInternal());
            newTicket.setMessage(requestPayload.getMessage());
            newTicket.setSubject(requestPayload.getSubject());
            newTicket.setTicketAgent(ticketAgent);
            newTicket.setTicketGroup(ticketGroup);
            newTicket.setTicketId(ticketId);
            newTicket.setTicketOpen(true);
            newTicket.setTicketReopen(false);
            newTicket.setTicketReopened(null);
            newTicket.setTicketReassign(false);
            newTicket.setTicketReassigned(null);
            newTicket.setTicketType(ticketType);
            newTicket.setSla(String.valueOf(ticketType.getSla().getTicketSla()) + String.valueOf(ticketType.getSla().getTicketSlaPeriod()));
            LocalDateTime slaExpiryTime = ticketType.getSla().getTicketSlaPeriod() == 'D'
                    ? LocalDateTime.now().plusDays(ticketType.getSla().getTicketSla())
                    : ticketType.getSla().getTicketSlaPeriod() == 'H'
                    ? LocalDateTime.now().plusHours(ticketType.getSla().getTicketSla())
                    : LocalDateTime.now().plusMinutes(ticketType.getSla().getTicketSla());
            newTicket.setSlaExpiry(slaExpiryTime);
            newTicket.setTicketLocked(false);
            newTicket.setTicketSource("Web");
            newTicket.setPriority(ticketType.getSla().getTicketSlaName());
            Tickets createTicket = xticketRepository.createTicket(newTicket);

            int fileIndex = 1;
            if (!requestPayload.getUploadedFiles().isEmpty()) {
                String newFileName = ticketId.replace("-", "");
                for (MultipartFile f : requestPayload.getUploadedFiles()) {
                    String fileExt = FilenameUtils.getExtension(f.getOriginalFilename());
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
            mailPayload.setSubject("Ticket Request Notification");
            String slaExpiry = dtf.format(slaExpiryTime);
            String message = "<h4>Dear " + ticketAgent.getAgent().getLastName() + "</h4>\n"
                    + "<p>A ticket has been opened as follows;</p>\n"
                    + "<p>Date Created: " + LocalDate.now().toString() + "</p>\n"
                    + "<p>Initiated By: " + appUser.getLastName() + ", " + appUser.getOtherName() + "</p>\n"
                    + "<p>Ticket ID: " + ticketId + "</p>\n"
                    + "<p>Ticket Group: " + ticketType.getTicketGroup().getTicketGroupName() + "</p>\n"
                    + "<p>Ticket Type: " + ticketType.getTicketTypeName() + "</p>\n"
                    + "<p>Ticket Priority: " + ticketType.getSla().getTicketSlaName() + "</p>\n"
                    + "<p>SLA Expiry: " + slaExpiry + "</p>\n"
                    + "<p>To view the ticket details or take action <a href=\"" + host + "/xticket" + "\">click here</a></p>"
                    + "<p>For support and enquiries, email: " + companyEmail + ".</p>\n"
                    + "<p>Best wishes,</p>"
                    + "<p>" + companyName + "</p>";
            mailPayload.setEmailBody(message);
            genericService.sendEmail(mailPayload, principal);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{ticketType.getTicketTypeName() + " Ticket", "Created"}, Locale.ENGLISH));
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
    public XTicketPayload replyTicket(XTicketPayload requestPayload, String principal) {
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
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
            mailPayload.setSubject("Ticket Reply Notification");
            String message = "<h4>Dear " + ticket.getTicketAgent().getAgent().getLastName() + "</h4>\n"
                    + "<p>A reply to a ticket initiated as follows;</p>\n"
                    + "<p>Reply Date: " + LocalDate.now().toString() + "</p>\n"
                    + "<p>Reply By: " + appUser.getLastName() + ", " + appUser.getOtherName() + "</p>\n"
                    + "<p>Ticket ID: " + ticket.getTicketId() + "</p>\n"
                    + "<p>Ticket Group: " + ticket.getTicketType().getTicketGroup().getTicketGroupName() + "</p>\n"
                    + "<p>Ticket Type: " + ticket.getTicketType().getTicketTypeName() + "</p>\n"
                    + "<p>Ticket Priority: " + ticket.getPriority() + "</p>\n"
                    + "<p>SLA Expiry: " + dtf.format(ticket.getSlaExpiry()) + "</p>\n"
                    + "<p>To view the ticket details or take action <a href=\"" + host + "/xticket" + "\">click here</a></p>"
                    + "<p>For support and enquiries, email: " + companyEmail + ".</p>\n"
                    + "<p>Best wishes,</p>"
                    + "<p>" + companyName + "</p>";
            mailPayload.setEmailBody(message);
            genericService.sendEmail(mailPayload, principal);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.success.ticket", new Object[]{ticket.getTicketType().getTicketTypeName() + " Ticket", "Created"}, Locale.ENGLISH));
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
    public XTicketPayload fetchOpenTicket(String principal) {
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Fetch all the tickets created by the user
            List<Tickets> tickets = xticketRepository.getOpenTicketsByUser(appUser);
            if (tickets != null) {
                List<XTicketPayload> ticketList = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(t.getCreatedAt().toLocalDate().toString());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setPriority(t.getPriority());
                    newTicket.setReopenedId(t.getTicketReopened() == null ? 0 : t.getTicketReopened().getId().intValue());
                    ticketList.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
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
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Fetch all the tickets created by the user
            List<Tickets> tickets = xticketRepository.getClosedTicketsByUser(appUser);
            if (tickets != null) {
                List<XTicketPayload> ticketList = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(t.getCreatedAt().toLocalDate().toString());
                    newTicket.setClosedAt(t.getCreatedAt().toLocalDate().toString());
                    newTicket.setClosedBy(t.getClosedBy().getLastName() + ", " + t.getClosedBy().getOtherName());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setPriority(t.getPriority());
                    ticketList.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
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
        XTicketPayload response = new XTicketPayload();
        try {
            Tickets ticket = xticketRepository.getTicketUsingId(Long.parseLong(id));
            if (ticket == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            BeanUtils.copyProperties(ticket, response);
            response.setCreatedAt(ticket.getCreatedAt().toLocalDate().toString());
            response.setId(ticket.getId().intValue());
            response.setTicketGroupName(ticket.getTicketGroup().getTicketGroupName());
            response.setTicketTypeName(ticket.getTicketType().getTicketTypeName());
            response.setPriority(ticket.getPriority());
            response.setReopenedId(ticket.getTicketReopened() == null ? 0 : ticket.getTicketReopened().getId().intValue());

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
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
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
    public XTicketPayload fetchTicketByUser(String principal) {
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
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
                    newTicket.setCreatedAt(t.getCreatedAt().toLocalDate().toString());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setPriority(t.getPriority());
                    newTicket.setMessage(t.getMessage());
                    ticketList.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
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
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
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
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
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
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            List<XTicketPayload> data = new ArrayList<>();
            //Fetch the ticket for the user
            List<Tickets> closedTickets = xticketRepository.getClosedTicketsByUser(appUser);
            List<Tickets> openTickets = xticketRepository.getOpenTicketsByUser(appUser);

            XTicketPayload closedTicket = new XTicketPayload();
            closedTicket.setValue(closedTickets.size());
            closedTicket.setName("Closed");
            data.add(closedTicket);

            XTicketPayload openTicket = new XTicketPayload();
            openTicket.setValue(openTickets.size());
            openTicket.setName("Open");
            data.add(openTicket);

            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
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
    public XTicketPayload closeTicket(String id, String ticketReopened, String ticketReopenedId, String principal) {
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            Tickets ticket = xticketRepository.getTicketUsingId(Long.parseLong(id));
            if (ticket == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{id}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //This is a close request for initial ticket
            if (ticketReopened.equalsIgnoreCase("f")) {
                //Update ticket
                ticket.setTicketOpen(false);
                ticket.setClosedAt(LocalDateTime.now());
                ticket.setClosedBy(appUser);
                xticketRepository.updateTicket(ticket);

                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //This is a request to close a reopened ticket
            TicketReopened ticketReopen = xticketRepository.getTicketReopenedUsingId(Long.parseLong(ticketReopenedId));
            if (ticketReopen == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{ticketReopenedId}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Update ticket
            ticket.setTicketOpen(false);
            ticket.setTicketReopen(false);
            xticketRepository.updateTicket(ticket);

            //Update the reopened ticket record
            ticketReopen.setClosedAt(LocalDateTime.now());
            ticketReopen.setClosedBy(appUser);
            xticketRepository.updateTicketReopen(ticketReopen);

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
    public XTicketPayload createReopenTicket(XTicketPayload requestPayload, String principal) {
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the person creating the record
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
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
            LocalDateTime slaExpiryTime = ticket.getTicketType().getSla().getTicketSlaPeriod() == 'D'
                    ? LocalDateTime.now().plusDays(ticket.getTicketType().getSla().getTicketSla())
                    : ticket.getTicketType().getSla().getTicketSlaPeriod() == 'H'
                    ? LocalDateTime.now().plusHours(ticket.getTicketType().getSla().getTicketSla())
                    : LocalDateTime.now().plusMinutes(ticket.getTicketType().getSla().getTicketSla());
            newReopenTicket.setSlaExpiry(slaExpiryTime);
            newReopenTicket.setPriority(ticket.getTicketType().getSla().getTicketSlaName());
            xticketRepository.createTicketReopen(newReopenTicket);

            //Reopen the ticket
            ticket.setTicketOpen(true);
            ticket.setTicketReopen(true);
            ticket.setTicketReopened(newReopenTicket);
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
            mailPayload.setSubject("Ticket Reply Notification");
            String slaExpiry = dtf.format(slaExpiryTime);
            String message = "<h4>Dear " + ticketAgent.getAgent().getLastName() + "</h4>\n"
                    + "<p>A ticket has been reopened as follows;</p>\n"
                    + "<p>Date Reopened: " + LocalDate.now().toString() + "</p>\n"
                    + "<p>Reopened By: " + appUser.getLastName() + ", " + appUser.getOtherName() + "</p>\n"
                    + "<p>Ticket ID: " + ticket.getTicketId() + "</p>\n"
                    + "<p>Ticket Group: " + ticket.getTicketType().getTicketGroup().getTicketGroupName() + "</p>\n"
                    + "<p>Ticket Type: " + ticket.getTicketType().getTicketTypeName() + "</p>\n"
                    + "<p>Ticket Priority: " + ticket.getPriority() + "</p>\n"
                    + "<p>SLA Expiry: " + slaExpiry + "</p>\n"
                    + "<p>To view the ticket details or take action <a href=\"" + host + "/xticket" + "\">click here</a></p>"
                    + "<p>For support and enquiries, email: " + companyEmail + ".</p>\n"
                    + "<p>Best wishes,</p>"
                    + "<p>" + companyName + "</p>";
            mailPayload.setEmailBody(message);
            genericService.sendEmail(mailPayload, principal);

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
    public XTicketPayload fetchOpenTicket() {
        XTicketPayload response = new XTicketPayload();
        try {
            //Fetch all the open tickets
            List<Tickets> tickets = xticketRepository.getOpenTickets();
            if (tickets != null) {
                List<XTicketPayload> ticketList = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(t.getCreatedAt().toLocalDate().toString());
                    newTicket.setCreatedBy(t.getCreatedBy().getLastName() + ", " + t.getCreatedBy().getOtherName());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setPriority(t.getPriority());
                    newTicket.setSlaExpiry(dtf.format(t.getSlaExpiry()));
                    newTicket.setReopenedId(t.getTicketReopened() == null ? 0 : t.getTicketReopened().getId().intValue());
                    ticketList.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
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
    public XTicketPayload fetchOpenTicketForAgent(String principal) {
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the agent
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
            if (appUser == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Fetch all the open tickets assigned to the agent
            List<Tickets> agentTickets = xticketRepository.getOpenAgentTickets(appUser);
            if (agentTickets != null) {
                List<XTicketPayload> data = new ArrayList<>();
                for (Tickets t : agentTickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(t.getCreatedAt().toLocalDate().toString());
                    newTicket.setCreatedBy(t.getCreatedBy().getLastName() + ", " + t.getCreatedBy().getOtherName());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setPriority(t.getPriority());
                    newTicket.setSlaExpiry(dtf.format(t.getSlaExpiry()));
                    newTicket.setReopenedId(t.getTicketReopened() == null ? 0 : t.getTicketReopened().getId().intValue());
                    data.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
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
        XTicketPayload response = new XTicketPayload();
        try {
            //Fetch all the Closed tickets 
            List<Tickets> tickets = xticketRepository.getClosedTickets();
            if (tickets != null) {
                List<XTicketPayload> ticketList = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(t.getCreatedAt().toLocalDate().toString());
                    newTicket.setClosedAt(t.getCreatedAt().toLocalDate().toString());
                    newTicket.setClosedBy(t.getClosedBy().getLastName() + ", " + t.getClosedBy().getOtherName());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setPriority(t.getPriority());
                    ticketList.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
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
            for (TicketAgent t : ticketAgents) {
                //Get the jobs currently assigned to the agent
                List<Tickets> ticketsByAgent = xticketRepository.getOpenAgentTickets(t.getAgent());
                jobStats[i] = String.valueOf(ticketsByAgent == null ? 0 : ticketsByAgent.size()) + "*" + String.valueOf(t.getId());
                i++;
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
        XTicketPayload response = new XTicketPayload();
        try {
            //Check if the user is valid. This is the agent
            AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
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
            if (Objects.equals(dtf, dtf)) {
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

            //Update the ticket record
            ticket.setTicketOpen(true);
            ticket.setTicketReassign(true);
            ticket.setTicketReassigned(ticketReassign);
            ticket.setSlaExpiry(slaExpiryTime);
            ticket.setTicketAgent(ticketAgent);
            xticketRepository.updateTicket(ticket);

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
    public XTicketPayload fetchTicketFullDetails(String ticketId) {
        XTicketPayload response = new XTicketPayload();
        try {
            //Fetch Ticket using the ticket id
            Tickets ticket = xticketRepository.getTicketUsingTicketId(ticketId);
            if (ticket == null) {
                response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[]{ticketId}, Locale.ENGLISH));
                response.setData(null);
                return response;
            }

            //Add details of the ticket
            response.setClosedAt(ticket.getClosedAt() == null ? null : dtf.format(ticket.getClosedAt()));
            response.setClosedBy(ticket.getClosedBy().getLastName() + ", " + ticket.getClosedBy().getOtherName());
            response.setCreatedBy(ticket.getClosedBy().getLastName() + ", " + ticket.getClosedBy().getOtherName());
            response.setCreatedAt(ticket.getCreatedAt() == null ? null : dtf.format(ticket.getCreatedAt()));
            response.setEscalated(ticket.isEscalated());
            response.setInternal(ticket.isInternal());
            response.setTicketId(ticketId);
            response.setTicketOpen(ticket.isTicketOpen());
            response.setReopened(ticket.isTicketReopen());
            response.setTicketGroupName(ticket.getTicketGroup().getTicketGroupName());
            response.setTicketTypeName(ticket.getTicketType().getTicketTypeName());
            response.setPriority(ticket.getPriority());
            response.setSlaExpiry(ticket.getSlaExpiry() == null ? null : dtf.format(ticket.getSlaExpiry()));
            response.setTicketLocked(ticket.isTicketLocked());
            response.setSource(ticket.getTicketSource());
            response.setMessage(ticket.getMessage());
            response.setSubject(ticket.getSubject());
            response.setTicketReopened(ticket.isTicketReopen());
            response.setTicketReassigned(ticket.isTicketReassign());
            response.setFileIndex(ticket.getFileIndex());

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
        boolean userType = false;
        AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
        if (appUser != null) {
            userType = appUser.isInternal();
        }

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
        XTicketPayload response = new XTicketPayload();
        try {
            List<Tickets> tickets = xticketRepository.getOpenTickets();
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

                //Loop through the list and transform
                List<XTicketPayload> data = new ArrayList<>();
                for (Tickets t : tickets) {
                    XTicketPayload newTicket = new XTicketPayload();
                    BeanUtils.copyProperties(t, newTicket);
                    newTicket.setCreatedAt(t.getCreatedAt().toLocalDate().toString());
                    newTicket.setCreatedBy(t.getCreatedBy().getLastName() + ", " + t.getCreatedBy().getOtherName());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setPriority(t.getPriority());
                    newTicket.setSlaExpiry(dtf.format(t.getSlaExpiry()));
                    newTicket.setTimeElapsed(getTimeElapsed(t.getSlaExpiry(), LocalDateTime.now()));
                    newTicket.setInternal(t.isInternal());
                    data.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
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
        XTicketPayload response = new XTicketPayload();
        try {
            List<Tickets> tickets = xticketRepository.getClosedTickets(LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()));
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
                    AppUser appUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
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
                    newTicket.setCreatedAt(t.getCreatedAt().toLocalDate().toString());
                    newTicket.setCreatedBy(t.getCreatedBy().getLastName() + ", " + t.getCreatedBy().getOtherName());
                    newTicket.setClosedAt(t.getClosedAt().toLocalDate().toString());
                    newTicket.setClosedBy(t.getClosedBy().getLastName() + ", " + t.getClosedBy().getOtherName());
                    newTicket.setTicketId(t.getTicketId());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
                    newTicket.setPriority(t.getPriority());
                    newTicket.setReopened(t.isTicketReopen());
                    newTicket.setSlaViolated(t.isSlaViolated());
                    newTicket.setInitialSla(t.getSla());
                    data.add(newTicket);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
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
    public XTicketPayload fetchAllAppUsers() {
        XTicketPayload response = new XTicketPayload();
        try {
            List<XTicketPayload> data = new ArrayList<>();
            List<AppUser> appUsers = xticketRepository.getUsers();
            if (appUsers != null) {
                for (AppUser t : appUsers) {
                    XTicketPayload payload = new XTicketPayload();
                    BeanUtils.copyProperties(t, payload);
                    payload.setCreatedAt(t.getCreatedAt().toLocalDate().toString());
                    payload.setCreatedBy(t.getLastName() + " " + t.getOtherName());
                    payload.setLastLogin(t.getLastLogin().format(dtf));
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
    public XTicketPayload fetchTicketByViolatedSla(XTicketPayload requestPayload) {
        XTicketPayload response = new XTicketPayload();
        try {
            List<Tickets> tickets = xticketRepository.getViolatedTickets(LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()));
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
                    AppUser appUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
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
                    newTicket.setCreatedAt(t.getCreatedAt().toLocalDate().toString());
                    newTicket.setCreatedBy(t.getCreatedBy().getLastName() + ", " + t.getCreatedBy().getOtherName());
                    newTicket.setId(t.getId().intValue());
                    newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                    newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
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
        XTicketPayload response = new XTicketPayload();
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
                        List<Tickets> ticketsByAgent = xticketRepository.getTicketClosedByAgent(t.getAgent());
                        payload.setCreatedAt(t.getCreatedAt().toLocalDate().toString());
                        payload.setCreatedBy(t.getCreatedBy().getLastName() + " " + t.getCreatedBy().getOtherName());
                        payload.setLastLogin(t.getAgent().getLastLogin().format(dtf));
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
                    List<Tickets> ticketsByAgent = xticketRepository.getTicketClosedByAgent(t);
                    payload.setCreatedAt(t.getCreatedAt().toLocalDate().toString());
                    payload.setCreatedBy(t.getLastName() + " " + t.getOtherName());
                    payload.setLastLogin(t.getLastLogin().format(dtf));
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
        XTicketPayload response = new XTicketPayload();
        try {
            AppUser appUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
            if (appUser != null) {
                List<Tickets> agentTickets = xticketRepository.getTicketClosedByAgent(appUser, LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()));
                if (agentTickets != null) {
                    List<XTicketPayload> data = new ArrayList<>();
                    for (Tickets t : agentTickets) {
                        XTicketPayload newTicket = new XTicketPayload();
                        BeanUtils.copyProperties(t, newTicket);
                        newTicket.setCreatedAt(t.getCreatedAt().toLocalDate().toString());
                        newTicket.setCreatedBy(t.getCreatedBy().getLastName() + ", " + t.getCreatedBy().getOtherName());
                        newTicket.setClosedAt(t.getClosedAt() == null ? null : t.getClosedAt().toLocalDate().toString());
                        newTicket.setClosedBy(t.getClosedBy().getLastName() + ", " + t.getClosedBy().getOtherName());
                        newTicket.setTicketGroupName(t.getTicketGroup().getTicketGroupName());
                        newTicket.setTicketTypeName(t.getTicketType().getTicketTypeName());
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
                    response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
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
        XTicketPayload response = new XTicketPayload();
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
                    reopen.setPriority(t.getPriority());
                    reopen.setTicketAgent(t.getTicketAgent().getAgent().getLastName() + ", " + t.getTicketAgent().getAgent().getOtherName());
                    data.add(reopen);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
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
        XTicketPayload response = new XTicketPayload();
        try {
            List<TicketReassign> tickets = xticketRepository.getDistinctTicketReassigned(LocalDate.parse(requestPayload.getStartDate()), LocalDate.parse(requestPayload.getEndDate()));
            if (tickets != null) {
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
                    AppUser appUser = xticketRepository.getAppUserUsingEmail(requestPayload.getEmail());
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
                    reassign.setPriority(t.getTicket().getPriority());
                    reassign.setInternal(t.getTicket().isInternal());
                    reassign.setTicketId(t.getTicket().getTicketId());
                    data.add(reassign);
                }
                response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response.setResponseMessage(messageSource.getMessage("appMessages.ticket.record", new Object[]{1}, Locale.ENGLISH));
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

    private String getTimeElapsed(LocalDateTime startTime, LocalDateTime endTime) {

        Date d0 = Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant());
        Date d1 = Date.from(endTime.atZone(ZoneId.systemDefault()).toInstant());

        // Calculating the time difference in milliseconds  
        long Time_difference = d1.getTime() - d0.getTime();

        // Calculating the time difference in terms of minutes,   
        // hours, seconds, years, and days  
        long seconds = TimeUnit.MILLISECONDS.toSeconds(Time_difference) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(Time_difference) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(Time_difference) % 24;
        long days = TimeUnit.MILLISECONDS.toDays(Time_difference) % 365;
        long years = TimeUnit.MILLISECONDS.toDays(Time_difference) / 365l;
        return days + " days " + hours + " hours " + minutes + " minutes ";
    }
}
