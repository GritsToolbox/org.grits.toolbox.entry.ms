package org.grits.toolbox.entry.ms.views.tabbed;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarElement;
import org.eclipse.e4.ui.model.application.ui.menu.MToolItem;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.entry.ms.command.ViewMassSpecOverviewCommandExecutor;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.entry.ms.views.tabbed.content.ScanHierarchyContentProvider;
import org.grits.toolbox.ms.file.scan.data.ScanView;

public class MassSpecScanHierarchyView {
	
	protected TreeViewer treeViewer;
	protected MassSpecEntityProperty property;
	protected MPart part;
	protected MassSpecMultiPageViewer parentView;
	@Inject EPartService partService;
	
	DecimalFormat df = new DecimalFormat("0.000");
	protected List<ScanView> scanList;

	@Inject
	public MassSpecScanHierarchyView () {
	}
	
	@PostConstruct
	public void postConstruct(Composite parent, MPart part) {
		this.part = part;	
		
		PatternFilter filter = new PatternFilter();
		FilteredTree tree = new FilteredTree(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL, filter, true);
		//tree.setInitialText("Start typing for search");
		//treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		treeViewer = tree.getViewer();
		treeViewer.setContentProvider(new ScanHierarchyContentProvider());
		treeViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ScanView) {
					ScanView view = (ScanView) element;
					if (view.getMsLevel() > 1) 
						return "Scan " + view.getScanNo() + " - " + df.format(view.getPreCursorMz()); //+ getNumberOfAnnotations((ScanView)element);
					else
						return "Scan " + view.getScanNo() + " - " + df.format(view.getRetentionTime()/60) + " min"; //+ getNumberOfAnnotations((ScanView)element);
				}
				return super.getText(element);
			}
		});
		treeViewer.addDoubleClickListener(new IDoubleClickListener()
		{
			@Override
			public void doubleClick(DoubleClickEvent event)
			{
				final IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				if (selection == null || selection.isEmpty())
					return;

				final ScanView selected= (ScanView) selection.getFirstElement();
				mouseDoubleClick(selected);
			}
		});
	}
	
	/**
	 * returns the number of annotations if applicable
	 * @param scanView the scan to show # of annotations
	 * 
	 * @return # annotations
	 */
	protected Integer getNumberOfAnnotations(ScanView scanView) {
		// not applicable for MassSpec data
		return null;
	}

	public void mouseDoubleClick(ScanView selected) {
		List<ScanView> path = findPath(selected);
		Entry newEntry = parentView.getEntry();
		int i=0;
		for (ScanView scan: path) {
			newEntry = MassSpecEntityProperty.getTableCompatibleEntry(newEntry);
			MassSpecEntityProperty msEntityProp = (MassSpecEntityProperty) newEntry.getProperty();
			Integer iMSLevel = scan.getMsLevel();
			Integer iParentScanNum = scan.getParentScan();
			Integer iScanNum = scan.getScanNo();
	
			msEntityProp.setParentScanNum(iParentScanNum);
			msEntityProp.setScanNum(iScanNum);
			msEntityProp.setMz(scan.getPreCursorMz());
			msEntityProp.setParentMz(property.getMz());
			msEntityProp.setDataFile(property.getDataFile());	
			msEntityProp.setMsLevel(Integer.valueOf(iMSLevel + 1));
			StringBuilder sb = new StringBuilder(newEntry.getDisplayName());
			if( scan.getPreCursorMz() != null && scan.getPreCursorMz() == 0.0 ) {
				sb.append(": ");
			} else { // fragmentation pathway
				sb.append("->");
			}
			sb.append("[Scan ");
			sb.append(iScanNum);
			sb.append(", MS");
			sb.append(iMSLevel);
			if(scan.getPreCursorMz() != null && scan.getPreCursorMz() > 0.0 ) {
				sb.append(", ");
				sb.append(df.format(scan.getPreCursorMz()));
			}
			sb.append("]");
		
			newEntry.setDisplayName(sb.toString());
			if (i == path.size() - 1)  // only open the last one
				showMSOverview(newEntry);  
			i++;
		}
	}

	protected void showMSOverview(Entry newEntry) {
		ViewMassSpecOverviewCommandExecutor.showMSOverview(parentView.getContext(), newEntry);		
	}
	
	public void setProperty(MassSpecEntityProperty property) {
		this.property = property;
		if (property != null)
			readData();
	}
	

	protected void readData() {
		// nothing to do at this level
	}

	public MassSpecEntityProperty getProperty() {
		return property;
	}
	
	@PreDestroy
	public void preDestroy() {
	}

	@Focus
	public void onFocus() {
		treeViewer.getControl().setFocus();
	}

	public void initializeView(List<ScanView> scans) {
		this.scanList = scans;
		treeViewer.setInput(scans);
		treeViewer.refresh();
		//treeViewer.expandAll();
	}

	public void setViewer(MassSpecMultiPageViewer massSpecMultiPageViewer) {
		this.parentView = massSpecMultiPageViewer;
	}
	
	@SuppressWarnings("unchecked")
	protected List<ScanView> findPath (ScanView scan) {
		List<ScanView> path = new ArrayList<>();
		List<ScanView> allScans = (List<ScanView>) treeViewer.getInput();
		for (ScanView s: allScans) {
			if (search(s, scan, path))
				break;
		}
	
		return path;
	}
	
	boolean search(ScanView node, ScanView selected, List<ScanView> track) {
        if (node == null) return false;
        if (node.equals(selected)) {
            track.add(node);
            return true;
        }
        for(ScanView child : node.getSubScans()) {
            if (search(child, selected, track)) {
                track.add(0, node);
                return true;
            }
        }
        return false;
    }
	
	public void expandAll () {
		if (treeViewer != null && !treeViewer.getTree().isDisposed())
			treeViewer.expandAll();
	}
	
	public void collapseAll() {
		if (treeViewer != null && !treeViewer.getTree().isDisposed())
			treeViewer.collapseAll();
	}

	public void filter(boolean filter) {
		// no filtering at this level
	}
}
