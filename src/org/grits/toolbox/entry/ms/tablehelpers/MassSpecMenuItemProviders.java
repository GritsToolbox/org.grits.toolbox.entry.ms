package org.grits.toolbox.entry.ms.tablehelpers;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import org.grits.toolbox.display.control.table.tablecore.GRITSMenuItemProviders;

public class MassSpecMenuItemProviders extends GRITSMenuItemProviders {	
	
	public static IMenuItemProvider showDefaultColumnsMenuItemProvider() {
		return showDefaultColumnsMenuItemProvider("Show default columns"); //$NON-NLS-1$
	}

	public static IMenuItemProvider showDefaultColumnsMenuItemProvider(final String menuLabel) {
		return new IMenuItemProvider() {

			public void addMenuItem(final NatTable natTable, Menu popupMenu) {
				MenuItem showAllColumns = new MenuItem(popupMenu, SWT.PUSH);
				showAllColumns.setText(menuLabel);
//				showAllColumns.setImage(GUIHelper.getImage("show_column")); //$NON-NLS-1$
				showAllColumns.setEnabled(true);

				showAllColumns.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						( (MassSpecTable) natTable).updateViewFromPreferenceSettings();
						( (MassSpecTable) natTable).performAutoResize();
					}
				});
			}
		};
	}
}	