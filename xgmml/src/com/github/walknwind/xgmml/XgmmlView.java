package com.github.walknwind.xgmml;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.internal.dot.DotImport;
import org.eclipse.zest.internal.dot.DotUiActivator;

import com.github.walknwind.xg2d.XgmmlConverter;

/**
 * View showing the Zest import for an XGMML input. It actually converts the
 * XGMML to DOT and relies largely on Zest's DOT support. This class
 * was a rework/modification of Zest's org.eclipse.zest.internal.dot.ZestGraphView.
 * 
 * @author walk_n_wind
 */
public final class XgmmlView extends ViewPart {

	public static final String ID = "com.github.walknwind.xgmml.XgmmlView"; //$NON-NLS-1$

	private static final String EXTENSION = "xgmml";
	
	private static final RGB BACKGROUND = JFaceResources.getColorRegistry()
			.getRGB("org.eclipse.jdt.ui.JavadocView.backgroundColor"); //$NON-NLS-1$

	private Composite composite;
	private Graph graph;
	private IFile file;

	private String dotString = ""; //$NON-NLS-1$

	/** Listener that passes a visitor if a resource is changed. */
	private IResourceChangeListener resourceChangeListener = new IResourceChangeListener() {
		public void resourceChanged(final IResourceChangeEvent event) {
			if (event.getType() != IResourceChangeEvent.POST_BUILD
					&& event.getType() != IResourceChangeEvent.POST_CHANGE) {
				return;
			}
			IResourceDelta rootDelta = event.getDelta();
			try {
				rootDelta.accept(resourceVisitor);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	};

	/**
	 * If a *.dot file or a file with DOT content is visited, we update the
	 * graph from it.
	 */
	private IResourceDeltaVisitor resourceVisitor = new IResourceDeltaVisitor() {
		/**
		 * We only want to react to a resource change if the file currently being viewed
		 * is updated/deleted.
		 */
		public boolean visit(final IResourceDelta delta) {
			IResource resource = delta.getResource();
			if (resource.getType() == IResource.FILE) {
				try {
					final IFile f = (IFile) resource;
					if (file == null || !file.equals(f)) {
						return true;
					}
					setGraph(f);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return true;
		}

	};

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(final Composite parent) {
		composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		composite.setBackground(new Color(composite.getDisplay(), BACKGROUND));
		if (file != null) {
			try {
				updateGraph();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener);
	}

	public Graph getGraph() {
		return graph;
	}

	public void setGraph(final String dot, boolean async) {
		dotString = dot;
		Runnable runnable = new Runnable() {
			public void run() {
				updateZestGraph(dot);
			}

			private void updateZestGraph(final String currentDot) {
				if (graph != null) {
					graph.dispose();
				}
				if (composite != null) {
					DotImport dotImport = new DotImport(dotString);
					if (dotImport.getErrors().size() > 0) {
						String message = String.format(
								"Could not import DOT: %s, DOT: %s", //$NON-NLS-1$
								dotImport.getErrors(), dotString);
						DotUiActivator
								.getDefault()
								.getLog()
								.log(new Status(Status.ERROR,
										DotUiActivator.PLUGIN_ID, message));
						return;
					}
					graph = dotImport.newGraphInstance(composite, SWT.NONE);
					setupLayout();
					composite.layout();
					graph.applyLayout();
				}
			}
		};
		Display display = getViewSite().getShell().getDisplay();
		if (async) {
			display.asyncExec(runnable);
		} else {
			display.syncExec(runnable);
		}
	}

	public void setGraph(final IFile file) {
		if (!file.getFileExtension().equals(EXTENSION.toLowerCase()))
			throw new IllegalArgumentException(getClass().getSimpleName() + " can only view XGMML files.");
		this.file = file;
		try {
			updateGraph();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	private void updateGraph() throws MalformedURLException {
		if (file == null || file.getLocationURI() == null || !file.exists()) {
			return;
		}
		
		final String currentDot = extractDot(file);
		if (currentDot.equals(dotString)) {
			return;
		}
		setGraph(currentDot, true);
	}
	
	private String extractDot(IFile file)
	{
		try {
			ByteArrayOutputStream tempOutputStream = new ByteArrayOutputStream(); 
			XgmmlConverter xg2d = new XgmmlConverter(tempOutputStream);
			xg2d.convert(file.getContents());
			return tempOutputStream.toString("UTF-8");
		} catch (CoreException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private void setupLayout() {
		if (graph != null) {
			GridData gd = new GridData(GridData.FILL_BOTH);
			graph.setLayout(new GridLayout());
			graph.setLayoutData(gd);
			Color color = new Color(graph.getDisplay(), BACKGROUND);
			graph.setBackground(color);
			graph.getParent().setBackground(color);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	public void dispose() {
		super.dispose();
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(
				resourceChangeListener);
		if (graph != null) {
			graph.dispose();
		}
		if (composite != null) {
			composite.dispose();
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		if (graph != null && !graph.isDisposed()) {
			graph.setFocus();
		}
	}
}
