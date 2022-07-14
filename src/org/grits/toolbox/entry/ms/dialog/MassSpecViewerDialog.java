package org.grits.toolbox.entry.ms.dialog;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.grits.toolbox.entry.ms.views.tabbed.MassSpecMultiPageViewer;

/**
 * Top-level dialog to interact with MassSpecMultiPageViewer. Should be sub-classed for specific functionality.
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 *
 */
public class MassSpecViewerDialog extends TitleAreaDialog {
	private static final Logger logger = Logger.getLogger(MassSpecViewerDialog.class);
	private Entry massSpecEntry;
	private List<IPropertyChangeListener> listeners = null;
	private MassSpecMultiPageViewer contextViewer = null;

	public MassSpecViewerDialog(Shell parentShell, MassSpecMultiPageViewer contextViewer) {
		super(parentShell);
		this.contextViewer = contextViewer; 
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
	 * @return the Entry associated with this dialog
	 */
	public Entry getMassSpecEntry() {
		return massSpecEntry;
	}

	/**
	 * @param massSpecEntry
	 * 		the Entry to associate with this dialog
	 */
	public void setMassSpecEntry(Entry massSpecEntry) {
		this.massSpecEntry = massSpecEntry;
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
	protected Entry getEntryForCurrentViewer() {
		MassSpecMultiPageViewer viewer = getCurrentViewer();
		if ( viewer == null ) {
			return null;
		}
		return viewer.getEntry();
	}

	/**
	 * @return the associated injected Context Viewer for this dialog
	 */
	public MassSpecMultiPageViewer getContextViewer() {
		return contextViewer;
	}
	
	/**
	 * Allows other classes to register themselves for when data is changed for this MS entry.
	 * 
	 * @param listener
	 */
	public void addListener( IPropertyChangeListener listener ) {
		if ( listeners == null ) {
			listeners = new ArrayList<IPropertyChangeListener>();
		}
		listeners.add(listener);
	}
	
	/**
	 * @return the list of property change listeners for the current MS viewer
	 */
	public List<IPropertyChangeListener> getListeners() {
		return listeners;
	}
	

}
