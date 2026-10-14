/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.helper;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Writes messages of the LPEX menu extension to the platform log.
 * <p>
 * Trace messages are written to <code>System.out</code>. They are enabled with
 * the system property <code>biz.isphere.ide.lpex.menu.trace</code> set to
 * <code>true</code>, e.g. in the VM arguments of a launch configuration.
 */
public final class LogHelper {

    private static final String TRACE_PROPERTY = "biz.isphere.ide.lpex.menu.trace"; //$NON-NLS-1$
    private static final String TRACE_PREFIX = "[LpexMenuExtension] "; //$NON-NLS-1$

    private static final boolean TRACE = "true".equalsIgnoreCase(System.getProperty(TRACE_PROPERTY)); //$NON-NLS-1$

    private LogHelper() {
    }

    public static void logError(String message, Throwable e) {
        log(IStatus.ERROR, message, e);
    }

    public static void logWarning(String message) {
        log(IStatus.WARNING, message, null);
    }

    public static boolean isTraceEnabled() {
        return TRACE;
    }

    public static void trace(String message) {
        if (TRACE) {
            System.out.println(TRACE_PREFIX + message);
        }
    }

    private static void log(int severity, String message, Throwable e) {

        try {
            Bundle bundle = FrameworkUtil.getBundle(LogHelper.class);
            if (bundle != null) {
                Platform.getLog(bundle).log(new Status(severity, bundle.getSymbolicName(), message, e));
                return;
            }
        } catch (Throwable ignore) {
            // Fall back to the console.
        }

        System.err.println(TRACE_PREFIX + message);
        if (e != null) {
            e.printStackTrace();
        }
    }
}
