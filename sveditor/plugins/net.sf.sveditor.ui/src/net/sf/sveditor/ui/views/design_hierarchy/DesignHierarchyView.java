package net.sf.sveditor.ui.views.design_hierarchy;

import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.design_hierarchy.DesignHierarchyNode;
import net.sf.sveditor.ui.SVEditorUtil;
import net.sf.sveditor.ui.SVUiPlugin;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

public class DesignHierarchyView extends ViewPart {
	private TreeViewer							fTreeViewer;
	private DesignHierarchyContentProvider		fContentProvider;
	private DesignHierarchyLabelProvider		fLabelProvider;
	private Action								fRefreshAction;
	private boolean								fRefreshJobRunning;
	
	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		
		IActionBars action_bars = site.getActionBars();
		
		fRefreshAction = new Action("Refresh", Action.AS_PUSH_BUTTON) {
			public void run() {
				fTreeViewer.refresh();
				synchronized (this) {
					if (!fRefreshJobRunning) {
						setRefreshJobRunning(true);
						refreshJob.schedule();
					}
				}
			}
		};
		fRefreshAction.setImageDescriptor(SVUiPlugin.getImageDescriptor("/icons/elcl16/refresh.gif"));
		action_bars.getToolBarManager().add(fRefreshAction);
	}

	private Job					refreshJob = new Job("Design Hierarchy Refresh") {
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				fContentProvider.build(monitor);
				Display.getDefault().asyncExec(refreshViewer);
			} finally {
				setRefreshJobRunning(false);
			}
			return Status.OK_STATUS;
		}
	};
	
	private Runnable			refreshViewer = new Runnable() {
		@Override
		public void run() {
			fTreeViewer.refresh();
		}
	};
	
	private synchronized void setRefreshJobRunning(boolean running) {
		fRefreshJobRunning = running;
	}
	
	@Override
	public void createPartControl(Composite parent) {
		fTreeViewer = new TreeViewer(parent);
		fTreeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		fContentProvider = new DesignHierarchyContentProvider();
		fLabelProvider = new DesignHierarchyLabelProvider();
		
		fTreeViewer.setContentProvider(fContentProvider);
		fTreeViewer.setLabelProvider(fLabelProvider);
		
		fTreeViewer.addDoubleClickListener(doubleClickListener);
		
		fTreeViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
		
		refreshJob.schedule();
	}
	
	private IDoubleClickListener doubleClickListener = new IDoubleClickListener() {
		
		@Override
		public void doubleClick(DoubleClickEvent event) {
			IStructuredSelection sel = (IStructuredSelection)fTreeViewer.getSelection();
			
			if (sel.getFirstElement() != null) {
				Object sel_o = sel.getFirstElement();
				
				if (sel_o instanceof DesignHierarchyNode) {
					DesignHierarchyNode dn = (DesignHierarchyNode)sel_o;
					Object target = dn.getTarget();
					
					if (target instanceof ISVDBItemBase) {
						try {
							SVEditorUtil.openEditor((ISVDBItemBase)target);
						} catch (PartInitException e) {
						}
					}
				}
			}
		}
	};
	

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

}
