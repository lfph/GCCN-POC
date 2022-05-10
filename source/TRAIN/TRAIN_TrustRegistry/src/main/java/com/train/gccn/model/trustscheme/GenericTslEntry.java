package com.train.gccn.model.trustscheme;

import org.apache.log4j.Logger;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class GenericTslEntry implements TslEntry {
    
    private static Logger logger = Logger.getLogger(GenericTslEntry.class);
    
    private final HashMap<String, String> fields;
    private String serviceName;
    private X509Certificate cert;
    private String schemeId;
    
    public GenericTslEntry(X509Certificate certificate, String serviceName, String schemeId) {
        this.fields = new HashMap<String, String>();
        this.cert = certificate;
        this.serviceName = serviceName;
        this.schemeId = schemeId;
    }
    
    public void setField(String field, String value) {
        GenericTslEntry.logger.info(String.format("set field %-15s to %s", field, value));
        this.fields.put(field, value);
    }
    
    public void setAllFields(Map<String, String> fields) {
        for(Map.Entry<String, String> param : fields.entrySet()) {
            this.setField(param.getKey(), param.getValue());
        }
    }
    
    @Override
    public X509Certificate getCertificate() {
        return this.cert;
    }
    
    @Override
    public String getField(String field) {
        return this.fields.get(field);
    }
    
    @Override
    public boolean fieldExists(String field) {
        return this.fields.containsKey(field);
    }
    
    @Override
    public String getServiceName() {
        return this.serviceName;
    }
    
    @Override
    public String getSchemeId() {
        return this.schemeId;
    }
    
    @Override
    public String toString() {
        return "GenericTslEntry{" + "\n" +
                "fields=" + this.fields + "\n" +
                ", serviceName='" + this.serviceName + '\'' + "\n" +
                ", schemeId='" + this.schemeId + '\'' + "\n" +
                '}';
    }
}
