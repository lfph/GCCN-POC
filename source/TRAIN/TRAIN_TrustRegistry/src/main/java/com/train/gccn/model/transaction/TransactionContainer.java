package com.train.gccn.model.transaction;

import eu.europa.esig.dss.validation.process.MessageTag;

import java.util.ArrayList;
import java.util.List;

public abstract class TransactionContainer implements SignedTransactionContainer {
    
    protected static boolean errorIgnored(String error) {
        List<String> ignoreList = new ArrayList<>();
    
    
        ignoreList.add(MessageTag.QUAL_TRUSTED_CERT_PATH_ANS.getMessage()); // chain verify not needed
        ignoreList.add(MessageTag.BBB_XCV_CCCBB_SIG_ANS.getMessage());
        ignoreList.add(MessageTag.QUAL_HAS_CAQC_ANS.getMessage()); // chain verify not needed
        ignoreList.add(MessageTag.QUAL_HAS_CERT_TYPE_COVERAGE_ANS.getMessage()); // chain verify not needed
    
        // TODO: this is a temporary measurement.
        //       (remove this after we figure out LTV etc.)
        //       https://extgit.iaik.tugraz.at/LIGHTest/AutomaticTrustVerifier/issues/5
        ignoreList.add(MessageTag.ARCH_LTVV_ANS.getMessage()); // LTV
    
        for(String msg : ignoreList) {
            if(msg.equals(error)) {
                return true;
            }
        }
        
        return false;
    }
    
    public String getTransactionType() {
        return "Base Transaction";
    }
    
    //public abstract void setTrustList(String trustList);

//    public abstract DataFile extractFile(String path);
    
    @Override
    public String toString() {
        return "Transaction type is " + getTransactionType();
    }
    
    public abstract byte[] extractFileBytes(String path);
    
    public String extractFileString(String path) {
        byte[] b = this.extractFileBytes(path);
        return (b != null) ? new String(b) : null;
    }
    
    public abstract List<String> getFileList();
}
