package com.train.gccn.model.transaction;

import org.apache.log4j.Logger;
import org.digidoc4j.*;
import org.digidoc4j.exceptions.DigiDoc4JException;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class DD4JASiCTransaction extends TransactionContainer {
    
    private static Logger logger = Logger.getLogger(DD4JASiCTransaction.class);
    
    private String transactionType = "ASiC-XAdES";
    private Container container;
    
    //private String trustList;
    //private Configuration configuration;
    
    public DD4JASiCTransaction(Container container) {
        this.container = container;
        this.transactionType = container.getType();
    }
    
    @Override
    public boolean verifySignature(X509Certificate cert, ASiCSignature sig) {
        Signature signatureToVerify = ((DD4JASiCSignature) sig).get();
    
        boolean signedUsingGivenCert = signatureToVerify.getSigningCertificate().getX509Certificate().equals(cert);
        if(!signedUsingGivenCert) {
            DD4JASiCTransaction.logger.error("Signature was issued by '" + signatureToVerify.getSigningCertificate().getX509Certificate().getSubjectDN() + "',");
            DD4JASiCTransaction.logger.error(" but you asked for      '" + cert.getSubjectDN().getName() + "'.");
        }
    
        ValidationResult result = signatureToVerify.validateSignature();
        
        if(result.hasWarnings()) {
            for(DigiDoc4JException warning : result.getWarnings()) {
                DD4JASiCTransaction.logger.warn(warning.toString());
            }
        }
        
        if(result != null && result.isValid()) {
            return true;
        } else {
            boolean statusAfterIgnorelist = true;
            for(DigiDoc4JException error : result.getErrors()) {
                if(TransactionContainer.errorIgnored(error.getMessage())) {
                    DD4JASiCTransaction.logger.error("[IGNORED] " + error.toString());
                } else {
                    DD4JASiCTransaction.logger.error(error.toString());
                    statusAfterIgnorelist = false;
                }
            }
            DD4JASiCTransaction.logger.info("statusAfterIgnorelist: " + statusAfterIgnorelist);
    
            return statusAfterIgnorelist && signedUsingGivenCert;
        }
    }

//    @Override
//    public boolean validateTransaction() {
//
//        Boolean answer = false;
//
//        Configuration.getInstance().setTslLocation(this.trustList); // default Configuration
//        this.container.getConfiguration().setTslLocation(this.trustList); // container Configuration
//
//        if(this.transactionType == "ASICS" || this.transactionType == "ASICE") {
//            ValidationResult result = this.container.validate();
//            // This will only get the first signature
//            this.signature = this.getSignature();
//
//            if(result.hasWarnings()) {
//                DD4JASiCTransaction.logger.error("Container Validation Warnings:");
//                for(DigiDoc4JException error : result.getWarnings()) {
//                    DD4JASiCTransaction.logger.error("  * " + error.getMessage());
//                }
//            }
//
//            if(result.isValid()) {
//                answer = true;
//            } else {
//                answer = true;
//                DD4JASiCTransaction.logger.error("Container Validation Errors:");
//
//                for(DigiDoc4JException error : result.getErrors()) {
//                    if(DD4JASiCTransaction.errorIgnored(error)) {
//                        DD4JASiCTransaction.logger.error("[IGNORED] " + error.toString());
//                    } else {
//                        DD4JASiCTransaction.logger.error(error.toString());
//                        answer = false;
//                    }
//                }
//            }
//        } else {
//            DD4JASiCTransaction.logger.error("Container Validation Error: Container type is not ASICS/ASICE");
//        }
//
//        return answer;
//    }

//    @Override
//    public void setTrustList(String trustList) {
//        DD4JASiCTransaction.logger.info("Setting trust list to " + trustList);
//        this.trustList = trustList;
//    }
    
    //Get transaction type
    @Override
    public String getTransactionType() {
        return this.transactionType;
    }
    
    @Override
    public List<ASiCSignature> getSignatures() {
        List<ASiCSignature> encapsulatedSigs = new ArrayList<>();
        
        for(Signature containerSignature : this.container.getSignatures()) {
            encapsulatedSigs.add(new DD4JASiCSignature(containerSignature));
        }
        
        return encapsulatedSigs;
    }
    
    private Signature getSignature() {
        List<ASiCSignature> signatures = getSignatures();
        if(signatures.isEmpty()) {
            DD4JASiCTransaction.logger.error("No signatures found on ASiC container.");
            return null;
        }
        Signature signature = ((DD4JASiCSignature) signatures.get(0)).get();
        if(signature == null) {
            throw new NullPointerException("Signature is null.");
        }
        
        if(this.container.getSignatures().size() > 1) {
            DD4JASiCTransaction.logger.warn("Container has more than one signature (has " + this.container.getSignatures().size() + "), using first.");
        }
    
        return signature;
    }
    
    @Override
    public X509Certificate getSigningCertificate() {
        Signature firstSignature = getSignature();
        if(firstSignature == null) {
            throw new NullPointerException("Signature is null.");
        }
    
        X509Cert x509Cert = firstSignature.getSigningCertificate();
        if(x509Cert == null) {
            throw new NullPointerException("SigningCertificate is null.");
        }
        
        return x509Cert.getX509Certificate();
    }
    
    
    public DataFile extractFile(String path) {
        for(DataFile data : this.container.getDataFiles()) {
            if(data.getName().equals(path)) {
                return data;
            }
        }
        return null;
    }
    
    @Override
    public byte[] extractFileBytes(String path) {
        DataFile f = this.extractFile(path);
        return f != null ? f.getBytes() : null;
    }
    
    @Override
    public List<String> getFileList() {
        List<String> files = new ArrayList<>();
        
        for(DataFile data : this.container.getDataFiles()) {
            files.add(data.getName());
        }
        
        return files;
    }
}
