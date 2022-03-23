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

package com.sqlines.studio.model.license;

import com.sqlines.studio.model.CoreProcess;
import com.sqlines.studio.model.license.listener.LicenseChangeListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Works with license file.
 *
 * @see LicenseChangeListener
 */
public class License implements Runnable {
    private static final Logger logger = LogManager.getLogger(License.class);

    private final CoreProcess coreProcess;
    private final List<LicenseChangeListener> licenseListeners = new ArrayList<>(5);
    private long lastModified;

    /**
     * Creates a new {@link License} with the specified sqlines command line program.
     *
     * @param coreProcess sqlines command line program
     */
    public License(@NotNull CoreProcess coreProcess) {
        this.coreProcess = coreProcess;
        try {
            File file = getLicenseFile();
            lastModified = file.lastModified();
        } catch (Exception e) {
            logger.error("License file not found: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                monitorLicenseFile();
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                logger.error("run() - " + e.getMessage());
                break;
            }
        }
    }

    /**
     * @return true if license is active, false otherwise
     *
     * @throws IllegalStateException if license file was not found
     * @throws IllegalStateException if sqlines command-line program was not found
     */
    public synchronized boolean isActive() {
        getLicenseFile(); // Check license file presence
        String logFilePath = createLogFile();
        try {
            coreProcess.runAndWait(logFilePath);
            deleteLogFile(logFilePath);
        } catch (IOException | SecurityException e) {
            return false;
        } finally {
            deleteLogFile(logFilePath);
        }

        String out = coreProcess.getOutput();
        return !out.contains("FOR EVALUATION USE ONLY");
    }

    /**
     * Updates license file and checks license status.
     * <p>
     * Notifies all {@link LicenseChangeListener} listeners of the license status change.
     *
     * @param regName registration name to set
     * @param regNumber registration number to set
     *
     * @throws IllegalArgumentException if registration data is invalid
     * @throws IllegalStateException if license file was not found
     * @throws IllegalStateException if sqlines command-line program was not found
     * @throws IOException if any IO error occurred
     * @throws SecurityException if a security manager exists and its checkWrite method
     * denies write access to the file
     */
    public synchronized void changeLicense(@NotNull String regName, @NotNull String regNumber)
            throws IOException {
        File licenseFile = getLicenseFile();
        try (FileOutputStream stream = new FileOutputStream(licenseFile)) {
            String info = "SQLines license file:\n" +
                    "\nRegistration Name: " + regName +
                    "\nRegistration Number: " + regNumber;
            stream.write(info.getBytes(StandardCharsets.UTF_8));

            if (isActive()) {
                licenseListeners.forEach(license -> license.changed(true));
            } else {
                licenseListeners.forEach(license -> license.changed(false));
                throw new IllegalArgumentException("Invalid registration data");
            }
        }
    }

    /**
     * Adds a listener which will be notified when the license status changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    public synchronized void addLicenseListener(@NotNull LicenseChangeListener listener) {
        licenseListeners.add(listener);
    }

    private @NotNull File getLicenseFile() {
        String path = System.getProperty("model.app-dir", "null") + "/license.txt";
        File licenseFile = new File(path);
        if (!licenseFile.exists()) {
            throw new IllegalStateException("File not found: " + path);
        }

        return licenseFile;
    }

    private synchronized void monitorLicenseFile() {
        File file = getLicenseFile();
        if (file.lastModified() != lastModified) {
            if (isActive()) {
                licenseListeners.forEach(license -> license.changed(true));
            } else {
                licenseListeners.forEach(license -> license.changed(false));
            }

            lastModified = file.lastModified();
        }
    }

    private @NotNull String createLogFile() {
        String path = "";
        try {
            File file = File.createTempFile("sqlines-log", ".tmp");
            logger.error("Log file created: " + file.getAbsolutePath());
            path = file.getAbsolutePath();
        } catch (Exception e) {
            logger.error("Cannot create log file: " + e.getMessage());
        }

        return path;
    }

    private void deleteLogFile(@NotNull String path) {
        try {
            File file = new File(path);
            boolean success = file.delete();
            if (!success) {
                logger.error("Cannot delete log file: " + path);
            }
        } catch (Exception e) {
            logger.error("Cannot delete log file: " + e.getMessage());
        }
    }
}
