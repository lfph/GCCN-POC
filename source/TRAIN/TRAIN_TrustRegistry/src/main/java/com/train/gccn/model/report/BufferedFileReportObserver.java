package com.train.gccn.model.report;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 * An observer for the ATV's {@link Report} mechanism.
 * <p>
 * This ReportObserver stores all messages reported to it.
 * After verification finished, the {@link #saveToFile(File)} method can be used to store the report.
 */
public class BufferedFileReportObserver implements ReportObserver {
    
    private List<String> buffer = new ArrayList<>();
    
    public List<String> getBuffer() {
        return this.buffer;
    }
    
    @Override
    public void update(Observable o, Object line) {
        this.buffer.add(line.toString());
    }
    
    public void saveToFile(File target) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(new FileOutputStream(target));
        for(String line : this.buffer) {
            pw.println(line);
        }
        pw.close();
    }
    
    
}
