/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/
package biz.isphere.core.xmlparser.internal;

import java.util.Stack;

public class CallbackHandlerStack {

    Stack<CallbackHandlerEntry> callbackHandlerStack;

    public CallbackHandlerStack() {
        this.callbackHandlerStack = new Stack<CallbackHandlerEntry>();
    }

    public void pushEntry(CallbackHandlerEntry callbackHandlerEntry) {
        callbackHandlerStack.push(callbackHandlerEntry);
    }

    public void popEntry() {
        callbackHandlerStack.pop();
    }

    public CallbackHandlerEntry peekEntry() {
        if (callbackHandlerStack.isEmpty()) {
            return null;
        }
        return callbackHandlerStack.peek();
    }
}
