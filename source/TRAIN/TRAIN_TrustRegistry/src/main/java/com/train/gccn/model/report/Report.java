package com.train.gccn.model.report;

import java.util.Collection;
import java.util.Observable;
import java.util.Observer;

/**
 * ATV verification report.
 * Used to retrieve a more detailed verification result.
 * <p>
 * A Report is used by various ATV components to 'report' status information for the user.
 * This class implements the {@link Observable} patter.
 * <p>
 * To retrieve the verification report, before verification, you need to register a {@link ReportObserver}
 * with the Report, using {@link Report#addObserver(Observer)}.
 * <p>
 * <br>
 * Example:
 * <pre>{@code
 * Report report = new Report();
 * BufferedStdOutReportObserver reportBuffer = new BufferedStdOutReportObserver();
 *
 * report.addObserver(reportBuffer);
 *
 * ATVClient atv = new LocalATVClient(report);
 * }</pre>
 */
public class Report extends Observable {

//    private List<AbstractReportLine> lines;
    
    public Report() {
//        this.lines = new ArrayList<>();
    }
    
    /**
     * Add a message to the report.
     * <p>
     * Use {@link #addLine(String, ReportStatus)} to use a status other can {@link ReportStatus#OK}.
     *
     * @param msg the message to report.
     */
    public void addLine(String msg) {
        this.addLine(msg, ReportStatus.OK);
    }
    
    /**
     * Add a message with a custom status to the report.
     *
     * @param msg    the message to report.
     * @param status the status of the message (e.g. <code>OK</code> or <code>FAILED</code>).
     */
    public void addLine(String msg, ReportStatus status) {
        if(msg != null) {
            AbstractReportLine line = new ReportLine(msg, status);
//        this.lines.add(line);
            
            setChanged();
            notifyObservers(line);
        }
        
    }

//    public List<AbstractReportLine> getLines() {
//        return this.lines;
//    }
    
    /**
     * Add a set of messages to the report.
     * <p>
     * All messages will be reported with status {@link ReportStatus#OK}.
     *
     * @param msgs the messages to report.
     */
    public void addLines(Collection<String> msgs) {
        if(msgs != null) {
            for(String reportLine : msgs) {
                this.addLine(reportLine);
            }
        }
    }
}
