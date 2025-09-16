package com.funniray.minimap.common;

import com.funniray.minimap.common.api.MinimapWorld;
import com.funniray.minimap.common.jm.data.JMConfig;
import com.funniray.minimap.common.jm.data.JMWorldConfig;
import com.funniray.minimap.common.voxel.data.VoxelConfig;
import com.funniray.minimap.common.voxel.data.VoxelWorldConfig;
import com.funniray.minimap.common.xaeros.XaerosConfig;
import com.funniray.minimap.common.xaeros.XaerosWorldConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ConfigSerializable
public class MinimapConfig {
    public UUID worldId = UUID.randomUUID();

    @Comment("If true, do not auto-create per-world config sections when new worlds are detected.")
    public boolean disablePerWorldProfiles = false;

    public JMConfig globalJourneymapConfig = new JMConfig();
    public XaerosConfig globalXaerosConfig = new XaerosConfig();
    @Comment("Only supports VoxelMap-Updated. See: https://github.com/funniray/minimap-control/issues/1")
    public VoxelConfig globalVoxelConfig = new VoxelConfig();
    public JMWorldConfig defaultWorldConfig = new JMWorldConfig();
    private Map<String, WorldConfig> worlds = JavaMinimapPlugin.getInstance().getServer().getWorlds().stream()
            .map(MinimapWorld::getName)
            .collect(Collectors.toMap(s->s, s->new WorldConfig()));

    public WorldConfig getWorldConfig(String world) {
        WorldConfig conf = worlds.get(world);

        if (conf == null) {
            if (disablePerWorldProfiles) {
                // Return a transient world config derived from defaults and globals, but do not persist
                return createTransientWorldConfig(world);
            }
            conf = new WorldConfig();
            worlds.put(world, conf);
            JavaMinimapPlugin.getInstance().saveConfig();
        }

        return conf;
    }

    private WorldConfig createTransientWorldConfig(String world) {
        WorldConfig transientCfg = new WorldConfig();
        // Deterministic UUID without persisting to file
        transientCfg.worldId = UUID.nameUUIDFromBytes(("minimap:" + world).getBytes(StandardCharsets.UTF_8));
        // Use default world config for JM
        transientCfg.journeymapConfig = defaultWorldConfig;
        // Use global configs for others (copy values)
        XaerosWorldConfig xwc = new XaerosWorldConfig();
        xwc.caveMode = globalXaerosConfig.caveMode;
        xwc.netherCaveMode = globalXaerosConfig.netherCaveMode;
        xwc.radar = globalXaerosConfig.radar;
        // keep xwc.enabled default (false) unless changed by admin when profiles are enabled
        transientCfg.xaerosConfig = xwc;

        VoxelWorldConfig vwc = new VoxelWorldConfig();
        vwc.radarAllowed = globalVoxelConfig.radarAllowed;
        vwc.radarMobsAllowed = globalVoxelConfig.radarMobsAllowed;
        vwc.radarPlayersAllowed = globalVoxelConfig.radarPlayersAllowed;
        vwc.cavesAllowed = globalVoxelConfig.cavesAllowed;
        vwc.teleportCommand = globalVoxelConfig.teleportCommand;
        // keep vwc.enabled default (false)
        transientCfg.voxelConfig = vwc;
        return transientCfg;
    }

    public Collection<WorldConfig> getWorldConfigs() {
        return worlds.values();
    }

    @ConfigSerializable
    public static class WorldConfig {
        public UUID worldId = UUID.randomUUID();
        public JMWorldConfig journeymapConfig = new JMWorldConfig();
        public XaerosWorldConfig xaerosConfig = new XaerosWorldConfig();
        public VoxelWorldConfig voxelConfig = new VoxelWorldConfig();
    }
}
