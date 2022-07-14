package org.grits.toolbox.entry.ms.property.datamodel;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "externalQuantAlias")
public class ExternalQuantAlias {
	public static final String CURRENT_VERSION = "1.0";

	private String sKey = null;
	private Integer id = null;
	private String sAlias = null;
	
	public void setAlias(String sAlias) {
		this.sAlias = sAlias;
	}
	@XmlAttribute(name="headerAlias")
	public String getAlias() {
		return sAlias;
	}
	
	public void setKey(String sKey) {
		this.sKey = sKey;
	}
	@XmlAttribute(name="headerKey")
	public String getKey() {
		return sKey;
	}
	
	public Integer getId() {
		return id;
	}
	@XmlAttribute(name="headerId")
	public void setId(Integer id) {
		this.id = id;
	}
	
}
