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

dcl-proc testParseNameTypeFilters_Asterisk export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('*');
  assertEqual(1: filters.numE);
  assertEqual(*off: filters.entry(1).isNegated);
  assertEqual('*': filters.entry(1).name);
  assertEqual('*': filters.entry(1).type);

end-proc;

dcl-proc testParseNameTypeFilters_AsteriskDotAsterisk export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('*.*');
  assertEqual(1: filters.numE);
  assertEqual('*': filters.entry(1).name);
  assertEqual('*': filters.entry(1).type);

end-proc;

dcl-proc testParseNameTypeFilters_TypeOnly export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('.rpgle');
  assertEqual(1: filters.numE);
  assertEqual('*': filters.entry(1).name);
  assertEqual('RPGLE': filters.entry(1).type);

end-proc;

dcl-proc testParseNameTypeFilters_NameOnly export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('README');
  assertEqual(1: filters.numE);
  assertEqual('README': filters.entry(1).name);
  assertEqual('*': filters.entry(1).type);

end-proc;

dcl-proc testParseNameTypeFilters_TrailingDot export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('*.');
  assertEqual(1: filters.numE);
  assertEqual('*': filters.entry(1).name);
  assertEqual('*': filters.entry(1).type);

end-proc;

dcl-proc testParseNameTypeFilters_LastDotSeparates export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('a.b.c');
  assertEqual(1: filters.numE);
  assertEqual('A.B': filters.entry(1).name);
  assertEqual('C': filters.entry(1).type);

  filters = parseNameTypeFilters('*.*.*');
  assertEqual(1: filters.numE);
  assertEqual('*.*': filters.entry(1).name);
  assertEqual('*': filters.entry(1).type);

end-proc;

dcl-proc testParseNameTypeFilters_Delimiters export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('f0.txt,f1.*;f2  f3');
  assertEqual(4: filters.numE);
  assertEqual('F0': filters.entry(1).name);
  assertEqual('TXT': filters.entry(1).type);
  assertEqual('F1': filters.entry(2).name);
  assertEqual('*': filters.entry(2).type);
  assertEqual('F2': filters.entry(3).name);
  assertEqual('*': filters.entry(3).type);
  assertEqual('F3': filters.entry(4).name);
  assertEqual('*': filters.entry(4).type);

end-proc;

dcl-proc testParseNameTypeFilters_Negated export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('*.txt |f0.* f1.*');
  assertEqual(3: filters.numE);
  assertEqual(*off: filters.entry(1).isNegated);
  assertEqual('*': filters.entry(1).name);
  assertEqual('TXT': filters.entry(1).type);
  assertEqual(*on: filters.entry(2).isNegated);
  assertEqual('F0': filters.entry(2).name);
  assertEqual('*': filters.entry(2).type);
  assertEqual(*on: filters.entry(3).isNegated);
  assertEqual('F1': filters.entry(3).name);
  assertEqual('*': filters.entry(3).type);

end-proc;

dcl-proc testParseNameTypeFilters_PipeOnly export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('|');
  assertEqual(1: filters.numE);
  assertEqual(*on: filters.entry(1).isNegated);
  assertEqual('*': filters.entry(1).name);
  assertEqual('*': filters.entry(1).type);

end-proc;

dcl-proc testParseNameTypeFilters_Empty export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('');
  assertEqual(0: filters.numE);

end-proc;

dcl-proc testMatchesNameTypeFilters_Asterisk export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('*');
  assert(matchesNameTypeFilters('f0.txt': filters): '''*'' must match ''f0.txt''');
  assert(matchesNameTypeFilters('Makefile': filters): '''*'' must match ''Makefile''');
  assert(matchesNameTypeFilters('.hidden': filters): '''*'' must match ''.hidden''');
  assert(matchesNameTypeFilters('a.b.c.txt': filters): '''*'' must match ''a.b.c.txt''');

end-proc;

dcl-proc testMatchesNameTypeFilters_AsteriskDotAsterisk export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('*.*');
  assert(matchesNameTypeFilters('f0.txt': filters): '''*.*'' must match ''f0.txt''');
  assert(matchesNameTypeFilters('Makefile': filters)
         : '''*.*'' must match ''Makefile'' (file name without dot)');
  assert(matchesNameTypeFilters('.hidden': filters): '''*.*'' must match ''.hidden''');

end-proc;

dcl-proc testMatchesNameTypeFilters_TypeOnly export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('.txt');
  assert(matchesNameTypeFilters('f0.txt': filters): '''.txt'' must match ''f0.txt''');
  assert(matchesNameTypeFilters('UPPER.TXT': filters): '''.txt'' must match ''UPPER.TXT''');
  assert(matchesNameTypeFilters('a.b.c.txt': filters): '''.txt'' must match ''a.b.c.txt''');
  assert(not matchesNameTypeFilters('f0.rpgle': filters)
         : '''.txt'' must not match ''f0.rpgle''');
  assert(not matchesNameTypeFilters('Makefile': filters)
         : '''.txt'' must not match ''Makefile''');

  filters = parseNameTypeFilters('.rpgle');
  assert(matchesNameTypeFilters('demo.rpgle': filters)
         : '''.rpgle'' must match ''demo.rpgle''');
  assert(not matchesNameTypeFilters('demo.sqlrpgle': filters)
         : '''.rpgle'' must not match ''demo.sqlrpgle''');

end-proc;

dcl-proc testMatchesNameTypeFilters_NameOnlyIgnoresCase export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('makefile');
  assert(matchesNameTypeFilters('Makefile': filters)
         : '''makefile'' must match ''Makefile''');
  assert(matchesNameTypeFilters('MAKEFILE.txt': filters)
         : '''makefile'' must match ''MAKEFILE.txt''');
  assert(not matchesNameTypeFilters('Makefile2': filters)
         : '''makefile'' must not match ''Makefile2''');
  assert(not matchesNameTypeFilters('my.makefile': filters)
         : '''makefile'' must not match ''my.makefile''');

end-proc;

dcl-proc testMatchesNameTypeFilters_SingleCharWildcard export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('f?.txt');
  assert(matchesNameTypeFilters('f0.txt': filters): '''f?.txt'' must match ''f0.txt''');
  assert(not matchesNameTypeFilters('f10.txt': filters)
         : '''f?.txt'' must not match ''f10.txt''');

end-proc;

dcl-proc testMatchesNameTypeFilters_NegatedMasks export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('*.txt |f0.* |f1.*');
  assert(not matchesNameTypeFilters('f0.txt': filters)
         : '''*.txt |f0.* |f1.*'' must not match ''f0.txt''');
  assert(not matchesNameTypeFilters('f1.txt': filters)
         : '''*.txt |f0.* |f1.*'' must not match ''f1.txt''');
  assert(matchesNameTypeFilters('f2.txt': filters)
         : '''*.txt |f0.* |f1.*'' must match ''f2.txt''');
  assert(not matchesNameTypeFilters('f2.rpgle': filters)
         : '''*.txt |f0.* |f1.*'' must not match ''f2.rpgle''');

end-proc;

dcl-proc testMatchesNameTypeFilters_NegatedMasksOnly export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('|demo* |test*');
  assert(not matchesNameTypeFilters('demo1.rpgle': filters)
         : '''|demo* |test*'' must not match ''demo1.rpgle''');
  assert(not matchesNameTypeFilters('Test.txt': filters)
         : '''|demo* |test*'' must not match ''Test.txt''');
  assert(matchesNameTypeFilters('abc.rpgle': filters)
         : '''|demo* |test*'' must match ''abc.rpgle''');
  assert(matchesNameTypeFilters('Makefile': filters)
         : '''|demo* |test*'' must match ''Makefile''');

end-proc;

dcl-proc testMatchesNameTypeFilters_LastDotSeparates export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('a.b.c');
  assert(matchesNameTypeFilters('a.b.c': filters): '''a.b.c'' must match ''a.b.c''');
  assert(matchesNameTypeFilters('A.B.C': filters): '''a.b.c'' must match ''A.B.C''');
  assert(not matchesNameTypeFilters('a.b.c.txt': filters)
         : '''a.b.c'' must not match ''a.b.c.txt''');
  assert(not matchesNameTypeFilters('a.b': filters): '''a.b.c'' must not match ''a.b''');

  filters = parseNameTypeFilters('*.*.*');
  assert(matchesNameTypeFilters('a.b.c.txt': filters)
         : '''*.*.*'' must match ''a.b.c.txt''');
  assert(not matchesNameTypeFilters('f0.txt': filters)
         : '''*.*.*'' must not match ''f0.txt''');
  assert(not matchesNameTypeFilters('Makefile': filters)
         : '''*.*.*'' must not match ''Makefile''');

end-proc;

dcl-proc testMatchesNameTypeFilters_PipeOnly export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('|');
  assert(not matchesNameTypeFilters('f0.txt': filters): '''|'' must not match ''f0.txt''');
  assert(not matchesNameTypeFilters('Makefile': filters): '''|'' must not match ''Makefile''');

end-proc;

dcl-proc testMatchesNameTypeFilters_Empty export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(nameTypeFilters_t) inz;

  filters = parseNameTypeFilters('');
  assert(matchesNameTypeFilters('f0.txt': filters)
         : 'An empty filter must match ''f0.txt''');
  assert(matchesNameTypeFilters('Makefile': filters)
         : 'An empty filter must match ''Makefile''');

end-proc;

