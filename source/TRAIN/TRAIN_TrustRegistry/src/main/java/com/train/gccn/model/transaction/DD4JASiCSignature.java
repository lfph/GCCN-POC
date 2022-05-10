package com.train.gccn.model.transaction;

import org.digidoc4j.Signature;
import org.digidoc4j.X509Cert;

import java.security.cert.X509Certificate;

public class DD4JASiCSignature implements ASiCSignature {
    
    private Signature signature;
    
    public DD4JASiCSignature(Signature signature) {
        this.signature = signature;
    }
    
    @Override
    public X509Certificate getSigningX509Certificate() {
        return this.getSigningCertificate().getX509Certificate();
    }
    
    public X509Cert getSigningCertificate() {
        return this.signature.getSigningCertificate();
    }
    
    public Signature get() {
        return this.signature;
    }
}
