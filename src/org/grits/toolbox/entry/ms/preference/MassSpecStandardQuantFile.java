package org.grits.toolbox.entry.ms.preference;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecStandardQuant;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecStandardQuantPeak;

/**
 * Utility class for reading/writing StandardQuant information from file.
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 *
 */
public class MassSpecStandardQuantFile {
	private static final Logger logger = Logger.getLogger(MassSpecStandardQuantFile.class);
	public static final String[] HEADER_COLUMN_LABELS = new String[] {"Peak m/z", "MS Level", "Peak Label"};
	public static final String DELIMITER = "\t";
	public static final String FILE_EXTENSION_TXT = ".txt";
	public static final String FILE_EXTENSION_TXT_NAME = "Tab-delimited (.txt)";
	public static final String FILE_EXTENSION_XML = ".xml";
	public static final String FILE_EXTENSION_XML_NAME = "GRITS Standard Quantitation XML (.xml)";

	public static String[] getHeaderColumns() {
		return HEADER_COLUMN_LABELS;
	}

	/**
	 * Tests a user-supplied file to see if it is formatted properly for reading in the peaks w/ their labels.
	 * 
	 * @param f, a tab-delimited file w/ a list of peaks to be added to a StandardQuant entry
	 * @return
	 */
	public static boolean testTxtFile( File f ) {
		BufferedReader reader;
		boolean bPass = false;
		try {
			reader = new BufferedReader(new FileReader(f));
			String header = reader.readLine();
			String[] toks = header.split(MassSpecStandardQuantFile.DELIMITER);
			if( toks[0].equalsIgnoreCase( MassSpecStandardQuantFile.HEADER_COLUMN_LABELS[0] ) ) {
				bPass = true;
			}
			reader.close();
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return bPass;
	}

	/**
	 * Reads a list of peaks to use for internal quantification from the specified file and populates the StandardQuant object.
	 * 
	 * @param msca, the MassSpecStandardQuant to populate with the peaks from the specified file.
	 * @param sFilePath, the path to the file containing the standard quantification peaks
	 * @return true if successful, false otherwise
	 */
	public static boolean readTxtFile(MassSpecStandardQuant mssq, String sFilePath) {
		BufferedReader reader;
		boolean bPass = false;
		try {
			File f = new File( sFilePath );			
			reader = new BufferedReader(new FileReader(f));
			String header = reader.readLine();
			String[] toks = header.split(MassSpecStandardQuantFile.DELIMITER);
			boolean bHasLabels = false;
			if( toks.length > 2 && toks[2].equalsIgnoreCase( MassSpecStandardQuantFile.HEADER_COLUMN_LABELS[2]) ) {
				bHasLabels = true;				
			}
			String sCurLine = reader.readLine();
			if( mssq.getStandardQuantPeaks() == null ) {
				mssq.setStandardQuantPeaks(new HashMap<Double, MassSpecStandardQuantPeak>());
			}

			while( sCurLine != null ) {
				try {
					if( ! sCurLine.trim().equals("") ) { 
						toks = sCurLine.split(MassSpecStandardQuantFile.DELIMITER);
						MassSpecStandardQuantPeak peak = new MassSpecStandardQuantPeak();
						Double dMz = Double.parseDouble(toks[0].trim());
						peak.setPeakMz(dMz);
						Integer iMSLevel = Integer.parseInt(toks[1].trim());
						peak.setMSLevel(iMSLevel);
						peak.setPeakLabel("");
						if( bHasLabels && toks.length == 3 && toks[2] != null) {
							String sLabel = toks[2].replaceAll("\\P{L}+", "");
							peak.setPeakLabel(sLabel);
						}
						mssq.getStandardQuantPeaks().put(peak.getPeakMz(), peak);


					}
				} catch( Exception e ) {
					logger.error(e.getMessage(), e);
				}
				sCurLine = reader.readLine();
			}
			reader.close();
			bPass = true;
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return bPass;		

	}

	/**
	 * Writes the list of peaks from the StandardQuant object to the specified file.
	 * 
	 * @param msca, the MassSpecStandardQuant containing the list of peaks.
	 * @param sFilePath, the path to the file to write the list of peaks.
	 * @return true if successful, false otherwise
	 */
	public static boolean writeTxtFile(MassSpecStandardQuant mssq, String sFilePath) {
		BufferedWriter writer;
		boolean bPass = false;
		try {
			File f = new File( sFilePath );			
			writer = new BufferedWriter(new FileWriter(f));
			String sw = "";
			for( int i = 0; i < MassSpecStandardQuantFile.HEADER_COLUMN_LABELS.length; i++ ) {
				writer.write(sw);
				writer.write(MassSpecStandardQuantFile.HEADER_COLUMN_LABELS[i]);
				if( sw.equals("") ) {
					sw = MassSpecStandardQuantFile.DELIMITER;
				}
			}
			writer.write(System.getProperty("line.separator"));
			Object[] peaks = mssq.getStandardQuantPeaks().values().toArray();
			for( int i = 0; i < peaks.length; i++ ) {
				MassSpecStandardQuantPeak peak = (MassSpecStandardQuantPeak) peaks[i];
				writer.write(Double.toString(peak.getPeakMz()));
				writer.write(MassSpecStandardQuantFile.DELIMITER);
				writer.write(Integer.toString(peak.getMSLevel()));
				writer.write(MassSpecStandardQuantFile.DELIMITER);
				writer.write(peak.getPeakLabel() == null ? "" : peak.getPeakLabel());
				writer.write(System.getProperty("line.separator"));
			}			
			writer.close();
			bPass = true;
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return bPass;		

	}

}
