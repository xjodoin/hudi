/*
 * Copyright (c) 2016 Uber Technologies, Inc. (hoodie-dev-group@uber.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uber.hoodie.cli.commands;

import com.uber.hoodie.avro.model.HoodieArchivedMetaEntry;
import com.uber.hoodie.cli.HoodieCLI;
import com.uber.hoodie.cli.HoodiePrintHelper;
import com.uber.hoodie.common.model.HoodieLogFile;
import com.uber.hoodie.common.table.HoodieTimeline;
import com.uber.hoodie.common.table.log.HoodieLogFormat;
import com.uber.hoodie.common.table.log.block.HoodieAvroDataBlock;
import com.uber.hoodie.common.util.FSUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

@Component
public class ArchivedCommitsCommand implements CommandMarker {

  @CliAvailabilityIndicator({"show archived commits"})
  public boolean isShowArchivedCommitAvailable() {
    return HoodieCLI.tableMetadata != null;
  }

  @CliCommand(value = "show archived commits", help = "Read commits from archived files and show details")
  public String showCommits(
      @CliOption(key = {
          "limit"}, mandatory = false, help = "Limit commits", unspecifiedDefaultValue = "10")
      final Integer limit) throws IOException {

    System.out
        .println("===============> Showing only " + limit + " archived commits <===============");
    String basePath = HoodieCLI.tableMetadata.getBasePath();
    FileStatus[] fsStatuses = FSUtils.getFs(basePath, HoodieCLI.conf)
        .globStatus(new Path(basePath + "/.hoodie/.commits_.archive*"));
    List<String[]> allCommits = new ArrayList<>();
    int commits = 0;
    for (FileStatus fs : fsStatuses) {
      //read the archived file
      HoodieLogFormat.Reader reader = HoodieLogFormat
          .newReader(FSUtils.getFs(basePath, HoodieCLI.conf),
          new HoodieLogFile(fs.getPath()), HoodieArchivedMetaEntry.getClassSchema());

      List<IndexedRecord> readRecords = new ArrayList<>();
      //read the avro blocks
      while (reader.hasNext()) {
        HoodieAvroDataBlock blk = (HoodieAvroDataBlock) reader.next();
        List<IndexedRecord> records = blk.getRecords();
        readRecords.addAll(records);
        if(commits == limit) {
          break;
        }
        commits++;
      }
      List<String[]> readCommits = readRecords.stream().map(r -> (GenericRecord) r)
          .map(r -> readCommit(r)).collect(Collectors.toList());
      allCommits.addAll(readCommits);
      if(commits == limit) {
        break;
      }
    }
    return HoodiePrintHelper.print(
        new String[]{"CommitTime", "CommitType", "CommitDetails"},
        allCommits.toArray(new String[allCommits.size()][]));
  }

  private String[] readCommit(GenericRecord record) {
    List<String> commitDetails = new ArrayList<>();
    try {
      switch (record.get("actionType").toString()) {
        case HoodieTimeline.CLEAN_ACTION: {
          commitDetails.add(record.get("commitTime").toString());
          commitDetails.add(record.get("actionType").toString());
          commitDetails.add(record.get("hoodieCleanMetadata").toString());
          break;
        }
        case HoodieTimeline.COMMIT_ACTION: {
          commitDetails.add(record.get("commitTime").toString());
          commitDetails.add(record.get("actionType").toString());
          commitDetails.add(record.get("hoodieCommitMetadata").toString());
          break;
        }
        case HoodieTimeline.DELTA_COMMIT_ACTION: {
          commitDetails.add(record.get("commitTime").toString());
          commitDetails.add(record.get("actionType").toString());
          commitDetails.add(record.get("hoodieCommitMetadata").toString());
          break;
        }
        case HoodieTimeline.ROLLBACK_ACTION: {
          commitDetails.add(record.get("commitTime").toString());
          commitDetails.add(record.get("actionType").toString());
          commitDetails.add(record.get("hoodieRollbackMetadata").toString());
          break;
        }
        case HoodieTimeline.SAVEPOINT_ACTION: {
          commitDetails.add(record.get("commitTime").toString());
          commitDetails.add(record.get("actionType").toString());
          commitDetails.add(record.get("hoodieSavePointMetadata").toString());
          break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return commitDetails.toArray(new String[commitDetails.size()]);
  }
}
