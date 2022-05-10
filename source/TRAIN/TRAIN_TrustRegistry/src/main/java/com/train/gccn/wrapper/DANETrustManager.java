package com.train.gccn.wrapper;

import com.train.gccn.exceptions.DANEException;
import com.train.gccn.exceptions.DNSException;
import org.xbill.DNS.TLSARecord;
import org.xbill.DNS.utils.base16;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

public class DANETrustManager implements X509TrustManager {
    
    private static final String DANE_PREFIX = "_443._tcp.";
    
    private X509TrustManager wrappedManager = null;
    private String host;
    private DNSHelper dnsHelper;
    private List<TLSARecord> tlsaRecords;
    
    public DANETrustManager(X509TrustManager managerToWrap) throws IOException {
        this.wrappedManager = managerToWrap;
        this.dnsHelper = new DNSHelper();
        
    }
    
    @Override
    public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        this.wrappedManager.checkClientTrusted(certs, authType);
    }
    
    @Override
    public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        System.out.println("DANETrustManager.checkServerTrusted(): for " + this.host);
//        System.out.println("  cert[0]  = " + certs[0].toString());
//        System.out.println("  authType = " + authType);
        
        this.wrappedManager.checkServerTrusted(certs, authType);
        
        try {
            this.initDANE();
        } catch(IOException | DNSException e) {
            throw new CertificateException(e);
        }
        
        // Subject & SubjectAltName check is done by wrappedManager
        
        for(int idx = 0; idx < certs.length; idx++) {
            this.performDANEVerify(idx, certs[idx]);
        }
        
    }
    
    private void performDANEVerify(int certIdx, X509Certificate cert) throws CertificateException {
        boolean status = false;
        
        for(TLSARecord tlsa : this.tlsaRecords) {
            try {
                status = this.performDANEVerify(certIdx, cert, tlsa);
            } catch(NoSuchAlgorithmException e) {
                throw new CertificateException(e);
            }
            if(status == true) {
                return;
            }
        }
        
        if(status == false) {
            //System.out.println(cert);
            throw new CertificateException(new DANEException("Verification did not pass. (at cert idx: " + certIdx + ")"));
        }
    }
    
    
    private boolean performDANEVerify(int certIdx, X509Certificate cert, TLSARecord tlsa) throws CertificateException, NoSuchAlgorithmException {
        // https://tools.ietf.org/html/rfc6698#section-2.1
        
        System.out.println("[DANE] CertificateUsage: " + tlsa.getCertificateUsage() + ", certIdx: " + certIdx);
        
        if(tlsa.getCertificateUsage() == 3) {
            // 3 ... look at the leaf cert
            
            if(certIdx == 0) {
                // you are at the leaf and you want the leaf
                boolean lookAtKey = tlsa.getSelector() == 1; // look at key (1) or at cert (0)
                byte[] material;
                if(lookAtKey) {
                    material = cert.getPublicKey().getEncoded();
                } else {
                    material = cert.getEncoded();
                }
                
                if(tlsa.getMatchingType() == 0) {
                    // material = material;
                } else {
                    MessageDigest md = MessageDigest.getInstance(tlsa.getMatchingType() == 1 ? "SHA-256" : "SHA-512");
                    md.update(material);
                    byte[] digest = md.digest();
                    material = digest;
                }
                
                System.out.println("[DANE] Data calculated: " + base16.toString(material));
                System.out.println("[DANE] Data in DNS:     " + base16.toString(tlsa.getCertificateAssociationData()));
                
                if(Arrays.equals(material, tlsa.getCertificateAssociationData())) {
                    System.out.println("[DANE] Verification passed!");
                    return true;
                } else {
                    System.out.println("[DANE] Hashes don't match!");
                    return false;
                }
            } else {
                // nothing to do anymore, since leaf is already checked ...
                return true;
            }
        } else if(tlsa.getCertificateUsage() < 0 || tlsa.getCertificateUsage() > 3) {
            System.out.println("[DANE] Invalid Certificate Usage: " + tlsa.getCertificateUsage());
            return false;
        } else {
            throw new CertificateException(new DANEException("Unsupported Certificate Usage: " + tlsa.getCertificateUsage()));
            // TODO: need support for 0, 1 & 2
            //       https://extgit.iaik.tugraz.at/LIGHTest/AutomaticTrustVerifier/issues/2
        }
        
        //return false; // unreachable
    }
    
    
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return this.wrappedManager.getAcceptedIssuers();
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    private void initDANE() throws IOException, DNSException {
        String host = DANETrustManager.DANE_PREFIX + (this.host.endsWith(".") ? this.host : this.host + ".");
        System.out.println("[DANE] Looking up TLSA record(s) of " + host);
        
        this.tlsaRecords = this.dnsHelper.queryAndParse(host, TLSARecord.class, DNSHelper.RECORD_TLSA);
        
        System.out.println("[DANE] Found " + this.tlsaRecords.size() + " TLSA record(s).");

//        for (TLSARecord rec : this.tlsaRecords_) {
//            // https://tools.ietf.org/html/rfc6698#section-2.1
//            System.out.println("usage:    " + rec.getCertificateUsage()); // 3 ... look at leaf cert, ignore cas!
//            System.out.println("selector: " + rec.getSelector()); // 0 ... full cert
//            System.out.println("matching: " + rec.getMatchingType()); // 1 ... SHA-256 hash
//            System.out.println("data:     " + base16.toString(rec.getCertificateAssociationData()));
//        }
    }
    
}
