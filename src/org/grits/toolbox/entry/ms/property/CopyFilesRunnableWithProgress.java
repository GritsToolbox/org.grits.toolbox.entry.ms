package org.grits.toolbox.entry.ms.property;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.PropertyDataFile;
import org.grits.toolbox.core.utilShare.CopyUtils;
import org.grits.toolbox.entry.ms.property.datamodel.MSPropertyDataFile;
import org.grits.toolbox.ms.file.MSFileInfo;

public class CopyFilesRunnableWithProgress implements IRunnableWithProgress {
	private static final Logger logger = Logger.getLogger(CopyFilesRunnableWithProgress.class);
	
	private final Entry entry;
	private boolean bSuccess;
	private boolean bCanceled;
	private String msPath;
	private String msEntryPath;

	private List<MSPropertyDataFile> allFiles;

	public CopyFilesRunnableWithProgress(List<MSPropertyDataFile> allFiles, Entry newMSEntry,
			String sMsPath, String entryPath) {
		this.allFiles = allFiles;
		this.entry = newMSEntry;
		this.bSuccess = false;
		this.bCanceled = false;
		this.msPath = sMsPath;
		this.msEntryPath = entryPath;
	}

	public boolean iCanceled() {
		return bCanceled;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
	InterruptedException {
		try{
			this.bSuccess = false;
			if( monitor != null ) {
				monitor.beginTask("Copying files into workspace....", IProgressMonitor.UNKNOWN);
			}
			long startTime = System.currentTimeMillis();
			try {
				copyFilesToLocalFolder( allFiles, entry, msPath, msEntryPath, monitor );
				this.bSuccess = true;
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			long endTime = System.currentTimeMillis();
			logger.debug("Time : " + (endTime - startTime));
			if( monitor != null ) {
				this.bCanceled = monitor.isCanceled();
			}

		} catch( Exception ex ) {
			logger.error("Error encountered performing annotation.", ex);
		} finally{
			if( monitor != null ) {
				monitor.done();
			}
		}
	}

	public boolean isSuccessful() {
		return bSuccess;
	}
	
	private void copyFilesToLocalFolder(List<MSPropertyDataFile> allFiles, Entry entry, String msPath, String msEntryPath, IProgressMonitor monitor) {
		boolean bCleanUp = false;
		try {
			if (allFiles == null) {  // nothing to copy
				return;
			}
			
			String[] fileNameArray = null;
			for (MSPropertyDataFile dataFile : allFiles) {
				if (monitor == null || ! monitor.isCanceled()) {
					if (dataFile.getName() != null && ! dataFile.getName().isEmpty()) {
						String endWith = MSFileInfo.getMSFormat(dataFile.getType());
						fileNameArray = getFilePathsInLocalFolder(dataFile.getName(), "." + endWith, msPath, msEntryPath);
						copyFileToLocalFolder(dataFile.getName(), fileNameArray[0], fileNameArray[1], msPath, monitor);
						dataFile.setName(fileNameArray[1]);
					} 
					if (dataFile.getChildren() != null && !dataFile.getChildren().isEmpty()) {
						for (MSPropertyDataFile child : dataFile.getChildren()) {
							String endWith = MSFileInfo.getMSFormat(child.getType());
							fileNameArray = getFilePathsInLocalFolder(child.getName(), "." + endWith, msPath, msEntryPath);
							copyFileToLocalFolder(child.getName(), fileNameArray[0], fileNameArray[1], msPath, monitor);
							child.setName(fileNameArray[1]);
						}
					}
				}
				if( monitor != null && monitor.isCanceled() ) {
					bCleanUp = true;
				}
			}
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
			bCleanUp = true;
		}
		// clean up!
		if( bCleanUp ) {
			for (MSPropertyDataFile dataFile : allFiles) {
				String endWith = MSFileInfo.getMSFormat(dataFile.getType());
				String[] fileNameArray = getFilePathsInLocalFolder(dataFile.getName(), "." + endWith, msPath, msEntryPath);
				CopyFilesRunnableWithProgress.removeFile(fileNameArray[0]);
			}
		}
	}

	/*private void copyFilesToLocalFolder( String origRawFile, String origFullRawFile, String origMzXMLFileName, String origFullMzXMLFileName,		
			Entry entry, String[] _sTempMzXMLFiles, String msPath, String msEntryPath, IProgressMonitor monitor ) {

		boolean bCleanUp = false;
		String[] sRawFileName = null;
		String[] sFullRawFileName = null;
		String[] sMzXMLFileName = null;
		String[] sFullMzXMLFileName = null;

		try {
			//			PageOne one = wizard.getOne();

			MassSpecProperty property = (MassSpecProperty) entry.getProperty();
			if( (monitor == null || ! monitor.isCanceled()) && origRawFile != null ) {
				sRawFileName = getFilePathsInLocalFolder(origRawFile, ".raw", msPath, msEntryPath);
				copyFileToLocalFolder(origRawFile, sRawFileName[0], sRawFileName[1], msPath, monitor);
				if( sRawFileName != null ) {
					PropertyDataFile rawFile = MassSpecProperty.getNewRawPropertyDataFile(sRawFileName[1], origRawFile, false);
					property.getMsSettings().addFile((MSPropertyDataFile) rawFile);
				}
			}

			//for getRawFullMSFile
			if( (monitor == null || ! monitor.isCanceled()) && origFullRawFile != null ) {
				sFullRawFileName = getFilePathsInLocalFolder(origFullRawFile, ".full.raw", msPath, msEntryPath);
				copyFileToLocalFolder(origFullRawFile, sFullRawFileName[0], sFullRawFileName[1], msPath, monitor);
				if( sFullRawFileName != null ) {
					PropertyDataFile rawFile = MassSpecProperty.getNewRawPropertyDataFile(sFullRawFileName[1], origFullRawFile, true);
					property.getMsSettings().addFile((MSPropertyDataFile)rawFile);
				}
			}

			String sMzXMLFile = null;
			if ( _sTempMzXMLFiles != null && _sTempMzXMLFiles[0] != null ) {
				sMzXMLFile = _sTempMzXMLFiles[0];
			} else if ( origMzXMLFileName != null && ! origMzXMLFileName.equals(MassSpecProperty.CONVERT_RAW)) {
				sMzXMLFile = origMzXMLFileName;
			}
			if( monitor == null || ! monitor.isCanceled() ) {
				int iLastDot = sMzXMLFile.lastIndexOf(".");
				String sExtension = ".mzXML"; // assuming mzXML
				if( iLastDot >= 0 ) { // just in case there is a dot, then take what we see
					sExtension = sMzXMLFile.substring(iLastDot+1);
				} 
				sMzXMLFileName = getFilePathsInLocalFolder(sMzXMLFile, "." + sExtension, msPath, msEntryPath);
				copyFileToLocalFolder(sMzXMLFile, sMzXMLFileName[0], sMzXMLFileName[1], msPath, monitor);
				if( sMzXMLFileName != null ) {
					PropertyDataFile mzFile = MassSpecProperty.getNewMzPropertyDataFile(sMzXMLFileName[1], sMzXMLFile, false);
					PropertyDataFile parent = property.getRawFile(false);
					if (parent == null) {  // create an empty parent
						parent = new MSPropertyDataFile("", MSFileInfo.MSFORMAT_RAW_CURRENT_VERSION, MSFileInfo.getType(MSFileInfo.MS_FILE, MSFileInfo.MSFORMAT_RAW_TYPE));
					}
					((MSPropertyDataFile) parent).setIsParent(true);
					((MSPropertyDataFile) parent).addChild((MSPropertyDataFile) mzFile);
				}
			}

			String sMzXMLFullFile = null;
			if ( _sTempMzXMLFiles != null && _sTempMzXMLFiles[1] != null ) {
				sMzXMLFullFile = _sTempMzXMLFiles[1];
			} else if ( origFullMzXMLFileName != null && ! origFullMzXMLFileName.equals(MassSpecProperty.CONVERT_RAW)) {
				sMzXMLFullFile = origFullMzXMLFileName;
			}
			//for getMzxmlFileName	
			if( (monitor == null || ! monitor.isCanceled()) && sMzXMLFullFile != null ) {
				int iLastDot = sMzXMLFullFile.lastIndexOf(".");
				String sExtension = ".mzXML"; // assuming mzXML
				if( iLastDot >= 0 ) {
					sExtension = sMzXMLFullFile.substring(iLastDot+1);
				}
				sFullMzXMLFileName = getFilePathsInLocalFolder(sMzXMLFullFile, ".full." + sExtension, msPath, msEntryPath);
				copyFileToLocalFolder(sMzXMLFullFile, sFullMzXMLFileName[0], sFullMzXMLFileName[1], msPath, monitor);
				//		two.setMzXMLFullFileName(sFullMzXMLFileName);
				if( sFullMzXMLFileName != null ) {
					PropertyDataFile mzFile = MassSpecProperty.getNewMzPropertyDataFile(sFullMzXMLFileName[1], sMzXMLFullFile, true);
					PropertyDataFile fullMSFile = ((MassSpecProperty) property).getRawFile(true);
					if (fullMSFile == null) { // create an empty parent
						fullMSFile = new MSPropertyDataFile("", MSFileInfo.MSFORMAT_RAW_CURRENT_VERSION, MSFileInfo.getType(MSFileInfo.MS_FULL_FILE, MSFileInfo.MSFORMAT_RAW_TYPE));
					} 
					((MSPropertyDataFile) fullMSFile).setIsParent(true);
					((MSPropertyDataFile) fullMSFile).addChild((MSPropertyDataFile)mzFile);
//					String sMsType = sExtension.toUpperCase().equals( MSFileInfo.MSFORMAT_MZXML_TYPE.toUpperCase() ) ? MSFileInfo.MSFORMAT_MZXML_TYPE : MSFileInfo.MSFORMAT_MZML_TYPE;
//					String sType = MSFileInfo.getType(MSFileInfo.MS_FULL_FILE, sMsType);
//					String sVersion = sExtension.toUpperCase().equals( MSFileInfo.MSFORMAT_MZXML_TYPE.toUpperCase() ) ? MSFileInfo.MSFORMAT_MZXML_CURRENT_VERSION : MSFileInfo.MSFORMAT_MZML_CURRENT_VERSION;
//					PropertyDataFile msFile = new PropertyDataFile(sFullMzXMLFileName[1], sVersion, sType);
//					property.getDataFiles().add(msFile);
				}
			}
			if( monitor != null && monitor.isCanceled() ) {
				bCleanUp = true;
			}
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
			bCleanUp = true;
		}
		// clean up!
		if( bCleanUp ) {
			if( sRawFileName != null ) {
				CopyFilesRunnableWithProgress.removeFile(sRawFileName[0]);
			}
			if( sFullRawFileName != null ) {
				CopyFilesRunnableWithProgress.removeFile(sFullRawFileName[0]);
			}
			if( sMzXMLFileName != null ) {
				CopyFilesRunnableWithProgress.removeFile(sMzXMLFileName[0]);
			}
			if( sFullMzXMLFileName != null ) {
				CopyFilesRunnableWithProgress.removeFile(sFullMzXMLFileName[0]);
			}
		}
	}*/

	private static void removeFile( String fileName ) {
		try {
			if( fileName == null || fileName.equals("") )
				return;

			File file1 = new File(fileName);
			if( file1.exists() ) {
				file1.delete();
			}		
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
	}

	/*public void copyPeakListFiles( String peakListFileName, String peakListFileFormat, Entry entry, String msPath, String msEntryPath, IProgressMonitor monitor ) {
		boolean bCleanUp = false;
		String[] sPeakListName = null;
		try {
			sPeakListName = getFilePathsInLocalFolder(peakListFileName, ".xml", msPath, msEntryPath);
			copyFileToLocalFolder(peakListFileName, sPeakListName[0], sPeakListName[1],msPath, monitor);
			//			two.setPeakListFileName(sPeakListName[1]);

			if( monitor != null && monitor.isCanceled() ) {
				bCleanUp = true;
			} else {
				MassSpecProperty property = (MassSpecProperty) entry.getProperty();
				PropertyDataFile peakListFile = MassSpecProperty.getNewPeakListPropertyDataFile(sPeakListName[1], peakListFileName, peakListFileFormat);
				PropertyDataFile parent = property.getRawFile(true);
				if (parent == null) {
					parent = property.getRawFile(false);
					if (parent == null) {
						parent = new MSPropertyDataFile("", MSFileInfo.MSFORMAT_RAW_CURRENT_VERSION, MSFileInfo.getType(MSFileInfo.MS_FULL_FILE, MSFileInfo.MSFORMAT_RAW_TYPE));
					}
				}
				if( peakListFile != null ) {
					((MSPropertyDataFile) parent).addChild((MSPropertyDataFile) peakListFile);
				}
			}
		} catch( IOException ex ) {
			logger.error(ex.getMessage(), ex);
			bCleanUp = true;
		} catch (JAXBException ex) {
			logger.error(ex.getMessage(), ex);
		}
		if( bCleanUp ) {
			if( sPeakListName != null ) {
				CopyFilesRunnableWithProgress.removeFile(sPeakListName[0]);
			}
		}
	}*/

	private String[] getFilePathsInLocalFolder(String filepath, String endWith, String msPath, String msEntryPath) {
		String oldFilePath = filepath.replace("\\", "/");
		int inx = oldFilePath.lastIndexOf(".");
		if( inx > 0 ) {
			oldFilePath = oldFilePath.substring(0, inx) + endWith;
			//					oldFilePath = oldFilePath + endWith;
		}
		//				Date date = new Date();
		//create new rawFileName
		String oldFile = oldFilePath.substring(oldFilePath.lastIndexOf("/")+1);
		String newfileName = msEntryPath + File.separator + oldFile;
		//create a new destination
		//				String newPath = workspaceLocation + projectName + File.separator + MassSpecProperty.getFoldername() + File.separator + newfileName;
		String newPath = msPath + File.separator + newfileName;		
		return new String[] {newPath, newfileName};
	}
	
	private void copyFileToLocalFolder(String filepath, String newPath, String newFileName, String msPath, IProgressMonitor monitor) throws IOException, JAXBException {
		if(filepath != null) {
			if(filepath.length() > 0) {
				CopyUtils.copyFilesFromTo(filepath, newPath, monitor);
				//addFilesToLockFile (newFileName, msPath);
			}
		}
	}
	
	/*private void addFilesToLockFile (String filePath, String msEntryPath) throws IOException, JAXBException {
		FileLockManager mng = FileLockingUtils.readLockFile(msEntryPath + File.separator + FileLockManager.LOCKFILE_NAME);
		mng.addFile(filePath);
		FileLockingUtils.writeLockFile(mng, msEntryPath + File.separator + FileLockManager.LOCKFILE_NAME);	
	}*/
}
