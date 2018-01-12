package fukuokaTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 * This is an example script to create (robo)taxi vehicle files. The vehicles are distributed randomly in the network.
 *
 */
public class CreateTaxiVehicles {
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		int numberofVehicles = 400;
		double operationStartTime = 0.; //t0
		double operationEndTime = 24*3600.;	//t1
		int seats = 4;
		String networkfile = "C:/Users/MATSim/eclipse-workspace/UT_MATSim/resources/MaebashiNetwork.xml";
		String taxisFile = "C:/Users/MATSim/eclipse-workspace/UT_MATSim/resources/taxis_"+numberofVehicles+".xml";
		List<Vehicle> vehicles = new ArrayList<>();
		Random random = MatsimRandom.getLocalInstance();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkfile);
		List<Id<Link>> allLinks = new ArrayList<>();
		allLinks.addAll(scenario.getNetwork().getLinks().keySet());
		for (int i = 0; i< numberofVehicles;i++){
			Link startLink;
			do {
			Id<Link> linkId = allLinks.get(random.nextInt(allLinks.size()));
			startLink =  scenario.getNetwork().getLinks().get(linkId);
			}
			while (!startLink.getAllowedModes().contains(TransportMode.car));
			//for multi-modal networks: Only links where cars can ride should be used.
			
			Vehicle v = new VehicleImpl(Id.create("taxi"+i, Vehicle.class), startLink, seats, operationStartTime, operationEndTime);
		    vehicles.add(v);    
			
		}
		new VehicleWriter(vehicles).write(taxisFile);
	}

}
