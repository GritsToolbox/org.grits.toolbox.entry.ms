package org.grits.toolbox.entry.ms.views.tabbed;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.e4.ui.model.application.ui.advanced.impl.PerspectiveStackImpl;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.e4.ui.workbench.modeling.IPartListener;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.core.dataShare.IGritsConstants;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.UnsupportedVersionException;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.editor.CancelableMultiPageEditor;
import org.grits.toolbox.core.editor.EntryEditorPart;
import org.grits.toolbox.core.editor.IEntryEditorPart;
import org.grits.toolbox.core.preference.share.IGritsPreferenceStore;
import org.grits.toolbox.core.preference.share.PreferenceEntity;
import org.grits.toolbox.core.service.IGritsDataModelService;
import org.grits.toolbox.core.service.IGritsUIService;
import org.grits.toolbox.datamodel.ms.preference.MassSpecViewerPreference;
import org.grits.toolbox.datamodel.ms.tablemodel.FillTypes;
import org.grits.toolbox.display.control.table.preference.TableViewerPreference;
import org.grits.toolbox.entry.ms.dialog.MassSpecCustomAnnotationDialog;
import org.grits.toolbox.entry.ms.dialog.MassSpecExternalQuantDialog;
import org.grits.toolbox.entry.ms.dialog.MassSpecPeakIntensityApplyDialog;
import org.grits.toolbox.entry.ms.dialog.MassSpecStandardQuantApplyDialog;
import org.grits.toolbox.entry.ms.exceptions.MSException;
import org.grits.toolbox.entry.ms.process.loader.MassSpecTableDataProcessor;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.grits.toolbox.entry.ms.property.datamodel.MSPropertyDataFile;
import org.grits.toolbox.entry.ms.tablehelpers.MassSpecTable;
import org.grits.toolbox.ms.file.MSFile;
import org.grits.toolbox.ms.file.reader.IMSAnnotationFileReader;
import org.grits.toolbox.ms.file.reader.IMSFileReader;
import org.grits.toolbox.ms.file.scan.data.ScanView;
import org.grits.toolbox.widgets.processDialog.GRITSProgressDialog;
import org.grits.toolbox.widgets.progress.CancelableThread;
import org.grits.toolbox.widgets.progress.IProgressListener.ProgressType;
import org.grits.toolbox.widgets.progress.IProgressThreadHandler;
import org.grits.toolbox.widgets.tools.GRITSProcessStatus;
import org.grits.toolbox.widgets.tools.GRITSWorker;


/**
 * A tabbed-editor for displaying information for Mass Spec Data.<br>
 * This editor will consist of property pages and one or more GRITS Tables.
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 * @see Entry
 * @see MassSpecPropertyView
 * @see MassSpecScansView
 * @see MassSpecSpectraView
 * @see MassSpecPeaksView
 *
 */
public class MassSpecMultiPageViewer extends CancelableMultiPageEditor implements IPropertyChangeListener {
	private static final Logger logger = Logger.getLogger(MassSpecMultiPageViewer.class);
	protected Entry entry = null;
	protected MassSpecPropertyView propertyView = null;
	protected int propertyViewTabIndex = -1;
	protected MassSpecScansView scansView = null;
	protected int scansViewTabIndex = -1;
	protected MassSpecSpectraView spectraView = null;
	protected int spectraViewTabIndex = -1;
	protected List<MassSpecPeaksView> alPeaksViews = null;
	protected int alPeaksViewsFirstTabIndex = -1;
	protected int iMinMSLevel;
	//protected tring mzXMLFileName;
	protected MSPropertyDataFile msFile;

	@Inject protected IGritsPreferenceStore gritsPreferenceStore;
	@Inject protected EPartService partService;
	@Inject protected EModelService modelService;
	@Inject protected static IGritsDataModelService gritsModelService;
	@Inject MDirtyable dirtyable;
	@Inject IEclipseContext context;
	@Inject protected MApplication application;

	public static Map<String, Integer> mSourceToMinMSLevel = new HashMap<>();
	public static final String VIEW_ID = "plugin.ms.annotation.views.MassSpecMultiPageViewer";
	public static final String MIN_MS_LEVEL_CONTEXT = "MinMSLevelContext";
	public static final String EVENT_PARENT_ENTRY_VALUE_MODIFIED = "Value_Modified_for_Checkboxes";
	public static final String SCAN_HIEARARCHYPARTNAME="ScanHierarchy";

	public static MassSpecCustomAnnotationDialog massSpecCustomAnnotationDialog = null;
	public static MassSpecStandardQuantApplyDialog massSpecStandardQuantApplyDialog = null;
	public static MassSpecExternalQuantDialog massSpecExternalQuantDialog = null;
	public static MassSpecPeakIntensityApplyDialog massSpecPeakIntensityApplyDialog = null;
	
	@Override
	public String toString() {
		return "MassSpecMultiPageViewer (" + entry + ")";
	}

	public MassSpecMultiPageViewer() {
		super();
		alPeaksViews = new ArrayList<MassSpecPeaksView>();
	}

	@Inject
	public MassSpecMultiPageViewer(Entry entry) {
		super();
		this.entry = entry;
		alPeaksViews = new ArrayList<MassSpecPeaksView>();
	}

	@Inject
	public MassSpecMultiPageViewer (MPart part) {
		super();
		this.entry = (Entry) part.getTransientData().get(IGritsUIService.TRANSIENT_DATA_KEY_PART_ENTRY);
		alPeaksViews = new ArrayList<MassSpecPeaksView>();
	}
	
	@PostConstruct
	public void createPartControl(Composite parent,  final MPart part) {
		super.createPartControl(parent, part);
		
		EPartService ePartService = part.getContext().get(EPartService.class);
		ePartService.addPartListener(new IPartListener() {
			
			@Override
			public void partVisible(MPart part) {
			}
			
			@Override
			public void partHidden(MPart targetMPart) {
			}
			
			@Override
			public void partDeactivated(MPart targetMPart) {
			}
			
			@SuppressWarnings("restriction")
			@Override
			public void partBroughtToTop(MPart targetMPart) {
				if (part == targetMPart) {
					if (part.getObject() != null && part.getObject() instanceof MassSpecMultiPageViewer) {
						// check if we are in the ms perspective before adding the page
						PerspectiveStackImpl perspectiveStack = (PerspectiveStackImpl) modelService.find(IGritsConstants.ID_DEFAULT_PERSPECTIVE_STACK, application);
						String perspectiveName = perspectiveStack.getSelectedElement().getElementId();
						if (perspectiveName.equals("org.grits.toolbox.core.perspective.msperspective") ||
								perspectiveName.equals("org.grits.toolbox.core.perspective.msperspective.<MS Perspective>"))
							addScanHierarchyPage((MassSpecEntityProperty) ((MassSpecMultiPageViewer) part.getObject()).getEntry().getProperty());
					}
				}
			}
			
			@Override
			public void partActivated(MPart part) {
			}
		});
	}
	
	protected String getScanHierarchyPartName() {
		return SCAN_HIEARARCHYPARTNAME;
	}
	
	@PreDestroy
	public void preDestroy(final MPart part) {
		clearScanHierarchyPage((MassSpecEntityProperty) getEntry().getProperty());
	}

	public IEclipseContext getContext() {
		return context;
	}

	public EPartService getPartService() {
		return partService;
	}

	public int getPropertyViewTabIndex() {
		return propertyViewTabIndex;
	}

	public void setPropertyViewTabIndex(int propertyViewTabIndex) {
		this.propertyViewTabIndex = propertyViewTabIndex;
	}

	public int getScansViewTabIndex() {
		return scansViewTabIndex;
	}

	public void setScansViewTabIndex(int scansViewTabIndex) {
		this.scansViewTabIndex = scansViewTabIndex;
	}

	public int getSpectraViewTabIndex() {
		return spectraViewTabIndex;
	}

	public void setSpectraViewTabIndex(int spectraViewTabIndex) {
		this.spectraViewTabIndex = spectraViewTabIndex;
	}

	public int getPeaksViewsFirstTabIndex() {
		return alPeaksViewsFirstTabIndex;
	}

	public void setPeaksViewsFirstTabIndex(int alPeaksViewsFirstTabIndex) {
		this.alPeaksViewsFirstTabIndex = alPeaksViewsFirstTabIndex;
	}

	public GRITSProgressDialog getThreadedDialog() {
		return dtpdThreadedDialog;
	}

	public void setThreadedDialog(GRITSProgressDialog dtpdThreadedDialog) {
		this.dtpdThreadedDialog = dtpdThreadedDialog;
	}

	public int getMinMSLevel() {
		return iMinMSLevel;
	}

	public void setMinMSLevel(int iMinMSLevel) {
		this.iMinMSLevel = iMinMSLevel;
	}

	public Entry getEntry() {
		return entry;
	}

	public void setEntry(Entry entry) {
		this.entry = entry;
	}

	public boolean hasMSFile( MassSpecEntityProperty msEntityProp ) {
		//String sFileName = msEntityProp.getMassSpecParentProperty().getMzXMLFileName();
		MSPropertyDataFile file = msEntityProp.getDataFile();
		return ( file != null && ! file.getName().equals("") );		
	}

	protected int getMinMSLevelFromSourceFile( MassSpecEntityProperty msEntityProp, Entry newEntry) {
		//String sFileName = msEntityProp.getMassSpecParentProperty().getFullyQualifiedMzXMLFileName(newEntry);
		if( ! hasMSFile(msEntityProp) ) {
			return 0;
		}
		MSPropertyDataFile sourceFile = msEntityProp.getDataFile();
		String sFileName = msEntityProp.getMassSpecParentProperty().getFullyQualifiedFolderName(newEntry) + File.separator + msEntityProp.getDataFile().getName();

		int iMinMSLevel = -1;
		if( MassSpecMultiPageViewer.mSourceToMinMSLevel.containsKey(sFileName) ) {
			iMinMSLevel = MassSpecMultiPageViewer.mSourceToMinMSLevel.get(sFileName);
		} else {
			MSFile msFile = sourceFile.getMSFileWithReader(msEntityProp.getMassSpecParentProperty().getFullyQualifiedFolderName(newEntry), 
					msEntityProp.getMassSpecParentProperty().getMassSpecMetaData().getMsExperimentType());
			IMSFileReader reader = msFile.getReader();
			if (reader != null && reader instanceof IMSAnnotationFileReader) {
				iMinMSLevel = ((IMSAnnotationFileReader) reader).getMinMSLevel(msFile);
				MassSpecMultiPageViewer.mSourceToMinMSLevel.put(sFileName, iMinMSLevel);
			}
		}

		if( iMinMSLevel < 0 ) {
			logger.error("An error occurred reading the MS file. It is likely not formatted correctly.");
			String sMessage = "An error occurred reading the MS file. It is likely invalid.";
			this.dtpdThreadedDialog.getMajorProgressBarListener().setError(sMessage);
		}
		return iMinMSLevel;
	}

	protected Entry getFirstPageEntry() {
		try {
			Entry parentEntry = null;
			if (gritsModelService.getLastSelection() != null
					&& gritsModelService.getLastSelection().getFirstElement() instanceof Entry)
				parentEntry = (Entry) gritsModelService.getLastSelection().getFirstElement();
			Entry newEntry = MassSpecEntityProperty.getTableCompatibleEntry(parentEntry);
			MassSpecEntityProperty msEntityProp = (MassSpecEntityProperty) newEntry.getProperty();
			msEntityProp.setScanNum(-1);
			msEntityProp.setParentScanNum(-1);
			msEntityProp.setMz(0.0);
			msEntityProp.setParentMz(0.0);
			//			int iMSLevel = getMinMSLevelFromSourceFile(msEntityProp, newEntry);
			int iMSLevel = 1;
			setMinMSLevel(iMSLevel);
			msEntityProp.setMsLevel(getMinMSLevel());
			newEntry.setDisplayName(newEntry.getDisplayName());
			return newEntry;
		}catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
	}

	protected Object getDesiredActivePage() {
		return this.propertyView;
	}


	public void setActivePage() {
		try {
			for (int i = 0; i < getPageCount(); i++ ) {
				Object page = getPageItem(i);
				if( page == null )
					continue;
				if( page.equals(getDesiredActivePage()) ) {
					setActivePage(i);
					return;
				}
			}
		} catch( Exception ex ) {
			logger.error("Unable to getFirstTabIndex", ex);
		}	
	}

	public int determineMinMSLevel() {
		try {
			CancelableThread t = new CancelableThread() {
				@Override
				public boolean threadStart(IProgressThreadHandler a_progressThreadHandler) throws Exception {
					try {
						logger.debug("Determing properties of MS file" );
						int iMinMSLevel = getMinMSLevelFromSourceFile((MassSpecEntityProperty) entry.getProperty(), entry);
						setMinMSLevel( iMinMSLevel );			
						return true;
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						return false;
					}
				}

			};
			t.setProgressThreadHandler(getThreadedDialog());
			getThreadedDialog().setThread(t);
			this.dtpdThreadedDialog.getMinorProgressBarListener(0).setProgressType(ProgressType.Indeterminant);
			t.start();	
			while ( ! t.isCanceled() && ! t.isFinished() && t.isAlive() ) 
			{
				if (!Display.getDefault().readAndDispatch()) 
				{
					Display.getDefault().sleep();
				}
			}
			this.dtpdThreadedDialog.getMinorProgressBarListener(0).setProgressType(ProgressType.Determinant);
			if( t.isCanceled() ) {
				t.interrupt();
				return GRITSProcessStatus.CANCEL;
			} else {
				return GRITSProcessStatus.OK;
			}
		} catch( Exception e ) {
			logger.error(e.getMessage(), e);
		}
		return GRITSProcessStatus.ERROR;
	}

	public void setStatus(int iStatus) {
		this.iStatus = iStatus;
	}

	public int getStatus() {
		return iStatus;
	}

	public int reLoadScansTab(List<String> columnKeys) {
		this.dtpdThreadedDialog = new GRITSProgressDialog(Display.getCurrent().getActiveShell(), 1, true);
		this.dtpdThreadedDialog.open();
		this.dtpdThreadedDialog.getMajorProgressBarListener().setMaxValue(1);
		MassSpecMultiPageViewerTableRefreshWorker msmpvw = new MassSpecMultiPageViewerTableRefreshWorker(this, columnKeys);
		this.dtpdThreadedDialog.setGritsWorker(msmpvw);
		int iSuccess = this.dtpdThreadedDialog.startWorker();
		return iSuccess;		
	}

	protected int addPages( int _iMajorCount, MassSpecEntityProperty prop ) {
		this.dtpdThreadedDialog = new GRITSProgressDialog(new Shell(), 1, true);
		this.dtpdThreadedDialog.open();
		this.dtpdThreadedDialog.getMajorProgressBarListener().setMaxValue(_iMajorCount);
		MassSpecMultiPageViewerWorker msmpvw = new MassSpecMultiPageViewerWorker(this, prop);
		this.dtpdThreadedDialog.setGritsWorker(msmpvw);
		int iSuccess = this.dtpdThreadedDialog.startWorker();
		return iSuccess;
	}

	protected int getNumMajorSteps(MassSpecEntityProperty prop) {
		int iCount = 2; // property view and spectra view
		int iTabCnt = 0;
		setPropertyViewTabIndex(iTabCnt++);
		setSpectraViewTabIndex(iTabCnt++);
		if ( getMinMSLevel() < 0 ) {
			iCount++; // we will  have to determine the min MS level
		}
		setScansViewTabIndex(iTabCnt++);
		iCount += 2; // we know we have to open the data file and create at least one table
		if( needsPeaksView( prop) ) {
			iCount+=2;
			setPeaksViewsFirstTabIndex(iTabCnt++);
		}
		/* I was never adding tabs for external quant. Did we want too? Putting this here was messing up the counter
		if( prop.getMassSpecParentProperty().getMsPeakListFileName() != null &&
				prop.getMassSpecParentProperty().getMsPeakListFileFormat().equals(PeakListInfo.PEAKLISTFORMAT_EXTRACT_TYPE) && 
				prop.getMsLevel() < 3) {
			iCount+=2;
		}
		if( prop.getMassSpecParentProperty().getMzXMLFullFileName() != null && prop.getMsLevel() < 3) {
			iCount+=2;
		}
		 */
		return iCount;
	}

	public int addPropertyPage() {
		try {
			boolean success = initMSPropertyView();	
			int iPageCount = getPageCount();
			if( success ) {
				try {
					int inx = getPropertyViewTabIndex();
					if( inx >= getPageCount() ) {
						inx = addPage( propertyView, this.entry );
						setPropertyViewTabIndex(inx);
					} else {
						addPage( inx, propertyView, this.entry );
					}
					setPageText(inx, "MS Properties");	
					setActivePage(inx);
					setStatus(GRITSProcessStatus.OK);
				} catch( Exception ex ) {
					logger.error("Unable to open MS property view", ex);
					setStatus(GRITSProcessStatus.ERROR);
				}
			}
			if( isCanceled() ) {
				setStatus(GRITSProcessStatus.CANCEL);
			}
			success = (getStatus() == GRITSProcessStatus.OK );
			if( ! success ) {
				if( getPageCount() != iPageCount ) {
					removePage(getPageCount());
				}
				propertyView = null;
			} 
		} catch( Exception ex ) {
			logger.error("Unable to open MS property view", ex);
			setStatus(GRITSProcessStatus.ERROR);
		}
		if( getStatus() == GRITSProcessStatus.ERROR ) {
			String sMessage = "An error occurred creating the MS Properties tab.";
			this.dtpdThreadedDialog.getMajorProgressBarListener().setError(sMessage);
		}
		return getStatus();
	}

	public int addSpectraPage( MassSpecEntityProperty prop ) {
		try {
			boolean success = true;
			Exception spectraException = null;
			try {
				success = initSpectraView(prop);
			} catch (MSException e) {
				spectraException = e;
				logger.error(e.getMessage(), e);
				success = false;
			}
			if( isCanceled() ) {
				setStatus(GRITSProcessStatus.CANCEL);
			}
			int iPageCount = getPageCount();
			if( success ) {
				try { 
					// we want spectra the page before Scans tab
					int inx = getSpectraViewTabIndex();
					if( inx >= getPageCount() ) {
						inx = addPage( spectraView.getView() );
						setSpectraViewTabIndex(inx);
					} else {
						addPage( inx, spectraView.getView() );
					}
					setPageText(inx, "Spectra");			
					setActivePage(inx);
				} catch( Exception ex ) {
					spectraView = null;
					logger.error("Error adding Spectra View tab.", ex);
				}
			} 
			success = (spectraView != null && spectraView.isLoaded());
			if( ! success ) {
				if( getPageCount() != iPageCount ) {
					removePage(getPageCount());
				}
				spectraView = null;
				String sMessage = null;
				if( spectraException instanceof MSException ) {
					sMessage = ((MSException) spectraException).getMessage();
				} else {
					sMessage = "An error occurred creating the Spectra tab.";
				}
				setStatus(GRITSProcessStatus.ERROR);
				this.dtpdThreadedDialog.getMajorProgressBarListener().setError(sMessage);
				return getStatus();
			} 
		} catch( Exception ex ) {
			logger.error("Unable to open MS spectra view", ex);
			setStatus(GRITSProcessStatus.ERROR);
		}
		if( getStatus() == GRITSProcessStatus.ERROR ) {
			String sMessage = "An error occurred creating the MS Spectra tab.";
			this.dtpdThreadedDialog.getMajorProgressBarListener().setError(sMessage);
		}
		return getStatus();
	}
	
	public int clearScanHierarchyPage (MassSpecEntityProperty prop) {
		MPart part = partService.findPart(getScanHierarchyPartName());
		if (part != null) {
			MassSpecScanHierarchyView view = (MassSpecScanHierarchyView) part.getObject();
			if (view != null && view.getProperty() != null && view.getProperty() == prop) {
				view.setProperty(null);
				view.initializeView(new ArrayList<>());
			}
			setStatus(GRITSProcessStatus.OK);
		}
		return getStatus();
	}
	
	public int addScanHierarchyPage(MassSpecEntityProperty prop) {
		MPart part = getScanHiearchyViewPart();
		if (part == null) {
			setStatus(GRITSProcessStatus.ERROR);
			this.dtpdThreadedDialog.getMajorProgressBarListener().setError("Part stack not found. Is the following ID correct?" + "org.grits.toolbox.core.partstack.ms");
			return getStatus();
		}
		MassSpecScanHierarchyView view = (MassSpecScanHierarchyView) part.getObject();
		if (view.getProperty() == null || view.getProperty() != prop) {
			// check if we are actually opening a different one
			if (needsScanHierarchyReload(view.getProperty(), prop)) {
				view.setViewer(this);
				view.setProperty(prop);
				try {
					List<ScanView> scans = getScanViewData(prop);
					if( scans == null || scans.isEmpty() ) {
						logger.error("No scan data returned from mzXML.");
					}	
					view.initializeView (scans);
				} catch (Exception e) {
					logger.error("Unable to open MS Scan Hiearchy view", e);
					setStatus(GRITSProcessStatus.ERROR);
				}
			}
		}
		if( getStatus() == GRITSProcessStatus.ERROR ) {
			String sMessage = "An error occurred creating the MS Scan Hierarchy View.";
			this.dtpdThreadedDialog.getMajorProgressBarListener().setError(sMessage);
		}
		return getStatus();
	}
	
	/**
	 * checks to see if the oldProp and newProp belongs to a different entry
	 * uniqueness is determined by the combination of MassSpecProperty (parent) and the dataFile name
	 * 
	 * @param oldProp The property currently displayed in Scan Hierarchy View
	 * @param newProp the property that needs to be displayed in Scan Hierarchy View
	 * @return true if the new property is different than the old one, false otherwise
	 */
	protected boolean needsScanHierarchyReload(MassSpecEntityProperty oldProp, MassSpecEntityProperty newProp) {
		if (oldProp != null && oldProp.getMassSpecParentProperty() == newProp.getMassSpecParentProperty()) {
			// then check if they are displaying the same annotation files
			if (oldProp.getDataFile().equals(newProp.getDataFile()))   
				return false; // no need to reload scan hierarchy view
		}
		return true;
	}

	protected MPart getScanHiearchyViewPart () {
		String label = "Scan Hierarchy View";
		//check if the part is already open
		MPart msPart = partService.findPart("ScanHierarchy-Annotation");
		if (msPart != null) {
			partService.hidePart(msPart, true);
		}
		MPart part = partService.findPart(SCAN_HIEARARCHYPARTNAME);
		if (part != null) {
			partService.activate(part);
		} else {
			part = partService.createPart(SCAN_HIEARARCHYPARTNAME);
			part.setLabel(label);
			List<MPartStack> stacks = modelService.findElements(application, "org.grits.toolbox.core.partstack.ms",
					MPartStack.class, null);
			if (stacks.size() < 1) {
				setStatus(GRITSProcessStatus.ERROR);
				this.dtpdThreadedDialog.getMajorProgressBarListener().setError("Part stack not found. Is the following ID correct?" + "org.grits.toolbox.core.partstack.ms");
				return null;
			} 
			stacks.get(0).getChildren().add(part);
			stacks.get(0).setVisible(true);
//			PropertyHandler.changePerspective("org.grits.toolbox.core.perspective.msperspective");
			// activates the part
			partService.showPart(part, PartState.ACTIVATE);
		}
		return part;
	}
	
	List<ScanView> getScanViewData (MassSpecEntityProperty prop) throws Exception {
		MSPropertyDataFile msSourceFile = prop.getDataFile();
	//	Integer iMSLevel = prop.getMsLevel();
	//	Integer iParentScanNum = prop.getParentScanNum() == null ? -1 : prop.getParentScanNum();
	//	Integer iScanNum = prop.getScanNum() == null ? -1 : prop.getScanNum();
		// get the reader for display purposes only 
		// to be able to read FullMS file with MzXMLReader instead of as an external quantification file
		MSFile msFile = msSourceFile.getMSFileWithReader(MassSpecProperty.getFullyQualifiedFolderName(this.entry), 
				prop.getMassSpecParentProperty().getMassSpecMetaData().getMsExperimentType());
		IMSFileReader reader = msFile.getReader();
		if (reader == null || !(reader instanceof IMSAnnotationFileReader))
			return null;
		int iMax = ((IMSAnnotationFileReader)reader).getMaxScanNumber(msFile);
		this.dtpdThreadedDialog.getMinorProgressBarListener(0).setMaxValue(iMax);
		this.dtpdThreadedDialog.getMinorProgressBarListener(0).setProgressMessage("Reading data file...");
		//mzXMLFileReader = new MzXmlReader();		
		//mzXMLFileReader.addProgressListeners(progressBarDialog.getMinorProgressBarListener(0));
		reader.addProgressListeners(dtpdThreadedDialog.getMinorProgressBarListener(0));
		//String sSourceFile = getMSSourceFile();
		//List<Scan> scans = mzXMLFileReader.readMzXmlFile(sSourceFile, iMSLevel, iParentScanNum, iScanNum);
		//List<ScanView> scans = ((IMSAnnotationFileReader)reader).readMSFileForView(msFile, iMSLevel, iParentScanNum, iScanNum);
		List<ScanView> scans = ((IMSAnnotationFileReader)reader).readMSFileForView(msFile, 1, -1, -1);
		this.dtpdThreadedDialog.getMinorProgressBarListener(0).setProgressMessage("Done...");
		return scans;
	}
	
	public int addMSScansTab_Step1( MassSpecEntityProperty prop) {
		try {
			int iSuccess = initScansView(prop);		
			dtpdThreadedDialog.setMinorStatus(iSuccess);
			if( iSuccess == GRITSProcessStatus.CANCEL ) {
				setStatus(GRITSProcessStatus.CANCEL);
				return GRITSProcessStatus.CANCEL;
			}
		} catch( Exception ex ) {
			logger.error("Unable to open MS Scans view", ex);
			setStatus(GRITSProcessStatus.ERROR);
		}
		if( getStatus() == GRITSProcessStatus.ERROR ) {
			String sMessage = "An error occurred creating the MS Scans tab.";
			this.dtpdThreadedDialog.getMajorProgressBarListener().setError(sMessage);
		}
		return getStatus();	
	}

	public int addMSScansTab_Step2() {
		try {
			boolean success = true;
			int iPageCount = getPageCount();
			try {
				int inx = getScansViewTabIndex();
				if( inx >= getPageCount() ) {
					inx = addPage( scansView, this.entry );
					setScansViewTabIndex(inx);
				} else {
					addPage( inx, scansView, this.entry );
				}
				setPageText(inx, "MS Scans");
				setActivePage(inx);
				int iSuccess = scansView.getStatus();
				setStatus(iSuccess);
				dtpdThreadedDialog.setMinorStatus(iSuccess);
			} catch( Exception ex ) {
				logger.error("Error adding MS Scans tab.", ex);
				setStatus(GRITSProcessStatus.ERROR);
			}			
			success = (getStatus() != GRITSProcessStatus.ERROR);

			if( ! success ) {
				if( getPageCount() != iPageCount ) {
					removePage(getPageCount() - 1);
				}
				scansView = null;
			} 
		} catch( Exception ex ) {
			logger.error("Unable to open MS Scans view", ex);
			setStatus(GRITSProcessStatus.ERROR);
		}
		if( getStatus() == GRITSProcessStatus.ERROR ) {
			String sMessage = "An error occurred creating the MS Scans tab.";
			this.dtpdThreadedDialog.getMajorProgressBarListener().setError(sMessage);
		}
		return getStatus();	
	}

	public int addPeakListPage_Step1( MassSpecEntityProperty prop ) {
		try {
			int iSuccess = initPeaksView(prop);	
			dtpdThreadedDialog.setMinorStatus(iSuccess);
			if( iSuccess == GRITSProcessStatus.CANCEL ) {
				setStatus(GRITSProcessStatus.CANCEL);
				return GRITSProcessStatus.CANCEL;
			}
		} catch( Exception ex ) {
			logger.error("Unable to open MS Peaks view", ex);
			setStatus(GRITSProcessStatus.ERROR);
		}
		if( getStatus() == GRITSProcessStatus.ERROR ) {
			String sMessage = "An error occurred creating the MS Peaks tab.";
			this.dtpdThreadedDialog.getMajorProgressBarListener().setError(sMessage);
		}
		return getStatus();	
	}

	public int addPeakListPage_Step2() {
		try {
			boolean success = true;
			int iPageCount = getPageCount();
			try {
				int inx = getPeaksViewsFirstTabIndex();
				if( inx >= getPageCount() ) {
					inx = addPage( alPeaksViews.get(0), this.entry);	
					setPeaksViewsFirstTabIndex(inx);
				} else {
					addPage( inx, alPeaksViews.get(0), this.entry);
				}
				setPageText(inx, "Peak List");
				setActivePage(inx);
				int iSuccess = scansView.getStatus();
				setStatus(iSuccess);
				dtpdThreadedDialog.setMinorStatus(iSuccess);
			} catch( Exception ex ) {
				logger.error("Error adding Peaks List tab.", ex);
				setStatus(GRITSProcessStatus.ERROR);
			}			
			success = (getStatus() != GRITSProcessStatus.ERROR);

			if( ! success ) {
				if( getPageCount() != iPageCount ) {
					removePage(getPageCount());
				}
				alPeaksViews = null;
			} 
		} catch( Exception ex ) {
			logger.error("Unable to open MS Peaks view", ex);
			setStatus(GRITSProcessStatus.ERROR);
		}
		if( getStatus() == GRITSProcessStatus.ERROR ) {
			String sMessage = "An error occurred creating the MS Peaks tab.";
			this.dtpdThreadedDialog.getMajorProgressBarListener().setError(sMessage);
		}
		return getStatus();	
	}

	public int addExternalQuantPage_Step1(MassSpecEntityProperty prop ) {
		try {
			int iSuccess = initPeaksView(prop);	
			dtpdThreadedDialog.setMinorStatus(iSuccess);
			if( iSuccess == GRITSProcessStatus.CANCEL ) {
				setStatus(GRITSProcessStatus.CANCEL);
				return GRITSProcessStatus.CANCEL;
			}
		} catch( Exception ex ) {
			logger.error("Unable to open External Quant tab: " + prop.getPeakListNumber(), ex);
			setStatus(GRITSProcessStatus.ERROR);
		}
		if( getStatus() == GRITSProcessStatus.ERROR ) {
			String sMessage = "An error occurred creating the External Quant tab: " + prop.getPeakListNumber();
			this.dtpdThreadedDialog.getMajorProgressBarListener().setError(sMessage);
		}
		return getStatus();	
	}

	public int addExternalQuantPage_Step2( String sTitle, MassSpecEntityProperty prop ) {
		try {
			boolean success = true;
			int iPageCount = getPageCount();
			try {
				int inx = addPage( alPeaksViews.get(prop.getPeakListNumber()), this.entry);		
				setPageText(inx, sTitle);
				setActivePage(inx);
				int iSuccess =  alPeaksViews.get(prop.getPeakListNumber()).getStatus();
				setStatus(iSuccess);
				dtpdThreadedDialog.setMinorStatus(iSuccess);
			} catch( Exception ex ) {
				logger.error("Error adding External Quant tab: " + prop.getPeakListNumber(), ex);
				setStatus(GRITSProcessStatus.ERROR);
			}			
			success = (getStatus() != GRITSProcessStatus.ERROR);

			if( ! success ) {
				if( getPageCount() != iPageCount ) {
					removePage(getPageCount());
				}
				alPeaksViews.set(prop.getPeakListNumber(), null);
			} 
		} catch( Exception ex ) {
			logger.error("Unable to open External Quant tab: " + prop.getPeakListNumber(), ex);
			setStatus(GRITSProcessStatus.ERROR);
		}
		if( getStatus() == GRITSProcessStatus.ERROR ) {
			String sMessage = "An error occurred creating the External Quant tab: " + prop.getPeakListNumber();
			this.dtpdThreadedDialog.getMajorProgressBarListener().setError(sMessage);
		}
		return getStatus();	
	}

	@Override
	public void createPages() {
		if (entry == null || !(entry.getProperty() instanceof MassSpecEntityProperty)) {
			entry = getFirstPageEntry();
		} 

		if (entry.getProperty() instanceof MassSpecEntityProperty) {
			if (((MassSpecEntityProperty) entry.getProperty()).getDataFile() != null)
				this.msFile = ((MassSpecEntityProperty) entry.getProperty()).getDataFile();
		}

		Property prop = entry.getProperty();	
		int iNumSteps = getNumMajorSteps( (MassSpecEntityProperty) prop);

		// need to add this multi-page editor into the part's context before adding pages
		// pages may need to access their parent editor
		getPart().getContext().set(MassSpecMultiPageViewer.class, this);

		iStatus = addPages(iNumSteps, (MassSpecEntityProperty) prop);
		//		if( iStatus != GRITSProcessStatus.OK ) {
		//			killMyself(input);
		//		}

		String fileNameHeader = this.msFile != null ? this.msFile.getName().substring(this.msFile.getName().lastIndexOf(File.separator)+1) : "";
		setPartName(entry.getDisplayName() + " (" + fileNameHeader + ")");	
		setActivePage();


		//	setTitleImage();
	}

	protected boolean initMSPropertyView() {
		try {
			propertyView = ContextInjectionFactory.make(
					MassSpecPropertyView.class, getPart().getContext());
			return true;
		} catch( Exception ex ) {
			logger.error("Unable to open ms property view", ex);
		}		
		return false;
	}

	protected int initScansView( MassSpecEntityProperty entityProperty ) {
		try {
			scansView = getNewScansView( this.entry, entityProperty );
			int iSuccess = scansView.setTableDataProcessor(this.dtpdThreadedDialog);
			return iSuccess;
		} catch( Exception ex ) {
			logger.error("Unable to open scans view", ex);
			return GRITSProcessStatus.ERROR;
		}		
	}

	/**
	 * Move the columns designated by the key values in the specified list to the first columns and refresh the scans table.
	 * 
	 * @param columnKeys list of key values to move to the beginning of the table
	 * @return the GRITSProcessStatus after attempting refresh
	 */
	protected int reInitScansView(List<String> columnKeys) {
		try {
			scansView.getTableDataProcessor().setProgressBarDialog(this.dtpdThreadedDialog);
			scansView.reLoadView();
			if( columnKeys != null ) {
				for( String keyVal : columnKeys ) {
					scansView.getViewBase().getNatTable().moveToFirstColumn(keyVal);
				}
				scansView.getViewBase().getNatTable().updatePreferenceSettingsFromCurrentView();
				scansView.getViewBase().getNatTable().getGRITSTableDataObject().getTablePreferences().writePreference();	
			}
			return GRITSProcessStatus.OK;
		} catch( Exception ex ) {
			logger.error("Unable to open scans view", ex);
			return GRITSProcessStatus.ERROR;
		}		
	}

	public boolean needsPeaksView(MassSpecEntityProperty entityProperty) {
		return entityProperty.getScanNum() != null && entityProperty.getScanNum() > 0;	
	}

	public boolean needsSpectraView(MassSpecEntityProperty entityProperty) {
		return true;
	}

	// peaks view for MS is the peak list for a particular scan
	protected int initPeaksView(MassSpecEntityProperty entityProperty) {
		try {
			MassSpecEntityProperty msProp = (MassSpecEntityProperty) entityProperty.clone();
			MassSpecPeaksView peaksView = getNewPeaksView( this.entry, msProp);
			int iSuccess = peaksView.setTableDataProcessor(this.dtpdThreadedDialog);
			if( iSuccess == GRITSProcessStatus.OK ) {
				alPeaksViews.add(peaksView);
			}
			return iSuccess;
		} catch( Exception ex ) {
			logger.error("Unable to open peaks view", ex);
			return GRITSProcessStatus.ERROR;
		}		
	}

	protected MassSpecSpectraView getNewSpectraView() {
		getPart().getContext().set(Entry.class, entry);
		MassSpecSpectraView view = ContextInjectionFactory.make(MassSpecSpectraView.class, getPart().getContext());

		//return new MassSpecSpectraView(this.entry);
		return view;
	}

	protected boolean initSpectraView( MassSpecEntityProperty prop ) throws MSException {
		try {
			int iMSLevelForSpectrum = 1;
			if ( prop.getMsLevel() != null ) {
				iMSLevelForSpectrum = prop.getMsLevel();
				if ( entry.getParent().getProperty() instanceof MassSpecEntityProperty ) {
					MassSpecEntityProperty parentProp = (MassSpecEntityProperty) entry.getParent().getProperty();
					iMSLevelForSpectrum = parentProp.getMsLevel();
				}
			}
			spectraView = getNewSpectraView();
			if( this.getScansView() != null ) {
				MassSpecTableDataProcessor scansTableProcessor = (MassSpecTableDataProcessor) this.getScansView().getTableDataProcessor();
				( (MassSpecSpectraView) spectraView).setScanListTableProcessor(scansTableProcessor);				
			}
			if( this.getPeaksView() != null && ! this.getPeaksView().isEmpty()) {
				MassSpecTableDataProcessor peaksTableProcessor = (MassSpecTableDataProcessor) this.getPeaksView().get(0).getTableDataProcessor();
				( (MassSpecSpectraView) spectraView).setPeakListTableProcessor(peaksTableProcessor);
			}
			//			spectraView =  getNewSpectraView();
			( (MassSpecSpectraView) spectraView).setMSLevel(iMSLevelForSpectrum);
			( (MassSpecSpectraView) spectraView).setScanNum(prop.getScanNum());
			//			MassSpecProperty msProperty = getMSProperty(entry);
			updateMSView(prop, spectraView);
			return true;
		} catch( Exception ex ) {
			logger.error("Unable to open spectra view", ex);
		}		
		return false;	
	}	

	protected MassSpecProperty getMSProperty( Entry entry ) {
		if ( entry == null || entry.getProperty() == null)
			return null;
		else if ( entry.getProperty() instanceof MassSpecProperty ) {
			return (MassSpecProperty) entry.getProperty();
		}
		return getMSProperty(entry.getParent());
	}

	protected void updateMSView( MassSpecEntityProperty msProperty, MassSpecSpectraView view ) throws MSException {

		if ( msProperty == null )
			return;
		String sMzXMLFile = null;

		MSPropertyDataFile msPDF = msProperty.getMassSpecParentProperty().getUpdatePropertyDataFile(msProperty.getDataFile());

		if ( (msProperty.getScanNum() == null || msProperty.getScanNum() <= 0) && 
				msProperty.getMassSpecParentProperty().getMassSpecMetaData().getMzXMLFullFileName() != null && 
				! msProperty.getMassSpecParentProperty().getMassSpecMetaData().getMzXMLFullFileName().equals("") ) {
			sMzXMLFile =  MassSpecProperty.getFullyQualifiedFolderName(entry) + File.separator + msProperty.getMassSpecParentProperty().getMassSpecMetaData().getMzXMLFullFileName();
		} else if ( msPDF != null && 
				! msPDF.getName().equals("")  ) {
			sMzXMLFile =  MassSpecProperty.getFullyQualifiedFolderName(entry) + File.separator + msPDF.getName();
		}
		view.setDescription(entry.getDisplayName());
		view.setMzXMLFile(sMzXMLFile);
		view.setScanNum(msProperty.getScanNum());
		try {
			view.createChart(this.getContainer());
		} catch( MSException e ) {
			throw e; 
		} catch (Exception e) {
			logger.error("Unable to update spectra view", e);
		}
	}

	protected void updateColumnVisibility( MassSpecTable table, MassSpecViewerPreference curPref, MassSpecViewerPreference updatePref ) {
		if( curPref.getClass().equals(updatePref.getClass()) && 
				curPref.getMSLevel() == updatePref.getMSLevel() && 
				curPref.getFillType() == updatePref.getFillType() ) {
			// don't update if not changed!
			if( ! updatePref.getColumnSettings().equals(curPref.getColumnSettings()) ) {
				table.getGRITSTableDataObject().setTablePreferences( updatePref );
				table.updateViewFromPreferenceSettings();						
			}
		}
	}	

	protected void updateColumnVisibilityForView( MassSpecScansView scansView, MassSpecViewerPreference updatePref ) {
		try {
			if( scansView == null || scansView.getViewBase() == null ) {
				return;
			}
			MassSpecTableBase viewBase = scansView.getViewBase();
			if( viewBase.getNatTable() == null ) {
				return;
			}
			MassSpecTable table = (MassSpecTable) viewBase.getNatTable();
			MassSpecViewerPreference curPref = (MassSpecViewerPreference) table.getGRITSTableDataObject().getTablePreferences();
			updateColumnVisibility(table, curPref, updatePref);
		} catch( Exception ex ) {
			logger.error("Error updating scans view from editor: " + getTitle(), ex);
		}

	}

	protected void updateColumnVisibility( MassSpecViewerPreference updatePref ) {
		if( getScansView() != null ) {
			try {
				MassSpecScansView scansView = getScansView();
				updateColumnVisibilityForView(scansView, updatePref);
			} catch( Exception ex ) {
				logger.error("Error updating scans view from editor: " + getTitle(), ex);
			}
		} 
		if ( getPeaksView() != null ) {
			for( int j = 0; j < getPeaksView().size(); j++ ) {	
				try {
					MassSpecPeaksView peaksView = getPeaksView().get(j);
					updateColumnVisibilityForView(peaksView, updatePref);
				} catch( Exception ex ) {
					logger.error("Error updating peak view: " + j + " from editor: " + getTitle(), ex);
				}
			}		
		}
	}

	@Optional @Inject
	public void updatePreferences(@UIEventTopic(IGritsPreferenceStore.EVENT_TOPIC_PREF_VALUE_CHANGED)
	String preferenceName)
	{
		if(MassSpecViewerPreference.getPreferenceID().equals(preferenceName)) {
			PreferenceEntity preferenceEntity;
			try {
				preferenceEntity = gritsPreferenceStore.getPreferenceEntity(preferenceName);

				MassSpecViewerPreference updatePref = (MassSpecViewerPreference) TableViewerPreference.getTableViewerPreference(preferenceEntity, MassSpecViewerPreference.class);
				this.updateColumnVisibility(updatePref);
			} catch (UnsupportedVersionException e) {
				logger.error("Error updating column visibility", e);
			}
		}
	}

	public static MassSpecMultiPageViewer getActiveViewerForEntry(IEclipseContext context, Entry entry ) {
		EPartService partService = context.get(EPartService.class);
		for (MPart part: partService.getParts()) {
			if (part.getObject() instanceof MassSpecMultiPageViewer) {
				if (((MassSpecMultiPageViewer)part.getObject()).getEntry().equals(entry)) {
					return (MassSpecMultiPageViewer)part.getObject();
				}
			}
		}
		return null;
	}

	protected MassSpecScansView getNewScansView( Entry entry, MassSpecEntityProperty entityProperty) {
		MassSpecEntityProperty msProp = (MassSpecEntityProperty) entityProperty.clone();
		msProp.setParentScanNum( entityProperty.getScanNum() );
		msProp.setScanNum(null);
		getPart().getContext().set(MIN_MS_LEVEL_CONTEXT, getMinMSLevel());
		getPart().getContext().set(Property.class, msProp);
		getPart().getContext().set(Entry.class, entry);
		MassSpecScansView view = ContextInjectionFactory.make(MassSpecScansView.class, getPart().getContext());

		//return new MassSpecScansView(this.getContainer(), entry, msProp, getMinMSLevel());
		return view;
	}

	protected MassSpecPeaksView getNewPeaksView( Entry entry, MassSpecEntityProperty entityProperty) {
		getPart().getContext().set(MIN_MS_LEVEL_CONTEXT, getMinMSLevel());
		getPart().getContext().set(Property.class, entityProperty);
		getPart().getContext().set(Entry.class, entry);
		MassSpecPeaksView view = ContextInjectionFactory.make(MassSpecPeaksView.class, getPart().getContext());
		return view;
	}

	public MassSpecScansView getScansView() {
		return scansView;
	}

	public List<MassSpecPeaksView> getPeaksView() {
		return alPeaksViews;
	}	

	public void setDirty(boolean d) {
		this.dirtyable.setDirty(d);
	}

	public boolean isDirty() {
		return this.dirtyable.isDirty();
	}

	@Persist
	public void doSave(IProgressMonitor monitor) {
		GRITSProgressDialog progressDialog = new GRITSProgressDialog(Display.getCurrent().getActiveShell(), 0, false, false);
		progressDialog.open();
		progressDialog.getMajorProgressBarListener().setMaxValue( 3 + alPeaksViews.size());
		progressDialog.setGritsWorker(new GRITSWorker() {

			@Override
			public int doWork() {
				if( propertyView != null && propertyView.isDirty()) {
					updateListeners("Saving properties", 1);
					propertyView.doSave(monitor);
				}
				if( scansView != null && scansView.isDirty()) {
					updateListeners("Saving scans", 2);
					scansView.doSave(monitor);
				}
				int i=3;
				for( MassSpecPeaksView peaksView : alPeaksViews ) {
					if( peaksView != null && peaksView.isDirty()) {
						updateListeners("Saving changes", i++);
						peaksView.doSave(monitor);
					}
				}
				setDirty(false);
				updateListeners("Done saving", i);
				return GRITSProcessStatus.OK;
			}
		});
		progressDialog.startWorker();
	}

	/** 
	 * this method is called whenever a page (tab) is updated 
	 * However we have to check to make sure the modified page is one of the pages of this
	 * multi-page editor
	 *  
	 * @param the part that gets dirty
	 */
	@Optional @Inject
	public void tabContentModified (@UIEventTopic
			(EntryEditorPart.EVENT_TOPIC_CONTENT_MODIFIED) IEntryEditorPart part) {
		if (part.equals(propertyView) || part.equals(scansView) || this.alPeaksViews.contains(part))
			setDirty(part.isDirty());
	}

	public static String[] getPreferencePageLabels( int _iMSLevel ) {
		if( _iMSLevel == 1 ) {
			return new String[]{"MS Scans"};
		} else {
			return new String[]{"MS Scans", "Peak List"};
		}
	}

	public static FillTypes[] getPreferencePageFillTypes( int _iMSLevel ) {
		if( _iMSLevel == 1 ) {
			return new FillTypes[]{FillTypes.Scans};
		} else {
			return new FillTypes[]{FillTypes.Scans, FillTypes.PeakList};
		}
	}

	public static int getPreferencePageMaxNumPages() {
		return 2;
	}


	@Focus
	public void setFocus() {
		getContainer().forceFocus();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if( event.getProperty().equals(MassSpecCustomAnnotationDialog.PROPERTY_WIN_CLOSED ) ) {
			MassSpecMultiPageViewer.massSpecCustomAnnotationDialog = null;
		} else if ( event.getProperty().equals(MassSpecStandardQuantApplyDialog.PROPERTY_WIN_CLOSED) ) {
			MassSpecMultiPageViewer.massSpecStandardQuantApplyDialog = null;
		}

	}
}
