package org.grits.toolbox.entry.ms.dialog;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

public class GlycresoftDialog extends TitleAreaDialog {
	
	String averagineModel = "glycan";
	private Combo averagineCombo;

	public GlycresoftDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Glycresoft Arguments");
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		//find the center of a main monitor
		Monitor primary = getShell().getDisplay().getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = getShell().getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		getShell().setLocation(x, y);
		
		GridLayout gd = new GridLayout (5, false);
		container.setLayout(gd);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label averagineLabel = new Label(container, SWT.NONE);
		averagineLabel.setText("Averagine Model");
		averagineCombo = new Combo(container, SWT.NONE);
		averagineCombo.setItems(new String[] {"glycan", "permethylated-glycan", "heparin", "peptide", "glycopeptide"});
		averagineCombo.select(0);
		return area;
	}
	
	@Override
	protected void okPressed() {
		int sIndx = averagineCombo.getSelectionIndex();
		if (sIndx != -1)
			averagineModel = averagineCombo.getItem(sIndx);
		super.okPressed();
	}
	
	public String getAveragineModel() {
		return averagineModel;
	}
}
