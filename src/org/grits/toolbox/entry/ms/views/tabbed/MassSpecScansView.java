package org.grits.toolbox.entry.ms.views.tabbed;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.editor.EntryEditorPart;
import org.grits.toolbox.datamodel.ms.tablemodel.FillTypes;
import org.grits.toolbox.display.control.table.process.TableDataProcessor;
import org.grits.toolbox.display.control.table.process.TableDataProcessorRunner;
import org.grits.toolbox.display.control.table.tablecore.DelayedResizeListener;
import org.grits.toolbox.entry.ms.process.loader.MassSpecTableDataProcessor;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.widgets.processDialog.GRITSProgressDialog;
import org.grits.toolbox.widgets.tools.GRITSProcessStatus;

public class MassSpecScansView extends EntryEditorPart implements IMSPeaksViewer {
	private static final Logger logger = Logger.getLogger(MassSpecScansView.class);
	public static final String VIEW_ID = "plugin.ms.annotation.views.MSAnnotationScansView"; //$NON-NLS-1$

	protected Composite parent = null;
	protected Composite compositeTop = null;
	protected Composite container = null;

	protected MassSpecTableBase viewBase = null;
	protected MassSpecResultsComposite resultsComposite = null;
	protected Property entityProperty = null;
	protected TableDataProcessor dataProcessor = null;
	protected int iMinMSLevel = -1;

	@Inject
	protected IEclipseContext context;
		
	@Inject
	public MassSpecScansView(Entry entry, Property msEntityProperty,
			@Named(MassSpecMultiPageViewer.MIN_MS_LEVEL_CONTEXT) int iMinMSLevel) {
		this.entry = entry;
		this.entityProperty = msEntityProperty;
		this.iMinMSLevel = iMinMSLevel;
	}
	
	public MassSpecResultsComposite getResultsView() {
		return resultsComposite;
	}
	
	public Composite getContainer() {
		return container;
	}
	
	public int getStatus() {
		if( viewBase == null ) {
			return GRITSProcessStatus.ERROR;
		} else if ( dataProcessor.isCanceled() ) {
			return GRITSProcessStatus.CANCEL;
		}
		return GRITSProcessStatus.OK;
	}

	public int getMinMSLevel() {
		return iMinMSLevel;
	}

	public void setMinMSLevel(int iMinMSLevel) {
		this.iMinMSLevel = iMinMSLevel;
	}

	public void setEntityProperty(MassSpecEntityProperty entityProperty) {
		this.entityProperty = entityProperty;
	}

	@Override
	public String toString() {
		return "MassSpecScansView (" + entry + ")";
	}
	
	protected void initResultsView(Composite parent) throws Exception {		
		this.parent = parent.getParent().getParent();    //CTabFolder
		compositeTop = new Composite(parent, SWT.BORDER);
		compositeTop.setLayout(new GridLayout(1, false));

		try {
			resultsComposite = getNewResultsComposite(compositeTop, SWT.NONE);
			resultsComposite.createPartControl(this.compositeTop, this, this.entityProperty, this.dataProcessor, FillTypes.Scans);
			resultsComposite.setLayout(new FillLayout());
			this.viewBase = resultsComposite.getBaseView();
		} catch (Exception e) {
			viewBase = null;
			resultsComposite = null;
			logger.error("Error in MassSpecScansView: initResultsView");
			throw new Exception(e.getMessage());
		}
	}

	protected void addListeners(Composite container) {
		DelayedResizeListener l = new DelayedResizeListener();
		if (resultsComposite != null) {
			l.addTable(resultsComposite.getBaseView().getNatTable());
		}
		container.addControlListener(l);
		
	}

	protected void createView(Composite container) throws Exception {
		initResultsView(container);
	}

	/**
	 * Create contents of the editor part.
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		try {
//			final Composite container = new Composite(parent, SWT.NONE);
			container = new Composite(parent, SWT.NONE);
			container.setLayout(new FillLayout());
			createView(container);
			addListeners(container);
		} catch( Exception e ) {
			viewBase = null;
			resultsComposite = null;
		}
	}

	protected MassSpecResultsComposite getNewResultsComposite(
			Composite composite, int style) {
		return new MassSpecResultsComposite(composite, style);
	}

	public MassSpecTableBase getViewBase() {
		return viewBase;
	}

	public TableDataProcessor getTableDataProcessor() {
		return dataProcessor;
	}

	public int setTableDataProcessor( GRITSProgressDialog dtpd ) {
		dataProcessor = getNewTableDataProcessor(this.entityProperty);
		int iStatus = GRITSProcessStatus.OK;
		if (dataProcessor == null) {
			//			String fileName = getSourceFile();
			try {
				dataProcessor = getNewTableDataProcessor(getEntry(), this.entityProperty);
				dataProcessor.setProgressBarDialog(dtpd);
				iStatus = openReadWriteDialog(TableDataProcessor.OPEN );
//				if( iStatus == GRITSProcessStatus.CANCEL ) {
//					dataProcessor.cancelWork();
//				}
			} catch (Exception e) {
				logger.error("Unable to create XML Extractor.", e);
				return GRITSProcessStatus.ERROR;
			}
		} else {
			dataProcessor.setProgressBarDialog(dtpd);			
		}
		return iStatus;
	}

	protected int openReadWriteDialog( Integer processType ) {
		TableDataProcessorRunner processRunner = new TableDataProcessorRunner( (MassSpecTableDataProcessor) getTableDataProcessor()); 	
		try {
			getTableDataProcessor().setProcessType(processType);
			int iStatus = processRunner.startJob();
			return iStatus;
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
		return GRITSProcessStatus.ERROR;		
	}

	protected TableDataProcessor getNewTableDataProcessor(Entry entry, Property entityProperty) {
		MassSpecTableDataProcessor proc = new MassSpecTableDataProcessor(
				entry, entityProperty, 
				FillTypes.Scans, getMinMSLevel());
		proc.initializeTableDataObject(entityProperty);
		return proc;
	}
	
	protected TableDataProcessor getNewTableDataProcessor(Property entityProperty) {
		MassSpecMultiPageViewer parentViewer = MassSpecMultiPageViewer.getActiveViewerForEntry(context, getEntry().getParent());
		if (parentViewer == null || parentViewer.getScansView() == null) {
			return null;
		}
		TableDataProcessor parentProc = parentViewer.getScansView().getTableDataProcessor();
		if( parentProc == null ) 
			return null;
		if ( ! parentProc.getSourceProperty().equals(entityProperty) ) {
			return null;
		}
		MassSpecTableDataProcessor proc = new MassSpecTableDataProcessor(
				parentProc, entityProperty, FillTypes.Scans);
		proc.setParentShell(parent.getShell());
		proc.initializeTableDataObject(entityProperty);
		return proc;
	}

	protected boolean isRightType(Entry entry) {
		if (entry.getProperty().getType().equals(MassSpecEntityProperty.TYPE)) {
			return true;
		}
		return false;
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
	protected Composite getParent() {
		return this.parent;
	}

	public void setParent(Composite parent) {
		this.parent = parent;
	}

	@Focus
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDirty(boolean d) {
		super.setDirty(d);
	}

	@Override
	public boolean isDirty() {
		return super.isDirty();
	}

	@Override
	public void reInitializeView() throws Exception {
		// do nothing?
	}

	@Override
	public void reLoadView() {
		try {
			resultsComposite.reInit(entityProperty);			
		} catch( Exception e) {
			viewBase = null;
			resultsComposite = null;
			logger.error("Error in MSAnnotationPeaksView: reInitializeView");
		}	
	}
}
