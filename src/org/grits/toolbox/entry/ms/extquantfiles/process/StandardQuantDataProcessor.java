package org.grits.toolbox.entry.ms.extquantfiles.process;

import org.apache.log4j.Logger;
import org.grits.toolbox.ms.file.extquant.data.QuantPeakMatch;
import org.grits.toolbox.ms.om.data.CustomExtraData;
import org.grits.toolbox.ms.om.data.Peak;

/**
 * @author D. Brent Weatherly (dbrentw@uga.edu)
 *
 */
public class StandardQuantDataProcessor extends QuantFileProcessor {
	//log4J Logger
	private static final Logger logger = Logger.getLogger(StandardQuantDataProcessor.class);
	protected QuantFileProcessor qfProcessor;

	public StandardQuantDataProcessor(QuantFileProcessor qfProcessor) {
		super(qfProcessor.getSettings());
		this.qfProcessor = qfProcessor;
	}

	public QuantFileProcessor getQuantFileProcessor() {
		return qfProcessor;
	}


	@Override
	public void loadExternalData() {
		qfProcessor.loadExternalData();
		setQuantPeakData(qfProcessor.getQuantPeakData());
	}

	@Override
	public boolean matchExternalPeaks(boolean _bRequirePrecursorPeak) {
		//		String sKey = QuantFileProcessor.getExternalQuantProcessorKey(getKeyID(), ExtractDataProcessor.DEFAULT_KEY);
		//		String sLabel = QuantFileProcessor.getExternalQuantProcessorLabel(getLabelAlias(), ExtractDataProcessor.DEFAULT_LABEL);
		String sKey = qfProcessor.getKeyID();
		String sLabel = qfProcessor.getLabelAlias();
		String sIntKey = ExternalQuantColumnInfo.getExternalQuantIntensity(sKey, sLabel).getKey();
		// read
		boolean bReadStandards = super.matchExternalPeaks(false);
		//		boolean bHasStandards = getSourcePeakListToMatch() != null && ! getSourcePeakListToMatch().isEmpty();
		boolean bUpdated = false;
		if( bReadStandards && qfProcessor.getSourcePeakListToMatch() != null ) {
			for( Peak precursorPeak : qfProcessor.getSourcePeakListToMatch() ) {
				if( ! _bRequirePrecursorPeak || precursorPeak.getIsPrecursor() ) {
					QuantPeakMatch precursorQPM = qfProcessor.findExternalPeak(precursorPeak.getMz());
					if( precursorQPM != null ) {
						// these peaks should have been filled with intensity info from the above call to super.matchExternalPeaks(..)
						for( Peak standardPeak : getSourcePeakListToMatch() ) {
							if( standardPeak.getDoubleProp().containsKey(sIntKey) ) { // intensity of the standard
								bUpdated |= setStandardQuantPeakData(precursorPeak, precursorQPM, standardPeak);		
							}
						}

					}
				}
			}
		}
		return bUpdated;
	}

	@Override
	public boolean removeExternalPeakMatches(boolean _bRequirePrecursorPeak) {
		boolean bUpdated = false;
		String sKey = qfProcessor.getKeyID();
		String sLabel = qfProcessor.getLabelAlias();
		String sIntKey = ExternalQuantColumnInfo.getExternalQuantIntensity(sKey, sLabel).getKey();
		if( qfProcessor.getSourcePeakListToMatch() != null ) {
			for( Peak precursorPeak : qfProcessor.getSourcePeakListToMatch() ) {
				if( ! _bRequirePrecursorPeak || precursorPeak.getIsPrecursor() ) {
					QuantPeakMatch precursorQPM = qfProcessor.findExternalPeak(precursorPeak.getMz());
					if( precursorQPM != null ) {
						// these peaks should have been filled with intensity info from the above call to super.matchExternalPeaks(..)
						for( Peak standardPeak : getSourcePeakListToMatch() ) {
							if( standardPeak.getDoubleProp().containsKey(sIntKey) ) { // intensity of the standard
								bUpdated |= setStandardQuantPeakData(precursorPeak, null, standardPeak);		
							}
						}

					}
				}
			}
		}
		return bUpdated;
	}

	@Override
	public void generateAllMatches() {
		qfProcessor.generateAllMatches();
	}

	@Override
	public boolean setExternalPeakData(Peak a_peak, QuantPeakMatch cPeak) {
		return qfProcessor.setExternalPeakData(a_peak, cPeak);
	}
	
	@Override
	public boolean matchesExternalQuantKey(String sExtQuantFullKey) {
		String sSourceKey = getKeyID();
		int inx = sExtQuantFullKey.indexOf(sSourceKey);
		if( inx >= 0 ) {
			String theRest = sExtQuantFullKey.substring(sSourceKey.length() + 1);
			int inx2 = theRest.indexOf("_");
			if( inx2 >= 0 ) {
				String sMz = theRest.substring(0, inx2);
				double dMz = 0.0;
				try {
					dMz = Double.parseDouble(sMz);
				} catch(NumberFormatException ex) {
					;
				}
				for( int i = 0; i < getSourcePeakListToMatch().size(); i++ ) {
					Peak peak = getSourcePeakListToMatch().get(i);
					if( peak.getMz() == dMz ) {
						return true;
					}
				}
				return false;
			}
		} 
		return false;
	}
	

	public boolean setStandardQuantPeakData(Peak a_peak, QuantPeakMatch precursorQPM, Peak dStdPeak) {
		String sSourceKey = qfProcessor.getKeyID();
		String sSourceLabel = qfProcessor.getLabelAlias();
		String sSourceIntKey = ExternalQuantColumnInfo.getExternalQuantIntensity(sSourceKey, sSourceLabel).getKey();
		String sSourceDeconvoIntKey = ExternalQuantColumnInfo.getExternalQuantDeconvolutedIntensity(sSourceKey, sSourceLabel).getKey();
		String sSourceMzKey = ExternalQuantColumnInfo.getExternalQuantIntensityMz(sSourceKey, sSourceLabel).getKey();

		if ( ! dStdPeak.getDoubleProp().containsKey(sSourceIntKey) ) {
			return false;
		}

		Double dStdIntensity = dStdPeak.getDoubleProp().get(sSourceIntKey);
		Double dStdMz = dStdPeak.getDoubleProp().get(sSourceMzKey);
		Double dStdDeconvoIntensity = dStdPeak.getDoubleProp().get(sSourceDeconvoIntKey);

		Double dMz = dStdPeak.getMz();
		String sKey = getKeyID() + "-" + dMz.toString();
		String sLabel = dMz.toString();
		if( getLabelAlias() != null && ! getLabelAlias().equals("") ) {
			sLabel = getLabelAlias() + "-" + dMz.toString();
		}
		String sDeconvoIntKey = ExternalQuantColumnInfo.getExternalQuantDeconvolutedIntensity(sKey, sLabel).getKey();
		String sIntKey = ExternalQuantColumnInfo.getExternalQuantIntensity(sKey, sLabel).getKey();
		String sMzKey = ExternalQuantColumnInfo.getExternalQuantIntensityMz(sKey, sLabel).getKey();
		String sCorrIntKey = ExternalQuantColumnInfo.getExternalQuantCorrectedIntensity(sKey, sLabel).getKey();
		String sRelIntKey = StandardQuantColumnInfo.getStandardQuantRelativeIntensity(sKey, sLabel).getKey();

		if ( precursorQPM == null ) {
			a_peak.getDoubleProp().remove( sDeconvoIntKey );
			a_peak.getDoubleProp().remove( sIntKey );
			a_peak.getDoubleProp().remove( sMzKey );
			a_peak.getDoubleProp().remove(sCorrIntKey);
			a_peak.getDoubleProp().remove(sRelIntKey);
			a_peak.getDoubleProp().remove(sSourceDeconvoIntKey);
			a_peak.getDoubleProp().remove(sSourceIntKey);
			a_peak.getDoubleProp().remove(sSourceMzKey);
			return true;			
		}
		boolean bAddSource = true;
		if( a_peak.getDoubleProp().containsKey(sSourceDeconvoIntKey) && 
				a_peak.getDoubleProp().containsKey(sSourceIntKey) &&
				a_peak.getDoubleProp().containsKey(sSourceMzKey) ) {
			Double dVal1 = a_peak.getDoubleProp().get(sSourceDeconvoIntKey);
			Double dVal2 = precursorQPM.getParent().getSumIntensity();
			bAddSource = false;
			if( dVal2 != null && (dVal1 == null || Double.compare(dVal1, dVal2 ) != 0) ) {
				bAddSource = true;
			}
			dVal1 = a_peak.getDoubleProp().get(sSourceIntKey);
			dVal2 = precursorQPM.getIntensitySum() ;
			if( dVal2 != null && (dVal1 == null || Double.compare(dVal1, dVal2 ) != 0) ) {
				bAddSource = true;
			}
			dVal1 = a_peak.getDoubleProp().get(sSourceMzKey);
			dVal2 = precursorQPM.getMzMostAbundant();
			if( dVal2 != null && (dVal1 == null || Double.compare(dVal1, dVal2 ) != 0) ) {
				bAddSource = true;
			}
		}
		if( bAddSource ) {
			a_peak.addDoubleProp( sSourceDeconvoIntKey, precursorQPM.getParent().getSumIntensity());
			a_peak.addDoubleProp( sSourceIntKey, precursorQPM.getIntensitySum());
			a_peak.addDoubleProp( sSourceMzKey, precursorQPM.getMzMostAbundant());
		}
		boolean bAddStd = true;
		if( a_peak.getDoubleProp().containsKey(sDeconvoIntKey) && 
				a_peak.getDoubleProp().containsKey(sIntKey) &&
				a_peak.getDoubleProp().containsKey(sMzKey) ) {
			bAddStd = false;
			Double dVal1 = a_peak.getDoubleProp().get(sSourceDeconvoIntKey);
			Double dVal2 = precursorQPM.getParent().getSumIntensity();
			if( dVal2 != null && (dVal1 == null || Double.compare(dVal1, dVal2 ) != 0) ) {
				bAddStd = true;
			}
			dVal1 = a_peak.getDoubleProp().get(sSourceIntKey);
			dVal2 = precursorQPM.getIntensitySum() ;
			if( dVal2 != null && (dVal1 == null || Double.compare(dVal1, dVal2 ) != 0) ) {
				bAddStd = true;
			}
			dVal1 = a_peak.getDoubleProp().get(sSourceMzKey);
			dVal2 = precursorQPM.getMzMostAbundant();
			if( dVal2 != null && (dVal1 == null || Double.compare(dVal1, dVal2 ) != 0) ) {
				bAddStd = true;
			}
		}
		if( bAddStd ) {
			a_peak.addDoubleProp( sDeconvoIntKey, dStdDeconvoIntensity);
			a_peak.addDoubleProp( sIntKey, dStdIntensity);
			a_peak.addDoubleProp( sMzKey, dStdMz);
		}

		return (bAddSource || bAddStd);
	}
}

