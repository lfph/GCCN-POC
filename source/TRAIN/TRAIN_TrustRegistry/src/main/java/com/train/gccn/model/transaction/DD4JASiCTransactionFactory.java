package com.train.gccn.model.transaction;

import org.digidoc4j.Container;
import org.digidoc4j.ContainerBuilder;

import java.io.File;

public class DD4JASiCTransactionFactory implements TransactionAbstractFactory {
    
    private final File containerFile;
    
    public DD4JASiCTransactionFactory(File transactionFile) {
        this.containerFile = transactionFile;
    }
    
    @Override
    public TransactionContainer createTransaction() {
        Container container = ContainerBuilder.
                aContainer().
                fromExistingFile(this.containerFile.getAbsolutePath()).
                build();
        
        if(!container.getType().startsWith("ASIC")) {
            throw new IllegalArgumentException("Container not ASIC, but " + container.getType() + " (or ASIC-CAdES, which is not supported here; try disabling DD4J).");
        }
        
        return new DD4JASiCTransaction(container);
    }
}
