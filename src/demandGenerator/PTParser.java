package demandGenerator;



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
 * person trip data (Japan) parser for basic demand generation
*  author: parady_UT; adapted from Zurich travel census case
 */

public class PTParser {
	private String separator = "\t";
	private Charset charset = Charset.forName("UTF-8");

	public List<PTEntry> readFile(String inFile)
	{
		List<PTEntry> entries = new ArrayList<PTEntry>();
		
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
				PTEntry censusEntry = new PTEntry();
			
				String[] cols = line.split(separator);
								
				censusEntry.id_person = parseInteger(cols[0]);
				censusEntry.tripnum = parseInteger(cols[1]);
				censusEntry.starttime = parseInteger(cols[2]);
				censusEntry.h_x = parseDouble(cols[3]);
				censusEntry.h_y = parseDouble(cols[4]);
				censusEntry.s_x = parseDouble(cols[5]);
				censusEntry.s_y = parseDouble(cols[6]);
				censusEntry.d_x = parseDouble(cols[7]);
				censusEntry.d_y = parseDouble(cols[8]);
				censusEntry.bike = parseInteger(cols[9]);
				censusEntry.age = parseInteger(cols[10]);
				censusEntry.gender = parseInteger(cols[11]);
				censusEntry.license = parseInteger(cols[12]);
				censusEntry.caravailability = parseInteger(cols[13]);
				censusEntry.day = parseInteger(cols[14]);
				censusEntry.tripmode = parseInteger(cols[15]);
				censusEntry.trippurpose = parseInteger(cols[16]);
				censusEntry.tripdistance = parseDouble(cols[17]);
				censusEntry.tripduration = parseInteger(cols[18]);
				censusEntry.id_tour = parseInteger(cols[19]);
								
				entries.add(censusEntry);
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

