package org.grits.toolbox.entry.ms.preference;

import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.grits.toolbox.core.utilShare.ComboPopupSelectionListener;

public class MassSpecPreferencePage extends PreferencePage
implements IPropertyChangeListener {

	//log4J Logger
	private static final Logger logger = Logger.getLogger(MassSpecPreferencePage.class);
	private final static Font boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT); 
	
	private MassSpecSettingPreferenceUI settingsPreference = null;
	protected MassSpecPreference preferences = null;

	public MassSpecPreferencePage() {
		preferences = (MassSpecPreference) MassSpecPreference.loadWorkspacePreferences();
	}

	@Override
	protected Control createContents(Composite parent) {
		settingsPreference = new MassSpecSettingPreferenceUI(parent, SWT.NONE, this);
		settingsPreference.setPreferences(preferences);
		settingsPreference.setbAddExperimentType(true);
		settingsPreference.initComponents();
		
		return parent;
	}
	
	@Override
	public boolean isValid() {
    	if( ! settingsPreference.isPageCompete() ) {
    		setErrorMessage( settingsPreference.getErrorMessage() );
    		return false;
    	}
        setErrorMessage(null);
        return true;
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		setValid(isValid());
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

//	protected void setPageComplete(boolean b) {
//		//To do 
//		if(b)
//		{
//			setErrorMessage(null);
//		}
//		setValid(b);
//	}

	//save values
	protected void save() {
		try {
			settingsPreference.updateSettings();
			settingsPreference.updatePreferences();
			logger.debug("Time to save values!");
			preferences.saveValues();
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
	}

	public static void setMandatoryLabel(Label label) {
		label.setText(label.getText()+"*");
		label.setFont(boldFont);
	}

	public static boolean hasChanged( String newVal, String prefVal, Set<String> prefVals ) {
		return newVal != null && ! newVal.equals("") && 
				! newVal.equals("other") && ! newVal.equals(prefVal) && 
				(prefVals == null || prefVals.contains(newVal));
	}
	
	public static Label createLabel(Composite container, String labelName) {
		Label label = new Label(container, SWT.NONE);
		label.setText(labelName);
		return label;
	}

	public static Text createText(Composite container, Label label, String selected) {
		GridData nameData = new GridData();
		label.setLayoutData(nameData);

		GridData nameTextData = new GridData();
		nameTextData.grabExcessHorizontalSpace = true;
		nameTextData.horizontalAlignment = GridData.FILL;
		nameTextData.horizontalSpan = 3;
		Text text = new Text(container, SWT.BORDER);
		text.setText(selected != null ? selected : "");
		text.setLayoutData(nameTextData);
		return text;
	}	
	
	public static Label createSeparator(Composite parent, int span) {
		GridData separatorData = new GridData();
		separatorData.grabExcessHorizontalSpace = true;
		separatorData.horizontalAlignment = GridData.FILL;
		separatorData.horizontalSpan = span;
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(separatorData);
		return separator;
	}
	
	public static Combo createCombo(Composite container, Label label, Set<String> values, String selected, boolean addEmpty, boolean addOther) {
		GridData derivatizationLabelData = new GridData();
		label.setLayoutData(derivatizationLabelData);

		//create list of species
		Combo combo = new Combo(container, SWT.FLAT | SWT.READ_ONLY);
		GridData comboData = new GridData();
		comboData.grabExcessHorizontalSpace = true;
		comboData.horizontalAlignment = GridData.FILL;
		comboData.horizontalSpan = 3;
		combo.setLayoutData(comboData);

		//add empty string as the first item in a combo list
		int count = 0;
		if( addEmpty ) {
			combo.add("");
			count++;
		}
		int sel = -1;

		if(values != null) {
			for(String tt : values) {
				if(selected != null) {
					if(tt.toString().equals(selected)) {
						sel = count;
					}
				}
				combo.add(tt);
				count++;
			}
		}
		if( addOther ) {
			combo.add("other");
		}
		if(sel != -1 ) {
			combo.select(sel);
		}

		//add a listener
		ComboPopupSelectionListener comboListener = new ComboPopupSelectionListener();
		comboListener.setParent(container);
		combo.addSelectionListener(comboListener);
		return combo;
	}
	
}
