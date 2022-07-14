package org.grits.toolbox.entry.ms.dialog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.ProjectProperty;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.datamodel.property.PropertyDataFile;
import org.grits.toolbox.core.datamodel.util.DataModelSearch;
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
 * clear external quantitation for the current entry. 
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 *
 */
public class MassSpecExternalQuantDialog extends MassSpecViewerDialog implements IDynamicTableProcessor {
	private static final Logger logger = Logger.getLogger(MassSpecExternalQuantDialog.class);

	private Grid extQuantFileGrid = null;
	protected GridLayout gridLayout = null;

	protected List<MSPropertyDataFile> storedExtQuantFiles = null;
	protected List<MSPropertyDataFile> appliedExtQuantFiles = null;
	protected FileLockManager fileLockManager = null;
	protected boolean bIsEditingTable = false;
	protected GridEditor editor = null;
	private static String[] columnHeaders = { "Applied", "External Quant File", "Type", "Alias" };
	private static int CELL_EDIT_COLUMN = 3;
	public final static String PROPERTY_WIN_CLOSED = "Window Closed";
	protected String lastUpdatedAlias = null;
	private String lockFilePath = null;
	
	public MassSpecExternalQuantDialog(Shell parentShell, MassSpecMultiPageViewer contextViewer) {
		super(parentShell, contextViewer);
		setShellStyle(SWT.APPLICATION_MODAL | SWT.RESIZE | SWT.DIALOG_TRIM);
		loadExternalQuantFiles();
		this.fileLockManager = loadLockFile();
	}

	/**
	 * Initializes the grid layout for the window.
	 */
	protected void initGridLayout() {
		gridLayout = new GridLayout(1, false);
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
	 * Returns the list of external quantification files from the source MS entry  
	 * @return the list of available external quantification files in MS entry
	 */
	protected List<MSPropertyDataFile> getQuantificationFiles() {
		MassSpecProperty msp = (MassSpecProperty) getEntryParentProperty();
		MassSpecUISettings entrySettings = msp.getMassSpecMetaData();
		return entrySettings.getQuantificationFiles();
	}

	/**
	 * Returns the MassSpecUISettings object to be used to list which external quantification files are associated with the entry.
	 * @return the MassSpecUISettings for the current entry
	 */
	protected MassSpecUISettings getEntrySettings() {
		MassSpecProperty msp = (MassSpecProperty) getEntryParentProperty();
		MassSpecUISettings entrySettings = msp.getMassSpecMetaData();
		return entrySettings;
	}
		
	/**
	 * Loads the list of available quantitation files for the Entry for the currently open viewer
	 */
	protected void loadExternalQuantFiles() {
		storedExtQuantFiles = getQuantificationFiles();
		
	}

	/**
	 * Adds a Grid object to the parent and populates it with the available External Quantitation files for the current Entry.
	 * 
	 * @param parent
	 * 		the control to which to add the Grid object
	 */
	protected void createQuantTableControl( Composite parent ) {
		initGridLayout();
		parent.setLayout(gridLayout);
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		Label purposeLabel = new Label(parent, SWT.NONE);
		purposeLabel.setText("Available External Quantitiation Files: ");
		purposeLabel.setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		extQuantFileGrid = new Grid(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL );
		extQuantFileGrid.setLinesVisible(true);
		extQuantFileGrid.setHeaderVisible(true);
		extQuantFileGrid.setLayoutData(gd2);
		for (int i = 0; i < columnHeaders.length; i++) {
			GridColumn column = null;
			if( i == 0 ) {
				column = new GridColumn(extQuantFileGrid, SWT.CHECK | SWT.CENTER);
				column.setCheckable(true);
			} else {
				column = new GridColumn(extQuantFileGrid, SWT.NONE);
			}
			column.setText(columnHeaders[i]);
		}

		MassSpecUISettings entrySettings = getEntrySettings();
		editor = new GridEditor(extQuantFileGrid);

		for( int i = 0; i < storedExtQuantFiles.size(); i++ ) {
			PropertyDataFile pdf = storedExtQuantFiles.get(i);
			if( ! ( pdf instanceof MSPropertyDataFile ) ) {
				logger.error("Property data file not of correct type!");
				continue;
			}
			MSPropertyDataFile mspdf = (MSPropertyDataFile) pdf;

			String sFilePath = mspdf.getName();
			String sFileName = MSPropertyDataFile.getFormattedName(mspdf);
			String sExtQuantType = MassSpecUISettings.getExternalQuantType(mspdf);
			
			ExternalQuantFileToAlias mAliases = entrySettings.getExternalQuantToAliasByQuantType(sExtQuantType);
			String sAlias = sFileName;
			if( mAliases != null && mAliases.getSourceDataFileNameToAlias().containsKey(sFilePath) ) {
				ExternalQuantAlias aliasInfo = mAliases.getSourceDataFileNameToAlias().get(sFilePath);
				sAlias = aliasInfo.getAlias();
			}
//			String sAlias = isInUse ? entrySettings.getSourceDataFileNameToAlias().get(sFilePath) : sFileName;
			GridItem item = new GridItem(extQuantFileGrid, SWT.NONE);
			boolean isInUse = mAliases != null && mAliases.getSourceDataFileNameToAlias().containsKey(sFilePath);
			item.setChecked(isInUse);
			item.addListener(SWT.Selection , new Listener() {

				@Override
				public void handleEvent(Event event) {
					logger.debug("Did you click the cell?");

				}
			});

			item.setText(1, sFileName);
			item.setText(2, mspdf.getType());
			item.setText(3, sAlias);				
		}		
		for (int i=0; i< columnHeaders.length; i++) {
			extQuantFileGrid.getColumn (i).pack ();
		}     

		extQuantFileGrid.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				super.mouseDown(e);
				disposeEditor();
				Point pt = new Point( e.x, e.y);

				GridColumn col = extQuantFileGrid.getColumn(pt);
				GridItem item = extQuantFileGrid.getItem( pt);
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

				final GridItem item = extQuantFileGrid.getItem( pt);
				final Point cell = extQuantFileGrid.getCell( pt);
				if ( item == null || cell == null) {
					return;
				}
				if( cell.x != CELL_EDIT_COLUMN ) {
					return;
				}
				extQuantFileGrid.deselect(cell.y);
				// The control that will be the editor must be a child of the Table
				final Text newEditor = new Text( extQuantFileGrid, SWT.BORDER | SWT.SINGLE);
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
	}

	/**
	 * Disposes an open editor object if a cell is no longer being edited
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
	protected Control createDialogArea(Composite parent) {
		setTitle("Select External Quantification Files");
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		//find the center of a main monitor
		Monitor primary = getShell().getDisplay().getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = getShell().getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		getShell().setLocation(x, y);

		GridLayout gd = new GridLayout (4, false);
		container.setLayout(gd);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		createQuantTableControl(container);
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);
		return area;
	}
		
	/**
	 * @return true if any of the checked/unchecked external quantitation files or their aliases has changed, false otherwise
	 */
	protected boolean externalQuantChanged() {
		boolean bChanged = false;
		try {
			MassSpecUISettings entrySettings = getEntrySettings();
			
			Property msp = getEntryParentProperty();
			Entry entry = findParentEntry (getEntryForCurrentViewer(), msp);
			for( int i = 0; i < extQuantFileGrid.getItems().length; i++ ) {
				GridItem gi = extQuantFileGrid.getItem(i);
				String sGridFileName = gi.getText(1);
				boolean isInUse = gi.getChecked(0);
				String sGridAlias = gi.getText(3);			
				for( int j = 0; j < storedExtQuantFiles.size(); j++ ) {
					PropertyDataFile pdf = storedExtQuantFiles.get(j);
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
//					boolean wasInUse = entrySettings.getQuantTypeToExternalQuant() != null && 
//							entrySettings.getQuantTypeToExternalQuant().containsKey(sFilePath);
					String sExtQuantType = MassSpecUISettings.getExternalQuantType(mspdf);
					boolean wasInUse = false;
					ExternalQuantFileToAlias mAliases = entrySettings.getExternalQuantToAliasByQuantType(sExtQuantType);
					String sAlias = sFileName;
					if( mAliases != null && mAliases.getSourceDataFileNameToAlias().containsKey(sFilePath) ) {
						ExternalQuantAlias aliasInfo = mAliases.getSourceDataFileNameToAlias().get(sFilePath);
						sAlias = aliasInfo.getAlias();
						wasInUse = true;
					}
//					String sAlias = wasInUse ? mAliases != null && entrySettings.getSourceDataFileNameToAlias().get(sFilePath) : sFileName;
					boolean bChangedUse = isInUse ^ wasInUse;
					boolean bThisChanged = bChangedUse || (! sAlias.equals(sGridAlias));
					if( bThisChanged ) {
						lastUpdatedAlias = null;
						if( ! isInUse ) {
							mAliases.getSourceDataFileNameToAlias().remove(sFilePath);
							fileLockManager.removeLock(sFilePath, entry);
							FileLockingUtils.writeLockFile(fileLockManager, this.lockFilePath);
							if( entrySettings.getSourceDataFileList() != null && entrySettings.getSourceDataFileList().contains(mspdf) ) {
								entrySettings.getSourceDataFileList().remove(mspdf);
							}
//							entrySettings.updateExternalQuantAliases(mspdf);
							entrySettings.updateQuantAliasKeyInfo(mspdf, mAliases, null);
						} else {
							ExternalQuantAlias eqa = new ExternalQuantAlias();
							eqa.setAlias(sGridAlias);
							entrySettings.addExternalQuantFile(mspdf, eqa);
//							entrySettings.updateExternalQuantAliases(mspdf);
							mAliases = entrySettings.getExternalQuantToAliasByQuantType(sExtQuantType);
							entrySettings.updateQuantAliasKeyInfo(mspdf, mAliases, null);
							
							fileLockManager.lockFile(sFilePath, entry);
							FileLockingUtils.writeLockFile(fileLockManager, this.lockFilePath);
							if( entrySettings.getSourceDataFileList() == null || ! entrySettings.getSourceDataFileList().contains(mspdf) ) {
								entrySettings.addSourceFile(mspdf);								
							}
							lastUpdatedAlias = sGridAlias;
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
	
	@Override
	protected void okPressed() {
		if( externalQuantChanged() ) {
			updateSettings();
			updateViewer();	
		}
		super.okPressed();
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
		for( int i = 0; i < extQuantFileGrid.getItems().length; i++ ) {
			GridItem gi = extQuantFileGrid.getItem(i);
			if( gi.equals(item) ) {
				return i;
			}
		}
		return -1;

	}
	
	@Override
	public boolean close() {
		if ( getListeners() != null ) {
			for( IPropertyChangeListener listener : getListeners() ) {
				listener.propertyChange(new PropertyChangeEvent(this, 
						MassSpecExternalQuantDialog.PROPERTY_WIN_CLOSED, Boolean.FALSE, Boolean.TRUE));
			}
		}
		return super.close();
	}

	@Override
	public List<String> getColumnKeyLabels() {
		if( this.lastUpdatedAlias == null ) {
			return null;			
		}
		
		List<String> sKeys = new ArrayList<>();
		sKeys.add(this.lastUpdatedAlias);
		return sKeys;
	}
	
}
