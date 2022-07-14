package org.grits.toolbox.entry.ms.extquantfiles.process;

import org.grits.toolbox.ms.om.data.CustomExtraData;

/**
 * @author D. Brent Weatherly (dbrentw@uga.edu)
 *
 */
public class CorrectedQuantColumnInfo {
	public static String CORRECTED_QUANT_KEY_PREFIX = "_corrected";
	public static String CORRECTED_QUANT_LABEL_PREFIX = " Corrected";
	
	public static CustomExtraData getDefaultExternalQuantCEDFromKey( String sDefaultKey, String sDefaultLabel, String _sPrevFullKey ) {
		int iInx = _sPrevFullKey.indexOf(QuantFileProcessor.QUANT_LABEL_PREFIX);
		String sEnding = _sPrevFullKey.substring(iInx);
		if( sEnding.equals(ExternalQuantColumnInfo.INTENSITY_KEY_POSTFIX + CorrectedQuantColumnInfo.CORRECTED_QUANT_KEY_PREFIX) ) {
			return CorrectedQuantColumnInfo.getCorrectedQuantIntensity(sDefaultKey, sDefaultLabel);
		} 		 
		return null;
	}	

	public static CustomExtraData getCorrectedQuantIntensity( String _sKeyPrefix, String _sLabelPrefix ) { 
		CustomExtraData ced =  new CustomExtraData( _sKeyPrefix + CORRECTED_QUANT_KEY_PREFIX, 
				_sLabelPrefix + CORRECTED_QUANT_LABEL_PREFIX, 
				"Generic Method", CustomExtraData.Type.Double, "0.0" );
		ced.setDoubleFormat("0.0");
		return ced;
	}
	
}
