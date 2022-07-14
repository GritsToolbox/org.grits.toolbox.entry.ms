package org.grits.toolbox.entry.ms.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.dialog.ModalDialog;

import org.grits.toolbox.datamodel.ms.tablemodel.MassSpecTableDataObject;
import org.grits.toolbox.entry.ms.adaptor.MassSpecExportFileAdapter;

/**
 * Enable to a user to download files related to Simian Entry
 * @author dbrentw
 *
 */
public class MassSpecExportDialog extends ModalDialog {

	private String[] downloadOptions = {"Export Mass Spec CSV file","Export GRITS XML file","Export Excel file"};

	private Button OKbutton;

	private Entry msEntry;
	private List downloadlist;

	private MassSpecExportFileAdapter msExportFileAdapter;
	private MassSpecTableDataObject tableDataObject = null;
	
	public MassSpecExportDialog(Shell parentShell, MassSpecExportFileAdapter msAnnotationExportFileAdapter) {
		super(parentShell);
		this.msExportFileAdapter = msAnnotationExportFileAdapter;
	}

	@Override
	public void create()
	{
		super.create();
		setTitle("Export");
		setMessage("Export a csv, xml, or excel file");
	}

	@Override
	protected Control createDialogArea(final Composite parent) 
	{
		//has to be gridLayout, since it extends TitleAreaDialog
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		gridLayout.verticalSpacing = 10;
		parent.setLayout(gridLayout);

		/*
		 * First row starts:download list
		 */
		createList(parent);
		createButtonCancel(parent);
		createButtonOK(parent);
		
		return parent;
	}

	SelectionListener downloadlistListener = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			//enables the download button
			OKbutton.setEnabled(true);
			if(downloadlist.getSelectionIndex() == 0)
			{
				msExportFileAdapter.setFileName(".csv");
			}
			else if(downloadlist.getSelectionIndex() == 1)
			{
				msExportFileAdapter.setFileName(".xml");
			}
			else if(downloadlist.getSelectionIndex() == 2)
			{
				msExportFileAdapter.setFileName(".xls");
				msExportFileAdapter.setCopy(false);
			}
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	};

	private void createList(Composite parent2) {
		downloadlist = new List(parent2, SWT.SINGLE);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan = 4;
		gridData.verticalSpan = 1;
		downloadlist.setLayoutData(gridData);
		//add data to list
		downloadlist.add(downloadOptions[0]);
		downloadlist.add(downloadOptions[1]);
		downloadlist.add(downloadOptions[2]);
		//add listener
		downloadlist.addSelectionListener(downloadlistListener);
	}

	@Override
	protected Button createButtonOK(final Composite parent2) {
		//create a gridData for OKButton
		GridData okData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		//okData.grabExcessHorizontalSpace = true;
		okData.horizontalSpan = 2;
		okData.widthHint = 100;
		OKbutton = new Button(parent2, SWT.PUSH);
		OKbutton.setText("Export");
		//add export file adaptor
//		msAnnotationExportFileAdapter = new MSAnnotationExportFileAdapter();
		msExportFileAdapter.setShell(parent2.getShell());
		msExportFileAdapter.setMassSpecEntry(this.msEntry);
		msExportFileAdapter.setTableDataObject(this.tableDataObject);
		msExportFileAdapter.setFileName("");
		OKbutton.addSelectionListener(msExportFileAdapter);
		OKbutton.setLayoutData(okData);
		OKbutton.setEnabled(false);
		return OKbutton;
	}

	@Override
	protected boolean isValidInput() {
		return true;
	}

	@Override
	protected Entry createEntry() {
		return msEntry;
	}

	public void setMassSpecEntry(Entry msAnnotationEntry) {
		this.msEntry = msAnnotationEntry;
	}

	public void setTableDataObject( MassSpecTableDataObject tableDataObject ) {
		this.tableDataObject = tableDataObject;
	}
	
	public MassSpecTableDataObject getTableDataObject() {
		return tableDataObject;
	}
	
}
