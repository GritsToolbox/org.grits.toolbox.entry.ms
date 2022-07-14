package org.grits.toolbox.entry.ms.adaptor;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.grits.toolbox.core.dataShare.PropertyHandler;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.ProjectProperty;
import org.grits.toolbox.core.datamodel.util.DataModelSearch;
import org.grits.toolbox.core.utilShare.CopyUtils;
import org.grits.toolbox.core.utilShare.ErrorUtils;

import org.grits.toolbox.entry.ms.property.MassSpecProperty;

/**
 * Export files to user selected directory.
 * @author kitaemyoung
 *
 */
public class MassSpecExportFileAdapter_old extends SelectionAdapter {
	
	//log4J Logger
	private static final Logger logger = Logger.getLogger(MassSpecExportFileAdapter.class);
	
	private Shell shell = null;
	private String fileName = null;
	private Entry msEntry = null;
	
	public void widgetSelected(SelectionEvent event) 
	{
		FileDialog dlg = new FileDialog(shell,SWT.SAVE);
		//set the default fileName
		dlg.setFileName(fileName);
		// Set the initial filter path according
		// to anything they've selected or typed in
		dlg.setFilterPath(null);
		// Change the title bar text
		dlg.setText("Directory Explorer");
		// Customizable message displayed in the dialog
		//dlg.setMessage("Select a directory to export the file");
		// Calling open() will open and run the dialog.
		// It will return the selected directory, or
		// null if user cancels
		String dir = dlg.open();
		if (dir != null) {
			// Set the text box to the new selection
			String workspaceLocation = PropertyHandler.getVariable("workspace_location");
			String projectName = DataModelSearch.findParentByType(msEntry, ProjectProperty.TYPE).getDisplayName();
			String from = workspaceLocation+projectName+"/"+MassSpecProperty.getFoldername()+"/"+fileName;
			try {//+"/"+fileName
				CopyUtils.copyFilesFromTo(from,dir);
			} catch (IOException e) {
				logger.error(e.getMessage(),e);
				ErrorUtils.createErrorMessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Unable to save file",e);
			}
		}
	}

	public Shell getShell() {
		return shell;
	}

	public void setShell(Shell shell) {
		this.shell = shell;
	}

	public void setMsEntry(Entry msEntry) {
		this.msEntry = msEntry;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}
