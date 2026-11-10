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
//  Wrapper (SYNCIFS - resolveGenericFiles)
//
// ------------------------------------------------------------------------------------------
//  STRPREPRC Compile Options:
//    >>PRE-COMPILER<<
//      >>CRTCMD<<    CRTBNDRPG  PGM(&LI/&OB) +
//                               SRCFILE(&SL/&SF) SRCMBR(&SM);
//        >>COMPILE<<
//          >>PARM<<  DFTACTGRP(*NO);
//          >>PARM<<  ACTGRP(ISPHERE);
//          >>PARM<<  BNDDIR(ISPHERE);
//          >>PARM<<  DBGVIEW(*NONE);
//          >>PARM<<  OPTIMIZE(*FULL);
//          >>PARM<<  OPTION(*EVENTF);
//          >>PARM<<  TGTRLS(&TR);
//        >>END-COMPILE<<
//        >>EXECUTE<<
//    >>END-PRE-COMPILER<<
// ------------------------------------------------------------------------------------------
ctl-opt bnddir('ISPHERE') main(W006007);
// ------------------------------------------------------------------------------------------

// Other External Prototypes
/include qcpysrc,syncifs

// Own Prototype
dcl-pr W006007 extpgm('W006007');
  i_handle     zoned(10: 0) const;
  i_mode       char(10) const;
  i_itemFilter char(1024) const;
  i_isRegEx    ind const;
  i_maxDepth   int(10);
end-pr;

// ------------------------------------------------------------------------------------------

dcl-proc W006007;
  dcl-pi *n;
    i_handle     zoned(10: 0) const;
    i_mode       char(10) const;
    i_itemFilter char(1024) const;
    i_isRegEx    ind const;
    i_maxDepth   int(10);
  end-pi;

  // ----------------------------------------------------------------------------------------

  // Call service program
  SYNCIFS_resolveGenericFiles(i_handle: i_mode: i_itemFilter: i_isRegEx: i_maxDepth);

end-proc;

