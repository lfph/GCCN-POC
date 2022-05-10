package com.train.gccn.model.format.eIDAS_qualified_certificate;

import eu.lightest.horn.specialKeywords.HornApiException;
import com.train.gccn.ATVConfiguration;
import com.train.gccn.exceptions.DNSException;
import com.train.gccn.model.format.AbstractFormatParser;
import com.train.gccn.model.format.FormatParser;
import com.train.gccn.model.report.Report;
import com.train.gccn.model.report.ReportStatus;
import com.train.gccn.model.tpl.TplApiListener;
import com.train.gccn.model.translation.TrustTranslation;
import com.train.gccn.model.translation.TrustTranslationFactory;
import com.train.gccn.model.trustscheme.*;
import iaik.asn1.structures.GeneralName;
import iaik.x509.extensions.AuthorityKeyIdentifier;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.*;

public class EidasCertFormat extends AbstractFormatParser {
    
    public static final String OID_AuthorityKeyIdentifier = AuthorityKeyIdentifier.oid.getID(); // "2.5.29.35";
    public static final String RESOLVETYPE_X509CERT = "X509Certificate";
    private static final String RESOLVETYPE_X509PRINCIPAL = "X500Principal";
    private static final String RESOLVETYPE_TSLENTRY = "TrustStatusListEntry";
    private static final String RESOLVETYPE_AuthorityKeyIdentifier = "AuthorityKeyIdentifier";
    private static final String FORMAT_ID = "eIDAS_qualified_certificate";
    private static final String PATH_ISSUER = "issuer";
    //private static final String PATH_ISSUER_SCHEME = "issuer.trustScheme";
    private static final String PATH_PUBKEY = "pubKey";
    private static final String PATH_TRUSTSCHEME = "trustScheme";
    private static final String PATH_TRANSLATION = "translation";
    private static final String PATH_TRUSTLISTENTRY = "TrustListEntry";
    private static Logger logger = Logger.getLogger(EidasCertFormat.class);
    //private static final String PATH_ISSUERPUBKEY = EidasCertFormat.PATH_ISSUER + "." + EidasCertFormat.PATH_TRUSTLISTENTRY + "." + EidasCertFormat.PATH_PUBKEY;
    
    private Map<String, String> schemeClaims = ATVConfiguration.getForPrefix("trustscheme_claim");
    private String schemeClaimDefault = ATVConfiguration.get().getString("trustscheme_claim_default");
    // private TSLEntry tslEntry = null; // removed via #25
    private TrustScheme trustScheme = null;
    private X509Certificate signingCertificate;
    private List<String> claims = null;
    private String translationSchemeClaimed;
    private String translationSchemeTrusted;
    
    public EidasCertFormat(Object transaction, Report report) {
        super(transaction, report);
        
        if(transaction == null) {
            EidasCertFormat.logger.error("Given Transaction is null. We need a file or a cert here ...");
            throw new IllegalArgumentException("Given Transaction is null.");
        }
        
        // cert can be given as X509Certificate or File ...
        
        if(transaction instanceof File) {
            try {
                // cert given as file, lets try to read it ...
                
                CertificateFactory fact = CertificateFactory.getInstance("X.509");
                FileInputStream is = new FileInputStream((File) transaction);
                this.signingCertificate = (X509Certificate) fact.generateCertificate(is);
                
            } catch(FileNotFoundException | CertificateException e) {
                // if this fails, we need to implement better parsing.
                // (right now only PEM is tested)
                
                EidasCertFormat.logger.error(e.toString());
                throw new IllegalArgumentException(e);
            }
    
        } else if(transaction instanceof X509Certificate) {
            this.signingCertificate = (X509Certificate) transaction;
    
        } else {
            EidasCertFormat.logger.error("Transaction of type:" + transaction.getClass().toString() + ",  expected: File or X509Certificate");
            throw new IllegalArgumentException("Transaction of type:" + transaction.getClass().toString() + ",  expected: File or X509Certificate");
        }
    
    }
    
    @Override
    public boolean onPrint(PrintObj printObj) {
        EidasCertFormat.logger.info("onPrint:");
        printList("path", printObj.mPath);
        
        if(printObj.mPath.size() >= 3
                && printObj.mPath.get(0).equals(EidasCertFormat.PATH_ISSUER)
                && printObj.mPath.get(1).equals(EidasCertFormat.PATH_TRUSTLISTENTRY)) {
            // issuer.TrustListEntry + $schemeId + $field
            
            EidasCertFormat.logger.info("Looking for a TSL Entry at " + String.join(".", printObj.mPath));
            
            String field = printObj.mPath.remove(3);
            FormatParser parser = getParser(printObj.mPath, true);
            
            if(parser == null) {
                EidasCertFormat.logger.error("No parser for " + printObj.mPath);
                return false;
            }
            
            printObj.mPath.add(field); // add back the field since we need to resolve it ...
            ResolvedObj resolvedObj = parser.resolveObj(pop(pop(pop(printObj.mPath))));
            this.report.addLine(field + ": " + resolvedObj.mValue, ReportStatus.PRINT);
            
            return true;
        }
        
        FormatParser parser = getParser(printObj.mPath.get(0));
        printObj.mPath = pop(printObj.mPath);
        EidasCertFormat.logger.info("calling onPrint on " + parser.getClass().toString());
        
        boolean status = parser.onPrint(printObj);
        
        if(status == false) {
            EidasCertFormat.logger.error("Path " + String.join(".", printObj.mPath) + " not available in this format.");
        }
        return status;
    }
    
    @Override
    public boolean onExtract(List<String> path, String query, List<String> output) throws HornApiException {
        EidasCertFormat.logger.info("onExtract1 @ " + this.getFormatId() + ":");
        printList("path", path);
        EidasCertFormat.logger.info("query: " + query);
        
        if(path.size() == 0) {
            switch(query) {
                case EidasCertFormat.PATH_PUBKEY:
                case EidasCertFormat.PATH_ISSUER:
                case EidasCertFormat.PATH_TRUSTSCHEME:
                case AbstractFormatParser.QUERY_FORMAT:
                    return true;
            }
        } else if(path.size() == 1 &&
                path.get(0).equals(EidasCertFormat.PATH_ISSUER)) {
            switch(query) {
                case EidasCertFormat.PATH_TRUSTSCHEME:
                    return true;
            }
        } else if(path.size() == 3 &&
                path.get(0).equals(EidasCertFormat.PATH_ISSUER) &&
                path.get(1).equals(EidasCertFormat.PATH_TRUSTLISTENTRY)) {
            
            String parserId = String.join(".", path);
            EidasCertFormat.logger.info("delegating to parser: " + parserId);
            
            FormatParser parser = getParser(parserId);
            if(parser == null) {
                EidasCertFormat.logger.error("No parser for TSL Entry. Did you forgot your format-extract?");
                // TPL policy needs to contain:
                //   extract(TrustListEntry, format, trustlist_entry),
                return false;
            }
            return parser.onExtract(pop(pop(pop(path))), query, output);
            // 3x pop, because path = issuer.TrustListEntry.<entry_id>
        }
        
        return false;
    }
    
    @Override
    public boolean onTrustschemeCheck(List<String> pathToClaim, String schemeId) {
        List<String> claimedClaims = getClaims();
        if(claimedClaims == null) {
            return false;
        }
        
        if(pathToClaim.size() == 2
                && pathToClaim.get(0).equals(EidasCertFormat.PATH_ISSUER)
                && pathToClaim.get(1).equals(EidasCertFormat.PATH_TRUSTSCHEME)) {
            // given cert == signer cert, so one step up for issuer
            
            for(String claimedClaim : claimedClaims) {
                if(verifyTrustschemeClaim(claimedClaim, schemeId) == true) {
                    return true;
                }
            }
        } else if(pathToClaim.size() == 1
                && pathToClaim.get(0).equals(EidasCertFormat.PATH_TRUSTSCHEME)) {
            // given cert == issuer cert
            
            for(String claimedClaim : claimedClaims) {
                if(verifyTrustschemeClaim(claimedClaim, schemeId) == true) {
                    return true;
                }
            }
        } else if(pathToClaim.size() == 0) {
            for(String claimedClaim : claimedClaims) {
                if(verifyTrustschemeClaim(claimedClaim, schemeId) == true) {
                    return true;
                }
            }
        }
        
        EidasCertFormat.logger.warn("onTrustschemeCheck not successful ...");
        return false;
    }
    
    @Override
    public boolean onVerifySignature(List<String> pathToSubject, List<String> pathToCert) {
        printList("pathToSubject", pathToSubject);
        printList("pathToCert", pathToCert);
        
        if(pathToSubject.size() != 0) {
            EidasCertFormat.logger.warn("Not sure what to verify here.");
            return false;
        }
        
        X509Certificate signingCert = this.signingCertificate;
        
        ResolvedObj issuerCertObj = this.rootListener.resolveObj(pathToCert);
        
        if(issuerCertObj == null || !issuerCertObj.mType.equals(EidasCertFormat.RESOLVETYPE_X509CERT) || !(issuerCertObj.mValue instanceof X509Certificate)) {
            EidasCertFormat.logger.error("Could not resolve certificate from " + String.join(".", pathToCert));
            this.report.addLine("Signer Verification failed: Certificate error.", ReportStatus.FAILED);
            return false;
        }
        
        X509Certificate issuerCert = (X509Certificate) issuerCertObj.mValue;
        
        try {
            EidasCertFormat.logger.info("Verifying signer cert (from transaction) using issuer cert (from TSL) ...");
            signingCert.verify(issuerCert.getPublicKey());
        } catch(CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
            EidasCertFormat.logger.error("ERROR: " + e.getMessage());
            this.report.addLine("Signer Verification failed: Signature error.", ReportStatus.FAILED);
            return false;
        }
        
        this.report.addLine("Signer Verification successful.");
        return true;
    }
    
    public List<String> getClaims() {
        if(this.claims != null && !this.claims.isEmpty()) {
            return this.claims;
        }
        
        this.claims = extractMembershipClaim();
        
        int numClaims = this.claims.size();
        
        
        if(numClaims == 0) {
            String defaultClaim = this.schemeClaims.get(this.schemeClaimDefault);
            if(defaultClaim == null) {
                this.report.addLine("No Trust Scheme Membership Claim found in certificate, and no default configured.", ReportStatus.FAILED);
                EidasCertFormat.logger.error("No match for configured default claim: " + this.schemeClaimDefault);
                return null;
            }
    
            EidasCertFormat.logger.warn("No claim found in transaction, using default: " + defaultClaim + ".");
            this.report.addLine("No Trust Scheme Membership Claim found in certificate, using default: " + this.schemeClaimDefault);
    
            this.claims.add(defaultClaim);
            
        } else if(numClaims > 1) {
            EidasCertFormat.logger.warn("More than 1 claim found, using only first one ...");
        }
    
        for(String claim : this.claims) {
            this.report.addLine("Claimed Signer: " + TrustScheme.cleanSchemeIdentifier(claim));
        }
    
    
        return this.claims;
    }
    
    private boolean verifyTrustschemeClaim(String claimedScheme, String requiredScheme) {
        TrustScheme scheme = createTrustScheme(claimedScheme);
        if(scheme == null) {
            EidasCertFormat.logger.error("Could not discover scheme " + claimedScheme);
            return false;
        }
        
        String claimedSchemeHost = scheme.getSchemeIdentifierCleaned();
        
        // resolve TPL constant to hostname:
        String requiredSchemeHost = this.schemeClaims.get(requiredScheme);
        if(requiredSchemeHost == null) {
            EidasCertFormat.logger.error("required scheme (" + requiredScheme + ") invalid.");
            return false;
        }
        
        if(!requiredSchemeHost.equals(claimedSchemeHost)) {
            EidasCertFormat.logger.info("FAIL! claimed: '" + claimedSchemeHost + "', but required: '" + requiredSchemeHost + "'");
            //this.report.addLine("Trustschemes did not match.", ReportStatus.WARNING);
            this.report.addLine("Claimed Scheme (" + claimedSchemeHost + ") does not match Trusted Scheme (" + requiredSchemeHost + ").", ReportStatus.WARNING);
            return false;
        }
    
        EidasCertFormat.logger.info("MATCH! claimed: " + claimedSchemeHost + ", required: " + requiredSchemeHost);
        this.report.addLine("Claimed Scheme matches Trusted Scheme: " + requiredSchemeHost);
        return true;
    }
    
    public TrustScheme createTrustScheme(String claimedScheme) {
        if(this.trustScheme == null) {
            
            TrustSchemeClaim claim = new TrustSchemeClaim(claimedScheme);
            
            try {
                this.trustScheme = TrustSchemeFactory.createTrustScheme(claim, this.report);
            } catch(IOException | DNSException e) {
                EidasCertFormat.logger.error("Exception: " + e.getLocalizedMessage());
                this.report.addLine("Could not create trust scheme: Technical error.", ReportStatus.FAILED);
                return null;
            }
            
        }
        
        return this.trustScheme;
    }
    
    @Override
    public boolean onTrustlist(List<String> pathToClaim, List<String> pathToIssuerCert, List<String> outputPathToEntry) {
        if(pathToClaim.size() == 0) {
            ResolvedObj certObj = this.rootListener.resolveObj(pathToIssuerCert);
            if(certObj == null || !certObj.mType.equals(EidasCertFormat.RESOLVETYPE_X509CERT)
                    || !(certObj.mValue instanceof X509Certificate)) {
                EidasCertFormat.logger.error("Could not resolve certificate from " + String.join(".", pathToIssuerCert));
                return false;
            }
    
            ResolvedObj sigObj = this.genResolvedObj(X509Helper.genAuthorityKeyIdentifier(this.signingCertificate), EidasCertFormat.RESOLVETYPE_AuthorityKeyIdentifier);
            if(sigObj == null || !sigObj.mType.equals(EidasCertFormat.RESOLVETYPE_AuthorityKeyIdentifier)
                    || !(sigObj.mValue instanceof AuthorityKeyIdentifier)) {
                EidasCertFormat.logger.error("Could not resolve certificate principal from " + String.join(".", pathToIssuerCert));
                return false;
            }
    
            AuthorityKeyIdentifier issuerKeyIdentifier = (AuthorityKeyIdentifier) sigObj.mValue;
            EidasCertFormat.logger.info("Loading trustlist entry for " + issuerKeyIdentifier.getName());
    
            List<String> claimedClaims = getClaims();
            if(claimedClaims == null) {
                return false;
            }
            String claimedClaim = claimedClaims.get(0);
            TrustScheme newTrustScheme = createTrustScheme(claimedClaim);
            if(newTrustScheme == null) {
                EidasCertFormat.logger.error("Could not discover scheme " + claimedClaim);
                return false;
            }
    
            EidasCertFormat.logger.info("Using trust list at " + newTrustScheme.getTSLlocation());
    
            EidasCertFormat.logger.info("Loading TSL Entry for scheme: " + newTrustScheme.getSchemeIdentifierCleaned());
    
            TslEntry tslEntry = newTrustScheme.getTSLEntry(issuerKeyIdentifier);
    
            if(tslEntry == null) {
                this.report.addLine("Certificate not found on Trust Status List of " + newTrustScheme.getSchemeIdentifierCleaned(), ReportStatus.WARNING);
                return false;
            }
    
            String tslEntryPath = EidasCertFormat.PATH_ISSUER + "."
                    + EidasCertFormat.PATH_TRUSTLISTENTRY + "."
                    + newTrustScheme.getSchemeIdentifierCleaned();
            cacheResolvedObj(tslEntryPath, tslEntry, EidasCertFormat.RESOLVETYPE_TSLENTRY);
    
            EidasCertFormat.logger.info(tslEntry.toString());
    
            String serviceName = tslEntry.getServiceName();
    
            this.report.addLine("Issuer found on Trust Status List: " + serviceName);
    
            outputPathToEntry.addAll(pathToIssuerCert);
            outputPathToEntry.add(EidasCertFormat.PATH_TRUSTLISTENTRY);
            outputPathToEntry.add(newTrustScheme.getSchemeIdentifierCleaned());
    
            printList("outputPathToEntry", outputPathToEntry);
    
            return true;
    
        }
        if(pathToClaim.size() == 2
                && pathToClaim.get(0).equals(EidasCertFormat.PATH_ISSUER)
                && pathToClaim.get(1).equals(EidasCertFormat.PATH_TRUSTSCHEME)) {
    
            ResolvedObj sigObj = this.rootListener.resolveObj(pathToIssuerCert);
            if(sigObj == null || !sigObj.mType.equals(EidasCertFormat.RESOLVETYPE_AuthorityKeyIdentifier)
                    || !(sigObj.mValue instanceof AuthorityKeyIdentifier)) {
                EidasCertFormat.logger.error("Could not resolve certificate principal from " + String.join(".", pathToIssuerCert));
                return false;
            }
            
            AuthorityKeyIdentifier issuerKeyIdentifier = (AuthorityKeyIdentifier) sigObj.mValue;
            EidasCertFormat.logger.info("Loading trustlist entry for " + issuerKeyIdentifier.getName());
    
            List<String> claimedClaims = getClaims();
            if(claimedClaims == null) {
                return false;
            }
            String claimedClaim = claimedClaims.get(0);
            TrustScheme newTrustScheme = createTrustScheme(claimedClaim);
            if(newTrustScheme == null) {
                EidasCertFormat.logger.error("Could not discover scheme " + claimedClaim);
                return false;
            }
    
            EidasCertFormat.logger.info("Using trust list at " + newTrustScheme.getTSLlocation());
    
            EidasCertFormat.logger.info("Loading TSL Entry for scheme: " + newTrustScheme.getSchemeIdentifierCleaned());
    
            TslEntry tslEntry = newTrustScheme.getTSLEntry(issuerKeyIdentifier);
            
            if(tslEntry == null) {
                this.report.addLine("Certificate not found on Trust Status List of " + newTrustScheme.getSchemeIdentifierCleaned(), ReportStatus.FAILED);
                return false;
            }
            
            String tslEntryPath = EidasCertFormat.PATH_ISSUER + "."
                    + EidasCertFormat.PATH_TRUSTLISTENTRY + "."
                    + newTrustScheme.getSchemeIdentifierCleaned();
            cacheResolvedObj(tslEntryPath, tslEntry, EidasCertFormat.RESOLVETYPE_TSLENTRY);
            
            EidasCertFormat.logger.info(tslEntry.toString());
            
            String serviceName = tslEntry.getServiceName();
            
            this.report.addLine("Issuer found on Trust Status List: " + serviceName);
            
            outputPathToEntry.addAll(pathToIssuerCert);
            outputPathToEntry.add(EidasCertFormat.PATH_TRUSTLISTENTRY);
            outputPathToEntry.add(newTrustScheme.getSchemeIdentifierCleaned());
            
            printList("outputPathToEntry", outputPathToEntry);
            
            return true;
        }
        
        this.report.addLine("Certificate not found on Trust Status List.", ReportStatus.FAILED);
        return false;
    }
    
    
    @Override
    public ResolvedObj resolveObj(List<String> path) {
        EidasCertFormat.logger.info("resolveObj @ " + this.getFormatId() + ":");
        printList("path", path);
        
        // TODO: do extract in `extract()` and only load from cache here?
        
        String query = String.join(".", path);
        switch(query) {
            case EidasCertFormat.PATH_PUBKEY:
                return this.genResolvedObj(this.signingCertificate, EidasCertFormat.RESOLVETYPE_X509CERT);
            case EidasCertFormat.PATH_ISSUER:
                return this.genResolvedObj(X509Helper.genAuthorityKeyIdentifier(this.signingCertificate), EidasCertFormat.RESOLVETYPE_AuthorityKeyIdentifier);
//            case EidasCertFormat.PATH_ISSUERPUBKEY:
//                return this.genResolvedObj(this.tslEntry.getCertificate(), EidasCertFormat.RESOLVETYPE_X509CERT);
            case AbstractFormatParser.QUERY_FORMAT:
                return genResolvedObj(getFormatId(), "STRING");
        }
        
        // TODO: refactor / cleanup
        //   1. switch/case for hardcode values
        //   2.   else: from resolvcache if exists (don't tread TLSentry as special case)
        //   3.     else: from parser if exists
        
        if(path.size() >= 3
                && path.get(0).equals(EidasCertFormat.PATH_ISSUER)
                && path.get(1).equals(EidasCertFormat.PATH_TRUSTLISTENTRY)) {
            // issuer.TrustListEntry + $schemeId + $field
            
            EidasCertFormat.logger.info("Looking for a TSL Entry at " + String.join(".", path));
            
            
            TslEntry tslEntry = getTSLEntryFromCache(path);
            
            if(tslEntry != null) {
                // found a tlsEntry for the full path, so no $field given, returning the TSL itself (for format-extract)
                EidasCertFormat.logger.info("No field given. Returning TLSentry itself.");
                return this.genResolvedObj(tslEntry, EidasCertFormat.RESOLVETYPE_TSLENTRY);
            }
            
            
            String field = path.remove(3);
            FormatParser parser = getParser(path, true);
            
            if(parser == null) {
                EidasCertFormat.logger.error("No parser for " + path);
                return null;
            }
            
            path.add(field); // add back the field since we need to resolve it ...
            return parser.resolveObj(pop(pop(pop(path))));
            
        }
        
        EidasCertFormat.logger.error("Could not resolveObj for path " + String.join(".", path));
        return null;
    }
    
    @Override
    public boolean setFormat(List<String> input, String format) {
        
        EidasCertFormat.logger.info("setFormat @ " + this.getFormatId() + ":");
        printList("input", input);
        EidasCertFormat.logger.info("   format: " + format);
        
        return extractFormat(input, format);
    }
    
    private TslEntry getTSLEntryFromCache(List<String> path) {
        EidasCertFormat.logger.info("getTSLEntryFromCache @ " + this.getFormatId() + ":");
        printList("path", path);
        
        String resolvPath = String.join(".", path); // = issuer.TrustListEntry + $schemeId
        ResolvedObj resolvedObj = this.getCachedResolvedObj(resolvPath);
        
        if(resolvedObj == null) {
            return null;
        }
        
        if(!resolvedObj.mType.equals(EidasCertFormat.RESOLVETYPE_TSLENTRY)) {
            EidasCertFormat.logger.error("Could not resolve obj at path " + path +
                    ", expected: " + EidasCertFormat.RESOLVETYPE_TSLENTRY +
                    ", received: " + resolvedObj.mType);
            return null;
        }
        
        return (TslEntry) resolvedObj.mValue;
    }
    
    @Override
    public boolean onTranslate(List<String> translationEntryPath, List<String> trustListEntryPath, List<String> trustedTrustListEntryPath) {
        
        ResolvedObj cachedTslEntry = this.rootListener.resolveObj(trustListEntryPath);
        if(cachedTslEntry.mType != EidasCertFormat.RESOLVETYPE_TSLENTRY || !(cachedTslEntry.mValue instanceof TslEntry)) {
            return false;
        }
        TslEntry tslEntry = (TslEntry) cachedTslEntry.mValue;
    
        String schemeClaimed = tslEntry.getSchemeId(); // = this.translationSchemeClaimed
        String schemeTrusted = this.translationSchemeTrusted;
        this.report.addLine("Performing Trust Translation (claimed/target: " + schemeClaimed + "--> trusted/source: " + schemeTrusted + ") ...");
        
        // not needed to use resolvObj for translationEntry, since we used translationEntryPath to find the parser
        ResolvedObj cachedTranslEntry = this.getCachedResolvedObj(translationEntryPath, AbstractFormatParser.RESOLVETYPE_XML_CONTENT);
        TrustTranslation translation = null;
        try {
            translation = TrustTranslationFactory.buildTranslation((String) cachedTranslEntry.mValue, this.report, schemeClaimed, schemeTrusted);
        } catch(ParserConfigurationException | SAXException | IOException e) {
            EidasCertFormat.logger.error(e.getMessage());
            return false;
        }
        
        TslEntry translatedEntry = translation.translate(tslEntry, this.report);
        
        if(translatedEntry == null) {
            EidasCertFormat.logger.error("Translation failed.");
            this.report.addLine("Trust Translation failed.", ReportStatus.FAILED);
            return false;
        }
        
        String translatedEntryPath = EidasCertFormat.PATH_ISSUER + "."
                + EidasCertFormat.PATH_TRUSTLISTENTRY + "."
                + String.join(".", translationEntryPath);
        cacheResolvedObj(translatedEntryPath, translatedEntry, EidasCertFormat.RESOLVETYPE_TSLENTRY);
        
        
        //trustedTrustListEntryPath.addAll(pathToIssuerCert);
        trustedTrustListEntryPath.add(EidasCertFormat.PATH_ISSUER);
        trustedTrustListEntryPath.add(EidasCertFormat.PATH_TRUSTLISTENTRY);
        trustedTrustListEntryPath.add(String.join(".", translationEntryPath));
        
        printList("trustedTrustListEntryPath", trustedTrustListEntryPath);
        
        return true;
    }
    
    @Override
    public boolean onEncodeTranslationDomain(List<String> pathToClaim, String trustedScheme, List<String> pathToTTADomain) {
        
        if(pathToClaim.size() == 2
                && pathToClaim.get(0).equals(EidasCertFormat.PATH_ISSUER)
                && pathToClaim.get(1).equals(EidasCertFormat.PATH_TRUSTSCHEME)) {
            
            // ttaDomain =
            //   .input.certificate                     = path to this parser (added by lower parsers)
            //   .translation                           = translation path (for resolveObj)
            //   .eidas.lightest.nlnetlabs.eu           = $claimedSchemeHost
            //   ._translate._trust                     = translation dns prefix
            //   .fantasyland.lightest.nlnetlabs.eu     = $trustedSchemeHost
            
            // step 1: extract claim from cert & discover corresponding scheme hostname
            List<String> claimedClaims = getClaims();
            if(claimedClaims == null) {
                return false;
            }
            String claimedScheme = claimedClaims.get(0);
            TrustScheme scheme = createTrustScheme(claimedScheme);
            if(scheme == null) {
                EidasCertFormat.logger.error("Could not discover scheme " + claimedScheme);
                return false;
            }
            String claimedSchemeHost = scheme.getSchemeIdentifierCleaned();
            EidasCertFormat.logger.info(" claimedScheme:     " + claimedScheme);
            EidasCertFormat.logger.info("$claimedSchemeHost: " + claimedSchemeHost);
    
            // step 2: resolve trustedScheme TPL-constant into hostname
            String trustedSchemeHost = this.schemeClaims.get(trustedScheme);
            if(trustedSchemeHost == null) {
                EidasCertFormat.logger.error("required scheme (" + trustedScheme + ") invalid.");
                return false;
            }
            EidasCertFormat.logger.info("$trustedSchemeHost: " + trustedSchemeHost);
    
            if(claimedSchemeHost == trustedSchemeHost) {
                this.report.addLine("$claimedSchemeHost == $trustedSchemeHost. What are you doing?");
            }
    
            this.translationSchemeClaimed = claimedSchemeHost;
            this.translationSchemeTrusted = trustedSchemeHost;
    
            this.report.addLine("Preparing discovery of Trust Translation Authority ...");
    
            String ttaHost;
    
            if(ATVConfiguration.get().getBoolean("translation.use_simple_dns_record", false)) {
                // DEMO MODE; REMOVE AFTER RESOLVING LIGHTest/TTA#5
                // _translation._trust.<scheme>.lightest.nlnetlabs.nl.
        
                // target=claimed, source=trusted
        
                String ttaScheme;
                if(ATVConfiguration.get().getBoolean("translation.use_simple_dns_record.target_mode", false)) {
                    // target mode (TTA's dns_uri_record_target = true)
                    // = TTA is operated by claimed scheme
                    EidasCertFormat.logger.info("Assuming TTA in TARGET mode.");
                    this.report.addLine("Assuming TTA in TARGET mode (= operated by scheme claimed by transaction).");
    
                    ttaScheme = claimedSchemeHost;
                } else {
                    // source mode (TTA's dns_uri_record_target = false)
                    // = TTA is operated by trusted scheme
                    EidasCertFormat.logger.info("Assuming TTA in SOURCE mode.");
                    this.report.addLine("Assuming TTA in SOURCE mode (= operated by scheme configured in policy).");
    
                    ttaScheme = trustedSchemeHost;
                }
        
                ttaHost = TrustTranslationFactory.POINTER_PREFIX + "." + ttaScheme + "." + "lightest.nlnetlabs.nl";
                this.report.addLine("Using TTA at " + ttaScheme + "." + "lightest.nlnetlabs.nl");
                
                // step 3: build path for resolveObj
                pathToTTADomain.add(EidasCertFormat.PATH_TRANSLATION);
                pathToTTADomain.addAll(Arrays.asList(ttaHost.split("\\.")));
                
            } else {
                // this is the default
                // <target_scheme>._translation._trust.<source_scheme>.
                // used when https://extgit.iaik.tugraz.at/LIGHTest/TTA/issues/5 is resolved
                ttaHost = claimedSchemeHost + "." + TrustTranslationFactory.POINTER_PREFIX + "." + trustedSchemeHost;
                
                // step 3: build path for resolveObj
                pathToTTADomain.add(EidasCertFormat.PATH_TRANSLATION);
                pathToTTADomain.addAll(Arrays.asList(ttaHost.split("\\.")));
            }
            
            String ttaCacheKey = EidasCertFormat.PATH_TRANSLATION + "." + ttaHost; // could be different
            
            cacheResolvedObj(ttaCacheKey, ttaHost, AbstractFormatParser.RESOLVETYPE_HTTP_URL);
    
            EidasCertFormat.logger.info("ttaHost: " + ttaHost);
            printList("ttaDomain", pathToTTADomain);
            return true;
        }
        
        // probably called this parser on the wrong variable
        return false;
    }
    
    //Extract trust membership claim
    public List<String> extractMembershipClaim() {
        X509Certificate cert = this.signingCertificate;
        
        List<String> altNames = new ArrayList<>();
        
        try {
            // you probably want to do that on a different cert, e.g. signer cert. Do we have that?
//            List<String> subjectAltNames = this.extractSubjectAlternativeNames(cert);
//            altNames.addAll(subjectAltNames);
            
            List<String> issuerAltNames = this.extractIssuerAlternativeNames(cert);
            altNames.addAll(issuerAltNames);
            
        } catch(CertificateParsingException e) {
            if(e.getCause() instanceof IOException) {
                this.report.addLine("Error extracting Claim: " + e.getMessage(), ReportStatus.FAILED);
            } else {
                EidasCertFormat.logger.error("", e);
            }
            EidasCertFormat.logger.warn(e.getMessage());
        }
        
        return altNames;
    }
    
    private List<String> extractSubjectAlternativeNames(X509Certificate cert) throws CertificateParsingException {
        List<String> values = new ArrayList<>();
        
        EidasCertFormat.logger.info("Looking for claims in SubjectAlternativeNames");
        Collection<List<?>> subjectAlternativeNames = null;
        subjectAlternativeNames = cert.getSubjectAlternativeNames();
        
        if(subjectAlternativeNames != null) {
            for(List<?> subjectAlternativeName : subjectAlternativeNames) {
                String altName = extractAltName(subjectAlternativeName);
                if(altName != null) {
                    values.add(altName);
                }
            }
        } else {
            EidasCertFormat.logger.info("No SubjectAlternativeNames found");
        }
        
        return values;
    }
    
    private List<String> extractIssuerAlternativeNames(X509Certificate cert) throws CertificateParsingException {
        List<String> values = new ArrayList<>();
        
        EidasCertFormat.logger.info("Looking for claims in IssuerAlternativeName");
        Collection<List<?>> issuerAlternativeNames = null;
        issuerAlternativeNames = cert.getIssuerAlternativeNames();
        
        if(issuerAlternativeNames != null) {
            for(List<?> issuerAlternativeName : issuerAlternativeNames) {
                String altName = extractAltName(issuerAlternativeName);
                if(altName != null) {
                    values.add(altName);
                }
            }
        } else {
            EidasCertFormat.logger.info("No IssuerAlternativeNames found");
        }
        
        return values;
    }
    
    private String extractAltName(List altNameFromCert) {
        String altName = null;
        Integer type = (Integer) altNameFromCert.get(0);
        
        if(type == GeneralName.dNSName) {
            altName = (String) altNameFromCert.get(1);
            EidasCertFormat.logger.info("Found: " + altName);
        }
        
        return altName;
    }
    
    @Override
    public String getFormatId() {
        return EidasCertFormat.FORMAT_ID;
    }
    
    @Override
    public void init() throws Exception {
        this.report.addLine("Signer certificate: " + TrustScheme.getFieldFromDN(this.signingCertificate.getSubjectX500Principal(), "CN"));
    }
    
    @Override
    public void setRootListener(TplApiListener listener) {
        
        this.rootListener = listener;
    }
    
    
}
