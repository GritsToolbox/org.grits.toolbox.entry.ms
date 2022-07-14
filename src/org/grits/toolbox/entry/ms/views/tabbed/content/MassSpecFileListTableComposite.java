package org.grits.toolbox.entry.ms.views.tabbed.content;

import java.io.File;
import java.util.List;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.grits.toolbox.entry.ms.property.FileLockManager;
import org.grits.toolbox.entry.ms.property.datamodel.MSPropertyDataFile;
import org.grits.toolbox.ms.file.FileCategory;
import org.grits.toolbox.ms.file.MSFileInfo;

/**
 * composite for displaying MassSpecProperty's files as a tree viewer
 * Usage Warning: MassSpecProperty and FileLockManager needs to be set before calling initComponents method
 * 
 * @author sena
 *
 */
public class MassSpecFileListTableComposite extends Composite {
	
	private List<MSPropertyDataFile> fileList;
	private TreeViewer fileTableViewer;
	private FileLockManager fileLockManager;
	private boolean inUsedRequired = true;

	public MassSpecFileListTableComposite(Composite parent, int style) {
		super(parent, style);
	}
	
	public MassSpecFileListTableComposite(Composite parent, int style, boolean inUseRequired) {
		this(parent, style);
		this.inUsedRequired = inUseRequired;
	}
	
	public void setFileList(List<MSPropertyDataFile> fileList) {
		this.fileList = fileList;
	}
	
	public TreeViewer getFileTableViewer() {
		return fileTableViewer;
	}
	
	public void setFileLockManager(FileLockManager fileLockManager) {
		this.fileLockManager = fileLockManager;
	}
	
	public void initComponents() {
		GridLayout layout2 = new GridLayout(1, true);
	    layout2.marginWidth = 2;
	    layout2.marginHeight = 2;
	    this.setLayout(layout2);
		fileTableViewer = new TreeViewer(this, SWT.BORDER | SWT.FULL_SELECTION);
		Tree fileTree = fileTableViewer.getTree();
		GridData gd_table_2 = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_table_2.heightHint = 300;
		fileTree.setLayoutData(gd_table_2);
		fileTree.setHeaderVisible(true);
		fileTree.setLinesVisible(true);
	
		TreeViewerColumn treeViewerColumn2 = new TreeViewerColumn(fileTableViewer, SWT.NONE);
		TreeColumn tblclmnFile = treeViewerColumn2.getColumn();
		tblclmnFile.setWidth(300);
		tblclmnFile.setText("File");
		treeViewerColumn2.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof MSPropertyDataFile) {
					String fileNameToDisplay = "";
					if (((MSPropertyDataFile) element).getOriginalFileName() != null) {
						String fileName = ((MSPropertyDataFile) element).getOriginalFileName();
						if (fileName.lastIndexOf(File.separator) != -1)
							fileNameToDisplay = fileName.substring(fileName.lastIndexOf(File.separator)+1);
						else
							fileNameToDisplay = fileName;
					} else {
						String fileName = ((MSPropertyDataFile) element).getName();
						if (fileName.lastIndexOf(File.separator) != -1)
							fileNameToDisplay = fileName.substring(fileName.lastIndexOf(File.separator)+1);
						else
							fileNameToDisplay = fileName;
					}
						
					if (((MSPropertyDataFile) element).getIsParent()) {
						// parent 
						if (fileNameToDisplay.isEmpty())
							fileNameToDisplay = "<Not specified>";
						if (((MSPropertyDataFile) element).getCategory().equals(FileCategory.ANNOTATION_CATEGORY))
							return FileCategory.ANNOTATION_CATEGORY.getLabel() + " File: " + fileNameToDisplay;
						else if (((MSPropertyDataFile) element).getCategory().equals(FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY))
							return FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY.getLabel() + " File: " + fileNameToDisplay;
					} else {
						return fileNameToDisplay;
					}
				}
				return "";
			}
		});
		
		TreeViewerColumn treeViewerColumn3 = new TreeViewerColumn(fileTableViewer, SWT.NONE);
		TreeColumn tblclmnFormat = treeViewerColumn3.getColumn();
		tblclmnFormat.setWidth(100);
		tblclmnFormat.setText("Format");
		treeViewerColumn3.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof MSPropertyDataFile)
					return MSFileInfo.getMSFormat(((MSPropertyDataFile) element).getType());
				return "";
			}
		});
		
		/*TreeViewerColumn treeViewerColumn5 = new TreeViewerColumn(fileTableViewer, SWT.NONE);
		TreeColumn tblclmnPurpose = treeViewerColumn5.getColumn();
		tblclmnPurpose.setWidth(100);
		tblclmnPurpose.setText("Purpose");	
		treeViewerColumn5.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof MSPropertyDataFile)
					return ((MSPropertyDataFile) element).getPurposeString();
				return "";
			}
		});*/
		
		if (inUsedRequired) {
			TreeViewerColumn treeViewerColumn4 = new TreeViewerColumn(fileTableViewer, SWT.NONE);
			TreeColumn tblclmnType = treeViewerColumn4.getColumn();
			tblclmnType.setWidth(100);
			tblclmnType.setText("In Use");	
			treeViewerColumn4.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof MSPropertyDataFile) {
						if (fileLockManager != null) {
							boolean locked = fileLockManager.isFileLocked(((MSPropertyDataFile) element).getName());
							if (locked)  return "Yes";
						}
					}
					return "No";
				}
			});
		}
		
		fileTableViewer.setContentProvider(new FileContentProvider());
		if (fileList != null) fileTableViewer.setInput(fileList);
		fileTableViewer.expandAll();
	}

}
