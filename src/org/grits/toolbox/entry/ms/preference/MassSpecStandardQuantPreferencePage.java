package org.grits.toolbox.entry.ms.preference;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class MassSpecStandardQuantPreferencePage extends PreferencePage implements IPropertyChangeListener{
	
	//log4J Logger
	private static final Logger logger = Logger.getLogger(MassSpecStandardQuantPreferencePage.class);

	protected MassSpecStandardQuantPreferenceUI standardQuantPreference;
	protected MassSpecPreference preferences;
	
	public MassSpecStandardQuantPreferencePage() {
		preferences = (MassSpecPreference) MassSpecPreference.loadWorkspacePreferences();
	}

	@Override
	protected Control createContents(Composite parent) {
		standardQuantPreference = new MassSpecStandardQuantPreferenceUI(parent, SWT.BORDER, this, false);
		standardQuantPreference.setLocalStandardQuant(preferences);
		standardQuantPreference.initComponents();
		
		return parent;
	}

	@Override
	public boolean isValid() {
    	if( ! standardQuantPreference.isPageCompete() ) {
    		setErrorMessage( standardQuantPreference.getErrorMessage() );
    		return false;
    	}
        setErrorMessage(null);
        return true;
	}
	
	@Override
	//when apply button is clicked
	protected void performApply() {
		//to check if everything is ok or not
		//but why we need this?
		save();
	}

	@Override
	public boolean performOk() {
		//need to save
		save();
		return true;
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		setValid(isValid());
	}
	
	/**
	 * save the values
	 */
	protected void save() {
		try {
			standardQuantPreference.updateSettings();
			standardQuantPreference.updatePreferences();
			logger.debug("Time to save values!");
			standardQuantPreference.save();
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
	}

	@Override
	protected void performDefaults() {
		boolean load = MessageDialog.openConfirm(getShell(), "Are you sure?", 
				"This will remove all the standard quantification preferences you've created, if any. Do you want to continue?");
		if (load) {
			preferences = new MassSpecPreference();
//			preferences.loadDefaultCustomAnnotations();
			
			preferences.saveValues();
			standardQuantPreference.clearValues();
			standardQuantPreference.initStoredStandardQuant();
			standardQuantPreference.initLocalStandardQuant();
			standardQuantPreference.processSelection(null);
			standardQuantPreference.updateUI();			
		}
	}

}
