package com.train.gccn.client;

import com.train.gccn.model.report.BufferedStdOutReportObserver;
import com.train.gccn.model.report.Report;
import com.train.gccn.model.trustscheme.TrustScheme;
import com.train.gccn.model.trustscheme.TrustSchemeClaim;
import com.train.gccn.model.trustscheme.TrustSchemeFactory;
import com.train.gccn.wrapper.XMLUtil;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
//import org.json.*;
import java.util.ArrayList;
import com.google.gson.*;

import java.io.IOException;
import java.util.List;

public class TLClient {

    public class CountryList {
       List<CountryScheme> TrustListEntries;

    }

    public class CountryScheme {
        String CountryName;
        String TrustScheme;

        public CountryScheme(String countryName, String schemeName) {
            CountryName = countryName;
            TrustScheme = schemeName;
        }
    }
    public class RegistryResponse {

        public ArrayList<CountryScheme> TrustListEntries = new ArrayList<CountryScheme>();

    }

    public RegistryResponse VerifyIdentity(String claim)
    {
        Report report = new Report();
        BufferedStdOutReportObserver reportBuffer = new BufferedStdOutReportObserver();
        report.addObserver(reportBuffer);

        report.addLine("Claim: " + claim);

        System.out.println("Claim: " + claim);

        TrustSchemeClaim TSClaim = new TrustSchemeClaim(claim);
        TrustSchemeFactory TSFactory = new TrustSchemeFactory();

        RegistryResponse resp = new RegistryResponse();
        ArrayList<CountryScheme> TrustListEntries = new ArrayList<>();


        try {
            TrustScheme scheme = TSFactory.createTrustScheme(TSClaim, report);

            if(scheme == null)
                throw new IOException("Did not find TrustScheme / TrustList");

            report.addLine("TrustScheme Hostname: " + scheme.getSchemeIdentifierCleaned());
            report.addLine("TrustList Location: " + scheme.getTSLlocation());


            XMLUtil util = new XMLUtil(scheme.getTSLcontent());
            NodeList TSPs = util.getElementsByXpath("//TrustServiceProviderList/TrustServiceProvider");
            //TSPinfo TSPs = new TSPinfo(util.getElementsByXpath("//TrustServiceProviderList/TrustServiceProvider"));

            for(int i=0;i<TSPs.getLength();i++)
            {
                Node TSP = TSPs.item(i);
                //String DID = util.getElementByXPath("//TrustServiceProviderList/TrustServiceProvider/TSPInformation/IssuerName/Name[1]/text()");
                String CountryName = util.getElementByXPath("TSPInformation/TSPLegalName/Name[1]/text()", TSP);
                String SchemeName = util.getElementByXPath("TSPInformation/TrustSchemeName/Name[1]/text()", TSP);
                report.addLine("Issuer (extracted): " + CountryName);
                report.addLine("SchemeName (extracted): " + SchemeName);
                TrustListEntries.add(new CountryScheme(CountryName,SchemeName));
                //CountryList countrylist = new CountryList();


            }
            resp.TrustListEntries = TrustListEntries;
            Gson gson = new Gson();
            String countrylistJSON = gson.toJson(TrustListEntries);
            //resp.VerificationResult = countrylistJSON;
            System.out.println("Result:" + countrylistJSON);
            System.out.println(resp);

        }catch (Exception e)
        {

            report.addLine("Checking identity failed: ");
            report.addLine(e.getMessage());

        }
        reportBuffer.print();






        return resp;
    }

    public class TrustListFetchClass {
        //boolean TrustListFoundAndLoaded = false;
        String TSPName = "";
        String ServiceTypeIdentifier = "";
        String SchemeServiceDefinition = "";
        String ServiceSupplyPoint = "";
        String ServiceDefinitionURI = "";
        String ServiceGovernanceURI = "";
        String ServiceDigitalID = "";
        String EntityIdentifierURI = "";
        String QualifierURI = "";

        public TrustListFetchClass(String tspName, String servicetype, String schemeservicedefinition, String servicesupplypoint, String servicedefintionURI, String servicegovernanceURI, String servicedigitalid, String tspEntityIdentifierURI, String qualifierURI) {
            this.TSPName = tspName;
            this.ServiceTypeIdentifier = servicetype;
            this.SchemeServiceDefinition = schemeservicedefinition;
            this.ServiceSupplyPoint = servicesupplypoint;
            this.ServiceDefinitionURI = servicedefintionURI;
            this.ServiceGovernanceURI = servicegovernanceURI;
            this.ServiceDigitalID = servicedigitalid;
            this.EntityIdentifierURI = tspEntityIdentifierURI;
            this.QualifierURI = qualifierURI;

        }
    }

    public class TrustListIndividualResponse {
        public ArrayList<TrustListFetchClass> TrustedServiceProviderDetails = new ArrayList<TrustListFetchClass>();
    }

    public TrustListIndividualResponse TrustListFetch (String claim) {
        Report report = new Report();
        BufferedStdOutReportObserver reportBuffer = new BufferedStdOutReportObserver();
        report.addObserver(reportBuffer);

        report.addLine("Claim: " + claim);

        System.out.println("Claim: " + claim);

        TrustSchemeClaim TSClaim = new TrustSchemeClaim(claim);
        TrustSchemeFactory TSFactory = new TrustSchemeFactory();

        TrustListIndividualResponse resp = new TrustListIndividualResponse();
        ArrayList<TrustListFetchClass> TrustedServiceProviderDetails = new ArrayList<>();


        try {
            TrustScheme scheme = TSFactory.createTrustScheme(TSClaim, report);

            if(scheme == null)
                throw new IOException("Did not find TrustScheme / TrustList");

            report.addLine("TrustScheme Hostname: " + scheme.getSchemeIdentifierCleaned());
            report.addLine("TrustList Location: " + scheme.getTSLlocation());


            XMLUtil util = new XMLUtil(scheme.getTSLcontent());
            NodeList TSPs = util.getElementsByXpath("//TrustServiceProviderList/TrustServiceProvider");
            //TSPinfo TSPs = new TSPinfo(util.getElementsByXpath("//TrustServiceProviderList/TrustServiceProvider"));

            for(int i=0;i<TSPs.getLength();i++)
            {
                Node TSP = TSPs.item(i);
                String TSPName = util.getElementByXPath("TSPInformation/TSPLegalName/Name[1]/text()", TSP);
                String servicetype = util.getElementByXPath("TSPServices/TSPService/ServiceInformation/ServiceTypeIdentifier/text()", TSP);
                System.out.println("servicetypeidentifier:" + servicetype);
                String schemeservicedefinition = util.getElementByXPath("TSPServices/TSPService/ServiceInformation/SchemeServiceDefinition/text()", TSP);
                String servicesupplypoint = util.getElementByXPath("TSPServices/TSPService/ServiceInformation/ServiceSupplyPoint/text()", TSP);
                String servicedefintionURI = util.getElementByXPath("TSPServices/TSPService/ServiceInformation/ServiceDefintionURI/text()", TSP);
                String servicegovernanceURI = util.getElementByXPath("TSPServices/TSPService/ServiceInformation/AdditionalServiceInformation/ServiceGovernanceURI/text()", TSP);
                String servicedigitalid = util.getElementByXPath("TSPServices/TSPService/ServiceInformation/ServiceDigitalIdentity/DigitalId/X509Certificate/text()", TSP);
                String tspEntityIdentifierURI = util.getElementByXPath("TSPInformation/TSPEntityIdentifierList/TSPEntityIdentifier/TSPEntityIdentifierURI/text()", TSP);
                String qualifierURI = util.getElementByXPath("TSPInformation/TSPQualifierList/TSPQualifier/QualifierURI/text()", TSP);

                TrustedServiceProviderDetails.add(new TrustListFetchClass(TSPName, servicetype, schemeservicedefinition, servicesupplypoint, servicedefintionURI, servicegovernanceURI, servicedigitalid, tspEntityIdentifierURI, qualifierURI));



            }
            resp.TrustedServiceProviderDetails = TrustedServiceProviderDetails;
            Gson gson = new Gson();
            String trustlistindividualJSON = gson.toJson(TrustedServiceProviderDetails);
            System.out.println("Result:" + trustlistindividualJSON);
            System.out.println(resp);

        }catch (Exception e)
        {

            report.addLine("Checking identity failed: ");
            report.addLine(e.getMessage());

        }
        reportBuffer.print();






        return resp;


    }
}