package org.grits.toolbox.entry.ms.property;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.jface.resource.ImageDescriptor;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.io.PropertyWriter;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.entry.ms.property.datamodel.MSPropertyDataFile;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecUISettings;

public class MassSpecEntityProperty extends Property {	
	public static final String TYPE = MassSpecProperty.class.getName();
	protected Integer scanNum = null;
	protected Integer parentScanNum = null;
	protected Double dMz = null;
	protected Double dParentMz = null;
	protected Integer msLevel = null;
	private String id = null;
	protected String description = null;
	private MassSpecProperty parentProperty = null;
	private Integer iPeakListNumber = 1;
	
	public MassSpecEntityProperty( MassSpecProperty parentProperty )
	{
		super();
		this.parentProperty = parentProperty;
		this.iPeakListNumber = 1;
	}
	
	@Override
	public Object clone() {
		MassSpecEntityProperty newProp = new MassSpecEntityProperty(this.parentProperty);
		newProp.setDescription(this.getDescription());
		newProp.setId(this.getId());
		newProp.setScanNum(this.getScanNum());
		newProp.setMsLevel(this.msLevel);
		newProp.setParentScanNum(getParentScanNum());
		newProp.setPeakListNumber(this.iPeakListNumber);
		newProp.setMz(this.dMz);
		newProp.setParentMz(this.dParentMz);
		newProp.setDataFile(getDataFile());
		return newProp;
	}

	
	public Integer getPeakListNumber() {
		return iPeakListNumber;
	}
	
	public void setPeakListNumber(Integer iPeakListNumber) {
		this.iPeakListNumber = iPeakListNumber;
	}
	
	public Double getMz() {
		return dMz;
	}
	
	public Double getParentMz() {
		return dParentMz;
	}
	
	public void setMz(Double dMz) {
		this.dMz = dMz;
	}
	
	public void setParentMz(Double dParentMz) {
		this.dParentMz = dParentMz;
	}
	
	/**
	 * This property should have only one data file
	 * 
	 * @return the first MSPropertyDataFile
	 */
	public MSPropertyDataFile getDataFile() {
		if (!getDataFiles().isEmpty())
			return (MSPropertyDataFile) getDataFiles().get(0);
		return null;
	}
	
	/**
	 * set the data file. There can only be one data file. Therefore the list is cleared first.
	 * 
	 * @param file MSPropertyDataFile to add to the list of data files
	 */
	public void setDataFile(MSPropertyDataFile file) {
		if (dataFiles == null)
			dataFiles = new ArrayList<>();
		dataFiles.clear();
		dataFiles.add(file);
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( ! (obj instanceof MassSpecEntityProperty) )
			return false;
		
		MassSpecEntityProperty castObj = (MassSpecEntityProperty) obj;
		boolean bRes = getDescription() != null && getDescription().equals( castObj.getDescription() );
		bRes &= getId() != null && getId().equals( castObj.getId() );
		bRes &= getScanNum() != null && getScanNum().equals( castObj.getScanNum() );
		bRes &= getMsLevel() != null && getMsLevel().equals( castObj.getMsLevel() );
		return bRes;
	}
	
	public static Entry getTableCompatibleEntry( Entry parentEntry ) {
		Entry msEntry = MassSpecProperty.getMSParentEntry(parentEntry);
		MassSpecProperty msProp = null;
		if( msEntry != null ) {
			msProp = (MassSpecProperty) msEntry.getProperty();
		} else {
			// error!
			return null;
		}
		MassSpecEntityProperty msEntityProp = new MassSpecEntityProperty(msProp);		
		Entry newEntry = new Entry();
		newEntry.setProperty(msEntityProp);	
		newEntry.setDisplayName(parentEntry.getDisplayName());
		newEntry.setParent(parentEntry);
		return newEntry;
	}
	
	public Integer getParentScanNum() {
		return parentScanNum;
	}
	
	public void setParentScanNum(Integer parentScanNum) {
		this.parentScanNum = parentScanNum;
	}

	@Override
	public Property getParentProperty() {
		return parentProperty;
	}	
	
	public MassSpecProperty getMassSpecParentProperty() {
		return parentProperty;
	}
	
	public void setMassSpecParentProperty(MassSpecProperty parentProperty) {
		this.parentProperty = parentProperty;
	}
	
	public Integer getScanNum() {
		return scanNum;
	}
	
	public void setScanNum(Integer scanNum) {
		this.scanNum = scanNum;
	}
	
	public Integer getMsLevel() {
		return msLevel;
	}
	
	public void setMsLevel(Integer msLevel) {
		this.msLevel = msLevel;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}	

	@Override
	public String getType() {
		return MassSpecEntityProperty.TYPE;
	}

	@Override
	public PropertyWriter getWriter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImageDescriptor getImage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(Entry entry) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	public MassSpecUISettings getMassSpecUISettings() {
		return getMassSpecParentProperty().getMassSpecMetaData();
	}
}
