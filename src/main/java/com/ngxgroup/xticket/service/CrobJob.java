package com.ngxgroup.xticket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.ngxgroup.xticket.repository.XTicketRepository;

/**
 *
 * @author bokon
 */
@Service
public class CrobJob {

    @Autowired
    XTicketRepository xpolicyRepository;

    @Scheduled(cron = "${xticket.cron.job}")
    private void setExpiryPolicies() {

    }
}
