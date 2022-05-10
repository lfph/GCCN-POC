package com.train.gccn.model.trustscheme;

import eu.europa.esig.dss.tsl.ServiceInfo;
import eu.europa.esig.dss.validation.process.qualification.trust.AdditionalServiceInformation;
import eu.europa.esig.dss.x509.CertificateToken;
import com.train.gccn.model.format.eIDAS_qualified_certificate.EidasCertFormat;
import org.apache.log4j.Logger;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EidasTslEntry implements TslEntry {
    
    private static Logger logger = Logger.getLogger(EidasCertFormat.class);
    private Set<ServiceInfo> trustServices;
    private ServiceInfo primaryTrustService;
    private CertificateToken token;
    private String schemeId;
    
    public EidasTslEntry(CertificateToken token, Set<ServiceInfo> trustServices, String schemeId) {
        
        this.token = token;
        this.trustServices = trustServices;
        
        int numServices = this.trustServices.size();
        if(numServices > 1) {
            EidasTslEntry.logger.warn("Found " + numServices + " trust services, using first one.");
        }
        this.primaryTrustService = this.trustServices.iterator().next();
        this.schemeId = schemeId;
    }
    
    private Set<ServiceInfo> getTrustServices() {
        return this.trustServices;
    }
    
    private CertificateToken getCertificateToken() {
        return this.token;
    }
    
    @Override
    public X509Certificate getCertificate() {
        return this.token.getCertificate();
    }
    
    @Override
    public String getField(String field) {
        switch(field) {
            case "tlCountryCode":
                return this.primaryTrustService.getTlCountryCode();
            case "tspName":
                return this.primaryTrustService.getTspName();
//            case "status":
//                return this.primaryTrustService.getStatus().toString();
            case "serviceStatus":
                return this.primaryTrustService.getStatus().getLatest().getStatus();
            case "serviceType":
                return this.getServiceType();
            case "serviceName":
                return this.primaryTrustService.getStatus().getLatest().getServiceName();
            case "serviceAdditionalServiceInfoUris":
                return this.primaryTrustService.getStatus().getLatest().getAdditionalServiceInfoUris().toString();
            case "serviceAdditionalServiceInfo":
                return this.getServiceInfo();
            default:
                return null;
        }
    }
    
    private String getServiceInfo() {
        List<String> infos = this.primaryTrustService.getStatus().getLatest().getAdditionalServiceInfoUris();
    
        if(infos == null) {
            EidasTslEntry.logger.warn("AdditionalServiceInfoUris is null.");
            return "";
        }
    
        if(AdditionalServiceInformation.isForeSignatures(infos)) {
            return "for_esignatures";
        }
        if(AdditionalServiceInformation.isForWebAuth(infos)) {
            return "for_webauth";
        }
        if(AdditionalServiceInformation.isForeSeals(infos)) {
            return "for_eseals";
        }
    
        return infos.toString();
        
    }
    
    private String getServiceType() {
        String type = this.primaryTrustService.getStatus().getLatest().getType();
        switch(type) {
            case "http://uri.etsi.org/TrstSvc/Svctype/CA/QC":
                return "qualified_certificate_authority";
            case "http://TrustSchemePumpkinOilFederation.com/ServiceTypes/buyers/":
            case "http://TrustSchemePumpkinOilFederation.com/ServiceTypes/buyers":
                return "qualified_pof_buyer";
            default:
                return type;
        }
    }
    
    @Override
    public boolean fieldExists(String field) {
        return this.getField(field) != null;
    }
    
    @Override
    public String getServiceName() {
        return this.getTrustServices().stream()
                .map(serviceInfo -> serviceInfo.getStatus().getLatest().getServiceName())
                .collect(Collectors.joining());
    }
    
    @Override
    public String getSchemeId() {
        return this.schemeId;
    }
    
    @Override
    public String toString() {
        return "EidasTslEntry{" + "\n" +
                "trustServices=\n" + this.trustServices + "\n" +
                ", schemeId='" + this.schemeId + '\'' + "\n" +
                '}';
    }
}
