package org.grits.toolbox.entry.ms.views.tabbed;

import org.grits.toolbox.display.control.table.process.TableDataProcessor;

public interface IMSPeaksViewer {
	public MassSpecTableBase getViewBase();
	public TableDataProcessor getTableDataProcessor();
	public void reInitializeView() throws Exception;
	public void reLoadView();
	public int getStatus();
}
