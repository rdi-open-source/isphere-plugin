**free
// ------------------------------------------------------------------------------------------
//
//  Copyright (c) 2012-2026 iSphere Project Owners
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Common Public License v1.0
//  which accompanies this distribution, and is available at
//  http://www.eclipse.org/legal/cpl-v10.html
//
// ------------------------------------------------------------------------------------------
//
//  Demo of iSphere Synchronize IFS Files API
//
// ------------------------------------------------------------------------------------------
//  STRPREPRC Compile Options:
//    >>PRE-COMPILER<<
//      >>CRTCMD<<    CRTBNDRPG  PGM(&LI/&OB) +
//                               SRCFILE(&SL/&SF) SRCMBR(&SM);
//        >>COMPILE<<
//          >>PARM<<  DFTACTGRP(*NO);
//          >>PARM<<  ACTGRP(*NEW);
//          >>PARM<<  DBGVIEW(*LIST);
//          >>PARM<<  OPTIMIZE(*NONE);
//          >>PARM<<  OPTION(*EVENTF);
//          >>PARM<<  TGTRLS(&TR);
//        >>END-COMPILE<<
//        >>EXECUTE<<
//    >>END-PRE-COMPILER<<
// ------------------------------------------------------------------------------------------

ctl-opt bnddir('ISPHERE') main(DEMO12);
/include qcpysrc,h_spec
// ------------------------------------------------------------------------------------------

// Work file
dcl-f SYNCIFSW disk(*ext) qualified keyed usage(*output) rename(SYNCIFSW: FILES) usropn;

// ------------------------------------------------------------------------------------------

/include QCPYSRC,SYNCIFS     // Procedure prototypes for modul 'FNDSTR'

// Own Prototype
dcl-pr DEMO12 extpgm('DEMO12');
  i_clearFiles   ind options(*nopass) const;
end-pr;

// Global type definitions

dcl-s handle_t zoned(10: 0) template;

// Global field declarations

dcl-s g_handle_left like(handle_t);
dcl-s g_handle_right like(handle_t);

dcl-s g_itemId int(10);

// ------------------------------------------------------------------------------------------

dcl-proc DEMO12;
  dcl-pi *n;
    i_clearFiles   ind options(*nopass) const;
  end-pi;

  // Optional parameters
  dcl-s clearFiles like(i_clearFiles);

  // ----------------------------------------------------------------------------------------

  if (%parms() >= %parmnum(i_clearFiles));
    clearFiles = i_clearFiles;
  else;
    clearFiles = *on;
  endif;

  // Simulate comparing members stored on different systems:
  // Read members on system A
  // g_handle_left = doCompare(MODE_LEFT: '\home\raddatz\isphere\demo*');
  g_handle_left = doCompare(MODE_LEFT: '\home\raddatz\*');

  // Read members on system B
  g_handle_right = doCompare(MODE_RIGHT: '\home\raddatz\isphere-DVP\demo*');

  // Now both results must be merged to finalize the compare.

  // Cleanup
  if (clearFiles);
    SYNCIFS_clear(g_handle_left);
    SYNCIFS_clear(g_handle_right);
  endif;

end-proc;

// ==========================================================================================
//  Appends a compare element to the work file.
// ==========================================================================================

dcl-proc doCompare;
  dcl-pi *n like(handle_t);
    i_mode     char(10) const;
    i_fileName varchar(1024) const;
  end-pi;

  // Return value
  dcl-s handle like(handle_t);

  dcl-s maxDepth int(10);
  dcl-s numElems int(10);
  dcl-s msg char(52);

  // ----------------------------------------------------------------------------------------

  g_itemId = 0;

  // Get handle
  handle = SYNCIFS_getHandle();

  // Build input data ...
  open SYNCIFSW;

  // ... add generic compare item
  if (i_mode = MODE_LEFT);
    appendLeftElement(handle: i_fileName);
  endif;

  // ... add generic compare item
  if (i_mode = MODE_RIGHT);
    appendRightElement(handle: i_fileName);
  endif;

  // ... finish setting start values
  close SYNCIFSW;

  // Resolve *generic* IFS files
  // maxDepth = 2; // 1 = selected directory, 2 = selected directory + direct subdirectories
  maxDepth = -1;  // current directory + all subdirectories
  // SYNCIFS_resolveGenericFiles(handle: i_mode: 'DEMO.{1,6}\.(PRT|DSP)F': *on: maxDepth);
  SYNCIFS_resolveGenericFiles(handle: i_mode: '': *off: maxDepth);

  // Get number of compare elements
  numElems = SYNCIFS_getNumberOfCompareElements(handle: i_mode);

  msg = 'Number of elements on system ' + %trim(i_mode) + ': ' + %char(numElems);
  dsply msg;

  return handle;

end-proc;

// ==========================================================================================
//  Appends a compare element to the work file.
// ==========================================================================================

dcl-proc appendLeftElement;
  dcl-pi *n;
    i_handle zoned(10: 0) const;
    i_fileName varchar(1024) Options(*trim) const;
  end-pi;

  // Locale field declarations
  dcl-ds data likerec(SYNCIFSW.FILES: *all);
  dcl-s type char(1);

  // ----------------------------------------------------------------------------------------

  g_itemId += 1;

  type = TYPE_FILE;

  clear data;
  data.XWHDL = i_handle;
  data.XWID = g_itemId;
  data.XWPARENT = 0;
  data.XWITEM = i_fileName;
  data.XWTYPE = type;
  data.XWLEFTLCHG = *loval;
  data.XWLEFTCRC = 0;

  write SYNCIFSW.FILES data;

end-proc;

// ==========================================================================================
//  Appends a compare element to the work file.
// ==========================================================================================

dcl-proc appendRightElement;
  dcl-pi *n;
    i_handle   zoned(10: 0) const;
    i_fileName varchar(1024) Options(*trim) const;
  end-pi;

  // Locale field declarations
  dcl-ds data likerec(SYNCIFSW.FILES: *all);
  dcl-s type char(1);

  // ----------------------------------------------------------------------------------------

  g_itemId += 1;

  type = TYPE_FILE;

  clear data;
  data.XWHDL = i_handle;
  data.XWID = g_itemId;
  data.XWPARENT = 0;
  data.XWITEM = i_fileName;
  data.XWTYPE = type;
  data.XWRGHTLCHG = *loval;
  data.XWRGHTCRC = 0;

  write SYNCIFSW.FILES data;

end-proc;

