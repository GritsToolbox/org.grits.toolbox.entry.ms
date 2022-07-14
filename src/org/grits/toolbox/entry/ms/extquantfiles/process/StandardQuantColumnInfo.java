package org.grits.toolbox.entry.ms.extquantfiles.process;

import org.grits.toolbox.ms.om.data.CustomExtraData;

/**
 * @author D. Brent Weatherly (dbrentw@uga.edu)
 *
 */
public class StandardQuantColumnInfo
{
	public static String RELATIVE_INTENSITY_KEY_POSTFIX = QuantFileProcessor.QUANT_LABEL_PREFIX + "_relative_quant";
	public static String RELATIVE_INTENSITY_LABEL_POSTFIX = " Relative Quantitation";
	public static String QUANT_PREFIX = "internal";
	
	public static CustomExtraData getDefaultStandardQuantCEDFromKey( String sDefaultKey, String sDefaultLabel, String _sPrevFullKey ) {
		int iInx = _sPrevFullKey.indexOf(QuantFileProcessor.QUANT_LABEL_PREFIX);
		String sEnding = _sPrevFullKey.substring(iInx);
		if( sEnding.equals(ExternalQuantColumnInfo.TOTAL_INTENSITY_KEY_POSTFIX) ) {
			return ExternalQuantColumnInfo.getExternalQuantDeconvolutedIntensity(sDefaultKey, sDefaultLabel);
		} else if( sEnding.equals(ExternalQuantColumnInfo.INTENSITY_KEY_POSTFIX) ) {
			return ExternalQuantColumnInfo.getExternalQuantIntensity(sDefaultKey, sDefaultLabel);
		} else if( sEnding.equals(ExternalQuantColumnInfo.MZ_KEY_POSTFIX) ) {
			return ExternalQuantColumnInfo.getExternalQuantIntensityMz(sDefaultKey, sDefaultLabel);
		} else if( sEnding.equals(RELATIVE_INTENSITY_LABEL_POSTFIX) ) {
			return StandardQuantColumnInfo.getStandardQuantRelativeIntensity(sDefaultKey, sDefaultLabel);
		} 
		return null;
	}
	
	public static CustomExtraData getStandardQuantRelativeIntensity( String _sKeyPrefix, String _sLabelPrefix ) { 
		return new CustomExtraData( _sKeyPrefix + RELATIVE_INTENSITY_KEY_POSTFIX, _sLabelPrefix + RELATIVE_INTENSITY_LABEL_POSTFIX, 
				"Generic Method", CustomExtraData.Type.Double, "0.0000" );
	}
}
