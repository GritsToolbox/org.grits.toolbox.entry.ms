package org.grits.toolbox.entry.ms.tablehelpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.layer.LayerUtil;
import org.eclipse.nebula.widgets.nattable.reorder.command.ColumnReorderCommand;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.datamodel.ms.preference.MassSpecViewerPreference;
import org.grits.toolbox.datamodel.ms.tablemodel.MassSpecTableDataObject;
import org.grits.toolbox.datamodel.ms.tablemodel.dmtranslate.DMScan;
import org.grits.toolbox.display.control.table.datamodel.GRITSColumnHeader;
import org.grits.toolbox.display.control.table.preference.TableViewerColumnSettings;
import org.grits.toolbox.display.control.table.process.TableDataProcessor;
import org.grits.toolbox.display.control.table.process.TableDataProcessorRunner;
import org.grits.toolbox.display.control.table.tablecore.GRITSTable;
import org.grits.toolbox.entry.ms.command.ViewColumnChooserCommandHandler;
import org.grits.toolbox.entry.ms.command.ViewMassSpecOverviewCommandExecutor;
import org.grits.toolbox.entry.ms.process.loader.MassSpecTableDataProcessor;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.entry.ms.views.tabbed.MassSpecTableBase;
import org.grits.toolbox.widgets.tools.GRITSProcessStatus;
/**
 * Extension of GRITSTable specifically Mass Spec data. Adds connection to a
 * parent viewer (MassSpecTableBase)
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 * @see MassSpecTableBase
*/
public class MassSpecTable extends GRITSTable {

	//log4J Logger
	private static final Logger logger = Logger.getLogger(MassSpecTable.class);
	public static final String POLARITYLABEL = "PolarityValue";
	private String sMzXMLPathName = null;
	protected MassSpecTableBase parentView = null;

	public MassSpecTable(Composite parent, TableDataProcessor tableDataExtractor) {
		super(parent, tableDataExtractor);
	}

	public MassSpecTable(MassSpecTableBase parent, TableDataProcessor xmlExtractor) throws Exception {
		super(parent.getParent(), xmlExtractor);
		this.parentView = parent;
		//			createMainTable();
	}	

	@Override
	public void loadData() throws Exception {
		int iStatus = openReadWriteDialog(TableDataProcessor.READ);
		if( iStatus == GRITSProcessStatus.ERROR ) {
			throw new Exception("Failed to read table data.");
		} else if ( iStatus == GRITSProcessStatus.CANCEL ) {
			return;
		}
		setSimDataObject(((MassSpecTableDataProcessor) getTableDataProcessor()).getSimianTableDataObject());
	}
	
	@Override
	public void createMainTable() throws Exception {
		try {
			initCommonTableComponents();
			initColumnChooserLayer();
			registerDoubleStyles(configRegistry);
			registerPolarityStyle (configRegistry);
			finishNatTable();
			performAutoResizeAfterPaint();
		} catch( Exception e ) {
			logger.error("Error initializing table.");
			throw new Exception(e.getMessage());
		}
	}

	protected void registerPolarityStyle(ConfigRegistry configRegistry) {
		DisplayConverter polarityConverter = new DisplayConverter() {
			
			@Override
			public Object displayToCanonicalValue(Object displayValue) {
				if (displayValue == null) {
					return null;
				}
				if (displayValue instanceof String) {
					if (((String) displayValue).isEmpty())
						return null;
					if (((String)displayValue).equals("+"))
						return "Yes";
					else if (((String)displayValue).equals("-"))
						return "No";
				}
				return null;
			}
			
			@Override
			public Object canonicalToDisplayValue(Object canonicalValue) {
				if (canonicalValue == null)
					return "";
				if (canonicalValue instanceof String) {
					if (canonicalValue.equals("Yes"))
						return "+";
					else 
						return "-";
				}
				return null;
			}
		};
		
		configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, polarityConverter, DisplayMode.NORMAL, POLARITYLABEL);
	}

	private MassSpecTableDataObject getMyTableDataObject() {
		return (MassSpecTableDataObject) getGRITSTableDataObject();
	}

	@Override
	protected boolean rowNeedsResize(int iRowNum) {
		return super.rowNeedsResize(iRowNum);
	}

	public void writeDataToXML( String _sNewXMLFile ) {
		getTableDataProcessor().prepareThreadForWrite(_sNewXMLFile);
		openReadWriteDialog(TableDataProcessor.WRITE);
	}

	protected int openReadWriteDialog( Integer processType) {
		TableDataProcessorRunner processRunner = new TableDataProcessorRunner( (MassSpecTableDataProcessor) getTableDataProcessor()); 	
		getTableDataProcessor().setProcessType(processType);
		try {
			getTableDataProcessor().setProcessType(processType);
			int iStatus = processRunner.startJob();
			return iStatus;
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
		return GRITSProcessStatus.ERROR;
	}

	public String getMzXMLPathName() {
		return sMzXMLPathName;
	}

	public void setMzXMLPathName(String sMzXMLPathName) {
		this.sMzXMLPathName = sMzXMLPathName;
	}

	@Override
	public void initCellAccumulator() {
		MassSpecCellOverrideLabelAccumulator cellLabelAccumulator = null;

		List<Integer> intensityCols = getAllIntensityColumns(null);
		cellLabelAccumulator = new MassSpecCellOverrideLabelAccumulator(this.bodyDataProvider, 0, intensityCols);
		if (getTableDataProcessor() != null && getTableDataProcessor().getTempPreference() != null) {
			int polarityColumn = getTableDataProcessor().getTempPreference().getPreferenceSettings().getColumnPosition(DMScan.scan_polarity.name());
			if (polarityColumn != -1)
				cellLabelAccumulator.setPolarityColumn (polarityColumn);
		}

		dataLayer.setConfigLabelAccumulator(cellLabelAccumulator);	
	}
	
	/**
	 * get all column indices that shows an intensity value
	 * <p>
	 * column name should contain "intensity" (case-insensitive)
	 * and should not contain "score" (to be able to exclude "intensity score" column) for it to be included in the list
	 * @param shift if null, there is no shifting of column indices, if not, then all the indices are shifted by the value of the integer
	 * @return list of intensity column indices
	 */
	protected List<Integer> getAllIntensityColumns (Integer shift) {
		int offset = shift == null ? 0 : shift.intValue();
		List<Integer> intensityCols = new ArrayList<>();
		if (getMyTableDataObject().getPeakIntensityCols() != null && !getMyTableDataObject().getPeakIntensityCols().isEmpty())
			intensityCols.addAll(getMyTableDataObject().getPeakIntensityCols());
		if (getMyTableDataObject().getPrecursorIntensityCols() != null && !getMyTableDataObject().getPrecursorIntensityCols().isEmpty())
			intensityCols.addAll(getMyTableDataObject().getPrecursorIntensityCols());
		
		if (getTableDataProcessor() == null || 
				getTableDataProcessor().getTempPreference() == null || getTableDataProcessor().getTempPreference().getPreferenceSettings() == null)
			return intensityCols;
		
		HashMap<String, Integer> customExtraDataColumnMap = getTableDataProcessor().getTempPreference().getPreferenceSettings().getUnrecognizedHeaders();
		for (String key : customExtraDataColumnMap.keySet()) {
			if (key.toLowerCase().contains("intensity") && !key.toLowerCase().contains("score")) {
				intensityCols.add(customExtraDataColumnMap.get(key) + offset);
			}
		}
		
		for (GRITSColumnHeader column: getTableDataProcessor().getTempPreference().getPreferenceSettings().getHeaders()) {
			if (column.getKeyValue().toLowerCase().contains("intensity") && !column.getKeyValue().toLowerCase().contains("score")) {
				int columnNo = getTableDataProcessor().getTempPreference().getPreferenceSettings().getColumnPosition(column);
				if (!intensityCols.contains(columnNo + offset)) {
					intensityCols.add(columnNo + offset);
				}
			}
		}
		
		return intensityCols;
	}


	@Override
	protected void initConfigRegistry() {
		super.initConfigRegistry();
	}

	public MassSpecViewerPreference getPreference() {
		return (MassSpecViewerPreference) getGRITSTableDataObject().getTablePreferences();
	}

	public void setPreference(MassSpecViewerPreference preference) {
		//		this.preference = preference;
		getGRITSTableDataObject().setTablePreferences(preference);
	}

	@Override
	protected void initColumnChooserLayer() {
		ViewColumnChooserCommandHandler columnChooserCommandHandler = new ViewColumnChooserCommandHandler( this );		
		columnGroupHeaderLayer.registerCommandHandler(columnChooserCommandHandler);		
	}

	protected void finishNatTable() {
		super.finishNatTable();
	}

	@Override
	protected boolean addHeaderListeners() {
		return super.addHeaderListeners();
	}

	/**
	 * Identifies the column(s) whose header key text begins with the specified "sStartLabel" and moves them into the first position in the table.
	 * This method is useful for dynamic addition of columns to ensure the user sees what was added when the table refreshes.
	 * 
	 * @param _sStartLabel
	 */
	public void moveToFirstColumn( String _sStartLabel ) {
		// first show if any are hidden
		List<Integer> alHidden = new ArrayList<>();
		for (int iFromPos = 0; iFromPos < this.columnHeaderDataLayer.getColumnCount(); iFromPos++) { // column position based
			String sThisHeaderKey = this.columnHeaderDataProvider.getDataValue(iFromPos, 0);
			if(sThisHeaderKey.startsWith(_sStartLabel)) {
				if(this.columnHideShowLayer.isColumnIndexHidden(iFromPos) ) {
					alHidden.add(iFromPos);
				}
			}
		}
		if( ! alHidden.isEmpty() ) {
			/*int[] iIndices = new int[alHidden.size()];
			int i = 0;
			for( Integer iInx : alHidden ) {
				iIndices[i++] = iInx;
			}*/

			this.columnHideShowLayer.showColumnIndexes(alHidden);
		}
		for (int iFromPos = 0; iFromPos < this.columnHideShowLayer.getColumnCount(); iFromPos++) { // column position based
			int iColPos = LayerUtil.convertColumnPosition(this.columnHideShowLayer, iFromPos, this.columnHeaderDataLayer);
			String sThisHeaderKey = this.columnHeaderDataProvider.getDataValue(iColPos, 0);
			if (sThisHeaderKey.startsWith(_sStartLabel)) {
				int iToPos = 0;
				if (iFromPos != iToPos) {
					ColumnReorderCommand command = new ColumnReorderCommand(this.columnHideShowLayer, iFromPos, iToPos);
					// System.out.println("Moving " + sHeaderText + " from "
					// + iFromPos + " to " + iToPos );
					this.columnHideShowLayer.doCommand(command);
				} else {
					// System.out.println("Staying put: " + sHeaderText +
					// " from " + iFromPos + " to " + iToPos );
				}
			}
		}		
	}
	
	@Override
	public void mouseDoubleClick(MouseEvent e) {
		if ( getMyTableDataObject().getScanNoCols().isEmpty() ) 
			return;

		if ( getMzXMLPathName() == null ) 
			return;

		GridLayer gl = (GridLayer) getLayer();

		int origRow = gl.getRowPositionByY(e.y); 
		if ( origRow < 2 )
			return;

		int origCol = gl.getColumnPositionByX(e.x); 
		if ( origCol < 1 )
			return;

		int iRowPostion = LayerUtil.convertRowPosition(gl, origRow, getBottomDataLayer());
		int iColPostion = LayerUtil.convertColumnPosition(gl, origCol, getBottomDataLayer());

		int iScanNum = -1;
		double dMz = 0.0;
		if ( hasColumnGroupHeader() ) { // if a merge result, you have to double-click the scan num of the experiment
			Object obj = getBottomDataLayer().getDataValueByPosition( iColPostion, iRowPostion);				
			try {
				iScanNum = Integer.parseInt(obj.toString());
			} catch(NumberFormatException ex) {
				logger.error(ex.getMessage(), ex);
				return;
			}
			//			int iInx = getMyTableDataObject().getScanNoCols().indexOf(iScanNum);
		} else { // if not a merge result, there is only 1 scan num, so just get(0)
			Object obj = getBottomDataLayer().getDataValueByPosition( getMyTableDataObject().getScanNoCols().get(0), iRowPostion);	
			if( obj != null ) {
				try {
					iScanNum = Integer.parseInt(obj.toString());
				} catch(NumberFormatException ex) {
					logger.error(ex.getMessage(), ex);
					return;
				}
			}
			if( ! getMyTableDataObject().getMzCols().isEmpty() ) {
				Object obj2 = getBottomDataLayer().getDataValueByPosition( getMyTableDataObject().getMzCols().get(0), iRowPostion);		
				if( obj2 != null ) {
					try {
						dMz = Double.parseDouble(obj2.toString());
					} catch(NumberFormatException ex) {
						logger.error(ex.getMessage(), ex);
						//					return;
					}
				}
			}
		}
		if ( iScanNum == -1 ) 
			return;

		String sRunName = parentView.getEntry().getParent().getDisplayName();

		int iParentScanNum = -1;
		double dParentMz = 0.0;
		if( parentView.getEntry().getProperty() instanceof MassSpecEntityProperty ) {
			iParentScanNum = ( (MassSpecEntityProperty) parentView.getEntry().getProperty()).getScanNum();
			dParentMz = ( (MassSpecEntityProperty) parentView.getEntry().getProperty()).getMz();
		}
		Entry newEntry = MassSpecEntityProperty.getTableCompatibleEntry(parentView.getEntry());
		MassSpecEntityProperty msEntityProp = (MassSpecEntityProperty) newEntry.getProperty();

		msEntityProp.setParentScanNum(iParentScanNum);
		msEntityProp.setScanNum(iScanNum);
		msEntityProp.setMz(dMz);
		msEntityProp.setParentMz(dParentMz);
		msEntityProp.setDataFile(((MassSpecEntityProperty) parentView.getEntityProperty()).getDataFile());
		int iCurMSLevel = ((MassSpecEntityProperty) parentView.getEntityProperty()).getMsLevel();		
		msEntityProp.setMsLevel(Integer.valueOf(iCurMSLevel + 1));
		StringBuilder sb = new StringBuilder(newEntry.getDisplayName());
		if( dMz == 0.0 ) {
			sb.append(": ");
		} else { // fragmentation pathway
			sb.append("->");
		}
		sb.append("[Scan ");
		sb.append(iScanNum);
		sb.append(", MS");
		sb.append(iCurMSLevel);
		if( dMz > 0.0 ) {
			sb.append(", ");
			sb.append(dMz);
		}
		sb.append("]");

		newEntry.setDisplayName(sb.toString());
		showMSOverview(newEntry);
	}

	protected void showMSOverview(Entry newEntry) {
		ViewMassSpecOverviewCommandExecutor.showMSOverview(parentView.getParentEditor().getContext(), newEntry);		
	}

	@Override
	public void mouseDown(MouseEvent e) {
		super.mouseDown(e);
	}

	@Override
	public void mouseUp(MouseEvent e) {
		super.mouseUp(e);
	}	

	@Override
	public TableViewerColumnSettings getPreferenceSettingsFromCurrentView() {
		return super.getPreferenceSettingsFromCurrentView();
	}	

	public MassSpecTableBase getParentView() {
		return parentView;
	}
}
