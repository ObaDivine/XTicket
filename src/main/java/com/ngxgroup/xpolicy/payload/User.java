package com.ngxgroup.xpolicy.payload;

import javax.naming.directory.Attributes;
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
public class User {

    private String distinguishedName;
    private String userPrincipal;
    private String commonName;

    public User(Attributes attr) throws javax.naming.NamingException {
        userPrincipal = (String) attr.get("userPrincipalName").get();
        commonName = (String) attr.get("cn").get();
        distinguishedName = (String) attr.get("distinguishedName").get();
    }

}
