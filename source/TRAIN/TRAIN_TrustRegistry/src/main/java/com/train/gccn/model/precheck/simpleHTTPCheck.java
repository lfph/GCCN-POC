package com.train.gccn.model.precheck;

import com.train.gccn.ATVConfiguration;
import com.train.gccn.model.report.Report;
import com.train.gccn.model.report.ReportStatus;
import com.train.gccn.wrapper.HTTPSHelper;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class simpleHTTPCheck implements Prechecker {
    
    private static Logger logger = Logger.getLogger(simpleHTTPCheck.class);
    
    @Override
    public boolean check(File transaction, File policy, Report report) {
        String checkURL = ATVConfiguration.get().getString("precheck.simpleHTTPCheck.url");
        String checkURLfail = "http://www.asdfasdf_unreachable.fail/";
        
        HTTPSHelper https = new HTTPSHelper();
        
        try {
            https.get(new URL(checkURL));
            
        } catch(IOException e) {
            report.addLine("Not able to reach internet. Network down?", ReportStatus.FAILED);
            simpleHTTPCheck.logger.error("", e);
            return false;
        }
        
        try {
            https.get(new URL(checkURLfail));
            report.addLine("Not able to reach internet. Network down?", ReportStatus.FAILED);
            return false;
            
        } catch(IOException e) {
            // all good, this is expected!
        }
        
        return true;
    }
    
    
}
