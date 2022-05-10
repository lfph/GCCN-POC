package com.train.gccn.model.report;

/**
 * Interface for report lines for the ATV's {@link Report} mechanism.
 */
public abstract class AbstractReportLine {
    
    private static String format = "%10s: %s";
    
    private String msg;
    private ReportStatus status;
    
    public AbstractReportLine(String msg, ReportStatus status) {
        this.msg = msg;
        this.status = status;
    }
    
    public String getMsg() {
        return this.msg;
    }
    
    public String getStatus() {
        return this.status.toString();
    }
    
    @Override
    public String toString() {
        //return "REPORT: " + this.status + "  " + this.msg;
        return String.format(AbstractReportLine.format, this.status, this.msg);

//        return "AbstractReportLine{" +
//                "type='" + type + '\'' +
//                ", msg='" + msg + '\'' +
//                ", status=" + status +
//                '}';
    }
}
