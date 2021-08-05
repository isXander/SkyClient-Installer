package dev.isxander.skyclient.installer.repo;

import dev.isxander.skyclient.installer.gui.MainGui;
import dev.isxander.skyclient.installer.repo.entry.EntryAction;
import dev.isxander.skyclient.installer.repo.entry.EntryWarning;
import dev.isxander.skyclient.installer.repo.entry.ModEntry;
import dev.isxander.skyclient.installer.repo.entry.PackEntry;
import dev.isxander.skyclient.installer.utils.UpdateHook;
import dev.isxander.xanderlib.utils.HttpsUtils;
import dev.isxander.xanderlib.utils.Multithreading;
import dev.isxander.xanderlib.utils.json.BetterJsonObject;
import com.google.gson.*;
import dev.isxander.skyclient.installer.utils.Log;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RepositoryManager {

    public static final String MODS_JSON_URL = "https://raw.githubusercontent.com/nacrt/SkyblockClient-REPO/main/files/mods.json";
    public static final String PACKS_JSON_URL = "https://raw.githubusercontent.com/nacrt/SkyblockClient-REPO/main/files/packs.json";
    public static final String ICONS_DIR_URL = "https://raw.githubusercontent.com/nacrt/SkyblockClient-REPO/main/files/icons/";
    public static final String MC_DIR_URL = "https://raw.githubusercontent.com/nacrt/SkyblockClient-REPO/main/files/mcdir/";
    public static final String PACKS_DIR_URL = "https://raw.githubusercontent.com/nacrt/SkyblockClient-REPO/main/files/packs/";

    public static final String FORGE_VERSION_JSON = "https://raw.githubusercontent.com/nacrt/SkyblockClient-REPO/main/files/forge/1.8.9-forge1.8.9-11.15.1.2318-1.8.9.json";
    public static final String FORGE_VERSION_JAR = "https://raw.githubusercontent.com/nacrt/SkyblockClient-REPO/main/files/forge/forge-1.8.9-11.15.1.2318-1.8.9.jar";

    public static final File CACHE_FOLDER = new File(new File(System.getProperty("user.home")), ".skyclient/");
    public static final long CACHE_TIME = TimeUnit.DAYS.toMillis(1);
    public static final File ICON_FOLDER = new File(CACHE_FOLDER, "icons");

    private final List<ModEntry> modEntries;
    private final List<PackEntry> packEntries;

    private final Map<String, BufferedImage> imageCache;
    private final BufferedImage unknownImage;

    public RepositoryManager() {
        this.modEntries = new ArrayList<>();
        this.packEntries = new ArrayList<>();
        this.imageCache = new HashMap<>();

        try {
            this.unknownImage = ImageIO.read(RepositoryManager.class.getResourceAsStream("/unknown.png"));
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Could not read unknown image.");
        }
    }

    public void fetchFiles() {
        // check if we need to refresh icons and stuff
        boolean refresh = shouldRefreshCache();
        if (refresh) {
            if (!CACHE_FOLDER.exists()) {
                CACHE_FOLDER.mkdirs();
            }
        }

        // get json from web
        JsonArray modsArr = JsonParser.parseString(HttpsUtils.getString(MODS_JSON_URL)).getAsJsonArray();
        JsonArray packsArr = JsonParser.parseString(HttpsUtils.getString(PACKS_JSON_URL)).getAsJsonArray();

        // Loop thru every element in the array
        for (JsonElement element : modsArr) {
            // Check if element is an object so we don't run into any weird errors
            if (!element.isJsonObject()) {
                Log.warn("Mods JSON included non-json-object.");
                continue;
            }

            // convert the element into a json object
            BetterJsonObject modJson = new BetterJsonObject(element.getAsJsonObject());

            // find all required packs and add them to array
            String[] packs = new String[0];
            if (modJson.has("packs")) {
                JsonArray packArray = modJson.get("packs").getAsJsonArray();
                packs = new String[packArray.size()];
                int i = 0;
                for (JsonElement packIdElement : packArray) {
                    packs[i] = packIdElement.getAsString();
                    i++;
                }
            }

            // find all required mods and add them to array
            String[] mods = new String[0];
            if (modJson.has("packages")) {
                JsonArray modArray = modJson.get("packages").getAsJsonArray();
                mods = new String[modArray.size()];
                int i = 0;
                for (JsonElement modIdElement : modArray) {
                    mods[i] = modIdElement.getAsString();
                    i++;
                }
            }

            // find all required files and add them to array
            String[] files = new String[0];
            if (modJson.has("files")) {
                JsonArray fileArray = modJson.get("files").getAsJsonArray();
                files = new String[fileArray.size()];
                int i = 0;
                for (JsonElement fileIdElement : fileArray) {
                    files[i] = fileIdElement.getAsString();
                    i++;
                }
            }

            // find all actions and add them to array
            EntryAction[] actions = new EntryAction[0];
            if (modJson.has("actions")) {
                JsonArray actionsArr = modJson.get("actions").getAsJsonArray();
                List<EntryAction> actionList = new ArrayList<>(actionsArr.size());
                for (JsonElement actionElement : actionsArr) {
                    BetterJsonObject actionObj = new BetterJsonObject(actionElement.getAsJsonObject());

                    String text;
                    Runnable action;
                    if (actionObj.has("document")) {
                        text = "Guide (Built-In)";
                        action = () -> {
                            try {
                                Desktop.getDesktop().browse(new URI(actionObj.optString("document")));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        };
                    } else {
                        text = actionObj.optString("text");
                        action = () -> {
                            try {
                                Desktop.getDesktop().browse(new URI(actionObj.optString("link")));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        };
                    }
                    actionList.add(new EntryAction(
                            text,
                            actionObj.optString("creator"),
                            action
                    ));
                }
                actions = actionList.toArray(new EntryAction[0]);
            }

            // find warning
            EntryWarning warning = null;
            if (modJson.has("warning")) {
                JsonArray lineArr = modJson.get("warning").getAsJsonObject().get("lines").getAsJsonArray();
                List<String> lineList = new ArrayList<>();
                for (JsonElement lineElement : lineArr) {
                    lineList.add(lineElement.getAsString());
                }
                warning = new EntryWarning(lineList);
            }

            // finally create the entry
            modEntries.add(new ModEntry(
                    modJson.optString("id"),
                    modJson.optBoolean("enabled", false),
                    modJson.optString("file"),
                    modJson.optString("url"),
                    modJson.optString("display"),
                    modJson.optString("description"),
                    modJson.optString("icon"),
                    modJson.optString("icon_scaling", "smooth"),
                    modJson.optString("creator", "Unknown"),
                    packs,
                    mods,
                    actions,
                    warning,
                    files,
                    modJson.optBoolean("hidden", false)
            ));
        }

        // loop thru the pack array
        for (JsonElement element : packsArr) {
            // check if element is object so we dont run into any weird errors
            if (!element.isJsonObject()) {
                Log.warn("Packs JSON included non-json-object.");
                continue;
            }

            // Convert element into object
            BetterJsonObject packJson = new BetterJsonObject(element.getAsJsonObject());

            // find all actions and add them to array
            EntryAction[] actions = new EntryAction[0];
            if (packJson.has("actions")) {
                JsonArray actionsArr = packJson.get("actions").getAsJsonArray();
                List<EntryAction> actionList = new ArrayList<>(actionsArr.size());
                for (JsonElement actionElement : actionsArr) {
                    BetterJsonObject actionObj = new BetterJsonObject(actionElement.getAsJsonObject());

                    String text;
                    Runnable action;
                    if (actionObj.has("document")) {
                        text = "Guide (Built-In)";
                        action = () -> {
                            try {
                                Desktop.getDesktop().browse(new URI(actionObj.optString("document")));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        };
                    } else {
                        text = actionObj.optString("text");
                        action = () -> {
                            try {
                                Desktop.getDesktop().browse(new URI(actionObj.optString("link")));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        };
                    }
                    actionList.add(new EntryAction(
                            text,
                            actionObj.optString("creator"),
                            action
                    ));
                }
                actions = actionList.toArray(new EntryAction[0]);
            }

            // find warning
            EntryWarning warning = null;
            if (packJson.has("warning")) {
                JsonArray lineArr = packJson.get("warning").getAsJsonObject().get("lines").getAsJsonArray();
                List<String> lineList = new ArrayList<>();
                for (JsonElement lineElement : lineArr) {
                    lineList.add(lineElement.getAsString());
                }
                warning = new EntryWarning(lineList);
            }

            // finally add pack entry
            packEntries.add(new PackEntry(
                    packJson.optString("id"),
                    packJson.optBoolean("enabled", false),
                    packJson.optString("file"),
                    PACKS_DIR_URL + packJson.optString("file"),
                    packJson.optString("display"),
                    packJson.optString("description"),
                    warning,
                    packJson.optString("icon"),
                    packJson.optString("icon_scaling", "smooth"),
                    actions,
                    packJson.optString("creator", "Unknown"),
                    packJson.optBoolean("hidden", false)
            ));
        }

        for (ModEntry mod : modEntries) {
            if (mod.isEnabled()) {
                for (String requiredModId : mod.getModRequirements()) {
                    getMod(requiredModId).setEnabled(true);
                }
                for (String requiredPackId : mod.getPackRequirements()) {
                    getPack(requiredPackId).setEnabled(true);
                }
            }
        }
    }

    public void getIcons(UpdateHook hook) {
        boolean refresh = shouldRefreshCache();

        if (refresh)
            Log.info("Refreshing Icon Cache...");
        else
            Log.info("Loading Icon Cache...");

        // add to another thread to prevent the program from freezing
        if (!ICON_FOLDER.exists())
            ICON_FOLDER.mkdirs();

        // loop through all the mod entries we just made and download the icons from it
        // do this after so we can have a list of all the mods as that is important
        // then get the images async
        for (ModEntry mod : modEntries) {
            if (mod.isHidden()) continue;

            Multithreading.runAsync(() -> {
                String iconFileName = mod.getIconFile();
                try {
                    // e.g. C:\Users\Xander\.skyclient\icons\neu.png
                    File iconFile = new File(ICON_FOLDER, iconFileName);
                    // If the icon doesn't already exist or the cache has expired
                    if (!iconFile.exists() || refresh) {
                        String url = ICONS_DIR_URL + iconFileName;
                        Log.info("Downloading icon: " + url + " -> " + iconFile.getAbsolutePath());
                        HttpsUtils.downloadFile(url, iconFile);
                    }
                    Log.info("Reading Image: " + iconFile.getPath());
                    imageCache.put(iconFileName, ImageIO.read(iconFile));

                    // this can be used to notify the gui that it needs to update
                    // the icon of a specified element. this reduces the work that needs to be done
                    hook.updateMod(mod);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        }

        for (PackEntry pack : packEntries) {
            if (pack.isHidden()) continue;

            Multithreading.runAsync(() -> {
                String iconFileName = pack.getIconFile();
                try {
                    File iconFile = new File(ICON_FOLDER, iconFileName);
                    if (!iconFile.exists() || refresh) {
                        HttpsUtils.downloadFile(ICONS_DIR_URL + iconFileName, iconFile);
                    }
                    imageCache.put(iconFileName, ImageIO.read(iconFile));
                    hook.updatePack(pack);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        }
    }

    public ModEntry getMod(String id) {
        for (ModEntry mod : modEntries) {
            if (mod.getId().equalsIgnoreCase(id)) return mod;
        }

        return null;
    }

    public PackEntry getPack(String id) {
        for (PackEntry pack : packEntries) {
            if (pack.getId().equalsIgnoreCase(id)) return pack;
        }

        return null;
    }

    public BufferedImage getImage(String fileName) {
        if (!imageCache.containsKey(fileName)) {
            return unknownImage;
        }

        return imageCache.get(fileName);
    }

    public List<ModEntry> getModEntries() {
        return modEntries;
    }

    public List<PackEntry> getPackEntries() {
        return packEntries;
    }

    public boolean shouldRefreshCache() {
        // if the cache folder doesnt exist or the cache was last modified over a day ago
        return !CACHE_FOLDER.exists() || System.currentTimeMillis() - CACHE_FOLDER.lastModified() > CACHE_TIME;
    }

}
