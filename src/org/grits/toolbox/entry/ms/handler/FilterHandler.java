package org.grits.toolbox.entry.ms.handler;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MToolItem;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.grits.toolbox.entry.ms.views.tabbed.MassSpecScanHierarchyView;

public class FilterHandler {
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part, final MToolItem item) {
		MassSpecScanHierarchyView curView = null;
		if (part != null && part.getObject() instanceof MassSpecScanHierarchyView ) {
			curView = (MassSpecScanHierarchyView) part.getObject();
			curView.filter(item.isSelected());
		}
		
	}
}
