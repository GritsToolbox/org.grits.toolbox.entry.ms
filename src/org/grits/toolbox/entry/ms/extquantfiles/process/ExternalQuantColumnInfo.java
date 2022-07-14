package org.grits.toolbox.entry.ms.extquantfiles.process;

import org.grits.toolbox.ms.om.data.CustomExtraData;

/**
 * @author D. Brent Weatherly (dbrentw@uga.edu)
 *
 */
public class ExternalQuantColumnInfo
{
	public static String TOTAL_INTENSITY_KEY_POSTFIX = QuantFileProcessor.QUANT_LABEL_PREFIX + "_total_intensity";
	public static String TOTAL_INTENSITY_LABEL_POSTFIX = " Total Intensity";
	public static String INTENSITY_KEY_POSTFIX = QuantFileProcessor.QUANT_LABEL_PREFIX + "_intensity";
	public static String INTENSITY_LABEL_POSTFIX = " Intensity";
	public static String MZ_KEY_POSTFIX = QuantFileProcessor.QUANT_LABEL_PREFIX + "_mz";
	public static String MZ_LABEL_POSTFIX = " m/z";
	public static String CHARGE_KEY_POSTFIX = QuantFileProcessor.QUANT_LABEL_PREFIX + "_charge";
	public static String CHARGE_LABEL_POSTFIX = " Charge";
	public static String QUANT_PREFIX = "external";
	
	public static CustomExtraData getDefaultExternalQuantCEDFromKey( String sDefaultKey, String sDefaultLabel, String _sPrevFullKey ) {
		int iInx = _sPrevFullKey.indexOf(QuantFileProcessor.QUANT_LABEL_PREFIX);
		String sEnding = _sPrevFullKey.substring(iInx);
		if( sEnding.equals(TOTAL_INTENSITY_KEY_POSTFIX) ) {
			return ExternalQuantColumnInfo.getExternalQuantDeconvolutedIntensity(sDefaultKey, sDefaultLabel);
		} else if( sEnding.equals(INTENSITY_KEY_POSTFIX) ) {
			return ExternalQuantColumnInfo.getExternalQuantIntensity(sDefaultKey, sDefaultLabel);
		} else if( sEnding.equals(MZ_KEY_POSTFIX) ) {
			return ExternalQuantColumnInfo.getExternalQuantIntensityMz(sDefaultKey, sDefaultLabel);
		} else if( sEnding.equals(CHARGE_KEY_POSTFIX) ) {
			return ExternalQuantColumnInfo.getExternalQuantCharge(sDefaultKey, sDefaultLabel);
		} 
		return null;
	}
	
	public static CustomExtraData getExternalQuantDeconvolutedIntensity( String _sKeyPrefix, String _sLabelPrefix ) { 
		CustomExtraData ced = new CustomExtraData( _sKeyPrefix + TOTAL_INTENSITY_KEY_POSTFIX, 
				_sLabelPrefix + TOTAL_INTENSITY_LABEL_POSTFIX, 
				"Generic Method", CustomExtraData.Type.Double, "0.0" );
		ced.setDoubleFormat("0.0");
		return ced;
	}

	public static CustomExtraData getExternalQuantIntensity( String _sKeyPrefix, String _sLabelPrefix ) { 
		CustomExtraData ced =  new CustomExtraData( _sKeyPrefix + INTENSITY_KEY_POSTFIX, _sLabelPrefix + INTENSITY_LABEL_POSTFIX, 
				"Generic Method", CustomExtraData.Type.Double, "0.0" );
		ced.setDoubleFormat("0.0");
		return ced;
	}

	public static CustomExtraData getExternalQuantCorrectedIntensity( String _sKeyPrefix, String _sLabelPrefix ) { 
		return CorrectedQuantColumnInfo.getCorrectedQuantIntensity(_sKeyPrefix + INTENSITY_KEY_POSTFIX, _sLabelPrefix + INTENSITY_LABEL_POSTFIX);
	}
	
	public static CustomExtraData getExternalQuantIntensityMz( String _sKeyPrefix, String _sLabelPrefix ) { 
		CustomExtraData ced =  new CustomExtraData( _sKeyPrefix + MZ_KEY_POSTFIX, _sLabelPrefix + MZ_LABEL_POSTFIX, 
				"Generic Method", CustomExtraData.Type.Double, "0.0000" );
		ced.setDoubleFormat("0.000");
		return ced;
	}

	public static CustomExtraData getExternalQuantCharge( String _sKeyPrefix, String _sLabelPrefix ) { 
		return new CustomExtraData( _sKeyPrefix + CHARGE_KEY_POSTFIX, _sLabelPrefix + CHARGE_LABEL_POSTFIX, 
				"Generic Method", CustomExtraData.Type.Integer );
	}
}
