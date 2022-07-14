package org.grits.toolbox.entry.ms.views.tabbed;

import javax.inject.Inject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.grits.toolbox.display.control.spectrum.chart.GRITSSpectralViewerData;

/**
 * Object that contains a "Composite" to contain the checkbox options for an MS Spectra viewer.
 * 
 * @author D Brent Weatherly (dbrentw@uga.edu)
 *
 */
public class MassSpecSpectraControlPanelView {

	protected MassSpecSpectraView parentView = null;
	protected Button cbShowSpectra = null;
	protected Composite parent;
	protected Button cbAnnotatedPeaks = null;
	protected Button cbAnnotatedPeakLabels = null;
	protected Button cbPickedPeaks = null;
	protected Button cbPickedPeakLabels = null;

	@Inject
	public MassSpecSpectraControlPanelView(MassSpecSpectraView parentView) {
		this.parentView = parentView;
	}
	
	public boolean showRaw() {
		return cbShowSpectra.getSelection();
	}
	public Button getShowSpectra() {
		return cbShowSpectra;
	}
	
	protected void updateChart() {
		parentView.updateChartPlot();
	}
	
	public MassSpecSpectraView getParentView() {
		return parentView;
	}
	
	/**
	 * Sets the MS Spectra viewer options based on the settings in the specified GRITSSpectralViewerData object.
	 * 
	 * @param svd, the GRITSSpectralViewerData object
	 */
	public void enableComponents( GRITSSpectralViewerData svd ) {
		cbShowSpectra.setEnabled(svd.getRawData() != null && ! svd.getRawData().isEmpty() );
		boolean bHasPicked = svd.getPickedPeaks() != null && ! svd.getPickedPeaks().isEmpty();
		cbPickedPeaks.setEnabled(bHasPicked);
		boolean bHasAnnotated = svd.getAnnotatedPeaks() != null && ! svd.getAnnotatedPeaks().isEmpty();
		cbAnnotatedPeaks.setEnabled(bHasAnnotated);
	}
	
	public boolean showPickedPeaks() {
		return cbPickedPeaks.getSelection();
	}
	public Button getPickedPeaks() {
		return cbPickedPeaks;
	}
	
	public boolean showPickedPeakLabels() {
		return cbPickedPeakLabels.getSelection();
	}
	public Button getPickedPeakLabels() {
		return cbPickedPeakLabels;
	}
	
	public boolean showAnnotatedPeakLabels() {
		return cbAnnotatedPeakLabels.getSelection();
	}
	public Button getAnnotatedPeakLabels() {
		return cbAnnotatedPeakLabels;
	}
	public boolean showAnnotatedPeaks() {
		return cbAnnotatedPeaks.getSelection();
	}
	public Button getAnnotatedPeaks() {
		return cbAnnotatedPeaks;
	}

	protected GridLayout getNewGridLayout() {
		return new GridLayout(3, false);
	}
			
	/**
	 * Adds the raw MS components
	 */
	protected void setMSElements() {
		cbShowSpectra = new Button(parent, SWT.CHECK);
		cbShowSpectra.setText("Raw Spectra");

		GridData gdRawSpectra = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 3, 1);	
		cbShowSpectra.setLayoutData(gdRawSpectra);
		cbShowSpectra.setSelection(true);
		cbShowSpectra.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateChart();	
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});			
	}
	
	/**
	 * Adds the Picked Peak components
	 */
	protected void setPickedPeaksElements() {
		cbPickedPeaks = new Button(parent, SWT.CHECK);
		cbPickedPeaks.setText("Picked Peaks");
		GridData gdPickedPeaks = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 1);
		cbPickedPeaks.setLayoutData(gdPickedPeaks);
		cbPickedPeaks.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if( ! cbPickedPeaks.getSelection() ) {
					cbPickedPeakLabels.setSelection(false);
				} 
				cbPickedPeakLabels.setEnabled(cbPickedPeaks.getSelection());
				updateChart();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});
		
		cbPickedPeakLabels = new Button(parent, SWT.CHECK);
		cbPickedPeakLabels.setText("Show labels");
		GridData gdPickedPeakLabels = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1);
		cbPickedPeakLabels.setLayoutData(gdPickedPeakLabels);
		cbPickedPeakLabels.setEnabled(false);
		cbPickedPeakLabels.addSelectionListener(new SelectionListener() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateChart();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});

	}
	
	/**
	 * Adds the annotated peak components
	 */
	protected void setAnnotatedElements() {
		cbAnnotatedPeaks = new Button(parent, SWT.CHECK);
		cbAnnotatedPeaks.setText("Annotated Peaks");
		GridData gdAnnotatedPeaks = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 1);
		cbAnnotatedPeaks.setLayoutData(gdAnnotatedPeaks);
		cbAnnotatedPeaks.addSelectionListener(new SelectionListener() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if( ! cbAnnotatedPeaks.getSelection() ) {
					cbAnnotatedPeakLabels.setSelection(false);
				} 
				cbAnnotatedPeakLabels.setEnabled(cbAnnotatedPeaks.getSelection());
				updateChart();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});

		cbAnnotatedPeakLabels = new Button(parent, SWT.CHECK);
		cbAnnotatedPeakLabels.setText("Show labels");
		GridData gdAnnotatedPeakLabels = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1);
		cbAnnotatedPeakLabels.setLayoutData(gdAnnotatedPeakLabels);
		cbAnnotatedPeakLabels.setEnabled(false);
		cbAnnotatedPeakLabels.addSelectionListener(new SelectionListener() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateChart();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});				
	}
	
	/**
	 * Adds all of the MS Spectra option components.
	 */
	protected void addElements() {
		setMSElements();
		setPickedPeaksElements();
		setAnnotatedElements();
	}
	
	/**
	 * Initializes the parent composite and adds all of the MS Spectra option components.
	 * 
	 * @param parent, the container for the MS Spectra options
	 */
	public void createPartControl(Composite parent) {
		this.parent = parent;
		GridLayout layout = getNewGridLayout();
		layout.horizontalSpacing = 25;
		layout.marginLeft = 10;
		layout.marginTop = 10;
		parent.setLayout( layout );
		addElements();
	}

}
