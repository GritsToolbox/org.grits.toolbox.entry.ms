package org.grits.toolbox.entry.ms.dialog;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class MSConvertDialog extends TitleAreaDialog {

	private ControlDecoration dec1;
	private ControlDecoration dec2;
	private Integer scanNumberB;
	private Integer scanNumberE;
	private Double scanTimeB;
	private Double scanTimeE;

	public MSConvertDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("MS Convert Arguments");
		setMessage("Please enter values if you would like to apply filters before conversion.\nLeave empty to convert the full spectra.");
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
		gd.marginRight = 8;
		gd.verticalSpacing = 20;
		gd.horizontalSpacing = 20;
		container.setLayout(gd);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label scanNoLabel = new Label(container, SWT.LEFT);
		scanNoLabel.setText("Scan Number");
		
		Text scanNoBegin = new Text(container, SWT.BORDER);
		// Create a control decoration for the control.
		dec1 = new ControlDecoration(scanNoBegin, SWT.CENTER);
		// Specify the decoration image and description
		Image image = JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_ERROR);
		dec1.setImage(image);
		dec1.setDescriptionText("Should enter a positive integer");
		dec1.hide();
		scanNoBegin.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Text newText = (Text) e.widget;
				String newValue = newText.getText();
				if (newValue != null)
					newValue = newValue.trim();
				try {
					Integer newInteger = Integer.parseInt(newValue);
					if (newInteger <= 0)
						dec1.show();
					else {
						scanNumberB = newInteger;
						dec1.hide();
					}
				} catch (NumberFormatException ex) {
					dec1.show();
				}
				
			}
		});
		Label dashLabel = new Label(container, SWT.LEFT);
		dashLabel.setText("-");
		Text scanNoEnd = new Text(container, SWT.BORDER);
		scanNoEnd.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Text newText = (Text) e.widget;
				String newValue = newText.getText();
				if (newValue != null)
					newValue = newValue.trim();
				try {
					Integer newInteger = Integer.parseInt(newValue);
					if (newInteger <= 0)
						dec1.show();
					else {
						scanNumberE = newInteger;
						dec1.hide();
					}
				} catch (NumberFormatException ex) {
					dec1.show();
				}
				
			}
		});
		new Label(container, SWT.NONE);
		
		Label scanTimeLabel = new Label(container, SWT.NONE);
		scanTimeLabel.setText("Scan Time");
		
		Text scanTimeBegin = new Text(container, SWT.BORDER);	
		// Create a control decoration for the control.
		dec2 = new ControlDecoration(scanTimeBegin, SWT.CENTER);
		dec2.setImage(image);
		dec2.setDescriptionText("Should enter a positive number");
		dec2.hide();
		scanTimeBegin.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Text newText = (Text) e.widget;
				String newValue = newText.getText();
				if (newValue != null)
					newValue = newValue.trim();
				try {
					Double newDouble = Double.parseDouble(newValue);
					if (newDouble <= 0)
						dec2.show();
					else {
						scanTimeB = newDouble;
						dec2.hide();
					}
				} catch (NumberFormatException ex) {
					dec2.show();
				}
				
			}
		});
		dashLabel = new Label(container, SWT.NONE);
		dashLabel.setText("-");
		Text scanTimeEnd = new Text(container, SWT.BORDER);
		scanTimeEnd.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Text newText = (Text) e.widget;
				String newValue = newText.getText();
				if (newValue != null)
					newValue = newValue.trim();
				try {
					Double newDouble = Double.parseDouble(newValue);
					if (newDouble <= 0)
						dec2.show();
					else {
						scanTimeE = newDouble;
						dec2.hide();
					}
				} catch (NumberFormatException ex) {
					dec2.show();
				}
				
			}
		});
		
		Label infoLabel = new Label(container, SWT.NONE);
		infoLabel.setText("(in seconds)");
		return area;
	}

	@Override
	protected void okPressed() {
		if ((scanNumberB != null && scanNumberE == null) ||
				(scanNumberB == null && scanNumberE != null)) {
			setErrorMessage("Please specify the range correctly for Scan Number (both begin and end should be specified or both should be left empty)");
		}
		else if ((scanTimeB != null && scanTimeE == null) ||
				(scanTimeB == null && scanTimeE != null)) {
			setErrorMessage("Please specify the range correctly for Scan Time (both begin and end should be specified or both should be left empty)");
		} else
			super.okPressed();
	}
	
	public Integer getScanNumberBegin() {
		return scanNumberB;
	}
	
	public Integer getScanNumberEnd() {
		return scanNumberE;
	}
	
	public Double getScanTimeBegin() {
		return scanTimeB;
	}
	
	public Double getScanTimeEnd() {
		return scanTimeE;
	}
}
