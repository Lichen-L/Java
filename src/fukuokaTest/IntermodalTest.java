package fukuokaTest;

import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.av.intermodal.router.VariableAccessTransitRouterModule;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessConfigGroup;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessModeConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiOutputModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class IntermodalTest {
	public static void main(String[] args) {
		new IntermodalTest().run(false);
	}

	public void run(boolean OTFVis) {
		Config config = ConfigUtils.loadConfig("C:/Users/MATSim/Desktop/FUKU-TEST/resources/configInter.xml", new TaxiConfigGroup(), new DvrpConfigGroup());

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		// yyyy Could you please javadoc the following?  EmissionsConfigGroup has an example how the explanatory strings
		// can be kept consistent between config file dump and javadoc.  Thx.  kai, jan'17
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

		config.transitRouter().setSearchRadius(15000);
		config.transitRouter().setExtensionRadius(0);

		OTFVisConfigGroup otfvis = new OTFVisConfigGroup();
		otfvis.setDrawNonMovingItems(true);
		config.addModule(otfvis);

		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();
		// ---
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new TaxiOutputModule());

        controler.addOverridingModule(new TaxiModule());
		
		controler.addOverridingModule(new VariableAccessTransitRouterModule());
		if (OTFVis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		
		controler.run();
	}
}
