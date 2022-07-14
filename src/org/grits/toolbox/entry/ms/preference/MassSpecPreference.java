package org.grits.toolbox.entry.ms.preference;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.grits.toolbox.core.datamodel.UnsupportedVersionException;
import org.grits.toolbox.core.preference.PreferenceHandler;
import org.grits.toolbox.core.preference.share.PreferenceEntity;
import org.grits.toolbox.core.preference.share.PreferenceReader;
import org.grits.toolbox.core.preference.share.PreferenceWriter;
import org.grits.toolbox.core.utilShare.XMLUtils;
import org.grits.toolbox.entry.ms.Activator;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecCustomAnnotation;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecCustomAnnotationTemplate;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecStandardQuant;
import org.grits.toolbox.ms.om.data.Method;


/**
 * Stores the preferences for an MS entry to be marshalled to/from XML.
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 *
 */
@XmlRootElement(name = "massSpecPreferences")
public class MassSpecPreference implements IMSPreferenceWithCustomAnnotation, IMSPreferenceWithStandardQuant {
	private static final Logger logger = Logger.getLogger(MassSpecPreference.class);
	private static final String PREFERENCE_NAME_ALL = "org.grits.toolbox.entry.ms.all";
	private static final String CURRENT_VERSION = "1.2";
	/*
	 * 02/11/16, version 1.1:  added MS Experiment type as a preference
	 * 11/13/17, version 1.2:  added support for Standard Quanti
	 */

	private static final String DEFAULT_ADDUCT_FILE = "msSettings.adductType.xml";
	private static final String DEFAULT_COLLISION_FILE = "msSettings.collisionType.xml";
	private static final String DEFAULT_DERIVITIZATION_FILE = "msSettings.derivType.xml";
	private static final String DEFAULT_GLYCAN_FILE = "msSettings.glycanType.xml";
	private static final String DEFAULT_RELEASE_FILE = "msSettings.releaseType.xml";
	private static final String VALUE_SEPERATOR = "~~~~~";

	private String derivatizationType = null;
	private Set<String> allDerivatizationTypes = null;

	private String collisionType = null;
	private Set<String> allCollisionTypes = null;

	private String adductType = null;
	private Set<String> allAdductTypes = null;

	private String releaseType = null;
	private Set<String> allReleaseTypes = null;

	private String glycanType = null;
	private Set<String> allGlycanTypes = null;

	private String instrument = "";
	private Double collisionEnergy = Double.valueOf(30.0);

	private String experimentType = null;
	private Set<String> allExperimentTypes = null;

	private List<MassSpecCustomAnnotation> customAnnotations = new ArrayList<MassSpecCustomAnnotation>();
	private String customAnnotationsText = null; // stores the XML that will be written out

	private List<MassSpecStandardQuant> standardQuantList = new ArrayList<MassSpecStandardQuant>();
	private String standardQuantText = null; // stores the XML that will be written out
	
	public String getExperimentType() {
		return experimentType;
	}
	@XmlAttribute(name="experimentType")
	public void setExperimentType(String experimentType) {
		this.experimentType = experimentType;
	}

	public Set<String> getAllExperimentTypes() {
		return allExperimentTypes;
	}
	@XmlTransient
	public void setAllExperimentTypes(Set<String> allExperimentTypes) {
		this.allExperimentTypes = allExperimentTypes;
	}

	public String getAdductType() {
		return adductType;
	}
	@XmlAttribute(name="adductType")
	public void setAdductType(String adductType) {
		this.adductType = adductType;
	}

	public Set<String> getAllAdductTypes() {
		return allAdductTypes;
	}
	@XmlTransient
	public void setAllAdductTypes(Set<String> allAdductTypes) {
		this.allAdductTypes = allAdductTypes;
	}

	public String getCollisionType() {
		return collisionType;
	}
	@XmlAttribute(name="collisionType")
	public void setCollisionType(String collisionType) {
		this.collisionType = collisionType;
	}

	public Set<String> getAllCollisionTypes() {
		return allCollisionTypes;
	}
	@XmlTransient
	public void setAllCollisionTypes(Set<String> allCollisionTypes) {
		this.allCollisionTypes = allCollisionTypes;
	}

	public String getDerivatizationType() {
		return derivatizationType;
	}
	@XmlAttribute(name="derivType")
	public void setDerivatizationType(String derivatizationType) {
		this.derivatizationType = derivatizationType;
	}

	public Set<String> getAllDerivatizationTypes() {
		return allDerivatizationTypes;
	}
	@XmlTransient
	public void setAllDerivatizationTypes(Set<String> allDerivatizationTypes) {
		this.allDerivatizationTypes = allDerivatizationTypes;
	}

	public String getGlycanType() {
		return glycanType;
	}
	@XmlAttribute(name="glycanType")
	public void setGlycanType(String glycanType) {
		this.glycanType = glycanType;
	}

	public Set<String> getAllGlycanTypes() {
		return allGlycanTypes;
	}
	@XmlTransient
	public void setAllGlycanTypes(Set<String> allGlycanTypes) {
		this.allGlycanTypes = allGlycanTypes;
	}

	public String getReleaseType() {
		return releaseType;
	}
	@XmlAttribute(name="releaseType")
	public void setReleaseType(String releaseType) {
		this.releaseType = releaseType;
	}

	public Set<String> getAllReleaseTypes() {
		return allReleaseTypes;
	}
	@XmlTransient
	public void setAllReleaseTypes(Set<String> allReleaseTypes) {
		this.allReleaseTypes = allReleaseTypes;
	}

	public String getInstrument() {
		return instrument;
	}
	@XmlAttribute(name="instrument")
	public void setInstrument(String instrument) {
		this.instrument = instrument;
	}

	public Double getCollisionEnergy() {
		return collisionEnergy;
	}
	@XmlAttribute(name="collisionEnergy")
	public void setCollisionEnergy(Double collisionEnergy) {
		this.collisionEnergy = collisionEnergy;
	}

	public List<MassSpecCustomAnnotation> getCustomAnnotations() {
		return customAnnotations;
	}
	@XmlTransient
	public void setCustomAnnotations(List<MassSpecCustomAnnotation> customAnnotations) {
		this.customAnnotations = customAnnotations;
	}

	public String getCustomAnnotationText() {
		return customAnnotationsText;
	}
	@XmlAttribute(name="customAnnotationsText")
	public void setCustomAnnotationText(String customAnnotationsText) {
		this.customAnnotationsText = customAnnotationsText;
	}
	
	public static MassSpecCustomAnnotation lookupCustomAnnotation( List<MassSpecCustomAnnotation> customAnnotations,
			String annotationName ) {
		if( customAnnotations != null && ! customAnnotations.isEmpty() ) {
			for( int i = 0; i < customAnnotations.size(); i++ ) {
				MassSpecCustomAnnotation annotation = customAnnotations.get(i);
				if( annotation.getAnnotationName().equalsIgnoreCase(annotationName) )  {
					return annotation;
				}
			}			
		}
		return null;
	}

	public static String createCustomAnnotationsText(MassSpecCustomAnnotation annotation) {
		annotation.updateAnnotatedPeakText();
		String asText = XMLUtils.marshalObjectXML(annotation);
		return asText;
	}

	public static String createCustomAnnotationsText(List<MassSpecCustomAnnotation> customAnnotations) {
		StringBuilder sb = new StringBuilder();
		int iAnnotCnt = 0;
		if( customAnnotations != null && ! customAnnotations.isEmpty() ) {
			for( int i = 0; i < customAnnotations.size(); i++ ) {
				MassSpecCustomAnnotation annotation = customAnnotations.get(i);
				if( annotation == null ) {
					continue;
				}
				if( iAnnotCnt > 0 ) {
					sb.append(VALUE_SEPERATOR);
				}
				String sXML = MassSpecPreference.createCustomAnnotationsText(annotation);
				sb.append(sXML);
				iAnnotCnt++;
			}
		}
		return sb.toString();
	}

	public static List<MassSpecCustomAnnotation> unmarshalCustomAnnotationsList( String sCustomAnnotationText ) {
		List<MassSpecCustomAnnotation> msca = new ArrayList<MassSpecCustomAnnotation>();
		try {
			if( sCustomAnnotationText != null ) {
				if(  sCustomAnnotationText.startsWith(VALUE_SEPERATOR) ) { // this is an error....fix just in case
					int iStartPos = sCustomAnnotationText.indexOf(VALUE_SEPERATOR) + VALUE_SEPERATOR.length();
					sCustomAnnotationText = sCustomAnnotationText.substring(iStartPos);
				}
				if( ! sCustomAnnotationText.equals("") ) {
					String[] annotations = sCustomAnnotationText.split(VALUE_SEPERATOR);
					if( annotations != null && annotations.length > 0 ) {
						for( int i = 0; i < annotations.length; i++ ) {
							String annotXML = annotations[i];
							MassSpecCustomAnnotation annotation = (MassSpecCustomAnnotation) XMLUtils.getObjectFromXML(annotXML, MassSpecCustomAnnotation.class);
							if( annotation != null ) {
								annotation.unmarshalAnnotatedPeakList();
								msca.add(annotation);
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			logger.error("Error loading default options from xml files.", ex);
		}
		return msca;
	}

	public List<MassSpecStandardQuant> getStandardQuant() {
		return standardQuantList;
	}
	@XmlTransient
	public void setStandardQuant(List<MassSpecStandardQuant> standardQuant) {
		this.standardQuantList = standardQuant;
	}

	public String getStandardQuantText() {
		return standardQuantText;
	}
	@XmlAttribute(name="standardQuantText")
	public void setStandardQuantText(String standardQuantText) {
		this.standardQuantText = standardQuantText;
	}
	
	public static MassSpecStandardQuant lookupStandardQuant( List<MassSpecStandardQuant> standardQuantList,
			String standardQuantName ) {
		if( standardQuantList != null && ! standardQuantList.isEmpty() ) {
			for( int i = 0; i < standardQuantList.size(); i++ ) {
				MassSpecStandardQuant standardQuant = standardQuantList.get(i);
				if( standardQuant.getStandardQuantName().equalsIgnoreCase(standardQuantName) )  {
					return standardQuant;
				}
			}			
		}
		return null;
	}

	public static String createStandardQuantText(MassSpecStandardQuant standardQuant) {
		standardQuant.updateStandardQuantPeakText();
		String asText = XMLUtils.marshalObjectXML(standardQuant);
		return asText;
	}

	public static String createStandardQuantText(List<MassSpecStandardQuant> standardQuantList) {
		StringBuilder sb = new StringBuilder();
		int iAnnotCnt = 0;
		if( standardQuantList != null && ! standardQuantList.isEmpty() ) {
			for( int i = 0; i < standardQuantList.size(); i++ ) {
				MassSpecStandardQuant standardQuant = standardQuantList.get(i);
				if( standardQuant == null ) {
					continue;
				}
				if( iAnnotCnt > 0 ) {
					sb.append(VALUE_SEPERATOR);
				}
				String sXML = MassSpecPreference.createStandardQuantText(standardQuant);
				sb.append(sXML);
				iAnnotCnt++;
			}
		}
		return sb.toString();
	}

	public static List<MassSpecStandardQuant> unmarshalStandardQuantList( String sStandardQuantText ) {
		List<MassSpecStandardQuant> mssq = new ArrayList<MassSpecStandardQuant>();
		try {
			if( sStandardQuantText != null ) {
				if(  sStandardQuantText.startsWith(VALUE_SEPERATOR) ) { // this is an error....fix just in case
					int iStartPos = sStandardQuantText.indexOf(VALUE_SEPERATOR) + VALUE_SEPERATOR.length();
					sStandardQuantText = sStandardQuantText.substring(iStartPos);
				}
				if( ! sStandardQuantText.equals("") ) {
					String[] stdQuantList = sStandardQuantText.split(VALUE_SEPERATOR);
					if( stdQuantList != null && stdQuantList.length > 0 ) {
						for( int i = 0; i < stdQuantList.length; i++ ) {
							String stdQuantXML = stdQuantList[i];
							MassSpecStandardQuant standardQuant = (MassSpecStandardQuant) XMLUtils.getObjectFromXML(stdQuantXML, MassSpecStandardQuant.class);
							if( standardQuant != null ) {
								standardQuant.unmarshalStandardQuantPeakList();
								mssq.add(standardQuant);
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			logger.error("Error loading default options from xml files.", ex);
		}
		return mssq;
	}
	
	private static MassSpecPreference getMassSpecPreferencesFromXML(String xmlString)
	{
		MassSpecPreference msPreference = (MassSpecPreference) XMLUtils.getObjectFromXML(xmlString, MassSpecPreference.class);
		return msPreference;
	}

	public static String marshalXML(MassSpecPreference object)
	{
		String xmlString = XMLUtils.marshalObjectXML(object);
		return xmlString;
	}

	private String loadDefaultOptions( Set<String> _sAllPreferences, String _sDefaultPreferenceFile ) {
		String sDefault = null;
		try {
			HashMap<String, Boolean> optionMap = PreferenceHandler.getPreferenceValues(_sDefaultPreferenceFile, Activator.PLUGIN_ID);
			for(String option : optionMap.keySet()) {
				_sAllPreferences.add(option);
				if(optionMap.get(option) ) {
					sDefault = option;
				}
			}
		} catch (Exception ex) {
			logger.error("Error loading default options from xml files.", ex);
			throw ex;
		}
		return sDefault;
	}

	private String loadDefaultOptions( Set<String> _sAllPreferences, String[] _sDefaultOptions ) {
		String sDefault = null;
		try {
			for(String option : _sDefaultOptions) {
				_sAllPreferences.add(option);
				if(sDefault == null ) { // just take the first one
					sDefault = option;
				}
			}
		} catch (Exception ex) {
			logger.error("Error loading default options from String array.", ex);
			throw ex;
		}
		return sDefault;
	}

	public void loadDefaultOptions() {
		allDerivatizationTypes = new HashSet<String>();
		allAdductTypes = new HashSet<String>();
		allCollisionTypes = new HashSet<String>();
		allGlycanTypes = new HashSet<String>();
		allReleaseTypes = new HashSet<String>();
		allExperimentTypes = new HashSet<String>();

		try	{
			// first load all of the options that go in controls. These are read from xml files in the plugin
			String sDefault = null;
			try {
				sDefault = loadDefaultOptions(allDerivatizationTypes, DEFAULT_DERIVITIZATION_FILE);
				if( this.derivatizationType == null ) {
					this.derivatizationType = sDefault;
				}
			} catch( Exception ex) {
				throw new Exception("Unable to load dervitization preferences", ex);
			}
			try {
				sDefault = loadDefaultOptions(allAdductTypes, DEFAULT_ADDUCT_FILE);
				if( this.adductType == null ) {
					this.adductType = sDefault;
				}
			} catch( Exception ex) {
				throw new Exception("Unable to load adduct preferences", ex);
			}
			try {
				sDefault = loadDefaultOptions(allCollisionTypes, DEFAULT_COLLISION_FILE);
				if( this.collisionType == null ) {
					this.collisionType = sDefault;
				}
			} catch( Exception ex) {
				throw new Exception("Unable to load collision preferences", ex);
			}
			try {
				sDefault = loadDefaultOptions(allGlycanTypes, DEFAULT_GLYCAN_FILE);
				if( this.glycanType == null ) {
					this.glycanType = sDefault;
				}
			} catch( Exception ex) {
				throw new Exception("Unable to load glycan preferences", ex);
			}
			try {
				sDefault = loadDefaultOptions(allReleaseTypes, DEFAULT_RELEASE_FILE);
				if( this.releaseType == null ) {
					this.releaseType = sDefault;
				}
			} catch( Exception ex) {
				throw new Exception("Unable to load release preferences", ex);
			}

			try {
				sDefault = loadDefaultOptions(allExperimentTypes, Method.MS_TYPES);
				if( this.experimentType == null ) {
					this.experimentType = sDefault;
				}				
			} catch( Exception ex) {
				throw new Exception("Unable to load experiment type preferences", ex);
			}

			if( customAnnotations == null ) {
				customAnnotations = new ArrayList<MassSpecCustomAnnotation>();				
			}


		} catch (Exception ex) {
			logger.error("Error loading default options", ex);
		}
	}

	public static MassSpecPreference getMassSpecPreferences(PreferenceEntity preferenceEntity) throws UnsupportedVersionException
	{
		MassSpecPreference preferenceSettings = null;
		if(preferenceEntity != null)
		{
			preferenceSettings = MassSpecPreference.getMassSpecPreferencesFromXML(preferenceEntity.getValue());
			List<MassSpecCustomAnnotation> ca = MassSpecPreference.unmarshalCustomAnnotationsList(preferenceSettings.getCustomAnnotationText());
			preferenceSettings.setCustomAnnotations(ca);
			List<MassSpecStandardQuant> sq = MassSpecPreference.unmarshalStandardQuantList(preferenceSettings.getStandardQuantText());
			preferenceSettings.setStandardQuant(sq);
			preferenceSettings.loadDefaultOptions(); // we always go to the file for the default layout, orientations, and styles
		}
		return preferenceSettings;
	}

	public boolean saveValues() {
		PreferenceEntity preferenceEntity = new PreferenceEntity(PREFERENCE_NAME_ALL);
		preferenceEntity.setVersion(CURRENT_VERSION);
		setCustomAnnotationText( MassSpecPreference.createCustomAnnotationsText(getCustomAnnotations()) );
		setStandardQuantText( MassSpecPreference.createStandardQuantText(getStandardQuant()));
		preferenceEntity.setValue(MassSpecPreference.marshalXML(this));
		return PreferenceWriter.savePreference(preferenceEntity);
	}

	public static PreferenceEntity getPreferenceEntity() throws UnsupportedVersionException {
		PreferenceEntity preferenceEntity = PreferenceReader.getPreferenceByName(PREFERENCE_NAME_ALL);
		return preferenceEntity;
	}
	
	public static IMSPreferenceWithCustomAnnotation loadWorkspacePreferences() {
		try {
			return MassSpecPreferenceLoader.getMassSpecPreferences();

		} catch (Exception ex) {
			logger.error("Error getting the mass spec preferences", ex);
		}
		return null;
	}

	public void loadDefaultCustomAnnotations() {
		// load all files in preference folder
		URL resourceURL;
		customAnnotations.clear();
		try {
			resourceURL = FileLocator.toFileURL(
					Platform.getBundle(org.grits.toolbox.entry.ms.Activator.PLUGIN_ID).getResource("preference"));
			File preferenceDir= new File(resourceURL.getPath());
			if (preferenceDir.exists() && preferenceDir.isDirectory()) {
				File[] prefSubDirs = preferenceDir.listFiles();
				for (File subDir : prefSubDirs) {
					if( subDir.isDirectory() && subDir.getName().equals("customAnnotation") ) {
						File[] files = subDir.listFiles();
						for (File file : files) {
							if (file.getName().endsWith(".xml")) {
								processMassSpecCustomAnnotation(file.getAbsolutePath());
							}
						}
					}
				}
			}
		} catch (IOException e) {
			logger.error("Could not load default preference files", e);
		} 
	}

	private void processMassSpecCustomAnnotation( String fileName ) {
		try {
			MassSpecCustomAnnotationTemplate template = MassSpecCustomAnnotationTemplate.unmarshalAnnotationTemplate(fileName);
			MassSpecCustomAnnotation msca = (MassSpecCustomAnnotation) template.copyToNewAnnotation();
			customAnnotations.add(msca);
		} catch (Exception e) {
			logger.warn(fileName + " is not a valid preference file");
		}
	}
	
}
