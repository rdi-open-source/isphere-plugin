**free
// ------------------------------------------------------------------------------------------
//  IRPGUnit Tests - SYNCMBR
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
//        >>PARM<< BNDSRVPGM(&LI/SYNCMBR);
//        >>PARM<< COPTION(*SRCSTMT *EVENTF);
//        >>PARM<< DBGVIEW(*LIST);
//      >>END-IMPORTANT<<
//      >>EXECUTE<<
//    >>END-PRE-COMPILER<<
// ------------------------------------------------------------------------------------------
ctl-opt nomain option(*srcstmt: *nodebugio);

/include qinclude,TESTCASE
/include qcpysrc,syncmbr

dcl-proc testParseMbrNameTypeFilters_Asterisk export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('*');
  assertEqual(1: filters.numE);
  assertEqual(*off: filters.entry(1).isNegated);
  assertEqual('*': filters.entry(1).name);
  assertEqual('*': filters.entry(1).srcType);

end-proc;

dcl-proc testParseMbrNameTypeFilters_AsteriskDotAsterisk export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('*.*');
  assertEqual(1: filters.numE);
  assertEqual('*': filters.entry(1).name);
  assertEqual('*': filters.entry(1).srcType);

end-proc;

dcl-proc testParseMbrNameTypeFilters_TypeOnly export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('.rpgle');
  assertEqual(1: filters.numE);
  assertEqual('*': filters.entry(1).name);
  assertEqual('RPGLE': filters.entry(1).srcType);

end-proc;

dcl-proc testParseMbrNameTypeFilters_NameOnly export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('demo*');
  assertEqual(1: filters.numE);
  assertEqual('DEMO*': filters.entry(1).name);
  assertEqual('*': filters.entry(1).srcType);

end-proc;

dcl-proc testParseMbrNameTypeFilters_TrailingDot export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('*.');
  assertEqual(1: filters.numE);
  assertEqual('*': filters.entry(1).name);
  assertEqual('*': filters.entry(1).srcType);

end-proc;

dcl-proc testParseMbrNameTypeFilters_LastDotSeparates export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('a.b.c');
  assertEqual(1: filters.numE);
  assertEqual('A.B': filters.entry(1).name);
  assertEqual('C': filters.entry(1).srcType);

  filters = parseMbrNameTypeFilters('*.*.*');
  assertEqual(1: filters.numE);
  assertEqual('*.*': filters.entry(1).name);
  assertEqual('*': filters.entry(1).srcType);

end-proc;

dcl-proc testParseMbrNameTypeFilters_Delimiters export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('demo1.rpgle,demo2.*;test*  abc');
  assertEqual(4: filters.numE);
  assertEqual('DEMO1': filters.entry(1).name);
  assertEqual('RPGLE': filters.entry(1).srcType);
  assertEqual('DEMO2': filters.entry(2).name);
  assertEqual('*': filters.entry(2).srcType);
  assertEqual('TEST*': filters.entry(3).name);
  assertEqual('*': filters.entry(3).srcType);
  assertEqual('ABC': filters.entry(4).name);
  assertEqual('*': filters.entry(4).srcType);

end-proc;

dcl-proc testParseMbrNameTypeFilters_Negated export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('*.rpgle |demo* test*');
  assertEqual(3: filters.numE);
  assertEqual(*off: filters.entry(1).isNegated);
  assertEqual('*': filters.entry(1).name);
  assertEqual('RPGLE': filters.entry(1).srcType);
  assertEqual(*on: filters.entry(2).isNegated);
  assertEqual('DEMO*': filters.entry(2).name);
  assertEqual('*': filters.entry(2).srcType);
  assertEqual(*on: filters.entry(3).isNegated);
  assertEqual('TEST*': filters.entry(3).name);
  assertEqual('*': filters.entry(3).srcType);

end-proc;

dcl-proc testParseMbrNameTypeFilters_PipeOnly export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('|');
  assertEqual(1: filters.numE);
  assertEqual(*on: filters.entry(1).isNegated);
  assertEqual('*': filters.entry(1).name);
  assertEqual('*': filters.entry(1).srcType);

end-proc;

dcl-proc testParseMbrNameTypeFilters_Empty export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('');
  assertEqual(0: filters.numE);

end-proc;

dcl-proc testParseMbrNameTypeFilters_WildcardsLongerThanTen export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('*a*b*c*d*e*.rpgle');
  assertEqual(1: filters.numE);
  assertEqual('*A*B*C*D*E*': filters.entry(1).name);
  assertEqual('RPGLE': filters.entry(1).srcType);

  filters = parseMbrNameTypeFilters('abcdefghij*.*sqlrpgle*');
  assertEqual(1: filters.numE);
  assertEqual('ABCDEFGHIJ*': filters.entry(1).name);
  assertEqual('*SQLRPGLE*': filters.entry(1).srcType);

end-proc;

dcl-proc testParseMbrNameTypeFilters_ConsecutiveAsterisks export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('ab**c***.**');
  assertEqual(1: filters.numE);
  assertEqual('AB*C*': filters.entry(1).name);
  assertEqual('*': filters.entry(1).srcType);

  filters = parseMbrNameTypeFilters('**********a*b*c*d*e*f*g*h*i*j**********');
  assertEqual(1: filters.numE);
  assertEqual('*A*B*C*D*E*F*G*H*I*J*': filters.entry(1).name);
  assertEqual('*': filters.entry(1).srcType);

end-proc;

dcl-proc testParseMbrNameTypeFilters_NameTooLong export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;
  dcl-ds msgInfo likeds(msgInfo_t) inz;

  monitor;
    filters = parseMbrNameTypeFilters('*.rpgle abcdefghijk.rpgle');
    fail('Must fail, because the name has more than 10 characters');
  on-error;
    msgInfo = getMonitoredMessage();
    aEqual('CPF9897': msgInfo.Id);
    aEqual('Invalid mask ''abcdefghijk.rpgle'': name or source type longer than 10 characters'
           : msgInfo.txt);
  endmon;

  monitor;
    filters = parseMbrNameTypeFilters('|???????????');
    fail('Must fail, because the name has more than 10 characters');
  on-error;
    msgInfo = getMonitoredMessage();
    aEqual('CPF9897': msgInfo.Id);
    aEqual('Invalid mask ''???????????'': name or source type longer than 10 characters'
           : msgInfo.txt);
  endmon;

end-proc;

dcl-proc testParseMbrNameTypeFilters_TypeTooLong export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;
  dcl-ds msgInfo likeds(msgInfo_t) inz;

  monitor;
    filters = parseMbrNameTypeFilters('demo.abcdefghijk');
    fail('Must fail, because the source type has more than 10 characters');
  on-error;
    msgInfo = getMonitoredMessage();
    aEqual('CPF9897': msgInfo.Id);
    aEqual('Invalid mask ''demo.abcdefghijk'': name or source type longer than 10 characters'
           : msgInfo.txt);
  endmon;

  monitor;
    filters = parseMbrNameTypeFilters('*a*b*c*d*e*f*g*h*i*j*k*l*m*n*o*p*q*r*s*t*.rpgle');
    fail('Must fail, because the name has more than 10 characters');
  on-error;
    msgInfo = getMonitoredMessage();
    aEqual('CPF9897': msgInfo.Id);
    aEqual('Invalid mask ''*a*b*c*d*e*f*g*h*i*j*k*l*m*n*o*p*q*r*s*t...'': '
           + 'name or source type longer than 10 characters'
           : msgInfo.txt);
  endmon;

end-proc;

dcl-proc testMatchesMbrNameTypeFilters_Asterisk export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('*');
  assert(matchesMbrNameTypeFilters('DEMO1.RPGLE': filters)
         : '''*'' must match ''DEMO1.RPGLE''');
  assert(matchesMbrNameTypeFilters('XYZ.': filters)
         : '''*'' must match ''XYZ.'' (member without source type)');

end-proc;

dcl-proc testMatchesMbrNameTypeFilters_AsteriskDotAsterisk export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('*.*');
  assert(matchesMbrNameTypeFilters('DEMO1.RPGLE': filters)
         : '''*.*'' must match ''DEMO1.RPGLE''');
  assert(matchesMbrNameTypeFilters('XYZ.': filters)
         : '''*.*'' must match ''XYZ.'' (member without source type)');

end-proc;

dcl-proc testMatchesMbrNameTypeFilters_TypeOnly export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('.rpgle');
  assert(matchesMbrNameTypeFilters('ABC.RPGLE': filters)
         : '''.rpgle'' must match ''ABC.RPGLE''');
  assert(not matchesMbrNameTypeFilters('DEMO2.CLLE': filters)
         : '''.rpgle'' must not match ''DEMO2.CLLE''');
  assert(not matchesMbrNameTypeFilters('RPGLE.TXT': filters)
         : '''.rpgle'' must not match ''RPGLE.TXT''');
  assert(not matchesMbrNameTypeFilters('ABC.SQLRPGLE': filters)
         : '''.rpgle'' must not match ''ABC.SQLRPGLE''');
  assert(not matchesMbrNameTypeFilters('XYZ.': filters)
         : '''.rpgle'' must not match ''XYZ.''');

end-proc;

dcl-proc testMatchesMbrNameTypeFilters_NameOnlyIgnoresCase export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('makefile');
  assert(matchesMbrNameTypeFilters('MAKEFILE.TXT': filters)
         : '''makefile'' must match ''MAKEFILE.TXT''');
  assert(matchesMbrNameTypeFilters('MAKEFILE.': filters)
         : '''makefile'' must match ''MAKEFILE.''');
  assert(not matchesMbrNameTypeFilters('MAKEFILE2.TXT': filters)
         : '''makefile'' must not match ''MAKEFILE2.TXT''');

  filters = parseMbrNameTypeFilters('rpgle');
  assert(matchesMbrNameTypeFilters('RPGLE.TXT': filters)
         : '''rpgle'' must match ''RPGLE.TXT''');
  assert(not matchesMbrNameTypeFilters('ABC.RPGLE': filters)
         : '''rpgle'' must not match ''ABC.RPGLE''');

end-proc;

dcl-proc testMatchesMbrNameTypeFilters_NegatedMasks export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('*.rpgle |demo* |test*');
  assert(matchesMbrNameTypeFilters('ABC.RPGLE': filters)
         : '''*.rpgle |demo* |test*'' must match ''ABC.RPGLE''');
  assert(not matchesMbrNameTypeFilters('DEMO1.RPGLE': filters)
         : '''*.rpgle |demo* |test*'' must not match ''DEMO1.RPGLE''');
  assert(not matchesMbrNameTypeFilters('TEST1.RPGLE': filters)
         : '''*.rpgle |demo* |test*'' must not match ''TEST1.RPGLE''');
  assert(not matchesMbrNameTypeFilters('RPGLE.TXT': filters)
         : '''*.rpgle |demo* |test*'' must not match ''RPGLE.TXT''');

end-proc;

dcl-proc testMatchesMbrNameTypeFilters_NegatedMasksOnly export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('|demo* |test*');
  assert(not matchesMbrNameTypeFilters('DEMO1.RPGLE': filters)
         : '''|demo* |test*'' must not match ''DEMO1.RPGLE''');
  assert(not matchesMbrNameTypeFilters('DEMO2.CLLE': filters)
         : '''|demo* |test*'' must not match ''DEMO2.CLLE''');
  assert(not matchesMbrNameTypeFilters('TEST1.RPGLE': filters)
         : '''|demo* |test*'' must not match ''TEST1.RPGLE''');
  assert(matchesMbrNameTypeFilters('ABC.RPGLE': filters)
         : '''|demo* |test*'' must match ''ABC.RPGLE''');
  assert(matchesMbrNameTypeFilters('RPGLE.TXT': filters)
         : '''|demo* |test*'' must match ''RPGLE.TXT''');
  assert(matchesMbrNameTypeFilters('XYZ.': filters)
         : '''|demo* |test*'' must match ''XYZ.''');

end-proc;

dcl-proc testMatchesMbrNameTypeFilters_LastDotSeparates export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('a.b.c');
  assert(matchesMbrNameTypeFilters('A.B.C': filters): '''a.b.c'' must match ''A.B.C''');
  assert(not matchesMbrNameTypeFilters('A.C': filters): '''a.b.c'' must not match ''A.C''');
  assert(not matchesMbrNameTypeFilters('A.B.CLLE': filters)
         : '''a.b.c'' must not match ''A.B.CLLE''');

  filters = parseMbrNameTypeFilters('*.*.*');
  assert(matchesMbrNameTypeFilters('A.B.C': filters): '''*.*.*'' must match ''A.B.C''');
  assert(not matchesMbrNameTypeFilters('ABC.RPGLE': filters)
         : '''*.*.*'' must not match ''ABC.RPGLE''');

end-proc;

dcl-proc testMatchesMbrNameTypeFilters_PipeOnly export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('|');
  assert(not matchesMbrNameTypeFilters('ABC.RPGLE': filters)
         : '''|'' must not match ''ABC.RPGLE''');
  assert(not matchesMbrNameTypeFilters('XYZ.': filters)
         : '''|'' must not match ''XYZ.''');

end-proc;

dcl-proc testMatchesMbrNameTypeFilters_Empty export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('');
  assert(matchesMbrNameTypeFilters('ABC.RPGLE': filters)
         : 'An empty filter must match ''ABC.RPGLE''');
  assert(matchesMbrNameTypeFilters('XYZ.': filters)
         : 'An empty filter must match ''XYZ.''');

end-proc;

dcl-proc testMatchesMbrNameTypeFilters_WildcardsLongerThanTen export;
  dcl-pi *n extproc(*dclcase);
  end-pi;

  dcl-ds filters likeds(mbrNameTypeFilters_t) inz;

  filters = parseMbrNameTypeFilters('*a*b*c*d*e*.rpgle');
  assert(matchesMbrNameTypeFilters('ABCDE.RPGLE': filters)
         : '''*a*b*c*d*e*.rpgle'' must match ''ABCDE.RPGLE''');
  assert(matchesMbrNameTypeFilters('ABCDEX.RPGLE': filters)
         : '''*a*b*c*d*e*.rpgle'' must match ''ABCDEX.RPGLE''');
  assert(matchesMbrNameTypeFilters('XAYBZCDE.RPGLE': filters)
         : '''*a*b*c*d*e*.rpgle'' must match ''XAYBZCDE.RPGLE''');
  assert(not matchesMbrNameTypeFilters('ABCD.RPGLE': filters)
         : '''*a*b*c*d*e*.rpgle'' must not match ''ABCD.RPGLE''');

  filters = parseMbrNameTypeFilters('abcdefghij*');
  assert(matchesMbrNameTypeFilters('ABCDEFGHIJ.RPGLE': filters)
         : '''abcdefghij*'' must match ''ABCDEFGHIJ.RPGLE''');
  assert(not matchesMbrNameTypeFilters('ABCDEFGHI.RPGLE': filters)
         : '''abcdefghij*'' must not match ''ABCDEFGHI.RPGLE''');

end-proc;
