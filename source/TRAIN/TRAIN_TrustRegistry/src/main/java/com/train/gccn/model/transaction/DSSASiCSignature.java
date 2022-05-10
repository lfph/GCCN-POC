package com.train.gccn.model.transaction;

import eu.europa.esig.dss.validation.AdvancedSignature;

import java.security.cert.X509Certificate;

public class DSSASiCSignature implements ASiCSignature {
    
    
    private final AdvancedSignature signature;
    
    public DSSASiCSignature(AdvancedSignature signature) {
        this.signature = signature;
    }
    
    
    @Override
    public X509Certificate getSigningX509Certificate() {
        return this.signature.getSigningCertificateToken().getCertificate();
    }
    
    public String getSignerDSSId() {
        return this.signature.getSigningCertificateToken().getDSSIdAsString();
    }
}
