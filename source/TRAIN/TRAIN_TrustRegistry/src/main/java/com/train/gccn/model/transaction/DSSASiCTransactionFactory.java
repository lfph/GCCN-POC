package com.train.gccn.model.transaction;

import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.FileDocument;
import eu.europa.esig.dss.asic.*;
import org.apache.log4j.Logger;

import java.io.File;

public class DSSASiCTransactionFactory implements TransactionAbstractFactory {
    
    private static Logger logger = Logger.getLogger(DSSASiCTransactionFactory.class);
    private final File containerFile;
    private String transactionType;
    
    public DSSASiCTransactionFactory(File transactionFile) {
        this.containerFile = transactionFile;
    }
    
    @Override
    public TransactionContainer createTransaction() {
        DSSDocument document = new FileDocument(this.containerFile);
        
        if(!ASiCUtils.isASiCMimeType(document.getMimeType())) {
            throw new IllegalArgumentException("Invalid Mime Type: " + document.getMimeType());
        }
        
        AbstractASiCContainerExtractor extractor = getArchiveExtractor(document);
        ASiCExtractResult containerContent = extractor.extract();
        
        return new DSSASiCTransaction(document, containerContent);
    }
    
    
    private AbstractASiCContainerExtractor getArchiveExtractor(DSSDocument container) {
        boolean cades = ASiCUtils.isArchiveContainsCorrectSignatureFileWithExtension(container, "p7s");
        boolean xades = ASiCUtils.isArchiveContainsCorrectSignatureFileWithExtension(container, "xml");
        
        if(cades) {
            DSSASiCTransactionFactory.logger.info("Returning ASiC with CAdES (CMS) container ...");
            this.transactionType = "ASiC-CAdES";
            return new ASiCWithCAdESContainerExtractor(container);
            
        } else if(xades) {
            DSSASiCTransactionFactory.logger.info("Returning ASiC with XAdES (XML) container ...");
            this.transactionType = "ASiC-XAdES";
            return new ASiCWithXAdESContainerExtractor(container);
            
        } else {
            throw new IllegalArgumentException("Invalid container, neither XAdES nor CAdES.");
        }
    }
    
}
