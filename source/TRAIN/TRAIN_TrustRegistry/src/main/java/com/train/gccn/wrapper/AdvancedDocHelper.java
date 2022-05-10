package com.train.gccn.wrapper;

import iaik.x509.X509Certificate;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public abstract class AdvancedDocHelper {
    
    private static Logger logger = Logger.getLogger(AdvancedDocHelper.class);
    private Map<X509Certificate, Boolean> verificationResults;
    
    public AdvancedDocHelper() {
        this.verificationResults = new HashMap<>();
    }
    
    public boolean verify(X509Certificate forCert) {
        //SubjectKeyIdentifier subjectKeyIdentifier = X509Helper.genSubjectKeyIdentifier(forCert);
        if(this.verificationResults.containsKey(forCert)) {
            return this.verificationResults.get(forCert);
        }
    
        AdvancedDocHelper.logger.error("No verification result for cert " + forCert.getIssuerDN());
        return false;
    }
    
    protected void setVerificationResult(X509Certificate certificate, boolean result) {
        //SubjectKeyIdentifier subjectKeyIdentifier = X509Helper.genSubjectKeyIdentifier(certificate);
        this.verificationResults.put(certificate, result);
    }
    
    public abstract boolean verify();
    
    public abstract boolean verify(boolean skipSignatureValidation);
    
    public abstract X509Certificate getCertificate();
}
