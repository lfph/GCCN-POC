'use strict';
var selectLists = require('./selectLists');
module.exports = {
    "FormSections": [
        {
            "SectionName": "Participating Entity Information",
            "Fields": [
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "EntityName",
                    "Label": "Entity Name",
                    "Tooltip": "Participating organization's legal name."
                },
                {
                    "Type": "Text",
                    "SubType": "URL",
                    "Key": "EntityUrl",
                    "Label": "Entity URL",
                    "Tooltip": "Participating organization's website URL."
                },
                {
                    "Type": "TextArea",
                    "Key": "EntityDescription",
                    "Label": "Entity Description",
                    "Tooltip": "Additional information about the participating organization, their role with respect to the trust registry."
                },
                {
                    "Type": "CheckBox",
                    "Key": "EntityRole",
                    "Label": "Entity Role",
                    "Values": [
                        {
                            "Key": "val1",
                            "Label": "Governing Authority",
                            "Value": "Governing Authority"
                        },
                        {
                            "Key": "val2",
                            "Label": "Issuer",
                            "Value": "Issuer"
                        },
                        {
                            "Key": "val3",
                            "Label": "Registry Administrator",
                            "Value": "Registry Administrator"
                        },
                        {
                            "Key": "val4",
                            "Label": "Verifier",
                            "Value": "Verifier"
                        },
                    ],
                    "Tooltip": "Select all roles that apply for the participating entity/organization."
                },
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "EntityIdentifiers",
                    "Label": "Entity Identifiers",
                    "Tooltip": "Legal identifiers for the organization such as GLEIF, Registration/License numbers etc."
                },
                {
                    "Type": "TextArea",
                    "Key": "EntityLegalBasis",
                    "Label": "Entity Legal Basis",
                    "Tooltip": "The legal basis for the entity - it may be dependent on the jurisdiction."
                },
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "EntityClassifications",
                    "Label": "Entity Classifications",
                    "Tooltip": "Business classifications for the participating organization, may be provided in the form of NAICS, UNSPC, GICS codes."
                },
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "EntityCertifications",
                    "Label": "Entity Certifications",
                    "Tooltip": "Jurisdiction or Industry compliance certifications, e.g., ISO, NIST certifications for a company."
                },
                {
                    "Type": "TextArea",
                    "Key": "Keywords",
                    "Label": "Keywords / Meta Tags",
                    "Tooltip": "Keywords or meta-tags for the participating organization and/or the trust registry service provided by them."
                }
            ]
        },
        {
            "SectionName": "Submitter Contact Information",
            "Fields": [
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "SubmitterName",
                    "Label": "Submitter Name",
                    "Tooltip": "Provide the name of the individual / party submitting this information for GCCN entry."
                },
                {
                    "Type": "Text",
                    "SubType": "Email",
                    "Key": "SubmitterEmail",
                    "Label": "Submitter Email",
                    "Tooltip": "Submitter Contact Email."
                },
                {
                    "Type": "Text",
                    "SubType": "Tel",
                    "Key": "SubmitterPhone",
                    "Label": "Submitter Phone",
                    "Tooltip": "Submitter Contact Phone."
                },
                {
                    "Type": "Select",
                    "Key": "SubmitterCountry",
                    "Label": "Country",
                    "Options": selectLists.CountryList
                },
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "SubmitterCompany",
                    "Label": "Company"
                },
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "SubmitterStreetAddress1",
                    "Label": "Street Address 1"
                },
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "SubmitterStreetAddress2",
                    "Label": "Street Address 2"
                },
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "SubmitterCity",
                    "Label": "City"
                },
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "SubmitterState",
                    "Label": "State/Province"
                },
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "SubmitterZipCode",
                    "Label": "Zip Code/Postal Code"
                }
            ]
        },
        {
            "SectionName": "Service Information",
            "Fields": [
                {
                    "Type": "Text",
                    "SubType": "URL",
                    "Key": "ServiceEndpoint",
                    "Label": "Service Endpoint",
                    "Tooltip": "Service endpoint for the trust registry service."
                },
                {
                    "Type": "TextArea",
                    "Key": "ServiceDescription",
                    "Label": "Service Description",
                    "Tooltip": "Additional information about the trust registry/service being provided at the service endpoint."
                },
                {
                    "Type": "Text",
                    "SubType": "URL",
                    "Key": "STSUrl",
                    "Label": "Service Technical Specifications URL",
                    "Tooltip": "Link to detailed technical specifications of the service provided at the service endpoint."
                },
                {
                    "Type": "Text",
                    "SubType": "URL",
                    "Key": "SRTUrl",
                    "Label": "Service root of trust URL",
                    "Tooltip": "Provide the root of trust for the service listed - may be in x509, DID formats."
                },
                {
                    "Type": "CheckBox",
                    "Key": "ServiceType",
                    "Label": "Service Type",
                    "Values": [
                        {
                            "Key": "val1",
                            "Label": "Vaccination",
                            "Value": "Vaccination"
                        },
                        {
                            "Key": "val2",
                            "Label": "Test",
                            "Value": "Test"
                        },
                        {
                            "Key": "val3",
                            "Label": "Recovery",
                            "Value": "Recovery"
                        },
                        {
                            "Key": "val4",
                            "Label": "Exemption",
                            "Value": "Exemption"
                        },
                        {
                            "Key": "val5",
                            "Label": "Travel Pass",
                            "Value": "Travel Pass"
                        }
                    ],
                    "Tooltip": "Select the types of credentials issued by the issuers listed in the trust registry."
                },
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "CredentialType",
                    "Label": "Credential Type",
                    "Validation": {
                        "Required": true
                    },
                    "Tooltip": "Type of credentials supported, such as DSC, HC1, Smart Health Card etc. (may create a controlled set of values later once we have gathered some entries)."
                },
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "TrustType",
                    "Label": "Trust Type/ Subtype",
                    "Tooltip": "Set of public keys, X509 certificates, URLs or technical format used."
                },
                {
                    "Type": "Container",
                    "Key": "GovernanceFrameworks",
                    "Label": "Governance Frameworks",
                    "EntityLabel": "Governance Framework",
                    "Components": [
                        {
                            "Type": "Text",
                            "SubType": "Text",
                            "Key": "LinkTitle",
                            "Label": "Link Title",
                            "Validation": {
                                "Required": true
                            }
                        },
                        {
                            "Type": "Text",
                            "SubType": "URL",
                            "Key": "LinkUrl",
                            "Label": "Link URL",
                            "Validation": {
                                "Required": true
                            }
                        }
                    ],
                    "Tooltip": "Applicable Governance Frameworks for the trust registry service (there may be more than one such as international or domestic governance frameworks)."
                }
            ]
        },
        {
            "SectionName": "Service Operational Contact Information",
            "Fields": [
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "OpsContactName",
                    "Label": "Ops Contact Name",
                    "Tooltip": "Opertational Contact Name."
                },
                {
                    "Type": "Text",
                    "SubType": "Email",
                    "Key": "OpsContactEmail",
                    "Label": "Ops Contact Email",
                    "Tooltip": "Operactional Contact Email."
                },
                {
                    "Type": "Text",
                    "SubType": "Tel",
                    "Key": "OpsContactPhone",
                    "Label": "Ops Contact Phone",
                    "Tooltip": "Operational Contact Phone Number."
                },
                {
                    "Type": "Select",
                    "Key": "OpsCountry",
                    "Label": "Country",
                    "Options": selectLists.CountryList,
                    "Validation": {
                        "Required": true
                    }
                },
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "OpsCompany",
                    "Label": "Company"
                },
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "OpsStreetAddress1",
                    "Label": "Street Address 1"
                },
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "OpsStreetAddress2",
                    "Label": "Street Address 2"
                },
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "OpsCity",
                    "Label": "City",
                    "Validation": {
                        "Required": true
                    }
                },
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "OpsState",
                    "Label": "State/Province",
                    "Validation": {
                        "Required": true
                    }
                },
                {
                    "Type": "Text",
                    "SubType": "Text",
                    "Key": "OpsZipCode",
                    "Label": "Zip Code/Postal Code",
                    "Validation": {
                        "Required": true
                    }
                }
            ]
        },
        {
            "SectionName": "Complete",
            "Fields": []
        }
    ]
}