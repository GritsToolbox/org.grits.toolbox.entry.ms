package org.grits.toolbox.entry.ms.preference.xml;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.Logger;
import org.grits.toolbox.core.utilShare.XMLUtils;

/**
 * Class for storing the information for quantification of internal standards of MS data. As an XML annotated class,
 * can be written/read from file.
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 *
 */
@XmlRootElement(name = "massSpecStandardQuantification")
public class MassSpecStandardQuant {
	private static final Logger logger = Logger.getLogger(MassSpecStandardQuant.class);
	public static final String VALUE_SEPERATOR = "!!!!!!";

	protected String sStandardQuantName = null;
	protected String sDescription = null;
	protected Integer iMSLevel = null;
	protected Double dMassTolerance = null;
	protected Boolean bIsPPM = null;
	protected HashMap<Double, MassSpecStandardQuantPeak> listPeaks = null;
	protected String sStandardQuantPeakText = null;
	
	public MassSpecStandardQuant() {
		// TODO Auto-generated constructor stub
	}
	
	public MassSpecStandardQuant( String sStandardQuantName, String sDescription, Integer iMSLevel, 
			Double dMassTolerance, Boolean bIsPPM ) {
		this.sStandardQuantName = sStandardQuantName;
		this.sDescription = sDescription;
		this.iMSLevel = iMSLevel;
		this.dMassTolerance = dMassTolerance;
		this.bIsPPM = bIsPPM;
		this.listPeaks = new HashMap<Double, MassSpecStandardQuantPeak>();
	}
	
	@Override
	public boolean equals(Object obj) {
		if( ! (obj instanceof MassSpecStandardQuant) ) {
			return false;
		}
		return ((MassSpecStandardQuant) obj).getStandardQuantName().equals(getStandardQuantName());
	}
	
	protected MassSpecStandardQuant getNewStandardQuantObject() {
		return new MassSpecStandardQuant();
	}
	
	@Override
	public Object clone() {
		MassSpecStandardQuant msca = getNewStandardQuantObject();
		msca.setStandardQuantName(getStandardQuantName());
		msca.setDescription(getDescription());
		msca.setMSLevel(getMSLevel());
		msca.setMassTolerance(getMassTolerance());
		msca.setIsPPM(getIsPPM());
		msca.setStandardQuantPeaks(new HashMap<>());
		Iterator<Double> itr = this.getStandardQuantPeaks().keySet().iterator();
		while( itr.hasNext() ) {
			Double dMz = itr.next();
			MassSpecStandardQuantPeak peak = this.getStandardQuantPeaks().get(dMz);
			msca.getStandardQuantPeaks().put(dMz, peak);
		}
		return msca;
	}
	
	public boolean differsFrom(MassSpecStandardQuant that) {
		if( ! this.getStandardQuantName().equals(that.getStandardQuantName() ) ) {
			return true;
		}
		if( (this.getIsPPM() && ! that.getIsPPM()) || (! this.getIsPPM() && that.getIsPPM()) ) {
			return true;
		}
		if( ! this.getDescription().equals(that.getDescription()) ) {
			return true;
		}
		if( Double.compare(this.getMassTolerance(), that.getMassTolerance()) != 0 ) {
			return true;
		}
		if( this.getMSLevel() != that.getMSLevel() ) {
			return true;
		}
		if( this.getStandardQuantPeaks().size() != that.getStandardQuantPeaks().size() ) {
			return true;
		}
		Collection thesePeaks = this.getStandardQuantPeaks().values();
		Collection thosePeaks = that.getStandardQuantPeaks().values();
		Iterator<MassSpecStandardQuantPeak> thisItr = thesePeaks.iterator();
		while( thisItr.hasNext() ) {
			MassSpecStandardQuantPeak thisPeak = thisItr.next();
			Iterator<MassSpecStandardQuantPeak> thatItr = thosePeaks.iterator();
			boolean bFound = false;
			while( thatItr.hasNext() && ! bFound ) {
				MassSpecStandardQuantPeak thatPeak = thatItr.next();
				if( thisPeak.equals(thatPeak) ) {
					bFound = true;
				}
			}
			if( ! bFound ) {
				return true;
			}
		}
		return false;
		
	}
	
	@Override
	public String toString() {
		return getStandardQuantName() + ", num peaks: " + getStandardQuantPeaks().size();
	}
	
	public String getStandardQuantName() {
		return sStandardQuantName;
	}
	@XmlAttribute(name = "standardQuantName")
	public void setStandardQuantName(String sStandardQuantName) {
		this.sStandardQuantName = sStandardQuantName;
	}
	
	public String getDescription() {
		return sDescription;
	}
	@XmlAttribute(name = "description")
	public void setDescription(String sDescription) {
		this.sDescription = sDescription;
	}
	
	public Integer getMSLevel() {
		return iMSLevel;
	}
	@XmlAttribute(name = "msLevel")
	public void setMSLevel(Integer iMSLevel) {
		this.iMSLevel = iMSLevel;
	}
	
	public Double getMassTolerance() {
		return dMassTolerance;
	}
	@XmlAttribute(name = "massTolerance")
	public void setMassTolerance(Double dMassTolerance) {
		this.dMassTolerance = dMassTolerance;
	}
	
	public Boolean getIsPPM() {
		return bIsPPM;
	}
	@XmlAttribute(name = "isPPM")
	public void setIsPPM(Boolean bIsPPM) {
		this.bIsPPM = bIsPPM;
	}
	
	public HashMap<Double, MassSpecStandardQuantPeak> getStandardQuantPeaks() {
		return listPeaks;
	}
	@XmlTransient
	public void setStandardQuantPeaks(HashMap<Double, MassSpecStandardQuantPeak> listPeaks) {
		this.listPeaks = listPeaks;
	}
	
	public String getStandardQuantPeakText() {
		return sStandardQuantPeakText;
	}
	@XmlAttribute(name = "standardQuantPeakText")
	public void setStandardQuantPeakText(String sStandardQuantPeakText) {
		this.sStandardQuantPeakText = sStandardQuantPeakText;
	}
	
	public void unmarshalStandardQuantPeakList() {
		HashMap<Double, MassSpecStandardQuantPeak> sStandardQuantPeaks = MassSpecStandardQuant.unmarshalAnnotatedPeakText(this);
		setStandardQuantPeaks(sStandardQuantPeaks);
	}
	
	public void updateStandardQuantPeakText() {
		String standardQuantPeakText = MassSpecStandardQuant.createStandardQuantPeakText(getStandardQuantPeaks());
		setStandardQuantPeakText(standardQuantPeakText);
	}
		
	public static String createStandardQuantPeakText( MassSpecStandardQuantPeak peak) {
		String asText = XMLUtils.marshalObjectXML(peak);
		return asText;
	}

	public static String createStandardQuantPeakText(HashMap<Double, MassSpecStandardQuantPeak> standardQuantPeaks) {
		StringBuilder sb = new StringBuilder();
		if( standardQuantPeaks != null && ! standardQuantPeaks.isEmpty() ) {
			Iterator<Double> itr = standardQuantPeaks.keySet().iterator();
			while( itr.hasNext() ) {
				Double dKey = itr.next();
				MassSpecStandardQuantPeak value = standardQuantPeaks.get(dKey);
				if( value == null ) {
					continue;
				}
				if( ! sb.toString().equals("") ) {
					sb.append(VALUE_SEPERATOR);
				}
				String sXML = MassSpecStandardQuant.createStandardQuantPeakText(value);
				sb.append(sXML);
			}
		}
		return sb.toString();
	}

	public static HashMap<Double, MassSpecStandardQuantPeak> unmarshalAnnotatedPeakText( MassSpecStandardQuant standardQuant ) {
		HashMap<Double, MassSpecStandardQuantPeak> standardQuantPeaks = new HashMap<Double, MassSpecStandardQuantPeak>();
		try {
			if( standardQuant != null && standardQuant.getStandardQuantPeakText() != null && ! standardQuant.getStandardQuantPeakText().equals("") ) {
				String[] standardQuants = standardQuant.getStandardQuantPeakText().trim().split(VALUE_SEPERATOR);
				if( standardQuants != null && standardQuants.length > 0 ) {
					for( int i = 0; i < standardQuants.length; i++ ) {
						String annotXML = standardQuants[i];
						MassSpecStandardQuantPeak peak = standardQuant.getPeakObjectFromXML(annotXML);
						if( peak != null ) {
							standardQuantPeaks.put(peak.getPeakMz(), peak);
						}
					}
				}
			}
		} catch (Exception ex) {
			logger.error("Error loading default options from xml files.", ex);
		}
		return standardQuantPeaks;
	}
	
	public MassSpecStandardQuantPeak getPeakObjectFromXML( String peakXML ) {
		MassSpecStandardQuantPeak peak = (MassSpecStandardQuantPeak) XMLUtils.getObjectFromXML(peakXML, MassSpecStandardQuantPeak.class);		
		return peak;
	}		

}
