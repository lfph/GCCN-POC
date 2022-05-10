package com.train.gccn;

import iaik.security.provider.IAIK;
import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ATVConfiguration {
    
    private static final File internalConfigPath = new File("atv.properties");
    private static AbstractConfiguration config = null;
    
    private static Logger logger = Logger.getLogger(ATVConfiguration.class);
    
    static {
        try {
            ATVConfiguration.init();
        } catch(ConfigurationException e) {
            ATVConfiguration.logger.error("", e);
        }
    }
    
    private ATVConfiguration() {
        // static class
    }
    
    private static void printConfig() {
        Iterator<String> keys = ATVConfiguration.config.getKeys();
        while(keys.hasNext()) {
            String key = keys.next();
            String value = ATVConfiguration.config.getString(key);
            ATVConfiguration.logger.info(String.format(" * %-35s: %s", key, value));
        }
    }
    
    public static void init() throws ConfigurationException {
        IAIK.addAsProvider();
        Configurations configs = new Configurations();
        
        ATVConfiguration.config = configs.properties(ATVConfiguration.internalConfigPath);
    
        ATVConfiguration.logger.info("Config initialized / reset done:");
        ATVConfiguration.printConfig();
    }
    
    public static void reset() throws ConfigurationException {
        ATVConfiguration.init();
    }
    
    public static void init(File customConfigPath) throws ConfigurationException {
        Configurations configs = new Configurations();
        
        if(!customConfigPath.exists()) {
            throw new IllegalArgumentException("File does not exist: " + customConfigPath.getAbsolutePath());
        }
        
        CompositeConfiguration cc = new CompositeConfiguration();
        cc.addConfiguration(configs.properties(customConfigPath));
        cc.addConfiguration(configs.properties(ATVConfiguration.internalConfigPath));
        
        ATVConfiguration.config = cc;
    
        ATVConfiguration.logger.info("Config initialized with custom config:");
        ATVConfiguration.printConfig();
    }
    
    public static AbstractConfiguration get() {
        return ATVConfiguration.config;
    }
    
    public static Map<String, String> getForPrefix(String prefix) {
        Map<String, String> entries = new HashMap<>();
        
        Iterator<String> keys = ATVConfiguration.get().getKeys(prefix);
        String prefixRegex = "^" + prefix + ".";
        
        while(keys.hasNext()) {
            String key = keys.next();
            String keySuffix = key.replaceFirst(prefixRegex, "");
            String value = ATVConfiguration.get().getString(key);
            entries.put(keySuffix, value);
        }
        
        return entries;
    }
}
