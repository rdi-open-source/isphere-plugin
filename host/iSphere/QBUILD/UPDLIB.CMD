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
     /* iSphere Update Library                                                */
     /*                                                                       */
     /* Changes the objects of the iSphere library, after the library has     */
     /* been restored with another name than ISPHERE.                         */
     /*                                                                       */
     /*-----------------------------------------------------------------------*/
     /* STRPREPRC Compile Options:                                            */
     /*   >>PRE-COMPILER<<                                                    */
     /*     >>CRTCMD<<    CRTCMD   CMD(&LI/&OB) +                             */
     /*                              SRCFILE(&SL/&SF) SRCMBR(&SM);            */
     /*       >>COMPILE<<                                                     */
     /*         >>PARM<<   PGM(*REXX);                                        */
     /*         >>PARM<<   REXSRCFILE(*LIBL/QBUILD);                          */
     /*         >>PARM<<   REXSRCMBR(UPDLIBCPP);                              */
     /*         >>PARM<<   REXCMDENV(*COMMAND);                               */
     /*         >>PARM<<   HLPPNLGRP(*LIBL/UPDLIBHLP);                        */
     /*         >>PARM<<   HLPID(UPDLIB);                                     */
     /*       >>END-COMPILE<<                                                 */
     /*       >>EXECUTE<<                                                     */
     /*   >>END-PRE-COMPILER<<                                                */
     /*-----------------------------------------------------------------------*/

             CMD        PROMPT('iSphere Update Library')

             PARM       KWD(LIB) TYPE(*NAME) LEN(10) MIN(1) +
                          PROMPT('Library')
