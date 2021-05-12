package co.uk.isxander.skyclient.installer;

import co.uk.isxander.skyclient.installer.gui.MainGui;
import co.uk.isxander.skyclient.installer.repo.RepositoryManager;
import co.uk.isxander.skyclient.installer.repo.entry.ModEntry;
import co.uk.isxander.skyclient.installer.repo.entry.PackEntry;
import co.uk.isxander.skyclient.installer.utils.Log;
import co.uk.isxander.skyclient.installer.utils.OSChecker;
import co.uk.isxander.xanderlib.utils.HttpsUtils;
import co.uk.isxander.xanderlib.utils.json.BetterJsonObject;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class SkyClient {

    private static SkyClient instance;

    private File mcDir;
    private String scPath;
    private final RepositoryManager repositoryManager;
    private MainGui mainGui;

    public SkyClient() {
        OSChecker.OSType type = OSChecker.getOperatingSystemType();
        if (type == OSChecker.OSType.WINDOWS) {
            mcDir = new File(new File(System.getenv("APPDATA")), ".minecraft");
        } else if (type == OSChecker.OSType.LINUX) {
            mcDir = new File("~/.minecraft");
        } else if (type == OSChecker.OSType.OS_X) {
            mcDir = new File("~/Library/Application Support/Minecraft");
        } else {
            Log.err("OS type is not supported. Cannot continue.");
            JOptionPane.showMessageDialog(null, "Your OS type is not supported by SkyClient (Java Edition).", "Fatal Error", JOptionPane.ERROR_MESSAGE);
            throw new IllegalStateException("OS type is not supported.");
        }
        scPath = "skyclient";

        this.repositoryManager = new RepositoryManager();
        mainGui = new MainGui(this);
    }

    public void install() throws IOException {
        RepositoryManager repo = getRepositoryManager();

        if (!mcDir.exists()) {
            JOptionPane.showMessageDialog(null, "Could not find your specified minecraft data folder.", "Failure", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // make directories
        File scDir = getScDir();
        scDir.mkdirs();
        File versionsDir = new File(getMcDir(), "versions");
        File modsDir = new File(scDir, "mods");
        File packsDir = new File(scDir, "resourcepacks");
        versionsDir.mkdirs();
        modsDir.mkdirs();
        packsDir.mkdirs();

        String forgeVersion = "1.8.9-forge1.8.9-11.15.1.2318-1.8.9";
        // create version
        File versionJsonFile = new File(versionsDir, forgeVersion + "/" + forgeVersion + ".json");
        if (!versionJsonFile.exists()) {
            versionJsonFile.getParentFile().mkdirs();
            HttpsUtils.downloadFile(RepositoryManager.FORGE_VERSION_JSON, versionJsonFile);
        }

        // download forge jar
        File forgeJarFile = new File(mcDir, "libraries/net/minecraftforge/forge/1.8.9-11.15.1.2318-1.8.9/forge-1.8.9-11.15.1.2318-1.8.9.jar");
        if (!forgeJarFile.exists()) {
            forgeJarFile.getParentFile().mkdirs();
            HttpsUtils.downloadFile(RepositoryManager.FORGE_VERSION_JAR, forgeJarFile);
        }

        for (ModEntry mod : repo.getModEntries()) {
            if (!mod.isEnabled()) continue;

            if (!mod.getDownloadUrl().startsWith("https://")) {
                Log.warn("Tried to download invalid URL: " + mod.getDownloadUrl());
                continue;
            }
            HttpsUtils.downloadFile(mod.getDownloadUrl(), new File(modsDir, mod.getFileName()));

            for (String requiredFileName : mod.getFiles()) {
                File requiredFile = new File(scDir, requiredFileName);
                requiredFile.getParentFile().mkdirs();

                HttpsUtils.downloadFile(RepositoryManager.MC_DIR_URL + requiredFileName, requiredFile);
            }
        }

        for (PackEntry pack : repo.getPackEntries()) {
            if (!pack.isEnabled()) continue;

            if (!pack.getDownloadUrl().startsWith("https://")) {
                Log.warn("Tried to download invalid URL: " + pack.getDownloadUrl());
                continue;
            }
            HttpsUtils.downloadFile(pack.getDownloadUrl(), new File(packsDir, pack.getFileName()));
        }
    }

    public void setMcDir(File newMc) {
        this.mcDir = newMc;
    }

    public File getScDir() {
        return new File(mcDir, scPath);
    }

    public MainGui getMainGui() {
        return mainGui;
    }

    public File getMcDir() {
        return mcDir;
    }

    public RepositoryManager getRepositoryManager() {
        return repositoryManager;
    }

    public static SkyClient getInstance() {
        if (instance == null)
            instance = new SkyClient();

        return instance;
    }

}
