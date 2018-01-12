package postProcessor;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.io.IOUtils;


public class PostProcessEvents {

		
	public static void main(String[] args) {

		// create and parse a MATSim network from file
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile("C:/Users/MATSim/eclipse-workspace/UT_MATSim/resources/output/output_network.xml.gz");

		
		// Creating an Events Manager
		EventsManager events = EventsUtils.createEventsManager();
		
		// creates a new instance of our event handler
		CarDistanceEvaluator carDistanceEvaluator = new CarDistanceEvaluator(network);
	
		ActivityChainEventHandler activityChainEventHandler = new ActivityChainEventHandler();
		
		TaxiTravelTimeEvaluator taxiTravelTimeEvaluator = new TaxiTravelTimeEvaluator();

		
		events.addHandler(activityChainEventHandler);
		
		events.addHandler(carDistanceEvaluator);
		
		events.addHandler(taxiTravelTimeEvaluator);
		
		//starts to stream through the events file, please set the path accordingly
		new MatsimEventsReader(events).readFile("C:/Users/MATSim/eclipse-workspace/UT_MATSim/resources/output/output_events.xml.gz");
		
		activityChainEventHandler.writeActivitestoFile("C:/Users/MATSim/eclipse-workspace/UT_MATSim/resources/output/activitychains.txt");
		
		taxiTravelTimeEvaluator.writeHashMapToFile("C:/Users/MATSim/eclipse-workspace/UT_MATSim/resources/output/taxiTravelWaitTime.txt");
		
		int[] result = carDistanceEvaluator.getDistanceBins();
		writeArrayToFile(result, "C:/Users/MATSim/Desktop/FUKU-TEST/resources/output/travelbins.txt");
	}
	
	static void writeArrayToFile(int[] data, String filename){
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			bw.write("bin\tdata");
			for (int i = 0; i<data.length;i++){
				bw.newLine();
				bw.write(i+"\t"+data[i]);
			}
			
			bw.flush();
			bw.close();
		} catch (IOException e) {
					e.printStackTrace();
		}
	}
}	
	
