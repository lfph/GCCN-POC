package com.train.gccn.model.format.genericXML;

import eu.lightest.horn.specialKeywords.HornApiException;
import com.train.gccn.model.format.AbstractFormatParser;
import com.train.gccn.model.format.FormatParser;
import com.train.gccn.model.format.eIDAS_qualified_certificate.EidasCertFormat;
import com.train.gccn.model.report.Report;
import com.train.gccn.model.report.ReportStatus;
import com.train.gccn.model.transaction.ASiCSignature;
import com.train.gccn.model.transaction.TransactionContainer;
import com.train.gccn.model.transaction.TransactionFactory;
import com.train.gccn.wrapper.XMLUtil;
import org.apache.commons.text.WordUtils;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericXMLFormat extends AbstractFormatParser {
    
    private static final String PATH_CERT = "certificate";
    private static final String FILEPATH_FORM = "data.xml"; // Path to file inside container
    private static Logger logger = Logger.getLogger(GenericXMLFormat.class);
    private String format_id = "genericXML";
    private InputStream format_template = null;
    private TransactionContainer transaction = null;
    private XMLUtil xml;
    private XMLUtil template_xml;
    
    public GenericXMLFormat(Object transactionFile, Report report) {
        super(transactionFile, report);
        if(transactionFile instanceof File) {
            this.transaction = TransactionFactory.getTransaction((File) transactionFile);
        } else {
            throw new IllegalArgumentException("Transaction of type:" + transactionFile.getClass().toString() + ",  expected: File");
        }
    }
    
    public static GenericXMLFormat getParserForFormat(String formatId, Object transactionFile, Report report) {
        Map<String, InputStream> formats = GenericXMLFormat.getSupportedFormats();
    
        InputStream templateStream = null;
        if(formats.isEmpty()) {
            GenericXMLFormat.logger.info("No templates found via classpath search, trying directly by formatId ...");
            templateStream = GenericXMLFormat.getFormatById(formatId);
        } else {
            templateStream = formats.get(formatId);
        }
    
        if(templateStream == null) {
            GenericXMLFormat.logger.warn("No template for format " + formatId + " found.");
            return null;
        }
    
        GenericXMLFormat.logger.info("Template for format " + formatId + " found, initializing parser ...");
    
        GenericXMLFormat parser = new GenericXMLFormat(transactionFile, report);
        parser.setFormatId(formatId);
        parser.setFormatTemplate(templateStream);
    
        return parser;
    }
    
    public static InputStream getFormatById(String formatId) {
        InputStream formatResourceOrig = GenericXMLFormat.class.getResourceAsStream(formatId + ".xml");
        InputStream formatResourceLower = GenericXMLFormat.class.getResourceAsStream(formatId.toLowerCase() + ".xml");
        return formatResourceOrig != null ? formatResourceOrig : (formatResourceLower != null ? formatResourceLower : null);
    }
    
    public static Map<String, InputStream> getSupportedFormats() {
        Map<String, InputStream> formats = new HashMap<>();
        URL templateStorePath = GenericXMLFormat.class.getResource(".");
        if(templateStorePath == null) {
            GenericXMLFormat.logger.warn("Cannot load templateStorePath, probably not present in your classpath? Returning empty store.");
            return formats;
        }
        
        //File templateStore = new File("at/tugraz/iaik/lightest/verifier/model/format/genericXML");
        File templateStore = new File(templateStorePath.getFile());
        
        GenericXMLFormat.logger.info("GenericXML FormatParser TemplateStore: " + templateStore.getAbsolutePath());
        
        if(templateStore == null || !templateStore.exists() || templateStore.isFile()) {
            GenericXMLFormat.logger.error("GenericXML FormatParser TemplateStore store does not exist. Returning empty store.");
            return formats;
        }
        
        for(File templatePath : templateStore.listFiles()) {
            GenericXMLFormat.logger.info("Template? " + templatePath);
            
            if(!templatePath.isDirectory() && templatePath.getName().endsWith(".xml")) {
                try {
                    String format = GenericXMLFormat.getTemplateFormatId(templatePath);
                    InputStream templateStream = GenericXMLFormat.class.getResourceAsStream(templatePath.getName());
                    formats.put(format, templateStream);
    
                } catch(IOException | ParserConfigurationException | SAXException e) {
                    GenericXMLFormat.logger.error("", e);
                }
            }
        }
        
        return formats;
    }
    
    private static String getTemplateFormatId(File template) throws IOException, ParserConfigurationException, SAXException {
        String xml = new String(Files.readAllBytes(Paths.get(template.getPath())));
        XMLUtil util = new XMLUtil(xml);
        return util.getAttribute("specification");
        
    }
    
    private void setFormatTemplate(InputStream templateStream) {
        this.format_template = templateStream;
    }
    
    @Override
    public boolean onExtract(List<String> path, String query, List<String> output) throws HornApiException {
        GenericXMLFormat.logger.info("onExtract @ " + this.getFormatId());
        GenericXMLFormat.logger.info("  path:  " + String.join(".", path));
        GenericXMLFormat.logger.info("  query: " + query);
    
        String joinedPath = path.isEmpty() ? query : String.join(".", String.join(".", path), query);
        if(this.xml.isElement(joinedPath)) {
            return true;
        }
    
        if(path.size() == 0) {
            switch(query) {
                case GenericXMLFormat.PATH_CERT:
                case AbstractFormatParser.QUERY_FORMAT:
                    return true;
            }
            return false;
        }
    
        String parserId = path.get(0);
        GenericXMLFormat.logger.info("delegating to parser: " + parserId);
        return getParser(parserId).onExtract(pop(path), query, output);
    }
    
    
    @Override
    public boolean onPrint(PrintObj printObj) {
        GenericXMLFormat.logger.info("onPrint:");
        //printList("path", path);
    
        String element = this.xml.getElement(printObj.mPath);
        String name = WordUtils.capitalize(String.join(" ", printObj.mPath));
        this.report.addLine(name + ": " + element, ReportStatus.PRINT);
    
        return true;
    }
    
    @Override
    public boolean onVerifySignature(List<String> pathToSubject, List<String> pathToCert) throws HornApiException {
        
        if(pathToSubject.size() == 0) {
            
            ResolvedObj sigObj = this.rootListener.resolveObj(pathToCert);
            if(sigObj == null || !sigObj.mType.equals(EidasCertFormat.RESOLVETYPE_X509CERT) || !(sigObj.mValue instanceof X509Certificate)) {
                GenericXMLFormat.logger.error("Could not resolve certificate from " + String.join(".", pathToCert));
                this.report.addLine("Signature Verification failed: Certificate error.", ReportStatus.FAILED);
                return false;
            }
            
            X509Certificate cert = (X509Certificate) sigObj.mValue;
            GenericXMLFormat.logger.info("Verifying signature using cert: " + cert.getSubjectDN());
    
            for(ASiCSignature signature : this.transaction.getSignatures()) {
                if(signature.getSigningX509Certificate().equals(cert)) {
                    GenericXMLFormat.logger.info("Found signature for given cert.");
                    if(!this.transaction.verifySignature(cert, signature)) {
                        this.report.addLine("Signature Verification failed.", ReportStatus.FAILED);
                        return false;
                    }
                }
            }
            
            this.report.addLine("Signature Verification successful.");
            return true;
        } else if(pathToSubject.size() == 1) {
            String parserId = pathToSubject.get(0);
            GenericXMLFormat.logger.info("delegating to parser: " + parserId);
            return getParser(parserId).onVerifySignature(pop(pathToSubject), pathToCert);
        }
        
        GenericXMLFormat.logger.warn("Invalid path: " + String.join(".", pathToSubject));
        return false;
    }
    
    @Override
    public ResolvedObj resolveObj(List<String> path) {
        GenericXMLFormat.logger.info("resolveObj: " + String.join(".", path));
        
        if(path.size() > 1) {
            String parserId = path.get(0);
            FormatParser parser = getParser(parserId);
            
            return parser.resolveObj(pop(path));
        }
        
        switch(path.get(0)) {
            case GenericXMLFormat.PATH_CERT:
                return genResolvedObj(this.transaction.getSigningCertificate(), EidasCertFormat.RESOLVETYPE_X509CERT);
            case AbstractFormatParser.QUERY_FORMAT:
                return genResolvedObj(getFormatId(), "STRING");
        }
    
        return extractFromXML(path);
    }
    
    private ResolvedObj extractFromXML(List<String> path) {
        String pathAsString = String.join(".", path);
    
        GenericXMLFormat.logger.info("Extracting from template: " + pathAsString);
        String elementType = extractTypeFromTemplate(path);
        if(elementType == null) {
    
            GenericXMLFormat.logger.warn("Not found in template: " + pathAsString);
            this.report.addLine("Field not found in GenericXML template: " + pathAsString, ReportStatus.FAILED);
            return null;
        }
        
        String element = this.xml.getElement(path);
        if(element != null) {
            switch(elementType) {
                case "int":
                    Integer intElem = Integer.valueOf(element);
                    return genResolvedObj(intElem, "INT");
                case "string":
                    return genResolvedObj(element, "STRING");
                default:
                    GenericXMLFormat.logger.error("Unknown type found in template: " + elementType);
            }
        } else {
            GenericXMLFormat.logger.warn("Not found in transaction: " + pathAsString);
        }
        
        return null;
    }
    
    private String extractTypeFromTemplate(List<String> path) {
        return this.template_xml.getElementAttribute(path, "type");
    }
    
    @Override
    public String getFormatId() {
        return this.format_id;
    }
    
    public void setFormatId(String format_id) {
        this.format_id = format_id;
    }
    
    @Override
    public void init() throws Exception {
        String formXML = this.transaction.extractFileString(GenericXMLFormat.FILEPATH_FORM);
        if(formXML == null) {
            String firstXML = getFirstXML();
            formXML = this.transaction.extractFileString(firstXML);
    
            GenericXMLFormat.logger.warn("While parsing form: " + GenericXMLFormat.FILEPATH_FORM + " not found. Using first xml file found (" + firstXML + ") ...");
            
            if(formXML == null) {
                throw new Exception("Error while parsing form. Wrong format? (" + GenericXMLFormat.FILEPATH_FORM + " not found.)");
            }
        }
        
        this.xml = new XMLUtil(formXML);
        if(this.format_template != null) {
            this.template_xml = new XMLUtil(this.format_template);
        } else {
            GenericXMLFormat.logger.warn("No Format Template given before init(). No template parser initialized.");
        }
    }
    
    private String getFirstXML() {
        String firstXML = null;
        
        for(String file : this.transaction.getFileList()) {
            if(file.endsWith(".xml")) {
                firstXML = file;
                break;
            }
        }
        
        return firstXML;
    }
    
}
