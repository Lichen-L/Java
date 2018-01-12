package demandGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class PTPopulationGenerator {
	
	/*
	 * Creates as subclass of the abstract class Id<Person> to generate IDs
	 */
	public class genID extends Id<Person> {
		
	}

	public static void main(String args[]) {

		/*
		 * Create population from sample input data.
		 */
		Scenario scenario = createPopulationFromCensusFile("C:/Users/MATSim/Desktop/ptmatsim.txt");

		/*
		 * Write population to file.
		 */
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write("C:/Users/MATSim/Desktop/population.xml");

	}

	
	private static Scenario createPopulationFromCensusFile(String censusFile)	{

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		/*
		 * Use Parser to read the census sample file.
		 */
		List<PTEntry> censusEntries = new PTParser().readFile(censusFile);

		/*
		 * Get Population and PopulationFactory objects.
		 * The Population contains all created Agents and their plans,
		 * the PopulationFactory should be used to create Plans,
		 * Activities, Legs and so on.
		 */
		Population population = scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();


		/*
		 * The census file contains one line per trip, meaning a typical person
		 * is represented by multiple lines / trips. Therefore in a first step
		 * we have to sort the trips based on the person who executes them.
		 */

		/*
		 * Create a Map with the PersonIds as key and a list of CensusEntry as values.
		 */
		Map<Integer, List<PTEntry>> personEntryMapping = new TreeMap<Integer, List<PTEntry>>();
		for (PTEntry censusEntry : censusEntries) {
			/*
			 * If the Map already contains an entry for the current person
			 * the list will not be null.
			 */
			List<PTEntry> entries = personEntryMapping.get(censusEntry.id_person);

			/*
			 * If no mapping exists -> create a new one
			 */
			if (entries == null) {
				entries = new ArrayList<PTEntry>();
				personEntryMapping.put(censusEntry.id_person, entries);
			}

			/*
			 *  Add currently processed entry to the list
			 */
			entries.add(censusEntry);
		}

		/*
		 * Now create a plan for each person - iterate over all entries in the map.
		 */
		for (List<PTEntry> personEntries : personEntryMapping.values()) {
			/*
			 * Get the first entry from the list - it will never be null.
			 */
			PTEntry entry = personEntries.get(0);

			/*
			 * Get id of the person from the censusEntry.
			 */
			int id_person = entry.id_person;
			
			/*
			 * Create new person and add it to the population.
			 * Use scenario.createId(String id) to create the Person's Id.
			 */
			Person person = populationFactory.createPerson(genID.createPersonId(id_person));
			
			population.addPerson(person);
			
			/*
			 * Add carAvailability
			 */
			


			/*
			 *  Create new plan and add it to the person.
			 */
			Plan plan = populationFactory.createPlan();
			person.addPlan(plan);

			/*
			 * Every Agent has at least one activity which is being at home.
			 * - set the activity type to "home"
			 * - set the start time to 0.0
			 * - add the Activity to the plan.
			 */
			Coord homeCoord =  new Coord(entry.h_x, entry.h_y);
			Activity homeActivity = populationFactory.createActivityFromCoord("home", homeCoord);
			homeActivity.setStartTime(0.0);
			plan.addActivity(homeActivity);

			/*
			 * Create objects that are needed when creating the other
			 * Activities and Legs of the Plan.
			 *
			 * Mind that we have to set a start and end time for each Activity
			 * (except the last one - it will last until the end of the simulated
			 * period). The end time of an Activity equals the departure time of
			 * the next Trip. We set the end time of an Activity when we process
			 * the next Trip by using a point to the last previously created
			 * Activity (initially this is the Home Activity).
			 */
			
			

			Coord endCoord = null;
			String transportMode = null;
			Leg leg = null;
			Activity activity = null;
			Activity previousActivity = homeActivity;

			/*
			 *  Create person's Trips and add them to the Plan.
			 */
			for (PTEntry personEntry : personEntries) {
				endCoord = new Coord(personEntry.d_x, personEntry.d_y);
				transportMode = getTransportMode(personEntry.tripmode);
				String activityType = getActivityType(personEntry.trippurpose);

				/*
				 * Create a new Leg using the PopulationFactory and set its parameters.
				 * Mind that MATSim uses seconds as time unit whereas the census uses minutes.
				 */
				leg = populationFactory.createLeg(transportMode);
				leg.setDepartureTime(personEntry.starttime * 60);
				leg.setTravelTime(personEntry.tripduration * 60);
				previousActivity.setEndTime(personEntry.starttime * 60);

				/*
				 * Create a new Activity using the Population Factory and set its parameters.
				 */
				activity = populationFactory.createActivityFromCoord(activityType, endCoord);
				activity.setStartTime(personEntry.starttime * 60 + personEntry.tripduration * 60);

				/*
				 * Add the Leg and the Activity to the plan.
				 */
				plan.addLeg(leg);
				plan.addActivity(activity);
				
				/*
				 * Do not forget to update the pointer to the previousActivity.
				 */
				previousActivity = activity;
			}

			/*
			 * ... and finally: If the last Activity takes place at the Home Coordinates
			 * we assume that the Agent is performing a "home" Activity.
			 */
			if (activity.getCoord().equals(homeCoord)) {
				activity.setType("home");
			}
		}
		return scenario;

	}

	/*
	 * Helper methods that convert the entries from the census file.
	 */
    
	//Defaults is based on representative mode classification#2 �ｼ亥�鬘橸ｼ抵ｼ� in PTH22
	private static String getTransportMode(int mode) {
		switch (mode) {
		case 1: return TransportMode.pt;
		case 2: return TransportMode.pt;
		case 3: return TransportMode.car;
		case 4: return TransportMode.car;
		case 5: return TransportMode.other; //motor-bike, engine assisted bike
		case 6: return TransportMode.bike;
		case 7: return TransportMode.walk;
		case 8: return TransportMode.other;
		default: return "undefined";
		}
	}

	//Defaults is based  on purpose (as-is on survey sheet) in PTH22
	private static String getActivityType(int activityType) {
		switch (activityType) {
		case 1: return "work";
		case 2: return "education";
		case 3: return "home";
		case 4: return "shop";
		case 5: return "leisure";
		case 6: return "leisure";
		case 7: return "hospital";
		case 8: return "other";
		case 9: return "other";
		case 10: return "work-related";
		case 11: return "work-related";
		case 12: return "work-related";
		case 13: return "work-related";
		case 14: return "work-related";
		default: return "undefined";
		}
	}

}
