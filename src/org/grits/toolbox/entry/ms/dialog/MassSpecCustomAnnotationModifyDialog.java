package org.grits.toolbox.entry.ms.dialog;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.entry.ms.preference.IMSPreferenceWithCustomAnnotation;
import org.grits.toolbox.entry.ms.preference.MassSpecCustomAnnotationsPreferenceUI;
import org.grits.toolbox.entry.ms.views.tabbed.MassSpecMultiPageViewer;

/**
 * User interface for modifying the custom annotations available for Mass Spec data. This window is called from the 
 * MassSpecCustomAnnnotationDialog when the user wants to modify the annotated peaks for an open window. This dialog
 * embeds an instance of MassSpecCustomAnnotationsPreferenceUI in order to manipulate the persistent custom annotation
 * information.
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 *
 * @see MassSpecCustomAnnotationDialog
 * @see MassSpecCustomAnnotationsPreferenceUI
 */
public class MassSpecCustomAnnotationModifyDialog extends MassSpecViewerDialog implements IPropertyChangeListener {
	private static final Logger logger = Logger.getLogger(MassSpecCustomAnnotationModifyDialog.class);
	protected final Font boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT); 
	private boolean paramsOk = false;
	private String sSelectedAnnotation = null;
	
	private MassSpecCustomAnnotationsPreferenceUI customAnnotUI = null;
	protected IMSPreferenceWithCustomAnnotation localAnnotations = null;
	protected IMSPreferenceWithCustomAnnotation entryAnnotations = null;

	public final static String PROPERTY_LOCAL_CHANGE = "Local changes";
	public final static String PROPERTY_PREFERENCE_CHANGE = "Apply pressed";
	public final static String PROPERTY_CLOSE = "Window Closed";
	
	public MassSpecCustomAnnotationModifyDialog(Shell parentShell, MassSpecMultiPageViewer contextViewer) {
		super(parentShell, contextViewer);
		setShellStyle(SWT.MODELESS | SWT.RESIZE | SWT.DIALOG_TRIM | SWT.ON_TOP);
	}
	
	/**
	 * @param preferences
	 * 		a working copy of the annotation information for the currently open Entry which may become dirty.
	 */
	public void setLocalAnnotations(IMSPreferenceWithCustomAnnotation preferences) {
		this.localAnnotations = preferences;
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
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		
		customAnnotUI.setSelectedItem(getSelectedAnnotation());

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
		return "Modify Mass Spec Custom Annotations";
	}

	/**
	 * @param container
	 * 		the container to add the new preference UI to
	 * @return a newly instantiated MassSpecCustomAnnotationsPreferenceUI
	 */
	protected MassSpecCustomAnnotationsPreferenceUI getNewMassSpecCustomAnnotationsPreferenceUI(Composite container) {
		return new MassSpecCustomAnnotationsPreferenceUI(container, SWT.BORDER, this, true);
	}	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(getFormTitle());
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		//find the center of a main monitor
		Monitor primary = getShell().getDisplay().getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = getShell().getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		getShell().setLocation(x, y);
		container.setLayout(new FillLayout());

		customAnnotUI = getNewMassSpecCustomAnnotationsPreferenceUI(container);
		customAnnotUI.setContextViewer(getContextViewer());
		customAnnotUI.setLocalAnnotations(getLocalAnnotations());
		customAnnotUI.setEntryAnnotations(getEntryAnnotations());
		customAnnotUI.setEntry(getMassSpecEntry());
		customAnnotUI.initComponents();
		return area;
	}

	/**
	 * @return the currently selected annotation
	 */
	public String getSelectedAnnotation() {
		return sSelectedAnnotation;
	}
	
	/**
	 * @param sSelectedAnnotation
	 * 		the name of a custom annotation that should be selected by default
	 */
	public void setSelectedAnnotation(String sSelectedAnnotation) {
		this.sSelectedAnnotation = sSelectedAnnotation;
	}
	
	/**
	 * Validates the input on the dialog to make sure all settings comply with a CustomAnnotation object
	 */
	public void validateInput(){
		if ( paramsOk ) {
			setErrorMessage(null);
			getButton(IDialogConstants.OK_ID).setEnabled(true);
			getButton(IDialogConstants.FINISH_ID).setEnabled(true);

		} else {
			setErrorMessage(customAnnotUI.getErrorMessage());
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			getButton(IDialogConstants.FINISH_ID).setEnabled(false);
		}

	}
		
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if( event.getProperty().equals(MassSpecCustomAnnotationsPreferenceUI.PAGE_COMPLETE_PROPERTY) ) {
			if( event.getNewValue() instanceof Boolean ) {
				paramsOk = ((Boolean) event.getNewValue()).booleanValue();
				validateInput();
			}
		}
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.FINISH_ID, "Apply",
				false);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if( IDialogConstants.FINISH_ID == buttonId ) {
			applyPressed();
		} else {
			super.buttonPressed(buttonId);
		}
	}
	
	/**
	 * Updates the MS Settings and the MS Preferences objects
	 */
	private void apply() {
		try {
			customAnnotUI.updateSettings();
			customAnnotUI.updatePreferences();
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
	}
	
	/**
	 * Saves the preferences, if desired, and notifies all listeners
	 */
	protected void applyPressed() {
		apply();
		if( customAnnotUI.getSaveAsDefault() ) {
			customAnnotUI.save();	
		}
		if ( getListeners() != null ) {
			for( IPropertyChangeListener listener : getListeners() ) {
				if( ! customAnnotUI.getSaveAsDefault() ) {
					listener.propertyChange(new PropertyChangeEvent(this, PROPERTY_LOCAL_CHANGE, Boolean.FALSE, Boolean.TRUE));
				} else {
					listener.propertyChange(new PropertyChangeEvent(this, PROPERTY_PREFERENCE_CHANGE, Boolean.FALSE, Boolean.TRUE));					
				}
			}
		}
		customAnnotUI.initStoredAnnotationsList();
		customAnnotUI.setResetEnabled();
	}
	
	@Override
	protected void okPressed() {
		applyPressed();
		super.okPressed();
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
	
}
