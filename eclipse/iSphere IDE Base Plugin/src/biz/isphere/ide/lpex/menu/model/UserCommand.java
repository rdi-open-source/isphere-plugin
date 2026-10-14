/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.model;

/**
 * User command of the LPEX editor: the name of the command and the class that
 * implements <code>com.ibm.lpex.core.LpexCommand</code>.
 */
public class UserCommand extends AbstractLpexAction<UserCommand> implements Comparable<UserCommand> {

    private String className;

    public UserCommand(String commandName, String className) {
        super(commandName);
        this.className = className;
    }

    public String getCommandName() {
        return getActionId();
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return getCommandName() + ACTION_DELIMITER + className;
    }

    @Override
    public int compareTo(UserCommand command) {

        if (command == null || command.getCommandName() == null) {
            return 1;
        }

        return getCommandName().compareTo(command.getCommandName());
    }
}
