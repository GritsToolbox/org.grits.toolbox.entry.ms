package org.grits.toolbox.entry.ms.preference.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class for description an individual MS Peak that should be considered an internal standard.
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 *
 */
@XmlRootElement(name = "massSpecStandardSetPeak")
public class MassSpecStandardQuantPeak {
	private Double dPeakMz = null;
	private Integer iMSLevel = null;
	private String sPeakLabel = null;
	
	public MassSpecStandardQuantPeak() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public boolean equals(Object obj) {
		if( ! (obj instanceof MassSpecStandardQuantPeak) ) {
			return false;
		}
		MassSpecStandardQuantPeak that = (MassSpecStandardQuantPeak) obj;
		return Double.compare(this.getPeakMz(), that.getPeakMz()) == 0 && 
				this.getMSLevel() == that.getMSLevel() && 
				this.getPeakLabel().equals(that.getPeakLabel());
	}
	
	public Double getPeakMz() {
		return dPeakMz;
	}
	@XmlElement(name="peakMz")
	public void setPeakMz(Double dPeakMz) {
		this.dPeakMz = dPeakMz;
	}
	
	public Integer getMSLevel() {
		return iMSLevel;
	}
	@XmlElement(name="msLevel")
	public void setMSLevel(Integer iMSLevel) {
		this.iMSLevel = iMSLevel;
	}
	
	public String getPeakLabel() {
		return sPeakLabel;
	}
	@XmlElement(name="peakLabel")
	public void setPeakLabel(String sPeakLabel) {
		this.sPeakLabel = sPeakLabel;
	}
		
}
