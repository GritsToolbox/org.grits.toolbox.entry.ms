package org.grits.toolbox.entry.ms.tablehelpers;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuBuilder;

import org.grits.toolbox.display.control.table.process.TableDataProcessor;

public class MassSpecResultsPopupMenuBuilder extends PopupMenuBuilder {
	protected TableDataProcessor simianToTableDataExtractor;

	public MassSpecResultsPopupMenuBuilder(NatTable parent, TableDataProcessor simianToTableDataExtractor) {
		super(parent);
		this.simianToTableDataExtractor = simianToTableDataExtractor;
	}

}
