package com.train.gccn.client;

import com.train.gccn.exceptions.DNSException;
import com.train.gccn.model.report.BufferedStdOutReportObserver;
import com.train.gccn.model.report.Report;
import com.train.gccn.model.trustscheme.TrustScheme;
import com.train.gccn.model.trustscheme.TrustSchemeClaim;
import com.train.gccn.model.trustscheme.TrustSchemeFactory;
import com.train.gccn.wrapper.XMLUtil;
import org.xml.sax.SAXException;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class GCCNClient {

    public class VerificationResultClass {
        String ReceivedTrustSchemePointer = "";
        boolean FindingCorrespondingTrustSchemeInitiated = false;
        String FoundCorrespondingTrustScheme = "";
        boolean TrustListDiscoveryInitiated = false;
        String TrustListFoundAndLoaded = "";
        String FoundIssuer = "";
        String ServiceTypeIdentifier = "";
        String SchemeServiceDefinition = "";
        String ServiceSupplyPoint = "";
        String ServiceDefinitionURI = "";
        String ServiceGovernanceURI = "";
        String ServiceDigitalID = "";
        String EntityIdentifierURI = "";
        String QualifierURI = "";
        boolean VerifyIssuer = false;
        boolean VerificationSuccessful = false;
    }

    public class GCCNResponse {

        public boolean VerificationStatus = false;
        public VerificationResultClass VerificationResult = new VerificationResultClass();

    }



    public GCCNResponse VerifyIdentity(String issuer, String claim)
    {
        Report report = new Report();
        BufferedStdOutReportObserver reportBuffer = new BufferedStdOutReportObserver();
        report.addObserver(reportBuffer);

        report.addLine("Checking Identity");
        report.addLine("DID: " + issuer);
        report.addLine("Claim: " + claim);

        System.out.println("DID: " + issuer);
        System.out.println("Claim: " + claim);

        TrustSchemeClaim TSClaim = new TrustSchemeClaim(claim);
        TrustSchemeFactory TSFactory = new TrustSchemeFactory();

        boolean bVerificationStatus = false; 

        GCCNResponse resp = new GCCNResponse();
        resp.VerificationResult.ReceivedTrustSchemePointer = claim;
        resp.VerificationResult.FindingCorrespondingTrustSchemeInitiated = true;


        try {
            TrustScheme scheme = TSFactory.createTrustScheme(TSClaim, report);

            if(scheme == null)
                throw new IOException("Did not find TrustScheme / TrustList");

            resp.VerificationResult.FoundCorrespondingTrustScheme = scheme.getSchemeIdentifierCleaned();
            resp.VerificationResult.TrustListDiscoveryInitiated = true;
            resp.VerificationResult.TrustListFoundAndLoaded = scheme.getTSLlocation();

            report.addLine("TrustScheme Hostname: " + scheme.getSchemeIdentifierCleaned());
            report.addLine("TrustList Location: " + scheme.getTSLlocation());


            XMLUtil util = new XMLUtil(scheme.getTSLcontent());
            NodeList TSPs = util.getElementsByXpath("//TrustServiceProviderList/TrustServiceProvider");
            //TSPinfo TSPs = new TSPinfo(util.getElementsByXpath("//TrustServiceProviderList/TrustServiceProvider"));
                        
            for(int i=0;i<TSPs.getLength();i++)
            {
                Node TSP = TSPs.item(i);
                //String DID = util.getElementByXPath("//TrustServiceProviderList/TrustServiceProvider/TSPInformation/IssuerName/Name[1]/text()");
                String DID = util.getElementByXPath("TSPInformation/TSPLegalName/Name[1]/text()", TSP);
                report.addLine("Issuer (extracted): " + DID);

                if (DID.equals(issuer))
                {
                    resp.VerificationResult.FoundIssuer = issuer;
                    resp.VerificationResult.VerifyIssuer = true;
                    String servicetype = util.getElementByXPath("TSPServices/TSPService/ServiceInformation/ServiceTypeIdentifier/text()", TSP);
                    System.out.println("servicetypeidentifier:" + servicetype);
                    String schemeservicedefinition = util.getElementByXPath("TSPServices/TSPService/ServiceInformation/SchemeServiceDefinition/text()", TSP);
                    String servicesupplypoint = util.getElementByXPath("TSPServices/TSPService/ServiceInformation/ServiceSupplyPoint/text()", TSP);
                    String servicedefintionURI = util.getElementByXPath("TSPServices/TSPService/ServiceInformation/ServiceDefintionURI/text()", TSP);
                    String servicegovernanceURI = util.getElementByXPath("TSPServices/TSPService/ServiceInformation/AdditionalServiceInformation/ServiceGovernanceURI/text()", TSP);
                    String servicedigitalid = util.getElementByXPath("TSPServices/TSPService/ServiceInformation/ServiceDigitalIdentity/DigitalId/X509Certificate/text()", TSP);
                    String tspEntityIdentifierURI = util.getElementByXPath("TSPInformation/TSPEntityIdentifierList/TSPEntityIdentifier/TSPEntityIdentifierURI/text()", TSP);
                    String qualifierURI = util.getElementByXPath("TSPInformation/TSPQualifierList/TSPQualifier/QualifierURI/text()", TSP);
                    report.addLine("tspEntityIdentifierURI:" + tspEntityIdentifierURI);
                    report.addLine("qualifierURI: " + qualifierURI);
                    report.addLine("servicedigitalid: " + servicedigitalid);
                    resp.VerificationResult.QualifierURI = qualifierURI;
                    resp.VerificationResult.EntityIdentifierURI = tspEntityIdentifierURI;
                    resp.VerificationResult.ServiceTypeIdentifier = servicetype;
                    resp.VerificationResult.SchemeServiceDefinition = schemeservicedefinition;
                    resp.VerificationResult.ServiceSupplyPoint = servicesupplypoint;
                    resp.VerificationResult.ServiceDefinitionURI = servicedefintionURI;
                    resp.VerificationResult.ServiceGovernanceURI = servicegovernanceURI;
                    resp.VerificationResult.ServiceDigitalID = servicedigitalid;
                    resp.VerificationResult.VerificationSuccessful = true;
                    resp.VerificationStatus = true;
                    break;
                    
                }
               

            }
        }catch (Exception e)
        {

            report.addLine("Checking identity failed: ");
            report.addLine(e.getMessage());

        }
        reportBuffer.print();






        return resp;
    }



}
