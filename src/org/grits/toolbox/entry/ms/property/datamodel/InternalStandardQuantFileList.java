package org.grits.toolbox.entry.ms.property.datamodel;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "internalStandardQuantFileList")
public class InternalStandardQuantFileList {
	// key is the name of the internal standard set
	private HashMap<String, ExternalQuantFileToAlias> quantNameToFileAlias = null;
	
	public InternalStandardQuantFileList() {
		quantNameToFileAlias = new HashMap<String, ExternalQuantFileToAlias>();
	}
	
	public void setQuantNameToFileAlias(HashMap<String, ExternalQuantFileToAlias> quantNameToFileAlias) {
		this.quantNameToFileAlias = quantNameToFileAlias;
	}
	@XmlElement(name="internalStandardQuantFileList")
	public HashMap<String, ExternalQuantFileToAlias> getQuantNameToFileAlias() {
		return quantNameToFileAlias;
	}	
	
	public ExternalQuantFileToAlias getExternalQuantToAliasByQuantType( String sQuantName ) {
		if( getQuantNameToFileAlias() == null ) {
			return null;
		}
		return getQuantNameToFileAlias().get(sQuantName);
	}
	
}
