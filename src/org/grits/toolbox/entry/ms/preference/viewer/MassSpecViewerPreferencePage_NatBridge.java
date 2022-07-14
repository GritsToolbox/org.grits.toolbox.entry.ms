package org.grits.toolbox.entry.ms.preference.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Composite;
import org.grits.toolbox.datamodel.ms.preference.MassSpecViewerPreference;
import org.grits.toolbox.datamodel.ms.preference.MassSpecViewerPreferenceLoader;
import org.grits.toolbox.datamodel.ms.tablemodel.FillTypes;
import org.grits.toolbox.datamodel.ms.tablemodel.MassSpecTableDataObject;
import org.grits.toolbox.display.control.table.datamodel.GRITSColumnHeader;
import org.grits.toolbox.display.control.table.datamodel.GRITSListDataRow;
import org.grits.toolbox.display.control.table.preference.TableViewerColumnSettings;
import org.grits.toolbox.display.control.table.preference.TableViewerPreference;
import org.grits.toolbox.display.control.table.tablecore.GRITSTable;
import org.grits.toolbox.entry.ms.process.loader.MassSpecTableDataProcessor;
import org.grits.toolbox.entry.ms.process.loader.MassSpecTableDataProcessorUtil;
import org.grits.toolbox.entry.ms.tablehelpers.MassSpecTable;


/**
 * @author D Brent Weatherly (dbrentw@uga.edu)
 * 
 * Description: Links the NatTable columns to the preferences
 *
 */
public class MassSpecViewerPreferencePage_NatBridge {
	//log4J Logger
	private static final Logger logger = Logger.getLogger(MassSpecViewerPreferencePage_NatBridge.class);
	protected int iMSLevel;
	protected GRITSTable natTable = null;
	protected FillTypes fillType;

	/**
	 * @param parent - the parent Composite (viewer)
	 * @param iMSLevel - current MS leel
	 * @param fillType - FillType to specify column settings for
	 */
	public MassSpecViewerPreferencePage_NatBridge( Composite parent, int iMSLevel, FillTypes fillType ) {
		natTable = getNewSimianTable(parent);
		this.iMSLevel = iMSLevel;
		this.fillType = fillType;
	}

	/**
	 * @param _bDefault - boolean value telling whether or not to restore defaults
	 * 
	 * Description:  initializes the preferences and the associate NatTable for viewing the ColumnChooser with the 
	 * preferred column visibility and order.  
	 */
	public void initializeComponents( boolean _bDefault ) {
		try {
			initializePreferences();
			if ( _bDefault || natTable.getGRITSTableDataObject().getTablePreferences().getPreferenceSettings() == null ) {
				setDefaultPreferences();
			}
			postProcessPreferences();
			initializeTableData(_bDefault);
			natTable.initCommonTableComponents();
		} catch( Exception e ) {
			logger.error("Unable to initialize MassSpecViewerPreferenceUI_NatBridge.", e);
		}		
	}

	/**
	 * @return MassSpecViewerPreference - the currentpreference associated with the underlying NatTable
	 */
	public MassSpecViewerPreference getPreference() {
		return (MassSpecViewerPreference) natTable.getGRITSTableDataObject().getTablePreferences();
	}

	/**
	 * @param parent - the parent Composite (viewer)
	 * @return GRITSTable - an instance of MassSpecTable
	 */
	protected GRITSTable getNewSimianTable(Composite parent) {
		return new MassSpecTable( parent, null ); 		
	}

	/**
	 * @return ArrayList<ArrayList<GRITSColumnHeader>> - List of list of GRITSColumnHeader used to manage column settings in preferences
	 */
	protected ArrayList<ArrayList<GRITSColumnHeader>> getEmptyHeader() {
		// this is a hack, but in order to use same code for the column chooser, we have to have some data...
		ArrayList<ArrayList<GRITSColumnHeader>> alTable = new ArrayList<ArrayList<GRITSColumnHeader>>();
		ArrayList<GRITSColumnHeader> alRow = new ArrayList<GRITSColumnHeader>();
		if( natTable.getGRITSTableDataObject().getTablePreferences().getPreferenceSettings() == null ) {
			natTable.getGRITSTableDataObject().initializePreferences();
		}
		Iterator<GRITSColumnHeader> itr = natTable.getGRITSTableDataObject().getTablePreferences().getPreferenceSettings().getHeaders().iterator();
		while( itr.hasNext() ) {
			GRITSColumnHeader header = itr.next();
			alRow.add(header);
		}
		if ( alRow == null || alRow.isEmpty() )
			return alTable;
		Collections.sort(alRow);
		alTable.add(alRow);
		return alTable;		
	}

	/**
	 * @return ArrayList<GRITSListDataRow> - an empty table to ensure that the ColumnChooser works properly
	 */
	@SuppressWarnings("unchecked")
	protected ArrayList<GRITSListDataRow> getEmptyData() {
		// this is a hack, but in order to use same code for the column chooser, we have to have some data...
		if (  natTable.getGRITSTableDataObject() == null || natTable.getGRITSTableDataObject().getTablePreferences() == null || 
				natTable.getGRITSTableDataObject().getTablePreferences().getPreferenceSettings() == null ) 
			return null;
		ArrayList<GRITSListDataRow> alTable = new ArrayList<GRITSListDataRow>();
		ArrayList<Object> alRow = new ArrayList<Object>();
		TableViewerColumnSettings settings = natTable.getGRITSTableDataObject().getTablePreferences().getPreferenceSettings();
		Iterator<GRITSColumnHeader> itr = settings.getHeaders().iterator();
		while( itr.hasNext() ) {
			GRITSColumnHeader header = itr.next();
			alRow.add("Blah");
		}
		GRITSListDataRow newRow = new GRITSListDataRow(0, alRow);
		if ( alRow == null || alRow.isEmpty() )
			return alTable;
		//		newRow.getDataRow().add(alRow);
		alTable.add(newRow);
		return alTable;		
	}

	/**
	 * @return MassSpecTableDataObject - a newly instantiated MassSpecTableDataObject
	 */ 
	protected MassSpecTableDataObject getNewTableDataObject() {
		return new MassSpecTableDataObject(this.iMSLevel, this.fillType);
	}

	/**
	 * @return TableViewerColumnSettings - a newly instantiated TableViewerColumnSettings object
	 */
	protected TableViewerColumnSettings getNewTableViewerSettings() {
		return new TableViewerColumnSettings();
	}

	/**
	 * To be overridden. Allows changing of labels, etc after the preferences have been read. 
	 */
	protected void postProcessPreferences() {
		;
	}
	
	/**
	 * @throws Exception
	 * 
	 * Initializes the MassSpecTableDataObject and the preferences
	 */
	protected void initializePreferences() throws Exception {
		natTable.setSimDataObject(getNewTableDataObject());
		((MassSpecTableDataObject) natTable.getGRITSTableDataObject()).initializePreferences();		
	}

	/**
	 * Sets the default column settings for the MassSpec NatTable
	 */
	protected void setDefaultPreferences() {
		TableViewerColumnSettings tvcs = getDefaultSettings();
		MassSpecTableDataProcessor.setDefaultColumnViewSettings(this.fillType, tvcs);
		natTable.getGRITSTableDataObject().getTablePreferences().setPreferenceSettings(tvcs);
		natTable.getGRITSTableDataObject().getTablePreferences().setColumnSettings(natTable.getGRITSTableDataObject().getTablePreferences().toString());
	}

	/**
	 * @param _bDefault - boolean value telling whether or not to restore defaults
	 * @throws Exception
	 * 
	 * Description:  initializes the MassSpecTableDataObject with the empty header and table  
	 */
	protected void initializeTableData( boolean _bDefaults ) throws Exception {
		ArrayList<ArrayList<GRITSColumnHeader>> prefData = getEmptyHeader();
		if ( prefData == null || prefData.isEmpty() ) {
			throw new Exception("Unable to initialize preferences.");
		}
		natTable.getGRITSTableDataObject().setHeaderData(prefData);
		ArrayList<GRITSListDataRow> emptyData = getEmptyData();
		natTable.getGRITSTableDataObject().setTableData(emptyData);
	}

	/**
	 * @return TableViewerPreference - a newly instantiated TableViewerPreference
	 */
	protected TableViewerPreference getNewTableViewerPreference() {
		return new TableViewerPreference();
	}

	/**
	 * @return TableViewerColumnSettings - newly instantiated TableViewerColumnSettings with default settings
	 */
	protected TableViewerColumnSettings getDefaultSettings() {
		TableViewerColumnSettings newSettings = getNewTableViewerSettings();
		if( this.fillType == FillTypes.Scans ) {
			MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsScan( newSettings, this.iMSLevel, false, true);
		}
		else if ( this.fillType == FillTypes.PeakList ) {
			MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsPeakList( newSettings);
		}
		else if ( this.fillType == FillTypes.PeaksWithFeatures ) {
			MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsPeakWithFeatures( newSettings, false);
		}
		return  newSettings;
	}

	/**
	 * Gets the preference object for the currently visible window and write them out to make them default.
	 */
	public void updatePreferences() {
		natTable.updatePreferenceSettingsFromCurrentView();
		natTable.getGRITSTableDataObject().getTablePreferences().writePreference();
	}

	/**
	 * @param _iMSLevel - the desired MS level
	 * @param _fillType - the FillType at the MS level (Scans or PeaksWithFeatures)
	 * @return MassSpecViewerPreference - the preferences read from persistent storage
	 */
	protected MassSpecViewerPreference getCurrentTableViewerPreference( int _iMSLevel, FillTypes _fillType ) {
		return MassSpecViewerPreferenceLoader.getTableViewerPreference(_iMSLevel, _fillType);
	}

	/**
	 * @return GRITSTable - The GRITSTable associated with this NatBridge
	 */
	public GRITSTable getNatTable() {
		return natTable;
	}

}
