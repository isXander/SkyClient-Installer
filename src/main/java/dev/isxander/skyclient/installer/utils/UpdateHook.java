package dev.isxander.skyclient.installer.utils;

import dev.isxander.skyclient.installer.repo.entry.ModEntry;
import dev.isxander.skyclient.installer.repo.entry.PackEntry;

public interface UpdateHook {

    void updateMod(ModEntry mod);

    void updatePack(PackEntry pack);

}
