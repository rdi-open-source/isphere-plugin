**free
// ------------------------------------------------------------------------------------------
//  Synchronize IFS Files API
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
//  Synchronize IFS Files API
//
// ------------------------------------------------------------------------------------------
//  Notes:
//   Mode is one of:
//     MODE_LEFT    Process IFS files of the left side of the comparison editor.
//     MODE_RIGHT   Process IFS files of the right side of the comparison editor.
// ------------------------------------------------------------------------------------------
//  Execution time:
//    Development IBM i: 13342 files in 4682 folders in 21 seconds.
// ------------------------------------------------------------------------------------------
//  STRPREPRC Compile Options:
//    >>PRE-COMPILER<<
//      >>CRTCMD<< CRTSQLRPGI   OBJ(&LI/&OB) SRCFILE(&SL/&SF) SRCMBR(&SM);
//        >>COMPILE<<
//          >>PARM<<  OBJTYPE(*MODULE);
//          >>PARM<<  COMMIT(*NONE);
//          >>PARM<<  TGTRLS(&TR);
//          >>PARM<<  CLOSQLCSR(*ENDMOD);
//          >>PARM<<  DBGVIEW(*NONE);
//          >>PARM<<  RPGPPOPT(*LVL2);
//          >>PARM<<  OPTION(*EVENTF);
//          --PARM<<  COMPILEOPT('OPTIMIZE(*FULL)');
//          >>PARM<<  COMPILEOPT('OPTIMIZE(*NONE) DBGVIEW(*LIST)');
//        >>END-COMPILE<<
//        >>EXECUTE<<
//    >>END-PRE-COMPILER<<
// ------------------------------------------------------------------------------------------
ctl-opt NoMain;
/include qcpysrc,h_spec
ctl-opt ccsid(*char: *jobrun);
/include qcpysrc,copyright

// ------------------------------------------------------------------------------------------

/include QCPYSRC,TYPES       // Common type definitions
/include QCPYSRC,STRING      // Procedure prototypes for modul 'STRING'
/include QCPYSRC,SYNCIFS     // Procedure prototypes for modul 'SYNCIFS'
/include QCPYSRC,NBRRNG      // Procedure prototypes for modul 'NBRRNG'
/include QCPYSRC,CRC32       // Procedure prototypes for modul 'CRC32'
/include QCPYSRC,IFSIO_H     // IFC C Header
/include QCPYSRC,REGEX       // Regular expression functions
/include QCPYSRC,ICONV       // Character translation functions
/include QCPYSRC,QUSRJOBI    // Procedure prototype for procedure 'QUSRJOBI'
/include QCPYSRC,LOCALE      // Locale functions
/include QCPYSRC,QMHSNDPM    // Send Program Message (QMHSNDPM) API
/include QCPYSRC,ERRC0100    // Format ERRC0100

// Global type definitions

dcl-s pattern_t varchar(1024) template;

dcl-ds hIconv_t qualified template;
  isInit    ind;
  job_ccsid int(10);
  nl_ccsid  int(10);
  iconv     likeds(iconv_t);
end-ds;

dcl-ds hRegex_t qualified template;
  isInit ind;
  regex  likeds(regex_t);
end-ds;

dcl-ds nameTypeFilters_t qualified template;
  numE  int(10);
  entry likeds(nameTypeFilter_t) dim(50);
end-ds;

dcl-ds nameTypeFilter_t qualified template;
  isNegated  ind;
  name like(ifsItem_t);
  type like(ifsItem_t);
end-ds;

dcl-ds statusUpdate_t qualified inz;
  time  timestamp;
  count int(10);
end-ds;

// Global values

dcl-ds g_hIconv likeds(hIconv_t) inz;

// ==========================================================================================
//  Get Handle
// ==========================================================================================

dcl-proc SYNCIFS_getHandle export;
  dcl-pi *n zoned(10: 0);
  end-pi;

  // Return value
  dcl-s handle zoned(10: 0);

  // ----------------------------------------------------------------------------------------

  // Set handle
  handle = NBRRNG_getNumber('HANDLE');

  // Write file 'Status'
  Exec SQL
    INSERT
      INTO
    SYNCIFSS
      (
       XSHDL,
       XSCNTFILE,
       XSCNTFND,
       XSCNL
      )
    VALUES
      (
       :handle,
       0,
       0,
       '*NO'
      );

  // Initialize character translation
  if (g_hIconv.isInit and g_hIconv.job_ccsid <> retrieveJobCcsid());
    freeIconv(g_hiconv);
  endif;

  if (not g_hIconv.isInit);
    g_hIconv = initIconv();
  endif;

  // Leave procedure
  return handle;

end-proc;

// ==========================================================================================
//  Resolves Generic Files.
// ==========================================================================================

dcl-proc SYNCIFS_resolveGenericFiles export;
  dcl-pi *n;
    i_handle     zoned(10: 0) const;
    i_mode       char(10) const;
    i_itemFilter char(1024) const;
    i_isRegEx    ind const;
    i_maxDepth   int(10);
  end-pi;

  // ----------------------------------------------------------------------------------------

  // Local field declarations
  dcl-ds SYNCIFSWRF extname('SYNCIFSW') qualified inz end-ds;
  dcl-s fileName varchar(2048);
  dcl-s dirPath varchar(2048);

  dcl-c FILTER_DELIMITER_CHARS ' ,;';

  dcl-s filterItem varchar(2048); // = name.type

  dcl-ds nameTypeFilters likeds(nameTypeFilters_t) inz;

  dcl-s parts like(ifsItem_t) dim(2);

  dcl-s pattern like(pattern_t);
  dcl-ds hRegex likeds(hRegex_t) inz;

  dcl-s i int(10);
  dcl-s isNegated ind;
  dcl-s maxItemID int(10);

  dcl-ds lastUpdateFilesResolved likeds(statusUpdate_t) inz;

  dcl-s countFilesResolved int(10);
  dcl-ds lastUpdateFound likeds(statusUpdate_t) inz;
  dcl-s type char(1);
  dcl-ds itemAttrs likeds(itemAttrs_t) inz;

  // ----------------------------------------------------------------------------------------

  if (i_isRegEx);
    if (%len(i_itemFilter) >= 2);
      pattern = %trimr(i_itemFilter);
      hRegex = initRegex(pattern);
    else;
      pattern = '';
      clear hRegex;
      hRegex.isInit = *off;
    endif;
  else;
    clear hRegex;
    hRegex.isInit = *off;
    nameTypeFilters.numE = 0;
    isNegated = *off;
    for-each filterItem in %split(i_itemFilter: FILTER_DELIMITER_CHARS);

      // Starting with a pipe (|) all following
      // entries are negated.
      if (startsWith('|': filterItem));
        if (%len(filterItem) >= 2);
          filterItem = %subst(filterItem: 2);
        else;
          filterItem = '';
        endif;
        isNegated = *on;
      endif;

      if (filterItem = '');
        filterItem = '*.*';
      endif;

      // First idea was to induvidially compare name and source type.
      // There the name and source type were stored separately.
      if (%len(%trim(filterItem)) > 0);
        parts = %split(filterItem: '.'); // = name.type
        if (parts(1) <> '' or parts(2) <> '' );
          nameTypeFilters.numE += 1;
          nameTypeFilters.entry(nameTypeFilters.numE).isNegated = isNegated;
          if (parts(1) <> '');
            nameTypeFilters.entry(nameTypeFilters.numE).name = %trim(ucase(parts(1)));
          else;
            nameTypeFilters.entry(nameTypeFilters.numE).name = '*';
          endif;
          if (parts(2) <> '');
            nameTypeFilters.entry(nameTypeFilters.numE).type = %trim(ucase(parts(2)));
          else;
            nameTypeFilters.entry(nameTypeFilters.numE).type = '*';
          endif;
        endif;
      endif;
    endfor;
  endif;

  type = TYPE_FILE;

  // Declare cursor
  Exec SQL
     DECLARE
        GENERIC_FILES
     CURSOR FOR
        SELECT
           *
        FROM
           SYNCIFSW
        WHERE
           XWHDL = :i_handle AND
           XWTYPE = :type AND
           (TRIM(XWITEM) LIKE '*%' OR TRIM(XWITEM) LIKE '%*'
            OR TRIM(XWITEM) LIKE '%*%' OR XWITEM = '*')
        FOR UPDATE;

  // Get maximum item ID
  maxItemID = getLastItemID(i_handle);

  // Open cursor
  Exec SQL
     OPEN
        GENERIC_FILES;

  clear lastUpdateFilesResolved;
  lastUpdateFilesResolved.time = *loval;

  clear lastUpdateFound;
  lastUpdateFound.time = *loval;

  dow (not isCanceled(i_handle));

    // Fetch from cursor
    Exec SQL
       FETCH NEXT FROM
          GENERIC_FILES
       INTO
          :SYNCIFSWRF;

    // Leave condition
    if (SQLCOD = 100 or SQLCOD < 0);
      leave;
    endif;

    // Set file name deping on the mode (left/right)
    select;
    when (i_mode = MODE_LEFT);
      fileName = normalizePath(SYNCIFSWRF.XWITEM);
    when (i_mode = MODE_RIGHT);
      fileName = normalizePath(SYNCIFSWRF.XWITEM);
    other;
      // TODO: incorrect mode
    endsl;

    i = %scanr('/': fileName);
    if (i > 0);
      dirPath = %subst(fileName: 1: i-1);
      fileName = %subst(fileName: i+1);
    else;
      dirPath = fileName;
      fileName = '*';
    endif;

    itemAttrs = SYNCIFS_retrieveItemAttributes(dirPath);

    type = TYPE_DIRECTORY;

    // Update generic input element
    Exec SQL
       UPDATE
          SYNCIFSW
       SET
          XWTYPE = :type,
          XWITEM = :dirPath,
          /* left item */
          XWLEFTCRC =  CASE WHEN :i_mode = :MODE_LEFT
                            THEN :itemAttrs.checksum
                            ELSE XWLEFTCRC
                            END,
          XWLEFTLCHG = CASE WHEN :i_mode = :MODE_LEFT
                            THEN :itemAttrs.lastChanged
                            ELSE XWLEFTLCHG
                            END,
          /* right item */
          XWRGHTCRC =  CASE WHEN :i_mode = :MODE_RIGHT
                            THEN :itemAttrs.checksum
                            ELSE XWRGHTCRC
                            END,
          XWRGHTLCHG = CASE WHEN :i_mode = :MODE_RIGHT
                            THEN :itemAttrs.lastChanged
                            ELSE XWRGHTLCHG
                            END
       WHERE
          CURRENT OF
             GENERIC_FILES;

    // Update each 1500 milliseconds
    countFilesResolved += 1;
    if (lastUpdateFilesResolved.time = *loval or
        %diff(%timestamp(): lastUpdateFilesResolved.time: *mseconds) > 1500);

      // Update file 'Status': Count Resolved Members
      updateStatusResolvedFiles(i_handle: lastUpdateFilesResolved: countFilesResolved);

      if (isCanceled(i_handle));
        leave;
      endif;

    endif;

    resolveGenericFiles(i_handle: i_mode: i_isRegEx: hRegEx: dirPath: fileName
                        : nameTypeFilters: lastUpdateFound: maxItemID: SYNCIFSWRF.XWID
                        : i_maxDepth);
    if (isCanceled(i_handle));
      leave;
    endif;

  enddo;

on-exit;

  // updateStatusResolvedFiles(i_handle: lastUpdateFilesResolved: -1);

  if (hRegex.isInit);
    freeRegex(hRegex);
  endif;

  // Close cursor
  Exec SQL
     CLOSE
        GENERIC_FILES;

end-proc;

// ==========================================================================================
//  Returns The Total Number Of Compare Members. (equals members found)
// ==========================================================================================

dcl-proc SYNCIFS_getNumberOfCompareElements export;
  dcl-pi *n zoned(6: 0);
    i_handle zoned(10: 0) const;
    i_mode   char(10) const;
  end-pi;

  // Local field declarations
  dcl-s numElems zoned(6: 0);

  // ----------------------------------------------------------------------------------------

  // Initialize 'Number of elements'
  numElems = 0;

  // Get number of elements
  select;
  when (i_mode = MODE_LEFT);
    Exec SQL
      SELECT
        XSCNTFND
      INTO
        :numElems
      FROM
        SYNCIFSS
      WHERE
        XSHDL = :i_handle;

  when (i_mode = MODE_RIGHT);
    Exec SQL
      SELECT
        XSCNTFND
      INTO
        :numElems
      FROM
        SYNCIFSS
      WHERE
        XSHDL = :i_handle;

  other;
    // TODO: incorrect mode
  endsl;

   return numElems;

end-proc;

// ==========================================================================================
//  Clear synchronisation data.
// ==========================================================================================

dcl-proc SYNCIFS_clear export;
  dcl-pi *n;
    i_handle zoned(10: 0) const;
  end-pi;

  // ----------------------------------------------------------------------------------------

  // Clear file 'Status'
  Exec SQL
    DELETE FROM
      SYNCIFSS
    WHERE
      XSHDL = :i_handle;

  // Clear file 'Work'
  Exec SQL
    DELETE FROM
      SYNCIFSW
    WHERE
      XWHDL = :i_handle;

end-proc;

// ==========================================================================================
//  Retrieve the attributes of a given member.
// ==========================================================================================

dcl-proc SYNCIFS_retrieveItemAttributes export;
  dcl-pi *n likeds(itemAttrs_t);
    i_path like(ifsItem_t) Options(*trim) const;
  end-pi;

  dcl-s path like(ifsItem_t);
  dcl-ds statAttrs likeds(statds) inz;
  dcl-s rc int(10);

  dcl-s errNbr int(10);
  dcl-s errText varchar(128);
  dcl-s objType like(statAttrs.st_objType);

  // Return value
  dcl-ds itemAttrs likeds(itemAttrs_t);

  // ----------------------------------------------------------------------------------------

  clear itemAttrs;

  rc = stat(i_path: %addr(statAttrs));
  if (rc = 0);

    objType = %str(%addr(statAttrs.st_objType): %size(objType));

    if (objType = '*STMF' or objType = '*DOC');
      itemAttrs.type = TYPE_FILE;
      itemAttrs.name = i_path;
      itemAttrs.checkSum = calculateChecksum(i_path);
    elseif (objType = '*DIR' or objType = '*FLR');
      itemAttrs.type = TYPE_DIRECTORY;
      itemAttrs.name = i_path;
      itemAttrs.checkSum = 0;
    else;
      kill('Unexpected type ' + objType + ' (' + path + ')');
    endif;

    itemAttrs.lastChanged = cvtEpochTimeToRpg(statAttrs.st_mtime);

  else;
    errNbr = errorNumber();
    errText = errorText(errNbr);
    logDiagMsg('Path: ' + path);
    kill(%proc() + ' (' + %char(errNbr) + '): ' + errText);
  endif;

  return itemAttrs;

end-proc;

// ==========================================================================================
//  Resolves the files of a given directory and file name mask.
// ==========================================================================================

dcl-proc resolveGenericFiles;
  dcl-pi *n extproc(*dclcase);
    i_handle           zoned(10: 0) const;
    i_mode             char(10) const;
    i_isRegEx          ind const;
    io_hRegEx          likeds(hRegex_t);
    i_dirPath          varchar(2048) const;
    i_fileName         varchar(2048) const;
    i_nameTypeFilters  likeds(nameTypeFilters_t) const;
    io_lastUpdateFound likeds(statusUpdate_t);
    io_maxItemID       int(10);
    i_parentID         int(10) const;
    i_maxDepth         int(10) const;
  end-pi;

  dcl-s pReadDir pointer;
  dcl-s pDirEnt pointer;
  dcl-ds dirEntry likeds(dirent) based(pDirEnt);

  dcl-s isFound ind;
  dcl-s fullFileName varchar(2048);
  dcl-s fileNameMask varchar(2048);
  dcl-ds itemAttrs likeds(itemAttrs_t) inz;
  dcl-s parentID like(i_parentID);

  dcl-s i int(10);
  dcl-s countFilesFound int(10);
  dcl-s subDirPath like(i_dirPath);

  // ----------------------------------------------------------------------------------------

  if (i_maxDepth > 0 and i_maxDepth < 1);
    return;
  endif;

  // Open directory
  pReadDir = opendir(i_dirPath);

  if (pReadDir <> *null);

    countFilesFound += 1;

    // Iterate files
    pDirEnt = readdir(pReadDir);
    dow (pDirEnt <> *null and not isCanceled(i_handle));

      fullFileName = %subst(dirEntry.d_name: 1: dirEntry.d_nameLen);

      if (fullFileName <> '.' and fullFileName <> '..');

        itemAttrs = SYNCIFS_retrieveItemAttributes(joinPath(i_dirPath: fullFileName));
        if (itemAttrs.type = TYPE_DIRECTORY);

          // Append record to work file.
          parentID = addDirectoryToWorkFile(i_handle: i_mode: io_maxItemID: i_parentId
                                 : fullFileName: itemAttrs);

          subDirPath = joinPath(i_dirPath: %subst(dirEntry.d_name: 1: dirEntry.d_namelen));

          resolveGenericFiles(i_handle: i_mode: i_isRegex: io_hRegex
                              : subDirPath: i_fileName: i_nameTypeFilters
                              : io_lastUpdateFound: io_maxItemID: parentID: i_maxDepth + 1);
        else;

          // Check non-negated patterns:
          isFound = *off;
          if (i_isRegEx);
            // Compare using regular expression
            isFound = matchRegex(fullFileName: io_hRegEx);
          else;
            // String compare
            if (i_nameTypeFilters.numE = 0);
              isFound = *on;
            else;
              for i = 1 to i_nameTypeFilters.numE;
                if (i_nameTypeFilters.entry(i).isNegated);
                  iter;
                endif;

                fileNameMask = i_nameTypeFilters.entry(i).name + '.'
                               + i_nameTypeFilters.entry(i).type;
                isFound = matches(fileNameMask : fullFileName: *on);
                if (isFound);
                  leave;
                endif;
              endfor;

              if (isFound);
                // Check negated patterns:
                for i = 1 to i_nameTypeFilters.numE;
                  if (not i_nameTypeFilters.entry(i).isNegated);
                    iter;
                  endif;

                  fileNameMask = i_nameTypeFilters.entry(i).name + '.'
                                 + i_nameTypeFilters.entry(i).type;
                  isFound = matches(fileNameMask : fullFileName: *on);
                  if (isFound);
                    leave;
                  endif;
                endfor;
              endif;
            endif;
          endif;

          if (isFound);

            countFilesFound += 1;

            // Append record to work file.
            addFileToWorkFile(i_handle: i_mode: io_maxItemID: i_parentId
                              : fullFileName: itemAttrs);
          endif;
        endif;

      endif;

      // Update each 1500 milliseconds
      if (io_lastUpdateFound.time = *loval or
          %diff(%timestamp(): io_lastUpdateFound.time: *mseconds) > 1500);

        // Update file 'Status': Count files Found
        updateStatusFilesFound(i_handle: io_lastUpdateFound: countFilesFound);

        if (isCanceled(i_handle));
          leave;
        endif;

      endif;

      pDirEnt = readdir(pReadDir);
    enddo;

    // Close directory
    closedir(pReadDir);
  endif;

  updateStatusFilesFound(i_handle: io_lastUpdateFound: countFilesFound);

end-proc;

// ==========================================================================================
//  Adds a DIRECTORY entry to the work file (SYNCIFSW).
// ==========================================================================================

dcl-proc addDirectoryToWorkFile;
  dcl-pi *n int(10) extproc(*dclcase);
    i_handle     zoned(10: 0) const;
    i_mode       char(10) const;
    io_maxItemID int(10);
    i_parentID   int(10) const;
    i_fileName   varchar(2048) const;
    i_itemAttrs  likeds(itemAttrs_t) const;
  end-pi;

  dcl-s type char(1);

  // ----------------------------------------------------------------------------------------

  io_maxItemID += 1;
  type = TYPE_DIRECTORY;

  select;
  when (i_mode = MODE_LEFT);
    Exec SQL
      INSERT INTO
        SYNCIFSW
          (
          XWHDL,
          XWID,
          XWPARENT,
          XWTYPE,
          XWITEM,
          XWLEFTLCHG,
          XWLEFTCRC
          )
      VALUES
        (
        :i_handle,
        :io_maxItemID,
        :i_parentID,
        :type,
        :i_fileName,              -- IFS File Name
        :i_itemAttrs.lastChanged, -- Left File Last Changed
        :i_itemAttrs.checkSum     -- Left File Checksum
        );

  when (i_mode = MODE_RIGHT);
    Exec SQL
      INSERT INTO
        SYNCIFSW
          (
          XWHDL,
          XWID,
          XWPARENT,
          XWTYPE,
          XWITEM,
          XWRGHTLCHG,
          XWRGHTCRC
          )
      VALUES
        (
        :i_handle,
        :io_maxItemID,
        :i_parentID,
        :type,
        :i_fileName,              -- IFS File Name
        :i_itemAttrs.lastChanged, -- Left File Last Changed
        :i_itemAttrs.checkSum     -- Left File Checksum
        );

  other;
    // TODO: incorrect mode
  endsl;

  return io_maxItemID;

end-proc;

// ==========================================================================================
//  Adds a FILE entry to the work file (SYNCIFSW).
// ==========================================================================================

dcl-proc addFileToWorkFile;
  dcl-pi *n extproc(*dclcase);
    i_handle     zoned(10: 0) const;
    i_mode       char(10) const;
    io_maxItemID int(10);
    i_parentID   int(10) const;
    i_fileName   varchar(2048) const;
    i_itemAttrs  likeds(itemAttrs_t) const;
  end-pi;

  dcl-s type char(1);

  // ----------------------------------------------------------------------------------------

  io_maxItemID += 1;
  type = TYPE_FILE;

  select;
  when (i_mode = MODE_LEFT);
    Exec SQL
      INSERT INTO
        SYNCIFSW
          (
          XWHDL,
          XWID,
          XWPARENT,
          XWTYPE,
          XWITEM,
          XWLEFTLCHG,
          XWLEFTCRC
          )
      VALUES
        (
        :i_handle,
        :io_maxItemID,
        :i_parentID,
        :type,
        :i_fileName,              -- IFS File Name
        :i_itemAttrs.lastChanged, -- Left File Last Changed
        :i_itemAttrs.checkSum     -- Left File Checksum
        );

  when (i_mode = MODE_RIGHT);
    Exec SQL
      INSERT INTO
        SYNCIFSW
          (
          XWHDL,
          XWID,
          XWPARENT,
          XWTYPE,
          XWITEM,
          XWRGHTLCHG,
          XWRGHTCRC
          )
      VALUES
        (
        :i_handle,
        :io_maxItemID,
        :i_parentID,
        :type,
        :i_fileName,              -- IFS File Name
        :i_itemAttrs.lastChanged, -- Left File Last Changed
        :i_itemAttrs.checkSum     -- Left File Checksum
        );

  other;
    // TODO: incorrect mode
  endsl;

end-proc;

// ==========================================================================================
//  Calculates the checksum of the member identified by a given path.
//  The line last changed date and the sequence numbers are not compared, because
//  the file is read from its IFS path. This way the date and sequence number columns
//  are dropped automatically.
// ==========================================================================================

dcl-proc calculateChecksum;
  dcl-pi *n like(crc32_checkSum_t) extproc(*dclcase);
    i_path like(ifsItem_t) Options(*trim) const;
  end-pi;

  // Return value
  dcl-s checkSum like(crc32_checkSum_t); //   inz(x'FFFFFFFF');

  // Local field declarations
  dcl-s hFile int(10) inz(-1);
  dcl-s opnFlags int(10);
  dcl-s pBuffer pointer;
  dcl-s buffer char(256) based(pBuffer); // only for debugging
  dcl-s size int(10);
  // dcl-s checkSumHex char(8);
  dcl-s totalSize int(10);

  dcl-s errNbr int(10);
  dcl-s errText varchar(128);

  dcl-c BUFFER_SIZE 16384;

  // ----------------------------------------------------------------------------------------

  opnFlags = %bitor(O_SHARE_RDWR: O_RDONLY);

  hFile = open(i_path: opnFlags);
  if (hFile <> -1);

    pBuffer = %alloc(BUFFER_SIZE);
    totalSize = 0;

    //checkSum = x'FFFFFFFF'; // Initialize result buffer
    checkSum = crc32_init();
    dou (size <= 0);
      size = read(hFile: pBuffer: BUFFER_SIZE);
      if (size > 0);
        checkSum = crc32_add(checkSum: pBuffer: size);
        totalSize = totalSize + size;
      endif;
    enddo;

    checkSum = crc32_finish(checkSum); // Finalize calculaion

  else;
    errNbr = errorNUmber();
    errText = errorText(errNbr);
    logDiagMsg('Path: ' + i_path);
    kill(%proc() + ' (' + %char(errNbr) + '): ' + errText);
    checkSum = CHECKSUM_NULL;
  endif;

  return checkSum;

on-exit;

  if (hFile <> -1);
    closef(hFile);
  endif;

  if (pBuffer <> *null);
    dealloc(n) pBuffer;
  endif;

end-proc;

// ==========================================================================================
//  Converts a given 13-byte API timestamp value to a RPG timestamp.
//  The format of the API timestamp is:
//    Byte 1-7 :   cyymmdd
//    Byte 8-13:   hhmmss
// ==========================================================================================

dcl-proc cvtEpochTimeToRpg;
  dcl-pi *n timestamp extproc(*dclcase);
    i_epochSec uns(10) const;
  end-pi;

  // Return value
  dcl-s rpgTimestamp timestamp;

  dcl-c EPOCH_START z'1970-01-01-00.00.00.000000';

  // ----------------------------------------------------------------------------------------

  rpgTimestamp = EPOCH_START + %seconds(i_epochSec);

  return rpgTimestamp;

end-proc;

// ==========================================================================================
//  Updates the number of resolved generic IFS files.
// ==========================================================================================

dcl-proc updateStatusResolvedFiles;
  dcl-pi *n extproc(*dclcase);
    i_handle              int(10)   const;
    io_lastUpdateResolved likeds(statusUpdate_t);
    io_count              int(10);
  end-pi;

  // ----------------------------------------------------------------------------------------

  io_lastUpdateResolved.count += io_count;
  io_lastUpdateResolved.time = %timestamp();

  // Update file 'Status': resolved members
  Exec SQL
    UPDATE
      SYNCIFSS
    SET
      XSCNTFILE = :io_lastUpdateResolved.count
    WHERE
      XSHDL = :i_handle;

  io_count = 0;

end-proc;

// ==========================================================================================
//  Updates the number of total IFS files found.
// ==========================================================================================

dcl-proc updateStatusFilesFound;
  dcl-pi *n extproc(*dclcase);
    i_handle           int(10) const;
    io_lastUpdateFound likeds(statusUpdate_t);
    io_count           int(10);
  end-pi;

  // ----------------------------------------------------------------------------------------

  io_lastUpdateFound.count += io_count;
  io_lastUpdateFound.time = %timestamp();

  // Update file 'Status': resolved members
  Exec SQL
    UPDATE
      SYNCIFSS
    SET
      XSCNTFND = :io_lastUpdateFound.count
    WHERE
      XSHDL = :i_handle;

  io_count = 0;

end-proc;

// ==========================================================================================
//  Check if process has to be canceled
// ==========================================================================================

dcl-proc isCanceled;
  dcl-pi *n ind extproc(*dclcase);
    i_handle int(10) const;
  end-pi;

  // Return value
  dcl-s isCanceled ind;

  // Local field declarations
  dcl-s cancel char(10);

  // ----------------------------------------------------------------------------------------

    cancel = '';

    Exec SQL
      SELECT
        XSCNL
      INTO
        :cancel
      FROM
         SYNCIFSS
      WHERE
        XSHDL = :i_handle;

    // Process has been canceled from RDi
    if (cancel = '*YES');
      isCanceled = *on;
    else;
      isCanceled = *off;
    endif;

    return isCanceled;

end-proc;

// ==========================================================================================
//  Retrieves the job ccsid.
// ==========================================================================================

dcl-proc retrieveJobCcsid;
  dcl-pi *n int(10) extproc(*dclcase);
  end-pi;

  // Return value
  dcl-s jobCcsid int(10);

  // Local field declarations
  dcl-ds qJob likeds(qJob_t) inz;
  dcl-ds jobi0400 likeds(jobi0400_t) inz;

  // ----------------------------------------------------------------------------------------

    clear qJob;
    qJob.name = '*';

    QUSRJOBI(jobi0400: %size(jobi0400): 'JOBI0400': qJob: '');

    jobCcsid = jobi0400.ccsid;

    return jobCcsid;

end-proc;

// ==========================================================================================
//  Initializes character translation.
// ==========================================================================================

dcl-proc initIconv;
  dcl-pi *n likeds(hIconv_t) extproc(*dclcase);
  end-pi;

  // Return value
  dcl-ds hIconv likeds(hIconv_t) inz;

  // Local field declarations
  dcl-ds from likeds(QtqCode_t) inz;
  dcl-ds to likeds(QtqCode_t) inz;

  dcl-c DIGITS const(%len(hIconv.nl_ccsid));
  dcl-c DECPOS const(%decpos(hIconv.nl_ccsid));

  // ----------------------------------------------------------------------------------------

    clear hIconv;
    hIconv.isInit = *off;

    hIconv.job_ccsid = retrieveJobCcsid();
    hIconv.nl_ccsid = %dec(%str(nl_langinfo(CODESET)): DIGITS: DECPOS);

    if (hIconv.job_ccsid = hIconv.nl_ccsid);
        return hIconv;
    endif;

    from.CCSID = hIconv.job_ccsid;
    from.ConvAlt = 0;    // Conversion alternative
    from.SubsAlt = 0;    // Substitution alternative
    from.ShiftAlt = 1;   // Shift-state alternative
    from.InpLenOp = 0;   // Input length option
    from.ErrorOpt = 1;   // Error option for mixed data
    from.Reserved = *ALLx'00';

    to.CCSID = hIconv.nl_ccsid;
    to.ConvAlt = 0;      // Conversion alternative
    to.SubsAlt = 0;      // Substitution alternative
    to.ShiftAlt = 1;     // Shift-state alternative
    to.InpLenOp = 0;     // Input length option
    to.ErrorOpt = 1;     // Error option for mixed data
    to.Reserved = *ALLx'00';

    hIconv.iconv = QtqIconvOpen(to: from);

    hIconv.isInit = *on;

    return hIconv;

end-proc;

// ==========================================================================================
//  Frees resources allcocated by the character translation service.
// ==========================================================================================

dcl-proc freeIconv;
  dcl-pi *n extproc(*dclcase);
    io_hIconv likeds(hIconv_t);
  end-pi;

  // ----------------------------------------------------------------------------------------

  if (not io_hIconv.isInit);
    return;
  endif;

  iconv_close(io_hIconv.iconv);

  clear io_hIconv;
  io_hIconv.isInit = *off;

end-proc;

// ==========================================================================================
//  Frees resources allcocated by the character translation service.
// ==========================================================================================

dcl-proc cvtCcsid;
  dcl-pi *n like(string_t) extproc(*dclcase);
    i_string like(string_t) value;
  end-pi;

  // Return value
  dcl-s string like(string_t) inz;

  dcl-s rc int(10);
  dcl-s bufferSize uns(10);
  dcl-s inBytesLeft uns(10);
  dcl-s outBytesLeft uns(10);
  dcl-s outBuffer char(512);
  dcl-s outLength int(10);
  dcl-s pInBuf pointer;
  dcl-s pOutBuf pointer;

  // ----------------------------------------------------------------------------------------

  if (not g_hIconv.isInit);
    return i_string;
  endif;

  bufferSize = %size(outBuffer);
  inBytesLeft = %len(%trimR(i_string));
  outBytesLeft = bufferSize;
  pInBuf = %addr(i_string: *data);
  pOutBuf= %addr(outBuffer);
  rc = iconv(g_hIconv.iconv: pInBuf: inBytesLeft: pOutBuf: outBytesLeft);
  if (rc <> 0);
    kill('cvtCcsid: iconv() returned error code ' + %char(rc));
  endif;

  outLength = bufferSize - outBytesLeft;

  string = %subst(outBuffer: 1: outLength);

  return string;

end-proc;

// ==========================================================================================
//  Compiles a regular expression pattern.
// ==========================================================================================

dcl-proc initRegex;
  dcl-pi *n likeds(hRegex_t) extproc(*dclcase);
    i_pattern like(string_t) const;
  end-pi;

  // Return value
  dcl-ds hRegex likeds(hRegex_t) inz;

  dcl-s rc int(10);
  dcl-s cflags int(10);
  dcl-s pattern like(i_pattern);

  // ----------------------------------------------------------------------------------------

  clear hRegex;

  pattern = cvtCcsid(i_pattern);

  cflags = REG_EXTENDED + REG_ICASE;
  rc = regcomp(hRegex.regex: pattern: cflags);
  if (rc <> 0);
    kill('initRegex: regcomp() returned error code ' + %char(rc));
  endif;

  hRegex.isInit = *on;

  return hRegex;

end-proc;

// ==========================================================================================
//  Frees all resources allocated by regular expressions.
// ==========================================================================================

dcl-proc freeRegex;
  dcl-pi *n extproc(*dclcase);
    io_hRegex likeds(hRegex_t);
  end-pi;

  // ----------------------------------------------------------------------------------------

  if (not io_hRegex.isInit);
    return;
  endif;

  regfree(io_hRegex.regex);

  clear io_hRegex;
  io_hRegex.isInit = *off;

end-proc;

// ==========================================================================================
//  Frees resources allcocated by the character translation service.
// ==========================================================================================

dcl-proc matchRegex;
  dcl-pi *n ind extproc(*dclcase);
    i_string  like(string_t) const;
    io_hRegex likeds(hRegex_t);
  end-pi;

  // Return value
  dcl-s isMatch ind;

  dcl-s rc int(10);
  dcl-s nmatch uns(10) inz(1);
  dcl-ds regmatch likeds(regmatch_t) inz;
  dcl-s error char(256);
  dcl-s string like(i_string);

  // ----------------------------------------------------------------------------------------

  string = cvtCcsid(i_string);
  rc = regexec(io_hRegex.regex: string: nmatch: regmatch: 0);

  select;
  when rc = 0;             // found!
    isMatch = *on;
  when rc = REG_NOMATCH;   // not found
    isMatch = *off;
  other;                   // error!
    regerror(rc: io_hRegex: %addr(error): %size(error));
    kill('matchRegex: regcomp() returned error code ' + %char(rc));
  endsl;

  return isMatch;

end-proc;

// ==========================================================================================
//  Returns the maximum assigned ID (XWID) of table SYNCIFSW.
// ==========================================================================================
dcl-proc getLastItemID;
  dcl-pi *n int(10) extproc(*dclcase);
    i_handle zoned(10: 0) const;
  end-pi;

  dcl-s maxID int(10);

  // ----------------------------------------------------------------------------------------

  // Fetch maximum ID
  Exec SQL
     SELECT
        COALESCE(MAX(XWID), 0)
     INTO
        :maxID
     FROM
        SYNCIFSW
     WHERE
        XWHDL = :i_handle;

     return maxID;

end-proc;

// ==========================================================================================
//  Normalizes a given path:
//   - replaces backslashes with slashes
//   - removes double slashes
//   - removes current directory parts './'
//   - resolves previous directory parts '../'
//   - removes a trailing slash (except for the root directory)
// ==========================================================================================
dcl-proc normalizePath export;
  dcl-pi *n like(ifsItem_t);
    i_path like(ifsItem_t) options(*trim: *nopass) const;
  end-pi;

  dcl-s UTF8_BACKSLASH varchar(1) ccsid(*utf8) inz(x'5C') static;
  dcl-s BACKSLASH varchar(1) static inz('');

  dcl-s path like(ifsItem_t);
  dcl-s segment like(ifsItem_t);
  dcl-s normalizedPath like(ifsItem_t);
  dcl-s start int(10);
  dcl-s pos int(10);

  // ----------------------------------------------------------------------------------------

  if (%parms() < %parmnum(i_path));
    return '';
  endif;

  if (BACKSLASH = '');
    BACKSLASH = UTF8_BACKSLASH;
  endif;

  path = %scanrpl(BACKSLASH: '/': i_path);
  if (path = '');
    return '';
  endif;

  // Absolute paths keep their leading slash
  if (%subst(path: 1: 1) = '/');
    normalizedPath = '/';
  else;
    normalizedPath = '';
  endif;

  // Process the path segment by segment
  start = 1;
  dow (start <= %len(path));

    pos = %scan('/': path: start);
    if (pos = 0);
      segment = %subst(path: start);
      start = %len(path) + 1;
    elseif (pos = start);
      segment = '';
      start = pos + 1;
    else;
      segment = %subst(path: start: pos - start);
      start = pos + 1;
    endif;

    select;
    when (segment = '' or segment = '.');
      // Empty segment (double or trailing slash) or current directory: ignore

    when (segment = '..');
      // Previous directory: remove the last segment, if possible
      if (normalizedPath = '/');
        // Root directory has no parent: ignore
      elseif (normalizedPath = '' or normalizedPath = '..' or endsWith('/..': normalizedPath));
        // Relative path leaving its base directory: keep '..'
        normalizedPath = appendSegment(normalizedPath: segment);
      else;
        pos = %scanr('/': normalizedPath);
        if (pos = 0);
          normalizedPath = '';
        elseif (pos = 1);
          normalizedPath = '/';
        else;
          normalizedPath = %subst(normalizedPath: 1: pos - 1);
        endif;
      endif;

    other;
      normalizedPath = appendSegment(normalizedPath: segment);
    endsl;

  enddo;

  return normalizedPath;

end-proc;

// ==========================================================================================
//  Appends a path segment to a given (already normalized) path.
// ==========================================================================================
dcl-proc appendSegment;
  dcl-pi *n like(ifsItem_t) extproc(*dclcase);
    i_path    like(ifsItem_t) const;
    i_segment like(ifsItem_t) const;
  end-pi;

  // ----------------------------------------------------------------------------------------

  if (i_path = '' or i_path = '/');
    return i_path + i_segment;
  endif;

  return i_path + '/' + i_segment;

end-proc;

// ==========================================================================================
//  Joins two path segments, eg. directory + fileName
// ==========================================================================================
dcl-proc joinPath export;
  dcl-pi *n like(ifsItem_t);
    i_directory like(ifsItem_t) options(*trim) const;
    i_fileName  like(ifsItem_t) options(*trim: *nopass) const;
  end-pi;

  dcl-s directory like(i_directory);
  dcl-s fileName like(i_fileName);
  dcl-s joinedPath like(ifsItem_t);

  // ----------------------------------------------------------------------------------------

  if (%parms() >= %parmnum(i_fileName));
    fileName = i_fileName;
  else;
    fileName = '';
  endif;

  directory = normalizePath(i_directory);

  if (endsWith('/': directory));
    joinedPath = directory + fileName;
  else;
    joinedPath = directory + '/' + fileName;
  endif;

  return joinedPath;

end-proc;

// ==========================================================================================
//  Returns the parent portion (directory) of a given IFS path.
// ==========================================================================================
dcl-proc getParentPath export;
  dcl-pi *n like(ifsItem_t);
    i_path like(ifsItem_t) options(*trim) const;
  end-pi;

  dcl-s parentPath like(i_path);

  dcl-s path like(i_path);
  dcl-s i int(10);

  // ----------------------------------------------------------------------------------------

  path = normalizePath(i_path);
  if (path = '' or path = '/');
    return '';
  endif;

  i = %scanr('/': path);
  if (i = 0);
    return '';
  endif;

  if (i = 1);
    return '/';
  endif;

  parentPath = %subst(path: 1: i-1);

  return parentPath;

end-proc;

// ==========================================================================================
//  Returns the error number of the C runtime.
// ==========================================================================================
dcl-proc errorNumber;
  dcl-pi *n int(10) extproc(*dclcase);
  end-pi;

  dcl-s pErrNbr pointer;
  dcl-s errNbr int(10) based(pErrNbr);

  // erno--Set Pointer to Runtime Error Code
  dcl-pr errno pointer extproc('__errno');
  end-pr;

  // ----------------------------------------------------------------------------------------

  pErrNbr = errno();

  return errNbr;

end-proc;

// ==========================================================================================
//  Returns the error text of a given C error number.
// ==========================================================================================
dcl-proc errorText;
  dcl-pi *n varchar(128) extproc(*dclcase);
    i_errorNumber int(10) const;
  end-pi;

  dcl-s errorText char(128);

  // strerror -- Set Pointer to Runtime Error Message
  dcl-pr strerror pointer extproc('strerror');
    i_errno        int(10) value;
  end-pr;

  // ----------------------------------------------------------------------------------------

  errorText = %str(strerror(i_errorNumber): %size(errorText));

  return %trim(errorText);

end-proc;

// ==========================================================================================
//  Sends a *DIAG message to the job log.
// ==========================================================================================
dcl-proc logDiagMsg;
  dcl-pi *n extproc(*dclcase);
    i_text varchar(256) value;
  end-pi;

  // Local fields
  dcl-s msgKey char(4);

  dcl-ds qMsgF likeds(qObj_t);

  // ------------------------------------------------------------------------------------------

  clear qMsgF;
  qMsgF.name = 'QCPFMSG';
  qMsgF.lib  = '*LIBL';

  clear ERRC0100;

  QMHSNDPM('CPF9897': qMsgF: i_text: %len(i_text): QMH_MT_DIAG
           : QMH_CSE_CTLBDY: 1: msgKey: ERRC0100);

end-proc;

// ==========================================================================================
//  Kills the program by sending an *ESCAPE message to the calling program.
// ==========================================================================================
dcl-proc kill;
  dcl-pi *n extproc(*dclcase);
    i_text varchar(128) value;
  end-pi;

  // Local fields
  dcl-s msgKey char(4);

  dcl-ds qMsgF likeds(qObj_t);

  // ------------------------------------------------------------------------------------------

  clear qMsgF;
  qMsgF.name = 'QCPFMSG';
  qMsgF.lib  = '*LIBL';

  clear ERRC0100;

  QMHSNDPM('CPF9897': qMsgF: i_text: %len(i_text): QMH_MT_ESCAPE
           : QMH_CSE_CTLBDY: 1: msgKey: ERRC0100);

end-proc;

