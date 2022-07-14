package org.grits.toolbox.entry.ms.preference.viewer;

import java.io.File;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.grits.toolbox.core.datamodel.UnsupportedVersionException;
import org.grits.toolbox.entry.ms.preference.MSConvertPreference;

public class MSConvertPreferencePage extends PreferencePage {
	private static final Logger logger = Logger.getLogger(MSConvertPreferencePage.class);
	
	MSConvertPreference preferences;

	private Text programLocation;

	private Combo fileTypeCombo;

	/**
	 * constructor for MSConvertPreferencePage. Loads the existing preferences, if any, from the preferences file
	 */
	public MSConvertPreferencePage() {
		try {
			preferences = MSConvertPreference.loadPreferences();
		} catch (UnsupportedVersionException e) {
			logger.error("Error loading MSConvert preferences: ", e);
			preferences = new MSConvertPreference();   // empty
			preferences.loadDefaultOptions();
		}
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(3, false);
		container.setLayout(gridLayout);
		
		Label locationLabel = new Label(container, SWT.NONE);
		locationLabel.setText("Program Location");
		
		programLocation = new Text(container, SWT.BORDER);
		programLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));	
		programLocation.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setValid(isReadyToFinish());
			}
		});
		if (preferences != null && preferences.getLocation() != null)
			programLocation.setText(preferences.getLocation());
		
		Button browseButton = new Button(container, SWT.PUSH);
		browseButton.setText("Browse");
		browseButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(container.getShell());
				String selectedDir = dialog.open();
				if (selectedDir != null) {
					// check to make sure msconvert is there
					File msConvert = new File (selectedDir + File.separator + "msconvert.exe");
					if (!msConvert.exists()) {
						MessageDialog.openWarning(getShell(), "Not Found", 
								"Could not locate msconvert.exe in the selected folder. Please make sure to select the correct folder");
						return;
					}
					programLocation.setText(selectedDir);
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		Label fileTypeLabel = new Label(container, SWT.NONE);
		fileTypeLabel.setText("Convert To");
		
		fileTypeCombo = new Combo(container, SWT.NONE);
		fileTypeCombo.setItems(new String[] {"mzXML", "mzML"});
		fileTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		fileTypeCombo.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				setValid(isReadyToFinish());
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		if (preferences != null && preferences.getFileType() != null) {
			if (preferences.getFileType().equalsIgnoreCase("mzml")) {
				fileTypeCombo.select(1);
				setValid(isReadyToFinish());
			}
			else {
				fileTypeCombo.select(0);
				setValid(isReadyToFinish());
			}
		} else {
			fileTypeCombo.select(0);
			setValid(isReadyToFinish());
		}
		
		new Label(container, SWT.NONE);
		
		// 3 empty lines
		for (int i=0; i <  9; i++)
			new Label(container, SWT.NONE);
		
		Link helpLink = new Link(container, SWT.NONE);
		helpLink.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 3, 3));
		helpLink.setText("Please check <A>http://proteowizard.sourceforge.net/tools.shtml/</A> for more information.");
		
		// Event handling when users click on links.
		helpLink.addSelectionListener(new SelectionAdapter()  {	 
		    @Override
		    public void widgetSelected(SelectionEvent e) {
		        Program.launch("http://proteowizard.sourceforge.net/tools.shtml");
		    }
		});
		return container;
	}

	@Override
	protected void performApply() {
		save();
	}
	
	/**
	 * checks whether the preference page is complete, i.e. the user entered all the necessary information
	 * 
	 * @return true if the user entered all the required information, false otherwise
	 */
	public boolean isReadyToFinish() {
		if (programLocation != null && programLocation.getText() != null && !programLocation.getText().isEmpty()) {
			if (fileTypeCombo != null && fileTypeCombo.getSelectionIndex() != -1) {
				setMessage(null);
				return true;
			} else {
				setMessage("Please select a file type to convert into");
			}
		}
		else {
			setMessage ("Please enter the location for the MSConvert tool");
		}
		return false;
	}
	
	@Override
	public boolean performOk() {
		boolean valid = isReadyToFinish();
		setValid(valid);
		if (valid)
			save();
		return valid;
	}

	/**
	 * save the user's (msconvert) preferences back to the file
	 */
	private void save() {
		preferences.setLocation(programLocation.getText());
		preferences.setFileType(fileTypeCombo.getItem(fileTypeCombo.getSelectionIndex()));
		preferences.saveValues();
	}
}
