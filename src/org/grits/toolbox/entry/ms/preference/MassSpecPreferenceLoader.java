package org.grits.toolbox.entry.ms.preference;

import org.apache.log4j.Logger;
import org.grits.toolbox.core.datamodel.UnsupportedVersionException;
import org.grits.toolbox.core.preference.share.PreferenceEntity;

import org.grits.toolbox.display.control.table.preference.TableViewerPreferenceLoader;
import org.grits.toolbox.display.control.table.preference.TableViewerPreferencePreVersion;

public class MassSpecPreferenceLoader {
	private static final Logger logger = Logger.getLogger(TableViewerPreferenceLoader.class);

	public static MassSpecPreference getMassSpecPreferences()  {
		MassSpecPreference preferences = null;
		try {
			PreferenceEntity preferenceEntity = MassSpecPreference.getPreferenceEntity(); 
			if( preferenceEntity == null ) { // previous version
				preferences = MassSpecPreferencePreVersion.getMassSpecPreference(preferenceEntity);
				
				if( preferences != null ) {
					TableViewerPreferencePreVersion.removeElements();
					preferences.saveValues();
				}
			} else {
				preferences = MassSpecPreference.getMassSpecPreferences(preferenceEntity);
			}
		} catch (UnsupportedVersionException ex) {
			logger.error(ex.getMessage(), ex);
			
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}		
		if( preferences == null ) { // well, either no preferences yet or some error. initialize to defaults and return
			preferences = new MassSpecPreference();
			preferences.loadDefaultOptions();
		}
		return preferences;
	}

}
