package org.grits.toolbox.entry.ms.extquantfiles.process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecCustomAnnotation;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecCustomAnnotationPeak;
import org.grits.toolbox.ms.file.extquant.data.ComparatorQuantPeakMatch;
import org.grits.toolbox.ms.file.extquant.data.ExternalQuantSettings;
import org.grits.toolbox.ms.file.extquant.data.QuantPeak;
import org.grits.toolbox.ms.file.extquant.data.QuantPeakData;
import org.grits.toolbox.ms.file.extquant.data.QuantPeakMatch;
import org.grits.toolbox.ms.file.reader.impl.InvalidFileFormatException;
import org.grits.toolbox.ms.om.data.CustomExtraData;
import org.grits.toolbox.ms.om.data.Peak;
import org.grits.toolbox.ms.om.data.Scan;

/**
 * @author D. Brent Weatherly (dbrentw@uga.edu)
 *
 */
public class CustomAnnotationDataProcessor extends QuantFileProcessor
{
	//log4J Logger
	private static final Logger logger = Logger.getLogger(CustomAnnotationDataProcessor.class);
	protected MassSpecCustomAnnotation msca = null;
	protected Scan curScan = null;

	protected HashMap<Integer, List<Peak>> hmPeakList = null; // map of ms level to peaks

	/**
	 * @param a_parameter - ExternalQuantSettings parameter file specific for CustomAnnotation
	 */
	public CustomAnnotationDataProcessor(ExternalQuantSettings a_parameter) {
		super(a_parameter);
	}
	
	/**
	 * Iterates through the list of custom annotated peaks associated with "MassSpecCustomAnnotation" member variable and 
	 * adds them to the "HashMap<Integer, List<Peak>>" member variable thereby mapping annotated peaks to MS level
	 */
	public void setCustomAnnotationPeakList() {
		if( getMassSpecCustomAnnotation() == null || getMassSpecCustomAnnotation() == null ) {
			return;
		}
		this.hmPeakList = new HashMap<Integer, List<Peak>>();
		Collection<MassSpecCustomAnnotationPeak> customPeaks = getMassSpecCustomAnnotation().getAnnotatedPeaks().values();
		Iterator<MassSpecCustomAnnotationPeak> itr = customPeaks.iterator();
		while( itr.hasNext() ) {
			MassSpecCustomAnnotationPeak customPeak = itr.next();
			Peak p = new Peak();
			p.setMz( customPeak.getPeakMz() );
			p.setIntensity(1.0d);
			List<Peak> lPeaks = null;
			if( ! hmPeakList.containsKey( customPeak.getMSLevel() ) ) {
				lPeaks = new ArrayList<Peak>();
				hmPeakList.put(customPeak.getMSLevel(), lPeaks);
			} else {
				lPeaks = hmPeakList.get(customPeak.getMSLevel());
			}
			lPeaks.add(p);
			
		}
	}

	/**
	 * @param lPeakList - HashMap<Integer, List<Peak>>, mapping of annotated peaks to MS level
	 */
	public void setPeakList(HashMap<Integer, List<Peak>> lPeakList) {
		this.hmPeakList = lPeakList;
	}

	/**
	 * @return HashMap<Integer, List<Peak>>, mapping of annotated peaks to MS level
	 */
	public HashMap<Integer, List<Peak>> getPeakList() {
		return hmPeakList;
	}

	/**
	 * @param iMSLevel - int value of desired MS Level for annotated peaks
	 * @return List<Peak> - list of annotated peaks at the specified MS level
	 */
	public List<Peak> getPeakList( Integer iMSLevel ) {
		return hmPeakList.get(iMSLevel);
	}
	
	/**
	 * @param curScan - Scan object being searched for custom annotation (usually a sub-scan)
	 */
	public void setCurScan(Scan curScan) {
		this.curScan = curScan;
	}
	
	/**
	 * @return Scan - the annotated Scan object
	 */
	public Scan getCurScan() {
		return curScan;
	}

	/* (non-Javadoc)
	 * @see org.grits.toolbox.entry.ms.extquantfiles.process.ExternalQuantFileProcessor#loadCorrectedData()
	 */
	@Override
	public void loadExternalData() {
		if ( this.m_settings.getTargetScanNumber() != null ) {
			try {
				this.m_quantPeakData = read();
				List<QuantPeakMatch> lQPM = m_quantPeakData.generateAllMatches();
				Collections.sort(lQPM, new ComparatorQuantPeakMatch());
				this.m_quantPeakMatches.put(curScan.getScanNo(), lQPM);
			}
			catch (Exception e) {
				logger.error(e.getMessage(),e);
			}
		}		
	}
	
	@Override
	public void generateAllMatches() {
		if ( m_quantPeakData != null ) {
			try {
				List<QuantPeakMatch> lQPM = m_quantPeakData.generateAllMatches();
				Collections.sort(lQPM, new ComparatorQuantPeakMatch());
				this.m_quantPeakMatches.put(curScan.getScanNo(), lQPM);
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
			}
		}
	}

	/**
	 * @param lCED - List of CustomExtraData to be removed from annotated peak
	 * 
	 * Description: The keys of the CustomExtraData objects are compared to those in current scan's annotated peak list.
	 * If found, the annotation is removed.
	 */
	public void clearPeakData( List<CustomExtraData> lCED ) {
		if( lCED == null || lCED.isEmpty() ) {
			return;
		}
		for( Peak p : getCurScan().getPeaklist() ) {
			// check each extra data for match to CED
			List<String> sToDelete = new ArrayList<String>();
			for( String sKey: p.getDoubleProp().keySet() ) {
				for( CustomExtraData ced : lCED ) {
					if( sKey.equals(ced.getKey()) ) {
						sToDelete.add(sKey);
					}
				}
			}
			for( String sKey : sToDelete ) {
				p.getDoubleProp().remove(sKey);
			}
			sToDelete.clear();
			for( String sKey: p.getIntegerProp().keySet() ) {
				for( CustomExtraData ced : lCED ) {
					if( sKey.equals(ced.getKey()) ) {
						sToDelete.add(sKey);
					}
				}
			}
			for( String sKey : sToDelete ) {
				p.getIntegerProp().remove(sKey);
			}
			sToDelete.clear();
			for( String sKey: p.getStringProp().keySet() ) {
				for( CustomExtraData ced : lCED ) {
					if( sKey.equals(ced.getKey()) ) {
						sToDelete.add(sKey);
					}
				}
			}
			for( String sKey : sToDelete ) {
				p.getStringProp().remove(sKey);
			}
		}
		
	}
	
	/**
	 * @return QuantPeakData - The quantitative peak that matched a peak in the current scan
	 */
	public QuantPeakData read() {
		QuantPeakData qpd = new QuantPeakData();
		try {	
			for( Peak p : getCurScan().getPeaklist()) {
				QuantPeak qp = QuantPeak.getQuantPeakData(p.getMz());				
				double dTol = QuantPeakMatch.getDaTolerance(p.getMz(), getSettings().getIntensityCorrectionValue(), getSettings().isIntensityCorrectionPpm());
				QuantPeakMatch qpm = QuantPeakMatch.findQuantPeakMatch(qp, p.getMz(), getPeakList(getCurScan().getMsLevel()), dTol);
				if( qpm != null ) {
					qp.add(qpm);
					qpd.add(qp);
					
				}
			}
		} catch( InvalidFileFormatException e ) {
			logger.error(e.getMessage(),e);
		} catch( Exception e ) {
			logger.error(e.getMessage(),e);
		}

		return qpd;
	}

	/* (non-Javadoc)
	 * @see org.grits.toolbox.entry.ms.extquantfiles.process.ExternalQuantFileProcessor#matchCorrectedPeaks(java.util.Map, boolean)
	 */
	public boolean matchCorrectedPeaks(Map<Integer, Scan> _mScans, boolean _bRequirePrecursorPeak) {
		Scan t_scanToProcess = null;
		boolean bUpdated = false;
		if ( this.m_quantPeakData.getScanNo() != -1 ) {
			t_scanToProcess = _mScans.get(this.m_quantPeakData.getScanNo());
		} else { // nasty empty scan value...just assume first MS1 scan then
			for (Scan t_scan : _mScans.values()) {
				if( t_scan.getMsLevel() == 1) {
					t_scanToProcess = t_scan;
					this.m_quantPeakData.setScanNo(t_scan.getScanNo());
					break;
				}
			}
		}
		if( t_scanToProcess != null && t_scanToProcess.getPeaklist() != null ) {
			for( Peak t_peak : t_scanToProcess.getPeaklist() ) {
				if( ! _bRequirePrecursorPeak || t_peak.getIsPrecursor() ) {
					QuantPeakMatch cPeak = findExternalPeak(t_peak.getMz());
					if( cPeak != null ) {
						findExternalPeak(t_peak.getMz());
						bUpdated |= setExternalPeakData(t_peak, cPeak);
					}
				}
			}
		}
		return bUpdated;
	}

	/* (non-Javadoc)
	 * @see org.grits.toolbox.entry.ms.extquantfiles.process.ExternalQuantFileProcessor#setCorrectedPeakData(org.grits.toolbox.ms.om.data.Peak, org.grits.toolbox.entry.ms.peaklist.extquant.data.QuantPeakMatch)
	 */
	@Override
	public boolean setExternalPeakData(Peak a_peak, QuantPeakMatch cPeak) {
		Double dKey = cPeak.getMzMostAbundant();
		String sLabel = dKey.toString();
		if( getMassSpecCustomAnnotation().getAnnotatedPeaks().containsKey(dKey) ) {			
			sLabel = getMassSpecCustomAnnotation().getAnnotatedPeaks().get(dKey).getPeakLabel();
		}
		String sIntKey = ExternalQuantColumnInfo.getExternalQuantIntensity(dKey.toString(), sLabel).getKey();
		String sMzKey = ExternalQuantColumnInfo.getExternalQuantIntensityMz(dKey.toString(), sLabel).getKey();
		boolean bAdd = true;
		// check if already populated. If so, compare values to see if changed
		if( a_peak.getDoubleProp().containsKey(sIntKey) && a_peak.getDoubleProp().containsKey(sMzKey) ) {
			bAdd = false;
			Double dVal1 = a_peak.getDoubleProp().get(sIntKey);
			Double dVal2 = a_peak.getIntensity();
			if( dVal2 != null ) {
//			if( dVal2 != null && (dVal1 == null || Double.compare(dVal1, dVal2 ) != 0) ) {
				bAdd = true;
			}
			dVal1 = a_peak.getDoubleProp().get(sMzKey);
			dVal2 = a_peak.getMz();
			if( dVal2 != null ) {
//			if( dVal2 != null && (dVal1 == null || Double.compare(dVal1, dVal2 ) != 0) ) {
				bAdd = true;
			}

		}		
		if( bAdd ) {
			fillPeakData(a_peak, cPeak);
		}
		return bAdd;
	}
	
	/**
	 * @param msca - the specified MassSpecCustomAnnotation object to be used for annotation
	 */
	public void setMassSpecCustomAnnotation(MassSpecCustomAnnotation msca) {
		this.msca = msca;
	}
	
	/**
	 * @return MassSpecCustomAnnotation
	 */
	public MassSpecCustomAnnotation getMassSpecCustomAnnotation() {
		return msca;
	}

	/**
	 * @param a_peak - Peak being annotation
	 * @param cPeak - QuantPeakMatch object from the CustomExtraData
	 * 
	 * Description: fills the Peak Double Prop fields w/ data from the CustomExtraData object
	 */
	protected void fillPeakData( Peak a_peak, QuantPeakMatch cPeak ) {
		Double dKey = cPeak.getMzMostAbundant();
		String sLabel = dKey.toString();
		if( getMassSpecCustomAnnotation().getAnnotatedPeaks().containsKey(dKey) ) {			
			sLabel = getMassSpecCustomAnnotation().getAnnotatedPeaks().get(dKey).getPeakLabel();
		}
		String sIntKey = ExternalQuantColumnInfo.getExternalQuantIntensity(dKey.toString(), sLabel).getKey();
		String sMzKey = ExternalQuantColumnInfo.getExternalQuantIntensityMz(dKey.toString(), sLabel).getKey();
		a_peak.addDoubleProp( sIntKey, a_peak.getIntensity());
		a_peak.addDoubleProp( sMzKey, a_peak.getMz());		
	}

	/**
	 * @param a_peak - Peak being annotation
	 * @param cPeak - QuantPeakMatch object from the CustomExtraData
	 * 
	 * Description: clears the Peak Double Prop fields w/ data from the CustomExtraData object
	 */
	protected void clearPeakData( Peak a_peak, QuantPeakMatch cPeak ) {
		Double dKey = cPeak.getMzMostAbundant();
		String sLabel = dKey.toString();
		if( getMassSpecCustomAnnotation().getAnnotatedPeaks().containsKey(dKey) ) {			
			sLabel = getMassSpecCustomAnnotation().getAnnotatedPeaks().get(dKey).getPeakLabel();
		}
		String sIntKey = ExternalQuantColumnInfo.getExternalQuantIntensity(dKey.toString(), sLabel).getKey();
		String sMzKey = ExternalQuantColumnInfo.getExternalQuantIntensityMz(dKey.toString(), sLabel).getKey();
		a_peak.getDoubleProp().remove(sIntKey);
		a_peak.getDoubleProp().remove(sMzKey);
	}
	
}
