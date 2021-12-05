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

package com.sqlines.studio.view.mainwindow;

import com.sqlines.studio.view.Window;
import com.sqlines.studio.view.mainwindow.editor.CodeEditor;
import com.sqlines.studio.view.mainwindow.event.RecentFileEvent;
import com.sqlines.studio.view.mainwindow.event.TabCloseEvent;
import com.sqlines.studio.view.mainwindow.listener.FocusChangeListener;
import com.sqlines.studio.view.mainwindow.listener.ModeChangeListener;
import com.sqlines.studio.view.mainwindow.listener.TabTitleChangeListener;
import com.sqlines.studio.view.mainwindow.listener.TextChangeListener;
import com.sqlines.studio.view.ErrorWindow;

import org.jetbrains.annotations.NotNull;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.WindowEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Optional;

/**
 * The concrete main window.
 */
public class MainWindow extends Window implements MainWindowView, MainWindowSettingsView {
    private final BorderPane layout = new BorderPane();
    private final MenuBar menuBar = new MenuBar();
    private final ToolBar toolBar = new ToolBar();
    private final TabPane tabBar = new TabPane();
    private final StatusBar statusBar = new StatusBar();

    private final List<TabTitleChangeListener> tabTitleListeners = new ArrayList<>(5);
    private final List<ModeChangeListener> sourceModeListeners = new ArrayList<>(5);
    private final List<ModeChangeListener> targetModeListeners = new ArrayList<>(5);
    private final List<TextChangeListener> sourceTextListeners = new ArrayList<>(5);
    private final List<TextChangeListener> targetTextListeners = new ArrayList<>(5);
    private final List<FocusChangeListener> focusListeners = new ArrayList<>(5);
    private EventHandler<TabCloseEvent> tabCloseEventHandler;
    private EventHandler<DragEvent> dragEventHandler;
    private EventHandler<DragEvent> dropEventHandler;

    private FieldInFocus inFocus = FieldInFocus.SOURCE;
    private TargetFieldPolicy targetFieldPolicy = TargetFieldPolicy.AS_NEEDED;
    private WrappingPolicy wrappingPolicy = WrappingPolicy.NO_WRAP;
    private HighlighterPolicy highlighterPolicy = HighlighterPolicy.HIGHLIGHT;
    private LineNumbersPolicy lineNumbersPolicy = LineNumbersPolicy.SHOW;

    public MainWindow() {
        menuBar.setOnCloseTabAction(this::fireCloseEvent);
        menuBar.setOnAboutAction(event -> showAbout());
        menuBar.setOnNextTabAction(event -> nextTab());
        menuBar.setOnPrevTabAction(event -> prevTab());
        menuBar.setOnUndoAction(event -> undo());
        menuBar.setOnRedoAction(event -> redo());
        menuBar.setOnSelectAllAction(event -> selectAll());
        menuBar.setOnCutAction(event -> cut());
        menuBar.setOnCopyAction(event -> copy());
        menuBar.setOnPasteAction(event -> paste());
        menuBar.setOnZoomInAction(event -> zoomIn());
        menuBar.setOnZoomOutAction(event -> zoomOut());

        tabBar.setOnDragOver(this::handleDragEvent);
        tabBar.setOnDragDropped(this::handleDropEvent);
        tabBar.focusedProperty().addListener(this::focusChanged);
        tabBar.getSelectionModel().selectedIndexProperty().addListener(this::tabSelectionChanged);

        toolBar.addSourceModeChangeListener(this::sourceModeChanged);
        toolBar.addTargetModeChangeListener(this::targetModeChanged);
        toolBar.addFocusChangeListener(this::focusChanged);

        menuBar.setCloseTabState(false);
        menuBar.setNextTabState(false);
        menuBar.setPrevTabState(false);
        menuBar.setOpenRecentState(false);
        menuBar.setUndoState(false);
        menuBar.setRedoState(false);
        menuBar.setStatusBarSelected(true);
        menuBar.setTargetFieldSelected(false);
        menuBar.setWrappingSelected(false);
        menuBar.setHighlighterSelected(true);
        menuBar.setLineNumbersSelected(true);

        tabBar.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        // Stop the tab key and the arrow keys from navigating through the controls
        tabBar.addEventFilter(KeyEvent.ANY, keyEvent -> {
            if ((keyEvent.getCode() == KeyCode.TAB
                    || keyEvent.getCode() == KeyCode.RIGHT
                    || keyEvent.getCode() == KeyCode.LEFT
                    || keyEvent.getCode() == KeyCode.UP
                    || keyEvent.getCode() == KeyCode.DOWN)
                    && tabBar.isFocused()) {
                keyEvent.consume();
            }
        });

        layout.setTop(new VBox(menuBar, toolBar));
        layout.setCenter(tabBar);
        layout.setBottom(statusBar);

        setRoot(layout);
        setTitle("SQLines Studio");
        setMinWidth(660);
        setMinHeight(300);
        setWidth(770);
        setHeight(650);
    }

    /**
     * Sets conversion modes displayed in the tool bar.
     *
     * @param sourceModes list of source modes to set
     * @param targetModes list of target modes to set
     *
     * @throws IllegalStateException if either source modes list or target modes list is empty
     */
    public void setConversionModes(@NotNull List<String> sourceModes,
                                   @NotNull List<String> targetModes) {
        toolBar.setSourceModes(sourceModes);
        toolBar.setTargetModes(targetModes);
    }

    @Override
    public @NotNull FieldInFocus getFieldInFocus(int tabIndex) {
        // Trows exception if tabIndex is out of valid range
        checkRange(tabIndex, 0, tabBar.getTabs().size());
        return inFocus;
    }

    @Override
    public void setSourceMode(@NotNull String mode) {
        toolBar.selectSourceMode(mode);
    }

    @Override
    public void setTargetMode(@NotNull String mode) {
        toolBar.selectTargetMode(mode);
    }

    @Override
    public void setSourceText(@NotNull String text, int tabIndex) {
        // Trows exception if tabIndex is out of valid range
        checkRange(tabIndex, 0, tabBar.getTabs().size());

        Tab tab = tabBar.getTabs().get(tabIndex);
        CentralNode centralNode = (CentralNode) tab.getContent();
        centralNode.setSourceText(text);
    }

    @Override
    public void setTargetText(@NotNull String text, int tabIndex) {
        // Trows exception if tabIndex is out of valid range
        checkRange(tabIndex, 0, tabBar.getTabs().size());

        Tab tab = tabBar.getTabs().get(tabIndex);
        CentralNode centralNode = (CentralNode) tab.getContent();
        centralNode.setTargetText(text);
    }

    @Override
    public void setWindowTitle(@NotNull String title) {
        setTitle(title);
    }

    @Override
    public void setTabTitle(@NotNull String title, int tabIndex) {
        // Trows exception if tabIndex is out of valid range
        checkRange(tabIndex, 0, tabBar.getTabs().size());

        Tab tab = tabBar.getTabs().get(tabIndex);
        tab.setText(title);
        tabTitleListeners.forEach(listener -> listener.changed(title, tabIndex));
    }

    @Override
    public void setCurrTabIndex(int tabIndex) {
        // Trows exception if tabIndex is out of valid range
        checkRange(tabIndex, 0, tabBar.getTabs().size());

        tabBar.getSelectionModel().select(tabIndex);
    }

    @Override
    public void setStatusBarPolicy(@NotNull StatusBarPolicy policy) {
        if (policy == StatusBarPolicy.SHOW && !layout.getChildren().contains(statusBar)) {
            layout.setBottom(statusBar);
            menuBar.setStatusBarSelected(true);
        } else if (policy == StatusBarPolicy.DO_NOT_SHOW) {
            layout.getChildren().remove(statusBar);
            menuBar.setStatusBarSelected(false);
        }
    }

    @Override
    public void setTargetFieldPolicy(@NotNull TargetFieldPolicy policy) {
        targetFieldPolicy = policy;

        if (policy == TargetFieldPolicy.ALWAYS) {
            for (Tab tab : tabBar.getTabs()) {
                CentralNode centralNode = (CentralNode) tab.getContent();
                centralNode.setTargetFieldPolicy(CentralNode.TargetFieldPolicy.ALWAYS);
            }
            menuBar.setTargetFieldSelected(true);
        } else if (policy == TargetFieldPolicy.AS_NEEDED) {
            for (Tab tab : tabBar.getTabs()) {
                CentralNode centralNode = (CentralNode) tab.getContent();
                centralNode.setTargetFieldPolicy(CentralNode.TargetFieldPolicy.AS_NEEDED);
            }
            menuBar.setTargetFieldSelected(false);
        }
    }

    @Override
    public void setWrappingPolicy(@NotNull WrappingPolicy policy) {
        wrappingPolicy = policy;

        if (policy == WrappingPolicy.WRAP_LINES) {
            for (Tab tab : tabBar.getTabs()) {
                CentralNode centralNode = (CentralNode) tab.getContent();
                centralNode.setWrappingPolicy(CodeEditor.WrappingPolicy.WRAP_LINES);
            }
            menuBar.setWrappingSelected(true);
        } else if (policy == WrappingPolicy.NO_WRAP) {
            for (Tab tab : tabBar.getTabs()) {
                CentralNode centralNode = (CentralNode) tab.getContent();
                centralNode.setWrappingPolicy(CodeEditor.WrappingPolicy.NO_WRAP);
            }
            menuBar.setWrappingSelected(false);
        }
    }

    @Override
    public void setHighlighterPolicy(@NotNull HighlighterPolicy policy) {
        highlighterPolicy = policy;

        if (policy == HighlighterPolicy.HIGHLIGHT) {
            for (Tab tab : tabBar.getTabs()) {
                CentralNode centralNode = (CentralNode) tab.getContent();
                centralNode.setHighlighterPolicy(CodeEditor.HighlighterPolicy.HIGHLIGHT);
            }
            menuBar.setHighlighterSelected(true);
        } else if (policy == HighlighterPolicy.DO_NOT_HIGHLIGHT) {
            for (Tab tab : tabBar.getTabs()) {
                CentralNode centralNode = (CentralNode) tab.getContent();
                centralNode.setHighlighterPolicy(CodeEditor.HighlighterPolicy.DO_NOT_HIGHLIGHT);
            }
            menuBar.setHighlighterSelected(false);
        }
    }

    @Override
    public void setLineNumbersPolicy(@NotNull LineNumbersPolicy policy) {
        lineNumbersPolicy = policy;

        if (policy == LineNumbersPolicy.SHOW) {
            for (Tab tab : tabBar.getTabs()) {
                CentralNode centralNode = (CentralNode) tab.getContent();
                centralNode.setLineNumbersPolicy(CodeEditor.LineNumbersPolicy.SHOW);
            }
            statusBar.showLineColumnNumberArea(true);
            menuBar.setLineNumbersSelected(true);
        } else if (policy == LineNumbersPolicy.DO_NOT_SHOW) {
            for (Tab tab : tabBar.getTabs()) {
                CentralNode centralNode = (CentralNode) tab.getContent();
                centralNode.setLineNumbersPolicy(CodeEditor.LineNumbersPolicy.DO_NOT_SHOW);
            }
            statusBar.showLineColumnNumberArea(false);
            menuBar.setLineNumbersSelected(false);
        }
    }

    @Override
    public void openTab(int tabIndex) {
        // Trows exception if tabIndex is out of valid range
        checkRange(tabIndex, 0, tabBar.getTabs().size() + 1);

        Tab newTab = new Tab();
        setTabTitle(newTab, tabIndex);
        newTab.setOnCloseRequest(event -> {
            int index = tabBar.getTabs().indexOf(newTab);
            TabCloseEvent closeEvent = new TabCloseEvent(index);
            tabBar.fireEvent(closeEvent);

            if (tabCloseEventHandler != null) {
                tabCloseEventHandler.handle(closeEvent);
                event.consume();
            }
        });

        CentralNode centralNode = new CentralNode();
        setUpCentralNode(newTab, centralNode);

        newTab.setContent(centralNode);
        tabBar.getTabs().add(tabIndex, newTab);
        tabBar.getSelectionModel().select(tabIndex);

        centralNode.focusOn(CentralNode.FieldInFocus.SOURCE);
        inFocus = FieldInFocus.SOURCE;
        focusListeners.forEach(listener -> listener.changed(inFocus, tabIndex));

        sourceModeListeners.forEach(listener -> listener.changed(toolBar.getSourceMode(), tabIndex));
        targetModeListeners.forEach(listener -> listener.changed(toolBar.getTargetMode(), tabIndex));

        int tabsNumber = tabBar.getTabs().size();
        menuBar.setCloseTabState(tabsNumber != 1);
        tabBar.getTabs().forEach(tab -> tab.setClosable(tabsNumber != 1));
    }

    @Override
    public void closeTab(int tabIndex) {
        // Trows exception if tabIndex is out of valid range
        checkRange(tabIndex, 0, tabBar.getTabs().size());

        tabBar.getTabs().remove(tabIndex);
        if (tabIndex != 0) {
            Tab tab = tabBar.getTabs().get(tabIndex - 1);
            CentralNode centralNode = (CentralNode) tab.getContent();
            centralNode.focusOn(CentralNode.FieldInFocus.SOURCE);
        }

        int tabsNumber = tabBar.getTabs().size();
        menuBar.setCloseTabState(tabsNumber != 1);
        tabBar.getTabs().forEach(tab -> tab.setClosable(tabsNumber != 1));
    }

    @Override
    public void closeAllTabs() {
        tabBar.getTabs().clear();
    }

    @Override
    public void showConversionStart(int tabIndex) {
        // Trows exception if tabIndex is out of valid range
        checkRange(tabIndex, 0, tabBar.getTabs().size());

        Tab currTab = tabBar.getTabs().get(tabIndex);
        CentralNode centralNode = (CentralNode) currTab.getContent();
        centralNode.setDisable(true);
    }

    @Override
    public void showConversionEnd(int tabIndex) {
        // Trows exception if tabIndex is out of valid range
        checkRange(tabIndex, 0, tabBar.getTabs().size());

        Tab currTab = tabBar.getTabs().get(tabIndex);
        CentralNode centralNode = (CentralNode) currTab.getContent();
        centralNode.setDisable(false);
        centralNode.focusOn(CentralNode.FieldInFocus.TARGET);
    }

    @Override
    public void showError(@NotNull String cause, @NotNull String errorMsg) {
        ErrorWindow errorWindow = new ErrorWindow(cause, errorMsg);
        if (getTheme() == Theme.LIGHT) {
            errorWindow.setLightStylesheets(getLightStylesheets());
            errorWindow.setTheme(Theme.LIGHT);
        } else if (getTheme() == Theme.DARK) {
            errorWindow.setDarkStylesheets(getDarkStylesheets());
            errorWindow.setTheme(Theme.DARK);
        }

        errorWindow.show();
    }

    @Override
    public void showFilePath(@NotNull String filePath) {
        statusBar.setFilePath(filePath);
    }

    @Override
    public @NotNull Optional<List<File>> choseFilesToOpen() {
        FileChooser chooser = new FileChooser();
        List<File> files = chooser.showOpenMultipleDialog(this);
        if (files != null) {
            return Optional.of(files);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public @NotNull Optional<String> choseFileSavingLocation() {
        FileChooser chooser = new FileChooser();
        File dir = chooser.showSaveDialog(this);
        if (dir != null) {
            return Optional.of(dir.getAbsolutePath());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void addRecentFile(@NotNull String filePath) {
        menuBar.addRecentFile(filePath);
        menuBar.setOpenRecentState(true);
    }

    @Override
    public void clearRecentFiles() {
        menuBar.clearRecentFiles();
        menuBar.setOpenRecentState(false);
    }

    @Override
    public void moveRecentFile(@NotNull String filePath, int moveTo) {
        menuBar.moveRecentFile(filePath, moveTo);
    }

    @Override
    public void addTabSelectionListener(@NotNull ChangeListener<Number> listener) {
        tabBar.getSelectionModel().selectedIndexProperty().addListener(listener);
    }

    @Override
    public void removeTabSelectionListener(@NotNull ChangeListener<Number> listener) {
        tabBar.getSelectionModel().selectedIndexProperty().removeListener(listener);
    }

    @Override
    public void addTabTitleListener(@NotNull TabTitleChangeListener listener) {
        tabTitleListeners.add(listener);
    }

    @Override
    public void removeTabTitleListener(@NotNull TabTitleChangeListener listener) {
        tabTitleListeners.remove(listener);
    }

    @Override
    public void addSourceTextListener(@NotNull TextChangeListener listener) {
        sourceTextListeners.add(listener);
    }

    @Override
    public void removeSourceTextListener(@NotNull TextChangeListener listener) {
        sourceTextListeners.remove(listener);
    }

    @Override
    public void addTargetTextListener(@NotNull TextChangeListener listener) {
        targetTextListeners.add(listener);
    }

    @Override
    public void removeTargetTextListener(@NotNull TextChangeListener listener) {
        targetTextListeners.remove(listener);
    }

    @Override
    public void addSourceModeListener(@NotNull ModeChangeListener listener) {
        sourceModeListeners.add(listener);
    }

    @Override
    public void removeSourceModeListener(@NotNull ModeChangeListener listener) {
        sourceModeListeners.remove(listener);
    }

    @Override
    public void addTargetModeListener(@NotNull ModeChangeListener listener) {
        targetModeListeners.add(listener);
    }

    @Override
    public void removeTargetModeListener(@NotNull ModeChangeListener listener) {
        targetModeListeners.remove(listener);
    }

    @Override
    public void addFocusListener(@NotNull FocusChangeListener listener) {
        focusListeners.add(listener);
    }

    @Override
    public void setOnTabCloseAction(@NotNull EventHandler<WindowEvent> action) {
        setOnCloseRequest(action);
    }

    @Override
    public void setOnDragAction(@NotNull EventHandler<DragEvent> action) {
        dragEventHandler = action;
    }

    @Override
    public void setOnDropAction(@NotNull EventHandler<DragEvent> action) {
        dropEventHandler = action;
    }

    @Override
    public void setOnPreferencesAction(@NotNull EventHandler<ActionEvent> action) {
        menuBar.setOnPreferencesAction(action);
    }

    @Override
    public void setOnNewTabAction(@NotNull EventHandler<ActionEvent> action) {
        menuBar.setOnNewTabAction(action);
        toolBar.setOnNewTabAction(action);
    }

    @Override
    public void setOnCloseTabAction(@NotNull EventHandler<TabCloseEvent> action) {
        tabCloseEventHandler = action;
    }

    @Override
    public void setOnOpenFileAction(@NotNull EventHandler<ActionEvent> action) {
        menuBar.setOnOpenFileAction(action);
        toolBar.setOnOpenFileAction(action);
    }

    @Override
    public void setOnRecentFileAction(@NotNull EventHandler<RecentFileEvent> action) {
        menuBar.setOnOpenRecentAction(action);
    }

    @Override
    public void setOnClearRecentAction(@NotNull EventHandler<ActionEvent> action) {
        menuBar.setOnClearRecentAction(action);
    }

    @Override
    public void setOnSaveFileAction(@NotNull EventHandler<ActionEvent> action) {
        menuBar.setOnSaveFileAction(action);
        toolBar.setOnSaveFileAction(action);
    }

    @Override
    public void setOnSaveAsAction(@NotNull EventHandler<ActionEvent> action) {
        menuBar.setOnSaveAsAction(action);
    }

    @Override
    public void setOnRunAction(@NotNull EventHandler<ActionEvent> action) {
        menuBar.setOnRunAction(action);
        toolBar.setOnRunAction(action);
    }

    @Override
    public void setOnOnlineHelpAction(@NotNull EventHandler<ActionEvent> action) {
        menuBar.setOnOnlineHelpAction(action);
    }

    @Override
    public void setOnOpenSiteAction(@NotNull EventHandler<ActionEvent> action) {
        menuBar.setOnOpenSiteAction(action);
    }

    @Override
    public void setOnStatusBarAction(@NotNull EventHandler<ActionEvent> action) {
        menuBar.setOnStatusBarAction(action);
    }

    @Override
    public void setOnTargetFieldAction(@NotNull EventHandler<ActionEvent> action) {
        menuBar.setOnTargetFieldAction(action);
    }

    @Override
    public void setOnWrappingAction(@NotNull EventHandler<ActionEvent> action) {
        menuBar.setOnWrappingAction(action);
    }

    @Override
    public void setOnHighlighterAction(@NotNull EventHandler<ActionEvent> action) {
        menuBar.setOnHighlighterAction(action);
    }

    @Override
    public void setOnLineNumbersAction(@NotNull EventHandler<ActionEvent> action) {
        menuBar.setOnLineNumbersAction(action);
    }

    private void checkRange(int tabIndex, int from, int to) {
        if (tabIndex < from || tabIndex >= to) {
            int endInd = (tabBar.getTabs().size() == 0) ? 0 : tabBar.getTabs().size() - 1;
            String errorMsg = "Invalid index: " + "(0:" + endInd + ") expected, " +
                    tabIndex + " provided";
            throw new IndexOutOfBoundsException(errorMsg);
        }
    }

    private void fireCloseEvent(@NotNull ActionEvent event) {
        int tabIndex = tabBar.getSelectionModel().getSelectedIndex();
        TabCloseEvent closeEvent = new TabCloseEvent(tabIndex);
        tabBar.fireEvent(closeEvent);

        if (tabCloseEventHandler != null) {
            tabCloseEventHandler.handle(closeEvent);
            event.consume();
        }
    }

    private void showAbout() {
        AboutWindow aboutWindow = new AboutWindow();
        if (getTheme() == Theme.LIGHT) {
            aboutWindow.setLightStylesheets(getLightStylesheets());
            aboutWindow.setTheme(Theme.LIGHT);
        } else if (getTheme() == Theme.DARK) {
            aboutWindow.setDarkStylesheets(getDarkStylesheets());
            aboutWindow.setTheme(Theme.DARK);
        }

        aboutWindow.show();
    }

    private void nextTab() {
        int currIndex = tabBar.getSelectionModel().getSelectedIndex();
        if (currIndex != tabBar.getTabs().size() - 1) {
            tabBar.getSelectionModel().select(currIndex + 1);
        }
    }

    private void prevTab() {
        int currIndex = tabBar.getSelectionModel().getSelectedIndex();
        if (currIndex != 0) {
            tabBar.getSelectionModel().select(currIndex - 1);
        }
    }

    private void undo() {
        Tab currTab = tabBar.getSelectionModel().getSelectedItem();
        CentralNode centralNode = (CentralNode) currTab.getContent();
        centralNode.undo();
    }

    private void redo() {
        Tab currTab = tabBar.getSelectionModel().getSelectedItem();
        CentralNode centralNode = (CentralNode) currTab.getContent();
        centralNode.redo();
    }

    private void selectAll() {
        Tab currTab = tabBar.getSelectionModel().getSelectedItem();
        CentralNode centralNode = (CentralNode) currTab.getContent();
        centralNode.selectAll();
    }

    private void cut() {
        Tab currTab = tabBar.getSelectionModel().getSelectedItem();
        CentralNode centralNode = (CentralNode) currTab.getContent();
        centralNode.cut();
    }

    private void copy() {
        Tab currTab = tabBar.getSelectionModel().getSelectedItem();
        CentralNode centralNode = (CentralNode) currTab.getContent();
        centralNode.copy();
    }

    private void paste() {
        Tab currTab = tabBar.getSelectionModel().getSelectedItem();
        CentralNode centralNode = (CentralNode) currTab.getContent();
        centralNode.paste();
    }

    private void zoomIn() {
        for (Tab tab : tabBar.getTabs()) {
            CentralNode centralNode = (CentralNode) tab.getContent();
            centralNode.zoomIn();
        }
    }

    private void zoomOut() {
        for (Tab tab : tabBar.getTabs()) {
            CentralNode centralNode = (CentralNode) tab.getContent();
            centralNode.zoomOut();
        }
    }

    private void handleDragEvent(@NotNull DragEvent event) {
        if (dragEventHandler != null) {
            dragEventHandler.handle(event);
        }
    }

    private void handleDropEvent(@NotNull DragEvent event) {
        if (dropEventHandler != null) {
            dropEventHandler.handle(event);
        }
    }

    private void focusChanged(@NotNull ObservableValue<? extends Boolean> observable,
                              @NotNull Boolean oldFocus,
                              @NotNull Boolean isFocusedNow) {
        if (tabBar.getSelectionModel().getSelectedIndex() == -1) {
            return;
        }

        Tab currTab = tabBar.getSelectionModel().getSelectedItem();
        CentralNode centralNode = (CentralNode) currTab.getContent();
        if (inFocus == FieldInFocus.SOURCE) {
            centralNode.focusOn(CentralNode.FieldInFocus.SOURCE);
        } else if (inFocus == FieldInFocus.TARGET) {
            centralNode.focusOn(CentralNode.FieldInFocus.TARGET);
        }

        menuBar.setUndoState(centralNode.isUndoAvailable());
        menuBar.setRedoState(centralNode.isRedoAvailable());
    }

    private void tabSelectionChanged(@NotNull ObservableValue<? extends Number> observable,
                                     @NotNull Number oldIndex,
                                     @NotNull Number newIndex) {
        int tabIndex = newIndex.intValue();
        menuBar.setPrevTabState(tabIndex > 0);
        menuBar.setNextTabState(tabIndex != tabBar.getTabs().size() - 1);

        Tab tab = tabBar.getTabs().get(tabIndex);
        CentralNode centralNode = (CentralNode) tab.getContent();
        centralNode.focusOn(CentralNode.FieldInFocus.SOURCE);
    }

    private void sourceModeChanged(@NotNull ObservableValue<? extends String> observable,
                                   String oldMode,
                                   @NotNull String newMode) {
        int currIndex = tabBar.getSelectionModel().getSelectedIndex();
        sourceModeListeners.forEach(listener -> listener.changed(newMode, currIndex));
    }

    private void targetModeChanged(@NotNull ObservableValue<? extends String> observable,
                                   String oldMode,
                                   @NotNull String newMode) {
        int currIndex = tabBar.getSelectionModel().getSelectedIndex();
        targetModeListeners.forEach(listener -> listener.changed(newMode, currIndex));
    }

    private void setTabTitle(@NotNull Tab tab, int tabIndex) {
        List<Integer> tabNumbers = new LinkedList<>();
        tabBar.getTabs().forEach(item -> {
            StringBuilder tabTitle = new StringBuilder(item.getText());
            tabTitle.delete(0, 4); // Delete "Tab "
            try {
                int tabNumber = Integer.parseInt(tabTitle.toString());
                tabNumbers.add(tabNumber);  // The tab has a standard title "Tab i"
            } catch (NumberFormatException ignored) {
                // The tab had a custom title. Skip
            }
        });

        Collections.sort(tabNumbers);
        int i = 0;
        for (; i < tabNumbers.size(); i++) {
            if (tabNumbers.get(i) != i + 1) {
                break;
            }
        }

        String title = "Tab " + (i + 1);
        tab.setText(title);
        tabTitleListeners.forEach(listener -> listener.changed(title, tabIndex));
    }

    private void setUpCentralNode(@NotNull Tab tab, @NotNull CentralNode centralNode) {
        if (targetFieldPolicy == TargetFieldPolicy.ALWAYS) {
            centralNode.setTargetFieldPolicy(CentralNode.TargetFieldPolicy.ALWAYS);
        } else if (targetFieldPolicy == TargetFieldPolicy.AS_NEEDED) {
            centralNode.setTargetFieldPolicy(CentralNode.TargetFieldPolicy.AS_NEEDED);
        }

        if (wrappingPolicy == WrappingPolicy.WRAP_LINES) {
            centralNode.setWrappingPolicy(CodeEditor.WrappingPolicy.WRAP_LINES);
        } else if (wrappingPolicy == WrappingPolicy.NO_WRAP) {
            centralNode.setWrappingPolicy(CodeEditor.WrappingPolicy.NO_WRAP);
        }

        if (highlighterPolicy == HighlighterPolicy.HIGHLIGHT) {
            centralNode.setHighlighterPolicy(CodeEditor.HighlighterPolicy.HIGHLIGHT);
        } else if (highlighterPolicy == HighlighterPolicy.DO_NOT_HIGHLIGHT) {
            centralNode.setHighlighterPolicy(CodeEditor.HighlighterPolicy.DO_NOT_HIGHLIGHT);
        }

        if (lineNumbersPolicy == LineNumbersPolicy.SHOW) {
            centralNode.setLineNumbersPolicy(CodeEditor.LineNumbersPolicy.SHOW);
        } else if (lineNumbersPolicy == LineNumbersPolicy.DO_NOT_SHOW) {
            centralNode.setLineNumbersPolicy(CodeEditor.LineNumbersPolicy.DO_NOT_SHOW);
        }

        centralNode.addSourceTextListener((observable, oldText, newText) -> {
            menuBar.setUndoState(centralNode.isUndoAvailable());
            menuBar.setRedoState(centralNode.isRedoAvailable());

            int tabIndex = tabBar.getTabs().indexOf(tab);
            sourceTextListeners.forEach(listener -> listener.changed(newText, tabIndex));
        });

        centralNode.addTargetTextListener((observable, oldText, newText) -> {
            menuBar.setUndoState(centralNode.isUndoAvailable());
            menuBar.setRedoState(centralNode.isRedoAvailable());

            int tabIndex = tabBar.getTabs().indexOf(tab);
            targetTextListeners.forEach(listener -> listener.changed(newText, tabIndex));
        });

        centralNode.addSourceLineListener((observable, oldLineIndex, newLineIndex) ->
            statusBar.setLineNumber(newLineIndex + 1));

        centralNode.addSourceColumnListener((observable, oldColumnIndex, newColumnIndex) ->
            statusBar.setColumnNumber(newColumnIndex + 1));

        centralNode.addTargetLineListener((observable, oldLineIndex, newLineIndex) ->
            statusBar.setLineNumber(newLineIndex + 1));

        centralNode.addTargetColumnListener((observable, oldColumnIndex, newColumnIndex) ->
            statusBar.setColumnNumber(newColumnIndex + 1));

        Runnable focusChanged = () -> {
            int lineNumber = 0;
            int columnNumber = 0;
            if (inFocus == FieldInFocus.SOURCE) {
                lineNumber = centralNode.getSourceLineIndex();
                columnNumber = centralNode.getSourceColumnIndex();
            } else if (inFocus == FieldInFocus.TARGET) {
                lineNumber = centralNode.getTargetLineIndex();
                columnNumber = centralNode.getTargetColumnIndex();
            }

            statusBar.setLineNumber(lineNumber + 1);
            statusBar.setColumnNumber(columnNumber + 1);

            menuBar.setUndoState(centralNode.isUndoAvailable());
            menuBar.setRedoState(centralNode.isRedoAvailable());

            int tabIndex = tabBar.getSelectionModel().getSelectedIndex();
            focusListeners.forEach(listener -> listener.changed(inFocus, tabIndex));
        };

        centralNode.addSourceFocusListener((observable, oldFocus, isFocused) -> {
            if (isFocused) {
                inFocus = FieldInFocus.SOURCE;
                focusChanged.run();
            }
        });

        centralNode.addTargetFocusListener((observable, oldFocus, isFocused) -> {
            if (isFocused) {
                inFocus = FieldInFocus.TARGET;
                focusChanged.run();
            }
        });
    }
}
