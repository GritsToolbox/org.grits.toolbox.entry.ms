 package org.grits.toolbox.entry.ms.tablehelpers.gui;

import org.apache.log4j.Logger;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.display.control.table.dialog.GRITSTableColumnChooser;
import org.grits.toolbox.display.control.table.dialog.GRITSTableColumnChooserDialog;

public class MassSpecColumnChooserDialog extends GRITSTableColumnChooserDialog {
	private static final Logger logger = Logger.getLogger(MassSpecColumnChooserDialog.class);
	private int DEFAULT_BTN_ID = 2;
	
	public MassSpecColumnChooserDialog(Shell parentShell,
			String availableLabel, String selectedLabel,
			GRITSTableColumnChooser colChooser ) {
		super(parentShell, availableLabel, selectedLabel, colChooser);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, DEFAULT_BTN_ID, JFaceResources.getString("defaults"), false);
		super.createButtonsForButtonBar(parent);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if( buttonId == DEFAULT_BTN_ID ) {
			performDefaults();
		} else {
			super.buttonPressed(buttonId);
		}
	}
			
	/**
	 * Resets preferences to the default settings.
	 */
	protected void performDefaults() {
		((MassSpecTableColumnChooser) colChooser).setDefaultPreferences();		
	}
		
}
	