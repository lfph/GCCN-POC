package com.train.gccn.model.report;

/**
 * A report line for the ATV's {@link Report} mechanism.
 */
public class ReportLine extends AbstractReportLine {
    
    public ReportLine(String msg, ReportStatus status) {
        super(msg, status);
    }
}
