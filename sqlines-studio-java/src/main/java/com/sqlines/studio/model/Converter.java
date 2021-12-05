/*
 * Copyright (c) 2021 SQLines
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sqlines.studio.model;

import com.sqlines.studio.model.tabsdata.ObservableTabsData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Allows you to start conversion.
 */
public class Converter {
    private static final Logger logger = LogManager.getLogger(Converter.class);

    private final ObservableTabsData tabsData;
    private final Map<String, String> cmdModes;
    private final CoreProcess coreProcess;

    /**
     *
     * @param tabsData source of tab data to set
     * @param cmdModes
     * @param coreProcess
     */
    public Converter(@NotNull ObservableTabsData tabsData,
                     @NotNull Map<String, String> cmdModes,
                     @NotNull CoreProcess coreProcess) {
        this.tabsData = tabsData;
        this.cmdModes = cmdModes;
        this.coreProcess = coreProcess;
    }

    /**
     * Runs conversion.
     * Takes data for the conversion from the specified tab. Updates data in the specified tab.
     *
     * @param tabIndex index of the tab with data to work with
     *
     * @throws IOException if any IO error occurred
     * @throws IllegalStateException if there is no conversion data
     * @throws IllegalStateException if the sqlines program was not found
     * @throws SecurityException if a security manager exists and its checkExec method
     * doesn't allow creation of the subprocess, or the standard input to the subprocess
     * was redirected from a file and the security manager's checkRead method denies read access
     * to the file, or the standard output or standard error of the subprocess was redirected to
     * a file and the security manager's checkWrite method denies write access to the file
     */
    public void run(int tabIndex) throws IOException {
        String sourcePath = null;
        String targetPath = null;
        String logPath = null;
        boolean tmpSourceCreated = false;
        try {
            String sourceMode = Objects.requireNonNull(cmdModes.get(tabsData.getSourceMode(tabIndex)));
            String targetMode = Objects.requireNonNull(cmdModes.get(tabsData.getTargetMode(tabIndex)));

            sourcePath = tabsData.getSourceFilePath(tabIndex);
            if (sourcePath.isEmpty()) {
                sourcePath = createSourceFile(tabIndex);
                tmpSourceCreated = true;
            }

            targetPath = createTargetFile(tabIndex);
            logPath = createLogFile();

            coreProcess.runAndWait(sourceMode, targetMode, sourcePath, targetPath, logPath);

            File file = new File(targetPath);
            try (FileInputStream stream = new FileInputStream(file)) {
                String data = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                tabsData.setTargetText(data, tabIndex);
                tabsData.setTargetFilePath(targetPath, tabIndex);
            }
        } catch (NullPointerException e) {
            deleteTargetFile(targetPath);
            String errorMsg = "No config file in application resources: " +
                    "source-modes.txt or target-modes.txt";
            throw new IllegalStateException(errorMsg);
        } catch (Exception e) {
            deleteTargetFile(targetPath);
            throw e;
        } finally {
            deleteLogFile(logPath);
            if (tmpSourceCreated) {
                deleteSourceFile(sourcePath);
            }
        }
    }

    private @NotNull String createSourceFile(int tabIndex) throws IOException {
        if (tabsData.getSourceText(tabIndex).isEmpty()) {
            throw new IllegalStateException("No conversion data");
        }

        File file;
        try {
            file = File.createTempFile(tabsData.getTabTitle(tabIndex), ".tmp");
        } catch (Exception e) {
            throw new IOException("Cannot create temp source file", e);
        }

        try (FileOutputStream stream = new FileOutputStream(file)) {
            stream.write(tabsData.getSourceText(tabIndex).getBytes());
        }

        return file.getAbsolutePath();
    }

    public void deleteSourceFile(String path) {
        if (path == null) {
            return;
        }

        try {
            File file = new File(path);
            boolean success = file.delete();
            if (!success) {
                logger.error("deleteSourceFile() - Cannot delete source file: " + path);
            }
        } catch (Exception e) {
            logger.error("deleteSourceFile() - Cannot delete source file: " + e.getMessage());
        }
    }

    private @NotNull String createTargetFile(int tabIndex) throws IOException {
        StringBuilder builder = new StringBuilder(System.getProperty("model.curr-dir"));
        builder.append("/");
        builder.append(tabsData.getTabTitle(tabIndex));
        builder.append(".");
        builder.append(cmdModes.get(tabsData.getTargetMode(tabIndex)));

        if (new File(builder.toString()).exists()) {
            throw new IOException("File already exists: " + builder);
        }

        File targetFile = new File(builder.toString());
        boolean success = targetFile.createNewFile();
        if (!success) {
            throw new IOException("Cannot create target file: " + targetFile.getAbsolutePath());
        }

        return targetFile.getAbsolutePath();
    }

    public void deleteTargetFile(String path) {
        if (path == null) {
            return;
        }

        try {
            File file = new File(path);
            boolean success = file.delete();
            if (!success) {
                logger.error("deleteTargetFile() - Cannot delete target file: " + path);
            }
        } catch (Exception e) {
            logger.error("deleteTargetFile() - Cannot delete target file: " + e.getMessage());
        }
    }

    private @NotNull String createLogFile() {
        String path = "";
        try {
            File file = File.createTempFile("sqlines-log", ".tmp");
            path = file.getAbsolutePath();
        } catch (Exception e) {
            logger.error("createLogFile() - Cannot create log file: " + e.getMessage());
        }

        return path;
    }

    public void deleteLogFile(String path) {
        if (path == null) {
            return;
        }

        try {
            File file = new File(path);
            boolean success = file.delete();
            if (!success) {
                logger.error("deleteLogFile() - Cannot delete log file: " + path);
            }
        } catch (Exception e) {
            logger.error("deleteLogFile() - Cannot delete log file: " + e.getMessage());
        }
    }
}
