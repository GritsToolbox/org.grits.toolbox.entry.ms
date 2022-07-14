package org.grits.toolbox.entry.ms.property.datamodel;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.Logger;
import org.grits.toolbox.core.datamodel.property.PropertyDataFile;
import org.grits.toolbox.entry.ms.extquantfiles.process.CorrectedQuantColumnInfo;
import org.grits.toolbox.entry.ms.extquantfiles.process.ExternalQuantColumnInfo;
import org.grits.toolbox.entry.ms.extquantfiles.process.ExtractDataProcessor;
import org.grits.toolbox.entry.ms.extquantfiles.process.FullMzXMLDataProcessor;
import org.grits.toolbox.entry.ms.preference.MassSpecPreference;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecCustomAnnotation;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecStandardQuant;
import org.grits.toolbox.ms.file.FileCategory;
import org.grits.toolbox.ms.file.MSFileInfo;
import org.grits.toolbox.ms.file.PeakListInfo;
import org.grits.toolbox.ms.om.data.CustomExtraData;

/**
 * @author Brent Weatherly
 *
 */
public abstract class MassSpecUISettings {
	private static final Logger logger = Logger.getLogger(MassSpecUISettings.class);
	private List<MassSpecCustomAnnotation> customAnnotations = null;
	private String customAnnotationsText = null; // stores the XML that will be written out	
	private List<MassSpecStandardQuant> standardQuant = null;
	private String standardQuantText = null; // stores the XML that will be written out	
	private List<MSPropertyDataFile> fileList;
	private List<MSPropertyDataFile> sourceDataFileList;

	/* quantTypeToExternalQuant overrides the specified peak labels for external quant so the aliases
	 * specified in the annotation show up in the columns.
	 * 
	 * key1: MSFileInfo.getType( 
	 * 		MSFileInfo.MS_FILE_TYPE_DATAFILE (mzXML, mzML)
	 *     	PeakListInfo.PEAKLISTFORMAT_TABDELIMITED_TYPE
	 *  	PeakListInfo.PEAKLISTFORMAT_EXTRACT_TYPE
	 * 		PeakListInfo.PEAKLISTFORMAT_MASCOTGENERIC_TYPE
	 * 
	 * key2: MSPropertyDataFile.getName()
	 * value: ExternalQuantAlias
	 * 
	 * 
	 * Three purposes: 
	 * 1) tags the ms file as in use for an entry, 
	 * 2) gives an alias for the file (file name is the alias if not changed)
	 * 3) gives a unique id for the entry to use for the column header key prefix
	 */
	private HashMap<String, ExternalQuantFileToAlias> quantTypeToExternalQuant = null; 
	private HashMap<String, InternalStandardQuantFileList> internalStandardQuantFiles = null; // list of names of files for internal standard quant
	private HashMap<String, QuantFileToCorrectedPeaks> quantFileToCorrectedPeaks = null;
	
	public static ExternalQuantAlias getNewExternalQuantAlias( String sAlias ) {
		ExternalQuantAlias eqa = new ExternalQuantAlias();
		eqa.setAlias(sAlias);
		return eqa;
	}

	public void addExternalQuantFile( MSPropertyDataFile mspdf, ExternalQuantAlias eqa ) {
		String sExtQuantType = MassSpecUISettings.getExternalQuantType(mspdf);
		ExternalQuantFileToAlias mAliases = getExternalQuantToAliasByQuantType(sExtQuantType);
		if( getQuantTypeToExternalQuant() == null ) {
			setQuantTypeToExternalQuant(new HashMap<>());
		}
		if( mAliases == null ) {
			mAliases = new ExternalQuantFileToAlias();
			getQuantTypeToExternalQuant().put(sExtQuantType, mAliases);
		}
		mAliases.getSourceDataFileNameToAlias().put(mspdf.getName(), eqa);		
	}

	public static CustomExtraData getDefaultExternalQuantCED( String sExtQuantType, String _sPrevFullKey ) {
		String sDefaultKey = null;
		String sDefaultLabel = null;
		if( sExtQuantType.equals(PeakListInfo.PEAKLISTFORMAT_EXTRACT_TYPE) ) {
			sDefaultKey = ExtractDataProcessor.DEFAULT_KEY;
			sDefaultLabel = ExtractDataProcessor.DEFAULT_LABEL;
		} else if( sExtQuantType.equals(MSFileInfo.MS_FILE_TYPE_DATAFILE) ){
			sDefaultKey = FullMzXMLDataProcessor.DEFAULT_KEY;
			sDefaultLabel = FullMzXMLDataProcessor.DEFAULT_LABEL;
		} 
		CustomExtraData ced = null;
		if( _sPrevFullKey.endsWith( CorrectedQuantColumnInfo.CORRECTED_QUANT_KEY_PREFIX ) ) {
			ced = CorrectedQuantColumnInfo.getDefaultExternalQuantCEDFromKey(sDefaultKey, sDefaultLabel, _sPrevFullKey);
		} else {
			ced = ExternalQuantColumnInfo.getDefaultExternalQuantCEDFromKey(sDefaultKey, sDefaultLabel, _sPrevFullKey);
		}
		if( ced == null ) { // perhaps this is corrected intensity? Try again
			logger.error("Unsupported quant type: " + sExtQuantType);
		}	
		return ced;
	}
	
	public void updateQuantAliasKeyInfo(MSPropertyDataFile mspdf, ExternalQuantFileToAlias mAliases, String sPrefix) {
		String sExtQuantType = MassSpecUISettings.getExternalQuantType(mspdf);
		if( mAliases == null ) {
			return;
		}
		int iCnt = 0;
		for( String sFileName : mAliases.getSourceDataFileNameToAlias().keySet() ) {
			ExternalQuantAlias cureqa = mAliases.getSourceDataFileNameToAlias().get(sFileName);
			if( cureqa == null ) {
				continue; // not part of this analysis
			}
			String sKeyPrefix = "";
			if( sExtQuantType.equals(PeakListInfo.PEAKLISTFORMAT_EXTRACT_TYPE) ) {
				sKeyPrefix = iCnt > 0 ? ExtractDataProcessor.DEFAULT_KEY + "_" + iCnt : ExtractDataProcessor.DEFAULT_KEY;
			} else if( sExtQuantType.equals(MSFileInfo.MS_FILE_TYPE_DATAFILE) ){
				sKeyPrefix = iCnt > 0 ? FullMzXMLDataProcessor.DEFAULT_KEY + "_" + iCnt : FullMzXMLDataProcessor.DEFAULT_KEY;				
			} else {
				logger.error("Unsupported quant type: " + sExtQuantType);
			}
			if( sPrefix != null ) {
				cureqa.setKey(sKeyPrefix + "_" + sPrefix);
			} else {
				cureqa.setKey(sKeyPrefix);
			}
			cureqa.setId(iCnt++);			
		}	
	}
	
	public HashMap<String, QuantFileToCorrectedPeaks> getQuantFileToCorrectedPeaks() {
		return quantFileToCorrectedPeaks;
	}
	
	public void setQuantFileToCorrectedPeaks(HashMap<String, QuantFileToCorrectedPeaks> quantFileToCorrectedPeaks) {
		this.quantFileToCorrectedPeaks = quantFileToCorrectedPeaks;
	}
	
	public HashMap<String, InternalStandardQuantFileList> getInternalStandardQuantFiles() {
		return internalStandardQuantFiles;
	}
	
	public void setInternalStandardQuantFiles(HashMap<String,InternalStandardQuantFileList> internalStandardQuantFiles) {
		this.internalStandardQuantFiles = internalStandardQuantFiles;
	}
	
	public void addInternalStandardQuantFile( String sStdQuantName, MSPropertyDataFile mspdf, ExternalQuantAlias eqa ) {
		String sExtQuantType = MassSpecUISettings.getExternalQuantType(mspdf);
		if( getInternalStandardQuantFiles() == null ) {
			setInternalStandardQuantFiles(new HashMap<>());
		}
		InternalStandardQuantFileList quantFileList = getInternalStandardQuantFiles().get(sExtQuantType);
		if( quantFileList == null ) {
			quantFileList = new InternalStandardQuantFileList();
			getInternalStandardQuantFiles().put(sExtQuantType, quantFileList);
		}
		
		if( quantFileList.getQuantNameToFileAlias() == null ) {
			quantFileList.setQuantNameToFileAlias(new HashMap<>());
		}
		ExternalQuantFileToAlias mAliases = quantFileList.getQuantNameToFileAlias().get(sStdQuantName);
		if( mAliases == null ) {
			mAliases = new ExternalQuantFileToAlias();
			quantFileList.getQuantNameToFileAlias().put(sStdQuantName, mAliases);
		}
		mAliases.getSourceDataFileNameToAlias().put(mspdf.getName(), eqa);		
	}
		
	public void removeInternalStandardQuantFile( String sStdQuantName, MSPropertyDataFile mspdf ) {
		if( getInternalStandardQuantFiles() == null ) {
			return;
		}
		String sExtQuantType = MassSpecUISettings.getExternalQuantType(mspdf);
		InternalStandardQuantFileList quantFileList = getInternalStandardQuantFiles().get(sExtQuantType);
		if( quantFileList == null || quantFileList.getQuantNameToFileAlias() == null) {
			return;
		}
		
		ExternalQuantFileToAlias mAliases = quantFileList.getQuantNameToFileAlias().get(sStdQuantName);
		if( mAliases == null ) {
			return;
		}
		mAliases.getSourceDataFileNameToAlias().remove(mspdf.getName());		
	}

	public boolean containsInternalStandardQuantFile( String sStdQuantName, MSPropertyDataFile mspdf ) {
		if( getInternalStandardQuantFiles() == null ) {
			return false;
		}
		String sExtQuantType = MassSpecUISettings.getExternalQuantType(mspdf);
		InternalStandardQuantFileList quantFileList = getInternalStandardQuantFiles().get(sExtQuantType);
		if( quantFileList == null || quantFileList.getQuantNameToFileAlias() == null) {
			return false;
		}
		
		ExternalQuantFileToAlias mAliases = quantFileList.getQuantNameToFileAlias().get(sStdQuantName);
		if( mAliases == null ) {
			return false;
		}
		return mAliases.getSourceDataFileNameToAlias().containsKey(mspdf.getName());		
	}
	
	public ExternalQuantFileToAlias getInternalQuantFileToAlias( String sStdQuantName, MSPropertyDataFile mspdf ) {
		if( getInternalStandardQuantFiles() == null ) {
			return null;
		}
		String sExtQuantType = MassSpecUISettings.getExternalQuantType(mspdf);
		InternalStandardQuantFileList quantFileList = getInternalStandardQuantFiles().get(sExtQuantType);
		if( quantFileList == null || quantFileList.getQuantNameToFileAlias() == null) {
			return null;
		}
		
		ExternalQuantFileToAlias mAliases = quantFileList.getQuantNameToFileAlias().get(sStdQuantName);
		return mAliases;
	}
	
	public void cloneSettings(MassSpecUISettings settings) {
		cloneSourceDataFileNameToAlias(settings);
		cloneInternalStandardQuant(settings);
		cloneFileList(settings);
		cloneSourceFileList(settings);
		cloneCustomAnnotations(settings);
	}

	private void cloneCustomAnnotations(MassSpecUISettings settings) {
		settings.setCustomAnnotations(new ArrayList<MassSpecCustomAnnotation>());
		for( MassSpecCustomAnnotation msca : this.getCustomAnnotations() ) {
			settings.getCustomAnnotations().add(msca);
		}
		settings.setCustomAnnotationText(this.getCustomAnnotationText());
	}

	private void cloneInternalStandardQuant(MassSpecUISettings settings) {
		if (this.getInternalStandardQuantFiles() == null)
			return;
		HashMap<String, InternalStandardQuantFileList> mLev1 = new HashMap<String, InternalStandardQuantFileList>();
		for( String sKey1 : this.getInternalStandardQuantFiles().keySet() ) {
			InternalStandardQuantFileList mSourceLev2 = getInternalStandardQuantFiles().get(sKey1);
			if( mSourceLev2 != null ) {
				InternalStandardQuantFileList mLev2 = new InternalStandardQuantFileList();
				mLev1.put(sKey1, mLev2);
				for( String sKey2 : mSourceLev2.getQuantNameToFileAlias().keySet() ) {
					ExternalQuantFileToAlias mSourceLev3 = mSourceLev2.getQuantNameToFileAlias().get(sKey2);
					if( mSourceLev3 != null ) {
						ExternalQuantFileToAlias mLev3 = new ExternalQuantFileToAlias();
						mLev2.getQuantNameToFileAlias().put(sKey2, mLev3);
						for( String sKey3 : mSourceLev3.getSourceDataFileNameToAlias().keySet() ) {
							mLev3.getSourceDataFileNameToAlias().put(sKey3, mSourceLev3.getSourceDataFileNameToAlias().get(sKey2));
						}
					}
				}
			}
		}
		settings.setInternalStandardQuantFiles(mLev1);
	}
	
	private void cloneSourceDataFileNameToAlias(MassSpecUISettings settings) {
		if (this.getQuantTypeToExternalQuant() == null)
			return;
		HashMap<String, ExternalQuantFileToAlias> mLev1 = new HashMap<>();
		for( String sKey1 : this.getQuantTypeToExternalQuant().keySet() ) {
			ExternalQuantFileToAlias mSourceLev2 = this.getQuantTypeToExternalQuant().get(sKey1);
			if( mSourceLev2 != null ) {
				ExternalQuantFileToAlias alias2 = new ExternalQuantFileToAlias();
				HashMap<String, ExternalQuantAlias> mLev2 = new HashMap<String, ExternalQuantAlias>();
				alias2.setSourceDataFileNameToAlias(mLev2);
				mLev1.put(sKey1, alias2);
				for( String sKey2 : mSourceLev2.getSourceDataFileNameToAlias().keySet() ) {
					mLev2.put(sKey2, mSourceLev2.getSourceDataFileNameToAlias().get(sKey2));
				}
			}
		}
		settings.setQuantTypeToExternalQuant(mLev1);
	}

	private void cloneFileList(MassSpecUISettings settings) {
		settings.setFileList(new ArrayList<MSPropertyDataFile>());
		if (this.fileList != null) {
			for( MSPropertyDataFile pdf : this.getFileList() ) {
				settings.addFile((MSPropertyDataFile) pdf.clone());
			}
		}
	}

	private void cloneSourceFileList(MassSpecUISettings settings) {
		settings.setSourceDataFileList(new ArrayList<MSPropertyDataFile>());
		if (this.getSourceDataFileList() != null) {
			for( MSPropertyDataFile pdf : this.getSourceDataFileList() ) {	
				settings.addSourceFile(pdf);
			}
		}
	}


	public MassSpecUISettings() {
		customAnnotations = new ArrayList<MassSpecCustomAnnotation>();
		standardQuant = new ArrayList<MassSpecStandardQuant>();
		quantTypeToExternalQuant = new HashMap<String, ExternalQuantFileToAlias>();				
	}

	public HashMap<String, ExternalQuantFileToAlias> getQuantTypeToExternalQuant() {
		return quantTypeToExternalQuant;
	}

	public void setQuantTypeToExternalQuant(HashMap<String, ExternalQuantFileToAlias> quantTypeToExternalQuant) {
		this.quantTypeToExternalQuant = quantTypeToExternalQuant;
	}

	public ExternalQuantFileToAlias getExternalQuantToAliasByQuantType( String sQuantType ) {
		if( getQuantTypeToExternalQuant() == null ) {
			return null;
		}
		return getQuantTypeToExternalQuant().get(sQuantType);
	}

	/**
	 * Returns the top-level key for the external quant aliasing hash map.
	 * @param mspdf
	 * @return
	 */
	public static String getExternalQuantType( MSPropertyDataFile mspdf ) {
		// we want to consider mzXML and mzML as the same thing, so we'll use the "converted" value
		if( mspdf.getType().equals( MSFileInfo.MSFORMAT_MZXML_TYPE ) || mspdf.getType().equals( MSFileInfo.MSFORMAT_MZML_TYPE) ) {
			return MSFileInfo.MS_FILE_TYPE_DATAFILE;
		}

		// return the actual type because these are all different
		return mspdf.getType();
	}
	
	@XmlTransient
	public List<MassSpecCustomAnnotation> getCustomAnnotations() {
		return customAnnotations;
	}
	public void setCustomAnnotations(List<MassSpecCustomAnnotation> customAnnotations) {
		this.customAnnotations = customAnnotations;
	}

	@XmlElement(name="customAnnotationsText")
	public String getCustomAnnotationText() {
		return customAnnotationsText;
	}
	public void setCustomAnnotationText(String customAnnotationsText) {
		this.customAnnotationsText = customAnnotationsText;
	}

	@XmlTransient
	public List<MassSpecStandardQuant> getStandardQuant() {
		return standardQuant;
	}
	public void setStandardQuant(List<MassSpecStandardQuant> standardQuant) {
		this.standardQuant = standardQuant;
	}

	@XmlElement(name="standardQuantText")
	public String getStandardQuantText() {
		return standardQuantText;
	}
	public void setStandardQuantText(String standardQuantText) {
		this.standardQuantText = standardQuantText;
	}
	
	@XmlElement(name="fileList")
	public List<MSPropertyDataFile> getFileList() {
		return fileList;
	}
	public void setFileList(List<MSPropertyDataFile> fileList) {
		this.fileList = fileList;
	}

	/**
	 * @return the source data file list (e.g. MS file)
	 */
	public List<MSPropertyDataFile> getSourceDataFileList() {
		return sourceDataFileList;
	}
	/**
	 * @param sourceDataFileList the source data file list (e.g. MS file)
	 */
	public void setSourceDataFileList(List<MSPropertyDataFile> sourceDataFileList) {
		this.sourceDataFileList = sourceDataFileList;
	}

	/**
	 * Add a source file to the sourceFile list. Source file is a file that was used to generate this entry
	 * @param newDataFile
	 */
	public void addSourceFile(MSPropertyDataFile newDataFile) {
		if (getSourceDataFileList() == null) {
			setSourceDataFileList(new ArrayList<>());
		}
		// make sure the file isn't already in the list! do the lookup by name, category and file type
		List<MSPropertyDataFile> toRemove = new ArrayList<MSPropertyDataFile>();
		for( MSPropertyDataFile file : getSourceDataFileList() ) {
			if( file.getName().equals(newDataFile.getName()) && file.getCategory().equals(newDataFile.getCategory()) 
					&& file.getMSFileType().equals(newDataFile.getMSFileType())) {
				toRemove.add(file);
			}
		}
		if( ! toRemove.isEmpty() ) {
			for( MSPropertyDataFile file : toRemove ) {
				getSourceDataFileList().remove(file);
			}
		}
		getSourceDataFileList().add(newDataFile);
	}	

	public MassSpecCustomAnnotation lookupCustomAnnotation( String annotationName ) {
		if( getCustomAnnotations() != null && ! getCustomAnnotations().isEmpty() ) {
			for( int i = 0; i < getCustomAnnotations().size(); i++ ) {
				MassSpecCustomAnnotation annotation = getCustomAnnotations().get(i);
				if( annotation.getAnnotationName().equalsIgnoreCase(annotationName) )  {
					return annotation;
				}
			}			
		}
		return null;
	}

	public void updateCustomAnotationData() {
		setCustomAnnotationText( MassSpecPreference.createCustomAnnotationsText(getCustomAnnotations()) );		
	}

	public MassSpecStandardQuant lookupStandardQuant( String standardQuantName ) {
		if( getStandardQuant() != null && ! getStandardQuant().isEmpty() ) {
			for( int i = 0; i < getStandardQuant().size(); i++ ) {
				MassSpecStandardQuant standardQuant = getStandardQuant().get(i);
				if( standardQuant.getStandardQuantName().equalsIgnoreCase(standardQuantName) )  {
					return standardQuant;
				}
			}			
		}
		return null;
	}

	public void updateStandardQuantData() {
		setStandardQuantText( MassSpecPreference.createStandardQuantText(getStandardQuant()) );		
	}
	
	/**
	 * Add a non-source file to the fileList (most likely a result file or a file associated w/ the entry by the user)
	 * @param newDataFile
	 * @return the existing file if the file to be added is already in the list
	 */
	public MSPropertyDataFile addFile(MSPropertyDataFile newDataFile) {
		if (getFileList() == null) {
			setFileList(new ArrayList<>());
		}
		// make sure the file isn't already in the list! 
		MSPropertyDataFile existing = null;
		for( MSPropertyDataFile file : getFileList() ) {
			if( file.getName().equals(newDataFile.getName()) && file.getCategory().equals(newDataFile.getCategory()) 
					&& file.getMSFileType().equals(newDataFile.getMSFileType())) {
				existing = file;
			}
		}
		if( existing == null) {  // if it is already there, do not add it again
			getFileList().add(newDataFile);
		}
		return existing;
	}	

	/**
	 * find all the peak list files
	 * 
	 * @return the list of peak list files
	 */
	private List<MSPropertyDataFile> getPeakListFile() {
		List<MSPropertyDataFile> peakListFiles = new ArrayList<>();
		if (getFileList() == null)
			return peakListFiles;
		Iterator<MSPropertyDataFile> itr = getFileList().iterator();
		while( itr.hasNext() ) {
			MSPropertyDataFile file = itr.next();
			if( file.getType().equals( PeakListInfo.PEAKLISTFORMAT_TABDELIMITED_TYPE ) || 
					file.getType().equals( PeakListInfo.PEAKLISTFORMAT_EXTRACT_TYPE ) ||
					file.getType().equals( PeakListInfo.PEAKLISTFORMAT_MASCOTGENERIC_TYPE ) ) {
				peakListFiles.add(file);
			}
		}
		return peakListFiles;  	
	}

	/**
	 * find all files that are for external quantification
	 * exclude instrument files
	 * 
	 * @return a list of external quantification files
	 */
	public List<MSPropertyDataFile> getQuantificationFiles() {
		List<MSPropertyDataFile> fileList = new ArrayList<>();
		if (getFileList() == null)
			return fileList;
		Iterator<MSPropertyDataFile> itr = getFileList().iterator();
		while( itr.hasNext() ) {
			PropertyDataFile file = itr.next();			
			if (file instanceof MSPropertyDataFile) {
				if (((MSPropertyDataFile) file).getChildren() != null) {
					for (MSPropertyDataFile child: ((MSPropertyDataFile) file).getChildren()) {
						if( child.getCategory().equals( FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY ) && 
								!child.getMSFileType().equals(MSFileInfo.MS_FILE_TYPE_INSTRUMENT)) {
							if( ! fileList.contains(child) ) {
								fileList.add(child);
							}
						}
					}
				}
			}
		}
		return fileList;
	}

	/**
	 * retrieve all data files that can be used in annotation
	 * 
	 * @return list of all data files that can be used in annotation
	 */
	public List<MSPropertyDataFile> getAnnotationFiles () {
		List<MSPropertyDataFile> fileList = new ArrayList<>();
		if (getFileList() == null)
			return fileList;
		Iterator<MSPropertyDataFile> itr = getFileList().iterator();
		while( itr.hasNext() ) {
			PropertyDataFile file = itr.next();			
			if (file instanceof MSPropertyDataFile) {
				if (((MSPropertyDataFile) file).getChildren() != null) {
					for (MSPropertyDataFile child: ((MSPropertyDataFile) file).getChildren()) {
						if( child.getCategory().equals( FileCategory.ANNOTATION_CATEGORY ) && 
								!child.getMSFileType().equals(MSFileInfo.MS_FILE_TYPE_INSTRUMENT)) {
							if( ! fileList.contains(child) ) {
								fileList.add(child);
							}
						}
					}
				}
			}
		}
		return fileList;
	}

	public void adjustPropertyFilePaths() {
		if (getFileList() != null) {
			Iterator<MSPropertyDataFile> itr2 = getFileList().iterator();
			while( itr2.hasNext() ) {
				MSPropertyDataFile file = itr2.next();
				if( file.getName().contains("\\") && ! File.separator.equals("\\") ) {
					file.setName( file.getName().replace("\\", File.separator));
				} else if( file.getName().contains("/") && ! File.separator.equals("/") ){
					file.setName( file.getName().replace("/", File.separator));
				}
				if (file.getChildren() != null) {
					for (MSPropertyDataFile child : file.getChildren()) {
						if( child.getName().contains("\\") && ! File.separator.equals("\\") ) {
							child.setName( child.getName().replace("\\", File.separator));
						} else if( file.getName().contains("/") && ! File.separator.equals("/") ){
							child.setName( child.getName().replace("/", File.separator));
						}
					}
				}
			}
		}
	}

	/**
	 * For legacy support. During reading of property, an MSPropertyDatafile w/ the deprecated type is created for the RAW file. This will be converted
	 * at the end. 
	 * 
	 * @param sRawFileName
	 * @param originalFileName
	 * @param isFull
	 * @return a new MSPropertyDatafile
	 */
	public static MSPropertyDataFile getLegacyVersionRawPropertyDataFile( String sRawFileName, String originalFileName, boolean isFull ) {
		MSPropertyDataFile msFile = null;
		if( sRawFileName != null ) {
			String sType = isFull ? MSFileInfo.getType(MSFileInfo.MS_FULL_FILE, MSFileInfo.MSFORMAT_RAW_TYPE) : MSFileInfo.getType(MSFileInfo.MS_FILE, MSFileInfo.MSFORMAT_RAW_TYPE);
			//msFile = new PropertyDataFile(sRawFileName, MSFileInfo.MSFORMAT_RAW_CURRENT_VERSION, sType);
			msFile = new MSPropertyDataFile(sRawFileName, MSFileInfo.MSFORMAT_RAW_CURRENT_VERSION, sType, FileCategory.ANNOTATION_CATEGORY, MSFileInfo.MS_FILE_TYPE_INSTRUMENT, originalFileName,
					Arrays.asList(new String[] {FileCategory.ANNOTATION_CATEGORY.getLabel()}), true);  // parent file
		}		
		return msFile;
	}

	/**
	 * For legacy support. During reading of property, an MSPropertyDatafile w/ the deprecated type is created for the mzXML file. This will be converted
	 * at the end. 
	 * 
	 * @param sMzFileName
	 * @param originalFileName
	 * @param isFull
	 * @return
	 */
	public static MSPropertyDataFile getLegacyVersionMzPropertyDataFile( String sMzFileName, String originalFileName, boolean isFull ) {
		MSPropertyDataFile msFile = null;
		if( sMzFileName != null ) {
			int iLastDot = sMzFileName.lastIndexOf(".");
			String sExtension = ".mzXML"; // assuming mzXML
			if( iLastDot >= 0 ) { // just in case there is a dot, then take what we see
				sExtension = sMzFileName.substring(iLastDot+1);
			} 
			String sMsType = sExtension.toUpperCase().startsWith( MSFileInfo.MSFORMAT_MZXML_TYPE.toUpperCase() ) ? MSFileInfo.MSFORMAT_MZXML_TYPE : MSFileInfo.MSFORMAT_MZML_TYPE;
			String sType = isFull ? MSFileInfo.getType(MSFileInfo.MS_FULL_FILE, sMsType) : MSFileInfo.getType(MSFileInfo.MS_FILE, sMsType);
			String sVersion = sExtension.toUpperCase().startsWith( MSFileInfo.MSFORMAT_MZXML_TYPE.toUpperCase() ) ? MSFileInfo.MSFORMAT_MZXML_CURRENT_VERSION : MSFileInfo.MSFORMAT_MZML_CURRENT_VERSION;

			//msFile = new PropertyDataFile(sMzFileName, sVersion, sType);
			if (isFull) 
				msFile = new MSPropertyDataFile(sMzFileName, sVersion, sType, FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY, MSFileInfo.MS_FILE_TYPE_DATAFILE, originalFileName, 
						Arrays.asList(new String[] {FileCategory.ANNOTATION_CATEGORY.getLabel()}));
			else
				msFile = new MSPropertyDataFile(sMzFileName, sVersion, sType, FileCategory.ANNOTATION_CATEGORY, MSFileInfo.MS_FILE_TYPE_DATAFILE, originalFileName, 
						Arrays.asList(new String[] {FileCategory.ANNOTATION_CATEGORY.getLabel()}));
		}		
		return msFile;
	}

	/**
	 * For legacy support. During reading of property, an MSPropertyDatafile w/ the deprecated type is created for the peak list file. This will be converted
	 * at the end. 
	 * 
	 * @param peakListFileName
	 * @param originalFileName
	 * @param peakListFileFormat
	 * @return
	 */
	public static MSPropertyDataFile getLegacyVersionPeakListPropertyDataFile(String peakListFileName, String originalFileName, String peakListFileFormat ) {
		MSPropertyDataFile peakList = null;
		if( peakListFileName != null && peakListFileFormat != null ) {
			String sVersion = null;

			String sType = null;
			if( peakListFileFormat.equals( PeakListInfo.PEAKLISTFORMAT_EXTRACT_TYPE ) ) {
				sVersion = PeakListInfo.PEAKLISTFORMAT_EXTRACT_CURRENT_VERSION;
				sType = PeakListInfo.PEAKLISTFORMAT_EXTRACT_TYPE;
			} else if ( peakListFileFormat.equals( PeakListInfo.PEAKLISTFORMAT_TABDELIMITED_TYPE ) ) {
				sVersion = PeakListInfo.PEAKLISTFORMAT_TABDELIMITED_CURRENT_VERSION;
				sType = PeakListInfo.PEAKLISTFORMAT_TABDELIMITED_TYPE;
			} else if ( peakListFileFormat.equals( PeakListInfo.PEAKLISTFORMAT_MASCOTGENERIC_TYPE ) ) {
				sVersion = PeakListInfo.PEAKLISTFORMAT_MASCOTGENERIC_CURRENT_VERSION;
				sType = PeakListInfo.PEAKLISTFORMAT_MASCOTGENERIC_TYPE;
			} else {
				return null;
			}

			//peakList = new PropertyDataFile(peakListFileName, sVersion, sType);
			peakList = new MSPropertyDataFile(peakListFileName, sVersion, sType, FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY, MSFileInfo.MS_FILE_TYPE_PROCESSED, originalFileName,
					Arrays.asList(new String[] {FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY.getLabel()}));
		}
		return peakList;
	}		

	/**
	 * should not be used since it returns only the first one
	 * there might be several peak list files
	 * 
	 * @return the format of the first peak list file
	 */
	@Deprecated
	public String getMsPeakListFileFormat() {
		if (getPeakListFile().isEmpty()) return null;
		PropertyDataFile file = getPeakListFile().get(0);
		return file != null ? file.getType() : null;
	}

	/**
	 * should not be used since it returns only the first one
	 * there might be several peak list files
	 * 
	 * @return the name of the first peak list file
	 */
	@Deprecated
	public String getMsPeakListFileName() {
		if (getPeakListFile().isEmpty()) return null;
		PropertyDataFile file = getPeakListFile().get(0);
		return file != null ? file.getName() : null;
	}

	/**
	 * find all annotation files (currently mzXML or mzML type)
	 * 
	 * @param isFull whether to get only FULL MS files or only MS/MS ones
	 * @return list of (full) MzXML or (full) MzML files based on the parameter isFull
	 */
	@Deprecated
	private List<PropertyDataFile> getMzXMLFile( boolean isFull ) {
		List<PropertyDataFile> fileList = new ArrayList<>();
		if (getFileList() == null)
			return fileList;
		Iterator<MSPropertyDataFile> itr = getFileList().iterator();
		while( itr.hasNext() ) {
			MSPropertyDataFile file = itr.next();
			if( isFull && file.getCategory().equals(FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY )) {
				if( file.getType().equals( MSFileInfo.MSFORMAT_MZXML_TYPE ) || file.getType().equals( MSFileInfo.MSFORMAT_MZML_TYPE) ) {
					fileList.add(file);
				}				
			} else if ( ! isFull && file.getCategory().equals(FileCategory.ANNOTATION_CATEGORY) ) {
				if( file.getType().equals( MSFileInfo.MSFORMAT_MZXML_TYPE ) || file.getType().equals( MSFileInfo.MSFORMAT_MZML_TYPE) ) {
					fileList.add(file);
				}				
			}


			if (file instanceof MSPropertyDataFile) {
				if (((MSPropertyDataFile) file).getChildren() != null) {
					for (MSPropertyDataFile child: ((MSPropertyDataFile) file).getChildren()) {
						if( isFull && child.getCategory().equals(FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY )) {
							if( child.getType().equals( MSFileInfo.MSFORMAT_MZXML_TYPE ) || child.getType().equals( MSFileInfo.MSFORMAT_MZML_TYPE) ) {
								fileList.add(child);
							}				
						} else if ( ! isFull && file.getCategory().equals(FileCategory.ANNOTATION_CATEGORY )) {
							if( child.getType().equals( MSFileInfo.MSFORMAT_MZXML_TYPE ) || child.getType().equals( MSFileInfo.MSFORMAT_MZML_TYPE) ) {
								fileList.add(child);
							}				
						}
					}
				}
			}
		}
		return fileList;
	}

	/**
	 * should not be used since it returns only the first one
	 * there might be several full MS files
	 * 
	 * @return the name of the first full MS file
	 */
	@Deprecated
	public String getMzXMLFullFileName() {
		List<PropertyDataFile> fileList = getMzXMLFile(true);
		PropertyDataFile file = null;
		if (!fileList.isEmpty())
			file = fileList.get(0);
		return file != null ? file.getName() : null;
	}   

	/**
	 * This method should only be used for migration purposes from version 1.2 to 1.3 or up
	 * It returns the first MzXML file found in the list of files
	 * 
	 * @return the name of the first mzXML file from the list of all files
	 */
	@Deprecated
	public String getMzXMLFileName() {
		List<PropertyDataFile> fileList = getMzXMLFile(false);
		PropertyDataFile file = null;
		if (!fileList.isEmpty())
			file = fileList.get(0);
		return file != null ? file.getName() : null;
	}   

}
