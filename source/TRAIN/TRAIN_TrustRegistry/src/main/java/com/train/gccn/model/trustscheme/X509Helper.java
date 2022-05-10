package com.train.gccn.model.trustscheme;

import iaik.x509.X509ExtensionInitException;
import iaik.x509.extensions.AuthorityKeyIdentifier;
import iaik.x509.extensions.SubjectKeyIdentifier;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class X509Helper {
    
    public static AuthorityKeyIdentifier genAuthorityKeyIdentifier(X509Certificate cert) {
        iaik.x509.X509Certificate iaikCert = null;
        
        try {
            iaikCert = new iaik.x509.X509Certificate(cert.getEncoded());
            return (AuthorityKeyIdentifier) iaikCert.getExtension(AuthorityKeyIdentifier.oid);
            
        } catch(CertificateException | X509ExtensionInitException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static SubjectKeyIdentifier genSubjectKeyIdentifier(X509Certificate cert) {
        iaik.x509.X509Certificate iaikCert = null;
        
        try {
            iaikCert = new iaik.x509.X509Certificate(cert.getEncoded());
            return (SubjectKeyIdentifier) iaikCert.getExtension(SubjectKeyIdentifier.oid);
            
        } catch(CertificateException | X509ExtensionInitException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static boolean equals(X509Certificate cert1, X509Certificate cert2) {
        SubjectKeyIdentifier subjectKeyIdentifier1 = X509Helper.genSubjectKeyIdentifier(cert1);
        SubjectKeyIdentifier subjectKeyIdentifier2 = X509Helper.genSubjectKeyIdentifier(cert2);
        return subjectKeyIdentifier1 != null &&
                subjectKeyIdentifier2 != null &&
                Arrays.equals(subjectKeyIdentifier1.get(), subjectKeyIdentifier2.get());
    }
    
    public static boolean equals(AuthorityKeyIdentifier authorityKeyIdentifier, X509Certificate subjectCert) {
        SubjectKeyIdentifier subjectKeyIdentifier = X509Helper.genSubjectKeyIdentifier(subjectCert);
        return X509Helper.equals(authorityKeyIdentifier, subjectKeyIdentifier);
    }
    
    public static boolean equals(AuthorityKeyIdentifier authorityKeyIdentifier, SubjectKeyIdentifier subjectKeyIdentifier) {
        if(authorityKeyIdentifier == null || subjectKeyIdentifier == null) {
            return false;
        }
        
        return Arrays.equals(authorityKeyIdentifier.getKeyIdentifier(), subjectKeyIdentifier.get());
    }
}
