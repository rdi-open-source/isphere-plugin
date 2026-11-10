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
//  Wrapper (SYNCIFS - retrieve item attributes)
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
//          >>PARM<<  DBGVIEW(*LIST);
//          >>PARM<<  OPTIMIZE(*NONE);
//          >>PARM<<  OPTION(*EVENTF);
//          >>PARM<<  TGTRLS(&TR);
//        >>END-COMPILE<<
//        >>EXECUTE<<
//    >>END-PRE-COMPILER<<
// ------------------------------------------------------------------------------------------
ctl-opt bnddir('ISPHERE') main(W006006);
// ------------------------------------------------------------------------------------------

// Other External Prototypes
/include qcpysrc,syncifs

// Own Prototype
dcl-pr W006006 extpgm('W006006');
  i_path      like(ifsItem_t) const;
  o_itemAttrs likeds(itemAttrs_t);
end-pr;

// ------------------------------------------------------------------------------------------

dcl-proc W006006;
  dcl-pi *n;
     i_path      like(ifsItem_t) const;
     o_itemAttrs likeds(itemAttrs_t);
  end-pi;

  // ----------------------------------------------------------------------------------------

  // Call service program
  o_itemAttrs = SYNCIFS_retrieveItemAttributes(i_path);

end-proc;

