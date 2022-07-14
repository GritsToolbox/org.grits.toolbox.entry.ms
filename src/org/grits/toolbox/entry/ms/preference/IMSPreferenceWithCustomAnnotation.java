package org.grits.toolbox.entry.ms.preference;

import java.util.List;

import org.grits.toolbox.entry.ms.preference.xml.MassSpecCustomAnnotation;

public interface IMSPreferenceWithCustomAnnotation {
	public List<MassSpecCustomAnnotation> getCustomAnnotations();
	public boolean saveValues();
}
