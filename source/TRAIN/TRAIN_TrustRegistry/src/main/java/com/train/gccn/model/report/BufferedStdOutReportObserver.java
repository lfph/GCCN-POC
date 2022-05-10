package com.train.gccn.model.report;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 * An observer for the ATV's {@link Report} mechanism.
 * <p>
 * This ReportObserver stores all messages reported to it.
 * After verification finished, a {@link #print()} method can be used to display the report.
 */
public class BufferedStdOutReportObserver implements ReportObserver {
    
    private List<String> buffer = new ArrayList<>();
    
    public List<String> getBuffer() {
        return this.buffer;
    }
    
    @Override
    public void update(Observable o, Object line) {
        this.buffer.add(line.toString());
    }
    
    /**
     * Print the stored report to <code>System.out</code>..
     */
    public void print() {
        for(String line : this.buffer) {
            System.out.println(line);
        }
    }
    
    /**
     * Print the stored report to the given {@link PrintStream}.
     *
     * @param stream the stream to print the report to.
     */
    public void print(PrintStream stream) {
        for(String line : this.buffer) {
            stream.println(line);
        }
    }
    
    /**
     * Clear the internal buffer.
     */
    public void clearBuffer() {
        this.buffer.clear();
    }
}
