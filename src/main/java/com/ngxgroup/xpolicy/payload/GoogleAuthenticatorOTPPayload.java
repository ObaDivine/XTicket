package com.ngxgroup.xpolicy.payload;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author briano
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GoogleAuthenticatorOTPPayload {

    @NotBlank(message = "Email is required")
    @Pattern(regexp = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$", message = "Valid email required")
    private String email;
    @NotBlank(message = "OTP 1 cannot be blank")
    private String otp1;
    @NotBlank(message = "OTP 2 cannot be blank")
    private String otp2;
    @NotBlank(message = "OTP 3 cannot be blank")
    private String otp3;
    @NotBlank(message = "OTP 4 cannot be blank")
    private String otp4;
    @NotBlank(message = "OTP 5 cannot be blank")
    private String otp5;
    @NotBlank(message = "OTP 6 cannot be blank")
    private String otp6;
    @NotBlank(message = "Hash value is required")
    private String hash;
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
}
