package com.ngxgroup.xticket.payload;

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

    String severity;
    String username;
    String source;
    String message;
}
