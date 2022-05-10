package com.train.gccn.model.transaction;

import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.asic.ASiCExtractResult;
import eu.europa.esig.dss.client.crl.OnlineCRLSource;
import eu.europa.esig.dss.client.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.validation.AdvancedSignature;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.executor.ValidationLevel;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.dss.validation.reports.SimpleReport;
import eu.europa.esig.dss.x509.CertificateToken;
import eu.europa.esig.dss.x509.CommonCertificateSource;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class DSSASiCTransaction extends TransactionContainer {
    
    private final static String validationPolicyPath = "dss-validation-policy.xml";
    private static Logger logger = Logger.getLogger(DSSASiCTransaction.class);
    private final DSSDocument container;
    private final ASiCExtractResult containerContent;
    private String transactionType;
    private List<ASiCSignature> containerSignatures;
    
    public DSSASiCTransaction(DSSDocument container, ASiCExtractResult containerContent) {
        super();
        this.container = container;
        this.containerContent = containerContent;
        this.containerSignatures = null;
    }
    
    private static Reports validate(DSSDocument document, CertificateToken trustedCert) {
        
        SignedDocumentValidator documentValidator = DSSASiCTransaction.initValidator(document, trustedCert);
        
        Reports reports = documentValidator.validateDocument(DSSASiCTransaction.validationPolicyPath);
        
        return reports;
    }
    
    private static SignedDocumentValidator initValidator(DSSDocument document, CertificateToken trustedCert) {
        // First, we need a Certificate verifier
        CertificateVerifier cv = new CommonCertificateVerifier(true);
        
        // We can inject several sources. eg: OCSP, CRL, AIA, trusted lists
        
        // Capability to download resources from AIA
        cv.setDataLoader(new CommonsDataLoader());
        
        // Capability to request OCSP Responders
        //cv.setOcspSource(new OnlineOCSPSource());
        
        // Capability to download CRL
        cv.setCrlSource(new OnlineCRLSource());
        
        // We now add trust anchors (trusted list, keystore,...)
        CommonCertificateSource trustedCertSource = new CommonCertificateSource();
        if(trustedCert != null) {
            trustedCertSource.addCertificate(trustedCert);
        }
        cv.setTrustedCertSource(trustedCertSource);
        
        // We also can add missing certificates
        //cv.setAdjunctCertSource(adjunctCertSource);
        
        // Here is the document to be validated (any kind of signature file)
        //DSSDocument document = new FileDocument(containerFile);
        
        // We create an instance of DocumentValidator
        // It will automatically select the supported validator from the classpath
        SignedDocumentValidator documentValidator = SignedDocumentValidator.fromDocument(document);
        
        documentValidator.setValidationLevel(ValidationLevel.BASIC_SIGNATURES);
        
        // We add the certificate verifier (which allows to verify and trust certificates)
        documentValidator.setCertificateVerifier(cv);
        
        return documentValidator;
    }
    
    private DSSDocument extractFile(String path) {
        for(DSSDocument data : this.containerContent.getSignedDocuments()) {
            if(data.getName().equals(path)) {
                return data;
            }
        }
        return null;
    }
    
    private byte[] extractFileBytes(DSSDocument doc) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            doc.writeTo(buffer);
        } catch(IOException e) {
            DSSASiCTransaction.logger.error("Error extracting file from container: " + doc.getName());
            return null;
        }
        return buffer.toByteArray();
    }
    
    @Override
    public byte[] extractFileBytes(String path) {
        DSSDocument f = this.extractFile(path);
        return f != null ? extractFileBytes(f) : null;
    }
    
    @Override
    public List<String> getFileList() {
        List<String> files = new ArrayList<>();
        
        for(DSSDocument doc : this.containerContent.getSignedDocuments()) {
            files.add(doc.getName());
        }
        
        return files;
    }
    
    @Override
    public boolean verifySignature(X509Certificate cert, ASiCSignature sig) {
        DSSASiCSignature signature = ((DSSASiCSignature) sig);
        
        DSSASiCTransaction.logger.info("Validating signature ...");
        
        Reports report = DSSASiCTransaction.validate(this.container, new CertificateToken(cert));
        
        SimpleReport simpleReport = report.getSimpleReport();
        
        String sigId = simpleReport.getFirstSignatureId();
        
        DSSASiCTransaction.logger.info("Requested signer: " + cert.getSubjectDN().getName());
        DSSASiCTransaction.logger.info("Actual signer:    " + simpleReport.getSignedBy(sigId));

//        if(!sigId.equals(signature.getSignerDSSId())) {
//            DSSASiCTransaction.logger.error("Wrong signer?");
//            DSSASiCTransaction.logger.error("Used:      " + sigId + " (from signature)");
//            DSSASiCTransaction.logger.error("Requested: " + signature.getSignerDSSId() + " (from signer cert)");
//            return false;
//        }
        
        for(String warning : simpleReport.getWarnings(sigId)) {
            DSSASiCTransaction.logger.warn(warning);
        }
        
        boolean statusAfterIgnorelist = true;
        for(String error : simpleReport.getErrors(sigId)) {
            if(TransactionContainer.errorIgnored(error)) {
                DSSASiCTransaction.logger.error("[IGNORED] " + error.toString());
            } else {
                DSSASiCTransaction.logger.error(error.toString());
                statusAfterIgnorelist = false;
            }
        }
        
        return statusAfterIgnorelist;
        // return simpleReport.isSignatureValid(sigId);
    }
    
    @Override
    public X509Certificate getSigningCertificate() {
        List<ASiCSignature> sigs = this.getSignatures();
        if(sigs.size() > 1) {
            DSSASiCTransaction.logger.warn("Container has more than one signature (has " + sigs.size() + "), using first.");
        }
        
        return sigs.get(0).getSigningX509Certificate();
    }
    
    @Override
    public List<ASiCSignature> getSignatures() {
        if(this.containerSignatures != null) {
            return this.containerSignatures;
        }
        
        this.containerSignatures = new ArrayList<>();
        
        SignedDocumentValidator validator = DSSASiCTransaction.initValidator(this.container, null);
        List<AdvancedSignature> signatures = validator.getSignatures();
        
        int signaturesCount = signatures.size();
        if(signaturesCount <= 0) {
            DSSASiCTransaction.logger.error("No signatures on document.");
            return this.containerSignatures;
        }
        
        for(AdvancedSignature signature : signatures) {
            this.containerSignatures.add(new DSSASiCSignature(signature));
        }
        
        return this.containerSignatures;
    }
    
    @Override
    public String getTransactionType() {
        return this.transactionType;
    }
}
