package org.grits.toolbox.entry.ms.preference.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.grits.toolbox.core.utilShare.XMLUtils;

@XmlRootElement(name = "massSpecCustomAnnotation")
public class MassSpecCustomAnnotationTemplate {
	private static final Logger logger = Logger.getLogger(MassSpecCustomAnnotationTemplate.class);

	protected String sAnnotationName = null;
	protected String sDescription = null;
	protected Integer iMSLevel = null;
	protected Double dMassTolerance = null;
	protected Boolean bIsPPM = null;
	private List<MassSpecCustomAnnotationPeak> annotatedPeaks = null;
	
	public MassSpecCustomAnnotationTemplate() {
		// TODO Auto-generated constructor stub
	}
	
	public MassSpecCustomAnnotationTemplate( String sAnnotationName, String sDescription, Integer iMSLevel, 
			Double dMassTolerance, Boolean bIsPPM ) {
		this.sAnnotationName = sAnnotationName;
		this.sDescription = sDescription;
		this.iMSLevel = iMSLevel;
		this.dMassTolerance = dMassTolerance;
		this.bIsPPM = bIsPPM;
		this.annotatedPeaks = new ArrayList<MassSpecCustomAnnotationPeak>();
	}
		
	@Override
	public String toString() {
		return getAnnotationName() + ", num peaks: " + getMassSpecCustomAnnotationPeaks().size();
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

	public MassSpecCustomAnnotation getNewAnnotation() {
		return new MassSpecCustomAnnotation();
	}
	
	private List<MassSpecCustomAnnotationPeak> getMassSpecCustomAnnotationPeaks() {
		return annotatedPeaks;
	}
	@XmlElement(name = "massSpecCustomAnnotationPeaks")
	private void setMassSpecCustomAnnotationPeaks(List<MassSpecCustomAnnotationPeak> annotatedPeaks) {
		this.annotatedPeaks = annotatedPeaks;
	}
	
	private void setMassSpecCustomAnnotationPeaks( MassSpecCustomAnnotation annotation ) {
		annotation.setAnnotatedPeaks(new HashMap<>());
		for( MassSpecCustomAnnotationPeak peak : getMassSpecCustomAnnotationPeaks() ) {
			annotation.getAnnotatedPeaks().put(peak.getPeakMz(),  peak);
		}
	}
	
	protected void setAnnotationPeaks( MassSpecCustomAnnotation annotation ) {
		setMassSpecCustomAnnotationPeaks(annotation);
	}
	
	private void setMassSpecCustomTemplatePeaks( MassSpecCustomAnnotation annotation ) {
		setMassSpecCustomAnnotationPeaks(new ArrayList<>() );
		for( Double dMz : annotation.getAnnotatedPeaks().keySet() ) {
			MassSpecCustomAnnotationPeak peak = annotation.getAnnotatedPeaks().get(dMz);
			getMassSpecCustomAnnotationPeaks().add(peak);
		}
	}

	protected void setTemplatePeaks( MassSpecCustomAnnotation annotation ) {
		setMassSpecCustomTemplatePeaks(annotation);
	}
	
	public MassSpecCustomAnnotation copyToNewAnnotation() {
		MassSpecCustomAnnotation annotation = getNewAnnotation();
		annotation.setAnnotationName(getAnnotationName());
		annotation.setDescription(getDescription());
		annotation.setIsPPM(getIsPPM());
		annotation.setMassTolerance(getMassTolerance());
		annotation.setMSLevel(getMSLevel());
		setAnnotationPeaks(annotation);
		
		return annotation;
	}
	
	public static MassSpecCustomAnnotationTemplate unmarshalAnnotationTemplate( String xmlFile ) {
		MassSpecCustomAnnotationTemplate annotationTemplate = (MassSpecCustomAnnotationTemplate) XMLUtils.unmarshalObjectXML(xmlFile, MassSpecCustomAnnotationTemplate.class);
		return annotationTemplate;
	}
	
	public static String marshalAnnotatedTemplate(MassSpecCustomAnnotationTemplate annotationTemplate) {
		String sAnnotationText = XMLUtils.marshalObjectXML(annotationTemplate);
		return sAnnotationText;
	}
}
