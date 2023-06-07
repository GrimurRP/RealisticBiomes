package com.untamedears.realisticbiomes;

import com.untamedears.realisticbiomes.commands.RBCommandManager;
import com.untamedears.realisticbiomes.listener.AnimalListener;
import com.untamedears.realisticbiomes.listener.BonemealListener;
import com.untamedears.realisticbiomes.listener.MobListener;
import com.untamedears.realisticbiomes.listener.PlantListener;
import com.untamedears.realisticbiomes.listener.PlayerListener;
import com.untamedears.realisticbiomes.model.Plant;
import com.untamedears.realisticbiomes.model.RBChunkCache;
import com.untamedears.realisticbiomes.model.RBDAO;
import com.untamedears.realisticbiomes.replant.AutoReplantListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.api.BlockBasedChunkMetaView;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.api.ChunkMetaAPI;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.table.TableBasedDataObject;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.table.TableStorageEngine;

import java.io.File;

public class RealisticBiomes extends ACivMod {

	private static RealisticBiomes plugin;

	public static RealisticBiomes getInstance() {
		return plugin;
	}

	private GrowthConfigManager growthConfigManager;
	private RBConfigManager configManager;
	private PlantManager plantManager;
	private AnimalConfigManager animalManager;
	private PlantLogicManager plantLogicManager;
	private PlantProgressManager plantProgressManager;
	private RBCommandManager commandManager;

	private PlantListener plantListener;
	private AnimalListener animalListener;
	private PlayerListener playerListener;
	private BonemealListener bonemealListener;
	private RBDAO dao;

	public RBConfigManager getConfigManager() {
		return configManager;
	}

	public RBDAO getDAO() {
		return dao;
	}

	public GrowthConfigManager getGrowthConfigManager() {
		return growthConfigManager;
	}

	public PlantLogicManager getPlantLogicManager() {
		return plantLogicManager;
	}

	public PlantManager getPlantManager() {
		return plantManager;
	}

	public PlantProgressManager getPlantProgressManager() {
		return plantProgressManager;
	}

	@Override
	public void onDisable() {
		dao.setBatchMode(true);
		if (plantManager != null) {
			plantManager.shutDown();
		}
		dao.cleanupBatches();
	}

	@Override
	public void onEnable() {
		super.onEnable();
		RealisticBiomes.plugin = this;
		configManager = new RBConfigManager(this);
		if (!configManager.parse()) {
			return;
		}
		animalManager = new AnimalConfigManager();
		growthConfigManager = new GrowthConfigManager(configManager.getPlantGrowthConfigs());

		reloadPersistent();

		plantLogicManager = new PlantLogicManager(plantManager, growthConfigManager);
		commandManager = new RBCommandManager(this);
		registerListeners();
	}

	private void registerListeners() {
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(plantListener = new PlantListener(this, plantManager, plantLogicManager), this);
		pm.registerEvents(animalListener = new AnimalListener(animalManager), this);
		pm.registerEvents(playerListener = new PlayerListener(growthConfigManager, animalManager, plantManager), this);
		pm.registerEvents(bonemealListener = new BonemealListener(configManager.getBonemealPreventedBlocks()), this);
		pm.registerEvents(new MobListener(), this);
		pm.registerEvents(new AutoReplantListener(configManager), this);
	}

	public final void reload() {
		reload("config.yml");
	}

	public final void reload(String fileName) {
		File file = new File(getDataFolder(), fileName);
		if (!file.exists()) {
			getLogger().severe("File " + file.getPath() + " does not exist");
			return;
		}

		FileConfiguration config = YamlConfiguration.loadConfiguration(file);
		configManager.reload(config);

		animalManager.reload();
		growthConfigManager.reload(configManager.getPlantGrowthConfigs());

		reloadPersistent();

		plantLogicManager.reload(plantManager, growthConfigManager);

		plantListener.reload(plantManager, plantLogicManager);
		animalListener.reload(animalManager);
		playerListener.reload(growthConfigManager, animalManager, plantManager);
		bonemealListener.reload(configManager.getBonemealPreventedBlocks());

		getLogger().info("Reloaded");
	}

	private void reloadPersistent() {
		if (!configManager.hasPersistentGrowthConfigs() || this.dao != null)
			return;

		this.dao = new RBDAO(getLogger(), configManager.getDatabase());
		if (!dao.updateDatabase()) {
			Bukkit.shutdown();
			return;
		}

		plantProgressManager = new PlantProgressManager();

		BlockBasedChunkMetaView<RBChunkCache, TableBasedDataObject, TableStorageEngine<Plant>> chunkMetaData = ChunkMetaAPI
				.registerBlockBasedPlugin(this, () -> {
					return new RBChunkCache(false, dao);
				}, dao, false);

		if (chunkMetaData == null) {
			getLogger().severe("Errors setting up chunk metadata API, shutting down");
			Bukkit.shutdown();
			return;
		}

		plantManager = new PlantManager(chunkMetaData);
	}

}
