/*-----------------------------------------------------------------------*/
/*                                                                       */
/* Copyright (c) 2012-2026 iSphere Project Owners                        */
/* All rights reserved. This program and the accompanying materials      */
/* are made available under the terms of the Common Public License v1.0  */
/* which accompanies this distribution, and is available at              */
/* http://www.eclipse.org/legal/cpl-v10.html                             */
/*                                                                       */
/*-----------------------------------------------------------------------*/
/*                                                                       */
/* iSphere Update Library - Command processor (CPP)                      */
/*                                                                       */
/* Some commands store the qualified names of other objects of the       */
/* library, e.g. their command processing program. They are created in   */
/* library ISPHERE. This command changes them to the given library,      */
/* after the library has been restored with another name.                */
/*                                                                       */
/*-----------------------------------------------------------------------*/

/* Register error handler */
Signal on Error

/* Setup NOVALUE and SYNTAX condition traps */
Signal on NoValue
Signal on Syntax

PARSE ARG 'LIB('library')'

library = translate(strip(library))

if (library = "") then do
  "SNDPGMMSG MSGID(CPF9898) MSGF(QCPFMSG) ",
    "MSGDTA('Usage: UPDLIB LIB(library)') ",
    "TOPGMQ(*PRV (*CTLBDY)) MSGTYPE(*ESCAPE)"
end

/* STRPREPRC: First change the command, so that it runs the program of */
/* the library, then let STRPREPRC create its own command again from   */
/* its source, because CHGCMD cannot change the message file.          */
"CHGCMD CMD("library"/STRPREPRC) PGM("library"/STRPREPRC) ",
  "HLPPNLGRP("library"/STRPREPRCP) PRDLIB("library")"

""library"/STRPREPRC USESRCFILE("library"/QSTRPREPRC) ",
  "USESRCMBR(STRPREPRCE) LIB("library") OBJ(*DFT) ",
  "SRCLIB(*DFT) SRCFILE(*DFT) SRCMBR(*DFT)"

/* OPTION(*EVENTF) in the source of the command created file EVFEVENT */
Signal off Error
"DLTF FILE("library"/EVFEVENT)"
Signal on Error

/* BUILD and UPDLIB: REXX source and help panel group */
"CHGCMD CMD("library"/BUILD) PGM(*REXX) REXSRCFILE("library"/QBUILD) ",
  "HLPPNLGRP("library"/BUILD_HLP)"

"CHGCMD CMD("library"/UPDLIB) PGM(*REXX) REXSRCFILE("library"/QBUILD) ",
  "HLPPNLGRP("library"/UPDLIBHLP)"

/* Done */
"SNDPGMMSG MSGID(CPF9897) MSGF(QCPFMSG) ",
  "MSGDTA('Library "library" has been updated.') ",
  "TOPGMQ(*PRV (*CTLBDY)) MSGTYPE(*COMP)"

EXIT

/* ---------------------------------------------------------------------- */
/* Global error handler                                                   */
/* ---------------------------------------------------------------------- */
Error:
NoValue:
Syntax:
  "SNDPGMMSG MSGID(CPF9898) MSGF(QCPFMSG) ",
    "MSGDTA('Could not update library "library". See the job log.') ",
    "TOPGMQ(*PRV (*CTLBDY)) MSGTYPE(*ESCAPE)"
  EXIT
