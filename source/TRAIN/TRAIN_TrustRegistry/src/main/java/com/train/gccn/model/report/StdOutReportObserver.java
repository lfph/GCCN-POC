package com.train.gccn.model.report;

import java.util.Observable;

/**
 * A simple observer for the ATV's {@link Report} mechanism.
 * <p>
 * This ReportObserver prints all messages reported to it to <code>System.out</code>.
 */
public class StdOutReportObserver implements ReportObserver {
    
    @Override
    public void update(Observable o, Object line) {
        System.out.println(line.toString());
    }
}
