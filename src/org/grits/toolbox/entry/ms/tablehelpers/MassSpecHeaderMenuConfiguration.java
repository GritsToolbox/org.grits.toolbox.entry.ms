package org.grits.toolbox.entry.ms.tablehelpers;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuBuilder;

import org.grits.toolbox.display.control.table.tablecore.GRITSHeaderMenuConfiguration;

public class MassSpecHeaderMenuConfiguration extends GRITSHeaderMenuConfiguration {	
	public MassSpecHeaderMenuConfiguration(NatTable natTable) {
		super(natTable);
	}
	
	@Override
	protected PopupMenuBuilder createColumnHeaderMenu(NatTable natTable) {
		PopupMenuBuilder pmb = super.createColumnHeaderMenu(natTable);
		pmb.withMenuItemProvider( MassSpecMenuItemProviders.showDefaultColumnsMenuItemProvider() );		
		return pmb;
	}
	
}
