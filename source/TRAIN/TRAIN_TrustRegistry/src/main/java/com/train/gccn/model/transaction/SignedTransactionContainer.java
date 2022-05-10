package com.train.gccn.model.transaction;

import java.security.cert.X509Certificate;
import java.util.List;

public interface SignedTransactionContainer {
    //public abstract boolean validateTransaction();
    
    //    public abstract List<Signature> getSignatures();
    //
    //    public abstract Signature getSignature();
    
    public boolean verifySignature(X509Certificate cert, ASiCSignature sig);
    
    public abstract X509Certificate getSigningCertificate();
    
    public List<ASiCSignature> getSignatures();
}
