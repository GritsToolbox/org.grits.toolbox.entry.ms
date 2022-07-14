package org.grits.toolbox.entry.ms.property.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.grits.toolbox.core.datamodel.io.PropertyWriter;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.datamodel.property.PropertyDataFile;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.jdom.Attribute;
import org.jdom.Element;

public class MassSpecPropertyWriter implements PropertyWriter
{
	@Override
	public void write(Property property, Element propertyElement) throws IOException {
		if(property instanceof MassSpecProperty) {
			MassSpecProperty msProperty = (MassSpecProperty) property;
			Iterator<PropertyDataFile> itr = msProperty.getDataFiles().iterator();
			while( itr.hasNext() ) {
				PropertyDataFile file = itr.next();
				Element fileElement = new Element("file");
				List<Attribute> attributes = new ArrayList<Attribute>();
				attributes.add(new Attribute("name", file.getName()));
				String version = file.getVersion() == null ? "Unversioned" : file.getVersion();
				attributes.add(new Attribute("version", version));
				String type = file.getType() == null ? PropertyDataFile.DEFAULT_TYPE : file.getType();
				attributes.add(new Attribute("type", type));
				fileElement.setAttributes(attributes);
//				propertyElement.setContent(fileElement);
				propertyElement.addContent(fileElement);
			}
		} else {
			throw new IOException("This property is not a Mass Spec Property");
		}
	}
}
