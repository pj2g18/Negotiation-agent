
import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JBFreq {
    public static List<List<Double>> getVals(List<List<Integer>> freqTable) {
        List<List<Double>> vals = new ArrayList<>();

        for (int i = 0; i < freqTable.size(); i++) {
            double k = freqTable.get(i).size();
            List<Double> issueVals = new ArrayList<>();
            for (int val : freqTable.get(i)) {
                double rank = 1;
                for (int testVal : freqTable.get(i)) {
                    if (val < testVal) rank++;
                }
                issueVals.add((k - rank + 1) / k);
            }
            vals.add(issueVals);
        }
        return vals;
    }

    public static List<Double> getWeights(List<List<Integer>> freqTable) {
        List<Double> weights = new ArrayList<>();

        for (List<Integer> offerFreq : freqTable) {
            double numBids = 0;
            double freqSquared = 0;
            for (Integer f : offerFreq) {
                numBids += f;
                freqSquared += (f * f);
            }
            weights.add(freqSquared / (numBids * numBids));
        }

        double weightsSum = 0;
        for (Double w : weights) {
            weightsSum += w;
        }

        for (int i = 0; i < weights.size(); i++) {
            weights.set(i, weights.get(i) / weightsSum);
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

    public static List<List<Integer>> addToFreqTable(Bid bid, List<List<Integer>> frequencyTable) {
        for (Issue issue: bid.getIssues()) {
            int i = issue.getNumber() - 1;
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            int j = 0;
            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
                if (bid.containsValue(issueDiscrete, valueDiscrete)){
                    int oldVal = frequencyTable.get(i).get(j);
                    frequencyTable.get(i).set(j,oldVal + 1);
                    break;
                }
                j += 1;
            }
        }
        
        return frequencyTable;
    }
}