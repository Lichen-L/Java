package facilityGenerator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author parady_UT
 *facility data (Japan) parser for facility xml generation
*  author: parady_UT; 
*  Parser needs to be edited to account for different facility type definitions (around line 51)
 */

public class FacilityParser {
	
	private String separator = "\t";
	private Charset charset = Charset.forName("UTF-8");

	public List<JFacility> readFile(String inFile)
	{
		List<JFacility> entries = new ArrayList<JFacility>();
		
		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;
	    
    	try 
    	{
    		fis = new FileInputStream(inFile);
    		isr = new InputStreamReader(fis, charset);
			br = new BufferedReader(isr);
			
			// skip first Line
			br.readLine();
			 
			String line;
			while((line = br.readLine()) != null)
			{
				JFacility jFacility = new JFacility();
			
				String[] cols = line.split(separator);
								
				jFacility.id = parseInteger(cols[0]);
				jFacility.x = parseDouble(cols[1]);
				jFacility.y = parseDouble(cols[2]);
				jFacility.type = cols[3];
	
				entries.add(jFacility);
			}
			
			br.close();
			isr.close();
			fis.close();
    	}
    	catch (FileNotFoundException e) 
    	{
			e.printStackTrace();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		return entries;
	}
	
	private int parseInteger(String string)
	{
		if (string == null) return 0;
		else if (string.trim().isEmpty()) return 0;
		else return Integer.valueOf(string);
	}
	
	private double parseDouble(String string)
	{
		if (string == null) return 0.0;
		else if (string.trim().isEmpty()) return 0.0;
		else return Double.valueOf(string);
	}
}
