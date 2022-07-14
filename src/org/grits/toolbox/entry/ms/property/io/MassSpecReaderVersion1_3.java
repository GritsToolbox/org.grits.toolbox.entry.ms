package org.grits.toolbox.entry.ms.property.io;

import java.util.List;

import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.entry.ms.preference.MassSpecPreference;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecStandardQuant;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecMetaData;
import org.jdom.Element;

/**
 * 
 * @author Brent Weatherly
 *
 */
public class MassSpecReaderVersion1_3
{
	public static Property read(Element propertyElement, MassSpecProperty msProperty)  {
		msProperty = (MassSpecProperty) MassSpecReaderVersion1_2.read(propertyElement, msProperty);
		MassSpecMetaData msMetaData = msProperty.getMassSpecMetaData();
		List<MassSpecStandardQuant> sq = MassSpecPreference.unmarshalStandardQuantList(msMetaData.getStandardQuantText());
		msMetaData.setStandardQuant(sq);
		return msProperty;
	}
}
