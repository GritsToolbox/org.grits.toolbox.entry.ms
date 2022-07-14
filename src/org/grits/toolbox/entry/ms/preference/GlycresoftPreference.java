package org.grits.toolbox.entry.ms.preference;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.grits.toolbox.core.datamodel.UnsupportedVersionException;
import org.grits.toolbox.core.preference.share.PreferenceEntity;
import org.grits.toolbox.core.preference.share.PreferenceReader;
import org.grits.toolbox.core.preference.share.PreferenceWriter;
import org.grits.toolbox.core.utilShare.XMLUtils;

@XmlRootElement
public class GlycresoftPreference {
	public static final String PREFERENCE_NAME = "org.grits.toolbox.entry.ms.glycresoft";

	private static final String CURRENT_VERSION = "1.0";
	
	String location;
	
	@XmlAttribute
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
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
	 * load glycresoft preferences from the .preferences.xml file (if any)
	 * 
	 * @return glycresoft preferences from the file or an empty one if there is currently no glycresoft preference in .preferences.xml file
	 * @throws UnsupportedVersionException if the version of the preference is not supported
	 */
	public static GlycresoftPreference loadPreferences() throws UnsupportedVersionException {
		GlycresoftPreference preferenceSettings = null;
		PreferenceEntity preferenceEntity = PreferenceReader.getPreferenceByName(GlycresoftPreference.PREFERENCE_NAME);
		if (preferenceEntity != null && preferenceEntity.getValue() != null && !preferenceEntity.getValue().trim().isEmpty()) {
			preferenceSettings = (GlycresoftPreference) XMLUtils.getObjectFromXML(preferenceEntity.getValue(), GlycresoftPreference.class);
		} else {
			preferenceSettings = new GlycresoftPreference();
		}
		return preferenceSettings;
	}
}
