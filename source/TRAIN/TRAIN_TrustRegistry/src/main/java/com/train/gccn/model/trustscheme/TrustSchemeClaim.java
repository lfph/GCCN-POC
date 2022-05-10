package com.train.gccn.model.trustscheme;

import java.util.Objects;

public class TrustSchemeClaim {
    
    private String claim;
    
    public TrustSchemeClaim(String claim) {
        this.claim = claim;
    }
    
    @Override
    public String toString() {
        return this.getClaimCleaned();
    }
    
    public String getClaim() {
        return this.claim;
    }
    
    public String getClaimCleaned() {
        String claim = this.getClaim().trim();
        return TrustScheme.cleanSchemeIdentifier(claim);
    }
    
    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        TrustSchemeClaim that = (TrustSchemeClaim) o;
        return this.claim.equals(that.claim);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(this.claim);
    }
}
