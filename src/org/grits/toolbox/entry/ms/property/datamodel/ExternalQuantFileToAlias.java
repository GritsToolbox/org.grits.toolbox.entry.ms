package org.grits.toolbox.entry.ms.property.datamodel;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "externalQuantFileToAlias")
public class ExternalQuantFileToAlias {
	public static final String CURRENT_VERSION = "1.0";
	private HashMap<String, ExternalQuantAlias> sourceDataFileNameToAlias = null;
	
	public ExternalQuantFileToAlias() {
		sourceDataFileNameToAlias = new HashMap<String, ExternalQuantAlias>();
	}
	
	public void setSourceDataFileNameToAlias(HashMap<String, ExternalQuantAlias> sourceDataFileNameToAlias) {
		this.sourceDataFileNameToAlias = sourceDataFileNameToAlias;
	}
	@XmlElement(name="sourceDataFileNameToAlias")
	public HashMap<String, ExternalQuantAlias> getSourceDataFileNameToAlias() {
		return sourceDataFileNameToAlias;
	}
	
}
