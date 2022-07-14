package org.grits.toolbox.entry.ms.views.tabbed;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.ProjectProperty;
import org.grits.toolbox.core.editor.ScrollableEntryEditorPart;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;

public class MassSpecPropertyView extends ScrollableEntryEditorPart {
	
	MPart part;
	
	@Inject
	public MassSpecPropertyView() {
	}
	
	@PostConstruct 
	public void postConstruct(MPart part, Composite parent) {
		this.part = part;
	}

	@Override
	protected void initializeComponents() {
		GridLayout layout = new GridLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 10;
		layout.verticalSpacing = 10;
		layout.numColumns = 2;

		getParent().setLayout(layout); 		

		MassSpecEntityProperty msep = (MassSpecEntityProperty) this.entry.getProperty();
		addEntries (this.entry.getParent());
		
		new Label(getParent(), SWT.NONE).setText("Data File");
		Text file = new Text(getParent(), SWT.BORDER);
		if (msep.getDataFile() != null)
			file.setText(msep.getDataFile().getName());
		file.setEditable(false);
		file.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		new Label(getParent(), SWT.NONE).setText("Scan Hierarchy (if any)");
		new Label(getParent(), SWT.NONE);
		
		addScanHierarchy();
	}

	/**
	 * adds a series of Label/Text components for each entry from the Project Entry to the displayed Entry
	 * 
	 * @param e the Entry to be displayed in the page
	 */
	private void addEntries(Entry e) {
		if (e.getParent() != null && !e.getProperty().getType().equals(ProjectProperty.TYPE))
			addEntries(e.getParent());
		if (! (e.getProperty() instanceof MassSpecEntityProperty)) {
			String type = e.getProperty().getType();
			String label = type.substring(type.lastIndexOf(".")+1);
			new Label(getParent(), SWT.NONE).setText(label.toUpperCase());
			Text text = new Text(getParent(), SWT.BORDER);
			text.setText(e.getDisplayName());
			text.setEditable(false);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		}
	}
	
	/**
	 * adds a tree control to show the scan hierarchy (while descending down through the scans)
	 */
	private void addScanHierarchy() {
		
		TreeViewer scanTree = new TreeViewer(getParent());
		scanTree.setContentProvider(new ITreeContentProvider() {
			
			@Override
			public boolean hasChildren(Object element) {
				if (element instanceof ScanEntry)
					return ((ScanEntry) element).hasChildren();
				if (element instanceof List) {
					return ((List<?>) element).size() > 0;
				}
				return false;
			}
			
			@Override
			public Object getParent(Object element) {
				return null;
			}
			
			@Override
			public Object[] getElements(Object inputElement) {
				return ((List<?>)inputElement).toArray();
			}
			
			@Override
			public Object[] getChildren(Object parentElement) {
				if (parentElement instanceof List) {
					return ((List<?>) parentElement).toArray();
				}
				if (parentElement instanceof ScanEntry) 
					return ((ScanEntry) parentElement).getChildren().toArray();
				return null;
			}

			@Override
			public void dispose() {
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		
		scanTree.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ScanEntry) {
					return ((ScanEntry) element).getDisplayName();
				}
				return super.getText(element);
			}
		});
		
		List<ScanEntry> scanEntries = new ArrayList<>();
		createScanHierarchy(this.entry, scanEntries);
		scanTree.setInput(scanEntries);
		scanTree.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 4));
		scanTree.expandAll();
	}
	
	/**
	 * finds (recursively) all the entries in the path from the given Entry (with property MassSpecEntityProperty) 
	 * to the top level MS Entry (with MassSpecEntityProperty)
	 * 
	 * @param e entry to start with
	 * @param scanEntries the list of all entries in the path (in a hierarchy - parent/child relationship)
	 */
	private void createScanHierarchy(Entry e, List<ScanEntry> scanEntries) {
		if (e.getParent() != null && e.getParent().getProperty() instanceof MassSpecEntityProperty) {
			ScanEntry parentEntry = new ScanEntry();
			parentEntry.setDisplayName(e.getParent().getDisplayName());
			scanEntries.add(parentEntry);
			ScanEntry newEntry = findEntry (scanEntries, e.getDisplayName());
			if (newEntry == null) {
				newEntry = new ScanEntry();
				newEntry.setDisplayName(e.getDisplayName());
			} else {
				scanEntries.remove(newEntry);
			}
			parentEntry.addChild(newEntry);
			createScanHierarchy(e.getParent(), scanEntries);
		}
	}
 
	/**
	 * checks if the entry with the given displayName is already in the list of ScanEntries
	 * 
	 * @param scanEntries the list of entries to search into
	 * @param displayName the display name of the entry we are looking for
	 * @return the ScanEntry found or null (if not found)
	 */
	private ScanEntry findEntry(List<ScanEntry> scanEntries, String displayName) {
		for (ScanEntry scanEntry : scanEntries) {
			if (scanEntry.getDisplayName().equals(displayName))
				return scanEntry;
		}
		return null;
	}

	@Override
	protected void updateProjectProperty() {
		// not editable, no need to update

	}

	@Override
	protected void savePreference() {
		// not editable, no need to update
	}
	
	/**
	 * inner class used to display the scan hierarchy in a tree control
	 * 
	 * @author sena
	 *
	 */
	class ScanEntry {
		String displayName;
		List<ScanEntry> children = new ArrayList<>();
		
		protected void addChild(ScanEntry e) {
			children.add(e);
		}
		
		public List<ScanEntry> getChildren() {
			return children;
		}
		
		public boolean hasChildren() {
			return children.size() > 0;
		}
		
		public String getDisplayName() {
			return displayName;
		}
		
		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}
	}
}
