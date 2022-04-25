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

package com.sqlines.studio;

import com.sqlines.studio.model.CoreProcess;
import com.sqlines.studio.model.license.License;
import com.sqlines.studio.model.PropertiesLoader;
import com.sqlines.studio.model.ResourceLoader;
import com.sqlines.studio.model.filehandler.FileHandler;
import com.sqlines.studio.model.Converter;
import com.sqlines.studio.model.tabsdata.ObservableTabsData;
import com.sqlines.studio.presenter.MainWindowPresenter;
import com.sqlines.studio.presenter.SettingsPresenter;
import com.sqlines.studio.view.mainwindow.MainWindow;
import com.sqlines.studio.view.settings.SettingsWindow;

import javafx.stage.Stage;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Application extends javafx.application.Application {
    private static final Logger logger = LogManager.getLogger(Application.class);

    private ObservableTabsData tabsData;
    private FileHandler fileHandler;
    private MainWindow mainWindow;
    private Thread fileChecker;
    private Thread licenseChecker;
    private Thread checkpointThread;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        try {
            logger.info("Loading properties");
            PropertiesLoader.loadProperties();
            logger.info("Properties loaded");
        } catch (Exception e) {
            logger.warn("Properties loading failed: " + e.getMessage());
            PropertiesLoader.setDefaults();
        }

        String saveSession = System.getProperty("model.save-session");
        if (saveSession.equals("enabled")) {
            logger.info("Loading last state");
            deserializeObjects();
        } else {
            tabsData = new ObservableTabsData();
            fileHandler = new FileHandler();
            fileHandler.setTabsData(tabsData);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        CoreProcess coreProcess = new CoreProcess();
        Converter converter = new Converter(tabsData, ResourceLoader.loadCmdModes(), coreProcess);
        License license = new License(coreProcess);

        mainWindow = new MainWindow();
        mainWindow.setConversionModes(ResourceLoader.loadSourceModes(), ResourceLoader.loadTargetModes());
        mainWindow.setLightStylesheets(ResourceLoader.loadMainLightStyles());
        mainWindow.setDarkStylesheets(ResourceLoader.loadMainDarkStyles());

        SettingsWindow settingsWindow = new SettingsWindow();
        settingsWindow.setLightStylesheets(ResourceLoader.loadSettingLightStyles());
        settingsWindow.setDarkStylesheets(ResourceLoader.loadSettingDarkStyles());

        SettingsPresenter settingsPresenter = new SettingsPresenter(
                license, settingsWindow, mainWindow, Arrays.asList(mainWindow, settingsWindow)
        );

        MainWindowPresenter mainPresenter = new MainWindowPresenter(
                tabsData, fileHandler, converter, mainWindow
        );

        fileChecker = new Thread(fileHandler, "FileChecker");
        fileChecker.setDaemon(true);
        fileChecker.start();

        licenseChecker = new Thread(license, "LicenseChecker");
        licenseChecker.setDaemon(true);
        licenseChecker.start();

        checkpointThread = new Thread(this::runCheckpointLoop, "CheckpointThread");
        checkpointThread.setDaemon(true);
        checkpointThread.start();
    }

    @Override
    public void stop() throws Exception {
        try {
            fileChecker.interrupt();
            licenseChecker.interrupt();
            checkpointThread.interrupt();

            int tabsNumber = tabsData.countTabs();
            for (int i = 0; i < tabsNumber; i++) {
                if (!tabsData.getSourceFilePath(i).isEmpty()) {
                    fileHandler.saveSourceFile(i);
                }

                if (!tabsData.getTargetFilePath(i).isEmpty()) {
                    fileHandler.saveTargetFile(i);
                }
            }
        } catch (Exception e) {
            logger.error("stop() - " + e.getMessage());
        }

        logger.info("Saving setting");
        saveUISettings();
        PropertiesLoader.saveProperties();
        logger.info("Settings saved");

        logger.info("Saving last state");
        serializeObjects();
        logger.info("Last state saved");
    }

    private void runCheckpointLoop() {
        while (true) {
            try {
                Thread.sleep(40000);
                serializeObjects();
                logger.info("Checkpoint made");
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                logger.error("Checkpoint loop: " + e.getMessage());
            }
        }
    }

    private void serializeObjects() {
        String tabsDataPath = System.getProperty("java.io.tmpdir") + "sqlines-tabsdata.serial";
        String fileHandlerPath = System.getProperty("java.io.tmpdir") + "sqlines-filehandler.serial";
        try (ObjectOutputStream tabsDataStream = new ObjectOutputStream(new FileOutputStream(tabsDataPath));
             ObjectOutputStream fileHandlerStream = new ObjectOutputStream(new FileOutputStream(fileHandlerPath))) {
            tabsDataStream.writeObject(tabsData);
            fileHandlerStream.writeObject(fileHandler);
        } catch (Exception e) {
            logger.warn("init() - Serialization error: " + e.getMessage());
        }
    }

    private void deserializeObjects() {
        String tabsDataPath = System.getProperty("java.io.tmpdir") + "sqlines-tabsdata.serial";
        String fileHandlerPath = System.getProperty("java.io.tmpdir") + "sqlines-filehandler.serial";
        try (ObjectInputStream tabsDataStream = new ObjectInputStream(new FileInputStream(tabsDataPath));
             ObjectInputStream fileHandlerStream = new ObjectInputStream(new FileInputStream(fileHandlerPath))) {
            tabsData = (ObservableTabsData) tabsDataStream.readObject();
            fileHandler = (FileHandler) fileHandlerStream.readObject();
        } catch (Exception e) {
            logger.warn("Deserialization error: " + e.getMessage());
            tabsData = new ObservableTabsData();
            fileHandler = new FileHandler();
        } finally {
            fileHandler.setTabsData(tabsData);
        }
    }

    private void saveUISettings() {
        System.setProperty("view.height", String.valueOf(mainWindow.getHeight()));
        System.setProperty("view.width", String.valueOf(mainWindow.getWidth()));
        System.setProperty("view.pos.x", String.valueOf(mainWindow.getX()));
        System.setProperty("view.pos.y", String.valueOf(mainWindow.getY()));
        System.setProperty("view.isMaximized", String.valueOf(mainWindow.isMaximized()));
    }
}
