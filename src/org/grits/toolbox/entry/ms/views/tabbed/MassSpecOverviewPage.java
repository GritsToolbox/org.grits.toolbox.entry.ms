package org.grits.toolbox.entry.ms.views.tabbed;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.grits.toolbox.core.dataShare.PropertyHandler;
import org.grits.toolbox.core.datamodel.DataModelHandler;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.ProjectProperty;
import org.grits.toolbox.core.datamodel.property.PropertyDataFile;
import org.grits.toolbox.core.datamodel.util.DataModelSearch;
import org.grits.toolbox.core.editor.ScrollableEntryEditorPart;
import org.grits.toolbox.core.img.ImageShare;
import org.grits.toolbox.core.service.IGritsUIService;
import org.grits.toolbox.core.utilShare.ComboPopupSelectionListener;
import org.grits.toolbox.core.utilShare.ErrorUtils;
import org.grits.toolbox.core.utilShare.ListenerFactory;
import org.grits.toolbox.entry.ms.Activator;
import org.grits.toolbox.entry.ms.ImageRegistry;
import org.grits.toolbox.entry.ms.command.ViewMassSpecOverviewCommandExecutor;
import org.grits.toolbox.entry.ms.dialog.MassSpecFileAddDialog;
import org.grits.toolbox.entry.ms.handler.GlycresoftHandler;
import org.grits.toolbox.entry.ms.handler.MSConvertHandler;
import org.grits.toolbox.entry.ms.handler.SpectraAverageHandler;
import org.grits.toolbox.entry.ms.preference.MassSpecPreference;
import org.grits.toolbox.entry.ms.preference.MassSpecPreferenceLoader;
import org.grits.toolbox.entry.ms.property.CopyFilesRunnableWithProgress;
import org.grits.toolbox.entry.ms.property.FileLock;
import org.grits.toolbox.entry.ms.property.FileLockManager;
import org.grits.toolbox.entry.ms.property.FileLockingUtils;
import org.grits.toolbox.entry.ms.property.LockEntry;
import org.grits.toolbox.entry.ms.property.MassSpecEntityProperty;
import org.grits.toolbox.entry.ms.property.MassSpecProperty;
import org.grits.toolbox.entry.ms.property.datamodel.MSPropertyDataFile;
import org.grits.toolbox.entry.ms.property.datamodel.MassSpecMetaData;
import org.grits.toolbox.entry.ms.views.tabbed.content.MassSpecFileListTableComposite;
import org.grits.toolbox.ms.file.FileCategory;
import org.grits.toolbox.ms.file.MSFile;
import org.grits.toolbox.ms.file.MSFileInfo;
import org.grits.toolbox.ms.file.PeakListInfo;
import org.grits.toolbox.ms.om.data.Method;

/**
 * MassSpec overview page that lists all the files uploaded for the experiment
 * 
 * @author sena
 *
 */
@SuppressWarnings("restriction")
public class MassSpecOverviewPage extends ScrollableEntryEditorPart {

	//log4J Logger
	private static final Logger logger = Logger.getLogger(MassSpecOverviewPage.class);
	private static final ImageDescriptor MSCONVERT_ICON = ImageRegistry.getImageDescriptor(Activator.PLUGIN_ID, ImageRegistry.MSImage.MSCONVERT_ICON);
	private static final ImageDescriptor GLYCRESOFT_ICON = ImageRegistry.getImageDescriptor(Activator.PLUGIN_ID, ImageRegistry.MSImage.GLYCRESOFT_ICON);
	// Masaaki added (7/26/2019)
	private static final ImageDescriptor PICKER_ICON = ImageRegistry.getImageDescriptor(Activator.PLUGIN_ID, ImageRegistry.MSImage.PICKER_ICON);
	private static final ImageDescriptor AVERAGE_ICON = ImageRegistry.getImageDescriptor(Activator.PLUGIN_ID, ImageRegistry.MSImage.AVERAGE_ICON);

	private Label msTypeLabel;
	private Label descriptionLabel;
	private Text descriptionText;
	private Combo experimentTypeCombo;
	private Label instrumentLabel;
	private Text instrumentText;

	private MassSpecPreference preferences = null;
	MPart part;
	private Tree fileTree;
	private TreeViewer fileTableViewer;
	FileLockManager fileLockManager;

	@Inject
	private MDirtyable dirtyable;

	@Inject
	public MassSpecOverviewPage(MPart part) {
		loadWorkspacePreferences();
		this.entry = (Entry) part.getTransientData().get(IGritsUIService.TRANSIENT_DATA_KEY_PART_ENTRY);
	}

	/**
	 * Load the lock file for the first time
	 * @return 
	 */
	private FileLockManager loadLockFile() {
		MassSpecProperty pp = (MassSpecProperty) this.entry.getProperty();
		try {
			return FileLockingUtils.readLockFile(pp.getLockFilePath(this.entry));
		} catch (IOException | JAXBException e) {
			logger.error("Cannot load the lock file", e);
		}
		return null;
	}

	@PostConstruct 
	public void postConstruct(MPart part, Composite parent) {
		this.part = part;
		this.createPartControl(parent);
	}

	protected void initializeComponents() {
		part.setLabel(this.entry.getDisplayName());

		GridLayout layout = new GridLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 10;
		layout.verticalSpacing = 10;
		layout.numColumns = 3;

		getParent().setLayout(layout); 		

		MassSpecProperty pp = (MassSpecProperty) this.entry.getProperty(); 

		//for descriptionText
		createDescription(pp);

		//for experiment type
		String sMSType = pp.getMassSpecMetaData() != null && pp.getMassSpecMetaData().getMsExperimentType() != null ? pp.getMassSpecMetaData().getMsExperimentType() : "";
		experimentTypeCombo = createCombo (msTypeLabel, experimentTypeCombo, "MS Experiment Type", preferences.getAllExperimentTypes(), sMSType);

		createInstrument(pp);
		
		// file list
		createFileTable(pp);  
	}
	
	private void createInstrument(MassSpecProperty pp) {
		instrumentLabel = new Label(getParent(), SWT.LEFT);
		instrumentLabel.setText("Instrument");
		instrumentLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 6));
		
		GridData gridData = new GridData();
		gridData.horizontalSpan = 2;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;
		instrumentText = new Text(getParent(), SWT.BORDER);
		String sInstr = pp.getMassSpecMetaData() != null && pp.getMassSpecMetaData().getInstrument() != null ? pp.getMassSpecMetaData().getInstrument() : "";
		instrumentText.setText(sInstr);
		instrumentText.setLayoutData(gridData);
		instrumentText.addModifyListener(getModListener());
		instrumentText.addTraverseListener(ListenerFactory.getTabTraverseListener());
		instrumentText.addKeyListener(ListenerFactory.getCTRLAListener());
		
	}

	private void createFileTable(MassSpecProperty pp) {
		Composite sectionParentComposite = new Composite(getParent(), SWT.FILL);
		sectionParentComposite.setLayout(new TableWrapLayout());

		GridData compositeLayoutData = new GridData(GridData.FILL_BOTH);
		compositeLayoutData.verticalSpan = 10;
		compositeLayoutData.horizontalSpan = 3;
		sectionParentComposite.setLayoutData(compositeLayoutData);

		Color sectionColor = new Color(Display.getCurrent(), 20, 199, 255);
		Color backgroundColor = Display.getCurrent().getSystemColor(
				SWT.COLOR_LIST_BACKGROUND);
		
		Section section2 = new Section(sectionParentComposite, Section.TITLE_BAR | Section.EXPANDED);
		section2.setText("Files"); 
		section2.setTitleBarBackground(sectionColor);
		section2.setBackground(backgroundColor);
		section2.setTitleBarForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));

		ToolBarManager toolBarManager2 = new ToolBarManager();
		toolBarManager2.add(new Action("Show Selected Data") {

			@Override
			public String getToolTipText() {
				return getText();
			}

			@Override
			public ImageDescriptor getImageDescriptor() {
				return ImageShare.SHOW_SELECTED_ICON;
			}

			@Override
			public void run() {
				// find selected file
				TreeItem[] items = fileTree.getSelection();
				// since we allow single selection only, get the first one
				if (items.length > 0) {
					TreeItem selected = items[0];
					MSPropertyDataFile selectedFile = (MSPropertyDataFile) selected.getData();
					openMassSpecViewer (selectedFile);
				}
			}
		});
		
		toolBarManager2.add(new Action ("Upload file") {
			
			@Override
			public ImageDescriptor getImageDescriptor() {
				return ImageShare.ADD_ICON;
			}
			
			@Override
			public String getToolTipText() {
				return "Upload a new file";
			}
			
			@Override
			public void run() {
				// find selected file
				TreeItem[] items = fileTree.getSelection();
				// since we allow single selection only, get the first one
				if (items.length > 0) {
					TreeItem selected = items[0];
					if (selected.getParentItem() == null) { // parent node
						MSPropertyDataFile parentFile = (MSPropertyDataFile) selected.getData();
						addFileToEntry (Display.getCurrent().getActiveShell(), pp, parentFile);
					} else {
						addFileToEntry (Display.getCurrent().getActiveShell(), pp, null);
					}
				}
				else {
					addFileToEntry (Display.getCurrent().getActiveShell(), pp, null);
				}
				
			}
		});
		
		toolBarManager2.add(new Action ("Edit") {
			
			@Override
			public ImageDescriptor getImageDescriptor() {
				return ImageShare.EDIT_ICON;
			}
			
			@Override
			public String getToolTipText() {
				return "Edit selected file";
			}
			
			@Override
			public void run() {
				// find selected file
				TreeItem[] items = fileTree.getSelection();
				// since we allow single selection only, get the first one
				if (items.length > 0) {
					TreeItem selected = items[0];
					MSPropertyDataFile selectedFile = (MSPropertyDataFile) selected.getData();
					editAction (Display.getCurrent().getActiveShell(), pp, selectedFile);
				}
				else {
					MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "No selection", "Please select an entry to edit");
				}
				
			}
		});
		
		toolBarManager2.add(new Action ("Delete") {
			
			@Override
			public ImageDescriptor getImageDescriptor() {
				return ImageShare.DELETE_ICON;
			}
			
			@Override
			public String getToolTipText() {
				return "Delete selected file";
			}
			
			@Override
			public void run() {
				TreeItem[] items = fileTree.getSelection();
				// since we allow single selection only, get the first one
				if (items.length > 0) {
					TreeItem selected = items[0];
					MSPropertyDataFile dataFile = (MSPropertyDataFile) selected.getData();
					boolean cont = MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Confirm", 
							"Are you sure to delete this file? Your action cannot be reverted");
					if (cont) {
						deleteAction(Display.getCurrent().getActiveShell(), pp, dataFile);
						if (isDirty()) {
							saveFileChanges();
							//setDirty(false);
						}
					}
				} else {
					MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "No selection", "Please select a file entry below to delete");
				}
			}

		});
		
		toolBarManager2.add(new Action("Download file") {
			@Override
			public ImageDescriptor getImageDescriptor() {
				return ImageShare.DOWNLOAD_ICON;
			}
			
			@Override
			public String getToolTipText() {
				return "Download selected file";
			}
			
			@Override
			public void run() {
				// find selected file
				TreeItem[] items = fileTree.getSelection();
				// since we allow single selection only, get the first one
				if (items.length > 0) {
					TreeItem selected = items[0];
					MSPropertyDataFile file = (MSPropertyDataFile) selected.getData();
					String filename = file.getName();
					if (filename == null || filename.isEmpty()) {
						MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "No File", "No file to download for this selection!");
						return;
					}
					downloadAction(Display.getCurrent().getActiveShell(), filename);
				} else {
					MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "No selection", "Please select a file entry below to download");
				}
			}
		});


		toolBarManager2.add(new Action("Convert") {
			@Override
			public ImageDescriptor getImageDescriptor() {
				return MSCONVERT_ICON;
			}
			@Override
			public String getToolTipText() {
				return "Convert selected instrument file using MSConvert";
			}

			@Override
			public boolean isEnabled() {
				return System.getProperty("os.name").startsWith("Windows");	
			}

			@Override
			public void run() {
				// find selected file
				TreeItem[] items = fileTree.getSelection();
				// since we allow single selection only, get the first one
				if (items.length > 0) {
					TreeItem selected = items[0];
					MSPropertyDataFile file = (MSPropertyDataFile) selected.getData();
					String filename = file.getName();
					if (filename == null || filename.isEmpty() || !file.getMSFileType().equals(MSFileInfo.MS_FILE_TYPE_INSTRUMENT)) {
						MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "No File", "No valid file selected for conversion!");
						return;
					} else {
						String msPath = getMSPath(entry);
						String convertedFile = convertFile (msPath, filename);
						if (convertedFile != null) {
							File cFile = new File(msPath + File.separator + convertedFile);
							if (!cFile.exists()) {
								MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", "Converted file cannot be found. Something went wrong with MSConvert!");
								return;
							}
							String fileFormat = findFileFormatType(file.getCategory(), convertedFile);
							MSPropertyDataFile newDataFile = new MSPropertyDataFile(convertedFile, MSFileInfo.MSFORMAT_MZXML_CURRENT_VERSION, 
									fileFormat, file.getCategory(), findMSFileType(fileFormat), convertedFile, file.getPurpose(), false);
							if (!file.containsChild(newDataFile)) {
								file.addChild(newDataFile);
								//setDirty(true);
								saveFileChanges();
								fileTableViewer.refresh();
								fileTableViewer.expandAll();
							}
						}
					}
				}
				else {
					MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "No File", "No valid file selected for conversion! Please select an instrument file");
					return;
				}
			}
		});

		toolBarManager2.add(new Action("Glycresoft") {
			@Override
			public ImageDescriptor getImageDescriptor() {
				return GLYCRESOFT_ICON;
			}
			@Override
			public String getToolTipText() {
				return "Convert mass spectral data files into deisotoped neutral mass peak lists using Glycresoft";
			}
			
			@Override
			public void run() {
				// find selected file
				TreeItem[] items = fileTree.getSelection();
				// since we allow single selection only, get the first one
				if (items.length > 0) {
					TreeItem selected = items[0];
					MSPropertyDataFile file = (MSPropertyDataFile) selected.getData();
					String filename = file.getName();
					if (filename == null || filename.isEmpty() || !(file.getType().equals(MSFileInfo.MSFORMAT_MZML_TYPE) || file.getType().equals(MSFileInfo.MSFORMAT_MZXML_TYPE))) {
						MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "No File", "No valid file selected for processing! Please select an annotation (mzxml/mzml) file");
						return;
					} else {
						String msPath = getMSPath(entry);
						String convertedFile = convertWithGlycresoft (msPath, filename);
						if (convertedFile != null) {
							File cFile = new File(convertedFile);
							if (!cFile.exists()) {
								MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", "Processed file cannot be found. Something went wrong with Glycresoft!");
								return;
							}
							// remove msPath from the file name
							convertedFile = convertedFile.replace(msPath, "");
							MSPropertyDataFile newDataFile = new MSPropertyDataFile(convertedFile, MSFileInfo.MSFORMAT_MZML_CURRENT_VERSION, 
									MSFileInfo.MSFORMAT_MZML_TYPE, FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY, MSFileInfo.MS_FILE_TYPE_PROCESSED, convertedFile, file.getPurpose(), false);
							// find parent of the selected file and add it to the same parent
							MSPropertyDataFile parent = findParentFile(pp, file);
							if (parent != null && !parent.containsChild(newDataFile)) {
								parent.addChild(newDataFile);
								//setDirty(true);
								saveFileChanges();
								fileTableViewer.refresh();
								fileTableViewer.expandAll();
							} // parent cannot be null
						}
					}
				}
				else {
					MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "No File", "No valid file selected for processing! Please select an annotation (mzxml/mzml) file");
					return;
				}
			}
		});

		toolBarManager2.add(new Action("SpectraAverage") {
			@Override
			public ImageDescriptor getImageDescriptor() {
				return AVERAGE_ICON;
			}

			@Override
			public String getToolTipText() {
				return "Average mass spectra which has the same precursor m/z value";
			}

			@Override
			public void run() {
				// find selected file
				TreeItem[] items = fileTree.getSelection();
				// since we allow single selection only, get the first one
				if (items.length == 0) {
					MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "No File", "No valid file selected for processing! Please select an annotation (mzxml/mzml) file");
					return;
				}
				TreeItem selected = items[0];
				MSPropertyDataFile file = (MSPropertyDataFile) selected.getData();
				String filename = file.getName();
				if (filename == null || filename.isEmpty() || !(/*file.getType().equals(MSFileInfo.MSFORMAT_MZML_TYPE) || */file.getType().equals(MSFileInfo.MSFORMAT_MZXML_TYPE))) {
					MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "No File", "No valid file selected for processing! Please select an annotation (mzxml) file");
					return;
				}

				String msPath = getMSPath(entry);
				String convertedFile = averageSpectra(msPath, filename);
				if (convertedFile == null)
					return;

				File cFile = new File(convertedFile);
				if (!cFile.exists()) {
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", "Processed file cannot be found. Something went wrong in averaging spectra!");
					return;
				}

				// remove msPath from the file name
				convertedFile = convertedFile.replace(msPath, "");
				MSPropertyDataFile newDataFile = new MSPropertyDataFile(convertedFile, MSFileInfo.MSFORMAT_MZXML_CURRENT_VERSION, 
						MSFileInfo.MSFORMAT_MZXML_TYPE, file.getCategory(), MSFileInfo.MS_FILE_TYPE_PROCESSED, convertedFile, file.getPurpose(), false);
				// find parent of the selected file and add it to the same parent
				MSPropertyDataFile parent = findParentFile(pp, file);
				if (parent != null && !parent.containsChild(newDataFile)) {
					parent.addChild(newDataFile);
					//setDirty(true);
					saveFileChanges();
					fileTableViewer.refresh();
					fileTableViewer.expandAll();
				} // parent cannot be null

			}
		});

		ToolBar toolbar2 = toolBarManager2.createControl(section2);

		TableWrapData sectionLayoutData = new TableWrapData(TableWrapData.FILL, TableWrapData.BOTTOM);
		sectionLayoutData.grabHorizontal = true;
		sectionLayoutData.grabVertical = true;
		section2.setLayoutData(sectionLayoutData);
		section2.setTextClient(toolbar2);
		
		this.fileLockManager = loadLockFile();
		
		// the following will put the table (tree) composite listing all the files
		MassSpecFileListTableComposite comp = new MassSpecFileListTableComposite(section2, SWT.WRAP);
		comp.setFileList(pp.getMassSpecMetaData().getFileList());
		comp.setFileLockManager(this.fileLockManager);
		comp.setBackground(backgroundColor);
		comp.setBackgroundMode(SWT.INHERIT_FORCE);
		comp.initComponents();
		fileTableViewer = comp.getFileTableViewer();
		fileTree = fileTableViewer.getTree();
		
		fileTableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				mouseDoubleClick(event);
			}
		});
		
		section2.setClient(comp);
	}
	
	protected MSPropertyDataFile findParentFile(MassSpecProperty pp, MSPropertyDataFile file) {
		if (pp.getMassSpecMetaData().getFileList() != null) {
			for (MSPropertyDataFile p: pp.getMassSpecMetaData().getFileList()) {
				if (p.containsChild(file))
					return p;
			}
		}
		return null;
	}

	protected String averageSpectra(String msPath, String filename) {
		ECommandService commandService = context.get(ECommandService.class);
		EHandlerService handlerService = context.get(EHandlerService.class);
		
		context.set(SpectraAverageHandler.PARAMETER2, msPath);
		context.set(SpectraAverageHandler.PARAMETER, filename);
		Object result = handlerService.executeHandler(
			commandService.createCommand(SpectraAverageHandler.COMMAND_ID, null));
		if (result != null) 
			return (String) result;
		return null;
	}

	/**
	 * execute Glycresoft (http://www.bumc.bu.edu/msr/glycresoft/) handler to process the file
	 * 
	 * @param msPath path to the MS folder
	 * @param filename name of the mzxml/mzml file to be processed
	 * @return the name of the generated file
	 */
	protected String convertWithGlycresoft(String msPath, String filename) {
		ECommandService commandService = context.get(ECommandService.class);
		EHandlerService handlerService = context.get(EHandlerService.class);
		
		context.set(GlycresoftHandler.PARAMETER2, msPath);
		context.set(GlycresoftHandler.PARAMETER, filename);
		Object result = handlerService.executeHandler(
			commandService.createCommand(GlycresoftHandler.COMMAND_ID, null));
		if (result != null) 
			return (String) result;
		return null;
	}

	/**
	 * execute MSConvertHandler command to convert the file 
	 * 
	 * @param msPath path to the MS folder
	 * @param filename name of the "instrument" file to be converted
	 * @return the name of the converted file
	 */
	protected String convertFile(String msPath, String filename) {
		ECommandService commandService = context.get(ECommandService.class);
		EHandlerService handlerService = context.get(EHandlerService.class);
		
		context.set(MSConvertHandler.PARAMETER2, msPath);
		context.set(MSConvertHandler.PARAMETER, filename);
		Object result = handlerService.executeHandler(
			commandService.createCommand(MSConvertHandler.COMMAND_ID, null));
		if (result != null) 
			return (String) result;
		return null;
	}

	/**
	 * open the MassSpec (multi-page) editor for the selected file if it's type is one we can display
	 * 
	 * @param selectedFile file to be displayed
	 */
	protected void openMassSpecViewer(MSPropertyDataFile selectedFile) {
		// check to make sure we can display the selected file: we can display any mzxml/mzml file
		if( selectedFile != null && selectedFile.isValidMSFile() ) {
			// open MassSpecMultiPageViewer with this file
			MassSpecEntityProperty msEntityProp = new MassSpecEntityProperty((MassSpecProperty) entry.getProperty());	
			msEntityProp.setDataFile(selectedFile);
			msEntityProp.setScanNum(-1);
			msEntityProp.setParentScanNum(-1);
			msEntityProp.setMz(0.0);
			msEntityProp.setParentMz(0.0);
			msEntityProp.setMsLevel(1);
			Entry newEntry = new Entry();
			newEntry.setProperty(msEntityProp);	
			newEntry.setDisplayName(entry.getDisplayName());
			newEntry.setParent(entry);
			
			ViewMassSpecOverviewCommandExecutor.showMSOverview(part.getContext(), newEntry);
		}
	}

	/**
	 * Double click to open mass spec viewer for the selected file
	 * 
	 * @param event double-click event
	 */
	protected void mouseDoubleClick(DoubleClickEvent event) {
		final IStructuredSelection selection = (IStructuredSelection)event.getSelection();
		if (selection == null || selection.isEmpty())
			return;

		final MSPropertyDataFile selectedFile = (MSPropertyDataFile) selection.getFirstElement();
		openMassSpecViewer (selectedFile);
	}

	/**
	 * Edit an existing file entry to make changes.
	 * Changing the file name or the parent is not allowed. If there was no file, the user can add a new one
	 * 
	 * @param shell active shell to display messages
	 * @param pp MassSpecProperty the file belongs to
	 * @param selectedFile the file entry to be modified
	 */
	protected void editAction(Shell shell, MassSpecProperty pp, MSPropertyDataFile selectedFile) {
		if (selectedFile == null)
			return;
		MassSpecFileAddDialog dialog = new MassSpecFileAddDialog(shell, pp.getMassSpecMetaData().getFileList(), null, selectedFile, true);
		if (dialog.open() == Window.OK) {
			String file = dialog.getFileName();
			boolean modified = false;
			if (file != null && !file.isEmpty()) {
				// check if it is different
				if (! (file.equals(selectedFile.getName()) || file.equals(selectedFile.getOriginalFileName()) )) {
					try {
						String format = findFileFormatType(dialog.getFileCategory(), file);
						String originalFileName = file == null || file.isEmpty() ? "" : file.substring(file.lastIndexOf(File.separator)+1);
						selectedFile.setType(format);
						String msFiletype = findMSFileType(selectedFile.getType());
						selectedFile.setMSFileType(msFiletype);
						selectedFile.setName(file);
						selectedFile.setOriginalFileName(originalFileName);
						processFile (shell, pp, selectedFile, msFiletype);
						modified = true;
					} catch (IOException | JAXBException e) {
						logger.error("Could not update the file", e);
						MessageDialog.openError(shell, "Edit Error", "Could not update the file info. Reason: " + e.getMessage());
						return;
					}
				}
			}

			if (! dialog.getFileCategory().equals(selectedFile.getCategory())) {
				// category change should be propagated to children if any
				selectedFile.setCategory(dialog.getFileCategory());
				if (selectedFile.getChildren() != null) {
					for (MSPropertyDataFile f : selectedFile.getChildren()) {
						f.setCategory(dialog.getFileCategory());
					}
				}
				modified = true;
			}
			if (modified) {
				saveFileChanges();
				fileTableViewer.refresh();
				fileTableViewer.expandAll();
			}
		}
	}

	/** 
	 * delete the selected file, checks if the file is locked before deletion
	 * 
	 * @param activeShell shell to display messages in case of a problem/issue
	 * @param pp MassSpecProperty to remove the file from
	 * @param dataFile file to be removed
	 */
	protected void deleteAction(Shell activeShell, MassSpecProperty pp, MSPropertyDataFile dataFile) {
		if (dataFile == null)
			return;
		// check if the selection is a parent selection with no file
		if (dataFile.getIsParent()) {
			// check if it has children
			if (dataFile.getChildren() != null && !dataFile.getChildren().isEmpty()) {
				logger.debug(dataFile + " cannot be deleted since it has children. Delete them first then try to delete this entry again!");
				MessageDialog.openInformation(activeShell, "Parent", dataFile.getOriginalFileName() + " cannot be deleted since it has children. Delete them first then try to delete this entry again!");
				return;
			} 
		}
		if (dataFile.getIsParent() && (dataFile.getName() == null || dataFile.getName().isEmpty())) {
			// no file to delete
			// remove the dataFile from the property
			pp.getMassSpecMetaData().getFileList().remove(dataFile);
			//setDirty(true);
			saveFileChanges();
			fileTableViewer.refresh();
			fileTableViewer.expandAll();
			return;
		}
	
		// if no children (or is not a parent node), delete the file then the table entry
		String fileName = dataFile.getName();
		if (fileName == null || fileName.isEmpty())
			return;
		// check if the file is currently locked
		fileLockManager = loadLockFile();
		DataModelHandler dm = DataModelHandler.instance();
		if (fileLockManager != null) {
			boolean locked = fileLockManager.isFileLocked(fileName);
			if (locked) {
				FileLock fileLock = fileLockManager.findFile(fileName);
				List<String> entryNames = new ArrayList<>();
				if (fileLock != null && fileLock.getUsedBy() != null) {
					for (LockEntry e: fileLock.getUsedBy()) {
						Entry entry1 = dm.findEntryById(e.getEntryId());
						if (entry1 != null)
							entryNames.add(entry1.getDisplayName());
					}
				}
				MessageDialog.openInformation(activeShell, "File in Use", dataFile.getOriginalFileName() + " cannot be deleted since it is in use by the following entries: " + entryNames);
				return;
			}
		}
	  	// not locked (or locking mechanism failed!!!), safe to delete
		String msPath = getMSPath(entry);
		File file = new File (msPath + File.separator + fileName);
		File parentDir = file.getParentFile();
		try {
			if (file.exists()) {
				boolean d = file.delete();
				if (d) {
					// remove the parentDir
					parentDir.delete();
					// remove from the lock table
					if (fileLockManager != null) {
						fileLockManager.deleteFile (fileName);
						FileLockingUtils.writeLockFile(fileLockManager, pp.getLockFilePath(entry));	
					}
					removeDataFileFromProperty (pp, dataFile);
					saveFileChanges();
					fileTableViewer.refresh();
					fileTableViewer.expandAll();
				} else {
					MessageDialog.openError(activeShell, "Error", "File could not be removed");
				}
			} else {
				// remove it from the property
				removeDataFileFromProperty (pp, dataFile);
				saveFileChanges();
				fileTableViewer.refresh();
				fileTableViewer.expandAll();
				MessageDialog.openWarning(activeShell, "Warning", "The file: " + file.getName() + " has already been removed from the workspace");
			}
		} catch (Exception e) {
			logger.error("Could not update lock file", e);
		}
		
		// delete empty parents, if any
		MSPropertyDataFile emptyParent = null;
		for (MSPropertyDataFile f: pp.getMassSpecMetaData().getFileList()) {
			if (f.getChildren() != null && f.getChildren().isEmpty()) {
				if (f.getName() != null && f.getName().isEmpty()) {
					emptyParent = f;
					break;
				}
			}
		}
		if (emptyParent != null) {
			pp.getMassSpecMetaData().getFileList().remove(emptyParent);
			saveFileChanges();
			fileTableViewer.refresh();
			fileTableViewer.expandAll();
		}
	}
	
	/**
	 * remove the given file from the property. it might be a top level file or one of the children
	 * 
	 * @param pp property to be updated
	 * @param dataFile to remove
	 */
	private void removeDataFileFromProperty (MassSpecProperty pp, MSPropertyDataFile dataFile) {
		if (pp.getMassSpecMetaData().getFileList().contains(dataFile))
			pp.getMassSpecMetaData().getFileList().remove(dataFile);
		else {  // look for children
			for (PropertyDataFile file: pp.getMassSpecMetaData().getFileList()) {
				if (file instanceof MSPropertyDataFile) {
					if (((MSPropertyDataFile) file).getChildren() != null) {
						if (((MSPropertyDataFile) file).getChildren().contains(dataFile)) {
							((MSPropertyDataFile) file).getChildren().remove(dataFile);
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param shell shell to display messages in case of a problem/issue
	 * @param pp MassSpecProperty to add the new file
	 * @param parentFile parent file to attach the new file, if null the file is added as a top level file into the property
	 */
	private void addFileToEntry(Shell shell, MassSpecProperty pp, MSPropertyDataFile parentFile) {
		MassSpecFileAddDialog dialog = new MassSpecFileAddDialog(shell, pp.getMassSpecMetaData().getFileList(), parentFile);
		if (dialog.open() == Window.OK) {
			boolean modified = false;
			String file = dialog.getFileName();
			String newFileName = ""; // allow empty parent nodes
			String fileFormat = "";
			try {
				if (file != null && !file.isEmpty()) {
					fileFormat = findFileFormatType(dialog.getFileCategory(), file);
					newFileName = file;
				}
				if( fileFormat.equals("") ) {
					MessageDialog.openInformation(shell, "Error", "Unable to determine file format type for file: " + dialog.getFileName() +
							"\nPlease make sure the extension of the file is one of the recognized ones by GRITS");
					logger.error("Unable to determine file format type for file: " + dialog.getFileName());
					return;
				}

				String msFileType = findMSFileType (fileFormat);
				String originalFileName = file == null || file.isEmpty() ? "" : file.substring(file.lastIndexOf(File.separator)+1);
				MSPropertyDataFile newDataFile = new MSPropertyDataFile(newFileName, MassSpecProperty.CURRENT_VERSION, 
						fileFormat, dialog.getFileCategory(), msFileType, originalFileName, Arrays.asList(new String[]{dialog.getFileCategory().getLabel()}));
				processFile (shell, pp, newDataFile, msFileType);
				MSPropertyDataFile selectedParentFile = dialog.getParentFile();
				if (selectedParentFile != null) { // child node
					if (!newFileName.isEmpty())  {// do not allow empty children nodes
						if (selectedParentFile.containsChild(newDataFile)) {
							MessageDialog.openInformation(shell, "Info", "This file is already in the list. Not adding it again");
						} else {
							selectedParentFile.addChild(newDataFile);
							modified = true;
						}
					} else {  // should not happen since dialog should prevent it
						MessageDialog.openError(shell, "Error", "File name cannot be empty. Not adding the new entry!");
					}
				}
				else {
					if (dialog.isInstrumentFile()) {
						// adding a new parent
						newDataFile.setIsParent(true);
						MSPropertyDataFile existing = pp.getMassSpecMetaData().addFile(newDataFile);
						if (existing != null)
							MessageDialog.openInformation(shell, "Info", "This file is already in the list. Not adding it again");
						else 
							modified = true;
					} else { // not an instrument file but parent is not available, create a new empty parent
						List<String> purpose = dialog.getFileCategory().equals(FileCategory.ANNOTATION_CATEGORY) ? 
								Arrays.asList(new String[] {FileCategory.ANNOTATION_CATEGORY.getLabel()}) : 
								Arrays.asList(new String[] {FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY.getLabel()});
						MSPropertyDataFile newParent = new MSPropertyDataFile("", MassSpecProperty.CURRENT_VERSION, 
								MSFileInfo.MSFORMAT_RAW_EXTENSION, dialog.getFileCategory(), MSFileInfo.MS_FILE_TYPE_INSTRUMENT, "", purpose );
						newParent.setIsParent(true);
						MSPropertyDataFile existing = pp.getMassSpecMetaData().addFile(newParent); // if this parent already exists, add the new file to the existing one
						if (existing != null) { // newParent is not added
							if (existing.containsChild(newDataFile)) {
								MessageDialog.openInformation(shell, "Info", "This file is already in the list. Not adding it again");
							} else {
								existing.addChild(newDataFile);
								modified = true;
							}
						}
						else {  // newParent is added
							newParent.addChild(newDataFile);
							modified = true;
						}
					}
				}

				if (modified) {
					saveFileChanges();
					fileTableViewer.setInput(pp.getMassSpecMetaData().getFileList());
					fileTableViewer.refresh();
					fileTableViewer.expandAll();
				}
			} catch (IOException | JAXBException e) {
				logger.error("Could not upload the file", e);
				MessageDialog.openError(shell, "Upload Error", "Could not upload the file. Reason: " + e.getMessage());
			}	
		}
	}

	/**
	 * determine the file type based on the category and type (file extension) given
	 * 
	 * @param type file format type (mzML, mzXML, ThermoExtract, etc)
	 * @return file type (Instrument, Processed or Converted)
	 */
	public static String findMSFileType (String type) {
		switch( type ) {
		case MSFileInfo.MSFORMAT_RAW_TYPE :
			return MSFileInfo.MS_FILE_TYPE_INSTRUMENT;
		case MSFileInfo.MSFORMAT_MZML_TYPE :
			return MSFileInfo.MS_FILE_TYPE_DATAFILE;
		case MSFileInfo.MSFORMAT_MZXML_TYPE :
			return MSFileInfo.MS_FILE_TYPE_DATAFILE;
		case PeakListInfo.PEAKLISTFORMAT_EXTRACT_TYPE :
			return MSFileInfo.MS_FILE_TYPE_PROCESSED;
		case PeakListInfo.PEAKLISTFORMAT_MASCOTGENERIC_TYPE :
			return MSFileInfo.MS_FILE_TYPE_PROCESSED;
		case PeakListInfo.PEAKLISTFORMAT_TABDELIMITED_TYPE :
			return MSFileInfo.MS_FILE_TYPE_PROCESSED;
		default:
			return "";							
		}
	}

	/**
	 * Determine the format of the file based on the category and extension of the file
	 * 
	 * @param category category of the file
	 * @param filename name of the file
	 * @return format of the file (raw, MS.mzML, FullMS.mzXML etc.)
	 * @see MSFileInfo
	 * @see PeakListInfo
	 */
	public static String findFileFormatType (FileCategory category, String filename) {
		if (filename != null && !filename.isEmpty()) {
			int extensionIdx = filename.lastIndexOf(".");			
			if (extensionIdx != -1) {
				String sExtension = filename.substring(extensionIdx+1);
				if (category.equals(FileCategory.ANNOTATION_CATEGORY)) { // annotation file, currently only 
					switch( sExtension ) {
					case MSFileInfo.MSFORMAT_MZML_EXTENSION :
						return MSFileInfo.MSFORMAT_MZML_TYPE;
					case MSFileInfo.MSFORMAT_MZXML_EXTENSION :
						return MSFileInfo.MSFORMAT_MZXML_TYPE;
					case MSFileInfo.MSFORMAT_RAW_EXTENSION:
					case MSFileInfo.MSFORMAT_RAW_TYPE:
						return MSFileInfo.MSFORMAT_RAW_TYPE;
					default:
						return MSFileInfo.MSFORMAT_RAW_TYPE;							
					}
				} else if (category.equals(FileCategory.EXTERNAL_QUANTIFICATION_CATEGORY)) {
					switch( sExtension ) {
					case MSFileInfo.MSFORMAT_MZML_EXTENSION :
						return MSFileInfo.MSFORMAT_MZML_TYPE;
					case MSFileInfo.MSFORMAT_MZXML_EXTENSION :
						return MSFileInfo.MSFORMAT_MZXML_TYPE;
					case MSFileInfo.MSFORMAT_RAW_EXTENSION:
					case MSFileInfo.MSFORMAT_RAW_TYPE:
						return MSFileInfo.MSFORMAT_RAW_TYPE;
					case PeakListInfo.PEAKLISTFORMAT_EXTRACT_XML_EXTENSION :
						return PeakListInfo.PEAKLISTFORMAT_EXTRACT_TYPE;
					case PeakListInfo.PEAKLISTFORMAT_MASCOTGENERIC_MGF_EXTENSION :
						return PeakListInfo.PEAKLISTFORMAT_MASCOTGENERIC_TYPE;
					case PeakListInfo.PEAKLISTFORMAT_TABDELIMITED_TXT_EXTENSION :
						return PeakListInfo.PEAKLISTFORMAT_TABDELIMITED_TYPE;
					default:
						return MSFileInfo.MSFORMAT_RAW_TYPE;							
					}
				}
			}
		}
		return "";
	}

	/**
	 * validate the file and copy using a progress dialog
	 * 
	 * @param shell shell to display error messages
	 * @param msProperty property to get the list of existing files (to be used to determine if we need a new folder) and the location of the lockfile
	 * @param newDataFile data file to be processed
	 * @param fileType type of the file (instrument, processed, converted)
	 * @throws IOException
	 * @throws JAXBException
	 */
	public void processFile (Shell shell, MassSpecProperty msProperty, MSPropertyDataFile newDataFile, String fileType) throws IOException, JAXBException {
		IRunnableWithProgress runnable = new IRunnableWithProgress() { 
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					List<MSPropertyDataFile> existingFiles = msProperty.getMassSpecMetaData().getFileList();
					monitor.beginTask ("Validating the file...", IProgressMonitor.UNKNOWN);
					MSFile msFile = newDataFile.getMSFileWithReader("", msProperty.getMassSpecMetaData().getMsExperimentType());
					if ((newDataFile.getMSFileType().equals(MSFileInfo.MS_FILE_TYPE_DATAFILE) || newDataFile.getMSFileType().equals(MSFileInfo.MS_FILE_TYPE_PROCESSED)) && msFile.getReader() == null) {
						throw new Exception ("The specified MS File (" + newDataFile.getName() + ") cannot be read.\nMake sure you've selected the correct categorty and type");
					} else if (msFile.getReader() != null) { 
						boolean bPass = msFile.getReader().isValid(msFile);
						if( ! bPass ) {
							throw new Exception ("The specified MS File (" + newDataFile.getName() + ") is not valid.\nPlease correct this and continue.");
						}
					}
					//get ms folder
					String msPath = getMSPath(entry);
					String entryPath = "";
					// need to copy file to local folder
					// get filename
					if (existingFiles != null && !existingFiles.isEmpty()) {
						String f = existingFiles.get(0).getName();
						if (f.contains(File.separator))
							entryPath = f.substring(0, f.lastIndexOf(File.separator));
					} else {
						SimpleDateFormat formater = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss.SSSS");
						Date date = new Date();
						entryPath = formater.format(date);
						File destMSPath = new File(msPath + File.separator + entryPath);
						destMSPath.mkdirs();
					}

					List<MSPropertyDataFile> allFiles = new ArrayList<>();
					allFiles.add(newDataFile);
					CopyFilesRunnableWithProgress runner = new CopyFilesRunnableWithProgress(allFiles, entry, msPath, entryPath);
					runner.run(monitor);
					if( ! runner.isSuccessful() ) {
						throw new Exception("Error occurred while copying files into workspace.");
					}
					if( ! runner.iCanceled() ) {
						String newFileName = newDataFile.getName();    // modified in CopyFilesRunnableWithProgress	
						// add the files into FileLockManager's list
						addFilesToLockFile(newFileName, msProperty.getLockFilePath(entry));
					}
				} catch (Exception e) {
					throw new InvocationTargetException(e);
				}
			}
		};

		ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(shell);
		try {
			progressMonitorDialog.run(true, false, runnable);
		} catch (InvocationTargetException e) {
			String cause = "";
			if (e.getTargetException() instanceof InvocationTargetException) {
				if (((InvocationTargetException) e.getTargetException()).getTargetException() != null)
					cause = ((InvocationTargetException) e.getTargetException()).getTargetException().getMessage();
			} else if (e.getTargetException() instanceof Throwable) {
				cause = e.getTargetException().getMessage();
			}
			MessageDialog.openError(shell, "Error", cause);
			logger.error("Error occurred while copying files into workspace.");
		} catch (InterruptedException e) {
			logger.info("Copying files interrupted", e);
		}
	}

	/**
	 * find the full Mass Spec (ms) path for the given entry
	 * MS files are under <workspace_location>/<project_name>/ms/
	 * 
	 * @param entry to start searching for the top-level "project" entry
	 * @return the full path for the ms files
	 */
	private static String getMSPath (Entry entry) {
		String workspaceLocation = PropertyHandler.getVariable("workspace_location");
		Entry projectEntry = DataModelSearch.findParentByType(entry, ProjectProperty.TYPE);
		String projectName = projectEntry.getDisplayName();
		File msFolder = new File(workspaceLocation + projectName + File.separator + MassSpecProperty.getFoldername() );
		return msFolder.getAbsolutePath();
	}

	/**
	 * read the lock file, lock the given file and write the lock file back
	 * 
	 * @param filePath name of the file to be locked
	 * @param lockFilePath the path of the lock file
	 * @throws IOException if the lock file cannot be read/updated
	 * @throws JAXBException if the lock file cannot be read/written
	 */
	private void addFilesToLockFile (String filePath, String lockFilePath) throws IOException, JAXBException {
		fileLockManager = FileLockingUtils.readLockFile(lockFilePath);
		fileLockManager.addFile(filePath);
		FileLockingUtils.writeLockFile(fileLockManager, lockFilePath);	
	}

	/**
	 * download the file (from the GRITS' ms folder) into the file system
	 * 
	 * @param shell shell the display error messages
	 * @param filename the name of the file to be download
	 */
	private void downloadAction (Shell shell, String filename) {
		// open up a file dialog to download the file
		FileDialog fd = new FileDialog(shell, SWT.SAVE);
		fd.setText("Download");
		fd.setFileName(filename);
		fd.setOverwrite(true);
		String selected = fd.open();
		try {
			if (selected != null && selected.trim().length() != 0) {
				downloadFile (filename, selected);
			}
		} catch (IOException e1) {
			logger.error("Could not download file", e1);
			MessageDialog.openError(shell, "File Download Error", "Could not download the file. It has been removed from the workspace");
		}
	}
	
	/**
	 * copy the selected file from GRITS' ms folder into the user's selected location in the file system
	 * 
	 * @param file the path of the file
	 * @param newPath the new path of the file
	 * @throws IOException if an error occurs during copying
	 */
	protected void downloadFile(String file, String newPath) throws IOException {
		String msPath = getMSPath(entry);
		String filePath = msPath + File.separator + file;
		File workspaceFile = new File(filePath);
		FileOutputStream out = new FileOutputStream(newPath);
		Files.copy(workspaceFile.toPath(), out);
		out.close();
	}

	private Combo createCombo(Label label, Combo combo, String labelName, Set<String> values, String selected) {
		GridData derivatizationLabelData = new GridData();
		label = new Label(getParent(), SWT.NONE);
		label.setText(labelName);
		label.setLayoutData(derivatizationLabelData);

		//create list of species
		combo = new Combo(getParent(), SWT.FLAT | SWT.READ_ONLY);
		GridData comboData = new GridData();
		comboData.grabExcessHorizontalSpace = true;
		comboData.horizontalAlignment = GridData.FILL;
		comboData.horizontalSpan = 2;
		combo.setLayoutData(comboData);

		//add empty string as the first item in a combo list
		combo.add("");

		//add empty selection
		int count = 1;
		int sel = -1;

		if(values != null) {
			for(String tt : values) {
				if(selected != null) {
					if(tt.equals(selected)) {
						sel = count;
					} 
				}
				combo.add(tt);
				count++;
			}
		}

		if(sel == -1 && selected != null && ! selected.equals(""))
		{
			//which means the preference xml is old version or does not have the value shown in the entry in project xml.
			// try to find the label from the new version
			String selectedLabel = Method.getMsLabelByType(selected);
			if (selectedLabel != null && combo.indexOf(selectedLabel) != -1)
				combo.select(combo.indexOf(selectedLabel));
			else { // if we cannot match
				//then add this value into the list and select it
				combo.add(selected);
				combo.select(values.size()+1);
			}
			combo.add("other");
			//need to update
			updateThisCombo(labelName,selected,combo.getItems());
		}
		else {
			combo.select(sel);
			//add "other" string as the last item in a combo list
			combo.add("other");
		}

		//add a listener
		ComboPopupSelectionListener comboLister = new ComboPopupSelectionListener();
		comboLister.setParent(getParent());
		combo.addSelectionListener(comboLister);
		combo.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				setDirty(true);
			}
		});
		return combo;
	}

	private void createDescription(MassSpecProperty pp) {
		descriptionLabel = new Label(getParent(), SWT.LEFT);
		descriptionLabel.setText("Description");
		descriptionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 6));
		
		GridData descriptionTextData = new GridData();
		descriptionTextData.horizontalSpan = 2;
		descriptionTextData.verticalSpan = 6;
		descriptionTextData.horizontalAlignment = GridData.FILL;
		descriptionTextData.verticalAlignment = GridData.FILL;
		descriptionTextData.grabExcessVerticalSpace = true;
		descriptionTextData.grabExcessHorizontalSpace = true;
		descriptionText = new Text(getParent(),SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		String sDesc = pp.getMassSpecMetaData() != null && pp.getMassSpecMetaData().getDescription() != null ? pp.getMassSpecMetaData().getDescription() : "";
		descriptionText.setText(sDesc);
		descriptionText.setLayoutData(descriptionTextData);
		descriptionText.addModifyListener(getModListener());
		descriptionText.addTraverseListener(ListenerFactory.getTabTraverseListener());
		descriptionText.addKeyListener(ListenerFactory.getCTRLAListener());
	}

	//how to validate input for editor?
	protected boolean isValidInput() {
		if(!checkBasicLengthCheck(descriptionLabel, descriptionText, 0, Integer.parseInt(PropertyHandler.getVariable("descriptionLength"))))
		{
			return false;
		}
		return true;
	}
	
	/**
	 * saves the settings file back (with potential file addition/removal)
	 */
	private void saveFileChanges() {
		try {
			Entry projectEntry = DataModelSearch.findParentByType(this.entry, ProjectProperty.TYPE);
			MassSpecProperty property = null;
			if( this.entry.getProperty() instanceof  MassSpecEntityProperty ) {
				property = (MassSpecProperty) ((MassSpecEntityProperty) this.entry.getProperty()).getParentProperty();
			} else {
				property = (MassSpecProperty)this.entry.getProperty();
			}
			MassSpecMetaData settings = property.getMassSpecMetaData();
			String settingsFile = MassSpecProperty.getFullyQualifiedFolderName(projectEntry) + File.separator + property.getMSSettingsFile().getName();
			property.marshallSettingsFile(settingsFile, settings);
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
	}

	/**
	 * save the changes made in the page (file changes are saved immediately, this is for other fields such as description etc.)
	 */
	protected void updateProjectProperty() {
		try {
			Entry projectEntry = DataModelSearch.findParentByType(this.entry, ProjectProperty.TYPE);
			MassSpecProperty property = null;
			if( this.entry.getProperty() instanceof  MassSpecEntityProperty ) {
				property = (MassSpecProperty) ((MassSpecEntityProperty) this.entry.getProperty()).getParentProperty();
			} else {
				property = (MassSpecProperty)this.entry.getProperty();
			}
			MassSpecMetaData settings = property.getMassSpecMetaData();
			settings.setDescription(descriptionText.getText());
			settings.setInstrument(instrumentText.getText());
			settings.setUpdateDate(new Date());
			if (experimentTypeCombo.getSelectionIndex() != -1) {
				String selectedLabel = experimentTypeCombo.getItem(experimentTypeCombo.getSelectionIndex());
				settings.setMsExperimentType(Method.getMsTypeByLabel(selectedLabel));
			}

			String settingsFile = MassSpecProperty.getFullyQualifiedFolderName(projectEntry) + File.separator + property.getMSSettingsFile().getName();
			property.marshallSettingsFile(settingsFile, settings);
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
	}

	@Focus
	public void setFocus() {
		// add this line to get focus on editor, otherwise double-click to open editors may not work
		getParent().setFocus();

		if ( descriptionLabel != null )
			descriptionLabel.setFocus();
	}
	
	public void setDirty(boolean d) {
		this.dirtyable.setDirty(d);
	}
	
	public boolean isDirty() {
		return this.dirtyable.isDirty();
	}
	
	public Label getDescriptionLabel() {
		return descriptionLabel;
	}

	private void updateThisCombo(String labelName, String selected, String[] values) {
		try {
			if(labelName.equals("MS Experiment Type"))
			{
				preferences.getAllExperimentTypes().add(selected);
			}
			preferences.saveValues();
		} catch( Exception e ) {
			logger.error(e.getMessage(), e);
			ErrorUtils.createErrorMessageBox(getParent().getShell(), "Preference Error",e);			
		}
	}


	@Override
	protected void savePreference() {
		try {
			preferences.saveValues();
		} catch( Exception ex ) {
			logger.error(ex.getMessage(), ex);
		}
	}

	private boolean loadWorkspacePreferences() {
		try {
			preferences = MassSpecPreferenceLoader.getMassSpecPreferences();

		} catch (Exception ex) {
			logger.error("Error getting the mass spec preferences", ex);
		}
		return (preferences != null);
	}
}
