package org.grits.toolbox.entry.ms.preference;

import org.apache.log4j.Logger;
import org.grits.toolbox.core.preference.project.UtilityPreferenceValue;
import org.grits.toolbox.core.preference.share.PreferenceEntity;
import org.grits.toolbox.core.preference.share.PreferenceReader;
import org.grits.toolbox.core.preference.share.PreferenceWriter;
import org.jdom.Element;

public class MassSpecPreferencePreVersion {
	private static final Logger logger = Logger.getLogger(MassSpecPreferencePreVersion.class);
	/* Legacy info. The types were removed */
	private static final String PREVIOUS_ADDUCT_FILE = "org.grits.toolbox.entry.ms.preference.type.Types.adductType";
	private static final String PREVIOUS_COLLISION_FILE = "org.grits.toolbox.entry.ms.preference.type.Types.collisionType";
	private static final String PREVIOUS_DERIVITIZATION_FILE = "org.grits.toolbox.entry.ms.preference.type.Types.derivType";
	private static final String PREVIOUS_GLYCAN_FILE = "org.grits.toolbox.entry.ms.preference.type.Types.glycanType";
	private static final String PREVIOUS_RELEASE_FILE = "org.grits.toolbox.entry.ms.preference.type.Types.releaseType";

	public static boolean removeElements() {
		try {
			PreferenceWriter.deletePreference(PREVIOUS_ADDUCT_FILE);
			PreferenceWriter.deletePreference(PREVIOUS_COLLISION_FILE);
			PreferenceWriter.deletePreference(PREVIOUS_DERIVITIZATION_FILE);
			PreferenceWriter.deletePreference(PREVIOUS_GLYCAN_FILE);
			PreferenceWriter.deletePreference(PREVIOUS_RELEASE_FILE);
			return true;
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
		return false;
	}

	public static MassSpecPreference getMassSpecPreference(PreferenceEntity preferenceEntity) {
		MassSpecPreference previousPreferences = null;
		try {
			previousPreferences = new MassSpecPreference();
			previousPreferences.loadDefaultOptions(); // make sure we have all values filled in case one or more entries is missing
			Element preferenceElement = PreferenceReader.getPreferenceElement(PREVIOUS_ADDUCT_FILE);
			if (preferenceElement != null && preferenceElement.getAttributeValue("values") != null && preferenceElement.getAttributeValue("values").length() != 0 ) {
				previousPreferences.setAllAdductTypes( UtilityPreferenceValue.getPreversioningValues(preferenceElement) ); 
				String selected = UtilityPreferenceValue.getPreversioningSelected(preferenceElement);				
				previousPreferences.setAdductType( selected );

			} 
			preferenceElement = PreferenceReader.getPreferenceElement(PREVIOUS_COLLISION_FILE);
			if (preferenceElement != null && preferenceElement.getAttributeValue("values") != null && preferenceElement.getAttributeValue("values").length() != 0 ) {
				previousPreferences.setAllCollisionTypes( UtilityPreferenceValue.getPreversioningValues(preferenceElement) ); 
				String selected = UtilityPreferenceValue.getPreversioningSelected(preferenceElement);				
				previousPreferences.setCollisionType( selected );

			} 
			preferenceElement = PreferenceReader.getPreferenceElement(PREVIOUS_DERIVITIZATION_FILE);
			if (preferenceElement != null && preferenceElement.getAttributeValue("values") != null && preferenceElement.getAttributeValue("values").length() != 0 ) {
				previousPreferences.setAllDerivatizationTypes( UtilityPreferenceValue.getPreversioningValues(preferenceElement) ); 
				String selected = UtilityPreferenceValue.getPreversioningSelected(preferenceElement);				
				previousPreferences.setDerivatizationType( selected );

			} 
			preferenceElement = PreferenceReader.getPreferenceElement(PREVIOUS_GLYCAN_FILE);
			if (preferenceElement != null && preferenceElement.getAttributeValue("values") != null && preferenceElement.getAttributeValue("values").length() != 0 ) {
				previousPreferences.setAllGlycanTypes( UtilityPreferenceValue.getPreversioningValues(preferenceElement) );
				String selected = UtilityPreferenceValue.getPreversioningSelected(preferenceElement);				
				previousPreferences.setGlycanType( selected );

			} 
			preferenceElement = PreferenceReader.getPreferenceElement(PREVIOUS_RELEASE_FILE);
			if (preferenceElement != null && preferenceElement.getAttributeValue("values") != null && preferenceElement.getAttributeValue("values").length() != 0 ) {
				previousPreferences.setAllReleaseTypes( UtilityPreferenceValue.getPreversioningValues(preferenceElement) ); 
				String selected = UtilityPreferenceValue.getPreversioningSelected(preferenceElement);				
				previousPreferences.setReleaseType( selected );

			} 

		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
			previousPreferences = null;
		}
		return previousPreferences;
	}

}
