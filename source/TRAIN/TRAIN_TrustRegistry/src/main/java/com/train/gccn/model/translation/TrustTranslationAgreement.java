package com.train.gccn.model.translation;

import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TrustTranslationAgreement {
    
    private static Logger logger = Logger.getLogger(TrustTranslationAgreement.class);
    
    // TODO dates, duration, status
    private String name;
    private Map<String, String> sourceParams;
    private String sourceName;
    private String sourceLevel;
    private String sourceProvider;
    private Map<String, String> targetParams;
    private String targetName;
    private String targetLevel;
    private String targetProvider;
    
    public TrustTranslationAgreement() {
        this.sourceParams = new HashMap<>();
        this.targetParams = new HashMap<>();
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSourceName() {
        return this.sourceName;
    }
    
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }
    
    public String getSourceLevel() {
        return this.sourceLevel;
    }
    
    public void setSourceLevel(String sourceLevel) {
        this.sourceLevel = sourceLevel;
    }
    
    public String getSourceProvider() {
        return this.sourceProvider;
    }
    
    public void setSourceProvider(String sourceProvider) {
        this.sourceProvider = sourceProvider;
    }
    
    public String getTargetName() {
        return this.targetName;
    }
    
    public void setTargetName(String targetName) {
        this.targetName = targetName.trim();
    }
    
    public String getTargetLevel() {
        return this.targetLevel;
    }
    
    public void setTargetLevel(String targetLevel) {
        this.targetLevel = targetLevel;
    }
    
    public String getTargetProvider() {
        return this.targetProvider;
    }
    
    public void setTargetProvider(String targetProvider) {
        this.targetProvider = targetProvider;
    }
    
    public void addSourceParam(String name, String value) {
        this.sourceParams.put(name, value);
    }
    
    public void addSourceParams(Map<String, String> params) {
        this.sourceParams.putAll(params);
    }
    
    public void addTargetParam(String name, String value) {
        this.targetParams.put(name, value);
    }
    
    public void addTargetParams(Map<String, String> params) {
        this.targetParams.putAll(params);
    }
    
    public Map<String, String> getSourceParams() {
        return this.sourceParams;
    }
    
    public Map<String, String> getTargetParams() {
        return this.targetParams;
    }
    
    @Override
    public String toString() {
        return "TrustTranslationAgreement{" +
                "name='" + this.name + '\'' + '\n' +
                " sourceName='" + this.sourceName + '\'' + '\n' +
                " sourceLevel='" + this.sourceLevel + '\'' + '\n' +
                " sourceProvider='" + this.sourceProvider + '\'' + '\n' +
                " sourceParams=" + Arrays.toString(this.sourceParams.entrySet().toArray()) + '\n' +
                " targetName='" + this.targetName + '\'' + '\n' +
                " targetLevel='" + this.targetLevel + '\'' + '\n' +
                " targetProvider='" + this.targetProvider + '\'' + '\n' +
                " targetParams=" + Arrays.toString(this.targetParams.entrySet().toArray()) + '\n' +
                '}';
    }
}
