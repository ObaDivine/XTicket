package com.ngxgroup.xpolicy.payload;

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
public class LogPayload {

    private String severity;
    private String username;
    private String source;
    private String message;
}
