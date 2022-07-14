package org.grits.toolbox.entry.ms.preference;

import java.util.List;

import org.grits.toolbox.entry.ms.preference.xml.MassSpecStandardQuant;

public interface IMSPreferenceWithStandardQuant {
	public List<MassSpecStandardQuant> getStandardQuant();
	public boolean saveValues();
}
