package com.train.gccn.model.precheck;

import com.train.gccn.model.report.Report;

import java.io.File;

public interface Prechecker {
    
    boolean check(File transaction, File policy, Report report);
}
