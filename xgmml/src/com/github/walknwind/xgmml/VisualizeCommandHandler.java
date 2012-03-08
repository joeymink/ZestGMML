package com.github.walknwind.xgmml;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class VisualizeCommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getActiveMenuSelection(event);
        if (selection instanceof TreeSelection)
        {
                TreeSelection navigatorSelection = (TreeSelection)selection;
                TreePath path = navigatorSelection.getPaths()[0];
                if (path.getFirstSegment() instanceof IProject)
                {
                        if (path.getLastSegment() instanceof IFile)
                        {
                        	IFile selectedFile = ((IFile)path.getLastSegment());
                    		try {
                    			IViewPart viewPart = getWorkbenchPage().showView(XgmmlView.ID);
                    			((XgmmlView)viewPart).setGraph(selectedFile);
                    		} catch (PartInitException e) {
                    			// TODO Auto-generated catch block
                    			e.printStackTrace();
                    		}
                        }
                }
        }
        return null;
	}

    private IWorkbenchPage getWorkbenchPage()
    {
            IWorkbench iworkbench = PlatformUI.getWorkbench();
            if (iworkbench == null)
                    return null;
            IWorkbenchWindow iworkbenchwindow = iworkbench.getActiveWorkbenchWindow();
            if (iworkbenchwindow == null)
                    return null;
            IWorkbenchPage iworkbenchpage = iworkbenchwindow.getActivePage();
            return iworkbenchpage;
    }
}
