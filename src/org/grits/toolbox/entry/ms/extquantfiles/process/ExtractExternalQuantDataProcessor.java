package org.grits.toolbox.entry.ms.extquantfiles.process;

import org.apache.log4j.Logger;
import org.grits.toolbox.ms.file.extquant.data.ExternalQuantSettings;
import org.grits.toolbox.ms.file.extquant.data.QuantPeakMatch;
import org.grits.toolbox.ms.om.data.Peak;

/**
 * @author D. Brent Weatherly (dbrentw@uga.edu)
 *
 */
public class ExtractExternalQuantDataProcessor extends ExtractDataProcessor {
	//log4J Logger
	private static final Logger logger = Logger.getLogger(ExtractExternalQuantDataProcessor.class);

	public ExtractExternalQuantDataProcessor(ExternalQuantSettings a_parameter) {
		super( a_parameter);
	}	

	@Override
	public boolean setExternalPeakData(Peak a_peak, QuantPeakMatch cPeak) {
		String sKey = QuantFileProcessor.getExternalQuantProcessorKey(getKeyID(), ExtractDataProcessor.DEFAULT_KEY);
		String sLabel = QuantFileProcessor.getExternalQuantProcessorLabel(getLabelAlias(), ExtractDataProcessor.DEFAULT_LABEL);
		String sDeconvoIntKey = ExternalQuantColumnInfo.getExternalQuantDeconvolutedIntensity(sKey, sLabel).getKey();
		String sIntKey = ExternalQuantColumnInfo.getExternalQuantIntensity(sKey, sLabel).getKey();
		String sMzKey = ExternalQuantColumnInfo.getExternalQuantIntensityMz(sKey, sLabel).getKey();
		String sChargeKey = ExternalQuantColumnInfo.getExternalQuantCharge(sKey, sLabel).getKey();
		if( cPeak == null ) {
			a_peak.getDoubleProp().remove( sDeconvoIntKey );
			a_peak.getDoubleProp().remove( sIntKey );
			a_peak.getDoubleProp().remove( sMzKey );
			a_peak.getIntegerProp().remove( sChargeKey );
			return true;
		}
		boolean bAdd = true;
		if( a_peak.getDoubleProp().containsKey(sDeconvoIntKey) && 
				a_peak.getDoubleProp().containsKey(sIntKey) &&
				a_peak.getDoubleProp().containsKey(sMzKey) &&
				a_peak.getDoubleProp().containsKey(sChargeKey) ) {
			bAdd = false;
			Double dVal1 = a_peak.getDoubleProp().get(sDeconvoIntKey);
			Double dVal2 = cPeak.getParent().getSumIntensity();
			if( dVal2 != null && (dVal1 == null || Double.compare(dVal1, dVal2 ) != 0) ) {
				bAdd = true;
			}
			dVal1 = a_peak.getDoubleProp().get(sIntKey);
			dVal2 = cPeak.getIntensitySum() ;
			if( dVal2 != null && (dVal1 == null || Double.compare(dVal1, dVal2 ) != 0) ) {
				bAdd = true;
			}
			dVal1 = a_peak.getDoubleProp().get(sMzKey);
			dVal2 = cPeak.getMzMostAbundant();
			if( dVal2 != null && (dVal1 == null || Double.compare(dVal1, dVal2 ) != 0) ) {
				bAdd = true;
			}

			Integer iVal1 = a_peak.getIntegerProp().get(sChargeKey);
			Integer iVal2 = cPeak.getCharge();
			if( iVal2 != null && (iVal1 == null || iVal1.compareTo(iVal2) != 0) ) {
				bAdd = true;
			}

		}
		if( bAdd ) {
			a_peak.addDoubleProp( sDeconvoIntKey, cPeak.getParent().getSumIntensity());
			a_peak.addDoubleProp( sIntKey, cPeak.getIntensitySum());
			a_peak.addDoubleProp( sMzKey, cPeak.getMzMostAbundant());
			a_peak.addIntegerProp( sChargeKey, cPeak.getCharge());
		}
		return bAdd;
	}

}
