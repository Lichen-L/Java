package facilityGenerator;


import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author parady_UT
 * facility xml generator using Tel-Point Data as input
*  author: parady_UT
 */

public class FacilityGenerator{
	

	public static void main(String[] args) {
		
		//define file name and input path
		String fname = "test facilities";
		String facilityData = "io/input/facTest.txt";
		
		//load facilities input data
		List<JFacility> jFacilities = new FacilityParser().readFile(facilityData);
		
		try {	
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			
			// set root element
			Element rootElement = doc.createElement("facilities");
			rootElement.setAttribute("name", fname);
			doc.appendChild(rootElement);
			
			// facility elements
			for(int i=0; i<jFacilities.size(); i++) {
				
				Element facility = doc.createElement("facility");
				rootElement.appendChild(facility);

				//set id, and x y coordinates
				int id = jFacilities.get(i).id;
				double x  = jFacilities.get(i).x;
				double y  = jFacilities.get(i).y;
				String type = jFacilities.get(i).type;
								
				facility.setAttribute("id", String.valueOf(id));
				facility.setAttribute("x", String.valueOf(x));
				facility.setAttribute("y", String.valueOf(y));
				
				// set activity type
				Element activity = doc.createElement("activity");
				activity.setAttribute("type", type);
				facility.appendChild(activity);
			}
		
			// write the content into xml file			
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File("io/output/test.xml"));
			transformer.transform(source, result);
			
			System.out.println("facility xml succesfully generated");

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}

}



