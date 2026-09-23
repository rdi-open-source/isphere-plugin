**free
// ------------------------------------------------------------------------------------------
//  IRPGUnit Tests - SYNCIFS
// ------------------------------------------------------------------------------------------
//
//  Copyright (c) 2012-2026 iSphere Project Owners
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Common Public License v1.0
//  which accompanies this distribution, and is available at
//  http://www.eclipse.org/legal/cpl-v10.html
//
// ------------------------------------------------------------------------------------------
//    >>PRE-COMPILER<<
//      >>CRTCMD<<  RUCRTRPG    TSTPGM(&LI/&OB) +
//                              SRCFILE(&SL/&SF) SRCMBR(&SM);
//      >>IMPORTANT<<
//        >>PARM<< BNDSRVPGM(&LI/SYNCIFS);
//        >>PARM<< COPTION(*SRCSTMT *EVENTF);
//        >>PARM<< DBGVIEW(*LIST);
//      >>END-IMPORTANT<<
//      >>EXECUTE<<
//    >>END-PRE-COMPILER<<
// ------------------------------------------------------------------------------------------
ctl-opt nomain option(*srcstmt: *nodebugio);

/include qinclude,TESTCASE
/include qcpysrc,syncifs

dcl-proc testGetParentPath_RootOnly export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-s parent varchar(256);

  parent = getParentPath('/');
  assertEqual('': parent);

end-proc;

dcl-proc testGetParentPath_DirectoryOnly export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-s parent varchar(256);

  parent = getParentPath('/parent/');
  assertEqual('/': parent);

end-proc;

dcl-proc testGetParentPath_DirectoryWithFile export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-s parent varchar(256);

  parent = getParentPath('/parent/name.txt');
  assertEqual('/parent': parent);

end-proc;

dcl-proc testGetParentPath_FileOnly export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-s parent varchar(256);

  parent = getParentPath('name.txt');
  assertEqual('': parent);

end-proc;

dcl-proc testJoinPath_RootWithFile export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-s path varchar(256);

  path = joinPath('/': 'name.txt');
  assertEqual('/name.txt': path);

end-proc;

dcl-proc testJoinPath_DirectoryWithFile export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-s path varchar(256);

  path = joinPath('/parent1/parent2/': 'name.txt');
  assertEqual('/parent1/parent2/name.txt': path);

end-proc;

dcl-proc testJoinPath_DirectoryNoSlashWithFile export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-s path varchar(256);

  path = joinPath('/parent1/parent2': 'name.txt');
  assertEqual('/parent1/parent2/name.txt': path);

end-proc;

dcl-proc testJoinPath_DirectoryWithFileCurrentDirectory export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-s path varchar(256);

  path = joinPath('/parent1/parent2/': './name.txt');
  assertEqual('/parent1/parent2/./name.txt': path);

end-proc;

dcl-proc testJoinPath_DirectoryWithFileParentDirectory export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-s path varchar(256);

  path = joinPath('/parent1/parent2/': '../name.txt');
  assertEqual('/parent1/parent2/../name.txt': path);

end-proc;

dcl-proc testNormalizePath_ReplaceBackslashes export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-s path varchar(256);

  path = normalizePath('\parent1\parent2\name.txt');
  assertEqual('/parent1/parent2/name.txt': path);

end-proc;

dcl-proc testNormalizePath_RemoveDoubleSlashes export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-s path varchar(256);

  path = normalizePath('/parent1//parent2//name.txt');
  assertEqual('/parent1/parent2/name.txt': path);

end-proc;

dcl-proc testNormalizePath_RemoveCurrentDirectory export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-s path varchar(256);

  path = normalizePath('/parent1//parent2//./');
  assertEqual('/parent1/parent2': path);

end-proc;

dcl-proc testNormalizePath_RemoveCurrentDirectoryEmbedded export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-s path varchar(256);

  path = normalizePath('/parent1//parent2//./file.txt');
  assertEqual('/parent1/parent2/file.txt': path);

end-proc;

dcl-proc testNormalizePath_WithPreviousDirectory export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-s path varchar(256);

  path = normalizePath('/parent1//parent2//../file.txt');
  assertEqual('/parent1/file.txt': path);

end-proc;

