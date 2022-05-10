package com.train.gccn.model.format.eIDAS_qualified_certificate;

import eu.lightest.horn.specialKeywords.HornApiException;
import com.train.gccn.model.format.AbstractFormatParser;
import com.train.gccn.model.report.Report;
import com.train.gccn.model.trustscheme.TslEntry;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Format parser for
 * eIDAS Trust Status List (TSL) Entry.
 * Tracked at https://extgit.iaik.tugraz.at/LIGHTest/AutomaticTrustVerifier/issues/26
 */
public class TslEntryFormat extends AbstractFormatParser {
    
    private static final String PATH_PUBKEY = "pubKey";
    private static final String FORMAT_ID = "trustlist_entry";
    private static Logger logger = Logger.getLogger(TslEntryFormat.class);
    private final TslEntry entry;
    
    public TslEntryFormat(Object transaction, Report report) {
        super(transaction, report);
        
        if(transaction instanceof TslEntry) {
            this.entry = (TslEntry) transaction;
        } else {
            TslEntryFormat.logger.error("Transaction of type:" + transaction.getClass().toString() + ",  expected: TslEntry");
            throw new IllegalArgumentException("Transaction of type:" + transaction.getClass().toString() + ",  expected: TslEntry");
        }
    }
    
    @Override
    public String getFormatId() {
        return TslEntryFormat.FORMAT_ID;
    }
    
    @Override
    public boolean onExtract(List<String> path, String query, List<String> output) throws HornApiException {
        TslEntryFormat.logger.info("onExtract1 @ " + this.getFormatId() + ":");
        printList("path", path);
        TslEntryFormat.logger.info("query: " + query);
        
        if(path.size() == 0) {
            switch(query) {
                case AbstractFormatParser.QUERY_FORMAT:
                    return true;
            }
        }
    
        if(query.equals(TslEntryFormat.PATH_PUBKEY)) {
            return this.entry != null;
        } else {
            boolean found = this.entry != null && this.entry.fieldExists(query);
            TslEntryFormat.logger.info(found ? "TSLEntry found & Field exists!" : "TSLEntry not found or Field does not exist.");
            return found;
        }
    }
    
    @Override
    public ResolvedObj resolveObj(List<String> path) {
        TslEntryFormat.logger.info("resolveObj @ " + this.getFormatId() + ":");
        printList("path", path);
        
        String query = String.join(".", path);
        switch(query) {
            case AbstractFormatParser.QUERY_FORMAT:
                return genResolvedObj(getFormatId(), "STRING");
            case TslEntryFormat.PATH_PUBKEY:
                return this.genResolvedObj(this.entry.getCertificate(), EidasCertFormat.RESOLVETYPE_X509CERT);
        }
        
        String value = this.entry.getField(query);
        ResolvedObj retval = value == null ? null : this.genResolvedObj(value, "STRING");
        TslEntryFormat.logger.info(retval == null ? "returning null" : ("returning " + retval.mType + ": " + retval.mValue));
        return retval;
    }
    
    
    @Override
    public void init() throws Exception {
        TslEntryFormat.logger.info("init(): nothing to do.");
    }
}
