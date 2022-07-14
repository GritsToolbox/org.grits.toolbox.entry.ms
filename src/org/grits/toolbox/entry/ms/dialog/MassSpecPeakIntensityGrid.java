package org.grits.toolbox.entry.ms.dialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridEditor;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.datamodel.ms.tablemodel.MassSpecTableDataObject;
import org.grits.toolbox.datamodel.ms.tablemodel.dmtranslate.DMPeak;
import org.grits.toolbox.datamodel.ms.tablemodel.dmtranslate.DMPrecursorPeak;
import org.grits.toolbox.display.control.table.datamodel.GRITSColumnHeader;
import org.grits.toolbox.display.control.table.datamodel.GRITSListDataRow;
import org.grits.toolbox.entry.ms.extquantfiles.process.CorrectedQuantColumnInfo;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.entry.ms.property.datamodel.ExternalQuantAlias;
import org.grits.toolbox.entry.ms.property.datamodel.ExternalQuantFileToAlias;
import org.grits.toolbox.entry.ms.property.datamodel.InternalStandardQuantFileList;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecUISettings;
import org.grits.toolbox.entry.ms.property.datamodel.QuantFilePeaksToCorrectedIntensities;
import org.grits.toolbox.entry.ms.property.datamodel.QuantFileToCorrectedPeaks;

/**
 * Extension of the Grid component that fills the table with MS peaks according to user-specified parameters.
 * The user can then specify the intensity of the peak to override what was determined from the MS file.
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 * @see MassSpecPeakIntensityApplyDialog
 * 
 */
public class MassSpecPeakIntensityGrid extends Grid implements IDynamicTableProcessor, IPropertyChangeListener {
	private static final Logger logger = Logger.getLogger(MassSpecPeakIntensityGrid.class);

	private static String[] columnHeaders = { "File Name", "Type", "m/z", "Source Intensity", "Corrected Intensity" };
	public static String TYPE_STD = "Standard";
	public static String TYPE_EXT = "External";
	public static String TYPE_PRE = "MSn Precursor";
	public static String TYPE_PEAK = "MS Peak";

	protected GridEditor editor = null;
	protected boolean bIsEditingTable = false;
	private static int CELL_EDIT_COLUMN = 4;
	public final static String PROPERTY_WIN_CLOSED = "Window Closed";
	public final static String PROPERTY_LOCAL_CHANGE = "Local changes";

	private org.eclipse.swt.graphics.Color bgColor = null;

	protected MassSpecPeakIntensityApplyDialog parentDialog = null;

	protected MassSpecTableDataObject msTableDataObject = null;

	protected boolean bHasStandards = false;
	protected boolean bShowStandards = false;

	protected boolean bHasExternal = false;
	protected boolean bShowExternal = false;
	protected boolean bShowInternal = false;
	protected Double dStartMz = null;
	protected Double dEndMz = null;
	private List<IPropertyChangeListener> listeners = null;

	//	 key1: file name, key2: type, key 3: m/z, value: [source int, user int]
	protected HashMap<String, HashMap<String, HashMap<Double, Double[]>>> peakIntensityData = null;
	//	 key1: file name, key2: type, key 3: m/z, value: text from cell
	protected HashMap<String, HashMap<String, HashMap<Double, String>>> peaksChanged = null;

	// key1: column name, value: [mz col, int col]
	protected HashMap<GRITSColumnHeader, int[]> peakColumnData = null;


	public MassSpecPeakIntensityGrid(Composite parent, MassSpecPeakIntensityApplyDialog parentDialog ) {
		super(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL );
		this.parentDialog = parentDialog;
	}

	public void setMSTableDataObject(MassSpecTableDataObject simianTableDataObject) {
		this.msTableDataObject = simianTableDataObject;
	}	
	
	public MassSpecTableDataObject getMSTableDataObject() {
		return msTableDataObject;
	}

	public void setShowExternal(boolean bShowExternal) {
		this.bShowExternal = bShowExternal;
	}

	public void setShowStandards(boolean bShowStandards) {
		this.bShowStandards = bShowStandards;
	}

	public void setShowInternal(boolean bShowInternal) {
		this.bShowInternal = bShowInternal;
	}

	public void setStartMz(Double dStartMz) {
		this.dStartMz = dStartMz;
	}

	public void setEndMz(Double dEndMz) {
		this.dEndMz = dEndMz;
	}

	public boolean hasStandards() {
		return bHasStandards;
	}

	public boolean hasExternal() {
		return bHasExternal;
	}

	protected boolean passesMzFilter( double dMz ) {
		if( this.dStartMz != null && dMz < this.dStartMz ) {
			return false;
		}
		if( this.dEndMz != null && dMz > this.dEndMz ) {
			return false;
		}
		return true;
	}

	public HashMap<String, HashMap<String, HashMap<Double, Double[]>>> getPeakIntensityData() {
		return peakIntensityData;
	}

	/**
	 * Sets the data in the peaks table
	 */
	protected void initPeakData() {
		peakIntensityData = new HashMap<>();
		peakColumnData = new HashMap<>();
		peaksChanged = new HashMap<>();
		if( getMSTableDataObject() == null ) {
			return;
		}
		List<GRITSColumnHeader> headerRow = getMSTableDataObject().getLastHeader();
		// look up the m/z headers first
		for( int i = 0; i < headerRow.size(); i++ ) {
			GRITSColumnHeader header = headerRow.get(i);
			if( header.getKeyValue().contains("_mz") ) {
				int[] iColNums = new int[3];
				iColNums[0] = i;				
				peakColumnData.put(header, iColNums);
			}
		}

		// now find the columns of the source intensities
		for( int i = 0; i < headerRow.size(); i++ ) {
			GRITSColumnHeader header = headerRow.get(i);
			Set<GRITSColumnHeader> keys = peakColumnData.keySet();
			for( GRITSColumnHeader keyHeader : keys ) {
				String sIntHeader = keyHeader.getKeyValue().replace("mz", "intensity");
				int[] iColNums = peakColumnData.get(keyHeader);
				if( header.getKeyValue().equals(sIntHeader) ) {
					iColNums[1] = i;									
				} else if ( header.getKeyValue().startsWith(sIntHeader) && header.getKeyValue().endsWith(CorrectedQuantColumnInfo.CORRECTED_QUANT_KEY_PREFIX) ) {
					iColNums[2] = i;														
				}
				if( ! keyHeader.getKeyValue().equals(DMPeak.peak_mz.name()) && 
						! keyHeader.getKeyValue().equals(DMPrecursorPeak.precursor_peak_mz.name()) && 
						keyHeader.getKeyValue().contains("_mz") ) {
					if( keyHeader.getKeyValue().contains("_internal") ) {
						this.bHasStandards = true;
					} else {
						this.bHasExternal = true;
					}
				}
			}
		}		

		// now process the table to get intensities
		for( int i = 0; i < getMSTableDataObject().getTableData().size(); i++ ) {
			GRITSListDataRow row = getMSTableDataObject().getTableData().get(i);
			Set<GRITSColumnHeader> keys = peakColumnData.keySet();
			for( GRITSColumnHeader keyHeader : keys ) {
				String sFileName = getFileFromColumnHeader(keyHeader);
				if ( sFileName == null ) {
					continue;
				}
				Double dIntStdMz = null;
				String sType = null;
				if( keyHeader.getKeyValue().equals(DMPeak.peak_mz.name()) ) {
					if( this.bShowInternal ) {
						sType = TYPE_PEAK;
					}
				} else if ( keyHeader.getKeyValue().equals(DMPrecursorPeak.precursor_peak_mz.name()) ) {
					if( this.bShowInternal ) {
						sType = TYPE_PRE;
					}
				} else if ( keyHeader.getKeyValue().contains("_mz") ) {
					if( keyHeader.getKeyValue().contains("_internal") ) {
						if( this.bShowStandards ) {
							sType = TYPE_STD;	
							int iInx1 = keyHeader.getKeyValue().indexOf("_internal-") + 10;
							String sPiece1 = keyHeader.getKeyValue().substring(iInx1);
							int iInx2 = sPiece1.indexOf("_quant");
							String sPiece2 = sPiece1.substring(0, iInx2);
							try {
								dIntStdMz = Double.parseDouble(sPiece2);
							} catch( NumberFormatException ex ) {
								logger.error(ex.getMessage(), ex);
							}
						}
					} else if( this.bShowExternal ) {
						sType = TYPE_EXT;									
					}
				}
				if( sType == null ) {
					continue;
				}

				HashMap<String, HashMap<Double, Double[]>> mFileToType = null;
				if( peakIntensityData.containsKey(sFileName) ) {
					mFileToType = peakIntensityData.get(sFileName);
				} else {
					mFileToType = new HashMap<String, HashMap<Double, Double[]>>();
					peakIntensityData.put(sFileName, mFileToType);
				}

				HashMap<Double, Double[]> dMztoInt = null;
				if( mFileToType.containsKey(sType) ) {
					dMztoInt = mFileToType.get(sType);
				} else {
					dMztoInt = new HashMap<Double, Double[]>();
					mFileToType.put(sType, dMztoInt);
				}
				int[] iColNums = peakColumnData.get(keyHeader);
				if( iColNums != null ) {
					Object oMz = row.getDataRow().get(iColNums[0]);
					Object oInt = row.getDataRow().get(iColNums[1]);
					if( oMz != null && oInt != null ) {
						Double dMz = (Double) oMz;
						if( ! passesMzFilter(dMz) ) {
							continue;
						}
						Double dInt = (Double) oInt;
						Double dCorInt = null;
						if( iColNums[2] > 0 && row.getDataRow().get(iColNums[2]) != null ) {
							dCorInt = (Double) row.getDataRow().get(iColNums[2]);
						}
						Double[] dVals = new Double[] {dInt, dCorInt};
						if( dIntStdMz != null ) {
							dMztoInt.put(dIntStdMz, dVals);							
						} else {
							dMztoInt.put(dMz, dVals);
						}
					}
				}
			}			
		}

	}

	/**
	 * Called when user clicks the apply button in the containing component 
	 * @return true if values changed
	 * @see MassSpecPeakIntensityApplyDialog
	 */
	public boolean applyPressed() {
		boolean bChanged = false;
		try {
			MassSpecUISettings entrySettings = parentDialog.getSourceMassSpecEntrySettings();

			for( int i = 0; i < getItems().length; i++ ) {
				GridItem gi = getItem(i);
				String sFileName = gi.getText(0);
				String sType = gi.getText(1);
				String sMz = gi.getText(2);
				Double dMz = null;
				String sVal = gi.getText(4);
				Double dCorrInt = null;
				try {
					dMz = Double.parseDouble(sMz);
					if( ! sVal.equals("") ) {
						dCorrInt = Double.parseDouble(sVal);
					}
				} catch( NumberFormatException ex ) {
					logger.error(ex.getMessage(), ex);
				}
				QuantFileToCorrectedPeaks qfcp = null;
				if( entrySettings.getQuantFileToCorrectedPeaks() == null ) {
					entrySettings.setQuantFileToCorrectedPeaks( new HashMap<>() );
				}
				if( ! entrySettings.getQuantFileToCorrectedPeaks().containsKey(sFileName) ) {
					qfcp = new QuantFileToCorrectedPeaks();
					entrySettings.getQuantFileToCorrectedPeaks().put(sFileName, qfcp);
				} else {
					qfcp = entrySettings.getQuantFileToCorrectedPeaks().get(sFileName);
				}
				QuantFilePeaksToCorrectedIntensities qfpci = null;
				if( ! qfcp.getPeakTypeToMZs().containsKey(sType) ) {
					qfpci = new QuantFilePeaksToCorrectedIntensities();
					qfcp.getPeakTypeToMZs().put(sType, qfpci);
				} else {
					qfpci = qfcp.getPeakTypeToMZs().get(sType);
				}

				Double dCorrectedInt = null;
				if( qfpci.getPeakMzToIntensity().containsKey(dMz) ) {
					dCorrectedInt = qfpci.getPeakMzToIntensity().get(dMz);
				}
				
				HashMap<String, HashMap<Double, Double[]>> hmStoredFiles = peakIntensityData.get(sFileName);
				HashMap<Double, Double[]> hmMZs = hmStoredFiles.get(sType);
				Double[] dInts = hmMZs.get(dMz);
				boolean bCurChanged = false;
				if( dCorrInt != null ) { // user set corrected int
					if( dInts[1] == null || dCorrectedInt == null || Double.compare(dCorrInt.doubleValue(), dCorrectedInt.doubleValue()) != 0 ) {
						qfpci.getPeakMzToIntensity().put(dMz, dCorrInt);
						bCurChanged = true;
					}
				} else if ( dCorrectedInt != null ) { // user cleared corrected int
					qfpci.getPeakMzToIntensity().put(dMz, null);
					bCurChanged = true;
				}
				bChanged |= bCurChanged;
				if( bCurChanged ) {
					dInts[1] = dCorrInt;
				}
			}		
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
		return bChanged;
	}

	protected String getFileFromColumnHeader( GRITSColumnHeader header ) {
		try {
			MassSpecUISettings entrySettings = parentDialog.getEntrySettingsForInternalStandardFiles();
			Entry entry = parentDialog.getEntryForCurrentViewer();

			String sFileName = null;
			if( entrySettings.getQuantTypeToExternalQuant() != null ) {
				Set<String> setQuantTypes = entrySettings.getQuantTypeToExternalQuant().keySet();
				for( String sQuantType : setQuantTypes ) {
					ExternalQuantFileToAlias eqfa = entrySettings.getExternalQuantToAliasByQuantType(sQuantType);
					Set<String> setQuantFileNames = eqfa.getSourceDataFileNameToAlias().keySet();
					for( String sQuantFileName : setQuantFileNames ) {
						ExternalQuantAlias eqa = eqfa.getSourceDataFileNameToAlias().get(sQuantFileName);
						if( eqa.getKey() != null && header.getKeyValue().startsWith(eqa.getKey()) ) {
							sFileName = sQuantFileName;
						}
					}
				}
				if( sFileName != null ) {
					return sFileName;
				}
			}
			if( entrySettings.getInternalStandardQuantFiles() != null ) {
				Set<String> setQuantTypes = entrySettings.getInternalStandardQuantFiles().keySet();
				for( String sQuantType : setQuantTypes ) {
					InternalStandardQuantFileList isqfl = entrySettings.getInternalStandardQuantFiles().get(sQuantType);
					Set<String> sQuantNames = isqfl.getQuantNameToFileAlias().keySet();
					for( String sQuantName : sQuantNames ) {
						ExternalQuantFileToAlias eqfa = isqfl.getQuantNameToFileAlias().get(sQuantName);
						Set<String> setQuantFileNames = eqfa.getSourceDataFileNameToAlias().keySet();
						for( String sQuantFileName : setQuantFileNames ) {
							ExternalQuantAlias eqa = eqfa.getSourceDataFileNameToAlias().get(sQuantFileName);
							if( eqa.getKey() == null ) {
								continue;
							}
							String sKeyPrefix = sQuantName + ":" + eqa.getKey();
							if( header.getKeyValue().startsWith(sKeyPrefix) ) {
								sFileName = sQuantFileName;
							}
						}					
					}
				}
				if( sFileName != null ) {
					return sFileName;
				}
			}

			if( header.getKeyValue() == DMPeak.peak_mz.name() || header.getKeyValue() == DMPrecursorPeak.precursor_peak_mz.name() ) {
				MassSpecEntityProperty msep = (MassSpecEntityProperty) entry.getProperty();
				sFileName = msep.getDataFile().getName();
				return sFileName;
			}
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
	}

	/**
	 * Adds a Grid object to the parent and populates it with the available files for the current Entry.
	 */
	public void initializeGridHeaders() {
		setLinesVisible(true);
		setHeaderVisible(true);

		for (int i = 0; i < columnHeaders.length; i++) {
			final GridColumn column = new GridColumn(this, SWT.NONE);			
			column.setText(columnHeaders[i]);
			column.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					if (column.getSort() == SWT.UP){
						column.setSort(SWT.DOWN);
					} else {
						column.setSort(SWT.UP);
					}               
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// TODO Auto-generated method stub

				}
			});
		}
		editor = new GridEditor(this);
	}

	public void initializeGrid() {
		//removeAll();
		disposeAllItems();
		initPeakData();
		if( peakIntensityData == null ) {
			return;
		}
		String sType = null;
		for( int i = 0; i < 4; i++ ) {
			if( i == 0) {
				sType = TYPE_STD; 
			} else if ( i == 1 ) {					
				sType = TYPE_EXT;					
			} else if ( i == 2 ) {
				sType = TYPE_PRE;
			} else {
				sType = TYPE_PEAK;
			}
			for( String sFileName : peakIntensityData.keySet() ) {
				HashMap<String, HashMap<Double, Double[]>> mFileToType = null;
				if( peakIntensityData.containsKey(sFileName) ) {
					mFileToType = peakIntensityData.get(sFileName);
				} else {
					mFileToType = new HashMap<String, HashMap<Double, Double[]>>();
					peakIntensityData.put(sFileName, mFileToType);
				}
				if( ! mFileToType.containsKey(sType) ) {
					continue;
				}
				HashMap<Double, Double[]> hMztoInt = mFileToType.get(sType);
				Set<Double> setMZs = hMztoInt.keySet();
				SortedSet<Double> sortedMZs = new TreeSet<Double>(setMZs);
				for( Double dMz : sortedMZs ) {
					// if the file is both internal and external and the mz is internal, then don't display as external too
					if( sType == TYPE_EXT && mFileToType.containsKey(TYPE_STD) && mFileToType.get(TYPE_STD).containsKey(dMz) ) {
						continue;
					}
					GridItem item = new GridItem(this, SWT.NONE);
					item.addListener(SWT.Selection , new Listener() {

						@Override
						public void handleEvent(Event event) {
							logger.debug("Did you click the cell?");

						}
					});

					item.setText(0, sFileName);
					item.setText(1, sType);
					item.setText(2, dMz.toString());

					Double[] dInts = hMztoInt.get(dMz);
					item.setText(3, dInts[0].toString());
					item.setText(4, dInts[1] != null ? dInts[1].toString() : "");
				}
			}

		}		
		for (int i=0; i< columnHeaders.length; i++) {
			getColumn(i).pack ();
		}     

		final Grid me = this;
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				super.mouseDown(e);
				disposeEditor();
				Point pt = new Point( e.x, e.y);

				GridColumn col = getColumn(pt);
				GridItem item = getItem( pt);
				if( item == null && col != null ) { // clicked the column header. Is there a better way to detect this?
					int iColNum = getColNumberForGridColumn(col);
					int iSortOrder = SWT.UP;
					if( col.getSort() == SWT.UP ) {
						iSortOrder = SWT.DOWN;
					} 
				}
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				bIsEditingTable= true;
				Control oldEditor = editor.getEditor();
				if ( oldEditor != null)
					oldEditor.dispose();

				Point pt = new Point( e.x, e.y);

				final GridItem item = getItem( pt);
				final Point cell = getCell( pt);
				if ( item == null || cell == null) {
					return;
				}
				if( cell.x != CELL_EDIT_COLUMN ) {
					return;
				}
				deselect(cell.y);
				// The control that will be the editor must be a child of the Table
				final Text newEditor = new Text( me, SWT.BORDER | SWT.SINGLE);
				String curText = item.getText(cell.x);
				newEditor.setText(curText);
				editor.setEditor( newEditor, item, cell.x);
				editor.grabHorizontal = true;
				editor.grabVertical = true;
				newEditor.addKeyListener(new KeyListener() {

					@Override
					public void keyReleased(KeyEvent e) {						
						item.setText(cell.x, newEditor.getText());
						setTableChanged(cell.x, cell.y, newEditor.getText());
					}

					@Override
					public void keyPressed(KeyEvent e) {
						// TODO Auto-generated method stub						
					}
				});
				newEditor.forceFocus();
				newEditor.setSelection(newEditor.getText().length());
				newEditor.selectAll();
			}
		});		
	}

	/**
	 * Called when key is released in the editable cell of the Grid table
	 * 
	 * @param x, x point of cell
	 * @param y, y point of cell
	 * @param sText, current value of the editable cell
	 */
	protected void setTableChanged( int x, int y, String sText ) {
		GridItem gi = getItem(y);
		String sFile = gi.getText(0);
		String sType = gi.getText(1);		
		String sMz = gi.getText(2);
		Double dMz = Double.parseDouble(sMz);

		HashMap<String, HashMap<Double, String>> hmFiles = null;
		if( this.peaksChanged.containsKey(sType) ) {
			hmFiles = peaksChanged.get(sType);
		} else {
			hmFiles = new HashMap<>();
			peaksChanged.put(sType, hmFiles);
		}

		HashMap<Double, String> hmPeaks = null;
		if( hmFiles.containsKey(sType) ) {
			hmPeaks = hmFiles.get(sFile);
		} else {
			hmPeaks = new HashMap<>();
			hmFiles.put(sFile, hmPeaks);
		}

		String sNewInt = sText.trim();
		hmPeaks.put(dMz, sNewInt);
		valueChanged();
	}

	/**
	 * Tests the text in the editable cell to see if it is a valid double.
	 * 
	 * @param sInt, String value of the current peak editable text
	 * @return true if sInt is a double, false otherwise
	 */
	protected boolean isValidIntensity( String sInt ) {
		if( ! sInt.equals("") ) {
			try {
				Double.parseDouble(sInt);
			} catch( NumberFormatException ex ) {
				return false;
			}				
		}		
		return true;
	}

	/**
	 * Tests the user-specified values from the Grid to see if there are any non-empty, non-double values. Returns an 
	 * error message if there is, otherwise return empty text.
	 * 
	 * @return an error message if a cell contains a non-double (non-empty) value
	 */
	public String getErrorMessage() {
		if( this.peaksChanged == null ) {
			return "";
		}

		for( String sType : this.peaksChanged.keySet() ) {
			HashMap<String, HashMap<Double, String>> hmFiles = peaksChanged.get(sType);
			for( String sFile : hmFiles.keySet() ) {
				HashMap<Double, String> hmPeaks = hmFiles.get(sFile);
				for( Double dMz : hmPeaks.keySet() ) {
					String sInt = hmPeaks.get(dMz);
					if( ! isValidIntensity(sInt) ) {
						return "Intensity value " + sInt + " is not a valid double.";
					}
				}
			}
		}
		return "";
	}

	/**
	 * Compares the current user-specified intensity of a cell (dOldInt, which could be null) to 
	 * the current, String value of the corresponding cell in the table (sNewInt, can be null)
	 * 
	 * @param dOldInt, the current user-specified intensity
	 * @param sNewInt, the current user-specified value of the corresponding cell
	 * @return true if the values differ, false otherwise
	 */
	public boolean isChanged( Double dOldInt, String sNewInt ) {
		Double dNewInt = null;
		if( ! sNewInt.equals("") ) {
			try {
				dNewInt = Double.parseDouble(sNewInt);
			} catch( NumberFormatException ex ) {
				;
			}				
		}
		if( dNewInt != null && dOldInt != null ) {
			return Double.compare(dNewInt.doubleValue(), dOldInt.doubleValue()) != 0;
		}
		return (dNewInt != null && dOldInt == null) || (dNewInt == null && dOldInt != null);		
	}

	/**
	 * Iterates through the current user-specified intensity values and checks to see if any
	 * differ from what is currently stored.
	 * 
	 * @return true if the user has changed any intensity values
	 */
	public boolean isChanged() {
		if( this.peaksChanged == null ) {
			return false;
		}

		for( String sType : this.peaksChanged.keySet() ) {
			HashMap<String, HashMap<Double, String>> hmFiles = peaksChanged.get(sType);
			for( String sFile : hmFiles.keySet() ) {
				HashMap<Double, String> hmPeaks = hmFiles.get(sFile);
				for( Double dMz : hmPeaks.keySet() ) {
					String sNewInt = hmPeaks.get(dMz);
					HashMap<String, HashMap<Double, Double[]>> hmStoredFiles = peakIntensityData.get(sFile);
					HashMap<Double, Double[]> hmMZs = hmStoredFiles.get(sType);
					Double[] dInts = hmMZs.get(dMz);
					Double dOldInt = dInts[1];
					if( isChanged(dOldInt, sNewInt) ) {
						return true;
					}
				}
			}
		}
		return false;
	}

	
	/**
	 * Disposes an open editor object if a cell is no longer being edited
	 *
	 */
	protected void disposeEditor() {
		if( editor == null ) {
			return;
		}
		Control oldEditor = editor.getEditor();
		if ( oldEditor != null) {
			oldEditor.dispose();
			editor.setEditor(null);
			bIsEditingTable = false;
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		// TODO Auto-generated method stub
		super.setEnabled(enabled);
		if( ! enabled ) {
			setBackground(bgColor);
			//			updateAppliedFileList(null);
		}
	}

	/**
	 * Finds the correct column number for the requested GridColumn
	 * @param col
	 * 		the column number in the grid
	 * @return the column number
	 */
	protected int getColNumberForGridColumn( GridColumn col ) {
		for( int i = 0; i < columnHeaders.length; i++ ) {
			if( col.getText().equals( columnHeaders[i] ) ) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public List<String> getColumnKeyLabels() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Allows other classes to register themselves for when data is changed for this MS entry.
	 * 
	 * @param listener
	 */
	public void addListener( IPropertyChangeListener listener ) {
		if ( listeners == null ) {
			listeners = new ArrayList<IPropertyChangeListener>();
		}
		listeners.add(listener);
	}

	/**
	 * @return the list of property change listeners for the current MS viewer
	 */
	public List<IPropertyChangeListener> getListeners() {
		return listeners;
	}	

	/**
	 * Sends a PropertyChangeEvent to all listeners if a value in the table has changed.
	 */
	protected void valueChanged() {
		PropertyChangeEvent pce = new PropertyChangeEvent(this, PROPERTY_LOCAL_CHANGE, Boolean.FALSE, Boolean.TRUE);
		for( IPropertyChangeListener listener : getListeners() ) {
			listener.propertyChange(pce);
		}
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if ( getListeners() != null ) {
			for( IPropertyChangeListener listener : getListeners() ) {
				listener.propertyChange(new PropertyChangeEvent(this, PROPERTY_LOCAL_CHANGE, Boolean.FALSE, Boolean.TRUE));
			}
		}
	}
}
