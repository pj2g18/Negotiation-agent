//package group3;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.Domain;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.EndNegotiation;
import genius.core.actions.Offer;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.ExperimentalUserModel;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;
import genius.core.utility.UncertainAdditiveUtilitySpace;

import java.io.BufferedWriter;
import java.io.File;  // Import the File class
import java.io.IOException;  // Import the IOException class to handle errors
import java.io.OutputStreamWriter;

/**
 * A simple example agent that makes random bids above a minimum target utility. 
 *
 * @author Tim Baarslag
 */
public class MyAgent extends AbstractNegotiationParty 
{
	Random random = new Random();
	List<List<Double>>probs = new ArrayList<>();
	List<List<String>>hyps = new ArrayList<>();
	List<List<Double>>weightProbs = new ArrayList<>();
	List<List<Double>>weightHyps = new ArrayList<>();
	List<List<String>> evalHypotheses = new ArrayList<>();
	List<Double> trust = new ArrayList<>();
	List<Double> utils = new ArrayList<>();
	List<Double> times = new ArrayList<>();

	private static double MINIMUM_TARGET = 0.3;
	private static double MAXIMUM_TARGET = 0.95;
	double currentTarget = 0.9;
	int numOfExperts = 3;
	double delta = 0.25;
	private Bid lastOffer;
	private Bid lastLastOffer;
	private List<List<Integer>> freqTable;
	private List<List<Integer>> ownFreqTable;
	AbstractUtilitySpace utilitySpace;
	UncertainAdditiveUtilitySpace realUtilitySpace;
	private List<Bid> allBids;
	List<Bid> offerHistory = new ArrayList<>();
	int fileNumber = 0;

	//private AdditiveUtilitySpace additiveUtilitySpace;
	private double[] K;
	private double deadline = 0.9;
	private double U = 0.1; //Time reasonable to negotiate with single agent.

	double sigCons = 0.75;
	double sigcons2 = 0;
	
	class BidComparator implements Comparator{ //Note - just to make sorting bids easier this is currently done in reverse (so b1 < b2 -> 1 instead of -1 like normal.)

		public int compare(Object o1, Object o2){

			Bid b1 = (Bid) o1;
			Bid b2 = (Bid) o2;

			double u1 = utilitySpace.getUtility(b1);
			double u2 = utilitySpace.getUtility(b2);
			if(u1 == u2){
				return 0;
			}else if(u1 < u2){
				return 1;
			}else{
				return -1;
			}
		}
	}

	/**
	 * Initializes a new instance of the agent.
	 */
	@Override
	public void init(NegotiationInfo info) 
	{
		super.init(info);
		initialiseTrust(trust);
		int numOfFiles = new File("log").list().length;
		sigcons2 = 1.2;
		sigCons =  Math.floor((double) numOfFiles/ 2) * 0.1 ;
		sigCons = 0.3;
		System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));

		if (hasPreferenceUncertainty()) {
		//	ExperimentalUserModel e = (ExperimentalUserModel) userModel;
		//	realUtilitySpace = e.getRealUtilitySpace();
			utilitySpace = estimateUtilitySpace(info.getUtilitySpace().getDomain());
		}

		//additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;


		List< Issue > issues = utilitySpace.getDomain().getIssues();
		freqTable = new ArrayList<>();
		ownFreqTable = new ArrayList<>();
		K = new double[issues.size()];
		
		createFile();

		for (Issue issue : issues) {

			List<Integer> valFreqList = new ArrayList<>();
			List<Integer> ownValFreqList = new ArrayList<>();

			int issueNumber = issue.getNumber();
	

			// Assuming that issues are discrete only
			IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
			
			
			//EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);

			List<Double> probList = new ArrayList<>();
			Double probVal = 1 / (double) issueDiscrete.getValues().size(); //Begin by assuming that all values have the same probability of being liked.
			List<String> valHypotheses = new ArrayList<>();


			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
				valFreqList.add(0);
				ownValFreqList.add(0);
				probList.add(probVal);
			
				String val = valueDiscrete.getValue();
				
				valHypotheses.add(val);
				
			}
			evalHypotheses.add(valHypotheses);
			freqTable.add(valFreqList);
			ownFreqTable.add(ownValFreqList);
			K[issueNumber - 1] = 0.1; //TODO - decide what constant this should start with.
			allBids = getAllPossibleBids(issues);
		}

		//double mse = 0;
		//for (Bid bid : allBids) {
		//	double util = realUtilitySpace.getUtility(bid);
		//	double estUtil = utilitySpace.getUtility(bid);
		//	mse += Math.pow(util - estUtil, 2);
		//}
		//mse /= allBids.size();
		//.out.println("MSE before elicit: " + mse);

		Bid maxBid = userModel.getBidRanking().getMaximalBid();
		Bid minBid = userModel.getBidRanking().getMinimalBid();
		for (Issue issue : maxBid.getIssues()) {
			Bid bidClone = new Bid(maxBid);
			bidClone = bidClone.putValue(issue.getNumber(),minBid.getValue(issue));
			userModel = user.elicitRank(bidClone, userModel);
		}
		utilitySpace = estimateUtilitySpace(info.getUtilitySpace().getDomain());

		//mse = 0;
		//for (Bid bid : allBids) {
		//	double util = realUtilitySpace.getUtility(bid);
		//	double estUtil = utilitySpace.getUtility(bid);
		//	mse += Math.pow(util - estUtil, 2);
		//}
		//mse /= allBids.size();
		//System.out.println("MSE after elicit: " + mse);

		hyps = OppModel.initialiseEvalHyps(evalHypotheses);
		probs = OppModel.initializeEvalProbabilities(hyps);
		weightHyps = OppModel.initialiseWeightHyps(10, evalHypotheses.size());
		weightProbs = OppModel.initializeWeightProbs(weightHyps);
	}
	
	public void initialiseTrust(List<Double> trust) {
		for (int i = 0; i < numOfExperts; i++)
		{
			trust.add((double) ((1.0/numOfExperts) * 100.0));
		}
	}
	@Override
	public void receiveMessage(AgentID sender, Action action) 
	{
		

		//For updating number of parties
		super.receiveMessage(sender,action);
		double sigma = 0;
		//System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
		double t = getTimeLine().getTime();

		sigma = sigcons2 - sigCons*t;
		double B = 1.5;
		//double sigma  = 2 - (1.6 * t);
		//double sigma = 1.5 - (1.4 * getTimeLine().getTime());
		//double sigma = sigCons + 0.75 * getTimeLine().getTime();
		//double sigma = 1.5 t;
		if (action instanceof Offer)
		{
			if (lastOffer != null) {
				lastLastOffer = lastOffer;
			}
			lastOffer = ((Offer) action).getBid();
			
			
			
	        int simCounter =0;
		
			
			//if (simCounter == offerHistory.size()) {
			double time = timeline.getTime();

			
				offerHistory.add(lastOffer);
				
				if (utils.size() >= 5) 
				{
					utils.remove(0);
					times.remove(0);
				}
				utils.add(getOppUtility(lastOffer));
				times.add(time);
				List<Double> evalWeights = OppModel.weightEvaluations(weightHyps, weightProbs);
				List<Double> evalHyps = OppModel.hypEvaluations(probs, lastOffer, hyps);
				probs = OppModel.updateUtilProbs(probs, evalHyps, evalWeights, sigma, lastOffer, timeline, times, utils);

				weightProbs = OppModel.updateWeightProbs(weightHyps, weightProbs, evalHyps, evalWeights, sigma, lastOffer, timeline, times, utils);
				System.out.println("freqtable: " + freqTable);
			//}
			
			

			
			JBFreq.addToFreqTable(lastOffer, freqTable);
			//System.out.println(freqTable);

	//		double opp1Util;
	//		double opp2Util;
	//		double opp3Util;
	//		evalWeights = OppModel.weightEvaluations(weightHyps, weightProbs);
	//		evalHyps = OppModel.hypEvaluations(probs, lastOffer, hyps);
	//		opp1Util = OppModel.estimateUtility(lastOffer, evalWeights, evalHyps);
			
	//
	//		List<Double> oppUtils = new ArrayList<>();

		//
		//	oppUtils.add(opp1Util);
		//	oppUtils.add(opp2Util);
		//	oppUtils.add(opp3Util);
		//	determineUtils(oppUtils, 100);
		//	updateTrust(oppUtils, trust);
		//	System.out.println("OPPUTIL: " + oppUtil);


		}

	}

	public void updateTrust(List<Double> newTrust, List<Double> trust)
	{
		int i = 0;
		for (Double item : trust)
		{
			trust.set(i, (item + newTrust.get(i))/2);
			i += 1;
		}

	}
	public void determineUtils(List<Double> oppUtils, int ranks)
	{
		//actualdouble Util = getOppUtil();
		double time = timeline.getTime();
		deadline = 0.0;
		//double actualUtil = 1 * Math.pow(delta, time);
		double actualUtil = 1;
		double summedUtil = 0;
		int index = 0;
		for (Double oppUtil : oppUtils)
		{
			oppUtils.set(index, Math.pow(1 - (actualUtil - oppUtil), 2));


			index += 1;
			summedUtil += Math.pow(1 -(actualUtil - oppUtil), 2);
		}
		index = 0;
		for (Double oppUtil : oppUtils)
		{
			oppUtils.set(index,  (oppUtil * ranks) / summedUtil);
			index += 1;
		}

	}

	public List<Bid> getAllPossibleBids(List<Issue> issues){
		List<Bid> bids = new ArrayList<>();
		Bid currBid = new Bid(userModel.getBidRanking().getBidOrder().get(0)); //Create default bid with all the issues in it - since we can't add issues to bid.

		bids = getBidsMap(issues,0,currBid,bids);

		/*for(Bid bid : bids){

		}*/
		return bids;
	}

	private String testBids(HashMap<Integer, ?> map) {
		StringBuilder mapAsString = new StringBuilder("{");
		for (Integer key : map.keySet()) {
			mapAsString.append(key + "=" + map.get(key) + ", ");
		}
		mapAsString.delete(mapAsString.length()-2, mapAsString.length()).append("}");
		return mapAsString.toString();
	}

	public List<Bid> getBidsMap(List<Issue> issues,int i,Bid currBid, List<Bid> bids){
		if(i >= issues.size()){
			return insertByUtility(bids,currBid);
		}

		IssueDiscrete currIssue = (IssueDiscrete) issues.get(i);
		int issueNum = currIssue.getNumber();
		int nextI = i+1;

		for(ValueDiscrete valueDiscrete : currIssue.getValues()){
			Bid bidClone = new Bid(currBid);
			bidClone = bidClone.putValue(issueNum,valueDiscrete);
			bids = getBidsMap(issues,nextI,bidClone,bids);
		}
		return bids;

	}

	List<Bid> insertByUtility(List<Bid> bids, Bid bid) {
		int index = Collections.binarySearch(bids, bid, new BidComparator());
		if (index < 0) {
			index = -index - 1;
		}
		bids.add(index, bid);
		return bids;
	}

	/**
	 * Makes a random offer above the minimum utility target
	 * Accepts everything above the reservation value at the end of the negotiation; or breaks off otherwise. 
	 */
	@Override
	public Action chooseAction(List<Class<? extends Action>> possibleActions) 
	{
		// Check for acceptance if we have received an offer
		double time = timeline.getTime();
		deadline = 0.0;

		// double currentTarget = MINIMUM_TARGET * Math.pow(delta, time);

//		double ft = Math.pow(time, delta);
//		double currentTarget = MINIMUM_TARGET + ((1 - ft) * (1 - MINIMUM_TARGET));

		//deadline = dynamicDeadline();

		Offer newOffer = null;
		if (time < 0.05) {
			newOffer = new Offer(getPartyId(), userModel.getBidRanking().getMaximalBid());
		} else if (time < 0.5) {
			currentTarget = 0.6 + (1 - Math.pow(time, 0.5)) * (1.0 - 0.6);
			Bid bid = generateBidFromAllBids(currentTarget);
			newOffer = new Offer(getPartyId(), bid);
		} else if (time < 0.9) {
			if (lastLastOffer != null) {
				//double diff = utilitySpace.getUtility(lastOffer) - utilitySpace.getUtility(lastLastOffer);
				double diff = getOppUtility(lastLastOffer) - getOppUtility(lastOffer);
				currentTarget -= diff;
				//currentTarget = Math.max(currentTarget, MAXIMUM_TARGET - 0.3 * Math.pow(time, 2));
			}

			Bid nashBid = estimateNashPoint(allBids);
			double nashUtil = utilitySpace.getUtility(nashBid);

			if (utilitySpace.getUtility(lastOffer) > currentTarget) {

				return new Accept(getPartyId(), lastOffer);
			}

			Bid generatedBid = generateBidFromAllBids(currentTarget);
			double util1 = utilitySpace.getUtility(generatedBid);

//			if (util1 > nashUtil) newOffer = new Offer(getPartyId(), generatedBid);
//			else
			newOffer = new Offer(getPartyId(), nashBid);
		} else if(time < 0.99) {
			if (utilitySpace.getUtility(lastOffer) > currentTarget) {
				return new Accept(getPartyId(), lastOffer);
			}
			Bid nashBid = estimateNashPoint(allBids);
			newOffer = new Offer(getPartyId(), nashBid);
		} else {
			if (utilitySpace.getUtility(lastOffer) >= utilitySpace.getReservationValueUndiscounted())
				return new Accept(getPartyId(), lastOffer);
			else
				return new EndNegotiation(getPartyId());
		}

		JBFreq.addToFreqTable(newOffer.getBid(), ownFreqTable);
		return newOffer;
	}

	//Maximise opp utility, get opp bid.
	Bid generateBidFromAllBids(double target) {

		Bid currBid = allBids.get(0);
		double maxOppUtil = 0.0;
		List<Double> trueUtils= OppGrapher.getOpp1Utils(allBids);
		double avgErr = 0;
		int i = 0;
		for(Bid bid : allBids){
			double util = utilitySpace.getUtility(bid);
			avgErr += (Math.abs(getOppUtility(bid) - trueUtils.get(i)));
			if (util > target) {
				double oppUtil = getOppUtility(bid) + getDifference(bid);
				if (oppUtil > maxOppUtil) {
					currBid = bid;
					maxOppUtil = oppUtil;
				}
			}
			i += 1;
		}
		try {
			writeOppBidAndUtility(allBids.get(10), avgErr/i);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return currBid;
	}

	private double getDifference(Bid bid) {
		double diffScore = 0;
		List<Issue> issues = bid.getIssues();
		for (int i = 0; i < issues.size(); i++) {
			IssueDiscrete issue = (IssueDiscrete) issues.get(i);
			ValueDiscrete val = (ValueDiscrete) bid.getValue(issue);
			int valIndex = issue.getValueIndex(val);

			int issueSum = 0;
			for (int valFreq : ownFreqTable.get(i)) issueSum += valFreq;
			diffScore += 1 - (double) ownFreqTable.get(i).get(valIndex) / issueSum;
		}
		diffScore /= issues.size();
		return diffScore;
	}
	
	private void createFile()
	{
		 Boolean fileCreated = false;
		 while(!fileCreated)
		 {
			 try 
			 {
			 File newFile = new File("OppEstimates_" + fileNumber + ".txt");
			 if (newFile.createNewFile()) {
				 fileCreated = true;
			 }else {
				 fileNumber += 1;
			 }
			 } catch(IOException e)
			 {
				 System.out.println("Error occured");
			 }
		 }
	}
	private void writeOppBidAndUtility(Bid bid, double oppUtility) throws IOException 
	{
		
		
		FileWriter fw = new FileWriter("OppEstimates_" + fileNumber + ".txt", true);
	    BufferedWriter bw = new BufferedWriter(fw);
	    PrintWriter out = new PrintWriter(bw);

		out.println(";" );
		out.println("XXX");
		out.println(Double.toString(oppUtility));

		for (Issue issue : bid.getIssues()) 
		{
			IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
			
			int valCounter = 0;
			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) 
			{
				valCounter += 1;

				if (bid.containsValue(issueDiscrete, valueDiscrete)){
					int val = valCounter;

					out.println(Integer.toString(val));

				}
			}
		
		}
		out.close();

	}
	
	
	private List<Double> getPreferenceOrder(HashMap<List<Double>, Double> bidRanks)
	{
		int expertCounter =0;

		List<Double> utils = new ArrayList<>();
		int totalRanks = 0;
		//List<Double> values = new ArrayList<>();
		for (List<Double> expert : bidRanks.keySet())
		{
			totalRanks += bidRanks.get(expert);
			int bidIndex = 0;
			if (expertCounter == 0){
				for (Double value : expert)
				{
					utils.add(value * expert.get(bidIndex));
					bidIndex+= 1;

				}
			}else {
				for (Double value : expert)
				{
					utils.set(bidIndex, utils.get(bidIndex) + value * expert.get(bidIndex));
					bidIndex +=1 ;
				}
			}
			expertCounter += 1;
		}


		for (Double util : utils)
		{
			util /= totalRanks;
		}

		return utils;
	}

	@Override
	public String getDescription() 
	{
		return "Places random bids >= " + MINIMUM_TARGET;
	}

	/**
	 * This stub can be expanded to deal with preference uncertainty in a more sophisticated way than the default behavior.
	 */
	@Override
	public AbstractUtilitySpace estimateUtilitySpace() 
	{
		return super.estimateUtilitySpace();
	}

	private Bid getMaxUtilityBid() {
		try {
			return utilitySpace.getMaxUtilityBid();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private double getMaxUtility() {
		try {
			Bid maxBid = getMaxUtilityBid();
			return utilitySpace.getUtility(maxBid);
		} catch (Exception e) {
			return 1;
		}
	}


	public AbstractUtilitySpace estimateUtilitySpace(Domain domain) {
	    return new LPUtilitySpace(domain, userModel.getBidRanking());
	}

	public Bid estimateNashPoint(List<Bid> offers){
		Bid nbs = offers.get(0);
		Double maxNBS = 0d;

		for(Bid b : offers){
			Double util = utilitySpace.getUtility(b);
			Double oppUtil = getOppUtility(b);

			Double currNBS = util * oppUtil;
			
			if(currNBS > maxNBS){
				maxNBS = currNBS;
				nbs = b;
			}
		}
		return nbs;
	}

	private double getOppUtility(Bid bid) {

		List<Double> evalUtils = OppModel.hypEvaluations(probs, bid, hyps);
		List<Double> evalWeights = OppModel.weightEvaluations(weightHyps, weightProbs);

		double opp1Util = OppModel.estimateUtility(bid, evalWeights, evalUtils);

		List<Double> freqWeights = new ArrayList<>();
		List<List<Double>> freqValues = new ArrayList<>();

		freqWeights = JBFreq.getWeights(freqTable);
		freqValues = JBFreq.getVals(freqTable);

		double opp2Util = JBFreq.getUtility(bid, freqValues, freqWeights);

		List<List<Double>> ktValues = KTFreq.getVals(freqTable);
		List<Double> ktWeights = KTFreq.getWeights(freqTable, ktValues);

		double opp3Util = KTFreq.getUtility(lastOffer, ktValues, ktWeights);

		List<Double> utils = new ArrayList<>();
		utils.add(opp1Util);
		utils.add(opp2Util);

		utils.add(opp3Util);
		return (opp1Util + opp2Util + opp3Util)/3.0;
		//return opp1Util;
	}
}