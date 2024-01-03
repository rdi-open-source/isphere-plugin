/*******************************************************************************
 * Copyright (c) 2012-2021 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.joblogexplorer.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import biz.isphere.base.internal.IntHelper;
import biz.isphere.base.internal.StringHelper;
import biz.isphere.core.swt.widgets.ContentAssistProposal;
import biz.isphere.joblogexplorer.Messages;
import biz.isphere.joblogexplorer.editor.tableviewer.filters.AbstractIntegerFilter;
import biz.isphere.joblogexplorer.model.listeners.MessageModifyEvent;
import biz.isphere.joblogexplorer.model.listeners.MessageModifyListener;

public class JobLogMessage {

    public static final int SEVERITY_BLANK = AbstractIntegerFilter.NULL_VALUE;

    public static final String EMPTY = ""; //$NON-NLS-1$
    public static final String ZERO = "0"; //$NON-NLS-1$

    private String id;
    private String type;
    private String severity;
    private String date;
    private String time;
    private Timestamp timestamp;
    private String text;
    private String help;

    private String toLibrary;
    private String toProgram;
    private String toModule;
    private String toProcedure;
    private String toStatement;

    private String fromLibrary;
    private String fromProgram;
    private String fromModule;
    private String fromProcedure;
    private String fromStatement;

    private transient int severityInt;
    private transient String lowerCaseText;
    private transient String error;

    private transient int pageNumber;
    private transient boolean selected;
    private transient List<MessageModifyListener> listeners;

    public static enum Fields {
        ID ("ID", 0, "CHAR(7)"),
        TYPE ("TYPE", 1, "CHAR(10)"),
        SEVERITY ("SEVERITY", 2, "INTEGER"),
        FROM_LIBRARY ("FROM_LIBRARY", 3, "CHAR(10)"),
        FROM_PROGRAM ("FROM_PROGRAM", 4, "CHAR(10)"),
        FROM_MODULE ("FROM_MODULE", 5, "CHAR(10)"),
        FROM_PROCEDURE ("FROM_PROCEDURE", 6, "CHAR(*)"),
        FROM_STATEMENT ("FROM_STATEMENT", 7, "CHAR(10)"),
        TO_LIBRARY ("TO_LIBRARY", 8, "CHAR(10)"),
        TO_PROGRAM ("TO_PROGRAM", 9, "CHAR(10)"),
        TO_MODULE ("TO_MODULE", 10, "CHAR(10)"),
        TO_PROCEDURE ("TO_PROCEDURE", 11, "CHAR(*)"),
        TO_STATEMENT ("TO_STATEMENT", 12, "CHAR(10)"),
        TEXT ("TEXT", 13, "CHAR(*)"),
        HELP ("HELP", 14, "CHAR(*)"),
        TIMESTAMP ("TIMESTAMP_SENT", 15, "TIMESTAMP()");

        private String fieldName;
        private int fieldIndex;
        private String sqlType;

        private Fields(String fieldName, int fieldIndex, String sqlType) {
            this.fieldName = fieldName;
            this.fieldIndex = fieldIndex;
            this.sqlType = sqlType;
        }

        public String fieldName() {
            return fieldName;
        }

        public int fieldIndex() {
            return fieldIndex;
        }

        public String sqltype() {
            return sqlType;
        }
    }

    private static HashMap<String, Integer> columnMappings;
    static {
        columnMappings = new HashMap<String, Integer>();
        addColumnMappingEntry(columnMappings, Fields.ID);
        addColumnMappingEntry(columnMappings, Fields.TYPE);
        addColumnMappingEntry(columnMappings, Fields.SEVERITY);
        addColumnMappingEntry(columnMappings, Fields.FROM_LIBRARY);
        addColumnMappingEntry(columnMappings, Fields.FROM_PROGRAM);
        addColumnMappingEntry(columnMappings, Fields.FROM_MODULE);
        addColumnMappingEntry(columnMappings, Fields.FROM_PROCEDURE);
        addColumnMappingEntry(columnMappings, Fields.FROM_STATEMENT);
        addColumnMappingEntry(columnMappings, Fields.TO_LIBRARY);
        addColumnMappingEntry(columnMappings, Fields.TO_PROGRAM);
        addColumnMappingEntry(columnMappings, Fields.TO_MODULE);
        addColumnMappingEntry(columnMappings, Fields.TO_PROCEDURE);
        addColumnMappingEntry(columnMappings, Fields.TO_STATEMENT);
        addColumnMappingEntry(columnMappings, Fields.TEXT);
        addColumnMappingEntry(columnMappings, Fields.HELP);
        addColumnMappingEntry(columnMappings, Fields.TIMESTAMP);
    }

    private static void addColumnMappingEntry(Map<String, Integer> columnMappings, Fields field) {
        columnMappings.put(field.fieldName(), field.fieldIndex());
    }

    private static List<ContentAssistProposal> proposals;
    static {
        proposals = new LinkedList<ContentAssistProposal>();
        proposals.add(new ContentAssistProposal(Fields.ID.fieldName(), getLabel(Fields.ID, Messages.LongFieldName_ID)));
        proposals.add(new ContentAssistProposal(Fields.TYPE.fieldName(), getLabel(Fields.TYPE, Messages.LongFieldName_TYPE)));
        proposals.add(new ContentAssistProposal(Fields.SEVERITY.fieldName(), getLabel(Fields.SEVERITY, Messages.LongFieldName_SEVERITY)));
        proposals.add(new ContentAssistProposal(Fields.FROM_LIBRARY.fieldName(), getLabel(Fields.FROM_LIBRARY, Messages.LongFieldName_FROM_LIBRARY)));
        proposals.add(new ContentAssistProposal(Fields.FROM_PROGRAM.fieldName(), getLabel(Fields.FROM_PROGRAM, Messages.LongFieldName_FROM_PROGRAM)));
        proposals.add(new ContentAssistProposal(Fields.FROM_MODULE.fieldName(), getLabel(Fields.FROM_MODULE, Messages.LongFieldName_FROM_MODULE)));
        proposals.add(new ContentAssistProposal(Fields.FROM_PROCEDURE.fieldName(), getLabel(Fields.FROM_PROCEDURE,
            Messages.LongFieldName_FROM_PROCEDURE)));
        proposals.add(new ContentAssistProposal(Fields.FROM_STATEMENT.fieldName(), getLabel(Fields.FROM_STATEMENT,
            Messages.LongFieldName_FROM_STATEMENT)));
        proposals.add(new ContentAssistProposal(Fields.TO_LIBRARY.fieldName(), getLabel(Fields.TO_LIBRARY, Messages.LongFieldName_TO_LIBRARY)));
        proposals.add(new ContentAssistProposal(Fields.TO_PROGRAM.fieldName(), getLabel(Fields.TO_PROGRAM, Messages.LongFieldName_TO_PROGRAM)));
        proposals.add(new ContentAssistProposal(Fields.TO_MODULE.fieldName(), getLabel(Fields.TO_MODULE, Messages.LongFieldName_TO_MODULE)));
        proposals.add(new ContentAssistProposal(Fields.TO_PROCEDURE.fieldName(), getLabel(Fields.TO_PROCEDURE, Messages.LongFieldName_TO_PROCEDURE)));
        proposals.add(new ContentAssistProposal(Fields.TO_STATEMENT.fieldName(), getLabel(Fields.TO_STATEMENT, Messages.LongFieldName_TO_STATEMENT)));
        proposals.add(new ContentAssistProposal(Fields.TEXT.fieldName(), getLabel(Fields.TEXT, Messages.LongFieldName_TEXT)));
        proposals.add(new ContentAssistProposal(Fields.HELP.fieldName(), getLabel(Fields.HELP, Messages.LongFieldName_HELP)));
        proposals.add(new ContentAssistProposal(Fields.TIMESTAMP.fieldName(), getLabel(Fields.TIMESTAMP, Messages.LongFieldName_TIMESTAMP)));
    }

    private static String getLabel(Fields fields, String description) {
        return fields.name() + " - " + fields.sqltype() + " - " + description;
    }

    public JobLogMessage(int pageNumber) {
        this.selected = false;
        this.pageNumber = pageNumber;
        this.listeners = new ArrayList<MessageModifyListener>();

        setSeverity(null);
    }

    public static JobLogMessage createEmpty() {

        JobLogMessage jobLogMessage = new JobLogMessage(0);

        jobLogMessage.setId(EMPTY);
        jobLogMessage.setType(EMPTY);
        jobLogMessage.setSeverity(ZERO);
        jobLogMessage.setDate(EMPTY);
        jobLogMessage.setTime(EMPTY);

        jobLogMessage.setFromLibrary(EMPTY);
        jobLogMessage.setFromProgram(EMPTY);
        jobLogMessage.setFromModule(EMPTY);
        jobLogMessage.setFromProcedure(EMPTY);
        jobLogMessage.setFromStatement(EMPTY);

        jobLogMessage.setToLibrary(EMPTY);
        jobLogMessage.setToProgram(EMPTY);
        jobLogMessage.setToModule(EMPTY);
        jobLogMessage.setToProcedure(EMPTY);
        jobLogMessage.setToStatement(EMPTY);

        return jobLogMessage;
    }

    public static Comparable<?>[] getSampleRow() {

        long now = new java.util.Date().getTime();

        JobLogMessage message = new JobLogMessage(0);
        message.setId("CPF9897");
        message.setType("Information");
        message.setSeverity("40");
        message.setFromLibrary("RADDATZ");
        message.setFromProgram("LIBL#LOAD");
        message.setFromModule("LIBL#LOAD");
        message.setFromProcedure("LIBL#LOAD");
        message.setFromStatement("5500");
        message.setToLibrary("RADDATZ");
        message.setToProgram("START#RZ");
        message.setToModule("START#RZ");
        message.setToProcedure("START#RZ");
        message.setToStatement("5300");
        message.setText("This is the message text.");
        message.setHelp("This is the message help text.");
        message.setTimestamp(new java.sql.Timestamp(now));

        return message.getRow();
    }

    public String getError() {
        return error;
    }

    public void setError(Exception e) {
        setError(e.getLocalizedMessage());
    }

    private void setError(String error) {
        this.error = error;
    }

    public int getPageNumber() {
        return this.pageNumber;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean select) {
        this.selected = select;
        if (this.selected) {
            notifyModifyListeners(new MessageModifyEvent(MessageModifyEvent.SELECTED, "1"));////$NON-NLS-1$
        } else {
            notifyModifyListeners(new MessageModifyEvent(MessageModifyEvent.SELECTED, "0"));//$NON-NLS-1$
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        notifyModifyListeners(new MessageModifyEvent(MessageModifyEvent.ID, this.id));
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = trimChecked(type);
        notifyModifyListeners(new MessageModifyEvent(MessageModifyEvent.TYPE, this.type));
    }

    public String getSeverity() {
        return severity;
    }

    public int getSeverityInt() {
        return severityInt;
    }

    public void setSeverity(String severity) {
        this.severity = trimChecked(severity);
        setSeverityIntValue(this.severity);
        notifyModifyListeners(new MessageModifyEvent(MessageModifyEvent.SEVERITY, this.severity));
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getText() {
        return text;
    }

    public String getLowerCaseText() {
        return lowerCaseText;
    }

    public void setText(String text) {
        this.text = text;
        this.lowerCaseText = this.text.toLowerCase();
    }

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    public String getToModule() {
        return toModule;
    }

    public void setToModule(String toModule) {
        this.toModule = toModule;
    }

    public String getToLibrary() {
        return toLibrary;
    }

    public void setToLibrary(String toLibrary) {
        this.toLibrary = toLibrary;
        notifyModifyListeners(new MessageModifyEvent(MessageModifyEvent.TO_LIBRARY, this.toLibrary));
    }

    public String getToProgram() {
        return toProgram;
    }

    public void setToProgram(String toProgram) {
        this.toProgram = toProgram;
        notifyModifyListeners(new MessageModifyEvent(MessageModifyEvent.TO_PROGRAM, this.toProgram));
    }

    public String getToProcedure() {
        return toProcedure;
    }

    public void setToProcedure(String toProcedure) {
        this.toProcedure = toProcedure;
    }

    public String getToStatement() {
        return toStatement;
    }

    public void setToStatement(String toStatement) {
        this.toStatement = toStatement;
        notifyModifyListeners(new MessageModifyEvent(MessageModifyEvent.TO_STMT, this.toStatement));
    }

    public String getFromModule() {
        return fromModule;
    }

    public void setFromModule(String fromModule) {
        this.fromModule = fromModule;
    }

    public String getFromLibrary() {
        return fromLibrary;
    }

    public void setFromLibrary(String fromLibrary) {
        this.fromLibrary = fromLibrary;
        notifyModifyListeners(new MessageModifyEvent(MessageModifyEvent.FROM_LIBRARY, this.fromLibrary));
    }

    public String getFromProgram() {
        return fromProgram;
    }

    public void setFromProgram(String fromProgram) {
        this.fromProgram = fromProgram;
        notifyModifyListeners(new MessageModifyEvent(MessageModifyEvent.FROM_PROGRAM, this.fromProgram));
    }

    public String getFromProcedure() {
        return fromProcedure;
    }

    public void setFromProcedure(String fromProcedure) {
        this.fromProcedure = fromProcedure;
    }

    public String getFromStatement() {
        return fromStatement;
    }

    public void setFromStatement(String fromStatement) {
        this.fromStatement = fromStatement;
        notifyModifyListeners(new MessageModifyEvent(MessageModifyEvent.FROM_STMT, this.fromStatement));
    }

    private void setSeverityIntValue(String severity) {
        if (StringHelper.isNullOrEmpty(severity)) {
            severityInt = SEVERITY_BLANK;
        } else {
            severityInt = IntHelper.tryParseInt(severity, SEVERITY_BLANK);
        }
    }

    public void addModifyChangedListener(MessageModifyListener listener) {

        listeners.add(listener);
    }

    public void removeModifyListener(MessageModifyListener listener) {

        listeners.remove(listener);
    }

    private String trimChecked(String value) {

        if (value == null) {
            return value;
        }

        return value.trim();
    }

    private void notifyModifyListeners(MessageModifyEvent event) {

        for (MessageModifyListener listener : listeners) {
            listener.modifyText(event);
        }

    }

    public static HashMap<String, Integer> getColumnMapping() {
        return columnMappings;
    }

    public Comparable<?>[] getRow() {

        Comparable<?>[] row = new Comparable[columnMappings.size()];

        row[Fields.ID.fieldIndex()] = getId();
        row[Fields.TYPE.fieldIndex()] = getType();
        row[Fields.SEVERITY.fieldIndex()] = getSeverityInt();
        row[Fields.FROM_LIBRARY.fieldIndex()] = getFromLibrary();
        row[Fields.FROM_PROGRAM.fieldIndex()] = getFromProgram();
        row[Fields.FROM_MODULE.fieldIndex()] = getFromModule();
        row[Fields.FROM_PROCEDURE.fieldIndex()] = getFromProcedure();
        row[Fields.FROM_STATEMENT.fieldIndex()] = getFromStatement();
        row[Fields.TO_LIBRARY.fieldIndex()] = getToLibrary();
        row[Fields.TO_PROGRAM.fieldIndex()] = getToProgram();
        row[Fields.TO_MODULE.fieldIndex()] = getToModule();
        row[Fields.TO_PROCEDURE.fieldIndex()] = getToProcedure();
        row[Fields.TO_STATEMENT.fieldIndex()] = getToStatement();
        row[Fields.TEXT.fieldIndex()] = getText();
        row[Fields.HELP.fieldIndex()] = getHelp();
        row[Fields.TIMESTAMP.fieldIndex()] = getTimestamp();

        return row;
    }

    public static List<ContentAssistProposal> getContentAssistProposals() {
        return proposals;
    }

    @Override
    public String toString() {

        StringBuilder buffer = new StringBuilder();
        buffer.append(getId());
        buffer.append(" ("); //$NON-NLS-1$
        buffer.append(getType());
        buffer.append(") "); //$NON-NLS-1$
        buffer.append(getText());

        return buffer.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((date == null) ? 0 : date.hashCode());
        result = prime * result + ((fromLibrary == null) ? 0 : fromLibrary.hashCode());
        result = prime * result + ((fromModule == null) ? 0 : fromModule.hashCode());
        result = prime * result + ((fromProcedure == null) ? 0 : fromProcedure.hashCode());
        result = prime * result + ((fromProgram == null) ? 0 : fromProgram.hashCode());
        result = prime * result + ((fromStatement == null) ? 0 : fromStatement.hashCode());
        result = prime * result + ((help == null) ? 0 : help.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((severity == null) ? 0 : severity.hashCode());
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        result = prime * result + ((time == null) ? 0 : time.hashCode());
        result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
        result = prime * result + ((toLibrary == null) ? 0 : toLibrary.hashCode());
        result = prime * result + ((toModule == null) ? 0 : toModule.hashCode());
        result = prime * result + ((toProcedure == null) ? 0 : toProcedure.hashCode());
        result = prime * result + ((toProgram == null) ? 0 : toProgram.hashCode());
        result = prime * result + ((toStatement == null) ? 0 : toStatement.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        JobLogMessage other = (JobLogMessage)obj;
        if (date == null) {
            if (other.date != null) return false;
        } else if (!date.equals(other.date)) return false;
        if (fromLibrary == null) {
            if (other.fromLibrary != null) return false;
        } else if (!fromLibrary.equals(other.fromLibrary)) return false;
        if (fromModule == null) {
            if (other.fromModule != null) return false;
        } else if (!fromModule.equals(other.fromModule)) return false;
        if (fromProcedure == null) {
            if (other.fromProcedure != null) return false;
        } else if (!fromProcedure.equals(other.fromProcedure)) return false;
        if (fromProgram == null) {
            if (other.fromProgram != null) return false;
        } else if (!fromProgram.equals(other.fromProgram)) return false;
        if (fromStatement == null) {
            if (other.fromStatement != null) return false;
        } else if (!fromStatement.equals(other.fromStatement)) return false;
        if (help == null) {
            if (other.help != null) return false;
        } else if (!help.equals(other.help)) return false;
        if (id == null) {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        if (severity == null) {
            if (other.severity != null) return false;
        } else if (!severity.equals(other.severity)) return false;
        if (text == null) {
            if (other.text != null) return false;
        } else if (!text.equals(other.text)) return false;
        if (time == null) {
            if (other.time != null) return false;
        } else if (!time.equals(other.time)) return false;
        if (timestamp == null) {
            if (other.timestamp != null) return false;
        } else if (!timestamp.equals(other.timestamp)) return false;
        if (toLibrary == null) {
            if (other.toLibrary != null) return false;
        } else if (!toLibrary.equals(other.toLibrary)) return false;
        if (toModule == null) {
            if (other.toModule != null) return false;
        } else if (!toModule.equals(other.toModule)) return false;
        if (toProcedure == null) {
            if (other.toProcedure != null) return false;
        } else if (!toProcedure.equals(other.toProcedure)) return false;
        if (toProgram == null) {
            if (other.toProgram != null) return false;
        } else if (!toProgram.equals(other.toProgram)) return false;
        if (toStatement == null) {
            if (other.toStatement != null) return false;
        } else if (!toStatement.equals(other.toStatement)) return false;
        if (type == null) {
            if (other.type != null) return false;
        } else if (!type.equals(other.type)) return false;
        return true;
    }
}
