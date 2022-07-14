package org.grits.toolbox.entry.ms.views.tabbed.content;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.grits.toolbox.entry.ms.property.datamodel.MSPropertyDataFile;
import org.grits.toolbox.ms.file.MSFileInfo;

/**
 * Class for the MassSpec file table's content
 * Table's input is the list of "MSPropertyDataFile"s
 * 
 * @author sena
 *
 */
public class FileContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement == null) 
			return null;
		List<Object> newList = new ArrayList<>();
		for (Object o: (List<?>)inputElement) {
			if (o instanceof MSPropertyDataFile) {
				if (((MSPropertyDataFile) o).getType().equals(MSFileInfo.MSMETADATA_TYPE))  // do not display settings file in the table, it is internal
					continue;
				newList.add(o);
			}
		}
		return ((List<?>)newList).toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof List) {
			return ((List<?>) parentElement).toArray();
		} else if (parentElement instanceof MSPropertyDataFile) {
			if (((MSPropertyDataFile) parentElement).getChildren() == null)
				return null;
			return ((MSPropertyDataFile) parentElement).getChildren().toArray();
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
		} else if (element instanceof MSPropertyDataFile) {
			if (((MSPropertyDataFile) element).getChildren() == null)
				return false;
			return ((MSPropertyDataFile) element).getChildren().size() > 0;
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