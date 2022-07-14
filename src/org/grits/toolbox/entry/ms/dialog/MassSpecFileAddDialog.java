package org.grits.toolbox.entry.ms.dialog;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.grits.toolbox.core.utilShare.FileSelectionAdapter;
import org.grits.toolbox.entry.ms.property.datamodel.MSPropertyDataFile;
import org.grits.toolbox.ms.file.FileCategory;
import org.grits.toolbox.ms.file.MSFileInfo;

public class MassSpecFileAddDialog extends TitleAreaDialog {

	private Text fileNameText;
	private Combo categoryCombo;
	//private Combo combo;
	private ComboViewer parentComboViewer;
	
	private String fileName;
	//private String fileType;
	private FileCategory fileCategory;
	//private List<String> filePurpose;
	
	MSPropertyDataFile parentFile;
	MSPropertyDataFile existingFile;
	
	boolean editingMode = false;
	private Label parentLabel;
	boolean instrumentFile = false;
	private List<MSPropertyDataFile> fileList;
	//private Table purposeTable;
	
	public MassSpecFileAddDialog(Shell shell, List<MSPropertyDataFile> fileList, MSPropertyDataFile parent) {
		super(shell);
		this.fileList = fileList;
		this.parentFile = parent;
		this.editingMode = false;
	}
	
	public MassSpecFileAddDialog(Shell shell, List<MSPropertyDataFile> fileList, MSPropertyDataFile parent, MSPropertyDataFile editing, boolean edit) {
		this(shell, fileList, parent);
		this.editingMode = edit;
		this.existingFile = editing;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Upload a new MS file");
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		//find the center of a main monitor
		Monitor primary = getShell().getDisplay().getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = getShell().getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		getShell().setLocation(x, y);
		
		GridLayout gd = new GridLayout (4, false);
		container.setLayout(gd);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Group qLabel = new Group(container, SWT.SHADOW_IN);
		qLabel.setText("Is this an instrument file?");
		qLabel.setLayout(new RowLayout(SWT.HORIZONTAL));
		qLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		Button yesOption = new Button (qLabel, SWT.RADIO);
		yesOption.setText("Yes");
		yesOption.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (yesOption.getSelection()) {
					if (parentComboViewer != null) {
						parentComboViewer.getCombo().setEnabled(false);
						parentComboViewer.getCombo().select(0);
					}
					if (parentLabel != null) parentLabel.setEnabled(false);
					instrumentFile = true;
					parentFile = null;  // no parent if it is an instrument file
					categoryCombo.setEnabled(true);
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		Button noOption = new Button (qLabel, SWT.RADIO);
		noOption.setText("No");
		noOption.setSelection(true);
		noOption.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (noOption.getSelection()) {
					instrumentFile = false;
					if (!editingMode || existingFile == null) {
						if (parentComboViewer != null) parentComboViewer.getCombo().setEnabled(true);
						if (parentLabel != null) parentLabel.setEnabled(true);
					}
					categoryCombo.setEnabled(true);
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		if (editingMode && existingFile != null) { // do not allow changing type
			yesOption.setSelection(existingFile.getIsParent());
			//noOption.setSelection(!existingFile.getIsParent());
			yesOption.setEnabled(false);
			noOption.setEnabled(false);
		}
		
		//PARENT
		if (fileList != null && !fileList.isEmpty()) {
			parentLabel = new Label(container, SWT.NONE);
			parentLabel.setText("Instrument File");
			parentComboViewer = new ComboViewer(container, SWT.NONE);
			parentComboViewer.setContentProvider(new ArrayContentProvider());
			
			parentComboViewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof MSPropertyDataFile) {
						if (((MSPropertyDataFile) element).getOriginalFileName().isEmpty()) {
							if (((MSPropertyDataFile) element).getCategory() == null) // empty selection
								return "";
							if (((MSPropertyDataFile) element).getCategory().equals(FileCategory.ANNOTATION_CATEGORY))
								return FileCategory.ANNOTATION_CATEGORY.getLabel() + ": ";
							else if (((MSPropertyDataFile) element).getCategory().equals(FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY))
								return FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY.getLabel() + ": ";
						} else
							return ((MSPropertyDataFile) element).getOriginalFileName();
					}
					return "";
				}
			});
			if (fileList != null) {
				List<MSPropertyDataFile> comboFileList = new ArrayList<>(fileList);
				comboFileList.add(0, new MSPropertyDataFile("", "0", null, null, null, "", null, true));    // to handle "no parent" option
				parentComboViewer.setInput(comboFileList);
			}
			parentComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					IStructuredSelection selection = (IStructuredSelection) parentComboViewer.getStructuredSelection();
					if (selection != null && !selection.isEmpty()) {
						MSPropertyDataFile parent = (MSPropertyDataFile) selection.getFirstElement();
						categoryCombo.select(categoryCombo.indexOf(FileCategory.lookUp(parent.getCategory().name()).getLabel()));
						categoryCombo.setEnabled(false);
					}
				}
			});
			
	 		if (editingMode && existingFile != null) {  // do not allow changing parents
				parentComboViewer.getCombo().setEnabled(false);
			}
	 		
	 		new Label(container, SWT.NONE);
			new Label(container, SWT.NONE);
		}
		
		//FILE
		Label fileNameLabel = new Label(container, SWT.NONE);
		fileNameLabel.setText("File");
		fileNameText = new Text(container, SWT.READ_ONLY | SWT.BORDER);
		fileNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		
		Button browseButton = new Button(container, SWT.PUSH);
		browseButton.setText("Browse");
		FileSelectionAdapter fileBrowserSelectionAdapter = new FileSelectionAdapter();
		fileBrowserSelectionAdapter.setShell(container.getShell());
		fileBrowserSelectionAdapter.setText(fileNameText);
		String sMzXML = "*." + MSFileInfo.MSFORMAT_MZXML_EXTENSION;
        String sMzML = "*." + MSFileInfo.MSFORMAT_MZML_EXTENSION;
		//fileBrowserSelectionAdapter.setFilterExtensions( new String[] {MSFileInfo.MSFILES_FILTER_EXTENSIONS, sMzML + ";" + sMzXML, "*.*"});
		//fileBrowserSelectionAdapter.setFilterNames( new String[] { MSFileInfo.MSFILES_FILTER_NAMES, MSFileInfo.MSFORMAT_MZML_DESC + ", " + MSFileInfo.MSFORMAT_MZXML_DESC, "All files"});
        fileBrowserSelectionAdapter.setFilterExtensions( new String[] {"*.*"});
        fileBrowserSelectionAdapter.setFilterNames( new String[] { "All files"});
      		
        browseButton.addSelectionListener(fileBrowserSelectionAdapter);
		
		if (editingMode && existingFile != null) {
			if (existingFile.getName() != null && !existingFile.getName().isEmpty()) {
				// do not allow editing
				fileNameText.setText(existingFile.getName());
				browseButton.setEnabled(false);
				fileNameText.setEnabled(false);
			}
		}
		
		fileNameText.addModifyListener( new ModifyListener() {		
			@Override
			public void modifyText(ModifyEvent e) {
				if (!fileNameText.getText().isEmpty())
					setMessage(null);
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
		});
		
		//TYPE
	/*	Label typeLabel = new Label(container, SWT.NONE);
		typeLabel.setText("Format");
		
		combo = new Combo(container, SWT.DROP_DOWN);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		combo.setItems(new String[] {MSFileInfo.getType(MSFileInfo.MS_FILE, MSFileInfo.MSFORMAT_MZXML_TYPE), 
				MSFileInfo.getType(MSFileInfo.MS_FILE, MSFileInfo.MSFORMAT_MZML_TYPE), 
				MSFileInfo.getType(MSFileInfo.MS_FILE, MSFileInfo.MSFORMAT_RAW_TYPE), 
				MSFileInfo.getType(MSFileInfo.MS_FULL_FILE, MSFileInfo.MSFORMAT_MZXML_TYPE),
				MSFileInfo.getType(MSFileInfo.MS_FULL_FILE, MSFileInfo.MSFORMAT_MZML_TYPE),
				MSFileInfo.getType(MSFileInfo.MS_FULL_FILE, MSFileInfo.MSFORMAT_RAW_TYPE),
				PeakListInfo.PEAKLISTFORMAT_EXTRACT_TYPE});
		combo.select(0);
		
		if (editingMode && existingFile != null) {
			int i=0;
			for (String item: combo.getItems()) {
				if (item.equals(existingFile.getType())) 
					combo.select(i);
				i++;
			}
		}
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);*/
		
		//CATEGORY
		Label categoryLabel = new Label(container, SWT.NONE);
		categoryLabel.setText("Category");
		
		categoryCombo = new Combo(container, SWT.READ_ONLY);
		categoryCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		categoryCombo.setItems(FileCategory.toList());
		categoryCombo.select(0);
		
		if (editingMode && existingFile != null) {
			int i=0;
			for (String item: categoryCombo.getItems()) {
				if (item.equals(existingFile.getCategory())) 
					categoryCombo.select(i);
				i++;
			}
		}
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);
		
		if (parentFile != null) {
			parentComboViewer.setSelection(new StructuredSelection(parentFile));
		}
		
		//PURPOSE
	/*	Label purposeLabel = new Label(container, SWT.NONE);
		purposeLabel.setText("Purpose");
		
		purposeTable = new Table(container, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		TableItem item = new TableItem(purposeTable, SWT.NONE);
	    item.setText(FileCategory.ANNOTATION_CATEGORY.getDescription());
	    TableItem item2 = new TableItem(purposeTable, SWT.NONE);
	    item2.setText(FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY.getDescription());
	    purposeTable.getItem(0).setChecked(true);
		
		if (editingMode && existingFile != null) {
			int i=0;
			purposeTable.deselectAll();
			for (TableItem tableItem: purposeTable.getItems()) {
				if (existingFile.getPurpose() != null && existingFile.getPurposeString().contains(tableItem.getText()))
					tableItem.setChecked(true);
				i++;
			}	
		}
		
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);*/
		
		return area;
	}

	@Override
	protected void okPressed() {
		// check whether the file is already in the list
		boolean alreadyExists = false;
		fileName = fileNameText.getText();
		if (parentComboViewer != null) {
			StructuredSelection select = (StructuredSelection) parentComboViewer.getSelection();
			if (select != null && !select.isEmpty()) {
				parentFile = (MSPropertyDataFile) select.getFirstElement();
				if (parentFile.getCategory() == null)   // empty selection
					parentFile = null;
			}
			else {
				parentFile = null;
			}
		/*	if (!instrumentFile && parentFile == null) {
				setMessage("Please select an instrument file");
				return;
			}*/
		}
		if (parentFile != null) {
			if (fileName == null || fileName.trim().isEmpty()) {
				setMessage("File name cannot be left empty");
				return;
			}
		}
		
		/*int s = combo.getSelectionIndex() == -1 ? 0 : combo.getSelectionIndex();
		fileType = combo.getItem(s);*/
		int s = categoryCombo.getSelectionIndex() == -1 ? 0 : categoryCombo.getSelectionIndex();
		fileCategory = FileCategory.findByLabel(categoryCombo.getItem(s));
		/*filePurpose = new ArrayList<>();
		for (TableItem item: purposeTable.getItems()) {
			if (item != null && item.getChecked()) {
				filePurpose.add(item.getText());
			}
		}
		if (filePurpose.isEmpty()) {
			setMessage("Select at least one purpose for the file");
			return;
		}*/
		
		// check only if the file has been changed
		if (!editingMode || (editingMode && (existingFile.getName() == null || existingFile.getName().isEmpty()) )) {
			if (fileList != null) {
				for (MSPropertyDataFile propertyDataFile : fileList) {
					if (fileName == null || fileName.isEmpty()) // skip empty files
						continue;
					if (propertyDataFile.getName().equals(fileName)) {
						alreadyExists = true;
						MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "Warning", "This file already exists for this entry");
						break;
					}
					
				}
			}
		}
		setMessage(null);
		if (!alreadyExists)
			super.okPressed();
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public FileCategory getFileCategory() {
		return fileCategory;
	}
	
	//public List<String> getFilePurpose() {
	//	return filePurpose;
	//}
	
	public MSPropertyDataFile getParentFile() {
		return parentFile;
	}
	
	public boolean isInstrumentFile() {
		return instrumentFile;
	}
}
