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
import org.grits.toolbox.entry.ms.dialog.MassSpecCustomAnnotationDialog;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecCustomAnnotation;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecCustomAnnotationPeak;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.entry.ms.views.tabbed.MassSpecMultiPageViewer;

/**
 * Interface for creating and/or modifying custom annotation of Mass Spec data. Custom annotation essentially allows 
 * the user to specify a label for particular peaks (m/z) in the sub-scans of a parent scan. Typically this would 
 * entail tagging the precursor scans produced by a particular MS1 scan with the intensity of peaks of interest in the subsequent
 * fragment (MS2) scan of each precursor.
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 * @see IMSPreferenceWithCustomAnnotation
 *
 */
public class MassSpecCustomAnnotationsPreferenceUI extends Composite {
	protected static final Logger logger = Logger.getLogger(MassSpecCustomAnnotationsPreferenceUI.class);
	public static final String PAGE_COMPLETE_PROPERTY = "Page Complete";

	protected boolean isComplete = true;
	protected String errorMessage = null;
	protected IPropertyChangeListener listener = null;

	protected GridLayout gridLayout = null;

	protected Label lblSelectAnnotation = null;
	protected Combo cmbSelectAnnotation = null;
	protected String sCurrentAnnotation = null;

	protected Button btnExportXML = null;
	protected Button btnImportXML = null;
	protected Button btnCreateNew = null;
	protected Button btnEditCurrent = null;
	protected Button btnDeleteCurrent = null;
	protected Button btnResetCurrent = null;
	protected Label lblSeparator = null;

	protected Label lblAnnotatioName = null;
	protected Text txtAnnotationName = null;
	protected String sAnnotationName = null;

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
	protected MassSpecCustomAnnotation currentCustomAnnotation = null;
	protected Button btnRemoveSelected = null;
	protected Button btnRemoveAllRows = null;

	protected Label lblAddAnnotationsFromFile = null;
	protected Text txtAddAnnotationsFromFile = null;
	protected Button btnAddAnnotationsFromFile = null;
	protected String sAnnotationFilePath = null;

	protected Button btnSaveAsDefault = null;
	protected boolean bAddSaveAsDefault = false;

	protected boolean bIsDirty = false;
	protected boolean bIsEditingTable = false;
	protected GridEditor editor = null;

	protected IMSPreferenceWithCustomAnnotation localAnnotations = null; // need to make this generic
	protected IMSPreferenceWithCustomAnnotation storedAnnotations = null; // need to make this generic
	protected IMSPreferenceWithCustomAnnotation entryAnnotations = null; // what is applied to the current entry
	protected Entry massSpecEntry = null; // need to add the temporary custom annotation
	protected MassSpecMultiPageViewer contextViewer = null;

	public MassSpecCustomAnnotationsPreferenceUI(Composite parent, int style, 
			IPropertyChangeListener listener, boolean bAddSaveAsDefault ) {
		super(parent, style);
		this.listener = listener;
		this.bAddSaveAsDefault = bAddSaveAsDefault;
		initStoredAnnotations();
	}

	/**
	 * Loads the Mass Spec preferences from the workspace XML.
	 * 
	 * @return
	 */
	public static IMSPreferenceWithCustomAnnotation loadWorkspacePreferences() {
		try {
			return MassSpecPreferenceLoader.getMassSpecPreferences();
		} catch (Exception ex) {
			logger.error("Error getting the mass spec preferences", ex);
		}
		return null;
	}

	/**
	 * Utility method that initializes the localAnnotations and entryAnnotations member variables based on the
	 * custom annotations currently applied to the current open Mass Spec entry.
	 * @see MassSpecPreference
	 */
	public void initLocalAnnotations() {
		localAnnotations = new MassSpecPreference();
		MassSpecCustomAnnotationsPreferenceUI.initAnnotationFromEntry(massSpecEntry, localAnnotations);
		entryAnnotations = new MassSpecPreference();
		MassSpecCustomAnnotationsPreferenceUI.initAnnotationFromEntry(massSpecEntry, entryAnnotations);
	}	

	/**
	 * Populates the passed annotation object with the custom annotation data applied to the passed entry.
	 * 
	 * @param entry
	 *    a MassSpeEntry
	 * @param annotations
	 *    the custom annotation object to be populated
	 * @see IMSPreferenceWithCustomAnnotation
	 * @see MassSpecPreference
	 * @see MassSpecCustomAnnotation
	 */
	public static void initAnnotationFromEntry( Entry entry, IMSPreferenceWithCustomAnnotation annotations ) {
		((MassSpecPreference)annotations).setCustomAnnotations( new ArrayList<MassSpecCustomAnnotation>() );
		List<String> localAnnotsAdded = new ArrayList<String>();
		if( entry != null ) {
			Property p = entry.getProperty();
			if( p != null ) {				
				List<MassSpecCustomAnnotation> l = ((MassSpecEntityProperty) p).getMassSpecParentProperty().getMassSpecMetaData().getCustomAnnotations();
				if( l != null && ! l.isEmpty() ) {
					for( MassSpecCustomAnnotation curAnnot : l ) {
						annotations.getCustomAnnotations().add( (MassSpecCustomAnnotation) curAnnot.clone() );
						localAnnotsAdded.add(curAnnot.getAnnotationName());
					}
				}
				((MassSpecPreference)annotations).setCustomAnnotationText( MassSpecPreference.createCustomAnnotationsText(annotations.getCustomAnnotations()) );
			}		
		}
	}

	/**
	 * Initializes the storedAnnotations member variable by loading the custom annotations from preferences in the workspace XML
	 */
	public void initStoredAnnotations() {
		storedAnnotations = MassSpecCustomAnnotationsPreferenceUI.loadWorkspacePreferences();		
	}

	/**
	 * @return the storedAnnotations member variable
	 */
	public IMSPreferenceWithCustomAnnotation getStoredAnnotations() {
		return storedAnnotations;
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
	 * Sets the localAnnotations member variable
	 * @param localAnnotations
	 */
	public void setLocalAnnotations(IMSPreferenceWithCustomAnnotation localAnnotations) {
		this.localAnnotations = localAnnotations;
	}

	/**
	 * @return the localAnnotations member variable
	 */
	public IMSPreferenceWithCustomAnnotation getLocalAnnotations() {
		return localAnnotations;
	}

	/**
	 * Sets the entryAnnotations member variable
	 * @param entryAnnotations
	 */
	public void setEntryAnnotations(IMSPreferenceWithCustomAnnotation entryAnnotations) {
		this.entryAnnotations = entryAnnotations;
	}

	/**
	 * @return the entryAnnotations member variable
	 */
	public IMSPreferenceWithCustomAnnotation getEntryAnnotations() {
		return entryAnnotations;
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
		addSelectAnnotationItem(this);

		addDeleteCurrent(this);
		addResetCurrent(this);
		addImportXML(this);
		addExportXML(this);
		addEditCurrent(this);
		addCreateNewItem(this);

		addSeparatorLine1(this);

		addAnnotationNameItem(this);
		addDescriptionItem(this);
		addMassToleranceItem(this);
		addToleranceTypeItem(this);
		addPeakListItem(this);
		addRemoveRowComponents(this);
		addRemoveAllRowsComponents(this);
		addAnnotationsFileItem(this);
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
	 * Used before saving preferences to merge any locally added or modified custom annotations into the 
	 * storedPreferences member variable in order to be written to workspace XML.
	 */
	protected void mergePreferences() {
		if( getStoredAnnotations() != null && getStoredAnnotations().getCustomAnnotations() != null ) {
			for( int i = 0; i < getStoredAnnotations().getCustomAnnotations().size(); i++ ) {
				MassSpecCustomAnnotation storedAnnot = getStoredAnnotations().getCustomAnnotations().get(i);
				boolean bFound = false;
				for( int j = 0; j < getLocalAnnotations().getCustomAnnotations().size(); j++ ) {
					MassSpecCustomAnnotation localAnnot = getLocalAnnotations().getCustomAnnotations().get(j);
					if( storedAnnot.getAnnotationName().equals(localAnnot.getAnnotationName()) )  {
						bFound = true; 
						break;
					}
				}
				if( ! bFound ) {
					getLocalAnnotations().getCustomAnnotations().add(storedAnnot);
				}
			}
		}		
	}

	/**
	 * If opened from a MassSpec viewer (i.e. not from Preferences) then the user has the option of making local
	 * modifications to custom annotations that arent's saved in preferences via a checkbox. If checked, all changes
	 * will be written to workspace XML, otherwise they are only stored in localAnnotations.
	 * 
	 * @return true if the btnSaveAsDefault checkbox is checked, false otherwise
	 */
	public boolean getSaveAsDefault() {
		return bAddSaveAsDefault && btnSaveAsDefault.getSelection();
	}

	/**
	 * Updates annotation lists and saves changes to workspace preferences, if "save as default" is selected (or called from Preferences)
	 */
	public void save() {
		//		if( getSaveAsDefault() ) {
		mergePreferences();
		getLocalAnnotations().saveValues();
		initStoredAnnotations();
		initLocalAnnotations();
		//		}
	}

	/**
	 * Initializes the grid layout for the window.
	 */
	protected void initGridLayout() {
		gridLayout = new GridLayout(6, false);
	}

	/**
	 * When the selection of the cmbSelectAnnotation combo control changes, processes the selection. If there is a local version
	 * of the custom annotation, then the information from that object is used. Otherwise, the information from the preference
	 * object is used.
	 * 
	 * @param _sAnnotName, the text from the cmbSelectAnnotation combo control w/ available custom annotations
	 */
	public void setSelectedItem( String _sAnnotName ) {
		for( int i = 0; i < cmbSelectAnnotation.getItemCount(); i++ ) {
			String sText = cmbSelectAnnotation.getItem(i);
			if( sText.trim().equals(_sAnnotName.trim()) ) {
				cmbSelectAnnotation.select(i);
				if( ! cmbSelectAnnotation.getText().trim().equals("") ) {
					String selAnnotName = cmbSelectAnnotation.getText().trim();
					MassSpecCustomAnnotation selAnnot = null;
					MassSpecCustomAnnotation prefAnnot = MassSpecCustomAnnotationDialog.getCurrentAnnot(selAnnotName, storedAnnotations);
					MassSpecCustomAnnotation localAnnot = MassSpecCustomAnnotationDialog.getCurrentAnnot(selAnnotName, localAnnotations);
					if( localAnnot != null ) {
						selAnnot = localAnnot;
					} else if( prefAnnot != null ) {
						selAnnot = prefAnnot;
					} else {
						return;
					}
					processSelection(selAnnot);
				}
				break;
			}
		}
	}

	/**
	 * Called from setSelectedItem(..) to update the interface after the user selects an annotation entry in the cmbSelectAnnotation combo.
	 * 
	 * @param desiredPreferences
	 */
	public void processSelection( MassSpecCustomAnnotation desiredPreferences ) {
		if( cancelIfDirty() ) {
			return;
		}				
		setEditEnabled(false);
		currentCustomAnnotation = null;
		if( desiredPreferences != null ) {
			btnEditCurrent.setEnabled(true);
			btnExportXML.setEnabled(true);
			btnDeleteCurrent.setEnabled(true);
			setCurrentAnnotationValues(desiredPreferences);
			setResetEnabled();
		} else {
			setPageComplete(true);
		}
	}

	/**
	 * Adds the cmbSelectAnnotation control and its label to the interface. 
	 * 
	 * @param parent
	 */
	protected void addSelectAnnotationItem( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
		lblSelectAnnotation = new Label(parent, SWT.NONE);
		lblSelectAnnotation.setText("Current Custom Annotations");
		lblSelectAnnotation.setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		cmbSelectAnnotation = new Combo(parent, SWT.READ_ONLY);
		cmbSelectAnnotation.setLayoutData(gd2);
		sCurrentAnnotation = null;
		initStoredAnnotationsList();
		cmbSelectAnnotation.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if( ! cmbSelectAnnotation.getText().trim().equals("") ) {
					String selAnnotName = cmbSelectAnnotation.getText().trim();
					MassSpecCustomAnnotation selAnnot = null;
					MassSpecCustomAnnotation prefAnnot = MassSpecCustomAnnotationDialog.getCurrentAnnot(selAnnotName, storedAnnotations);
					MassSpecCustomAnnotation localAnnot = MassSpecCustomAnnotationDialog.getCurrentAnnot(selAnnotName, localAnnotations);
					if( localAnnot != null ) {
						selAnnot = localAnnot;
					} else if( prefAnnot != null ) {
						selAnnot = prefAnnot;
					} else {
						return;
					}
					processSelection(selAnnot);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});
		cmbSelectAnnotation.addFocusListener(new FocusListener() {

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
			cmbSelectAnnotation.addPaintListener(new CustomAnnotationPaintListener(this));
		}

	}

	/**
	 * If changes have been made to the currently open custom annotation, the user might still choose
	 * to select a different annotation. This method prompts the user for confirmation before discarding changes. 
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
	 * If local modifications have been made to a custom annotation that is stored in preferences, the user can restore the values from 
	 * stored preferences. This method prompts the user for confirmation before overwriting. 
	 * @return true if the user wants to restore, false otherwise
	 */
	protected boolean confirmRestore() {
		boolean bContinue = MessageDialog.openConfirm(getShell(), "Reset Peaks", "This will reload values in preferences. Local changes will be lost. Continue?");
		return bContinue;
	}


	/**
	 * This method allows generic overriding with different preference types as the parameter. Each method casts to the 
	 * specific preference type and returns the list of custom annotations.
	 * 
	 * @param preferences
	 *   A preference object associated with the entry. 
	 * @return the list of custom annotations from the passed preferences.
	 */
	protected List<MassSpecCustomAnnotation> getPreferenceCustomAnnotations(Object preferences) {
		return ((MassSpecPreference) preferences).getCustomAnnotations();
	}

	/**
	 * This method allows generic overriding with different preference types as the parameter. Each method casts to the 
	 * specific preference type and sets the list of custom annotations.
	 * 
	 * @param preferences
	 *   a preference object associated with the entry. 
	 * @param annotations
	 *   a list of custom annotations to be stored with the passed preference.
	 */
	protected void setPreferenceCustomAnnotations( Object preferences, List<MassSpecCustomAnnotation> annotations ) {
		((MassSpecPreference) preferences).setCustomAnnotations(annotations);
	}

	/**
	 * Initializes the list of available CustomAnnotations for the selection ComboBox
	 */
	public void initStoredAnnotationsList() {
		String sToSelect = "";
		if( cmbSelectAnnotation.getItems().length != 0 ) {
			cmbSelectAnnotation.removeAll();
		}
		cmbSelectAnnotation.add("");
		// first add the stored preferences
		if( getStoredAnnotations() != null && getStoredAnnotations().getCustomAnnotations() != null ) {
			for( int i = 0; i < getStoredAnnotations().getCustomAnnotations().size(); i++ ) {
				MassSpecCustomAnnotation annot = getStoredAnnotations().getCustomAnnotations().get(i);
				cmbSelectAnnotation.add(annot.getAnnotationName());
				if( currentCustomAnnotation != null && annot.getAnnotationName().equals(currentCustomAnnotation.getAnnotationName()) ) {
					int iItemCount = cmbSelectAnnotation.getItemCount();
					sToSelect = cmbSelectAnnotation.getItem(iItemCount-1);
				}
			}
		}
		if( getContextViewer() != null && getLocalAnnotations() != null && getPreferenceCustomAnnotations(getLocalAnnotations()) != null ) {
			for( int i = 0; i < getPreferenceCustomAnnotations(getLocalAnnotations()).size(); i++ ) {
				MassSpecCustomAnnotation annot = getPreferenceCustomAnnotations(getLocalAnnotations()).get(i);
				MassSpecCustomAnnotationDialog.setComboEntryForTempAnnotation(annot, cmbSelectAnnotation, 
						entryAnnotations.getCustomAnnotations(),
						storedAnnotations.getCustomAnnotations());
				if( currentCustomAnnotation != null && annot.getAnnotationName().equals(currentCustomAnnotation.getAnnotationName()) ) {
					int iItemCount = cmbSelectAnnotation.getItemCount();
					sToSelect = cmbSelectAnnotation.getItem(iItemCount-1);
				}
			}
		}
		int iPrevSelInx = cmbSelectAnnotation.indexOf(sToSelect);
		cmbSelectAnnotation.select(iPrevSelInx);		
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
					int iSelInx = cmbSelectAnnotation.getSelectionIndex();
					MassSpecCustomAnnotation localAnnot = MassSpecCustomAnnotationDialog.getCurrentAnnot(cmbSelectAnnotation.getItem(iSelInx), localAnnotations);

					getPreferenceCustomAnnotations(getLocalAnnotations()).remove(localAnnot);
					cmbSelectAnnotation.remove(iSelInx);
					clearValues();
					setEditEnabled(false);
					currentCustomAnnotation = null;
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
					int iSelInx = cmbSelectAnnotation.getSelectionIndex();
					MassSpecCustomAnnotation prefAnnot = MassSpecCustomAnnotationDialog.getCurrentAnnot(cmbSelectAnnotation.getItem(iSelInx), getStoredAnnotations());
					MassSpecCustomAnnotation localAnnot = MassSpecCustomAnnotationDialog.getCurrentAnnot(cmbSelectAnnotation.getItem(iSelInx), getLocalAnnotations());
					localAnnot.setAnnotatedPeaks(prefAnnot.getAnnotatedPeaks()); // overwrite the peaks but not the text yet
					if( ! cmbSelectAnnotation.getText().trim().equals("") ) {
						String selAnnotName = cmbSelectAnnotation.getText().trim();
						MassSpecCustomAnnotation storedAnnot = MassSpecCustomAnnotationDialog.getCurrentAnnot(selAnnotName, getStoredAnnotations());
						processSelection(storedAnnot);
					}
					initStoredAnnotationsList();
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
				fd.setFilterNames( new String[] { MassSpecCustomAnnotationFile.FILE_EXTENSION_XML_NAME, "All files"});
				fd.setFilterExtensions( new String[] {"*" + MassSpecCustomAnnotationFile.FILE_EXTENSION_XML, "*.*"});
				String sPath = fd.open();
				if( sPath != null ) {
					if( ! sPath.endsWith(MassSpecCustomAnnotationFile.FILE_EXTENSION_XML) ) {
						sPath += MassSpecCustomAnnotationFile.FILE_EXTENSION_XML;
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
				fd.setFilterNames( new String[] { MassSpecCustomAnnotationFile.FILE_EXTENSION_XML_NAME, "All files"});
				fd.setFilterExtensions( new String[] {"*" + MassSpecCustomAnnotationFile.FILE_EXTENSION_XML, "*.*"});
				String sPath = fd.open();
				if( sPath != null ) {
					if( ! sPath.endsWith(MassSpecCustomAnnotationFile.FILE_EXTENSION_XML) ) {
						sPath += MassSpecCustomAnnotationFile.FILE_EXTENSION_XML;
					}
					readXMLFile (sPath);
					if( currentCustomAnnotation != null ) {
						cmbSelectAnnotation.add(currentCustomAnnotation.getAnnotationName());
						cmbSelectAnnotation.select(cmbSelectAnnotation.getItemCount() - 1);
						processSelection(currentCustomAnnotation);
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

	/**
	 * Opens an error message dialog if an invalid file is specified for import.
	 */
	protected void showInvalidImportFileMessage() {
		ErrorUtils.createErrorMessageBox(getShell(), "Not a valid Mass Spec Custom Annotation XML file.");
	}

	/**
	 * Writes the selected custom annotation to a file given
	 * 
	 * @param sPath
	 */
	protected void writeXMLFile(String sPath) {
		MassSpecCustomAnnotationFile.writeXMLFile(currentCustomAnnotation, sPath);
	}

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
				//				setCurrentAnnotationValues(cmbSelectAnnotation.getText());
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

	protected MassSpecCustomAnnotation getNewMassSpecCustomAnnotation() {
		return new MassSpecCustomAnnotation();
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
				btnExportXML.setEnabled(false);
				btnEditCurrent.setEnabled(false);
				clearValues();	
				currentCustomAnnotation = getNewMassSpecCustomAnnotation();			
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

	protected void setCurrentAnnotationValues(MassSpecCustomAnnotation selAnnot) {
		if( selAnnot == null ) {
			return;
		}
		currentCustomAnnotation = selAnnot;
		txtAnnotationName.setText(selAnnot.getAnnotationName());
		txtDescription.setText(selAnnot.getDescription());
		txtMassTolerance.setText(selAnnot.getMassTolerance().toString());
		if( selAnnot.getIsPPM() ) {
			btnTolerancePPM.setSelection(true);
		} else {
			btnToleranceDalton.setSelection(true);
		}
		initPeakListValues(selAnnot, -1, -1);
		setIsDirty(false);
	}

	public void setEditEnabled( boolean _bVal ) {
		btnDeleteCurrent.setEnabled(_bVal);
		btnExportXML.setEnabled(_bVal);
		btnEditCurrent.setEnabled(_bVal);
		lblAnnotatioName.setEnabled(_bVal);
		txtAnnotationName.setEnabled(_bVal);
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
		lblAddAnnotationsFromFile.setEnabled(_bVal);
//		txtAddAnnotationsFromFile.setEnabled(_bVal);
		btnAddAnnotationsFromFile.setEnabled(_bVal);
		gridSpecialPeaksGrid.redraw();
	}

	public void setResetEnabled() {
		btnResetCurrent.setEnabled(cmbSelectAnnotation.getText().trim().endsWith(MassSpecCustomAnnotationDialog.LOCAL_ANNOTATION_DIFFERS));
	}

	public void clearValues() {
		txtAnnotationName.setText("");
		txtDescription.setText("");
		//		cmbMSLevel.select(0);
		txtMassTolerance.setText("");
		gridSpecialPeaksGrid.clearAll(true);
		txtAddAnnotationsFromFile.setText("<Please Browse for your file>");
		cmbSelectAnnotation.select(0);
		btnRemoveSelected.setEnabled(false);
		btnRemoveAllRows.setEnabled(false);
		setIsDirty(false);
	}

	protected void addAnnotationNameItem( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		lblAnnotatioName = new Label(parent, SWT.NONE);
		lblAnnotatioName.setText("Name of Custom Annotation");
		lblAnnotatioName.setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1);
		txtAnnotationName = new Text(parent, SWT.BORDER);
		txtAnnotationName.setLayoutData(gd2);
		txtAnnotationName.addKeyListener(new KeyListener() {

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
		txtAnnotationName.addFocusListener(new FocusListener() {

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
					updateCurrentAnnotationPeakData();
					initPeakListValues(currentCustomAnnotation, iColNum, iSortOrder);
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
			if( iVal < 2 ) {
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
		btnRemoveSelected.setText("Remove Selected Row");
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
	
	protected void addAnnotationsFileItem( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		lblAddAnnotationsFromFile = new Label(parent, SWT.NONE);
		lblAddAnnotationsFromFile.setText("Add Annotations From File");
		lblAddAnnotationsFromFile.setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		txtAddAnnotationsFromFile = new Text(parent, SWT.BORDER);
		txtAddAnnotationsFromFile.setText("<Please Browse for your file>");
		txtAddAnnotationsFromFile.setLayoutData(gd2);	
		txtAddAnnotationsFromFile.setEnabled(false);

		GridData gd3 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		btnAddAnnotationsFromFile = new Button(parent, SWT.NONE);
		btnAddAnnotationsFromFile.setText("Browse");
		btnAddAnnotationsFromFile.setLayoutData(gd3);		
		btnAddAnnotationsFromFile.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseDown(MouseEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
				fd.setText("Please select file for import");
				fd.setFilterNames( new String[] { MassSpecCustomAnnotationFile.FILE_EXTENSION_TXT_NAME, "All files"});
				fd.setFilterExtensions( new String[] {"*" + MassSpecCustomAnnotationFile.FILE_EXTENSION_TXT, "*.*"});
				String sPath = fd.open();
				if( sPath != null ) {
					readTxtFile(sPath);	
					initPeakListValues(currentCustomAnnotation, -1, -1);
					txtAddAnnotationsFromFile.setText(sPath);
					setRemoveAllRowsEnabled();
					setIsDirty(true);
				}
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});
		btnAddAnnotationsFromFile.addFocusListener(new FocusListener() {

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
	 * Reads the contents given in sPath to the custom annotation object passed in
	 * 
	 * @param currentCustomAnnotation custom annotation object to be filled in
	 * @param sPath
	 */
	protected void readXMLFile(String sPath) {
		currentCustomAnnotation = MassSpecCustomAnnotationFile.readXMLFile(sPath);
	}

	/**
	 * Reads the contents given in sPath to the custom annotation object passed in
	 * 
	 * @param currentCustomAnnotation custom annotation object to be filled in
	 * @param sPath
	 */
	protected void readTxtFile(String sPath) {
		MassSpecCustomAnnotationFile.readTxtFile(currentCustomAnnotation, sPath);
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

	protected MassSpecCustomAnnotationPeak getNewMassSpecCustomAnnotationPeak() {
		return new MassSpecCustomAnnotationPeak();
	}

	protected void updateCurrentAnnotationPeakData() {
		if( currentCustomAnnotation != null ) {
			HashMap<Double, MassSpecCustomAnnotationPeak> annotatedPeaks = new HashMap<Double, MassSpecCustomAnnotationPeak>();
			for( int i = 0; i < gridSpecialPeaksGrid.getItems().length; i++ ) {
				GridItem gi = gridSpecialPeaksGrid.getItem(i);
				if( isBlankRow(gi) ) {
					continue;
				}
				MassSpecCustomAnnotationPeak peak = getNewMassSpecCustomAnnotationPeak();
				try {
					fillPeak(gi, peak);
					Double dMz = Double.parseDouble(gi.getText(0).trim());
					annotatedPeaks.put(dMz, peak);
				} catch( Exception ex ) {
					;
				}
			}
			currentCustomAnnotation.setAnnotatedPeaks(annotatedPeaks);
		}		
	}

	protected void addCurrentCustomAnnotation() {
		if( currentCustomAnnotation == null ) {
			return;
		}
		MassSpecCustomAnnotation toDelete = null;
		for( int i = 0; i < getPreferenceCustomAnnotations(getLocalAnnotations()).size(); i++ ) {
			MassSpecCustomAnnotation curAnnot = getPreferenceCustomAnnotations(getLocalAnnotations()).get(i);
			if( curAnnot.getAnnotationName().equals( currentCustomAnnotation.getAnnotationName() ) ) {
				toDelete = curAnnot;
			}
		}
		//		}
		if( toDelete != null ) {
			getPreferenceCustomAnnotations(getLocalAnnotations()).remove(toDelete);
		}
		getPreferenceCustomAnnotations(getLocalAnnotations()).add(currentCustomAnnotation);
	}

	protected String[] getHeaderColumns() {
		return MassSpecCustomAnnotationFile.HEADER_COLUMN_LABELS;
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

	protected void fillRow( GridItem gi, MassSpecCustomAnnotationPeak peak ) {
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

	protected void fillPeak( GridItem gi, MassSpecCustomAnnotationPeak peak ) {
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

	protected List<MassSpecCustomAnnotationPeak> getSortedPeaks( MassSpecCustomAnnotation customAnnotation, 
			int iSortCol, int sortDir ) {
		if( customAnnotation != null ) {
			List<Double> sortNumericKeys = new ArrayList<>();
			List<String> sortStringKeys = new ArrayList<>();
			Collection<MassSpecCustomAnnotationPeak> peakCollection = customAnnotation.getAnnotatedPeaks().values();
			Iterator<MassSpecCustomAnnotationPeak> itr = peakCollection.iterator();
			while( itr.hasNext() ) {
				MassSpecCustomAnnotationPeak peak = itr.next();
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
			List<MassSpecCustomAnnotationPeak> sortedList = new ArrayList<>();			
			for( int i = 0; i < list.size(); i++ ) {
				Object oVal = list.get(i);
				itr = peakCollection.iterator();
				while( itr.hasNext() ) {
					MassSpecCustomAnnotationPeak peak = itr.next();
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

	protected void initPeakListValues( MassSpecCustomAnnotation customAnnotation, int iSortCol, int iSortOrder ) {
		if( this.gridItemsSpecialPeaks == null ) {
			this.gridItemsSpecialPeaks = new ArrayList<GridItem>();
		}
		if( customAnnotation != null ) {
			removeBlankRows();			
			for( int i = 0; i < getHeaderColumns().length; i++ ) {
				GridColumn col = gridSpecialPeaksGrid.getColumn(i);
				col.setSort(0);
			}
			List<MassSpecCustomAnnotationPeak> lSortedList = getSortedPeaks(customAnnotation, iSortCol, iSortOrder);
			for( int i = 0; i < lSortedList.size(); i++ ) {
				GridItem gi = new GridItem(gridSpecialPeaksGrid, SWT.NONE);
				MassSpecCustomAnnotationPeak peak = lSortedList.get(i);
				fillRow( gi, peak);
				//				this.listCustomAnnotationPeaks.add(peak);
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
		if( currentCustomAnnotation != null ) {
			currentCustomAnnotation.setAnnotationName(sAnnotationName);
			currentCustomAnnotation.setDescription(sDescription);
			currentCustomAnnotation.setMassTolerance(dMassTolerance);
			currentCustomAnnotation.setIsPPM(bIsPPM);
			updateCurrentAnnotationPeakData();	
			//			addCurrentCustomAnnotation();
		}
		//		setIsDirty(false);
	}

	public void updatePreferences() {
		//		updateSettings();
		if( currentCustomAnnotation != null ) {
			addCurrentCustomAnnotation();
		}
		setIsDirty(false);
	}

	public void updateUI() {
		initStoredAnnotationsList();
		setEditEnabled(false);
		if( cmbSelectAnnotation.getSelectionIndex() == -1 ) {
			// added new, right?
			cmbSelectAnnotation.select(cmbSelectAnnotation.getItemCount() - 1);
		} else {
			cmbSelectAnnotation.select(cmbSelectAnnotation.getSelectionIndex()); // force an event?
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
		sAnnotationName = null;
		sDescription = null;
		sMSLevel = null;
		dMassTolerance = null;
		bIsPPM = null;

		if( ! txtAnnotationName.getText().trim().equals("") ) {
			sAnnotationName = txtAnnotationName.getText().trim();
		} else {
			setErrorMessage("Annotation name cannot be empty");
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

	public class CustomAnnotationPaintListener implements PaintListener {
		MassSpecCustomAnnotationsPreferenceUI parent = null;
		public CustomAnnotationPaintListener(MassSpecCustomAnnotationsPreferenceUI parent) {
			this.parent = parent;
		}
		@Override
		public void paintControl(PaintEvent e) {
			Entry entry = getEntryForCurrentViewer();
			if( entry != null && ! entry.equals( parent.getEntry() )) {
				parent.setEntry( entry );
				parent.initLocalAnnotations();
				parent.initStoredAnnotationsList();
			}
		}
	}

}
