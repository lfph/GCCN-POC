'use strict';
const { response } = require('express');
var express = require('express');
const session = require('express-session');
var router = express.Router();
var axios = require('axios');
var path = require('path');

/* GET list of all entries (table) page. */
router.get('/list', function (req, res) {
    axios.get('https://essif.trust-scheme.de/train/api/v1/gccn/trustregistry/gccn-registry.train.trust-scheme.de/')
        .then(function (response) {
            return res.render('./networkEntries/networkEntriesList', {
                currentNavigationName: 'Network Entries',
                title: 'Network Entries',
                json: JSON.stringify(response.data.TrustListEntries)
            });
        }).catch(function (error) {
            console.log('Error: ', err.message);
            return res.json({ success: false });
        });
});

/* Get details of one entry */
router.get('/details/:trustScheme', function (req, res, next) {
    axios.get('https://essif.trust-scheme.de/train/api/v1/gccn/trustlist/individual/' + req.params['trustScheme'])
        .then(function (response) {
            if (response.data.TrustedServiceProviderDetails.length == 0) return next();
            return res.render('./networkEntries/networkEntryDetails', {
                currentNavigationName: 'Network Entries',
                title: 'Network Entry Details',
                json: response.data.TrustedServiceProviderDetails[0]
            });
        }).catch(function (error) {
            console.log('Error: ', err.message);
            return res.json({ success: false });
        });
});

/* Get submission xml */
router.get('/Submission/:FileName', function (req, res, next) {
    var fileName = req.params.FileName;
    var filePath = path.join(__dirname, '../../data/submissions/', fileName);
    res.sendFile(filePath, function (err) {
        if (err) {
            var err = new Error('File was not found.');
            err.status = 404;
            next(err);
        }
    });
});

/* Get verfiy page */
router.get('/Verify', function (req, res, next) {
    return res.render('./networkEntries/verifyNetworkEntry', {
        currentNavigationName: 'Verify',
        title: 'Verify'
    });
});

/* Post from verify page */
/* 
 * Verification logic to be added
 */
router.post('/Verify', function (req, res, next) {
    return res.render('./networkEntries/verificationResult', {
        currentNavigationName: 'Verify',
        title: 'Verify',
        IssuerName: req.body.IssuerName,
        TrustSchemePointer: req.body.TrustSchemePointer
    });
});

module.exports = router;