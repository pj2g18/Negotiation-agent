//package group3;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.timeline.TimeLineInfo;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.commons.math3.util.Pair;

 	
class OppModel	 {


	
	public static double estimateUtility(Bid bid,  List<Double> weights, List<Double >hypEvals) {
		double estimate = 0;
	
		
		for (Issue issue : bid.getIssues()) 
		{
			int i = issue.getNumber() - 1;
			IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) 
			{
				if (bid.containsValue(issueDiscrete, valueDiscrete)){
					estimate += weights.get(i) * hypEvals.get(i);
				//estimate += probs.get(i).get(j) * (weights.get(i)  * 1);
					break;
				}
			}
		}

		return estimate;
	}
	
	public static double estimatePartialUtility(Bid bid, List<Double> weights, List<Double> hypEvals, int issueNum) {
		double estimate = 0;
		for (Issue issue : bid.getIssues()) 
		{
			int i = issue.getNumber() - 1;
			IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) 
			{
				if (i != issueNum) {
					if (bid.containsValue(issueDiscrete, valueDiscrete)){
						estimate += weights.get(i) * hypEvals.get(i);
						//estimate += probs.get(i).get(j) * (weights.get(i)  * 1);
						break;
				}
				}
			}
		}

		return estimate;
	}

	
	static double determineOppUtil(int issSize, int hypValIndex, int bidIndex)
	{
		List<Double> sortedList = new ArrayList<>();
		for (int x = 0; x < issSize; x++)
		{
			sortedList.add((double) x);
		}
		int oppUtilHyp = hypValIndex+1;
		int oppOffer = bidIndex+1;
		
		double util;								

		if (oppOffer < oppUtilHyp)
		{		 
			//leftwards slope			
			double grad = 1/(oppUtilHyp - (sortedList.get(0))); 	 
			util = grad * oppOffer - grad * (sortedList.get(0));
			
		//	util = 0.3;
								
		}else if (oppOffer == oppUtilHyp)
		{
			util = 1;
		}else
		{	
			//rightwards slope
			double grad = -1/((sortedList.get(sortedList.size()-1) + 2) - oppUtilHyp) ;
			util = grad * oppOffer - grad * (oppOffer-sortedList.get(0));
			
		}
						 
		return util;
	}
	
	
	
	static List<List<Double>> initializeEvalProbabilities(List<List<String>> hyps)
	{
		List<List<Double>> probs = new ArrayList<>();
		
		for (int i = 0; i < hyps.size(); i++) 
		{
			List<Double> valueProbs = new ArrayList<>();
			for (int j = 0;j <  hyps.get(i).size(); j++)
			{
				valueProbs.add((double) (1.0/hyps.get(i).size()));

			}
			probs.add(valueProbs);
		}
		return probs;
	}
	
	public static List<Double> hypEvaluations(List<List<Double>> probs, Bid bid, List<List<String>> hyps )
	{
		List<Double> bidEval = new ArrayList<>();
		double util;
		List<Integer> bidNums = new ArrayList<>();
		for (Issue issue: bid.getIssues()) {
			IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
			int bidNum = 0;
			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
				if (bid.containsValue(issueDiscrete, valueDiscrete)){
					bidNums.add(bidNum);
				}
				bidNum += 1;

			}
		}
		
		
		for (int i = 0; i < probs.size(); i++)
		{
			double issueProb = 0;

			for (int j = 0; j < probs.get(i).size(); j++)
			{
				util = determineOppUtil(probs.get(i).size(), j, bidNums.get(i) );
				issueProb += probs.get(i).get(j) * util;

				
			}
			bidEval.add(issueProb);
		}

		
		return bidEval;
	}
	
	public static List<Double> weightEvaluations(List<List<Double>> weightHyps, List<List<Double>> weightProbs) {
		int numOfHyps = 5;
		List<Double> weights = new ArrayList<>();

		for (int i = 0; i < weightHyps.size(); i++)
		{
			double issueEval = 0;
			double sum = weightProbs.get(i).stream().mapToDouble(Double::doubleValue).sum();
			for (int j = 0; j < weightHyps.get(i).size(); j++)
			{
				issueEval+= weightProbs.get(i).get(j) * weightHyps.get(i).get(j);
			}
			weights.add(issueEval);
		}
		
		double sum = weights.stream().mapToDouble(Double::doubleValue).sum();
		for (int i = 0; i < weights.size(); i++)
		{
			weights.set(i, weights.get(i)/sum);
		}
		

		
		return weights;
		
	}
	
	public static List<List<Double>> initializeWeightProbs(List<List<Double>> weightHyps) 
	{
		List<List<Double>> weightProbs = new ArrayList<>();
		for (int i = 0; i < weightHyps.size(); i++) 
		{
			List<Double> issueHyps = new ArrayList<>();

			for (int j = 0; j < weightHyps.get(i).size(); j++)
			{
				issueHyps.add(1.0/weightHyps.get(i).size());
			}
			weightProbs.add(issueHyps);

		}
		
		return weightProbs;
	}
	
	
	public static List<List<Double>> initialiseWeightHyps(int numOfHyps, int numOfIssues) 
	{
		List<List<Double>> weightHyps = new ArrayList<>();
		for (int i = 0; i < numOfIssues; i++) 
		{
			List<Double> issueHyps = new ArrayList<>();

			for (int j = 0; j < numOfHyps; j++)
			{
				issueHyps.add((j+1.0)/(numOfHyps));
			}
			weightHyps.add(issueHyps);
		}
		return weightHyps;
	}
	
	
	static List<List<Double>> determineEvalCondProbs(List<List<Double>> probs, List<Double> hypEvals, double nextUtil, List<Double> weights, double sigma, Bid bid)
	{
		List<List<Double>> condProbs = new ArrayList<>();
		
		List<Integer> bidNums = new ArrayList<>();

		for (Issue issue: bid.getIssues()) {
			IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
			int bidNum = 0;
			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
				if (bid.containsValue(issueDiscrete, valueDiscrete)){
					bidNums.add(bidNum);
				}
				bidNum += 1;

			}
		}
		
		
		double util;
		for (int i = 0; i < probs.size(); i++) 
        {
			double totalProbs = 0;

			double partialUtil = estimatePartialUtility(bid, weights, hypEvals, i);
			List<Double> issueCondProbs = new ArrayList<>();
			for (int j = 0; j < probs.get(i).size(); j++) 
            {	
				util = determineOppUtil(probs.get(i).size(),j ,bidNums.get(i));
				if (i == 0) {

				}
				double condprob = Math.exp(-Math.pow((partialUtil + weights.get(i) * (1-util))  - nextUtil, 2)/(2 * Math.pow(sigma, 2) ))/(sigma * Math.sqrt(2 * Math.PI));
				issueCondProbs.add(condprob);
				totalProbs += condprob;
            }
			for (int n = 0; n <  issueCondProbs.size(); n++) {
		//		issueCondProbs.set(n, issueCondProbs.get(n)/totalProbs);
			}
			condProbs.add(issueCondProbs);
        }
		
		
		
		return condProbs;
	}
	
	static List<List<Double>> determineWeightCondProbs(List<List<Double>> weightHyps, List<Double> hypEvals, double nextUtil, List<Double> weights, double sigma, Bid bid)
	{
		List<List<Double>> condProbs = new ArrayList<>();
		double sum = 0;

		for (int i = 0; i < weightHyps.size(); i++) 
        {
			double totalProbs = 0;

			double util = estimatePartialUtility(bid, weights, hypEvals, i);
			List<Double> issueCondProbs = new ArrayList<>();
			sum +=weights.get(i);	
			for (int j = 0; j < weightHyps.get(i).size(); j++) 
            {
				
				
				double condprob = Math.exp(-Math.pow((util + weights.get(i) * weightHyps.get(i).get(j))  -	nextUtil, 2)/(2 * Math.pow(sigma, 2) )) / (sigma * Math.sqrt(2 * Math.PI)) ;
				issueCondProbs.add(condprob);
				totalProbs += condprob;

            }
			for (int n = 0; n <  issueCondProbs.size(); n++) {
				issueCondProbs.set(n, issueCondProbs.get(n)/totalProbs);
			}
			condProbs.add(issueCondProbs);
        }
		return condProbs;
	}
		
	public static List<List<Double>> updateUtilProbs(List<List<Double>> probs,List<Double> hypEvals, List<Double> weights, double sigma, Bid bid, TimeLineInfo timeline ,List<Double> times, List<Double> utils)
	{
		double t = timeline.getCurrentTime();
		//double nextUtil = getDeadline(times, utils, timeline);
		double nextUtil = 1 - 0.05*t;

		
		
		List<List<Double>> evalCondProbs = determineEvalCondProbs(probs, hypEvals, nextUtil, weights, sigma, bid);

		List<List<Double>> newProbs = new ArrayList<>();
		List<Double> summedProbs = new ArrayList<>();		
		for (int i = 0; i < probs.size(); i++)
		{
			double prob = 0;
			for (int j = 0; j < probs.get(i).size(); j++)
			{
				prob += probs.get(i).get(j) * evalCondProbs.get(i).get(j);
			}
			summedProbs.add(prob);
		}

		for (int i = 0; i < probs.size(); i++)
		{
			List<Double> issueProbs = new ArrayList<>();

			for (int j = 0; j < probs.get(i).size(); j++)
			{
				
				
				double newProb = probs.get(i).get(j) * evalCondProbs.get(i).get(j) / summedProbs.get(i);
				issueProbs.add(newProb);
			}
			
			double sum = issueProbs.stream().mapToDouble(Double::doubleValue).sum();
			newProbs.add(issueProbs);
		}

		return newProbs;
	}
	
	static List<List<Double>> updateWeightProbs(List<List<Double>> weightHyps, List<List<Double>> weightProbs, List<Double> hypEvals, List<Double> weights,	double sigma, Bid bid, TimeLineInfo timeline, List<Double> times, List<Double> utils) {
		double t = timeline.getCurrentTime();
		double delta = 0.8;
		
		//double nextUtil = getDeadline(times, utils, timeline);
		double nextUtil = 1 - 0.05*t;
		
		System.out.println("next util: " + nextUtil);
		List<List<Double>> weightCondProbs = determineWeightCondProbs(weightHyps, hypEvals, nextUtil, weights, sigma, bid);
		List<List<Double>> newProbs = new ArrayList<>();
		List<Double> summedProbs = new ArrayList<>();
		for (int i = 0; i < weightProbs.size(); i++)
		{
			double prob = 0;

			for (int j = 0; j < weightProbs.get(i).size(); j++)
			{
				prob += weightProbs.get(i).get(j) * weightCondProbs.get(i).get(j);
			}
			summedProbs.add(prob);
		}
		
		for (int i = 0; i < weightProbs.size(); i++)
		{
			List<Double> issueProbs = new ArrayList<>();

			for (int j = 0; j < weightProbs.get(i).size(); j++)
			{
				
				double numerator = weightProbs.get(i).get(j);
				
				double newProb = weightProbs.get(i).get(j) * weightCondProbs.get(i).get(j) / summedProbs.get(i);
				issueProbs.add(newProb);
			}
			newProbs.add(issueProbs);
			double sum = issueProbs.stream().mapToDouble(Double::doubleValue).sum();
		}
		return newProbs;
	}

	static List<List<String>> initialiseEvalHyps(List<List<String>> evals)
	{		
		int issuenum = evals.size();
		List<List<String>> hypSpace = new ArrayList<>();

		for (int i = 0; i < issuenum; i++) 
		{
			List<String> issueHyps = new ArrayList<>();
			issueHyps = evals.get(i);
			hypSpace.add(issueHyps);
		}
		return hypSpace;
	}
	public static boolean isNumeric(String strNum) {
	    if (strNum == null) {
	        return false;
	    }
	    try {
	        double d = Double.parseDouble(strNum);
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	    return true;
	}

	public static double getDeadline(List<Double> times, List<Double> utils, TimeLineInfo timeline)
	{
		 SimpleRegression simpleRegression = new SimpleRegression(true);

	     // passing data to the model
	     // model will be fitted automatically by the class
		 double[] target = new double[times.size()];
		 for (int i = 0; i < target.length; i++) {
		    target[i] = times.get(i).doubleValue();  // java 1.4 style
		    target[i] = times.get(i);                // java 1.5+ style (outboxing)
		    
		 }
		 
		 
		 HashMap<Double, Double> entries = new HashMap<Double, Double>();
		 
		 List<Map.Entry<Double,Double>> subjectNumArr = new ArrayList<>(times.size());
		
		 for (int i = 0; i < times.size(); i++)
		 {
			 entries.put(times.get(i), utils.get(i));
		 }
		
		 double[][] arr = new double[times.size()][2];
		 Set entrs = entries.entrySet();
		 Iterator entriesIterator = entrs.iterator();

		 int i = 0;
		 System.out.println(times);
		 System.out.println(utils);
		 for (Entry<Double, Double> entry : entries.entrySet())
		 {
			 

		     arr[i][0] = entry.getKey();
		     arr[i][1] = entry.getValue();
		     System.out.println(entry.getKey());
		     i++;
		 }
		 
		 System.out.println("arr: " + arr[0][0]);
		 System.out.println(arr[0][1]);

		 
	     simpleRegression.addData(arr
	        );
	        System.out.println("slope = " + simpleRegression.getSlope());
	        System.out.println("intercept = " + simpleRegression.getIntercept());
			double time = timeline.getCurrentTime();
			
			
			if (times.size() < 4){
				return 1;
			}else {
			time = (times.get(times.size()-1)) + 0.005;
	        return simpleRegression.getSlope() * time + simpleRegression.getIntercept();
			}
			//return 1 - 0.05 * time;
	}
	
	
	
	
	
}

