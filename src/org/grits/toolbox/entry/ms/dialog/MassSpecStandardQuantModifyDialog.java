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
import org.grits.toolbox.entry.ms.preference.IMSPreferenceWithStandardQuant;
import org.grits.toolbox.entry.ms.preference.MassSpecStandardQuantPreferenceUI;
import org.grits.toolbox.entry.ms.views.tabbed.MassSpecMultiPageViewer;

/**
 * User interface for modifying the standard quantifications available for Mass Spec data. This window is called from the 
 * MassSpecStandardQuantApplyDialog when the user wants to modify the quantified peaks for an open window. This dialog
 * embeds an instance of MassSpecStandardQuantsPreferenceUI in order to manipulate the persistent standard quantification
 * information.
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 *
 * @see MassSpecStandardQuantApplyDialog
 * @see MassSpecStandardQuantPreferenceUI
 */
public class MassSpecStandardQuantModifyDialog extends MassSpecViewerDialog implements IPropertyChangeListener {
	private static final Logger logger = Logger.getLogger(MassSpecStandardQuantModifyDialog.class);
	protected final Font boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT); 
	private boolean paramsOk = false;
	private String sSelectedStdQuant = null;
	
	private MassSpecStandardQuantPreferenceUI stdQuantUI = null;
	protected IMSPreferenceWithStandardQuant localStdQuant = null;
	protected IMSPreferenceWithStandardQuant entryStdQuant = null;

	public final static String PROPERTY_LOCAL_CHANGE = "Local changes";
	public final static String PROPERTY_PREFERENCE_CHANGE = "Apply pressed";
	public final static String PROPERTY_CLOSE = "Window Closed";
	
	public MassSpecStandardQuantModifyDialog(Shell parentShell, MassSpecMultiPageViewer contextViewer) {
		super(parentShell, contextViewer);
		setShellStyle(SWT.MODELESS | SWT.RESIZE | SWT.DIALOG_TRIM | SWT.ON_TOP);
	}
	
	/**
	 * @param preferences
	 * 		a working copy of the standard quantification information for the currently open Entry which may become dirty.
	 */
	public void setLocalStdQuant(IMSPreferenceWithStandardQuant preferences) {
		this.localStdQuant = preferences;
	}
	
	/**
	 * @return the working copy of the entry's standard quantification information.
	 */
	public IMSPreferenceWithStandardQuant getLocalStdQuant() {
		return localStdQuant;
	}
	
	/**
	 * @return the read-only copy of the standard quantification information for the currently open Entry
	 */
	public IMSPreferenceWithStandardQuant getEntryStdQuant() {
		return entryStdQuant;
	}
	
	/**
	 * @param entryStdQuant
	 * 		a read-only copy of the standard quantification information for the currently open Entry
	 */
	public void setEntryStdQuant(IMSPreferenceWithStandardQuant entryStdQuant) {
		this.entryStdQuant = entryStdQuant;
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
		
		stdQuantUI.setSelectedItem(getSelectedStdQuant());

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
		return "Modify Mass Spec Standard Quantification";
	}

	/**
	 * @param container
	 * 		the container to add the new preference UI to
	 * @return a newly instantiated MassSpecStandardQuantPreferenceUI
	 */
	protected MassSpecStandardQuantPreferenceUI getNewMassSpecStandardQuantPreferenceUI(Composite container) {
		return new MassSpecStandardQuantPreferenceUI(container, SWT.BORDER, this, true);
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

		stdQuantUI = getNewMassSpecStandardQuantPreferenceUI(container);
		stdQuantUI.setContextViewer(getContextViewer());
		stdQuantUI.setLocalStandardQuant(getLocalStdQuant());
		stdQuantUI.setEntryStandardQuant(getEntryStdQuant());
		stdQuantUI.setEntry(getMassSpecEntry());
		stdQuantUI.initComponents();
		return area;
	}

	/**
	 * @return the currently selected standard quantification
	 */
	public String getSelectedStdQuant() {
		return sSelectedStdQuant;
	}
	
	/**
	 * @param sSelectedStdQuant
	 * 		the name of a standard quantification that should be selected by default
	 */
	public void setSelectedStdQuant(String sSelectedStdQuant) {
		this.sSelectedStdQuant = sSelectedStdQuant;
	}
	
	/**
	 * Validates the input on the dialog to make sure all settings comply with a StandardQuant object
	 */
	public void validateInput(){
		if ( paramsOk ) {
			setErrorMessage(null);
			getButton(IDialogConstants.OK_ID).setEnabled(true);
			getButton(IDialogConstants.FINISH_ID).setEnabled(true);

		} else {
			setErrorMessage(stdQuantUI.getErrorMessage());
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			getButton(IDialogConstants.FINISH_ID).setEnabled(false);
		}

	}
		
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if( event.getProperty().equals(MassSpecStandardQuantPreferenceUI.PAGE_COMPLETE_PROPERTY) ) {
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
			stdQuantUI.updateSettings();
			stdQuantUI.updatePreferences();
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
	}
	
	/**
	 * Saves the preferences, if desired, and notifies all listeners
	 */
	protected void applyPressed() {
		apply();
		if( stdQuantUI.getSaveAsDefault() ) {
			stdQuantUI.save();	
		}
		if ( getListeners() != null ) {
			for( IPropertyChangeListener listener : getListeners() ) {
				if( ! stdQuantUI.getSaveAsDefault() ) {
					listener.propertyChange(new PropertyChangeEvent(this, PROPERTY_LOCAL_CHANGE, Boolean.FALSE, Boolean.TRUE));
				} else {
					listener.propertyChange(new PropertyChangeEvent(this, PROPERTY_PREFERENCE_CHANGE, Boolean.FALSE, Boolean.TRUE));					
				}
			}
		}
		stdQuantUI.initStoredStandardQuantList();
		stdQuantUI.setResetEnabled();
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
						MassSpecStandardQuantApplyDialog.PROPERTY_WIN_CLOSED, Boolean.FALSE, Boolean.TRUE));
			}
		}
		return super.close();
	}
	
}
