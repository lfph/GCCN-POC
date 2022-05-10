package com.train.gccn.model.format;

import com.train.gccn.model.format.genericXML.GenericXMLFormat;
import com.train.gccn.model.report.Report;
import com.train.gccn.model.report.ReportStatus;
import org.apache.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class FormatParserFactory {
    
    private static HashMap<String, String> parsers = null; // key: formatid, value: fqcn
    private static Logger logger = Logger.getLogger(FormatParserFactory.class);
    
    
    private static void init() {
        // TODO: automate this using reflection
        //       https://extgit.iaik.tugraz.at/LIGHTest/AutomaticTrustVerifier/issues/5
    
        FormatParserFactory.parsers = new HashMap<>();
    
        FormatParserFactory.parsers.put("simpleContract", "eu.lightest.verifier.model.format.simpleContract.SCFormat");
        FormatParserFactory.parsers.put("peppolTransaction", "eu.lightest.verifier.model.format.peppolTransaction.PEPPOLTransactionFormat");
        FormatParserFactory.parsers.put("eIDAS_qualified_certificate", "eu.lightest.verifier.model.format.eIDAS_qualified_certificate.EidasCertFormat");
        FormatParserFactory.parsers.put("ssi", "eu.lightest.verifier.model.format.ssi.SSIFormat");
        FormatParserFactory.parsers.put("x509cert", "eu.lightest.verifier.model.format.eIDAS_qualified_certificate.EidasBasedX509CertFormat");
        FormatParserFactory.parsers.put("theAuctionHouse2019", "eu.lightest.verifier.model.format.theAuctionHouse2019.AH19Format");
        FormatParserFactory.parsers.put("delegationxml", "eu.lightest.verifier.model.format.Delegation.DelegationXMLFormat");
        FormatParserFactory.parsers.put("proxyCert", "eu.lightest.verifier.model.format.Delegation.DelegationXMLFormat");
        FormatParserFactory.parsers.put("delegation", "eu.lightest.verifier.model.format.Delegation.DelegationXMLFormat");
        FormatParserFactory.parsers.put("dp_entry", "eu.lightest.verifier.model.format.Delegation.DPFormat");
        FormatParserFactory.parsers.put("trustlist_entry", "eu.lightest.verifier.model.format.eIDAS_qualified_certificate.TslEntryFormat");
        //FormatParserFactory.parsers.put("theAuctionHouse2019format", "eu.lightest.verifier.model.format.theAuctionHouse2019.AH19Format");
        // FormatParserFactory.parsers.put("genericXML", "eu.lightest.verifier.model.format.genericXML.GenericXMLFormat");
        FormatParserFactory.parsers.put("pades", "eu.lightest.verifier.model.format.advancedDoc.PAdESFormat");
        FormatParserFactory.parsers.put("padesInAsic", "eu.lightest.verifier.model.format.advancedDoc.PAdESinASICFormat");
        FormatParserFactory.parsers.put("xades", "eu.lightest.verifier.model.format.advancedDoc.XAdESFormat");
        FormatParserFactory.parsers.put("xadesInAsic", "eu.lightest.verifier.model.format.advancedDoc.XAdESinASICFormat");
    
        FormatParserFactory.parsers.put("simpleFido", "eu.lightest.verifier.model.format.fido.simpleFidoFormat");
        FormatParserFactory.parsers.put("simpleFidoMapping", "eu.lightest.verifier.model.format.fido.fidoMappingFormat");
    }
    
    private static FormatParser getParser(String fqcn, Object transaction, Report report) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        // via Method 2 of https://stackoverflow.com/a/9886331/1518225
    
        FormatParserFactory.logger.info("Getting class ...");
        Class<?> clazz = Class.forName(fqcn);
    
        FormatParserFactory.logger.info("Getting ctor ...");
        Constructor<?> constructor = clazz.getConstructor(Object.class, Report.class);
    
        FormatParserFactory.logger.info("Creating instance ...");
        Object instance = constructor.newInstance(transaction, report);
    
        FormatParserFactory.logger.info("Creating parser done!");
        return (FormatParser) instance;
    }
    
    private static FormatParser getGenericParser(String formatId, Object transaction, Report report) {
        return GenericXMLFormat.getParserForFormat(formatId, transaction, report);
    }
    
    public static FormatParser get(String formatId, Object transaction, Report report) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if(FormatParserFactory.parsers == null) {
            FormatParserFactory.init();
            FormatParserFactory.logger.info("FormatParserFactory initialized " + FormatParserFactory.parsers.size() + " parsers: ");
            for(Map.Entry<String, String> parser : FormatParserFactory.parsers.entrySet()) {
                FormatParserFactory.logger.info(parser.getKey() + ": " + parser.getValue());
            }
        }
    
        FormatParserFactory.logger.info("Loading parser for id " + formatId);
        
        String fqcn = FormatParserFactory.parsers.get(formatId);
        if(fqcn != null) {
            FormatParserFactory.logger.info("Building parser " + fqcn);
            return FormatParserFactory.getParser(fqcn, transaction, report);
        }
    
        FormatParserFactory.logger.info("No static parser for format " + formatId + ", looking for a generic parser ...");
    
        FormatParser parser = FormatParserFactory.getGenericParser(formatId, transaction, report);
        if(parser != null) {
            return parser;
        }
    
        FormatParserFactory.logger.error("No FormatParser for formatId: " + formatId);
        report.addLine("No parser for " + formatId + " found.", ReportStatus.FAILED);
        //throw new ClassNotFoundException("No FormatParser for formatId: " + formatId);
        return null;
    }
    
    
}
