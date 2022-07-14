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
import org.grits.toolbox.entry.ms.preference.xml.MassSpecCustomAnnotation;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecCustomAnnotationPeak;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecCustomAnnotationTemplate;

/**
 * Utility class for reading/writing CustomAnnotation information from file.
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 *
 */
public class MassSpecCustomAnnotationFile {
	private static final Logger logger = Logger.getLogger(MassSpecCustomAnnotationFile.class);
	public static final String[] HEADER_COLUMN_LABELS = new String[] {"Peak m/z", "MS Level", "Peak Label"};
	public static final String DELIMITER = "\t";
	public static final String FILE_EXTENSION_TXT = ".txt";
	public static final String FILE_EXTENSION_TXT_NAME = "Tab-delimited (.txt)";
	public static final String FILE_EXTENSION_XML = ".xml";
	public static final String FILE_EXTENSION_XML_NAME = "GRITS Custom Annotation XML (.xml)";

	public static String[] getHeaderColumns() {
		return HEADER_COLUMN_LABELS;
	}

	/**
	 * Tests the top line read from a file to see if it matches the known column headers.
	 * 
	 * @param sCurLine, top line read from a peak file
	 * @return true if the line contains the header text
	 */
	public static boolean hasHeaders( String sCurLine ) {
		String[] toks = sCurLine.split(MassSpecCustomAnnotationFile.DELIMITER);
		boolean bHasMz = false;
		if( toks.length > 0 && toks[0].equalsIgnoreCase( MassSpecCustomAnnotationFile.HEADER_COLUMN_LABELS[0]) ) {
			bHasMz = true;	
		}
		return bHasMz;
	}

	/**
	 * Tests a user-supplied file to see if it is formatted properly for reading in the peaks w/ their labels.
	 * 
	 * @param f, a tab-delimited file w/ a list of peaks to be added to a CustomAnnotation entry
	 * @return
	 */
	public static boolean testTxtFile( File f ) {
		BufferedReader reader;
		boolean bPass = false;
		try {
			reader = new BufferedReader(new FileReader(f));
			String header = reader.readLine();
			String[] toks = header.split(MassSpecCustomAnnotationFile.DELIMITER);
			if( toks[0].equalsIgnoreCase( MassSpecCustomAnnotationFile.HEADER_COLUMN_LABELS[0] ) ) {
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
	 * Reads a list of peaks to annotate from the specified file and populates the CustomAnnotation object.
	 * 
	 * @param msca, the MassSpecCustomAnnotation to populate with the peaks from the specified file.
	 * @param sFilePath, the path to the file containing the custom annotation peaks
	 * @return true if successful, false otherwise
	 */
	public static boolean readTxtFile(MassSpecCustomAnnotation msca, String sFilePath) {
		BufferedReader reader;
		boolean bPass = false;
		try {
			File f = new File( sFilePath );			
			reader = new BufferedReader(new FileReader(f));
			String sCurLine = reader.readLine();
			boolean hasHeader = MassSpecCustomAnnotationFile.hasHeaders(sCurLine);
			if( hasHeader ) {
				sCurLine = reader.readLine();
			}
			if( msca.getAnnotatedPeaks() == null ) {
				msca.setAnnotatedPeaks(new HashMap<Double, MassSpecCustomAnnotationPeak>());
			}

			while( sCurLine != null ) {
				try {
					if( ! sCurLine.trim().equals("") ) { 
						String[] toks = sCurLine.split(MassSpecCustomAnnotationFile.DELIMITER);
						MassSpecCustomAnnotationPeak peak = new MassSpecCustomAnnotationPeak();
						Double dMz = Double.parseDouble(toks[0].trim());
						peak.setPeakMz(dMz);
						Integer iMSLevel = Integer.parseInt(toks[1].trim());
						peak.setMSLevel(iMSLevel);
						peak.setPeakLabel("");
						if( toks.length >= 3 && toks[2] != null) {
							String sLabel = toks[2].trim();
							peak.setPeakLabel(sLabel);
						}
						msca.getAnnotatedPeaks().put(peak.getPeakMz(), peak);


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
	 * Writes the list of peaks from the CustomAnnotation object to the specified file.
	 * 
	 * @param msca, the MassSpecCustomAnnotation containing the list of peaks.
	 * @param sFilePath, the path to the file to write the list of peaks.
	 * @return true if successful, false otherwise
	 */
	public static boolean writeTxtFile(MassSpecCustomAnnotation msca, String sFilePath) {
		BufferedWriter writer;
		boolean bPass = false;
		try {
			File f = new File( sFilePath );			
			writer = new BufferedWriter(new FileWriter(f));
			String sw = "";
			for( int i = 0; i < MassSpecCustomAnnotationFile.HEADER_COLUMN_LABELS.length; i++ ) {
				writer.write(sw);
				writer.write(MassSpecCustomAnnotationFile.HEADER_COLUMN_LABELS[i]);
				if( sw.equals("") ) {
					sw = MassSpecCustomAnnotationFile.DELIMITER;
				}
			}
			writer.write(System.getProperty("line.separator"));
			Object[] peaks = msca.getAnnotatedPeaks().values().toArray();
			for( int i = 0; i < peaks.length; i++ ) {
				MassSpecCustomAnnotationPeak peak = (MassSpecCustomAnnotationPeak) peaks[i];
				writer.write(Double.toString(peak.getPeakMz()));
				writer.write(MassSpecCustomAnnotationFile.DELIMITER);
				writer.write(Integer.toString(peak.getMSLevel()));
				writer.write(MassSpecCustomAnnotationFile.DELIMITER);
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

	/**
	 * Reads an XML file containing MassSpecCustomAnnotationTemplate data, creates a new MassSpecCustomAnnotation with it.
	 * @param sFilePath, path to the user-specified XML file.
	 * @return, a new MassSpecCustomAnnotation object
	 * @see MassSpecCustomAnnotationTemplate
	 */
	public static MassSpecCustomAnnotation readXMLFile(String sFilePath) {
		BufferedReader reader;
		try {
			File f = new File( sFilePath );			
			reader = new BufferedReader(new FileReader(f));
			MassSpecCustomAnnotationTemplate template = MassSpecCustomAnnotationTemplate.unmarshalAnnotationTemplate(sFilePath);
			MassSpecCustomAnnotation msca = template.copyToNewAnnotation();
			reader.close();
			return msca;
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
	}

	/**
	 * Creates a new MassSpecCustomAnnotationTemplate and populates it with data from 
	 * the specified MassSpecCustomAnnotation and writes the MassSpecCustomAnnotationTemplate object to 
	 * the specified file.
	 * 
	 * @param msca, a MassSpecCustomAnnotation object
	 * @param sFilePath, destination file to write XML
	 * @return true if successful, false otherwise
	 */
	public static boolean writeXMLFile(MassSpecCustomAnnotation msca, String sFilePath) {
		BufferedWriter writer;
		boolean bPass = false;
		try {
			File f = new File( sFilePath );			
			writer = new BufferedWriter(new FileWriter(f));
			MassSpecCustomAnnotationTemplate template = msca.copyToNewTemplate();
			String sXML = MassSpecCustomAnnotationTemplate.marshalAnnotatedTemplate(template);
			writer.write(sXML);
			writer.write(System.getProperty("line.separator"));
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
