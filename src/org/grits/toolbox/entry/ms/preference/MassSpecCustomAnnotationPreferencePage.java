package org.grits.toolbox.entry.ms.preference;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class MassSpecCustomAnnotationPreferencePage extends PreferencePage implements IPropertyChangeListener{
	
	//log4J Logger
	private static final Logger logger = Logger.getLogger(MassSpecCustomAnnotationPreferencePage.class);

	protected MassSpecCustomAnnotationsPreferenceUI customAnnotationPreference;
	protected MassSpecPreference preferences;
	
	public MassSpecCustomAnnotationPreferencePage() {
		preferences = (MassSpecPreference) MassSpecPreference.loadWorkspacePreferences();
	}

	@Override
	protected Control createContents(Composite parent) {
		customAnnotationPreference = new MassSpecCustomAnnotationsPreferenceUI(parent, SWT.BORDER, this, false);
		customAnnotationPreference.setLocalAnnotations(preferences);
		customAnnotationPreference.initComponents();
		
		return parent;
	}

	@Override
	public boolean isValid() {
    	if( ! customAnnotationPreference.isPageCompete() ) {
    		setErrorMessage( customAnnotationPreference.getErrorMessage() );
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
			customAnnotationPreference.updateSettings();
			customAnnotationPreference.updatePreferences();
			logger.debug("Time to save values!");
			customAnnotationPreference.save();
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
	}

	@Override
	protected void performDefaults() {
		boolean load = MessageDialog.openConfirm(getShell(), "Are you sure?", 
				"This will remove all the preferences you've created and load the default ones if any. Do you want to continue?");
		if (load) {
			preferences = new MassSpecPreference();
			preferences.loadDefaultCustomAnnotations();
			
			preferences.saveValues();
			customAnnotationPreference.clearValues();
			customAnnotationPreference.initStoredAnnotations();
			customAnnotationPreference.initLocalAnnotations();
			customAnnotationPreference.processSelection(null);
			customAnnotationPreference.updateUI();			
		}
	}

}
