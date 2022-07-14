package org.grits.toolbox.entry.ms.tablehelpers.gui;

import org.eclipse.nebula.widgets.nattable.Messages;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.display.control.table.dialog.ColumnChooserDialog;
import org.grits.toolbox.display.control.table.dialog.GRITSTableColumnChooser;
import org.grits.toolbox.display.control.table.preference.TableViewerColumnSettings;
import org.grits.toolbox.display.control.table.preference.TableViewerPreference;
import org.grits.toolbox.display.control.table.tablecore.IGritsTable;
import org.grits.toolbox.entry.ms.process.loader.MassSpecTableDataProcessor;

public class MassSpecTableColumnChooser extends GRITSTableColumnChooser {

	public MassSpecTableColumnChooser(Shell shell,
			boolean sortAvailableColumns,
			boolean asGlobalPreference, IGritsTable gritsTable) {
		super(shell, sortAvailableColumns, asGlobalPreference, gritsTable);
	}

	@Override
	protected ColumnChooserDialog getNewColumnChooserDialog(Shell shell) {
		if ( asGlobalPreference ) 		
			columnChooserDialog = new ColumnChooserDialog(shell, Messages.getString("ColumnChooser.availableColumns"), Messages.getString("ColumnChooser.selectedColumns")); //$NON-NLS-1$ //$NON-NLS-2$
		else
			columnChooserDialog = new MassSpecColumnChooserDialog(shell, 
					Messages.getString("ColumnChooser.availableColumns"), 
					Messages.getString("ColumnChooser.selectedColumns"), this); //$NON-NLS-1$ //$NON-NLS-2$	
		
		return columnChooserDialog;
	}

	protected TableViewerColumnSettings getDefaultSettings() {
		MassSpecTableDataProcessor proc = (MassSpecTableDataProcessor) getGRITSTable().getTableDataProcessor();
		TableViewerPreference newPref = proc.initializePreferences();
		TableViewerColumnSettings newSettings = newPref.getPreferenceSettings();
		MassSpecTableDataProcessor.setDefaultColumnViewSettings(proc.getSimianTableDataObject().getFillType(), newSettings);
		return  newSettings;
	}
	
	protected void setDefaultPreferences() {
		TableViewerColumnSettings tvcs = getDefaultSettings();
		gritsTable.getGRITSTableDataObject().getTablePreferences().setPreferenceSettings(tvcs);
		gritsTable.getGRITSTableDataObject().getTablePreferences().setColumnSettings(gritsTable.getGRITSTableDataObject().getTablePreferences().toString());
		gritsTable.updateViewFromPreferenceSettings();
		
		getHiddenColumnEntries().clear();
		getColumnChooserDialog().removeAllLeaves();
		reInit(getGRITSTable());
		refreshColumnChooserDialog();
	}
	
}
