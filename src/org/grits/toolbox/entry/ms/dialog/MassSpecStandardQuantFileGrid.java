package org.grits.toolbox.entry.ms.dialog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridEditor;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.ProjectProperty;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.datamodel.property.PropertyDataFile;
import org.grits.toolbox.core.datamodel.util.DataModelSearch;
import org.grits.toolbox.entry.ms.extquantfiles.process.StandardQuantColumnInfo;
import org.grits.toolbox.entry.ms.property.FileLockManager;
import org.grits.toolbox.entry.ms.property.FileLockingUtils;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.grits.toolbox.entry.ms.property.datamodel.ExternalQuantAlias;
import org.grits.toolbox.entry.ms.property.datamodel.ExternalQuantFileToAlias;
import org.grits.toolbox.entry.ms.property.datamodel.MSPropertyDataFile;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecUISettings;
import org.grits.toolbox.entry.ms.views.tabbed.MassSpecMultiPageViewer;

/**
 * Simple pop-up dialog accessed from a Mass Spec GRITS table that allows the user to apply or 
 * clear internal standard quantitation for the current entry. 
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 *
 */
public class MassSpecStandardQuantFileGrid extends Grid implements IDynamicTableProcessor {
	private static final Logger logger = Logger.getLogger(MassSpecStandardQuantFileGrid.class);

	protected List<MSPropertyDataFile> storedStdQuantFiles = null;
	protected List<MSPropertyDataFile> appliedStdQuantFiles = null;
	protected FileLockManager fileLockManager = null;
	private static String[] columnHeaders = { "Selected", "MS File", "Type", "Purpose", "Alias" };
	protected GridEditor editor = null;
	protected boolean bIsEditingTable = false;
	private static int CELL_EDIT_COLUMN = 4;
	public final static String PROPERTY_WIN_CLOSED = "Window Closed";
	protected String lastUpdatedQuantFile = null;
	private String lockFilePath = null;
	protected String sStdQuantName = null;
	private MassSpecMultiPageViewer contextViewer = null;
	private org.eclipse.swt.graphics.Color bgColor = null;

	public MassSpecStandardQuantFileGrid(Composite parent, MassSpecMultiPageViewer contextViewer ) {
		super(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL );
		this.contextViewer = contextViewer;

		loadStandardQuantificationFiles();
		this.fileLockManager = loadLockFile();
	}

	/**
	 * @return the currently visible MassSpecMultiPageViewer
	 */
	public MassSpecMultiPageViewer getCurrentViewer() {
		try {
			EPartService partService = getContextViewer().getPartService();
			MPart mPart = partService.getActivePart();
			if( mPart != null && mPart.equals(mPart.getParent().getSelectedElement())) {
				if( mPart.getObject() instanceof MassSpecMultiPageViewer ) {
					MassSpecMultiPageViewer viewer = (MassSpecMultiPageViewer) mPart.getObject();
					if( viewer.getEntry().getProperty() != null && viewer.getEntry().getProperty() instanceof MassSpecEntityProperty ) {
						return viewer;
					}
				}
			}	
		} catch( Exception e ) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * @return the Entry associated with the currently visible MassSpecMultiPageViewer
	 */
	protected Entry getEntryForCurrentViewer() {
		MassSpecMultiPageViewer viewer = getCurrentViewer();
		if ( viewer == null ) {
			return null;
		}
		return viewer.getEntry();
	}

	/**
	 * @return the associated injected Context Viewer for this dialog
	 */
	public MassSpecMultiPageViewer getContextViewer() {
		return contextViewer;
	}

	/**
	 * Load the lock file for the first time
	 * @return 
	 */
	private FileLockManager loadLockFile() {
		Entry entry = getEntryForCurrentViewer();
		MassSpecEntityProperty msep = (MassSpecEntityProperty) entry.getProperty();
		MassSpecProperty pp = msep.getMassSpecParentProperty();
		try {
			this.lockFilePath = pp.getLockFilePath(entry);
			fileLockManager = FileLockingUtils.readLockFile(this.lockFilePath);
		} catch (IOException | JAXBException e) {
			logger.error("Cannot load the lock file", e);
			fileLockManager = null;
		}

		return fileLockManager;
	}

	/**
	 * @return the MassSpecProperty from the Entry associated with the current open MassSpecViewer
	 */ 
	public Property getEntryParentProperty() {
		try {
			Entry entry = getEntryForCurrentViewer();
			MassSpecEntityProperty msep = (MassSpecEntityProperty) entry.getProperty();
			MassSpecProperty pp = msep.getMassSpecParentProperty();
			return pp;
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
	}

	/**
	 * Returns the list of files from the source MS entry that the user may use for internal standard quant
	 * @return the list of available files in MS entry (external quant and annotation)
	 */
	protected List<MSPropertyDataFile> getStandardQuantificationFiles() {
		MassSpecUISettings entrySettings = getEntrySettings();
		List<MSPropertyDataFile> fileList = new ArrayList<>();
		fileList.addAll( entrySettings.getAnnotationFiles() );
		fileList.addAll( entrySettings.getQuantificationFiles() );
		return fileList;
	}

	/**
	 * Returns the MassSpecUISettings object to be used to list which files are associated with the entry.
	 * @return the MassSpecUISettings for the current entry
	 */
	protected MassSpecUISettings getEntrySettings() {
		MassSpecProperty msp = (MassSpecProperty) getEntryParentProperty();
		MassSpecUISettings entrySettings = msp.getMassSpecMetaData();
		return entrySettings;
	}

	/**
	 * Loads the list of available files for the Entry for the currently open viewer
	 */
	protected void loadStandardQuantificationFiles() {
		storedStdQuantFiles = getStandardQuantificationFiles();		
	}

	private MSPropertyDataFile getPropertyFileByName( String sGridFileName ) {
		for( int i = 0; i < storedStdQuantFiles.size(); i++ ) {
			PropertyDataFile pdf = storedStdQuantFiles.get(i);
			if( ! ( pdf instanceof MSPropertyDataFile ) ) {
				continue;
			}
			MSPropertyDataFile mspdf = (MSPropertyDataFile) pdf;
			String sFileName = MSPropertyDataFile.getFormattedName(mspdf);
			if( sFileName.equals(sGridFileName) ) {
				return mspdf;
			}
		}
		return null;
	}

	public void updateAppliedFileList( String sCurIntStdQuant ) {
		MassSpecUISettings entrySettings = getEntrySettings();
		for( int i = 0; i < getItems().length; i++ ) {
			GridItem gi = getItem(i);

			String sGridFileName = gi.getText(1);
			MSPropertyDataFile mspdf = getPropertyFileByName(sGridFileName);
			if( mspdf == null ) {
				continue;
			}

			String sFilePath = mspdf.getName();
			String sFileName = MSPropertyDataFile.getFormattedName(mspdf);
			String sExtQuantType = MassSpecUISettings.getExternalQuantType(mspdf);
			String sAlias = sFileName;
			String sPurpose = mspdf.getPurposeString();
			boolean isInUse = false;
			if( sCurIntStdQuant != null ) {
				ExternalQuantFileToAlias mAliases = entrySettings.getInternalQuantFileToAlias(sCurIntStdQuant, mspdf);					
				if( mAliases != null && mAliases.getSourceDataFileNameToAlias().containsKey(sFilePath) ) {
					ExternalQuantAlias aliasInfo = mAliases.getSourceDataFileNameToAlias().get(sFilePath);
					sAlias = aliasInfo.getAlias();
					isInUse = true;
				}
				if( ! isInUse ) { // if not saved with internal quant alias, see if there is one associated w/ external quant and use it for starters
					ExternalQuantFileToAlias mExtAliases = entrySettings.getExternalQuantToAliasByQuantType(sExtQuantType);
					if( mExtAliases != null && mExtAliases.getSourceDataFileNameToAlias().containsKey(sFilePath) ) {
						ExternalQuantAlias aliasInfo = mExtAliases.getSourceDataFileNameToAlias().get(sFilePath);
						sAlias = aliasInfo.getAlias();					
					}
				}
				//			boolean isInUse = false;
				//			if( sCurIntStdQuant != null ) {
				//				isInUse = entrySettings.containsInternalStandardQuantFile(sCurIntStdQuant, mspdf);
			}
			gi.setChecked(0, isInUse);
			gi.setText(4, sAlias);
			this.sStdQuantName = sCurIntStdQuant;

		}
	}
	/**
	 * Adds a Grid object to the parent and populates it with the available files for the current Entry.
	 */
	public void initializeGrid() {

		setLinesVisible(true);
		setHeaderVisible(true);
		for (int i = 0; i < columnHeaders.length; i++) {
			GridColumn column = null;
			if( i == 0 ) {
				column = new GridColumn(this, SWT.CHECK | SWT.CENTER);
				column.setCheckable(true);
			} else {
				column = new GridColumn(this, SWT.NONE);
			}
			column.setText(columnHeaders[i]);
		}
		MassSpecUISettings entrySettings = getEntrySettings();
		editor = new GridEditor(this);

		for( int i = 0; i < storedStdQuantFiles.size(); i++ ) {
			PropertyDataFile pdf = storedStdQuantFiles.get(i);
			if( ! ( pdf instanceof MSPropertyDataFile ) ) {
				logger.error("Property data file not of correct type!");
				continue;
			}
			MSPropertyDataFile mspdf = (MSPropertyDataFile) pdf;

			String sFileName = MSPropertyDataFile.getFormattedName(mspdf);
			String sAlias = sFileName;
			String sPurpose = mspdf.getPurposeString();
			GridItem item = new GridItem(this, SWT.NONE);

			item.addListener(SWT.Selection , new Listener() {

				@Override
				public void handleEvent(Event event) {
					logger.debug("Did you click the cell?");

				}
			});

			item.setText(1, sFileName);
			item.setText(2, mspdf.getType());
			item.setText(3, sPurpose);	
			item.setText(4, sAlias);
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
						//						if (isValidCellValue(cell.x, newEditor.getText()) && isReadyToFinish()) {
						//							setPageComplete(true);
						//						} else {
						//							setPageComplete(false);
						//						}
					}

					@Override
					public void keyPressed(KeyEvent e) {
						// TODO Auto-generated method stub						
					}
				});
				newEditor.forceFocus();
				newEditor.setSelection(newEditor.getText().length());
				newEditor.selectAll();
				//                newEditor.setSelection(0, curText.length());

			}
		});		

		setEnabled(false);
		bgColor = getBackground();
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
			updateAppliedFileList(null);
		}
	}

	protected int getNumSelectedEntries() {
		int iCnt = 0;
		for( int i = 0; i < getItems().length; i++ ) {
			GridItem gi = getItem(i);
			boolean isInUse = gi.getChecked(0);
			if( isInUse ) {
				iCnt++;
			}
		}		
		return iCnt;
	}

	/**
	 * Iterates over the files in the table and stores in a Map those that are checked.
	 * 
	 * @return Map of file names to aliases
	 */
	public Map<String, String> getSelectedQuantFileAliases() {
		try {
			Map<String, String> mFiles = new HashMap<>();
			for( int i = 0; i < getItems().length; i++ ) {
				GridItem gi = getItem(i);
				String sGridFileName = gi.getText(1);
				String sGridAlias = gi.getText(4);
				boolean isInUse = gi.getChecked(0);
				if( isInUse ) {
					mFiles.put(sGridFileName, sGridAlias);
				}
			}
			return mFiles;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * @return true if any of the checked/unchecked external quantitation files or their aliases has changed, false otherwise
	 */
	public boolean updateIntQuantFileSettings() {
		boolean bChanged = false;
		try {
			MassSpecUISettings entrySettings = getEntrySettings();

			Property msp = getEntryParentProperty();
			Entry entry = findParentEntry (getEntryForCurrentViewer(), msp);
			for( int i = 0; i < getItems().length; i++ ) {
				GridItem gi = getItem(i);
				String sGridFileName = gi.getText(1);
				String sGridAlias = gi.getText(4);
				boolean isInUse = gi.getChecked(0);
				for( int j = 0; j < storedStdQuantFiles.size(); j++ ) {
					PropertyDataFile pdf = storedStdQuantFiles.get(j);
					if( ! ( pdf instanceof MSPropertyDataFile ) ) {
						logger.error("Property data file not of correct type!");
						continue;
					}
					MSPropertyDataFile mspdf = (MSPropertyDataFile) pdf;

					String sFilePath = mspdf.getName();
					String sFileName = MSPropertyDataFile.getFormattedName(mspdf);
					if( ! sGridFileName.equals(sFileName) ) {
						continue;
					}
					String sExtQuantType = MassSpecUISettings.getExternalQuantType(mspdf);

					boolean wasInUse = false;
					String sAlias = sFileName;
					ExternalQuantFileToAlias mAliases = entrySettings.getInternalQuantFileToAlias(this.sStdQuantName, mspdf);					
					if( mAliases != null && mAliases.getSourceDataFileNameToAlias().containsKey(sFilePath) ) {
						ExternalQuantAlias aliasInfo = mAliases.getSourceDataFileNameToAlias().get(sFilePath);
						sAlias = aliasInfo.getAlias();
						wasInUse = true;
					}

					boolean wasInUseForExternalQuant = false; // we need to know if the file is used by external quant (see below)
					ExternalQuantFileToAlias mExtQuantAliases = entrySettings.getExternalQuantToAliasByQuantType(sExtQuantType);
					if( mExtQuantAliases != null && mExtQuantAliases.getSourceDataFileNameToAlias().containsKey(sFilePath) ) {
						wasInUseForExternalQuant = true;
					}
					boolean bThisChanged = isInUse ^ wasInUse || (! sAlias.equals(sGridAlias));
					if( bThisChanged ) {
						lastUpdatedQuantFile = null;
						if( ! isInUse ) {
							entrySettings.removeInternalStandardQuantFile(this.sStdQuantName, mspdf);
							fileLockManager.removeLock(sFilePath, entry);
							FileLockingUtils.writeLockFile(fileLockManager, this.lockFilePath);
							if( ! wasInUseForExternalQuant && entrySettings.getSourceDataFileList() != null && entrySettings.getSourceDataFileList().contains(mspdf) ) {
								entrySettings.getSourceDataFileList().remove(mspdf);
							}
							mAliases = entrySettings.getInternalQuantFileToAlias(this.sStdQuantName, mspdf);
							entrySettings.updateQuantAliasKeyInfo(mspdf, mAliases, StandardQuantColumnInfo.QUANT_PREFIX);
						} else {
							ExternalQuantAlias eqa = new ExternalQuantAlias();
							eqa.setAlias(sGridAlias);
							entrySettings.addInternalStandardQuantFile(this.sStdQuantName, mspdf, eqa);
							mAliases = entrySettings.getInternalQuantFileToAlias(this.sStdQuantName, mspdf);
							entrySettings.updateQuantAliasKeyInfo(mspdf, mAliases, StandardQuantColumnInfo.QUANT_PREFIX);
							fileLockManager.lockFile(sFilePath, entry);
							FileLockingUtils.writeLockFile(fileLockManager, this.lockFilePath);
							if( ! wasInUseForExternalQuant && (entrySettings.getSourceDataFileList() == null || ! entrySettings.getSourceDataFileList().contains(mspdf)) ) {
								entrySettings.addSourceFile(mspdf);								
							}
							lastUpdatedQuantFile = sGridFileName;
						}
						bChanged = true;
					}
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			bChanged = false;
		}
		return bChanged;
	}

	/** 
	 * find the Entry starting from the given entry's top level project entry, that has the given property
	 * @param entry entry to determine the project entry
	 * @param msp property to match
	 * @return Entry that has the given property, or null if not found
	 */
	private Entry findParentEntry (Entry entry, Property msp) {
		Entry parent = DataModelSearch.findParentByType(entry, ProjectProperty.TYPE);
		Entry entryFound = null;
		if (parent != null) {
			for (Entry e: parent.getChildren()) {
				entryFound = findEntryWithProperty (e, msp);
				if (entryFound != null)
					break;
			}
		}
		return entryFound;
	}

	/**
	 * recursively search for the Entry with the given property starting from the given entry and descending down to its children if necessary
	 * 
	 * @param entry Entry to start from
	 * @param msp property to match
	 * @return Entry that has the given property, or null if not found
	 */
	private Entry findEntryWithProperty(Entry entry, Property msp) {
		if (entry.getProperty().equals(msp))
			return entry;
		Entry entryFound = null;
		for (Entry e: entry.getChildren()) {
			entryFound =  findEntryWithProperty(e, msp);
			if (entryFound != null)
				break;
		}
		return entryFound;	
	}

	/**
	 * Updates the MS Settings for the Entry's mass spec property and then marshalls it to the workspace xml file
	 */
	protected void updateSettings() {
		MassSpecProperty property = (MassSpecProperty) getEntryParentProperty();
		// need to save the projectEntry to cause the data files for the MassSpecProperty to be updated
		try {
			Entry projectEntry = getEntryForCurrentViewer();

			String settingsFile = MassSpecProperty.getFullyQualifiedFolderName(projectEntry) + File.separator + property.getMSSettingsFile().getName();
			property.marshallSettingsFile(settingsFile, property.getMassSpecMetaData());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);			
		}
	}

	/**
	 * Determines the current viewer and refreshes the GRITS tables that were updated with the quantitation.
	 * For MassSpec data, this is only the scans tab.
	 */
	protected void updateViewer() {
		try {
			MassSpecMultiPageViewer viewer = getCurrentViewer();
			List<String> sKeyVals = getColumnKeyLabels();
			viewer.reLoadScansTab(sKeyVals);
		} catch( Exception e ) {
			logger.error(e.getMessage(), e);			
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

	/**
	 * 
	 * Locates the row corresponding to the specified GridItem.
	 * 
	 * @param item
	 * 		the GridItem that has been clicked in the Grid table
	 * @return the row number
	 */
	protected int getRowNumberForItem( GridItem item ) {
		for( int i = 0; i < getItems().length; i++ ) {
			GridItem gi = getItem(i);
			if( gi.equals(item) ) {
				return i;
			}
		}
		return -1;

	}

	@Override
	public List<String> getColumnKeyLabels() {
		if( this.lastUpdatedQuantFile == null ) {
			return null;			
		}

		List<String> sKeys = new ArrayList<>();
		sKeys.add(this.lastUpdatedQuantFile);
		return sKeys;
	}

	/**
	 * Checks to see if any changes were made to the selected files and then updates the settings if so.
	 *
	public void updateIntQuantFileSettings() {
		if( stdQuantFileListChanged() ) {
			updateSettings();
		}
	}
	 */
}
