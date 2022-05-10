package com.train.gccn.model.trustscheme;

import com.train.gccn.ATVConfiguration;
import com.train.gccn.exceptions.DNSException;
import com.train.gccn.model.report.Report;
import com.train.gccn.model.report.ReportStatus;
import com.train.gccn.wrapper.DNSHelper;
import com.train.gccn.wrapper.HTTPSHelper;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class TrustSchemeFactory {
    
    public static final String CLAIM_PREFIX = "_scheme._trust";
    private static Logger logger = Logger.getLogger(TrustSchemeFactory.class);
    private static DNSHelper dns;
    
    
    public static TrustScheme createTrustScheme(TrustSchemeClaim claim, Report report) throws IOException, DNSException {
        TrustSchemeFactory.dns = new DNSHelper();
    
        String schemeHostname = TrustSchemeFactory.discoverTrustScheme(claim, report);
    
        if(schemeHostname != null) {
            //report.addLine("TrustScheme discovered.", ReportStatus.OK);
            TrustSchemeFactory.logger.info("Found trust scheme: " + schemeHostname);
        } else {
            report.addLine("Trust Scheme discovery failed for claim " + claim + "!", ReportStatus.FAILED);
            return null;
        }
    
        String tslLocation = TrustSchemeFactory.discoverTrustList(schemeHostname, report);
    
        if(tslLocation != null) {
            //report.addLine("TrustList discovered.", ReportStatus.OK);
            TrustSchemeFactory.logger.info("Found trust list: " + tslLocation);
        } else {
            report.addLine("Trust Status List discovery failed for scheme " + schemeHostname + "!", ReportStatus.FAILED);
            return null;
        }
    
        String tslContent = TrustSchemeFactory.loadTrustList(schemeHostname, tslLocation, report);
    
        if(tslContent != null) {
            report.addLine("Trust Status List discovered & loaded.", ReportStatus.OK);
        } else {
            report.addLine("Trust Status List loading failed from URL " + tslLocation, ReportStatus.FAILED);
            return null;
        }
    
        if(ATVConfiguration.get().getBoolean("dane_verification_enabled") == true) {
            boolean tslValid = false;
        
            if(tslValid == true) {
                report.addLine("Trust Status List Signature validation successful.", ReportStatus.OK);
            } else {
                report.addLine("Trust Status List Signature validation failed.", ReportStatus.FAILED);
                return null;
            }
        } else {
            TrustSchemeFactory.logger.warn("Trust Status List Signature validation disabled. ");
        }
    
        TrustScheme scheme = new TrustScheme(tslLocation, schemeHostname, tslContent);
        
        return scheme;
    }
    
   /* private static boolean verifyTrustList(String schemeHostname, String tslContent) throws IOException {
        return SMIMEAHelper.verifyXMLdocument(schemeHostname, tslContent);
        
    } */
    
    private static String loadTrustList(String schemeHostname, String tslLocation, Report report) {
        try {
            System.out.println("tsl_location" + tslLocation);
            HTTPSHelper https = new HTTPSHelper();
            String trustlist = https.get(new URL(tslLocation));
            //System.out.println("trust_list" + trustlist);
            return trustlist;
    
        } catch(IOException e) {
            TrustSchemeFactory.logger.error("Error loading Trust Status List: " + e.getMessage());
            report.addLine("Error loading Trust Status List: " + e.getMessage(), ReportStatus.FAILED);
            return null;
        }
    }
    
    private static String discoverTrustScheme(TrustSchemeClaim claim, Report report) {
        TrustSchemeFactory.logger.info("Discovering TrustScheme for Claim: " + claim);
        String hostname = TrustSchemeFactory.buildHostname(claim);
        
        String scheme_identifier = null;
        List<String> schemes = null;
        try {
            schemes = TrustSchemeFactory.dns.queryPTR(hostname);

            int numSchemes = schemes.size();
        
            if(numSchemes <= 0) {
                TrustSchemeFactory.logger.info("found no schemes for this claim ...");
                report.addLine("Found no Trust Scheme for given Claim.", ReportStatus.FAILED);
                return null;
            }
            
            for(String scheme : schemes) {
                TrustSchemeFactory.logger.info("found trust scheme: " + schemes);
                report.addLine("Found Trust Scheme:" + scheme, ReportStatus.OK);
            }
            
            if(numSchemes > 1) {
                TrustSchemeFactory.logger.warn(numSchemes + " schemes found, but currently only 1 supported. Returning first ...");
            }
        } catch(IOException | DNSException e) {
            TrustSchemeFactory.logger.error("Error discovering trust scheme: " + e.getMessage());
            report.addLine("Error discovering Trust Scheme: " + e.getMessage(), ReportStatus.FAILED);
            return null;
        }
        
        // int numSchemes = schemes.size();
        
        // if(numSchemes <= 0) {
        //     TrustSchemeFactory.logger.info("found no schemes for this claim ...");
        //     report.addLine("Found no Trust Scheme for given Claim.", ReportStatus.FAILED);
        //     return null;
        // }
        
        // for(String scheme : schemes) {
        //     TrustSchemeFactory.logger.info("found trust scheme: " + schemes);
        // }
        
        // if(numSchemes > 1) {
        //     TrustSchemeFactory.logger.warn(numSchemes + " schemes found, but currently only 1 supported. Returning first ...");
        // }
        
        return schemes.get(0);
    }
    
    private static String buildHostname(TrustSchemeClaim claim) {
        String claimhost = claim.getClaim();
        if(claimhost.startsWith(TrustSchemeFactory.CLAIM_PREFIX)) {
            return claimhost;
        } else {
            return TrustSchemeFactory.CLAIM_PREFIX + "." + claimhost;
        }
    }
    
    private static String discoverTrustList(String schemeHostname, Report report) {
        TrustSchemeFactory.logger.info("Discovering TrustList for Scheme: " + schemeHostname);
        
        String tsl_location = null;
        List<String> lists = null;
        try {
            lists = TrustSchemeFactory.dns.queryURI(schemeHostname);
        } catch(IOException | DNSException e) {
            TrustSchemeFactory.logger.error("Error discovering Trust Status List: " + e.getMessage());
            report.addLine("Error discovering Trust Status List: " + e.getMessage(), ReportStatus.FAILED);
            return null;
        }
        
        int numLists = lists.size();
        
        if(numLists <= 0) {
            TrustSchemeFactory.logger.info("found no trust lists for this scheme ...");
            report.addLine("Found no Trust Status List for given Trust Scheme.", ReportStatus.FAILED);
            return null;
        }
        
        for(String tsl : lists) {
            TrustSchemeFactory.logger.info("found list: " + tsl);
            report.addLine("Found Trust List:" + tsl, ReportStatus.OK);
        }
        
        if(numLists > 1) {
            TrustSchemeFactory.logger.warn(numLists + " trust lists found, but currently only 1 supported. Returning first ...");
        }
        
        return lists.get(0);
    }
}
