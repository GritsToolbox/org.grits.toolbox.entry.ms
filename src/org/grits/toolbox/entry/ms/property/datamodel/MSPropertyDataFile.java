package org.grits.toolbox.entry.ms.property.datamodel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;

import org.apache.log4j.Logger;
import org.grits.toolbox.core.datamodel.property.PropertyDataFile;
import org.grits.toolbox.entry.ms.extquantfiles.process.ExtractDataProcessor;
import org.grits.toolbox.entry.ms.extquantfiles.process.FullMzXMLDataProcessor;
import org.grits.toolbox.ms.file.FileCategory;
import org.grits.toolbox.ms.file.MSFile;
import org.grits.toolbox.ms.file.MSFileInfo;
import org.grits.toolbox.ms.file.PeakListInfo;
import org.grits.toolbox.ms.file.reader.IMSFileReader;
import org.grits.toolbox.ms.file.reader.impl.ExtractReader;
import org.grits.toolbox.ms.file.reader.impl.MSXMLReader;
import org.grits.toolbox.ms.file.reader.impl.MzXmlReader;

/**
 * Extend PropertyDataFile with category (type) and purpose of the MassSpec files
 * 
 * @author sena
 *
 */
public class MSPropertyDataFile extends PropertyDataFile {
	private static final Logger logger = Logger.getLogger(MSPropertyDataFile.class);
	FileCategory category; // MS/MS, or Full MS
	String msFileType; 
	/* msFileType is set to:
	 * MSFileInfo.MS_FILE_TYPE_INSTRUMENT
	 * MSFileInfo.MS_FILE_TYPE_DATAFILE
	 * MSFileInfo.MS_FILE_TYPE_PROCESSED
	 */
	List<String> purpose; 
	List<MSPropertyDataFile> children; // to have hiearchical view: RAW data file, if exists, should be the parent
	Boolean isParent = false;
	String originalFileName;
	
	/* 
	 *  NOTE: the inherited "type" is set to
	 *  MSFileInfo.MSFORMAT_RAW_TYPE
	 *  MSFileInfo.MSFORMAT_MZML_TYPE
	 *  MSFileInfo.MSFORMAT_MZXML_TYPE
	 *  PeakListInfo.PEAKLISTFORMAT_TABDELIMITED_TYPE
	 *  PeakListInfo.PEAKLISTFORMAT_EXTRACT_TYPE
	 *  PeakListInfo.PEAKLISTFORMAT_MASCOTGENERIC_TYPE
	 */
	
	// required by JAXB
	public MSPropertyDataFile() {
		logger.debug("Test");
	}

	/**
	 * MSPropertyDataFile constructor
	 * 
	 * @param name
	 * @param version
	 * @param type
	 * @param category
	 * @param msFileType
	 * @param originalFile
	 * @param purpose 
	 */
	public MSPropertyDataFile(String name, String version, String type, FileCategory category, String msFileType, String originalFile, List<String> purpose) {
		this(name, version, type, category, msFileType, originalFile, purpose, false);
	}

	/**
	 * MSPropertyDataFile constructor
	 * 
	 * @param name
	 * @param version
	 * @param type
	 * @param category
	 * @param msFileType
	 * @param originalFile
	 * @param purpose
	 * @param isParent whether it is a parent or child entry
	 */
	public MSPropertyDataFile(String name, String version, String type, FileCategory category, String msFileType, String originalFile, List<String> purpose, boolean isParent) {
		super (name, version, type);
		this.category = category;
		this.msFileType = msFileType;
		this.purpose = purpose;
		this.isParent = isParent;
		this.originalFileName = originalFile;
	}

	/**
	 * @param name
	 * @param version
	 * @param type
	 */
	public MSPropertyDataFile(String name, String version, String type) {
		super (name, version, type);
	}
	
	/**
	 * for backward compatibility
	 */
	public static MSPropertyDataFile getCurrentVersionFileFromLegacyVersion(String name, String version, String type) {
		MSPropertyDataFile pdf = new MSPropertyDataFile(name, version, type);
		if (pdf.getType().equals( PeakListInfo.PEAKLISTFORMAT_TABDELIMITED_TYPE ) || 
				pdf.getType().equals( PeakListInfo.PEAKLISTFORMAT_EXTRACT_TYPE ) ||
				pdf.getType().equals( PeakListInfo.PEAKLISTFORMAT_MASCOTGENERIC_TYPE ) ) {
			pdf.setCategory(FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY);
			pdf.setMSFileType(MSFileInfo.MS_FILE_TYPE_PROCESSED);
		} else if (pdf.getType().equals( MSFileInfo.getType(MSFileInfo.MS_FULL_FILE, MSFileInfo.MSFORMAT_MZXML_TYPE) ) ) {
			pdf.setCategory(FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY);
			pdf.setMSFileType(MSFileInfo.MS_FILE_TYPE_DATAFILE);
			pdf.setType(MSFileInfo.MSFORMAT_MZXML_TYPE);
		} else if (pdf.getType().equals( MSFileInfo.getType(MSFileInfo.MS_FULL_FILE, MSFileInfo.MSFORMAT_MZML_TYPE))) {
			pdf.setCategory(FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY);
			pdf.setMSFileType(MSFileInfo.MS_FILE_TYPE_DATAFILE);
			pdf.setType(MSFileInfo.MSFORMAT_MZML_TYPE);
		} else if (pdf.getType().equals( MSFileInfo.getType(MSFileInfo.MS_FILE, MSFileInfo.MSFORMAT_MZXML_TYPE) ) ) {
			pdf.setCategory(FileCategory.ANNOTATION_CATEGORY);
			pdf.setMSFileType(MSFileInfo.MS_FILE_TYPE_DATAFILE);
			pdf.setType(MSFileInfo.MSFORMAT_MZXML_TYPE);
		} else if (pdf.getType().equals( MSFileInfo.getType(MSFileInfo.MS_FILE, MSFileInfo.MSFORMAT_MZML_TYPE))) {
			pdf.setCategory(FileCategory.ANNOTATION_CATEGORY);
			pdf.setMSFileType(MSFileInfo.MS_FILE_TYPE_DATAFILE);
			pdf.setType(MSFileInfo.MSFORMAT_MZML_TYPE);
		} else if (pdf.getType().equals( MSFileInfo.getType(MSFileInfo.MS_FULL_FILE, MSFileInfo.MSFORMAT_RAW_TYPE))) {
			pdf.setCategory(FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY);
			pdf.setMSFileType(MSFileInfo.MS_FILE_TYPE_INSTRUMENT);
			pdf.setType(MSFileInfo.MSFORMAT_RAW_TYPE);
		} else if (pdf.getType().equals( MSFileInfo.getType(MSFileInfo.MS_FILE, MSFileInfo.MSFORMAT_RAW_TYPE))) {
			pdf.setCategory(FileCategory.ANNOTATION_CATEGORY);
			pdf.setMSFileType(MSFileInfo.MS_FILE_TYPE_INSTRUMENT);
			pdf.setType(MSFileInfo.MSFORMAT_RAW_TYPE);
		}
		List<String> purposes = new ArrayList<>();
		if (type != null && MSFileInfo.getMSType(type).equals(MSFileInfo.MS_FILE)) {
			purposes.add(FileCategory.ANNOTATION_CATEGORY.getLabel());
		} else {
			purposes.add(FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY.getLabel());
		} 
		pdf.setPurpose(purposes);
		pdf.setOriginalFileName(name);
		return pdf;
	}
	/**
	 * set original file name
	 * 
	 * @param originalFileName original name of the file
	 */
	public void setOriginalFileName(String originalFileName) {
		this.originalFileName = originalFileName;
	}

	/**
	 * 
	 * @return original file name
	 */
	@XmlAttribute
	public String getOriginalFileName() {
		return originalFileName;
	}

	/**
	 * 
	 * @return the category of the file
	 */
	@XmlAttribute
	public FileCategory getCategory() {
		return category;
	}

	/**
	 * 
	 * @param category category to set
	 */
	public void setCategory(FileCategory category) {
		this.category = category;
	}

	/**
	 * 
	 * @return the purpose of the file
	 */
	public List<String> getPurpose() {
		return purpose;
	}

	/**
	 * 
	 * @param purpose purpose to set
	 */
	public void setPurpose(List<String> purpose) {
		this.purpose = purpose;
	}

	/**
	 * create comma separated purpose attribute
	 * @return a string containing a comma separated list of purpose values
	 */
	public String getPurposeString() {
		if (this.purpose == null)
			return PropertyDataFile.DEFAULT_TYPE;
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String string : this.purpose) {
			if (first) {
				sb.append(string);
				first = false;
			}
			else sb.append("," + string);
		}
		return sb.toString();
	}

	/**
	 * 
	 * @return list of children files
	 */
	public List<MSPropertyDataFile> getChildren() {
		return children;
	}

	/**
	 * 
	 * @param children children to set
	 */
	public void setChildren(List<MSPropertyDataFile> children) {
		this.children = children;
	}

	/**
	 * add a child file
	 * @param file name of the file to be added
	 */
	public void addChild (MSPropertyDataFile file) {
		if (this.children == null)
			this.children = new ArrayList<>();
		this.children.add(file);
	}

	/**
	 * Checks if the given file already exists in the list of children. Checks only by name
	 * @param file MS file
	 * @return true if the file with the same name already exists as a child, false otherwise
	 */
	public boolean containsChild (MSPropertyDataFile file) {
		if (this.children == null)
			return false;
		for (MSPropertyDataFile msPropertyDataFile : children) {
			if (msPropertyDataFile.getName().equals(file.getName()))
				return true;
		}
		return false;
	}

	public void setIsParent(Boolean isParent) {
		this.isParent = isParent;
	}

	@XmlAttribute
	public Boolean getIsParent() {
		return isParent;
	}

	public void setMSFileType(String msFileType) {
		this.msFileType = msFileType;
	}

	@XmlAttribute
	public String getMSFileType() {
		return msFileType;
	}

	/**
	 * Create MSFile object appropriate to describe annotation/quantification files.
	 * It also instantiates the appropriate reader for the file
	 * 
	 * @param pathToFile full path of the MS file
	 * @param experimentType type of the experiment (Direct Infusion, TIM etc. defined in {@link org.grits.toolbox.ms.om.data.Method})
	 * @return MSFile object appropriate to describe annotation/quantification files for other plugins/jars
	 */
	public MSFile getMSFileWithReader (String pathToFile, String experimentType) {
		MSFile msFile = new MSFile();
		msFile.setFileName(pathToFile + File.separator + getName());
		msFile.setExperimentType(experimentType);
		msFile.setCategory(getCategory());
		msFile.setVersion(getVersion());
		msFile.setReader(getReaderForFile());  
		return msFile;
	}

	/**
	 * return the reader based on the file properties.
	 * IMPORTANT: when new file types are supported this method needs to be updated
	 * @param file MS file to be read
	 * @return reader appropriate for reading the provided file
	 */
	public IMSFileReader getReaderForFile () {
		switch (getCategory()) {
		case ANNOTATION_CATEGORY:
			if (getType().equals(MSFileInfo.MSFORMAT_MZML_TYPE) ||
					getType().equals(MSFileInfo.MSFORMAT_MZXML_TYPE) )
				return new MzXmlReader();
			break;
		case EXTERNAL_QUANTIFICATION_CATEGORY:
			if (getType().equals(PeakListInfo.PEAKLISTFORMAT_EXTRACT_TYPE) )
				return new ExtractReader();
			else if (getType().equals(MSFileInfo.MSFORMAT_MZML_TYPE) ||
					getType().equals(MSFileInfo.MSFORMAT_MZXML_TYPE))
				return new MSXMLReader();
			break;
		}
		return null;
	}
		
	/**
	 * @param mspdf
	 * 		a MSPropertyDataFile
	 * @return the name of the MSPropertyDatafile trimmed of any path information
	 */
	public static String getFormattedName(MSPropertyDataFile mspdf) {
		String sFilePath = mspdf.getName();
		int inx = sFilePath.lastIndexOf(File.separator);
		String sFileName = sFilePath;			
		if( inx >= 0 ) {
			sFileName = sFilePath.substring(inx+1);		
		}
		return sFileName;
	}

	
	/**
	 * Returns the alias prior to supporting user-defined aliases
	 * @param mspdf
	 * @return
	 */
	public static String getLegacyExternalQuantAlias( MSPropertyDataFile mspdf ) {
		// we want to consider mzXML and mzML as the same thing, so we'll use the "converted" value
		if( mspdf.getType().equals( MSFileInfo.MSFORMAT_MZXML_TYPE ) || mspdf.getType().equals( MSFileInfo.MSFORMAT_MZML_TYPE) ) {
			return FullMzXMLDataProcessor.DEFAULT_LABEL;
		}

		// we only supported Extract at this time!
		return ExtractDataProcessor.DEFAULT_LABEL;
	}

	public MSFile getMSFileWithReader(String msPath, String msExperimentType, boolean displayOnly) {
		MSFile msFile = getMSFileWithReader (msPath, msExperimentType);
		if (displayOnly) {
			if (getType().equals(MSFileInfo.MSFORMAT_MZML_TYPE) || getType().equals(MSFileInfo.MSFORMAT_MZXML_TYPE))
				msFile.setReader(new MzXmlReader());
		} else {
			if( isValidMSFile() ) {
				IMSFileReader reader = getReaderForFile();
				msFile.setReader(reader);
			}
		}
		return msFile;
	}
	
	public boolean isValidMSFile() {
		boolean isMzXml = (getType().equals(MSFileInfo.MSFORMAT_MZML_TYPE) || getType().equals(MSFileInfo.MSFORMAT_MZXML_TYPE));
		return isMzXml;
	}
	
	
	protected MSPropertyDataFile getNewMSPropertyDataFile(){
		MSPropertyDataFile clonePdf = new MSPropertyDataFile();
		return clonePdf;
	}
	
	public void clone(MSPropertyDataFile clonePdf) {
		clonePdf.setCategory(getCategory());
		clonePdf.setIsParent(getIsParent());
		clonePdf.setMSFileType(getMSFileType());
		clonePdf.setName(getName());
		clonePdf.setOriginalFileName(getOriginalFileName());
		clonePdf.setPurpose(getPurpose());
		clonePdf.setType(getType());
		clonePdf.setVersion(getVersion());
		
		if (getChildren() != null) {
			for (MSPropertyDataFile child: getChildren()) 
				clonePdf.addChild((MSPropertyDataFile) child.clone());
		}		
	}
	
	@Override
	public Object clone() {
		MSPropertyDataFile clonePdf = getNewMSPropertyDataFile();
		clone(clonePdf);
		return clonePdf;
	}
	
	
}
