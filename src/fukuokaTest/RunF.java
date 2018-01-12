package fukuokaTest;
/**
 * @author LLC
 * 
 */
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.av.intermodal.router.VariableAccessTransitRouterModule;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessConfigGroup;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessModeConfigGroup;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareHandler;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiOutputModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TripsToLegsModule;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouterModule;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class RunF {
	
	private static final String ROOTDIR = "C:/Users/MATSim/Desktop/FUKU-TEST/resources/";
	private static final String OUTPUTDIR = ROOTDIR+"/output";
	private static final String NETWORKFILE = ROOTDIR+"fukuokaEPSG2444.xml";;
	private static final String PLANSFILE = ROOTDIR+"population+1.xml";
	private static final String CONFIGFILE = ROOTDIR+"config.xml";
	
	private static final String TRANSITSCHEDULEFILE = ROOTDIR+"transitSchedule.xml";
	private static final String TRANSITVEHICLEFILE = ROOTDIR+"transitVehicles.xml";
	
    private static final String SUBPOP_ATTRIB_NAME = "subpopulation";
    private static final String SUBPOP1_NAME = "CARGROUP"; 
    private static final String SUBPOP2_NAME = "PAVGROUP"; 
    
    private static final int ITERATIONTIMES = 1; 
    
    private static final boolean ACTIVATE_OTFVIS_ORNOT = true; 
	
	
	public static void main(String[] args) {
		
		RunF.createControler(CONFIGFILE, ACTIVATE_OTFVIS_ORNOT).run();
	}
	
	public static Controler createControler(String CONFIGFILE, boolean otfvis) {	
		
		Config config = ConfigUtils.loadConfig(CONFIGFILE, new DvrpConfigGroup(), new TaxiConfigGroup(),
				new OTFVisConfigGroup(), new TaxiFareConfigGroup());
		
		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();	
		
	
		config.network().setInputFile(NETWORKFILE);
		config.plans().setInputFile(PLANSFILE);
		config.controler().setOutputDirectory(OUTPUTDIR);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(ITERATIONTIMES-1);
		config.controler().setWriteEventsInterval(1);
		config.controler().setMobsim("qsim");
		config.qsim().setStartTime(0);
		config.qsim().setEndTime(86400);
	

// ----------------for public transit-------------------------------------
			
		VariableAccessConfigGroup vacfg = new VariableAccessConfigGroup();
		{
			VariableAccessModeConfigGroup taxi = new VariableAccessModeConfigGroup();
			taxi.setDistance(20000);
			taxi.setTeleported(false);
			taxi.setMode("taxi");
			vacfg.setAccessModeGroup(taxi);
		}
		{
			VariableAccessModeConfigGroup walk = new VariableAccessModeConfigGroup();
			walk.setDistance(1000);
			walk.setTeleported(true);
			walk.setMode("walk");
			vacfg.setAccessModeGroup(walk);
		}
		config.addModule(vacfg);
		
		config.transit().setUseTransit(true);
		config.transit().setTransitScheduleFile(TRANSITSCHEDULEFILE);
		config.transit().setVehiclesFile(TRANSITVEHICLEFILE);
		config.transit().setTransitModes("pt");
		
		config.transitRouter().setSearchRadius(15000);
		config.transitRouter().setExtensionRadius(0);
		
// ----------------for mixed traffic(in order to simulate two different modes, and notice that taxi is an exception)--------------			
		Collection<String> mainModes = new ArrayList<>();
		mainModes.add("car");
		mainModes.add("pav");//do not and no need adding taxi, pt, walk, access_walk and egress_walk, since they have been injected from related module. LLC
		
		config.qsim().setMainModes(mainModes);
		config.qsim().setLinkDynamics(LinkDynamics.PassingQ);
		config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);
				
		config.plansCalcRoute().setNetworkModes(mainModes);
		
		
		config.travelTimeCalculator().setAnalyzedModes("car,pav"); // still, not add taxi and pt. LLC(not sure)
		//config.travelTimeCalculator().setAnalyzedModes("car");// ==mainModes
		config.travelTimeCalculator().setSeparateModes(true);
		

// ---------------------score settings---------------------------------------		
		
		config.planCalcScore().getOrCreateModeParams("pav");// this will set default scoring params for pav
		String[] modesChanged = {"car","walk","pav","pt"};// modes that could be chosen
		//String[] modesChanged = {"car","taxi","walk","pt"};
		config.changeMode().setModes(modesChanged);
		
		
// ---------------------simulation loading---------------------------------------			
		
		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();	
		
		// notice that after load the scenario, do not modify config anymore! LLC
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		addExtraModesToNetwork(scenario, "pav","pt","taxi");

		// create new transport mode and/or set performance parameters for it
		VehicleType car =scenario.getVehicles().getFactory().createVehicleType(Id.create("car",VehicleType.class));
		car.setMaximumVelocity(80/3.6);
		car.setPcuEquivalents(1.0);
		scenario.getVehicles().addVehicleType(car);
		
		//VehicleType taxi =scenario.getVehicles().getFactory().createVehicleType(Id.create("taxi",VehicleType.class));
		VehicleType taxi = new VehicleTypeImpl(Id.create("taxi", VehicleType.class));
		taxi.setMaximumVelocity(80/3.6);
		taxi.setPcuEquivalents(1.0);
		scenario.getVehicles().addVehicleType(taxi);
		
		
		VehicleType pt = new VehicleTypeImpl(Id.create("pt", VehicleType.class));
		taxi.setMaximumVelocity(80/3.6);
		taxi.setPcuEquivalents(1.0);
		scenario.getVehicles().addVehicleType(pt);
		
		
		VehicleType pav = new VehicleTypeImpl(Id.create("pav", VehicleType.class));
		pav.setMaximumVelocity(100/3.6);
		pav.setPcuEquivalents(0.8);
		pav.setFlowEfficiencyFactor(1.5);
		scenario.getVehicles().addVehicleType(pav);
		
		
		
		 // allocate a certain percentage of agents with PAV mode 
		Random random = new Random();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (random.nextDouble() > 0.2 ) continue;
			
			Plan plan = person.getSelectedPlan();
			List<PlanElement> pes = plan.getPlanElements();

			for (PlanElement pe : pes) {
				if (pe instanceof Leg) {
					((Leg) pe).setRoute(null);
					((Leg) pe).setMode("pav");
				}
			}		
		}
		/*
		Random random2 = new Random();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (random2.nextDouble() > 0.1 ) continue;
			
			Plan plan = person.getSelectedPlan();
			List<PlanElement> pes = plan.getPlanElements();

			for (PlanElement pe : pes) {
				if (pe instanceof Leg) {
					((Leg) pe).setRoute(null);
					((Leg) pe).setMode("pt");
				}
			}		
		}
		*/
		
		
		// car&pav availability settings
		for (Person person : scenario.getPopulation().getPersons().values()) {
			ObjectAttributes personAttributes = scenario.getPopulation().getPersonAttributes();
			personAttributes.putAttribute(person.getId().toString(), "carAvail", "never");
			person.getCustomAttributes().put("carAvail", "never");
			person.getAttributes().putAttribute("carAvail","never") ;
		}
		
		int totalPersonSize = scenario.getPopulation().getPersons().size();
	        for (Id<Person> p : scenario.getPopulation().getPersons().keySet()) {
	            int personIdInteger = Integer.valueOf(p.toString());
	            if ( personIdInteger < totalPersonSize/2  ) {
	                scenario.getPopulation().getPersonAttributes().putAttribute(p.toString(), SUBPOP_ATTRIB_NAME, SUBPOP1_NAME);
	            } else {
	                scenario.getPopulation().getPersonAttributes().putAttribute(p.toString(), SUBPOP_ATTRIB_NAME, SUBPOP2_NAME);
	            }
	        }
	        
	        scenario.getConfig().plans().setSubpopulationAttributeName(SUBPOP_ATTRIB_NAME);
	        // clear previous strategies
	        scenario.getConfig().strategy().clearStrategySettings();
	        
	        // add innovative modules for SUBPOP1
	        {
	            StrategyConfigGroup.StrategySettings modeChoiceStrategySettings = new StrategyConfigGroup.StrategySettings() ;
	            modeChoiceStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.toString());
	            modeChoiceStrategySettings.setSubpopulation(SUBPOP1_NAME);
	            modeChoiceStrategySettings.setWeight(0.3);
	            scenario.getConfig().strategy().addStrategySettings(modeChoiceStrategySettings);


	            StrategyConfigGroup.StrategySettings changeExpBetaStrategySettings = new StrategyConfigGroup.StrategySettings();
	            changeExpBetaStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
	            changeExpBetaStrategySettings.setSubpopulation(SUBPOP1_NAME);
	            changeExpBetaStrategySettings.setWeight(0.7);
	            scenario.getConfig().strategy().addStrategySettings(changeExpBetaStrategySettings);
	        }
	        
	        // add innovative modules for SUBPOP2
	        {
	            StrategyConfigGroup.StrategySettings modeChoiceStrategySettings = new StrategyConfigGroup.StrategySettings() ;
	            modeChoiceStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.toString().concat(SUBPOP2_NAME)); // a different name is must. Amit June'17
	            modeChoiceStrategySettings.setSubpopulation(SUBPOP2_NAME);
	            modeChoiceStrategySettings.setWeight(0.3);
	            scenario.getConfig().strategy().addStrategySettings(modeChoiceStrategySettings);

	            StrategyConfigGroup.StrategySettings changeExpBetaStrategySettings = new StrategyConfigGroup.StrategySettings();
	            changeExpBetaStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
	            changeExpBetaStrategySettings.setSubpopulation(SUBPOP2_NAME);
	            changeExpBetaStrategySettings.setWeight(0.7);
	            scenario.getConfig().strategy().addStrategySettings(changeExpBetaStrategySettings);
	        }
	       
	        
		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new TaxiOutputModule());
		
		controler.addOverridingModule(new TaxiModule());
		
		controler.addOverridingModule(new VariableAccessTransitRouterModule());
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().to(TaxiFareHandler.class).asEagerSingleton();
			}
		});
	 
		 controler.addOverridingModule(new AbstractModule() {
	            @Override
	            public void install() {
	                final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
	                addPlanStrategyBinding(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.name().concat(SUBPOP2_NAME)).toProvider(new javax.inject.Provider<PlanStrategy>() {
	                    final String[] availableModes = {"walk", "taxi", "pav","pt"};
	                    @Inject
	                    Scenario sc;

	                    @Override
	                    public PlanStrategy get() {
	                        final PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());
	                        builder.addStrategyModule(new TripsToLegsModule(tripRouterProvider, sc.getConfig().global()));
	                        builder.addStrategyModule(new ChangeLegMode(sc.getConfig().global().getNumberOfThreads(), availableModes, true));
	                        builder.addStrategyModule(new ReRoute(sc, tripRouterProvider));
	                        return builder.build();
	                    }
	                });
	            }
	        });
	 

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		controler.run();
		
		return controler;
	}

	private static void addExtraModesToNetwork(Scenario scenario, String... args) {
		for (Link link : scenario.getNetwork().getLinks().values()) {
			Set<String> allowedModes = new HashSet<>();
			for (String s:args)
			allowedModes.add(s);
			allowedModes.add("pav");
			allowedModes.add("pt");
			allowedModes.addAll(link.getAllowedModes());
			link.setAllowedModes(allowedModes);
		}
		
		
	}


}
