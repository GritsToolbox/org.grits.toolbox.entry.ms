package org.grits.toolbox.entry.ms.property.datamodel;

import java.util.Date;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.grits.toolbox.core.datamodel.property.PropertyDataFile;
import org.grits.toolbox.ms.file.MSFileInfo;

/**
 * The meta-data associated with an MS Entry. Extends MassSpecUISettings.
 * @author D Brent Weatherly (dbrentw@uga.edu)
 * @see MassSpecUISettings
 *
 */
@XmlRootElement(name = "msMetaData")
public class MassSpecMetaData extends MassSpecUISettings {
	public static final String CURRENT_VERSION = "1.2";
	protected String name = null;
	protected String version = null;
	protected String description = null;
	protected String msExperimentType = null;
	protected String derivatizationName=null;
	protected String instrument=null;
	protected String collisionTypeName=null;
    protected Double collisionEnergy=null;
    protected String adductName=null;
    protected String releaseType=null;
    protected String glycanType=null;
        
    protected Date creationDate = null;
    protected Date updateDate = null;

    public MassSpecMetaData() {
    	super();
	}
    
    public static String getCurrentVersion() {
		return CURRENT_VERSION;
	}
    
	/**
	 * @return the name
	 */
	@XmlAttribute(name = "name", required= true)
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the description
	 */
	@XmlElement(name = "description", required= false)
	public String getDescription() {
		return description;
	}
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return the version
	 */
	@XmlAttribute(name = "version", required= true)
	public String getVersion() {
		return version;
	}
	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}
    
	/**
	 * @param msExperimentType the type of MS Experiment. See the MS_TYPE_* static variables in Method class.
	 * @see org.grits.toolbox.ms.om.data.Method
	 */
	@XmlAttribute(name = "msExperimentType", required= true)
	public void setMsExperimentType(String msExperimentType) {
		this.msExperimentType = msExperimentType;
	}    
    /**
     * @return the type of MS Experiment.
     */
    public String getMsExperimentType() {
		return msExperimentType;
	}
   
	/**
	 * @return the derivatizationName
	 */
	@XmlAttribute(name = "derivatizationName", required=false)
    public String getDerivatizationName() {
        return derivatizationName;
    }
	/**
	 * @param derivatizationName the derivatizationName to set
	 */
    public void setDerivatizationName(String derivatizationName) {
        this.derivatizationName = derivatizationName;
    }

	/**
	 * @return the instrument
	 */
	@XmlAttribute(name = "instrument", required=false)
    public String getInstrument() {
        return instrument;
    }
	/**
	 * @param instrument the instrument to set
	 */
    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

	/**
	 * @return the collisionTypeName
	 */
	@XmlAttribute(name = "collisionTypeName", required=false)
    public String getCollisionTypeName() {
        return collisionTypeName;
    }
	/**
	 * @param collisionTypeName the collisionTypeName to set
	 */
    public void setCollisionTypeName(String collisionTypeName) {
        this.collisionTypeName = collisionTypeName;
    }

	/**
	 * @return the collisionEnergy
	 */
	@XmlAttribute(name = "collisionEnergy", required=false)
    public Double getCollisionEnergy() {
        return collisionEnergy;
    }
	/**
	 * @param collisionEnergy the collisionEnergy to set
	 */
    public void setCollisionEnergy(Double collisionEnergy) {
        this.collisionEnergy = collisionEnergy;
    }

	/**
	 * @return the adductName
	 */
	@XmlAttribute(name = "adductName", required=false)
    public String getAdductName() {
        return adductName;
    }
	/**
	 * @param adductName the adductName to set
	 */
    public void setAdductName(String adductName) {
        this.adductName = adductName;
    }

	/**
	 * @return the releaseType
	 */
	@XmlAttribute(name = "releaseType", required=false)
    public String getReleaseType() {
        return releaseType;
    }
	/**
	 * @param releaseType the releaseType to set
	 */
    public void setReleaseType(String releaseType) {
        this.releaseType = releaseType;
    }

	/**
	 * @return the glycanType
	 */
	@XmlAttribute(name = "glycanType", required=false)
    public String getGlycanType() {
        return glycanType;
    }
	/**
	 * @param glycanType the glycanType to set
	 */
    public void setGlycanType(String glycanType) {
        this.glycanType = glycanType;
    }
        
	@XmlAttribute
	public Date getCreationDate() {
		return creationDate;
	}
		
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	
	@XmlAttribute
	public Date getUpdateDate() {
		return updateDate;
	}
	
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	public static PropertyDataFile getNewSettingsFile( String msDetails, MassSpecMetaData metaData ) {
		PropertyDataFile msPropertyDataFile = new PropertyDataFile(msDetails, MassSpecMetaData.CURRENT_VERSION, MSFileInfo.MSMETADATA_TYPE);	
		return msPropertyDataFile;
	}
	
	@Override
	public Object clone() {
		MassSpecMetaData newSettings = new MassSpecMetaData();
		cloneSettings(newSettings);
		return newSettings;
	}
	
	@Override
	public void cloneSettings(MassSpecUISettings settings) {
		MassSpecMetaData newSettings = (MassSpecMetaData) settings;
		newSettings.setAdductName(adductName);
		newSettings.setCollisionEnergy(collisionEnergy);
		newSettings.setCollisionTypeName(collisionTypeName);
		newSettings.setDerivatizationName(derivatizationName);
		newSettings.setGlycanType(glycanType);
		newSettings.setInstrument(instrument);
		newSettings.setMsExperimentType(msExperimentType);
		newSettings.setReleaseType(releaseType);
		newSettings.setUpdateDate(updateDate);
		newSettings.setCreationDate(creationDate);
		super.cloneSettings(settings);
	}
}
