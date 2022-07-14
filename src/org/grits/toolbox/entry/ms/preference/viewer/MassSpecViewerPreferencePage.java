package org.grits.toolbox.entry.ms.preference.viewer;

import org.apache.log4j.Logger;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.grits.toolbox.datamodel.ms.preference.MassSpecViewerPreference;
import org.grits.toolbox.datamodel.ms.tablemodel.FillTypes;
import org.grits.toolbox.display.control.table.dialog.GRITSTableColumnChooser;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.grits.toolbox.entry.ms.views.tabbed.MassSpecMultiPageViewer;

/**
 * Preference page that allows the user to specify the Mass Spec Viewer preferences for
 * all MS levels and table types.<br>
 * The preference page utilizes the GRITS Table (NatTable) column chooser components to
 * present them to the user in a Preferences page. <br>
 * This connection is made through a "bridge" class.
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 * @see MassSpecViewerPreferencePage_NatBridge
 * @see GRITSTableColumnChooser
 */
public class MassSpecViewerPreferencePage extends PreferencePage implements IPropertyChangeListener {
	//log4J Logger
	private static final Logger logger = Logger.getLogger(MassSpecViewerPreferencePage.class);
	protected Composite parent;
	protected GRITSTableColumnChooser chooser = null;
	protected Point originalSize = null;
	protected boolean bNeedPaint = true;
	protected Label comboLabel = null;
	protected Combo comboMSlevel = null;
	protected Label comboTableLabel = null;
	protected Combo comboTablelevel = null;
	protected static String OVERVIEW = "Overview";
	protected static String MAX_DISPLAY = ">3";
	protected static int MAX_VAL = 5;

	protected Group natContainer = null;
	protected MassSpecViewerPreferencePage_NatBridge[][] natBridge = null;	
	public final static String[] MS_LEVELS = {OVERVIEW, "1", "2", "3", MAX_DISPLAY};	

	protected Group spectraContainer = null;
	protected Boolean[][] showSpectra = null;
	protected Button cbShowSpectra = null;

	protected Button cbAnnotatedPeaks = null;
	protected Boolean[][] showAnnotatedPeaks = null;

	protected Button cbAnnotatedPeakLabels = null;
	protected Boolean[][] showAnnotatedPeakLabels = null;

	protected Button cbPickedPeaks = null;
	protected Boolean[][] showPickedPeaks = null;

	protected Button cbPickedPeakLabels = null;
	protected Boolean[][] showPickedPeakLabels = null;
	
	protected int getTableNumber() {
		int iTableNum = this.comboTablelevel.getSelectionIndex();			 
		return iTableNum;
	}

	/**
	 * @return the labels to be displayed for the supported MS Levels 
	 */
	protected String[] getMSLevels() {
		return MS_LEVELS;
	}

	/**
	 * @return the supported table type for the current MS level and table type
	 */
	protected FillTypes getTableFillType() {
		FillTypes[] fillTypes = MassSpecMultiPageViewer.getPreferencePageFillTypes(getCurMSLevel());
		FillTypes fillType = fillTypes[getTableNumber()];
		return fillType;
	}

	/**
	 * @return the label to be displayed for the current MS level and table type
	 */
	protected String getTableFillLabel() {
		String[] labels = MassSpecMultiPageViewer.getPreferencePageLabels(getCurMSLevel());
		String label = labels[getTableNumber()];
		return label;
	}
	
	/**
	 * Adds the table type components to the specified container
	 * 
	 * @param container
	 * 		the container to add the table type components
	 */
	protected void initTableTypes(Composite container) {
		GridData gridData1 = GridDataFactory.fillDefaults().grab(true, false).create();
		gridData1.horizontalSpan=2;
		comboTableLabel = new Label(container, SWT.NONE);
		comboTableLabel.setText("Table Type:");
		comboTableLabel.setLayoutData(gridData1);

		GridData gridData2 = GridDataFactory.fillDefaults().grab(true, false).create();
		gridData2.horizontalSpan=2;
		comboTablelevel = new Combo(container, SWT.READ_ONLY);
		comboTablelevel.setLayoutData(gridData2);
		updateTableTypeCombo(1);
		addTableTypeComboSelectionListener();
	}

	/**
	 * Updates the list of supported table types for the specified MS level
	 * 
	 * @param _iMSLevel 
	 * 		the current MS level in the displayed Combo
	 */
	protected void updateTableTypeCombo(int _iMSLevel) {
		String[] tableTypes = MassSpecMultiPageViewer.getPreferencePageLabels(_iMSLevel);
		String defaultTable = tableTypes[0];
		comboTablelevel.setItems(tableTypes);
		comboTablelevel.setText(defaultTable);		
	}
	
	/**
	 * Adds the listeners on the table type Combo
	 */
	protected void addTableTypeComboSelectionListener() {
		comboTablelevel.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateColumnChooserElements(natContainer, false);				
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	/**
	 * Adds the MS level components to the specified container
	 * 
	 * @param container
	 * 		the container to add the MS level components
	 */
	protected void initMSLevel( Composite container ) {
		GridData gridData1 = GridDataFactory.fillDefaults().grab(true, false).create();
		gridData1.horizontalSpan = 2;		
		comboLabel = new Label(container, SWT.NONE);
		comboLabel.setText("MS Level:");
		comboLabel.setLayoutData(gridData1);

		GridData gridData2 = GridDataFactory.fillDefaults().grab(true, false).create();
		gridData2.horizontalSpan = 2;		
		comboMSlevel = new Combo(container, SWT.READ_ONLY);
		comboMSlevel.setItems(getMSLevels());
		comboMSlevel.setText(getMSLevels()[0]);
		comboMSlevel.setLayoutData(gridData2);
		addMSLevelComboSelectionListener();
	}
	
	/**
	 * Adds the listeners on the MS level Combo
	 */
	protected void addMSLevelComboSelectionListener() {
		comboMSlevel.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				int iMSLevel = getCurMSLevel();
				updateTableTypeCombo(iMSLevel);
				updateColumnChooserElements(natContainer, false);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub				
			}
		});
	}
	

	/**
	 * @return true if "Picked Peaks" check box is selected, false otherwise
	 */
	public boolean showPickedPeaks() {
		return cbPickedPeaks.getSelection();
	}

	/**
	 * @return true if "Picked Peak Labels" check box is selected, false otherwise
	 */
	public boolean showPickedPeakLabels() {
		return cbPickedPeakLabels.getSelection();
	}

	/**
	 * @return true if "Annotated Peak Labels" check box is selected, false otherwise
	 */
	public boolean showAnnotatedPeakLabels() {
		return cbAnnotatedPeakLabels.getSelection();
	}

	/**
	 * @return true if "Annotated Peaks" check box is selected, false otherwise
	 */
	public boolean showAnnotatedPeaks() {
		return cbAnnotatedPeaks.getSelection();
	}

	/**
	 * Creates a container to hold the MS Spectra setting options
	 * @param container
	 * 		parent container to which to add the Spectra Options panel
	 */
	protected void createSpectraContainer(Composite container) {
		spectraContainer = new Group(container, SWT.NONE);
		spectraContainer.setText("Spectrum Viewer Settings");
		GridData gridData1 = GridDataFactory.fillDefaults().grab(true, false).create();
		gridData1.horizontalSpan = 4;		
		spectraContainer.setLayoutData(gridData1);		
		GridLayout layout = new GridLayout(4, false);
		//		layout.marginWidth = 10;
		layout.horizontalSpacing = 25;
		layout.marginLeft = 10;
		layout.marginTop = 10;
		spectraContainer.setLayout( layout );
	}

	/**
	 * Creates a container to hold the GRITS Table settings
	 * @param container
	 * 		parent container to which to add the GRITS Table Options panel
	 */
	protected void createNatContainer(Composite container) {
		natContainer = new Group(container, SWT.NONE);
		natContainer.setText("");
		GridData gridData1 = GridDataFactory.fillDefaults().grab(true, false).create();
		gridData1.horizontalSpan = 4;		
		natContainer.setLayoutData(gridData1);		
		GridLayout layout = new GridLayout(4, false);
		//		layout.marginWidth = 10;
		layout.horizontalSpacing = 25;
		layout.marginLeft = 10;
		layout.marginTop = 10;
		natContainer.setLayout( layout );
	}
	
	/**
	 * Adds the MS Spectra options to a parent container. 
	 * @param container
	 * 		parent container to which to add the MS spectra options 
	 */
	protected void setMSElements(Composite container) {
		cbShowSpectra = new Button(container, SWT.CHECK);
		cbShowSpectra.setText("Raw Spectra");
		GridData gdRawSpectra = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 4, 1);	
		cbShowSpectra.setLayoutData(gdRawSpectra);
		cbShowSpectra.setSelection(true);
		cbShowSpectra.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				int iCurMS = getCurMSLevel() - 1;
				showSpectra[getTableNumber()][iCurMS] = cbShowSpectra.getSelection();				
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		showSpectra = new Boolean[getMaxNumTables()][MAX_VAL];
	}

	/**
	 * Adds the "Picked Peak" controls to a parent container
	 * @param container
	 * 		parent container to which to add the Picked Peak controls
	 */
	protected void setPickedPeaksElements(Composite container) {
		cbPickedPeaks = new Button(container, SWT.CHECK);
		cbPickedPeaks.setText("Picked Peaks");
		GridData gdPickedPeaks = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1);
		cbPickedPeaks.setLayoutData(gdPickedPeaks);
		showPickedPeaks = new Boolean[getMaxNumTables()][MAX_VAL];
		cbPickedPeaks.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int iCurMS = getCurMSLevel() - 1;
				showPickedPeaks[getTableNumber()][iCurMS] = cbPickedPeaks.getSelection();				
				if( ! cbPickedPeaks.getSelection() ) {
					cbPickedPeakLabels.setSelection(false);
				} 
				cbPickedPeakLabels.setEnabled(cbPickedPeaks.getSelection());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});

		cbPickedPeakLabels = new Button(container, SWT.CHECK);
		cbPickedPeakLabels.setText("Show labels");
		GridData gdPickedPeakLabels = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1);
		cbPickedPeakLabels.setLayoutData(gdPickedPeakLabels);
		cbPickedPeakLabels.setEnabled(false);
		cbPickedPeakLabels.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				int iCurMS = getCurMSLevel() - 1;
				showPickedPeakLabels[getTableNumber()][iCurMS] = cbPickedPeakLabels.getSelection();				
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		showPickedPeakLabels = new Boolean[getMaxNumTables()][MAX_VAL];
	}

	/**
	 * Adds the "Annotated Peak" controls to a parent container
	 * @param container
	 * 		parent container to which to add the Annotated Peak controls
	 */
	protected void setAnnotatedElements(Composite container) {
		cbAnnotatedPeaks = new Button(container, SWT.CHECK);
		cbAnnotatedPeaks.setText("Annotated Peaks");
		GridData gdAnnotatedPeaks = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1);
		cbAnnotatedPeaks.setLayoutData(gdAnnotatedPeaks);
		showAnnotatedPeaks = new Boolean[getMaxNumTables()][MAX_VAL];
		cbAnnotatedPeaks.addSelectionListener(new SelectionListener() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				int iCurMS = getCurMSLevel() - 1;
				showAnnotatedPeaks[getTableNumber()][iCurMS] = cbAnnotatedPeaks.getSelection();				
				if( ! cbAnnotatedPeaks.getSelection() ) {
					cbAnnotatedPeakLabels.setSelection(false);
				} 
				cbAnnotatedPeakLabels.setEnabled(cbAnnotatedPeaks.getSelection());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});

		cbAnnotatedPeakLabels = new Button(container, SWT.CHECK);
		cbAnnotatedPeakLabels.setText("Show labels");
		GridData gdAnnotatedPeakLabels = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1);
		cbAnnotatedPeakLabels.setLayoutData(gdAnnotatedPeakLabels);
		cbAnnotatedPeakLabels.setEnabled(false);
		cbAnnotatedPeakLabels.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				int iCurMS = getCurMSLevel() - 1;
				showAnnotatedPeakLabels[getTableNumber()][iCurMS] = cbAnnotatedPeakLabels.getSelection();				
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		showAnnotatedPeakLabels = new Boolean[getMaxNumTables()][MAX_VAL];
	}

	/**
	 * Top-level method that should be called to build the preference page.
	 * 
	 * @param container
	 * 		a parent container to which to add all of the components
	 */
	protected void initComponents( Composite container ) {
		initMSLevel(container);
		initTableTypes(container);
		initNatTable(container);
		createSpectraContainer(container);
		setMSElements(spectraContainer);
		setPickedPeaksElements(spectraContainer);
		setAnnotatedElements(spectraContainer);
	}

	/**
	 * Creates and initializes the GRITS Table component and adds to a parent container.
	 * @param container
	 * 		a parent container to which to the GRITS Table components
	 */
	protected void initNatTable(Composite container) {
		createNatContainer(container);
		natBridge = new MassSpecViewerPreferencePage_NatBridge[getMaxNumTables()][MAX_VAL];
		addColumnChooserElements(natContainer);
	}

	/**
	 * This method should be overridden by sub-classing MS preference pages in order
	 * to tell the page how many table types are allowed.<br>
	 * For example, for MS, there are only 2: "MS Scans" and "Peak List". 
	 * Other MS-based plugins might have more or different table types.
	 * @return the number of table types for the page
	 */
	protected int getMaxNumTables() {
		int iMaxTableTypes = MassSpecMultiPageViewer.getPreferencePageMaxNumPages();
		return iMaxTableTypes;
	}

	/* Automatically called.
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {

		this.parent = parent;
		// DBW:  I had difficulty with the best layout to use. I left this commented-out
		// code in we wanted to change in the future
		
//		FillLayout layout = new FillLayout();
//		container = new Composite(parent, SWT.NONE);
//		container.setLayout(layout); 	
		GridLayout layout = new GridLayout(4, false);
		//		layout.marginWidth = 10;
//		layout.horizontalSpacing = 25;
//		layout.marginLeft = 10;
//		layout.marginTop = 10;
		parent.setLayout( layout );

		initComponents(parent);	
		
		// Creates a paint listener that only runs the first time
		// the page is painted. If there's a better way to do this, then do it.
		parent.addPaintListener(new PaintListener() {

			@Override
			public void paintControl(PaintEvent e) {
				if ( bNeedPaint ) {
					updateColumnChooserElements(natContainer, false);		
//					addColumnChooserElements();
					bNeedPaint = false;
				}
			}
		});

		return parent;
	}

	/**
	 * Adds the GRITS Table column chooser elements to the parent container. <br>
	 * Because there are multiple MS levels and table types, there are multiple
	 * GRITS tables.<br> 
	 * If the current one isn't initialized, then it is initialized. 
	 * If the current column chooser isn't initialized, then it is initialized.
	 * @param container
	 * 		a parent container to which to add the column chooser components
	 */
	protected void addColumnChooserElements(Composite container) {
		int iTableNum = getTableNumber();
		int iCurMS = getCurMSLevel() - 1;
		if ( natBridge[iTableNum][iCurMS] == null ) {
			natBridge[iTableNum][iCurMS] = getPreferenceUItoNatBridge(false);
		}
		if( chooser == null) {
			chooser = new GRITSTableColumnChooser(
					parent.getShell(),
					false, true, natBridge[iTableNum][iCurMS].getNatTable());			
			chooser.getColumnChooserDialog().populateDialogArea(container);
			GridData layout = (GridData) container.getLayoutData();
			layout.horizontalSpan = 4;
			chooser.addListenersOnColumnChooserDialog();
		}
	}
	
	/**
	 * Updates the GRITS Table column chooser elements on the parent container. <br>
	 * Because there are multiple MS levels and table types, there are multiple
	 * GRITS tables.<br> 
	 * If the current one isn't initialized, then it is initialized. 
	 * If the current column chooser isn't initialized, then it is initialized.
	 * Also updates the MS Spectra options based on the preferences.
	 * @param container
	 * 		a parent container to which to add the column chooser components
	 */
	protected void updateColumnChooserElements(Composite container, boolean _bDefault) {
		int iTableNum = getTableNumber();
		int iCurMS = getCurMSLevel() - 1;
		if ( natBridge[iTableNum][iCurMS] == null || _bDefault) {
			natBridge[iTableNum][iCurMS] = getPreferenceUItoNatBridge(_bDefault);
		}
		if( chooser == null) {
			chooser = new GRITSTableColumnChooser(
					parent.getShell(),
					false, true, natBridge[iTableNum][iCurMS].getNatTable());			
			chooser.getColumnChooserDialog().populateDialogArea(container);
			chooser.addListenersOnColumnChooserDialog();			
		} else {			
			chooser.getHiddenColumnEntries().clear();
			chooser.getColumnChooserDialog().removeAllLeaves();
			chooser.reInit(	natBridge[iTableNum][iCurMS].getNatTable() );
		}
		chooser.populateDialog();
		GridData layout = (GridData) container.getLayoutData();
		layout.horizontalSpan = 4;
		if( showSpectra[iTableNum][iCurMS] == null ) {
			MassSpecViewerPreference pref = natBridge[iTableNum][iCurMS].getPreference();
			showSpectra[iTableNum][iCurMS] = new Boolean( pref.isShowRaw());
			showPickedPeaks[iTableNum][iCurMS] = new Boolean( pref.isShowPicked());
			showPickedPeakLabels[iTableNum][iCurMS] = new Boolean( pref.isShowPickedLabels());
			showAnnotatedPeaks[iTableNum][iCurMS] = new Boolean( pref.isShowAnnotated());
			showAnnotatedPeakLabels[iTableNum][iCurMS] = new Boolean( pref.isShowAnnotatedLabels());
		}
		cbShowSpectra.setSelection(showSpectra[iTableNum][iCurMS]);
		if ( this.comboMSlevel.getText().equals( OVERVIEW ) ) {
			cbPickedPeaks.setEnabled(false);
			cbPickedPeakLabels.setEnabled(false);
			cbAnnotatedPeaks.setEnabled(false);
			cbAnnotatedPeakLabels.setEnabled(false);			
		} else {
			cbPickedPeaks.setEnabled(true);
			cbAnnotatedPeaks.setEnabled(true);			
		}
		cbPickedPeaks.setSelection(showPickedPeaks[iTableNum][iCurMS]);
		if( cbPickedPeaks.getSelection() ) {
			cbPickedPeakLabels.setEnabled(true);
		} else {
			cbPickedPeakLabels.setEnabled(false);
		}
		cbPickedPeakLabels.setSelection(showPickedPeakLabels[iTableNum][iCurMS]);
		cbAnnotatedPeaks.setSelection(showAnnotatedPeaks[iTableNum][iCurMS]);
		if( cbAnnotatedPeaks.getSelection() ) {
			cbAnnotatedPeakLabels.setEnabled(true);
		} else {
			cbAnnotatedPeakLabels.setEnabled(false);			
		}
		cbAnnotatedPeakLabels.setSelection(showAnnotatedPeakLabels[iTableNum][iCurMS]);			
	}

	/**
	 * @return the current MS level as selected in the MS Level combo
	 */
	protected int getCurMSLevel() {
		int iMSLevel = 1;
		try {
			if ( this.comboMSlevel.getText().equals(MAX_DISPLAY) ) {
				iMSLevel = MAX_VAL; //  maximum
			}
			else if (! this.comboMSlevel.getText().equals( OVERVIEW )) {
				iMSLevel = Integer.parseInt(this.comboMSlevel.getText()) + 1;
			} 
		} catch( NumberFormatException ex ) {
			iMSLevel = 1; // should never happen
		}
		return iMSLevel;
	}

	@Override
	protected Point doComputeSize() {	
		if ( originalSize == null ) { // calculate size before data populated, then populate
			originalSize = super.doComputeSize();
		}
		return originalSize;
	}

	@Override
	public Point computeSize() {
		if ( originalSize == null ) { // calculate size before data populated, then populate
			originalSize = super.computeSize();
		}
		logger.debug("Dialog size: " + originalSize);
		return originalSize;
	}

	/**
	 * Creates a new Preference Page to GRITS Table "bridge" that allows the column chooser dialog
	 * and underlying components to be utilized in a GRITS preference page.
	 * 
	 * @param _bDefault true if default settings should be restored, false otherwise
	 * @return a new Preference Page to GRITS Table "bridge"
	 */
	protected MassSpecViewerPreferencePage_NatBridge getPreferenceUItoNatBridge(boolean _bDefault) {
		int iMSLevel = getCurMSLevel();

		MassSpecViewerPreferencePage_NatBridge natBridge = new MassSpecViewerPreferencePage_NatBridge( 
				new Composite(getShell(), SWT.NONE), 
				iMSLevel, getTableFillType() );
		natBridge.initializeComponents(_bDefault);
		return natBridge;
	}

	/**
	 * @return the Property of the underlying entry
	 */
	protected String getType() {
		return MassSpecProperty.TYPE;
	}

	@Override
	public void setContainer(IPreferencePageContainer container) {
		super.setContainer(container);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	//when apply button is clicked
	protected void performApply() {
		save();
	}

	@Override
	public boolean performOk() {
		//need to save
		save();
		return true;
	}

	protected void setPageComplete(boolean b) {
		//To do 
		if(b)
		{
			setErrorMessage(null);
		}
		setValid(b);
	}

	/**
	 * Saves all of the NatBridges in the array
	 */
	protected void save() {
		for( int i = 0; i < getMaxNumTables(); i++ ) {
			for( int j = 0; j < MAX_VAL; j++ ) {
				if( natBridge[i][j] != null ) {
					updateMassSpecViewerPreference(i, j);
					save(natBridge[i][j]);		
				}
			}
		}
	}
	
	/**
	 * Updates the individual settings for the NatBridge in the array
	 * with the specified indices
	 * 
	 * @param iMSLevel
	 * 		the MS Level for the desired NatBridge
	 * @param iTableType
	 * 		the table type for the desired NatBridge
	 */
	protected void updateMassSpecViewerPreference( int iMSLevel, int iTableType ) {
		MassSpecViewerPreference pref = natBridge[iMSLevel][iTableType].getPreference();
		pref.setShowRaw(showSpectra[iMSLevel][iTableType]);
		pref.setShowPicked(showPickedPeaks[iMSLevel][iTableType]);
		pref.setShowPickedLabels(showPickedPeakLabels[iMSLevel][iTableType]);
		pref.setShowAnnotated(showAnnotatedPeaks[iMSLevel][iTableType]);
		pref.setShowAnnotatedLabels(showAnnotatedPeakLabels[iMSLevel][iTableType]);
	}

	/**
	 * Updates the preferences for the specified NatBridge
	 * 
	 * @param natBridge table that needs updating
	 */
	protected void save(MassSpecViewerPreferencePage_NatBridge natBridge) {
		natBridge.updatePreferences();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		updateColumnChooserElements(natContainer, true);
	}
}
