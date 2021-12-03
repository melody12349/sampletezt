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

package com.sqlines.studio.model.filehandler;

import com.sqlines.studio.model.tabsdata.ObservableTabsData;
import com.sqlines.studio.model.tabsdata.listener.TabsChangeListener;
import com.sqlines.studio.model.filehandler.listener.RecentFilesChangeListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Updates the tab data if the data in the opened files have been changed.
 * <p>
 * Contains the list of recent files.
 *
 * @apiNote Use {@link Runnable#run()} to start file verification.
 *
 * @see RecentFilesChangeListener
 */
public class FileHandler implements Runnable, Serializable {
    private static final Logger logger = LogManager.getLogger(FileHandler.class);
    private static final long serialVersionUID = 646756239;

    private ObservableTabsData tabsData;
    private List<Long> sourceFilesLastModified = new ArrayList<>();
    private List<Long> targetFilesLastModified = new ArrayList<>();
    private List<File> recentFiles = new ArrayList<>();

    private List<RecentFilesChangeListener> recentFilesListeners = new ArrayList<>(5);

    /**
     * Starts file verification.
     * Updates the tab data if the data in the files have been changed.
     */
    @Override
    public void run() {
        while (true) {
            try {
                monitorFileChanged();
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                logger.error("run() - " + e.getMessage());
                break;
            }
        }
    }

    /**
     * Sets the underlying source of the tab data.
     *
     * @param tabsData source of tab data to set
     */
    public synchronized void setTabsData(@NotNull ObservableTabsData tabsData) {
        this.tabsData = tabsData;
        tabsData.addTabsListener(this::tabsDataChanged);
    }

    /**
     * Opens files and updates tabs data.
     * Adds all the files to the the recent files list. If a file is already is the list,
     * moves it to the top of the list.
     * <p>
     * Notifies all {@link RecentFilesChangeListener} listeners of the occurred change.
     *
     * @param files files to open
     *
     * @throws FileNotFoundException if the file does not exist,
     * is a directory rather than a regular file, or for some other
     * reason cannot be opened for reading.
     * @throws IOException if any IO error occurred
     * @throws SecurityException if a security manager exists and its checkRead
     * method denies read access to the file.
     */
    public synchronized void openSourceFiles(@NotNull List<File> files) throws IOException {
        files.removeIf(file -> file.isDirectory() || file.canExecute());

        for (File file : files) {
            int currIndex = tabsData.getCurrTabIndex();
            if (!tabsData.getSourceText(currIndex).isEmpty()) {
                tabsData.openTab(currIndex + 1);
                currIndex = tabsData.getCurrTabIndex();
            }

            try (FileInputStream stream = new FileInputStream(file.getAbsoluteFile())) {
                String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                tabsData.setSourceText(text, currIndex);
                tabsData.setSourceFilePath(file.getAbsolutePath(), currIndex);
                tabsData.setTabTitle(file.getName(), currIndex);
                sourceFilesLastModified.set(currIndex, file.lastModified());

                if (!recentFiles.contains(file)) {
                    recentFiles.add(file);

                    String path = file.getAbsolutePath();
                    int index = recentFiles.indexOf(file);
                    RecentFilesChangeListener.Change change = new RecentFilesChangeListener.Change(
                            RecentFilesChangeListener.Change.ChangeType.FILE_ADDED, path, index
                    );
                    recentFilesListeners.forEach(listener -> listener.onChange(change));
                } else {
                    int from = recentFiles.indexOf(file);
                    recentFiles.remove(file);
                    recentFiles.add(0, file);

                    String path = file.getAbsolutePath();
                    RecentFilesChangeListener.Change change = new RecentFilesChangeListener.Change(
                            RecentFilesChangeListener.Change.ChangeType.FILE_MOVED, path, from, 0
                    );
                    recentFilesListeners.forEach(listener -> listener.onChange(change));
                }
            }
        }
    }

    /**
     * Saves source data from the specified tab from {@link ObservableTabsData} to
     * the source file from the specified tab from {@link ObservableTabsData}.
     *
     * @param tabIndex the index of the tab with source data and source file path
     *
     * @throws IndexOutOfBoundsException – if the index is out of range
     * (tabIndex < 0 || tabIndex >= {@link ObservableTabsData#countTabs()})
     * @throws IllegalStateException if there is no source file opened
     * @throws FileNotFoundException if the file exists but is a directory rather than a regular file,
     * does not exist but cannot be created, or cannot be opened for any other reason
     * @throws IOException if any I/O error occurred
     * @throws SecurityException if a security manager exists and its checkWrite method
     * denies write access to the file
     */
    public synchronized void saveSourceFile(int tabIndex) throws IOException {
        String filePath = tabsData.getSourceFilePath(tabIndex);
        File file = new File(filePath);
        if (!file.exists()) {
            String errorMsg = "File does not exist: " + file.getAbsolutePath();
            throw new IllegalStateException(errorMsg);
        }

        try (FileOutputStream stream = new FileOutputStream(file.getAbsoluteFile())) {
            byte[] data = tabsData.getSourceText(tabIndex).getBytes();
            stream.write(data);
            tabsData.setTabTitle(file.getName(), tabIndex);
            sourceFilesLastModified.set(tabIndex, file.lastModified());
        }
    }

    /**
     * Saves target data from the specified tab from {@link ObservableTabsData} to
     * the target file from the specified tab from {@link ObservableTabsData}.
     *
     * @param tabIndex the index of the tab with target data and target file path
     *
     * @throws IndexOutOfBoundsException – if the index is out of range
     * (tabIndex < 0 || tabIndex >= {@link ObservableTabsData#countTabs()})
     * @throws IllegalStateException if there is no target file opened
     * @throws FileNotFoundException if the file exists but is a directory rather than a regular file,
     * does not exist but cannot be created, or cannot be opened for any other reason
     * @throws IOException if any I/O error occurred
     * @throws SecurityException if a security manager exists and its checkWrite method
     * denies write access to the file
     */
    public synchronized void saveTargetFile(int tabIndex) throws IOException {
        String filePath = tabsData.getTargetFilePath(tabIndex);
        File file = new File(filePath);
        if (!file.exists()) {
            String errorMsg = "File does not exist: " + file.getAbsolutePath();
            throw new IllegalStateException(errorMsg);
        }

        try (FileOutputStream stream = new FileOutputStream(file.getAbsoluteFile())) {
            byte[] data = tabsData.getTargetText(tabIndex).getBytes();
            stream.write(data);
            targetFilesLastModified.set(tabIndex, file.lastModified());
        }
    }

    /**
     * Creates a new file with the specified file path. Writes source data from the specified tab
     * from {@link ObservableTabsData} to this file.
     *
     * @param tabIndex the index of the tab with source data
     * @param path file path

     * @throws IndexOutOfBoundsException – if the index is out of range
     * (tabIndex < 0 || tabIndex >= {@link ObservableTabsData#countTabs()})
     * @throws FileNotFoundException if the file exists but is a directory rather than a regular file,
     * does not exist but cannot be created, or cannot be opened for any other reason
     * @throws IOException if some I/O error occurred
     * @throws SecurityException if a security manager exists and its checkWrite method
     * denies write access to the file
     */
    public synchronized void saveSourceFileAs(int tabIndex, @NotNull String path) throws IOException  {
        File file = new File(path);
        try (FileOutputStream stream = new FileOutputStream(file.getAbsoluteFile())) {
            byte[] data = tabsData.getSourceText(tabIndex).getBytes();
            stream.write(data);
            tabsData.setSourceFilePath(path, tabIndex);
            tabsData.setTabTitle(file.getName(), tabIndex);
            sourceFilesLastModified.set(tabIndex, file.lastModified());
        }
    }

    /**
     * Creates a new file with the specified file path. Writes target data from the specified tab
     * from {@link ObservableTabsData} to this file.
     *
     * @param tabIndex the index of the tab with target data
     * @param path file path

     * @throws IndexOutOfBoundsException – if the index is out of range
     * (tabIndex < 0 || tabIndex >= {@link ObservableTabsData#countTabs()})
     * @throws FileNotFoundException if the file exists but is a directory rather than a regular file,
     * does not exist but cannot be created, or cannot be opened for any other reason
     * @throws IOException if some I/O error occurred
     * @throws SecurityException if a security manager exists and its checkWrite method
     * denies write access to the file
     */
    public synchronized void saveTargetFileAs(int tabIndex, @NotNull String path) throws IOException  {
        File file = new File(path);
        try (FileOutputStream stream = new FileOutputStream(file.getAbsoluteFile())) {
            byte[] data = tabsData.getTargetText(tabIndex).getBytes();
            stream.write(data);
            tabsData.setTargetFilePath(path, tabIndex);
            targetFilesLastModified.set(tabIndex, file.lastModified());
        }
    }

    /**
     * @param index the index of the recent file to get
     *
     * @return the specified recent file
     */
    public @NotNull String getRecentFile(int index) {
        return recentFiles.get(index).getAbsolutePath();
    }

    /**
     * Removes all recent files from the list of recent files.
     */
    public void clearRecentFiles() {
        Stream<String> paths = recentFiles.stream().map(File::getAbsolutePath);
        recentFiles.clear();

        paths.forEach(path -> {
            RecentFilesChangeListener.Change change = new RecentFilesChangeListener.Change(
                    RecentFilesChangeListener.Change.ChangeType.FILE_REMOVED, path, 0
            );
            recentFilesListeners.forEach(listener -> listener.onChange(change));
        });
    }

    /**
     * @return the number of recent files in the list of recent files
     */
    public int countRecentFiles() {
        return recentFiles.size();
    }

    /**
     * Adds a listener which will be notified when the list of recent files changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    public void addRecentFileListener(@NotNull RecentFilesChangeListener listener) {
        recentFilesListeners.add(listener);
    }

    private synchronized void tabsDataChanged(@NotNull TabsChangeListener.Change change) {
        if (change.getChangeType() == TabsChangeListener.Change.ChangeType.TAB_ADDED) {
            int tabIndex = change.getTabIndex();
            addSourceFile(tabsData.getSourceFilePath(tabIndex), tabIndex);
            addTargetFile(tabsData.getTargetFilePath(tabIndex), tabIndex);
        } else if (change.getChangeType() == TabsChangeListener.Change.ChangeType.TAB_REMOVED) {
            int tabIndex = change.getTabIndex();
            removeSourceFile(tabIndex);
            removeTargetFile(tabIndex);
        }
    }

    private void addSourceFile(@NotNull String filePath, int index) {
        File file = new File(filePath);
        sourceFilesLastModified.add(index, file.lastModified());
    }

    private void addTargetFile(@NotNull String filePath, int index) {
        File file = new File(filePath);
        targetFilesLastModified.add(index, file.lastModified());
    }

    private void removeSourceFile(int index) {
        sourceFilesLastModified.remove(index);
    }

    private void removeTargetFile(int index) {
        targetFilesLastModified.remove(index);
    }

    private synchronized void monitorFileChanged() {
        for (int i = 0; i < sourceFilesLastModified.size(); i++) {
            String filePath = tabsData.getSourceFilePath(i);
            File file = new File(filePath);
            long lastModified = sourceFilesLastModified.get(i);

            if (!filePath.isEmpty() && !file.exists() // If the file was deleted
                    || file.lastModified() != lastModified) { // Or the file was modified
               updateSourceFile(i);
           }
        }

        for (int i = 0; i < targetFilesLastModified.size(); i++) {
            String filePath = tabsData.getTargetFilePath(i);
            File file = new File(filePath);
            long lastModified = targetFilesLastModified.get(i);

            if (!filePath.isEmpty() && !file.exists() // If the file was deleted
                    || file.lastModified() != lastModified) { // Or the file was modified
                updateTargetFile(i);
            }
        }
    }

    private void updateSourceFile(int index) {
        String filePath = tabsData.getSourceFilePath(index);
        File file = new File(filePath);

        // If the file was deleted
        if (!file.exists() && !filePath.isEmpty()) {
            tabsData.setSourceFilePath("", index);
            sourceFilesLastModified.set(index, 0L);
            return;
        }

        if (!file.exists()) {
            sourceFilesLastModified.set(index, 0L);
            return;
        }

        // If the file was modified
        try (FileInputStream stream = new FileInputStream(file.getAbsoluteFile())) {
            String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            tabsData.setSourceText(text, index);
            sourceFilesLastModified.set(index, file.lastModified());
        } catch (Exception e) {
            logger.error("updateSourceFile() - " + e.getMessage());
        }
    }

    private void updateTargetFile(int index) {
        String filePath = tabsData.getTargetFilePath(index);
        File file = new File(filePath);

        // If the file was deleted
        if (!file.exists() && !filePath.isEmpty()) {
            tabsData.setTargetFilePath("", index);
            targetFilesLastModified.set(index, 0L);
            return;
        }

        if (!file.exists()) {
            targetFilesLastModified.set(index, 0L);
            return;
        }

        // If the file was modified
        try (FileInputStream stream = new FileInputStream(file.getAbsoluteFile())) {
            String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            tabsData.setTargetText(text, index);
            targetFilesLastModified.set(index, file.lastModified());
        } catch (Exception e) {
            logger.error("updateTargetFile() - " + e.getMessage());
        }
    }

    private synchronized void readObject(@NotNull ObjectInputStream stream)
            throws ClassNotFoundException, IOException {
        sourceFilesLastModified = new ArrayList<>();
        targetFilesLastModified = new ArrayList<>();
        recentFiles = new ArrayList<>();
        recentFilesListeners = new ArrayList<>();

        int sourceFilesNumber = stream.readInt();
        for (int i = 0 ; i < sourceFilesNumber; i++) {
            Long data = (Long) stream.readObject();
            sourceFilesLastModified.add(data);
        }

        int targetFilesNumber = stream.readInt();
        for (int i = 0 ; i < targetFilesNumber; i++) {
            Long data = (Long) stream.readObject();
            targetFilesLastModified.add(data);
        }

        int recentFilesNumber = stream.readInt();
        for (int i = 0 ; i < recentFilesNumber; i++) {
            File data = (File) stream.readObject();
            recentFiles.add(data);
        }
    }

    private synchronized void writeObject(@NotNull ObjectOutputStream stream) throws IOException {
        stream.writeInt(sourceFilesLastModified.size());
        for (Long data : sourceFilesLastModified) {
            stream.writeObject(data);
        }

        stream.writeInt(targetFilesLastModified.size());
        for (Long data : targetFilesLastModified) {
            stream.writeObject(data);
        }

        stream.writeInt(recentFiles.size());
        for (File data : recentFiles) {
            stream.writeObject(data);
        }
    }
}
