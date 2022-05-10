package com.train.gccn.model.precheck;

import com.train.gccn.model.report.Report;
import com.train.gccn.model.report.ReportStatus;
import org.apache.log4j.Logger;
import org.reflections.Reflections;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

public class Precheck {
    
    private static Logger logger = Logger.getLogger(Precheck.class);
    private final File transaction;
    private final File policy;
    private final Report report;
    
    public Precheck(File transaction, File policy, Report report) {
        this.transaction = transaction;
        this.policy = policy;
        this.report = report;
    }
    
    public boolean runAllChecks() {
        boolean allChecksOK = true;
        
        Set<Class<? extends Prechecker>> allCheckers = getAllCheckers();
        
        if(allCheckers == null) {
            return false;
        }
    
        int numCheckersOK = 0;
    
        for(Class<? extends Prechecker> checkerClass : allCheckers) {
            Precheck.logger.info("Calling pre-check '" + checkerClass.getSimpleName() + "' ...");
            
            try {
                Constructor<?> constructor = checkerClass.getConstructor();
                Prechecker checker = (Prechecker) constructor.newInstance();
                
                boolean checkerStatus = checker.check(this.transaction, this.policy, this.report);
                if(checkerStatus == false) {
                    allChecksOK = false;
                    Precheck.logger.warn("Pre-check " + checkerClass.getSimpleName() + " failed.");
                    this.report.addLine("Pre-check " + checkerClass.getSimpleName() + " failed.", ReportStatus.FAILED);
                } else {
                    numCheckersOK++;
                }
    
    
            } catch(InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                this.report.addLine("Could not initialize Checker " + checkerClass, ReportStatus.FAILED);
                Precheck.logger.error("", e);
            }
        }
        
        if(allChecksOK) {
            this.report.addLine(numCheckersOK + " pre-checks passed!");
        }
        
        return allChecksOK;
    }
    
    private Set<Class<? extends Prechecker>> getAllCheckers() {
        Reflections reflections = new Reflections("eu.lightest.verifier.model.precheck");
        return reflections.getSubTypesOf(Prechecker.class);
    }
}
