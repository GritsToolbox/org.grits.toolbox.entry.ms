package org.grits.toolbox.entry.ms.tablehelpers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.CellOverrideLabelAccumulator;

import org.grits.toolbox.display.control.table.datamodel.GRITSListDataProvider;
import org.grits.toolbox.display.control.table.datamodel.GRITSListDataRow;
import org.grits.toolbox.display.control.table.process.TableDataProcessor;
import org.grits.toolbox.display.control.table.tablecore.DoubleFormat;

public class MassSpecCellOverrideLabelAccumulator<T> extends
		CellOverrideLabelAccumulator<T> {

	protected IRowDataProvider<T> dataProvider;
	protected Integer iSelectedCol = null;
	protected Integer polarityCol = null;
	protected List<Integer> intensityCols = new ArrayList<>();
	
	public MassSpecCellOverrideLabelAccumulator(IRowDataProvider<T> dataProvider ) {
		super(dataProvider);
		this.dataProvider = dataProvider;
		this.iSelectedCol = null;
	}

	public MassSpecCellOverrideLabelAccumulator(IRowDataProvider<T> dataProvider, Integer iSelectedCol, List<Integer> intensityCols ) {
		super(dataProvider);
		this.dataProvider = dataProvider;
		this.iSelectedCol = iSelectedCol;
		this.intensityCols = intensityCols;
	}
	
	@Override
	public void accumulateConfigLabels(LabelStack configLabels, int columnPosition, int rowPosition) {
		GRITSListDataRow rowObj = ((GRITSListDataProvider) dataProvider).getGRITSListDataRow(rowPosition);
		if ( rowObj == null ) 
			return;
		
		if ( iSelectedCol != null && columnPosition == iSelectedCol ) {
			configLabels.addLabel( TableDataProcessor.selColHeader.getLabel() );
		}
		
		if (intensityCols != null && intensityCols.contains(columnPosition)) {
			configLabels.addLabel(DoubleFormat.SCIENTIFIC_NOTATION.name());
		}
		
		if (polarityCol != null && polarityCol == columnPosition) 
			configLabels.addLabel(MassSpecTable.POLARITYLABEL);
	}

	public void setPolarityColumn(int polarityColumn) {
		this.polarityCol = polarityColumn;
	}
}
