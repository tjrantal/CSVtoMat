import java.io.*;
import java.util.*;
public class CSVReader{
	public Vector<String> columnHeadings;
	public Vector<Vector<Double>> columns;
	public CSVReader(String fileName,String separator){
		try {
			BufferedReader br = new BufferedReader( new FileReader(fileName));
			String strLine = "";
			StringTokenizer st = null;
			int tokenNumber;
			/*Read column headings from first row*/
			strLine = br.readLine(); /*Read the headings row*/
			st = new StringTokenizer(strLine, separator);
			columnHeadings = new Vector<String>();
			columns = new Vector<Vector<Double>>();
			while(st.hasMoreTokens()){
				columnHeadings.add(st.nextToken().trim());
				columns.add(new Vector<Double>());
			}
			/*Read data row by row*/
			while( (strLine = br.readLine()) != null){
				st = new StringTokenizer(strLine, separator);
				tokenNumber = 0;
				while(st.hasMoreTokens()){
					columns.get(tokenNumber).add(Double.valueOf(st.nextToken()));
					++tokenNumber;
				}
			}
			br.close();
		} catch (Exception err){System.err.println("Error: "+err.getMessage());}
	}
}