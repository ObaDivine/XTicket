package com.ngxgroup.xpolicy.service;

import com.ngxgroup.xpolicy.payload.XPolicyPayload;
import java.util.List;

/**
 *
 * @author bokon
 */
public interface AdminService {

    String authenticateUser(XPolicyPayload requestPayload);
    
    List<String> fetchRoleGroup();
}
