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

package com.sqlines.studio.view.settings;

import com.sqlines.studio.view.ErrorWindow;
import com.sqlines.studio.view.Window;
import com.sqlines.studio.view.settings.event.ChangeLicenseEvent;

import org.jetbrains.annotations.NotNull;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.StageStyle;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * The concrete setting window.
 */
public class SettingsWindow extends Window implements SettingsWindowView {
    // General settings
    private final ChoiceBox<String> dirsBox = new ChoiceBox<>();
    private final Button addDirButton = new Button();
    private final Button setDefaultsButton = new Button();
    private final RadioButton saveSessionButton = new RadioButton();

    // Editor settings
    private final ChoiceBox<String> themesBox = new ChoiceBox<>();
    private final RadioButton statusBarButton = new RadioButton();
    private final RadioButton targetFieldButton = new RadioButton();
    private final RadioButton wrappingButton = new RadioButton();
    private final RadioButton highlighterButton = new RadioButton();
    private final RadioButton lineNumbersButton = new RadioButton();

    // License settings
    private final Text licenseInfo = new Text();
    private final TextField regNameField = new TextField();
    private final TextField regNumberField = new TextField();
    private final Button changeButton = new Button();

    private EventHandler<ChangeLicenseEvent> licenseEventHandler;

    public SettingsWindow() {
        TabPane tabPane = new TabPane();
        setRoot(tabPane);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getSelectionModel().selectedIndexProperty().addListener(this::tabIndexChanged);
        tabPane.getTabs().add(new Tab("General", makeGeneralTab()));
        tabPane.getTabs().add(new Tab("Appearance", makeAppearanceTab()));
        tabPane.getTabs().add(new Tab("License", makeLicenseTab()));

        initStyle(StageStyle.UTILITY);
        setTitle("Preferences");
        setWidth(320);
        setHeight(210);
        setResizable(false);
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
    public @NotNull Optional<String> choseDirectoryToAdd() {
        DirectoryChooser chooser = new DirectoryChooser();
        File dir = chooser.showDialog(this);
        if (dir != null) {
            return Optional.of(dir.getAbsolutePath());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void addDirectory(@NotNull String dir) {
        if (dir.isEmpty()) {
            throw new IllegalArgumentException("Working directory is empty");
        }

        dirsBox.getItems().add(dir);
    }

    @Override
    public final void setWorkingDirectories(@NotNull List<String> dirs) {
        if (dirs.isEmpty()) {
            throw new IllegalArgumentException("List of working directories is empty");
        }

        dirsBox.getItems().clear();
        dirsBox.getItems().addAll(dirs);
        dirsBox.getSelectionModel().select(0);
    }

    @Override
    public final void setThemes(@NotNull List<String> themes) {
        if (themes.isEmpty()) {
            throw new IllegalArgumentException("List of themes is empty");
        }

        themesBox.getItems().addAll(themes);
        themesBox.getSelectionModel().select(0);
    }

    @Override
    public void selectDirectory(@NotNull String dir) {
        if (!dirsBox.getItems().contains(dir)) {
            throw new IllegalArgumentException("Such a directory does not exist: " + dir);
        }

        dirsBox.getSelectionModel().select(dir);
    }

    @Override
    public void selectTheme(@NotNull Theme theme) {
        if (theme == Theme.LIGHT) {
            themesBox.getSelectionModel().select(0);
        } else if (theme == Theme.DARK) {
            themesBox.getSelectionModel().select(1);
        }
    }

    @Override
    public void setLicenseInfo(@NotNull String info) {
        licenseInfo.setText(info);
    }

    @Override
    public void setSaveSessionSelected(boolean isSelected) {
        saveSessionButton.setSelected(isSelected);
    }

    @Override
    public void setStatusBarSelected(boolean isSelected) {
        statusBarButton.setSelected(isSelected);
    }

    @Override
    public void setTargetFieldSelected(boolean isSelected) {
        targetFieldButton.setSelected(isSelected);
    }

    @Override
    public void setWrappingSelected(boolean isSelected) {
        wrappingButton.setSelected(isSelected);
    }

    @Override
    public void setHighlighterSelected(boolean isSelected) {
        highlighterButton.setSelected(isSelected);
    }

    @Override
    public void setLineNumbersSelected(boolean isSelected) {
        lineNumbersButton.setSelected(isSelected);
    }

    @Override
    public void addThemeChangeListener(@NotNull ChangeListener<String> listener) {
        themesBox.getSelectionModel().selectedItemProperty().addListener(listener);
    }

    @Override
    public void addDirChangeListener(@NotNull ChangeListener<String> listener) {
        dirsBox.getSelectionModel().selectedItemProperty().addListener(listener);
    }

    @Override
    public void setOnAddDirAction(@NotNull EventHandler<ActionEvent> action) {
        addDirButton.setOnAction(action);
    }

    @Override
    public void setOnSaveSessionAction(@NotNull EventHandler<ActionEvent> action) {
        saveSessionButton.setOnAction(action);
    }

    @Override
    public void setOnSetDefaultsAction(@NotNull EventHandler<ActionEvent> action) {
        setDefaultsButton.setOnAction(action);
    }

    @Override
    public void setOnStatusBarAction(@NotNull EventHandler<ActionEvent> action) {
        statusBarButton.setOnAction(action);
    }

    @Override
    public void setOnTargetFieldAction(@NotNull EventHandler<ActionEvent> action) {
        targetFieldButton.setOnAction(action);
    }

    @Override
    public void setOnWrappingAction(@NotNull EventHandler<ActionEvent> action) {
        wrappingButton.setOnAction(action);
    }

    @Override
    public void setOnHighlighterAction(@NotNull EventHandler<ActionEvent> action) {
        highlighterButton.setOnAction(action);
    }

    @Override
    public void setOnLineNumbersAction(@NotNull EventHandler<ActionEvent> action) {
        lineNumbersButton.setOnAction(action);
    }

    @Override
    public void setOnChangeLicenseAction(@NotNull EventHandler<ChangeLicenseEvent> action) {
        licenseEventHandler = action;
    }

    private @NotNull VBox makeGeneralTab() {
        addDirButton.setText("Add new");
        saveSessionButton.setText("Save last session");
        setDefaultsButton.setText("Set defaults");

        GridPane topLayout = new GridPane();
        topLayout.setHgap(10);
        topLayout.setVgap(10);
        topLayout.add(new Text("Working directory:"), 0, 0);
        topLayout.add(dirsBox, 1, 0);
        topLayout.add(addDirButton, 1, 1);

        VBox mainLayout = new VBox(topLayout, saveSessionButton, setDefaultsButton);
        mainLayout.setPadding(new Insets(10, 15, 10, 15));
        mainLayout.setSpacing(10);

        return mainLayout;
    }

    private @NotNull VBox makeAppearanceTab() {
        statusBarButton.setText("Status bar");
        targetFieldButton.setText("Always show target field");
        wrappingButton.setText("Wrap lines to editor width");
        highlighterButton.setText("Highlighter");
        lineNumbersButton.setText("Line numbers");

        GridPane topLayout = new GridPane();
        topLayout.setVgap(10);
        topLayout.setHgap(10);
        topLayout.add(new Text("Theme:"), 0, 0);
        topLayout.add(themesBox, 1, 0);

        VBox mainLayout = new VBox(topLayout, statusBarButton, targetFieldButton,
                wrappingButton, highlighterButton, lineNumbersButton);
        mainLayout.setPadding(new Insets(10, 15, 10, 15));
        mainLayout.setSpacing(10);

        return mainLayout;
    }

    private @NotNull VBox makeLicenseTab() {
        regNameField.setPromptText("Enter registration name");
        regNumberField.setPromptText("Enter registration number");

        changeButton.setText("Commit change");
        changeButton.setOnAction(event -> {
            ChangeLicenseEvent clickedEvent = new ChangeLicenseEvent(
                    regNameField.getText(), regNumberField.getText()
            );
            changeButton.fireEvent(clickedEvent);
            if (licenseEventHandler != null) {
                licenseEventHandler.handle(clickedEvent);
            }
        });

        VBox topLayout = new VBox(licenseInfo);
        topLayout.setPadding(new Insets(10, 0, 5, 0));

        VBox mainLayout = new VBox(topLayout, regNameField,
                regNumberField, changeButton);
        mainLayout.setPadding(new Insets(10, 15, 10, 15));
        mainLayout.setSpacing(10);

        return mainLayout;
    }

    private void tabIndexChanged(@NotNull ObservableValue<? extends Number> observable,
                                 @NotNull Number oldIndex,
                                 @NotNull Number newIndex) {
        int tabIndex = newIndex.intValue();
        if (tabIndex == 0) { // General settings tab
            setWidth(320);
            setHeight(210);
        } else if (tabIndex == 1) { // Editor settings tab
            setWidth(320);
            setHeight(240);
        } else if (tabIndex == 2) { // License settings tab
            setWidth(320);
            setHeight(220);
        }
    }
}
