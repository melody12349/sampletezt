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

package com.sqlines.studio.presenter;

import com.sqlines.studio.model.Converter;
import com.sqlines.studio.model.filehandler.FileHandler;
import com.sqlines.studio.model.filehandler.listener.RecentFilesChangeListener;
import com.sqlines.studio.model.tabsdata.ObservableTabsData;
import com.sqlines.studio.model.tabsdata.listener.*;
import com.sqlines.studio.view.mainwindow.MainWindowView;
import com.sqlines.studio.view.mainwindow.event.RecentFileEvent;
import com.sqlines.studio.view.mainwindow.event.TabCloseEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Responds to user actions in the main window.
 * Retrieves data from the model, and displays it in the main window.
 */
public class MainWindowPresenter {
    private static final Logger logger = LogManager.getLogger(MainWindowPresenter.class);

    private final ObservableTabsData tabsData;
    private final FileHandler fileHandler;
    private final Converter converter;
    private final MainWindowView view;

    private final TabsChangeListener modelTabsListener = this::modelTabsChanged;
    private final TabIndexChangeListener modelIndexListener = this::modelTabIndexChanged;
    private final TabTitleChangeListener modelTabTitleListener = this::modelTabTileChanged;
    private final ModeChangeListener modelSourceModeListener = this::modelSourceModeChanged;
    private final ModeChangeListener modelTargetModeListener = this::modelTargetModeChanged;
    private final TextChangeListener modelSourceTextListener = this::modelSourceTextChanged;
    private final TextChangeListener modelTargetTextListener = this::modelTargetTextChanged;

    private final ChangeListener<Number> viewTabIndexListener = this::viewTabIndexChanged;
    private final com.sqlines.studio.view.mainwindow.listener.TabTitleChangeListener viewTabTitleListener;
    private final com.sqlines.studio.view.mainwindow.listener.ModeChangeListener viewSourceModeListener;
    private final com.sqlines.studio.view.mainwindow.listener.ModeChangeListener viewTargetModeListener;
    private final com.sqlines.studio.view.mainwindow.listener.TextChangeListener viewSourceTextListener;
    private final com.sqlines.studio.view.mainwindow.listener.TextChangeListener viewTargetTextListener;

    public MainWindowPresenter(@NotNull ObservableTabsData tabsData,
                               @NotNull FileHandler fileHandler,
                               @NotNull Converter converter,
                               @NotNull MainWindowView view) {
        this.tabsData = tabsData;
        this.fileHandler = fileHandler;
        this.converter = converter;
        this.view = view;

        viewTabTitleListener = this::viewTabTitleChanged;
        viewSourceModeListener = this::viewSourceModeChanged;
        viewTargetModeListener = this::viewTargetModeChanged;
        viewSourceTextListener = this::viewSourceTextChanged;
        viewTargetTextListener = this::viewTargetTextChanged;

        tabsData.addTabsListener(modelTabsListener);
        tabsData.addTabIndexListener(modelIndexListener);
        tabsData.addTabTitleListener(modelTabTitleListener);
        tabsData.addSourceModeListener(modelSourceModeListener);
        tabsData.addTargetModeListener(modelTargetModeListener);
        tabsData.addSourceTextListener(modelSourceTextListener);
        tabsData.addTargetTextListener(modelTargetTextListener);
        tabsData.addSourceFilePathListener(this::modelSourcePathChanged);
        tabsData.addTargetFilePathListener(this::modelTargetPathChanged);

        fileHandler.addRecentFileListener(this::modelRecentFilesChanged);

        view.addTabSelectionListener(viewTabIndexListener);
        view.addTabTitleListener(viewTabTitleListener);
        view.addSourceModeListener(viewSourceModeListener);
        view.addTargetModeListener(viewTargetModeListener);
        view.addSourceTextListener(viewSourceTextListener);
        view.addTargetTextListener(viewTargetTextListener);
        view.addFocusListener(this::viewFocusChanged);
        view.setOnDragAction(this::receiveDrag);
        view.setOnDropAction(this::receiveDrop);
        view.setOnNewTabAction(event -> openTabPressed());
        view.setOnCloseTabAction(this::closeTabPressed);
        view.setOnOpenFileAction(event -> openFilePressed());
        view.setOnRecentFileAction(this::openRecentFilePressed);
        view.setOnClearRecentAction(event -> clearRecentFilesPressed());
        view.setOnSaveFileAction(event -> saveFilePressed());
        view.setOnSaveAsAction(event -> saveFileAsPressed());
        view.setOnRunAction(event -> runConversionPressed());
        view.setOnOnlineHelpAction(event -> openOnlineHelpPressed());
        view.setOnOpenSiteAction(event -> openSitePressed());

        initView();
        view.show();
    }

    private void initView() {
        try {
            int tabsNumber = tabsData.countTabs();
            if (tabsNumber == 0) {
                openTabPressed();
                return;
            }

            int currIndex = tabsData.getCurrTabIndex();
            for (int i = 0; i < tabsNumber; i++) {
                String title = tabsData.getTabTitle(i);
                String sourceMode = tabsData.getSourceMode(i);
                String targetMode = tabsData.getTargetMode(i);

                view.openTab(i);
                tabsData.setTabTitle(title, i);
                tabsData.setSourceMode(sourceMode, i);
                tabsData.setTargetMode(targetMode, i);
                modelSourceTextChanged(tabsData.getSourceText(i), i);
                modelTargetTextChanged(tabsData.getTargetText(i), i);
            }

            tabsData.setCurrTabIndex(currIndex);

            for (int i = 0; i < fileHandler.countRecentFiles(); i++) {
                view.addRecentFile(fileHandler.getRecentFile(i));
            }

            logger.info("Last state loaded");
        } catch (Exception e) {
            logger.error("Loading last state failed: " + e.getMessage());

            tabsData.removeAllTabs();
            view.closeAllTabs();
            openTabPressed();
            fileHandler.clearRecentFiles();
            view.clearRecentFiles();
        }
    }

    private void modelTabsChanged(@NotNull TabsChangeListener.Change change) {
        if (change.getChangeType() == TabsChangeListener.Change.ChangeType.TAB_ADDED) {
            view.openTab(change.getTabIndex());
            tabsData.setCurrTabIndex(change.getTabIndex());
        } else if (change.getChangeType() == TabsChangeListener.Change.ChangeType.TAB_REMOVED) {
            view.closeTab(change.getTabIndex());
        }
    }

    private void modelTabIndexChanged(int newIndex) {
        Platform.runLater(() -> {
            view.removeTabSelectionListener(viewTabIndexListener);
            view.setCurrTabIndex(newIndex);
            view.addTabSelectionListener(viewTabIndexListener);

            view.removeSourceModeListener(viewSourceModeListener);
            view.setSourceMode(tabsData.getSourceMode(newIndex));
            view.addSourceModeListener(viewSourceModeListener);

            view.removeTargetModeListener(viewTargetModeListener);
            view.setTargetMode(tabsData.getTargetMode(newIndex));
            view.addTargetModeListener(viewTargetModeListener);

            MainWindowView.FieldInFocus inFocus = view.getFieldInFocus(newIndex);
            if (inFocus == MainWindowView.FieldInFocus.SOURCE) {
                view.showFilePath(tabsData.getSourceFilePath(newIndex));
            } else if (inFocus == MainWindowView.FieldInFocus.TARGET) {
                view.showFilePath(tabsData.getTargetFilePath(newIndex));
            }
        });
    }

    private void modelTabTileChanged(@NotNull String newTitle, int tabIndex) {
        Platform.runLater(() -> {
            view.removeTabTitleListener(viewTabTitleListener);
            view.setTabTitle(newTitle, tabIndex);
            view.addTabTitleListener(viewTabTitleListener);
        });
    }

    private void modelSourceModeChanged(@NotNull String newMode, int tabIndex) {
        Platform.runLater(() -> {
            view.removeSourceModeListener(viewSourceModeListener);
            view.setSourceMode(newMode);
            view.addSourceModeListener(viewSourceModeListener);
        });
    }

    private void modelTargetModeChanged(@NotNull String newMode, int tabIndex) {
        Platform.runLater(() -> {
            view.removeTargetModeListener(viewTargetModeListener);
            view.setTargetMode(newMode);
            view.addTargetModeListener(viewTargetModeListener);
        });
    }

    private void modelSourceTextChanged(@NotNull String newText, int tabIndex) {
        Platform.runLater(() -> {
            view.removeSourceTextListener(viewSourceTextListener);
            view.setSourceText(newText, tabIndex);
            view.addSourceTextListener(viewSourceTextListener);
        });
    }

    private void modelTargetTextChanged(@NotNull String newText, int tabIndex) {
        Platform.runLater(() -> {
            view.removeTargetTextListener(viewTargetTextListener);
            view.setTargetText(newText, tabIndex);
            view.addTargetTextListener(viewTargetTextListener);
        });
    }

    private void modelSourcePathChanged(@NotNull String newPath, int tabIndex) {
        if (tabIndex == tabsData.getCurrTabIndex()) {
            MainWindowView.FieldInFocus inFocus = view.getFieldInFocus(tabIndex);
            if (inFocus == MainWindowView.FieldInFocus.SOURCE
                    || inFocus == MainWindowView.FieldInFocus.NONE) {
                Platform.runLater(() -> view.showFilePath(newPath));
            }
        }
    }

    private void modelTargetPathChanged(@NotNull String newPath, int tabIndex) {
        if (tabIndex == tabsData.getCurrTabIndex()) {
            MainWindowView.FieldInFocus inFocus = view.getFieldInFocus(tabIndex);
            if (inFocus == MainWindowView.FieldInFocus.TARGET
                    || inFocus == MainWindowView.FieldInFocus.NONE) {
                Platform.runLater(() -> view.showFilePath(newPath));
            }
        }
    }

    private void modelRecentFilesChanged(@NotNull RecentFilesChangeListener.Change change) {
        if (change.getChangeType() == RecentFilesChangeListener.Change.ChangeType.FILE_ADDED) {
            Platform.runLater(() -> view.addRecentFile(change.getFilePath()));
        } else if (change.getChangeType() == RecentFilesChangeListener.Change.ChangeType.FILE_MOVED) {
            String filePath = change.getFilePath();
            int movedTo = change.getMovedTo();
            Platform.runLater(() -> view.moveRecentFile(filePath, movedTo));
        }
    }

    private void viewTabIndexChanged(@NotNull ObservableValue<? extends Number> observable,
                                     @NotNull Number oldIndex,
                                     @NotNull Number newIndex) {
        int tabIndex = newIndex.intValue();
        tabsData.removeTabIndexListener(modelIndexListener);
        tabsData.setCurrTabIndex(tabIndex);
        tabsData.addTabIndexListener(modelIndexListener);

        String sourceMode = tabsData.getSourceMode(tabIndex);
        if (!sourceMode.isEmpty()) {
            view.setSourceMode(tabsData.getSourceMode(tabIndex));
        }

        String targetMode = tabsData.getTargetMode(tabIndex);
        if (!targetMode.isEmpty()) {
            view.setTargetMode(tabsData.getTargetMode(tabIndex));
        }

        MainWindowView.FieldInFocus inFocus = view.getFieldInFocus(tabIndex);
        if (inFocus == MainWindowView.FieldInFocus.SOURCE) {
            view.showFilePath(tabsData.getSourceFilePath(tabIndex));
        } else if (inFocus == MainWindowView.FieldInFocus.TARGET) {
            view.showFilePath(tabsData.getTargetFilePath(tabIndex));
        }
    }

    private void viewTabTitleChanged(@NotNull String newTitle, int tabIndex) {
        tabsData.removeTabTitleListener(modelTabTitleListener);
        tabsData.setTabTitle(newTitle, tabIndex);
        tabsData.addTabTitleListener(modelTabTitleListener);
    }

    private void viewSourceModeChanged(@NotNull String newMode, int tabIndex) {
        tabsData.removeSourceModeListener(modelSourceModeListener);
        tabsData.setSourceMode(newMode, tabIndex);
        tabsData.addSourceModeListener(modelSourceModeListener);
    }

    private void viewTargetModeChanged(@NotNull String newMode, int tabIndex) {
        tabsData.removeTargetModeListener(modelTargetModeListener);
        tabsData.setTargetMode(newMode, tabIndex);
        tabsData.addTargetModeListener(modelTargetModeListener);
    }

    private void viewSourceTextChanged(@NotNull String newText, int tabIndex) {
        tabsData.removeSourceTextListener(modelSourceTextListener);
        tabsData.setSourceText(newText, tabIndex);
        tabsData.addSourceTextListener(modelSourceTextListener);
    }

    private void viewTargetTextChanged(@NotNull String newText, int tabIndex) {
        tabsData.removeTargetTextListener(modelTargetTextListener);
        tabsData.setTargetText(newText, tabIndex);
        tabsData.addTargetTextListener(modelTargetTextListener);
    }

    private void viewFocusChanged(@NotNull MainWindowView.FieldInFocus inFocus, int tabIndex) {
        if (inFocus == MainWindowView.FieldInFocus.SOURCE) {
            view.showFilePath(tabsData.getSourceFilePath(tabIndex));
        } else if (inFocus == MainWindowView.FieldInFocus.TARGET) {
            view.showFilePath(tabsData.getTargetFilePath(tabIndex));
        }
    }

    private void receiveDrag(@NotNull DragEvent dragEvent) {
        Dragboard dragboard = dragEvent.getDragboard();
        if (dragboard.hasFiles()) {
            Stream<File> files = dragboard.getFiles().stream();
            if (!files.allMatch(file -> file.isDirectory() || file.canExecute())) {
                dragEvent.acceptTransferModes(TransferMode.COPY);
            }

            dragEvent.consume();
        }
    }

    private void receiveDrop(@NotNull DragEvent dragEvent) {
       openFiles(dragEvent.getDragboard().getFiles());
    }

    private void openFiles(@NotNull List<File> files) {
        try {
            logger.info("Opening " + files.size() + " files");
            fileHandler.openSourceFiles(files);
            logger.info(files.size() + " files opened");
        } catch (Exception e) {
            view.showError("File opening error: ", e.getMessage());
        }
    }

    private void openTabPressed() {
        int nextIndex = tabsData.getCurrTabIndex() + 1;
        tabsData.removeTabsListener(modelTabsListener);
        tabsData.openTab(nextIndex);
        tabsData.addTabsListener(modelTabsListener);

        view.openTab(nextIndex);
        tabsData.setCurrTabIndex(nextIndex);

        logger.info("Tab opened. Index: " + nextIndex);
    }

    private void closeTabPressed(@NotNull TabCloseEvent closeRequestEvent) {
        if (tabsData.countTabs() == 1) {
            return;
        }

        tabsData.removeTabsListener(modelTabsListener);
        int tabIndex = closeRequestEvent.getTabIndex();
        tabsData.removeTab(tabIndex);
        tabsData.addTabsListener(modelTabsListener);

        view.closeTab(tabIndex);
        if (tabIndex != 0) {
            view.setCurrTabIndex(tabIndex - 1);
        } else {
            view.setCurrTabIndex(tabIndex);
        }

        logger.info("Tab " + tabIndex + " closed");
    }

    private void openFilePressed() {
        Optional<List<File>> selectedFiles = view.choseFilesToOpen();
        if (selectedFiles.isEmpty()) {
            return;
        }

        List<File> mutableFilesList = new ArrayList<>(selectedFiles.get());
        openFiles(mutableFilesList);
    }

    private void openRecentFilePressed(@NotNull RecentFileEvent recentFileEvent) {
        File file = new File(recentFileEvent.getFilePath());
        List<File> mutableList = new ArrayList<>();
        mutableList.add(file);
        openFiles(mutableList);
    }

    private void clearRecentFilesPressed() {
        fileHandler.clearRecentFiles();
        view.clearRecentFiles();
        logger.info("Recent files cleared");
    }

    private void saveFilePressed() {
        int currIndex = tabsData.getCurrTabIndex();
        MainWindowView.FieldInFocus inFocus = view.getFieldInFocus(currIndex);
        try {
            if (inFocus == MainWindowView.FieldInFocus.SOURCE) {
                if (tabsData.getSourceFilePath(currIndex).isEmpty()) {
                    saveFileAsPressed();
                    return;
                }

                logger.info("Saving source file in tab " + currIndex);
                fileHandler.saveSourceFile(currIndex);
                logger.info("Source file saved in tab " + currIndex);
            } else if (inFocus == MainWindowView.FieldInFocus.TARGET) {
                if (tabsData.getTargetFilePath(currIndex).isEmpty()) {
                    saveFileAsPressed();
                    return;
                }

                logger.info("Saving target file in tab " + currIndex);
                fileHandler.saveTargetFile(currIndex);
                logger.info("Target file saved in tab " + currIndex);
            }
        } catch (Exception e) {
            logger.error("Saving file: " + e.getMessage());
            view.showError("Filesystem error", e.getMessage());
        }
    }

    private void saveFileAsPressed() {
        int currIndex = tabsData.getCurrTabIndex();
        MainWindowView.FieldInFocus inFocus = view.getFieldInFocus(currIndex);
        Optional<String> optionalFilePath = view.choseFileSavingLocation();
        String filePath;
        if (optionalFilePath.isPresent()) {
            filePath = optionalFilePath.get();
        } else {
            return;
        }

        try {
            if (inFocus == MainWindowView.FieldInFocus.SOURCE) {
                logger.info("Saving source file: " + filePath);
                fileHandler.saveSourceFileAs(currIndex, filePath);
                logger.info("Source file saved: " + filePath);
            } else if (inFocus == MainWindowView.FieldInFocus.TARGET) {
                logger.info("Saving target file: " + filePath);
                fileHandler.saveTargetFileAs(currIndex, filePath);
                logger.info("Target file saved: " + filePath);
            }
        } catch (Exception e) {
            logger.error("Saving file: " + e.getMessage());
            view.showError("Filesystem error", e.getMessage());
        }
    }

    private void runConversionPressed() {
        int currIndex = tabsData.getCurrTabIndex();
        try {
            if (!tabsData.getSourceFilePath(currIndex).isEmpty()) {
                logger.info("Saving source file in tab " + currIndex);
                fileHandler.saveSourceFile(currIndex);
                logger.info("Source file saved in tab " + currIndex);
            }

            Platform.runLater(() -> view.showConversionStart(currIndex));
            logger.info("Running conversion in tab " + currIndex);
            converter.run(currIndex);
            logger.info("Conversion ended in tab " + currIndex);
        } catch (Exception e) {
            String errorMsg = "Conversion error in tab " + (currIndex + 1) +
                    ".\n" + e.getMessage();
            logger.error(errorMsg);
            view.showError("Conversion error", errorMsg);
        } finally {
            Platform.runLater(() -> view.showConversionEnd(currIndex));
        }
    }

    private void openOnlineHelpPressed() {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI("https://www.sqlines.com/contact-us"));
            } catch (Exception e) {
               logger.error("Open online help: " + e.getMessage());
            }
        }
    }

    private void openSitePressed() {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI("https://www.sqlines.com"));
            } catch (Exception e) {
                logger.error("Open site: " + e.getMessage());
            }
        }
    }
}
