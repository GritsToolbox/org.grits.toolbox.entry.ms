package org.grits.toolbox.entry.ms.property.datamodel;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "peakMZsToCorrectedIntensities")
public class QuantFilePeaksToCorrectedIntensities {
	public static final String CURRENT_VERSION = "1.0";
	private HashMap<Double, Double> peakMzToIntensity = null;
	
	public QuantFilePeaksToCorrectedIntensities() {
		peakMzToIntensity = new HashMap<Double, Double>();
	}
	
	public void setPeakMzToIntensity(HashMap<Double, Double> peakMzToIntensity) {
		this.peakMzToIntensity = peakMzToIntensity;
	}
	@XmlElement(name="peakMzToCorrectedIntensity")
	public HashMap<Double, Double> getPeakMzToIntensity() {
		return peakMzToIntensity;
	}
	
}
