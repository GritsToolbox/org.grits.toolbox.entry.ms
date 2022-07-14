package org.grits.toolbox.entry.ms.property.io;

import java.io.File;
import java.util.List;

import org.grits.toolbox.core.dataShare.PropertyHandler;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.entry.ms.preference.MassSpecPreference;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecCustomAnnotation;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecMetaData;
import org.jdom.Element;

/**
 * 
 * @author Brent Weatherly
 *
 */
public class MassSpecReaderVersion1_2
{
	public static Property read(Element propertyElement, MassSpecProperty msProperty)  {
		msProperty.adjustPropertyFilePaths();
		
		// DBW 08/22/17: Is this call necessary? I think the path adjustment was just for old projects and should no longer be necessary
		// Sena 09/21/17: at this point, msSettings is always null!
		if (msProperty.getMassSpecMetaData() != null && msProperty.getMassSpecMetaData().getFileList() != null) {
			msProperty.getMassSpecMetaData().adjustPropertyFilePaths();
		}
		Element entryElement = propertyElement.getDocument().getRootElement().getChild("entry");
		String projectName = entryElement == null ? null : entryElement.getAttributeValue("name");

		String workspaceFolder = PropertyHandler.getVariable("workspace_location");
		String msFolder = workspaceFolder.substring(0, workspaceFolder.length()-1) 
				+ File.separator
				+ projectName + File.separator
				+ MassSpecProperty.getFoldername();

		// lets read the settings file
		String msFile = msProperty.getMSSettingsFile().getName();
		String fullPath = msFolder + File.separator + msFile;
		MassSpecMetaData msMetaData = msProperty.unmarshallSettingsFile(fullPath);
		// fix the file paths here!
		msMetaData.adjustPropertyFilePaths();
		List<MassSpecCustomAnnotation> ca = MassSpecPreference.unmarshalCustomAnnotationsList(msMetaData.getCustomAnnotationText());
		msMetaData.setCustomAnnotations(ca);
		msProperty.setMassSpecMetaData(msMetaData);
		return msProperty;
	}
}
