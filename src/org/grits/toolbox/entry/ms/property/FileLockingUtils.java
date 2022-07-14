package org.grits.toolbox.entry.ms.property;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.grits.toolbox.core.dataShare.PropertyHandler;

public class FileLockingUtils {
	
	public static final String LOCK_FILE = ".lockFile";
	
	/**
	 * reads the lock file 
	 * @param filePath path to the lock file
	 * @return FileLockManager object containing all files and their lock status
	 * @throws IOException
	 * @throws JAXBException
	 */
	public static FileLockManager readLockFile (String filePath) throws IOException, JAXBException {
		File lockFile = new File (filePath);
		FileLockManager mng = null;
		if (!lockFile.exists()) {
			// create the lock file
			mng = new FileLockManager();
			writeLockFile(mng, filePath);
		} else {
			// read the lock file
			FileInputStream inputStream = new FileInputStream(filePath);
	        InputStreamReader reader = new InputStreamReader(inputStream, PropertyHandler.GRITS_CHARACTER_ENCODING);
	        JAXBContext context = JAXBContext.newInstance(FileLockManager.class);
	        Unmarshaller unmarshaller = context.createUnmarshaller();
	        mng = (FileLockManager) unmarshaller.unmarshal(reader);
		}
        return mng;	
	}
	
	/**
	 * writes the lock file information back to the "lock file"
	 * @param mng object that contains the list of files and their lock status
	 * @param filePath path to the lock file
	 * 
	 * @throws JAXBException
	 * @throws IOException
	 */
	public static void writeLockFile (FileLockManager mng, String filePath) throws JAXBException, IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();   
        JAXBContext context = JAXBContext.newInstance(FileLockManager.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, PropertyHandler.GRITS_CHARACTER_ENCODING);
        marshaller.marshal(mng, os);
        
        File lockF = new File (filePath);
        if (lockF.exists())
        	lockF.delete();
        
        FileOutputStream outputStream = new FileOutputStream(filePath);
        byte[] strToBytes = os.toString((String) marshaller.getProperty(Marshaller.JAXB_ENCODING)).getBytes();
        outputStream.write(strToBytes);
        outputStream.close();
        
        lockF = new File (filePath);
        lockF.setReadOnly();
	}
}
