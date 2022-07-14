package org.grits.toolbox.entry.ms.process.loader;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.grits.toolbox.datamodel.ms.tablemodel.dmtranslate.DMPeak;
import org.grits.toolbox.datamodel.ms.tablemodel.dmtranslate.DMPrecursorPeak;
import org.grits.toolbox.datamodel.ms.tablemodel.dmtranslate.DMScan;
import org.grits.toolbox.display.control.table.datamodel.GRITSColumnHeader;
import org.grits.toolbox.display.control.table.preference.TableViewerColumnSettings;
import org.grits.toolbox.entry.ms.dialog.MassSpecPeakIntensityGrid;
import org.grits.toolbox.entry.ms.extquantfiles.process.CorrectedQuantColumnInfo;
import org.grits.toolbox.entry.ms.extquantfiles.process.ExternalQuantColumnInfo;
import org.grits.toolbox.entry.ms.extquantfiles.process.QuantFileProcessor;
import org.grits.toolbox.entry.ms.extquantfiles.process.StandardQuantColumnInfo;
import org.grits.toolbox.entry.ms.extquantfiles.process.StandardQuantDataProcessor;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecCustomAnnotation;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecCustomAnnotationPeak;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.entry.ms.property.datamodel.ExternalQuantFileToAlias;
import org.grits.toolbox.entry.ms.property.datamodel.MSPropertyDataFile;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecMetaData;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecUISettings;
import org.grits.toolbox.entry.ms.property.datamodel.QuantFilePeaksToCorrectedIntensities;
import org.grits.toolbox.entry.ms.property.datamodel.QuantFileToCorrectedPeaks;
import org.grits.toolbox.ms.om.data.CustomExtraData;
import org.grits.toolbox.ms.om.data.Peak;
import org.grits.toolbox.ms.om.data.Scan;

/**
 * @author D Brent Weatherly (dbrentw@uga.edu)
 *
 */
/**
 * @author Brent Weatherly
 *
 */
public class MassSpecTableDataProcessorUtil
{
	private static final Logger logger = Logger.getLogger(MassSpecTableDataProcessorUtil.class);
	public static DecimalFormat formatDec4 = new DecimalFormat("0.0000");
	public static DecimalFormat formatDec2 = new DecimalFormat("0.00");
	public static DecimalFormat formatDec1 = new DecimalFormat("0.0");

	/**
	 * @param _columnSettings - a TableViewerColumnSettings being filled with MS-specific columns
	 * @param _iMSLevel - the current MS level of the Entry
	 * @param bIsPseudoScan - pseudo scan is added by GRITS if no summary scan exists (e.g. TIM, LC-MS/MS) 
	 * @param bHasSubScans - boolean, whether or not the current scan has sub-scan data
	 * @return int - number of columns added
	 * 
	 * Description: adds columns to the GRITS table specific to MS data
	 */
	public static int fillMassSpecColumnSettingsScan( TableViewerColumnSettings _columnSettings, int _iMSLevel, 
			boolean bIsPseudoScan, boolean bHasSubScans ) {
		try {
			int iNumColumns = 0;
			if( bIsPseudoScan ) {
				_columnSettings.addColumn(DMScan.scan_pseudoScanNo.getLabel(), DMScan.scan_pseudoScanNo.name());
				iNumColumns++;
			} else {
				_columnSettings.addColumn(DMScan.scan_scanNo.getLabel(), DMScan.scan_scanNo.name());
				iNumColumns++;
			}

			if ( _iMSLevel > 1 ) {
				_columnSettings.addColumn( DMPeak.peak_mz.getLabel(), DMPeak.peak_mz.name() );
				_columnSettings.addColumn( DMPeak.peak_intensity.getLabel(), DMPeak.peak_intensity.name() ); 
				_columnSettings.addColumn( DMPeak.peak_relative_intensity.getLabel(), DMPeak.peak_relative_intensity.name() ); 
				_columnSettings.addColumn( DMPrecursorPeak.precursor_peak_mz.getLabel(), DMPrecursorPeak.precursor_peak_mz.name() );
				_columnSettings.addColumn( DMPrecursorPeak.precursor_peak_intensity.getLabel(), DMPrecursorPeak.precursor_peak_intensity.name() ); 
				_columnSettings.addColumn( DMPrecursorPeak.precursor_peak_charge.getLabel(), DMPrecursorPeak.precursor_peak_charge.name() );
				_columnSettings.addColumn( DMScan.scan_parentScan.getLabel(), DMScan.scan_parentScan.name() );
				iNumColumns += 7;
			}
			_columnSettings.addColumn( DMScan.scan_retentionTime.getLabel(), DMScan.scan_retentionTime.name());
			_columnSettings.addColumn( DMScan.scan_scanStart.getLabel(), DMScan.scan_scanStart.name() );
			_columnSettings.addColumn( DMScan.scan_scanEnd.getLabel(), DMScan.scan_scanEnd.name() );
			_columnSettings.addColumn( DMScan.scan_msLevel.getLabel(), DMScan.scan_msLevel.name() );
			_columnSettings.addColumn( DMScan.scan_polarity.getLabel(), DMScan.scan_polarity.name() );
			_columnSettings.addColumn( DMScan.scan_activationMethode.getLabel(), DMScan.scan_activationMethode.name() );
			iNumColumns += 6;
			if( bHasSubScans ) {
				_columnSettings.addColumn( DMScan.scan_numsubscans.getLabel(), DMScan.scan_numsubscans.name() );
				iNumColumns++;
			}
			return iNumColumns;
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Column Settings for Scan.", ex);
		}
		return 0;

	}

	/**
	 * @param _columnSettings - a TableViewerColumnSettings being filled with MS-specific columns
	 * @param _iMSLevel - the current MS level of the Entry
	 * @param _sExtQuantPrefix - String prefix for the key of column header of this external quant data
	 * @param _sExtQuantLabel - String label for column header of this external quant data
	 * @return int - number of columns added
	 * 
	 * Description: Adds column headers for External Quant that include charge state. 
	 * Initially calls fillMassSpecColumnSettingsExternalQuant for non-charge columns.
	 */
	public static int fillMassSpecColumnSettingsExternalQuantWCharge( TableViewerColumnSettings _columnSettings, int _iMSLevel, 
			String _sExtQuantPrefix, String _sExtQuantLabel ) {
		try {
			if ( _iMSLevel > 1 ) {
				int iNumColumns = fillMassSpecColumnSettingsExternalQuant(_columnSettings, _iMSLevel, _sExtQuantPrefix, _sExtQuantLabel);
				CustomExtraData cedCharge = ExternalQuantColumnInfo.getExternalQuantCharge(_sExtQuantPrefix, _sExtQuantLabel);
				_columnSettings.addColumn( cedCharge.getLabel(), cedCharge.getKey() );	
				iNumColumns += 1;
				return iNumColumns;
			}
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Column Settings for Extract.", ex);
		}
		return 0;
	}	

	/**
	 * @param _columnSettings - a TableViewerColumnSettings being filled with MS-specific columns
	 * @param _iMSLevel - the current MS level of the Entry
	 * @param _sExtQuantPrefix - String prefix for the key of column header of this external quant data
	 * @param _sExtQuantLabel - String label for column header of this external quant data
	 * @return int - number of columns added
	 * 
	 * Description: Adds column headers for External Quant
	 */
	public static int fillMassSpecColumnSettingsExternalQuant( TableViewerColumnSettings _columnSettings, int _iMSLevel, 
			String _sExtQuantPrefix, String _sExtQuantLabel ) {
		int iNumColumns = 0;
		try {
			if ( _iMSLevel > 1 ) {
				CustomExtraData cedInt = ExternalQuantColumnInfo.getExternalQuantIntensity(_sExtQuantPrefix, _sExtQuantLabel);
				CustomExtraData cedCorInt = ExternalQuantColumnInfo.getExternalQuantCorrectedIntensity(_sExtQuantPrefix, _sExtQuantLabel);
				CustomExtraData cedIntMz = ExternalQuantColumnInfo.getExternalQuantIntensityMz(_sExtQuantPrefix, _sExtQuantLabel);
				CustomExtraData cedTotInt = ExternalQuantColumnInfo.getExternalQuantDeconvolutedIntensity(_sExtQuantPrefix, _sExtQuantLabel);
				_columnSettings.addColumn( cedInt.getLabel(), cedInt.getKey() );				
				_columnSettings.addColumn( cedCorInt.getLabel(), cedCorInt.getKey() );				
				//				_columnSettings.setVisColInx(_columnSettings.getColumnHeader(cedCorInt.getKey()), -1);
				_columnSettings.addColumn( cedIntMz.getLabel(), cedIntMz.getKey() );				
				_columnSettings.addColumn( cedTotInt.getLabel(), cedTotInt.getKey() );				
				iNumColumns += 4;
			}
			return iNumColumns;
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Column Settings for Full MS.", ex);
		}
		return 0;
	}	

	/**
	 * @param _columnSettings - a TableViewerColumnSettings being filled with MS-specific columns
	 * @param htKeyToPos - HashMap<String, Integer> that maps the External Quant label to its column position
	 * @param _sExtQuantPrefix - String prefix for the key of column header of this external quant data
	 * @param _sExtQuantLabel - String label for column header of this external quant data
	 * @return int - number of columns filled.
	 * 
	 * Description: The HashMap<String, Integer> was populated when the preferences were loaded. As the columns from 
	 * external quant are recognized and filled, the keys are removed in order to mark that the column was
	 * processed. After all columns have been processed, the HashMap tells GRITS which columns were unrecognized (if not empty)
	 */
	public static int fillMassSpecColumnSettingsExternalQuant( 
			TableViewerColumnSettings _columnSettings, 
			HashMap<String, Integer> htKeyToPos,
			String _sExtQuantPrefix, String _sExtQuantLabel) {
		try {
			CustomExtraData cedInt = ExternalQuantColumnInfo.getExternalQuantIntensity(_sExtQuantPrefix, _sExtQuantLabel);
			CustomExtraData cedCorInt = ExternalQuantColumnInfo.getExternalQuantCorrectedIntensity(_sExtQuantPrefix, _sExtQuantLabel);
			CustomExtraData cedIntMz = ExternalQuantColumnInfo.getExternalQuantIntensityMz(_sExtQuantPrefix, _sExtQuantLabel);
			CustomExtraData cedTotInt = ExternalQuantColumnInfo.getExternalQuantDeconvolutedIntensity(_sExtQuantPrefix, _sExtQuantLabel);
			if( htKeyToPos != null && htKeyToPos.containsKey(cedIntMz.getKey() ) ) {
				_columnSettings.putColumn(cedIntMz.getLabel(), 
						cedIntMz.getKey(), 
						htKeyToPos.get(cedIntMz.getKey()));	
				htKeyToPos.remove(cedIntMz.getKey());
			} else { // just add?
				_columnSettings.addColumn( cedIntMz.getLabel(), 
						cedIntMz.getKey() );		
			}	
			if( htKeyToPos != null && htKeyToPos.containsKey(cedCorInt.getKey() ) ) {
				_columnSettings.putColumn(cedCorInt.getLabel(), 
						cedCorInt.getKey(), 
						htKeyToPos.get(cedCorInt.getKey()));	
				htKeyToPos.remove(cedCorInt.getKey());
			} else { // just add?
				_columnSettings.addColumn( cedCorInt.getLabel(), 
						cedCorInt.getKey() );		
			}	
			if( htKeyToPos != null && htKeyToPos.containsKey(cedInt.getKey() ) ) {
				_columnSettings.putColumn(cedInt.getLabel(), 
						cedInt.getKey(), 
						htKeyToPos.get(cedInt.getKey()));	
				htKeyToPos.remove(cedInt.getKey());
			} else { // just add?
				_columnSettings.addColumn( cedInt.getLabel(), 
						cedInt.getKey() );		
			}	
			if( htKeyToPos != null && htKeyToPos.containsKey(cedTotInt.getKey() ) ) {
				_columnSettings.putColumn(cedTotInt.getLabel(), 
						cedTotInt.getKey(), 
						htKeyToPos.get(cedTotInt.getKey()));	
				htKeyToPos.remove(cedTotInt.getKey());
			} else { // just add?
				_columnSettings.addColumn( cedTotInt.getLabel(), 
						cedTotInt.getKey() );		
			}	
			return 3;
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Column Settings for Full MS Data.", ex);
		}
		return 0;
	}

	/**
	 * @param _columnSettings - a TableViewerColumnSettings being filled with MS-specific columns
	 * @param htKeyToPos - HashMap<String, Integer> that maps the External Quant label to its column position
	 * @param _sExtQuantPrefix - String prefix for the key of column header of this external quant data
	 * @param _sExtQuantLabel - String label for column header of this external quant data
	 * @return int - number of columns filled.
	 * 
	 * Description: Initially calls fillMassSpecColumnSettingsExternalQuant to process non-charge column. See that method for
	 * further description.
	 */
	public static int fillMassSpecColumnSettingsExternalQuantWCharge( 
			TableViewerColumnSettings _columnSettings, 
			HashMap<String, Integer> htKeyToPos,
			String _sExtQuantPrefix, String _sExtQuantLabel ) {
		try {
			int iNumColumns = fillMassSpecColumnSettingsExternalQuant(_columnSettings, htKeyToPos, _sExtQuantPrefix, _sExtQuantLabel);
			CustomExtraData cedCharge = ExternalQuantColumnInfo.getExternalQuantCharge(_sExtQuantPrefix, _sExtQuantLabel);
			if( htKeyToPos != null && htKeyToPos.containsKey(cedCharge.getKey() ) ) {
				_columnSettings.putColumn(cedCharge.getLabel(), 
						cedCharge.getKey(), 
						htKeyToPos.get(cedCharge.getKey()));	
				htKeyToPos.remove(cedCharge.getKey());
			} else { // just add?
				_columnSettings.addColumn( cedCharge.getLabel(), 
						cedCharge.getKey() );		
			}	
			iNumColumns += 1;
			return iNumColumns;
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Column Settings for Extract Data.", ex);
		}
		return 0;
	}

	public static Double getFormattedMZValue( double dMz ) {
		return new Double(formatDec4.format(dMz));
	}
	
	public static Double getFormattedRetentionTime( double dRT ) {
		return new Double(formatDec2.format(dRT / 60.0));
	}
	
	public static Double getUnFormattedRetentionTime( double dRT ) {
		return new Double(formatDec4.format(dRT * 60.0));
	}

	/**
	 * @param a_scan - the Scan object containing the data
	 * @param _tableRow - The ArrayList<Object> to be filled
	 * @param _columnSettings - the TableViewerColumnSettings object with the positions of the columns in the table row
	 */
	public static void fillMassSpecScanData(Scan a_scan,  ArrayList<Object> _tableRow, 
			TableViewerColumnSettings _columnSettings  ) {    	
		try {
			// scan data
			MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMScan.scan_scanNo.name()), 
					a_scan.getScanNo(), _tableRow);
			if ( a_scan.getMsLevel() > 1 && a_scan.getPrecursor() != null ) {
				// The peak info from the parent peak list
				if( a_scan.getPrecursor().getMz() != null )
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPeak.peak_mz.name()), 
							new Double(formatDec4.format(a_scan.getPrecursor().getMz())), _tableRow);
				if( a_scan.getPrecursor().getIntensity() != null )
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPeak.peak_intensity.name()), 
							new Double(formatDec1.format(a_scan.getPrecursor().getIntensity())), _tableRow);
				if( a_scan.getPrecursor().getRelativeIntensity() != null )
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPeak.peak_relative_intensity.name()), 
							new Double(formatDec4.format(a_scan.getPrecursor().getRelativeIntensity())), _tableRow);

				// the precursor info
				if( a_scan.getPrecursor().getPrecursorMz() != null )
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPrecursorPeak.precursor_peak_mz.name()), 
							new Double(formatDec4.format(a_scan.getPrecursor().getPrecursorMz())), _tableRow);
				if( a_scan.getPrecursor().getPrecursorIntensity() != null )
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPrecursorPeak.precursor_peak_intensity.name()), 
							new Double(formatDec1.format(a_scan.getPrecursor().getPrecursorIntensity())), _tableRow);
				if( a_scan.getPrecursor().getPrecursorCharge() != null )
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPrecursorPeak.precursor_peak_charge.name()), 
							a_scan.getPrecursor().getPrecursorCharge(), _tableRow);

				if( a_scan.getParentScan() != null )
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMScan.scan_parentScan.name()), 
							a_scan.getParentScan(), _tableRow);
			} 
			if( a_scan.getRetentionTime() != null )
				MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMScan.scan_retentionTime.name()), 
						getFormattedRetentionTime(a_scan.getRetentionTime()), _tableRow);
			if( a_scan.getScanStart() != null )
				MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMScan.scan_scanStart.name()), 
						new Double(formatDec4.format(a_scan.getScanStart())), _tableRow);
			if( a_scan.getScanEnd() != null )
				MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMScan.scan_scanEnd.name()), 
						new Double(formatDec4.format(a_scan.getScanEnd())), _tableRow);
			if( a_scan.getMsLevel() != null )
				MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMScan.scan_msLevel.name()), 
						a_scan.getMsLevel(), _tableRow);
			if( a_scan.getPolarity() != null )
				MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMScan.scan_polarity.name()), 
						a_scan.getPolarity(), _tableRow);
			if( a_scan.getActivationMethode() != null )
				MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMScan.scan_activationMethode.name()), 
						a_scan.getActivationMethode(), _tableRow);
			if( a_scan.getSubScans() != null )
				MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMScan.scan_numsubscans.name()), 
						a_scan.getSubScans() != null ? a_scan.getSubScans().size() : 0, _tableRow);
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Row Data for Scan.", ex);
		}
	}

	/**
	 * @param a_scan - the Scan object containing the data
	 * @param _tableRow - The ArrayList<Object> to be filled
	 * @param _columnSettings - the TableViewerColumnSettings object with the positions of the columns in the table row
	 * @param _externalPeak - the peak from the external quant file that matched the precursor
	 * @param _sExtQuantPrefix - String prefix for the key of column header of this external quant data
	 * @param _sExtQuantLabel - String label for column header of this external quant data
	 * 
	 * Description: fills in the charge-specific external quant stored in the matched external Peak. 
	 * Initially calls fillMassSpecScanDataExternalQuant
	 * 
	 */
	public static void fillMassSpecScanDataExternalQuantWCharge(Scan a_scan,  ArrayList<Object> _tableRow, 
			TableViewerColumnSettings _columnSettings, Peak _externalPeak, String _sExtQuantPrefix, String _sExtQuantLabel ) 
	{    	
		if ( a_scan.getMsLevel() > 1 && a_scan.getPrecursor() != null ) {
			fillMassSpecScanDataExternalQuant(a_scan, _tableRow, _columnSettings, _externalPeak, _sExtQuantPrefix, _sExtQuantLabel);

			CustomExtraData cedCharge = ExternalQuantColumnInfo.getExternalQuantCharge(_sExtQuantPrefix, _sExtQuantLabel);
			if( _externalPeak != null && _externalPeak.getIntegerProp() != null && 
					_externalPeak.getIntegerProp().containsKey(cedCharge.getKey()) ) {
				MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(cedCharge.getKey()), 
						_externalPeak.getIntegerProp().get(cedCharge.getKey()),
						_tableRow);
			}
		} 
	}

	/**
	 * @param a_scan - the Scan object containing the data
	 * @param _tableRow - The ArrayList<Object> to be filled
	 * @param _columnSettings - the TableViewerColumnSettings object with the positions of the columns in the table row
	 * @param _externalPeak - the peak from the external quant file that matched the precursor
	 * @param _sExtQuantPrefix - String prefix for the key of column header of this external quant data
	 * @param _sExtQuantLabel - String label for column header of this external quant data
	 * 
	 * Description: fills in the non-charge-specific external quant stored in the matched external Peak. 
	 */
	public static void fillMassSpecScanDataExternalQuant(Scan a_scan,  ArrayList<Object> _tableRow, 
			TableViewerColumnSettings _columnSettings, Peak _externalPeak, String _sExtQuantPrefix, String _sExtQuantLabel ) 
	{    	
		try {
			if ( a_scan.getMsLevel() > 1 && a_scan.getPrecursor() != null ) {
				CustomExtraData cedInt = ExternalQuantColumnInfo.getExternalQuantIntensity(_sExtQuantPrefix, _sExtQuantLabel);
				//				CustomExtraData cedCorInt = ExternalQuantColumnInfo.getExternalQuantCorrectedIntensity(_sExtQuantPrefix, _sExtQuantLabel);
				CustomExtraData cedIntMz = ExternalQuantColumnInfo.getExternalQuantIntensityMz(_sExtQuantPrefix, _sExtQuantLabel);
				CustomExtraData cedTotInt = ExternalQuantColumnInfo.getExternalQuantDeconvolutedIntensity(_sExtQuantPrefix, _sExtQuantLabel);
				if( _externalPeak != null && _externalPeak.getDoubleProp() != null && 
						_externalPeak.getDoubleProp().containsKey(cedIntMz.getKey()) ) {
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(cedIntMz.getKey()), 
							new Double(cedIntMz.getDoubleFormat().format(_externalPeak.getDoubleProp().get(cedIntMz.getKey()))),
							_tableRow);
				}
				if( _externalPeak != null && _externalPeak.getDoubleProp() != null && 
						_externalPeak.getDoubleProp().containsKey(cedInt.getKey()) ) {
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(cedInt.getKey()), 
							new Double(cedInt.getDoubleFormat().format(_externalPeak.getDoubleProp().get(cedInt.getKey()))),
							_tableRow);
				}
				if( _externalPeak != null && _externalPeak.getDoubleProp() != null && 
						_externalPeak.getDoubleProp().containsKey(cedTotInt.getKey()) ) {
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(cedTotInt.getKey()), 
							new Double(cedTotInt.getDoubleFormat().format(_externalPeak.getDoubleProp().get(cedTotInt.getKey()))),
							_tableRow);
				}
			} 
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Row Data for Full MS.", ex);
		}
	}

	/**
	 * @param a_scan - the Scan object containing the data
	 * @param _tableRow - The ArrayList<Object> to be filled
	 * @param _columnSettings - the TableViewerColumnSettings object with the positions of the columns in the table row
	 * @param _iMSLevel - the MS level of the current Entry
	 * 
	 * Description: If we were going to modify data from MS, we would update it from the Scan object.
	 * 
	 * NOTE: this method is not used (yet)
	 */
	public static void updateMassSpecScanData(Scan a_scan, ArrayList<Object> _tableRow, 
			TableViewerColumnSettings _columnSettings, int _iMSLevel) 
	{
		try {
			// scan data
			if ( _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_scanNo.name()) ) != null &&
					! _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_scanNo.name()) ).equals( a_scan.getScanNo() ) ) {
				a_scan.setScanNo( (Integer) _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_scanNo.name()) ));
			}
			if ( _iMSLevel > 1 ) {
				if ( _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_mz.name()) ) != null &&
						! _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_mz.name()) ).equals( a_scan.getPrecursor().getMz() ) ) {
					a_scan.getPrecursor().setMz( (Double) _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_mz.name()) ));
				}
				if ( _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_intensity.name()) ) != null &&
						! _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_intensity.name()) ).equals( a_scan.getPrecursor().getIntensity() ) ) {
					a_scan.getPrecursor().setIntensity( (Double) _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_intensity.name()) ));
				}
				if ( _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_relative_intensity.name()) ) != null &&
						! _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_relative_intensity.name()) ).equals( a_scan.getPrecursor().getRelativeIntensity() ) ) {
					a_scan.getPrecursor().setRelativeIntensity( (Double) _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_relative_intensity.name()) ));
				}

				if ( _tableRow.get( _columnSettings.getColumnPosition(DMPrecursorPeak.precursor_peak_mz.name()) ) != null &&
						! _tableRow.get( _columnSettings.getColumnPosition(DMPrecursorPeak.precursor_peak_mz.name()) ).equals( a_scan.getPrecursor().getPrecursorMz() ) ) {
					a_scan.getPrecursor().setPrecursorMz( (Double) _tableRow.get( _columnSettings.getColumnPosition(DMPrecursorPeak.precursor_peak_mz.name()) ));
				}
				if ( _tableRow.get( _columnSettings.getColumnPosition(DMPrecursorPeak.precursor_peak_intensity.name()) ) != null &&
						! _tableRow.get( _columnSettings.getColumnPosition(DMPrecursorPeak.precursor_peak_intensity.name()) ).equals( a_scan.getPrecursor().getPrecursorIntensity() ) ) {
					a_scan.getPrecursor().setPrecursorIntensity( (Double) _tableRow.get( _columnSettings.getColumnPosition(DMPrecursorPeak.precursor_peak_intensity.name()) ));
				}
				if ( _tableRow.get( _columnSettings.getColumnPosition(DMPrecursorPeak.precursor_peak_charge.name()) ) != null &&
						! _tableRow.get( _columnSettings.getColumnPosition(DMPrecursorPeak.precursor_peak_charge.name()) ).equals( a_scan.getPrecursor().getPrecursorCharge() ) ) {
					a_scan.getPrecursor().setPrecursorCharge( (Integer) _tableRow.get( _columnSettings.getColumnPosition(DMPrecursorPeak.precursor_peak_charge.name()) ));
				}
				if ( _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_parentScan.name()) ) != null &&
						! _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_parentScan.name()) ).equals( a_scan.getParentScan() ) ) {
					a_scan.setParentScan( (Integer) _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_parentScan.name()) ));
				}
			}
			if ( _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_retentionTime.name()) ) != null &&
					! _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_retentionTime.name()) ).equals( a_scan.getRetentionTime() ) ) {
				a_scan.setRetentionTime( getUnFormattedRetentionTime( (Double) _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_retentionTime.name()) ) ) );
			}
			if ( _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_scanEnd.name()) ) != null &&
					! _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_scanEnd.name()) ).equals( a_scan.getScanStart() ) ) {
				a_scan.setScanStart( (Double) _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_scanEnd.name()) ));
			}
			if ( _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_scanEnd.name()) ) != null &&
					! _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_scanEnd.name()) ).equals( a_scan.getScanEnd() ) ) {
				a_scan.setScanEnd( (Double) _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_scanEnd.name()) ));
			}
			if ( _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_msLevel.name()) ) != null &&
					! _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_msLevel.name()) ).equals( a_scan.getMsLevel() ) ) {
				a_scan.setMsLevel( (Integer) _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_msLevel.name()) ));
			}
			if ( _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_polarity.name()) ) != null &&
					! _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_polarity.name()) ).equals( a_scan.getPolarity() ) ) {
				a_scan.setPolarity( (Boolean) _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_polarity.name()) ));
			}
			if ( _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_activationMethode.name()) ) != null &&
					! _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_activationMethode.name()) ).equals( a_scan.getActivationMethode() ) ) {
				a_scan.setActivationMethode( (String) _tableRow.get( _columnSettings.getColumnPosition(DMScan.scan_activationMethode.name()) ));
			}
		} catch( Exception ex ) {
			logger.error("Error updating Mass Spec Row Data for Scan.", ex);
		}
	}

	/**
	 * @param _columnSettings - the TableViewerColumnSettings object to be populated with peak-specific columns.
	 * @return int - the number of columns added
	 * 
	 * Description: Adds the columns specific to the Peak class to the TableViewerColumnSettings object
	 */
	public static int fillMassSpecColumnSettingsPeakList( TableViewerColumnSettings _columnSettings ) {
		try {
			_columnSettings.addColumn(DMPeak.peak_id.getLabel(), DMPeak.peak_id.name());
			_columnSettings.addColumn(DMPeak.peak_mz.getLabel(), DMPeak.peak_mz.name());
			_columnSettings.addColumn(DMPeak.peak_intensity.getLabel(), DMPeak.peak_intensity.name());
			_columnSettings.addColumn(DMPeak.peak_relative_intensity.getLabel(), DMPeak.peak_relative_intensity.name());
			_columnSettings.addColumn(DMPeak.peak_charge.getLabel(), DMPeak.peak_charge.name());
			_columnSettings.addColumn(DMPeak.peak_is_precursor.getLabel(), DMPeak.peak_is_precursor.name());
			return 6;
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Column Settings for Peak List.", ex);
		}
		return 0;
	}

	/**
	 * @param a_scan - the Scan object containing the scan-specific data
	 * @param a_peak - the Peak object containing the peak-specific data
	 * @param _tableRow - The ArrayList<Object> to be filled
	 * @param _columnSettings - the TableViewerColumnSettings object with the positions of the columns in the table row
	 */
	public static void fillMassSpecPeakListData(Scan a_scan, Peak a_peak,  ArrayList<Object> _tableRow, 
			TableViewerColumnSettings _columnSettings )  {
		try {
			MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPeak.peak_id.name()), 
					a_peak.getId(), _tableRow);
			MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPeak.peak_mz.name()), 
					new Double(formatDec4.format(a_peak.getMz())), _tableRow);
			if( a_peak.getIntensity() != null )
				MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPeak.peak_intensity.name()), 
						new Double(formatDec1.format(a_peak.getIntensity())), _tableRow);
			if( a_peak.getRelativeIntensity() != null )
				MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPeak.peak_relative_intensity.name()), 
						new Double(formatDec4.format(a_peak.getRelativeIntensity())), _tableRow);
			MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPeak.peak_charge.name()), 
					a_peak.getCharge(), _tableRow);
			MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPeak.peak_is_precursor.name()), 
					a_peak.getIsPrecursor() ? "Yes" : "No", _tableRow);
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Row Data for Peak List.", ex);
		}

	}

	/**
	 * @param a_scan - the Scan object containing the scan-specific data
	 * @param a_peak - the Peak object containing the peak-specific data
	 * @param _tableRow - The ArrayList<Object> to be filled
	 * @param _columnSettings - the TableViewerColumnSettings object with the positions of the columns in the table row
	 * 
	 * Description: If we were going to modify peak data from MS, we would update it from the Peak object.
	 * 
	 * NOTE: this method is not used (yet)
	 */
	public static void updateMassSpecPeakListData(Scan a_scan, Peak a_peak, ArrayList<Object> _tableRow, TableViewerColumnSettings _columnSettings ) 
	{
		try {
			if ( _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_id.name()) ) != null &&
					! _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_id.name()) ).equals( a_peak.getId() ) ) {
				a_peak.setId( (Integer) _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_id.name()) ));
			}
			if ( _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_charge.name()) ) != null &&
					! _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_charge.name()) ).equals( a_peak.getCharge() ) ) {
				a_peak.setCharge( (Integer) _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_charge.name()) ));
			}
			if ( _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_intensity.name()) ) != null &&
					! _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_intensity.name()) ).equals( a_peak.getIntensity() ) ) {
				a_peak.setIntensity( (Double) _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_intensity.name()) ));
			}
			if ( _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_relative_intensity.name()) ) != null &&
					! _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_relative_intensity.name()) ).equals( a_peak.getRelativeIntensity() ) ) {
				a_peak.setRelativeIntensity( (Double) _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_relative_intensity.name()) ));
			}
			if ( _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_mz.name()) ) != null &&
					! _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_mz.name()) ).equals( a_peak.getMz() ) ) {
				a_peak.setMz( (Double) _tableRow.get( _columnSettings.getColumnPosition(DMPeak.peak_mz.name()) ));
			}
		} catch( Exception ex ) {
			logger.error("Error updating Mass Spec Row Data for Peak List.", ex);
		}
	}

	/**
	 * @param _columnSettings - the TableViewerColumnSettings object with the positions of the columns in the table row
	 * @param bIsPseudoScan - boolean value whether this is a "pseudo" scan to summarize sub-scans (TIM, LC-MS/MS)
	 * @return int - the number of columns added
	 * 
	 * Description: adds the columns to the TableViewerColumnSettings applicable to precursor peaks (might be annotated w/ features)
	 */
	public static int fillMassSpecColumnSettingsPeakWithFeatures( TableViewerColumnSettings _columnSettings, boolean bIsPseudoScan ) {
		try {
			if( bIsPseudoScan ) {
				_columnSettings.addColumn(DMScan.scan_pseudoScanNo.getLabel(), DMScan.scan_pseudoScanNo.name());
			} else {
				_columnSettings.addColumn(DMScan.scan_scanNo.getLabel(), DMScan.scan_scanNo.name());
			}
			_columnSettings.addColumn(DMScan.scan_retentionTime.getLabel(), DMScan.scan_retentionTime.name());
			_columnSettings.addColumn(DMScan.scan_parentScan.getLabel(), DMScan.scan_parentScan.name());

			// by definition, a Peak with Features had to be a precursor, so no need to check if column needed or not
			_columnSettings.addColumn( DMPeak.peak_id.getLabel(), DMPeak.peak_id.name() );
			_columnSettings.addColumn( DMPeak.peak_mz.getLabel(), DMPeak.peak_mz.name() );
			_columnSettings.addColumn( DMPeak.peak_intensity.getLabel(), DMPeak.peak_intensity.name() ); 
			_columnSettings.addColumn( DMPeak.peak_relative_intensity.getLabel(), DMPeak.peak_relative_intensity.name() ); 
			_columnSettings.addColumn( DMPrecursorPeak.precursor_peak_mz.getLabel(), DMPrecursorPeak.precursor_peak_mz.name() );
			_columnSettings.addColumn( DMPrecursorPeak.precursor_peak_intensity.getLabel(), DMPrecursorPeak.precursor_peak_intensity.name() ); 
			_columnSettings.addColumn( DMPrecursorPeak.precursor_peak_charge.getLabel(), DMPrecursorPeak.precursor_peak_charge.name() );
			return 10;
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Column Settings for Peak List with Features.", ex);
		}
		return 0;
	}

	/**
	 * @param _columnSettings - the TableViewerColumnSettings object with the positions of the columns in the table row
	 * @param _sExtQuantPrefix - String prefix for the key of column header of this external quant data
	 * @param _sExtQuantLabel - String label for column header of this external quant data
	 * @return int - the number of columns added
	 * 
	 * Description: adds the charge-specific external quant columns to the TableViewerColumnSettings 
	 * applicable to precursor peaks (might be annotated w/ features).
	 * Initially calls fillMassSpecColumnSettingsPeakWithFeaturesExternalQuant(..) for non-charge columns
	 */
	public static int fillMassSpecColumnSettingsPeakWithFeaturesExternalQuantWCharge( TableViewerColumnSettings _columnSettings, 
			String _sExtQuantPrefix, String _sExtQuantLabel ) {
		try {
			int iNumColumns = fillMassSpecColumnSettingsPeakWithFeaturesExternalQuant(_columnSettings, _sExtQuantPrefix, _sExtQuantLabel);
			CustomExtraData cedCharge = ExternalQuantColumnInfo.getExternalQuantCharge(_sExtQuantPrefix, _sExtQuantLabel);
			_columnSettings.addColumn( cedCharge.getLabel(), 
					cedCharge.getKey() );	
			iNumColumns += 1;
			return iNumColumns;
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Column Settings for Peak Features with Extract.", ex);
		}
		return 0;
	}

	/**
	 * @param _columnSettings - the TableViewerColumnSettings object with the positions of the columns in the table row
	 * @param _sExtQuantPrefix - String prefix for the key of column header of this external quant data
	 * @param _sExtQuantLabel - String label for column header of this external quant data
	 * @return int - the number of columns added
	 * 
	 * Description: adds the non-charge-specific external quant columns to the TableViewerColumnSettings 
	 * applicable to precursor peaks (might be annotated w/ features)
	 */
	public static int fillMassSpecColumnSettingsPeakWithFeaturesExternalQuant( TableViewerColumnSettings _columnSettings, 
			String _sExtQuantPrefix, String _sExtQuantLabel ) {
		try {
			CustomExtraData cedInt = ExternalQuantColumnInfo.getExternalQuantIntensity(_sExtQuantPrefix, _sExtQuantLabel);
			CustomExtraData cedCorInt = ExternalQuantColumnInfo.getExternalQuantCorrectedIntensity(_sExtQuantPrefix, _sExtQuantLabel);
			CustomExtraData cedTotInt = ExternalQuantColumnInfo.getExternalQuantDeconvolutedIntensity(_sExtQuantPrefix, _sExtQuantLabel);
			CustomExtraData cedIntMz = ExternalQuantColumnInfo.getExternalQuantIntensityMz(_sExtQuantPrefix, _sExtQuantLabel);
			_columnSettings.addColumn( cedIntMz.getLabel(), cedIntMz.getKey() );				
			_columnSettings.addColumn( cedCorInt.getLabel(), cedCorInt.getKey() );
			//			_columnSettings.setVisColInx(_columnSettings.getColumnHeader(cedCorInt.getKey()), -1);
			_columnSettings.addColumn( cedInt.getLabel(), cedInt.getKey() );		
			_columnSettings.addColumn( cedTotInt.getLabel(), cedTotInt.getKey() );		
			return 3;
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Column Settings for Peak List with Features Full MS.", ex);
		}
		return 0;
	}

	/**
	 * @param a_parentScanInt - int value of the parent scan number for this precursor
	 * @param a_scan - Scan object for this precursor
	 * @param a_peak - Peak object from the parent scan
	 * @param _tableRow - The ArrayList<Object> to be filled
	 * @param _columnSettings - the TableViewerColumnSettings object with the positions of the columns in the table row
	 * @param bIsPseudoScan - boolean value whether this is a "pseudo" scan to summarize sub-scans (TIM, LC-MS/MS)
	 * 
	 * Description: fills the non-charge-specific external quant columns to the TableViewerColumnSettings 
	 * applicable to precursor peaks (might be annotated w/ features)
	 */
	public static void fillMassSpecPeakWithFeaturesData(Integer a_parentScanInt, Scan a_scan, Peak a_peak,  
			ArrayList<Object> _tableRow, TableViewerColumnSettings _columnSettings, boolean bIsPseudoScan ) 	{
		try {
			if( a_parentScanInt != null ){
				if( a_parentScanInt != null )
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMScan.scan_parentScan.name()), 
							a_parentScanInt, _tableRow);			
			}
			if( a_scan != null ){
				if( bIsPseudoScan ) {
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMScan.scan_pseudoScanNo.name()), 
							a_scan.getScanNo(), _tableRow);
				} else {
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMScan.scan_scanNo.name()), 
							a_scan.getScanNo(), _tableRow);
				}
				if( a_scan.getRetentionTime() != null ) {
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMScan.scan_retentionTime.name()), 
							getFormattedRetentionTime(a_scan.getRetentionTime()), _tableRow);
				}
			}
			// The peak info from the parent peak list
			if( a_peak.getId() != null )
				MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPeak.peak_id.name()), 
						a_peak.getId(), _tableRow);
			if( a_peak.getMz() != null )
				MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPeak.peak_mz.name()), 
						new Double(formatDec4.format(a_peak.getMz())), _tableRow);
			if( a_peak.getIntensity() != null )
				MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPeak.peak_intensity.name()), 
						new Double(formatDec1.format(a_peak.getIntensity())), _tableRow);
			if( a_peak.getRelativeIntensity() != null )
				MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPeak.peak_relative_intensity.name()), 
						new Double(formatDec4.format(a_peak.getRelativeIntensity())), _tableRow);

			// the precursor info
			if( a_peak.getPrecursorMz() != null )
				MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPrecursorPeak.precursor_peak_mz.name()), 
						new Double(formatDec4.format(a_peak.getPrecursorMz())), _tableRow);
			if( a_peak.getPrecursorIntensity() != null )
				MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPrecursorPeak.precursor_peak_intensity.name()), 
						new Double(formatDec1.format(a_peak.getPrecursorIntensity())), _tableRow);
			if( a_peak.getPrecursorCharge() != null )
				MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(DMPrecursorPeak.precursor_peak_charge.name()), 
						a_peak.getPrecursorCharge(), _tableRow);
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Row Data for Peak List with Features.", ex);
		}
	}

	/**
	 * @param a_scan - Scan object for this precursor
	 * @param _peak - Peak object from the parent scan
	 * @param _tableRow - The ArrayList<Object> to be filled
	 * @param _columnSettings - the TableViewerColumnSettings object with the positions of the columns in the table row
	 * @param _sExtQuantPrefix - String prefix for the key of column header of this external quant data
	 * @param _sExtQuantLabel - String label for column header of this external quant data
	 * 
	 * Description: fills the charge-specific external quant columns table Row (found in the TableViewerColumnSettings) 
	 * applicable to precursor peaks (might be annotated w/ features).
	 * Initially calls fillMassSpecPeakWithFeaturesDataExternalQuant to load the non-charge columns
	 */
	public static void fillMassSpecPeakWithFeaturesDataExternalQuantWCharge(Scan a_scan, Peak _peak,  ArrayList<Object> _tableRow, 
			TableViewerColumnSettings _columnSettings, String _sExtQuantPrefix, String _sExtQuantLabel ) {
		try {
			fillMassSpecPeakWithFeaturesDataExternalQuant(a_scan, _peak, _tableRow, _columnSettings, _sExtQuantPrefix, _sExtQuantLabel);
			CustomExtraData cedCharge = ExternalQuantColumnInfo.getExternalQuantCharge(_sExtQuantPrefix, _sExtQuantLabel);			
			if( a_scan != null && a_scan.getMsLevel() > 1 ) {
				if( _peak != null && _peak.getIntegerProp() != null && 
						_peak.getIntegerProp().containsKey(cedCharge.getKey()) ) {
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(cedCharge.getKey()), 
							_peak.getIntegerProp().get(cedCharge.getKey()),
							_tableRow);
				}        		
			}
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Row Data for Peak List with Features Extract.", ex);
		}
	}

	/**
	 * @param a_scan - Scan object for this precursor
	 * @param _peak - Peak object from the parent scan
	 * @param _tableRow - The ArrayList<Object> to be filled
	 * @param _columnSettings - the TableViewerColumnSettings object with the positions of the columns in the table row
	 * @param _sExtQuantPrefix - String prefix for the key of column header of this external quant data
	 * @param _sExtQuantLabel - String label for column header of this external quant data
	 * 
	 * Description: fills the non-charge-specific external quant columns table Row (found in the TableViewerColumnSettings) 
	 * applicable to precursor peaks (might be annotated w/ features).
	 */
	public static void fillMassSpecPeakWithFeaturesDataExternalQuant(Scan a_scan, Peak _peak,  ArrayList<Object> _tableRow, 
			TableViewerColumnSettings _columnSettings, String _sExtQuantPrefix, String _sExtQuantLabel ) {
		try {
			CustomExtraData cedInt = ExternalQuantColumnInfo.getExternalQuantIntensity(_sExtQuantPrefix, _sExtQuantLabel);
			CustomExtraData cedIntMz = ExternalQuantColumnInfo.getExternalQuantIntensityMz(_sExtQuantPrefix, _sExtQuantLabel);
			CustomExtraData cedTotInt = ExternalQuantColumnInfo.getExternalQuantDeconvolutedIntensity(_sExtQuantPrefix, _sExtQuantLabel);
			if( a_scan != null && a_scan.getMsLevel() > 1 ) {
				if( _peak != null && _peak.getDoubleProp() != null && 
						_peak.getDoubleProp().containsKey(cedIntMz.getKey()) ) {
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(cedIntMz.getKey()), 
							new Double(cedIntMz.getDoubleFormat().format(_peak.getDoubleProp().get(cedIntMz.getKey()))),
							_tableRow);
				}
				if( _peak != null && _peak.getDoubleProp() != null && 
						_peak.getDoubleProp().containsKey(cedInt.getKey()) &&
						_peak.getDoubleProp().get(cedInt.getKey()) != null ) {
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(cedInt.getKey()), 
							new Double(cedInt.getDoubleFormat().format(_peak.getDoubleProp().get(cedInt.getKey()))),
							_tableRow);
				}
				if( _peak != null && _peak.getDoubleProp() != null && 
						_peak.getDoubleProp().containsKey(cedTotInt.getKey()) ) {
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(cedTotInt.getKey()), 
							new Double(cedTotInt.getDoubleFormat().format(_peak.getDoubleProp().get(cedTotInt.getKey()))),
							_tableRow);
				}
			}
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Row Data for Peak List with Features Full MS.", ex);
		}
	}

	/**
	 * @param a_scan - the Scan object containing the data
	 * @param _tableRow - The ArrayList<Object> to be filled
	 * @param _columnSettings - the TableViewerColumnSettings object with the positions of the columns in the table row
	 * @param _externalPeak - the peak from the external quant file that matched the precursor
	 * @param sqdp - the StandardQuantDataProcessor object with the list of internal standard peaks 
	 * 
	 * Description: fills in the non-charge-specific external quant stored in the matched external Peak. 
	 */
	public static void fillMassSpecScanDataStandardQuant(Scan a_scan, Peak _externalPeak, ArrayList<Object> _tableRow, 
			TableViewerColumnSettings _columnSettings,  StandardQuantDataProcessor sqdp ) 
	{    	
		try {
			if ( a_scan.getMsLevel() > 1 && a_scan.getPrecursor() != null ) {
				if( sqdp != null && sqdp != null ) {
					List<Peak> peaks = sqdp.getSourcePeakListToMatch();
					if( peaks == null || peaks.isEmpty() ) {
						return;
					}
					Iterator<Peak> itr = peaks.iterator();
					while( itr.hasNext() ) {
						Peak peak = itr.next();	

						Double dMz = peak.getMz();
						String sKey = sqdp.getKeyID() + "-" + dMz.toString();
						String sLabel = dMz.toString();
						if( sqdp.getLabelAlias() != null && ! sqdp.getLabelAlias().equals("") ) {
							sLabel = sqdp.getLabelAlias() + "-" + dMz.toString();
						}
						CustomExtraData cedInt = ExternalQuantColumnInfo.getExternalQuantIntensity(sKey, sLabel);
						CustomExtraData cedIntMz = ExternalQuantColumnInfo.getExternalQuantIntensityMz(sKey, sLabel);
						CustomExtraData cedDeconvoInt = ExternalQuantColumnInfo.getExternalQuantDeconvolutedIntensity(sKey, sLabel);
						if( _externalPeak != null && _externalPeak.getDoubleProp() != null && 
								_externalPeak.getDoubleProp().containsKey(cedIntMz.getKey()) ) {
							int iColPos = _columnSettings.getColumnPosition(cedIntMz.getKey());
							Double dVal = new Double(cedIntMz.getDoubleFormat().format(_externalPeak.getDoubleProp().get(cedIntMz.getKey())));
							MassSpecTableDataProcessorUtil.setRowValue(iColPos, dVal, _tableRow);
						}
						if( _externalPeak != null && _externalPeak.getDoubleProp() != null && 
								_externalPeak.getDoubleProp().containsKey(cedInt.getKey()) ) {
							int iColPos = _columnSettings.getColumnPosition(cedInt.getKey());
							Double dVal = new Double(cedInt.getDoubleFormat().format(_externalPeak.getDoubleProp().get(cedInt.getKey())));
							MassSpecTableDataProcessorUtil.setRowValue(iColPos, dVal, _tableRow);
						}
						if( _externalPeak != null && _externalPeak.getDoubleProp() != null && 
								_externalPeak.getDoubleProp().containsKey(cedDeconvoInt.getKey()) ) {
							int iColPos = _columnSettings.getColumnPosition(cedDeconvoInt.getKey());
							Double dVal = new Double(cedDeconvoInt.getDoubleFormat().format(_externalPeak.getDoubleProp().get(cedDeconvoInt.getKey())));
							MassSpecTableDataProcessorUtil.setRowValue(iColPos, dVal, _tableRow);
						}
					}
				}
			} 
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Row Data for Full MS.", ex);
		}
	}

	/**
	 * @param _columnSettings - the TableViewerColumnSettings object with the positions of the columns in the table row
	 * @param sqdp - the StandardQuantDataProcessor object with the list of internal standard peaks 
	 * @return int - the number of columns added
	 * 
	 * Description: adds the custom annotation data columns to the TableViewerColumnSettings 
	 */
	public static int fillMassSpecColumnSettingsStandardQuantitation( 
			TableViewerColumnSettings _columnSettings, 
			StandardQuantDataProcessor sqdp ) {
		int iNumColumns = 0;
		try {
			if( sqdp != null && sqdp != null ) {
				List<Peak> peaks = sqdp.getSourcePeakListToMatch();
				if( peaks == null || peaks.isEmpty() ) {
					return 0;
				}
				Iterator<Peak> itr = peaks.iterator();
				while( itr.hasNext() ) {
					Peak peak = itr.next();	

					Double dMz = peak.getMz();
					String sKey = sqdp.getKeyID() + "-" + dMz.toString();
					String sLabel = dMz.toString();
					if( sqdp.getLabelAlias() != null && ! sqdp.getLabelAlias().equals("") ) {
						sLabel = sqdp.getLabelAlias() + "-" + dMz.toString();
					}
					CustomExtraData cedInt = ExternalQuantColumnInfo.getExternalQuantIntensity(sKey, sLabel);
					CustomExtraData cedCorInt = ExternalQuantColumnInfo.getExternalQuantCorrectedIntensity(sKey, sLabel);
					CustomExtraData cedIntMz = ExternalQuantColumnInfo.getExternalQuantIntensityMz(sKey, sLabel);
					CustomExtraData cedDeconvoInt = ExternalQuantColumnInfo.getExternalQuantDeconvolutedIntensity(sKey, sLabel);
					CustomExtraData cedRelInt = StandardQuantColumnInfo.getStandardQuantRelativeIntensity(sKey, sLabel);
					_columnSettings.addColumn( cedInt.getLabel(), cedInt.getKey() );				
					_columnSettings.addColumn( cedCorInt.getLabel(), cedCorInt.getKey() );				
					//					_columnSettings.setVisColInx(_columnSettings.getColumnHeader(cedCorInt.getKey()), -1);
					_columnSettings.addColumn( cedIntMz.getLabel(), cedIntMz.getKey() );				
					_columnSettings.addColumn( cedDeconvoInt.getLabel(), cedDeconvoInt.getKey() );				
					_columnSettings.addColumn( cedRelInt.getLabel(), cedRelInt.getKey() );		
					iNumColumns += 5;
				}
			}
			return iNumColumns;
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Column Settings for Special Peaks.", ex);
		}
		return 0;
	}	

	/**
	 * Description: adds the standard quantitation data columns to the TableViewerColumnSettings that were loaded into htKeyToPos.
	 * Once added, the keys are removed. When complete, if any keys remain in htKeyToPos, they are unrecognized headers found in the project
	 * but not in persistent preferences.
	 * 
	 * @param _columnSettings
	 * @param htKeyToPos
	 * @param sqdp
	 * @return
	 */
	public static int fillMassSpecColumnSettingsStandardQuantitation ( 
			TableViewerColumnSettings _columnSettings, 
			HashMap<String, Integer> htKeyToPos,
			StandardQuantDataProcessor sqdp) {
		try {
			int iNumColumns = 0;
			if( sqdp != null && sqdp != null ) {
				List<Peak> peaks = sqdp.getSourcePeakListToMatch();
				if( peaks == null || peaks.isEmpty() ) {
					return 0;
				}
				Iterator<Peak> itr = peaks.iterator();
				while( itr.hasNext() ) {
					Peak peak = itr.next();	

					Double dMz = peak.getMz();
					String sKey = sqdp.getKeyID() + "-" + dMz.toString();
					String sLabel = dMz.toString();
					if( sqdp.getLabelAlias() != null && ! sqdp.getLabelAlias().equals("") ) {
						sLabel = sqdp.getLabelAlias() + "-" + dMz.toString();
					}
					CustomExtraData cedInt = ExternalQuantColumnInfo.getExternalQuantIntensity(sKey, sLabel);
					CustomExtraData cedCorInt = ExternalQuantColumnInfo.getExternalQuantCorrectedIntensity(sKey, sLabel);
					CustomExtraData cedIntMz = ExternalQuantColumnInfo.getExternalQuantIntensityMz(sKey, sLabel);
					CustomExtraData cedDeconvoInt = ExternalQuantColumnInfo.getExternalQuantDeconvolutedIntensity(sKey, sLabel);
					CustomExtraData cedRelInt = StandardQuantColumnInfo.getStandardQuantRelativeIntensity(sKey, sLabel);

					if( htKeyToPos != null && htKeyToPos.containsKey(cedIntMz.getKey() ) ) {
						_columnSettings.putColumn(cedIntMz.getLabel(), 
								cedIntMz.getKey(), 
								htKeyToPos.get(cedIntMz.getKey()));	
						htKeyToPos.remove(cedIntMz.getKey());
						iNumColumns++;
					}	
					if( htKeyToPos != null && htKeyToPos.containsKey(cedInt.getKey() ) ) {
						_columnSettings.putColumn(cedInt.getLabel(), 
								cedInt.getKey(), 
								htKeyToPos.get(cedInt.getKey()));	
						htKeyToPos.remove(cedInt.getKey());
						iNumColumns++;
					}	
					if( htKeyToPos != null && htKeyToPos.containsKey(cedCorInt.getKey() ) ) {
						_columnSettings.putColumn(cedCorInt.getLabel(), 
								cedCorInt.getKey(), 
								htKeyToPos.get(cedCorInt.getKey()));	
						htKeyToPos.remove(cedCorInt.getKey());
						iNumColumns++;
					}	
					if( htKeyToPos != null && htKeyToPos.containsKey(cedDeconvoInt.getKey() ) ) {
						_columnSettings.putColumn(cedDeconvoInt.getLabel(), 
								cedDeconvoInt.getKey(), 
								htKeyToPos.get(cedDeconvoInt.getKey()));	
						htKeyToPos.remove(cedDeconvoInt.getKey());
						iNumColumns++;
					}	
					if( htKeyToPos != null && htKeyToPos.containsKey(cedInt.getKey() ) ) {
						_columnSettings.putColumn(cedRelInt.getLabel(), 
								cedRelInt.getKey(), 
								htKeyToPos.get(cedRelInt.getKey()));	
						htKeyToPos.remove(cedRelInt.getKey());
						iNumColumns++;
					}	
				}
			}
			return iNumColumns;
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Column Settings for Special Peaks Data.", ex);
		}
		return 0;
	}

	/**
	 * @param _columnSettings - the TableViewerColumnSettings object with the positions of the columns in the table row
	 * @param _iMSLevel - the MS level of the current Entry
	 * @param msca - the MassSpecCustomAnnotation object with the custom extra data 
	 * @return int - the number of columns added
	 * 
	 * Description: adds the custom annotation data columns to the TableViewerColumnSettings 
	 */
	public static int fillMassSpecColumnSettingsCustomAnnotation( TableViewerColumnSettings _columnSettings, 
			int _iMSLevel, 	MassSpecCustomAnnotation msca ) {
		int iNumColumns = 0;
		try {
			if( msca != null && msca != null ) {
				Collection<MassSpecCustomAnnotationPeak> annotatedPeaks = msca.getAnnotatedPeaks().values();
				if( annotatedPeaks == null || annotatedPeaks.isEmpty() ) {
					return 0;
				}
				Iterator<MassSpecCustomAnnotationPeak> itr = annotatedPeaks.iterator();
				while( itr.hasNext() ) {
					MassSpecCustomAnnotationPeak mscap = itr.next();	
					Integer iMSLevel = mscap.getMSLevel();
					if( iMSLevel.intValue() != _iMSLevel ) {
						continue;
					}

					Double dMz = mscap.getPeakMz();
					String sKey = dMz.toString();
					String sLabel = dMz.toString();
					if( mscap.getPeakLabel() != null && ! mscap.getPeakLabel().equals("") ) {
						sLabel = sKey + " - " + mscap.getPeakLabel();
					}
					CustomExtraData cedInt = ExternalQuantColumnInfo.getExternalQuantIntensity(sKey, sLabel);
					CustomExtraData cedIntMz = ExternalQuantColumnInfo.getExternalQuantIntensityMz(sKey, sLabel);
					_columnSettings.addColumn( cedInt.getLabel(), cedInt.getKey() );				
					_columnSettings.addColumn( cedIntMz.getLabel(), cedIntMz.getKey() );				
					iNumColumns += 2;
				}
			}
			return iNumColumns;
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Column Settings for Special Peaks.", ex);
		}
		return 0;
	}	

	public static int fillMassSpecColumnSettingsCorrectedPeakScanData( 
			TableViewerColumnSettings _columnSettings, 
			MassSpecEntityProperty msep ) {
		if ( msep.getMsLevel() == 1 ) {
			return 0;
		}

		int iNumCols = 0;
		try {
			MassSpecMetaData metaData = msep.getMassSpecParentProperty().getMassSpecMetaData();
			QuantFileToCorrectedPeaks dataFileQFCP = null;
			if( metaData.getAnnotationFiles() == null || metaData.getQuantFileToCorrectedPeaks() == null ) {
				return 0;
			}
			for( MSPropertyDataFile mspdf : metaData.getAnnotationFiles() ) {
				if( metaData.getQuantFileToCorrectedPeaks().containsKey( mspdf.getName() ) ) {
					dataFileQFCP = metaData.getQuantFileToCorrectedPeaks().get( mspdf.getName() );
					if( dataFileQFCP != null ) {
						if( dataFileQFCP.getPeakTypeToMZs().containsKey(MassSpecPeakIntensityGrid.TYPE_PRE) ) {
							GRITSColumnHeader header = _columnSettings.getColumnHeader(DMPrecursorPeak.precursor_peak_intensity.name());
							CustomExtraData cedCorInt = CorrectedQuantColumnInfo.getCorrectedQuantIntensity(header.getKeyValue(), header.getLabel());								
							_columnSettings.addColumn( cedCorInt.getLabel(), cedCorInt.getKey() );				
							iNumCols++;
						} 
						if( dataFileQFCP.getPeakTypeToMZs().containsKey(MassSpecPeakIntensityGrid.TYPE_PEAK) ) {
							GRITSColumnHeader header = _columnSettings.getColumnHeader(DMPeak.peak_intensity.name());
							CustomExtraData cedCorInt = CorrectedQuantColumnInfo.getCorrectedQuantIntensity(header.getKeyValue(), header.getLabel());								
							_columnSettings.addColumn( cedCorInt.getLabel(), cedCorInt.getKey() );				
							iNumCols++;
						}
					}

				}
			}
		} catch( Exception e ) {
			logger.error("addScanData: error adding scans data to table model.", e);
		}
		return iNumCols;
	}

	public static boolean removeMassSpecScanDataStandardQuantPeakScanData( Peak _quantPeak, StandardQuantDataProcessor qfp  ) {
		boolean bUpdated = false;
		try {
			String sStdQuantKeyId = qfp.getKeyID();
			String sStdQuantLabel = qfp.getLabelAlias();
			for( Peak peak : qfp.getSourcePeakListToMatch() ) {
				Double dStdPeakMz = peak.getMz();
				String sStdPeakKeyId = sStdQuantKeyId + "-" + dStdPeakMz.toString();
				String sStdPeakLabel = dStdPeakMz.toString();
				if( sStdQuantLabel != null && ! sStdQuantLabel.equals("") ) {
					sStdPeakLabel = sStdQuantLabel + "-" + dStdPeakMz.toString();
				}
				String sStdPeakMzKeyId = ExternalQuantColumnInfo.getExternalQuantIntensityMz(sStdPeakKeyId, sStdPeakLabel).getKey();
				if( _quantPeak.getDoubleProp().containsKey(sStdPeakMzKeyId) ) {
					CustomExtraData cedQuantRelInt = StandardQuantColumnInfo.getStandardQuantRelativeIntensity(sStdPeakKeyId, sStdQuantLabel);								
					if( _quantPeak.getDoubleProp().containsKey(cedQuantRelInt.getKey()) ) {
						_quantPeak.getDoubleProp().remove(cedQuantRelInt.getKey());
						bUpdated = true;
					}

				}
			}

		} catch( Exception e ) {
			logger.error("addScanData: error adding scans data to table model.", e);
		}
		return bUpdated;
	}

	private static Double getCorrectedIntensity( MassSpecEntityProperty msep, StandardQuantDataProcessor qfp, Double dStdPeakMz ) {
		try {
			MassSpecMetaData metaData = msep.getMassSpecParentProperty().getMassSpecMetaData();
			MassSpecUISettings msAnnotSettings = msep.getMassSpecUISettings();
			QuantFileToCorrectedPeaks dataFileQFCP = null;
			if( metaData.getQuantFileToCorrectedPeaks() != null ) {
				for( String sFileName : metaData.getQuantFileToCorrectedPeaks().keySet() ) {
					if( qfp.getSettings().getCorrectedFile().getFileName().endsWith(sFileName) ) {
						dataFileQFCP = metaData.getQuantFileToCorrectedPeaks().get( sFileName );
						if( dataFileQFCP != null ) {
							if( dataFileQFCP.getPeakTypeToMZs().containsKey(MassSpecPeakIntensityGrid.TYPE_EXT) ) {
								QuantFilePeaksToCorrectedIntensities qfpci = dataFileQFCP.getPeakTypeToMZs().get(MassSpecPeakIntensityGrid.TYPE_EXT);
								if( qfpci != null && qfpci.getPeakMzToIntensity() != null && qfpci.getPeakMzToIntensity().containsKey(dStdPeakMz) ) {
									// ok, there's corrected intensity in the external quant, but is the file still being used for external quant???
									boolean bOk = false;									
									for( String sType : msAnnotSettings.getQuantTypeToExternalQuant().keySet() ) {
										ExternalQuantFileToAlias eqfa = msAnnotSettings.getQuantTypeToExternalQuant().get(sType);
										if( eqfa != null && eqfa.getSourceDataFileNameToAlias().containsKey(sFileName) ) {
											bOk = true;
											break;
										}
									}
									if( bOk ) {
										Double dCorrectedInt = qfpci.getPeakMzToIntensity().get(dStdPeakMz);
										return dCorrectedInt;
									}
								}
							}
						}
					}
				}
			}
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
	}

	public static boolean fillMassSpecScanDataStandardQuantPeakScanData( Peak _quantPeak, ArrayList<Object> _tableRow, 
			TableViewerColumnSettings _settings, MassSpecEntityProperty msep, StandardQuantDataProcessor qfp  ) {
		boolean bUpdated = false;
		try {
			String sStdQuantKeyId = qfp.getKeyID();
			String sStdQuantLabel = qfp.getLabelAlias();
			for( Peak peak : qfp.getSourcePeakListToMatch() ) {
				Double dStdPeakMz = peak.getMz();
				String sStdPeakKeyId = sStdQuantKeyId + "-" + dStdPeakMz.toString();
				String sStdPeakLabel = dStdPeakMz.toString();
				if( sStdQuantLabel != null && ! sStdQuantLabel.equals("") ) {
					sStdPeakLabel = sStdQuantLabel + "-" + dStdPeakMz.toString();
				}
				String sStdPeakMzKeyId = ExternalQuantColumnInfo.getExternalQuantIntensityMz(sStdPeakKeyId, sStdPeakLabel).getKey();
				if( _quantPeak.getDoubleProp().containsKey(sStdPeakMzKeyId) ) {
					String sStdPeakIntKeyId = ExternalQuantColumnInfo.getExternalQuantIntensity(sStdPeakKeyId, sStdPeakLabel).getKey();
					GRITSColumnHeader stdPeakIntHeader = _settings.getColumnHeader(sStdPeakIntKeyId);
					if( stdPeakIntHeader != null ) {
						Double dStdPeakInt = _quantPeak.getDoubleProp().get(sStdPeakIntKeyId);
						String sStdPeakCorIntKey = ExternalQuantColumnInfo.getExternalQuantCorrectedIntensity(sStdPeakKeyId, sStdPeakLabel).getKey();
						Double dStdPeakCorInt = _quantPeak.getDoubleProp().get(sStdPeakCorIntKey);
						Double dStdPeakRelQuantInt = dStdPeakCorInt != null ? dStdPeakCorInt : dStdPeakInt;			

						QuantFileProcessor quantProc = qfp.getQuantFileProcessor();
						String sQuantKey = quantProc.getKeyID();
						String sQuantLabel = quantProc.getLabelAlias();
						String sQuantPeakIntKey = ExternalQuantColumnInfo.getExternalQuantIntensity(sQuantKey, sQuantLabel).getKey();
						Double dQuantPeakInt = _quantPeak.getDoubleProp().get(sQuantPeakIntKey);							
						if( dQuantPeakInt == null ) {
							continue; // not present
						}
						CustomExtraData cedPeakInt = ExternalQuantColumnInfo.getExternalQuantIntensityMz(sQuantKey, sQuantLabel);
						Double dQuantPeakCorInt = null;
						if( _quantPeak.getDoubleProp().containsKey(cedPeakInt.getKey()) ) {
							Double dFormattedPeakMz = new Double(cedPeakInt.getDoubleFormat().format(_quantPeak.getDoubleProp().get(cedPeakInt.getKey())));						
							dQuantPeakCorInt = getCorrectedIntensity(msep, qfp, dFormattedPeakMz);
						}
						Double dQuantPeakRelQuantInt = dQuantPeakCorInt != null ? dQuantPeakCorInt : dQuantPeakInt;			
						Double dRelInt = dQuantPeakRelQuantInt / dStdPeakRelQuantInt;

						CustomExtraData cedQuantRelInt = StandardQuantColumnInfo.getStandardQuantRelativeIntensity(sStdPeakKeyId, sStdQuantLabel);								
						if( ! _quantPeak.getDoubleProp().containsKey(cedQuantRelInt.getKey()) || 
								Double.compare(_quantPeak.getDoubleProp().get(cedQuantRelInt.getKey()), dRelInt) != 0 ) {
							_quantPeak.getDoubleProp().put(cedQuantRelInt.getKey(), dRelInt);
							bUpdated = true;
						}
						MassSpecTableDataProcessorUtil.setRowValue(_settings.getColumnPosition(cedQuantRelInt.getKey()), 
								new Double(cedQuantRelInt.getDoubleFormat().format(dRelInt)),
								_tableRow);
					}
				}
			}

		} catch( Exception e ) {
			logger.error("addScanData: error adding scans data to table model.", e);
		}
		return bUpdated;
	}

	public static boolean fillMassSpecScanDataCorrectedPeakScanData( Peak _quantPeak, ArrayList<Object> _tableRow, 
			TableViewerColumnSettings _settings, MassSpecEntityProperty msep, QuantFileProcessor qfp  ) {
		boolean bUpdated = false;
		try {
			MassSpecMetaData metaData = msep.getMassSpecParentProperty().getMassSpecMetaData();
			QuantFileToCorrectedPeaks dataFileQFCP = null;
			if( _quantPeak != null && metaData.getQuantFileToCorrectedPeaks() != null ) {
				String sQuantKeyId = qfp.getKeyID();
				String sQuantLabel = qfp.getLabelAlias();
				for( String sFileName : metaData.getQuantFileToCorrectedPeaks().keySet() ) {
					if( qfp.getSettings().getCorrectedFile().getFileName().endsWith(sFileName) ) {
						dataFileQFCP = metaData.getQuantFileToCorrectedPeaks().get( sFileName );
						if( dataFileQFCP != null ) {
							if( qfp instanceof StandardQuantDataProcessor && dataFileQFCP.getPeakTypeToMZs().containsKey(MassSpecPeakIntensityGrid.TYPE_STD) ) {
								QuantFilePeaksToCorrectedIntensities qfpci = dataFileQFCP.getPeakTypeToMZs().get(MassSpecPeakIntensityGrid.TYPE_STD);
								for( Double dStdPeakMz : qfpci.getPeakMzToIntensity().keySet() ) {
									String sStdPeakKeyId = sQuantKeyId + "-" + dStdPeakMz.toString();
									String sStdPeakLabel = dStdPeakMz.toString();
									if( sQuantLabel != null && ! sQuantLabel.equals("") ) {
										sStdPeakLabel = sQuantLabel + "-" + dStdPeakMz.toString();
									}
									String sStdPeakMzKeyId = ExternalQuantColumnInfo.getExternalQuantIntensityMz(sStdPeakKeyId, sStdPeakLabel).getKey();
									if( _quantPeak.getDoubleProp().containsKey(sStdPeakMzKeyId) ) {
										if( qfpci != null && qfpci.getPeakMzToIntensity() != null && qfpci.getPeakMzToIntensity().containsKey(dStdPeakMz) ) {
											String sStdPeakIntKeyId = ExternalQuantColumnInfo.getExternalQuantIntensity(sStdPeakKeyId, sStdPeakLabel).getKey();
											GRITSColumnHeader stdPeakIntHeader = _settings.getColumnHeader(sStdPeakIntKeyId);
											if( stdPeakIntHeader != null ) {
												CustomExtraData cedStdPeakCorInt = CorrectedQuantColumnInfo.getCorrectedQuantIntensity(stdPeakIntHeader.getKeyValue(), stdPeakIntHeader.getLabel());								
												Double dCorrectedInt = qfpci.getPeakMzToIntensity().get(dStdPeakMz);
												if( dCorrectedInt != null ) {
													if( ! _quantPeak.getDoubleProp().containsKey(cedStdPeakCorInt.getKey()) || 
															Double.compare(_quantPeak.getDoubleProp().get(cedStdPeakCorInt.getKey()), dCorrectedInt) != 0 ) {
														_quantPeak.getDoubleProp().put(cedStdPeakCorInt.getKey(), dCorrectedInt);
														bUpdated = true;
													}
													MassSpecTableDataProcessorUtil.setRowValue(_settings.getColumnPosition(cedStdPeakCorInt.getKey()), 
															new Double(cedStdPeakCorInt.getDoubleFormat().format(dCorrectedInt)),
															_tableRow);
												} else {
													_quantPeak.getDoubleProp().remove(cedStdPeakCorInt.getKey());
													MassSpecTableDataProcessorUtil.setRowValue(_settings.getColumnPosition(cedStdPeakCorInt.getKey()), 
															null, _tableRow);
												}
											}
										}
									}
								}								
							} else if ( dataFileQFCP.getPeakTypeToMZs().containsKey(MassSpecPeakIntensityGrid.TYPE_EXT) ) {
								QuantFilePeaksToCorrectedIntensities qfpci = dataFileQFCP.getPeakTypeToMZs().get(MassSpecPeakIntensityGrid.TYPE_EXT);
								CustomExtraData cedPeakInt = ExternalQuantColumnInfo.getExternalQuantIntensityMz(sQuantKeyId, sQuantLabel);
								if( _quantPeak.getDoubleProp().get(cedPeakInt.getKey()) == null ) {
									continue;
								}
								Double dFormattedPeakMz = new Double(cedPeakInt.getDoubleFormat().format(_quantPeak.getDoubleProp().get(cedPeakInt.getKey())));
								if( qfpci != null && qfpci.getPeakMzToIntensity() != null && qfpci.getPeakMzToIntensity().containsKey(dFormattedPeakMz) ) {
									String sStdPeakIntKeyId = ExternalQuantColumnInfo.getExternalQuantIntensity(sQuantKeyId, sQuantLabel).getKey();
									GRITSColumnHeader peakIntHeader = _settings.getColumnHeader(sStdPeakIntKeyId);
									if( peakIntHeader != null ) {
										CustomExtraData cedPeakCorInt = CorrectedQuantColumnInfo.getCorrectedQuantIntensity(peakIntHeader.getKeyValue(), peakIntHeader.getLabel());								
										Double dCorrectedInt = qfpci.getPeakMzToIntensity().get(dFormattedPeakMz);
										if( dCorrectedInt != null ) {
											if( ! _quantPeak.getDoubleProp().containsKey(cedPeakCorInt.getKey()) || 
													Double.compare(_quantPeak.getDoubleProp().get(cedPeakCorInt.getKey()), dCorrectedInt) != 0 ) {
												_quantPeak.getDoubleProp().put(cedPeakCorInt.getKey(), dCorrectedInt);
												bUpdated = true;
											}
											MassSpecTableDataProcessorUtil.setRowValue(_settings.getColumnPosition(cedPeakCorInt.getKey()), 
													new Double(cedPeakCorInt.getDoubleFormat().format(dCorrectedInt)),
													_tableRow);											

										} else {
											_quantPeak.getDoubleProp().remove(cedPeakCorInt.getKey());
											MassSpecTableDataProcessorUtil.setRowValue(_settings.getColumnPosition(cedPeakCorInt.getKey()), 
													null, _tableRow);
											qfpci.getPeakMzToIntensity().remove(dFormattedPeakMz);
											//													qfpci.getPeakMzToIntensity().remove(cedStdPeakCorInt.getKey());
										}
									}
								}
							}
						}
					}

				}
			}
		} catch( Exception e ) {
			logger.error("addScanData: error adding scans data to table model.", e);
		}
		return bUpdated;
	}

	/**
	 * @param _quantPeak
	 * @param _tableRow
	 * @param _settings
	 * @param msep
	 */
	public static boolean fillMassSpecScanDataCorrectedPeakScanData( Peak _quantPeak, ArrayList<Object> _tableRow, 
			TableViewerColumnSettings _settings, MassSpecEntityProperty msep ) {
		boolean bUpdated = false;
		try {
			MassSpecMetaData metaData = msep.getMassSpecParentProperty().getMassSpecMetaData();
			QuantFileToCorrectedPeaks dataFileQFCP = null;
			if( _quantPeak == null || metaData.getAnnotationFiles() == null || metaData.getQuantFileToCorrectedPeaks() == null ) {
				return false;
			}
			for( MSPropertyDataFile mspdf : metaData.getAnnotationFiles() ) {
				if( metaData.getQuantFileToCorrectedPeaks().containsKey( mspdf.getName() ) ) {
					dataFileQFCP = metaData.getQuantFileToCorrectedPeaks().get( mspdf.getName() );
					if( dataFileQFCP != null ) {
						if( dataFileQFCP.getPeakTypeToMZs().containsKey(MassSpecPeakIntensityGrid.TYPE_PRE) ) {
							QuantFilePeaksToCorrectedIntensities qfpci = dataFileQFCP.getPeakTypeToMZs().get(MassSpecPeakIntensityGrid.TYPE_PRE);
							Double dPeakMz = MassSpecTableDataProcessorUtil.getFormattedMZValue(_quantPeak.getPrecursorMz());
							if( qfpci != null && qfpci.getPeakMzToIntensity() != null && qfpci.getPeakMzToIntensity().containsKey(dPeakMz) ) {
								GRITSColumnHeader header = _settings.getColumnHeader(DMPrecursorPeak.precursor_peak_intensity.name());
								if( header != null ) {
									CustomExtraData cedCorInt = CorrectedQuantColumnInfo.getCorrectedQuantIntensity(header.getKeyValue(), header.getLabel());								
									Double dCorrectedInt = qfpci.getPeakMzToIntensity().get(dPeakMz);
									if( dCorrectedInt != null ) {
										if( ! _quantPeak.getDoubleProp().containsKey(cedCorInt.getKey()) || 
												Double.compare(_quantPeak.getDoubleProp().get(cedCorInt.getKey()), dCorrectedInt) != 0 ) {
											_quantPeak.getDoubleProp().put(cedCorInt.getKey(), dCorrectedInt);
											bUpdated = true;
										}
										MassSpecTableDataProcessorUtil.setRowValue(_settings.getColumnPosition(cedCorInt.getKey()), 
												new Double(cedCorInt.getDoubleFormat().format(dCorrectedInt)),
												_tableRow);
									} else {
										_quantPeak.getDoubleProp().remove(cedCorInt.getKey());
										MassSpecTableDataProcessorUtil.setRowValue(_settings.getColumnPosition(cedCorInt.getKey()), 
												null, _tableRow);
										qfpci.getPeakMzToIntensity().remove(dPeakMz);
									}
								}
							}
						} 
						if( dataFileQFCP.getPeakTypeToMZs().containsKey(MassSpecPeakIntensityGrid.TYPE_PEAK) ) {
							QuantFilePeaksToCorrectedIntensities qfpci = dataFileQFCP.getPeakTypeToMZs().get(MassSpecPeakIntensityGrid.TYPE_PEAK);
							Double dPeakMz = MassSpecTableDataProcessorUtil.getFormattedMZValue(_quantPeak.getMz());
							if( qfpci != null && qfpci.getPeakMzToIntensity() != null && qfpci.getPeakMzToIntensity().containsKey(dPeakMz) ) {
								GRITSColumnHeader header = _settings.getColumnHeader(DMPeak.peak_intensity.name());
								if( header != null ) {
									CustomExtraData cedCorInt = CorrectedQuantColumnInfo.getCorrectedQuantIntensity(header.getKeyValue(), header.getLabel());								
									Double dCorrectedInt = qfpci.getPeakMzToIntensity().get(dPeakMz);
									if( dCorrectedInt != null ) {
										if( ! _quantPeak.getDoubleProp().containsKey(cedCorInt.getKey()) || 
												Double.compare(_quantPeak.getDoubleProp().get(cedCorInt.getKey()), dCorrectedInt) != 0 ) {
											_quantPeak.getDoubleProp().put(cedCorInt.getKey(), dCorrectedInt);
											bUpdated = true;
										}
										MassSpecTableDataProcessorUtil.setRowValue(_settings.getColumnPosition(cedCorInt.getKey()), 
												new Double(cedCorInt.getDoubleFormat().format(dCorrectedInt)),
												_tableRow);
									} else {
										_quantPeak.getDoubleProp().remove(cedCorInt.getKey());
										MassSpecTableDataProcessorUtil.setRowValue(_settings.getColumnPosition(cedCorInt.getKey()), 
												null, _tableRow);
										qfpci.getPeakMzToIntensity().remove(dPeakMz);										
									}
								}
							}
						}
					}
				}
			}
		} catch( Exception e ) {
			logger.error("addScanData: error adding scans data to table model.", e);
		}
		return bUpdated;

	}

	/**
	 * Description: adds the custom annotation data columns to the TableViewerColumnSettings that were loaded into htKeyToPos.
	 * Once added, the keys are removed. When complete, if any keys remain in htKeyToPos, they are unrecognized headers found in the project
	 * but not in persistent preferences.
	 * 
	 * @param _columnSettings - the TableViewerColumnSettings object with the positions of the columns in the table row
	 * @param htKeyToPos - HashMap<String, Integer> that maps the External Quant label to its column position
	 * @param msca - the MassSpecCustomAnnotation object with the custom extra data 
	 * @return int - the number of columns added
	 */
	public static int fillMassSpecColumnSettingsCustomAnnotation( 
			TableViewerColumnSettings _columnSettings, 
			HashMap<String, Integer> htKeyToPos,
			int _iMSLevel, 
			MassSpecCustomAnnotation msca) {
		try {
			int iNumColumns = 0;
			if( msca != null && msca != null ) {
				Collection<MassSpecCustomAnnotationPeak> annotatedPeaks = msca.getAnnotatedPeaks().values();
				if( annotatedPeaks == null || annotatedPeaks.isEmpty() ) {
					return 0;
				}
				Iterator<MassSpecCustomAnnotationPeak> itr = annotatedPeaks.iterator();
				while( itr.hasNext() ) {
					MassSpecCustomAnnotationPeak mscap = itr.next();				
					Integer iMSLevel = mscap.getMSLevel();
					if( iMSLevel.intValue() != _iMSLevel ) {
						continue;
					}
					Double dMz = mscap.getPeakMz();
					String sKey = dMz.toString();
					String sLabel = dMz.toString();
					if( mscap.getPeakLabel() != null && ! mscap.getPeakLabel().equals("") ) {
						sLabel = sKey + " - " + mscap.getPeakLabel();
					}

					CustomExtraData cedInt = ExternalQuantColumnInfo.getExternalQuantIntensity(sKey, sLabel);
					CustomExtraData cedIntMz = ExternalQuantColumnInfo.getExternalQuantIntensityMz(sKey, sLabel);
					if( htKeyToPos != null && htKeyToPos.containsKey(cedIntMz.getKey() ) ) {
						_columnSettings.putColumn(cedIntMz.getLabel(), 
								cedIntMz.getKey(), 
								htKeyToPos.get(cedIntMz.getKey()));	
						htKeyToPos.remove(cedIntMz.getKey());
						iNumColumns++;
					}	
					if( htKeyToPos != null && htKeyToPos.containsKey(cedInt.getKey() ) ) {
						_columnSettings.putColumn(cedInt.getLabel(), 
								cedInt.getKey(), 
								htKeyToPos.get(cedInt.getKey()));
						htKeyToPos.remove(cedInt.getKey());
						iNumColumns++;
					}	
				}
			}
			return iNumColumns;
		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Column Settings for Special Peaks Data.", ex);
		}
		return 0;
	}

	/**
	 * @param _precursorPeak - Peak object from the parent scan
	 * @param _subscanPeak - Peak object from the sub scan being annotated
	 * @param _tableRow - The ArrayList<Object> to be filled
	 * @param _columnSettings - the TableViewerColumnSettings object with the positions of the columns in the table row
	 * @param hmAnnotatedPeaks
	 * @return boolean - whether or not data were added/updated in the table.
	 * 
	 * Description: adds the custom annotation data columns to the TableViewerColumnSettings that were loaded into htKeyToPos.
	 * Once added, the keys are removed. When complete, if any keys remain in htKeyToPos, they are unrecognized headers found in the project
	 * but not in persistent preferences.
	 */
	public static boolean fillMassSpecScanDataCustomAnnotation(Peak _precursorPeak, Peak _subscanPeak, 
			ArrayList<Object> _tableRow, 
			TableViewerColumnSettings _columnSettings, HashMap<Double, MassSpecCustomAnnotationPeak> hmAnnotatedPeaks ) {
		boolean bUpdated = false;
		try {
			if( _subscanPeak == null || _subscanPeak.getDoubleProp() == null ) {
				return false;
			}
			if( _subscanPeak.getDoubleProp().isEmpty() ) {
				return false;
			}
			if( hmAnnotatedPeaks == null ) {
				return false;
			}
			Collection<MassSpecCustomAnnotationPeak> annotatedPeaks = hmAnnotatedPeaks.values();
			if( annotatedPeaks == null || annotatedPeaks.isEmpty() ) {
				return false;
			}
			Iterator<MassSpecCustomAnnotationPeak> itr = annotatedPeaks.iterator();
			while( itr.hasNext() ) {
				MassSpecCustomAnnotationPeak mscap = itr.next();				
				Double dMz = mscap.getPeakMz();
				String sKey = dMz.toString();
				String sLabel = dMz.toString();
				if( mscap.getPeakLabel() != null && ! mscap.getPeakLabel().equals("") ) {
					sLabel = sKey + " - " + mscap.getPeakLabel();
				}
				CustomExtraData cedInt = ExternalQuantColumnInfo.getExternalQuantIntensity(sKey, sLabel);
				CustomExtraData cedIntMz = ExternalQuantColumnInfo.getExternalQuantIntensityMz(sKey, sLabel);
				if( _subscanPeak == null || _subscanPeak.getDoubleProp() == null || _subscanPeak.getDoubleProp().isEmpty() ) {
					continue;
				}
				Double dMatchedMz = _subscanPeak.getDoubleProp().get(cedIntMz.getKey());
				Double dMatchedInt = _subscanPeak.getDoubleProp().get(cedInt.getKey());
				if( dMatchedMz != null && dMatchedInt != null && _subscanPeak.getMz().equals(dMatchedMz) ) {
					bUpdated = true;
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(cedIntMz.getKey()), 
							new Double(cedIntMz.getDoubleFormat().format(dMatchedMz)),
							_tableRow);
					_precursorPeak.addDoubleProp(cedIntMz.getKey(), dMatchedMz);
					MassSpecTableDataProcessorUtil.setRowValue(_columnSettings.getColumnPosition(cedInt.getKey()), 
							new Double(cedInt.getDoubleFormat().format(dMatchedInt)),
							_tableRow);
					_precursorPeak.addDoubleProp(cedInt.getKey(), dMatchedInt);
				}
			}


		} catch( Exception ex ) {
			logger.error("Error filling Mass Spec Row Data for Peak List with Features Special Peaks.", ex);
		}
		return bUpdated;
	}

	/**
	 * @param iColNum - int value of the column number to fill the data
	 * @param headerVal - a GRITSColumnHeader object that is added
	 * @param alList - the ArrayList<GRITSColumnHeader> representing the entire header
	 * 
	 * Description: sets the header value at positing "iColNum" with header "headerVal" in list of headers "alList"
	 */
	public static void setHeaderValue( int iColNum, GRITSColumnHeader headerVal, ArrayList<GRITSColumnHeader> alList ) {
		try {
			if ( headerVal == null )
				return;

			if ( alList == null ) 
				alList = new ArrayList<GRITSColumnHeader>();

			if ( alList.size() <= iColNum ) {
				// must empty fill
				for ( int i = alList.size(); i <= iColNum; i++ ) {
					alList.add(new GRITSColumnHeader("", ""));
				}
			}
			// now put in list
			// do we need a decimal formatter?
			alList.set(iColNum, headerVal);            	
		} catch( Exception ex ) {
			logger.error("Error setting header value.", ex);
		}
	}

	/**
	 * @param iColNum - int value of the column number to fill the data
	 * @param objVal - some object that is added
	 * @param alList - the ArrayList<Object> representing the entire row of data
	 * 
	 * Description: sets the data value at positing "iColNum" with data "objVal" in list of row values "alList"
	 */
	public static void setRowValue( int iColNum, Object objVal, ArrayList<Object> alList ) {
		try {
			if ( iColNum < 0 )
				return;
			if ( objVal == null )
				return;

			if ( alList == null ) 
				alList = new ArrayList();

			if ( alList.size() <= iColNum ) {
				// must empty fill
				for ( int i = alList.size(); i <= iColNum; i++ ) {
					alList.add("");
				}
			}
			// now put in list
			// do we need a decimal formatter?
			if ( objVal instanceof Boolean ) {
				alList.set(iColNum, (Boolean) objVal ? "Yes" : "No" );
			} else {
				alList.set(iColNum, objVal);        
			}
		} catch( Exception ex ) {
			logger.error("Error setting row value.", ex);
		}
	}

	protected static void errorMessage(String a_message)
	{
		logger.error(a_message);    
	}

}
