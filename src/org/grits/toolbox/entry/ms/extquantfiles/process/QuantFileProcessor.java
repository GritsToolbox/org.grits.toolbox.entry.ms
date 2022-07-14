package org.grits.toolbox.entry.ms.extquantfiles.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.grits.toolbox.ms.file.extquant.data.ExternalQuantSettings;
import org.grits.toolbox.ms.file.extquant.data.QuantPeak;
import org.grits.toolbox.ms.file.extquant.data.QuantPeakData;
import org.grits.toolbox.ms.file.extquant.data.QuantPeakMatch;
import org.grits.toolbox.ms.om.data.Peak;
import org.grits.toolbox.ms.om.data.Scan;

/**
 * @author D. Brent Weatherly (dbrentw@uga.edu)
 *
 */
public abstract class QuantFileProcessor {
	protected String sLabelAlias = "";
	protected String sKeyID = "";
	public final static String QUANT_LABEL_PREFIX = "_quant";
	
	protected ExternalQuantSettings m_settings = null;
	protected Map<Integer, List<QuantPeakMatch>> m_quantPeakMatches = new HashMap<Integer, List<QuantPeakMatch>>();
	protected QuantPeakData m_quantPeakData = null;

	protected List<Peak> sourcePeakListToMatch = null;
	
	public abstract void loadExternalData();
	public abstract void generateAllMatches();
	public abstract boolean setExternalPeakData(Peak a_peak, QuantPeakMatch cPeak); // return whether data gets set or not
	
	public QuantFileProcessor(ExternalQuantSettings m_settings) {
		this.m_settings = m_settings;
	}
	
	public String getLabelAlias() {
		return sLabelAlias;
	}
	public void setLabelAlias(String sLabelAlias) {
		this.sLabelAlias = sLabelAlias;
	}

	public String getKeyID() {
		return sKeyID;
	}
	public void setKeyID(String sKeyID) {
		this.sKeyID = sKeyID;
	}

	public ExternalQuantSettings getSettings() {
		return m_settings;
	}
	
	public Map<Integer, List<QuantPeakMatch>> getQuantPeakMatches() {
		return m_quantPeakMatches;
	}
	
	public QuantPeakData getQuantPeakData() {
		return m_quantPeakData;
	}
	
	public void setQuantPeakData(QuantPeakData m_quantPeakData) {
		this.m_quantPeakData = m_quantPeakData;
	}
	
	public static String getExternalQuantProcessorKey( String _sKeyID, String _sKeyPrefix ) {
		return (_sKeyID != null && ! _sKeyID.equals("")) ? _sKeyID : _sKeyPrefix;
	}

	public static String getExternalQuantProcessorLabel( String _sLabelAlias, String _sLabelPrefix ) {
		return ! _sLabelAlias.equals("") ? _sLabelAlias : _sLabelPrefix;
	}

	public boolean matchesExternalQuantKey( String sExtQuantFullKey ) {
		return sExtQuantFullKey.startsWith(getKeyID() + QuantFileProcessor.QUANT_LABEL_PREFIX);
	}

	public List<Peak> getSourcePeakListToMatch() {
		return sourcePeakListToMatch;
	}
	
	public void setSourcePeakListToMatch(List<Peak> sourcePeakListToMatch) {
		this.sourcePeakListToMatch = sourcePeakListToMatch;
	}
	
	public void setSourcePeakList(Map<Integer, Scan> _mScans) {
		//    	boolean bUpdated = false;
		Scan t_scanToProcess = null;
		if ( this.m_quantPeakData.getScanNo() != -1 ) {
			t_scanToProcess = _mScans.get(this.m_quantPeakData.getScanNo());
		} else { // nasty empty scan value...just assume first MS1 scan then
			for (Scan t_scan : _mScans.values())
			{
				if( t_scan.getMsLevel() == 1) {
					t_scanToProcess = t_scan;
					this.m_quantPeakData.setScanNo(t_scan.getScanNo());
					break;
				}
			}
		}
		setSourcePeakListToMatch(t_scanToProcess.getPeaklist());
	}

	public boolean matchExternalPeaks( boolean _bRequirePrecursorPeak ) {
		boolean bUpdated = false;
		if( getSourcePeakListToMatch() != null ) {
			for( Peak t_peak : getSourcePeakListToMatch() ) {
				if( ! _bRequirePrecursorPeak || t_peak.getIsPrecursor() ) {
					QuantPeakMatch cPeak = findExternalPeak(t_peak.getMz());
					if( cPeak != null ) {
						bUpdated |= setExternalPeakData(t_peak, cPeak);
					}
				}
			}
		}
		return bUpdated;
	}
	
	public boolean removeExternalPeakMatches(boolean _bRequirePrecursorPeak) {
		boolean bUpdated = false;
		if( getSourcePeakListToMatch() != null ) {
			for( Peak t_peak : getSourcePeakListToMatch()) {
				if( ! _bRequirePrecursorPeak || t_peak.getIsPrecursor() ) {
					QuantPeakMatch cPeak = findExternalPeak(t_peak.getMz());
					if( cPeak != null ) {
						bUpdated |= setExternalPeakData(t_peak, null);
					}
				}
			}
		}
		return bUpdated;
	}

	protected QuantPeakMatch findExternalPeak(Double _dCandidateMz) {
		List<QuantPeakMatch> listMatches = new ArrayList<>();
		for (QuantPeak t_peak : this.m_quantPeakData.getPeaks()) {
			for( QuantPeakMatch t_match : t_peak.getMatch() ) {
				if ( t_match.getMinMz() <= _dCandidateMz && t_match.getMaxMz() >= _dCandidateMz ) {
					listMatches.add(t_match);
				}
				else if ( t_match.getMinMz() > _dCandidateMz ) {
					break;
				}
			}
		}
		if( listMatches.isEmpty() ) {
			return null;
		} else if ( listMatches.size() == 1 ) {
			return listMatches.get(0);
		} else { // take most abundant
			double dMax = Double.MIN_VALUE;
			QuantPeakMatch match = null;
			for( int i = 0; i < listMatches.size(); i++ ) {
				QuantPeakMatch qpm = listMatches.get(i);
				if( qpm.getIntensitySum() > dMax ) {
					dMax = qpm.getIntensitySum();
					match = qpm;
				}				
			}
			return match;
		}		
	}
	
}
