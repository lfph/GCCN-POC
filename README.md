# GCCN Trust Registry Network - Proof of Concept


## Initiative Background

Global COVID Certificate Network (GCCN) is an initiative at Linux Foundation Public Health. In response to the recommendations from the Good Health Pass work, we launched GCCN in June 2021 to enable interoperable and trustworthy verification of COVID certificates between jurisdictions for safe border reopening.
Initial scope for GCCN
* A trust registry network that will provide a way for the various COVID certificate systems in any jurisdiction to find each other and determine whether to accept each other’s certificates. 
* A complete toolkit to build COVID certificate ecosystems, which includes a governance framework template, schema definitions and minimum datasets, technical specifications, implementation guides, and open source reference implementations.
* A vendor network for GCCN, who can competently work on these kinds of projects, so that governments and institutions can easily get running.


### What is the GCCN?  

The GCCN Trust Registry Network is an online resource that provides human and machine readable information pertaining to it’s network entries. Network participants consist of public and private sector entities providing the issuance and/or verification of digital or digitized paper Covid Certificates, Credentials or Passes required for use by jurisdictions to allow free and safe movement within or across locales. 

The GCCN Trust Registry Network will provide it’s users with qualitative information regarding the validity of each registry entry. The Registry Network will support pre-operational discovery of any registry entry, presenting registry users with vetted descriptions for each registry entry. 


### TRAIN (TRust mAnagement INfrastructure): Trust Management Infrastructure Component for the ESSIF-Lab Architecture

Our component aims to extend the ESSIF-Framework through a flexible trust infrastructure that can be used to verify the trustworthiness of involved parties exchanging credentials. 
TRAIN is a software component that allows for the definition, consideration, and verification of Trust Schemes compliance (e.g. eIDAS including LoAs or other Trust Schemes that can also be application/industry-specific) of involved parties. It is not dependent on a hierarchical CA infrastructure.
The component builds on the infrastructure developed in the EU project LIGHTest (2016-2020, G.A. No. 700321). The trust layer is flexible, individual parties can define their own trust policies, manage and publish them.
TRAIN is fully in line with the open and decentral SSI approach and complements other approaches.


## GCCN Meta-Network Solution

### Identifying Trusted Entities

The challenge of discovering and verifying trusted issuers of Covid certificates is addressed by GCCN:
1. Globally discoverable ...
2. Local definition and control ... 
3. Vetted entries ... 
4. Root of Trust Agnostic ... 


### GCCN Technical Solution

### TRAIN (TRust mAnagement INfrastructure): Trust Management Infrastructure Component

*  introduces a  trust infrastructure that allows to verify the trustworthiness of involved parties (in an electronic transaction), e.g. is the issuer trustworthy (is it a real bank or just a fake bank)
*  adds a Trust-Component to ESSIF-Framework, which enables for the verification credential issuers, as well as the definition, consideration and verification of eIDAS compliance (including LoAs) of involved parties
 
TRAIN provides a decentralized framework for the publication and querying of trust information, conceptually comparable to OCSP. It makes use of the Internet Domain Name System (DNS) with its existing global infrastructure, organization, governance and security standards.
The trust layer is flexible, individual parties can define their own trust policies, manage and publish them.
TRAIN is fully in line with the open and decentral SSI approach and complements other approaches.
Trust standards such as Trust Schemes (eIDAS, Pan Canadian Trust Framework, but also self-defined Schemes and Policies) can be integrated. 

TRAIN is a software component. Information is published via the so called TSPA server. The chain of trust is verified using DNSSEC components and the verification of a root of trust is achieved through the use of published Trust Policies. The basic technology has already been validated through pilots and prototypes (outside the SSI-context).

The general approach of TRAIN is described in the following paper: https://dl.gi.de/handle/20.500.12116/36489 .
The TRAIN component builds upon work completed in the EU H2020 LIGHTest research project (www.lightest.eu).

**Key Concepts of TRAIN**
*  TSPA - Trust Scheme Publication Authority, a higher-level component to develop and publish different trust schemes

*  TPA - Trust Publication Authority, maintains and publishes a list of trusted issuers (Trust List with Issuer Details)

*  ATV – Automatic Trust Verifier, performs trustworthiness verification of the issuer based on the DNS host name of the TPA 

**The TRAIN API**

The TRAIN components are access via a REST API, the API provides methods for inserting, updating and verifiying the status of GCCN Trust Registry entries.
The API is accessible via Swagger at: https://essif.trust-scheme.de/swagger_train_gccn_v2/#/TRAIN_GCCN

**Licensing**
UNDP components will be released under Apache 2.0 license.

For more information, please contact:
lucy.cci@lfph.io
