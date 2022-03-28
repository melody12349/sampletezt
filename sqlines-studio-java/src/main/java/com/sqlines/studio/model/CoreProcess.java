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

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jetbrains.annotations.NotNull;

/**
 * SQLines command-line program.
 */
public class CoreProcess {
    private static final Logger logger = LogManager.getLogger(CoreProcess.class);
    private String output = "";

    /**
     * Starts a new process with 1 command-line argument - log file path.
     * Causes the current thread to wait, if necessary, until the process has terminated.
     * <p>
     * The program path is taken from the {@link java.util.Properties}.
     * Key - model.process-dir.
     *
     * @param logFilePath command-line argument to set. Log file path
     *
     * @throws IllegalStateException if the sqlines program was not found
     * @throws IOException if an I/O error occurs
     * @throws SecurityException if a security manager exists and its checkExec
     * method doesn't allow creation of the subprocess, or the standard input to the
     * subprocess was redirected from a file and the security manager's checkRead method
     * denies read access to the file, or the standard output or standard error of the
     * subprocess was redirected to a file and the security manager's checkWrite method
     * denies write access to the file
     */
    public void runAndWait(@NotNull String logFilePath) throws IOException {
        String[] args = { getProcessPath(),
                          "-log=" + logFilePath };
        runAndWait(args);
    }

    private @NotNull String getProcessPath() {
        String processPath = System.getProperty("model.app-dir");
        boolean osIsWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        if (osIsWindows) {
            processPath += "/sqlines.exe";
        } else {
            processPath += "/sqlines";
        }

        if (!new File(processPath).exists()) {
            throw new IllegalStateException("SQLines command-line program was not found:\n"
                    + processPath);
        }

        return processPath;
    }

    private void runAndWait(@NotNull String[] args) throws IOException {
        output = "";
        try {
            Process process = new ProcessBuilder(args).start();
            logger.info("Running SQLines command-line program: " + args);
            process.waitFor();
            logger.info("SQLines command-line program finished successfully");
            output = new String(process.getInputStream().readAllBytes());

        } catch (InterruptedException e) {
            logger.error("SQLines command-line program crashed: " + e.getMessage());
        }
    }

    /**
     * Starts a new process with the specified command-line arguments.
     * Causes the current thread to wait, if necessary, until the process has terminated.
     * <p>
     * The program path is taken from the {@link java.util.Properties}.
     * Key - model.process-dir.
     *
     * @param sourceMode command-line argument to set. Source conversion mode
     * @param targetMode command-line argument to set. Target conversion mode
     * @param sourceFilePath command-line argument to set. Source file path
     * @param targetFilePath command-line argument to set. Target file path
     * @param logFilePath command-line argument to set. Log file path
     *
     * @throws IllegalStateException if the sqlines program was not found
     * @throws IOException if an I/O error occurs
     * @throws SecurityException if a security manager exists and its checkExec
     * method doesn't allow creation of the subprocess, or the standard input to the
     * subprocess was redirected from a file and the security manager's checkRead method
     * denies read access to the file, or the standard output or standard error of the
     * subprocess was redirected to a file and the security manager's checkWrite method
     * denies write access to the file
     */
    public void runAndWait(@NotNull String sourceMode,
                           @NotNull String targetMode,
                           @NotNull String sourceFilePath,
                           @NotNull String targetFilePath,
                           @NotNull String logFilePath) throws IOException {
        String[] args = { getProcessPath(),
                          "-s = " + sourceMode,
                          "-t = " + targetMode,
                          "-in = " + sourceFilePath,
                          "-out = " + targetFilePath,
                          "-log = " + logFilePath };
        runAndWait(args);
    }

    /**
     * @return the process output string
     */
    public @NotNull String getOutput() {
        return output;
    }
}
