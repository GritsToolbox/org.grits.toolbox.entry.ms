package org.grits.toolbox.entry.ms.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import javax.inject.Named;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.core.datamodel.UnsupportedVersionException;
import org.grits.toolbox.entry.ms.dialog.GlycresoftDialog;
import org.grits.toolbox.entry.ms.preference.GlycresoftPreference;


/**
 * Glycresoft:
 * LC-MS/MS Data Preprocessing and Deconvolution
 * Convert raw mass spectral data files into deisotoped neutral mass peak lists written to a new mzML [Martens2011] file. 
 * For tandem mass spectra, recalculate precursor ion monoisotopic peaks. 
 * This task is computationally intensive, and uses several collaborative processes to share the work.
 * 
 * This class is a wrapper class to execute the above described command-line tool.
 * 
 * @author sena
 *
 */
public class GlycresoftHandler {
	private static final Logger logger = Logger.getLogger(GlycresoftHandler.class);
	
	public static final String PARAMETER = "mzXMLFile_Path";
	public static final String PARAMETER2 = "msPath";
	public static final String COMMAND_ID = "org.grits.toolbox.entry.ms.command.glycresoft";  //deconvolution 
	
	private String processedFile;
	String averagineModel = "glycan";
	
	/**
	 * 
	 * @param shell activeShell injected
	 * @param file name of the file to be processed (injected named parameter)
	 * @param msPath path to the ms entry (injected named parameter)
	 * @return name of the generated processed file
	 */
	@Execute
	public String execute(@Named (IServiceConstants.ACTIVE_SHELL) Shell shell, 
			@Named (PARAMETER) String file, @Named (PARAMETER2) String msPath) {
		try
	    {
			GlycresoftPreference preferences = GlycresoftPreference.loadPreferences();
			if (preferences == null || preferences.getLocation() == null) {
				MessageDialog.openError(shell, "Error", "Please set the preferences for Glycresoft first!");
				return null;
			}
			
			GlycresoftDialog d = new GlycresoftDialog(shell);
			if (d.open() == Window.OK) {
				averagineModel = d.getAveragineModel();
			} else {
				return null;
			}
			
			IRunnableWithProgress runnable = new IRunnableWithProgress() { 
		        
				@Override
		        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		        	try {
		        		monitor.beginTask("Processing " + file + "...", IProgressMonitor.UNKNOWN);
			        	Runtime r = Runtime.getRuntime();
			        	String fullPath = msPath + File.separator + file;
			        	String outputFile = msPath + File.separator + file.substring(0, file.lastIndexOf(".")) + "-glycresoft.mzML";   
			        	String cmdName;
			        	if (System.getProperty("os.name").startsWith("Windows"))
			        		cmdName = "glycresoft-cli.exe";
			        	else
			        		cmdName = "glycresoft-cli";
				        String[] cmd = new String[] {preferences.getLocation() + File.separator + cmdName, "mzml", 
				        		"preprocess", "-a", averagineModel,"-t", "20", "-an", averagineModel, "-tn", "10",
				        		"-b", "5", "-p", "4", "-c", "8", "-m",  "3", "-mn", "1",
				        		fullPath, outputFile};
		                Process p = r.exec(cmd);
				        BufferedReader bfr = new BufferedReader(new InputStreamReader(p.getInputStream()));
				        String line = "";
				        StringBuffer errorString = new StringBuffer();
				        while (bfr.ready()) {
				        	line = bfr.readLine();
				        	// display each output line form glycresoft-cli
				        	logger.info(line);
				        	errorString.append(line + "\n");
				        }
				     /*   while((line = bfr.readLine()) != null) {
				        	// display each output line form glycresoft-cli
				        	logger.info(line);
				        	errorString.append(line + "\n");
				        }*/
				        bfr.close();
				        
				        BufferedReader errorReader = new BufferedReader(
				                new InputStreamReader(p.getErrorStream()));
				        while (errorReader.ready()) {
				        	line = errorReader.readLine();
				        	logger.info(line);
				        	errorString.append(line + "\n");
				        }
				      /*  while ((line = errorReader.readLine()) != null) {
				            errorString.append(line + "\n");
				        }*/
				        errorReader.close();
				        
				        int exitValue = p.waitFor();
				        if (exitValue != 0) {
				            logger.error("glycresoft failed! Exit Value:" + exitValue + " Reason: " + errorString);
				            throw new InvocationTargetException(new Throwable(errorString.toString()));
				        }
				        
				        processedFile = outputFile;
				        monitor.done();   
		        	} catch (Exception e)
		    	    {
		        		throw new InvocationTargetException(e);
		    		}
		        }
			};
			
			ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(shell);
			progressMonitorDialog.run(true, false, runnable);
			return processedFile;
	    } catch (InvocationTargetException e) {
	    	String cause = "";
	    	if (e.getTargetException() instanceof InvocationTargetException) {
	    		if (((InvocationTargetException) e.getTargetException()).getTargetException() != null)
	    			cause = ((InvocationTargetException) e.getTargetException()).getTargetException().getMessage();
	    	} else if (e.getTargetException() instanceof Throwable) {
	    		cause = e.getTargetException().getMessage();
	    	}
		    if (cause != null && cause.equals("glycresoft-cli: not found"))
		        logger.error("No Glycresoft tool found.");
		    else 
		    	logger.error("Failed to execute glycresoft", e);
		    MessageDialog.openError(shell, "Error", "Failed to execute Glycresoft. Reason: " + cause);
	    } catch (InterruptedException e) {
	    	logger.info("Glycresoft is interrupted", e);
	    } catch (UnsupportedVersionException e) {
	    	logger.error("Preference version is not supported", e);
	    }
	    return null;
	}
}
