package com.train.gccn.model.transaction;

import com.train.gccn.ATVConfiguration;

import java.io.File;

public class TransactionFactory {
    
    public static boolean DD4J_ENABLED = ATVConfiguration.get().getBoolean("dd4j_enabled", true);
    
    public static TransactionContainer getTransaction(File transactionFile) {
        
        TransactionAbstractFactory factory;
        
        if(TransactionFactory.DD4J_ENABLED) {
            factory = new DD4JASiCTransactionFactory(transactionFile);
        } else {
            factory = new DSSASiCTransactionFactory(transactionFile);
        }
        
        TransactionContainer transaction = factory.createTransaction();
        
        return transaction;
    }
}
