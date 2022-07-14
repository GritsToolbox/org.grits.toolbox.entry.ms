package org.grits.toolbox.entry.ms.dialog;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.grits.toolbox.tools.spectrafiltering.om.SpectraFilterSettings;

public class SpectraAverageDialog extends TitleAreaDialog {

	private Text m_txtAccuracyValue;
	private boolean m_bIsPPM;

	private SpectraFilterSettings m_filter;

	public SpectraAverageDialog(Shell parentShell, SpectraFilterSettings filter) {
		super(parentShell);
		this.m_filter = filter;
	}

	public SpectraFilterSettings getFilter() {
		return m_filter;
	}

	@Override
	public void create() {
		super.create();
		setTitle("Spectra Average Settings");
		setMessage("Please specify the accuracy for averaging the spectra with the same precursor m/z values.", IMessageProvider.INFORMATION);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(5, false);
		layout.horizontalSpacing = 10;
		layout.verticalSpacing = 10;
		area.setSize(100, 100);
		container.setLayout(layout);
		createControls(container);
		return area;
	}

	private void createControls(Composite container) {
		Label lbl;

		// Accuracy
		lbl = new Label(container, SWT.NONE);
		lbl.setText("Accuracy");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		m_txtAccuracyValue = new Text(container, SWT.BORDER);
		m_txtAccuracyValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		final Combo c = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		String items[] = { "Dalton", "PPM" };
		c.setItems(items);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		c.select(1);
		m_bIsPPM = true;
		c.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (c.getText().equals("Dalton")) {
					m_bIsPPM = false;
				} else if (c.getText().equals("PPM")) {
					m_bIsPPM = true;
				}
			}
		});

	}

	private boolean validate() {
		try {
			Double.parseDouble(m_txtAccuracyValue.getText());
		} catch (Exception e) {
			setErrorMessage("accuracyValue Value should be Double");
			return false;
		}
		setErrorMessage(null);
		return true;
	}

	@Override
	protected void okPressed() {
		if ( !this.validate() )
			return;

		m_filter.setAccuracy(Double.parseDouble(m_txtAccuracyValue.getText()));
		m_filter.setPPM(m_bIsPPM);

		super.okPressed();
	}
}
