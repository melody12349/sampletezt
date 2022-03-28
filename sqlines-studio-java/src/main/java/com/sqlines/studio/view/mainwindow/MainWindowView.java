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

import com.sqlines.studio.view.BaseView;
import com.sqlines.studio.view.mainwindow.event.RecentFileEvent;
import com.sqlines.studio.view.mainwindow.event.TabCloseEvent;
import com.sqlines.studio.view.mainwindow.listener.FocusChangeListener;
import com.sqlines.studio.view.mainwindow.listener.ModeChangeListener;
import com.sqlines.studio.view.mainwindow.listener.TabTitleChangeListener;
import com.sqlines.studio.view.mainwindow.listener.TextChangeListener;

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.DragEvent;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

/**
 * Interface through with the presenter / controller can interact with the main window UI.
 * Provides methods for basic work with the main window.
 * <p>
 * Allows listeners to track changes when they occur.
 *
 * @see ModeChangeListener
 * @see TextChangeListener
 */
public interface MainWindowView extends BaseView {

    /**
     * Represents currently focused text input field.
     */
    enum FieldInFocus { SOURCE, TARGET, NONE }

    /**
     * @param tabIndex the index of the tab with field in focus to get
     *
     * @return currently focused text input field in the specified tab
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     * (tabIndex < 0 || tabIndex > the number of opened tabs)
     */
    @NotNull FieldInFocus getFieldInFocus(int tabIndex);

    /**
     * Sets the specified source conversion mode as current.
     * <p>
     * Notifies all source {@link ModeChangeListener} listeners of the change.
     *
     * @param mode source mode to set
     *
     * @throws IllegalArgumentException if such a mode does not exist
     */
    void setSourceMode(@NotNull String mode);

    /**
     * Sets the specified target conversion mode as current.
     * <p>
     * Notifies all target {@link ModeChangeListener} listeners of the change.
     *
     * @param mode target mode to set
     *
     * @throws IllegalArgumentException if such a mode does not exist
     */
    void setTargetMode(@NotNull String mode);

    /**
     * Replaces the entire content of the source text input field with the given text.
     * <p>
     * Notifies all source {@link TextChangeListener} listeners of the change.
     *
     * @param text text to set
     * @param tabIndex the index of the tab with source text to set
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     * (tabIndex < 0 || tabIndex > the number of opened tabs)
     */
    void setSourceText(@NotNull String text, int tabIndex);

    /**
     * Replaces the entire content of the target text input field with the given text.
     * <p>
     * Notifies all target {@link TextChangeListener} listeners of the change.
     *
     * @param text text to set
     * @param tabIndex the index of the tab with target text to set
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     * (tabIndex < 0 || tabIndex > the number of opened tabs)
     */
    void setTargetText(@NotNull String text, int tabIndex);

    /**
     * Sets the tab title to the tab with the specified index.
     * <p>
     * Notifies all {@link TabTitleChangeListener} listeners of the change.
     *
     * @param title tab title to set
     * @param tabIndex the index of the tab with tab title to set
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     * (tabIndex < 0 || tabIndex > the number of opened tabs)
     */
    void setTabTitle(@NotNull String title, int tabIndex);

    /**
     * Sets the specified tab as current.
     *
     * @param tabIndex tab index to set
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     * (tabIndex < 0 || tabIndex > the number of opened tabs)
     */
    void setCurrTabIndex(int tabIndex);

    /**
     * Creates new tab.
     *
     * @param tabIndex the index at which a tab is to be added
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     * (tabIndex < 0 || tabIndex > the number of opened tabs + 1)
     */
    void openTab(int tabIndex);

    /**
     * Closes a tab with a specified index.
     *
     * @param tabIndex the index of the tab to close
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     * (tabIndex < 0 || tabIndex > the number of opened tabs)
     */
    void closeTab(int tabIndex);

    /**
     * Closes all opened tabs.
     */
    void closeAllTabs();

    /**
     * Show the start of the conversion in the specified tab.
     *
     * @param tabIndex the index of the tab where the conversion started
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     * (tabIndex < 0 || tabIndex > the number of opened tabs)
     */
    void showConversionStart(int tabIndex);

    /**
     * Show the end of the conversion in the specified tab.
     *
     * @param tabIndex the index of the tab where the conversion ended
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     * (tabIndex < 0 || tabIndex > the number of opened tabs)
     */
    void showConversionEnd(int tabIndex);

    /**
     * Shows the specified file path in the toolbar.
     *
     * @param filePath file path to show
     */
    void showFilePath(@NotNull String filePath);

    /**
     * Shows a window prompting the user to select files to open.
     *
     * @return a list of selected files or {@link Optional#empty()}
     * if the user did not select any file
     */
    @NotNull Optional<List<File>> choseFilesToOpen();

    /**
     * Shows a window prompting the user to choose where to save a file.
     *
     * @return a selected file path or {@link Optional#empty()}
     * if the user did not select a file path
     */
    @NotNull Optional<String> choseFileSavingLocation();

    /**
     * Adds new recent file path to the Open Recent menu in the menu bar.
     *
     * @param filePath file path to add
     *
     * @throws IllegalArgumentException if file path is empty
     */
    void addRecentFile(@NotNull String filePath);

    /**
     * Deletes all recent files paths in the Open Recent menu.
     */
    void clearRecentFiles();

    /**
     * Moves the specified recent file path in the Open Recent menu in
     * the menu bar to the specified position.
     *
     * @throws IndexOutOfBoundsException if moveTo is out of range
     * (moveTo < 0 || moveTo > the number of recent file paths)
     * @throws IllegalArgumentException if no such recent file exists
     *
     * @param filePath file path to move
     * @param moveTo the index to move the file path to
     */
    void moveRecentFile(@NotNull String filePath, int moveTo);

    /**
     * Adds a listener which will be notified when the tab selection changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    void addTabSelectionListener(@NotNull ChangeListener<Number> listener);

    /**
     * Removes the specified listener.
     *
     * @param listener the listener to remove
     */
    void removeTabSelectionListener(@NotNull ChangeListener<Number> listener);

    /**
     * Adds a listener which will be notified when the tab title in any tab changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    void addTabTitleListener(@NotNull TabTitleChangeListener listener);

    /**
     * Removes the specified listener.
     *
     * @param listener the listener to remove
     */
    void removeTabTitleListener(@NotNull TabTitleChangeListener listener);

    /**
     * Adds a listener which will be notified when the text in the
     * source text input field in any tab changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    void addSourceTextListener(@NotNull TextChangeListener listener);

    /**
     * Removes the specified listener.
     *
     * @param listener the listener to remove
     */
    void removeSourceTextListener(@NotNull TextChangeListener listener);

    /**
     * Adds a listener which will be notified when the text in the
     * target text input field in any tab changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    void addTargetTextListener(@NotNull TextChangeListener listener);

    /**
     * Removes the specified listener.
     *
     * @param listener the listener to remove
     */
    void removeTargetTextListener(@NotNull TextChangeListener listener);

    /**
     * Adds a listener which will be notified when the source mode in any tab changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    void addSourceModeListener(@NotNull ModeChangeListener listener);

    /**
     * Removes the specified listener.
     *
     * @param listener the listener to remove
     */
    void removeSourceModeListener(@NotNull ModeChangeListener listener);

    /**
     * Adds a listener which will be notified when the target mode in any tab changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    void addTargetModeListener(@NotNull ModeChangeListener listener);

    /**
     * Removes the specified listener.
     *
     * @param listener the listener to remove
     */
    void removeTargetModeListener(@NotNull ModeChangeListener listener);

    /**
     * Adds a listener which will be notified when the focused field in any tab changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    void addFocusListener(@NotNull FocusChangeListener listener);

    /**
     * Sets the action which is invoked when the file is dragged over the main window.
     *
     * @param action the action to register
     */
    void setOnDragAction(@NotNull EventHandler<DragEvent> action);

    /**
     * Sets the action which is invoked when the file is dropped in the main window.
     *
     * @param action the action to register
     */
    void setOnDropAction(@NotNull EventHandler<DragEvent> action);

    /**
     * Sets the action which is invoked whenever the New Tab menu item
     * in the menu bar or the New Tab button in the Tool bar is clicked.
     *
     * @param action the action to register
     */
    void setOnNewTabAction(@NotNull EventHandler<ActionEvent> action);

    /**
     * Sets the action which is invoked whenever the Close Tab menu item
     * in the menu bar or the "x" button on the tab is clicked.
     *
     * @param action the action to register
     */
    void setOnCloseTabAction(@NotNull EventHandler<TabCloseEvent> action);

    /**
     * Sets the action which is invoked whenever the Open File menu item
     * in the menu bar or the Open File button in the Tool bar is clicked.
     *
     * @param action the action to register
     */
    void setOnOpenFileAction(@NotNull EventHandler<ActionEvent> action);

    /**
     * Sets the action which is invoked when
     * the Recent file in the Open Recent menu in the menu bar is clicked.
     *
     * @param action the action to register
     */
    void setOnRecentFileAction(@NotNull EventHandler<RecentFileEvent> action);

    /**
     * Sets the action which is invoked when the Clear menu item
     * in the Open Recent menu in the menu bar is clicked.
     *
     * @param action the action to register
     */
    void setOnClearRecentAction(@NotNull EventHandler<ActionEvent> action);

    /**
     * Sets the action which is invoked whenever the Save File menu item
     * in the menu bar or the Save File button in the Tool bar is clicked.
     *
     * @param action the action to register
     */
    void setOnSaveFileAction(@NotNull EventHandler<ActionEvent> action);

    /**
     * Sets the action which is invoked when
     * the Save File As menu item in the menu bar is clicked.
     *
     * @param action the action to register
     */
    void setOnSaveAsAction(@NotNull EventHandler<ActionEvent> action);

    /**
     * Sets the action which is invoked whenever the Run Conversion menu item
     * in the menu bar or the Run Conversion button in the Tool bar is clicked.
     *
     * @param action the action to register
     */
    void setOnRunAction(@NotNull EventHandler<ActionEvent> action);

    /**
     * Sets the action which is invoked when
     * the Open Online Help menu item in the menu bar is clicked.
     *
     * @param action the action to register
     */
    void setOnOnlineHelpAction(@NotNull EventHandler<ActionEvent> action);

    /**
     * Sets the action which is invoked when
     * the Open Official Site menu item in the menu bar is clicked.
     *
     * @param action the action to register
     */
    void setOnOpenSiteAction(@NotNull EventHandler<ActionEvent> action);
}
