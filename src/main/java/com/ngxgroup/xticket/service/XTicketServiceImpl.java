package com.ngxgroup.xticket.service;

import com.ngxgroup.xticket.payload.XTicketPayload;
import com.google.gson.Gson;
import com.ngxgroup.xticket.constant.ResponseCodes;
import com.ngxgroup.xticket.model.AppRoles;
import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.model.GroupRoles;
import com.ngxgroup.xticket.model.RoleGroups;
import com.ngxgroup.xticket.model.TicketAgent;
import com.ngxgroup.xticket.model.TicketGroup;
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
import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    private BCryptPasswordEncoder passwordEncoder;
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
    @Value("${xticket.adauth.domain}")
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
    @Value("${xticket.password.pattern}")
    private String passwordPattern;

    @Override
    public XTicketPayload processSignin(XTicketPayload requestPayload) {
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
                    if(appUser.getEmail().contains(s)){
                    useADAuth = true;
                    }
                }
            }

            //Uset AD Authentication
            if (useADAuth) {
                Hashtable<Object, Object> env = new Hashtable<>();
                env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                env.put(Context.PROVIDER_URL, "ldap://" + "ngxgroup.com" + ":389");
                env.put(Context.SECURITY_AUTHENTICATION, "simple");
                env.put(Context.REFERRAL, "follow");
                env.put(Context.SECURITY_PRINCIPAL, requestPayload.getEmail());
                env.put(Context.SECURITY_CREDENTIALS, requestPayload.getPassword());

                // attempt to authenticate
                DirContext ctx = new InitialDirContext(env);
                ctx.close();
                if (ctx.equals("")) {
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
            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            return response;
        }
    }

    @Override
    public XTicketPayload processSignup(XTicketPayload requestPayload) {
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
                response.setResponseMessage(messageSource.getMessage("appMessages.user.exist", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH));
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
    public XTicketPayload processSignUpActivation(String activationId) {
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
    public XTicketPayload processFetchProfile(String principal) {
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
    public XTicketPayload processChangePassword(XTicketPayload requestPayload) {
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
    public XTicketPayload processForgotPassword(XTicketPayload requestPayload) {
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
    public List<AppUser> processFetchAppUsers() {
        return xticketRepository.getUsers();
    }

    @Override
    public List<AppUser> processFetchInternalAppUsers() {
        return xticketRepository.getInternalAppUsers();
    }

    @Override
    public void processUserOnline(String principal, boolean userOnline) {
        //Check if the user is valid. Update the online status
        AppUser appUser = xticketRepository.getAppUserUsingEmail(principal);
        if (appUser != null) {
            appUser.setOnline(userOnline);
            xticketRepository.updateAppUser(appUser);
        }
    }

    @Override
    public XTicketPayload processUpdateAppUser(XTicketPayload requestPayload, String principal) {
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
    public List<RoleGroups> processFetchRoleGroup() {
        return xticketRepository.getRoleGroupList();
    }

    @Override
    public XTicketPayload processFetchRoleGroup(String id) {
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
    public List<AppRoles> processFetchAppRoles() {
        return xticketRepository.getAppRoles();
    }

    @Override
    public XTicketPayload processCreateRoleGroup(XTicketPayload requestPayload, String principal) {
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
    public XTicketPayload processDeleteRoleGroup(String id, String principal) {
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
    public XTicketPayload processFetchGroupRoles(String groupName) {
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
    public XTicketPayload processUpdateGroupRoles(XTicketPayload requestPayload) {
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

    /**
     * Ticket Group *
     */
    @Override
    public XTicketPayload processFetchTicketGroup() {
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
    public XTicketPayload processFetchTicketGroup(String id) {
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
    public XTicketPayload processCreateTicketGroup(XTicketPayload requestPayload, String principal) {
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
    public XTicketPayload processDeleteTicketGroup(String id, String principal) {
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
    public XTicketPayload processFetchTicketType() {
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
    public XTicketPayload processFetchTicketType(String id) {
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
    public XTicketPayload processCreateTicketType(XTicketPayload requestPayload, String principal) {
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

                TicketType newTicketType = new TicketType();
                newTicketType.setCreatedAt(LocalDateTime.now());
                newTicketType.setCreatedBy(appUser);
                newTicketType.setTicketTypeCode(requestPayload.getTicketTypeCode());
                newTicketType.setTicketTypeName(requestPayload.getTicketTypeName());
                newTicketType.setEscalationEmails(requestPayload.getEscalationEmails());
                newTicketType.setInternal(requestPayload.isInternal());
                newTicketType.setEmailEscalationIndex(0);
                newTicketType.setSlaMins(requestPayload.getSlaMins());
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

            ticketType.setCreatedAt(LocalDateTime.now());
            ticketType.setCreatedBy(appUser);
            ticketType.setStatus(requestPayload.getStatus());
            ticketType.setTicketTypeCode(requestPayload.getTicketTypeCode());
            ticketType.setTicketTypeName(requestPayload.getTicketTypeName());
            ticketType.setEscalationEmails(requestPayload.getEscalationEmails());
            ticketType.setInternal(requestPayload.isInternal());
            ticketType.setEmailEscalationIndex(0);
            ticketType.setSlaMins(requestPayload.getSlaMins());
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
    public XTicketPayload processDeleteTicketType(String id, String principal) {
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
    public List<TicketType> processFetchTicketTypeUsingGroup(String ticketGroupCode, String principal) {
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
     * Ticket Agent *
     */
    @Override
    public XTicketPayload processFetchTicketAgent() {
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
    public XTicketPayload processCreateTicketAgent(XTicketPayload requestPayload, String principal) {
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
    public XTicketPayload processFetchAgentTicketTypes(String principal) {
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
    public XTicketPayload processCreateTicket(XTicketPayload requestPayload, String principal) {
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

            String ticketId = UUID.randomUUID().toString();
            Tickets newTicket = new Tickets();
            newTicket.setClosedAt(null);
            newTicket.setClosedBy(null);
            newTicket.setCreatedAt(LocalDateTime.now());
            newTicket.setCreatedBy(appUser);
            newTicket.setEscalated(false);
            newTicket.setInternal(ticketType.isInternal());
            newTicket.setTicketGroup(ticketGroup);
            newTicket.setTicketId(ticketId);
            newTicket.setTicketOpen(true);
            newTicket.setTicketReopened(false);
            newTicket.setTicketType(ticketType);
            xticketRepository.createTicket(newTicket);

            if (!requestPayload.getUploadedFiles().isEmpty()) {
                String newFileName = ticketId.replace("-", "");
                int fileIndex = 1;
                for (MultipartFile f : requestPayload.getUploadedFiles()) {
                    String fileExt = FilenameUtils.getExtension(f.getOriginalFilename());
                    //Copy the file to destination
                    String path = servletContext.getRealPath("/") + "WEB-INF/classes/document" + "/" + newFileName + fileIndex + "." + fileExt;
                    File newFile = new File(path);
                    FileCopyUtils.copy(f.getBytes(), newFile);
                    fileIndex++;
                }
            }

            //Get the ticket agents
            List<TicketAgent> ticketAgents = xticketRepository.getTicketAgentUsingTicketType(ticketType);
            if (ticketAgents != null) {
                for (TicketAgent t : ticketAgents) {
                    //Send notification to ticket agents
                    XTicketPayload mailPayload = new XTicketPayload();
                    mailPayload.setRecipientEmail(t.getAgent().getEmail());
                    mailPayload.setSubject("Ticket Request Notification");
                    String message = "<h4>Dear " + t.getAgent().getLastName() + "</h4>\n"
                            + "<p>A ticket has been opened as follows;</p>\n"
                            + "<p>Date Created: " + LocalDate.now().toString() + "</p>\n"
                            + "<p>Initiated By: " + t.getAgent().getLastName() + ", " + t.getAgent().getOtherName() + "</p>\n"
                            + "<p>Ticket ID: " + ticketId + "</p>\n"
                            + "<p>Ticket Group: " + ticketType.getTicketGroup().getTicketGroupName() + "</p>\n"
                            + "<p>Ticket Type: " + ticketType.getTicketTypeName() + "</p>\n"
                            + "<p>SLA Expiry: " + dtf.format(LocalDateTime.now().plusMinutes(ticketType.getSlaMins())) + "</p>\n"
                            + "<p>To view the ticket details or take action <a href=\"" + host + "/xticket" + "\">click here</a></p>"
                            + "<p>For support and enquiries, email: " + companyEmail + ".</p>\n"
                            + "<p>Best wishes,</p>"
                            + "<p>" + companyName + "</p>";
                    mailPayload.setEmailBody(message);
                    genericService.sendEmail(mailPayload, principal);
                }
            }

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
}
