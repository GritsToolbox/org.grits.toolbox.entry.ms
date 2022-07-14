package org.grits.toolbox.entry.ms.preference;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.grits.toolbox.core.datamodel.UnsupportedVersionException;
import org.grits.toolbox.core.preference.share.PreferenceEntity;
import org.grits.toolbox.core.preference.share.PreferenceReader;
import org.grits.toolbox.core.preference.share.PreferenceWriter;
import org.grits.toolbox.core.utilShare.XMLUtils;

@XmlRootElement
public class MSConvertPreference {
	public static final String PREFERENCE_NAME = "org.grits.toolbox.entry.ms.msconvert";

	private static final String CURRENT_VERSION = "1.0";
	
	String location;
	String fileType;    // mzXML or mzML
	
	@XmlAttribute
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	
	@XmlAttribute
	public String getFileType() {
		return fileType;
	}
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	
	/**
	 * default options. there is no default location. default file type is mzXML
	 */
	public void loadDefaultOptions() {
		fileType = "mzXML";
	}
	
	/**
	 * save the preference into .preferences.xml file
	 * 
	 * @return true if successful, false if failed
	 */
	public boolean saveValues() {
		PreferenceEntity preferenceEntity = new PreferenceEntity(PREFERENCE_NAME);
		preferenceEntity.setVersion(CURRENT_VERSION);
		preferenceEntity.setValue(XMLUtils.marshalObjectXML(this));
		return PreferenceWriter.savePreference(preferenceEntity);	
	}
	
	/**
	 * load ms convert preferences from the .preferences.xml file (if any)
	 * 
	 * @return msconvert preferences from the file or an empty one if there is currently no msconvert preference in .preferences.xml file
	 * @throws UnsupportedVersionException if the version of the preference is not supported
	 */
	public static MSConvertPreference loadPreferences() throws UnsupportedVersionException {
		MSConvertPreference preferenceSettings = null;
		PreferenceEntity preferenceEntity = PreferenceReader.getPreferenceByName(MSConvertPreference.PREFERENCE_NAME);
		if (preferenceEntity != null && preferenceEntity.getValue() != null && !preferenceEntity.getValue().trim().isEmpty()) {
			preferenceSettings = (MSConvertPreference) XMLUtils.getObjectFromXML(preferenceEntity.getValue(), MSConvertPreference.class);
		} else {
			preferenceSettings = new MSConvertPreference();
		}
		return preferenceSettings;
	}
}
