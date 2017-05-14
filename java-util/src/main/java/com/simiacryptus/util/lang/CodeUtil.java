/*
 * Copyright (c) 2017 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.util.lang;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CodeUtil {
  private static List<File> codeRoots = Arrays.asList(
      "src/main/java", "src/test/java", "src/main/scala", "src/test/scala"
  ).stream().map(x -> new File(x)).collect(Collectors.toList());
  
  public static File findFile(StackTraceElement callingFrame) {
    return findFile(callingFrame.getClassName(), callingFrame.getFileName());
  }
  
  public static File findFile(String className, String fileName) {
    String[] packagePath = className.split("\\.");
    String path = Arrays.stream(packagePath).limit(packagePath.length - 1).collect(Collectors.joining(File.separator)) + File.separator + fileName;
    return findFile(path);
  }
  
  public static String getIndent(String txt) {
    Matcher matcher = Pattern.compile("^\\s+").matcher(txt);
    return matcher.find() ? matcher.group(0) : "";
  }
  
  public static File findFile(String path) {
    for (File root : codeRoots) {
      File file = new File(root, path);
      if (file.exists()) return file;
    }
    throw new RuntimeException(String.format("Not Found: %s; Current Directory = %s", path, new File(".").getAbsolutePath()));
  }
  
  public static String getInnerText(StackTraceElement callingFrame) throws IOException {
    File file = findFile(callingFrame);
    assert (null != file);
    int start = callingFrame.getLineNumber() - 1;
    List<String> allLines = Files.readAllLines(file.toPath());
    String txt = allLines.get(start);
    String indent = getIndent(txt);
    ArrayList<String> lines = new ArrayList<>();
    for (int i = start + 1; i < allLines.size() && (getIndent(allLines.get(i)).length() > indent.length() || allLines.get(i).trim().isEmpty()); i++) {
      String line = allLines.get(i);
      lines.add(line.substring(Math.min(indent.length(), line.length())));
    }
    return lines.stream().collect(Collectors.joining("\n"));
  }
}