package com.train.gccn.model.tpl;

import com.train.gccn.model.format.AbstractFormatParser;
import com.train.gccn.model.report.Report;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TplApiListener extends AbstractFormatParser {
    
    private static Logger logger = Logger.getLogger(TplApiListener.class);
    
    private Map<String, String> returnValues;
    
    public TplApiListener(File rootTransactionFile, Report report) {
        super(rootTransactionFile, report);
        this.returnValues = new HashMap<>();
        this.rootListener = this;
        this.report.addLine("TPL Interpreter initialized!");
    }
    
    @Override
    public String getFormatId() {
        return "RootFormat";
    }
    
    @Override
    public void init() throws Exception {
        TplApiListener.logger.warn("init() not needed for root TplApiListener.");
    }
    
    public void addReturnValue(String name, String value) {
        TplApiListener.logger.info("Storing returnValue: " + name + "=" + value);
        this.returnValues.put(name, value);
    }
    
    public Map<String, String> getReturnValues() {
        return this.returnValues;
    }
    
    public String getReturnValue(String name) {
        return this.returnValues.get(name);
    }
}
