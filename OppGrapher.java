import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.NegotiationInfo;
import java.io.File;  // Import the File class
import java.io.FileNotFoundException;  // Import this class to handle errors
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner; // Import the Scanner class to read text files
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;  
import javax.xml.parsers.DocumentBuilder;  
import org.w3c.dom.Document;  
import org.w3c.dom.NodeList;

import com.opencsv.CSVWriter;

import org.w3c.dom.Node;  
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.io.File;  

public class OppGrapher {

	public static void main(String[] args) {
	    getEstimates(args[0]);
	 }
	
	public static void getEstimates(String fileNumber) 
	{	
		List<List<Integer>> opp1bids = new ArrayList<>();
		List<List<Integer>> opp2bids = new ArrayList<>();

		List<Integer> bid = new ArrayList<>();
		List<Double> opp1values = new ArrayList<>();
		List<Double> opp2values = new ArrayList<>();
		
		String agID = null;
		Boolean agTracker = false;
		try {
			int filenum = Integer.parseInt(fileNumber);
			
		      File myObj = new File("OppEstimates_" + filenum + ".txt");
		      Scanner myReader = new Scanner(myObj);
		      
		      while (myReader.hasNextLine()) {
		    	  String data = myReader.nextLine();
		    	  
		    	  if (data.charAt(0) == ';')
		    	  {	
		    		  String agName = myReader.nextLine();
		    		  if (agName.contains("XXX")) {
		    			  agTracker = false;
		    		  }else {
		    			  agTracker = true;
		    		  }
		    		  
		    		  String data2 = myReader.nextLine();
			    	  if (agTracker) {
		    			  opp1values.add(Double.parseDouble(data2));
		    		  }else {
		    			  opp2values.add(Double.parseDouble(data2));
		    		  }
		    		  if (bid.size() != 0) 
		    		  {
				    	  if (agTracker) {
				    		  opp1bids.add(bid);
			    		  }else {
				    		  opp2bids.add(bid);
			    		  }
			    		  bid = new ArrayList<>();

		    		  }
		    		  
		    	  }		    	  
		    	  else {
		    		  bid.add(Integer.parseInt(data));
		    	  }

		      }
		     
		      opp2bids.add(bid);
		      myReader.close();
		    } catch (FileNotFoundException e) {
		      System.out.println("An error occurred.");
		      e.printStackTrace();
		    }
		
		
	


		
		
		List<Double> opp1Utils = getOpp1UtilSpace(opp1bids);
		List<Double> opp2Utils = getOpp2UtilitySpace(opp2bids);
		
		
	
		//System.out.println(opp2Utils);

		
		writetoCSV(opp1values, opp1Utils, opp2values, opp2Utils);
	}

		
	
	public static void writetoCSV(List<Double> opp1values,List<Double> opp1Utils,List<Double> opp2values,List<Double> opp2Utils)
	{
	    // first create file object for file placed at location
	    // specified by filepath
	    File file = new File("OPPUTILS.csv");
	    try {
	        // create FileWriter object with file as parameter
	        FileWriter outputfile = new FileWriter(file);
	  
	        // create CSVWriter object filewriter object as parameter
	        CSVWriter writer = new CSVWriter(outputfile);
	  
	        // adding header to csv
	        String[] header = { "Name", "Class", "Marks" };
	        writer.writeNext(header);
	  
	        // add data to csv
	        String[] data1 = new String[opp1values.size()];
	        
	        for(int i=0; i< opp1values.size(); i++) {
	            data1[i] = opp1values.get(i).toString();
	         }
	        

	        writer.writeNext(data1);
	        
	        
	        String[] data2 = new String[opp1Utils.size()];
	        
	        for(int i=0; i< opp1Utils.size(); i++) {
	            data2[i] = opp1Utils.get(i).toString();
	         }	        
	        writer.writeNext(data2);
	        
	        String[] data3 = new String[opp2values.size()];
	        
	        for(int i=0; i< opp2values.size(); i++) {
	            data3[i] = opp2values.get(i).toString();
	         }	        
	        writer.writeNext(data3);
	        
	        String[] data4 = new String[opp2Utils.size()];
	        
	        for(int i=0; i< opp2Utils.size(); i++) {
	            data4[i] = opp2Utils.get(i).toString();
	         }	        
	        writer.writeNext(data4);
	        // closing writer connection
	        writer.close();
	        
	    }
	    catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    }
	}
	public static List<Double> getOpp1UtilSpace(List<List<Integer>> oppBids) 
	{
		List<List<Double>> oppValues = new ArrayList<>();
		List<Double> oppWeights = new ArrayList<>();
		
		oppWeights.add(0.19);
		oppWeights.add(0.28);
		oppWeights.add(0.19);
		oppWeights.add(0.05);
		oppWeights.add(0.19);
		oppWeights.add(0.10);

		
		try   
		{  
		//creating a constructor of file class and parsing an XML file  
		File file = new File("party1_utility.xml");  
		//an instance of factory that gives a document builder  
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();  
		//an instance of builder to parse the specified xml file  
		DocumentBuilder db = dbf.newDocumentBuilder();  
		Document doc = db.parse(file);  
		doc.getDocumentElement().normalize();  
		
		NodeList nodeList = doc.getElementsByTagName("issue");  
		
		for (int itr = 0; itr < nodeList.getLength(); itr++)   
		{  
		Node node = nodeList.item(itr);  
		
		if (node.getNodeType() == Node.ELEMENT_NODE)   
		{  
			NodeList nodeMap = node.getChildNodes();  
			List<Double> issueVals = new ArrayList<>();

			
				for (int i = 0; i < nodeMap.getLength(); i++)   
				{  
					
					if (nodeMap.item(i).getNodeType() == Node.ELEMENT_NODE)   
					{  										
						Node newNode = nodeMap.item(i);  
						
						NamedNodeMap nm = newNode.getAttributes();
						
						
						Node nnode = nm.item(1);  
			
						issueVals.add(Double.parseDouble(nnode.getNodeValue()));
						
				
						}
				
					}  
				oppValues.add(issueVals);
			 
				}
		 
		
		
			}
		}
		catch(Exception e)   
		{  
		e.printStackTrace();  
		}  
		
		
		List<Double> utils = new ArrayList<>();
		for (int i = 0; i < oppBids.size(); i++)
		{
			double actualUtil = 0;

			for (int j = 0; j < oppBids.get(i).size(); j++)
			{
				actualUtil += oppWeights.get(j) * (oppValues.get(j).get(oppBids.get(i).get(j)-1)/findSum(oppValues.get(j)));
			}
			utils.add(actualUtil);
		}
		return utils;
	}
	
	public static List<Double> getOpp1Utils(List<Bid> oppBids) 
	{
		List<List<Double>> oppValues = new ArrayList<>();
		List<Double> oppWeights = new ArrayList<>();
		
		oppWeights.add(0.19);
		oppWeights.add(0.28);
		oppWeights.add(0.19);
		oppWeights.add(0.05);
		oppWeights.add(0.19);
		oppWeights.add(0.10);

		
		try   
		{  
		//creating a constructor of file class and parsing an XML file  
		File file = new File("party1_utility.xml");  
		//an instance of factory that gives a document builder  
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();  
		//an instance of builder to parse the specified xml file  
		DocumentBuilder db = dbf.newDocumentBuilder();  
		Document doc = db.parse(file);  
		doc.getDocumentElement().normalize();  
		
		NodeList nodeList = doc.getElementsByTagName("issue");  
		
		for (int itr = 0; itr < nodeList.getLength(); itr++)   
		{  
		Node node = nodeList.item(itr);  
		
		if (node.getNodeType() == Node.ELEMENT_NODE)   
		{  
			NodeList nodeMap = node.getChildNodes();  
			List<Double> issueVals = new ArrayList<>();

			
				for (int i = 0; i < nodeMap.getLength(); i++)   
				{  
					
					if (nodeMap.item(i).getNodeType() == Node.ELEMENT_NODE)   
					{  										
						Node newNode = nodeMap.item(i);  
						
						NamedNodeMap nm = newNode.getAttributes();
						
						
						Node nnode = nm.item(1);  
			
						issueVals.add(Double.parseDouble(nnode.getNodeValue()));
						
				
						}
				
					}  
				oppValues.add(issueVals);
			 
				}
		 
		
		
			}
		}
		catch(Exception e)   
		{  
		e.printStackTrace();  
		}  
		
		
		List<Double> utils = new ArrayList<>();
		for (int i = 0; i < oppBids.size(); i++)
		{
			double actualUtil = 0;
			int j = 0;
			for (Issue issue : oppBids.get(i).getIssues()) 
			{
				IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
				
				int valCounter = 0;
				for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) 
				{
					if (oppBids.get(i).containsValue(issueDiscrete, valueDiscrete))
					{

					actualUtil += oppWeights.get(j) * (oppValues.get(j).get(valCounter)/findSum(oppValues.get(j)));
					}
					valCounter += 1;
				}
				j+= 1;

			}
			utils.add(actualUtil);
		}
		return utils;
	}
	
	public static Double findSum(List<Double> array) {
		Double max = 0.0;
	    for (int i = 0; i < array.size(); i++)
	    {
	    	if (max < array.get(i))
	    	{
	    		max = array.get(i);
	    	}
	    }
		
		return max;
	}
	
	public static List<Double> getOpp2UtilitySpace(List<List<Integer>> oppBids)
	{
		List<List<Double>> opp2Values = new ArrayList<>();
		List<Double> opp2Weights = new ArrayList<>();
		opp2Weights.add(0.0951912078);
		opp2Weights.add(0.228313908);
		opp2Weights.add(0.10913144);
		opp2Weights.add(0.214198);
		opp2Weights.add(0.108076725);
		opp2Weights.add(0.245087997);
		try   
		{  
		//creating a constructor of file class and parsing an XML file  
		File file = new File("party2_utility.xml");  
		//an instance of factory that gives a document builder  
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();  
		//an instance of builder to parse the specified xml file  
		DocumentBuilder db = dbf.newDocumentBuilder();  
		Document doc = db.parse(file);  
		doc.getDocumentElement().normalize();  
		
		NodeList nodeList = doc.getElementsByTagName("issue");  
		
		for (int itr = 0; itr < nodeList.getLength(); itr++)   
		{  
		Node node = nodeList.item(itr);  
		
		if (node.getNodeType() == Node.ELEMENT_NODE)   
		{  
			NodeList nodeMap = node.getChildNodes();  
			List<Double> issueVals = new ArrayList<>();

			
				for (int i = 0; i < nodeMap.getLength(); i++)   
				{  
					
					if (nodeMap.item(i).getNodeType() == Node.ELEMENT_NODE)   
					{  										
						Node newNode = nodeMap.item(i);  
						
						NamedNodeMap nm = newNode.getAttributes();
						
						
						Node nnode = nm.item(1);  
			
						issueVals.add(Double.parseDouble(nnode.getNodeValue()));
						
				
						}
				
					}  
				opp2Values.add(issueVals);
			 
				}
		 
		
		
			}
		}
		catch(Exception e)   
		{  
		e.printStackTrace();  
		}
		
		
		
		
		List<Double> utils = new ArrayList<>();
		for (int i = 0; i < oppBids.size(); i++)
		{
			double actualUtil = 0;

			for (int j = 0; j < oppBids.get(i).size(); j++)
			{
			
				actualUtil += opp2Weights.get(j) * (opp2Values.get(j).get(oppBids.get(i).get(j)-1)/findSum(opp2Values.get(j)));
			}
			utils.add(actualUtil);
		}
		return utils;
	}
	
}



