package com.ngxgroup.xpolicy.service;

import com.ngxgroup.xpolicy.model.Policy;
import com.ngxgroup.xpolicy.payload.XPolicyPayload;
import com.ngxgroup.xpolicy.repository.XPolicyRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 *
 * @author bokon
 */
@Service
public class CrobJob {

    @Autowired
    GenericService genericService;
    @Autowired
    XPolicyRepository xpolicyRepository;
    @Value("${xpolicy.expiry.days}")
    private int daysBeforeExpiry;
    @Value("${xpolicy.email.notification}")
    private String emailNotification;

    @Scheduled(cron = "${xpolicy.cron.job}")
    private void setExpiryPolicies() {
        List<Policy> policyList = xpolicyRepository.getPolicyExpiringToday(daysBeforeExpiry);
        if (policyList != null) {
            for (Policy pol : policyList) {
                //Send email notification
                XPolicyPayload emailPayload = new XPolicyPayload();
                emailPayload.setRecipientEmail(emailNotification);
                emailPayload.setEmailBody("<h5>Dear Sir/Madam,</h5>\n"
                        + "<p>This is to notify you that the Policy " + pol.getPolicyName() + " will expire on " + pol.getExpiryDate() + "</p> \n"
                        + "<p>Kindly initiate the process of renewal.</p>\n"
                        + "<p>Please click <a href=\"https://web.ngxgroup.com/xpolicy/login" + "\">here to login to the X-Policy Application</a></p>\n"
                        + "<p>Regards</p>\n"
                        + "<p>Nigerian Exchange Group</p>\n");
                emailPayload.setEmailSubject("Expiring Policy Notification");
                genericService.sendEmail(emailPayload, "System");

                //Check if the date is a match
                if (LocalDate.now().isEqual(pol.getExpiryDate())) {
                    pol.setExpired(true);
                    xpolicyRepository.updatePolicy(pol);
                }
            }
        }
    }
}
