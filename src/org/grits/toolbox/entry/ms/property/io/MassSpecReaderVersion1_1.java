package org.grits.toolbox.entry.ms.property.io;

import java.util.Iterator;

import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.datamodel.property.PropertyDataFile;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecMetaData;
import org.grits.toolbox.ms.file.MSFileInfo;
import org.jdom.Element;

/**
 * 
 * @author Brent Weatherly
 *
 */
public class MassSpecReaderVersion1_1
{
	public static Property read(Element propertyElement, MassSpecProperty msProperty)  {
		// the only difference between version 1.0 and 1.2 is CustomAnnotationData
		// I didn't create a separate reader for 1.2 when I added CustomAnnotationData though
		// So the property version before 1.2 won't necessary be accurate
		// But in 1.0, the custom annotation data will be null and ignored
		if( msProperty.getDataFiles() != null && ! msProperty.getDataFiles().isEmpty() ) {
			Iterator<PropertyDataFile> itr = msProperty.getDataFiles().iterator();
			while( itr.hasNext() ) {
				PropertyDataFile pdf = itr.next();
				if( pdf.getType().equals( MSFileInfo.MSMETADATA_TYPE ) ) {
					pdf.setVersion( MassSpecMetaData.CURRENT_VERSION );
				}
			}
		}
		return MassSpecReaderVersion1_2.read(propertyElement, msProperty);
	}
}
