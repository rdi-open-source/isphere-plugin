/*******************************************************************************
 * Copyright (c) 2012-2014 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.spooledfiles;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import com.ibm.as400.access.QSYSObjectPathName;
import com.ibm.as400.access.SpooledFile;

public class SpooledFileTransformerHTML extends AbstractSpooledFileTransformer {

    private BufferedWriter writer = null;

    public SpooledFileTransformerHTML(String connectionName, SpooledFile spooledFile) {
        super(connectionName, spooledFile);
    }

    @Override
    protected QSYSObjectPathName getWorkstationCustomizationObject() {
        return new QSYSObjectPathName(getISphereLibrary(), "SPLFHTML", "WSCST");
    }

    @Override
    public String getCharset() {
        return Charset.defaultCharset().name();
    }

    @Override
    protected void openPrinter(String target) throws IOException {
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(target), getCharset()));
    }

    @Override
    protected void closePrinter() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    @Override
    protected void initPrinter() throws IOException {
        writer.write("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=" + getCharset()
            + "\"><title></title></head><body><table><tr><td><pre>");
    }

    @Override
    protected void resetPrinter() throws IOException {
        writer.write("</pre></td></tr></table></body></html>");
    }

    @Override
    protected void formfeed() throws IOException {
        writer.write("</pre><hr/><pre>");
    }

    @Override
    protected void newLine() throws IOException {
        writer.write(CR_LF);
    }

    @Override
    protected void print(String text) throws IOException {
        writer.write(text);
    }

}
