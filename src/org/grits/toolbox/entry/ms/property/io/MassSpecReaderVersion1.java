package org.grits.toolbox.entry.ms.property.io;

import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.jdom.Element;

/**
 * 
 * @author Brent Weatherly
 *
 */
public class MassSpecReaderVersion1
{
	public static Property read(Element propertyElement, MassSpecProperty msProperty)  {
		// the only difference between version 1.0 and 1.2 is CustomAnnotationData
		// I didn't create a separate reader for 1.2 when I added CustomAnnotationData though
		// So the property version before 1.2 won't necessary be accurate
		// But in 1.0, the custom annotation data will be null and ignored
		// call the 1.1 reader which will fix everything
		return MassSpecReaderVersion1_1.read(propertyElement, msProperty);
	}
}
