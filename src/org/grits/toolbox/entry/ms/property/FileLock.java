package org.grits.toolbox.entry.ms.property;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.grits.toolbox.core.datamodel.Entry;

/**
 * used for locking files when they are in use by annotation entries
 * 
 * @author sena
 *
 */
@XmlRootElement(name="file")
public class FileLock {
	String filename;
	Integer lockCount = 0;
	List<LockEntry> usedBy;
	
	/**
	 * @return the filename
	 */
	@XmlElement
	public String getFilename() {
		return filename;
	}
	/**
	 * @param filename the filename to set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}
	/**
	 * @return the lockCount
	 */
	@XmlAttribute
	public Integer getLockCount() {
		return lockCount;
	}
	/**
	 * @param lockCount the lockCount to set
	 */
	public void setLockCount(Integer lockCount) {
		this.lockCount = lockCount;
	}
	
	public void incrementLock() {
		this.lockCount++;
	}
	
	public void decrementLock() {
		if (this.lockCount > 0)
			this.lockCount --;
	}
	
	boolean isLocked () {
		return lockCount > 0;
	}
	
	public List<LockEntry> getUsedBy() {
		return usedBy;
	}
	
	public void setUsedBy(List<LockEntry> usedBy) {
		this.usedBy = usedBy;
	}
	
	/**
	 * adds the given entry into the usedBy list
	 * @param entry Entry to be added
	 */
	public void addEntry (Entry entry) {
		if (usedBy == null)
			usedBy = new ArrayList<>();
		LockEntry lockEntry = new LockEntry();
		lockEntry.entryId = entry.getId();
		lockEntry.entryName = entry.getDisplayName();
		if (findEntry(lockEntry) == null)
			usedBy.add(lockEntry);
	}
	
	private LockEntry findEntry (LockEntry entry) {
		if (usedBy == null) return null;
		for (LockEntry e: usedBy) {
			if (e.entryId != null && e.entryId.equals(entry.entryId))
				return e;
		}
		return null;
	}
	
	/**
	 * deletes the given entry from the usedBy list if exists
	 * 
	 * @param entry Entry to be removed
	 */
	public void deleteEntry (Entry entry) {
		if (usedBy == null)
			return;
		LockEntry lockEntry = new LockEntry();
		lockEntry.entryId = entry.getId();
		lockEntry.entryName = entry.getDisplayName();
		LockEntry toDelete = findEntry(lockEntry);
		if (toDelete != null)
			usedBy.remove(toDelete);
	}
	public void addEntry(LockEntry lockEntry) {
		if (usedBy == null)
			usedBy = new ArrayList<>();
		if (findEntry(lockEntry) == null)
			usedBy.add(lockEntry);
	}
	public void removeEntry(LockEntry lockEntry) {
		LockEntry existing = findEntry(lockEntry);
		if (existing != null)
			usedBy.remove(existing);
	}
}
