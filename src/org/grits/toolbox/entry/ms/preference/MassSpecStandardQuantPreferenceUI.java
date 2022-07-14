package org.grits.toolbox.entry.ms.preference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridEditor;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.utilShare.ErrorUtils;
import org.grits.toolbox.entry.ms.dialog.MassSpecStandardQuantApplyDialog;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecStandardQuant;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecStandardQuantPeak;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecUISettings;
import org.grits.toolbox.entry.ms.views.tabbed.MassSpecMultiPageViewer;

/**
 * Interface for creating and/or modifying quantification by internal standards of Mass Spec data. Standard quantification essentially allows 
 * the user to specify particular peaks (m/z) in a scan to be used for relative quantification to other peaks. \
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 * @see IMSPreferenceWithStandardQuant
 *
 */
public class MassSpecStandardQuantPreferenceUI extends Composite {
	protected static final Logger logger = Logger.getLogger(MassSpecStandardQuantPreferenceUI.class);
	public static final String PAGE_COMPLETE_PROPERTY = "Page Complete";

	protected boolean isComplete = true;
	protected String errorMessage = null;
	protected IPropertyChangeListener listener = null;

	protected GridLayout gridLayout = null;

	protected Label lblSelectStandardQuant = null;
	protected Combo cmbSelectStandardQuant = null;
	protected String sCurrentStandardQuant = null;

	protected Button btnExportXML = null;
	protected Button btnImportXML = null;
	protected Button btnCreateNew = null;
	protected Button btnEditCurrent = null;
	protected Button btnDeleteCurrent = null;
	protected Button btnResetCurrent = null;
	protected Label lblSeparator = null;

	protected Label lblStandardQuantName = null;
	protected Text txtStandardQuantName = null;
	protected String sStandardQuantName = null;

	protected Label lblDescription = null;
	protected Text txtDescription = null;
	protected String sDescription = null;

	protected Label lblMassTolerance = null;
	//	protected Combo cmbMSLevel = null;
	protected String sMSLevel = null;
	protected Text txtMassTolerance = null;
	protected Double dMassTolerance = null;

	protected Label lblToleranceType = null;
	protected Button btnTolerancePPM = null;
	protected Button btnToleranceDalton = null;
	protected Boolean bIsPPM = false;

	protected Grid gridSpecialPeaksGrid = null;
	protected Point gridSpecialPeaksGridSize = null;
	protected List<GridItem> gridItemsSpecialPeaks = null;
	protected MassSpecStandardQuant currentStandardQuant = null;
	protected Button btnRemoveSelected = null;
	protected Button btnRemoveAllRows = null;

	protected Label lblAddStandardQuantFromFile = null;
	protected Text txtAddStandardQuantFromFile = null;
	protected Button btnAddStandardQuantFromFile = null;
	protected String sStandardQuantFilePath = null;

	protected Button btnSaveAsDefault = null;
	protected boolean bAddSaveAsDefault = false;

	protected boolean bIsDirty = false;
	protected boolean bIsEditingTable = false;
	protected GridEditor editor = null;

	protected IMSPreferenceWithStandardQuant localStandardQuant = null; // need to make this generic
	protected IMSPreferenceWithStandardQuant storedStandardQuant = null; // need to make this generic
	protected IMSPreferenceWithStandardQuant entryStandardQuant = null; // what is applied to the current entry
	protected Entry massSpecEntry = null; // need to add the temporary standard quantification
	protected MassSpecMultiPageViewer contextViewer = null;

	public MassSpecStandardQuantPreferenceUI(Composite parent, int style, 
			IPropertyChangeListener listener, boolean bAddSaveAsDefault ) {
		super(parent, style);
		this.listener = listener;
		this.bAddSaveAsDefault = bAddSaveAsDefault;
		initStoredStandardQuant();
	}

	/**
	 * Loads the Mass Spec preferences from the workspace XML.
	 * 
	 * @return
	 */
	public static IMSPreferenceWithStandardQuant loadWorkspacePreferences() {
		try {
			return MassSpecPreferenceLoader.getMassSpecPreferences();
		} catch (Exception ex) {
			logger.error("Error getting the mass spec preferences", ex);
		}
		return null;
	}

	/**
	 * Utility method that initializes the localStandardQuant and entryStandardQuant member variables based on the
	 * standard quantifications currently applied to the current open Mass Spec entry.
	 * @see MassSpecPreference
	 */
	public void initLocalStandardQuant() {
		localStandardQuant = new MassSpecPreference();
		MassSpecUISettings msSettings = getMassSpecUISettingsFromEntry(massSpecEntry);
		MassSpecStandardQuantPreferenceUI.initStandardQuantFromEntry(msSettings, localStandardQuant);
		entryStandardQuant = new MassSpecPreference();
		MassSpecStandardQuantPreferenceUI.initStandardQuantFromEntry(msSettings, entryStandardQuant);
	}	

	public static MassSpecUISettings getMassSpecUISettingsFromEntry( Entry entry ) {
		Property p = entry.getProperty();
		if( p != null ) {				
			return ((MassSpecEntityProperty) p).getMassSpecParentProperty().getMassSpecMetaData();
		}		
		return null;		
	}

	/**
	 * Populates the passed standard quantification object with the standard quantification data applied to the passed entry.
	 * 
	 * @param entry
	 *    a MassSpeEntry
	 * @param standardQuant
	 *    the standard quantification object to be populated
	 * @see IMSPreferenceWithStandardQuant
	 * @see MassSpecPreference
	 * @see MassSpecStandardQuant
	 */
	public static void initStandardQuantFromEntry( MassSpecUISettings msSettings, IMSPreferenceWithStandardQuant standardQuant ) {
		((MassSpecPreference)standardQuant).setStandardQuant( new ArrayList<MassSpecStandardQuant>() );
		List<String> localStandardQuantAdded = new ArrayList<String>();
		if( msSettings != null ) {
			List<MassSpecStandardQuant> l = msSettings.getStandardQuant();
			if( l != null && ! l.isEmpty() ) {
				for( MassSpecStandardQuant curStandardQuant : l ) {
					standardQuant.getStandardQuant().add( (MassSpecStandardQuant) curStandardQuant.clone() );
					localStandardQuantAdded.add(curStandardQuant.getStandardQuantName());
				}
			}
			((MassSpecPreference)standardQuant).setStandardQuantText( MassSpecPreference.createStandardQuantText(standardQuant.getStandardQuant()) );
		}		
	}

	/**
	 * Initializes the storedStandardQuant member variable by loading the standard quantification from preferences in the workspace XML
	 */
	public void initStoredStandardQuant() {
		storedStandardQuant = MassSpecStandardQuantPreferenceUI.loadWorkspacePreferences();		
	}

	/**
	 * @return the storedStandardQuant member variable
	 */
	public IMSPreferenceWithStandardQuant getStoredStandardQuant() {
		return storedStandardQuant;
	}

	/**
	 * Sets the massSpecEntry member variable
	 * @param massSpecEntry
	 */
	public void setEntry(Entry massSpecEntry) {
		this.massSpecEntry = massSpecEntry;
	}

	/**
	 * @return the massSpecEntry member variable
	 */
	public Entry getEntry() {
		return massSpecEntry;
	}

	/**
	 * Sets the localStandardQuant member variable
	 * @param localStandardQuant
	 */
	public void setLocalStandardQuant(IMSPreferenceWithStandardQuant localStandardQuant) {
		this.localStandardQuant = localStandardQuant;
	}

	/**
	 * @return the localStandardQuant member variable
	 */
	public IMSPreferenceWithStandardQuant getLocalStandardQuant() {
		return localStandardQuant;
	}

	/**
	 * Sets the entryStandardQuant member variable
	 * @param entryStandardQuant
	 */
	public void setEntryStandardQuant(IMSPreferenceWithStandardQuant entryStandardQuant) {
		this.entryStandardQuant = entryStandardQuant;
	}

	/**
	 * @return the entryStandardQuant member variable
	 */
	public IMSPreferenceWithStandardQuant getEntryStandardQuant() {
		return entryStandardQuant;
	}

	/**
	 * If this interface is opened when a MassSpec entry is being viewed, then the viewer is associated with the
	 * class to enable updating of the viewer if changes are made.
	 * 
	 * @return the MassSpecMultiPageViewer associated with the class
	 */
	public MassSpecMultiPageViewer getContextViewer() {
		return contextViewer;
	}

	/**
	 * Sets the MassSpecMultiPageViewer associated with the class <br>
	 * If this interface is opened when a MassSpec entry is being viewed, then the viewer is associated with the
	 * class to enable updating of the viewer if changes are made.
	 * @param contextViewer
	 */
	public void setContextViewer(MassSpecMultiPageViewer contextViewer) {
		this.contextViewer = contextViewer;
	}

	/**
	 * Called from ceateContents to initialize all of the individual components in this window. 
	 */
	public void initComponents() {	
		initGridLayout();
		setLayout(gridLayout);
		addSelectStandardQuantItem(this);

		addDeleteCurrent(this);
		addResetCurrent(this);
		//		addImportXML(this);
		//		addExportXML(this);
		addEditCurrent(this);
		addCreateNewItem(this);

		addSeparatorLine1(this);

		addStandardQuantNameItem(this);
		addDescriptionItem(this);
		addMassToleranceItem(this);
		addToleranceTypeItem(this);
		addPeakListItem(this);
		addRemoveRowComponents(this);
		addRemoveAllRowsComponents(this);
		addStandardQuantFileItem(this);
		addSaveSettingsItem(this, bAddSaveAsDefault);

		setEditEnabled(false); // disable bottom entries until something loaded
		setResetEnabled();
	}


	/**
	 * @return the Entry for the currently open viewer
	 * @see MassSpecMultiPageViewer
	 */
	protected Entry getEntryForCurrentViewer() {
		MassSpecMultiPageViewer viewer = getCurrentViewer();
		if ( viewer == null ) {
			return null;
		}
		return viewer.getEntry();
	}

	/**
	 * Uses the PartService to return the open MassSpecMultiPageViewer if its entry property is of type MassSpecEntityProperty
	 * @return the currently open viewer.
	 * @see MassSpecEntityProperty
	 */
	public MassSpecMultiPageViewer getCurrentViewer() {
		if( getContextViewer() == null ) {
			return null;
		}
		try {
			EPartService partService = getContextViewer().getPartService();
			MPart mPart = partService.getActivePart();
			if( mPart != null && mPart.getParent() != null && mPart.equals(mPart.getParent().getSelectedElement())) {
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
	 * Used before saving preferences to merge any locally added or modified standard quantification into the 
	 * storedPreferences member variable in order to be written to workspace XML.
	 */
	protected void mergePreferences() {
		if( getStoredStandardQuant() != null && getStoredStandardQuant().getStandardQuant() != null ) {
			for( int i = 0; i < getStoredStandardQuant().getStandardQuant().size(); i++ ) {
				MassSpecStandardQuant storedStdQuant = getStoredStandardQuant().getStandardQuant().get(i);
				boolean bFound = false;
				for( int j = 0; j < getLocalStandardQuant().getStandardQuant().size(); j++ ) {
					MassSpecStandardQuant localStdQuant = getLocalStandardQuant().getStandardQuant().get(j);
					if( storedStdQuant.getStandardQuantName().equals(localStdQuant.getStandardQuantName()) )  {
						bFound = true; 
						break;
					}
				}
				if( ! bFound ) {
					getLocalStandardQuant().getStandardQuant().add(storedStdQuant);
				}
			}
		}		
	}

	/**
	 * If opened from a MassSpec viewer (i.e. not from Preferences) then the user has the option of making local
	 * modifications to standard quantification that arent's saved in preferences via a checkbox. If checked, all changes
	 * will be written to workspace XML, otherwise they are only stored in localStandardQuant.
	 * 
	 * @return true if the btnSaveAsDefault checkbox is checked, false otherwise
	 */
	public boolean getSaveAsDefault() {
		return bAddSaveAsDefault && btnSaveAsDefault.getSelection();
	}

	/**
	 * Updates standard quantification lists and saves changes to workspace preferences, if "save as default" is selected (or called from Preferences)
	 */
	public void save() {
		//		if( getSaveAsDefault() ) {
		mergePreferences();
		getLocalStandardQuant().saveValues();
		initStoredStandardQuant();
		initLocalStandardQuant();
		//		}
	}

	/**
	 * Initializes the grid layout for the window.
	 */
	protected void initGridLayout() {
		gridLayout = new GridLayout(6, false);
	}

	/**
	 * When the selection of the cmbSelectStandardQuant combo control changes, processes the selection. If there is a local version
	 * of the standard quantification, then the information from that object is used. Otherwise, the information from the preference
	 * object is used.
	 * 
	 * @param _sStandardQuantName, the text from the cmbSelectStandardQuant combo control w/ available custom standard quantitation sets
	 */
	public void setSelectedItem( String _sStandardQuantName ) {
		for( int i = 0; i < cmbSelectStandardQuant.getItemCount(); i++ ) {
			String sText = cmbSelectStandardQuant.getItem(i);
			if( sText.trim().equals(_sStandardQuantName.trim()) ) {
				cmbSelectStandardQuant.select(i);
				if( ! cmbSelectStandardQuant.getText().trim().equals("") ) {
					String selStdQuantName = cmbSelectStandardQuant.getText().trim();
					MassSpecStandardQuant selStdQuant = null;
					MassSpecStandardQuant prefStdQuant = MassSpecStandardQuantApplyDialog.getCurrentStdQuant(selStdQuantName, storedStandardQuant);
					MassSpecStandardQuant localStdQuant = MassSpecStandardQuantApplyDialog.getCurrentStdQuant(selStdQuantName, localStandardQuant);
					if( localStdQuant != null ) {
						selStdQuant = localStdQuant;
					} else if( prefStdQuant != null ) {
						selStdQuant = prefStdQuant;
					} else {
						return;
					}
					processSelection(selStdQuant);
				}
				break;
			}
		}
	}

	/**
	 * Called from setSelectedItem(..) to update the interface after the user selects a standard quant entry in the cmbSelectStandardQuant combo.
	 * 
	 * @param desiredPreferences
	 */
	public void processSelection( MassSpecStandardQuant desiredPreferences ) {
		if( cancelIfDirty() ) {
			return;
		}				
		setEditEnabled(false);
		currentStandardQuant = null;
		if( desiredPreferences != null ) {
			btnEditCurrent.setEnabled(true);
			//			btnExportXML.setEnabled(true);
			btnDeleteCurrent.setEnabled(true);
			setCurrentStandardQuantValues(desiredPreferences);
			setResetEnabled();
		} else {
			setPageComplete(true);
		}
	}

	/**
	 * Adds the cmbSelectStandardQuant control and its label to the interface. 
	 * 
	 * @param parent
	 */
	protected void addSelectStandardQuantItem( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
		lblSelectStandardQuant = new Label(parent, SWT.NONE);
		lblSelectStandardQuant.setText("Current Quantification Set");
		lblSelectStandardQuant.setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		cmbSelectStandardQuant = new Combo(parent, SWT.READ_ONLY);
		cmbSelectStandardQuant.setLayoutData(gd2);
		sCurrentStandardQuant = null;
		initStoredStandardQuantList();
		cmbSelectStandardQuant.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if( ! cmbSelectStandardQuant.getText().trim().equals("") ) {
					String selStdQuantName = cmbSelectStandardQuant.getText().trim();
					MassSpecStandardQuant selStdQuant = null;
					MassSpecStandardQuant prefStdQuant = MassSpecStandardQuantApplyDialog.getCurrentStdQuant(selStdQuantName, storedStandardQuant);
					MassSpecStandardQuant localStdQuant = MassSpecStandardQuantApplyDialog.getCurrentStdQuant(selStdQuantName, localStandardQuant);
					if( localStdQuant != null ) {
						selStdQuant = localStdQuant;
					} else if( prefStdQuant != null ) {
						selStdQuant = prefStdQuant;
					} else {
						return;
					}
					processSelection(selStdQuant);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});
		cmbSelectStandardQuant.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				//				bIsEditingTable = false; // ?

			}

			@Override
			public void focusGained(FocusEvent e) {
				disposeEditor();	
				setDeleteSelectedEnabled();
			}
		});
		if( getContextViewer() != null ) {
			cmbSelectStandardQuant.addPaintListener(new StandardQuantPaintListener(this));
		}

	}

	/**
	 * If changes have been made to the currently open standard quantification, the user might still choose
	 * to select a different standard quantification. This method prompts the user for confirmation before discarding changes. 
	 * 
	 * @return true if the user wants to cancel discarding changes, false otherwise.
	 */
	protected boolean cancelIfDirty() {
		if( bIsDirty ) {
			boolean bContinue = MessageDialog.openConfirm(getShell(), "Values Changed", "The values in the current selection have changed. Discard?");
			if( ! bContinue ) {
				return true;
			}
		}		
		return false;
	}

	/**
	 * If local modifications have been made to a standard quantification that is stored in preferences, the user can restore the values from 
	 * stored preferences. This method prompts the user for confirmation before overwriting. 
	 * @return true if the user wants to restore, false otherwise
	 */
	protected boolean confirmRestore() {
		boolean bContinue = MessageDialog.openConfirm(getShell(), "Reset Peaks", "This will reload values in preferences. Local changes will be lost. Continue?");
		return bContinue;
	}


	/**
	 * This method allows generic overriding with different preference types as the parameter. Each method casts to the 
	 * specific preference type and returns the list of standard quantification.
	 * 
	 * @param preferences
	 *   A preference object associated with the entry. 
	 * @return the list of standard quantifications from the passed preferences.
	 */
	protected List<MassSpecStandardQuant> getPreferenceStandardQuant(Object preferences) {
		return ((MassSpecPreference) preferences).getStandardQuant();
	}

	/**
	 * This method allows generic overriding with different preference types as the parameter. Each method casts to the 
	 * specific preference type and sets the list of quantifications.
	 * 
	 * @param preferences
	 *   a preference object associated with the entry. 
	 * @param standardQuantList
	 *   a list of standard quantifications to be stored with the passed preference.
	 */
	protected void setPreferenceStandardQuant( Object preferences, List<MassSpecStandardQuant> standardQuantList ) {
		((MassSpecPreference) preferences).setStandardQuant(standardQuantList);
	}

	/**
	 * Initializes the list of available StandardQuant for the selection ComboBox
	 */
	public void initStoredStandardQuantList() {
		String sToSelect = "";
		if( cmbSelectStandardQuant.getItems().length != 0 ) {
			cmbSelectStandardQuant.removeAll();
		}
		cmbSelectStandardQuant.add("");
		// first add the stored preferences
		if( getStoredStandardQuant() != null && getStoredStandardQuant().getStandardQuant() != null ) {
			for( int i = 0; i < getStoredStandardQuant().getStandardQuant().size(); i++ ) {
				MassSpecStandardQuant stdQuant = getStoredStandardQuant().getStandardQuant().get(i);
				cmbSelectStandardQuant.add(stdQuant.getStandardQuantName());
				if( currentStandardQuant != null && stdQuant.getStandardQuantName().equals(currentStandardQuant.getStandardQuantName()) ) {
					int iItemCount = cmbSelectStandardQuant.getItemCount();
					sToSelect = cmbSelectStandardQuant.getItem(iItemCount-1);
				}
			}
		}
		if( getContextViewer() != null && getLocalStandardQuant() != null && getPreferenceStandardQuant(getLocalStandardQuant()) != null ) {
			for( int i = 0; i < getPreferenceStandardQuant(getLocalStandardQuant()).size(); i++ ) {
				MassSpecStandardQuant prefStdQuant = getPreferenceStandardQuant(getLocalStandardQuant()).get(i);
				MassSpecStandardQuantApplyDialog.setComboEntryForTempStdQuant(prefStdQuant, cmbSelectStandardQuant, 
						entryStandardQuant.getStandardQuant(),
						storedStandardQuant.getStandardQuant());
				if( currentStandardQuant != null && prefStdQuant.getStandardQuantName().equals(currentStandardQuant.getStandardQuantName()) ) {
					int iItemCount = cmbSelectStandardQuant.getItemCount();
					sToSelect = cmbSelectStandardQuant.getItem(iItemCount-1);
				}
			}
		}
		int iPrevSelInx = cmbSelectStandardQuant.indexOf(sToSelect);
		cmbSelectStandardQuant.select(iPrevSelInx);		
	}

	protected void addDeleteCurrent( Composite parent ) {
		GridData gd3 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		btnDeleteCurrent = new Button(parent, SWT.NONE);
		btnDeleteCurrent.setText("Delete Selected");
		btnDeleteCurrent.setLayoutData(gd3);				

		btnDeleteCurrent.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseDown(MouseEvent e) {
				boolean bVal = MessageDialog.openConfirm(getShell(), "Delete Selected?", "Delete selected. Are you sure?");
				if( bVal ) {
					int iSelInx = cmbSelectStandardQuant.getSelectionIndex();
					MassSpecStandardQuant localStdQuant = MassSpecStandardQuantApplyDialog.getCurrentStdQuant(cmbSelectStandardQuant.getItem(iSelInx), localStandardQuant);

					getPreferenceStandardQuant(getLocalStandardQuant()).remove(localStdQuant);
					cmbSelectStandardQuant.remove(iSelInx);
					clearValues();
					setEditEnabled(false);
					currentStandardQuant = null;
					setPageComplete(true);
					setResetEnabled();
					setRemoveAllRowsEnabled();
				}
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});
		btnDeleteCurrent.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {

			}

			@Override
			public void focusGained(FocusEvent e) {
				disposeEditor();
				setDeleteSelectedEnabled();
			}
		});

	}

	protected void addResetCurrent( Composite parent ) {
		GridData gd3 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		btnResetCurrent = new Button(parent, SWT.NONE);
		btnResetCurrent.setText("Restore Preferences");
		btnResetCurrent.setLayoutData(gd3);				

		btnResetCurrent.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseDown(MouseEvent e) {
				boolean bVal = confirmRestore();
				if( bVal ) {
					int iSelInx = cmbSelectStandardQuant.getSelectionIndex();
					MassSpecStandardQuant prefStandardQuant = MassSpecStandardQuantApplyDialog.getCurrentStdQuant(cmbSelectStandardQuant.getItem(iSelInx), getStoredStandardQuant());
					MassSpecStandardQuant localStandardQuant = MassSpecStandardQuantApplyDialog.getCurrentStdQuant(cmbSelectStandardQuant.getItem(iSelInx), getLocalStandardQuant());
					localStandardQuant.setStandardQuantPeaks(prefStandardQuant.getStandardQuantPeaks()); // overwrite the peaks but not the text yet
					if( ! cmbSelectStandardQuant.getText().trim().equals("") ) {
						String selStandardQuantName = cmbSelectStandardQuant.getText().trim();
						MassSpecStandardQuant storedStandardQuant = MassSpecStandardQuantApplyDialog.getCurrentStdQuant(selStandardQuantName, getStoredStandardQuant());
						processSelection(storedStandardQuant);
					}
					initStoredStandardQuantList();
					setRemoveAllRowsEnabled();

					setPageComplete(true);					
				}
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});
		btnDeleteCurrent.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {

			}

			@Override
			public void focusGained(FocusEvent e) {
				disposeEditor();
				setDeleteSelectedEnabled();
			}
		});

	}
	/*
	protected void addExportXML( Composite parent ) {
		GridData gd3 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		btnExportXML = new Button(parent, SWT.NONE);
		btnExportXML.setText("    Export    ");
		btnExportXML.setLayoutData(gd3);				

		btnExportXML.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseDown(MouseEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.SAVE);
				fd.setText("Please select file for export");
				fd.setFilterNames( new String[] { MassSpecStandardQuantFile.FILE_EXTENSION_XML_NAME, "All files"});
				fd.setFilterExtensions( new String[] {"*" + MassSpecStandardQuantFile.FILE_EXTENSION_XML, "*.*"});
				String sPath = fd.open();
				if( sPath != null ) {
					if( ! sPath.endsWith(MassSpecStandardQuantFile.FILE_EXTENSION_XML) ) {
						sPath += MassSpecStandardQuantFile.FILE_EXTENSION_XML;
					}
					writeXMLFile (sPath);
				}
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});
		btnExportXML.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {

			}

			@Override
			public void focusGained(FocusEvent e) {
				disposeEditor();
				setDeleteSelectedEnabled();
			}
		});

	}
	 */
	/*
	protected void addImportXML( Composite parent ) {
		GridData gd3 = new GridData(SWT.FILL, SWT.FILL, false, false,1, 1);
		btnImportXML = new Button(parent, SWT.NONE);
		btnImportXML.setText("    Import    ");
		btnImportXML.setLayoutData(gd3);				

		btnImportXML.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseDown(MouseEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
				fd.setText("Please select file for import");
				fd.setFilterNames( new String[] { MassSpecStandardQuantFile.FILE_EXTENSION_XML_NAME, "All files"});
				fd.setFilterExtensions( new String[] {"*" + MassSpecStandardQuantFile.FILE_EXTENSION_XML, "*.*"});
				String sPath = fd.open();
				if( sPath != null ) {
					if( ! sPath.endsWith(MassSpecStandardQuantFile.FILE_EXTENSION_XML) ) {
						sPath += MassSpecStandardQuantFile.FILE_EXTENSION_XML;
					}
					readXMLFile (sPath);
					if( currentStandardQuant != null ) {
						cmbSelectStandardQuant.add(currentStandardQuant.getStandardQuantName());
						cmbSelectStandardQuant.select(cmbSelectStandardQuant.getItemCount() - 1);
						processSelection(currentStandardQuant);
					} else {
						showInvalidImportFileMessage();
					}
				}
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});
		btnImportXML.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {

			}

			@Override
			public void focusGained(FocusEvent e) {
				disposeEditor();
				setDeleteSelectedEnabled();
			}
		});

	}
	 */
	/**
	 * Opens an error message dialog if an invalid file is specified for import.
	 */
	protected void showInvalidImportFileMessage() {
		ErrorUtils.createErrorMessageBox(getShell(), "Not a valid Mass Spec Custom StandardQuant XML file.");
	}

	/**
	 * Writes the selected standard quantitation to a file given
	 * 
	 * @param sPath
	 *
	protected void writeXMLFile(String sPath) {
		MassSpecStandardQuantFile.writeXMLFile(currentStandardQuant, sPath);
	}
	 */
	protected void addEditCurrent( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		btnEditCurrent = new Button(parent, SWT.NONE);
		btnEditCurrent.setText("Edit Selected");
		btnEditCurrent.setLayoutData(gd1);					
		btnEditCurrent.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseDown(MouseEvent e) {
				setEditEnabled(true);
				//				setCurrentStandardQuantValues(cmbSelectStandardQuant.getText());
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});
		btnEditCurrent.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				//				bIsEditingTable = false; // ?

			}

			@Override
			public void focusGained(FocusEvent e) {
				disposeEditor();
				setDeleteSelectedEnabled();
			}
		});
	}

	protected MassSpecStandardQuant getNewMassSpecStandardQuant() {
		return new MassSpecStandardQuant();
	}

	protected void addCreateNewItem( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		btnCreateNew = new Button(parent, SWT.NONE);
		btnCreateNew.setText("Create New");
		btnCreateNew.setLayoutData(gd1);					
		btnCreateNew.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseDown(MouseEvent e) {
				if( cancelIfDirty() ) {
					return;
				}				
				setEditEnabled(true);
				//				btnExportXML.setEnabled(false);
				btnEditCurrent.setEnabled(false);
				clearValues();	
				currentStandardQuant = getNewMassSpecStandardQuant();			
				setResetEnabled();
				setRemoveAllRowsEnabled();
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});
		btnCreateNew.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				//				bIsEditingTable = false; // ?

			}

			@Override
			public void focusGained(FocusEvent e) {
				disposeEditor();
				setDeleteSelectedEnabled();
			}
		});

	}


	protected void addSeparatorLine1( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, false, 6, 1);
		lblSeparator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);	
		lblSeparator.setLayoutData(gd1);
	}

	protected void setCurrentStandardQuantValues(MassSpecStandardQuant selStdQuant) {
		if( selStdQuant == null ) {
			return;
		}
		currentStandardQuant = selStdQuant;
		txtStandardQuantName.setText(selStdQuant.getStandardQuantName());
		txtDescription.setText(selStdQuant.getDescription());
		txtMassTolerance.setText(selStdQuant.getMassTolerance().toString());
		if( selStdQuant.getIsPPM() ) {
			btnTolerancePPM.setSelection(true);
		} else {
			btnToleranceDalton.setSelection(true);
		}
		initPeakListValues(selStdQuant, -1, -1);
		setIsDirty(false);
	}

	public void setEditEnabled( boolean _bVal ) {
		btnDeleteCurrent.setEnabled(_bVal);
		//		btnExportXML.setEnabled(_bVal);
		btnEditCurrent.setEnabled(_bVal);
		lblStandardQuantName.setEnabled(_bVal);
		txtStandardQuantName.setEnabled(_bVal);
		lblDescription.setEnabled(_bVal);
		txtDescription.setEnabled(_bVal);
		lblMassTolerance.setEnabled(_bVal);
		lblToleranceType.setEnabled(_bVal);
		btnTolerancePPM.setEnabled(_bVal);
		btnToleranceDalton.setEnabled(_bVal);
		//		cmbMSLevel.setEnabled(_bVal);
		txtMassTolerance.setEnabled(_bVal);
		gridSpecialPeaksGrid.setEnabled(_bVal);
		//		btnDeleteSelected.setEnabled(_bVal);
		lblAddStandardQuantFromFile.setEnabled(_bVal);
//		txtAddStandardQuantFromFile.setEnabled(_bVal);
		btnAddStandardQuantFromFile.setEnabled(_bVal);
		gridSpecialPeaksGrid.redraw();
	}

	public void setResetEnabled() {
		btnResetCurrent.setEnabled(cmbSelectStandardQuant.getText().trim().endsWith(MassSpecStandardQuantApplyDialog.LOCAL_STANDARDQUANT_DIFFERS));
	}

	public void clearValues() {
		txtStandardQuantName.setText("");
		txtDescription.setText("");
		//		cmbMSLevel.select(0);
		txtMassTolerance.setText("");
		gridSpecialPeaksGrid.clearAll(true);
		txtAddStandardQuantFromFile.setText("<Please Browse for your file>");
		cmbSelectStandardQuant.select(0);
		btnRemoveSelected.setEnabled(false);
		btnRemoveAllRows.setEnabled(false);
		setIsDirty(false);
	}

	protected void addStandardQuantNameItem( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		lblStandardQuantName = new Label(parent, SWT.NONE);
		lblStandardQuantName.setText("Name of Quantification Set");
		lblStandardQuantName.setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1);
		txtStandardQuantName = new Text(parent, SWT.BORDER);
		txtStandardQuantName.setLayoutData(gd2);
		txtStandardQuantName.addKeyListener(new KeyListener() {

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void keyPressed(KeyEvent e) {
				setIsDirty(true);			
				if (isReadyToFinish()) {
					setPageComplete(true);
				} else {
					setPageComplete(false);
				}
			}
		});
		txtStandardQuantName.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				//				bIsEditingTable = false; // ?

			}

			@Override
			public void focusGained(FocusEvent e) {
				disposeEditor();
				setDeleteSelectedEnabled();
			}
		});

	}

	protected void addDescriptionItem( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 5);
		lblDescription = new Label(parent, SWT.NONE);
		lblDescription.setText("Description");
		lblDescription.setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, true, 5, 5);
		txtDescription = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL );
		txtDescription.setText("");
		txtDescription.setLayoutData(gd2);
		txtDescription.addKeyListener(new KeyListener() {

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void keyPressed(KeyEvent e) {
				setIsDirty(true);
				if (isReadyToFinish()) {
					setPageComplete(true);
				} else {
					setPageComplete(false);
				}
			}
		});
		txtDescription.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				//				bIsEditingTable = false; // ?

			}

			@Override
			public void focusGained(FocusEvent e) {
				disposeEditor();
				setDeleteSelectedEnabled();
			}
		});
	}

	public void setIsDirty(boolean bIsDirty) {
		this.bIsDirty = bIsDirty;
	}


	protected void addMassToleranceItem( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		lblMassTolerance = new Label(parent, SWT.NONE);
		lblMassTolerance.setText("Mass Tolerance to Match Peak");
		lblMassTolerance.setLayoutData(gd1);

		GridData gd3 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		txtMassTolerance = new Text(parent, SWT.BORDER);
		txtMassTolerance.setLayoutData(gd3);		
		txtMassTolerance.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setIsDirty(true);
				if (isReadyToFinish()) {
					setPageComplete(true);
				} else {
					setPageComplete(false);
				}
			}
		});
		txtMassTolerance.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				//				bIsEditingTable = false; // ?

			}

			@Override
			public void focusGained(FocusEvent e) {
				disposeEditor();
				setDeleteSelectedEnabled();
			}
		});
		GridData gd4= new GridData(SWT.FILL, SWT.FILL, false, false, 4, 1);
		Label dummy4 = new Label(parent, SWT.NONE);		
		dummy4.setLayoutData(gd4);
	}

	protected void addToleranceTypeItem( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		lblToleranceType = new Label(parent, SWT.NONE);
		lblToleranceType.setText("Tolerance Type");
		lblToleranceType.setLayoutData(gd1);

		Composite cmp = new Composite(parent, SWT.NONE);
		GridData cmpGD = new GridData(GridData.FILL_HORIZONTAL);
		cmpGD.horizontalSpan = 4;
		cmp.setLayoutData(cmpGD);
		cmp.setLayout(new RowLayout());

		btnTolerancePPM = new Button(cmp, SWT.RADIO);
		btnTolerancePPM.setText("PPM");
		btnTolerancePPM.setSelection(true);

		btnToleranceDalton = new Button(cmp, SWT.RADIO);
		btnToleranceDalton.setText("Dalton");

		GridData gd4= new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		Label dummy4 = new Label(parent, SWT.NONE);		
		dummy4.setLayoutData(gd4);
		btnToleranceDalton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				setIsDirty(true);
				if (isReadyToFinish()) {
					setPageComplete(true);
				} else {
					setPageComplete(false);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});
		btnToleranceDalton.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				//				bIsEditingTable = false; // ?

			}

			@Override
			public void focusGained(FocusEvent e) {
				disposeEditor();
				setDeleteSelectedEnabled();
			}
		});

		btnTolerancePPM.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				setIsDirty(true);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});
		btnTolerancePPM.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				//				bIsEditingTable = false; // ?

			}

			@Override
			public void focusGained(FocusEvent e) {
				disposeEditor();
				setDeleteSelectedEnabled();
			}
		});
	}

	protected int getColNumberForGridColumn( GridColumn col ) {
		for( int i = 0; i < getHeaderColumns().length; i++ ) {
			if( col.getText().equals( getHeaderColumns()[i] ) ) {
				return i;
			}
		}
		return -1;
	}

	protected int getRowNumberForItem( GridItem item ) {
		for( int i = 0; i < gridSpecialPeaksGrid.getItems().length; i++ ) {
			GridItem gi = gridSpecialPeaksGrid.getItem(i);
			if( gi.equals(item) ) {
				return i;
			}
		}
		return -1;

	}

	protected void addPeakListItem( Composite parent ) {
		gridSpecialPeaksGrid = new Grid(this, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		gridSpecialPeaksGrid.setHeaderVisible(true);
		GridData gd_gridSpecialPeaksGrid = new GridData(SWT.FILL, SWT.FILL, true, true, 6, 10);
		gd_gridSpecialPeaksGrid.heightHint = 150;
		gridSpecialPeaksGrid.setLayoutData(gd_gridSpecialPeaksGrid);
		initGridColumns();
		initPeakListValues(null, -1, -1);

		editor = new GridEditor(gridSpecialPeaksGrid);
		gridSpecialPeaksGrid.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				super.mouseDown(e);
				disposeEditor();
				setDeleteSelectedEnabled();
				setRemoveAllRowsEnabled();
				Point pt = new Point( e.x, e.y);

				GridColumn col = gridSpecialPeaksGrid.getColumn(pt);
				GridItem item = gridSpecialPeaksGrid.getItem( pt);
				if( item == null && col != null ) { // clicked the column header. Is there a better way to detect this?
					int iColNum = getColNumberForGridColumn(col);
					int iSortOrder = SWT.UP;
					if( col.getSort() == SWT.UP ) {
						iSortOrder = SWT.DOWN;
					} 
					updateCurrentStandardQuantPeakData();
					initPeakListValues(currentStandardQuant, iColNum, iSortOrder);
				}
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				setIsDirty(true);
				bIsEditingTable= true;
				Control oldEditor = editor.getEditor();
				if ( oldEditor != null)
					oldEditor.dispose();

				Point pt = new Point( e.x, e.y);

				final GridItem item = gridSpecialPeaksGrid.getItem( pt);
				final Point cell = gridSpecialPeaksGrid.getCell( pt);
				if ( item == null || cell == null)
					return;
				gridSpecialPeaksGrid.deselect(cell.y);
				// The control that will be the editor must be a child of the Table
				final Text newEditor = new Text( gridSpecialPeaksGrid, SWT.BORDER | SWT.SINGLE);
				String curText = item.getText(cell.x);
				newEditor.setText(curText);
				editor.setEditor( newEditor, item, cell.x);
				editor.grabHorizontal = true;
				editor.grabVertical = true;
				newEditor.addKeyListener(new KeyListener() {

					@Override
					public void keyReleased(KeyEvent e) {
						item.setText(cell.x, newEditor.getText());	

						//						listEntries.put( item.getText(0), newEditor.getText() );
						if (isValidCellValue(cell.x, newEditor.getText()) && isReadyToFinish()) {
							setPageComplete(true);
						} else {
							setPageComplete(false);
						}
					}

					@Override
					public void keyPressed(KeyEvent e) {
						// TODO Auto-generated method stub						
					}
				});
				newEditor.forceFocus();
				newEditor.setSelection(newEditor.getText().length());
				newEditor.selectAll();
				//                newEditor.setSelection(0, curText.length());

				int iRowNum = getRowNumberForItem(item);
				if( iRowNum == (gridSpecialPeaksGrid.getItemCount() -1) ) {
					addBlankRows(1);					
				}
			}
		});		
	}

	protected boolean isValidCellValue( int x, String sValue ) {
		if( x == 0 ) { // must be valid double
			double dVal = -1;
			try {
				dVal = Double.parseDouble(sValue);
			} catch( NumberFormatException ex ) {
				;
			}
			if( dVal < 0 ) {
				setErrorMessage("m/z value must be a valid, positive double value");
				return false;
			}
		} else if( x == 1 ) { // must be valid integer
			int iVal = -1;
			try {
				iVal = Integer.parseInt(sValue);
			} catch( NumberFormatException ex ) {
				;
			}
			if( iVal < 1 ) {
				setErrorMessage("MS level value must be a valid, positive integer > 1");
				return false;
			}
		}
		return true;

	}
	protected void disposeEditor() {
		Control oldEditor = editor.getEditor();
		if ( oldEditor != null) {
			oldEditor.dispose();
			editor.setEditor(null);
			bIsEditingTable = false;
		}
	}

	protected void setDeleteSelectedEnabled() {
		if( gridSpecialPeaksGrid != null && gridSpecialPeaksGrid.getSelectionIndex() != -1 ) {
			GridItem item = gridSpecialPeaksGrid.getItem( gridSpecialPeaksGrid.getSelectionIndex() );
			if( item != null ) {
				int iItemCnt = getHeaderColumns().length;
				for( int i = 0; i < iItemCnt; i++ ) {
					if( ! item.getText(i).equals("") ) {
						btnRemoveSelected.setEnabled(true);
						return;					
					}
				}
			}
		}
		btnRemoveSelected.setEnabled(false);
	}

	protected void setRemoveAllRowsEnabled() {
		if( gridSpecialPeaksGrid != null && gridSpecialPeaksGrid.getItems().length > 0 ) {
			for( int i = 0; i < gridSpecialPeaksGrid.getItems().length; i++ ) {
				GridItem item = gridSpecialPeaksGrid.getItem(i);
				if( ! item.getText(0).trim().equals("") ) {
					btnRemoveAllRows.setEnabled(true);
					return;
				}
			}
		}
		btnRemoveAllRows.setEnabled(false);		
	}
	
	protected void addRemoveRowComponents( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		btnRemoveSelected = new Button(parent, SWT.NONE);
		btnRemoveSelected.setText("Remove Row");
		btnRemoveSelected.setLayoutData(gd1);	
//		GridData gd4= new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1);
//		Label dummy4 = new Label(parent, SWT.NONE);		
//		dummy4.setLayoutData(gd4);
		btnRemoveSelected.setEnabled(false);
		btnRemoveSelected.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseDown(MouseEvent e) {
				int iSelRow = gridSpecialPeaksGrid.getSelectionIndex();
				if( iSelRow != -1 ) {
					GridItem item = gridSpecialPeaksGrid.getItem(iSelRow);
					gridItemsSpecialPeaks.remove(iSelRow);
					gridSpecialPeaksGrid.remove(iSelRow);
				}
				setIsDirty(true);
				btnRemoveSelected.setEnabled(false);
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});
		btnRemoveSelected.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				//				bIsEditingTable = false; // ?

			}

			@Override
			public void focusGained(FocusEvent e) {
				disposeEditor();
			}
		});
	}

	protected void addRemoveAllRowsComponents( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		btnRemoveAllRows = new Button(parent, SWT.NONE);
		btnRemoveAllRows.setText("Remove All Rows");
		btnRemoveAllRows.setLayoutData(gd1);	
		GridData gd4= new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		Label dummy4 = new Label(parent, SWT.NONE);		
		dummy4.setLayoutData(gd4);
		btnRemoveAllRows.setEnabled(false);
		btnRemoveAllRows.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseDown(MouseEvent e) {
				int iVal = ErrorUtils.createMessageBoxReturn(getShell(), "Confirmation", "Are you sure?");
				if( iVal == 1 ) {
					gridSpecialPeaksGrid.disposeAllItems();
				}
				setIsDirty(true);
				btnRemoveSelected.setEnabled(false);
				btnRemoveAllRows.setEnabled(false);
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});
		btnRemoveSelected.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				//				bIsEditingTable = false; // ?

			}

			@Override
			public void focusGained(FocusEvent e) {
				disposeEditor();
			}
		});
	}
	
	protected void addStandardQuantFileItem( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		lblAddStandardQuantFromFile = new Label(parent, SWT.NONE);
		lblAddStandardQuantFromFile.setText("Add StandardQuant From File");
		lblAddStandardQuantFromFile.setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		txtAddStandardQuantFromFile = new Text(parent, SWT.BORDER);
		txtAddStandardQuantFromFile.setText("<Please Browse for your file>");
		txtAddStandardQuantFromFile.setLayoutData(gd2);	
		txtAddStandardQuantFromFile.setEnabled(false);

		GridData gd3 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		btnAddStandardQuantFromFile = new Button(parent, SWT.NONE);
		btnAddStandardQuantFromFile.setText("Browse");
		btnAddStandardQuantFromFile.setLayoutData(gd3);		
		btnAddStandardQuantFromFile.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseDown(MouseEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
				fd.setText("Please select file for import");
				fd.setFilterNames( new String[] { MassSpecStandardQuantFile.FILE_EXTENSION_TXT_NAME, "All files"});
				fd.setFilterExtensions( new String[] {"*" + MassSpecStandardQuantFile.FILE_EXTENSION_TXT, "*.*"});
				String sPath = fd.open();
				if( sPath != null ) {
					readTxtFile(sPath);	
					initPeakListValues(currentStandardQuant, -1, -1);
					txtAddStandardQuantFromFile.setText(sPath);
					setRemoveAllRowsEnabled();
					setIsDirty(true);
				}
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});
		btnAddStandardQuantFromFile.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				//				bIsEditingTable = false; // ?

			}

			@Override
			public void focusGained(FocusEvent e) {
				disposeEditor();
				setDeleteSelectedEnabled();
			}
		});
	}

	/**
	 * Reads the contents given in sPath to the standard quantification object passed in
	 * 
	 * @param currentStandardQuant standard quantification object to be filled in
	 * @param sPath
	 */
	protected void readTxtFile(String sPath) {
		MassSpecStandardQuantFile.readTxtFile(currentStandardQuant, sPath);
	}


	protected void addSaveSettingsItem( Composite parent, boolean bAddItem ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		if( bAddItem ) {
			btnSaveAsDefault = new Button(parent, SWT.CHECK);
			btnSaveAsDefault.setText("Save in workspace preferences");
			btnSaveAsDefault.setLayoutData(gd1);
			btnSaveAsDefault.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					setIsDirty(true);
					disposeEditor();
					setDeleteSelectedEnabled();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// TODO Auto-generated method stub

				}
			});
		} else {
			Label label = new Label(parent, SWT.NONE);
			label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		}
		GridData gd4= new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1);
		Label dummy4 = new Label(parent, SWT.NONE);		
		dummy4.setLayoutData(gd4);
	}

	protected void addBottomButtonsItem( Composite parent ) {

	}

	protected MassSpecStandardQuantPeak getNewMassSpecStandardQuantPeak() {
		return new MassSpecStandardQuantPeak();
	}

	protected void updateCurrentStandardQuantPeakData() {
		if( currentStandardQuant != null ) {
			HashMap<Double, MassSpecStandardQuantPeak> standardQuantPeaks = new HashMap<Double, MassSpecStandardQuantPeak>();
			for( int i = 0; i < gridSpecialPeaksGrid.getItems().length; i++ ) {
				GridItem gi = gridSpecialPeaksGrid.getItem(i);
				if( isBlankRow(gi) ) {
					continue;
				}
				MassSpecStandardQuantPeak peak = getNewMassSpecStandardQuantPeak();
				try {
					fillPeak(gi, peak);
					Double dMz = Double.parseDouble(gi.getText(0).trim());
					standardQuantPeaks.put(dMz, peak);
				} catch( Exception ex ) {
					;
				}
			}
			currentStandardQuant.setStandardQuantPeaks(standardQuantPeaks);
		}		
	}

	protected void addCurrentStandardQuant() {
		if( currentStandardQuant == null ) {
			return;
		}
		MassSpecStandardQuant toDelete = null;
		for( int i = 0; i < getPreferenceStandardQuant(getLocalStandardQuant()).size(); i++ ) {
			MassSpecStandardQuant curStandardQuant = getPreferenceStandardQuant(getLocalStandardQuant()).get(i);
			if( curStandardQuant.getStandardQuantName().equals( currentStandardQuant.getStandardQuantName() ) ) {
				toDelete = curStandardQuant;
			}
		}
		//		}
		if( toDelete != null ) {
			getPreferenceStandardQuant(getLocalStandardQuant()).remove(toDelete);
		}
		getPreferenceStandardQuant(getLocalStandardQuant()).add(currentStandardQuant);
	}

	protected String[] getHeaderColumns() {
		return MassSpecStandardQuantFile.HEADER_COLUMN_LABELS;
	}

	protected void initGridColumns() {
		GridColumn[] cols = new GridColumn[getHeaderColumns().length];
		for( int i = 0; i < getHeaderColumns().length; i++ ) {
			cols[i] = new GridColumn(gridSpecialPeaksGrid, SWT.NONE);
			cols[i].setText(getHeaderColumns()[i]);		
			cols[i].setWidth(getDefaultColumnWidth(i));
		}
		if( btnRemoveSelected != null ) {
			btnRemoveSelected.setEnabled(false);
		}
	}

	protected int getDefaultColumnWidth( int _iColNum ) {
		if( _iColNum == 0 ) { // m/z
			return 100;
		} else if ( _iColNum == 1 ) { // ms level
			return 100;
		} else if ( _iColNum == 2 ) { // peak label 
			return 200;
		}
		return 50; // ??
	}

	protected void fillRow( GridItem gi, MassSpecStandardQuantPeak peak ) {
		int j = 0;
		try {
			if( peak == null || peak.getPeakMz() == null ) {
				return;
			}
			gi.setText(j++, peak != null ? Double.toString(peak.getPeakMz()) : "");
			gi.setText(j++, peak != null ? Integer.toString(peak.getMSLevel()) : "");
			gi.setText(j++, peak != null ? peak.getPeakLabel() : "");	
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
	}

	protected boolean isBlankRow( GridItem gi ) {
		for( int i = 0; i < getHeaderColumns().length; i++ ) {
			if( ! gi.getText(i).equals("") ) {
				return false;
			}
		}
		return true;
	}

	protected void fillPeak( GridItem gi, MassSpecStandardQuantPeak peak ) {
		try {
			if( isBlankRow(gi) ) {
				return;
			}
			int j = 0;
			peak.setPeakMz( Double.parseDouble(gi.getText(j++)) );
			peak.setMSLevel( Integer.parseInt(gi.getText(j++)) );
			peak.setPeakLabel( gi.getText(j++) );
		} catch( NumberFormatException ex ) {
			logger.error(ex.getMessage(), ex);
		}
	}

	protected List<MassSpecStandardQuantPeak> getSortedPeaks( MassSpecStandardQuant standardQuant, 
			int iSortCol, int sortDir ) {
		if( standardQuant != null ) {
			List<Double> sortNumericKeys = new ArrayList<>();
			List<String> sortStringKeys = new ArrayList<>();
			Collection<MassSpecStandardQuantPeak> peakCollection = standardQuant.getStandardQuantPeaks().values();
			Iterator<MassSpecStandardQuantPeak> itr = peakCollection.iterator();
			while( itr.hasNext() ) {
				MassSpecStandardQuantPeak peak = itr.next();
				if( iSortCol <= 0 ) {
					sortNumericKeys.add(peak.getPeakMz());
				} else if ( iSortCol == 1 ) {
					sortNumericKeys.add((double)peak.getMSLevel());
				} else {
					sortStringKeys.add(peak.getPeakLabel());
				}
			}
			if( iSortCol >= 0 ) {
				if( sortDir == SWT.UP ) {
					Collections.sort(sortNumericKeys);
					Collections.sort(sortStringKeys);
				} else {
					Collections.sort(sortNumericKeys, Collections.reverseOrder());
					Collections.sort(sortStringKeys, Collections.reverseOrder());

				}
			}
			List list = sortNumericKeys.isEmpty() ? sortStringKeys : sortNumericKeys;
			List<MassSpecStandardQuantPeak> sortedList = new ArrayList<>();			
			for( int i = 0; i < list.size(); i++ ) {
				Object oVal = list.get(i);
				itr = peakCollection.iterator();
				while( itr.hasNext() ) {
					MassSpecStandardQuantPeak peak = itr.next();
					if( sortedList.contains(peak) ) {
						continue;
					}
					if( iSortCol <= 0 ) {
						if( (Double) oVal == peak.getPeakMz() ) {					
							sortedList.add(peak);
						}
					} else if ( iSortCol == 1 ) {
						if( ((Double) oVal).intValue() == peak.getMSLevel() ) {					
							sortedList.add(peak);
						}
					} else {
						if( ((String) oVal).equals(peak.getPeakLabel()) ){					
							sortedList.add(peak);
						}
					}
				}
			}
			return sortedList;		
		}
		return null;
	}

	protected void initPeakListValues( MassSpecStandardQuant standardQuant, int iSortCol, int iSortOrder ) {
		if( this.gridItemsSpecialPeaks == null ) {
			this.gridItemsSpecialPeaks = new ArrayList<GridItem>();
		}
		if( standardQuant != null ) {		
			removeBlankRows();			
			for( int i = 0; i < getHeaderColumns().length; i++ ) {
				GridColumn col = gridSpecialPeaksGrid.getColumn(i);
				col.setSort(0);
			}
			List<MassSpecStandardQuantPeak> lSortedList = getSortedPeaks(standardQuant, iSortCol, iSortOrder);
			for( int i = 0; i < lSortedList.size(); i++ ) {
				GridItem gi = new GridItem(gridSpecialPeaksGrid, SWT.NONE);
				MassSpecStandardQuantPeak peak = lSortedList.get(i);
				fillRow( gi, peak);
				//				this.listStandardQuantPeaks.add(peak);
				gridItemsSpecialPeaks.add(gi);		
			}
			if( iSortOrder >= 0 ) {
				gridSpecialPeaksGrid.getColumn(iSortCol).setSort(iSortOrder);
			}			
		}
		addBlankRows(3);
	}

	protected void addBlankRows( int iNumRows) {
		int iStart = gridSpecialPeaksGrid.getItemCount();
		int iEnd = iStart + iNumRows;
		for( int i = iStart; i < iEnd; i++ ) { // add 3 blank rows
			GridItem gi = new GridItem(gridSpecialPeaksGrid, SWT.NONE);
			fillRow(gi, null);
			gridItemsSpecialPeaks.add(gi);
		}		
	}

	protected void removeBlankRows() {
		List<Integer> iBlankRows = new ArrayList<>();
		int iEnd = gridSpecialPeaksGrid.getItemCount();
		for( int i = 0; i < iEnd; i++ ) { // add 3 blank rows
			GridItem gi = gridSpecialPeaksGrid.getItem(i);
			boolean bDone = false;
			for( int j = 0; j < getHeaderColumns().length; j++ ) {
				String sVal = gi.getText(j);
				if( sVal != null && ! sVal.trim().equals("") ) {
					bDone = true;
					break;
				}
			}
			if( ! bDone ) {
				iBlankRows.add(i);
			}
		}	
		if( ! iBlankRows.isEmpty() ) {
			int[] iRows = new int[iBlankRows.size()];
			int iCnt = 0;
			for( int i = 0; i < iBlankRows.size(); i++ ) {
				iRows[iCnt++] = iBlankRows.get(i);
			}
			gridSpecialPeaksGrid.remove(iRows);
		}
	}
	
	public void updateSettings() {
		disposeEditor();
		if( currentStandardQuant != null ) {
			currentStandardQuant.setStandardQuantName(sStandardQuantName);
			currentStandardQuant.setDescription(sDescription);
			currentStandardQuant.setMassTolerance(dMassTolerance);
			currentStandardQuant.setIsPPM(bIsPPM);
			updateCurrentStandardQuantPeakData();	
			//			addCurrentStandardQuant();
		}
		//		setIsDirty(false);
	}

	public void updatePreferences() {
		//		updateSettings();
		if( currentStandardQuant != null ) {
			addCurrentStandardQuant();
		}
		setIsDirty(false);
	}

	public void updateUI() {
		initStoredStandardQuantList();
		setEditEnabled(false);
		if( cmbSelectStandardQuant.getSelectionIndex() == -1 ) {
			// added new, right?
			cmbSelectStandardQuant.select(cmbSelectStandardQuant.getItemCount() - 1);
		} else {
			cmbSelectStandardQuant.select(cmbSelectStandardQuant.getSelectionIndex()); // force an event?
		}
		btnEditCurrent.setEnabled(true);
		btnExportXML.setEnabled(true);
		btnDeleteCurrent.setEnabled(true);
		setDeleteSelectedEnabled();
		setRemoveAllRowsEnabled();
		setResetEnabled();
	}

	protected void handleEdit() {
		if(gridSpecialPeaksGrid.getSelectionIndices().length>1){
			setErrorMessage("You can edit only one specialPeak at a time");
			return;
		}
		if(isReadyToFinish())
			setPageComplete(true);
		else
			setPageComplete(false);
	}

	protected boolean isReadyToFinish(){
		sStandardQuantName = null;
		sDescription = null;
		sMSLevel = null;
		dMassTolerance = null;
		bIsPPM = null;

		if( ! txtStandardQuantName.getText().trim().equals("") ) {
			sStandardQuantName = txtStandardQuantName.getText().trim();
		} else {
			setErrorMessage("StandardQuant name cannot be empty");
			return false;
		}
		sDescription = txtDescription.getText().trim();

		boolean bPass = true;
		if( ! txtMassTolerance.getText().trim().equals("") ) {
			try {
				dMassTolerance = Double.parseDouble(txtMassTolerance.getText().trim());
			} catch( NumberFormatException ex ) {
				bPass = false;
			}
			if( dMassTolerance <= 0.0 ) {
				bPass = false;
			}
		} else {
			bPass = false;
		}
		if( ! bPass ) {
			setErrorMessage("Mass tolerance must be a valid, positive number");
			return false;
		}

		if( btnTolerancePPM.getSelection() ) {
			bIsPPM = Boolean.TRUE;
		} else {
			bIsPPM = Boolean.FALSE;
		}
		if( gridItemsSpecialPeaks == null || gridItemsSpecialPeaks.isEmpty() ){
			setErrorMessage("No peaks specified!");
			return false;
		}
		for( int i = 0; i < gridSpecialPeaksGrid.getItems().length; i++ ) {
			GridItem gi = gridSpecialPeaksGrid.getItem(i);
			if( isBlankRow(gi) ) {
				continue;
			}
			for( int j = 0; j < getHeaderColumns().length; j++ ) {
				String sText = gi.getText(j);
				if( ! isValidCellValue(j, sText) ) {
					return false;
				}
			}
		}		
		setErrorMessage(null);
		//		save();
		return true;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setPageComplete(boolean isComplete) {
		PropertyChangeEvent e = new PropertyChangeEvent(this, PAGE_COMPLETE_PROPERTY, this.isComplete, isComplete);
		this.isComplete = isComplete;
		listener.propertyChange(e);
	}

	public boolean isPageCompete() {
		return this.isComplete;
	}

	public class StandardQuantPaintListener implements PaintListener {
		MassSpecStandardQuantPreferenceUI parent = null;
		public StandardQuantPaintListener(MassSpecStandardQuantPreferenceUI parent) {
			this.parent = parent;
		}
		@Override
		public void paintControl(PaintEvent e) {
			Entry entry = getEntryForCurrentViewer();
			if( entry != null && ! entry.equals( parent.getEntry() )) {
				parent.setEntry( entry );
				parent.initLocalStandardQuant();
				parent.initStoredStandardQuantList();
			}
		}
	}

}
