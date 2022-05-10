package com.train.gccn.model.translation;

import com.train.gccn.model.report.Report;
import com.train.gccn.model.trustscheme.GenericTslEntry;
import com.train.gccn.model.trustscheme.GenericTslEntryDecorator;
import com.train.gccn.model.trustscheme.TslEntry;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrustTranslation {
    
    private static Logger logger = Logger.getLogger(TrustTranslation.class);
    private static String XML_PREFIX = "trustlevel-translation-agreement";
    
    private final String content;
    private final String schemeTrusted; // = source/trusted/from policy, even if TTA in target mode
    private List<TrustTranslationAgreement> translationAgreements;
    
    public TrustTranslation(String translationContent, String schemeTrusted) throws IOException, SAXException, ParserConfigurationException {
        this.content = translationContent;
        this.schemeTrusted = schemeTrusted;
        this.translationAgreements = new ArrayList<>();
    }
    
    public void addTranslationAgreement(TrustTranslationAgreement agreement) {
        this.translationAgreements.add(agreement);
    }
    
    @Override
    public String toString() {
        return "TrustTranslation:" +
                "  content=\n" +
                this.content;
    }
    
    public TslEntry translate(TslEntry tslEntry, Report report) {
    
        if(this.translationAgreements.size() == 0) {
            TrustTranslation.logger.error("No Translation Agreements set. Did you forget to use TrustTranslationFactory to build this?");
        }
    
        TrustTranslationAgreement matchedAgreement = findMatchingAgreement(tslEntry);
        if(matchedAgreement == null) {
            TrustTranslation.logger.error("Cannot match TslEntry with Translation ...");
            report.addLine("Trust Translation failed: Not able to find Agreement that matches Transaction.");
            return null;
        }
    
        return translateEntry(tslEntry, matchedAgreement);
    }
    
    private boolean verifySchemeIds(TslEntry tslEntry, TrustTranslationAgreement matchedAgreement) {
        TrustTranslation.logger.info("Source:      " + matchedAgreement.getSourceName()); // = trusted in policy
        TrustTranslation.logger.info("Policy:      " + this.schemeTrusted); // = source/trusted/policy
        boolean sourceStatus = this.schemeTrusted.equals(matchedAgreement.getSourceName());
        
        TrustTranslation.logger.info("Target:      " + matchedAgreement.getTargetName()); // = claimed in transaction
        TrustTranslation.logger.info("Transaction: " + tslEntry.getSchemeId()); // == target/claimed/transaction
        boolean targetStatus = tslEntry.getSchemeId().equals(matchedAgreement.getTargetName());
        
        return sourceStatus && targetStatus;
    }
    
    private TslEntry translateEntry(TslEntry tslEntry, TrustTranslationAgreement matchedAgreement) {
        GenericTslEntry translatedEntry = new GenericTslEntryDecorator(tslEntry, matchedAgreement.getTargetName());
        
        translatedEntry.setAllFields(matchedAgreement.getSourceParams());
        
        return translatedEntry;
    }
    
    private TrustTranslationAgreement findMatchingAgreement(TslEntry tslEntry) {
        List<TrustTranslationAgreement> translationAgreements = this.translationAgreements;
        
        TrustTranslation.logger.info("Matching against " + this.translationAgreements.size() + " Translation Agreements ...");
        
        for(TrustTranslationAgreement translationAgreement : translationAgreements) {
            // Step 1: Find agreements that match given source/target
            //         (this could be done using a filter)
            boolean schemeIdsVerified = verifySchemeIds(tslEntry, translationAgreement);
            if(!schemeIdsVerified) {
                continue;
            }
    
            // Step 2: Check if all fields required by agreement are present in transaction
            boolean status = verifyAgreement(tslEntry, translationAgreement);
            if(status == true) {
                return translationAgreement;
            }
        }
        
        return null;
    }
    
    private boolean verifyAgreement(TslEntry tslEntry, TrustTranslationAgreement agreement) {
        boolean status = true;
        
        for(Map.Entry<String, String> param : agreement.getTargetParams().entrySet()) {
            String field = param.getKey();
            
            if(!tslEntry.fieldExists(field)) {
                TrustTranslation.logger.warn("Field not present in TSL Entry: " + field);
                status = false;
                continue;
            }
            
            String fieldValueEntry = tslEntry.getField(field);
            String fieldValueTransl = param.getValue();
            
            if(!fieldValueTransl.equals(fieldValueEntry)) {
                TrustTranslation.logger.warn("field: " + field + ", value: " + fieldValueEntry + ",  but expected: " + fieldValueTransl);
                status = false;
            }
        }
        
        return status;
    }
    
    
    public void log() {
        int i = 0;
        for(TrustTranslationAgreement translationAgreement : this.translationAgreements) {
            TrustTranslation.logger.info("Agreement #" + i + ": \n " + translationAgreement.toString());
            i++;
        }
    }
    
}
