package org.grits.toolbox.entry.ms.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.entry.ms.extquantfiles.process.StandardQuantColumnInfo;
import org.grits.toolbox.entry.ms.preference.IMSPreferenceWithStandardQuant;
import org.grits.toolbox.entry.ms.preference.MassSpecPreference;
import org.grits.toolbox.entry.ms.preference.MassSpecStandardQuantPreferenceUI;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecStandardQuant;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecStandardQuantPeak;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecMetaData;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecUISettings;
import org.grits.toolbox.entry.ms.views.tabbed.MassSpecMultiPageViewer;

/**
 * Simple pop-up dialog accessed from a Mass Spec GRITS table that allows the user to apply or 
 * clear internal standard quantification for the current entry. It also allows the user to open the Modify StandardQuant Dialog.
 * Note that the combo box w/ available standard quantification sets updates when the user changes windows.
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 *
 */
public class MassSpecStandardQuantApplyDialog extends MassSpecViewerDialog implements IPropertyChangeListener, IDynamicTableProcessor {
	private static final Logger logger = Logger.getLogger(MassSpecStandardQuantApplyDialog.class);
	protected final Font boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT); 
	protected Text txtOutput;
	protected static final String PARAMS_OK = "Valid parameters";

	protected GridLayout gridLayout = null;

	protected Label lblSelectStandards = null;
	protected Combo cmbSelectStandards = null;
	protected MassSpecStandardQuant selStandards = null;

	protected MassSpecStandardQuantFileGrid fileGrid = null;
	protected Button btnApplyFilter = null;
	protected Button btnModifyList = null;
	//	protected Button btnSelectQuant = null;
	protected Button btnClearStandardQuant = null;

	protected IMSPreferenceWithStandardQuant storedStandardQuant = null; // persistent copy, won't reflect changes until after save
	protected IMSPreferenceWithStandardQuant localStandardQuant = null; // local copy, can get updated if the user modifies preferences but doesn't save
	protected IMSPreferenceWithStandardQuant entryStandardQuant = null; // what is applied to the current entry
	protected MassSpecStandardQuantModifyDialog modQuantWin = null;
	protected MassSpecStandardQuantFileGrid selQuantFileWin = null;

	public final static String PROPERTY_WIN_CLOSED = "Window Closed";

	public final static String STANDARDQUANT_APPLIED = "# ";
	public final static String STANDARDQUANT_DIRTY = "* ";
	public final static String LOCAL_STANDARDQUANT_CURRENT_ENTRY = " (current entry only)";
	public final static String LOCAL_STANDARDQUANT_DIFFERS = " (differs from preferences)";

	public MassSpecStandardQuantApplyDialog(Shell parentShell, MassSpecMultiPageViewer contextViewer) {
		super(parentShell, contextViewer);
		setShellStyle(SWT.MODELESS | SWT.DIALOG_TRIM | SWT.ON_TOP | SWT.RESIZE);
		initStoredStandardQuant();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(getFormTitle());
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		//find the center of a main monitor
		Monitor primary = getShell().getDisplay().getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = getShell().getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		getShell().setLocation(x, y);
		initGridLayout();
		container.setLayout(gridLayout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		addSelectStdQuantItem(container);
		addSelectStdQuantFilesItem(container);
		//		addClearStandardQuant(container);
		addFilterItem(container);
		addModifyStdQuantItem(container);
		container.addPaintListener(new StandardQuantPaintListener(this));
		return area;
	}

	/**
	 * Initializes the 2 MassSpecPreference objects according to what is stored in the MS Metadata for this entry.
	 */
	protected void initLocalStandardQuant() {
		localStandardQuant = new MassSpecPreference();
		MassSpecUISettings msSettings = MassSpecStandardQuantPreferenceUI.getMassSpecUISettingsFromEntry(getMassSpecEntry());
		MassSpecStandardQuantPreferenceUI.initStandardQuantFromEntry(msSettings, localStandardQuant);
		entryStandardQuant = new MassSpecPreference();
		MassSpecStandardQuantPreferenceUI.initStandardQuantFromEntry(msSettings, entryStandardQuant);
	}	


	/**
	 * @param localStandardQuant
	 * 		a working copy of the standard quantification information for the currently open Entry which may become dirty.
	 */
	public void setLocalStandardQuant(IMSPreferenceWithStandardQuant localStandardQuant) {
		this.localStandardQuant = localStandardQuant;
	}

	/**
	 * @return the working copy of the entry's standard quantification information.
	 */
	public IMSPreferenceWithStandardQuant getLocalStandardQuant() {
		return localStandardQuant;
	}

	/**
	 * @return the read-only copy of the standard quantification information for the currently open Entry
	 */
	public IMSPreferenceWithStandardQuant getEntryStandardQuant() {
		return entryStandardQuant;
	}

	/**
	 * @param entryStandardQuant
	 * 		a read-only copy of the standard quantification information for the currently open Entry 
	 */
	public void setEntryStandardQuant(IMSPreferenceWithStandardQuant entryStandardQuant) {
		this.entryStandardQuant = entryStandardQuant;
	}

	/**
	 * Initializes the MassSpecPreference object according to what is stored in the workspace preferences
	 */
	public void initStoredStandardQuant() {
		storedStandardQuant = MassSpecStandardQuantPreferenceUI.loadWorkspacePreferences();				
	}

	/**
	 * @return the read-only copy of the standard quantification information for the workspace preferences
	 */
	public IMSPreferenceWithStandardQuant getStoredStandardQuant() {
		return storedStandardQuant;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.CLOSE_LABEL, true);
	}

	/**
	 * Initializes the GridLayout for this control.
	 */
	private void initGridLayout() {
		gridLayout = new GridLayout(4, false);
	}

	/**
	 * Adds the Label and Combo for the available MS Custom StandardQuant to the parent control.
	 * 
	 * @param parent
	 */
	private void addSelectStdQuantItem( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		lblSelectStandards = new Label(parent, SWT.NONE);
		lblSelectStandards.setText("Standard Quantification Peak Sets");
		lblSelectStandards.setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		cmbSelectStandards = new Combo(parent, SWT.NONE);
		cmbSelectStandards.setLayoutData(gd2);
		initLocalStandardQuant();
		initStoredStandardQuantList();
		cmbSelectStandards.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				processCmbSelectStandardsSelect();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});
		GridData gd3 = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		Label dummy = new Label(parent, SWT.NONE);		
		dummy.setLayoutData(gd3);
		GridData gd4 = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		Label dummy2 = new Label(parent, SWT.NONE);		
		dummy2.setLayoutData(gd4);
	}

	/**
	 * Performs the code to update the backend values associated with the currently selected Standard Quantitation Peak Set
	 */
	protected void processCmbSelectStandardsSelect() {
		if( cmbSelectStandards == null || btnApplyFilter == null || fileGrid == null ) {
			return;
		}
		boolean bVal = false;
		if( ! cmbSelectStandards.getText().trim().equals("") ) {
			setCurrentStdQuantValues(cmbSelectStandards.getText());
			bVal = true;
		}
		btnApplyFilter.setEnabled(bVal);
		fileGrid.setEnabled(bVal);		
	}

	/**
	 * @param selStdQuantName
	 * 		the text from the Standard Quantification selection combo
	 * @return true if there were changes to the local entry standard quantifications
	 */
	protected boolean hasLocalChanges(String selStdQuantName) {
		return selStdQuantName.endsWith(MassSpecStandardQuantApplyDialog.LOCAL_STANDARDQUANT_CURRENT_ENTRY) ||
				selStdQuantName.endsWith(MassSpecStandardQuantApplyDialog.LOCAL_STANDARDQUANT_DIFFERS);
	}

	/**
	 * Sets the "selStdQuantName" member variable based on whether there are local changes or not.
	 * 
	 * @param selStdQuantName
	 * 		the text from the selection combo control
	 */
	protected void setCurrentStdQuantValues(String selStdQuantName) {
		selStandards = null;
		if( selStdQuantName == null ) {
			return;
		}
		MassSpecStandardQuant prefStdQuant = MassSpecStandardQuantApplyDialog.getCurrentStdQuant(selStdQuantName, getStoredStandardQuant());
		MassSpecStandardQuant localStdQuant = MassSpecStandardQuantApplyDialog.getCurrentStdQuant(selStdQuantName, getLocalStandardQuant());
		if( selStdQuantName.endsWith(MassSpecStandardQuantApplyDialog.LOCAL_STANDARDQUANT_CURRENT_ENTRY) ||
				selStdQuantName.endsWith(MassSpecStandardQuantApplyDialog.LOCAL_STANDARDQUANT_DIFFERS)) {
			selStandards = localStdQuant;
		} else {
			selStandards = prefStdQuant;
		}
		fileGrid.updateAppliedFileList(selStandards.getStandardQuantName());
	}

	/**
	 * @param selStdQuantName
	 * 		the text from the selection combo control
	 * @param curStdQuant
	 * 		a candidate MassSpecStandardQuant object
	 * @return true if the text in the selection combo item is associated with the passed standard quantification object
	 */
	public static boolean isTempStdQuantForSelectedItem(String selStdQuantName, MassSpecStandardQuant curStdQuant) {
		if( curStdQuant.getStandardQuantName().equals(selStdQuantName)) {
			return true;
		}
		String sCompareTo = curStdQuant.getStandardQuantName() + MassSpecStandardQuantApplyDialog.LOCAL_STANDARDQUANT_DIFFERS;;
		if( sCompareTo.equals(selStdQuantName) ){
			return true;
		}
		sCompareTo = MassSpecStandardQuantApplyDialog.STANDARDQUANT_APPLIED + curStdQuant.getStandardQuantName();
		if( sCompareTo.equals(selStdQuantName) ){
			return true;
		}
		sCompareTo = MassSpecStandardQuantApplyDialog.STANDARDQUANT_APPLIED + curStdQuant.getStandardQuantName() + MassSpecStandardQuantApplyDialog.LOCAL_STANDARDQUANT_CURRENT_ENTRY;
		if( sCompareTo.equals(selStdQuantName) ){
			return true;
		}
		sCompareTo = MassSpecStandardQuantApplyDialog.STANDARDQUANT_APPLIED + curStdQuant.getStandardQuantName() + MassSpecStandardQuantApplyDialog.LOCAL_STANDARDQUANT_DIFFERS;
		if( sCompareTo.equals(selStdQuantName) ){
			return true;
		}
		sCompareTo = MassSpecStandardQuantApplyDialog.STANDARDQUANT_DIRTY + curStdQuant.getStandardQuantName();
		if( sCompareTo.equals(selStdQuantName) ){
			return true;
		}
		sCompareTo = MassSpecStandardQuantApplyDialog.STANDARDQUANT_DIRTY + curStdQuant.getStandardQuantName() + MassSpecStandardQuantApplyDialog.LOCAL_STANDARDQUANT_CURRENT_ENTRY;
		if( sCompareTo.equals(selStdQuantName) ){
			return true;
		}
		sCompareTo = MassSpecStandardQuantApplyDialog.STANDARDQUANT_DIRTY + curStdQuant.getStandardQuantName() + MassSpecStandardQuantApplyDialog.LOCAL_STANDARDQUANT_DIFFERS;
		if( sCompareTo.equals(selStdQuantName) ){
			return true;
		}
		return false;
	}

	/**
	 * @param selStdQuantName
	 * 		the text from the selection combo control
	 * @param preferences
	 * 		a candidate standard quantification preference object
	 * @return
	 */
	public static MassSpecStandardQuant getCurrentStdQuant(String selStdQuantName, 
			IMSPreferenceWithStandardQuant preferences) {
		MassSpecStandardQuant retStdQuant = null;
		if( preferences != null && preferences.getStandardQuant() != null ) {
			for( int i = 0; i < preferences.getStandardQuant().size(); i++ ) {
				MassSpecStandardQuant curStdQuant =  preferences.getStandardQuant().get(i);
				if( curStdQuant == null ) {
					// this is an error! remove it?
					continue;
				}
				if( isTempStdQuantForSelectedItem(selStdQuantName, curStdQuant) ) {
					retStdQuant = curStdQuant; 			
				}
			}
		}
		return retStdQuant;
	}

	/**
	 * Adds the appropriate text for the passed MassSpecStandardQuant to the passed cmbSelectStandards.
	 * 
	 * @param stdQuant
	 * 		a MassSpecStandardQuant object
	 * @param cmbSelectStandards
	 * 		a selection Combo object to fill
	 * @param entryStandardQuant
	 * 		the standard quantifications associated with the current Entry
	 * @param storedPrefStandardQuant
	 * 		the standard quantifications associated with the project
	 */
	public static void setComboEntryForTempStdQuant(MassSpecStandardQuant stdQuant, Combo cmbSelectStandards,
			List<MassSpecStandardQuant> entryStandardQuant,
			List<MassSpecStandardQuant> storedPrefStandardQuant) {
		int inx = cmbSelectStandards.indexOf(stdQuant.getStandardQuantName());
		String sDisplayAs = "";
		if( inx == -1 ) {
			//			sDisplayAs = MassSpecStandardQuantApplyDialog.STANDARDQUANT_APPLIED + stdQuant.getStandardQuantName() + MassSpecStandardQuantApplyDialog.LOCAL_STANDARDQUANT_CURRENT_ENTRY;
			MassSpecStandardQuant entryStdQuant = null;
			if( entryStandardQuant != null ) {
				for( int i = 0; i < entryStandardQuant.size(); i++ ) {
					MassSpecStandardQuant curStdQuant = entryStandardQuant.get(i);
					if( stdQuant.equals(curStdQuant) ) {
						entryStdQuant = curStdQuant;
						break;
					}
				}
			}
			String sStartWith = MassSpecStandardQuantApplyDialog.STANDARDQUANT_DIRTY;
			if( entryStdQuant != null && ! stdQuant.differsFrom(entryStdQuant)) {
				sStartWith = MassSpecStandardQuantApplyDialog.STANDARDQUANT_APPLIED;
			}			
			sDisplayAs = sStartWith + stdQuant.getStandardQuantName() + MassSpecStandardQuantApplyDialog.LOCAL_STANDARDQUANT_CURRENT_ENTRY;
			cmbSelectStandards.add(sDisplayAs);
		} else {
			MassSpecStandardQuant prefStdQuant = null;
			if( storedPrefStandardQuant != null ) {
				for( int i = 0; i < storedPrefStandardQuant.size(); i++ ) {
					MassSpecStandardQuant curStdQuant = storedPrefStandardQuant.get(i);
					if( stdQuant.equals(curStdQuant) ) {
						prefStdQuant = curStdQuant;
						break;
					}
				}
			}
			MassSpecStandardQuant entryStdQuant = null;
			if( entryStandardQuant != null ) {
				for( int i = 0; i < entryStandardQuant.size(); i++ ) {
					MassSpecStandardQuant curStdQuant = entryStandardQuant.get(i);
					if( stdQuant.equals(curStdQuant) ) {
						entryStdQuant = curStdQuant;
						break;
					}
				}
			}

			String sStartWith = "";
			String sEndWith = "";
			if( entryStdQuant != null ) {
				sStartWith = MassSpecStandardQuantApplyDialog.STANDARDQUANT_APPLIED;
				if( entryStdQuant.differsFrom(stdQuant) ) {
					sStartWith = MassSpecStandardQuantApplyDialog.STANDARDQUANT_DIRTY;											
				}				
				if( prefStdQuant == null ) {
					sEndWith = MassSpecStandardQuantApplyDialog.LOCAL_STANDARDQUANT_CURRENT_ENTRY;
					if( entryStdQuant.differsFrom(stdQuant) ) {
						sEndWith = MassSpecStandardQuantApplyDialog.LOCAL_STANDARDQUANT_DIFFERS;
					}
				} else { 
					boolean bDiffersFromPref = entryStdQuant.differsFrom(prefStdQuant);
					boolean bDiffersFromLocal = entryStdQuant.differsFrom(stdQuant);
					if ( bDiffersFromPref && ! bDiffersFromLocal ) {
						sEndWith = MassSpecStandardQuantApplyDialog.LOCAL_STANDARDQUANT_DIFFERS;
					} else if ( bDiffersFromLocal ) {
						sEndWith = MassSpecStandardQuantApplyDialog.LOCAL_STANDARDQUANT_DIFFERS;
					}
				}
			} else {
				if( prefStdQuant == null ) {
					sEndWith = MassSpecStandardQuantApplyDialog.LOCAL_STANDARDQUANT_CURRENT_ENTRY;
				} else if( stdQuant.differsFrom(prefStdQuant)) {
					sEndWith = MassSpecStandardQuantApplyDialog.LOCAL_STANDARDQUANT_DIFFERS;
				}
			}

			sDisplayAs = sStartWith + stdQuant.getStandardQuantName() + sEndWith;									
			cmbSelectStandards.remove(inx);
			cmbSelectStandards.add( sDisplayAs );

		}
	}

	/**
	 * Initializes the standard quantification selection combo and populates it using the standard quantification sets
	 * from preferences and the current entry.
	 */
	public void initStoredStandardQuantList() {
		String sToSelect = "";
		if( cmbSelectStandards.getItems().length != 0 ) {
			cmbSelectStandards.removeAll();
		}
		cmbSelectStandards.add("");
		// first add the stored preferences
		if( getStoredStandardQuant() != null && getStoredStandardQuant().getStandardQuant() != null ) {
			for( int i = 0; i < getStoredStandardQuant().getStandardQuant().size(); i++ ) {
				MassSpecStandardQuant storedStdQuant = getStoredStandardQuant().getStandardQuant().get(i);
				cmbSelectStandards.add(storedStdQuant.getStandardQuantName());
				if( selStandards != null && storedStdQuant.getStandardQuantName().equals(selStandards.getStandardQuantName()) ) {
					int iItemCount = cmbSelectStandards.getItemCount();
					sToSelect = cmbSelectStandards.getItem(iItemCount-1);
				}
			}
		}
		if( getContextViewer() != null && getLocalStandardQuant() != null && getLocalStandardQuant().getStandardQuant() != null ) {
			for( int i = 0; i < getLocalStandardQuant().getStandardQuant().size(); i++ ) {
				MassSpecStandardQuant localStdQuant = getLocalStandardQuant().getStandardQuant().get(i);
				MassSpecStandardQuantApplyDialog.setComboEntryForTempStdQuant(localStdQuant, cmbSelectStandards, 
						entryStandardQuant.getStandardQuant(),
						storedStandardQuant.getStandardQuant());
				if( selStandards != null && localStdQuant.getStandardQuantName().equals(selStandards.getStandardQuantName()) ) {
					int iItemCount = cmbSelectStandards.getItemCount();
					sToSelect = cmbSelectStandards.getItem(iItemCount-1);
				}
			}
		}
		int iPrevSelInx = cmbSelectStandards.indexOf(sToSelect);
		cmbSelectStandards.select(iPrevSelInx);		
		processCmbSelectStandardsSelect();
	}

	/**
	 * Adds a button allowing the user to clear standard quantifications from an MS entry.
	 * 
	 * @param parent
	 * 		the control to add the Clear StandardQuant Button
	 */
	protected void addClearStandardQuant( Composite parent ) {
		GridData gd3 = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		btnClearStandardQuant = new Button(parent, SWT.NONE);
		btnClearStandardQuant.setText("Clear Standard Quant Peaks");
		btnClearStandardQuant.setLayoutData(gd3);	
		btnClearStandardQuant.setEnabled(false);
		btnClearStandardQuant.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseDown(MouseEvent e) {
				clearStdQuant();
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});
	}	

	/**
	 * If the currently selected item in the standard quantification combo is applied to the current entry, then the 
	 * data is removed from the MS entry and reloads the scan table.
	 * 
	 */
	protected void clearStdQuant() {
		MassSpecEntityProperty ep = (MassSpecEntityProperty) getMassSpecEntry().getProperty();
		MassSpecProperty prop = (MassSpecProperty) ep.getParentProperty();
		MassSpecMetaData msSettings = prop.getMassSpecMetaData();
		try {
			fileGrid.updateIntQuantFileSettings();
			if( msSettings.getStandardQuant().contains(selStandards) ) {
				msSettings.getStandardQuant().remove(selStandards);
			}
			String sFileName = prop.getFullyQualifiedMetaDataFileName(getMassSpecEntry());
			msSettings.updateStandardQuantData();
			prop.updateMSSettings(msSettings, sFileName);
			initLocalStandardQuant();
		} catch( Exception e ) {
			logger.error(e.getMessage(), e);
		}
		try {
			updateViewer(null);
			initStoredStandardQuantList();
		} catch (Exception e1) {
			logger.error(e1.getMessage(), e1);
		}		
	}

	/**
	 * To make sure that peaks removed from a standard quant peak set are removed from the archive, the list is first cleared
	 * and then the current set of peaks can be added.
	 */
	protected void clearBeforeReApply() {
		MassSpecEntityProperty ep = (MassSpecEntityProperty) getMassSpecEntry().getProperty();
		MassSpecProperty prop = (MassSpecProperty) ep.getParentProperty();
		MassSpecMetaData msSettings = prop.getMassSpecMetaData();
		if( msSettings.getStandardQuant().contains(selStandards) ) {
			try {
				msSettings.getStandardQuant().remove(selStandards);
				String sFileName = prop.getFullyQualifiedMetaDataFileName(getMassSpecEntry());				
				msSettings.updateStandardQuantData();
				prop.updateMSSettings(msSettings, sFileName);
				updateViewer(null);

			} catch (Exception e1) {
				logger.error(e1.getMessage(), e1);
			}		
		}
	}

	/**
	 * Applies the standard quantification data for the currently selected item to the current entry and reloads the scan table.
	 */
	protected void performStdQuant() {
		clearBeforeReApply();
		MassSpecEntityProperty ep = (MassSpecEntityProperty) getMassSpecEntry().getProperty();
		MassSpecProperty prop = (MassSpecProperty) ep.getParentProperty();
		MassSpecMetaData msSettings = prop.getMassSpecMetaData();
		try {
			fileGrid.updateIntQuantFileSettings();
			if( selStandards != null ) { // nice place to fix the possible scenario where a null std quant was added before..ugh
				msSettings.getStandardQuant().add(selStandards);
			}
			String sFileName = prop.getFullyQualifiedMetaDataFileName(getMassSpecEntry());				
			msSettings.updateStandardQuantData();
			prop.updateMSSettings(msSettings, sFileName);
			initLocalStandardQuant();
		} catch( Exception e ) {
			logger.error(e.getMessage(), e);
		}
		try {
			List<String> sKeyVals = getColumnKeyLabels();
			updateViewer(sKeyVals);
			initStoredStandardQuantList();
		} catch (Exception e1) {
			logger.error(e1.getMessage(), e1);
		}		
	}

	/**
	 * Adds a button allowing the user to add std quantification to an MS entry.
	 * 
	 * @param parent
	 * 		the control to add the Perform Standard Quantification Button
	 */
	private void addFilterItem( Composite parent ) {
		GridData gd3 = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		btnApplyFilter = new Button(parent, SWT.NONE);
		btnApplyFilter.setText("Apply Standard Quantification");
		btnApplyFilter.setLayoutData(gd3);	
		btnApplyFilter.setEnabled(false);
		btnApplyFilter.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseDown(MouseEvent e) {
				int iNumSel = fileGrid.getNumSelectedEntries();
				if( iNumSel == 0 ) { 
					clearStdQuant();
				} else {
					performStdQuant();
				}
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});
	}	

	protected MassSpecStandardQuantModifyDialog getNewQuantModifyDialog( Shell shell, MassSpecMultiPageViewer viewer ) {
		return new MassSpecStandardQuantModifyDialog(shell, viewer);
	}
	/**
	 * Opens the Modify Standard Quant Dialog so the user can add/edit lists of peaks to quantify.
	 */
	protected void modifyStandardQuant() {
		MassSpecMultiPageViewer curView = getCurrentViewer();
		if (curView == null)
			return;
		Entry msEntry = curView.getEntry();
		if( msEntry == null ) {
			return;
		}
		Property msProp = msEntry.getProperty();
		if( msProp instanceof MassSpecEntityProperty ) {
			if( modQuantWin == null ) {
				modQuantWin = getNewQuantModifyDialog(getShell(), curView);
				modQuantWin.setLocalStdQuant(getLocalStandardQuant());
				modQuantWin.setEntryStdQuant(getEntryStandardQuant());
				modQuantWin.setSelectedStdQuant(cmbSelectStandards.getText());
				modQuantWin.addListener(this);
				modQuantWin.open();
			} else {
				modQuantWin.getShell().forceActive();
			}
			return;
		} 		
	}

	/**
	 * Adds a button allowing the user to open the Modify Internal Standard Sets dialog.
	 * 
	 * @param parent
	 * 		the control to add the Modify Internal Standard Sets Button
	 */
	private void addModifyStdQuantItem( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		btnModifyList = new Button(parent, SWT.NONE);
		btnModifyList.setText("Modify Internal Standard Sets");
		btnModifyList.setLayoutData(gd1);		
		btnModifyList.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseDown(MouseEvent e) {
				modifyStandardQuant();
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});
	}

	protected MassSpecStandardQuantFileGrid getNewMassSpecStandardQuantFileGrid( Composite parent, MassSpecMultiPageViewer curView ) {
		return new MassSpecStandardQuantFileGrid(parent, curView);
	}

	/**
	 * Adds a button allowing the user to open the Standard Quant File dialog.
	 * 
	 * @param parent
	 * 		the control to add the Select Standard Quant File Button
	 */
	private void addSelectStdQuantFilesItem( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		MassSpecMultiPageViewer curView = getCurrentViewer();
		if (curView == null)
			return;
		Entry msEntry = curView.getEntry();
		if( msEntry == null ) {
			return;
		}
		Property msProp = msEntry.getProperty();
		if( msProp instanceof MassSpecEntityProperty ) {
			fileGrid = getNewMassSpecStandardQuantFileGrid(parent, curView);
			fileGrid.initializeGrid();
			fileGrid.setLayoutData(gd1);
			fileGrid.setEnabled(false);
		}
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
		return "Mass Spec Standard Quantification";
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
						MassSpecStandardQuantApplyDialog.PROPERTY_WIN_CLOSED, Boolean.FALSE, Boolean.TRUE));
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
		// Initializes the standard quantification lists based on the action of interfaces that this dialog is listening to.
		if( event.getSource() instanceof MassSpecStandardQuantModifyDialog ) {
			if( event.getProperty().equals( MassSpecStandardQuantModifyDialog.PROPERTY_LOCAL_CHANGE ) ) {
				initStoredStandardQuantList();
			} else if( event.getProperty().equals( MassSpecStandardQuantModifyDialog.PROPERTY_PREFERENCE_CHANGE ) ) {
				initStoredStandardQuant();
				initStoredStandardQuantList();
			} else if( event.getProperty().equals( MassSpecStandardQuantModifyDialog.PROPERTY_CLOSE ) ) {
				this.modQuantWin = null;
				this.selQuantFileWin = null;
			}
		}
	}

	/**
	 * Listener for changes in current entry. If changed, then the list of standard quantifications must be reloaded.
	 */
	public class StandardQuantPaintListener implements PaintListener {
		MassSpecStandardQuantApplyDialog parent = null;
		public StandardQuantPaintListener(MassSpecStandardQuantApplyDialog parent) {
			this.parent = parent;
		}
		@Override
		public void paintControl(PaintEvent e) {
			Entry entry = getEntryForCurrentViewer();
			if( entry != null && ! entry.equals( parent.getMassSpecEntry() )) {
				parent.setMassSpecEntry( entry );
				initLocalStandardQuant();
				parent.initStoredStandardQuantList();
			}
		}
	}

	/**
	 * Determines the current viewer and refreshes the GRITS tables that were updated with the quantitation.
	 * For MassSpec data, this is only the scans tab. It then attempts to move the columns containing the specified key
	 * values to the beginning of the table.
	 * 
	 * @param sKeyVals, list of key values to move to beginning of the table
	 */
	protected void updateViewer(List<String> sKeyVals) {
		try {
			MassSpecMultiPageViewer viewer = getCurrentViewer();
			viewer.reLoadScansTab(sKeyVals);			
		} catch( Exception e ) {
			logger.error(e.getMessage(), e);			
		}
	}

	@Override
	public List<String> getColumnKeyLabels() {
		if( selStandards == null ) {
			return null;
		}
		List<String> keyVals = new ArrayList<>();
		if( fileGrid != null && fileGrid.getSelectedQuantFileAliases() != null && 
				selStandards.getStandardQuantPeaks() != null ) {
			for( String sFileName : fileGrid.getSelectedQuantFileAliases().keySet() ) {
				String sFileAlias = fileGrid.getSelectedQuantFileAliases().get(sFileName);
				String sColPrefix = selStandards.getStandardQuantName() + " : " + sFileAlias;
				for( MassSpecStandardQuantPeak peak : selStandards.getStandardQuantPeaks().values() ) {
					keyVals.add(sColPrefix + "-" + Double.toString(peak.getPeakMz()) );
				}
			}
		}
		return keyVals;
	}

}
