package com.train.gccn.exceptions;

public class DANEException extends Exception {
    
    public DANEException(String s) {
        super(s);
    }
    
    public DANEException(Exception e) {
        super(e);
    }
}

