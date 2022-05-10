package com.train.gccn.model.trustscheme;

import java.security.cert.X509Certificate;

public interface TslEntry {
    
    public X509Certificate getCertificate();
    
    String getField(String field);
    
    boolean fieldExists(String field);
    
    String getServiceName();
    
    String getSchemeId();
}
