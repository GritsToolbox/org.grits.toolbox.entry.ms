package org.grits.toolbox.entry.ms.adaptor;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

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
import org.grits.toolbox.widgets.processDialog.ProgressDialog;

import org.grits.toolbox.datamodel.ms.tablemodel.MassSpecTableDataObject;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;

/**
 * MSAnnotation File copy 
 * @author kitaemyoung
 *
 */
public class MassSpecExportFileAdapter extends SelectionAdapter {

	//log4J Logger
	private static final Logger logger = Logger.getLogger(MassSpecExportFileAdapter.class);

	protected Shell shell = null;
	protected String fileName = null;
	protected Entry msEntry = null;
	protected String sOutputFile = null;
	protected boolean copy = true;
	protected MassSpecTableDataObject tableDataObject = null;
	
	public void widgetSelected(SelectionEvent event) 
	{
		FileDialog dlg = new FileDialog(shell,SWT.SAVE);
		//set the default fileName
		dlg.setFileName(msEntry.getDisplayName()+fileName);
		// Set the initial filter path according
		// to anything they've selected or typed in
		dlg.setFilterPath(null);
		// Change the title bar text
		dlg.setText("Directory Explorer");
		// Customizable message displayed in the dialog
		// Calling open() will open and run the dialog.
		// It will return the selected directory, or
		// null if user cancels
		sOutputFile = dlg.open();
		try {
			if (sOutputFile != null) {
				String workspaceLocation = PropertyHandler.getVariable("workspace_location");
				String projectName = DataModelSearch.findParentByType(msEntry, ProjectProperty.TYPE).getDisplayName();
				//need to load data into Object
				MassSpecEntityProperty mseproperty = (MassSpecEntityProperty)this.msEntry.getProperty();
				MassSpecProperty property = mseproperty.getMassSpecParentProperty();
//				String id = mseproperty.getId();
				if(copy)
				{
//					String from = workspaceLocation+projectName+File.separator+property.getFoldername()+File.separator+id+fileName;
					String from = property.getFullyQualifiedFolderName(msEntry) + File.separator +	fileName;
					CopyUtils.copyFilesFromTo(from,sOutputFile);
				}
				else
				{
//					exportExcel(workspaceLocation+projectName+"/"+MSAnnotationProperty.getFolder(),id);
					exportExcel();
				}
				//close
//				shell.close();
			}
		} catch (NullPointerException e)
		{
			//delete files that were created!
			logger.error(e.getMessage(),e);
			ErrorUtils.createErrorMessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Unable to save file",e);
		
		} catch (IOException e) {
			//delete files that were created!
			logger.error(e.getMessage(),e);
			ErrorUtils.createErrorMessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Unable to save file",e);
		} catch (JAXBException e) {
			//delete files that were created!
			logger.error(e.getMessage(),e);
			ErrorUtils.createErrorMessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Unable to save file",e);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			ErrorUtils.createErrorMessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Unable to save file",e);
		} 
	}
	
	protected void exportExcel() throws IOException, Exception {
        //create progress dialog for copying files
        ProgressDialog t_dialog = new ProgressDialog(this.shell);
        //fill parameter
        /* need to add ms importer
		MassSpecExportProcess t_worker = new MassSpecExportProcess();
		t_worker.setOutputFile(sOutputFile);
		t_worker.setTableDataObject(getTableDataObject());
		//set the worker
        t_dialog.setWorker(t_worker);
        
        //check Cancel
        if(t_dialog.open() != SWT.OK)
        {
        	//delete the file
        	new File(sOutputFile).delete();
        }
        */
	}
	
	public MassSpecTableDataObject getTableDataObject() {
		return tableDataObject;
	}
	
	public void setTableDataObject(MassSpecTableDataObject tableDataObject) {
		this.tableDataObject = tableDataObject;
	}

	public Shell getShell() {
		return shell;
	}

	public void setShell(Shell shell) {
		this.shell = shell;
	}

	public void setMassSpecEntry(Entry msEntry) {
		this.msEntry = msEntry;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getDir() {
		return sOutputFile;
	}

	public void setCopy(boolean copy) {
		this.copy = copy;
	}
}
