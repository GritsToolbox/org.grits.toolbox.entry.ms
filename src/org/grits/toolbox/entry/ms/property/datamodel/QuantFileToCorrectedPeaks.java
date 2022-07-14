package org.grits.toolbox.entry.ms.property.datamodel;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "peakToCorrectedIntensities")
public class QuantFileToCorrectedPeaks {
	public static final String CURRENT_VERSION = "1.0";
	private HashMap<String, QuantFilePeaksToCorrectedIntensities> peakTypeToMZs = null;
	
	public QuantFileToCorrectedPeaks() {
		peakTypeToMZs = new HashMap<String, QuantFilePeaksToCorrectedIntensities>();
	}
	
	public void setPeakTypeToMZs(HashMap<String, QuantFilePeaksToCorrectedIntensities> peakTypeToMZs) {
		this.peakTypeToMZs = peakTypeToMZs;
	}
	@XmlElement(name="peakTypeToMZs")
	public HashMap<String, QuantFilePeaksToCorrectedIntensities> getPeakTypeToMZs() {
		return peakTypeToMZs;
	}
	
}
