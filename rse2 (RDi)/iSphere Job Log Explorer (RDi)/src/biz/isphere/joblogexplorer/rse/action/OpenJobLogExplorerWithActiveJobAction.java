/*******************************************************************************
 * Copyright (c) 2012-2021 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.joblogexplorer.rse.action;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.rse.core.model.IHost;

import biz.isphere.base.internal.ExceptionHelper;
import biz.isphere.joblogexplorer.action.rse.AbstractOpenJobLogExplorerAction;
import biz.isphere.joblogexplorer.externalapi.Access;
import biz.isphere.joblogexplorer.rse.Messages;
import biz.isphere.rse.connection.ConnectionManager;

import com.ibm.etools.iseries.comm.interfaces.ISeriesJobName;
import com.ibm.etools.iseries.subsystems.qsys.jobs.QSYSRemoteJob;

public class OpenJobLogExplorerWithActiveJobAction extends AbstractOpenJobLogExplorerAction {

    public static final String ID = "biz.isphere.joblogexplorer.rse.action.OpenJobLogExplorerWithActiveJobAction"; //$NON-NLS-1$

    @Override
    protected void execute(Object object) {

        if (object instanceof QSYSRemoteJob) {

            try {

                QSYSRemoteJob remoteJob = (QSYSRemoteJob)object;
                IHost host = remoteJob.getRemoteJobContext().getJobSubsystem().getCmdSubSystem().getHost();
                String connectionName = ConnectionManager.getConnectionName(host);
                ISeriesJobName jobName = new ISeriesJobName(remoteJob.getFullJobName());

                Access.openJobLogExplorer(shell, connectionName, jobName.getName(), jobName.getUser(), jobName.getNumber());

            } catch (Exception e) {
                MessageDialog.openError(shell, Messages.E_R_R_O_R, ExceptionHelper.getLocalizedMessage(e));
            }
        }
    }
}
