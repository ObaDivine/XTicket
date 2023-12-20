package com.ngxgroup.xpolicy.service;

import com.ngxgroup.xpolicy.payload.XPolicyPayload;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.ngxgroup.xpolicy.constant.ResponseCodes;
import com.ngxgroup.xpolicy.model.AppRoles;
import com.ngxgroup.xpolicy.model.AppUser;
import com.ngxgroup.xpolicy.model.AuditLog;
import com.ngxgroup.xpolicy.model.Company;
import com.ngxgroup.xpolicy.model.Department;
import com.ngxgroup.xpolicy.model.Division;
import com.ngxgroup.xpolicy.model.GroupRoles;
import com.ngxgroup.xpolicy.model.Notification;
import com.ngxgroup.xpolicy.model.Policy;
import com.ngxgroup.xpolicy.model.PolicyRead;
import com.ngxgroup.xpolicy.model.PolicyReview;
import com.ngxgroup.xpolicy.model.PolicyTemp;
import com.ngxgroup.xpolicy.model.PolicyType;
import com.ngxgroup.xpolicy.model.RoleGroups;
import com.ngxgroup.xpolicy.payload.LogPayload;
import com.ngxgroup.xpolicy.payload.LoginPayload;
import com.ngxgroup.xpolicy.repository.XPolicyRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.WordUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

/**
 *
 * @author briano
 */
@Service
public class XPolicyServiceImpl implements XPolicyService {

    @Autowired
    MessageSource messageSource;
    @Autowired
    XPolicyRepository xpolicyRepository;
    @Autowired
    GenericService genericService;
    @Autowired
    Gson gson;
    @Value("${xpolicy.password.retry.count}")
    private String passwordRetryCount;
    @Value("${xpolicy.password.reset.time}")
    private String passwordResetTime;
    @Value("${xpolicy.qrcode.height}")
    private int qrCodeHeight;
    @Value("${xpolicy.qrcode.width}")
    private int qrCodeWidth;
    @Value("${xpolicy.qrcode.image.url}")
    private String qrCodeBaseDir;
    @Value("${xpolicy.download.policy.base.dir}")
    private String uploadBaseDir;
    @Value("${xpolicy.ngx.email.domain}")
    private String emailDomain;
    @Value("${xpolicy.email.notification}")
    private String emailNotification;
    static final Logger logger = Logger.getLogger(XPolicyServiceImpl.class.getName());

    @Override
    public XPolicyPayload processSignin(LoginPayload requestPayload) {
        XPolicyPayload xpolicyPayload = new XPolicyPayload();
        LogPayload log = new LogPayload();
        try {
            AppUser appUser = xpolicyRepository.getAppUserUsingUsername(requestPayload.getEmail());
            if (appUser == null) {
                String message = messageSource.getMessage("appMessages.user.notexist", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Login");
                log.setUsername(requestPayload.getEmail());
                logger.log(Level.INFO, gson.toJson(log));
                //Log the error
                xpolicyPayload.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            //fetch the secret key
            String secretKey = genericService.decryptString(appUser.getTwoFactorSecretKey());
            String totp = genericService.getTOTP(secretKey);
            String userOTP = requestPayload.getOtp1() + requestPayload.getOtp2() + requestPayload.getOtp3() + requestPayload.getOtp4() + requestPayload.getOtp5() + requestPayload.getOtp6();
            //Check if user is disable
            if (!appUser.isEnabled() || appUser.isLocked()) {
                String message = messageSource.getMessage("appMessages.user.disabled", new Object[0], Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Login");
                log.setUsername(requestPayload.getEmail());
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
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
                        xpolicyPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
                        xpolicyPayload.setResponseMessage(message);
                        return xpolicyPayload;
                    }

                    //Connection succeeded
                    if (!totp.equalsIgnoreCase(userOTP)) {
                        String message = messageSource.getMessage("appMessages.invalid.totp", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH);
                        log.setMessage(message);
                        log.setSeverity("INFO");
                        log.setSource("Login");
                        log.setUsername(requestPayload.getEmail());
                        logger.log(Level.INFO, gson.toJson(log));
                        xpolicyPayload.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                        xpolicyPayload.setResponseMessage(message);
                        return xpolicyPayload;
                    }
                    //Clear failed login
                    appUser.setLoginFailCount(0);
                    xpolicyRepository.updateAppUser(appUser);
                    xpolicyRepository.updateAppUser(appUser);

                    AuditLog newAudit = new AuditLog();
                    newAudit.setAuditAction(appUser.getName() + " Login as " + requestPayload.getEmail());
                    newAudit.setAuditCategory("Login");
                    newAudit.setAuditClass("Login");
                    newAudit.setCreatedAt(LocalDateTime.now());
                    newAudit.setNewValue("");
                    newAudit.setOldValue("");
                    newAudit.setRefNo("");
                    newAudit.setUsername(requestPayload.getEmail());
                    xpolicyRepository.createAuditLog(newAudit);

                    String message = messageSource.getMessage("appMessages.success.signin", new Object[0], Locale.ENGLISH);
                    xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                    xpolicyPayload.setResponseMessage(message);

                    //Set the last login
                    appUser.setLastLogin(LocalDate.now());
                    log.setMessage(message);
                    log.setSeverity("INFO");
                    log.setSource("Login");
                    log.setUsername(requestPayload.getEmail());
                    logger.log(Level.INFO, gson.toJson(log));
                    return xpolicyPayload;
                }

                //Check the fail count
                if (appUser.getLoginFailCount() == Integer.parseInt(passwordRetryCount)) {
                    appUser.setLoginFailCount(appUser.getLoginFailCount() + 1);
                    appUser.setResetTime(LocalDateTime.now().plusMinutes(Integer.parseInt(passwordResetTime)));
                    xpolicyRepository.updateAppUser(appUser);

                    String message = messageSource.getMessage("appMessages.user.multiple.attempt", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH);
                    log.setMessage(message);
                    log.setSeverity("INFO");
                    log.setSource("Login");
                    log.setUsername(requestPayload.getEmail());
                    logger.log(Level.INFO, gson.toJson(log));
                    xpolicyPayload.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                    xpolicyPayload.setResponseMessage(message);
                    return xpolicyPayload;
                }

                //Login failed. Set fail count
                appUser.setLoginFailCount(appUser.getLoginFailCount() + 1);
                xpolicyRepository.updateAppUser(appUser);

                String message = messageSource.getMessage("appMessages.login.failed", new Object[0], Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Login");
                log.setUsername(requestPayload.getEmail());
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            if (!totp.equalsIgnoreCase(userOTP)) {
                String message = messageSource.getMessage("appMessages.invalid.totp", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Login");
                log.setUsername(requestPayload.getEmail());
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            //Clear failed login
            appUser.setLoginFailCount(0);
            xpolicyRepository.updateAppUser(appUser);
            xpolicyRepository.updateAppUser(appUser);

            AuditLog newAudit = new AuditLog();
            newAudit.setAuditAction(appUser.getName() + " Login as " + requestPayload.getEmail());
            newAudit.setAuditCategory("Login");
            newAudit.setAuditClass("Login");
            newAudit.setCreatedAt(LocalDateTime.now());
            newAudit.setNewValue("");
            newAudit.setOldValue("");
            newAudit.setRefNo("");
            newAudit.setUsername(requestPayload.getEmail());
            xpolicyRepository.createAuditLog(newAudit);

            String message = messageSource.getMessage("appMessages.success.signin", new Object[0], Locale.ENGLISH);
            xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            xpolicyPayload.setResponseMessage(message);

            //Set the last login
            appUser.setLastLogin(LocalDate.now());
            log.setMessage(message);
            log.setSeverity("INFO");
            log.setSource("Login");
            log.setUsername(requestPayload.getEmail());
            logger.log(Level.INFO, gson.toJson(log));
            return xpolicyPayload;
        } catch (Exception ex) {
            xpolicyPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            xpolicyPayload.setResponseMessage(ex.getMessage());
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Login");
            log.setUsername(requestPayload.getEmail());
            logger.log(Level.INFO, gson.toJson(log));
            return xpolicyPayload;
        }
    }

    @Override
    public String processSignUp(XPolicyPayload requestPayload) {
        try {

            return "";
        } catch (Exception ex) {
            return ex.getMessage();
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
    public String validateTwoFactorAuthentication(XPolicyPayload requestPayload) {
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

    @Override
    public XPolicyPayload generateQRCode(XPolicyPayload requestPayload) {
        XPolicyPayload xpolicyPayload = new XPolicyPayload();
        LogPayload log = new LogPayload();
        try {
            //Check if the email exist in the AD
            AppUser appUser = xpolicyRepository.getAppUserUsingUsername(requestPayload.getEmail());
            if (appUser == null) {
                String message = messageSource.getMessage("appMessages.user.notexist", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("QR Code");
                log.setUsername(requestPayload.getEmail());
                logger.log(Level.INFO, gson.toJson(log));
                //Log the error
                xpolicyPayload.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            String decryptedSecretKey = genericService.decryptString(appUser.getTwoFactorSecretKey());
            String qrcodeData = "otpauth://totp/"
                    + URLEncoder.encode("NGX Group Policy" + ":" + requestPayload.getEmail(), "UTF-8").replace("+", "%20")
                    + "?secret=" + URLEncoder.encode(decryptedSecretKey, "UTF-8").replace("+", "%20")
                    + "&issuer=" + URLEncoder.encode("NGX Group Policy", "UTF-8").replace("+", "%20");

            BitMatrix matrix = new MultiFormatWriter().encode(qrcodeData, BarcodeFormat.QR_CODE, qrCodeWidth, qrCodeHeight);
            String dest = qrCodeBaseDir + "/" + requestPayload.getEmail() + ".png";
            File file = new File(dest);
            file.createNewFile();
            FileOutputStream out = new FileOutputStream(file);
            MatrixToImageWriter.writeToStream(matrix, "png", out);
            xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            xpolicyPayload.setQrCodeImage(requestPayload.getEmail() + ".png");

            AuditLog newAudit = new AuditLog();
            newAudit.setAuditAction(requestPayload.getEmail() + " Generated QR Code");
            newAudit.setAuditCategory("QR Code");
            newAudit.setAuditClass("Create");
            newAudit.setCreatedAt(LocalDateTime.now());
            newAudit.setNewValue("");
            newAudit.setOldValue("");
            newAudit.setRefNo("");
            newAudit.setUsername(requestPayload.getEmail());
            xpolicyRepository.createAuditLog(newAudit);

            return xpolicyPayload;
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("QR Code");
            log.setUsername(requestPayload.getEmail());
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            xpolicyPayload.setResponseMessage(ex.getMessage());
            return xpolicyPayload;
        }
    }

    @Override
    public List<Company> getCompanyList() {
        return xpolicyRepository.getCompanies();
    }

    @Override
    public List<Division> processCompanyDivisionUsingId(String company) {
        LogPayload log = new LogPayload();
        try {
            Company companyRecord = xpolicyRepository.getCompanyUsingId(Long.parseLong(company));
            if (companyRecord != null) {
                List<Division> companyDivisions = xpolicyRepository.getDivisionUsingCompany(companyRecord);
                return companyDivisions;
            }
            return null;
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Company");
            log.setUsername("System");
            logger.log(Level.INFO, gson.toJson(log));
            return null;
        }
    }

    @Override
    public List<Department> processDivisionDepartmentsUsingId(String division) {
        LogPayload log = new LogPayload();
        try {
            Division divisionRecord = xpolicyRepository.getDivisionUsingId(Long.parseLong(division));
            if (divisionRecord != null) {
                List<Department> divisionDepartments = xpolicyRepository.getDepartmentUsingDivision(divisionRecord);
                return divisionDepartments;
            }
            return null;
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Department");
            log.setUsername("System");
            logger.log(Level.INFO, gson.toJson(log));
            return null;
        }
    }

    @Override
    public int[] fetchStatistics() {
        int[] stats = new int[11];
        try {
            int company = xpolicyRepository.getCompanies().size();
            stats[0] = company;
            PolicyType policy = xpolicyRepository.getPolicyTypeUsingCode("POL");
            if (policy != null) {
                List<Policy> policies = xpolicyRepository.getPoliciesUsingType(policy);
                if (policies != null) {
                    stats[1] = policies.size();
                } else {
                    stats[1] = 0;
                }
            } else {
                stats[1] = 0;
            }
            PolicyType sop = xpolicyRepository.getPolicyTypeUsingCode("SOP");
            if (sop != null) {
                List<Policy> sops = xpolicyRepository.getPoliciesUsingType(sop);
                if (sops != null) {
                    stats[2] = sops.size();
                } else {
                    stats[2] = 0;
                }

            } else {
                stats[2] = 0;
            }
            PolicyType charter = xpolicyRepository.getPolicyTypeUsingCode("CHT");
            if (charter != null) {
                List<Policy> charters = xpolicyRepository.getPoliciesUsingType(charter);
                if (charters != null) {
                    stats[3] = charters.size();
                } else {
                    stats[3] = 0;
                }
            } else {
                stats[3] = 0;
            }
            PolicyType framework = xpolicyRepository.getPolicyTypeUsingCode("FWK");
            if (framework != null) {
                List<Policy> frameworks = xpolicyRepository.getPoliciesUsingType(framework);
                if (frameworks != null) {
                    stats[4] = frameworks.size();
                } else {
                    stats[4] = 0;
                }
            } else {
                stats[4] = 0;
            }

            PolicyType procedure = xpolicyRepository.getPolicyTypeUsingCode("PRO");
            if (procedure != null) {
                List<Policy> procedures = xpolicyRepository.getPoliciesUsingType(procedure);
                if (procedures != null) {
                    stats[5] = procedures.size();
                } else {
                    stats[5] = 0;
                }
            } else {
                stats[5] = 0;
            }

            PolicyType form = xpolicyRepository.getPolicyTypeUsingCode("FRM");
            if (form != null) {
                List<Policy> forms = xpolicyRepository.getPoliciesUsingType(form);
                if (forms != null) {
                    stats[6] = forms.size();
                } else {
                    stats[6] = 0;
                }
            } else {
                stats[6] = 0;
            }

            PolicyType standard = xpolicyRepository.getPolicyTypeUsingCode("STD");
            if (standard != null) {
                List<Policy> standards = xpolicyRepository.getPoliciesUsingType(standard);
                if (standards != null) {
                    stats[7] = standards.size();
                } else {
                    stats[7] = 0;
                }
            } else {
                stats[7] = 0;
            }

            PolicyType schedule = xpolicyRepository.getPolicyTypeUsingCode("SCH");
            if (schedule != null) {
                List<Policy> schedules = xpolicyRepository.getPoliciesUsingType(schedule);
                if (schedules != null) {
                    stats[8] = schedules.size();
                } else {
                    stats[8] = 0;
                }
            } else {
                stats[8] = 0;
            }
            return stats;
        } catch (Exception ex) {
            stats = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
            return stats;
        }
    }

    @Override
    public List<Policy> processPolicy(XPolicyPayload xpolicyPayload, String principal) {
        AppUser appUser = xpolicyRepository.getAppUserUsingUsername(principal);
        LogPayload log = new LogPayload();
        if (appUser == null) {
            return null;
        }

        PolicyType policyType = xpolicyRepository.getPolicyTypeUsingCode(xpolicyPayload.getPolicyType());
        List<Policy> recordList = new ArrayList<>();
        try {
            if (xpolicyPayload.getCompany().equalsIgnoreCase("") && xpolicyPayload.getDivision().equalsIgnoreCase("") && xpolicyPayload.getDepartment().equalsIgnoreCase("")) {
                recordList = xpolicyRepository.getPolicies(policyType, appUser.getAccessLevel());
                return recordList;
            }
            if (!xpolicyPayload.getCompany().equalsIgnoreCase("") && !xpolicyPayload.getDivision().equalsIgnoreCase("") && !xpolicyPayload.getDepartment().equalsIgnoreCase("")) {
                Company company = xpolicyRepository.getCompanyUsingId(Long.parseLong(xpolicyPayload.getCompany()));
                Division division = xpolicyRepository.getDivisionUsingId(Long.parseLong(xpolicyPayload.getDivision()));
                Department department = xpolicyRepository.getDepartmentUsingId(Long.parseLong(xpolicyPayload.getDepartment()));
                recordList = xpolicyRepository.getPolicies(company, division, department, policyType, appUser.getAccessLevel());
                return recordList;
            }
            if (xpolicyPayload.getCompany().equalsIgnoreCase("") && !xpolicyPayload.getDivision().equalsIgnoreCase("") && !xpolicyPayload.getDepartment().equalsIgnoreCase("")) {
                Division division = xpolicyRepository.getDivisionUsingId(Long.parseLong(xpolicyPayload.getDivision()));
                Department department = xpolicyRepository.getDepartmentUsingId(Long.parseLong(xpolicyPayload.getDepartment()));
                recordList = xpolicyRepository.getPolicies(division, department, policyType, appUser.getAccessLevel());
                return recordList;
            }
            if (xpolicyPayload.getCompany().equalsIgnoreCase("") && xpolicyPayload.getDivision().equalsIgnoreCase("") && !xpolicyPayload.getDepartment().equalsIgnoreCase("")) {
                Department department = xpolicyRepository.getDepartmentUsingId(Long.parseLong(xpolicyPayload.getDepartment()));
                recordList = xpolicyRepository.getPolicies(department, policyType, appUser.getAccessLevel());
                return recordList;
            }
            if (!xpolicyPayload.getCompany().equalsIgnoreCase("") && xpolicyPayload.getDivision().equalsIgnoreCase("") && xpolicyPayload.getDepartment().equalsIgnoreCase("")) {
                Company company = xpolicyRepository.getCompanyUsingId(Long.parseLong(xpolicyPayload.getCompany()));
                recordList = xpolicyRepository.getPolicies(company, policyType, appUser.getAccessLevel());
                return recordList;
            }
            if (!xpolicyPayload.getCompany().equalsIgnoreCase("") && !xpolicyPayload.getDivision().equalsIgnoreCase("") && xpolicyPayload.getDepartment().equalsIgnoreCase("")) {
                Company company = xpolicyRepository.getCompanyUsingId(Long.parseLong(xpolicyPayload.getCompany()));
                Division division = xpolicyRepository.getDivisionUsingId(Long.parseLong(xpolicyPayload.getDivision()));
                recordList = xpolicyRepository.getPolicies(company, division, policyType, appUser.getAccessLevel());
                return recordList;
            }
            return recordList;
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Policy");
            log.setUsername(principal);
            logger.log(Level.INFO, gson.toJson(log));
            return null;
        }
    }

    @Override
    public Policy processPolicyUsingId(String id) {
        return xpolicyRepository.getPolicyUsingId(Long.parseLong(id));
    }

    @Override
    public void processPolicyRead(String fileName, String principal) {
        LogPayload log = new LogPayload();
        try {
            AppUser appUser = xpolicyRepository.getAppUserUsingUsername(principal);
            Policy policy = xpolicyRepository.getPolicyUsingName(fileName);
            if (appUser != null && policy != null) {
                PolicyRead policyRead = xpolicyRepository.getPolicyReadUsingUserAndPolicy(appUser, policy);
                if (policyRead == null) {
                    PolicyRead newPolicyRead = new PolicyRead();
                    newPolicyRead.setAppUser(appUser);
                    newPolicyRead.setCreatedAt(LocalDateTime.now());
                    newPolicyRead.setPolicy(policy);
                    xpolicyRepository.createPolicyRead(newPolicyRead);
                } else {
                    policyRead.setCreatedAt(LocalDateTime.now());
                    xpolicyRepository.updatePolicyRead(policyRead);
                }
            }
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Policy");
            log.setUsername(principal);
            logger.log(Level.INFO, gson.toJson(log));
        }
    }

    @Override
    public List<Policy> getMustReadPolicy() {
        LogPayload log = new LogPayload();
        try {
            return xpolicyRepository.getMustReadPolicy();
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Policy");
            log.setUsername("System");
            logger.log(Level.INFO, gson.toJson(log));
            return null;
        }
    }

    @Override
    public String getPrincipalName(String principal) {
        try {
            if (principal == null) {
                return "Guest";
            }

            AppUser appUser = xpolicyRepository.getAppUserUsingUsername(principal);
            if (appUser == null) {
                return "Guest";
            }

            return appUser.getName();
        } catch (Exception ex) {
            return "Guest";
        }
    }

    @Override
    public List<Notification> getNotifications(String principal) {
        LogPayload log = new LogPayload();
        try {
            AppUser appUser = xpolicyRepository.getAppUserUsingUsername(principal);
            if (appUser == null) {
                return null;
            }
            List<Notification> notifications = xpolicyRepository.getNotifications(appUser.getName());
            return notifications;
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Notification");
            log.setUsername(principal);
            logger.log(Level.INFO, gson.toJson(log));
            return null;
        }
    }

    @Override
    public List<GroupRoles> getUserRoles(String principal) {
        AppUser appUser = xpolicyRepository.getAppUserUsingUsername(principal);
        if (appUser == null) {
            return null;
        }

        List<GroupRoles> groupRoles = xpolicyRepository.getGroupRolesUsingRoleGroup(appUser.getRole());
        return groupRoles;
    }

    @Override
    public List<Policy> getPolicies() {
        LogPayload log = new LogPayload();
        try {
            return xpolicyRepository.getPolicies();
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Policy");
            log.setUsername("System");
            logger.log(Level.INFO, gson.toJson(log));
            return null;
        }
    }

    @Override
    public XPolicyPayload processCreatePolicy(XPolicyPayload requestPayload, String principal) {
        XPolicyPayload xpolicyPayload = new XPolicyPayload();
        LogPayload log = new LogPayload();
        String message = "";
        try {
            if (requestPayload.getId() == 0) {
                //Check if policy exist
                Policy policyByCode = xpolicyRepository.getPolicyUsingCode(requestPayload.getPolicyCode());
                if (policyByCode != null) {
                    message = messageSource.getMessage("appMessages.policy.exist", new Object[]{"Policy Code", requestPayload.getPolicyCode()}, Locale.ENGLISH);
                    log.setMessage(message);
                    log.setSeverity("INFO");
                    log.setSource("Create Policy");
                    log.setUsername(principal);
                    logger.log(Level.INFO, gson.toJson(log));
                    xpolicyPayload.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    xpolicyPayload.setResponseMessage(message);
                    return xpolicyPayload;
                }
                Policy policyByName = xpolicyRepository.getPolicyUsingName(requestPayload.getPolicyName());
                if (policyByName != null) {
                    message = messageSource.getMessage("appMessages.policy.exist", new Object[]{"Policy Name", requestPayload.getPolicyName()}, Locale.ENGLISH);
                    log.setMessage(message);
                    log.setSeverity("INFO");
                    log.setSource("Create Policy");
                    log.setUsername(principal);
                    logger.log(Level.INFO, gson.toJson(log));
                    xpolicyPayload.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    xpolicyPayload.setResponseMessage(message);
                    return xpolicyPayload;
                }

                //Check if file is uploaded
                if (requestPayload.getFileUpload().isEmpty()) {
                    message = messageSource.getMessage("appMessages.policy.file.notsupplied", new Object[0], Locale.ENGLISH);
                    log.setMessage(message);
                    log.setSeverity("INFO");
                    log.setSource("Create Policy");
                    log.setUsername(principal);
                    logger.log(Level.INFO, gson.toJson(log));
                    xpolicyPayload.setResponseCode(ResponseCodes.INPUT_MISSING.getResponseCode());
                    xpolicyPayload.setResponseMessage(message);
                    return xpolicyPayload;
                }

                //Check the file extension
                String extension = FilenameUtils.getExtension(requestPayload.getFileUpload().getOriginalFilename());
                if (!"pdf".equalsIgnoreCase(extension) && !"doc".equalsIgnoreCase(extension) && !"docx".equalsIgnoreCase(extension)
                        && !"xls".equalsIgnoreCase(extension) && !"xlsx".equalsIgnoreCase(extension)) {
                    message = messageSource.getMessage("appMessages.policy.file.notsupported", new Object[]{"PDF"}, Locale.ENGLISH);
                    log.setMessage(message);
                    log.setSeverity("INFO");
                    log.setSource("Create Policy");
                    log.setUsername(principal);
                    logger.log(Level.INFO, gson.toJson(log));
                    xpolicyPayload.setResponseCode(ResponseCodes.INPUT_MISSING.getResponseCode());
                    xpolicyPayload.setResponseMessage(message);
                    return xpolicyPayload;
                }

                //Create a directory and dump the file
                String documentId = UUID.randomUUID().toString().replace("-", "");
                String path = uploadBaseDir + "/" + documentId + "." + extension;
                File newFile = new File(path);
                FileCopyUtils.copy(requestPayload.getFileUpload().getBytes(), newFile);

                PolicyTemp newPolicy = new PolicyTemp();
                newPolicy.setAccessLevel(requestPayload.getAccessLevel());
                Company company = xpolicyRepository.getCompanyUsingId(Long.parseLong(requestPayload.getUpdateCompany()));
                newPolicy.setCompany(company);
                newPolicy.setCreatedAt(LocalDateTime.now());
                newPolicy.setCreatedBy(principal);
                Department department = xpolicyRepository.getDepartmentUsingId(Long.parseLong(requestPayload.getUpdateDepartment()));
                newPolicy.setDepartment(department);
                Division division = xpolicyRepository.getDivisionUsingId(Long.parseLong(requestPayload.getUpdateDivision()));
                newPolicy.setDivision(division);
                newPolicy.setExpiryDate(requestPayload.getExpiryDate().equalsIgnoreCase("") ? LocalDate.parse("2050-01-01") : LocalDate.parse(requestPayload.getExpiryDate()));
                File newFileRead = new File(path);
                long fileSize = newFileRead.length() / (1024 * 1024);
                newPolicy.setFileSize(String.valueOf(fileSize) + " MB");
                newPolicy.setLastReview(LocalDate.now());
                newPolicy.setMustRead(requestPayload.isMustRead());
                newPolicy.setPolicyAuthor(requestPayload.getPolicyAuthor());
                newPolicy.setPolicyCode(requestPayload.getPolicyCode());
                newPolicy.setPolicyDescription(requestPayload.getPolicyDescription());
                newPolicy.setPolicyDocumentId(documentId);
                newPolicy.setPolicyDocumentExt(extension);
                PolicyType policyType = xpolicyRepository.getPolicyTypeUsingCode(requestPayload.getPolicyType());
                newPolicy.setPolicyName(requestPayload.getPolicyName());
                newPolicy.setPolicyType(policyType);
                newPolicy.setUnderReview(requestPayload.isUnderReview());
                newPolicy.setActionType("NEW");
                xpolicyRepository.createPolicyTemp(newPolicy);

                //Create audit log
                AuditLog newAudit = new AuditLog();
                newAudit.setAuditAction("Create Policy " + requestPayload.getPolicyName());
                newAudit.setAuditCategory("Policy");
                newAudit.setAuditClass("Create");
                newAudit.setCreatedAt(LocalDateTime.now());
                newAudit.setNewValue(gson.toJson(newPolicy));
                newAudit.setOldValue("");
                newAudit.setRefNo(requestPayload.getPolicyCode());
                newAudit.setUsername(principal);
                xpolicyRepository.createAuditLog(newAudit);

                message = messageSource.getMessage("appMessages.success.policy", new Object[0], Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Create Policy");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);

                //Send email notification
                XPolicyPayload emailPayload = new XPolicyPayload();
                emailPayload.setRecipientEmail(emailNotification);
                emailPayload.setEmailBody("<h5>Dear Sir/Madam,</h5>\n"
                        + "<p>This is to bring to your notice that you have a pending Policy/SOP <b>" + requestPayload.getPolicyName() + "</b> awaiting your approval.</p> \n"
                        + "<p>Regards</p>\n"
                        + "<p>Nigerian Exchange Group</p>\n");
                emailPayload.setEmailSubject("X-Policy Upload");
                genericService.sendEmail(emailPayload, principal);

                return xpolicyPayload;
            }

            Policy policyRecord = xpolicyRepository.getPolicyUsingId(requestPayload.getId());
            if (policyRecord == null) {
                message = messageSource.getMessage("appMessages.policy.notexist", new Object[]{requestPayload.getId()}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Create Policy");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            Policy policyByCode = xpolicyRepository.getPolicyUsingCode(requestPayload.getPolicyCode());
            if (policyByCode != null && policyByCode != policyRecord) {
                message = messageSource.getMessage("appMessages.policy.exist", new Object[]{"Policy Code", requestPayload.getPolicyCode()}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Update Policy");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            Policy policyByName = xpolicyRepository.getPolicyUsingName(requestPayload.getPolicyName());
            if (policyByName != null && policyByName != policyRecord) {
                message = messageSource.getMessage("appMessages.policy.exist", new Object[]{"Policy Name", requestPayload.getPolicyName()}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Update Policy");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            //Check for pending policy transaction
            PolicyTemp pendingPolicy = xpolicyRepository.getPolicyTempUsingPolicy(policyRecord);
            if (pendingPolicy != null) {
                message = messageSource.getMessage("appMessages.policy.pending", new Object[0], Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Update Policy");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            //Check if file is uploaded
            File newFile = null;
            if (!requestPayload.getFileUpload().isEmpty()) {
                //Check the file extension
                String extension = FilenameUtils.getExtension(requestPayload.getFileUpload().getOriginalFilename());
                if (!"pdf".equalsIgnoreCase(extension)) {
                    message = messageSource.getMessage("appMessages.policy.file.notsupported", new Object[]{"PDF"}, Locale.ENGLISH);
                    log.setMessage(message);
                    log.setSeverity("INFO");
                    log.setSource("Update Policy");
                    log.setUsername(principal);
                    logger.log(Level.INFO, gson.toJson(log));
                    xpolicyPayload.setResponseCode(ResponseCodes.INPUT_MISSING.getResponseCode());
                    xpolicyPayload.setResponseMessage(message);
                    return xpolicyPayload;
                }

                //Create a directory and dump the file
                String path = uploadBaseDir + "/" + policyRecord.getPolicyDocumentId() + "." + extension;
                newFile = new File(path);
                FileCopyUtils.copy(requestPayload.getFileUpload().getBytes(), newFile);
                long fileSize = newFile.length() / (1024 * 1024);
                policyRecord.setFileSize(String.valueOf(fileSize) + " MB");

            }

            //Enter the record in the temp
            PolicyTemp policyTemp = new PolicyTemp();
            policyTemp.setAccessLevel(requestPayload.getAccessLevel());
            Company company = xpolicyRepository.getCompanyUsingId(Long.parseLong(requestPayload.getUpdateCompany()));
            policyTemp.setCompany(company);
            policyTemp.setCreatedBy(principal);
            policyTemp.setCreatedAt(LocalDateTime.now());
            Department department = xpolicyRepository.getDepartmentUsingId(Long.parseLong(requestPayload.getUpdateDepartment()));
            policyTemp.setDepartment(department);
            Division division = xpolicyRepository.getDivisionUsingId(Long.parseLong(requestPayload.getUpdateDivision()));
            policyTemp.setDivision(division);
            policyTemp.setExpiryDate(requestPayload.getExpiryDate().equalsIgnoreCase("") ? LocalDate.parse("2050-01-01") : LocalDate.parse(requestPayload.getExpiryDate()));
            policyTemp.setLastReview(LocalDate.now());
            policyTemp.setMustRead(requestPayload.isMustRead());
            policyTemp.setPolicyAuthor(requestPayload.getPolicyAuthor());
            policyTemp.setPolicyCode(requestPayload.getPolicyCode());
            policyTemp.setPolicyDescription(requestPayload.getPolicyDescription());
            PolicyType policyType = xpolicyRepository.getPolicyTypeUsingCode(requestPayload.getPolicyType());
            policyTemp.setPolicyName(requestPayload.getPolicyName());
            policyTemp.setPolicyDocumentId(policyRecord.getPolicyDocumentId());
            policyTemp.setPolicyDocumentExt(policyRecord.getPolicyDocumentExt());
            policyTemp.setPolicyType(policyType);
            policyTemp.setActionType("EDIT");
            policyTemp.setUnderReview(requestPayload.isUnderReview());
            policyTemp.setPolicy(policyRecord);
            xpolicyRepository.createPolicyTemp(policyTemp);

            //Create audit log
            AuditLog newAudit = new AuditLog();
            newAudit.setAuditAction("Update Policy " + policyRecord.getPolicyName());
            newAudit.setAuditCategory("Policy");
            newAudit.setAuditClass("Update");
            newAudit.setCreatedAt(LocalDateTime.now());
            newAudit.setNewValue(gson.toJson(policyTemp));
            newAudit.setOldValue(gson.toJson(policyRecord));
            newAudit.setRefNo("");
            newAudit.setUsername(principal);
            xpolicyRepository.createAuditLog(newAudit);

            message = messageSource.getMessage("appMessages.success.policy", new Object[0], Locale.ENGLISH);
            log.setMessage(message);
            log.setSeverity("INFO");
            log.setSource("Update Policy");
            log.setUsername(principal);
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            xpolicyPayload.setResponseMessage(message);
            return xpolicyPayload;
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Create Policy");
            log.setUsername(principal);
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            xpolicyPayload.setResponseMessage(ex.getMessage());
            return xpolicyPayload;
        }
    }

    @Override
    public XPolicyPayload processDeletePolicy(String id, String principal) {
        XPolicyPayload xpolicyPayload = new XPolicyPayload();
        LogPayload log = new LogPayload();
        try {
            //Fetch policy record
            String message = "";
            Policy policyRecord = xpolicyRepository.getPolicyUsingId(Long.parseLong(id));
            if (policyRecord == null) {
                message = messageSource.getMessage("appMessages.policy.notexist", new Object[]{id}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Delete Policy");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            //Check for pending policy transaction
            PolicyTemp pendingPolicy = xpolicyRepository.getPolicyTempUsingPolicy(policyRecord);
            if (pendingPolicy != null) {
                message = messageSource.getMessage("appMessages.policy.pending", new Object[0], Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Delete Policy");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            //Enter policy details in temp
            PolicyTemp policyTemp = new PolicyTemp();
            BeanUtils.copyProperties(policyRecord, policyTemp);
            policyTemp.setActionType("DELETE");
            policyTemp.setPolicy(policyRecord);
            policyTemp.setCreatedBy(principal);
            xpolicyRepository.createPolicyTemp(policyTemp);

            //Create audit log
            AuditLog newAudit = new AuditLog();
            newAudit.setAuditAction("Delete Policy " + policyRecord.getPolicyName());
            newAudit.setAuditCategory("Policy");
            newAudit.setAuditClass("Delete");
            newAudit.setCreatedAt(LocalDateTime.now());
            newAudit.setNewValue("");
            newAudit.setOldValue(gson.toJson(policyRecord));
            newAudit.setRefNo("");
            newAudit.setUsername(principal);
            xpolicyRepository.createAuditLog(newAudit);

            message = messageSource.getMessage("appMessages.delete.temp.policy", new Object[0], Locale.ENGLISH);
            log.setMessage(message);
            log.setSeverity("INFO");
            log.setSource("Delete Policy");
            log.setUsername(principal);
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            xpolicyPayload.setResponseMessage(message);
            return xpolicyPayload;
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            xpolicyPayload.setResponseMessage(ex.getMessage());
            return xpolicyPayload;
        }
    }

    @Override
    public List<AppUser> getUsers() {
        LogPayload log = new LogPayload();
        try {
            return xpolicyRepository.getUsers();
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("User");
            log.setUsername("System");
            logger.log(Level.INFO, gson.toJson(log));
            return null;
        }
    }

    @Override
    public XPolicyPayload processCreateUser(XPolicyPayload requestPayload, String principal) {
        XPolicyPayload xpolicyPayload = new XPolicyPayload();
        LogPayload log = new LogPayload();
        String message = "";
        try {
            if (requestPayload.getId() == 0) {
                //Fetch policy record;
                AppUser appUserRecord = xpolicyRepository.getAppUserUsingUsername(requestPayload.getEmail());
                if (appUserRecord != null) {
                    message = messageSource.getMessage("appMessages.user.exist", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH);
                    log.setMessage(message);
                    log.setSeverity("INFO");
                    log.setSource("Create User");
                    log.setUsername(principal);
                    logger.log(Level.INFO, gson.toJson(log));
                    xpolicyPayload.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    xpolicyPayload.setResponseMessage(message);
                    return xpolicyPayload;
                }

                //Check the email domain
                String domain = requestPayload.getEmail().split("@")[1];
                if (!domain.equalsIgnoreCase(emailDomain)) {
                    message = messageSource.getMessage("appMessages.user.invalid.domain", new Object[]{domain}, Locale.ENGLISH);
                    log.setMessage(message);
                    log.setSeverity("INFO");
                    log.setSource("Create User");
                    log.setUsername(principal);
                    logger.log(Level.INFO, gson.toJson(log));
                    xpolicyPayload.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                    xpolicyPayload.setResponseMessage(message);
                    return xpolicyPayload;
                }

                AppUser newUser = new AppUser();
                Company company = xpolicyRepository.getCompanyUsingId(Long.parseLong(requestPayload.getUpdateCompany()));
                Department department = xpolicyRepository.getDepartmentUsingId(Long.parseLong(requestPayload.getUpdateDepartment()));
                Division division = xpolicyRepository.getDivisionUsingId(Long.parseLong(requestPayload.getUpdateDivision()));
                newUser.setAccessLevel(requestPayload.getAccessLevel());
                newUser.setCompany(company);
                newUser.setCreatedAt(LocalDateTime.now());
                newUser.setCreatedBy(principal);
                newUser.setDepartment(department);
                newUser.setDivision(division);
                newUser.setEmail(requestPayload.getEmail());
                newUser.setEnabled(true);
                newUser.setExpired(false);
                newUser.setLocked(false);
                newUser.setLoginFailCount(0);
                newUser.setName(requestPayload.getFullName());
                newUser.setPolicyChampion(requestPayload.isPolicyChampion());
                newUser.setResetTime(LocalDateTime.now());
                RoleGroups role = xpolicyRepository.getRoleGroupUsingId(Long.valueOf(requestPayload.getRoleName()));
                newUser.setRole(role);
                newUser.setTwoFactorSecretKey(genericService.encryptString(genericService.generateTOTPSecretKey()));
                newUser.setUpdatedAt(LocalDateTime.now());
                newUser.setUpdatedBy(principal);
                newUser.setUserType(requestPayload.getUserType());
                String userId = UUID.randomUUID().toString().replace("-", "");
                newUser.setUserId(userId);
                xpolicyRepository.createAppUser(newUser);

                //Create audit log
                AuditLog newAudit = new AuditLog();
                newAudit.setAuditAction("Create User " + requestPayload.getEmail());
                newAudit.setAuditCategory("User");
                newAudit.setAuditClass("Create");
                newAudit.setCreatedAt(LocalDateTime.now());
                newAudit.setNewValue(gson.toJson(newUser));
                newAudit.setOldValue("");
                newAudit.setRefNo("");
                newAudit.setUsername(principal);
                xpolicyRepository.createAuditLog(newAudit);

                message = messageSource.getMessage("appMessages.success.user", new Object[0], Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Create User");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            //Get user record
            AppUser appUser = xpolicyRepository.getAppUserUsingId(requestPayload.getId());
            if (appUser == null) {
                message = messageSource.getMessage("appMessages.user.notexist", new Object[]{requestPayload.getId()}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Update User");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            String oldValues = gson.toJson(appUser);
            //Check the email domain
            String domain = requestPayload.getEmail().split("@")[1];
            if (!domain.equalsIgnoreCase(emailDomain)) {
                message = messageSource.getMessage("appMessages.user.invalid.domain", new Object[]{domain}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Update User");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            AppUser appUserByEmail = xpolicyRepository.getAppUserUsingUsername(requestPayload.getEmail());
            if (appUserByEmail != null && appUserByEmail != appUser) {
                message = messageSource.getMessage("appMessages.user.exist", new Object[]{requestPayload.getEmail()}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Update User");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            Company company = xpolicyRepository.getCompanyUsingId(Long.parseLong(requestPayload.getUpdateCompany()));
            Department department = xpolicyRepository.getDepartmentUsingId(Long.parseLong(requestPayload.getUpdateDepartment()));
            Division division = xpolicyRepository.getDivisionUsingId(Long.parseLong(requestPayload.getUpdateDivision()));
            appUser.setAccessLevel(requestPayload.getAccessLevel());
            appUser.setCompany(company);
            appUser.setDepartment(department);
            appUser.setDivision(division);
            appUser.setEmail(requestPayload.getEmail());
            appUser.setEnabled(requestPayload.isEnabled());
            appUser.setLoginFailCount(requestPayload.isFailedLogin() ? 0 : appUser.getLoginFailCount());
            appUser.setName(requestPayload.getFullName());
            appUser.setPolicyChampion(requestPayload.isPolicyChampion());
            RoleGroups role = xpolicyRepository.getRoleGroupUsingId(Long.parseLong(requestPayload.getRoleName()));
            appUser.setRole(role);
            appUser.setUserType(requestPayload.getUserType());
            String newValues = gson.toJson(appUser);
            xpolicyRepository.updateAppUser(appUser);

            //Create audit log
            AuditLog newAudit = new AuditLog();
            newAudit.setAuditAction("Update User " + requestPayload.getEmail());
            newAudit.setAuditCategory("User");
            newAudit.setAuditClass("Update");
            newAudit.setCreatedAt(LocalDateTime.now());
            newAudit.setNewValue(newValues);
            newAudit.setOldValue(oldValues);
            newAudit.setRefNo("");
            newAudit.setUsername(principal);
            xpolicyRepository.createAuditLog(newAudit);

            message = messageSource.getMessage("appMessages.success.user", new Object[0], Locale.ENGLISH);
            log.setMessage(message);
            log.setSeverity("INFO");
            log.setSource("Update User");
            log.setUsername(principal);
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            xpolicyPayload.setResponseMessage(message);
            return xpolicyPayload;
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Update User");
            log.setUsername(principal);
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            xpolicyPayload.setResponseMessage(ex.getMessage());
            return xpolicyPayload;
        }
    }

    @Override
    public XPolicyPayload processDeleteUser(String id, String principal) {
        XPolicyPayload xpolicyPayload = new XPolicyPayload();
        LogPayload log = new LogPayload();
        try {
            //Fetch policy record
            String message = "";
            AppUser appUserRecord = xpolicyRepository.getAppUserUsingId(Long.parseLong(id));
            if (appUserRecord == null) {
                message = messageSource.getMessage("appMessages.user.notexist", new Object[]{Long.valueOf(id)}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Delete User");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            //Delete policy 
            xpolicyRepository.deleteAppUser(appUserRecord);

            //Create audit log
            AuditLog newAudit = new AuditLog();
            newAudit.setAuditAction("Delete User " + appUserRecord.getEmail());
            newAudit.setAuditCategory("User");
            newAudit.setAuditClass("Delete");
            newAudit.setCreatedAt(LocalDateTime.now());
            newAudit.setNewValue("");
            newAudit.setOldValue(gson.toJson(appUserRecord));
            newAudit.setRefNo(id);
            newAudit.setUsername(principal);
            xpolicyRepository.createAuditLog(newAudit);

            message = messageSource.getMessage("appMessages.delete.user", new Object[0], Locale.ENGLISH);
            log.setMessage(message);
            log.setSeverity("INFO");
            log.setSource("Delete User");
            log.setUsername(principal);
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            xpolicyPayload.setResponseMessage(message);
            return xpolicyPayload;
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Delete User");
            log.setUsername(principal);
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            xpolicyPayload.setResponseMessage(ex.getMessage());
            return xpolicyPayload;
        }
    }

    @Override
    public List<AppUser> getAppUsers() {
        LogPayload log = new LogPayload();
        try {
            return xpolicyRepository.getUsers();
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Fetch User");
            log.setUsername("System");
            logger.log(Level.INFO, gson.toJson(log));
            return null;
        }
    }

    @Override
    public XPolicyPayload getAppUserUsingId(String id) {
        XPolicyPayload xpolicyPayload = new XPolicyPayload();
        LogPayload log = new LogPayload();
        try {
            AppUser appUser = xpolicyRepository.getAppUserUsingUserId(id);
            if (appUser == null) {
                String message = messageSource.getMessage("appMessages.user.notexist", new Object[]{id}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Fetch User");
                log.setUsername("System");
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            BeanUtils.copyProperties(appUser, xpolicyPayload);
            xpolicyPayload.setUpdateCompany(appUser.getCompany().getId().toString());
            xpolicyPayload.setUpdateDivision(appUser.getDivision().getId().toString());
            xpolicyPayload.setUpdateDepartment(appUser.getDepartment().getId().toString());
            xpolicyPayload.setFullName(appUser.getName());
            xpolicyPayload.setId(appUser.getId().intValue());
            xpolicyPayload.setRoleName(appUser.getRole().getId().toString());
            xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            xpolicyPayload.setResponseMessage("Success");
            return xpolicyPayload;
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Fetch User");
            log.setUsername("System");
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            xpolicyPayload.setResponseMessage(ex.getMessage());
            return xpolicyPayload;
        }
    }

    @Override
    public XPolicyPayload getPolicyUsingId(String id) {
        XPolicyPayload xpolicyPayload = new XPolicyPayload();
        LogPayload log = new LogPayload();
        try {
            Policy policy = xpolicyRepository.getPolicyUsingDocumentId(id);
            if (policy == null) {
                String message = messageSource.getMessage("appMessages.policy.notexist", new Object[]{id}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Fetch Policy");
                log.setUsername("System");
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            BeanUtils.copyProperties(policy, xpolicyPayload);
            xpolicyPayload.setUpdateCompany(policy.getCompany().getId().toString());
            xpolicyPayload.setUpdateDivision(policy.getDivision().getId().toString());
            xpolicyPayload.setUpdateDepartment(policy.getDepartment().getId().toString());
            xpolicyPayload.setPolicyType(policy.getPolicyType().getPolicyTypeCode());
            xpolicyPayload.setExpiryDate(policy.getExpiryDate() == null ? "" : policy.getExpiryDate().toString());
            xpolicyPayload.setId(policy.getId().intValue());
            xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            xpolicyPayload.setResponseMessage("Success");
            return xpolicyPayload;
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Fetch Policy");
            log.setUsername("System");
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            xpolicyPayload.setResponseMessage(ex.getMessage());
            return xpolicyPayload;
        }
    }

    @Override
    public XPolicyPayload processReviewPolicy(XPolicyPayload requestPayload, String principal) {
        XPolicyPayload xpolicyPayload = new XPolicyPayload();
        LogPayload log = new LogPayload();
        String message = "";
        try {
            AppUser appUser = xpolicyRepository.getAppUserUsingUsername(principal);
            if (appUser == null) {
                message = messageSource.getMessage("appMessages.user.notexist", new Object[]{principal}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Review Policy");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                //Log the error
                xpolicyPayload.setResponseCode(ResponseCodes.FAILED_LOGIN.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            Policy policyRecord = xpolicyRepository.getPolicyUsingId(Long.parseLong(requestPayload.getPolicyId()));
            if (policyRecord == null) {
                message = messageSource.getMessage("appMessages.policy.notexist", new Object[]{requestPayload.getId()}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Review Policy");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            PolicyReview newPolicyReview = new PolicyReview();
            newPolicyReview.setAppUser(appUser);
            newPolicyReview.setCreatedAt(LocalDateTime.now());
            newPolicyReview.setPolicy(policyRecord);
            newPolicyReview.setReviewComment(requestPayload.getReviewComment());
            newPolicyReview.setReviewedAt(requestPayload.getReviewedAt());
            newPolicyReview.setReviewedBy(requestPayload.getReviewedBy());
            xpolicyRepository.createPolicyReview(newPolicyReview);
            xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            xpolicyPayload.setResponseMessage("Success");
            return xpolicyPayload;
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Review Policy");
            log.setUsername(principal);
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            xpolicyPayload.setResponseMessage(ex.getMessage());
            return xpolicyPayload;
        }
    }

    @Override
    public List<PolicyReview> getPolicyAllReview() {
        LogPayload log = new LogPayload();
        try {
            return xpolicyRepository.getAllPolicyReview();
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Review Policy");
            log.setUsername("System");
            logger.log(Level.INFO, gson.toJson(log));
            return null;
        }
    }

    @Override
    public XPolicyPayload generateReport(XPolicyPayload requestPayload) {
        XPolicyPayload xpolicyPayload = new XPolicyPayload();
        List<Policy> policies = new ArrayList<>();
        List<AppUser> appUsers = new ArrayList<>();
        LogPayload log = new LogPayload();
        try {
            if (requestPayload.getReportCategory().equalsIgnoreCase("Policy")) {
                //Check if the policy type is selected
                if (requestPayload.getPolicyCode().equalsIgnoreCase("")) {
                    policies = xpolicyRepository.getPolicies();
                } else {
                    if (requestPayload.getPolicyCode().equalsIgnoreCase("EXPPOL")) {
                        //Check if request is for expired policies
                        policies = xpolicyRepository.getExpiredPolicies();
                    } else if (requestPayload.getPolicyCode().equalsIgnoreCase("POLLWAPP")) {
                        //This is for policies awaiting approval
                        List<PolicyTemp> tempPolicies = xpolicyRepository.getPendingPoliciesUpload();
                        BeanUtils.copyProperties(tempPolicies, policies);
                    } else {
                        PolicyType policyType = xpolicyRepository.getPolicyTypeUsingCode(requestPayload.getPolicyCode());
                        policies = xpolicyRepository.getPolicies(policyType);
                    }
                }

                //Check if the list is empty
                if (policies == null) {
                    xpolicyPayload.setPolicies(null);
                    xpolicyPayload.setReportPage("Policy");
                    xpolicyPayload.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[0], Locale.ENGLISH));
                    return xpolicyPayload;
                }

                //Filter the policy list
                if (!requestPayload.getCompany().equalsIgnoreCase("")) {
                    Company company = xpolicyRepository.getCompanyUsingId(Long.parseLong(requestPayload.getCompany()));
                    policies = policies.stream().filter(t -> t.getCompany() == company).collect(Collectors.toList());
                }

                if (!requestPayload.getDivision().equalsIgnoreCase("")) {
                    Division division = xpolicyRepository.getDivisionUsingId(Long.parseLong(requestPayload.getDivision()));
                    policies = policies.stream().filter(t -> t.getDivision() == division).collect(Collectors.toList());
                }

                if (!requestPayload.getDepartment().equalsIgnoreCase("")) {
                    Department department = xpolicyRepository.getDepartmentUsingId(Long.parseLong(requestPayload.getDepartment()));
                    policies = policies.stream().filter(t -> t.getDepartment() == department).collect(Collectors.toList());
                }

                xpolicyPayload.setPolicies(policies);
                xpolicyPayload.setReportPage("Policy");
                return xpolicyPayload;
            }

            if (requestPayload.getReportCategory().equalsIgnoreCase("PolicyRead")) {
                Policy policy = xpolicyRepository.getPolicyUsingId(Long.parseLong(requestPayload.getReportType()));
                List<PolicyRead> policyRead = xpolicyRepository.getPolicyReadUsingPolicy(policy);
                xpolicyPayload.setPolicyRead(policyRead);
                xpolicyPayload.setReportPage("PolicyRead");
                return xpolicyPayload;
            }

            //This is for other type of reports
            if (requestPayload.getReportCategory().equalsIgnoreCase("Users")) {
                if (requestPayload.getReportType().equalsIgnoreCase("ActiveUser")) {
                    appUsers = xpolicyRepository.getActiveUsers();
                }

                if (requestPayload.getReportType().equalsIgnoreCase("DisabledUser")) {
                    appUsers = xpolicyRepository.getDisabledUsers();
                }

                if (requestPayload.getReportType().equalsIgnoreCase("PolicyChampions")) {
                    appUsers = xpolicyRepository.getPolicyChampions();
                }

                if (appUsers == null) {
                    xpolicyPayload.setPolicies(null);
                    xpolicyPayload.setReportPage("Users");
                    xpolicyPayload.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[0], Locale.ENGLISH));
                    return xpolicyPayload;
                }

                //Filter the policy list
                if (!requestPayload.getUpdateCompany().equalsIgnoreCase("")) {
                    Company company = xpolicyRepository.getCompanyUsingId(Long.parseLong(requestPayload.getUpdateCompany()));
                    appUsers = appUsers.stream().filter(t -> t.getCompany() == company).collect(Collectors.toList());
                }

                if (!requestPayload.getUpdateDivision().equalsIgnoreCase("")) {
                    Division division = xpolicyRepository.getDivisionUsingId(Long.parseLong(requestPayload.getUpdateDivision()));
                    appUsers = appUsers.stream().filter(t -> t.getDivision() == division).collect(Collectors.toList());
                }

                if (!requestPayload.getUpdateDepartment().equalsIgnoreCase("")) {
                    Department department = xpolicyRepository.getDepartmentUsingId(Long.parseLong(requestPayload.getUpdateDepartment()));
                    appUsers = appUsers.stream().filter(t -> t.getDepartment() == department).collect(Collectors.toList());
                }

                xpolicyPayload.setAppUsers(appUsers);
                xpolicyPayload.setReportPage("User");
                return xpolicyPayload;
            }

            if (requestPayload.getReportCategory().equalsIgnoreCase("UserActivity")) {
                List<AuditLog> auditLog = xpolicyRepository.getAuditLogUsingDate(requestPayload.getStartDate(), requestPayload.getEndDate());
                if (auditLog == null) {
                    xpolicyPayload.setUserActivity(null);
                    xpolicyPayload.setReportPage("UserActivity");
                    xpolicyPayload.setResponseMessage(messageSource.getMessage("appMessages.norecord", new Object[0], Locale.ENGLISH));
                    return xpolicyPayload;
                }

                if (requestPayload.getReportType().equalsIgnoreCase("")) {
                    xpolicyPayload.setUserActivity(auditLog);
                    xpolicyPayload.setReportPage("UserActivity");
                    return xpolicyPayload;
                }

                if (requestPayload.getReportType().equalsIgnoreCase("Login")) {
                    auditLog = auditLog.stream().filter(t -> t.getAuditCategory().equalsIgnoreCase("Login")).collect(Collectors.toList());
                    xpolicyPayload.setUserActivity(auditLog);
                    xpolicyPayload.setReportPage("UserActivity");
                    return xpolicyPayload;
                }

                if (requestPayload.getReportType().equalsIgnoreCase("Policy")) {
                    auditLog = auditLog.stream().filter(t -> t.getAuditCategory().equalsIgnoreCase("Policy")).collect(Collectors.toList());
                    xpolicyPayload.setUserActivity(auditLog);
                    xpolicyPayload.setReportPage("UserActivity");
                    return xpolicyPayload;
                }

                if (requestPayload.getReportType().equalsIgnoreCase("User")) {
                    auditLog = auditLog.stream().filter(t -> t.getAuditCategory().equalsIgnoreCase("User")).collect(Collectors.toList());
                    xpolicyPayload.setUserActivity(auditLog);
                    xpolicyPayload.setReportPage("UserActivity");
                    return xpolicyPayload;
                }

                if (requestPayload.getReportType().equalsIgnoreCase("Roles")) {
                    auditLog = auditLog.stream().filter(t -> t.getAuditCategory().equalsIgnoreCase("Roles")).collect(Collectors.toList());
                    xpolicyPayload.setUserActivity(auditLog);
                    xpolicyPayload.setReportPage("UserActivity");
                    return xpolicyPayload;
                }
            }
            return xpolicyPayload;
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Report");
            log.setUsername("System");
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            xpolicyPayload.setResponseMessage(ex.getMessage());
            return xpolicyPayload;
        }
    }

    @Override
    public List<PolicyTemp> getPendingPolicies() {
        LogPayload log = new LogPayload();
        try {
            return xpolicyRepository.getPendingPolicies();
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Pending Policy");
            log.setUsername("System");
            logger.log(Level.INFO, gson.toJson(log));
            return null;
        }
    }

    @Override
    public XPolicyPayload processApprovePolicy(String id, String principal) {
        XPolicyPayload xpolicyPayload = new XPolicyPayload();
        String message = "";
        LogPayload log = new LogPayload();
        try {
            PolicyTemp pendingPolicy = xpolicyRepository.getPolicyTempUsingDocumentId(id);
            if (pendingPolicy == null) {
                message = messageSource.getMessage("appMessages.policy.notexist", new Object[]{id}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Approve Policy");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            //Check the type of transaction
            switch (pendingPolicy.getActionType()) {
                case "NEW": {
                    //Move the record to the policy table
                    Policy newPolicy = new Policy();
                    BeanUtils.copyProperties(pendingPolicy, newPolicy);
                    newPolicy.setId(null);
                    xpolicyRepository.createPolicy(newPolicy);

                    //Delete from the pending
                    xpolicyRepository.deletePolicyTemp(pendingPolicy);

                    //Create audit log
                    AuditLog newAudit = new AuditLog();
                    newAudit.setAuditAction("Approve New Policy " + pendingPolicy.getPolicyName());
                    newAudit.setAuditCategory("Policy");
                    newAudit.setAuditClass("Approval");
                    newAudit.setCreatedAt(LocalDateTime.now());
                    newAudit.setNewValue("");
                    newAudit.setOldValue("");
                    newAudit.setRefNo(id);
                    newAudit.setUsername(principal);
                    xpolicyRepository.createAuditLog(newAudit);

                    message = messageSource.getMessage("appMessages.success.approve", new Object[]{pendingPolicy.getPolicyName(), " New "}, Locale.ENGLISH);
                    log.setMessage(message);
                    log.setSeverity("INFO");
                    log.setSource("Approve Policy");
                    log.setUsername(principal);
                    logger.log(Level.INFO, gson.toJson(log));
                    xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                    xpolicyPayload.setResponseMessage(message);

                    //Send email notification
                    XPolicyPayload emailPayload = new XPolicyPayload();
                    emailPayload.setRecipientEmail(emailNotification);
                    emailPayload.setEmailBody("<h5>Dear Sir/Madam,</h5>\n"
                            + "<p>Trust your day is going well.</p> \n"
                            + "<p>This is to bring to your notice that the policy with name <b>" + pendingPolicy.getPolicyName() + "</b> has been approved and successfully uploaded on the X-Policy.</p> \n"
                            + "<p>Regards</p>\n"
                            + "<p>Nigerian Exchange Group</p>\n");
                    emailPayload.setEmailSubject("Policy Approved");
                    genericService.sendEmail(emailPayload, principal);
                    return xpolicyPayload;
                }
                case "EDIT": {
                    Policy policy = xpolicyRepository.getPolicyUsingId(pendingPolicy.getPolicy().getId());
                    if (policy != null) {
                        policy.setAccessLevel(pendingPolicy.getAccessLevel());
                        policy.setCompany(pendingPolicy.getCompany());
                        policy.setDepartment(pendingPolicy.getDepartment());
                        policy.setDivision(pendingPolicy.getDivision());
                        policy.setExpiryDate(pendingPolicy.getExpiryDate());
                        policy.setFileSize(pendingPolicy.getFileSize());
                        policy.setLastReview(pendingPolicy.getLastReview());
                        policy.setMustRead(pendingPolicy.isMustRead());
                        policy.setPolicyAuthor(pendingPolicy.getPolicyAuthor());
                        policy.setPolicyCode(pendingPolicy.getPolicyCode());
                        policy.setPolicyDescription(pendingPolicy.getPolicyDescription());
                        policy.setPolicyDocumentId(pendingPolicy.getPolicyDocumentId());
                        policy.setPolicyDocumentExt(pendingPolicy.getPolicyDocumentExt());
                        policy.setPolicyName(pendingPolicy.getPolicyName());
                        policy.setPolicyType(pendingPolicy.getPolicyType());
                        policy.setUnderReview(pendingPolicy.isUnderReview());
                        xpolicyRepository.updatePolicy(policy);

                        //Delete from the pending
                        xpolicyRepository.deletePolicyTemp(pendingPolicy);

                        //Create audit log
                        AuditLog newAudit = new AuditLog();
                        newAudit.setAuditAction("Approve Update Policy " + pendingPolicy.getPolicyName());
                        newAudit.setAuditCategory("Policy");
                        newAudit.setAuditClass("Approval");
                        newAudit.setCreatedAt(LocalDateTime.now());
                        newAudit.setNewValue("");
                        newAudit.setOldValue("");
                        newAudit.setRefNo(id);
                        newAudit.setUsername(principal);
                        xpolicyRepository.createAuditLog(newAudit);

                        message = messageSource.getMessage("appMessages.success.approve", new Object[]{pendingPolicy.getPolicyName(), " Edit "}, Locale.ENGLISH);
                        log.setMessage(message);
                        log.setSeverity("INFO");
                        log.setSource("Approve Policy");
                        log.setUsername(principal);
                        logger.log(Level.INFO, gson.toJson(log));
                        xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                        xpolicyPayload.setResponseMessage(message);

                        //Send email notification
                        XPolicyPayload emailPayload = new XPolicyPayload();
                        emailPayload.setRecipientEmail(emailNotification);
                        emailPayload.setEmailBody("<h5>Dear Sir/Madam,</h5>\n"
                                + "<p>Trust your day is going well.</p> \n"
                                + "<p>This is to bring to your notice that the policy with name <b>" + pendingPolicy.getPolicyName() + "</b> has been updated.</p> \n"
                                + "<p>Regards</p>\n"
                                + "<p>Nigerian Exchange Group</p>\n");
                        emailPayload.setEmailSubject("X-Policy Update");
                        genericService.sendEmail(emailPayload, principal);
                        return xpolicyPayload;
                    }
                }
                case "DELETE": {
                    Policy policy = xpolicyRepository.getPolicyUsingId(pendingPolicy.getPolicy().getId());
                    if (policy != null) {
                        //Delete from the pending
                        xpolicyRepository.deletePolicyTemp(pendingPolicy);

                        //Check Policy Read for the policy
                        List<PolicyRead> policyRead = xpolicyRepository.getPolicyReadUsingPolicy(policy);
                        if (policyRead != null) {
                            for (PolicyRead polRead : policyRead) {
                                xpolicyRepository.deletePolicyRead(polRead);
                            }
                        }

                        //Check Policy Review
                        List<PolicyReview> policyReview = xpolicyRepository.getPolicyReviewUsingPolicy(policy);
                        if (policyReview != null) {
                            for (PolicyReview polRev : policyReview) {
                                xpolicyRepository.deletePolicyReview(polRev);
                            }
                        }

                        //Delete from the Policy
                        xpolicyRepository.deletePolicy(policy);

                        //Delete the file associated with the policy
                        //Create a directory and dump the file
                        String path = uploadBaseDir + "/" + policy.getPolicyDocumentId() + "." + policy.getPolicyDocumentExt();
                        File file = new File(path);
                        file.delete();

                        //Create audit log
                        AuditLog newAudit = new AuditLog();
                        newAudit.setAuditAction("Approve Delete Policy " + pendingPolicy.getPolicyName());
                        newAudit.setAuditCategory("Policy");
                        newAudit.setAuditClass("Approval");
                        newAudit.setCreatedAt(LocalDateTime.now());
                        newAudit.setNewValue("");
                        newAudit.setOldValue("");
                        newAudit.setRefNo(id);
                        newAudit.setUsername(principal);
                        xpolicyRepository.createAuditLog(newAudit);

                        message = messageSource.getMessage("appMessages.success.approve", new Object[]{pendingPolicy.getPolicyName(), " Delete "}, Locale.ENGLISH);
                        log.setMessage(message);
                        log.setSeverity("INFO");
                        log.setSource("Approve Policy");
                        log.setUsername(principal);
                        logger.log(Level.INFO, gson.toJson(log));
                        xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                        xpolicyPayload.setResponseMessage(message);

                        //Send email notification
                        XPolicyPayload emailPayload = new XPolicyPayload();
                        emailPayload.setRecipientEmail(emailNotification);
                        emailPayload.setEmailBody("<h5>Dear Sir/Madam,</h5>\n"
                                + "<p>Trust your day is going well.</p> \n"
                                + "<p>This is to bring to your notice that the policy with name <b>" + pendingPolicy.getPolicyName() + "</b> has been deleted.</p> \n"
                                + "<p>Regards</p>\n"
                                + "<p>Nigerian Exchange Group</p>\n");
                        emailPayload.setEmailSubject("X-Policy Delete");
                        genericService.sendEmail(emailPayload, principal);
                        return xpolicyPayload;
                    }
                }
                default: {
                    message = messageSource.getMessage("appMessages.success.approve", new Object[0], Locale.ENGLISH);
                    log.setMessage(message);
                    log.setSeverity("INFO");
                    log.setSource("Approve Policy");
                    log.setUsername(principal);
                    logger.log(Level.INFO, gson.toJson(log));
                    xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                    xpolicyPayload.setResponseMessage(message);
                    return xpolicyPayload;
                }
            }
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Approve Policy");
            log.setUsername(principal);
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            xpolicyPayload.setResponseMessage(ex.getMessage());
            return xpolicyPayload;
        }
    }

    @Override
    public XPolicyPayload processDeclinePolicy(XPolicyPayload requestPayload, String principal) {
        XPolicyPayload xpolicyPayload = new XPolicyPayload();
        String message = "";
        LogPayload log = new LogPayload();
        try {
            PolicyTemp pendingPolicy = xpolicyRepository.getPolicyTempUsingDocumentId(requestPayload.getReference());
            if (pendingPolicy == null) {
                message = messageSource.getMessage("appMessages.policy.notexist", new Object[]{requestPayload.getReference()}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Decline Policy");
                log.setUsername(principal);
                log.setSource("Decline Policy");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            //Delete from the pending
            xpolicyRepository.deletePolicyTemp(pendingPolicy);

            //Create audit log
            AuditLog newAudit = new AuditLog();
            newAudit.setAuditAction("Decline Approving Policy " + pendingPolicy.getPolicyName());
            newAudit.setAuditCategory("Policy");
            newAudit.setAuditClass("Approval");
            newAudit.setCreatedAt(LocalDateTime.now());
            newAudit.setNewValue("");
            newAudit.setOldValue("");
            newAudit.setRefNo(requestPayload.getReference());
            newAudit.setUsername(principal);
            xpolicyRepository.createAuditLog(newAudit);

            message = messageSource.getMessage("appMessages.success.decline", new Object[]{pendingPolicy.getPolicyName()}, Locale.ENGLISH);
            log.setMessage(message);
            log.setSeverity("INFO");
            log.setSource("Decline Policy");
            log.setUsername(principal);
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            xpolicyPayload.setResponseMessage(message);

            //Send email notification
            XPolicyPayload emailPayload = new XPolicyPayload();
            emailPayload.setRecipientEmail(pendingPolicy.getCreatedBy());
            emailPayload.setEmailBody("<h5>Dear Sir/Madam,</h5>\n"
                    + "<p>Trust your day is going well.</p>\n"
                    + "<p>This is to bring to your notice that the approval of policy with name <b>" + pendingPolicy.getPolicyName() + "</b> has been declined. </p>\n"
                    + "<p>This is due to <b>" + requestPayload.getComment() + "</b>. Please reload after addressing the issue raised.</p>\n"
                    + "<p>Regards</p>\n"
                    + "<p>Nigerian Exchange Group</p>\n");
            emailPayload.setEmailSubject("X-Policy Decline");
            genericService.sendEmail(emailPayload, principal);
            return xpolicyPayload;
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Decline Policy");
            log.setUsername(principal);
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            xpolicyPayload.setResponseMessage(ex.getMessage());
            return xpolicyPayload;
        }
    }

    @Override
    public XPolicyPayload generateTwoFactorDetails() {
        XPolicyPayload xpolicyPayload = new XPolicyPayload();
        String message = "";
        LogPayload log = new LogPayload();
        int count = 0;
        List<AppUser> appUserList = xpolicyRepository.getAppUsersForTFAFix();
        if (appUserList != null) {
            for (AppUser a : appUserList) {
                a.setTwoFactorSecretKey(genericService.encryptString(genericService.generateTOTPSecretKey()));
                String userId = UUID.randomUUID().toString().replace("-", "");
                a.setUserId(userId);
                xpolicyRepository.updateAppUser(a);
                count++;
            }
        }

        message = "Total Count = " + count;
        log.setMessage(message);
        log.setSeverity("INFO");
        log.setSource("Generate Two Factor Code");
        log.setUsername("System");
        logger.log(Level.INFO, gson.toJson(log));
        xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
        xpolicyPayload.setResponseMessage(message);
        return xpolicyPayload;
    }

    @Override
    public List<RoleGroups> getRoleGroupList() {
        XPolicyPayload xpolicyPayload = new XPolicyPayload();
        String message = "";
        LogPayload log = new LogPayload();
        try {
            return xpolicyRepository.getRoleGroupList();
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Role Group");
            log.setUsername("");
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            xpolicyPayload.setResponseMessage(ex.getMessage());
            return null;
        }
    }

    @Override
    public XPolicyPayload getRoleGroupUsingId(String id) {
        XPolicyPayload xpolicyPayload = new XPolicyPayload();
        LogPayload log = new LogPayload();
        try {
            RoleGroups roleGroup = xpolicyRepository.getRoleGroupUsingId(Long.valueOf(id));
            if (roleGroup == null) {
                String message = messageSource.getMessage("appMessages.roles.notexist", new Object[]{id}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Fetch Roles");
                log.setUsername("System");
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            BeanUtils.copyProperties(roleGroup, xpolicyPayload);
            xpolicyPayload.setId(roleGroup.getId().intValue());
            xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            xpolicyPayload.setResponseMessage("Success");
            return xpolicyPayload;
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Fetch Roles");
            log.setUsername("System");
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            xpolicyPayload.setResponseMessage(ex.getMessage());
            return xpolicyPayload;
        }
    }

    @Override
    public XPolicyPayload processDeleteRoleGroup(String id, String principal) {
        XPolicyPayload xpolicyPayload = new XPolicyPayload();
        LogPayload log = new LogPayload();
        try {
            //Fetch policy record
            String message = "";
            RoleGroups roleRecord = xpolicyRepository.getRoleGroupUsingId(Long.parseLong(id));
            if (roleRecord == null) {
                message = messageSource.getMessage("appMessages.roles.notexist", new Object[]{Long.valueOf(id)}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Delete Roles");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            //Delete policy 
            xpolicyRepository.deleteRoleGroups(roleRecord);

            //Create audit log
            AuditLog newAudit = new AuditLog();
            newAudit.setAuditAction("Delete Role " + roleRecord.getGroupName());
            newAudit.setAuditCategory("User");
            newAudit.setAuditClass("Delete");
            newAudit.setCreatedAt(LocalDateTime.now());
            newAudit.setNewValue("");
            newAudit.setOldValue(gson.toJson(roleRecord));
            newAudit.setRefNo(id);
            newAudit.setUsername(principal);
            xpolicyRepository.createAuditLog(newAudit);

            message = messageSource.getMessage("appMessages.delete.roles", new Object[0], Locale.ENGLISH);
            log.setMessage(message);
            log.setSeverity("INFO");
            log.setSource("Delete Roles");
            log.setUsername(principal);
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            xpolicyPayload.setResponseMessage(message);
            return xpolicyPayload;
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Delete Roles");
            log.setUsername(principal);
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            xpolicyPayload.setResponseMessage(ex.getMessage());
            return xpolicyPayload;
        }
    }

    @Override
    public XPolicyPayload processCreateRoleGroup(XPolicyPayload requestPayload, String principal) {
        XPolicyPayload xpolicyPayload = new XPolicyPayload();
        LogPayload log = new LogPayload();
        String message = "";
        try {
            if (requestPayload.getId() == 0) {
                //Fetch policy record;
                RoleGroups roleRecord = xpolicyRepository.getRoleGroupUsingGroupName(requestPayload.getGroupName());
                if (roleRecord != null) {
                    message = messageSource.getMessage("appMessages.roles.exist", new Object[]{" name ", requestPayload.getGroupName()}, Locale.ENGLISH);
                    log.setMessage(message);
                    log.setSeverity("INFO");
                    log.setSource("Create Roles");
                    log.setUsername(principal);
                    logger.log(Level.INFO, gson.toJson(log));
                    xpolicyPayload.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    xpolicyPayload.setResponseMessage(message);
                    return xpolicyPayload;
                }

                RoleGroups newRole = new RoleGroups();
                newRole.setGroupName(requestPayload.getGroupName());
                newRole.setCreatedAt(LocalDateTime.now());
                xpolicyRepository.createRoleGroup(newRole);

                //Create audit log
                AuditLog newAudit = new AuditLog();
                newAudit.setAuditAction("Create Role " + requestPayload.getGroupName());
                newAudit.setAuditCategory("Roles");
                newAudit.setAuditClass("Create");
                newAudit.setCreatedAt(LocalDateTime.now());
                newAudit.setNewValue(gson.toJson(newRole));
                newAudit.setOldValue("");
                newAudit.setRefNo("");
                newAudit.setUsername(principal);
                xpolicyRepository.createAuditLog(newAudit);

                message = messageSource.getMessage("appMessages.success.roles", new Object[0], Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Create Roles");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            //Get user record
            RoleGroups roleGroup = xpolicyRepository.getRoleGroupUsingId(requestPayload.getId());
            if (roleGroup == null) {
                message = messageSource.getMessage("appMessages.roles.notexist", new Object[]{requestPayload.getId()}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Update Roles");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            String oldValues = gson.toJson(roleGroup);

            RoleGroups roleGroupByName = xpolicyRepository.getRoleGroupUsingGroupName(requestPayload.getGroupName());
            if (roleGroupByName != null && roleGroupByName != roleGroup) {
                message = messageSource.getMessage("appMessages.roles.exist", new Object[]{requestPayload.getGroupName()}, Locale.ENGLISH);
                log.setMessage(message);
                log.setSeverity("INFO");
                log.setSource("Update Roles");
                log.setUsername(principal);
                logger.log(Level.INFO, gson.toJson(log));
                xpolicyPayload.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                xpolicyPayload.setResponseMessage(message);
                return xpolicyPayload;
            }

            roleGroup.setGroupName(requestPayload.getGroupName());
            String newValues = gson.toJson(roleGroup);
            xpolicyRepository.updateRoleGroup(roleGroup);

            //Create audit log
            AuditLog newAudit = new AuditLog();
            newAudit.setAuditAction("Update Roles " + requestPayload.getGroupName());
            newAudit.setAuditCategory("Roles");
            newAudit.setAuditClass("Update");
            newAudit.setCreatedAt(LocalDateTime.now());
            newAudit.setNewValue(newValues);
            newAudit.setOldValue(oldValues);
            newAudit.setRefNo("");
            newAudit.setUsername(principal);
            xpolicyRepository.createAuditLog(newAudit);

            message = messageSource.getMessage("appMessages.success.roles", new Object[0], Locale.ENGLISH);
            log.setMessage(message);
            log.setSeverity("INFO");
            log.setSource("Update Roles");
            log.setUsername(principal);
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            xpolicyPayload.setResponseMessage(message);
            return xpolicyPayload;
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("INFO");
            log.setSource("Update User");
            log.setUsername(principal);
            logger.log(Level.INFO, gson.toJson(log));
            xpolicyPayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            xpolicyPayload.setResponseMessage(ex.getMessage());
            return xpolicyPayload;
        }
    }

    @Override
    public Object fetchGroupRecord(String roleName) {
        List<XPolicyPayload> roleList = new ArrayList<>();
        List<String> userRoles = new ArrayList<>();
        List<String> userNoRoles = new ArrayList<>();

        //Fecth the Group details
        RoleGroups roleGroup = xpolicyRepository.getRoleGroupUsingGroupName(roleName);
        if (roleGroup == null) {
            return messageSource.getMessage("appMessages.roles.notexist", new Object[]{roleName}, Locale.ENGLISH);
        }

        List<GroupRoles> groupRoles = xpolicyRepository.getGroupRolesUsingRoleGroup(roleGroup);
        if (groupRoles != null) {
            for (GroupRoles r : groupRoles) {
                userRoles.add(r.getAppRole().getRoleName());
            }
        }
        //Get all the app roles 
        List<AppRoles> allRoles = xpolicyRepository.getAppRoles();
        if (allRoles != null) {
            for (AppRoles r : allRoles) {
                if (!userRoles.contains(r.getRoleName())) {
                    userNoRoles.add(r.getRoleName());
                }
            }
        }

        for (String r : userRoles) {
            XPolicyPayload newRole = new XPolicyPayload();
            newRole.setRoleName(r);
            newRole.setRoleExist("true");
            roleList.add(newRole);
        }

        for (String r : userNoRoles) {
            XPolicyPayload newRole = new XPolicyPayload();
            newRole.setRoleName(r);
            newRole.setRoleExist("false");
            roleList.add(newRole);
        }

        return roleList;
    }

    @Override
    public String updateGroupRoles(XPolicyPayload requestPayload, String principal) {
        LogPayload log = new LogPayload();
        log.setUsername(principal);
        log.setSource("Group Role");
        try {
            String[] roles = requestPayload.getRolesToUpdate().split(",");
            if (roles.length > 0) {
                RoleGroups roleGroup = xpolicyRepository.getRoleGroupUsingGroupName(requestPayload.getGroupName());
                List<GroupRoles> currentRoles = xpolicyRepository.getGroupRolesUsingRoleGroup(roleGroup);
                //Delete the old roles
                if (currentRoles != null) {
                    for (GroupRoles rol : currentRoles) {
                        xpolicyRepository.deteleGroupRoles(rol);
                    }
                }
                //Add the new roles
                for (String rol : roles) {
                    AppRoles appRole = xpolicyRepository.getRoleUsingRoleName(rol);
                    GroupRoles newRole = new GroupRoles();
                    newRole.setCreatedAt(LocalDateTime.now());
                    newRole.setAppRole(appRole);
                    newRole.setRoleGroup(roleGroup);
                    xpolicyRepository.createGroupRoles(newRole);
                }
            }

            //Create audit log
            AuditLog newAuditLog = new AuditLog();
            newAuditLog.setAuditAction("Update Group Roles with role name  " + requestPayload.getRoleGroup());
            newAuditLog.setAuditCategory("Roles");
            newAuditLog.setAuditClass("Update");
            newAuditLog.setCreatedAt(LocalDateTime.now());
            newAuditLog.setOldValue("");
            newAuditLog.setNewValue("");
            newAuditLog.setRefNo(requestPayload.getGroupName());
            newAuditLog.setUsername(principal);
            xpolicyRepository.createAuditLog(newAuditLog);

            String message = messageSource.getMessage("appMessages.success.roles", new Object[0], Locale.ENGLISH);
            return message;
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("SEVERE");
            logger.log(Level.SEVERE, gson.toJson(log));
            return ex.getMessage();
        }
    }

}
