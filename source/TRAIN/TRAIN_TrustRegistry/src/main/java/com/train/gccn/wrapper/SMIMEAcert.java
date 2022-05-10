package com.train.gccn.wrapper;

import iaik.utils.Util;
import iaik.x509.PublicKeyInfo;
import iaik.x509.X509Certificate;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.xbill.DNS.SMIMEARecord;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Arrays;


public class SMIMEAcert {
    
    private static Logger logger = Logger.getLogger(SMIMEAcert.class);
    private final byte[] data; // https://tools.ietf.org/html/rfc6698#section-2.1.4
    private CertUsage certificateUsage; // https://tools.ietf.org/html/rfc6698#section-2.1.1
    private Selector selector; // https://tools.ietf.org/html/rfc6698#section-2.1.2
    private MatchingType matchingType; // https://tools.ietf.org/html/rfc6698#section-2.1.3
    
    public SMIMEAcert(CertUsage certificateUsage, Selector selector, MatchingType matchingType, byte[] data) {
        this.certificateUsage = certificateUsage;
        this.selector = selector;
        this.matchingType = matchingType;
        this.data = data;
    }
    
    public SMIMEAcert(SMIMEARecord rec) {
        this.certificateUsage = CertUsage.getByValue(rec.getCertificateUsage());
        this.selector = Selector.getByValue(rec.getSelector());
        this.matchingType = MatchingType.getByValue(rec.getMatchingType());
        this.data = rec.getCertificateAssociationData();
    }
    
    public CertUsage getCertificateUsage() {
        return this.certificateUsage;
    }
    
    public Selector getSelector() {
        return this.selector;
    }
    
    public MatchingType getMatchingType() {
        return this.matchingType;
    }
    
    public byte[] getCertificateAssociationData() {
        return this.data;
    }
    
    public X509Certificate getCertificate() throws CertificateException {
//        if(this.getCertificateUsage() != CertUsage.Domain_issued_certificate.value) {
//            throw new CertificateException("Cert usage must be 3 (Domain_issued_certificate).");
//        }
//
//        if(this.getSelector() != Selector.Full.value) {
//            throw new CertificateException("Selector must be 0 (Full).");
//        }

//        if(this.getMatchingType() != MatchingType.NoHash) {
//            throw new CertificateException("Matching type must be 0 (NoHash), is  " + this.matchingType);
//        }
        
        return new X509Certificate(this.getCertificateAssociationData());
    }
    
    public PublicKey getPublicKey() throws InvalidKeyException {
        PublicKey publicKey = PublicKeyInfo.getPublicKey(this.data);
        
        return publicKey;
    }
    
    public void init() {
        SMIMEAcert.logger.info("  usage:         " + this.getCertificateUsage());
        SMIMEAcert.logger.info("  selector:      " + this.getSelector());
        SMIMEAcert.logger.info("  matching type: " + this.getMatchingType());
        
        if(this.matchingType == MatchingType.SHA256 || this.matchingType == MatchingType.SHA512) {
            
            int lenBits = this.data.length * 8; // bytes -> bits
            if(lenBits == 256 || lenBits == 512) {
                SMIMEAcert.logger.info("hash length: " + lenBits + " bits");
                SMIMEAcert.logger.info(Hex.encodeHexString(this.data));
                return;
            }
            SMIMEAcert.logger.warn("MatchingType = hash, but invalid hashlength (" + lenBits + ") ... let's try to decode.");
        }
        
        if(this.selector == Selector.Full) {
            try {
                X509Certificate cert = null;
                cert = this.getCertificate();
                SMIMEAcert.logger.info("  Cert from DNS:");
                SMIMEAcert.logger.info("    DN:               " + cert.getSubjectDN());
                SMIMEAcert.logger.info("    SHA1 Fingerprint: " + Util.toString(cert.getFingerprintSHA()));
    
                this.matchingType = MatchingType.NoHash;
                return;
    
            } catch(CertificateException e) {
                SMIMEAcert.logger.warn("Could not parse cert of record: " + e.getMessage());
            }
    
        }
        //else { // selector is SubjectPublicKeyInfo
        try {
            PublicKey key = this.getPublicKey();
            SMIMEAcert.logger.info("  data: " + key.toString());
            this.matchingType = MatchingType.NoHash;
            this.selector = Selector.SubjectPublicKeyInfo;
            return;
    
        } catch(InvalidKeyException e) {
            SMIMEAcert.logger.warn("Could not parse key of record: " + e.getMessage());
        }
        //}
    }
    
    public byte[] calculateAssociationDataForCert(X509Certificate cert) throws NoSuchAlgorithmException, CertificateEncodingException {
        byte[] certData = null;
        
        if(this.selector == Selector.Full) {
            certData = cert.getEncoded();
            
        } else if(this.selector == Selector.SubjectPublicKeyInfo) {
            certData = cert.getPublicKey().getEncoded();
        }
        
        if(this.matchingType == MatchingType.NoHash) {
            // signingCertData = signingCertData
        } else {
            String algorithm = null;
            
            // https://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#MessageDigest
            if(this.matchingType == MatchingType.SHA256) {
                algorithm = "SHA-256";
            } else if(this.matchingType == MatchingType.SHA512) {
                algorithm = "SHA-512";
            }
            
            MessageDigest hasher = MessageDigest.getInstance(algorithm);
            certData = hasher.digest(certData);
        }
        
        return certData;
    }
    
    public boolean match(X509Certificate signingCert) throws CertificateEncodingException, NoSuchAlgorithmException {
        if(this.certificateUsage != CertUsage.Domain_issued_certificate) {
            SMIMEAcert.logger.error("CertUsage must be " + CertUsage.Domain_issued_certificate + ", is: " + this.certificateUsage);
            return false;
        }
        
        byte[] signingCertData = calculateAssociationDataForCert(signingCert);
        byte[] dnsData = this.getCertificateAssociationData();
    
        SMIMEAcert.logger.info("dataFromDNS:  " + Hex.encodeHexString(dnsData));
        SMIMEAcert.logger.info("dataFromFile: " + Hex.encodeHexString(signingCertData));
        
        return Arrays.equals(signingCertData, dnsData);
    }
    
    public enum CertUsage {
        CA_constraint(0), // PKIX-TA: Certificate Authority Constraint
        Service_certificate_constraint(1), // PKIX-EE: Service Certificate Constraint
        Trust_anchor_assertion(2), // DANE-TA: Trust Anchor Assertion
        Domain_issued_certificate(3); // DANE-EE: Domain Issued Certificate
        
        private final int value;
        
        private CertUsage(int value) {
            this.value = value;
        }
        
        public static CertUsage getByValue(int value) {
            return CertUsage.values()[value];
        }
    }
    
    public enum Selector {
        Full(0),
        SubjectPublicKeyInfo(1);
        
        private final int value;
        
        private Selector(int value) {
            this.value = value;
        }
    
        public static Selector getByValue(int value) {
            return Selector.values()[value];
        }
    }
    
    public enum MatchingType {
        NoHash(0),
        SHA256(1),
        SHA512(2);
        
        private final int value;
        
        private MatchingType(int value) {
            this.value = value;
        }
    
        public static MatchingType getByValue(int value) {
            return MatchingType.values()[value];
        }
    }
}
