package com.train.gccn.model.trustscheme;

public class GenericTslEntryDecorator extends GenericTslEntry {
    
    private final TslEntry entryToDecorate;
    
    public GenericTslEntryDecorator(TslEntry entryToDecorate, String schemeId) {
        super(entryToDecorate.getCertificate(), entryToDecorate.getServiceName(), schemeId);
        this.entryToDecorate = entryToDecorate;
    }
    
    @Override
    public String getField(String field) {
        // first try me, then try the entry we decorate
        // (a field set on me overwrites a field in the decoratee; needed for translations)
    
        if(super.fieldExists(field)) {
            return super.getField(field);
        }
        return this.entryToDecorate.getField(field);
    }
    
    @Override
    public boolean fieldExists(String field) {
        return this.entryToDecorate.fieldExists(field) || super.fieldExists(field);
    }
    
    @Override
    public String toString() {
        return this.entryToDecorate.toString();
    }
}
