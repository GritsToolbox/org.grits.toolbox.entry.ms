package org.grits.toolbox.entry.ms.extquantfiles.process;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.grits.toolbox.ms.file.extquant.data.ComparatorQuantPeakMatch;
import org.grits.toolbox.ms.file.extquant.data.ExternalQuantSettings;
import org.grits.toolbox.ms.file.extquant.data.QuantPeakMatch;
import org.grits.toolbox.ms.file.reader.impl.MSXMLReader;
import org.grits.toolbox.ms.om.data.Peak;

public class FullMzXMLDataProcessor extends QuantFileProcessor
{
	//log4J Logger
	private static final Logger logger = Logger.getLogger(FullMzXMLDataProcessor.class);
	private List<Peak> precursorPeaks = null;
	public static final String DEFAULT_KEY = "full_ms";
	public static final String DEFAULT_LABEL = "Full MS";

	public void setPrecursorPeaks(List<Peak> precursorPeaks) {
		this.precursorPeaks = precursorPeaks;
	}

	public List<Peak> getPrecursorPeaks() {
		return precursorPeaks;
	}

	public FullMzXMLDataProcessor(ExternalQuantSettings a_parameter) {
		super(a_parameter);
	}

	@Override
	public void loadExternalData() {
		if ( this.m_settings.getCorrectedFile() != null ) {
			try {
				MSXMLReader t_reader = (MSXMLReader) this.m_settings.getCorrectedFile().getReader();
				t_reader.setPrecursorPeaks(getPrecursorPeaks());
				t_reader.setMsLevel(-1);
				t_reader.setParentScanNum(-1);
				t_reader.setScanNum(1);
				this.m_quantPeakData = t_reader.read(this.m_settings.getCorrectedFile(),
						this.m_settings.isIntensityCorrectionPpm(),
						this.m_settings.getIntensityCorrectionValue());				
//				List<QuantPeakMatch> lQPM = m_quantPeakData.generateAllMatches();
//				Collections.sort(lQPM, new ComparatorQuantPeakMatch());
//				this.m_quantPeakMatches.put(getSettings().getTargetScanNumber(), lQPM);
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
				this.m_quantPeakMatches.put(getSettings().getTargetScanNumber(), lQPM);
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
			}
		}
	}
	

	@Override
	public boolean setExternalPeakData(Peak a_peak, QuantPeakMatch cPeak) {
		// TODO Auto-generated method stub
		return false;
	}
}
