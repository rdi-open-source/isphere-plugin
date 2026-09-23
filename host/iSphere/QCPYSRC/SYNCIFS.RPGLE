**free
// ------------------------------------------------------------------------------------------
//
//  Demo of iSphere Synchronize Source Member API, Prototypes and Type Definitions
//
// ------------------------------------------------------------------------------------------
//
//  Copyright (c) 2012-2026 iSphere Project Owners
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Common Public License v1.0
//  which accompanies this distribution, and is available at
//  http://www.eclipse.org/legal/cpl-v10.html
//
// ------------------------------------------------------------------------------------------

dcl-c MODE_LEFT '*LEFT';
dcl-c MODE_RIGHT '*RIGHT';

dcl-c LAST_CHANGED_NULL z'0001-01-01-00.00.00.000000';
dcl-c CHECKSUM_NULL 0;
dcl-c SRCTYPE_NULL '';

dcl-c TYPE_FILE 'F';
dcl-c TYPE_DIRECTORY 'D';

dcl-s ifsItem_t varchar(1024) template;

dcl-ds itemAttrs_t qualified template;
  name        like(ifsItem_t);
  type        char(1);
  lastChanged timestamp;
  checkSum    int(20);
end-ds;

dcl-pr SYNCIFS_getHandle zoned(10: 0) extproc(*dclcase);
end-pr;

dcl-pr SYNCIFS_resolveGenericFiles extproc(*dclcase);
  i_handle      zoned(10: 0) const;
  i_mode        char(10) const;
  i_itemsFilter char(1024) const;
  i_isRegEx     ind const;
  i_maxDepth    int(10);
end-pr;

dcl-pr SYNCIFS_getNumberOfCompareElements zoned(6: 0) extproc(*dclcase);
  i_handle zoned(10: 0) const;
  i_mode   char(10) const;
end-pr;

dcl-pr SYNCIFS_clear extproc(*dclcase);
  i_handle zoned(10: 0) const;
end-pr;

dcl-pr SYNCIFS_retrieveItemAttributes likeds(itemAttrs_t) extproc(*dclcase);
  i_path like(ifsItem_t) Options(*trim) const;
end-pr;

// ------------------------------------------------------------------------------------------
//   Exported only for iRPGUnit tests.
// ------------------------------------------------------------------------------------------

dcl-pr getParentPath like(ifsItem_t) extproc(*dclcase);
  i_path like(ifsItem_t) options(*trim) const;
end-pr;

dcl-pr normalizePath like(ifsItem_t) extproc(*dclcase);
  i_path like(ifsItem_t) options(*trim: *nopass) const;
end-pr;

dcl-pr joinPath like(ifsItem_t) extproc(*dclcase);
  i_directory like(ifsItem_t) options(*trim) const;
  i_fileName  like(ifsItem_t) options(*trim: *nopass) const;
end-pr;

