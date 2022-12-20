//package group3;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.OutcomeComparison;

import gurobi.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class PreferenceEstimator {

    List<Double> phiVals;
    public PreferenceEstimator(BidRanking bidRanking) throws GRBException {

        int phisNum = 0;
        for (Issue issue : bidRanking.getBidIssues()) {
            phisNum += ((IssueDiscrete) issue).getValues().size();
        }
        int numCompare = bidRanking.getAmountOfComparisons();

        GRBEnv env = new GRBEnv(true);
        env.start();
        GRBModel model = new GRBModel(env);

        List<GRBVar> zs = new ArrayList<>();
        List<GRBVar> phis = new ArrayList<>();

        GRBLinExpr expr = new GRBLinExpr();
        for (int i = 0; i < numCompare; i++) {
            GRBVar v = model.addVar(0.0, GRB.INFINITY, 1.0, GRB.CONTINUOUS, "z"+i);
            zs.add(v);
            expr.addTerm(1.0, v);
        }
        for (int i = 0; i < phisNum; i++) {
            GRBVar v = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "phi"+i);
            phis.add(v);
            expr.addTerm(0.0, v);
        }
        model.setObjective(expr, GRB.MINIMIZE);

        List<OutcomeComparison> outcomes = bidRanking.getPairwiseComparisons();

        for (int i = 0; i < outcomes.size(); i++) {
            OutcomeComparison outcome = outcomes.get(i);
            GRBLinExpr uzExpr = new GRBLinExpr();
            // need zoo' + duoo' >= 0
            uzExpr.addTerm(1.0, zs.get(i));
            // duoo'
            List<Issue> issues = bidRanking.getBidIssues();
            Bid bid1 = outcome.getBid1();
            Bid bid2 = outcome.getBid2();
            // Keeps track of which issue's phis we are looking at
            int currentIssue = 0;
            for (int j = 0; j < bidRanking.getBidIssues().size(); j++) {
                IssueDiscrete issue = (IssueDiscrete) issues.get(j);

                int v1 = issue.getValueIndex((ValueDiscrete) bid1.getValue(issue));
                int v2 = issue.getValueIndex((ValueDiscrete) bid2.getValue(issue));

                uzExpr.addTerm(-1.0,phis.get(currentIssue + v1));
                uzExpr.addTerm(1.0,phis.get(currentIssue + v2));

                currentIssue += issue.getNumberOfValues();
            }
            model.addConstr(uzExpr, GRB.GREATER_EQUAL, 0.0, "uz"+i);
        }

        //Expression maxExp = model.addExpression("maximal").level(1);
        GRBLinExpr maxExp = new GRBLinExpr();
        Bid bestBid = bidRanking.getMaximalBid();
        List<Issue> issues = bestBid.getIssues();
        int currentIssue = 0;
        for (Issue issue : issues) {
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            int v = issueDiscrete.getValueIndex((ValueDiscrete) bestBid.getValue(issueDiscrete));
            maxExp.addTerm(1.0, phis.get(currentIssue + v));
            currentIssue += issueDiscrete.getNumberOfValues();
        }
        model.addConstr(maxExp, GRB.EQUAL, 1.0, "maximal");

        GRBLinExpr minExp = new GRBLinExpr();
        Bid worstBid = bidRanking.getMinimalBid();
        currentIssue = 0;
        for (Issue issue : issues) {
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            int v = issueDiscrete.getValueIndex((ValueDiscrete) worstBid.getValue(issueDiscrete));
            minExp.addTerm(1.0, phis.get(currentIssue + v));
            currentIssue += issueDiscrete.getNumberOfValues();
        }
        model.addConstr(minExp, GRB.EQUAL, 0.0, "minimal");

        model.optimize();

        phiVals = new ArrayList<>();
        for (GRBVar phi : phis) {
            phiVals.add(phi.get(GRB.DoubleAttr.X));
        }
//        try {
//            PrintWriter writer = new PrintWriter("the-file-name.txt", "UTF-8");
//
//            System.out.println(model.get(GRB.DoubleAttr.ObjVal));
//            for (GRBVar z : zs) {
//                writer.println(z.get(GRB.StringAttr.VarName) + " : " + z.get(GRB.DoubleAttr.X));
//            }
//            for (GRBVar phi : phis) {
//                writer.println(phi.get(GRB.StringAttr.VarName) + " : " + phi.get(GRB.DoubleAttr.X));
//            }
//            writer.close();
//        } catch (FileNotFoundException | UnsupportedEncodingException e) {
//            throw new RuntimeException(e);
//        }
        model.dispose();
        env.dispose();
    }

    public Double getUtility(Bid bid) {
        double utility = 0;
        int currentIssue = 0;
        for (Issue issue : bid.getIssues()) {
            int v = ((IssueDiscrete) issue).getValueIndex((ValueDiscrete) bid.getValue(issue));
            utility += phiVals.get(currentIssue + v);
            currentIssue += ((IssueDiscrete) issue).getNumberOfValues();
        }
        return utility;
    }
}