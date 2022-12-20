import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import genius.cli.Runner;
import genius.core.exceptions.InstantiateException;
public class paramGrapher {

	
	public static void main(String[] args) throws JAXBException, IOException, InstantiateException {
		
		List<Double> sigma = new ArrayList<>();
		int counter = 0;
		for (int x = 0; x < 10; x++) {
			String logfile = "log/log" + counter;
			runTourn(logfile, 1);
			counter += 1;
			logfile = "log/log" + counter;
			counter += 1;

			
			runTourn(logfile, 2);
				
		}

	}
	
	public static void runTourn(String logfile, int number) throws JAXBException, IOException, InstantiateException
	{
		String[] args = {"multilateraltournament" + number + ".xml", logfile};
		genius.cli.Runner.main(args);
	}
	
	
}	
