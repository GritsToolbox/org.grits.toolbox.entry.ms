package org.grits.toolbox.entry.ms.property.io;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.grits.toolbox.core.dataShare.PropertyHandler;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.UnsupportedTypeException;
import org.grits.toolbox.core.datamodel.UnsupportedVersionException;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.datamodel.property.PropertyDataFile;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecUISettings;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecMetaData;
import org.jdom.Element;

/**
 * 
 * @author Brent Weatherly
 *
 */
public class MassSpecReaderVersion0 {
	private static final Logger logger = Logger.getLogger(MassSpecReaderVersion0.class);

	public static Property read(Element propertyElement, MassSpecProperty msProperty) throws IOException, UnsupportedVersionException, UnsupportedTypeException {
		String t_attributeValue = null;
		Element entryElement = propertyElement.getDocument().getRootElement().getChild("entry");
		String projectName = entryElement == null ? null : entryElement.getAttributeValue("name");

		String workspaceFolder = PropertyHandler.getVariable("workspace_location");
		String msFolder = workspaceFolder.substring(0, workspaceFolder.length()-1) 
				+ File.separator
				+ projectName + File.separator
				+ MassSpecProperty.getFoldername();

		Entry msEntry = new Entry();

		MassSpecMetaData model = new MassSpecMetaData();
		msProperty.setMassSpecMetaData(model);
		msEntry.setProperty(msProperty);

		Element child = propertyElement.getChild("settings");
		if( child != null ) {
			//     <settings derivatisation="" instrument="" experimentType="Direct Infusion" collusionTypeName="" 
			//               collusionEnergy="0.0" adductName="" releaseType="" glycanType="" />

			t_attributeValue = child.getAttributeValue("derivatisation");
			model.setDerivatizationName(t_attributeValue);
			t_attributeValue = child.getAttributeValue("instrument");
			model.setInstrument(t_attributeValue);
			t_attributeValue = child.getAttributeValue("experimentType");
			model.setMsExperimentType(t_attributeValue);
			t_attributeValue = child.getAttributeValue("collusionTypeName"); // used to be collusion..duh
			model.setCollisionTypeName(t_attributeValue);
			t_attributeValue = child.getAttributeValue("collusionEnergy");
			model.setCollisionEnergy(Double.parseDouble(t_attributeValue));
			t_attributeValue = child.getAttributeValue("adductName");
			model.setAdductName(t_attributeValue);
			t_attributeValue = child.getAttributeValue("glycanType");
			model.setGlycanType(t_attributeValue);		
		} 
		if( model.getMsExperimentType() == null || model.getMsExperimentType().equals("") ) {
			throw new UnsupportedVersionException("Expecting the 'settings' element and 'experimentType' attribute in the project file.", "Preversion");
		}
		
		
		//Set msn
		String origRawFile = null;
		child = propertyElement.getChild("spectra_msn");
		if(child != null) {
			t_attributeValue = child.getAttributeValue("raw");
			if ( t_attributeValue != null ) {				
				origRawFile = t_attributeValue;
				PropertyDataFile rawFile = MassSpecUISettings.getLegacyVersionRawPropertyDataFile(t_attributeValue, t_attributeValue, false);
				msProperty.getDataFiles().add(rawFile);
				//				origRawFile = msFolder + File.separator + t_attributeValue;
			}

			t_attributeValue = child.getAttributeValue("mzXML");
			if ( t_attributeValue != null ) {
				PropertyDataFile mzFile = MassSpecUISettings.getLegacyVersionMzPropertyDataFile(t_attributeValue, t_attributeValue, false);
				msProperty.getDataFiles().add(mzFile);
			}
		} 
		if( origRawFile == null ){
			throw new UnsupportedVersionException("Expecting the 'spectra_msn' element and 'raw' attribute in the project file.", "Preversion");
		}
		
		// parse out the date prefix from the raw file so we can write the details
		Pattern p = Pattern.compile("^(\\d{4}\\.\\d{2}\\.\\d{2}\\-\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{4})\\.");
		Matcher m = p.matcher(origRawFile);
		String datePrefix = null;
		if( m.find() ) {
			datePrefix = m.group(1);			
		} else { // make new
			SimpleDateFormat formater = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss.SSSS");
			Date date = new Date();
			datePrefix = formater.format(date);			
		}
		
		String msDetails = datePrefix + "." + "msMetaData.xml";
		msProperty.marshallSettingsFile(msFolder + File.separator + msDetails, model);
		model.setVersion(MassSpecMetaData.CURRENT_VERSION);
		PropertyDataFile msMetaData = MassSpecMetaData.getNewSettingsFile(msDetails, model);
		msProperty.getDataFiles().add(msMetaData);
		
		// full 
		child = propertyElement.getChild("spectra_full");
		if(child != null) {
			t_attributeValue = child.getAttributeValue("raw");
			if ( t_attributeValue != null ) {
				PropertyDataFile rawFile = MassSpecUISettings.getLegacyVersionRawPropertyDataFile(t_attributeValue, t_attributeValue, true);
				msProperty.getDataFiles().add(rawFile);
//				origFullRawFile = msFolder + File.separator + t_attributeValue;
			}
			t_attributeValue = child.getAttributeValue("mzXML");
			if ( t_attributeValue != null ) {
				PropertyDataFile mzFile = MassSpecUISettings.getLegacyVersionMzPropertyDataFile(t_attributeValue, t_attributeValue, true);
				msProperty.getDataFiles().add(mzFile);
//				origFullMzXMLFile = msFolder + File.separator + t_attributeValue;
			}
		}
		child = propertyElement.getChild("peakListInfo");
		if(child != null) {
			String origPeakListFile = null;
			String origPeakListFormat = null;
			t_attributeValue = child.getAttributeValue("peakListFileName");
			if ( t_attributeValue != null ) {
				origPeakListFile = msFolder + File.separator + t_attributeValue;
			}
			t_attributeValue = child.getAttributeValue("peakListFileFormat");
			if ( t_attributeValue != null ) {
				origPeakListFormat = msFolder + File.separator + t_attributeValue;
			}
			PropertyDataFile peakListFile = MassSpecUISettings.getLegacyVersionPeakListPropertyDataFile(origPeakListFile, origPeakListFile, origPeakListFormat);
			if( peakListFile != null ) {
				msProperty.getDataFiles().add(peakListFile);
			}
		}

		return msProperty;
	}

}
