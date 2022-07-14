package org.grits.toolbox.entry.ms.views.tabbed;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.editor.IEntryEditorPart;
import org.grits.toolbox.datamodel.ms.preference.MassSpecViewerPreference;
import org.grits.toolbox.datamodel.ms.tablemodel.FillTypes;
import org.grits.toolbox.datamodel.ms.tablemodel.MassSpecTableDataObject;
import org.grits.toolbox.display.control.spectrum.chart.GRITSChartComposite;
import org.grits.toolbox.display.control.spectrum.chart.GRITSJFreeChart;
import org.grits.toolbox.display.control.spectrum.chart.GRITSSpectralViewerChart;
import org.grits.toolbox.display.control.spectrum.chart.GRITSSpectralViewerData;
import org.grits.toolbox.display.control.spectrum.datamodel.MSIonData;
import org.grits.toolbox.display.control.table.datamodel.GRITSListDataRow;
import org.grits.toolbox.entry.ms.exceptions.MSException;
import org.grits.toolbox.entry.ms.extquantfiles.process.CustomAnnotationDataProcessor;
import org.grits.toolbox.entry.ms.extquantfiles.process.QuantFileProcessor;
import org.grits.toolbox.entry.ms.preference.xml.MassSpecCustomAnnotationPeak;
import org.grits.toolbox.entry.ms.process.loader.MassSpecTableDataProcessor;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.grits.toolbox.entry.ms.property.datamodel.MSPropertyDataFile;
import org.grits.toolbox.ms.file.MSFile;
import org.grits.toolbox.ms.file.extquant.data.QuantPeakMatch;
import org.grits.toolbox.ms.file.reader.IMSAnnotationFileReader;
import org.grits.toolbox.ms.file.reader.IMSFileReader;
import org.grits.toolbox.ms.om.data.Peak;
import org.grits.toolbox.ms.om.data.Scan;


/**
 * @author D Brent Weatherly (dbrentw@uga.edu)
 *
 */
public class MassSpecSpectraView implements IEntryEditorPart {
	public static final String VIEW_ID = "plugin.ms.views.MSSpectraView";

	//log4J Logger
	private static final Logger logger = Logger.getLogger(MassSpecSpectraView.class);
	protected Composite parent = null;
	protected String sMzXMLFile = null;
	protected Integer iScanNum = null;
	protected String sDescription = null;
	protected Double dMz = null;
	protected Integer iMSLevel = null;
	protected MassSpecTableDataProcessor peakListTableProcessor = null;
	protected MassSpecTableDataProcessor scanListtableProcessor = null;
	protected Composite compositeTop = null;
	protected Composite compositeBottom = null;
	protected MSSpectraViewerSashForm sashForm = null;
	protected MassSpecSpectraControlPanelView controlPanel = null;
	protected Boolean bIsCentroid = false;

	protected GRITSSpectralViewerChart svChart = null;
	protected GRITSJFreeChart jFreeChart = null;

	protected GRITSChartComposite chartComposite = null;
	protected Entry entry;

	private MPart part;

	/**
	 * @param entry - optional Entry  associated with this Spectra Viewer
	 */
	@Inject
	public MassSpecSpectraView(@Optional Entry entry) {
		this.entry = entry;
	}

	/* (non-Javadoc)
	 * @see org.grits.toolbox.core.editor.IEntryEditorPart#getEntry()
	 */
	public Entry getEntry() {
		return entry;
	}

	/**
	 * @return boolean - whether or not the associated SpectralViewerChart is null
	 */
	public boolean isLoaded() {
		return svChart != null;
	}

	/**
	 * @param tableProcessor - the MassSpecTableDataProcessor for the Peaks FillType table viewer, if present
	 */
	public void setPeakListTableProcessor(MassSpecTableDataProcessor tableProcessor) {
		this.peakListTableProcessor = tableProcessor;
	}

	/**
	 * @return MassSpecTableDataProcessor - the MassSpecTableDataProcessor for the Peaks FillType table viewer, if present
	 */
	public MassSpecTableDataProcessor getPeakListTableProcessor() {
		return peakListTableProcessor;
	}

	/**
	 * @param tableProcessor - the MassSpecTableDataProcessor for the Scans FillType table viewer, if present
	 */
	public void setScanListTableProcessor(MassSpecTableDataProcessor tableProcessor) {
		this.scanListtableProcessor = tableProcessor;
	}

	/**
	 * @return MassSpecTableDataProcessor - the MassSpecTableDataProcessor for the Scans FillType table viewer, if present
	 */
	public MassSpecTableDataProcessor getScanListTableProcessor() {
		return scanListtableProcessor;
	}

	/**
	 * @return SpectralViewerChart - member variable "svChart"
	 */
	public GRITSSpectralViewerChart getSpectralViewerChart() {
		return this.svChart;
	}

	/**
	 * @param _sMzXMLFile - name of MS file to be checked with member variable "sMzXMLFile"
	 * @return
	 */
	public boolean isSameRunAs( String _sMzXMLFile ) {
		if ( this.sMzXMLFile == null || _sMzXMLFile == null )
			return false;
		return this.sMzXMLFile.equals(_sMzXMLFile);

	}

	/**
	 * @return Composite - member variable "sashForm"
	 */
	public Composite getView() {
		return this.sashForm;
	}

	/**
	 * @return SimianChartComposite - member variable "chartComposite"
	 */
	public GRITSChartComposite getChart() {
		return this.chartComposite;
	}

	/**
	 * @param sDescription - String value of description to place on chart, sets value of member variable "sDescription"
	 */
	public void setDescription(String sDescription) {
		this.sDescription = sDescription;
	}

	/**
	 * @param sMzXMLFile - String full path to MS file from which to read spectra, sets value of member variable "sMzXMLFile"
	 */
	public void setMzXMLFile(String sMzXMLFile) {
		this.sMzXMLFile = sMzXMLFile;
	}

	/**
	 * @param iScanNum - Integer value of desired scan number, sets value of member variable "iScanNum"
	 */
	public void setScanNum(Integer iScanNum) {
		this.iScanNum = iScanNum;
	}

	/**
	 * @param iMSLevel - Integer value of desired MS level, sets value of member variable "iMSLevel"
	 */
	public void setMSLevel(Integer iMSLevel) {
		this.iMSLevel = iMSLevel;
	}

	/**
	 * @param dMz - Double value of desired m/z, if present, sets value of member variable "dMz"
	 */
	public void setMz(Double dMz) {
		this.dMz = dMz;
	}	

	/**
	 * @return the path for the MS files for the MS entry
	 */
	public String getMSPath() {
		Entry msEntry = MassSpecProperty.getMSParentEntry(getEntry());
		MassSpecProperty msProp = (MassSpecProperty) msEntry.getProperty();
		String folderName = MassSpecProperty.getFullyQualifiedFolderName(msEntry);
		return folderName;
	}

	/**
	 * 
	 * @return the data file for this entry
	 */
	public MSPropertyDataFile getMSSourceFile() {
		if (entry == null)
			return null;
		MSPropertyDataFile msSourceFile = ((MassSpecEntityProperty)getEntry().getProperty()).getDataFile();
		Property prop = entry.getProperty();
		MassSpecProperty msProp = null;
		MassSpecEntityProperty msep = null;
		if( prop instanceof MassSpecEntityProperty ) {
			msep = (MassSpecEntityProperty) prop;
			msProp = (MassSpecProperty) msep.getMassSpecParentProperty();
		} else {
			msProp = (MassSpecProperty) prop;
		}
		
		MSPropertyDataFile newMSSourceFile = msProp.getUpdatePropertyDataFile(msSourceFile);
		if( ! msSourceFile.equals(newMSSourceFile) ) {
			if( msep != null ) {
				msep.setDataFile(newMSSourceFile);
			}
			return newMSSourceFile;
		}
		
		return msSourceFile;
	}

	/**
	 * @return ArrayList<MSIonData> - List of MSIonData representing a pseudo  MS1 scan for TIM
	 * 
	 * Description: if MS File is TIM MS type, read all precursor data and place in ArrayList. These peaks become
	 * a pseudo parent scan. 
	 */
	protected ArrayList<MSIonData> getCurrentScanDataTIM(MassSpecProperty prop) {
		ArrayList<MSIonData> alIons = new ArrayList<>();
		MSPropertyDataFile msSourceFile = getMSSourceFile();
		if (msSourceFile == null)
			return alIons;
		// get the reader for display purposes only 
		// to be able to read FullMS file with MzXMLReader instead of as an external quantification file
		MSFile msFile = msSourceFile.getMSFileWithReader(getMSPath(), prop.getMassSpecMetaData().getMsExperimentType(), true);
		IMSFileReader reader = msFile.getReader();
		if (reader == null || !(reader instanceof IMSAnnotationFileReader))
			return alIons;
		try {
			List<Scan> scans = ((IMSAnnotationFileReader)reader).readMSFile(msFile, 2, -1, -1);
			for( int i = 0; i < scans.size(); i++ ) {
				org.grits.toolbox.ms.om.data.Scan scan = scans.get(i);
				if( scan == null || scan.getPrecursor() == null ) {
					continue;
				}
				MSIonData ion = new MSIonData( scan.getPrecursor().getMz(), scan.getPrecursor().getIntensity() );
				alIons.add(ion);
			}
			bIsCentroid = true;
			return alIons;
		} catch ( Exception ex ) {
			reader = null;
			logger.error("General Exception in getCurrentScanData.", ex);
		}
		return null;
	}

	/**
	 * @return ArrayList<MSIonData> 
	 * @throws MSException
	 * 
	 * Return the peak list in form of ArrayList<MSIonData>. Either calls "getCurrentScanDataTIM()" or builds
	 * that list from the peaks in the current scan
	 */
	protected ArrayList<MSIonData> getCurrentScanData() throws MSException {
		ArrayList<MSIonData> alIons = new ArrayList<>();
		MSPropertyDataFile msSourceFile = getMSSourceFile();
		if (msSourceFile == null)
			return alIons;
		MassSpecProperty prop = null;		
		Entry msEntry = MassSpecProperty.getMSParentEntry(getEntry());
		if( msEntry != null && msEntry.getProperty() instanceof MassSpecProperty ) {
			prop = (MassSpecProperty) msEntry.getProperty();		
		}
		// get the reader for display purposes only 
		// to be able to read FullMS file with MzXMLReader instead of as an external quantification file
		MSFile msFile = msSourceFile.getMSFileWithReader(getMSPath(), prop.getMassSpecMetaData().getMsExperimentType(), true);
		IMSFileReader reader = msFile.getReader();
		if (reader == null || !(reader instanceof IMSAnnotationFileReader))
			return alIons;
		if( iScanNum == null || iScanNum <= 0 ) {
			boolean bHasMS1 = ((IMSAnnotationFileReader) reader).hasMS1Scan(msFile);
			if( ! bHasMS1 ) { // if the  run doesn't have an MS1 scan, treat like TIM
				return getCurrentScanDataTIM(prop);
			}
			/*
			if( prop.getMassSpecMetaData().getMsExperimentType().equals(Method.MS_TYPE_TIM) ) {		
				return getCurrentScanDataTIM();
			} else if ( prop.getMassSpecMetaData().getMsExperimentType().equals(Method.MS_TYPE_INFUSION) ) {
				boolean bHasMS1 = ((IMSAnnotationFileReader) reader).hasMS1Scan(msFile);
				if( ! bHasMS1 ) { // if the DI run doesn't have an MS1 scan, treat like TIM
					return getCurrentScanDataTIM();
				}
			}
			*/
		} 

			
		try {
			Scan scan = null;
			if ( iScanNum == null || iScanNum <= 0 ) {
				Scan firstScan = ((IMSAnnotationFileReader) reader).getFirstMS1Scan(msFile);
				List<Scan> scans = ((IMSAnnotationFileReader) reader).readMSFile(msFile, -1, -1, firstScan.getScanNo());
				if( ! scans.isEmpty() ) {
					scan = scans.get(0);
				}
			} else {
				List<Scan> scans = ((IMSAnnotationFileReader) reader).readMSFile(msFile, -1, -1, iScanNum);
				if( ! scans.isEmpty() ) {
					scan = scans.get(0);
				}
			}
			if( scan == null ) {
				throw new MSException(MSException.SCAN_NOT_FOUND);
			}
			iScanNum = scan.getScanNo();
			bIsCentroid = scan.getIsCentroided();
			if( ! scan.getPeaklist().isEmpty() ) {
				for( int i = 0; i < scan.getPeaklist().size(); i ++ ) {
					Peak p = scan.getPeaklist().get(i);
					if( p.getIntensity() <= 0 ) {
						continue;
					}
					MSIonData ionData = new MSIonData( p.getMz(), p.getIntensity() );
					alIons.add(ionData);
				}
			}
		} catch( MSException e ) {
			throw e;
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
		return alIons;
	}

	/**
	 * @param parent - parent Composite object
	 * @param part - MPart to contain the chart
	 */
	@PostConstruct
	public void postConstruct(Composite parent, MPart part) {
		this.parent = parent;		
		this.part = part;
	}

	@Focus
	public void setFocus() {
		// TODO Auto-generated method stub
		//		createView(this.parent);

	}

	/**
	 * @param partName - String value of this part's name
	 */
	public void setPartName(String partName) {
		part.setLabel(partName);
	}

	/**
	 * @return SpectralViewerData
	 * @throws MSException
	 * 
	 * Description: Instantiate and loads a new SpectralViewerData with data from the current scan object
	 */
	protected GRITSSpectralViewerData initSpectralViewerData() throws MSException {
		try {
			GRITSSpectralViewerData svd = new GRITSSpectralViewerData();
			ArrayList<MSIonData> alRawData = getCurrentScanData();
			if( alRawData == null || alRawData.isEmpty() ) {
				return svd;
			}
			svd.setRawData(alRawData);
			setScanListTableData(svd);
			setPeakListTableData(svd);
			return svd;
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
	}

	/**
	 * @param svd - the SpectralViewerData object getting peaks and peak annotations set
	 * @throws MSException
	 * 
	 * Description: Iterates over data in the Scan TableProcessor's TableDataObject to load data for the spectrum 
	 */
	protected void setScanListTableData(GRITSSpectralViewerData svd) throws MSException {
		try {
			ArrayList<MSIonData> alPickedPeaks = new ArrayList<>();
			HashMap<Double, List<Object>> htPickedPeakLabels = new HashMap<>();
			svd.setPickedPeaks(alPickedPeaks);
			svd.setPickedPeakLabels(htPickedPeakLabels);

			if( getScanListTableProcessor() == null || svd == null ) 
				return;		

			MassSpecTableDataObject msTable = (MassSpecTableDataObject) getScanListTableProcessor().getSimianTableDataObject();

			if( msTable.getMzCols() == null || msTable.getMzCols().isEmpty() ) 
				return;
			for( int i = 0; i < msTable.getTableData().size(); i++  ) {			
				GRITSListDataRow alRow = msTable.getTableData().get(i);
				Double dMz = null;
				Double dIntensity = null;
				Integer iScanNo = null;	
				boolean bIsPrecursor = false;
				try {
					dMz = (Double) alRow.getDataRow().get(msTable.getMzCols().get(0));
					dIntensity = (Double) alRow.getDataRow().get(msTable.getPeakIntensityCols().get(0));
					if( ! msTable.getScanNoCols().isEmpty() && alRow.getDataRow().get(msTable.getScanNoCols().get(0)) != null ) {
						iScanNo = (Integer) alRow.getDataRow().get(msTable.getScanNoCols().get(0));
					}
					String sIsPrecursor = null;
					if( msTable.getPeakIsPrecursorCols() != null && ! msTable.getPeakIsPrecursorCols().isEmpty() ) {
						sIsPrecursor = (String) alRow.getDataRow().get(msTable.getPeakIsPrecursorCols().get(0));
					}
					// hacky: if sIsPrecursor, this table is probably a scan table, in which case all rows are precursors. If not-null, then a peak list table
					bIsPrecursor = iScanNo != null || (sIsPrecursor != null && sIsPrecursor.equals("Yes")) ? true : false;
				} catch( Exception ex ) {
					logger.error("Invalid number format for m/z or intensity in table", ex);
				}
				if( dMz == null || dIntensity == null ) {
					continue;
				}
				MSIonData msData = new MSIonData(dMz, dIntensity);
				if( bIsPrecursor ) {
					if( alPickedPeaks.contains(msData) ) { // prevent duplicates!
						continue;
					}
					alPickedPeaks.add(msData);
					Object oLabel = getPeakLabel(dMz, iScanNo);
					if( oLabel != null ) {
						List<Object> al = null;
						if( htPickedPeakLabels.containsKey(dMz) ) {
							al = htPickedPeakLabels.get(dMz);
						} else {
							al = new ArrayList<>();
							htPickedPeakLabels.put(dMz, al);
						}
						if( ! al.contains(oLabel)) {
							al.add(oLabel);
						}
					}
				}				
			}

		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
	}

	/**
	 * @param svd - the SpectralViewerData object getting peaks and peak annotations set
	 * @throws MSException
	 * 
	 * Description: Iterates over data in the Peak TableProcessor's TableDataObject to load data for the spectrum 
	 */
	protected void setPeakListTableData(GRITSSpectralViewerData svd) throws MSException {
		try {
			ArrayList<MSIonData> alAnnotatedPeaks = new ArrayList<>();
			HashMap<Double, List<Object>> htAnnotatedLabels = new HashMap<>();
			svd.setAnnotatedPeaks(alAnnotatedPeaks);
			svd.setAnnotatedPeakLabels(htAnnotatedLabels);
			if( getPeakListTableProcessor() == null || svd == null ) 
				return;		

			MassSpecTableDataObject msTable = (MassSpecTableDataObject) getPeakListTableProcessor().getSimianTableDataObject();
			if( msTable.getMzCols() == null || msTable.getMzCols().isEmpty() || msTable.getFillType() == FillTypes.Scans ) 
				return;
			for( int i = 0; i < msTable.getTableData().size(); i++  ) {			
				GRITSListDataRow alRow = msTable.getTableData().get(i);
				Double dMz = null;
				Double dIntensity = null;
				try {
					dMz = (Double) alRow.getDataRow().get(msTable.getMzCols().get(0));
					dIntensity = (Double) alRow.getDataRow().get(msTable.getPeakIntensityCols().get(0));
				} catch( Exception ex ) {
					logger.error("Invalid number format for m/z or intensity in table", ex);
				}
				if( dMz == null || dIntensity == null ) {
					continue;
				}
				MSIonData msData = new MSIonData(dMz, dIntensity);
				int iMSLevel =  ((MassSpecEntityProperty) getPeakListTableProcessor().getSourceProperty()).getMsLevel() - 1;
				Object oLabel = getPeakLabelForSpecialPeaks(dMz, dIntensity, iMSLevel);
				if( oLabel != null ) {
					if( alAnnotatedPeaks.contains(msData) ) { // prevent duplicates!
						continue;
					}
					alAnnotatedPeaks.add(msData);
					List<Object> al = null;
					if( htAnnotatedLabels.containsKey(dMz) ) {
						al = htAnnotatedLabels.get(dMz);
					} else {
						al = new ArrayList<>();
						htAnnotatedLabels.put(dMz, al);
					}
					if( ! al.contains(oLabel) ) {
						al.add(oLabel);
					}
				}

			}
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
	}

	/**
	 * @param dMz - m/z value of a peak to check for custom annotation
	 * @param dIntensity - intensity value of a peak to check for custom annotation
	 * @param iMSLevel - ms level of the scan to check for custom annotation
	 * @return Object - a label of some sort for a peak, if matched (could be String or an Image)
	 */
	protected Object getPeakLabelForSpecialPeaks( Double dMz, Double dIntensity, int iMSLevel ) {
		if( getPeakListTableProcessor().getQuantFileProcessors() == null || getPeakListTableProcessor().getQuantFileProcessors().isEmpty() )
			return null;
		Iterator<QuantFileProcessor> itr = getPeakListTableProcessor().getQuantFileProcessors().iterator();
		while( itr.hasNext() ) {
			QuantFileProcessor fp = itr.next();
			if( fp instanceof CustomAnnotationDataProcessor ) {
				CustomAnnotationDataProcessor sfp = (CustomAnnotationDataProcessor) fp;
				if( iScanNum == null ) {
					continue;
				}
				if( sfp.getQuantPeakData() == null || sfp.getQuantPeakData().getPeaks() == null || sfp.getQuantPeakMatches() == null) { 
					continue;
				}
				if( sfp.getQuantPeakMatches().get(iScanNum ) == null ) {
					continue;
				}
				QuantPeakMatch match = null;
				for( QuantPeakMatch qpm : sfp.getQuantPeakMatches().get(iScanNum ) ) {
					if( qpm.getMinMz() < dMz && qpm.getMaxMz() > dMz) {
						match = qpm;
						break;
					}
				}
				if( match != null ) {
					String sLabel = Double.toString(match.getMzMostAbundant());
					MassSpecCustomAnnotationPeak annotatedPeak = sfp.getMassSpecCustomAnnotation().getAnnotatedPeaks().get( match.getMzMostAbundant() );
					if( annotatedPeak != null && annotatedPeak.getMSLevel().intValue() == iMSLevel ) {
						sLabel = match.getMzMostAbundant() + " - " + sfp.getMassSpecCustomAnnotation().getAnnotatedPeaks().get(match.getMzMostAbundant()).getPeakLabel();
					}
					return sLabel;
				}
			}
		}
		return null;
	}

	/**
	 * @param dMz - m/z value of a peak to check for custom annotation
	 * @param iScanNo - scan number for peak
	 * @return Object - a label of some sort for a peak, if matched (could be String or an Image)
	 */
	protected Object getPeakLabel( Double dMz, Integer iScanNo ) {
		if( iScanNo == null ) {
			return null;
		}
		DecimalFormat df = new DecimalFormat("0.00");
		String sLabel = iScanNo != null ? "Scan: " + iScanNo + ", " + df.format(dMz) : df.format(dMz);
		return sLabel;
	}

	/**
	 * @return SpectralViewerChart - new instance of SpectralViewerChart
	 */
	protected GRITSSpectralViewerChart getNewSpectralViewerChart() {
		return new GRITSSpectralViewerChart( this.sDescription, this.iScanNum, iMSLevel, ! bIsCentroid, false );
	}

	/**
	 * @throws MSException
	 * 
	 * Description: initializes the components to display the spectrum.
	 */
	protected void initializeChartData() throws MSException {
		try {
			GRITSSpectralViewerData svd = initSpectralViewerData();
			if( svd == null || svd.getRawData() == null || svd.getRawData().isEmpty() ) {
				throw new MSException(MSException.EMPTY_PEAK_LIST);
			}
			svChart = getNewSpectralViewerChart();
			controlPanel.enableComponents(svd);
			svChart.setSpectralViewerData(svd);
		} catch( MSException ex ) {
			throw ex;
		} catch( Exception e ) {
			logger.error("Error initializing chart data.", e);
			svChart = null;
		}
	}

	/**
	 * Updates the persistent preference with settings for showing peaks and their annotations
	 */
	protected void updatePrefs() {
		MassSpecViewerPreference pref = (MassSpecViewerPreference) getScanListTableProcessor().getSimianTableDataObject().getTablePreferences();
		pref.setShowRaw(controlPanel.getShowSpectra().getSelection());
		pref.setShowPicked(controlPanel.getPickedPeaks().getSelection());
		pref.setShowPickedLabels(controlPanel.getPickedPeakLabels().getSelection());
		pref.setShowAnnotated(controlPanel.getAnnotatedPeaks().getSelection());
		pref.setShowAnnotatedLabels(controlPanel.getAnnotatedPeakLabels().getSelection());
		pref.writePreference();
	}

	/**
	 * Initializes the preferred settings for controls on the page and calls "createChart" to generate a JFreeChart
	 */
	protected void initializeChartPlot() {		
		MassSpecViewerPreference pref = (MassSpecViewerPreference) getScanListTableProcessor().getSimianTableDataObject().getTablePreferences();
		controlPanel.getShowSpectra().setSelection(pref.isShowRaw());
		controlPanel.getAnnotatedPeaks().setSelection(pref.isShowAnnotated());
		if( controlPanel.getAnnotatedPeaks().getSelection() ) {
			controlPanel.getAnnotatedPeaks().setEnabled(true);
			controlPanel.getAnnotatedPeakLabels().setEnabled(true);
			controlPanel.getAnnotatedPeakLabels().setSelection(pref.isShowAnnotatedLabels());			
		} 
		controlPanel.getPickedPeaks().setSelection(pref.isShowPicked());
		if( controlPanel.getPickedPeaks().getSelection() ) {
			controlPanel.getPickedPeaks().setEnabled(true);
			controlPanel.getPickedPeakLabels().setEnabled(true);
			controlPanel.getPickedPeakLabels().setSelection(pref.isShowPickedLabels());
		} 
		this.jFreeChart = svChart.createChart();	
		//		MSSpectraRangeAxis yAxis = (MSSpectraRangeAxis) this.jFreeChart.getXYPlot().getRangeAxis();
		//		yAxis.au

	}

	/**
	 * Updates the chart and preferences based on interaction w/ user for peak and annotation visibility
	 */
	public void updateChartPlot() {
		MassSpecSpectraControlPanelView cp = (MassSpecSpectraControlPanelView) controlPanel;
		svChart.updateChart( cp.showRaw(), cp.showPickedPeaks(), cp.showPickedPeakLabels(), 
				cp.showAnnotatedPeaks(), cp.showAnnotatedPeakLabels(),
				false, false);
		updatePrefs();
	}

	/**
	 * @return MassSpecSpectraControlPanelView - a new instance of MassSpecSpectraControlPanelView
	 */
	protected MassSpecSpectraControlPanelView getNewSpectraControlPanel() {
		part.getContext().set(MassSpecSpectraView.class, this);
		MassSpecSpectraControlPanelView cp = ContextInjectionFactory.make(MassSpecSpectraControlPanelView.class, part.getContext());
		return cp;
	}

	/**
	 * @return MassSpecSpectraControlPanelView - the "controlPanel" member variable
	 */
	public MassSpecSpectraControlPanelView getControlPanel() {
		return controlPanel;
	}

	/**
	 * @param parent - Calling parent Composite
	 * @throws MSException
	 * 
	 * Description: Creates the components for the chart and sets the layout.
	 */
	public void createChart(Composite parent) throws MSException {
		sashForm = new MSSpectraViewerSashForm(parent, SWT.VERTICAL, this);
		controlPanel = getNewSpectraControlPanel();
		compositeTop = new Composite(sashForm, SWT.BORDER);
		compositeTop.setLayout(new GridLayout(1,false));

		controlPanel.createPartControl(compositeTop);
		controlPanel.getShowSpectra().addPaintListener(new PaintListener() {

			@Override
			public void paintControl(PaintEvent e) {
				setWeights();
				controlPanel.cbShowSpectra.removePaintListener(this);
			}
		});

		createThisChart();		
	}

	protected int[] getSashWeights() {
		int dPrefTopWeight = getPrefEntityScrollerWeight();
		int dPrefBotWeight = 1000 - dPrefTopWeight;
		return new int[] {dPrefTopWeight, dPrefBotWeight};
	}

	protected int getPrefEntityScrollerWeight() {
		if( compositeTop == null ) 
			return 1;
		int iTopHeight = (controlPanel.getShowSpectra().getSize().y * 4 + 30);
		compositeTop.setSize(compositeTop.getSize().x, 100);
		int iFormHeight = sashForm.getSize().y;
		int iTopWeight = (int) Math.ceil( ((double) iTopHeight / (double) iFormHeight) * 1000.0) + 30;
		return iTopWeight;		
	}

	/**
	 * Sets the initial position of the divider on the SashForm
	 */
	protected void setWeights() {
		//		int[] dWeights = new int[] {4, 20};
		//		sashForm.setWeights(dWeights);	
		int[] dWeights = getSashWeights();
		try {
			sashForm.setWeights(dWeights);	
		} catch (IllegalArgumentException e) {
			logger.debug("Weight calculation failed: " + dWeights);
			//default weight
			sashForm.setWeights(new int[] {10, 90});
		}
	}

	/**
	 * @throws MSException
	 * 
	 * Called from createChart to build the spectrum viewer composite
	 */
	public void createThisChart() throws MSException {
		try {		
			if( compositeBottom != null )
				compositeBottom.dispose();
			compositeBottom = new Composite(sashForm, SWT.BORDER);
			compositeBottom.setLayout(new GridLayout(1,false));
			GridData gridData = new GridData();
			gridData.verticalAlignment = GridData.FILL;
			gridData.grabExcessVerticalSpace = true;
			gridData.horizontalAlignment = GridData.FILL;
			gridData.grabExcessHorizontalSpace = true;
			initializeChartData();
			initializeChartPlot();
			updateChartPlot();
			chartComposite = new GRITSChartComposite(compositeBottom, SWT.NONE, this.jFreeChart, true);	
			chartComposite.setMzXMLFile(sMzXMLFile);
			chartComposite.setScanNum(iScanNum);  
			chartComposite.setLayoutData(gridData);	
			sashForm.layout(true);
			//			sashForm.setWeights(new int[] {4,30});				
			setWeights();

		} catch( MSException e ) {
			throw e;
		} catch ( Exception e ) {
			logger.error("Error parsing mzXML file", e);
		}	
	}


	/**
	 * @throws MSException
	 * 
	 * Called from MultiPageViewer to create the main window
	 */
	public void createView() throws MSException {
		GridLayout layout = new GridLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 10;
		layout.verticalSpacing = 10;
		layout.numColumns = 1;
		parent.setLayout(layout);

		createChart(this.parent);

		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		chartComposite.setLayoutData(gridData);		
	}

	/**
	 * @param _sMzXMLFile - String value for an MS File
	 * @param _iScanNum - Integer value for desired scan number
	 * @return boolean - whether or not the spectrum viewer has the same mzXML file and scan number
	 */
	public boolean equals( String _sMzXMLFile, Integer _iScanNum ) {
		if ( this.iScanNum == null || _iScanNum == null ) 
			return false;
		return (isSameRunAs( _sMzXMLFile ) && this.iScanNum.equals(_iScanNum));							
	}

	public class MSSpectraViewerSashForm extends SashForm {
		private MassSpecSpectraView view;

		public MSSpectraViewerSashForm(Composite parent, int style, MassSpecSpectraView view ) {
			super(parent, style);
			this.view = view;
		}

		public MassSpecSpectraView getMSSpectraView() {
			return view;
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDirty(boolean b) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setEntry(Entry entry) {
		this.entry = entry;
	}
}
