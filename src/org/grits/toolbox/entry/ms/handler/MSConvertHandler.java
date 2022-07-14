package org.grits.toolbox.entry.ms.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import javax.inject.Named;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.core.datamodel.UnsupportedVersionException;
import org.grits.toolbox.entry.ms.dialog.MSConvertDialog;
import org.grits.toolbox.entry.ms.preference.MSConvertPreference;

public class MSConvertHandler {
	private static final Logger logger = Logger.getLogger(MSConvertHandler.class);
	
	public static final String PARAMETER = "instrumentfile_Path";
	public static final String PARAMETER2 = "msPath";
	public static final String COMMAND_ID = "org.grits.toolbox.entry.ms.command.msconvert";

	String convertedFile = null;
	String scanNumberFilter = null;
	String scanTimeFilter = null;
	
	String scanNumberExt = "";
	String scanTimeExt = "";
	
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
		try
	    {
			MSConvertPreference preferences = MSConvertPreference.loadPreferences();
			if (preferences == null || preferences.getLocation() == null || preferences.getFileType() == null) {
				MessageDialog.openError(shell, "Error", "Please set the preferences for MSConvert first!");
				return null;
			}
			
			this.scanNumberFilter = null;
			this.scanTimeFilter = null;
			this.scanNumberExt = "";
			this.scanTimeExt = "";
			MSConvertDialog argumentDialog = new MSConvertDialog(shell);
			if (argumentDialog.open() == Window.OK) {
				Integer scanNumberB = argumentDialog.getScanNumberBegin();
				Integer scanNumberE = argumentDialog.getScanNumberEnd();
				if (scanNumberB != null && scanNumberE != null) {
					scanNumberFilter = "scanNumber " + scanNumberB + "-" + scanNumberE + " ";
					scanNumberExt = "_" + scanNumberB + "_" + scanNumberE;
				}
				Double scanTimeB = argumentDialog.getScanTimeBegin();
				Double scanTimeE = argumentDialog.getScanTimeEnd();
				if (scanTimeB != null && scanTimeE != null) {
					scanTimeFilter = "scanTime [" + scanTimeB + "-" + scanTimeE + "] ";
					scanTimeExt = "_" + scanTimeB + "_" + scanTimeE;
				}
			} else {
				return null;
			}
			IRunnableWithProgress runnable = new IRunnableWithProgress() { 
		        @Override
		        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		        	try {
		        		monitor.beginTask("Converting " + file + "...", IProgressMonitor.UNKNOWN);
			        	Runtime r = Runtime.getRuntime();
			        	String fullPath = msPath + File.separator + file;
			        	String outputDir = msPath + File.separator + file.substring(0, file.lastIndexOf(File.separator));
			        	// file format: mzxml mzml (--mzxml or --mzml)
			        	// 32 bit encoding (--32)
			        	// write index (this is default)
			        	// use zlib compression (--zlib)
			        	// tpp compatibility (?)
		                String tppline = "\"titleMaker <RunId>.<ScanNumber>.<ScanNumber>.<ChargeState> File:\"<SourcePath>\", NativeID:\"<Id>\"\"";
				        
				        String[] cmd = null;
				        if (scanTimeFilter != null && scanNumberFilter != null) 
				        	cmd = new String[] {preferences.getLocation() + File.separator + "msconvert", fullPath, "--32", "--zlib", "--ignoreUnknownInstrumentError", "--" + preferences.getFileType(), 
				        		"-o", outputDir, "--filter", tppline, "--filter", scanTimeFilter, "--filter", scanNumberFilter};
				        else if (scanTimeFilter != null)
				        	cmd = new String[] {preferences.getLocation() + File.separator + "msconvert", fullPath, "--32", "--zlib", "--ignoreUnknownInstrumentError", "--" + preferences.getFileType(), 
					        		"-o", outputDir, "--filter", tppline, "--filter", scanTimeFilter};
				        else if (scanNumberFilter != null)
				        	cmd = new String[] {preferences.getLocation() + File.separator + "msconvert", fullPath, "--32", "--zlib", "--ignoreUnknownInstrumentError", "--" + preferences.getFileType(), 
					        		"-o", outputDir, "--filter", tppline, "--filter", scanNumberFilter};
				        else 
				        	cmd = new String[] {preferences.getLocation() + File.separator + "msconvert", fullPath, "--32", "--zlib", "--ignoreUnknownInstrumentError", "--" + preferences.getFileType(), 
					        		"-o", outputDir, "--filter", tppline};
				        logger.info("Executing command: " );
				        for (int i = 0; i < cmd.length; i++) {
							logger.info (" " + cmd[i]);
						}
		                Process p = r.exec(cmd);
				        BufferedReader bfr = new BufferedReader(new InputStreamReader(p.getInputStream()));
				        String line = "";
				        StringBuffer errorString = new StringBuffer();
				        while(bfr.ready()) {
				        	line = bfr.readLine();
				        	// display each output line form msconvert
				        	logger.info(line);
				        	errorString.append(line + "\n");
				        }
				        bfr.close();
				        
				        BufferedReader errorReader = new BufferedReader(
				                new InputStreamReader(p.getErrorStream()));
				        while (errorReader.ready()) {
				        	line = errorReader.readLine();
				        	logger.debug(line);
				            errorString.append(line + "\n");
				        }
				        errorReader.close();
				        
				        int exitValue = p.waitFor();
				        if (exitValue != 0) {
				            logger.error("MSConvert failed! Reason: " + errorString);
				            throw new InvocationTargetException(new Throwable(errorString.toString()));
				        }
				        
				        if (file.lastIndexOf(".") != -1) {
				        	convertedFile = file.substring(0, file.lastIndexOf("."));
				        	convertedFile += "." + preferences.getFileType();
				        }
				        if (!scanTimeExt.isEmpty() || !scanNumberExt.isEmpty()) {
					        File cFile = new File(msPath + File.separator + convertedFile);
					        String newFileName = file.substring(0, file.lastIndexOf("."));
					        if (!scanTimeExt.isEmpty()) 
					        	newFileName += scanTimeExt;
				        	if (!scanNumberExt.isEmpty())
				        		newFileName += scanNumberExt;
				        	newFileName += "." + preferences.getFileType();
				        	cFile.renameTo(new File(msPath + File.separator + newFileName));
				        	convertedFile = newFileName;
				        }
				        monitor.done();   
		        	} catch (Exception e)
		    	    {
		        		throw new InvocationTargetException(e);
		    		}
		        }
			};
			
			ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(shell);
			progressMonitorDialog.run(true, false, runnable);
			return convertedFile;
	    } catch (InvocationTargetException e) {
	    	String cause = "";
	    	if (e.getTargetException() instanceof InvocationTargetException) {
	    		if (((InvocationTargetException) e.getTargetException()).getTargetException() != null)
	    			cause = ((InvocationTargetException) e.getTargetException()).getTargetException().getMessage();
	    	} else if (e.getTargetException() instanceof Throwable) {
	    		cause = e.getTargetException().getMessage();
	    	}
		    if (cause != null && cause.equals("msconvert: not found"))
		        logger.error("No msconvert tool found.");
		    else 
		    	logger.error("Failed to execute MSConvert", e);
		    MessageDialog.openError(shell, "Error", "Failed to execute MSConvert. Reason: " + cause);
	    } catch (InterruptedException e) {
	    	logger.info("MSConvert is interrupted", e);
	    } catch (UnsupportedVersionException e) {
	    	logger.error("Preference version is not supported", e);
	    }
	    return null;
	}
	/**
	 * can execute MSConvert only on a Windows machine
	 * 
	 * @return true if this command can be executed or false if it should be disabled
	 */
	@CanExecute
	public boolean canExecute() {
		return System.getProperty("os.name").startsWith("Windows");	
	}
}