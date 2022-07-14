package org.grits.toolbox.entry.ms.views.tabbed;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.datamodel.ms.tablemodel.FillTypes;
import org.grits.toolbox.display.control.table.process.TableDataProcessor;
import org.grits.toolbox.entry.ms.process.loader.MassSpecTableDataProcessor;

public class MassSpecPeaksView extends MassSpecScansView {
	
	private static final Logger logger = Logger.getLogger(MassSpecPeaksView.class);
	public static final String VIEW_ID = "plugin.ms.annotation.views.MassSpecPeaks"; //$NON-NLS-1$

	private Composite compositeTop = null;

	/*public MassSpecPeaksView( Composite parent, Entry entry, MassSpecEntityProperty msEntityProperty, int iMinMSLevel ) {
		super(parent, entry, msEntityProperty, iMinMSLevel);		
	}*/
	
	@Inject
	public MassSpecPeaksView (@Optional Entry entry, @Optional Property msEntityProperty,
			@Named(MassSpecMultiPageViewer.MIN_MS_LEVEL_CONTEXT) int iMinMSLevel) {
		super (entry, msEntityProperty, iMinMSLevel);
	}

	@Override
	public String toString() {
		return "MassSpecPeaksView (" + entry + ")";
	}

	@Override
	protected void initResultsView( Composite parent ) throws Exception {
		this.parent = parent.getParent().getParent();    //CTabFolder
		compositeTop = new Composite(parent, SWT.BORDER);
		compositeTop.setLayout(new GridLayout(1,false));

		try {
			resultsComposite = getNewResultsComposite(compositeTop, SWT.NONE);
			( (MassSpecResultsComposite) resultsComposite).createPartControl(this.compositeTop, this, this.entityProperty, this.dataProcessor, 
					FillTypes.PeakList);
			//			resultsView.createPartControl(this.compositeTop, this, this.entityProperty, this.dataProcessor, this.fillType);
			resultsComposite.setLayout(new FillLayout());
			this.viewBase = (MassSpecTableBase) resultsComposite.getBaseView();
		} catch( Exception e ) {
			viewBase = null;
			resultsComposite = null;
			logger.error("Error in MassSpecScansView: initResultsView");
			throw new Exception(e.getMessage());
		}		
	}

	protected void createPeaksView(Composite container)  throws Exception {
		initResultsView(container);
	}

	/**
	 * Create contents of the editor part.
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		try {
			final Composite container = new Composite(parent, SWT.NONE);
			container.setLayout(new FillLayout());
			this.entry = getEntry();

			createPeaksView(container);
			addListeners(container);
		} catch( Exception e ) {
			viewBase = null;
			resultsComposite = null;
		}
	}

	protected MassSpecResultsComposite getNewResultsComposite( Composite composite, int style ) {
		return new MassSpecResultsComposite(composite, style);
	}

	public Composite getTopPane() {
		return compositeTop;
	}

	@Override
	protected TableDataProcessor getNewTableDataProcessor( Entry entry, Property entityProperty) {
		MassSpecTableDataProcessor proc = new MassSpecTableDataProcessor(
				entry, entityProperty, 
				FillTypes.PeakList, getMinMSLevel() );
		proc.initializeTableDataObject(entityProperty);
		return proc;
	}

	@Override
	protected TableDataProcessor getNewTableDataProcessor(Property entityProperty) {		
		MassSpecMultiPageViewer parentViewer = MassSpecMultiPageViewer.getActiveViewerForEntry(context, getEntry().getParent());
		if( parentViewer == null || parentViewer.getPeaksView().isEmpty() ) {
			return null;
		}
		TableDataProcessor parentProc = parentViewer.getPeaksView().get(0).getTableDataProcessor();
		if( parentProc == null ) 
			return null;
		if ( ! parentProc.getSourceProperty().equals(entityProperty) ) {
			return null;
		}
		MassSpecTableDataProcessor proc = new MassSpecTableDataProcessor(parentProc, entityProperty, FillTypes.PeakList);
		proc.initializeTableDataObject(entityProperty);
		return proc;		
	}

	@Override
	protected void updateProjectProperty() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void savePreference() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDirty(boolean d) {
		// TODO Auto-generated method stub
		super.setDirty(d);
	}
	
	@Persist
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		//		super.doSave(monitor);
		this.viewBase.doSave(monitor);
		setDirty(false);
	}

	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return super.isDirty();
	}

}
