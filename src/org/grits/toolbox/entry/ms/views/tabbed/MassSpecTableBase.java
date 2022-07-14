package org.grits.toolbox.entry.ms.views.tabbed;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.editor.EntryEditorPart;
import org.grits.toolbox.datamodel.ms.tablemodel.FillTypes;
import org.grits.toolbox.display.control.table.process.TableDataProcessor;
import org.grits.toolbox.display.control.table.tablecore.GRITSTable;
import org.grits.toolbox.entry.ms.process.loader.MassSpecTableDataProcessor;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.grits.toolbox.entry.ms.tablehelpers.MassSpecTable;


public class MassSpecTableBase {
	protected MassSpecTable natTable = null;
	protected TableDataProcessor dataProcessor = null;
	
	protected boolean bIsDirty = false;
	protected Composite parent = null;
	protected Property entityProperty = null;
	protected EntryEditorPart parentEditor = null;
	protected FillTypes fillType;
	
	public MassSpecTableBase() {
		super();
	}
	
	public MassSpecTableBase( Composite parent, EntryEditorPart parentEditor, 
			Property entityProperty, TableDataProcessor dataProcessor, FillTypes fillType ) throws Exception {
		this.parent = parent;
		this.parentEditor = parentEditor;
		this.entityProperty = entityProperty;
		this.dataProcessor = dataProcessor;
		this.fillType = fillType;
	}

	public void initializeTable() throws Exception {
		this.natTable = (MassSpecTable) getNewSimianTable(this, dataProcessor);				
		//this.natTable.setMzXMLPathName( ( (MassSpecProperty) ( (MassSpecEntityProperty) getEntityProperty()).getMassSpecParentProperty() ).getFullyQualifiedMzXMLFileName(this.parentEditor.getEntry()));
		this.natTable.setMzXMLPathName(((MassSpecTableDataProcessor) getTableDataProcessor()).getMSPath() + File.separator +
				((MassSpecTableDataProcessor) getTableDataProcessor()).getMSSourceFile().getName());
		this.natTable.loadData();
		this.natTable.createMainTable();
	}
	
	public void layout() {
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		natTable.setLayoutData(gridData);				
	}
	
	public GRITSTable getNewSimianTable( MassSpecTableBase _viewBase, TableDataProcessor _extractor ) throws Exception {
		return new MassSpecTable(_viewBase, _extractor);
	}
	
	public MassSpecTable getNatTable() {
		return this.natTable;
	}
			
	public void setDirty(boolean d) {
		this.bIsDirty = d;
		this.parentEditor.setDirty(d);
	}
			
	public boolean isDirty() {
		return this.bIsDirty;
	}

	public void doSave(IProgressMonitor monitor) {
		String sSourceFile = ((MassSpecTableDataProcessor)getTableDataProcessor()).getMSPath() + File.separator + 
				((MassSpecTableDataProcessor) getTableDataProcessor()).getMSSourceFile().getName();
		getNatTable().writeDataToXML(sSourceFile); // an in-place overwrite
		setDirty(false);
	}
	
	protected TableDataProcessor getTableDataProcessor() {
		return this.dataProcessor;
	}
	
	public Composite getParent() {
		return parent;
	}	
	
	public String getTitle() {
		return getEntry().getDisplayName();
	}
	
	public Entry getEntry() {
		return getParentEditor().getEntry();
	}
	
	public EntryEditorPart getParentEditor() {
		return parentEditor;
	}
	
	public Property getEntityProperty() {
		return this.entityProperty;
	}
}
