package org.grits.toolbox.entry.ms.adaptor;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.dialog.ProjectExplorerDialog;

import org.grits.toolbox.entry.ms.property.MassSpecProperty;

/**
 * Let a user selects a MS entry in a dialog
 * @author kitaemyoung
 *
 */
public class MassSpecSelectionAdapter extends SelectionAdapter {
	private Composite parent = null;
	private Entry entry = null;
	private Text text;
	
	public void widgetSelected(SelectionEvent event) 
	{
		Shell newShell = new Shell(parent.getShell(),SWT.PRIMARY_MODAL | SWT.SHEET);
		ProjectExplorerDialog dlg = new ProjectExplorerDialog(newShell);
		// Set the parent as a filter
		dlg.addFilter(MassSpecProperty.TYPE);
		// Change the title bar text
		dlg.setTitle("MS Selection");
		// Customizable message displayed in the dialog
		dlg.setMessage("Choose a MS");
		// Calling open() will open and run the dialog.
		if (dlg.open() == Window.OK) {
			entry = dlg.getEntry();
			if (entry != null) {
				// Set the text box as the project text
				text.setText(entry.getDisplayName());
			}
		}
	}

	public Entry getEntry() {
		return entry;
	}

	public void setEntry(Entry entry) {
		this.entry = entry;
	}

	public Text getText() {
		return text;
	}

	public void setText(Text text) {
		this.text = text;
	}

	public Composite getParent() {
		return parent;
	}

	public void setParent(Composite parent) {
		this.parent = parent;
	}
	
}
