package com.ngxgroup.xticket.service;

import com.ngxgroup.xticket.payload.XTicketPayload;
import com.google.gson.Gson;
import com.ngxgroup.xticket.constant.ResponseCodes;
import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.model.AuditLog;
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

/**
 *
 * @author briano
 */
@Service
public class XTicketServiceImpl implements XTicketService {

    @Autowired
    MessageSource messageSource;
    @Autowired
    XTicketRepository xticketRepository;
    @Autowired
    GenericService genericService;
    @Autowired
    Gson gson;
    @Value("${xticket.password.retry.count}")
    private String passwordRetryCount;
    @Value("${xticket.password.reset.time}")
    private String passwordResetTime;
    @Value("${xticket.ngx.email.domain}")
    private String emailDomain;
    @Value("${xticket.email.notification}")
    private String emailNotification;
    static final Logger logger = Logger.getLogger(XTicketServiceImpl.class.getName());

    @Override
    public XTicketPayload processSignin(XTicketPayload requestPayload) {
        XTicketPayload xticketPayload = new XTicketPayload();
        LogPayload log = new LogPayload();
        try {
            AppUser appUser = xticketRepository.getAppUserUsingUsername(requestPayload.getEmail());
            if (appUser == null) {
                String message = messageSource.getMessage("appMessages.user.notexist", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Login");
                log.setUsername(requestPayload.getEmail());
                logger.log(Level.INFO, gson.toJson(log));
                //Log the error
                xticketPayload.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                xticketPayload.setResponseMessage(message);
                return xticketPayload;
            }

            //Check if user is disable
            if (!appUser.isEnabled() || appUser.isLocked()) {
                String message = messageSource.getMessage("appMessages.user.disabled", new Object[0], Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Login");
                log.setUsername(requestPayload.getEmail());
                logger.log(Level.INFO, gson.toJson(log));
                xticketPayload.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                xticketPayload.setResponseMessage(message);
                return xticketPayload;
            }

            //Check authentication
            String response = authenticateADUser(requestPayload.getEmail(), requestPayload.getPassword(), appUser.getCompany().getDomainController(), "");
            if (!response.equalsIgnoreCase("Success")) {
                //Check for network connection
                if (response.equalsIgnoreCase(appUser.getCompany().getDomainController() + ":389")) {
                    //Try again using the IP Address of the Domain Controller
                    response = authenticateADUser(requestPayload.getEmail(), requestPayload.getPassword(), appUser.getCompany().getDomainControllerIp(), "");
                    if (response.equalsIgnoreCase(appUser.getCompany().getDomainControllerIp() + ":389")) {
                        String message = messageSource.getMessage("appMessages.connection.failed", new Object[]{appUser.getCompany().getDomainController()}, Locale.ENGLISH);
                        log.setMessage(message);
                        log.setSeverity("INFO");
                        log.setSource("Login");
                        log.setUsername(requestPayload.getEmail());
                        logger.log(Level.INFO, gson.toJson(log));
                        xticketPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
                        xticketPayload.setResponseMessage(message);
                        return xticketPayload;
                    }

                    //Clear failed login
                    appUser.setLoginFailCount(0);
                    xticketRepository.updateAppUser(appUser);
                    xticketRepository.updateAppUser(appUser);

                    AuditLog newAudit = new AuditLog();
                    newAudit.setAuditAction(appUser.getName() + " Login as " + requestPayload.getEmail() + " Successful");
                    newAudit.setAuditCategory("Successful Login");
                    newAudit.setAuditClass("Login");
                    newAudit.setCreatedAt(LocalDateTime.now());
                    newAudit.setNewValue("");
                    newAudit.setOldValue("");
                    newAudit.setRefNo("");
                    newAudit.setUsername(requestPayload.getEmail());
                    xticketRepository.createAuditLog(newAudit);

                    String message = messageSource.getMessage("appMessages.success.signin", new Object[0], Locale.ENGLISH);
                    xticketPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                    xticketPayload.setResponseMessage(message);

                    //Set the last login
                    appUser.setLastLogin(LocalDate.now());
                    log.setMessage(message);
                    log.setSeverity("INFO");
                    log.setSource("Login");
                    log.setUsername(requestPayload.getEmail());
                    logger.log(Level.INFO, gson.toJson(log));
                    return xticketPayload;
                }

                //Check the fail count
                if (appUser.getLoginFailCount() == Integer.parseInt(passwordRetryCount)) {
                    appUser.setLoginFailCount(appUser.getLoginFailCount() + 1);
                    appUser.setResetTime(LocalDateTime.now().plusMinutes(Integer.parseInt(passwordResetTime)));
                    xticketRepository.updateAppUser(appUser);

                    String message = messageSource.getMessage("appMessages.user.multiple.attempt", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH);
                    log.setMessage(message);
                    log.setSeverity("INFO");
                    log.setSource("Login");
                    log.setUsername(requestPayload.getEmail());
                    logger.log(Level.INFO, gson.toJson(log));
                    xticketPayload.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                    xticketPayload.setResponseMessage(message);
                    return xticketPayload;
                }

                //Login failed. Set fail count
                appUser.setLoginFailCount(appUser.getLoginFailCount() + 1);
                xticketRepository.updateAppUser(appUser);

                AuditLog newAudit = new AuditLog();
                newAudit.setAuditAction(appUser.getName() + " Login as " + requestPayload.getEmail() + " Failed");
                newAudit.setAuditCategory("Failed Login");
                newAudit.setAuditClass("Login");
                newAudit.setCreatedAt(LocalDateTime.now());
                newAudit.setNewValue("");
                newAudit.setOldValue("");
                newAudit.setRefNo("");
                newAudit.setUsername(requestPayload.getEmail());
                xticketRepository.createAuditLog(newAudit);

                String message = messageSource.getMessage("appMessages.login.failed", new Object[0], Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Login");
                log.setUsername(requestPayload.getEmail());
                logger.log(Level.INFO, gson.toJson(log));
                xticketPayload.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                xticketPayload.setResponseMessage(message);
                return xticketPayload;
            }

            //Clear failed login
            appUser.setLoginFailCount(0);
            xticketRepository.updateAppUser(appUser);
            xticketRepository.updateAppUser(appUser);

            AuditLog newAudit = new AuditLog();
            newAudit.setAuditAction(appUser.getName() + " Login as " + requestPayload.getEmail() + " Failed");
            newAudit.setAuditCategory("Failed Login");
            newAudit.setAuditClass("Login");
            newAudit.setCreatedAt(LocalDateTime.now());
            newAudit.setNewValue("");
            newAudit.setOldValue("");
            newAudit.setRefNo("");
            newAudit.setUsername(requestPayload.getEmail());
            xticketRepository.createAuditLog(newAudit);

            String message = messageSource.getMessage("appMessages.success.signin", new Object[0], Locale.ENGLISH);
            xticketPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            xticketPayload.setResponseMessage(message);

            //Set the last login
            appUser.setLastLogin(LocalDate.now());
            log.setMessage(message);
            log.setSeverity("INFO");
            log.setSource("Login");
            log.setUsername(requestPayload.getEmail());
            logger.log(Level.INFO, gson.toJson(log));
            return xticketPayload;
        } catch (Exception ex) {
            xticketPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            xticketPayload.setResponseMessage(ex.getMessage());
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Login");
            log.setUsername(requestPayload.getEmail());
            logger.log(Level.INFO, gson.toJson(log));
            return xticketPayload;
        }
    }

    @Override
    public XTicketPayload processSignup(XTicketPayload requestPayload) {
        XTicketPayload response = new XTicketPayload();
        try {

            return response;
        } catch (Exception ex) {
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(ex.getMessage());
            return response;
        }
    }

    @Override
    public String processSignUpActivation(String id) {
        try {

            return "";
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String processDashboard(String username) {
        try {

            return "";
        } catch (Exception ex) {
            return ex.getMessage();
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

}
