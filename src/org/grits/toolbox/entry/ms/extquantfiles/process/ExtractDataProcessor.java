package org.grits.toolbox.entry.ms.extquantfiles.process;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.grits.toolbox.ms.file.extquant.data.ComparatorQuantPeakMatch;
import org.grits.toolbox.ms.file.extquant.data.ExternalQuantSettings;
import org.grits.toolbox.ms.file.extquant.data.QuantPeakMatch;
import org.grits.toolbox.ms.file.reader.IMSExtQuantFileReader;
import org.grits.toolbox.ms.om.data.Peak;

/**
 * @author D. Brent Weatherly (dbrentw@uga.edu)
 *
 */
public class ExtractDataProcessor extends QuantFileProcessor {
	//log4J Logger
	private static final Logger logger = Logger.getLogger(ExtractDataProcessor.class);
	public static final String DEFAULT_KEY = "extract";
	public static final String DEFAULT_LABEL = "Extract";

	public ExtractDataProcessor(ExternalQuantSettings a_parameter) {
		super(a_parameter);
	}	

	@Override
	public void loadExternalData() {
		if ( this.m_settings.getCorrectedFile() != null ) {
			try {
				IMSExtQuantFileReader t_reader = (IMSExtQuantFileReader) this.m_settings.getCorrectedFile().getReader();
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
