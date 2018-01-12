package postProcessor;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;

public class CarDistanceEvaluator implements PersonDepartureEventHandler, PersonArrivalEventHandler, LinkEnterEventHandler {

		// the network contains all links, we require their length
		final private Network network;
		private int[] distanceBins = new int[31];
		final private Map<Id<Person>,Double> currentTraveledDistance = new HashMap<>();
		
		public CarDistanceEvaluator(Network network) {

			this.network = network;
			
		}
		
		public int[] getDistanceBins() {
			return distanceBins;
		}
		
		@Override
		public void handleEvent(PersonDepartureEvent event) {
			if (event.getPersonId().toString().startsWith("pt")){
				return;
			}
			
			if (event.getLegMode().equals("car")){
				currentTraveledDistance.put(event.getPersonId(), 0.0);
			}
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Id<Person> personId = Id.createPersonId(event.getVehicleId());
			//This will work as long as people are driving in cars named exactly like them.
			if (currentTraveledDistance.containsKey(personId)){
				double distanceTraveledSoFar  = currentTraveledDistance.get(personId); // get the value=distance for personId
				double linkLength = network.getLinks().get(event.getLinkId()).getLength();
				currentTraveledDistance.put(personId, linkLength+distanceTraveledSoFar);
			}
			
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			if (currentTraveledDistance.containsKey(event.getPersonId())){
				//currentTraveledDistance is a Map<Id<Person>, Double> type, so you have to remove the Id first in order to calculate bins. 
				double traveledDistance = currentTraveledDistance.remove(event.getPersonId()) / 1000.0;
				int bin = (int) traveledDistance;
				if (bin>30) {
					bin = 30;
				}
				distanceBins[bin]++;
			}
		}

		

		@Override
		public void reset(int iteration) {
			// this method is called before each iteration starts. No need to fill anything if you use your EventHandler only in Postprocessing
		}
		
}
