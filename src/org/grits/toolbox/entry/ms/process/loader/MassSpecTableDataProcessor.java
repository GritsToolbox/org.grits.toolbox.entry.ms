package org.grits.toolbox.entry.ms.process.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.datamodel.ms.preference.MassSpecViewerPreference;
import org.grits.toolbox.datamodel.ms.tablemodel.FillTypes;
import org.grits.toolbox.datamodel.ms.tablemodel.MassSpecTableDataObject;
import org.grits.toolbox.datamodel.ms.tablemodel.dmtranslate.DMPeak;
import org.grits.toolbox.datamodel.ms.tablemodel.dmtranslate.DMPrecursorPeak;
import org.grits.toolbox.datamodel.ms.tablemodel.dmtranslate.DMScan;
import org.grits.toolbox.display.control.table.datamodel.GRITSColumnHeader;
import org.grits.toolbox.display.control.table.datamodel.GRITSListDataRow;
import org.grits.toolbox.display.control.table.preference.TableViewerColumnSettings;
import org.grits.toolbox.display.control.table.preference.TableViewerPreference;
import org.grits.toolbox.display.control.table.process.TableDataProcessor;
import org.grits.toolbox.entry.ms.extquantfiles.process.CustomAnnotationDataProcessor;
import org.grits.toolbox.entry.ms.extquantfiles.process.ExtractDataProcessor;
import org.grits.toolbox.entry.ms.extquantfiles.process.ExtractExternalQuantDataProcessor;
import org.grits.toolbox.entry.ms.extquantfiles.process.FullMzXMLDataProcessor;
import org.grits.toolbox.entry.ms.extquantfiles.process.FullMzXMLExternalQuantDataProcessor;
import org.grits.toolbox.entry.ms.extquantfiles.process.QuantFileProcessor;
import org.grits.toolbox.entry.ms.extquantfiles.process.StandardQuantDataProcessor;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecCustomAnnotation;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecStandardQuant;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecStandardQuantPeak;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.grits.toolbox.entry.ms.property.datamodel.ExternalQuantAlias;
import org.grits.toolbox.entry.ms.property.datamodel.ExternalQuantFileToAlias;
import org.grits.toolbox.entry.ms.property.datamodel.InternalStandardQuantFileList;
import org.grits.toolbox.entry.ms.property.datamodel.MSPropertyDataFile;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecMetaData;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecUISettings;
import org.grits.toolbox.ms.file.FileCategory;
import org.grits.toolbox.ms.file.FileReaderUtils;
import org.grits.toolbox.ms.file.MSFile;
import org.grits.toolbox.ms.file.extquant.data.ExternalQuantSettings;
import org.grits.toolbox.ms.file.extquant.data.QuantPeak;
import org.grits.toolbox.ms.file.extquant.data.QuantPeakMatch;
import org.grits.toolbox.ms.file.reader.IMSAnnotationFileReader;
import org.grits.toolbox.ms.file.reader.IMSExtQuantFileReader;
import org.grits.toolbox.ms.file.reader.IMSFileReader;
import org.grits.toolbox.ms.file.reader.impl.ExtractReader;
import org.grits.toolbox.ms.file.reader.impl.MSXMLReader;
//import org.grits.toolbox.ms.annotation.input.MzXmlReader;
import org.grits.toolbox.ms.om.data.Method;
import org.grits.toolbox.ms.om.data.Peak;
import org.grits.toolbox.ms.om.data.Scan;
import org.grits.toolbox.widgets.progress.IProgressListener.ProgressType;

/**
 * @author D Brent Weatherly (dbrentw@uga.edu)
 * Extends TableDataProcessor with specific options for displaying Mass Spec data
 */
public class MassSpecTableDataProcessor extends TableDataProcessor
{
	private static final Logger logger = Logger.getLogger(MassSpecTableDataProcessor.class);
	protected MassSpecTableDataObject simianTableDataObject = null;
	protected List<QuantFileProcessor> quantFileProcessors = null;
	protected List<QuantFileProcessor> quantFileProcessorsToRemove = null;
	protected int iMinMSLevel = 1;

	// "data" is generic so not tying it to a particular MS type
	protected Object data = null;
	protected FillTypes fillType = FillTypes.Scans;		    

	/**
	 * @param _entry - current MS Entry
	 * @param _sourceProperty - current MS property
	 * @param iMinMSLevel - min MS level of this MS run
	 */
	public MassSpecTableDataProcessor(Entry _entry, Property _sourceProperty, int iMinMSLevel ) {
		super(_entry, _sourceProperty);
		this.iMinMSLevel = iMinMSLevel;
	}

	/**
	 * @param _entry - current MS Entry
	 * @param _sourceProperty - current MS property
	 * @param fillType - FillType, options are "Scans" and "PeakList" and determine how to fill the GRITSTable 
	 * @param iMinMSLevel - min MS level of this MS run
	 */
	public MassSpecTableDataProcessor(Entry _entry, Property _sourceProperty, FillTypes fillType, int iMinMSLevel ) {
		this(_entry, _sourceProperty, iMinMSLevel);
		this.fillType = fillType;
	}

	/**
	 * @param _entry - current MS Entry
	 * @param _sourceProperty - current MS property
	 * @param fillType - FillType, options are "Scans" and "PeakList" and determine how to fill the GRITSTable 
	 */
	public MassSpecTableDataProcessor( TableDataProcessor _parent, Property _sourceProperty, FillTypes fillType)
	{
		super(_parent.getEntry(), _sourceProperty);
		this.data = ( (MassSpecTableDataProcessor) _parent).getData();
		this.quantFileProcessors = ( (MassSpecTableDataProcessor) _parent).getQuantFileProcessors();
		this.fillType = fillType;
		this.iMinMSLevel = ( (MassSpecTableDataProcessor) _parent).iMinMSLevel;
	}

	/**
	 * 
	 * @return the data file for this entry
	 */
	public MSPropertyDataFile getMSSourceFile() {
		if (entry == null)
			return null;

		return ((MassSpecEntityProperty)getEntry().getProperty()).getDataFile();
	}

	/**
	 * @return int, the minimum MS level from the MS file
	 */
	public int getMinMSLevel() {
		return iMinMSLevel;
	}

	/**
	 * @param iMinMSLevel, the minimum MS level from the MS file
	 */
	public void setMinMSLevel(int iMinMSLevel) {
		this.iMinMSLevel = iMinMSLevel;
	}

	/**
	 * @return Map<Integer,Scan>, casts the data object for specific use
	 */
	protected Map<Integer,Scan> getScanData() {
		return (Map<Integer,Scan>) this.data;
	}

	/**
	 * @return List<QuantFileProcessor>, if provided, the list of ExternalQuantFileProcessor for external quant
	 */
	public List<QuantFileProcessor> getQuantFileProcessors() {
		return this.quantFileProcessors;
	}

	/**
	 * @return Object - the generic data object
	 */
	public Object getData() {
		return data;
	}

	/**
	 * @param _preferences - current MassSpecViewerPreference object
	 * @param _processor - a CustomAnnotationDataProcessor that contains the CustomAnnotationData
	 * @return int - the number of columns added to the MassSpecViewerPreference object after adding custom annotations
	 * 
	 * Description: add the custom annotation headers to the GRITS table
	 */
	protected int addCustomAnnotationPeaks( MassSpecViewerPreference _preferences, CustomAnnotationDataProcessor _processor ) {
		int iColCnt = MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsCustomAnnotation(
				_preferences.getPreferenceSettings(),
				_preferences.getPreferenceSettings().getUnrecognizedHeaders(),
				getMassSpecEntityProperty().getMsLevel(), ((CustomAnnotationDataProcessor) _processor).getMassSpecCustomAnnotation());		

		return iColCnt;
	}

	/**
	 * @param _preferences - current MassSpecViewerPreference object
	 * @return - the number of columns added to the MassSpecViewerPreference object after adding unknown headers
	 * 
	 * Description: if the project contains column headers that aren't stored in preferences, handle them
	 */
	protected int addUnrecognizedHeaders( MassSpecViewerPreference _preferences ) {
		int iColCnt = 0;
		if( this.quantFileProcessors != null ) {
			for( QuantFileProcessor quantFileProcessor : this.quantFileProcessors ) {
				if( quantFileProcessor instanceof ExtractDataProcessor ) {
					iColCnt += MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsExternalQuantWCharge(
							_preferences.getPreferenceSettings(),
							_preferences.getPreferenceSettings().getUnrecognizedHeaders(), 
							QuantFileProcessor.getExternalQuantProcessorKey(quantFileProcessor.getKeyID(), ExtractDataProcessor.DEFAULT_KEY), 
							QuantFileProcessor.getExternalQuantProcessorLabel(quantFileProcessor.getLabelAlias(), ExtractDataProcessor.DEFAULT_LABEL));
				} else if( quantFileProcessor instanceof FullMzXMLDataProcessor ) {
					iColCnt += MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsExternalQuant(
							_preferences.getPreferenceSettings(), 
							_preferences.getPreferenceSettings().getUnrecognizedHeaders(), 
							QuantFileProcessor.getExternalQuantProcessorKey(quantFileProcessor.getKeyID(), FullMzXMLDataProcessor.DEFAULT_KEY), 
							QuantFileProcessor.getExternalQuantProcessorLabel(quantFileProcessor.getLabelAlias(), FullMzXMLDataProcessor.DEFAULT_LABEL));
				} else if( quantFileProcessor instanceof CustomAnnotationDataProcessor ) {
					iColCnt += addCustomAnnotationPeaks(_preferences, (CustomAnnotationDataProcessor) quantFileProcessor);
				}
			}
		}	
		return iColCnt;
	}

	/* (non-Javadoc)
	 * @see org.grits.toolbox.display.control.table.process.TableDataProcessor#getNewTableViewerPreferences()
	 * 
	 * Creates a new MassSpecViewerPreference object
	 */
	@Override
	protected TableViewerPreference getNewTableViewerPreferences() {
		return new MassSpecViewerPreference();
	}

	/* (non-Javadoc)
	 * @see org.grits.toolbox.display.control.table.process.TableDataProcessor#initializePreferences()
	 * 
	 * Instantiates and initializes a TableViewerPreference specific to MassSpec
	 */
	@Override
	public TableViewerPreference initializePreferences() {
		try {
			MassSpecViewerPreference newPreferences = (MassSpecViewerPreference) super.initializePreferences();
			newPreferences.setFillType(this.fillType);
			newPreferences.setMSLevel( getMassSpecEntityProperty().getMsLevel());
			return newPreferences;
		} catch( Exception e ) {
			logger.error("initializePreferences: unable to initialize preferences.", e);
		}
		return null;
	}

	/**
	 * @param _sourceProperty - current MS property
	 * 
	 * Description: Instantiates and initializes the MassSpecTableDataObject associated with this MassSpecTableDataProcessor.
	 */
	public void initializeTableDataObject( Property _sourceProperty) {
		setSimianTableDataObject(new MassSpecTableDataObject( ( (MassSpecEntityProperty) _sourceProperty).getMsLevel(), this.fillType));
		getSimianTableDataObject().initializePreferences();    	
		if( getSimianTableDataObject().getTablePreferences().settingsNeedInitialization() ) {
			TableViewerPreference tvp = initializePreferences();
			MassSpecTableDataProcessor.setDefaultColumnViewSettings(this.fillType, tvp.getPreferenceSettings());
			tvp.setColumnSettings(tvp.getPreferenceSettings().toString());
			getSimianTableDataObject().setTablePreferences(tvp);
			getSimianTableDataObject().getTablePreferences().writePreference();
		}       
	}

	/**
	 * @return MassSpecEntityProperty
	 */
	protected MassSpecEntityProperty getMassSpecEntityProperty() {
		return (MassSpecEntityProperty) this.sourceProperty;
	}

	/**
	 * @return MassSpecTableDataObject
	 */
	public MassSpecTableDataObject getSimianTableDataObject() {
		return simianTableDataObject;
	}

	/**
	 * @param simianTableDataObject - a MassSpecTableDataObject object
	 */
	public void setSimianTableDataObject(MassSpecTableDataObject simianTableDataObject) {
		this.simianTableDataObject = simianTableDataObject;
	}

	/**
	 * @param iMaxNumCols - int value for number of columns to initialize
	 * @return ArrayList<Object> - a blank row to be added to a MassSpecTableDataObject
	 */
	public static ArrayList<Object> getNewRow( int iMaxNumCols ) {
		ArrayList<Object> alRow = new ArrayList<Object>(iMaxNumCols);
		for( int i = 0; i < iMaxNumCols; i++ ) {
			alRow.add(null);
		}
		return alRow;
	}

	/* (non-Javadoc)
	 * @see org.grits.toolbox.display.control.table.process.TableDataProcessor#saveChanges()
	 * 
	 * We don't save changes for MS data. Just returns true
	 */
	@Override    
	public boolean saveChanges() throws Exception {
		return true;
	}

	// for debugging purposes!
	protected void showUnrecognizedCols() {
		TableViewerColumnSettings prefSettings = getSimianTableDataObject().getTablePreferences().getPreferenceSettings();
		for( GRITSColumnHeader tempHeader : getTempPreference().getPreferenceSettings().getHeaders() ) {
			GRITSColumnHeader prefHeader = prefSettings.getColumnHeader(tempHeader.getKeyValue());
			if( prefHeader == null ) {
				logger.warn("In new settings but not preferences: " + tempHeader);
			}
		}
		for( String sTempCol : getTempPreference().getPreferenceSettings().getUnrecognizedHeaders().keySet() ) {
			logger.warn("In new settings unrecognized headers: " + sTempCol);			
		}
		for( GRITSColumnHeader prefHeader : prefSettings.getHeaders() ) {
			GRITSColumnHeader tempHeader = getTempPreference().getPreferenceSettings().getColumnHeader(prefHeader.getKeyValue());
			if( tempHeader == null ) {
				logger.warn("In pref settings but not new: " + prefHeader);
			}
		}
		for( String sTempCol : prefSettings.getUnrecognizedHeaders().keySet() ) {
			logger.warn("In pref settings unrecognized headers: " + sTempCol);			
		}
	}

	/**
	 * Description: method that a page should call to initialize the ExternalQuantProcessors associated with this project.
	 */
	protected void loadExternalQuant() {
		try {
			this.progressBarDialog.getMinorProgressBarListener(0).setProgressType(ProgressType.Indeterminant);
			this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Loading external quant...");
			initializeQuantFiles();
			this.progressBarDialog.getMinorProgressBarListener(0).setProgressType(ProgressType.Determinant);
			this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Done...");		
		} catch( Exception e) {
			logger.error("loadExternalQuant: unable to load external quant.", e);
		}	
	}

	/**
	 * Description: Handles column headers that are associated with the project but aren't in the preferences.
	 */
	protected void addUnrecognizedHeaders() {
		try {
			this.progressBarDialog.getMinorProgressBarListener(0).setProgressType(ProgressType.Indeterminant);
			this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Adding unrecognized headers...");
			// we need to check to see if there were unrecognized (custom, probably) headers in preference file that we haven't handled yet
			if( getSimianTableDataObject().getTablePreferences().getPreferenceSettings().hasUnrecognizedHeaders() ) {
				int iNumCols = addUnrecognizedHeaders( (MassSpecViewerPreference) getSimianTableDataObject().getTablePreferences());
				setLastVisibleCol(getLastVisibleCol()+iNumCols);
			}
			this.progressBarDialog.getMinorProgressBarListener(0).setProgressType(ProgressType.Determinant);
			this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Done...");		
		} catch( Exception e) {
			logger.error("addUnrecognizedHeaders: unable to add unrecognized headers.", e);
		}			
	}

	/* (non-Javadoc)
	 * @see org.grits.toolbox.display.control.table.process.TableDataProcessor#createTable()
	 */
	@Override
	public boolean createTable() throws Exception {
		try {
			loadExternalQuant();
			addUnrecognizedHeaders();
			setTempPreference(initializePreferences());
			buildTable();
			quantFileProcessorsToRemove = null;
			if( bCancel ) {
				return true;
			}
			int iNumPref = getSimianTableDataObject().getTablePreferences().getPreferenceSettings().getNumColumns();
			int iNumNew = getTempPreference().getPreferenceSettings().getNumColumns();
			if( getSimianTableDataObject().getTablePreferences().settingsNeedInitialization() ) {
				showUnrecognizedCols();
				MassSpecTableDataProcessor.setDefaultColumnViewSettings(this.fillType, getTempPreference().getPreferenceSettings());
				getSimianTableDataObject().getTablePreferences().setPreferenceSettings(getTempPreference().getPreferenceSettings());
				getSimianTableDataObject().getTablePreferences().writePreference();        	
			}else if( iNumPref > iNumNew ) {
				logger.warn("Unrecognized headers in preferences. Saving them, but this is really an error.");
				getSimianTableDataObject().getTablePreferences().writePreference();        	
			} 
			getSimianTableDataObject().getTablePreferences().mergeSettings(getTempPreference().getPreferenceSettings());
			this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Done loading from XML..");
		} catch( Exception e) {
			logger.error("createTable: unable to read mzXML.", e);
			return false;
		}	
		return true;
	}

	/**
	 * @param tvs - a TableViewerColumnSettings object
	 * @param header - a GRITSColumnHeader object
	 * @param iPos - the integer position of this header in the TableViewerColumnSettings object
	 */
	public static void setDefaultColumnPosition( TableViewerColumnSettings tvs, GRITSColumnHeader header, int iPos ) {
		int iLastPos = 0;
		int iCurHeaderPos = tvs.getColumnPosition(header);
		for( GRITSColumnHeader curHeader : tvs.getHeaders() ) {
			int iVisInx = tvs.getColumnPosition(curHeader);
			if( iVisInx < 0 ) {
				continue; // hidden
			} 
			if ( iVisInx <= iPos && iVisInx > iCurHeaderPos ) {
				tvs.setVisColInx(curHeader, iVisInx-1);				
			} else if ( iVisInx >= iPos && iVisInx < iCurHeaderPos ) {
				tvs.setVisColInx(curHeader, iVisInx+1);				
			}
			if( iLastPos < iVisInx ) {
				iLastPos = iVisInx;
			}
		}
		if( iPos > iLastPos ) {
			tvs.setVisColInx(header, iLastPos);
		} else {
			tvs.setVisColInx(header, iPos);
		}

	}

	/**
	 * Description: sets the default order of columns for the MS table.
	 *
	 * @param fillType - the FillType of the current page
	 * @param tvs - a TableViewerColumnSettings object
	 */
	public static void setDefaultColumnViewSettings( FillTypes fillType, TableViewerColumnSettings tvs ) {
		if ( fillType == FillTypes.PeakList || fillType == FillTypes.PeaksWithFeatures) {
			GRITSColumnHeader header = tvs.getColumnHeader( DMPeak.peak_id.name());
			if( header != null ) {
				tvs.setVisColInx(header, -1);
			}	
		}
	}

	/* (non-Javadoc)
	 * @see org.grits.toolbox.display.control.table.process.TableDataProcessor#initializeColumnSettings()
	 */
	@Override
	protected TableViewerColumnSettings initializeColumnSettings() {
		try {
			int iNumCols = 0;
			TableViewerColumnSettings newSettings = getNewTableViewerSettings();
			boolean bHasSubScans = ! getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData().getMsExperimentType().equals(Method.MS_TYPE_MSPROFILE);
			boolean bHasScanNum = (getMassSpecEntityProperty().getMsLevel() == 1) || bHasSubScans;
			if( this.fillType == FillTypes.Scans ) {				
				iNumCols += MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsScan(newSettings, 
						getMassSpecEntityProperty().getMsLevel(),
						! bHasScanNum, bHasSubScans );
				iNumCols += MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsCorrectedPeakScanData( 
						newSettings, 
						getMassSpecEntityProperty() );
				if( this.quantFileProcessors != null ) {
					for( QuantFileProcessor quantFileProcessor : this.quantFileProcessors ) {
						if( quantFileProcessor instanceof ExtractDataProcessor ){
							iNumCols += MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsExternalQuantWCharge(newSettings, 
									getMassSpecEntityProperty().getMsLevel(),
									QuantFileProcessor.getExternalQuantProcessorKey(quantFileProcessor.getKeyID(), ExtractDataProcessor.DEFAULT_KEY), 
									QuantFileProcessor.getExternalQuantProcessorLabel(quantFileProcessor.getLabelAlias(), ExtractDataProcessor.DEFAULT_LABEL));
						} else if( quantFileProcessor instanceof FullMzXMLDataProcessor ) {
							iNumCols += MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsExternalQuant(newSettings, 
									getMassSpecEntityProperty().getMsLevel(),
									QuantFileProcessor.getExternalQuantProcessorKey(quantFileProcessor.getKeyID(), FullMzXMLDataProcessor.DEFAULT_KEY), 
									QuantFileProcessor.getExternalQuantProcessorLabel(quantFileProcessor.getLabelAlias(), FullMzXMLDataProcessor.DEFAULT_LABEL));
						} else if( quantFileProcessor instanceof CustomAnnotationDataProcessor ) {
							MassSpecCustomAnnotation msca = ((CustomAnnotationDataProcessor) quantFileProcessor).getMassSpecCustomAnnotation();
							iNumCols += MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsCustomAnnotation(newSettings, 
									getMassSpecEntityProperty().getMsLevel(), msca);
						} else if( quantFileProcessor instanceof StandardQuantDataProcessor ) {
							iNumCols += MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsStandardQuantitation(
									newSettings, (StandardQuantDataProcessor) quantFileProcessor);
						} else {
							throw new Exception("Unrecognized ExternalQuantFileProcessor: " + quantFileProcessor);
						}
					}
				}
			}
			else if ( this.fillType == FillTypes.PeakList ) {
				iNumCols += MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsPeakList(newSettings);
			}
			else if ( this.fillType == FillTypes.PeaksWithFeatures ) {
				iNumCols += MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsPeakWithFeatures(newSettings, ! bHasScanNum);
				iNumCols += MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsCorrectedPeakScanData( 
						newSettings, 
						getMassSpecEntityProperty() );
				if( this.quantFileProcessors != null ) {
					for( QuantFileProcessor quantFileProcessor : this.quantFileProcessors ) {
						if( quantFileProcessor instanceof ExtractDataProcessor ){
							iNumCols += MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsPeakWithFeaturesExternalQuantWCharge(newSettings, 
									QuantFileProcessor.getExternalQuantProcessorKey(quantFileProcessor.getKeyID(), ExtractDataProcessor.DEFAULT_KEY),
									QuantFileProcessor.getExternalQuantProcessorLabel(quantFileProcessor.getLabelAlias(), ExtractDataProcessor.DEFAULT_LABEL));
						} else if( quantFileProcessor instanceof FullMzXMLDataProcessor ) {
							iNumCols += MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsPeakWithFeaturesExternalQuant(newSettings, 
									QuantFileProcessor.getExternalQuantProcessorKey(quantFileProcessor.getKeyID(), FullMzXMLDataProcessor.DEFAULT_KEY),
									QuantFileProcessor.getExternalQuantProcessorLabel(quantFileProcessor.getLabelAlias(), FullMzXMLDataProcessor.DEFAULT_LABEL));
						} else if( quantFileProcessor instanceof CustomAnnotationDataProcessor ) {
							MassSpecCustomAnnotation msca = ((CustomAnnotationDataProcessor) quantFileProcessor).getMassSpecCustomAnnotation();
							iNumCols += MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsCustomAnnotation(newSettings, 
									getMassSpecEntityProperty().getMsLevel(), msca);
						} else if( quantFileProcessor instanceof StandardQuantDataProcessor ) {
							iNumCols += MassSpecTableDataProcessorUtil.fillMassSpecColumnSettingsStandardQuantitation(
									newSettings, (StandardQuantDataProcessor) quantFileProcessor);
						} else {
							throw new Exception("Unrecognized ExternalQuantFileProcessor: " + quantFileProcessor);
						}
					}
				}
			}
			setLastVisibleCol(iNumCols);
			return newSettings;
		} catch( Exception e ) {
			logger.error("initializeColumnSettings: unable to initialize all columns.", e);
		}
		return null;
	}

	/**
	 * @return boolean - whether or not external quant was loaded
	 * 
	 * Description: Iterates over the initialized ExternalQuantFileProcessors and calls the appropriate "loader" for each
	 */
	protected boolean processExternalQuant() {
		if( this.quantFileProcessors == null ) 
			return false;

		this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Loading external quantitation data...");
		boolean bLoaded = false;
		try {
			for( QuantFileProcessor quantFileProcessor : this.quantFileProcessors ) {
				if( quantFileProcessor instanceof CustomAnnotationDataProcessor ) {
					bLoaded |= loadSubScanExternalQuant(((QuantFileProcessor) quantFileProcessor));
				} else {
					int iParentScanNum = -1;
					if ( getMassSpecEntityProperty().getParentScanNum() != null ) { 
						iParentScanNum = getMassSpecEntityProperty().getParentScanNum();
					}
					if( iParentScanNum != -1 ) { // if TIM or DI and no MS1 scan in the MS file, parent scan == 0. But it should be 1 in the Full
						bLoaded |= loadPrecursorExternalQuantData(iParentScanNum, quantFileProcessor, false);
					}
				}
			}
		} catch( Exception e ) {
			logger.error("processExternalQuant: unable to initialize all columns.", e);
		}
		return bLoaded;
	}


	/**
	 * @param _processor - the current ExternalQuantFileProcessor
	 * @param msScan - the Scan to which External Quant is being added
	 * @return boolean - whether or not external quant was loaded
	 * 
	 * Description: this method loads external quant by reading the sub-scans of the specified msScan
	 */
	protected boolean loadSubScanExternalQuant(QuantFileProcessor _processor, Scan msScan) {
		boolean bUpdated = false;
		try {
			this.progressBarDialog.getMinorProgressBarListener(0).setMaxValue(getScanData().size() );
			int iScanNum = msScan.getScanNo();
			//			((SpecialPeaksDataProcessor)_processor).setPeakListFromMzList(((SpecialPeaksDataProcessor)_processor).getSpecialPeaksFile().getMzValues());
			((CustomAnnotationDataProcessor)_processor).setCurScan(msScan);
			_processor.loadExternalData();
			_processor.getQuantPeakData().setScanNo(iScanNum);
			_processor.getSettings().setTargetScanNumber(iScanNum);
			// NOTE: There was a previous implementation and code was commented out for a while.
			//       Could be at some point you might want to look at code before 02/01/16
			_processor.setSourcePeakList(getScanData());
			bUpdated = _processor.matchExternalPeaks(false);
			//			bUpdated = _processor.matchCorrectedPeaks(getScanData(), false);
		} catch( Exception e ) {
			logger.error("loadSubScanExternalQuant: unable to initialize all columns.", e);
		}
		return bUpdated;
	}

	/**
	 * @param _processor - the current QuantFileProcessor
	 * @return boolean - whether or not external quant was loaded
	 * 
	 * Description: iterates of the ScanData to identify Scans that should be processed for sub-scan external quant.
	 * Calls loadSubScanExternalQuant(QuantFileProcessor, Scan) for each Scan if it matches the required properties.
	 */
	protected boolean loadSubScanExternalQuant(QuantFileProcessor _processor) {
		this.progressBarDialog.getMinorProgressBarListener(0).setMaxValue(getScanData().size() );
		boolean bUpdated = false;
		try {
			for( Integer iScan : getScanData().keySet() ) {
				Scan msScan = getScanData().get(iScan);
				if ( getMassSpecEntityProperty().getMsLevel() != null && 
						! msScan.getMsLevel().equals( getMassSpecEntityProperty().getMsLevel() ) && 
						! msScan.getMsLevel().equals( (getMassSpecEntityProperty().getMsLevel()-1) ) ) {
					continue;
				}
				if ( getMassSpecEntityProperty().getScanNum() !=  null &&
						getMassSpecEntityProperty().getScanNum() != -1 && 
						! msScan.getScanNo().equals(getMassSpecEntityProperty().getScanNum()) ) {
					continue;
				}
				if ( getMassSpecEntityProperty().getParentScanNum() !=  null &&
						getMassSpecEntityProperty().getParentScanNum() != -1 && 
						( msScan.getParentScan() == null || 
						! getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData().getMsExperimentType().equals(Method.MS_TYPE_INFUSION) &&
						( ! msScan.getParentScan().equals(getMassSpecEntityProperty().getParentScanNum()) &&
								! msScan.getScanNo().equals(getMassSpecEntityProperty().getParentScanNum())) ) ) {
					continue;
				}
				bUpdated |= loadSubScanExternalQuant(_processor, msScan);
				//				_processor.getExternalPeaks();
				//			this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Building scans table. Scan: " + iCnt + " of " + getScanData().size());
				//			this.progressBarDialog.getMinorProgressBarListener(0).setProgressValue(iCnt++);
				if ( bCancel ) {
					return false;
				}
			}    
		} catch( Exception e ) {
			logger.error("loadSubScanExternalQuant: unable to initialize all columns.", e);
		}
		return bUpdated;
	}

	/**
	 * @throws Exception
	 * 
	 * Description: called by createTable to build the GRITS table w/ headers and rows
	 * Based on FillType, add scans or peaks 
	 */
	public void buildTable() throws Exception {
		processExternalQuant();

		this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Building table...");
		int iMax = getScanData().size();
		//		this.progressBarDialog.startMinorThread();
		this.progressBarDialog.getMinorProgressBarListener(0).setMaxValue( iMax );
		ArrayList<GRITSColumnHeader> alHeader = new ArrayList<GRITSColumnHeader>();
		try {
			addHeaderLine(alHeader);
		} catch (Exception e) {
			throw new Exception("Unable to build table", e);
		}
		this.getSimianTableDataObject().getTableHeader().add(alHeader);
		if( this.fillType == FillTypes.Scans ) {
			addScansTableData();
		} else {
			if( getMassSpecEntityProperty().getPeakListNumber() == 1 ) {
				addPeaksTableData();
			} else {
				// the peak number is offset by 1 to match the quant processor number desired
				addAdditionalPeaksTableData(getMassSpecEntityProperty().getPeakListNumber() - 1);
			}
		}
		if( getSimianTableDataObject().getTableData().isEmpty() ) {
			// adding 2 blank rows to the subset table		
			getSimianTableDataObject().getTableData().add( TableDataProcessor.getNewRow( getSimianTableDataObject().getLastHeader().size(), getSimianTableDataObject().getTableData().size() ) );
			getSimianTableDataObject().getTableData().add( TableDataProcessor.getNewRow( getSimianTableDataObject().getLastHeader().size(), getSimianTableDataObject().getTableData().size() ) );			
		}
	}


	/**
	 * @param alHeader - ArrayList<GRITSColumnHeader> to become the NatTable header 
	 * @throws Exception
	 */
	protected void addHeaderLine( ArrayList<GRITSColumnHeader> alHeader ) throws Exception {
		for( GRITSColumnHeader colHeader : getTempPreference().getPreferenceSettings().keySet() ) {
			int iPrefColNum = getTempPreference().getPreferenceSettings().getColumnPosition(colHeader);
			addHeaderLine(iPrefColNum, colHeader, alHeader);
		}
	}

	/**
	 * @param iPrefColNum - int value of the column number in the header
	 * @param colHeader - current GRITSColumnHeader being added
	 * @param alHeader - ArrayList<GRITSColumnHeader> to become the NatTable header 
	 * 
	 * Description: Looks at the GRITSColumnHeader to see if we need to associate the column number with known Scan data
	 * that will be used elsewhere (e.g. scan number column)
	 */
	protected void addHeaderLine( int iPrefColNum, GRITSColumnHeader colHeader, ArrayList<GRITSColumnHeader> alHeader ) {   	
		if ( colHeader.getKeyValue().equals( DMScan.scan_scanNo.name()) || colHeader.getKeyValue().equals( DMScan.scan_pseudoScanNo.name())  ) {
			this.getSimianTableDataObject().addScanNoCol(iPrefColNum);
		} else if ( colHeader.getKeyValue().equals( DMScan.scan_parentScan.name() ) ) {
			this.getSimianTableDataObject().addParentNoCol(iPrefColNum);
		} else if ( colHeader.getKeyValue().equals( DMPeak.peak_id.name() ) ) {
			this.getSimianTableDataObject().addPeakIdCol(iPrefColNum);
		} else if ( colHeader.getKeyValue().equals( DMPeak.peak_mz.name() ) ) {
			this.getSimianTableDataObject().addMzCol(iPrefColNum);
		} else if ( colHeader.getKeyValue().equals( DMPeak.peak_intensity.name() ) ) {
			this.getSimianTableDataObject().addPeakIntensityCol(iPrefColNum);
		} else if ( colHeader.getKeyValue().equals( DMPeak.peak_is_precursor.name() ) ) {
			this.getSimianTableDataObject().addPeakIsPrecursorCol(iPrefColNum);
		} else if ( colHeader.getKeyValue().equals( DMPrecursorPeak.precursor_peak_intensity.name()) ) {
			this.getSimianTableDataObject().addPrecursorIntensityCol(iPrefColNum);
		}
		MassSpecTableDataProcessorUtil.setHeaderValue(iPrefColNum, colHeader, alHeader);
	}

	protected InternalStandardQuantFileList getInternalStandardQuantFileList(MSPropertyDataFile quantFile) {
		MassSpecUISettings uiSettings = getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData();
		String sExtQuantType = MassSpecUISettings.getExternalQuantType(quantFile);
		if( uiSettings.getInternalStandardQuantFiles() == null || uiSettings.getInternalStandardQuantFiles().isEmpty() ) {
			return null;
		}

		InternalStandardQuantFileList isqfl = uiSettings.getInternalStandardQuantFiles().get(sExtQuantType);
		return isqfl;		
	}

	/**
	 * @param quantFile, a MSPropertyDataFile representing an external quantification file
	 * @return the alias that the user specified for the external quantification file
	 */
	protected ExternalQuantAlias getExternalQuantAliasForInternalQuant(String sQuantSetName, MSPropertyDataFile quantFile) {
		InternalStandardQuantFileList isqfl = getInternalStandardQuantFileList(quantFile);
		if( isqfl == null ) {
			return null;
		}

		ExternalQuantFileToAlias mAliases = isqfl.getExternalQuantToAliasByQuantType(sQuantSetName);
		if( mAliases == null ) {
			return null;
		}
		ExternalQuantAlias foundAlias = null;
		if( mAliases != null ) {
			for( String sFileName : mAliases.getSourceDataFileNameToAlias().keySet() ) {
				ExternalQuantAlias aliasInfo = mAliases.getSourceDataFileNameToAlias().get(sFileName);
				if( aliasInfo == null ) {
					continue; // not part of this analysis
				}
				if( sFileName.equals(quantFile.getName()) ) {
					foundAlias = aliasInfo;
				}
			}
		}
		return foundAlias;
	}


	/**
	 * @param quantFile, a MSPropertyDataFile representing an external quantification file
	 * @return the alias that the user specified for the external quantification file
	 */
	protected ExternalQuantAlias getExternalQuantAlias(MSPropertyDataFile quantFile) {
		MassSpecUISettings uiSettings = getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData();
		String sExtQuantType = MassSpecUISettings.getExternalQuantType(quantFile);

		ExternalQuantFileToAlias mAliases = uiSettings.getExternalQuantToAliasByQuantType(sExtQuantType);
		//		String sAlias = MSPropertyDataFile.getFormattedName(quantFile);;
		ExternalQuantAlias foundAlias = null;
		if( mAliases != null ) {
			int iCnt = 0;
			for( String sFileName : mAliases.getSourceDataFileNameToAlias().keySet() ) {
				ExternalQuantAlias aliasInfo = mAliases.getSourceDataFileNameToAlias().get(sFileName);
				if( aliasInfo == null ) {
					continue; // not part of this analysis
				}
				if( sFileName.equals(quantFile.getName()) ) {
					foundAlias = aliasInfo;
				}
			}
		}
		return foundAlias;
	}

	private QuantFileProcessor getNewQuantProcessor( MSPropertyDataFile quantFile, ExternalQuantAlias aliasInfo ) {
		String sExpType = getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData().getMsExperimentType();
		String sMSPath = getMSPath();
		MSFile msFile = quantFile.getMSFileWithReader(sMSPath, sExpType);
		IMSFileReader reader = msFile.getReader();
		if (reader == null || !(reader instanceof IMSExtQuantFileReader)) {
			logger.error("Null or invalid external quantreader for msFile: " + msFile.getFileName());
			return null;
		}

		QuantFileProcessor processor = null;
		if( reader instanceof MSXMLReader ) {
			ExternalQuantSettings parameter = new ExternalQuantSettings(msFile, true, 500.0d);
			processor = new FullMzXMLExternalQuantDataProcessor(parameter);
		} else if ( reader instanceof ExtractReader ) {
			// Xtract
			ExternalQuantSettings parameter = new ExternalQuantSettings(msFile, true, 500.0d);
			processor = new ExtractExternalQuantDataProcessor(parameter);
		} else {
			logger.error("Unsupported external quant reader for file: " + msFile.getFileName());
			return null;
		}
		processor.setKeyID(aliasInfo.getKey());
		processor.setLabelAlias(aliasInfo.getAlias());
		return processor;
	}

	/**
	 * Sets the list of QuantFileProcessors 
	 * 
	 * @param lQuantFiles
	 */
	protected void setQuantFiles( List<QuantFileProcessor> lQuantFiles ) {
		if( this.quantFileProcessors == null ) {
			this.quantFileProcessors = new ArrayList<QuantFileProcessor>();
		} else {
			this.quantFileProcessors.clear();
		}
		for( QuantFileProcessor newQuantFile : lQuantFiles ) {
			this.quantFileProcessors.add(newQuantFile);
		}		

	}

	protected List<MassSpecCustomAnnotation> getCustomAnnotationList() {
		if( getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData() != null && 
				getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData().getCustomAnnotations() != null &&
				! getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData().getCustomAnnotations().isEmpty() ) {
			MassSpecMetaData msSettings = getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData();
			return msSettings.getCustomAnnotations();
		}
		return null;
	}

	protected CustomAnnotationDataProcessor getNewCustomAnnotationProcessor(ExternalQuantSettings quantSettings) {		
		CustomAnnotationDataProcessor spdf = new CustomAnnotationDataProcessor(quantSettings);	
		return spdf;
	}

	private List<Peak> getPeakListForInternalStandardQuant(MassSpecStandardQuant mssq) {
		if( mssq == null || mssq == null ) {
			return null;
		}
		int iMSLevel = getMassSpecEntityProperty().getMsLevel() - 1;
		List<Peak> peakList = new ArrayList<>();
		Collection<MassSpecStandardQuantPeak> quantPeaks = mssq.getStandardQuantPeaks().values();
		Iterator<MassSpecStandardQuantPeak> itr = quantPeaks.iterator();
		while( itr.hasNext() ) {
			MassSpecStandardQuantPeak customPeak = itr.next();
			if( customPeak.getMSLevel() == iMSLevel ) {
				Peak p = new Peak();
				p.setMz( customPeak.getPeakMz() );
				p.setIntensity(1.0d);
				p.setIsPrecursor(true);
				peakList.add(p);
			}			
		}
		return peakList;						
	}

	protected List<MassSpecStandardQuant> getStandardQuantitationList() {
		if( getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData() != null && 
				getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData().getStandardQuant() != null &&
				! getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData().getStandardQuant().isEmpty() ) {
			MassSpecMetaData msSettings = getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData();
			return msSettings.getStandardQuant();
		}
		return null;
	}

	protected HashMap<String, InternalStandardQuantFileList> getStandardQuantitationFileList() {
		if( getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData() != null && 
				getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData().getInternalStandardQuantFiles() != null &&
				! getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData().getInternalStandardQuantFiles().isEmpty() ) {
			MassSpecMetaData msSettings = getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData();
			return msSettings.getInternalStandardQuantFiles();
		}
		return null;
	}

	/**
	 * Initializes the list of quant file processors
	 */
	protected void initializeQuantFiles() {
		List<QuantFileProcessor> loadedProcessors = new ArrayList<QuantFileProcessor>();
		/* Look for External Quant files */
		List<MSPropertyDataFile> quantMSPDFs = getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData().getQuantificationFiles();
		List<MSPropertyDataFile> annotMSPDFs = getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData().getAnnotationFiles();
		for( MSPropertyDataFile mspdf : quantMSPDFs ) {
			ExternalQuantAlias aliasInfo = getExternalQuantAlias(mspdf);
			if( aliasInfo == null ) { // not included in the list
				continue;
			}
			try {
				this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Reading peak file...");
				QuantFileProcessor quantFileProcessor = getNewQuantProcessor(mspdf, aliasInfo);
				if( quantFileProcessor != null ) {
					loadedProcessors.add(quantFileProcessor);
				} 
			} catch( Exception e ) {
				logger.error("initializeExternalQuantProcessors: unable to add external quant file file.", e);
			}			
		}

		List<MassSpecCustomAnnotation> customAnnotations = getCustomAnnotationList();
		if( customAnnotations != null && ! customAnnotations.isEmpty() ) {
			this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Reading special peak file...");
			for( MassSpecCustomAnnotation annotation : customAnnotations ) {
				// testing...there is no reader for the CustomAnnotation, so to speak. I'm using it because it makes it easier, but there
				// is no file to read because the info is read from preferences or the meta data
				ExternalQuantSettings quantSettings = new ExternalQuantSettings(null, 
						annotation.getIsPPM(), annotation.getMassTolerance());
				CustomAnnotationDataProcessor spdf = new CustomAnnotationDataProcessor(quantSettings);	
				spdf.setMassSpecCustomAnnotation(annotation);
				spdf.setCustomAnnotationPeakList();
				loadedProcessors.add(spdf);
			}			
		}

		List<MassSpecStandardQuant> internalStdQuantList = getStandardQuantitationList();
		HashMap<String, InternalStandardQuantFileList> internalStdQuantFileList = getStandardQuantitationFileList();
		if( internalStdQuantList != null && ! internalStdQuantList.isEmpty() && internalStdQuantFileList != null && ! internalStdQuantFileList.isEmpty()) {
			this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Processing internal standard quant...");
			for( String sQuantType : internalStdQuantFileList.keySet() ) {
				InternalStandardQuantFileList fileList = internalStdQuantFileList.get(sQuantType);
				if( fileList != null && fileList.getQuantNameToFileAlias() != null && ! fileList.getQuantNameToFileAlias().isEmpty() ) {
					for( MassSpecStandardQuant mssq : internalStdQuantList ) {
						//					for( String sQuantName : fileList.getQuantNameToFileAlias().keySet() ) {
						List<Peak> peakList = getPeakListForInternalStandardQuant(mssq);
						ExternalQuantFileToAlias eqfa = fileList.getQuantNameToFileAlias().get(mssq.getStandardQuantName());
						if( eqfa == null ) {
							continue;
						}
						for( String sFilePath : eqfa.getSourceDataFileNameToAlias().keySet() ) {
							boolean bMatched = false;
							for( MSPropertyDataFile quantFile : quantMSPDFs ) {
								if( quantFile.getName().equals(sFilePath) ) {
									this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Reading peak file...");
									ExternalQuantAlias aliasInfo = getExternalQuantAliasForInternalQuant(mssq.getStandardQuantName(), quantFile);
									QuantFileProcessor externalProcessor = getNewQuantProcessor(quantFile, aliasInfo);
									if( externalProcessor != null ) {
										QuantFileProcessor standardProcessor = new StandardQuantDataProcessor(externalProcessor);						
										standardProcessor.setLabelAlias(mssq.getStandardQuantName() + " : " + externalProcessor.getLabelAlias());
										standardProcessor.setKeyID(mssq.getStandardQuantName() + ":" + externalProcessor.getKeyID());
										standardProcessor.setSourcePeakListToMatch(peakList);
										loadedProcessors.add(standardProcessor);
										bMatched = true;
									} 
								}
							}
							if( ! bMatched ) {
								for( MSPropertyDataFile quantFile : annotMSPDFs ) {									
									if( quantFile.getName().equals(sFilePath) ) {
										// we have to force the category to be external quant to ensure we get the correct reader (hacky)
										MSPropertyDataFile extQuantFile = (MSPropertyDataFile) quantFile.clone();
										extQuantFile.setCategory(FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY);
										this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Reading peak file...");
										ExternalQuantAlias aliasInfo = getExternalQuantAliasForInternalQuant(mssq.getStandardQuantName(), quantFile);
										QuantFileProcessor externalProcessor = getNewQuantProcessor(extQuantFile, aliasInfo);
										if( externalProcessor != null ) {
											QuantFileProcessor standardProcessor = new StandardQuantDataProcessor(externalProcessor);						
											standardProcessor.setLabelAlias(mssq.getStandardQuantName() + " : " + externalProcessor.getLabelAlias());
											standardProcessor.setKeyID(mssq.getStandardQuantName() + ":" + externalProcessor.getKeyID());
											standardProcessor.setSourcePeakListToMatch(peakList);
											loadedProcessors.add(standardProcessor);
											bMatched = true;
										} 
									}								
								}
							}
						}
					}
				}
			}
		}
		setQuantFiles(loadedProcessors);
	}

	/**
	 * @return int[] - array of ints containing the MS level, parent scan number, and scan number from the Entry's property
	 */
	protected int[] getMSReadParameters() {
		int iMSLevel = -1;
		int iParentScanNum = -1;
		int iScanNum = -1;
		if ( getMassSpecEntityProperty().getMsLevel() != null && this.fillType != FillTypes.PeakList ) {
			iMSLevel = getMassSpecEntityProperty().getMsLevel();
		}
		// modified on 03/22/17 for the Maor project. This was preventing filling of the peak list. I ohpe this doesn't create a bug elsewhere!
		//		if ( getMassSpecEntityProperty().getParentScanNum() != null && this.fillType != FillTypes.PeakList ) { 
		if ( getMassSpecEntityProperty().getParentScanNum() != null ) { 
			iParentScanNum = getMassSpecEntityProperty().getParentScanNum();
		}
		if ( getMassSpecEntityProperty().getScanNum() !=  null ) { 
			iScanNum = getMassSpecEntityProperty().getScanNum();
		}
		return new int[] {iMSLevel, iParentScanNum, iScanNum};
	}


	/* (non-Javadoc)
	 * @see org.grits.toolbox.display.control.table.process.TableDataProcessor#readDataFromFile()
	 */
	@Override
	public boolean readDataFromFile() {
		int[] iParams = getMSReadParameters();

		final int iMSLevel = iParams[0];
		final int iParentScanNum = iParams[1];
		final int iScanNum = iParams[2];
		try {
			MSPropertyDataFile msSourceFile = getMSSourceFile();
			if (msSourceFile == null)
				return false;
			Entry msEntry = MassSpecProperty.getMSParentEntry(getEntry());
			MassSpecProperty msProp = (MassSpecProperty) msEntry.getProperty();

			// get the reader for display purposes only 
			// to be able to read FullMS file with MzXMLReader instead of as an external quantification file
			MSFile msFile = msSourceFile.getMSFileWithReader(getMSPath(), msProp.getMassSpecMetaData().getMsExperimentType(), true);
			IMSFileReader reader = msFile.getReader();
			if (reader == null || !(reader instanceof IMSAnnotationFileReader))
				return false;
			int iMax = ((IMSAnnotationFileReader)reader).getMaxScanNumber(msFile);
			this.progressBarDialog.getMinorProgressBarListener(0).setMaxValue(iMax);
			this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Reading data file...");
			//mzXMLFileReader = new MzXmlReader();		
			//mzXMLFileReader.addProgressListeners(progressBarDialog.getMinorProgressBarListener(0));
			reader.addProgressListeners(progressBarDialog.getMinorProgressBarListener(0));
			getlLongRunningProcesses().add(reader);
			//String sSourceFile = getMSSourceFile();
			//List<Scan> scans = mzXMLFileReader.readMzXmlFile(sSourceFile, iMSLevel, iParentScanNum, iScanNum);
			List<Scan> scans = ((IMSAnnotationFileReader)reader).readMSFile(msFile, iMSLevel, iParentScanNum, iScanNum);
			if( scans == null || scans.isEmpty() ) {
				logger.error("No scan data returned from mzXML.");
			}
			data = FileReaderUtils.listToHashMap(scans);				
			getlLongRunningProcesses().remove(reader);
			this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Done...");
		} catch( Exception e ) {
			logger.error("readDataFromFile: unable to read mzXML.", e);
			return false;
		}
		return true;
	}

	/**
	 * @return the path for the MS files for the MS entry
	 */
	public String getMSPath() {
		Entry msEntry = MassSpecProperty.getMSParentEntry(getEntry());
		MassSpecProperty msProp = (MassSpecProperty) msEntry.getProperty();
		String folderName = msProp.getFullyQualifiedFolderName(msEntry);
		return folderName;
	}

	/**
	 * @param _iParentScanNum - Scan number of scan being annotated with external quant
	 * @param _iExternalScanNum - Scan number of external MS file to read external quant
	 * 
	 * Description: For Direct Infusion experiments, reads the mzXML file for the external quant and adds 
	 * it to the current Scan object by parent scan number
	 */
	protected void loadExternalQuantDataForDirectInfusion(int _iParentScanNum, int _iExternalScanNum) {
		try {
			Scan scan = getScanData().get(_iParentScanNum);
			if( scan == null || scan.getPeaklist().isEmpty() ) {
				MSPropertyDataFile msSourceFile = getMSSourceFile();
				if (msSourceFile == null)
					return;
				Entry msEntry = MassSpecProperty.getMSParentEntry(getEntry());
				MassSpecProperty msProp = (MassSpecProperty) msEntry.getProperty();
				String folderName = msProp.getFullyQualifiedFolderName(msEntry);

				MSFile msFile = msSourceFile.getMSFileWithReader(folderName, msProp.getMassSpecMetaData().getMsExperimentType());
				IMSFileReader reader = msFile.getReader();
				if (reader == null || !(reader instanceof IMSAnnotationFileReader))
					return;
				//String sSourceFile = getMSSourceFile();
				//this.mzXMLFileReader = new MzXmlReader();
				//mzXMLFileReader.addProgressListeners(progressBarDialog.getMinorProgressBarListener(0));
				reader.addProgressListeners(progressBarDialog.getMinorProgressBarListener(0));
				getlLongRunningProcesses().add(reader);
				int iMSLevel = getScanData().get(_iExternalScanNum).getMsLevel();
				List<Scan> scans = ((IMSAnnotationFileReader) reader).readMSFile(msFile, iMSLevel, -1, _iParentScanNum);
				Map<Integer,Scan> tmpData = FileReaderUtils.listToHashMap(scans);
				//				Map<Integer,Scan> tmpData = this.mzXMLFileReader.readMzXmlFile(sSourceFile, iMSLevel, -1, _iParentScanNum);
				getScanData().put(_iParentScanNum, tmpData.get(_iParentScanNum));
				getlLongRunningProcesses().remove(reader);
			}
			getScanData().put(_iExternalScanNum, getScanData().get(_iParentScanNum));

		} catch( Exception e ) {
			logger.error("loadExternalQuantDataForDirectInfusion: error loading the scan data necessary for external quant.", e);
		}
	}

	/**
	 * @param _iParentScanNum - Scan number of scan being annotated with external quant
	 * @param _quantFileProcessor - the QuantFileProcessor to be used to add external quant to the parent scan
	 * @return boolean - whether or not external quant was loaded
	 * 
	 * Description: Processes precursor external quant for the specified parent scan
	 */
	protected boolean loadPrecursorExternalQuantData(int _iParentScanNum, QuantFileProcessor _quantFileProcessor, boolean _bRemove) {
		if( this.fillType == FillTypes.PeakList )
			return false;
		boolean bUpdated = false;
		QuantFileProcessor subProc = null;
		try {
			loadExternalQuantDataForDirectInfusion( _iParentScanNum, _iParentScanNum );
			if( _quantFileProcessor.getQuantPeakData() == null ||_quantFileProcessor.getQuantPeakData().getPeaks().isEmpty() ) {					
				if( _quantFileProcessor instanceof FullMzXMLDataProcessor ) {			
					((FullMzXMLDataProcessor)_quantFileProcessor).setPrecursorPeaks(getScanData().get(_iParentScanNum).getPeaklist());
				} else if ( _quantFileProcessor instanceof StandardQuantDataProcessor ) {
					subProc = ((StandardQuantDataProcessor) _quantFileProcessor).getQuantFileProcessor();
					if( subProc instanceof FullMzXMLDataProcessor ) {
						((FullMzXMLDataProcessor)subProc).setPrecursorPeaks(getScanData().get(_iParentScanNum).getPeaklist());
						// The Full MS file will be filtered based on the observed precursors in the annotation file.
						// Add the standard peaks to the precursor list just in case the peak wasn't observed in the annotation
						((FullMzXMLDataProcessor)subProc).getPrecursorPeaks().addAll(_quantFileProcessor.getSourcePeakListToMatch());
					}
				}
				_quantFileProcessor.loadExternalData();
			}
			_quantFileProcessor.getQuantPeakData().setScanNo(_iParentScanNum);
			_quantFileProcessor.getSettings().setTargetScanNumber(_iParentScanNum);
			// NOTE: There was a previous implementation and code was commented out for a while.
			//       Could be at some point you might want to look at code before 02/01/16

			if( _quantFileProcessor.getSourcePeakListToMatch() == null || _quantFileProcessor.getSourcePeakListToMatch().isEmpty() ) {
				_quantFileProcessor.setSourcePeakList(getScanData());
			}
			if( subProc != null && (subProc.getSourcePeakListToMatch() == null || subProc.getSourcePeakListToMatch().isEmpty()) ) {
				_quantFileProcessor.setQuantPeakData(subProc.getQuantPeakData());
				_quantFileProcessor.getSettings().setTargetScanNumber(subProc.getSettings().getTargetScanNumber());;
				subProc.setSourcePeakList(getScanData());
			}
			if( _bRemove ) {
				bUpdated |= _quantFileProcessor.removeExternalPeakMatches(true);
				//				bUpdated |= _processor.removeCorrectedPeaks(getScanData(), true);
			} else {	
				bUpdated |= _quantFileProcessor.matchExternalPeaks(true);
			}

		} catch( Exception e ) {
			logger.error("loadExtractData: unable to read extract file.", e);
		}

		return bUpdated;
	}


	/**
	 * @param msScan - the Scan object to be added to the table.
	 */
	protected void addScanRow( Scan msScan ) {
		if ( getMassSpecEntityProperty().getMsLevel() != null && 
				! msScan.getMsLevel().equals( getMassSpecEntityProperty().getMsLevel() )) {
			return;
		} else if ( getMassSpecEntityProperty().getParentScanNum() != null &&
				getMassSpecEntityProperty().getParentScanNum() != -1 && 
				! msScan.getParentScan().equals(getMassSpecEntityProperty().getParentScanNum()) ) {
			return;
		} else if ( getMassSpecEntityProperty().getScanNum() !=  null &&
				getMassSpecEntityProperty().getScanNum() != -1 && 
				! msScan.getScanNo().equals(getMassSpecEntityProperty().getScanNum()) ) {
			return;
		}
		GRITSListDataRow alRow = getNewRow();
		addScanData(msScan, getTempPreference().getPreferenceSettings(), alRow);
		getSimianTableDataObject().getTableData().add(alRow);				    	
	}	

	/**
	 * Top-level method called to build a Scans FillType table
	 */
	protected void addScansTableData() {
		int iCnt = 1;
		this.progressBarDialog.getMinorProgressBarListener(0).setMaxValue(getScanData().size() );
		for( Integer iScan : getScanData().keySet() ) {
			Scan msScan = getScanData().get(iScan);
			if( (iCnt%100) == 0 ) {
				this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Building scans table. Scan: " + iCnt + " of " + getScanData().size());
				this.progressBarDialog.getMinorProgressBarListener(0).setProgressValue(iCnt);
			}
			iCnt++;
			addScanRow(msScan);
			if ( bCancel ) {
				setSimianTableDataObject(null);
				return;
			}
		}    	
		this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Done.");
		this.progressBarDialog.getMinorProgressBarListener(0).setProgressValue(getScanData().size());
	}

	/**
	 * Top-level method called to build a Peaks FillType table
	 */
	protected void addPeaksTableData() {
		int iCnt = 1;
		for( Integer iScan : getScanData().keySet() ) {
			Scan msScan = getScanData().get(iScan);
			if( (iCnt%100) == 0 ) {
				this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Building peaks table. Scan: " + iCnt + " of " + getScanData().size());
				this.progressBarDialog.getMinorProgressBarListener(0).setProgressValue(iCnt);
			}
			addPeakData(msScan, getTempPreference().getPreferenceSettings());
			if ( bCancel ) {
				setSimianTableDataObject(null);
				return;
			}
		}    	
		this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Done.");
		this.progressBarDialog.getMinorProgressBarListener(0).setProgressValue(getScanData().size());
	}

	/**
	 * @param _iQuantFileNum - the index of the QuantFileProcessor
	 * 
	 * ** eventually allow multiple peak lists added **
	 */
	protected void addAdditionalPeaksTableData( int _iQuantFileNum ) {
		try {
			if( this.quantFileProcessors == null || _iQuantFileNum > (this.quantFileProcessors.size() - 1) )
				return;
			int iCnt = 1;
			this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Scan: 1");
			QuantFileProcessor processor = this.quantFileProcessors.get(_iQuantFileNum);
			for( QuantPeak extPeak : processor.getQuantPeakData().getPeaks() ) {
				for( QuantPeakMatch match : extPeak.getMatch() ) {
					Peak peak = new Peak();
					peak.setMz( match.getMzMostAbundant() );
					peak.setIntensity( match.getIntensitySum() );
					peak.setCharge( match.getCharge() );
					this.progressBarDialog.getMinorProgressBarListener(0).setProgressMessage("Scan: "+ iCnt++);
					GRITSListDataRow alRow = getNewRow();
					addPeaksData(null, null, peak, getTempPreference().getPreferenceSettings(), alRow);
					getSimianTableDataObject().getTableData().add(alRow);
				}
			}

		} catch( Exception e ) {
			logger.error("addAdditionalPeaksTableData: error adding external quant to table model.", e);
		}

	}

	/**
	 * @param msScan - the Scan containing the peak list that should be added to table
	 * @param _settings - the TableViewerColumnSettings containing the column positions 
	 */
	protected void addPeakData( Scan msScan, TableViewerColumnSettings _settings) {		
		if ( this.fillType != FillTypes.PeakList &&
				getMassSpecEntityProperty().getMsLevel() != null && 
				getMassSpecEntityProperty().getMsLevel() != -1 &&
				msScan.getMsLevel() != null &&
				! msScan.getMsLevel().equals(getMassSpecEntityProperty().getMsLevel()) ) {
			return;
		} else if ( getMassSpecEntityProperty().getParentScanNum() != null &&
				getMassSpecEntityProperty().getParentScanNum() != -1 && 
				msScan.getParentScan() != null &&
				! msScan.getParentScan().equals(getMassSpecEntityProperty().getParentScanNum() )) {
			return;
		} else if ( getMassSpecEntityProperty().getScanNum() !=  null &&
				getMassSpecEntityProperty().getScanNum() != -1 && 
				msScan.getScanNo() != null && 
				! msScan.getScanNo().equals(getMassSpecEntityProperty().getScanNum()) ) {
			return;
		}
		for (Peak peak : msScan.getPeaklist() ) {
			if( peak == null) {
				continue;
			}
			GRITSListDataRow alRow = getNewRow();
			addPeaksData(msScan.getScanNo(), null, peak, _settings, alRow);
			getSimianTableDataObject().getTableData().add(alRow);
		}
	}

	/**
	 * If external quant or standard quant is modified, remove data from peak hashmaps that is no longer userd.
	 * 
	 * @param _peak, current Peak
	 * @param quantFileProcessor, current QuantFileProcessor
	 * @return true if data was removed, false otherwise
	 */
	protected boolean removeExternalQuantPeaksData( Peak _peak, QuantFileProcessor quantFileProcessor ) {
		boolean bRemoveQuant = false;
		try {
			if( ! quantFileProcessor.getSettings().getQuantifyPrecursor() ) {
				return bRemoveQuant;
			}
			if( _peak.getIntegerProp() != null && ! _peak.getIntegerProp().isEmpty()) {
				List<String> sKeysToRemove = new ArrayList<>();
				for( String sKey : _peak.getIntegerProp().keySet() ) {
					if( quantFileProcessor.matchesExternalQuantKey(sKey) ) {
						sKeysToRemove.add(sKey);
					}
				}
				for( String sKey : sKeysToRemove ) {
					_peak.getIntegerProp().remove(sKey);
					bRemoveQuant  = true;
				}
			}
			if( _peak.getDoubleProp() != null && ! _peak.getDoubleProp().isEmpty() ) {
				List<String> sKeysToRemove = new ArrayList<>();
				for( String sKey : _peak.getDoubleProp().keySet() ) {
					if( quantFileProcessor.matchesExternalQuantKey(sKey) ) {
						sKeysToRemove.add(sKey);
					}
				}
				for( String sKey : sKeysToRemove ) {
					_peak.getDoubleProp().remove(sKey);
					bRemoveQuant  = true;
				}
			}			
		} catch( Exception e ) {
			logger.error("addPeaksData: error adding peaks data to table model.", e);
		}
		return bRemoveQuant;
	}
	
	/**
	 * @param _peak - the associated parent Peak being removed
	 * @return boolean - whether or not external quant was removed and needs to be saved
	 */
	protected boolean removeExternalQuantPeaksData( Peak _peak ) {
		boolean bRemoveQuant = false;
		try {
			if( this.fillType != FillTypes.PeaksWithFeatures ) {
				return false;
			}
			if( this.quantFileProcessorsToRemove != null ) {
				for( QuantFileProcessor quantFileProcessor : this.quantFileProcessorsToRemove ) {
					bRemoveQuant |= removeExternalQuantPeaksData(_peak, quantFileProcessor);
				}
			}
		} catch( Exception e ) {
			logger.error("addPeaksData: error adding peaks data to table model.", e);
		}
		return bRemoveQuant;
	}

	/**
	 * @param _parentScanNum - parent Scan for the Scan object being added
	 * @param _scan - the Scan object being added
	 * @param _peak - the associated parent Peak being added
	 * @param _settings - the TableViewerColumnSettings containing the column positions 
	 * @param alRow - the GRITSListDataRow being populated with values
	 * @return boolean - whether or not external quant was added and needs to be saved
	 */
	protected boolean addPeaksData( Integer _parentScanNum, Scan _scan, Peak _peak, TableViewerColumnSettings _settings, GRITSListDataRow alRow ) {
		boolean bAddQuant = false;
		boolean bHasSubScans = ! getMassSpecEntityProperty().getMassSpecParentProperty().getMassSpecMetaData().getMsExperimentType().equals(Method.MS_TYPE_MSPROFILE);
		boolean bHasScanNum = (getMassSpecEntityProperty().getMsLevel() == 1) || bHasSubScans;

		try {
			if( this.fillType == FillTypes.PeakList ) {
				MassSpecTableDataProcessorUtil.fillMassSpecPeakListData(_scan, _peak, alRow.getDataRow(), _settings);
			}
			else if( this.fillType == FillTypes.PeaksWithFeatures ) {
				MassSpecTableDataProcessorUtil.fillMassSpecPeakWithFeaturesData(_parentScanNum, _scan, _peak, alRow.getDataRow(), _settings, ! bHasScanNum);
				if( _scan != null && _scan.getPrecursor() != null ) {
					MassSpecTableDataProcessorUtil.fillMassSpecScanDataCorrectedPeakScanData(_scan.getPrecursor(), alRow.getDataRow(), _settings, getMassSpecEntityProperty());
				}
				if( this.quantFileProcessors != null && _scan != null && _scan.getPrecursor() != null) {
					for( QuantFileProcessor quantFileProcessor : this.quantFileProcessors ) {
						if( ! quantFileProcessor.getSettings().getQuantifyPrecursor() ) {
							continue;
						}
						Peak extPeak = getPrecursorPeak(_scan.getPrecursor().getMz(), quantFileProcessor.getSettings().getTargetScanNumber());
						if( extPeak == null ) {
							continue;
						}
						if ( ! (quantFileProcessor instanceof CustomAnnotationDataProcessor) ) {
							MassSpecTableDataProcessorUtil.fillMassSpecScanDataCorrectedPeakScanData(extPeak, alRow.getDataRow(), _settings, getMassSpecEntityProperty(), quantFileProcessor);
						}
						if( quantFileProcessor instanceof ExtractDataProcessor ) {
							MassSpecTableDataProcessorUtil.fillMassSpecPeakWithFeaturesDataExternalQuantWCharge(_scan, extPeak, alRow.getDataRow(), 
									_settings, 
									QuantFileProcessor.getExternalQuantProcessorKey(quantFileProcessor.getKeyID(), ExtractDataProcessor.DEFAULT_KEY),
									QuantFileProcessor.getExternalQuantProcessorLabel(quantFileProcessor.getLabelAlias(), ExtractDataProcessor.DEFAULT_LABEL));
						} else if ( quantFileProcessor instanceof FullMzXMLDataProcessor ) {
							MassSpecTableDataProcessorUtil.fillMassSpecPeakWithFeaturesDataExternalQuant(_scan, extPeak, alRow.getDataRow(), 
									_settings, 
									QuantFileProcessor.getExternalQuantProcessorKey(quantFileProcessor.getKeyID(), FullMzXMLDataProcessor.DEFAULT_KEY), 
									QuantFileProcessor.getExternalQuantProcessorLabel(quantFileProcessor.getLabelAlias(), FullMzXMLDataProcessor.DEFAULT_LABEL));
						} else if ( quantFileProcessor instanceof StandardQuantDataProcessor ) {
							MassSpecTableDataProcessorUtil.fillMassSpecScanDataStandardQuant(_scan, extPeak, alRow.getDataRow(), _settings, (StandardQuantDataProcessor) quantFileProcessor );
						} else if ( quantFileProcessor instanceof CustomAnnotationDataProcessor ) {
							CustomAnnotationDataProcessor spdp = (CustomAnnotationDataProcessor) quantFileProcessor;
							addSubScanPeaksData(_scan, extPeak, _settings, alRow);
						} else {
							throw new Exception("Unrecognized ExternalQuantFileProcessor: " + quantFileProcessor);
						}
					}
					// now that update any relative quantitation
					for( QuantFileProcessor quantFileProcessor : this.quantFileProcessors ) {				
						Peak extPeak = getPrecursorPeak(_scan.getPrecursor().getMz(), quantFileProcessor.getSettings().getTargetScanNumber());
						if( extPeak == null ) {
							continue;
						}
						if ( quantFileProcessor instanceof StandardQuantDataProcessor ) {
							MassSpecTableDataProcessorUtil.fillMassSpecScanDataStandardQuantPeakScanData(extPeak, alRow.getDataRow(), 
									_settings, getMassSpecEntityProperty(), (StandardQuantDataProcessor) quantFileProcessor);
						}							
						if( _peak.getIntegerProp() == null || _peak.getIntegerProp().size() != extPeak.getIntegerProp().size() ||
								_peak.getDoubleProp() == null || _peak.getDoubleProp().size() != extPeak.getDoubleProp().size()) {
							for( String sKey : extPeak.getIntegerProp().keySet() ) {
								Integer iVal = extPeak.getIntegerProp().get(sKey);
								if( ! _peak.getIntegerProp().containsKey(sKey) || _peak.getIntegerProp().get(sKey) != iVal ) {
									_peak.getIntegerProp().put(sKey, iVal);
									bAddQuant = true;
								}
							}
							for( String sKey : extPeak.getDoubleProp().keySet() ) {
								Double dVal = extPeak.getDoubleProp().get(sKey);
								if( ! _peak.getDoubleProp().containsKey(sKey) || _peak.getDoubleProp().get(sKey) != dVal ) {
									_peak.getDoubleProp().put(sKey, dVal);
									bAddQuant  = true;
								}
							}
						}
					}				

				}
			}
		} catch( Exception e ) {
			logger.error("addPeaksData: error adding peaks data to table model.", e);
		}
		return bAddQuant;
	}

	/**
	 * @param _scan - the Scan object being added
	 * @param _peak - the associated parent Peak being added
	 * @param _settings - the TableViewerColumnSettings containing the column positions 
	 * @param alRow - the GRITSListDataRow being populated with values
	 * @return boolean - whether or not MassSpecCustomAnnotation was added and needs to be saved
	 */
	protected boolean addSubScanPeaksData( Scan _scan, Peak _peak, TableViewerColumnSettings _settings, GRITSListDataRow alRow ) {
		boolean bAddQuant = false;
		try {
			MassSpecEntityProperty eProp = getMassSpecEntityProperty();
			MassSpecProperty prop = eProp.getMassSpecParentProperty();

			if( prop.getMassSpecMetaData().getCustomAnnotations() != null && _scan != null ) { // && _scan.getPrecursor() != null) {

				for( MassSpecCustomAnnotation annotation : prop.getMassSpecMetaData().getCustomAnnotations() ) {
					for( int i = 0; i < _scan.getPeaklist().size(); i++ ) {
						Peak subpeak = _scan.getPeaklist().get(i);
						bAddQuant |= MassSpecTableDataProcessorUtil.fillMassSpecScanDataCustomAnnotation(_peak, subpeak, 
								alRow.getDataRow(), _settings, annotation.getAnnotatedPeaks() );	
					}
				}
			}

		} catch( Exception e ) {
			logger.error("addSubScanPeaksData: error adding peaks data to table model.", e);
		}
		return bAddQuant;
	}

	/**
	 * @param _dMz - m/z value of a precursor
	 * @param _iScanNo - scan number of the source Scan (could be parent or external)
	 * @return Peak - the Peak in the source scan that is closest by m/z to the value of _dMz 
	 */
	protected Peak getPrecursorPeak( double _dMz, int _iScanNo ) {
		Peak precursor = null;
		double minDelta = Double.MAX_VALUE;
		for( Peak extractPeak : getScanData().get(_iScanNo).getPeaklist() ) {
			double delta = Math.abs( _dMz - extractPeak.getMz() );
			if( delta < minDelta ) {
				precursor = extractPeak;
				minDelta = delta;
			}
		}
		return precursor;
	}

	/**
	 * @param _scan - a org.grits.toolbox.ms.om.data.Scan object
	 * @param _settings - a TableViewerColumnSettings object
	 * @param alRow - a GRITSListDataRow object to be filled
	 * 
	 * Description: Populates a GRITSListDataRow with data from a Scan object at the column positions defined 
	 * in TableViewerColumnSettings object
	 */
	protected void addScanData( Scan _scan, TableViewerColumnSettings _settings, GRITSListDataRow alRow  ) {
		try {
			MassSpecTableDataProcessorUtil.fillMassSpecScanData(_scan, alRow.getDataRow(), _settings);
			MassSpecTableDataProcessorUtil.fillMassSpecScanDataCorrectedPeakScanData(_scan.getPrecursor(), alRow.getDataRow(), _settings, getMassSpecEntityProperty());
			if( this.quantFileProcessors != null &&
					_scan != null &&
					_scan.getPrecursor() != null ) {
				for( QuantFileProcessor quantFileProcessor : this.quantFileProcessors ) {
					Peak extPeak = getPrecursorPeak(_scan.getPrecursor().getMz(), quantFileProcessor.getSettings().getTargetScanNumber());
					if ( ! (quantFileProcessor instanceof CustomAnnotationDataProcessor) ) {
						MassSpecTableDataProcessorUtil.fillMassSpecScanDataCorrectedPeakScanData(extPeak, alRow.getDataRow(), _settings, getMassSpecEntityProperty(), quantFileProcessor);
					}
					if( quantFileProcessor instanceof ExtractDataProcessor ) {
						MassSpecTableDataProcessorUtil.fillMassSpecScanDataExternalQuantWCharge(_scan, alRow.getDataRow(), _settings, extPeak, 
								QuantFileProcessor.getExternalQuantProcessorKey(quantFileProcessor.getKeyID(), ExtractDataProcessor.DEFAULT_KEY),
								QuantFileProcessor.getExternalQuantProcessorLabel(quantFileProcessor.getLabelAlias(), ExtractDataProcessor.DEFAULT_LABEL));
					} else if ( quantFileProcessor instanceof FullMzXMLDataProcessor ) {
						MassSpecTableDataProcessorUtil.fillMassSpecScanDataExternalQuant(_scan, alRow.getDataRow(), _settings, extPeak, 
								QuantFileProcessor.getExternalQuantProcessorKey(quantFileProcessor.getKeyID(), FullMzXMLDataProcessor.DEFAULT_KEY),
								QuantFileProcessor.getExternalQuantProcessorLabel(quantFileProcessor.getLabelAlias(), FullMzXMLDataProcessor.DEFAULT_LABEL));
					} else if ( quantFileProcessor instanceof StandardQuantDataProcessor ) {
						MassSpecTableDataProcessorUtil.fillMassSpecScanDataStandardQuant(_scan, extPeak, alRow.getDataRow(), _settings, (StandardQuantDataProcessor) quantFileProcessor ); 
					} else if ( quantFileProcessor instanceof CustomAnnotationDataProcessor ) {
						CustomAnnotationDataProcessor spdp = (CustomAnnotationDataProcessor) quantFileProcessor;
						addSubScanPeaksData(_scan, extPeak, _settings, alRow);
						//						MassSpecTableDataProcessorUtil.fillMassSpecScanDataSpecialPeaks(_scan, extPeak, alRow.getDataRow(), 
						//								_settings, getMassSpecEntityProperty().getMsLevel(), ((SpecialPeaksDataProcessor) processor).getSpecialPeaksFile());							
					} else {
						throw new Exception("Unrecognized ExternalQuantFileProcessor: " + quantFileProcessor);
					}
				}

				// now that update any relative quantitation
				for( QuantFileProcessor quantFileProcessor : this.quantFileProcessors ) {				
					if ( quantFileProcessor instanceof StandardQuantDataProcessor ) {
						Peak extPeak = getPrecursorPeak(_scan.getPrecursor().getMz(), quantFileProcessor.getSettings().getTargetScanNumber());
						MassSpecTableDataProcessorUtil.fillMassSpecScanDataStandardQuantPeakScanData(extPeak, alRow.getDataRow(), 
								_settings, getMassSpecEntityProperty(), (StandardQuantDataProcessor) quantFileProcessor);
					}				
				}
			}
		} catch( Exception e ) {
			logger.error("addScanData: error adding scans data to table model.", e);
		}
	}

	/**
	 * @return GRITSListDataRow - a new instantiated row for the MassSpec data
	 */
	protected GRITSListDataRow getNewRow() {
		return super.getNewRow(getSimianTableDataObject().getTableHeader().get(getSimianTableDataObject().getTableHeader().size()-1).size(), getSimianTableDataObject().getTableData().size());
	}


}