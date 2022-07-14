package org.grits.toolbox.entry.ms.handler;

import java.io.File;

import javax.inject.Named;

import org.apache.log4j.Logger;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.entry.ms.dialog.SpectraAverageDialog;
import org.grits.toolbox.entry.ms.dialog.process.ProgressReporterDialog;
import org.grits.toolbox.tools.spectrafiltering.om.SpectraFilterSettings;
import org.grits.toolbox.tools.spectrafiltering.process.SpectraAverageThread;

public class SpectraAverageHandler {
	private static final Logger logger = Logger.getLogger(SpectraAverageHandler.class);
	
	public static final String PARAMETER = "mzXMLFile_Path";
	public static final String PARAMETER2 = "msPath";
	public static final String COMMAND_ID = "org.grits.toolbox.entry.ms.command.spectraaverage";

	/**
	 * 
	 * @param shell activeShell injected
	 * @param file name of the file to be converted (injected named parameter)
	 * @param msPath path to the ms entry (injected named parameter)
	 * @return name of the converted file
	 */
	@Execute
	public String execute(@Named (IServiceConstants.ACTIVE_SHELL) Shell shell, 
			@Named (PARAMETER) String file, @Named (PARAMETER2) String msPath) {

		SpectraFilterSettings filter = new SpectraFilterSettings();
		SpectraAverageDialog dlg = new SpectraAverageDialog(shell, filter);
		if ( dlg.open() != Window.OK )
			return null;
		String suffix = "-avgWith"+filter.getAccuracy();
		suffix += (filter.getPPM())? "PPM" : "Dalton";

		filter.setOpenFrom(msPath + File.separator + file);
		filter.setSaveLocation(msPath + File.separator + file.substring(0, file.lastIndexOf("."))+suffix+".mzXML");

		SpectraAverageThread worker = new SpectraAverageThread(filter);
		ProgressReporterDialog dlgProgress = new ProgressReporterDialog(shell);
		dlgProgress.setWorker(worker);
		dlgProgress.open();

		if ( !new File(filter.getSaveLocation()).exists() )
			return null;

		return filter.getSaveLocation();
	}
}