/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.view;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.ibm.lpex.alef.LpexAbstractTextEditor;
import com.ibm.lpex.core.LpexActiveWindowListener;
import com.ibm.lpex.core.LpexMultiWindow;
import com.ibm.lpex.core.LpexView;
import com.ibm.lpex.core.LpexWindow;

import biz.isphere.ide.lpex.helper.LogHelper;
import biz.isphere.ide.lpex.menu.logic.UserEntries;

/**
 * Finds the LPEX views of all LPEX editors and attaches them to a
 * {@link ViewExtender}. LPEX does not notify about new views. Therefore the
 * tracker listens to the workbench windows, the editors and the split windows
 * of the editors:
 * <ul>
 * <li>Editors that are already open, when the tracker starts.</li>
 * <li>Editors that are opened, activated or become visible later.</li>
 * <li>Views that are added by splitting the editor window.</li>
 * </ul>
 * The tracker also listens to the shutdown of the workbench. After the
 * workbench has shut down, it calls the shutdown handler, so that the menu
 * extension is stopped even if its plug-in does not stop it.
 * <p>
 * All methods must be called on the UI thread, except
 * {@link #start(UserEntries)} and {@link #stop()}.
 */
public class ViewExtensionTracker implements IWindowListener, IPartListener2, IWorkbenchListener {

    private static final int START_DELAY = 500;
    private static final int MAX_START_ATTEMPTS = 120;

    private ViewExtender extender;
    private Runnable shutdownHandler;
    private Map<IWorkbenchWindow, Boolean> windows;
    private Map<LpexMultiWindow, Boolean> multiWindows;
    private LpexActiveWindowListener activeWindowListener;
    private DisposeListener multiWindowDisposeListener;

    private Display display;
    private volatile boolean isStopped;
    private boolean isStarted;
    private UserEntries restoredEntries;

    /**
     * Creates a tracker.
     *
     * @param extender - extender that is attached to the views
     * @param shutdownHandler - called on the UI thread, after the workbench has
     *        shut down, or <code>null</code>
     */
    public ViewExtensionTracker(ViewExtender extender, Runnable shutdownHandler) {

        this.extender = extender;
        this.shutdownHandler = shutdownHandler;
        this.windows = new IdentityHashMap<IWorkbenchWindow, Boolean>();
        this.multiWindows = new IdentityHashMap<LpexMultiWindow, Boolean>();

        this.activeWindowListener = new LpexActiveWindowListener() {
            public void activeWindowChanged(LpexWindow window) {
                if (window != null) {
                    attachViews(window.getLpexView(), null);
                }
            }
        };

        this.multiWindowDisposeListener = new DisposeListener() {
            public void widgetDisposed(DisposeEvent event) {
                multiWindows.remove(event.widget);
            }
        };
    }

    /**
     * Starts the tracker. The listeners are registered asynchronously on the UI
     * thread, as soon as the workbench is running.
     *
     * @param restoredEntries - user entries that were restored in the LPEX
     *        settings at start. They are set in the views that are already
     *        open.
     */
    public void start(UserEntries restoredEntries) {

        this.restoredEntries = restoredEntries;

        display = Display.getDefault();
        display.asyncExec(new Runnable() {

            private int attempts = 0;

            public void run() {

                if (isStopped) {
                    return;
                }

                if (!PlatformUI.isWorkbenchRunning()) {
                    attempts++;
                    if (attempts < MAX_START_ATTEMPTS) {
                        display.timerExec(START_DELAY, this);
                    } else {
                        LogHelper.logWarning("Workbench not running. LPEX views are not extended."); //$NON-NLS-1$
                    }
                    return;
                }

                try {
                    register();
                } catch (Throwable e) {
                    LogHelper.logError("Could not start tracking LPEX views.", e); //$NON-NLS-1$
                }
            }
        });
    }

    /**
     * Stops the tracker and detaches all views. If the workbench is still
     * running, e.g. because the plug-in is stopped at runtime, the extension is
     * also removed from the open views.
     */
    public void stop() {

        isStopped = true;

        try {

            if (display == null || display.isDisposed()) {
                // Workbench has been closed. Nothing left to unregister.
                LogHelper.trace("Tracker stopped (display disposed): views=" + extender.getViewCount()); //$NON-NLS-1$
                windows.clear();
                multiWindows.clear();
                extender.detachAll(false);
                return;
            }

            if (display.getThread() == Thread.currentThread()) {
                unregister();
                return;
            }

            // Called from a non-UI thread, e.g. when the plug-in is stopped
            // while the workbench is running.
            display.asyncExec(new Runnable() {
                public void run() {
                    unregister();
                }
            });

        } catch (Throwable e) {
            LogHelper.logError("Could not stop tracking LPEX views.", e); //$NON-NLS-1$
        }
    }

    private void register() {

        IWorkbench workbench = PlatformUI.getWorkbench();
        workbench.addWindowListener(this);
        workbench.addWorkbenchListener(this);
        isStarted = true;

        // Views that are open now may have applied the LPEX settings before
        // the user entries were restored.
        for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
            hookWindow(window, restoredEntries);
        }

        LogHelper.trace("Tracker started: windows=" + windows.size() + ", views=" + extender.getViewCount() + ", restoredEntries=" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + restoredEntries);
    }

    private void unregister() {

        boolean removeExtensions = false;

        try {

            if (isStarted && PlatformUI.isWorkbenchRunning()) {
                removeExtensions = !PlatformUI.getWorkbench().isClosing();
                PlatformUI.getWorkbench().removeWindowListener(this);
                PlatformUI.getWorkbench().removeWorkbenchListener(this);
                for (IWorkbenchWindow window : new ArrayList<IWorkbenchWindow>(windows.keySet())) {
                    unhookWindow(window);
                }
            }

            for (LpexMultiWindow multiWindow : new ArrayList<LpexMultiWindow>(multiWindows.keySet())) {
                if (!multiWindow.isDisposed()) {
                    multiWindow.removeActiveWindowListener(activeWindowListener);
                    multiWindow.removeDisposeListener(multiWindowDisposeListener);
                }
            }

            LogHelper.trace("Tracker stopped: views=" + extender.getViewCount() + ", removeExtensions=" + removeExtensions); //$NON-NLS-1$ //$NON-NLS-2$

        } catch (Throwable e) {
            LogHelper.logError("Could not stop tracking LPEX views.", e); //$NON-NLS-1$
        } finally {
            windows.clear();
            multiWindows.clear();
            extender.detachAll(removeExtensions);
            isStarted = false;
        }
    }

    private void hookWindow(IWorkbenchWindow window, UserEntries userEntries) {

        if (window == null || windows.containsKey(window)) {
            return;
        }

        windows.put(window, Boolean.TRUE);
        window.getPartService().addPartListener(this);

        for (IWorkbenchPage page : window.getPages()) {
            for (IEditorReference reference : page.getEditorReferences()) {
                hookEditor(reference.getEditor(false), userEntries);
            }
        }
    }

    private void unhookWindow(IWorkbenchWindow window) {

        if (windows.remove(window) != null) {
            window.getPartService().removePartListener(this);
        }
    }

    private void hookPart(IWorkbenchPartReference partReference) {

        if (isStopped || !(partReference instanceof IEditorReference)) {
            return;
        }

        hookEditor(((IEditorReference)partReference).getEditor(false), null);
    }

    private void hookEditor(IEditorPart editor, UserEntries userEntries) {

        if (!(editor instanceof LpexAbstractTextEditor)) {
            return;
        }

        try {

            LpexView view = ((LpexAbstractTextEditor)editor).getFirstLpexView();
            if (view == null) {
                // Part control not yet created.
                return;
            }

            attachViews(view, userEntries);
            hookMultiWindow(view);

        } catch (Throwable e) {
            LogHelper.logError("Could not track LPEX editor: " + editor.getTitle(), e); //$NON-NLS-1$
        }
    }

    private void attachViews(LpexView view, UserEntries userEntries) {

        if (isStopped || view == null) {
            return;
        }

        LpexView[] views = view.getLpexViews();
        if (views == null) {
            extender.attach(view, userEntries);
            return;
        }

        for (LpexView documentView : views) {
            extender.attach(documentView, userEntries);
        }
    }

    private void hookMultiWindow(LpexView view) {

        LpexWindow window = view.window();
        if (window == null) {
            return;
        }

        Composite parent = window.getParent();
        while (parent != null && !(parent instanceof LpexMultiWindow)) {
            parent = parent.getParent();
        }

        if (parent == null || parent.isDisposed()) {
            return;
        }

        LpexMultiWindow multiWindow = (LpexMultiWindow)parent;
        if (multiWindows.containsKey(multiWindow)) {
            return;
        }

        multiWindows.put(multiWindow, Boolean.TRUE);
        multiWindow.addActiveWindowListener(activeWindowListener);
        multiWindow.addDisposeListener(multiWindowDisposeListener);
    }

    /*
     * IWindowListener
     */

    public void windowActivated(IWorkbenchWindow window) {
        hookWindow(window, null);
    }

    public void windowDeactivated(IWorkbenchWindow window) {
    }

    public void windowClosed(IWorkbenchWindow window) {
        unhookWindow(window);
    }

    public void windowOpened(IWorkbenchWindow window) {
        hookWindow(window, null);
    }

    /*
     * IWorkbenchListener
     */

    public boolean preShutdown(IWorkbench workbench, boolean forced) {
        // Another listener may still cancel the shutdown.
        return true;
    }

    public void postShutdown(IWorkbench workbench) {

        if (isStopped || shutdownHandler == null) {
            return;
        }

        LogHelper.trace("Workbench shut down: stopping menu extension"); //$NON-NLS-1$

        try {
            shutdownHandler.run();
        } catch (Throwable e) {
            LogHelper.logError("Could not stop LPEX menu extension at shutdown.", e); //$NON-NLS-1$
        }
    }

    /*
     * IPartListener2
     */

    public void partActivated(IWorkbenchPartReference partReference) {
        hookPart(partReference);
    }

    public void partBroughtToTop(IWorkbenchPartReference partReference) {
        hookPart(partReference);
    }

    public void partClosed(IWorkbenchPartReference partReference) {
    }

    public void partDeactivated(IWorkbenchPartReference partReference) {
    }

    public void partOpened(IWorkbenchPartReference partReference) {
        hookPart(partReference);
    }

    public void partHidden(IWorkbenchPartReference partReference) {
    }

    public void partVisible(IWorkbenchPartReference partReference) {
        hookPart(partReference);
    }

    public void partInputChanged(IWorkbenchPartReference partReference) {
        hookPart(partReference);
    }
}
