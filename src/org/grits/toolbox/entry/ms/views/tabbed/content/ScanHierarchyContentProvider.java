package org.grits.toolbox.entry.ms.views.tabbed.content;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.grits.toolbox.ms.file.scan.data.ScanView;

public class ScanHierarchyContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement == null) 
			return null;
		return ((List<?>)inputElement).toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof List) {
			return ((List<?>) parentElement).toArray();
		} else if (parentElement instanceof ScanView) {
			if (((ScanView) parentElement).getSubScans() == null)
				return null;
			return ((ScanView) parentElement).getSubScans().toArray();
		} 
		return null;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof List) {
			return ((List<?>) element).size() > 0;
		} else if (element instanceof ScanView) {
			if (((ScanView) element).getSubScans() == null)
				return false;
			return ((ScanView) element).getSubScans().size() > 0;
		} 
		return false;
	}
	
	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub
		
	}
}
