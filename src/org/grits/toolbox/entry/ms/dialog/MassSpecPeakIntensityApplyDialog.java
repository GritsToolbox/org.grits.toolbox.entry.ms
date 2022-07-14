package org.grits.toolbox.entry.ms.dialog;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
//import org.eclipse.swt.layout.GridData;
//import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.datamodel.ms.tablemodel.MassSpecTableDataObject;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecUISettings;
import org.grits.toolbox.entry.ms.views.tabbed.MassSpecMultiPageViewer;

/**
 * Simple pop-up dialog accessed from a Mass Spec GRITS table that allows the user to specify
 * the intensity of individual MS peaks if the user determines the value from file is incorrect.
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 * @see MassSpecPeakIntensityGrid
 * 
 */
public class MassSpecPeakIntensityApplyDialog extends MassSpecViewerDialog implements IPropertyChangeListener, IDynamicTableProcessor {
	private static final Logger logger = Logger.getLogger(MassSpecPeakIntensityApplyDialog.class);
	protected final Font boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT); 
	protected Text txtOutput;
	protected static final String PARAMS_OK = "Valid parameters";

	protected Label lblPeakTypes = null;
	protected Button cbShowStandards = null;
	protected Button cbShowExternals = null;
	protected Button cbShowAnnotated = null;
	
	protected Label lblMzRange = null;
	protected Label lblStartMz = null;
	protected Text txtStartMz = null;
	protected Label lblEndMz = null;
	protected Text txtEndMz = null;
	
	protected Button btnRefreshList = null;

	protected Button btnApplyFilter = null;
	protected Button btnModifyList = null;
	protected Button btnClearStandardQuant = null;

	protected MassSpecPeakIntensityGrid peakIntensityGrid = null;

	public final static String PROPERTY_WIN_CLOSED = "Window Closed";
	protected MassSpecTableDataObject msTableDataObject = null;
	protected boolean bHasStandards = false;
	protected boolean bHasExternal = false;
	
	public MassSpecPeakIntensityApplyDialog(Shell parentShell, 
			MassSpecMultiPageViewer contextViewer, MassSpecTableDataObject msTableDataObject) {
		super(parentShell, contextViewer);
		this.msTableDataObject = msTableDataObject;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(getFormTitle());
		Composite area = (Composite) super.createDialogArea(parent);
		
		//find the center of a main monitor
		Monitor primary = getShell().getDisplay().getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = getShell().getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		getShell().setLocation(x, y);
		
		area.setLayout(new GridLayout());
		
		Composite composite = new Composite(area, SWT.NONE);		
		GridLayout subLayout = new GridLayout(5, false);
		composite.setLayout(subLayout);
				
		addPeakTypeCheckboxes(composite);
		addMzRangeItems(composite);
		addBlankRow(composite);
		addRefreshButton(composite);
		addMassSpecPeakIntensityGrid(area);

		this.cbShowStandards.setEnabled(this.peakIntensityGrid.hasStandards());
		this.cbShowExternals.setEnabled(this.peakIntensityGrid.hasExternal());
		boolean bShowSomething = false;
		if( this.cbShowStandards.isEnabled() ) {
			cbShowStandards.setSelection(true);
			bShowSomething = true;
		}
		if( this.cbShowExternals.isEnabled() ) {
			cbShowExternals.setSelection(false);
			bShowSomething = true;
		}
		if( ! bShowSomething ) {
			cbShowAnnotated.setSelection(true);
		}
		txtStartMz.setText("800.0");
		txtEndMz.setText("1000.0");
		updateList();
		return area;
	}
	
	/**
	 * @return the currently visible MassSpecMultiPageViewer
	 */
	public MassSpecMultiPageViewer getCurrentViewer() {
		try {
			EPartService partService = getContextViewer().getPartService();
			MPart mPart = partService.getActivePart();
			if( mPart != null && mPart.equals(mPart.getParent().getSelectedElement())) {
				if( mPart.getObject() instanceof MassSpecMultiPageViewer ) {
					MassSpecMultiPageViewer viewer = (MassSpecMultiPageViewer) mPart.getObject();
					if( viewer.getEntry().getProperty() != null && viewer.getEntry().getProperty() instanceof MassSpecEntityProperty ) {
						return viewer;
					}
				}
			}	
		} catch( Exception e ) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * @return the Entry associated with the currently visible MassSpecMultiPageViewer
	 */
	public Entry getEntryForCurrentViewer() {
		MassSpecMultiPageViewer viewer = getCurrentViewer();
		if ( viewer == null ) {
			return null;
		}
		return viewer.getEntry();
	}


	/**
	 * @return the MassSpecProperty from the Entry associated with the current open MassSpecViewer
	 */ 
	public Property getEntryParentProperty() {
		try {
			Entry entry = getEntryForCurrentViewer();
			MassSpecEntityProperty msep = (MassSpecEntityProperty) entry.getProperty();
			MassSpecProperty pp = msep.getMassSpecParentProperty();
			return pp;
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
	}

	/**
	 * Returns the MassSpecUISettings object to be used to list which files are associated with the entry.
	 * @return the MassSpecUISettings for the current entry
	 */
	public MassSpecUISettings getSourceMassSpecEntrySettings() {
		MassSpecProperty msp = (MassSpecProperty) getEntryParentProperty();
		MassSpecUISettings entrySettings = msp.getMassSpecMetaData();
		return entrySettings;
	}

	/**
	 * Returns the MassSpecUISettings object to be used to list which files are associated with the entry.
	 * @return the MassSpecUISettings for the current entry
	 */
	public MassSpecUISettings getEntrySettingsForInternalStandardFiles() {
		MassSpecProperty msp = (MassSpecProperty) getEntryParentProperty();
		MassSpecUISettings entrySettings = msp.getMassSpecMetaData();
		return entrySettings;
	}
	
	protected MassSpecPeakIntensityGrid getNewMassSpecPeakIntensityGrid( Composite parent ) {
		return new MassSpecPeakIntensityGrid(parent, this);
	}

	protected void addBlankRow( Composite parent ) {		
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, true, 5, 1);
		Label lblGoDawgs = new Label(parent, SWT.NONE);
		lblGoDawgs.setLayoutData(gd1);
		lblGoDawgs.setText("    ");
	}
	
	/**
	 * Adds the checkbox components for peak type.
	 * 
	 * @param parent
	 */
	protected void addPeakTypeCheckboxes( Composite parent ) {		
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1);
		lblPeakTypes = new Label(parent, SWT.NONE);
		lblPeakTypes.setText("Select peak types to show:");
		lblPeakTypes.setLayoutData(gd1);
				
		gd1 = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
		Label lblGoDawgs = new Label(parent, SWT.NONE);
		lblGoDawgs.setLayoutData(gd1);
		lblGoDawgs.setText("    ");

		GridData gd2 = new GridData(SWT.LEFT, SWT.FILL, true, false, 4, 1); 
		cbShowStandards = new Button(parent, SWT.CHECK);
		cbShowStandards.setText("Internal Standards");
		cbShowStandards.setLayoutData(gd2);
		

		gd1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		lblGoDawgs = new Label(parent, SWT.NONE);
		lblGoDawgs.setLayoutData(gd1);
		lblGoDawgs.setText("    ");

		gd2 = new GridData(SWT.LEFT, SWT.FILL, true, false, 4, 1); 
		cbShowExternals = new Button(parent, SWT.CHECK);
		cbShowExternals.setText("External Quantification");
		cbShowExternals.setLayoutData(gd2);
		
		gd1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		lblGoDawgs = new Label(parent, SWT.NONE);
		lblGoDawgs.setLayoutData(gd1);
		lblGoDawgs.setText("    ");

		gd2 = new GridData(SWT.LEFT, SWT.FILL, true, false, 4, 1); 
		cbShowAnnotated = new Button(parent, SWT.CHECK);
		cbShowAnnotated.setText("Peaks from Annotation File");
		cbShowAnnotated.setLayoutData(gd2);		
	}
	
	/**
	 * Adds the components for user specified m/z range
	 * @param parent
	 */
	protected void addMzRangeItems( Composite parent ) {		
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1);		
		lblMzRange = new Label(parent, SWT.NONE);
		lblMzRange.setText("Specify m/z range to show:");
		lblMzRange.setLayoutData(gd1);
		
		gd1 = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
		Label lblGoDawgs = new Label(parent, SWT.NONE);
		lblGoDawgs.setLayoutData(gd1);
		lblGoDawgs.setText("    ");
		
		GridData gd2 = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
		lblStartMz = new Label(parent, SWT.NONE);
		lblStartMz.setLayoutData(gd2);
		lblStartMz.setText("Start: ");

		GridData gd3 = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
		txtStartMz = new Text(parent, SWT.NONE);
		txtStartMz.setLayoutData(gd3);

		GridData gd4 = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
		lblEndMz = new Label(parent, SWT.NONE);
		lblEndMz.setLayoutData(gd4);
		lblEndMz.setText("End: ");

		GridData gd5 = new GridData(SWT.LEFT, SWT.FILL, true, false, 1, 1);
		txtEndMz = new Text(parent, SWT.NONE);
		txtEndMz.setLayoutData(gd5);
	}

	/**
	 * Is called to update the MassSpecPeakIntensityGrid after the user changes values and clicks the update button
	 */
	protected void updateList() {
		peakIntensityGrid.setShowStandards(false);
		if( cbShowStandards.isEnabled() ) {
			peakIntensityGrid.setShowStandards(cbShowStandards.getSelection());
		}
		peakIntensityGrid.setShowExternal(false);
		if( cbShowExternals.isEnabled() ) {
			peakIntensityGrid.setShowExternal(cbShowExternals.getSelection());
		}
		peakIntensityGrid.setShowInternal(false);
		if( cbShowAnnotated.isEnabled() ) {
			peakIntensityGrid.setShowInternal(cbShowAnnotated.getSelection());
		}
		try {
			peakIntensityGrid.setStartMz(null);
			if( ! txtStartMz.getText().equals("") ){
				peakIntensityGrid.setStartMz(Double.parseDouble(txtStartMz.getText()));
			}
		} catch( NumberFormatException ex ) {
			logger.error(ex.getMessage(), ex);
		}
		try {
			peakIntensityGrid.setEndMz(null);
			if( ! txtEndMz.getText().equals("") ){
				peakIntensityGrid.setEndMz(Double.parseDouble(txtEndMz.getText()));
			}
		} catch( NumberFormatException ex ) {
			logger.error(ex.getMessage(), ex);
		}
		peakIntensityGrid.initializeGrid();		
	}
	
	/**
	 * Adds the Update button
	 * @param parent
	 */
	protected void addRefreshButton( Composite parent ) {
		GridData gd2 = new GridData(SWT.FILL, SWT.BOTTOM, true, true, 5, 2); 
		btnApplyFilter = new Button(parent, SWT.BUTTON1);
		btnApplyFilter.setText("Update Peak List");
		btnApplyFilter.setLayoutData(gd2);
		
		btnApplyFilter.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateList();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	/**
	 * Adds the MassSpecPeakIntensityGrid and initializes with default settings
	 * 
	 * @param parent
	 * 		the control to add the Select Standard Quant File Button
	 */
	protected void addMassSpecPeakIntensityGrid( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 50);
		MassSpecMultiPageViewer curView = getCurrentViewer();
		if (curView == null)
			return;
		Entry msEntry = curView.getEntry();
		if( msEntry == null ) {
			return;
		}
		Property msProp = msEntry.getProperty();
		if( msProp instanceof MassSpecEntityProperty ) {
			peakIntensityGrid = getNewMassSpecPeakIntensityGrid(parent);
			peakIntensityGrid.setMSTableDataObject(msTableDataObject);
			peakIntensityGrid.initializeGridHeaders();
			peakIntensityGrid.initializeGrid();
			peakIntensityGrid.setLayoutData(gd1);
			peakIntensityGrid.addListener(this);

		}
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if( buttonId == IDialogConstants.PROCEED_ID ) {
			boolean bChanged = this.peakIntensityGrid.applyPressed();
			if( bChanged ) {
				updateSettings();
				updateViewer();
				getButton(IDialogConstants.CANCEL_ID).setText(IDialogConstants.CLOSE_LABEL);
				getButton(IDialogConstants.PROCEED_ID).setEnabled(false);
			}
		} else {
			super.buttonPressed(buttonId);
		}
	}
		
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.PROCEED_ID,
				"Apply", true);
		getButton(IDialogConstants.PROCEED_ID).setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, true);
	}


	@Override
	public int open() 
	{
		Entry entry = getEntryForCurrentViewer();
		if( entry == null ) {
			return -1;
		}
		setMassSpecEntry(entry);
		super.create();

		getShell().open();
		getShell().layout();
		getShell().pack();
		getShell().setSize(900, 700);
		return getReturnCode();
	}	

	@Override
	protected boolean isResizable() {
		return true;
	}

	/**
	 * @return the desired text for the form's title.
	 */
	protected String getFormTitle() {
		return "Mass Spec Peak Intensity Correction";
	}

	protected String getStandardQuantificationLabelText() {
		return "MS Std Quantification";
	}

	/**
	 * Validates the input on the page. In this case, we ensure the entry is not null.
	 */
	public void validateInput(){
		txtOutput.setText(PARAMS_OK);
		if( getMassSpecEntry() == null ) {
			txtOutput.setText("Please select Mass Spec Results");
		}
	}

	@Override
	public boolean close() {
		if ( getListeners() != null ) {
			for( IPropertyChangeListener listener : getListeners() ) {
				listener.propertyChange(new PropertyChangeEvent(this, 
						MassSpecPeakIntensityApplyDialog.PROPERTY_WIN_CLOSED, Boolean.FALSE, Boolean.TRUE));
			}
		}
		return super.close();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 * 
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if( event.getSource() instanceof MassSpecPeakIntensityGrid ) {
			if( event.getProperty().equals( MassSpecPeakIntensityGrid.PROPERTY_LOCAL_CHANGE ) ) {
				getButton(IDialogConstants.PROCEED_ID).setEnabled(false);
				String sError = peakIntensityGrid.getErrorMessage();
				if( ! sError.equals("") ) {
					setErrorMessage(sError);
					return;
				}
				if( peakIntensityGrid.isChanged() ) {
					getButton(IDialogConstants.PROCEED_ID).setEnabled(true);					
				}
					
			}
		}
	}

	/**
	 * Updates the MS Settings for the Entry's mass spec property and then marshalls it to the workspace xml file
	 */
	protected void updateSettings() {
		MassSpecProperty property = (MassSpecProperty) getEntryParentProperty();
		// need to save the projectEntry to cause the data files for the MassSpecProperty to be updated
		try {
			Entry projectEntry = getEntryForCurrentViewer();

			String settingsFile = MassSpecProperty.getFullyQualifiedFolderName(projectEntry) + File.separator + property.getMSSettingsFile().getName();
			property.marshallSettingsFile(settingsFile, property.getMassSpecMetaData());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);			
		}
	}
	
	/**
	 * Determines the current viewer and refreshes the GRITS tables that were updated with the quantitation.
	 * For MassSpec data, this is only the scans tab.
	 */
	public void updateViewer() {
		try {
			MassSpecMultiPageViewer viewer = getCurrentViewer();
			List<String> sKeyVals = getColumnKeyLabels();
			viewer.reLoadScansTab(sKeyVals);
		} catch( Exception e ) {
			logger.error(e.getMessage(), e);			
		}
	}
	

	@Override
	public List<String> getColumnKeyLabels() {
		return null;
	}
	
}
