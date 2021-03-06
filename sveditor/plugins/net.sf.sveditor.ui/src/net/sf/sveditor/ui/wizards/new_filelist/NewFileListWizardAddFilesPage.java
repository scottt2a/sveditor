package net.sf.sveditor.ui.wizards.new_filelist;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.SVFileUtils;
import net.sf.sveditor.core.argfile.creator.SVArgFileCreator;
import net.sf.sveditor.core.db.index.ISVDBFileSystemProvider;
import net.sf.sveditor.core.db.index.SVDBWSFileSystemProvider;
import net.sf.sveditor.ui.ResourceSelCheckboxMgr;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class NewFileListWizardAddFilesPage extends WizardPage {
	private CheckboxTreeViewer			fTreeView;
	private ResourceSelCheckboxMgr		fCheckMgr;
	private Text						fText;
	private boolean						fOrganizeFiles;
	private Button						fOrganizeFilesButton;
	private Button						fUpdateButton;
	private boolean						fUpdateRequired = true;
	private SVArgFileCreator			fArgFileCreator;
	private ISVDBFileSystemProvider		fFSProvider;
	private Set<String>					fSVFileExts;
	
	public NewFileListWizardAddFilesPage() {
		super("Add Files", 
				"Locate SystemVerilog files to populate the filelist", null);
		fFSProvider = new SVDBWSFileSystemProvider();
		fArgFileCreator = new SVArgFileCreator(fFSProvider);
		fSVFileExts = new HashSet<String>();
		
		for (String ext : SVCorePlugin.getDefault().getDefaultSVExts()) {
			fSVFileExts.add(ext);
		}
	}
	
	public String getArgFileContent() {
		StringBuilder sb = new StringBuilder();

		if (fOrganizeFiles) {
			for (String incdir : fArgFileCreator.getIncDirs()) {
				sb.append("+incdir+" + incdir + "\n");
			}
		}
	
		if (fOrganizeFiles) {
			for (String path : fArgFileCreator.getRootFiles()) {
				sb.append(path + "\n");
			}
		} else {
			for (String path : fArgFileCreator.getFiles()) {
				sb.append(path + "\n");
			}
		}
		
		return sb.toString();
	}	
	
	public boolean updateRequired() {
		return fUpdateRequired;
	}
	
	public ISVDBFileSystemProvider getFSProvider() {
		return fFSProvider;
	}

	@Override
	public void createControl(Composite parent) {
		GridData gd;
		Label l;
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout());
		
		SashForm sash = new SashForm(c, SWT.VERTICAL+SWT.FLAT);
		sash.setSashWidth(4);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Group g;
		
		g = new Group(sash, SWT.FLAT);
		g.setText("Source Files to Include");
		g.setLayout(new GridLayout(2, false));
//		g.setLayout(new GridLayout());
		fTreeView = new CheckboxTreeViewer(g);
		fTreeView.setLabelProvider(new WorkbenchLabelProvider());
		fTreeView.setContentProvider(new WorkbenchContentProvider());
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		fTreeView.getTree().setLayoutData(gd);
		fCheckMgr = new ResourceSelCheckboxMgr(fTreeView) {

			@Override
			protected boolean shouldIncludeInBlockSelection(Object parent, Object elem) {
				if (elem instanceof IFile) {
					String path = ((IFile)elem).getFullPath().toString();
					String ext = SVFileUtils.getPathExt(path);
					
					return (ext != null && fSVFileExts.contains(ext));
				}
				return super.shouldIncludeInBlockSelection(parent, elem);
			}
		};
		fTreeView.addCheckStateListener(fCheckMgr);
		
		fTreeView.setInput(ResourcesPlugin.getWorkspace());

		l = new Label(g, SWT.NONE);
		l.setText("Organize Files: ");
		fOrganizeFiles = true;
		fOrganizeFilesButton = new Button(g, SWT.CHECK);
		fOrganizeFilesButton.addSelectionListener(fSelectionListener);
		fOrganizeFilesButton.setSelection(true);
		
		fUpdateButton = new Button(g, SWT.PUSH);
		fUpdateButton.setText("Compute Filelist");
		fUpdateButton.addSelectionListener(fSelectionListener);
		
		fTreeView.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Display.getDefault().asyncExec(new Runnable() {
					
					@Override
					public void run() {
						fUpdateRequired = true;
						validate();
					}
				});
			}
		});
		
	
		g = new Group(sash, SWT.FLAT);
		g.setText("Filelist Contents");
		g.setLayout(new GridLayout());
		fText = new Text(g, SWT.NONE+SWT.READ_ONLY+SWT.H_SCROLL+SWT.V_SCROLL);
		fText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		validate();
		
		setControl(c);
	}
	
	private void validate() {
		String msg = null;
		String msg_info = null;
		List<Object> items = fCheckMgr.getCheckedItems();
	
		if (items.size() == 0) {
			if (msg == null) {
				msg = "Select Source Folders and Files";
			}
			fUpdateButton.setEnabled(false);
		} else {
			fUpdateButton.setEnabled(true);
		}
		
		if (fUpdateRequired) {
			msg_info = "Need to Update";
		}
	
		setMessage(null, INFORMATION);
		setMessage(null, WARNING);
		setErrorMessage(null);
		
		if (msg != null) {
			setErrorMessage(msg);
		} else if (msg_info != null) {
			setMessage(msg_info, INFORMATION);
		}
		
		setPageComplete(msg == null);
	}
	
	public void runUpdateOperation() {
		try {
			List<String> search_paths = new ArrayList<String>();
			for (Object r : fCheckMgr.getCheckedItems()) {
				// We've pre-filtered files here
				if (r instanceof IFile) {
					search_paths.add("${workspace_loc}" + ((IResource)r).getFullPath());
				}
			}
					
			fArgFileCreator.setSearchPaths(search_paths);
			
			getContainer().run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
				InterruptedException {
					
					fArgFileCreator.discover_files(new SubProgressMonitor(monitor, 50));
					
					if (fOrganizeFiles) {
						fArgFileCreator.organize_files(new SubProgressMonitor(monitor, 50));
					}
					
				}
			});
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		} catch (InvocationTargetException ex) {
			ex.printStackTrace();
		} 			
	}
	
	private SelectionListener			fSelectionListener = new SelectionListener() {
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			if (e.widget == fUpdateButton) {
				// TODO: run update

				runUpdateOperation();
				fText.setText(getArgFileContent());
			
				fUpdateRequired = false;
				validate();
			} else if (e.widget == fOrganizeFilesButton) {
				if (fOrganizeFiles != fOrganizeFilesButton.getSelection()) {
					fOrganizeFiles = fOrganizeFilesButton.getSelection();
					fUpdateRequired = true;
				}
				validate();
			}
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) { }
	};

}
