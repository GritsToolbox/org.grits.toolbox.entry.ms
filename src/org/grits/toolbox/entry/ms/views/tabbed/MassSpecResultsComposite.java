package org.grits.toolbox.entry.ms.views.tabbed;

import java.io.File;

import org.eclipse.swt.widgets.Composite;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.editor.EntryEditorPart;
import org.grits.toolbox.datamodel.ms.tablemodel.FillTypes;
import org.grits.toolbox.display.control.table.process.TableDataProcessor;
import org.grits.toolbox.entry.ms.process.loader.MassSpecTableDataProcessor;
import org.grits.toolbox.entry.ms.tablehelpers.MassSpecTable;

public class MassSpecResultsComposite extends Composite {
	protected MassSpecTableBase baseView = null;

	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public MassSpecResultsComposite(Composite parent, int style) {
		super(parent, style);
	}

	public boolean isOpen( Property entityProperty ) {
		return getBaseView().getEntityProperty().equals(entityProperty);
	}	

	@Override
	public void layout() {
		super.layout();
		baseView.getNatTable().redraw();
	}

	public void reInit(Property entityProperty) throws Exception {
		((MassSpecTableDataProcessor) getBaseView().getTableDataProcessor()).initializeTableDataObject(entityProperty);

		MassSpecTable newTable = (MassSpecTable) getBaseView().getNewSimianTable( getBaseView(), getBaseView().getNatTable().getTableDataProcessor());
		
		//newTable.setMzXMLPathName( ( (MassSpecProperty) ( (MassSpecEntityProperty) getBaseView().getEntityProperty()).getMassSpecParentProperty() ).getFullyQualifiedMzXMLFileName(getBaseView().getParentEditor().getEntry()));
		newTable.setMzXMLPathName(((MassSpecTableDataProcessor) getBaseView().getTableDataProcessor()).getMSPath() + File.separator +
				((MassSpecTableDataProcessor) getBaseView().getTableDataProcessor()).getMSSourceFile().getName());
		newTable.loadData();

		this.baseView.natTable.dispose();
		this.baseView.natTable = newTable;
		this.baseView.natTable.createMainTable();
		
		
		this.baseView.layout();
		this.baseView.parent.layout();
	}
	
	public void createPartControl( Composite parent, EntryEditorPart parentEditor,
			Property entityProperty, TableDataProcessor dataProcessor, FillTypes fillType ) throws Exception {
		this.baseView = new MassSpecTableBase(parent, parentEditor, entityProperty, dataProcessor, fillType);	
		this.baseView.initializeTable();
		this.baseView.layout();
	}

	public MassSpecTableBase getBaseView() {
		return baseView;
	}
}
