package com.train.gccn.model.format;

import eu.lightest.horn.specialKeywords.IAtvApiListener;

public interface FormatParser extends IAtvApiListener {
    
    public String getFormatId();
    
    public void init() throws Exception;
    
}
