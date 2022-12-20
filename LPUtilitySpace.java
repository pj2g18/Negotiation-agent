//package group3;

import genius.core.Bid;
import genius.core.Domain;
import genius.core.uncertainty.BidRanking;
import genius.core.utility.CustomUtilitySpace;
import gurobi.GRBException;

public class LPUtilitySpace extends CustomUtilitySpace  {
    private PreferenceEstimator pe;
    public LPUtilitySpace(Domain domain, BidRanking bidRanking) {
        super(domain);
        try {
            pe = new PreferenceEstimator(bidRanking);
        } catch (GRBException e) {
            // Should log an error or something
        	System.out.println(e);
        	System.out.println("FAILED");
            pe = null;
        }
    }

    @Override
    public double getUtility(Bid bid) {
        return pe.getUtility(bid);
    }
    
}