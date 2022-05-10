package com.train.gccn.model.format;

import eu.lightest.horn.specialKeywords.HornApiException;
import com.train.gccn.model.format.eIDAS_qualified_certificate.EidasCertFormat;
import com.train.gccn.model.report.Report;
import com.train.gccn.model.report.ReportStatus;
import com.train.gccn.model.tpl.TplApiListener;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractFormatParser implements FormatParser {
    
    public static final String RESOLVETYPE_HTTP_URL = "HTTP_URL";
    public static final String RESOLVETYPE_XML_CONTENT = "XML_CONTENT";
    public static final String PATH_LOOKEDUP_DOC = "lookedup";
    protected static final String QUERY_FORMAT = "format";
    private static Logger logger = Logger.getLogger(AbstractFormatParser.class);
    private final Object transactionData;
    protected Report report = null;
    protected TplApiListener rootListener = null;
    private HashMap<String, FormatParser> data = null;
    private HashMap<String, ResolvedObj> resolveCache = null;
    
    public AbstractFormatParser(Object transaction, Report report) {
        this.data = new HashMap<>();
        this.resolveCache = new HashMap<>();
        this.report = report;
        this.transactionData = transaction;
    }
    
    protected void cacheResolvedObj(List<String> path, Object value, String type) {
        this.cacheResolvedObj(String.join(".", path), value, type);
    }
    
    protected void cacheResolvedObj(String path, Object value, String type) {
        AbstractFormatParser.logger.info("cacheResolvedObj @ " + this.getFormatId() + ":");
        AbstractFormatParser.logger.info("   path: " + path);
        
        ResolvedObj resolvedObj = genResolvedObj(value, type);
        this.resolveCache.put(path, resolvedObj);
    }
    
    protected ResolvedObj getCachedResolvedObj(List<String> path) {
        return getCachedResolvedObj(String.join(".", path));
    }
    
    protected ResolvedObj getCachedResolvedObj(List<String> path, String expectedType) {
        ResolvedObj cachedResolvedObj = this.getCachedResolvedObj(path);
        if(cachedResolvedObj != null && !cachedResolvedObj.mType.equals(expectedType)) {
            AbstractFormatParser.logger.error("Could not resolve obj. Wrong type. Expected: " + expectedType + ", loaded: " + cachedResolvedObj.mType);
            return null;
        }
        return cachedResolvedObj;
    }
    
    protected ResolvedObj getCachedResolvedObj(String path) {
        ResolvedObj resolvedObj = this.resolveCache.get(path);
        if(resolvedObj == null) {
            AbstractFormatParser.logger.error("Could not resolve obj from path " + path);
            return null;
        }
        return resolvedObj;
    }
    
    protected ResolvedObj genResolvedObj(Object value, String type) {
        //AbstractFormatParser.logger.info("genResolvedObj: ");
        //AbstractFormatParser.logger.info("   type: " + type);
        //AbstractFormatParser.logger.info("   value: " + value.toString());
        return new ResolvedObj(value, type);
    }
    
    @Override
    public boolean onExtract(List<String> path, String query, List<String> output) throws HornApiException {
        AbstractFormatParser.logger.info("onExtract1 @ " + this.getFormatId() + ":");
        printList("path", path);
        AbstractFormatParser.logger.info("   query: " + query);
    
        if(path.size() == 0 && query.equals(AbstractFormatParser.QUERY_FORMAT)) {
            return true;
        }
    
        if(path.size() <= 0) {
            AbstractFormatParser.logger.error("No input path provided, I am not sure what to do.");
            return false;
        }
        
        output.addAll(path);
        output.add(query);
        
        String parserId = path.get(0);
        FormatParser parser = getParser(parserId);
    
        if(parser == null) {
            return false;
        }
    
        AbstractFormatParser.logger.info("Delegating to parser at path: " + parserId);
        boolean status = parser.onExtract(pop(path), query, output);
        
        printList("output", output);
        
        if(status == false) {
    
            AbstractFormatParser.logger.error("Path " + String.join(".", path) + "." + query + " not available in this format.");
            this.report.addLine("Error in policy: Field '" + query + "' does not exist in transaction.", ReportStatus.FAILED);
        }
        return status;
    }
    
    @Override
    public boolean setFormat(List<String> input, String format) {
    
        AbstractFormatParser.logger.info("setFormat @ " + this.getFormatId() + ":");
        printList("input", input);
        AbstractFormatParser.logger.info("   format: " + format);
        
        if(input.size() > 1) {
            String parserId = input.get(0);
            AbstractFormatParser.logger.info("Delegating to " + parserId + " ...");
            FormatParser parser = getParser(parserId);
            if(parser == null) {
                return false;
            }
    
            return parser.setFormat(pop(input), format);
        }
    
        return extractFormat(input, format);
    }
    
    protected boolean extractFormat(List<String> path, String format) {
        
        AbstractFormatParser.logger.info("  >> check if format of input is " + format);
    
        Object formData = this.resolveObj(path).mValue;
    
        if(formData == null) {
            this.report.addLine("Could not load " + String.join(".", path) + ".");
            AbstractFormatParser.logger.error("Could not load " + String.join(".", path));
            return false;
        }
    
        // let's try to get a instance of this format ...
        FormatParser parser = null;
        try {
            parser = FormatParserFactory.get(format, formData, this.report);
        } catch(ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | IllegalArgumentException e) {
            AbstractFormatParser.logger.error("Error from FormatParserFactory:", e);
    
            this.report.addLine(format + "format extraction failed.", ReportStatus.FAILED);
            return false;
        }
    
        if(parser == null) {
            return false;
        }
        
        // no exception -> parser exists. so let's try to parse the transaction ...
        try {
            parser.init();
        } catch(Exception e) {
            AbstractFormatParser.logger.error("Error during " + parser.getFormatId() + " parser initialization: " + e.toString());
            this.report.addLine("" + e.getMessage() + " during format extraction.", ReportStatus.FAILED);
            return false;
        }
        
        ((AbstractFormatParser) parser).setRootListener(this.rootListener);
        addParser(String.join(".", path), parser);
        
        AbstractFormatParser.logger.info("Format extraction of '" + parser.getFormatId() + "' at path '" + path + "' successful!");
        this.report.addLine(parser.getFormatId() + " extraction successful.");
        return true;
    }
    
    @Override
    public boolean onVerifySignature(List<String> pathToSubject, List<String> pathToCert) throws HornApiException {
        AbstractFormatParser.logger.info("onVerifySignature @ " + this.getFormatId() + ":");
        printList("pathToSubject", pathToSubject);
        printList("pathToCert", pathToCert);
    
        if(pathToSubject.size() == 0) {
        
            return false;
        }
    
        String parserId = pathToSubject.get(0);
        AbstractFormatParser.logger.info("Delegating to " + parserId + " ...");
        FormatParser parserSubject = getParser(parserId);
    
        return parserSubject.onVerifySignature(pop(pathToSubject), pathToCert);
    }
    
    @Override
    public boolean onVerifyHash(List<String> object, List<String> hash) throws HornApiException {
        AbstractFormatParser.logger.info("onVerifyHash @ " + this.getFormatId() + ":");
        printList("object", object);
        printList("hash", hash);
    
        String parserId = object.get(0);
        AbstractFormatParser.logger.info("Delegating to " + parserId + " ...");
        FormatParser parserSubject = getParser(parserId);
    
        return parserSubject.onVerifyHash(pop(object), hash);
    
    }
    
    @Override
    public boolean onTrustschemeCheck(List<String> pathToClaim, String schemeId) throws HornApiException {
        AbstractFormatParser.logger.info("onTrustschemeCheck:");
        printList("claim", pathToClaim);
        AbstractFormatParser.logger.info("  schemeId: " + schemeId);
        
        String parserId = pathToClaim.get(0);
        AbstractFormatParser.logger.info("Delegating to " + parserId + " ...");
        FormatParser parserSubject = getParser(parserId);
        
        return parserSubject.onTrustschemeCheck(pop(pathToClaim), schemeId);
    }
    
    @Override
    public boolean onLookup(List<String> pathToDomain, List<String> pathToLoadedDoc) throws HornApiException {
        AbstractFormatParser.logger.info("onLookup @ " + this.getFormatId() + ":");
        printList("pathToDomain", pathToDomain);
    
        FormatParser parser = getParser(pathToDomain, false);
        if(parser != null) {
            pathToLoadedDoc.add(pathToDomain.get(0));
            return parser.onLookup(pop(pathToDomain), pathToLoadedDoc);
        }
    
        // not needed to use resolvObj here, since we used pathToDomain to find the parser
        ResolvedObj cachedResolvedObj = this.getCachedResolvedObj(pathToDomain, EidasCertFormat.RESOLVETYPE_HTTP_URL);
        if(cachedResolvedObj == null) {
            // if we did not cache it yet, we might need to load it directly
            cachedResolvedObj = this.resolveObj(pathToDomain);
            if(cachedResolvedObj == null) {
                return false;
            }
        }
        String discoveryPointer = (String) cachedResolvedObj.mValue;
        AbstractFormatParser.logger.info("Looking up " + discoveryPointer + " ...");
    
        //String document = TrustDiscoveryWrapper.loadAndVerify(discoveryPointer, this.report);
        String document = null;
        if(document == null) {
            AbstractFormatParser.logger.error("Loading of document failed from " + discoveryPointer);
            //document = "Demo blabla resolved from " + discoveryPointer;
            return false;
        }
    
        String resolvKey = AbstractFormatParser.PATH_LOOKEDUP_DOC + "_" + String.join("_", pathToDomain);
        cacheResolvedObj(resolvKey, document, AbstractFormatParser.RESOLVETYPE_XML_CONTENT);
    
        pathToLoadedDoc.add(resolvKey);
        //pathToLoadedDoc.add(AbstractFormatParser.PATH_LOOKEDUP_DOC);
        //pathToLoadedDoc.addAll(pathToDomain);
    
        printList("pathToLoadedDoc", pathToLoadedDoc);
        return true;
    }
    
    @Override
    public boolean onTrustlist(List<String> pathToClaim, List<String> pathToCert, List<String> outputPathToEntry) throws HornApiException {
        AbstractFormatParser.logger.info("onTrustlist:");
        printList("pathToClaim", pathToClaim);
        printList("pathToCert", pathToCert);
        printList("outputPathToEntry", outputPathToEntry);
    
        FormatParser parser = getParser(pathToClaim);
        AbstractFormatParser.logger.info("calling onTrustlist on " + parser.getClass().getSimpleName());
    
        return parser.onTrustlist(pop(pathToClaim), pathToCert, outputPathToEntry);
    }
    
    
    @Override
    public boolean onPrint(PrintObj printObj) {
        switch(printObj.mType) {
            case Obj:
                AbstractFormatParser.logger.info("onPrint Obj: " + String.join(".", printObj.mPath));
                if(printObj.mPath.isEmpty()) {
                    return false;
                }
                printList("path", printObj.mPath);
                FormatParser parser = getParser(printObj.mPath);
                printObj.mPath = pop(printObj.mPath);
                return parser.onPrint(printObj);
            case Str:
                AbstractFormatParser.logger.info("onPrint Str: " + String.join(".", printObj.mValue));
                String value = printObj.mValue;
                value = value.replace("_", " ");
                value = StringUtils.capitalize(value);
                this.report.addLine(value, ReportStatus.PRINT);
                return true;
        }
        return true;
    }
    
    @Override
    public ResolvedObj resolveObj(List<String> path) {
        AbstractFormatParser.logger.info("resolveObj @ " + this.getFormatId() + ":");
        printList("path", path);
    
        if(path.size() == 1 && path.get(0).equals("input")) {
            // special case, its me!
            return genResolvedObj(this.transactionData, "FILE");
        
        } else if(path.size() == 1 && path.get(0).equals(AbstractFormatParser.QUERY_FORMAT)) {
            return genResolvedObj(getFormatId(), "STRING");
        }
    
        FormatParser parser = getParser(path);
    
        ResolvedObj out = parser.resolveObj(pop(path));
    
        if(out == null) {
            AbstractFormatParser.logger.error("Path " + String.join(".", path) + " not available in this format.");
        }
        return out;
    }
    
    @Override
    public boolean onTranslate(List<String> translationEntryPath, List<String> trustListEntryPath, List<String> trustedTrustListEntryPath) throws HornApiException {
        AbstractFormatParser.logger.info("onTranslate @ " + this.getFormatId() + ":");
        printList("translationEntry path:", translationEntryPath);
        printList("trustListEntry path:", trustListEntryPath);
    
        String parserId = translationEntryPath.get(0);
        AbstractFormatParser.logger.info("Delegating to " + parserId + " ...");
        FormatParser parserSubject = getParser(parserId);
    
        trustedTrustListEntryPath.add(translationEntryPath.get(0));
    
        return parserSubject.onTranslate(pop(translationEntryPath), trustListEntryPath, trustedTrustListEntryPath);
    }
    
    @Override
    public boolean onEncodeTranslationDomain(List<String> pathToClaim, String trustedScheme, List<String> ttaDomain) throws HornApiException {
        AbstractFormatParser.logger.info("onEncodeTranslationDomain @ " + this.getFormatId() + ":");
        printList("claim path:", pathToClaim);
        AbstractFormatParser.logger.info("  trustedScheme: " + trustedScheme);
    
        if(pathToClaim.size() <= 0) {
            return false;
        }
    
        String parserId = pathToClaim.get(0);
        AbstractFormatParser.logger.info("Delegating to " + parserId + " ...");
        FormatParser parserSubject = getParser(parserId);
    
        ttaDomain.add(pathToClaim.get(0));
        // every parser adds its path before delegating the request
        // e.g.
        // pathToClaim = input.certificate.issuer.trustScheme
        // trustedScheme = fantasyland_qualified  (fantasyland.lightest.nlnetlabs.eu)
        // -> in the end, EidasCertFormat builds the transl domain, and ttaDomain contains:
        // input.certificate.translation.eidas.lightest.nlnetlabs.eu._translate._trust.fantasyland.lightest.nlnetlabs.eu
        // -> this allows resolveObj of EidasCertFormat to return
        // eidas.lightest.nlnetlabs.eu._translate._trust.fantasyland.lightest.nlnetlabs.eu
    
        return parserSubject.onEncodeTranslationDomain(pop(pathToClaim), trustedScheme, ttaDomain);
    }
    
    protected List<String> pop(List list) {
        List<String> newlist = new ArrayList(list);
        newlist.remove(0);
        return newlist;
    }
    
    protected void printList(String listname, List<String> list) {
        String path = String.join(".", list);
        AbstractFormatParser.logger.info("  " + listname + " (" + list.size() + "): " + path);
    }
    
    protected void addParser(String path, FormatParser parser) {
        if(path.contains(".")) {
            AbstractFormatParser.logger.warn("Adding parser for multiple levels (" + path + "). Are you sure?");
        }
        
        this.data.put(path, parser);
    }
    
    protected FormatParser getParser(List<String> path) {
        return getParser(path.get(0));
    }
    
    protected FormatParser getParser(List<String> path, boolean allowMultiLevel) {
        if(allowMultiLevel) {
            return getParser(String.join(".", path));
        } else {
            return getParser(path);
        }
    }
    
    protected FormatParser getParser(String path) {
        if(path.contains(".")) {
            AbstractFormatParser.logger.warn("Getting parser for multiple levels (" + path + "). Are you sure?");
        }
    
        FormatParser parser = this.data.get(path);
        if(parser == null) {
            AbstractFormatParser.logger.error("No parser for path: " + path);
        }
        return parser;
    }
    
    protected void setRootListener(TplApiListener rootListener) {
        this.rootListener = rootListener;
    }
}
