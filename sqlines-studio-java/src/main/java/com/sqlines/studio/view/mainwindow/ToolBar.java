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

import org.jetbrains.annotations.NotNull;

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.List;

/**
 * Horizontal bar containing icons used to select the application's most frequently used tools.
 * <p>
 * Contains the following items:
 * <li>Open New Tab button
 * <li>Open File button
 * <li>Save File button
 * <li>Run Conversion button
 * <li>Source modes choice box
 * <li>Target modes choice box
 */
class ToolBar extends javafx.scene.control.ToolBar {
    private final Button newTabButton = new Button();
    private final Button openFileButton = new Button();
    private final Button saveFileButton = new Button();
    private final Button runButton = new Button();
    private final ChoiceBox<String> sourceModesBox = new ChoiceBox<>();
    private final ChoiceBox<String> targetModesBox = new ChoiceBox<>();

    /**
     * Creates a new {@link ToolBar}.
     *
     * @throws IllegalStateException if any of the toolbar icons
     * were not found in application resources
     */
    public ToolBar() {
        // Stop the tab key and the arrow keys from navigating through the controls
        addEventFilter(KeyEvent.ANY, keyEvent -> {
            if (keyEvent.getCode() == KeyCode.TAB
                    || keyEvent.getCode() == KeyCode.RIGHT
                    || keyEvent.getCode() == KeyCode.LEFT
                    || keyEvent.getCode() == KeyCode.UP
                    || keyEvent.getCode() == KeyCode.DOWN) {
                keyEvent.consume();
            }
        });

        URL[] resources = loadResources();

        ImageView newTabImg = new ImageView(new Image(resources[0].toExternalForm()));
        newTabImg.setFitHeight(17);
        newTabImg.setFitWidth(17);
        newTabButton.setGraphic(newTabImg);
        newTabButton.setTooltip(new Tooltip("New tab"));

        ImageView openFileImg = new ImageView(new Image(resources[1].toExternalForm()));
        openFileImg.setFitHeight(17);
        openFileImg.setFitWidth(17);
        openFileButton.setGraphic(openFileImg);
        openFileButton.setTooltip(new Tooltip("Open file"));

        ImageView saveFileImg = new ImageView(new Image(resources[2].toExternalForm()));
        saveFileImg.setFitHeight(17);
        saveFileImg.setFitWidth(17);
        saveFileButton.setGraphic(saveFileImg);
        saveFileButton.setTooltip(new Tooltip("Save file"));

        ImageView runImg = new ImageView(new Image(resources[3].toExternalForm()));
        runImg.setFitHeight(17);
        runImg.setFitWidth(17);
        runButton.setGraphic(runImg);
        runButton.setTooltip(new Tooltip("Run conversion"));

        sourceModesBox.setTooltip(new Tooltip("Source conversion mode"));
        targetModesBox.setTooltip(new Tooltip("Target conversion mode"));

        getItems().add(newTabButton);
        getItems().add(new Separator());
        getItems().addAll(openFileButton, saveFileButton);
        getItems().add(new Separator());
        getItems().add(runButton);
        getItems().add(new Separator());
        getItems().addAll(new Text(" Source:  "), sourceModesBox,
                new Text(" Target:  "), targetModesBox);
    }

    /**
     * @return currently selected source conversion mode
     *
     * @throws IllegalStateException if none of the source modes is currently selected
     */
    public @NotNull String getSourceMode() {
        String currMode = sourceModesBox.getSelectionModel().getSelectedItem();
        if (currMode == null) {
            throw new IllegalStateException("No source mode selected");
        }

        return currMode;
    }

    /**
     * @return currently selected target conversion mode
     *
     * @throws IllegalStateException if none of the target modes is currently selected
     */
    public @NotNull String getTargetMode() {
        String currMode = targetModesBox.getSelectionModel().getSelectedItem();
        if (currMode == null) {
            throw new IllegalStateException("No target mode selected");
        }

        return currMode;
    }

    /**
     * Appends all of the elements from the list to the source modes check box.
     * <p>
     * Selects the first mode from the list as current.
     *
     * @param modes source modes to set
     *
     * @throws IllegalArgumentException if the list of source modes is empty
     */
    public void setSourceModes(@NotNull List<String> modes) {
        if (modes.isEmpty()) {
            throw new IllegalArgumentException("List of source modes is empty");
        }

        sourceModesBox.getItems().addAll(modes);
        sourceModesBox.setValue(sourceModesBox.getItems().get(0));
    }

    /**
     * Appends all of the elements from the list to the target modes check box.
     * <p>
     * Selects the first mode from the list as current.
     *
     * @param modes target modes to set
     *
     * @throws IllegalArgumentException if the list of target modes is empty
     */
    public void setTargetModes(@NotNull List<String> modes) {
        if (modes.isEmpty()) {
            throw new IllegalArgumentException("List of target modes is empty");
        }

        targetModesBox.getItems().addAll(modes);
        targetModesBox.setValue(targetModesBox.getItems().get(0));
    }

    /**
     * Selects the specified source conversion mode as current.
     *
     * @param mode a source mode to select
     *
     * @throws IllegalArgumentException if such a mode does not exist
     */
    public void selectSourceMode(@NotNull String mode) {
        if (!sourceModesBox.getItems().contains(mode)) {
            throw new IllegalArgumentException("Such a mode does not exist: " + mode);
        }

        sourceModesBox.getSelectionModel().select(mode);
    }

    /**
     * Selects the specified target conversion mode as current.
     *
     * @param mode a target mode to select
     *
     * @throws IllegalArgumentException if such a mode does not exist
     */
    public void selectTargetMode(@NotNull String mode) {
        if (!targetModesBox.getItems().contains(mode)) {
            throw new IllegalArgumentException("Such a mode does not exist: " + mode);
        }

        targetModesBox.getSelectionModel().select(mode);
    }

    /**
     * Adds a listener which will be notified when the source conversion mode changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    public void addSourceModeChangeListener(@NotNull ChangeListener<String> listener) {
       sourceModesBox.getSelectionModel().selectedItemProperty().addListener(listener);
    }

    /**
     * Adds a listener which will be notified when the target conversion mode changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    public void addTargetModeChangeListener(@NotNull ChangeListener<String> listener) {
        targetModesBox.getSelectionModel().selectedItemProperty().addListener(listener);
    }

    /**
     * Adds a listener which will be notified when the focus changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    public void addFocusChangeListener(@NotNull ChangeListener<Boolean> listener) {
        newTabButton.focusedProperty().addListener(listener);
        openFileButton.focusedProperty().addListener(listener);
        saveFileButton.focusedProperty().addListener(listener);
        runButton.focusedProperty().addListener(listener);
        sourceModesBox.focusedProperty().addListener(listener);
        targetModesBox.focusedProperty().addListener(listener);
    }

    /**
     * Sets the action which is invoked when the New Tab button is clicked.
     *
     * @param action the action to register
     */
    public void setOnNewTabAction(@NotNull EventHandler<ActionEvent> action) {
        newTabButton.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Open File button is clicked.
     *
     * @param action the action to register
     */
    public void setOnOpenFileAction(@NotNull EventHandler<ActionEvent> action) {
        openFileButton.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Save File button is clicked.
     *
     * @param action the action to register
     */
    public void setOnSaveFileAction(@NotNull EventHandler<ActionEvent> action) {
        saveFileButton.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Run button is clicked.
     *
     * @param action the action to register
     */
    public void setOnRunAction(@NotNull EventHandler<ActionEvent> action) {
        runButton.setOnAction(action);
    }

    private @NotNull URL[] loadResources() {
        URL newTabImgUrl = getClass().getResource("/icons/open-tab.png");
        if (newTabImgUrl == null) {
            String errorMsg = "File not found in application resources: icons/open-tab.png";
            throw new IllegalStateException(errorMsg);
        }

        URL openFileImgUrl = getClass().getResource("/icons/open-file.png");
        if (openFileImgUrl == null) {
            String errorMsg = "File not found in application resources: icons/open-file.png";
            throw new IllegalStateException(errorMsg);
        }

        URL saveFileImgUrl = getClass().getResource("/icons/save-file.png");
        if (saveFileImgUrl == null) {
            String errorMsg = "File not found in application resources: icons/save-file.png";
            throw new IllegalStateException(errorMsg);
        }

        URL runImgUrl = getClass().getResource("/icons/run.png");
        if (runImgUrl == null) {
            String errorMsg = "File not found in application resources: icons/run.png";
            throw new IllegalStateException(errorMsg);
        }

        return new URL[] { newTabImgUrl, openFileImgUrl, saveFileImgUrl, runImgUrl };
    }
}
