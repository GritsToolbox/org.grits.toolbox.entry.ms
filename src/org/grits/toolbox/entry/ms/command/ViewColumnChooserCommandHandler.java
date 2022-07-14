/*******************************************************************************
 * Copyright (c) 2012 Original authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Original authors and others - initial API and implementation
 ******************************************************************************/
package org.grits.toolbox.entry.ms.command;

import org.grits.toolbox.display.control.table.command.GRITSTableDisplayColumnChooserCommand;
import org.grits.toolbox.display.control.table.command.GRITSTableDisplayColumnChooserCommandHandler;
import org.grits.toolbox.display.control.table.dialog.GRITSTableColumnChooser;
import org.grits.toolbox.display.control.table.tablecore.GRITSTable;
import org.grits.toolbox.display.control.table.tablecore.IGritsTable;
import org.grits.toolbox.entry.ms.tablehelpers.MassSpecTable;
import org.grits.toolbox.entry.ms.tablehelpers.gui.MassSpecTableColumnChooser;

public class ViewColumnChooserCommandHandler 
	extends GRITSTableDisplayColumnChooserCommandHandler {
	
	public ViewColumnChooserCommandHandler(
			IGritsTable gritsTable ) {

		this(false, gritsTable);
	}

	public ViewColumnChooserCommandHandler(
			boolean sortAvalableColumns,
			IGritsTable gritsTable ) {
		super( sortAvalableColumns, gritsTable );
	}
	
	@Override
	public boolean doCommand(GRITSTableDisplayColumnChooserCommand command) {
		boolean bRes = super.doCommand(command);
		boolean bReset = true;
		if( bRes ) {
			bReset = !updatePreferences();
		} 		
		if( bReset ){
			resetPreferences();
		}
		this.gritsTable.performAutoResize();			
		return true;

	}
	
	protected boolean updatePreferences() {
		if ( ( (MassSpecTable) this.gritsTable).getPreference() == null || 
				! GRITSTable.updatePreferencesFromColumnChooser.getUpdate() )
			return false;
		this.gritsTable.updatePreferenceSettingsFromCurrentView();
		this.gritsTable.getGRITSTableDataObject().getTablePreferences().writePreference();	
		return true;
	}

	protected void resetPreferences() {
		this.gritsTable.updateViewFromPreferenceSettings();
	}
	
	@Override
	public GRITSTableColumnChooser getNewGRITSTableColumnChooser(
			GRITSTableDisplayColumnChooserCommand command) {
		MassSpecTableColumnChooser columnChooser = new MassSpecTableColumnChooser(
			command.getNatTable().getShell(),
			sortAvailableColumns, false, gritsTable);
		return columnChooser;
	
	}
	
	@Override
	public Class<GRITSTableDisplayColumnChooserCommand> getCommandClass() {
		return GRITSTableDisplayColumnChooserCommand.class;
	}
}
