package org.grits.toolbox.entry.ms.views.tabbed;

import org.apache.log4j.Logger;
import org.grits.toolbox.entry.ms.extquantfiles.process.ExtractDataProcessor;
import org.grits.toolbox.entry.ms.extquantfiles.process.QuantFileProcessor;
import org.grits.toolbox.entry.ms.process.loader.MassSpecTableDataProcessor;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.widgets.tools.GRITSProcessStatus;
import org.grits.toolbox.widgets.tools.GRITSWorker;

public class MassSpecMultiPageViewerWorker extends GRITSWorker {
	private static final Logger logger = Logger.getLogger(MassSpecMultiPageViewerWorker.class);
	protected int iMajorCount = 0;
	protected MassSpecEntityProperty prop = null;
	private MassSpecMultiPageViewer parentEditor = null;

	public MassSpecMultiPageViewerWorker( MassSpecMultiPageViewer parentEditor, MassSpecEntityProperty prop ) {
		this.setParentEditor(parentEditor);
		this.prop = prop;
	}

	@Override
	public int doWork() {
		iMajorCount = 0;
		int iSuccess = addPropertyPage( iMajorCount++);
		if( iSuccess != GRITSProcessStatus.OK ) {
			return iSuccess;
		} 

		if( ! getParentEditor().hasMSFile(prop) ) {
			return GRITSProcessStatus.OK;
		}

		if( getParentEditor().getMinMSLevel() < 0 ) {
			iSuccess = determineMinMSLevel(iMajorCount++);
			if( iSuccess != GRITSProcessStatus.OK ) {
				return iSuccess;
			} 
		}

		iSuccess = addMSScansTab(prop, iMajorCount);
		iMajorCount+=2;  // 2 steps to load the scans!!
		if( iSuccess != GRITSProcessStatus.OK ) {
			return iSuccess;
		} 

		if( getParentEditor().needsPeaksView(prop) ) {
			iSuccess = addPeakListPage(prop, iMajorCount);
			iMajorCount+=2;  // 2 steps to load the scans!!
			if( iSuccess != GRITSProcessStatus.OK ) {
				return iSuccess;
			} 
		}
		
		/* disabling the addition of the external quant pages. This was not working anyway, but the counter was off
		if( getParentEditor().getScansView() != null &&  getParentEditor().getScansView().getStatus() == GRITSProcessStatus.OK ) {
			MassSpecTableDataProcessor proc = (MassSpecTableDataProcessor) getParentEditor().getScansView().getTableDataProcessor();
			if( proc.getExternalQuantFileProcessors() != null && ! proc.getExternalQuantFileProcessors().isEmpty() ) {
				iSuccess = addExternalQuantPages(input, prop, iMajorCount);
				iMajorCount += (proc.getExternalQuantFileProcessors().size() * 2);  // 2 steps per quant to load the scans!!
			}
		}
		*/

		iSuccess = addSpectraPage( prop, iMajorCount++ );
		if( iSuccess != GRITSProcessStatus.OK ) {
			return iSuccess;
		} 
		
		iSuccess = addScanHierarcyPage(prop, iMajorCount++ );
		if( iSuccess != GRITSProcessStatus.OK ) {
			return iSuccess;
		} 

		updateListeners("Finished MassSpec work!", iMajorCount);
		logger.debug("Finished MassSpec work");
		return iSuccess;
	}

	public MassSpecMultiPageViewer getParentEditor() {
		return parentEditor;
	}

	public void setParentEditor(MassSpecMultiPageViewer parentEditor) {
		this.parentEditor = parentEditor;
	}

	public int addPropertyPage(  int iProcessCount ) {
		try {
			updateListeners("Creating property tab (loading)", iProcessCount);
			int iSuccess = getParentEditor().addPropertyPage();	
			updateListeners("Creating property tab (done)", iProcessCount + 1);
			return iSuccess;
		} catch( Exception ex ) {
			logger.error("Unable to open MS property view", ex);
			return GRITSProcessStatus.ERROR;
		}
	}		

	public int determineMinMSLevel(int iProcessCount) {
		try {
			updateListeners("Determining properties of MS files (loading)", iProcessCount);
			int iSuccess = getParentEditor().determineMinMSLevel();
			updateListeners("Determining properties of MS files (done)", iProcessCount + 1);
			return iSuccess;				 
		} catch( Exception e ) {
			logger.error("Unable to determine properties of file.", e);
		}
		return GRITSProcessStatus.ERROR;
	}

	public int addSpectraPage(final MassSpecEntityProperty prop, int iProcessCount ) {
		try {
			updateListeners("Creating MS spectra tab (loading)", iProcessCount);
			int iSuccess = getParentEditor().addSpectraPage( prop );
			updateListeners("Creating MS spectra tab (done)", iProcessCount + 1);
			return iSuccess;				 
		} catch( Exception e ) {
			logger.error("Unable to open MS property view", e);
		}
		return GRITSProcessStatus.ERROR;
	}

	public int addMSScansTab(final MassSpecEntityProperty prop, int iProcessCount) {
		try {
			updateListeners("Creating MS Scans tab (loading)", iProcessCount);
			int iSuccess = getParentEditor().addMSScansTab_Step1(prop);
			if( iSuccess != GRITSProcessStatus.OK ) {
				return iSuccess;
			}
			updateListeners("Creating MS Scans tab (populating)", iProcessCount + 1);
			iSuccess = getParentEditor().addMSScansTab_Step2();
			updateListeners("Creating MS Scans tab (done)", iProcessCount + 2);
			return iSuccess;				 
		} catch( Exception e ) {
			logger.error("Unable to open MS Scans view", e);
		}
		return GRITSProcessStatus.ERROR;
	}

	public int addPeakListPage(MassSpecEntityProperty prop, int iProcessCount ) {
		try {
			updateListeners("Creating MS Peak tab (loading)", iProcessCount);
			int iSuccess = getParentEditor().addPeakListPage_Step1(prop);
			if( iSuccess != GRITSProcessStatus.OK ) {
				return iSuccess;
			}
			updateListeners("Creating MS Peak tab (populating)", iProcessCount + 1);
			iSuccess = getParentEditor().addPeakListPage_Step2();
			updateListeners("Creating MS Peak tab (done)", iProcessCount + 2);
			return iSuccess;				 
		} catch( Exception e ) {
			logger.error("Unable to open MS Peaks view", e);
		}
		return GRITSProcessStatus.ERROR;
	}

	protected int addExternalQuantPages(MassSpecEntityProperty prop, int iProcessCount) {
		try {	
			boolean success = true;
			MassSpecTableDataProcessor proc = (MassSpecTableDataProcessor) getParentEditor().getScansView().getTableDataProcessor();
			for( int i = 0; success && i < proc.getQuantFileProcessors().size(); i++ ) {
				QuantFileProcessor processor = proc.getQuantFileProcessors().get(i);
				MassSpecEntityProperty newProp = (MassSpecEntityProperty) prop.clone();
				newProp.setPeakListNumber(i+1);
				updateListeners("Creating External Quant tab " + (i+1) + " (loading)", iProcessCount + i);
				int iSuccess = getParentEditor().addExternalQuantPage_Step1(newProp);
				if( iSuccess != GRITSProcessStatus.OK ) {
					return iSuccess;
				}
				updateListeners("CreatingExternal Quant tab" + (i+1) + " (populating)", iProcessCount + 1);
				String sTitle = null;
				if( processor instanceof ExtractDataProcessor ) {
					sTitle = "Extract Peak List";
				} else {
					sTitle = "Full MS Peak List";
				}
				iSuccess = getParentEditor().addExternalQuantPage_Step2(sTitle, newProp);
				updateListeners("CreatingExternal Quant tab" + (i+1) + " (done)", iProcessCount + 2);
				if( iSuccess != GRITSProcessStatus.OK ) {
					return iSuccess;
				}
			}
			return GRITSProcessStatus.OK;
		} catch( Exception e ) {
			logger.error("Unable to open MS External Quant views", e);
		}
		return GRITSProcessStatus.ERROR;
	}
	
	protected int addScanHierarcyPage (MassSpecEntityProperty prop, int iProcessCount) {
		try {
			updateListeners("Creating MS Scan Hierarchy (loading)", iProcessCount);
			//add the hierarchy page to the project explorer area
			int iSuccess = getParentEditor().addScanHierarchyPage( prop );
			updateListeners("Creating MS Scan Hierarchy", iProcessCount + 1);
			return iSuccess;
		} catch (Exception e) {
			logger.error("Unable to open MS Scan hierarcy view", e);
		}
		return GRITSProcessStatus.ERROR;
	}
}