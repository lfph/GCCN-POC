package com.train.gccn.model.translation;

import com.train.gccn.model.report.Report;
import com.train.gccn.wrapper.XMLUtil;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class TrustTranslationFactory {
    
    
    public static final String POINTER_PREFIX = "_translation._trust";
    private static final String XML_TRANSLATION_AGREEMENT = "trustlevel-translation-agreement";
    
    private static Logger logger = Logger.getLogger(TrustTranslationFactory.class);
    
    public static TrustTranslation createTranslation(TranslationPointer pointer, Report report) throws IOException {
        
        //String translationDoc = TrustDiscoveryWrapper.loadAndVerify(pointer.getPointer(), report);
        String translationDoc = null;
        if(translationDoc == null) {
            return null;
        }
        
        try {
            return TrustTranslationFactory.buildTranslation(translationDoc, report, null, null);
        } catch(SAXException | ParserConfigurationException e) {
            TrustTranslationFactory.logger.error("", e);
            return null;
        }
    }
    
    public static TrustTranslation buildTranslation(String translationDoc, Report report, String schemeClaimed, String schemeTrusted) throws ParserConfigurationException, SAXException, IOException {
        TrustTranslation translation = new TrustTranslation(translationDoc, schemeTrusted);
        XMLUtil translationXML = new XMLUtil(translationDoc);
        
        NodeList nodeList = translationXML.getNodesAsMap(TrustTranslationFactory.XML_TRANSLATION_AGREEMENT);
        
        TrustTranslationFactory.logger.info("Parsing " + nodeList.getLength() + " translation agreements ...");
        
        for(int i = 0; i < nodeList.getLength(); i++) {
            Node agreementNode = nodeList.item(i);
    
            TrustTranslationAgreement agreement = TrustTranslationFactory.genTranslationAgreement(translationXML, agreementNode, schemeClaimed);
            if(agreement != null) {
                translation.addTranslationAgreement(agreement);
            }
        }
        
        return translation;
    }
    
    private static TrustTranslationAgreement genTranslationAgreement(XMLUtil translationXML, Node agreementNode, String transactionSchemeId) {
        TrustTranslationAgreement agreement = new TrustTranslationAgreement();
        
        if(transactionSchemeId != null &&
                !TrustTranslationFactory.verifySchemeId(transactionSchemeId, translationXML, agreementNode)) {
            return null;
        }
        
        agreement.setName(translationXML.getElement("name", agreementNode));
        
        // TODO dates, duration, status
        
        agreement.setSourceName(translationXML.getElement("source.scheme-name", agreementNode));
        agreement.setSourceProvider(translationXML.getElement("source.provider", agreementNode));
        agreement.setSourceLevel(translationXML.getElement("source.level", agreementNode));
        agreement.addSourceParams(translationXML.getNodesAsMap("source.params.param", "name", "value", agreementNode));
        
        agreement.setTargetName(translationXML.getElement("target.scheme-name", agreementNode));
        agreement.setTargetProvider(translationXML.getElement("target.provider", agreementNode));
        agreement.setTargetLevel(translationXML.getElement("target.level", agreementNode));
        agreement.addSourceParams(translationXML.getNodesAsMap("target.params.param", "name", "value", agreementNode));
        
        return agreement;
    }
    
    private static boolean verifySchemeId(String transactionSchemeId, XMLUtil translationXML, Node agreementNode) {
        String targetSchemeId = translationXML.getElement("target.scheme-name", agreementNode).trim();
        
        if(transactionSchemeId.equals(targetSchemeId)) {
            return true;
        }
        
        TrustTranslationFactory.logger.warn("Expected scheme " + transactionSchemeId + ", but found " + targetSchemeId);
        return false;
    }
    
}
