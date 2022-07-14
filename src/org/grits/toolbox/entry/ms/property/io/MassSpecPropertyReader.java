package org.grits.toolbox.entry.ms.property.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.grits.toolbox.core.dataShare.PropertyHandler;
import org.grits.toolbox.core.datamodel.UnsupportedTypeException;
import org.grits.toolbox.core.datamodel.UnsupportedVersionException;
import org.grits.toolbox.core.datamodel.io.PropertyReader;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.datamodel.property.PropertyDataFile;
import org.grits.toolbox.core.utilShare.XMLUtils;
import org.grits.toolbox.entry.ms.preference.MassSpecPreference;
import org.grits.toolbox.entry.ms.property.FileLock;
import org.grits.toolbox.entry.ms.property.FileLockManager;
import org.grits.toolbox.entry.ms.property.FileLockingUtils;
import org.grits.toolbox.entry.ms.property.LockEntry;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.grits.toolbox.entry.ms.property.datamodel.ExternalQuantAlias;
import org.grits.toolbox.entry.ms.property.datamodel.ExternalQuantFileToAlias;
import org.grits.toolbox.entry.ms.property.datamodel.MSPropertyDataFile;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecMetaData;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecUISettings;
import org.grits.toolbox.ms.file.FileCategory;
import org.grits.toolbox.ms.file.MSFileInfo;
import org.grits.toolbox.ms.file.PeakListInfo;
import org.jdom.Element;

/**
 * Reader for sample entry. Should check for empty values
 * @author Brent Weatherly
 *
 */
public class MassSpecPropertyReader extends PropertyReader
{
	private static final Logger logger = Logger.getLogger(MassSpecPropertyReader.class);

	@Override
	public Property read(Element propertyElement) throws IOException, UnsupportedVersionException
	{
		MassSpecProperty property = new MassSpecProperty();

		PropertyReader.addGenericInfo(propertyElement, property);

		if(property.getVersion() == null) {
			// we must also convert the meta-data to the model and write out. Do that here?
			// get the sampleGroup folder
			try {
				MassSpecReaderVersion0.read(propertyElement, property);
				property.setVersion(MassSpecProperty.CURRENT_VERSION);
				if( property.getMassSpecMetaData() != null ) {
					property.getMassSpecMetaData().setVersion(MassSpecMetaData.CURRENT_VERSION);
				}
				convertIntoMSPropertyDataFiles(property, propertyElement);
				addFilesToLockManager(propertyElement, property);
				updateMassSpecMetaData(property, propertyElement);
				PropertyReader.UPDATE_PROJECT_XML = true;
			} catch (UnsupportedTypeException e) {
				throw new IOException(e.getMessage(), e);
			}
		}
		else if(property.getVersion().equals("1.0")) {
			MassSpecReaderVersion1.read(propertyElement, property);
			property.setVersion(MassSpecProperty.CURRENT_VERSION);
			if( property.getMassSpecMetaData() != null ) {
				property.getMassSpecMetaData().setVersion(MassSpecMetaData.CURRENT_VERSION);
			}
			convertIntoMSPropertyDataFiles(property, propertyElement);
			addFilesToLockManager(propertyElement, property);
			updateMassSpecMetaData(property, propertyElement);
			PropertyReader.UPDATE_PROJECT_XML = true;
		}
		else if(property.getVersion().equals("1.1")) {
			MassSpecReaderVersion1_1.read(propertyElement, property);
			if( property.getMassSpecMetaData() != null ) {
				property.getMassSpecMetaData().setVersion(MassSpecMetaData.CURRENT_VERSION);
			}
			property.setVersion(MassSpecProperty.CURRENT_VERSION);
			convertIntoMSPropertyDataFiles(property, propertyElement);
			addFilesToLockManager(propertyElement, property);
			updateMassSpecMetaData(property, propertyElement);
			PropertyReader.UPDATE_PROJECT_XML = true;
		}
		else if(property.getVersion().equals("1.2")) {
			MassSpecReaderVersion1_2.read(propertyElement, property);
			property.setVersion(MassSpecProperty.CURRENT_VERSION);
			convertIntoMSPropertyDataFiles(property, propertyElement);
			addFilesToLockManager(propertyElement, property);
			updateMassSpecMetaData(property, propertyElement);
			PropertyReader.UPDATE_PROJECT_XML = true;
		} else if (property.getVersion().equals("1.3")) {
			// Note that there are no differences between the 1.2 reader for the 1.3 property, so we call the 1.2 reader
			// Note 01/12/18: Added standard quant so new 1.3 reader...now call it!
			MassSpecReaderVersion1_3.read(propertyElement, property);
			updateMassSpecMetaData(property, propertyElement);
			adjustFilePathsForLockFile (propertyElement, property);
			PropertyReader.UPDATE_PROJECT_XML = true;
		}
		else 
			throw new UnsupportedVersionException("This version is currently not supported.", property.getVersion());


		return property;
	}

	protected void adjustFilePathsForLockFile(Element propertyElement, MassSpecProperty property) {
		try {
			FileLockManager lockMng = null; 
			// add all the files to the .lockFile, if not added before
			String metadataPath = property.getMSSettingsFile().getName();
			String lockFilePath = getMSFolder(propertyElement) + File.separator;
			if (metadataPath.lastIndexOf(File.separator) != -1) {
				lockFilePath = lockFilePath + 
						metadataPath.substring(0, metadataPath.lastIndexOf(File.separator)) + File.separator + FileLockManager.LOCKFILE_NAME;
			} else
				lockFilePath += FileLockManager.LOCKFILE_NAME;
			lockMng = FileLockingUtils.readLockFile(lockFilePath);
			for (FileLock file: lockMng.getFileList()) {
				if( file.getFilename().contains("\\") && ! File.separator.equals("\\") ) {
					file.setFilename( file.getFilename().replace("\\", File.separator));
				} else if( file.getFilename().contains("/") && ! File.separator.equals("/") ){
					file.setFilename( file.getFilename().replace("/", File.separator));
				}
			}
			FileLockingUtils.writeLockFile(lockMng, lockFilePath);
		} catch (JAXBException e) {
			logger.error("Could not fix file paths in the lock file. Locking mechanism may not work", e);
		} catch (IOException e) {
			logger.error("Could not fix file paths in the lock file. Locking mechanism may not work", e);
		}	
	}

	String getMSFolder (Element propertyElement) {
		Element entryElement = propertyElement.getDocument().getRootElement().getChild("entry");
		String projectName = entryElement == null ? null : entryElement.getAttributeValue("name");

		String workspaceFolder = PropertyHandler.getVariable("workspace_location");
		String msFolder = workspaceFolder.substring(0, workspaceFolder.length()-1) 
				+ File.separator
				+ projectName + File.separator
				+ MassSpecProperty.getFoldername();
		return msFolder;
	}

	/**
	 * find all children "entry" element for the "entry" element with the given propertyId
	 * 
	 * @param propertyElement the root element to start the search
	 * @param propertyId the id of the element for which the children will be returned
	 * @return list of LockEntry object for all the children of the entry with the given id, an empty list if not entry is found or no children 
	 */
	List<LockEntry> getAllChildrenEntries (Element propertyElement, String propertyId) {
		List <LockEntry> children = new ArrayList<>();
		Element matchEntry = findEntryElementById (propertyElement, propertyId);
		if (matchEntry != null) {  // this should be the MS Entry, get all child annotation entries and add them to the list
			for(Object ch : matchEntry.getChildren("entry")){
				if(ch instanceof Element) {
					String entryId = ((Element) ch).getAttributeValue("id");
					String displayName = ((Element) ch).getAttributeValue("name");
					LockEntry entry = new LockEntry();
					entry.setEntryId(Integer.parseInt(entryId));
					entry.setEntryName(displayName);
					children.add(entry);
				}
			}
		}
		return children;
	}

	/**
	 * recursive method to find the "entry" element with the given id
	 * 
	 * @param entryElement root element to start the search
	 * @param propertyId the id to look for
	 * @return Element ("entry") with the given id or null if not found
	 */
	private Element findEntryElementById(Element entryElement, String propertyId) {
		Element element = null;
		for(Object ch : entryElement.getChildren("entry")){
			if(ch instanceof Element) {
				if (((Element) ch).getAttributeValue("id").equals(propertyId)) {
					element = (Element) ch;
					break;
				} else {
					// look in its children
					Element e =  findEntryElementById ((Element) ch, propertyId);
					if (e != null) {
						element = e;
						break;
					}
				}
			}
		}
		return element;
	}

	/**
	 * make sure lock manager contains all the files used in earlier versions
	 * 
	 * @param propertyElement
	 * @param property
	 * @throws IOException
	 */
	void addFilesToLockManager (Element propertyElement, MassSpecProperty property) throws IOException {
		try {
			FileLockManager lockMng = null; 
			// add all the files to the .lockFile, if not added before
			String metadataPath = property.getMSSettingsFile().getName();
			String lockFilePath = getMSFolder(propertyElement) + File.separator;
			if (metadataPath.lastIndexOf(File.separator) != -1) {
				lockFilePath = lockFilePath + 
						metadataPath.substring(0, metadataPath.lastIndexOf(File.separator)) + File.separator + FileLockManager.LOCKFILE_NAME;
			} else
				lockFilePath += FileLockManager.LOCKFILE_NAME;
			lockMng = FileLockingUtils.readLockFile(lockFilePath);
			if (property.getMassSpecMetaData().getFileList() == null)
				return;
			for (PropertyDataFile file : property.getMassSpecMetaData().getFileList()) {
				if (file instanceof MSPropertyDataFile) { // has to be
					if (file.getName() != null && !file.getName().isEmpty()) // we might have empty parent files 
						lockMng.addFile(file.getName());
					// we only allow one level of hierarchy
					if (((MSPropertyDataFile) file).getChildren() != null) {
						for (MSPropertyDataFile child: ((MSPropertyDataFile) file).getChildren()) {
							lockMng.addFile(child.getName());
							// handle external quantification
							if( child.getCategory().equals(FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY )) {
								ExternalQuantAlias eqa = new ExternalQuantAlias();
								eqa.setAlias(MSPropertyDataFile.getLegacyExternalQuantAlias(child));
								//							eqa.setKeyId(MassSpecUISettings.getRandomId());
								property.getMassSpecMetaData().addExternalQuantFile(child, eqa);
								String sExtQuantType = MassSpecUISettings.getExternalQuantType(child);
								ExternalQuantFileToAlias mAliases = property.getMassSpecMetaData().getExternalQuantToAliasByQuantType(sExtQuantType);
								property.getMassSpecMetaData().updateQuantAliasKeyInfo(child, mAliases, null);
//								property.getMassSpecMetaData().updateExternalQuantAliases(child);

								//							property.getMassSpecMetaData().getSourceDataFileNameToAlias().put(child.getName(), MSPropertyDataFile.getFormattedName(child));
							}
						}
					}
				}
			}

			// Mark all files as "in use" by all the children entries
			String propertyId = propertyElement.getAttributeValue("id");
			Element entryElement = propertyElement.getDocument().getRootElement().getChild("entry");
			List<LockEntry> entries = getAllChildrenEntries(entryElement, propertyId);
			for (MSPropertyDataFile file: property.getMassSpecMetaData().getFileList()) {
				if (file.getChildren() != null) {
					for (MSPropertyDataFile child : file.getChildren()) {  // do not lock "instrument" files (parent), only children
						for (LockEntry entry: entries) {
							lockMng.lockFile(child.getName(), entry);
							if( child.getCategory() == FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY ) {
								ExternalQuantAlias eqa = new ExternalQuantAlias();
								eqa.setAlias(MSPropertyDataFile.getLegacyExternalQuantAlias(child));
								//							eqa.setKeyId(MassSpecUISettings.getRandomId());
								property.getMassSpecMetaData().addExternalQuantFile(child, eqa);
								String sExtQuantType = MassSpecUISettings.getExternalQuantType(child);
								ExternalQuantFileToAlias mAliases = property.getMassSpecMetaData().getExternalQuantToAliasByQuantType(sExtQuantType);
								property.getMassSpecMetaData().updateQuantAliasKeyInfo(child, mAliases, null);
								//							property.getMassSpecMetaData().getSourceDataFileNameToAlias().put(child.getName(), MSPropertyDataFile.getFormattedName(child));
							}
						}
					}
				}
			}

			FileLockingUtils.writeLockFile(lockMng, lockFilePath);
		} catch (JAXBException e) {
			logger.error("Could not add files into the lock file. Locking mechanism may not work", e);
		}
	}

	/**
	 * convert the old type of data files to the newer version (1.3) by adding category and purpose attributes
	 * also create the hierarchy of file, raw file is the parent, the rest are the children
	 * add these files under MassSpecMetaData (not to the Property anymore)
	 * 
	 * @param property whose files are to be converted
	 * @param propertyElement 
	 */
	void convertIntoMSPropertyDataFiles (Property property, Element propertyElement) {
		List<PropertyDataFile> dataFiles = new ArrayList<>();
		List<MSPropertyDataFile> settingsDataFiles = new ArrayList<MSPropertyDataFile>();
		if (property.getDataFiles() == null)
			return;

		MSPropertyDataFile parentFile = null;

		if (property instanceof MassSpecProperty) {
			// find the settings file and put all files under that
			MassSpecMetaData settings = ((MassSpecProperty) property).getMassSpecMetaData();
			if (settings != null) {
				boolean fullMS = false;
				// find the raw data file if exists
				PropertyDataFile rawFile = getRawFile(property, false);
				String sRawFileName = rawFile == null ? "" : rawFile.getName();
				String sVersion = rawFile == null ? "1.0" : rawFile.getVersion();
				String sType = rawFile == null ? MSFileInfo.getType(MSFileInfo.MS_FILE, MSFileInfo.MSFORMAT_RAW_TYPE) : rawFile.getType();
				parentFile = MSPropertyDataFile.getCurrentVersionFileFromLegacyVersion(sRawFileName, sVersion, sType);
				parentFile.setIsParent(true);

				MSPropertyDataFile extractFile = null;
				for (PropertyDataFile propertyDataFile : property.getDataFiles()) {
					if (propertyDataFile.getType().equals(MSFileInfo.MSLOCKFILE_TYPE)) {
						dataFiles.add(propertyDataFile);
						continue;
					}
					if (propertyDataFile.getType().equals(MSFileInfo.getType(MSFileInfo.MS_FILE, MSFileInfo.MSFORMAT_RAW_TYPE))) // skip the raw file, it is already processed as the parent file
						continue;
					if (propertyDataFile.getType().equals(MSFileInfo.getType(MSFileInfo.MS_FULL_FILE, MSFileInfo.MSFORMAT_RAW_TYPE))) // skip the raw file, it is already processed as the parent file
						continue;
					if (propertyDataFile.getType().equals(MSFileInfo.MSMETADATA_TYPE)) { // this one should stay under the property
						dataFiles.add(propertyDataFile);
						continue;
					} else if (propertyDataFile.getType().equals(MSFileInfo.getType(MSFileInfo.MS_FULL_FILE, MSFileInfo.MSFORMAT_MZML_TYPE)) ||
							propertyDataFile.getType().equals(MSFileInfo.getType(MSFileInfo.MS_FULL_FILE, MSFileInfo.MSFORMAT_MZXML_TYPE))) {   // will be handled in the next case
						fullMS = true;
						continue; 
					} else if (propertyDataFile.getType().equals(PeakListInfo.PEAKLISTFORMAT_EXTRACT_TYPE))  { // needs to be added to the full file
						extractFile = MSPropertyDataFile.getCurrentVersionFileFromLegacyVersion(propertyDataFile.getName(), propertyDataFile.getVersion(), propertyDataFile.getType());
						continue;
					}

					// if passed all conditionals, should be the annotation MS/MS file
					MSPropertyDataFile newDataFile = MSPropertyDataFile.getCurrentVersionFileFromLegacyVersion(propertyDataFile.getName(), propertyDataFile.getVersion(), propertyDataFile.getType());
					if( newDataFile != null ) {
						parentFile.addChild(newDataFile);
					}
				}

				MSPropertyDataFile parentFile2 = null;
				PropertyDataFile fullMSFile = getRawFile(property, true);

				if (fullMSFile == null) {
					if (fullMS) {
						// create an empty parent
						parentFile2 = MSPropertyDataFile.getCurrentVersionFileFromLegacyVersion("", MSFileInfo.MSFORMAT_RAW_CURRENT_VERSION, MSFileInfo.MSFORMAT_RAW_TYPE);
						parentFile2.setIsParent(true);
					}
					if (!fullMS && extractFile != null) {  // no full-ms parent, add extract to the raw data parent
						// need to add it to the parentFile
						parentFile.addChild(extractFile);
					}
				} else {
					parentFile2 = MSPropertyDataFile.getCurrentVersionFileFromLegacyVersion(fullMSFile.getName(), fullMSFile.getVersion(), fullMSFile.getType());
					parentFile2.setIsParent(true);
				}

				if (parentFile2 != null) {
					for (PropertyDataFile propertyDataFile : property.getDataFiles()) {
						if (propertyDataFile.getType().equals(MSFileInfo.MSLOCKFILE_TYPE))
							continue;
						if (propertyDataFile.getType().equals(MSFileInfo.getType(MSFileInfo.MS_FILE, MSFileInfo.MSFORMAT_RAW_TYPE))) // skip the raw file, it is already processed as the parent file
							continue;
						if (propertyDataFile.getType().equals(MSFileInfo.getType(MSFileInfo.MS_FULL_FILE, MSFileInfo.MSFORMAT_RAW_TYPE))) // skip the raw file, it is already processed as the parent file
							continue;
						if (propertyDataFile.getType().equals(MSFileInfo.MSMETADATA_TYPE)) { 
							continue;
						} else if (propertyDataFile.getType().equals(MSFileInfo.getType(MSFileInfo.MS_FILE, MSFileInfo.MSFORMAT_MZML_TYPE)) ||
								propertyDataFile.getType().equals(MSFileInfo.getType(MSFileInfo.MS_FILE, MSFileInfo.MSFORMAT_MZXML_TYPE))) {    // handled in the previous case
							continue; 
						}
						MSPropertyDataFile newDataFile = MSPropertyDataFile.getCurrentVersionFileFromLegacyVersion(propertyDataFile.getName(), propertyDataFile.getVersion(), propertyDataFile.getType());
						parentFile2.addChild(newDataFile);
					}
					settingsDataFiles.add(parentFile2);
				}
				settingsDataFiles.add(parentFile);
				settings.setFileList(settingsDataFiles);
				//				String settingsFilePath = getMSFolder(propertyElement) + File.separator + ((MassSpecProperty) property).getMSSettingsFile().getName();
				//				MassSpecProperty.updateMSSettings(settings, settingsFilePath);
			}
			property.getDataFiles().clear();
			property.setDataFiles(dataFiles);
		}
	}

	protected void updateMassSpecMetaData(Property property, Element propertyElement) {
		if (property instanceof MassSpecProperty) {
			// find the settings file and put all files under that
			MassSpecMetaData settings = ((MassSpecProperty) property).getMassSpecMetaData();
			if (settings != null) {		
				String settingsFilePath = getMSFolder(propertyElement) + File.separator + ((MassSpecProperty) property).getMSSettingsFile().getName();
				((MassSpecProperty) property).updateMSSettings(settings, settingsFilePath);
			}
		}
	}

	private PropertyDataFile getRawFile (Property property, boolean isFull) {
		Iterator<PropertyDataFile> itr = property.getDataFiles().iterator();
		while( itr.hasNext() ) {
			PropertyDataFile file = itr.next();
			if( isFull && file.getType().equals( MSFileInfo.getType(MSFileInfo.MS_FULL_FILE, MSFileInfo.MSFORMAT_RAW_TYPE) ) ) {
				return file;
			}
			if( ! isFull && file.getType().equals( MSFileInfo.getType(MSFileInfo.MS_FILE, MSFileInfo.MSFORMAT_RAW_TYPE) ) ) {
				return file;
			}
		}
		return null;
	}

	protected static MassSpecMetaData getMassSpecMetaDataFromXML(String xmlString) {
		MassSpecMetaData metaData = (MassSpecMetaData) XMLUtils.getObjectFromXML(xmlString, MassSpecPreference.class);
		return metaData;
	}

}
