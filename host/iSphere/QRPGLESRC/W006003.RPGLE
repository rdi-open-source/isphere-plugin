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
//  Wrapper (SYNCIFS - clear)
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
ctl-opt bnddir('ISPHERE') main(W006003);
// ------------------------------------------------------------------------------------------

// Other External Prototypes
/include qcpysrc,syncifs

// Own Prototype
dcl-pr W006003 extpgm('W006003');
  i_handle zoned(10: 0) const;
end-pr;

// ------------------------------------------------------------------------------------------

dcl-proc W006003;
  dcl-pi *n;
    i_handle zoned(10: 0) const;
  end-pi;

  // ----------------------------------------------------------------------------------------

  // Call service program
  SYNCIFS_clear(i_handle);

end-proc;

