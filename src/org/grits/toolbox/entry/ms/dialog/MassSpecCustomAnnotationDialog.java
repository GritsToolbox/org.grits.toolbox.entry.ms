package org.grits.toolbox.entry.ms.dialog;

import java.util.ArrayList;
import java.util.List;

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
import org.grits.toolbox.entry.ms.preference.IMSPreferenceWithCustomAnnotation;
import org.grits.toolbox.entry.ms.preference.MassSpecCustomAnnotationsPreferenceUI;
import org.grits.toolbox.entry.ms.preference.MassSpecPreference;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecCustomAnnotation;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecCustomAnnotationPeak;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecMetaData;
import org.grits.toolbox.entry.ms.views.tabbed.MassSpecMultiPageViewer;

/**
 * Simple pop-up dialog accessed from a Mass Spec GRITS table that allows the user to annotate or 
 * clear annotations for the current entry. It also allows the user to open the Modify Annotations Dialog.
 * Note that the combo box w/ available custom annotations updates when the user changes windows.
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 *
 */
public class MassSpecCustomAnnotationDialog extends MassSpecViewerDialog implements IPropertyChangeListener, IDynamicTableProcessor {
	private static final Logger logger = Logger.getLogger(MassSpecCustomAnnotationDialog.class);
	protected final Font boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT); 
	protected Text txtOutput;
	protected static final String PARAMS_OK = "Valid parameters";

	protected GridLayout gridLayout = null;

	protected Label lblSelectAnnotation = null;
	protected Combo cmbSelectAnnotation = null;
	protected MassSpecCustomAnnotation selAnnot = null;

	protected Button btnApplyFilter = null;
	protected Button btnModifyList = null;
	protected Button btnClearCustomAnnotations = null;

	protected IMSPreferenceWithCustomAnnotation storedAnnotations = null; // persistent copy, won't reflect changes until after save
	protected IMSPreferenceWithCustomAnnotation localAnnotations = null; // local copy, can get updated if the user modifies preferences but doesn't save
	protected IMSPreferenceWithCustomAnnotation entryAnnotations = null; // what is applied to the current entry
	protected MassSpecCustomAnnotationModifyDialog win = null;

	public final static String PROPERTY_WIN_CLOSED = "Window Closed";

	public final static String ANNOTATION_APPLIED = "# ";
	public final static String ANNOTATION_DIRTY = "* ";
	public final static String LOCAL_ANNOTATION_CURRENT_ENTRY = " (current entry only)";
	public final static String LOCAL_ANNOTATION_DIFFERS = " (differs from preferences)";

	public MassSpecCustomAnnotationDialog(Shell parentShell, MassSpecMultiPageViewer contextViewer) {
		super(parentShell, contextViewer);
		setShellStyle(SWT.MODELESS | SWT.DIALOG_TRIM | SWT.ON_TOP);
		initStoredAnnotations();
	}

	/**
	 * Initializes the 2 MassSpecPreference objects according to what is stored in the MS Metadata for this entry.
	 */
	protected void initLocalAnnotations() {
		localAnnotations = new MassSpecPreference();
		MassSpecCustomAnnotationsPreferenceUI.initAnnotationFromEntry(getMassSpecEntry(), localAnnotations);
		entryAnnotations = new MassSpecPreference();
		MassSpecCustomAnnotationsPreferenceUI.initAnnotationFromEntry(getMassSpecEntry(), entryAnnotations);
	}	

	/**
	 * @param localAnnotations
	 * 		a working copy of the annotation information for the currently open Entry which may become dirty.
	 */
	public void setLocalAnnotations(IMSPreferenceWithCustomAnnotation localAnnotations) {
		this.localAnnotations = localAnnotations;
	}

	/**
	 * @return the working copy of the entry's annotation information.
	 */
	public IMSPreferenceWithCustomAnnotation getLocalAnnotations() {
		return localAnnotations;
	}

	/**
	 * @return the read-only copy of the annotation information for the currently open Entry
	 */
	public IMSPreferenceWithCustomAnnotation getEntryAnnotations() {
		return entryAnnotations;
	}

	/**
	 * @param entryAnnotations
	 * 		a read-only copy of the annotation information for the currently open Entry 
	 */
	public void setEntryAnnotations(IMSPreferenceWithCustomAnnotation entryAnnotations) {
		this.entryAnnotations = entryAnnotations;
	}

	/**
	 * Initializes the MassSpecPreference object according to what is stored in the workspace preferences
	 */
	public void initStoredAnnotations() {
		storedAnnotations = MassSpecCustomAnnotationsPreferenceUI.loadWorkspacePreferences();				
	}

	/**
	 * @return the read-only copy of the annotation information for the workspace preferences
	 */
	public IMSPreferenceWithCustomAnnotation getStoredAnnotations() {
		return storedAnnotations;
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
	 * Adds the Label and Combo for the available MS Custom Annotations to the parent control.
	 * 
	 * @param parent
	 */
	private void addSelectAnnotationItem( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		lblSelectAnnotation = new Label(parent, SWT.NONE);
		lblSelectAnnotation.setText("Current Custom Annotations");
		lblSelectAnnotation.setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		cmbSelectAnnotation = new Combo(parent, SWT.NONE);
		cmbSelectAnnotation.setLayoutData(gd2);
		initLocalAnnotations();
		initStoredAnnotationsList();
		cmbSelectAnnotation.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean bVal = false;
				if( ! cmbSelectAnnotation.getText().trim().equals("") ) {
					setCurrentAnnotationValues(cmbSelectAnnotation.getText());
					bVal = true;
				}
				btnClearCustomAnnotations.setEnabled(bVal);
				btnApplyFilter.setEnabled(bVal);
				//				btnModifyList.setEnabled(bVal);				
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
	 * @param selAnnotName
	 * 		the text from the Custom Annotation selection combo
	 * @return true if there were changes to the local entry annotations
	 */
	protected boolean hasLocalChanges(String selAnnotName) {
		return selAnnotName.endsWith(MassSpecCustomAnnotationDialog.LOCAL_ANNOTATION_CURRENT_ENTRY) ||
				selAnnotName.endsWith(MassSpecCustomAnnotationDialog.LOCAL_ANNOTATION_DIFFERS);
	}

	/**
	 * Sets the "selAnnot" member variable based on whether there are local changes or not.
	 * 
	 * @param selAnnotName
	 * 		the text from the selection combo control
	 */
	protected void setCurrentAnnotationValues(String selAnnotName) {
		selAnnot = null;
		if( selAnnotName == null ) {
			return;
		}
		MassSpecCustomAnnotation prefAnnot = MassSpecCustomAnnotationDialog.getCurrentAnnot(selAnnotName, getStoredAnnotations());
		MassSpecCustomAnnotation localAnnot = MassSpecCustomAnnotationDialog.getCurrentAnnot(selAnnotName, getLocalAnnotations());
		if( selAnnotName.endsWith(MassSpecCustomAnnotationDialog.LOCAL_ANNOTATION_CURRENT_ENTRY) ||
				selAnnotName.endsWith(MassSpecCustomAnnotationDialog.LOCAL_ANNOTATION_DIFFERS)) {
			selAnnot = localAnnot;
		} else {
			selAnnot = prefAnnot;
		}
	}

	/**
	 * @param selAnnotName
	 * 		the text from the selection combo control
	 * @param curAnnot
	 * 		a candidate MassSpecCustomAnnotation object
	 * @return true if the text in the selection combo item is associated with the passed annotation object
	 */
	public static boolean isTempAnnotationForSelectedItem(String selAnnotName, MassSpecCustomAnnotation curAnnot) {
		if( curAnnot.getAnnotationName().equals(selAnnotName)) {
			return true;
		}
		String sCompareTo = curAnnot.getAnnotationName() + MassSpecCustomAnnotationDialog.LOCAL_ANNOTATION_DIFFERS;;
		if( sCompareTo.equals(selAnnotName) ){
			return true;
		}
		sCompareTo = MassSpecCustomAnnotationDialog.ANNOTATION_APPLIED + curAnnot.getAnnotationName();
		if( sCompareTo.equals(selAnnotName) ){
			return true;
		}
		sCompareTo = MassSpecCustomAnnotationDialog.ANNOTATION_APPLIED + curAnnot.getAnnotationName() + MassSpecCustomAnnotationDialog.LOCAL_ANNOTATION_CURRENT_ENTRY;
		if( sCompareTo.equals(selAnnotName) ){
			return true;
		}
		sCompareTo = MassSpecCustomAnnotationDialog.ANNOTATION_APPLIED + curAnnot.getAnnotationName() + MassSpecCustomAnnotationDialog.LOCAL_ANNOTATION_DIFFERS;
		if( sCompareTo.equals(selAnnotName) ){
			return true;
		}
		sCompareTo = MassSpecCustomAnnotationDialog.ANNOTATION_DIRTY + curAnnot.getAnnotationName();
		if( sCompareTo.equals(selAnnotName) ){
			return true;
		}
		sCompareTo = MassSpecCustomAnnotationDialog.ANNOTATION_DIRTY + curAnnot.getAnnotationName() + MassSpecCustomAnnotationDialog.LOCAL_ANNOTATION_CURRENT_ENTRY;
		if( sCompareTo.equals(selAnnotName) ){
			return true;
		}
		sCompareTo = MassSpecCustomAnnotationDialog.ANNOTATION_DIRTY + curAnnot.getAnnotationName() + MassSpecCustomAnnotationDialog.LOCAL_ANNOTATION_DIFFERS;
		if( sCompareTo.equals(selAnnotName) ){
			return true;
		}
		return false;
	}

	/**
	 * @param selAnnotName
	 * 		the text from the selection combo control
	 * @param preferences
	 * 		a candidate custom annotation preference object
	 * @return
	 */
	public static MassSpecCustomAnnotation getCurrentAnnot(String selAnnotName, 
			IMSPreferenceWithCustomAnnotation preferences) {
		MassSpecCustomAnnotation retAnnot = null;
		if( preferences != null && preferences.getCustomAnnotations() != null ) {
			for( int i = 0; i < preferences.getCustomAnnotations().size(); i++ ) {
				MassSpecCustomAnnotation curAnnot =  preferences.getCustomAnnotations().get(i);
				if( curAnnot == null ) {
					// this is an error! remove it?
					continue;
				}
				if( isTempAnnotationForSelectedItem(selAnnotName, curAnnot) ) {
					retAnnot = curAnnot; 			
				}
			}
		}
		return retAnnot;
	}

	/**
	 * Adds the appropriate text for the passed MassSpecCustomAnnotation to the passed cmbSelectAnnotation.
	 * 
	 * @param annot
	 * 		a MassSpecCustomAnnotation object
	 * @param cmbSelectAnnotation
	 * 		a selection Combo object to fill
	 * @param entryAnnotations
	 * 		the annotations associated with the current Entry
	 * @param storedPrefAnnotations
	 * 		the annotations associated with the project
	 */
	public static void setComboEntryForTempAnnotation(MassSpecCustomAnnotation annot, Combo cmbSelectAnnotation,
			List<MassSpecCustomAnnotation> entryAnnotations,
			List<MassSpecCustomAnnotation> storedPrefAnnotations) {
		int inx = cmbSelectAnnotation.indexOf(annot.getAnnotationName());
		String sDisplayAs = "";
		if( inx == -1 ) {
//			sDisplayAs = MassSpecCustomAnnotationDialog.ANNOTATION_APPLIED + annot.getAnnotationName() + MassSpecCustomAnnotationDialog.LOCAL_ANNOTATION_CURRENT_ENTRY;
			MassSpecCustomAnnotation entryCA = null;
			if( entryAnnotations != null ) {
				for( int i = 0; i < entryAnnotations.size(); i++ ) {
					MassSpecCustomAnnotation curCA = entryAnnotations.get(i);
					if( annot.equals(curCA) ) {
						entryCA = curCA;
						break;
					}
				}
			}
			String sStartWith = MassSpecCustomAnnotationDialog.ANNOTATION_DIRTY;
			if( entryCA != null && ! annot.differsFrom(entryCA)) {
				sStartWith = MassSpecCustomAnnotationDialog.ANNOTATION_APPLIED;
			}			
			sDisplayAs = sStartWith + annot.getAnnotationName() + MassSpecCustomAnnotationDialog.LOCAL_ANNOTATION_CURRENT_ENTRY;
			cmbSelectAnnotation.add(sDisplayAs);
		} else {
			MassSpecCustomAnnotation prefAnnot = null;
			if( storedPrefAnnotations != null ) {
				for( int i = 0; i < storedPrefAnnotations.size(); i++ ) {
					MassSpecCustomAnnotation curAnnot = storedPrefAnnotations.get(i);
					if( annot.equals(curAnnot) ) {
						prefAnnot = curAnnot;
						break;
					}
				}
			}
			MassSpecCustomAnnotation entryAnnot = null;
			if( entryAnnotations != null ) {
				for( int i = 0; i < entryAnnotations.size(); i++ ) {
					MassSpecCustomAnnotation curAnnot = entryAnnotations.get(i);
					if( annot.equals(curAnnot) ) {
						entryAnnot = curAnnot;
						break;
					}
				}
			}

			String sStartWith = "";
			String sEndWith = "";
			if( entryAnnot != null ) {
				sStartWith = MassSpecCustomAnnotationDialog.ANNOTATION_APPLIED;
				if( entryAnnot.differsFrom(annot) ) {
					sStartWith = MassSpecCustomAnnotationDialog.ANNOTATION_DIRTY;											
				}				
				if( prefAnnot == null ) {
					sEndWith = MassSpecCustomAnnotationDialog.LOCAL_ANNOTATION_CURRENT_ENTRY;
					if( entryAnnot.differsFrom(annot) ) {
						sEndWith = MassSpecCustomAnnotationDialog.LOCAL_ANNOTATION_DIFFERS;
					}
				} else { 
					boolean bDiffersFromPref = entryAnnot.differsFrom(prefAnnot);
					boolean bDiffersFromLocal = entryAnnot.differsFrom(annot);
					if ( bDiffersFromPref && ! bDiffersFromLocal ) {
						sEndWith = MassSpecCustomAnnotationDialog.LOCAL_ANNOTATION_DIFFERS;
					} else if ( bDiffersFromLocal ) {
						sEndWith = MassSpecCustomAnnotationDialog.LOCAL_ANNOTATION_DIFFERS;
					}
				}
			} else {
				if( prefAnnot == null ) {
					sEndWith = MassSpecCustomAnnotationDialog.LOCAL_ANNOTATION_CURRENT_ENTRY;
				} else if( annot.differsFrom(prefAnnot)) {
					sEndWith = MassSpecCustomAnnotationDialog.LOCAL_ANNOTATION_DIFFERS;
				}
			}

			sDisplayAs = sStartWith + annot.getAnnotationName() + sEndWith;									
			cmbSelectAnnotation.remove(inx);
			cmbSelectAnnotation.add( sDisplayAs );

		}
	}

	/**
	 * Initializes the custom annotation selection combo and populates it using the annotations from preferences and the current entry.
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
				if( selAnnot != null && annot.getAnnotationName().equals(selAnnot.getAnnotationName()) ) {
					int iItemCount = cmbSelectAnnotation.getItemCount();
					sToSelect = cmbSelectAnnotation.getItem(iItemCount-1);
				}
			}
		}
		if( getContextViewer() != null && getLocalAnnotations() != null && getLocalAnnotations().getCustomAnnotations() != null ) {
			for( int i = 0; i < getLocalAnnotations().getCustomAnnotations().size(); i++ ) {
				MassSpecCustomAnnotation annot = getLocalAnnotations().getCustomAnnotations().get(i);
				MassSpecCustomAnnotationDialog.setComboEntryForTempAnnotation(annot, cmbSelectAnnotation, 
						entryAnnotations.getCustomAnnotations(),
						storedAnnotations.getCustomAnnotations());
				if( selAnnot != null && annot.getAnnotationName().equals(selAnnot.getAnnotationName()) ) {
					int iItemCount = cmbSelectAnnotation.getItemCount();
					sToSelect = cmbSelectAnnotation.getItem(iItemCount-1);
				}
			}
		}
		int iPrevSelInx = cmbSelectAnnotation.indexOf(sToSelect);
		cmbSelectAnnotation.select(iPrevSelInx);		
	}

	/**
	 * If the currently selected item in the custom annotation combo is applied to the current entry, then the 
	 * data is removed from the MS entry and reloads the scan table.
	 * 
	 */
	protected void clearAnnotation() {
		MassSpecEntityProperty ep = (MassSpecEntityProperty) getMassSpecEntry().getProperty();
		MassSpecProperty prop = (MassSpecProperty) ep.getParentProperty();
		MassSpecMetaData msSettings = prop.getMassSpecMetaData();
		setCurrentAnnotationValues( cmbSelectAnnotation.getText().trim() );
		try {
			String sFileName = prop.getFullyQualifiedMetaDataFileName(getMassSpecEntry());
			if( msSettings.getCustomAnnotations().contains(selAnnot) ) {
				msSettings.getCustomAnnotations().remove(selAnnot);
			}
			msSettings.updateCustomAnotationData();
			prop.updateMSSettings(msSettings, sFileName);
			initLocalAnnotations();
		} catch( Exception e ) {
			logger.error(e.getMessage(), e);
		}
		try {
			updateViewer(null);
			initStoredAnnotationsList();
		} catch (Exception e1) {
			logger.error(e1.getMessage(), e1);
		}		
	}

	/**
	 * Adds a button allowing the user to clear annotations from an MS entry.
	 * 
	 * @param parent
	 * 		the control to add the Clear Annotations Button
	 */
	protected void addClearAnnotations( Composite parent ) {
		GridData gd3 = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		btnClearCustomAnnotations = new Button(parent, SWT.NONE);
		btnClearCustomAnnotations.setText("Clear Annotated Peaks");
		btnClearCustomAnnotations.setLayoutData(gd3);	
		btnClearCustomAnnotations.setEnabled(false);
		btnClearCustomAnnotations.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseDown(MouseEvent e) {
				clearAnnotation();
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});
	}	

	/**
	 * Applies the custom annotation data for the currently selected item to the current entry and reloads the scan table.
	 */
	protected void performAnnotation() {
		MassSpecEntityProperty ep = (MassSpecEntityProperty) getMassSpecEntry().getProperty();
		MassSpecProperty prop = (MassSpecProperty) ep.getParentProperty();
		MassSpecMetaData msSettings = prop.getMassSpecMetaData();
		setCurrentAnnotationValues( cmbSelectAnnotation.getText() );
		try {
			if( msSettings.getCustomAnnotations().contains(selAnnot) ) {
				msSettings.getCustomAnnotations().remove(selAnnot);
			}
			if( selAnnot != null ) { // nice place to fix the possible scenario where a null annotation was added before..ugh
				msSettings.getCustomAnnotations().add(selAnnot);
			}
			String sFileName = prop.getFullyQualifiedMetaDataFileName(getMassSpecEntry());				
			msSettings.updateCustomAnotationData();
			prop.updateMSSettings(msSettings, sFileName);
			initLocalAnnotations();
		} catch( Exception e ) {
			logger.error(e.getMessage(), e);
		}
//		MassSpecMultiPageViewer viewer = getCurrentViewer();
		try {
			List<String> sKeyVals = getColumnKeyLabels();
//			viewer.reLoadScansTab(sKeyVals);
			updateViewer(sKeyVals);
			initStoredAnnotationsList();
		} catch (Exception e1) {
			logger.error(e1.getMessage(), e1);
		}		
	}

	/**
	 * Adds a button allowing the user to add annotations to an MS entry.
	 * 
	 * @param parent
	 * 		the control to add the Perform Annotation Button
	 */
	private void addFilterItem( Composite parent ) {
		GridData gd3 = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		btnApplyFilter = new Button(parent, SWT.NONE);
		btnApplyFilter.setText("Annotate Peaks");
		btnApplyFilter.setLayoutData(gd3);	
		btnApplyFilter.setEnabled(false);
		btnApplyFilter.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseDown(MouseEvent e) {
				performAnnotation();
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});
	}	

	/**
	 * Opens the Modify Annotations Dialog so the user can add/edit lists of peaks to annotate.
	 */
	protected void modifyAnnotations() {
		MassSpecMultiPageViewer curView = getCurrentViewer();
		if (curView == null)
			return;
		Entry msEntry = curView.getEntry();
		if( msEntry == null ) {
			return;
		}
		Property msProp = msEntry.getProperty();
		if( msProp instanceof MassSpecEntityProperty ) {
			if( win == null ) {
				win = new MassSpecCustomAnnotationModifyDialog(getShell(), curView);
				win.setLocalAnnotations(getLocalAnnotations());
				win.setEntryAnnotations(getEntryAnnotations());
				win.setSelectedAnnotation(cmbSelectAnnotation.getText());
				win.addListener(this);
				win.open();
			} else {
				win.getShell().forceActive();
			}
			return;
		} 		
	}

	/**
	 * Adds a button allowing the user to open the Modify Custom Annotations dialog.
	 * 
	 * @param parent
	 * 		the control to add the Modify Custom Annotations Button
	 */
	private void addAdvancedItem( Composite parent ) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		btnModifyList = new Button(parent, SWT.NONE);
		btnModifyList.setText("Modify Annotations");
		btnModifyList.setLayoutData(gd1);		
		btnModifyList.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseDown(MouseEvent e) {
				modifyAnnotations();
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});
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
		return "Mass Spec Custom Annotation";
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
		addSelectAnnotationItem(container);
		addClearAnnotations(container);
		addFilterItem(container);
		addAdvancedItem(container);
		container.addPaintListener(new CustomAnnotationPaintListener(this));
		return area;
	}

	protected String getAnnotationLabelText() {
		return "MS Annotation";
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
						MassSpecCustomAnnotationDialog.PROPERTY_WIN_CLOSED, Boolean.FALSE, Boolean.TRUE));
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
		// Initializes the custom annotation lists based on the action of interfaces that this dialog is listening to.
		if( event.getSource() instanceof MassSpecCustomAnnotationModifyDialog ) {
			if( event.getProperty().equals( MassSpecCustomAnnotationModifyDialog.PROPERTY_LOCAL_CHANGE ) ) {
				initStoredAnnotationsList();
			} else if( event.getProperty().equals( MassSpecCustomAnnotationModifyDialog.PROPERTY_PREFERENCE_CHANGE ) ) {
				initStoredAnnotations();
				initStoredAnnotationsList();
			} else if( event.getProperty().equals( MassSpecCustomAnnotationModifyDialog.PROPERTY_CLOSE ) ) {
				win = null;
			}
		}
	}

	/**
	 * Listener for changes in current entry. If changed, then the list of annotations must be reloaded.
	 */
	public class CustomAnnotationPaintListener implements PaintListener {
		MassSpecCustomAnnotationDialog parent = null;
		public CustomAnnotationPaintListener(MassSpecCustomAnnotationDialog parent) {
			this.parent = parent;
		}
		@Override
		public void paintControl(PaintEvent e) {
			Entry entry = getEntryForCurrentViewer();
			if( entry != null && ! entry.equals( parent.getMassSpecEntry() )) {
				parent.setMassSpecEntry( entry );
				initLocalAnnotations();
				parent.initStoredAnnotationsList();
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
		if( selAnnot == null ) {
			return null;
		}
		List<String> keyVals = new ArrayList<>();
		if( selAnnot != null && selAnnot.getAnnotatedPeaks() != null ) {
			for( MassSpecCustomAnnotationPeak peak : selAnnot.getAnnotatedPeaks().values() ) {
				keyVals.add(Double.toString(peak.getPeakMz()));
			}
		}
		return keyVals;
	}
	
}
