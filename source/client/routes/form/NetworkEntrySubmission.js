'use strict';
var express = require('express');
const session = require('express-session');
var router = express.Router();
var axios = require('axios');
var formJSON = require('../../data/formFields');
var xml2js = require('xml2js');
var parser = new xml2js.Parser()
var fs = require("fs");
var crypto = require('crypto');
var builder = new xml2js.Builder();
var trustListSpecificationJsonPromise = require('../../data/publishTrustListXMLSpecification');

/* GET form page. */
router.get('/:step', function (req, res) {
    var step = parseInt(req.params['step']);
    var sessionData = req.session.sessionData;
    if (!sessionData) {
        sessionData = {
            formData: {}
        };
        req.session.sessionData = sessionData;
    }
    var completedStep = sessionData.completedStep;
    if (step > 1 && !(completedStep && step <= completedStep + 1)) {
        return res.redirect('/form/gccn-network-entry-submission/' + (step - 1));
    }
    if (step == 5) {
        if (req.session.submitted) {
            req.session.sessionData = null;
            req.session.submitted = null;
        } else {
            return res.redirect('/form/gccn-network-entry-submission/4');
        }
    }
    res.render('./form/networkEntrySubmission', {
        currentStep: step,
        formJSON: formJSON,
        formData: sessionData.formData,
        currentNavigationName: 'Submit Network Entry'
    });
});

/* Post form page. */
router.post('/:step', async function (req, res) {
    var step = parseInt(req.params['step']);
    var sessionData = req.session.sessionData;
    if (!sessionData) {
        sessionData = {
            formData: {}
        };
        req.session.sessionData = sessionData;
    }
    var formData = sessionData.formData;
    var completedStep = sessionData.completedStep;
    if (step > 1 && !(completedStep && step <= completedStep + 1)) {
        return res.redirect('/form/gccn-network-entry-submission/' + (step - 1));
    }
    Object.assign(formData, req.body);
    var completedStep = sessionData.completedStep;
    if (completedStep)
        sessionData.completedStep = Math.max(completedStep, step);
    else
        sessionData.completedStep = step;
    var nextStep = req.body.SubmitType == 'Previous' ? step - 1 : step + 1;
    if (nextStep == 5) {
        await putFormData(sessionData.formData, req.headers.host);
        req.session.submitted = true;
    }
    res.redirect('/form/gccn-network-entry-submission/' + nextStep);
});


// generate XML and 'PUT' form data
function putFormData(formData, hostName) {
    return new Promise(async (resolve, reject) => {
        var trustListSpecificationJson = await trustListSpecificationJsonPromise;
        var trustListSpecificationJson = JSON.parse(trustListSpecificationJson);
        var nation = 'example-nation';
        var trustSchemeName = `${nation}.gccn.train.trust-scheme.de`;
        populateTrustListSpecificationJson(trustListSpecificationJson, formData, trustSchemeName);
        await putIndividualTrustSchemeXML(trustListSpecificationJson, trustSchemeName, hostName, nation);
        await putGccnRegistryXML(trustListSpecificationJson, trustSchemeName);
        resolve();
    });
}

// Generates and Saves XML file for Individual Trust Scheme on /data/submissions directory and makes the first PUT request with the xml file
function putIndividualTrustSchemeXML(trustListSpecificationJson, trustSchemeName, hostName, nation) {
    return new Promise(async (resolve, reject) => {
        var outputXml = builder.buildObject(trustListSpecificationJson);
        var putRequestUrl = `https://essif.trust-scheme.de/tspa/api/v1/${trustSchemeName}/trust-list`;
        var submissionAccessUrl = `https://${hostName}/Network-Entries/Submission/${nation}.xml`;
        fs.writeFile(`./data/submissions/${nation}.xml`, outputXml, function (err) {
            if (err) reject('Error writing XML file.');
            axios.put(putRequestUrl, {
                "url": submissionAccessUrl,
                "certificate": [
                    {
                        "usage": "dane-ee",
                        "selector": "cert",
                        "matching": "sha256",
                        "data": "17E3D13418999053253CD8C5C6C050F64AEEF07C0C5B5E6A9232F79E6817E701"
                    }
                ]
            }).then(function (response) {
                console.log("Logging the response here: ", response.status, response.statusText, response.headers);
                resolve();
            }).catch(function (error) {
                reject(error.message);
            });
        });
    });
}

// Updates /data/submissions/gccn-registry.xml file by adding Individual Trust Scheme information to TSP list and makes the second PUT request with the updated XML file
function putGccnRegistryXML(trustListSpecificationJson, trustSchemeName) {
    return new Promise(async (resolve, reject) => {
        fs.readFile('./data/submissions/gccn-registry.xml', function (err, data) {
            if (err) reject("Error Reading XML file");
            parser.parseStringPromise(data)
                .then(function (gccnRegistryJson) {
                    // add TSP information
                    var currentTspInformation = trustListSpecificationJson.TrustServiceStatusList.TrustServiceProviderList[0].TrustServiceProvider[0];
                    var tspList = gccnRegistryJson.TrustServiceStatusList.TrustServiceProviderList[0].TrustServiceProvider;
                    createOrUpdateTspEntry(tspList, currentTspInformation, trustSchemeName);
                    var outputXml = builder.buildObject(gccnRegistryJson);
                    fs.writeFile('./data/submissions/gccn-registry.xml', outputXml, function (err) {
                        if (err) reject("Error writing XML file");
                        // no need to make second PUT request since the URL of gccn-registry.xml file remains same
                        // However, if second put request is made, everything is same as the first PUT request except following:
                        // 1. In PUT request URL, trustSchemeName should be gccn-registry.train.trust-scheme.de
                        // 2. In the JSON body, URL should point to gccn-registry.xml file
                        resolve();
                    });
                })
                .catch(function (err) {
                    reject(err)
                });
        });
    });
}

function createOrUpdateTspEntry(tspList, tspInformation, trustSchemeName) {
    // TSPServices node is not required in the TrustServiceProviderList
    delete tspInformation.TSPServices;
    var updated = false;
    for (let i = 0; i < tspList.length; ++i) {
        if (tspList[i].TSPInformation[0].TrustSchemeName[0].Name[0]._ == trustSchemeName) {
            tspList[i] = tspInformation;
            updated = true;
        }
    }
    if (!updated) tspList.push(tspInformation);
}

// takes the json object created from XML specification and populates it with form post values
function populateTrustListSpecificationJson(trustListSpecificationJson, formData, nation) {
    var trustServiceProvider = trustListSpecificationJson.TrustServiceStatusList.TrustServiceProviderList[0].TrustServiceProvider[0];
    var tspInformation = trustServiceProvider.TSPInformation[0];
    var serviceInformation = trustServiceProvider.TSPServices[0].TSPService[0].ServiceInformation[0];
    AddParticipatingEntityInformation(tspInformation, formData, nation);
    AddSubmitterContactInformation(tspInformation, formData);
    AddServiceInformation(serviceInformation, formData);
    AddServiceOperationalContactInformation(tspInformation, formData);
}

// Populate user-entered data coming from first step of forms (Participating Entity Information)
function AddParticipatingEntityInformation(tspInformation, formData, trustSchemeName) {
    // Entity Name
    if (formData.EntityName != '') tspInformation.TSPName[0].Name[0]._ = formData.EntityName;
    // Trust Scheme Name
    tspInformation.TrustSchemeName[0].Name[0]._ = trustSchemeName;
    // Entity URL
    tspInformation.TSPInformationURI[0].URI[0]._ = formData.EntityUrl;
    // Entity Description
    tspInformation.TSPAddress[0].ElectronicAddress[0].URI[0]._ = formData.EntityDescription;
    // Entity Role
    var role = formData.EntityRole;
    tspInformation.TSPRole[0]._ = role ? role.join(',') : '';
    // Entity Identifier
    tspInformation.TSPEntityIdentifierList[0].TSPEntityIdentifier[0].TSPEntityIdentifierName[0].Name[0]._ = formData.EntityIdentifiers;
    // Entity Legal Basis
    if (formData.EntityLegalBasis != '') tspInformation.TSPLegalName[0].Name[0]._ = formData.EntityLegalBasis;
    var qualifierList = tspInformation.TSPQualifierList[0].TSPQualifier;
    // Entity Classifications
    qualifierList[0].Name[0] = 'Entity Classifications';
    qualifierList[0].Value[0] = formData.EntityClassifications;
    // Entity certifications
    var certification = JSON.parse(JSON.stringify(qualifierList[0]));
    certification.Name[0] = 'Entity Certifications';
    certification.Value[0] = formData.EntityCertifications;
    qualifierList.push(certification);
    // Keywords/Meta tags
    var keywords = JSON.parse(JSON.stringify(qualifierList[0]));
    keywords.Name[0] = 'Keywords/Meta tags';
    keywords.Value[0] = formData.Keywords;
    qualifierList.push(keywords);
}

// Populate user-entered data coming from second step of forms (Submitter Contact Information)
function AddSubmitterContactInformation(tspInformation, formData) {
    var tspAgents = tspInformation.TSPAgentList[0].TSPAgent;
    // tspAgents[0] will contain Submitter Contact Information, while tspAgents[1] will contain Service Operational Contact Information (coming from form Step 5)
    tspAgents[1] = JSON.parse(JSON.stringify(tspAgents[0]));
    AddAgentData(tspAgents[0], formData, {
        Name: 'SubmitterName',
        Email: 'SubmitterEmail',
        Country: 'SubmitterCountry',
        Company: 'SubmitterCompany',
        Street: 'SubmitterStreetAddress1',
        City: 'SubmitterCity',
        State: 'SubmitterState',
        Zip: 'SubmitterZipCode'
    });
}

// Populate user-entered data coming from third step of forms (Service Information)
function AddServiceInformation(serviceInformation, formData) {
    var additionalInfo = serviceInformation.AdditionalServiceInformation[0];
    // Service Endpoint
    serviceInformation.ServiceSupplyPoint = formData.ServiceEndpoint;
    // Service Description
    serviceInformation.ServiceDefintionURI = formData.ServiceDescription;
    // Service Technical Specification URL
    serviceInformation.SchemeServiceDefinition = formData.STSUrl;
    // Service Root of Trust URL
    serviceInformation.ServiceDigitalIdentity[0].DigitalId[1] = formData.SRTUrl;
    // Service Type
    serviceInformation.ServiceTypeIdentifier = formData.ServiceType;
    // Credential Type
    additionalInfo.ServiceCredentialTypes[0].KeyType[0] = formData.CredentialType;
    // Trust Type
    additionalInfo.ServiceCredentialTypes[0].KeyType[1] = formData.TrustType;
    // Governance Frameworks
    /*
     * If it was rendered as an edit grid.
        if (formData.GovernanceFrameworks)
            additionalInfo.ServiceGovernanceURI = formData.GovernanceFrameworks.map(x=>x.LinkUrl);
    */
    additionalInfo.ServiceGovernanceURI = formData.GovernanceFrameworks.LinkUrl;
}

// Populate user-entered data coming from fourth step of forms (Service Operational Contact Information)
function AddServiceOperationalContactInformation(tspInformation, formData) {
    var tspAgents = tspInformation.TSPAgentList[0].TSPAgent;
    AddAgentData(tspAgents[1], formData, {
        Name: 'OpsContactName',
        Email: 'OpsContactEmail',
        Country: 'OpsCountry',
        Company: 'OpsCompany',
        Street: 'OpsStreetAddress1',
        City: 'OpsCity',
        State: 'OpsState',
        Zip: 'OpsZipCode'
    });
}

function AddAgentData(tspAgent, formData, fieldMaps) {
    var postalAddress = tspAgent.TSPAgentAddress[0].PostalAddresses[0].PostalAddress[0];
    // Name
    tspAgent.TSPAgentName[0].Name[0]._ = formData[fieldMaps.Name];
    // Email
    tspAgent.TSPAgentAddress[0].ElectronicAddress[0].URI[0]._ = formData[fieldMaps.Email];
    // Phone
    // No data available
    // Country
    postalAddress.CountryName = formData[fieldMaps.Country];
    // Company
    postalAddress.Company = formData[fieldMaps.Company];
    // Street Address
    postalAddress.StreetAddress = formData[fieldMaps.Street];
    // City
    postalAddress.Locality[0] = formData[fieldMaps.City];
    // State
    postalAddress.Locality[1] = formData[fieldMaps.State];
    // Postal Code
    postalAddress.PostalCode = formData[fieldMaps.Zip];
}

module.exports = router;