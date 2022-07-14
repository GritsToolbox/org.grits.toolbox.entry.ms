package org.grits.toolbox.entry.ms.views.tabbed;

import java.util.List;

import org.apache.log4j.Logger;
import org.grits.toolbox.widgets.tools.GRITSProcessStatus;
import org.grits.toolbox.widgets.tools.GRITSWorker;

public class MassSpecMultiPageViewerTableRefreshWorker extends GRITSWorker {
	private static final Logger logger = Logger.getLogger(MassSpecMultiPageViewerTableRefreshWorker.class);
	protected int iMajorCount = 0;
	private MassSpecMultiPageViewer parentEditor = null;
	protected List<String> columnKeys = null;

	public MassSpecMultiPageViewerTableRefreshWorker( MassSpecMultiPageViewer parentEditor, List<String> columnKeys ) {
		this.setParentEditor(parentEditor);
		this.columnKeys = columnKeys;
	}

	@Override
	public int doWork() {
		iMajorCount = 0;
		updateListeners("Starting MassSpec work!", 2);
		int iSuccess = updateMSScansTab(iMajorCount);
		iMajorCount+=1;  // 2 steps to load the scans!!
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

	/**
	 * Refreshes the Scans view table(s), moving any columns containing the labels in the column keys list to the beginning
	 * @param iProcessCount
	 * @return
	 */
	public int updateMSScansTab(int iProcessCount) {
		try {
			updateListeners("Updating MS Scans tab (loading)", iProcessCount);
			getParentEditor().reInitScansView(this.columnKeys);
			updateListeners("Updating MS Scans tab (done)", iProcessCount + 1);
			return GRITSProcessStatus.OK;				 
		} catch( Exception e ) {
			logger.error("Unable to update MS Scans view", e);
		}
		return GRITSProcessStatus.ERROR;
	}

}