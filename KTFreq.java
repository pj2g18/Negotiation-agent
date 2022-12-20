//package group3;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KTFreq {

    public static List<List<Double>> getVals(List<List<Integer>> freqTable) {
        List<List<Double>> vals = new ArrayList<>();
        // Each i is an issue
        for (List<Integer> valFreqs : freqTable) {
            double jmost = Collections.max(valFreqs);
            double z = 0;
            for (int vfreq : valFreqs) z += vfreq;
            z -= jmost;

            List<Double> propFreqs = new ArrayList<>();
            for (int vfreq : valFreqs) {
                if (vfreq == jmost) propFreqs.add(1.0);
                else if (z == 0) propFreqs.add(0.0);
                else propFreqs.add(vfreq / z);
            }
            vals.add(propFreqs);
        }
        return vals;
    }

    public static List<Double> getWeights(List<List<Integer>> freqTable, List<List<Double>> vals) {
        List<Double> imps = new ArrayList<>();
        for (int i = 0; i < freqTable.size(); i++) {
            List<Integer> valFreqs = freqTable.get(i);
            List<Double> valPropFreqs = vals.get(i);
            double imp = 0;
            int totalFreq = 0;
            for (int j = 0; j < valFreqs.size(); j++) {
                int valFreq = valFreqs.get(j);
                imp += valFreq * valPropFreqs.get(j);
                totalFreq += valFreq;
            }
            imps.add(imp / totalFreq);
        }

        int k = freqTable.size();
        List<Double> weights = new ArrayList<>();
        for (double imp : imps) {
            double rank = 1;
            for (double i : imps)
                if (imp > i) rank += 1.0;
            weights.add(1.0/(2.0 * k) + (rank - 1)/(k * (k-1)));
        }
        return weights;
    }

    public static double getUtility(Bid bid, List<List<Double>> vals, List<Double> weights) {
        double estimate = 0;
        for (Issue issue : bid.getIssues()) {
            int i = issue.getNumber() - 1;
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            int j = 0;
            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
                if (bid.containsValue(issueDiscrete, valueDiscrete)){
                    estimate += weights.get(i) * vals.get(i).get(j);
                    break;
                }
                j += 1;
            }
        }

        return estimate;
    }
}