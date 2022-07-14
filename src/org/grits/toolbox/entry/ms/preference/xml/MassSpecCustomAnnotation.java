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
 * Class for storing the information for CustomAnnotation of MS data. As an XML annotated class,
 * can be written/read from file.
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 *
 */
@XmlRootElement(name = "massSpecCustomAnnotation")
public class MassSpecCustomAnnotation {
	private static final Logger logger = Logger.getLogger(MassSpecCustomAnnotation.class);
	public static final String VALUE_SEPERATOR = "!!!!!!";

	protected String sAnnotationName = null;
	protected String sDescription = null;
	protected Integer iMSLevel = null;
	protected Double dMassTolerance = null;
	protected Boolean bIsPPM = null;
	protected HashMap<Double, MassSpecCustomAnnotationPeak> listPeaks = null;
	protected String sAnnotatedPeakText = null;
	
	public MassSpecCustomAnnotation() {
		// TODO Auto-generated constructor stub
	}
	
	public MassSpecCustomAnnotation( String sAnnotationName, String sDescription, Integer iMSLevel, 
			Double dMassTolerance, Boolean bIsPPM ) {
		this.sAnnotationName = sAnnotationName;
		this.sDescription = sDescription;
		this.iMSLevel = iMSLevel;
		this.dMassTolerance = dMassTolerance;
		this.bIsPPM = bIsPPM;
		this.listPeaks = new HashMap<Double, MassSpecCustomAnnotationPeak>();
	}
	
	@Override
	public boolean equals(Object obj) {
		if( ! (obj instanceof MassSpecCustomAnnotation) ) {
			return false;
		}
		return ((MassSpecCustomAnnotation) obj).getAnnotationName().equals(getAnnotationName());
	}
	
	protected MassSpecCustomAnnotation getNewCustomAnnotationObject() {
		return new MassSpecCustomAnnotation();
	}
	
	@Override
	public Object clone() {
		MassSpecCustomAnnotation msca = getNewCustomAnnotationObject();
		msca.setAnnotationName(getAnnotationName());
		msca.setDescription(getDescription());
		msca.setMSLevel(getMSLevel());
		msca.setMassTolerance(getMassTolerance());
		msca.setIsPPM(getIsPPM());
		msca.setAnnotatedPeaks(new HashMap<>());
		Iterator<Double> itr = this.getAnnotatedPeaks().keySet().iterator();
		while( itr.hasNext() ) {
			Double dMz = itr.next();
			MassSpecCustomAnnotationPeak peak = this.getAnnotatedPeaks().get(dMz);
			msca.getAnnotatedPeaks().put(dMz, peak);
		}
		return msca;
	}
	
	public boolean differsFrom(MassSpecCustomAnnotation that) {
		if( ! this.getAnnotationName().equals(that.getAnnotationName() ) ) {
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
		if( this.getAnnotatedPeaks().size() != that.getAnnotatedPeaks().size() ) {
			return true;
		}
		Collection thesePeaks = this.getAnnotatedPeaks().values();
		Collection thosePeaks = that.getAnnotatedPeaks().values();
		Iterator<MassSpecCustomAnnotationPeak> thisItr = thesePeaks.iterator();
		while( thisItr.hasNext() ) {
			MassSpecCustomAnnotationPeak thisPeak = thisItr.next();
			Iterator<MassSpecCustomAnnotationPeak> thatItr = thosePeaks.iterator();
			boolean bFound = false;
			while( thatItr.hasNext() && ! bFound ) {
				MassSpecCustomAnnotationPeak thatPeak = thatItr.next();
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
		return getAnnotationName() + ", num peaks: " + getAnnotatedPeaks().size();
	}
	
	public String getAnnotationName() {
		return sAnnotationName;
	}
	@XmlAttribute(name = "annotationName")
	public void setAnnotationName(String sAnnotationName) {
		this.sAnnotationName = sAnnotationName;
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
	
	public HashMap<Double, MassSpecCustomAnnotationPeak> getAnnotatedPeaks() {
		return listPeaks;
	}
	@XmlTransient
	public void setAnnotatedPeaks(HashMap<Double, MassSpecCustomAnnotationPeak> listPeaks) {
		this.listPeaks = listPeaks;
	}
	
	public String getsAnnotatedPeakText() {
		return sAnnotatedPeakText;
	}
	@XmlAttribute(name = "annotatedPeakText")
	public void setsAnnotatedPeakText(String sAnnotatedPeakText) {
		this.sAnnotatedPeakText = sAnnotatedPeakText;
	}
	
	public void unmarshalAnnotatedPeakList() {
		HashMap<Double, MassSpecCustomAnnotationPeak> annotatedPeaks = MassSpecCustomAnnotation.unmarshalAnnotatedPeakText(this);
		setAnnotatedPeaks(annotatedPeaks);
	}
	
	public void updateAnnotatedPeakText() {
		String annotatedPeakText = MassSpecCustomAnnotation.createAnnotatedPeaksText(getAnnotatedPeaks());
		setsAnnotatedPeakText(annotatedPeakText);
	}
	
	public static String createAnnotatedPeaksText( MassSpecCustomAnnotationPeak peak) {
		String asText = XMLUtils.marshalObjectXML(peak);
		return asText;
	}

	public static String createAnnotatedPeaksText(HashMap<Double, MassSpecCustomAnnotationPeak> annotatedPeaks) {
		StringBuilder sb = new StringBuilder();
		if( annotatedPeaks != null && ! annotatedPeaks.isEmpty() ) {
			Iterator<Double> itr = annotatedPeaks.keySet().iterator();
			while( itr.hasNext() ) {
				Double dKey = itr.next();
				MassSpecCustomAnnotationPeak value = annotatedPeaks.get(dKey);
				if( value == null ) {
					continue;
				}
				if( ! sb.toString().equals("") ) {
					sb.append(VALUE_SEPERATOR);
				}
				String sXML = MassSpecCustomAnnotation.createAnnotatedPeaksText(value);
				sb.append(sXML);
			}
		}
		return sb.toString();
	}

	public static HashMap<Double, MassSpecCustomAnnotationPeak> unmarshalAnnotatedPeakText( MassSpecCustomAnnotation annotation ) {
		HashMap<Double, MassSpecCustomAnnotationPeak> annotatedPeaks = new HashMap<Double, MassSpecCustomAnnotationPeak>();
		try {
			if( annotation != null && annotation.getsAnnotatedPeakText() != null && ! annotation.getsAnnotatedPeakText().equals("") ) {
				String[] annotations = annotation.getsAnnotatedPeakText().trim().split(VALUE_SEPERATOR);
				if( annotations != null && annotations.length > 0 ) {
					for( int i = 0; i < annotations.length; i++ ) {
						String annotXML = annotations[i];
						MassSpecCustomAnnotationPeak peak = annotation.getPeakObjectFromXML(annotXML);
						if( peak != null ) {
							annotatedPeaks.put(peak.getPeakMz(), peak);
						}
					}
				}
			}
		} catch (Exception ex) {
			logger.error("Error loading default options from xml files.", ex);
		}
		return annotatedPeaks;
	}
	
	public MassSpecCustomAnnotationPeak getPeakObjectFromXML( String peakXML ) {
		MassSpecCustomAnnotationPeak peak = (MassSpecCustomAnnotationPeak) XMLUtils.getObjectFromXML(peakXML, MassSpecCustomAnnotationPeak.class);		
		return peak;
	}
	
	public MassSpecCustomAnnotationTemplate getNewTemplate() {
		return new MassSpecCustomAnnotationTemplate();
	}
	
	public MassSpecCustomAnnotationTemplate copyToNewTemplate() {
		MassSpecCustomAnnotationTemplate template = getNewTemplate();
		template.setAnnotationName(getAnnotationName());
		template.setDescription(getDescription());
		template.setIsPPM(getIsPPM());
		template.setMassTolerance(getMassTolerance());
		template.setMSLevel(getMSLevel());
		template.setTemplatePeaks(this);
		
		return template;
	}

}
