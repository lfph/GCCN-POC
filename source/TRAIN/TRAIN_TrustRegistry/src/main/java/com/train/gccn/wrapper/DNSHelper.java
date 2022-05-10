package com.train.gccn.wrapper;

import com.train.gccn.ATVConfiguration;
import com.train.gccn.exceptions.DNSException;
import org.apache.log4j.Logger;
import org.jitsi.dnssec.validator.ValidatingResolver;
import org.xbill.DNS.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DNSHelper {
    
    public static final String DNS_GOOGLE1 = "8.8.8.8"; // https://developers.google.com/speed/public-dns/
    public static final String DNS_GOOGLE2 = "8.8.4.4"; // https://developers.google.com/speed/public-dns/
    public static final String DNS_CLOUDFLARE1 = "1.1.1.1"; // https://1.1.1.1/
    public static final String DNS_CLOUDFLARE2 = "1.0.0.1"; // https://1.0.0.1/
    public static final String DNS_CLOUDFLARE1v6 = "[2606:4700:4700::1111]";
    public static final String DNS_CLOUDFLARE2v6 = "[2606:4700:4700::1001]";
    public static final String DNS_CISCOopendns1 = "208.67.222.222";
    public static final String DNS_CISCOopendns2 = "208.67.220.220";
    public static final String DNS_CISCO = "171.70.168.183";
    public static final String DNS_EDIS = "151.236.4.166";
    public static final String CNS_CCC = "213.73.91.35";
    public static final int RECORD_A = Type.A;
    public static final int RECORD_CNAME = Type.CNAME;
    public static final int RECORD_URI = Type.URI;
    public static final int RECORD_PTR = Type.PTR;
    public static final int RECORD_TXT = Type.TXT;
    public static final int RECORD_TLSA = Type.TLSA;
    public static final int RECORD_SMIMEA = Type.SMIMEA;
    private final static String ROOT_PATH = ATVConfiguration.get().getString("dnssec_root_key");
    //private final static String ROOT_PATH2 = "/var/lib/unbound/root.key";
    
    private final static String DNSROOT = ". IN DNSKEY 257 3 8 AwEAAaz/tAm8yTn4Mfeh5eyI96WSVexTBAvkMgJzkKTOiW1vkIbzxeF3+/4RgWOq7HrxRixHlFlExOLAJr5emLvN7SWXgnLh4+B5xQlNVz8Og8kvArMtNROxVQuCaSnIDdD5LKyWbRd2n9WGe2R8PzgCmr3EgVLrjyBxWezF0jLHwVN8efS3rCj/EWgvIWgb9tarpVUDK/b58Da+sqqls3eNbuv7pr+eoZG+SrDK6nWeL3c6H5Apxz7LjVc1uTIdsIXxuOLYA4/ilBmSVIzuDWfdRUfhHdY6+cn8HFRm+2hM8AnXGXws9555KrUB5qihylGa8subX2Nn6UwNR1AkUTV74bU=";
    
    private static Logger logger = Logger.getLogger(DNSHelper.class);
    private final Resolver resolver;
    
    public DNSHelper() throws IOException {
        //this(DNSHelper.DNS_CLOUDFLARE1);
        this(DNSHelper.DNS_GOOGLE1);
    }
    
    public DNSHelper(String dnsServerHostname) throws IOException {
        this(dnsServerHostname, DNSHelper.ROOT_PATH);
    }
    
    private DNSHelper(String dnsServerHostname, String rootPath) throws IOException {
        File f = new File(rootPath);
        InputStream rootInputStream = null;
        
        try {
            if(f.exists()) {
                
                rootInputStream = new FileInputStream(rootPath);
                
            } else {
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream(rootPath);
                if(inputStream != null) {
                    rootInputStream = inputStream;
                } else {
                    DNSHelper.logger.error("DNSSEC Root-key: File Not Found: " + f.getAbsolutePath() + " (configured: " + rootPath + "). Using hardcoded backup.");
                    rootInputStream = new ByteArrayInputStream(DNSHelper.DNSROOT.getBytes());
                }
                
            }
            System.out.println("ResolverName" + dnsServerHostname);
            SimpleResolver simpleResolver = new SimpleResolver(dnsServerHostname);
            
            if(ATVConfiguration.get().getBoolean("dnssec_verification_enabled")) {
                System.out.println("reached here1");
                ValidatingResolver validatingResolver = new ValidatingResolver(simpleResolver);
                validatingResolver.loadTrustAnchors(rootInputStream);
                this.resolver = validatingResolver;
                
            } else {
                System.out.println("reached here");
                this.resolver = simpleResolver;
                
            }
        } catch(IOException e) {
            throw e;
        } finally {
            if(rootInputStream != null) {
                rootInputStream.close();
            }
        }
    }
    
    
    public static <R extends org.xbill.DNS.Record> List<R> parseMessage(Message response) throws IOException, DNSException {
        
        List<R> list = new ArrayList<>();
        
        //Message response = this.query(host, recordTypeID);
        RRset[] answerRRsets = response.getSectionRRsets(Section.ANSWER);
        
        for(RRset set : answerRRsets) {
            for(Iterator<org.xbill.DNS.Record> it = set.rrs(); it.hasNext(); ) {
                org.xbill.DNS.Record elem = it.next();
                //if(recordTypeClass.isInstance(elem)) {
                R rec = (R) elem;
                list.add(rec);
                //}
            }
        }
        
        return list;
    }
    
    public List<String> queryTXT(String host) throws IOException, DNSException {
        Message response = this.query(host, Type.TXT);
        List<org.xbill.DNS.Record> records = DNSHelper.parseMessage(response);
        List<String> result = new ArrayList<>();
        
        for(org.xbill.DNS.Record record : records) {
            if(record instanceof TXTRecord) {
                TXTRecord rec = (TXTRecord) record;
                for(Object text : rec.getStrings()) {
                    result.add((String) text);
                }
            } else {
                String actualClass = record.getClass().toString();
                System.out.println("Record was not of type TXT (but " + actualClass + "). Ignoring.");
            }
        }
        
        return result;
    }
    
    public List<String> queryPTR(String host) throws IOException, DNSException {
        Message response = this.query(host, Type.PTR);
        List<org.xbill.DNS.Record> records = DNSHelper.parseMessage(response);
        List<String> result = new ArrayList<>();
        
        for(org.xbill.DNS.Record record : records) {
            if(record instanceof PTRRecord) {
                PTRRecord rec = (PTRRecord) record;
                result.add(rec.getTarget().toString());
            } else {
                String actualClass = record.getClass().toString();
                System.out.println("Record was not of type PTR (but " + actualClass + "). Ignoring.");
            }
        }
        
        return result;
    }
    
    public List<String> queryURI(String host) throws IOException, DNSException {
        Message response = this.query(host, Type.URI);
        List<org.xbill.DNS.Record> records = DNSHelper.parseMessage(response);
        List<String> result = new ArrayList<>();
        
        for(org.xbill.DNS.Record record : records) {
            if(record instanceof URIRecord) {
                URIRecord rec = (URIRecord) record;
                result.add(rec.getTarget().toString());
            } else {
                String actualClass = record.getClass().toString();
                System.out.println("Record was not of type URI (but " + actualClass + "). Ignoring.");
            }
        }
        
        return result;
    }
    
    public List<SMIMEAcert> querySMIMEA(String host) throws IOException, DNSException {
        // https://tools.ietf.org/html/rfc6698#section-2
        
        Message response = this.query(host, Type.SMIMEA);
        List<org.xbill.DNS.Record> records = DNSHelper.parseMessage(response);
        List<SMIMEAcert> result = new ArrayList<>();
        
        for(org.xbill.DNS.Record record : records) {
            if(record instanceof SMIMEARecord) {
                SMIMEARecord rec = (SMIMEARecord) record;
                result.add(new SMIMEAcert(rec));
            } else {
                String actualClass = record.getClass().toString();
                System.out.println("Record was not of type SMIMEA (but " + actualClass + "). Ignoring.");
            }
        }
        
        return result;
    }
    
    
    public Message query(String host, int type) throws IOException, DNSException {
        if(!host.endsWith(".")) {
            host = host + ".";
        }
        
        org.xbill.DNS.Record query = org.xbill.DNS.Record.newRecord(Name.fromConstantString(host), type, DClass.IN);
    
        DNSHelper.logger.info("DNS query: " + query.toString());
        
        Message response = this.resolver.send(Message.newQuery(query));
        System.out.println("DNSResponse" + response);


        if(!response.getHeader().getFlag(Flags.AD)) {
            System.out.println("NO AD flag present!");
            //throw new DNSException("No AD flag. (Host not using DNSSec?)");
        }
        
        int rcode = response.getRcode();
        if (rcode != Rcode.SERVFAIL) {
            if (rcode != Rcode.NOERROR) {

                for (RRset set : response.getSectionRRsets(Section.ADDITIONAL)) {
                    System.out.println("Zone: " + set.getName().toString());

                    if (set.getName().equals(Name.root) && set.getType() == Type.TXT
                            && set.getDClass() == ValidatingResolver.VALIDATION_REASON_QCLASS) {

                        System.out.println("Reason:  " + ((TXTRecord) set.first()).getStrings().get(0));
                    }
                }
                System.out.println("RCode: " + rcode + " (" + Rcode.string(rcode) + ")");
                throw new DNSException("RCode: " + rcode + " (" + Rcode.string(rcode) + ")");
            }
        }
        return response;
    }
    
    public <R extends org.xbill.DNS.Record> List<R> queryAndParse(String host, Class recordTypeClass, int recordTypeID) throws IOException, DNSException {
        
        List<R> list = new ArrayList<R>();
        
        Message response = this.query(host, recordTypeID);
        RRset[] answerRRsets = response.getSectionRRsets(Section.ANSWER);
        
        for(RRset set : answerRRsets) {
            for(Iterator<org.xbill.DNS.Record> it = set.rrs(); it.hasNext(); ) {
                org.xbill.DNS.Record elem = it.next();
                if(recordTypeClass.isInstance(elem)) {
                    R rec = (R) elem;
                    list.add(rec);
                }
            }
        }
        
        return list;
        
    }
    
    
}
