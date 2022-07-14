package org.grits.toolbox.entry.ms.property;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.grits.toolbox.core.datamodel.Entry;

@XmlRootElement(name="file_locks")
public class FileLockManager {
	private static final Logger logger = Logger.getLogger(FileLockManager.class);
	public static final String LOCKFILE_NAME = ".lockFile";
	private List<FileLock> fileList = new ArrayList<>();
	
	/**
	 * Load the contents of the .lockfile at the specified location
	 * 
	 * @param filePath file path to the lock file
	 */
	public FileLockManager (String filePath) {
		try {
			FileLockManager loaded = FileLockingUtils.readLockFile(filePath);
			this.fileList = loaded.fileList;
		} catch (IOException e) {
			logger.error("Lock file cannot be loaded", e);
		} catch (JAXBException e) {
			logger.error("Lock file cannot be loaded", e);
		}
	}

	public FileLockManager() {
		
	}

	/**
	 * @return the fileList
	 */
	public List<FileLock> getFileList() {
		return fileList;
	}

	/**
	 * @param fileList the fileList to set
	 */
	public void setFileList(List<FileLock> fileList) {
		this.fileList = fileList;
	}
	
	/**
	 * increments the lock count of the file given by the fileName
	 * @param fileName name of the file to be locked
	 * @param entry Entry that uses the file
	 */
	public void lockFile(String fileName, Entry entry) {
		FileLock existing = findFile(fileName);
		if (existing == null) {
			existing = new FileLock();
			existing.setFilename(fileName);
			fileList.add(existing);
		}
		existing.addEntry(entry);
		existing.incrementLock();
	}
	
	/**
	 * adds file given by the fileName to the list of files
	 * @param fileName name of the file to be added
	 */
	public void addFile(String fileName) {
		FileLock existing = findFile(fileName);
		if (existing == null) {
			existing = new FileLock();
			existing.setFilename(fileName);
			fileList.add(existing);
		}
	}
	
	/**
	 * finds the FileLock from the fileList for the file given by the filename
	 * 
	 * @param filename name of the file to be searched
	 * @return FileLock object for the given file if exists, null if not
	 */
	public FileLock findFile (String filename) {
		File file = new File (filename);
		for (FileLock fileLock : fileList) {
			File file2 = new File (fileLock.getFilename());
			if (!file.getName().equals(file2.getName()))
				continue;
			if (file.getParent() != null && !file.getParent().equals(file2.getParent()))
				continue;
			return fileLock;
		}
		return null;
	}
	
	/**
	 * returns whether the given file is already locked
	 * @param filename name of the file to be checked
	 * @return true if the file is locked, false otherwise or if the file is not found 
	 */
	public boolean isFileLocked(String filename) {
		FileLock existing = findFile(filename);
		if (existing != null)
			return existing.isLocked();
		return false;
	}

	/**
	 * deletes the given file if it exists in the file list
	 * does not check if the file is locked or not, 
	 * it is the user's responsibility to check before calling this method to delete
	 * 
	 * @param fileName file to be removed from the list of files
	 */
	public void deleteFile(String fileName) {
		FileLock existing = findFile(fileName);
		if (existing != null) fileList.remove(existing);
	}

	/**
	 * This version is used if you only have "id" and the "name" for the entry, not the Entry object
	 * 
	 * @param fileName file to be locked
	 * @param entry LockEntry object with entry id and name
	 */
	public void lockFile(String fileName, LockEntry entry) {
		FileLock existing = findFile(fileName);
		if (existing == null) {
			existing = new FileLock();
			existing.setFilename(fileName);
			fileList.add(existing);
		}
		existing.addEntry(entry);
		existing.incrementLock();
	}

	/**
	 * decrements the lock count for the given file
	 * This version is used if you only have "id" and the "name" for the entry, not the Entry object
	 * 
	 * @param fileName name of the file to be unlocked
	 * @param usedBy entry that no longer uses the file
	 */
	public void removeLock(String fileName, LockEntry usedBy) {
		FileLock existing = findFile(fileName);
		if (existing != null) {
			existing.decrementLock();
			existing.removeEntry (usedBy);
		}
	}
	
	/**
	 * decrements the lock count for the given file
	 * 
	 * @param fileName name of the file to be unlocked
	 * @param entry entry that no longer uses the file
	 */
	public void removeLock(String fileName, Entry entry) {
		FileLock existing = findFile(fileName);
		if (existing != null) {
			existing.decrementLock();
			existing.deleteEntry (entry);
		}
	}
}
