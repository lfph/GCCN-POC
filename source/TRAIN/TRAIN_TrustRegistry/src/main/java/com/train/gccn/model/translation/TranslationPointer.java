package com.train.gccn.model.translation;

public class TranslationPointer {
    
    private String pointer;
    
    public TranslationPointer(String pointer) {
        this.pointer = pointer;
    }
    
    public String getPointer() {
        return this.pointer;
    }
    
    @Override
    public String toString() {
        return this.pointer;
    }
}
