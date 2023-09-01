package com.ngxgroup.xpolicy.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author bokon
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginPayload {

    private String email;
    private String password;
    private String totp;
    private String otp1;
    private String otp2;
    private String otp3;
    private String otp4;
    private String otp5;
    private String otp6;
}
