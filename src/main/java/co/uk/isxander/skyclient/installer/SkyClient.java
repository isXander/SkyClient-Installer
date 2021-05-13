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
import java.text.SimpleDateFormat;
import java.util.Calendar;

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

        // no, not a token logger
        File launcherProfilesFile = new File(mcDir, "launcher_profiles.json");
        BetterJsonObject launcherProfilesJson = BetterJsonObject.getFromFile(launcherProfilesFile);
        String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(Calendar.getInstance().getTime());

        BetterJsonObject profile = new BetterJsonObject();
        profile.addProperty("creator", currentDate);
        profile.addProperty("icon", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAARCSURBVFhHvZfPa1RXFMfvG7WVFkpH0bQiGNSaWQi6C6hBod2VLuzCdpMutCD9gZRGV+J/EKFRsApaaLJpS6V0UcimVJLSkoWQQAuTVNs0FW2iNCJVMSrP9zlzv8/z3jyNbvzA4/54957zPefcmbmThEUY+OODlPbAxhO2NkkW3fJU1GJbSZqa73D07xfCsQsf2oA5zT8tCsbzSAE4wSnO4dgPC2HjwPu5gbKIr8Yns6kWcaoA09goi8gFsMA/4rsNd2IvK8Obz8Xew1Jka9Pxudm0seZlGwNzsWswXPfjwZT9O5YuibMtTEBpvUH0WiwRROBLgWP6W1attmfi6hxDQ+8A58t+ux5HRSpLwIbYNfZ8fzvc3dyKEBFV4Lx5+bq1PJmgttNajh5qVdGjVrUHn3pQFrau7ki+npiyOZy/u7UrYY5HJfI1//nefXs8bRnwB83Tt+6WtV4M4nEKtHE65/xfH+dlBGXPiyoI0EEB1d0rVhkAoYqyCu/E179cwoIARSlHOC9vsIMY51Q+Wu/QR15OOXibuQCiZ5Pf6M8BSCAoUzhnL2sRgfPdF5+3d+CdEZj2bfhsnwnOBcg4jr2jKmSEw+jPDM4UcVXkgFAv3gQoekHfR1+OQjCvMaIxrH20iJAzv08gvoZzHzHOvXoZ8GBcojhgfn/VF463wVofUOEQCkUhw96AHCyd2RfWLj9kz/ilIzbnRYHPhihns6bFi9UOZOj1JX1h21ud1oc/J2+EE5/3xtGjqcpO7eInZxKp4ivXq6Wvd7QyMDJxJHyx61Xrr+96KQy8XQ/vrfwp7L5UtzmhDLKv7JzoZ944mlgJ2PjPqRfDmrNT+eKdX87ZAzjfdn4h7L25vnCYcAz8EEG9Xjdbfs3/36y1fUL9PcOtL7HCGejs7DQRLGo0GqGjo8MM4rzMrlOT1pKJRt+v1gdEwP6xFeZcYEfOvT0TMDs7awPo7u4O8/Pz1pcxoXkZGPj9fth77or19a7ZbFp0Gnt4J/r7+y0FJmBoaChBxPT0NENDixFBNgTG/ZgMcCDHxsYqnQ4PD1fOi7aPobKBGK+4Ck6/MgCI8Pj9ssscAdsgIxfgJ70IqSdqsuHLwidAjIyM2H7ODfiMens+y1DIwOjoaFJeAD6Fiurfqf+sJQuC/XImEDY4OBh4jp/9Nnx0/HR806KtBF4Em71BhBDh40pDJikFNnCWXc/yHys+rlxe/X2xTQAvM8c4zzcj4nFOfRaAIHD+zpZN+XeE4OrGXPZLWPw5hqm5hVQb4sXSUicQwkN0+ir258Djr+kCm4jy5AKI/LVVy6yvi6ZAhMqi9toFa9qiF5n4/MKKYx7dmqHsw/7ZIEL4sWrW09OT8vT29qY7TzbtXdenv6hvdp4Evzb/6AFOSZ2u2HG6DQTMbD9sfT4Nr2xaEc7tt9uxzT0TCJnMkIFIfPMM8KUqEVc8KSE8AJC1I56Vh+j7AAAAAElFTkSuQmCC");
        profile.addProperty("lastUsed", currentDate);
        profile.addProperty("lastVersionId", forgeVersion);
        profile.addProperty("name", "SkyClient");
        profile.addProperty("type", "custom");

        launcherProfilesJson.getObj("profiles").add("SkyClient", profile);
        launcherProfilesJson.writeToFile(launcherProfilesFile);
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
