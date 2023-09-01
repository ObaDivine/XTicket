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
public class GoogleAuthenticatorPayload {

    @NotBlank(message = "Email is required")
    @Pattern(regexp = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$", message = "Valid email required")
    private String email;
    @NotBlank(message = "Secret key is required")
    private String secretKey;
    @NotBlank(message = "Hash value is required")
    private String hash;
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
}
