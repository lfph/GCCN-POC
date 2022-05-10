package com.train.gccn.model.transaction;

import java.security.cert.X509Certificate;

public interface ASiCSignature {
    
    public X509Certificate getSigningX509Certificate();
    
}
