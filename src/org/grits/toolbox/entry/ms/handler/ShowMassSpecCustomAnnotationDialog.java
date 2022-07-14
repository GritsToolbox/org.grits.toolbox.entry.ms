package org.grits.toolbox.entry.ms.handler;

import javax.inject.Named;

import org.apache.log4j.Logger;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.utilShare.ErrorUtils;
import org.grits.toolbox.entry.ms.dialog.MassSpecCustomAnnotationDialog;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.entry.ms.views.tabbed.MassSpecMultiPageViewer;

public class ShowMassSpecCustomAnnotationDialog {
	private static final Logger logger = Logger.getLogger(ShowMassSpecCustomAnnotationDialog.class);

	@Execute
	public Object execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
			@Named (IServiceConstants.ACTIVE_SHELL) Shell shell) {
		MassSpecMultiPageViewer curView = null;
		if (part != null && part.getObject() instanceof MassSpecMultiPageViewer ) {
			curView = (MassSpecMultiPageViewer) part.getObject();
		}
		if( curView == null ) {
			logger.error("An MS Glycan report must be open and active in order to use the Filter option");
			ErrorUtils.createWarningMessageBox(shell, "No open report", "An MS Glycan report must be open and active in order to use the Filter option");
			return null;
		}
		Entry msEntry = curView.getEntry();
		Property msProp = msEntry.getProperty();
		if( msProp instanceof MassSpecEntityProperty ) {
			if( MassSpecMultiPageViewer.massSpecCustomAnnotationDialog == null || 
					MassSpecMultiPageViewer.massSpecCustomAnnotationDialog.getShell() == null || 
					MassSpecMultiPageViewer.massSpecCustomAnnotationDialog.getShell().isDisposed() ) {
				MassSpecMultiPageViewer.massSpecCustomAnnotationDialog  = new MassSpecCustomAnnotationDialog(shell, curView);
				MassSpecMultiPageViewer.massSpecCustomAnnotationDialog.addListener(curView);
				MassSpecMultiPageViewer.massSpecCustomAnnotationDialog.open();
			} else {
				MassSpecMultiPageViewer.massSpecCustomAnnotationDialog.getShell().forceActive();
			}
			return null;
		} 
		logger.error("An MS Glycan report must be open and active in order to use the Filter option");
		ErrorUtils.createWarningMessageBox(shell, "No open report", "An MS Glycan report must be open and active in order to use the Filter option");
		return null;

	}

	@CanExecute
	public boolean isEnabled(@Named(IServiceConstants.ACTIVE_PART) MPart part, EPartService partService) {
		MassSpecMultiPageViewer curView = null;
		if (part != null && part.getObject() instanceof MassSpecMultiPageViewer ) {
			curView = (MassSpecMultiPageViewer) part.getObject();
		}
		else { // try to find an open part of the required type
			for (MPart mPart: partService.getParts()) {
				if (mPart.getObject() instanceof MassSpecMultiPageViewer) {
					if (mPart.equals(mPart.getParent().getSelectedElement())) {
						curView = (MassSpecMultiPageViewer) part.getObject();
					}
				}
			}
		}
		if (curView == null)
			return false;
		return (curView.getClass().equals(MassSpecMultiPageViewer.class));
	}
}
