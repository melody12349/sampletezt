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

import com.sqlines.studio.model.license.License;
import com.sqlines.studio.model.PropertiesLoader;
import com.sqlines.studio.view.Window;
import com.sqlines.studio.view.mainwindow.MainWindowSettingsView;
import com.sqlines.studio.view.settings.event.ChangeLicenseEvent;
import com.sqlines.studio.view.settings.SettingsWindowView;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Responds to user actions in the settings window.
 * Retrieves data from the model, and displays it in the settings window.
 */
public class SettingsPresenter {
    private static final Logger logger = LogManager.getLogger(SettingsPresenter.class);
    private static final Properties properties = System.getProperties();

    private final License license;
    private final SettingsWindowView settingsWindow;
    private final MainWindowSettingsView mainWindow;
    private final List<? extends Window> windows;

    /**
     * Constructs a new  SettingsPresenter.
     *
     * @param license license
     * @param settingsWindowView settings window
     * @param mainWindowView main window
     * @param windows list of all application windows
     */
    public SettingsPresenter(@NotNull License license,
                             @NotNull SettingsWindowView settingsWindowView,
                             @NotNull MainWindowSettingsView mainWindowView,
                             @NotNull List<? extends Window> windows) {
        this.license = license;
        this.settingsWindow = settingsWindowView;
        this.mainWindow = mainWindowView;
        this.windows = windows;

        settingsWindow.setThemes(List.of("Light", "Dark"));
        settingsWindow.setWorkingDirectories(List.of("Home"));

        initHandlers();
        loadProperties();
        checkLicense();
    }

    private void initHandlers() {
        license.addLicenseListener(this::licenseChanged);

        mainWindow.setOnPreferencesAction(event -> showSettingsWindow());
        mainWindow.setOnTabCloseAction(event -> windows.forEach(Window::close));

        settingsWindow.addThemeChangeListener(this::themeChanged);
        settingsWindow.addDirChangeListener(this::workingDirChanged);
        settingsWindow.setOnAddDirAction(event -> addDirPressed());
        settingsWindow.setOnSaveSessionAction(event -> saveLastSessionPressed());
        settingsWindow.setOnSetDefaultsAction(event -> setDefaultsPressed());
        settingsWindow.setOnChangeLicenseAction(this::changeLicensePressed);

        EventHandler<ActionEvent> changeStatusBarPolicy = event -> changeStatusBarPolicyPressed();
        mainWindow.setOnStatusBarAction(changeStatusBarPolicy);
        settingsWindow.setOnStatusBarAction(changeStatusBarPolicy);

        EventHandler<ActionEvent> changeTargetFieldPolicy = event -> changeTargetFieldPolicyPressed();
        mainWindow.setOnTargetFieldAction(changeTargetFieldPolicy);
        settingsWindow.setOnTargetFieldAction(changeTargetFieldPolicy);

        EventHandler<ActionEvent> changeWrappingPolicy = event -> changeWrappingPolicyPressed();
        mainWindow.setOnWrappingAction(changeWrappingPolicy);
        settingsWindow.setOnWrappingAction(changeWrappingPolicy);

        EventHandler<ActionEvent> changeHighlighterPolicy = event -> changeHighlighterPolicePressed();
        mainWindow.setOnHighlighterAction(changeHighlighterPolicy);
        settingsWindow.setOnHighlighterAction(changeHighlighterPolicy);

        EventHandler<ActionEvent> changeLineNumbersPolicy = event -> changeLineNumbersPolicyPressed();
        mainWindow.setOnLineNumbersAction(changeLineNumbersPolicy);
        settingsWindow.setOnLineNumbersAction(changeLineNumbersPolicy);
    }

    private void loadProperties() {
        try {
            loadWindowProperties();
            loadSessionSaving();
            loadDirectories();
            loadTheme();
            loadStatusBarSetting();
            loadTargetFieldSetting();
            loadWrappingSetting();
            loadHighlighterSetting();
            loadLineNumbersSetting();
        } catch (Exception e) {
            logger.error("Loading properties: " + e.getMessage());
            PropertiesLoader.setDefaults();
            loadProperties();
        }
    }

    private void loadWindowProperties() {
        String heightProperty = properties.getProperty("view.height", "650.0");
        double height = Double.parseDouble(heightProperty);
        mainWindow.setHeight(height);

        String widthProperty = properties.getProperty("view.width", "770.0");
        double width = Double.parseDouble(widthProperty);
        mainWindow.setWidth(width);

        String posXProperty = properties.getProperty("view.pos.x", "0.0");
        double posX = Double.parseDouble(posXProperty);
        mainWindow.setX(posX);

        String posYProperty = properties.getProperty("view.pos.y", "0.0");
        double posY = Double.parseDouble(posYProperty);
        mainWindow.setY(posY);

        String isMaximized = properties.getProperty("view.is-maximized", "false");
        if (isMaximized.equals("true")) {
            mainWindow.setFullScreen(true);
        } else if (isMaximized.equals("false")) {
            mainWindow.setFullScreen(false);
        }
    }

    private void loadSessionSaving() {
        String saveSession = properties.getProperty("model.save-session", "enabled");
        if (saveSession.equals("enabled")) {
            settingsWindow.setSaveSessionSelected(true);
        } else if (saveSession.equals("disabled")) {
            settingsWindow.setSaveSessionSelected(false);
        }
    }

    private void loadDirectories() {
        int dirsNumber = Integer.parseInt(properties.getProperty("model.dirs-number", "0"));
        for (int i = 0; i < dirsNumber; i++) {
            String dir = properties.getProperty("model.dir-" + i);
            settingsWindow.addDirectory(dir);
        }

        String currDir = properties.getProperty("model.curr-dir", "Home");
        if (currDir.equals(properties.getProperty("user.home"))) {
            currDir = "Home";
        }

        settingsWindow.selectDirectory(currDir);
    }

    private void loadTheme() {
        String theme = properties.getProperty("view.theme", "light");
        if (theme.equals("light")) {
            settingsWindow.selectTheme(Window.Theme.LIGHT);
            windows.forEach(window -> window.setTheme(Window.Theme.LIGHT));
        } else if (theme.equals("dark")) {
            settingsWindow.selectTheme(Window.Theme.DARK);
            windows.forEach(window -> window.setTheme(Window.Theme.DARK));
        }
    }

    private void loadStatusBarSetting() {
        String showStatusBar = properties.getProperty("view.status-bar", "show");
        if (showStatusBar.equals("show")) {
            mainWindow.setStatusBarPolicy(MainWindowSettingsView.StatusBarPolicy.SHOW);
            settingsWindow.setStatusBarSelected(true);
        } else if (showStatusBar.equals("do-not-show")) {
            mainWindow.setStatusBarPolicy(MainWindowSettingsView.StatusBarPolicy.DO_NOT_SHOW);
            settingsWindow.setStatusBarSelected(false);
        }
    }

    private void loadTargetFieldSetting() {
        String showTargetField = properties.getProperty("view.target-field", "as-needed");
        if (showTargetField.equals("always")) {
            mainWindow.setTargetFieldPolicy(MainWindowSettingsView.TargetFieldPolicy.ALWAYS);
            settingsWindow.setTargetFieldSelected(true);
        } else if (showTargetField.equals("as-needed")) {
            mainWindow.setTargetFieldPolicy(MainWindowSettingsView.TargetFieldPolicy.AS_NEEDED);
            settingsWindow.setTargetFieldSelected(false);
        }
    }

    private void loadWrappingSetting() {
        String enableWrapping = properties.getProperty("view.wrapping", "enabled");
        if (enableWrapping.equals("enabled")) {
            mainWindow.setWrappingPolicy(MainWindowSettingsView.WrappingPolicy.WRAP_LINES);
            settingsWindow.setWrappingSelected(true);
        } else if (enableWrapping.equals("disabled")) {
            mainWindow.setWrappingPolicy(MainWindowSettingsView.WrappingPolicy.NO_WRAP);
            settingsWindow.setWrappingSelected(false);
        }
    }

    private void loadHighlighterSetting() {
        String enableHighlighter = properties.getProperty("view.highlighter", "enabled");
        if (enableHighlighter.equals("enabled")) {
            mainWindow.setHighlighterPolicy(MainWindowSettingsView.HighlighterPolicy.HIGHLIGHT);
            settingsWindow.setHighlighterSelected(true);
        } else if (enableHighlighter.equals("disabled")) {
            mainWindow.setHighlighterPolicy(MainWindowSettingsView.HighlighterPolicy.DO_NOT_HIGHLIGHT);
            settingsWindow.setHighlighterSelected(false);
        }
    }

    private void loadLineNumbersSetting() {
        String lineNumbersProperty = properties.getProperty("view.line-numbers", "enabled");
        if (lineNumbersProperty.equals("enabled")) {
            mainWindow.setLineNumbersPolicy(MainWindowSettingsView.LineNumbersPolicy.SHOW);
            settingsWindow.setLineNumbersSelected(true);
        } else if (lineNumbersProperty.equals("disabled")) {
            mainWindow.setLineNumbersPolicy(MainWindowSettingsView.LineNumbersPolicy.DO_NOT_SHOW);
            settingsWindow.setLineNumbersSelected(false);
        }
    }

    private void checkLicense() {
        try {
            if (license.isActive()) {
                mainWindow.setWindowTitle("SQLines Studio");
                settingsWindow.setLicenseInfo("License: Active");
            } else {
                mainWindow.setWindowTitle("SQLINES STUDIO - FOR EVALUATION USE ONLY");
                settingsWindow.setLicenseInfo("License: For evaluation use only");
            }
        } catch (Exception e) {
            mainWindow.setWindowTitle("SQLINES STUDIO - FOR EVALUATION USE ONLY");
            settingsWindow.setLicenseInfo("License: For evaluation use only");
            logger.error("License check: " + e.getMessage());
        }
    }

    private void showSettingsWindow() {
        if (!settingsWindow.isShowing()) {
            settingsWindow.show();
        } else {
            settingsWindow.toFront();
        }
    }

    private void licenseChanged(boolean isActive) {
        Platform.runLater(() -> {
            if (isActive) {
                mainWindow.setWindowTitle("SQLines Studio");
                settingsWindow.setLicenseInfo("License: Active");
            } else {
                mainWindow.setWindowTitle("SQLINES STUDIO - FOR EVALUATION USE ONLY");
                settingsWindow.setLicenseInfo("License: For evaluation use only");
            }
        });
    }

    private void themeChanged(@NotNull ObservableValue<? extends String> observable,
                              String oldTheme,
                              @NotNull String newTheme) {
        try {
            if (newTheme.equals("Light")) {
                properties.setProperty("view.theme", "light");
                windows.forEach(window -> window.setTheme(Window.Theme.LIGHT));
                logger.info("Theme changed. New theme - light");
            } else if (newTheme.equals("Dark")) {
                properties.setProperty("view.theme", "dark");
                windows.forEach(window -> window.setTheme(Window.Theme.DARK));
                logger.info("Theme changed. New theme - dark");
            }

            PropertiesLoader.saveProperties();
        } catch (Exception e) {
            String errorMsg = "An error occurred while " +
                    "saving the settings.\n" + e.getMessage();
            logger.error(errorMsg);
            settingsWindow.showError("Error", errorMsg);
        }
    }

    private void workingDirChanged(@NotNull ObservableValue<? extends String> observable,
                                   String oldDir,
                                   @NotNull String newDir) {
        try {
            properties.setProperty("model.curr-dir", newDir);
            PropertiesLoader.saveProperties();
            logger.info("Working dir changed: " + newDir);
        } catch (Exception e) {
            String errorMsg = "An error occurred while " +
                    "saving the settings.\n" + e.getMessage();
            logger.error(errorMsg);
            settingsWindow.showError("Error", errorMsg);
        }
    }

    private void addDirPressed() {
        try {
            Optional<String> dir = settingsWindow.choseDirectoryToAdd();
            if (dir.isPresent()) {
                String dirPath = dir.get();
                properties.setProperty("model.curr-dir", dirPath);
                int dirsNumber = Integer.parseInt(properties.getProperty("model.dirs-number", "0"));
                properties.setProperty("model.dir-" + dirsNumber, dirPath);
                properties.setProperty("model.dirs-number", String.valueOf((dirsNumber + 1)));

                settingsWindow.addDirectory(dirPath);
                settingsWindow.selectDirectory(dirPath);

                PropertiesLoader.saveProperties();
                logger.info("New dir added: " + dir.get());
            }
        } catch (Exception e) {
            String errorMsg = "An error occurred while " +
                    "saving the settings.\n" + e.getMessage();
            logger.error(errorMsg);
            settingsWindow.showError("Error", errorMsg);
        }
    }

    private void saveLastSessionPressed() {
        try {
            String policy = properties.getProperty("model.save-session");
            if (policy.equals("enabled")) {
                properties.setProperty("model.save-session", "disabled");
            } else if (policy.equals("disabled")) {
                properties.setProperty("model.save-session", "enabled");
            }

            PropertiesLoader.saveProperties();
        } catch (Exception e) {
            String errorMsg = "An error occurred while " +
                    "saving the settings.\n" + e.getMessage();
            logger.error(errorMsg);
            settingsWindow.showError("Error", errorMsg);
        }
    }

    private void setDefaultsPressed() {
        try {
            PropertiesLoader.setDefaults();
            loadProperties();
            PropertiesLoader.saveProperties();
            logger.info("Default settings set");
        } catch (Exception e) {
            String errorMsg = "An error occurred while " +
                    "saving the settings.\n" + e.getMessage();
            logger.error(errorMsg);
            settingsWindow.showError("Error", errorMsg);
        }
    }

    private void changeLicensePressed(@NotNull ChangeLicenseEvent event) {
        try {
            license.changeLicense(event.getRegName(), event.getRegNumber());
            logger.info("License status changed");
        } catch (Exception e) {
            settingsWindow.showError("Change license status: ", e.getMessage());
        }
    }

    private void changeStatusBarPolicyPressed() {
        try {
            String policy = properties.getProperty("view.status-bar", "show");
            if (policy.equals("show")) {
                properties.setProperty("view.status-bar", "do-not-show");
                mainWindow.setStatusBarPolicy(MainWindowSettingsView.StatusBarPolicy.DO_NOT_SHOW);
                settingsWindow.setStatusBarSelected(false);
                logger.info("Status bar policy changed. New policy: Do not show");
            } else if (policy.equals("do-not-show")) {
                properties.setProperty("view.status-bar", "show");
                mainWindow.setStatusBarPolicy(MainWindowSettingsView.StatusBarPolicy.SHOW);
                settingsWindow.setStatusBarSelected(true);
                logger.info("Status bar policy changed. New policy: Show");
            }

            PropertiesLoader.saveProperties();
        } catch (Exception e) {
            String errorMsg = "An error occurred while " +
                    "saving the settings.\n" + e.getMessage();
            logger.error(errorMsg);
            settingsWindow.showError("Error", errorMsg);
        }
    }

    private void changeTargetFieldPolicyPressed() {
        try {
            String policy = properties.getProperty("view.target-field", "as-needed");
            if (policy.equals("always")) {
                properties.setProperty("view.target-field", "as-needed");
                mainWindow.setTargetFieldPolicy(MainWindowSettingsView.TargetFieldPolicy.AS_NEEDED);
                settingsWindow.setTargetFieldSelected(false);
                logger.info("Target field policy changed. New policy: As needed");
            } else if (policy.equals("as-needed")) {
                properties.setProperty("view.target-field", "always");
                mainWindow.setTargetFieldPolicy(MainWindowSettingsView.TargetFieldPolicy.ALWAYS);
                settingsWindow.setTargetFieldSelected(true);
                logger.info("Target field policy changed. New policy: Always");
            }

            PropertiesLoader.saveProperties();
        } catch (Exception e) {
            String errorMsg = "An error occurred while " +
                    "saving the settings.\n" + e.getMessage();
            logger.error(errorMsg);
            settingsWindow.showError("Error", errorMsg);
        }
    }

    private void changeWrappingPolicyPressed() {
        try {
            String policy = properties.getProperty("view.wrapping", "enabled");
            if (policy.equals("enabled")) {
                properties.setProperty("view.wrapping", "disabled");
                mainWindow.setWrappingPolicy(MainWindowSettingsView.WrappingPolicy.NO_WRAP);
                settingsWindow.setWrappingSelected(false);
                logger.info("Wrapping policy changed. New policy: No wrap");
            } else if (policy.equals("disabled")) {
                properties.setProperty("view.wrapping", "enabled");
                mainWindow.setWrappingPolicy(MainWindowSettingsView.WrappingPolicy.WRAP_LINES);
                settingsWindow.setWrappingSelected(true);
                logger.info("Wrapping policy changed. New policy: Wrap lines");
            }

            PropertiesLoader.saveProperties();
        } catch (Exception e) {
            String errorMsg = "An error occurred while " +
                    "saving the settings.\n" + e.getMessage();
            logger.error(errorMsg);
            settingsWindow.showError("Error", errorMsg);
        }
    }

    public void changeHighlighterPolicePressed() {
        try {
            String policy = properties.getProperty("view.highlighter", "enabled");
            if (policy.equals("enabled")) {
                properties.setProperty("view.highlighter", "disabled");
                mainWindow.setHighlighterPolicy(MainWindowSettingsView.HighlighterPolicy.DO_NOT_HIGHLIGHT);
                settingsWindow.setHighlighterSelected(false);
                logger.info("Highlighter policy changed. New policy: Do not highlight");
            } else if (policy.equals("disabled")) {
                properties.setProperty("view.highlighter", "enabled");
                mainWindow.setHighlighterPolicy(MainWindowSettingsView.HighlighterPolicy.HIGHLIGHT);
                settingsWindow.setHighlighterSelected(true);
                logger.info("Highlighter policy changed. New policy: Highlight");
            }

            PropertiesLoader.saveProperties();
        } catch (Exception e) {
            String errorMsg = "An error occurred while " +
                    "saving the settings.\n" + e.getMessage();
            logger.error(errorMsg);
            settingsWindow.showError("Error", errorMsg);
        }
    }

    private void changeLineNumbersPolicyPressed() {
        try {
            String policy = properties.getProperty("view.line-numbers", "enabled");
            if (policy.equals("enabled")) {
                properties.setProperty("view.line-numbers", "disabled");
                mainWindow.setLineNumbersPolicy(MainWindowSettingsView.LineNumbersPolicy.DO_NOT_SHOW);
                settingsWindow.setLineNumbersSelected(false);
                logger.info("Line numbers area policy changed. New policy: Do not show");
            } else if (policy.equals("disabled")) {
                properties.setProperty("view.line-numbers", "enabled");
                mainWindow.setLineNumbersPolicy(MainWindowSettingsView.LineNumbersPolicy.SHOW);
                settingsWindow.setLineNumbersSelected(true);
                logger.info("Line numbers area policy changed. New policy: Show");
            }
            PropertiesLoader.saveProperties();
        } catch (Exception e) {
            String errorMsg = "An error occurred while " +
                    "saving the settings.\n" + e.getMessage();
            logger.error(errorMsg);
            settingsWindow.showError("Error", errorMsg);
        }
    }
}
