package org.grits.toolbox.entry.ms.preference;

import org.apache.log4j.Logger;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import org.grits.toolbox.ms.om.data.Method;

public class MassSpecSettingPreferenceUI extends Composite {
	private static final Logger logger = Logger.getLogger(MassSpecSettingPreferenceUI.class);

	private Combo msExperimentCombo;
	private String msExperimentType=null;
	private Label msExperimentLabel;

	private Text instrumentText;
	private String instrument=null;
	private Label instrumentLabel;

	private MassSpecPreference preferences = null;
	private boolean isComplete = true;
	private String errorMessage = null;
	IPropertyChangeListener listener = null;
	
	private boolean bAddExperimentType = false;

	public MassSpecSettingPreferenceUI(Composite parent, int style, IPropertyChangeListener listener ) {
		super(parent, style);
		this.listener = listener;
	}

	public void setbAddExperimentType(boolean bAddExperimentType) {
		this.bAddExperimentType = bAddExperimentType;
	}
	
	public void setPreferences(MassSpecPreference preferences) {
		this.preferences = preferences;
	}

	public MassSpecPreference getPreferences() {
		return preferences;
	}

	public void initComponents() {
		//has to be gridLayout, since it extends TitleAreaDialog
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		gridLayout.verticalSpacing = 10;
		setLayout(gridLayout);
		initInitialValues();
		String sDefaultExpType = preferences.getExperimentType() != null && ! preferences.getExperimentType().equals("") ? 
				preferences.getExperimentType() : Method.MS_TYPE_INFUSION_LABEL;

		if( this.bAddExperimentType ) {
			msExperimentLabel = MassSpecPreferencePage.createLabel(this, "Experiment Type");
			msExperimentCombo = MassSpecPreferencePage.createCombo(this, msExperimentLabel, preferences.getAllExperimentTypes(), sDefaultExpType, false, false);
			msExperimentCombo.addListener(SWT.Modify, getComboListener());
			MassSpecPreferencePage.setMandatoryLabel(msExperimentLabel);
		}
		
	/*	derivatizationLabel = MassSpecPreferencePage.createLabel(this, "Derivatization");
		derivatizationCombo = MassSpecPreferencePage.createCombo(this, derivatizationLabel, preferences.getAllDerivatizationTypes(), preferences.getDerivatizationType(), true, true);
		derivatizationCombo.addListener(SWT.Modify, getComboListener());*/

		instrumentLabel = MassSpecPreferencePage.createLabel(this, "Instrument");
		instrumentText = MassSpecPreferencePage.createText(this, instrumentLabel, preferences.getInstrument());
		instrumentText.addModifyListener(modifyListener);

		// create collisionTypeCombl
	/*	collisionTypeLabel = MassSpecPreferencePage.createLabel(this, "Collision Type");
		collisionTypeCombo = MassSpecPreferencePage.createCombo(this, collisionTypeLabel, preferences.getAllCollisionTypes(), preferences.getCollisionType(), true, true);
		collisionTypeCombo.addListener(SWT.Modify, getComboListener());*/

		//collisionEnergyLabel = MassSpecPreferencePage.createLabel(this, "Collision Energy");
	//	collisionEnergyText = MassSpecPreferencePage.createText(this, collisionEnergyLabel, Double.toString(preferences.getCollisionEnergy()));
	//	collisionEnergyText.addModifyListener(modifyListener);

		//createAdductCombo();
		/*adductLabel = MassSpecPreferencePage.createLabel(this, "Adduct");
		adductCombo = MassSpecPreferencePage.createCombo(this, adductLabel, preferences.getAllAdductTypes(), preferences.getAdductType(), true, true);
		adductCombo.addListener(SWT.Modify, getComboListener());*/

		//createReleaseTypeCombo();
		/*releaseTypeLabel = MassSpecPreferencePage.createLabel(this, "Release Type");
		releaseTypeCombo = MassSpecPreferencePage.createCombo(this, releaseTypeLabel, preferences.getAllReleaseTypes(), preferences.getReleaseType(), true, true);
		releaseTypeCombo.addListener(SWT.Modify, getComboListener());

		//createGlycanTypeCombo();
		glycanTypeLabel = MassSpecPreferencePage.createLabel(this, "Glycan Type");
		glycanTypeCombo = MassSpecPreferencePage.createCombo(this, glycanTypeLabel, preferences.getAllGlycanTypes(), preferences.getGlycanType(), true, true);
		glycanTypeCombo.addListener(SWT.Modify, getComboListener());*/
	}

	private Listener getComboListener() {
		Listener listener = new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				//if other was clicked and OK button was clicked
				if(event.text == "other")
				{
					//then need to update this list in preference xml file

				}
				if(isReadyToFinish())
					setPageComplete(true);
				else
					setPageComplete(false);
			}
		};
		return listener;
	}
	
	private ModifyListener modifyListener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent e) {
			if(isReadyToFinish())
				setPageComplete(true);
			else
				setPageComplete(false);
		}
	};

	private void initInitialValues() {
		try {			
			//derivatizationName = preferences != null && preferences.getDerivatizationType() != null  ? preferences.getDerivatizationType() : "";
			//collisionTypeName = preferences != null && preferences.getCollisionType() != null  ? preferences.getCollisionType() : "";
			//adductName = preferences != null && preferences.getAdductType() != null  ? preferences.getAdductType() : "";
			//releaseType = preferences != null && preferences.getReleaseType() != null  ? preferences.getReleaseType() : "";
			//glycanType = preferences != null && preferences.getGlycanType() != null  ? preferences.getGlycanType() : "";
			instrument = preferences != null && preferences.getInstrument() != null ? preferences.getInstrument() : ""; 
			//collisionEnergy = preferences != null && preferences.getCollisionEnergy() != null ? preferences.getCollisionEnergy() : null;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public boolean isReadyToFinish() {
		//need to check everything here
	/*	if(collisionEnergyText.getText().length() != 0) {
			try {
				double number = Double.parseDouble(collisionEnergyText.getText());
				if(number < 0) {
					setErrorMessage("Collision energy value cannot be less than 0.");
					setPageComplete(false);
					return false;
				}
			} catch(NumberFormatException e) {
				setErrorMessage("Invalid number format for collision energy.");
				setPageComplete(false);
				return false;
			}
		}*/
		setErrorMessage(null);
		return true;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setPageComplete(boolean isComplete) {
		PropertyChangeEvent e = new PropertyChangeEvent(this, "Page Complete", this.isComplete, isComplete);
		this.isComplete = isComplete;
		listener.propertyChange(e);
	}

	public boolean isPageCompete() {
		return this.isComplete;
	}

	public void updateSettings() {
		//need to save variables
		if( this.bAddExperimentType ) {
			msExperimentType = msExperimentCombo.getText();
		}
		//derivatizationName = derivatizationCombo.getText();
		instrument = instrumentText.getText();
	//	collisionTypeName = collisionTypeCombo.getText();
		/*if(collisionEnergyText.getText().length() != 0)
		{
			collisionEnergy = Double.parseDouble(collisionEnergyText.getText());
		}*/
		//adductName = adductCombo.getText();
		//releaseType = releaseTypeCombo.getText();
		//glycanType = glycanTypeCombo.getText();
	}

	public void updatePreferences() {
		if( this.bAddExperimentType ) {
			if( MassSpecPreferencePage.hasChanged(msExperimentType, preferences.getExperimentType(), preferences.getAllExperimentTypes()) ) {
				preferences.setExperimentType(msExperimentType);
			}
		}
	/*	if( MassSpecPreferencePage.hasChanged(derivatizationName, preferences.getDerivatizationType(), preferences.getAllDerivatizationTypes()) ) {
			preferences.setDerivatizationType(derivatizationName);
		}*/
		if( MassSpecPreferencePage.hasChanged(instrument, preferences.getInstrument(), null) ) {
			preferences.setInstrument(instrument);
		}
	/*	if( MassSpecPreferencePage.hasChanged(collisionTypeName, preferences.getCollisionType(), preferences.getAllCollisionTypes()) ) {
			preferences.setCollisionType(collisionTypeName);
		}
		if( collisionEnergy != null && MassSpecPreferencePage.hasChanged(collisionEnergy.toString(), preferences.getCollisionEnergy().toString(), null) ) {
			preferences.setCollisionEnergy(collisionEnergy);
		}
		if( MassSpecPreferencePage.hasChanged(adductName, preferences.getAdductType(), preferences.getAllAdductTypes()) ) {
			preferences.setAdductType(adductName);
		}
		if( MassSpecPreferencePage.hasChanged(releaseType, preferences.getReleaseType(), preferences.getAllReleaseTypes()) ) {
			preferences.setReleaseType(releaseType);
		}
		if( MassSpecPreferencePage.hasChanged(glycanType, preferences.getGlycanType(), preferences.getAllGlycanTypes()) ) {
			preferences.setGlycanType(glycanType);
		}*/
	}

	/*public String getDerivatizationName() {
		return derivatizationName;
	}

	public void setDerivatizationName(String derivatizationName) {
		this.derivatizationName = derivatizationName;
	}*/

	public String getInstrument() {
		return instrument;
	}

	public void setInstrument(String instrument) {
		this.instrument = instrument;
	}

	/*public String getCollisionTypeName() {
		return collisionTypeName;
	}

	public void setCollisionTypeName(String collusionTypeName) {
		this.collisionTypeName = collusionTypeName;
	}

	public Double getCollisionEnergy() {
		return collisionEnergy;
	}

	public void setCollisionEnergy(Double collusionEnergy) {
		this.collisionEnergy = collusionEnergy;
	}

	public String getAdductName() {
		return adductName;
	}

	public void setAdductName(String adductName) {
		this.adductName = adductName;
	}

	public String getReleaseType() {
		return releaseType;
	}

	public void setReleaseType(String releaseType) {
		this.releaseType = releaseType;
	}

	public String getGlycanType() {
		return glycanType;
	}

	public void setGlycanType(String glycanType) {
		this.glycanType = glycanType;
	}*/
}
