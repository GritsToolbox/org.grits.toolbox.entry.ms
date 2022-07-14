package org.grits.toolbox.entry.ms.property;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="entry")
public class LockEntry {
	String entryName;
	Integer entryId;
	
	public LockEntry() {	
	}
	
	@XmlAttribute
	public Integer getEntryId() {
		return entryId;
	}
	
	@XmlAttribute
	public String getEntryName() {
		return entryName;
	}
	
	public void setEntryId(Integer entryId) {
		this.entryId = entryId;
	}
	
	public void setEntryName(String entryName) {
		this.entryName = entryName;
	}
}
