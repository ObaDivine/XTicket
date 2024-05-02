package com.ngxgroup.xticket.service;

import com.ngxgroup.xticket.payload.XTicketPayload;
import java.util.List;

/**
 *
 * @author bokon
 */
public interface AdminService {

    String authenticateUser(XTicketPayload requestPayload);
    
    List<String> fetchRoleGroup();
}
