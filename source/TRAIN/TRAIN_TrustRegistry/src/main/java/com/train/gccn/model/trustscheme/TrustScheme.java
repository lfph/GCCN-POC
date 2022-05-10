package com.train.gccn.model.trustscheme;

import eu.europa.esig.dss.tsl.ServiceInfo;
import eu.europa.esig.dss.x509.CertificateToken;
import com.train.gccn.ATVConfiguration;
import iaik.x509.extensions.AuthorityKeyIdentifier;
import org.apache.log4j.Logger;
import org.digidoc4j.Configuration;
import org.digidoc4j.TSLCertificateSource;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.x500.X500Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TrustScheme {
    
    private static Logger logger = Logger.getLogger(TrustScheme.class);
    private String tsl_location; // URL to Trust Status List
    private String schemeIdentifier; // DNS pointer to scheme (contains URI record)
    private String tsl_content; // the actuall Trust Status List
    private String tsl_keystore_location;
    private String tsl_keystore_password;
    
    public TrustScheme(String tsl_location, String scheme_identifier, String tsl_content) {
        this(tsl_location, scheme_identifier, tsl_content, null, null);
    }
    
    public TrustScheme(String tsl_location, String scheme_identifier, String tsl_content, String tsl_keystore_location, String tsl_keystore_password) {
        this.tsl_location = tsl_location;
        this.schemeIdentifier = scheme_identifier;
        this.tsl_content = tsl_content;
        this.tsl_keystore_location = tsl_keystore_location;
        this.tsl_keystore_password = tsl_keystore_password;
    }
    
    private static String removePrefix(String s, String prefix) {
        if(s != null && s.startsWith(prefix)) {
            return s.substring(prefix.length());
        }
        return s;
    }
    
    public static String cleanSchemeIdentifier(String schemeIdentifier) {
        //id = TrustScheme.removeSuffix(id, ".");
        if(schemeIdentifier.charAt(schemeIdentifier.length() - 1) == '.') {
            schemeIdentifier = schemeIdentifier.substring(0, schemeIdentifier.length() - 1);
        }
        schemeIdentifier = TrustScheme.removePrefix(schemeIdentifier, ".");
        schemeIdentifier = TrustScheme.removePrefix(schemeIdentifier, TrustSchemeFactory.CLAIM_PREFIX);
        schemeIdentifier = TrustScheme.removePrefix(schemeIdentifier, ".");
        return schemeIdentifier;
    }
    
    private static String getCountryFromPrincipal(X500Principal principal) {
        String default_country = "ES";
        String country = TrustScheme.getFieldFromDN(principal, "C");

//        for(Rdn rdn : ldapDN.getRdns()) {
//            if(rdn.getType().equals("C")) {
//                country = (String) rdn.getValue();
//            }
//        }
        if(country == null || country.trim().equals("")) {
            TrustScheme.logger.warn("Country not found in principal, using default: " + default_country);
            country = default_country;
        } else {
            TrustScheme.logger.info("found country: " + country);
        }
    
        return country;
    }
    
    public static String getFieldFromDN(X500Principal principal, String field) {
        LdapName ldapDN = null;
        try {
            ldapDN = new LdapName(principal.getName());
        } catch(InvalidNameException e) {
            TrustScheme.logger.error("", e);
            return null;
        }
        return ldapDN.getRdns()
                .stream()
                .filter(rdn -> rdn.getType().equals(field))
                .map(rdn -> rdn.getValue().toString())
                .collect(Collectors.joining());
    }
    
    public String getTSLcontent() {
        return this.tsl_content;
    }
    
    public String getTSLlocation() {
        return this.tsl_location;
    }
    
    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        TrustScheme scheme = (TrustScheme) o;
        return this.tsl_location.equals(scheme.tsl_location) &&
                this.schemeIdentifier.equals(scheme.schemeIdentifier);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(this.tsl_location, this.schemeIdentifier);
    }
    
    public String getSchemeIdentifier() {
        return this.schemeIdentifier;
    }
    
    public TslEntry getTSLEntry(AuthorityKeyIdentifier issuerKeyIdentifier) {
        // TODO: Replace DD4J with DSS?
        //       https://ec.europa.eu/cefdigital/DSS/webapp-demo/doc/dss-documentation.html#_non_european_trusted_lists_support
        
        // TrustScheme.logger.info("issuerKeyIdentifier on trust list?");
        
        TrustScheme.logger.info("Configuring new TLS location ...");
        Configuration.getInstance().setTslLocation(this.tsl_location);
        
        
        // use local TSL instead of downloading it again?
        //TslLoader loader = new TslLoader();
        //Configuration.getInstance().setTSL();
        
        if(ATVConfiguration.get().getBoolean("dane_verification_enabled")) {
            String keystoreLocation = Configuration.getInstance().getTslKeyStoreLocation();
            TrustScheme.logger.info("TLS keystore: " + keystoreLocation);
//            // We need to verify if the TSL has been signed by the cert pinned in TSPA's SMIMEA,
//            // so we need to tell DD4J to use this cert (as root of trust).
//            if(this.tsl_keystore_location != null) {
//                Configuration.getInstance().setTslKeyStoreLocation(this.tsl_keystore_location);
//                Configuration.getInstance().setTslKeyStorePassword(this.tsl_keystore_password);
//            } else {
//                TrustScheme.logger.error("TSL Verification failed, no root key found (should be loaded from SMIMA in TrustSchemeFactory).");
//            }
        }
        
        TrustScheme.logger.info("Loading TSL ...");
        TSLCertificateSource tsl = Configuration.getInstance().getTSL();
        
        TrustScheme.logger.info("Loading certs ...");
        List<CertificateToken> certificates = tsl.getCertificates();
        
        TrustScheme.logger.info("Looking for issuer cert ...");
        List<CertificateToken> tokens = new ArrayList<>();
        tokens = certificates.stream()
                .filter(cert -> X509Helper.equals(issuerKeyIdentifier, cert.getCertificate()))
                .collect(Collectors.toList());
        
        int numTokens = tokens.size();
        if(numTokens == 0) {
            TrustScheme.logger.error("Cert not found on TSL.");
            return null;
        } else if(numTokens > 1) {
            TrustScheme.logger.warn("Found " + numTokens + " tokens, using first one.");
        }
        
        CertificateToken token = tokens.get(0);
        
        Set<ServiceInfo> trustServices = tsl.getTrustServices(token);
        
        TrustScheme.logger.info("found " + trustServices.size() + " matching trust service(s).");
        
        return new EidasTslEntry(token, trustServices, this.getSchemeIdentifierCleaned());
    }
    
    public String getSchemeIdentifierCleaned() {
        String id = this.getSchemeIdentifier().trim();
        return TrustScheme.cleanSchemeIdentifier(id);
    }
    
}
