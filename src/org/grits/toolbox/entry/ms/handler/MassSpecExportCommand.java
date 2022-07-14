package org.grits.toolbox.entry.ms.handler;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.core.dataShare.PropertyHandler;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.service.IGritsDataModelService;
import org.grits.toolbox.core.service.IGritsUIService;
import org.grits.toolbox.core.utilShare.ErrorUtils;
import org.grits.toolbox.datamodel.ms.tablemodel.MassSpecTableDataObject;
import org.grits.toolbox.entry.ms.adaptor.MassSpecExportFileAdapter;
import org.grits.toolbox.entry.ms.dialog.MassSpecExportDialog;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.grits.toolbox.entry.ms.views.tabbed.MassSpecMultiPageViewer;

/**
 * Export command. call SimianExportDialog.
 * 
 * @author kitaemyoung
 * 
 */
public class MassSpecExportCommand {
	private static final Logger logger = Logger.getLogger(MassSpecExportCommand.class);

	private Entry entry = null;
	private MassSpecTableDataObject tableDataObject = null;
	
	@Inject static IGritsDataModelService gritsDataModelService = null;
    @Inject static IGritsUIService gritsUIService = null;
    @Inject IEclipseContext context;

	@Execute
	public Object execute(@Named(IServiceConstants.ACTIVE_SELECTION) Object object,
			@Named (IServiceConstants.ACTIVE_SHELL) Shell shell) {

		if (checkIfCorrectEntryChosen(object)) {
			// need to show a dialog which contains three elements in a list:
			// SimGlycanCSV, XML, and load data into XML to export
			createSimianExportDialog(shell);
		} else {
			// need to show dialog saying please choose a simGlycanEntry
			ErrorUtils.createWarningMessageBox(
					shell, "Invalid Entry",	"An MS entry must be open and active in order to export.");
		}
		return null;
	}

	private void createSimianExportDialog(Shell activeShell) {
		MassSpecExportFileAdapter adapter = new MassSpecExportFileAdapter();
		MassSpecExportDialog dialog = new MassSpecExportDialog(
				PropertyHandler.getModalDialog(activeShell), adapter);
		// set parent entry
		dialog.setMassSpecEntry(entry);
		dialog.setTableDataObject(tableDataObject);
		if (dialog.open() == Window.OK) {
			// to do something..
		}
	}
	
	@CanExecute
	public boolean canExecute (@Named(IServiceConstants.ACTIVE_SELECTION) Object object) {
		return checkIfCorrectEntryChosen(object);
	}

	private boolean checkIfCorrectEntryChosen( Object selection ) {
		Entry selectedEntry = null;
		if(selection instanceof Entry)
		{
			selectedEntry = (Entry) selection;
		}
		else if (selection instanceof StructuredSelection)
		{
			if(((StructuredSelection) selection).getFirstElement() instanceof Entry)
			{
				selectedEntry = (Entry) ((StructuredSelection) selection).getFirstElement();
			}
		}
		// try getting the last selection from the data model
		if(selectedEntry == null
				&& gritsDataModelService.getLastSelection() != null
				&& gritsDataModelService.getLastSelection().getFirstElement() instanceof Entry)
		{
			selectedEntry = (Entry) gritsDataModelService.getLastSelection().getFirstElement();
		}
        
		if (selectedEntry == null)
			return false;
				
		
		this.entry = selectedEntry;

		//find out which property
		if (entry.getProperty().getType().equals(MassSpecProperty.TYPE)) {
			try {
				MassSpecMultiPageViewer viewer = MassSpecMultiPageViewer.getActiveViewerForEntry(context, entry);
				if ( viewer == null ) {
					return false;
				}
				MassSpecTableDataObject data = (MassSpecTableDataObject) viewer.getScansView().getViewBase().getNatTable().getGRITSTableDataObject();
				this.tableDataObject = data;
				return true;
			} catch( Exception e ) {
				logger.error(e.getMessage(), e);
				return false;
			}

		}
		else {
			return false;
		}
	}
}
