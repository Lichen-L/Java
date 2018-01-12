package postProcessor;

import java.io.BufferedWriter;
import java.io.IOException;
//import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;


public class TaxiTravelTimeEvaluator implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, PersonArrivalEventHandler{
	

	final private static Map<Id<Person>,Double> travelTime = new LinkedHashMap<>();
	final private static Map<Id<Person>,Double> departureTime = new LinkedHashMap<>();
	final private static Map<Id<Person>,Double> enterTaxiTime = new LinkedHashMap<>();
	final private static Map<Id<Person>,Double> waitTime = new LinkedHashMap<>();
	final private static Map<Id<Person>,Integer> alreadyLoaded = new LinkedHashMap<>();
	
	
	public TaxiTravelTimeEvaluator() {

	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals("taxi")){
			// record id of persons that plans to take taxi 
			departureTime.put(event.getPersonId(), event.getTime());
		}
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id<Person> enterTaxiPersonId = event.getPersonId();
		// get specific PersonEntersVehicleEvent for those who took taxi
		if (departureTime.containsKey(enterTaxiPersonId)){
			
			// waited time for this person is calculated by the time lag between departureEvent and personEntersVehicleEvent
			double TimeWaited = event.getTime() - departureTime.get(enterTaxiPersonId);
			Integer timesOfThisId = -99; // just for initializing, no actual meaning 
	
			//check if this Id have ever appeared in output data, if so, timesOfThisId +=1, and update alreadyLoaded map.
			if(alreadyLoaded.containsKey(enterTaxiPersonId)) {
				timesOfThisId = alreadyLoaded.get(enterTaxiPersonId);
				timesOfThisId += 1; 
				alreadyLoaded.put(enterTaxiPersonId, timesOfThisId);
			}
			else {
				timesOfThisId = 1;
				alreadyLoaded.put(enterTaxiPersonId, timesOfThisId);
			}
			
			// combine the id with times it appeared
			Id<Person> combined_Id = Id.createPersonId(enterTaxiPersonId.toString()+"_"+String.valueOf(alreadyLoaded.get(enterTaxiPersonId)));
			
			waitTime.put(combined_Id, TimeWaited);
			enterTaxiTime.put(combined_Id, event.getTime());

			}
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Id<Person> arrivalPersonId = event.getPersonId();
		if(event.getLegMode().equals("taxi")) {
			if(departureTime.containsKey(arrivalPersonId)) {
				Id<Person> depId = Id.createPersonId(arrivalPersonId.toString()+"_"+String.valueOf(alreadyLoaded.get(arrivalPersonId)));		
				if (enterTaxiTime.containsKey(depId)){
					double TimeTraveled = event.getTime() - enterTaxiTime.get(depId);
					travelTime.put(depId, TimeTraveled);
				}
			}		
		}
	}
	
	public void writeHashMapToFile(String filename){
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			bw.write("ID\t\tWaitTime\tTravelTime");
			for(Id<Person> ID: waitTime.keySet()) {
				bw.newLine();
				bw.write(ID+"\t"+"\t"+waitTime.get(ID)+"\t"+travelTime.get(ID));
				
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
					e.printStackTrace();
		}
	}
}
