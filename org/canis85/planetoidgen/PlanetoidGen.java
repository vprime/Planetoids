package org.canis85.planetoidgen;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.config.Configuration;

/**
 * Sample plugin for Bukkit
 *
 * @author Dinnerbone
 */
public class PlanetoidGen extends JavaPlugin {

   String worldName = null;
   Configuration planetConfig;
   private final Map<String, Object> CONFIG_DEFAULTS = new HashMap<String, Object>();
   private BukkitScheduler scheduler;
   public static World planetoids = null;

   private void loadDefaults() {
      CONFIG_DEFAULTS.put("planetoids.worldname", "Planetoids");
      CONFIG_DEFAULTS.put("planetoids.alwaysnight", Boolean.valueOf(false));
      CONFIG_DEFAULTS.put("planetoids.weather", Boolean.valueOf(false));
      CONFIG_DEFAULTS.put("planetoids.commands.pltp", Boolean.valueOf(true));
      CONFIG_DEFAULTS.put("planetoids.disablemonsters", Boolean.valueOf(true));
      CONFIG_DEFAULTS.put("planetoids.disableanimals", Boolean.valueOf(false));
      CONFIG_DEFAULTS.put("planetoids.seed", getServer().getWorlds().get(0).getSeed());
      CONFIG_DEFAULTS.put("planetoids.planets.density", 2000);
      CONFIG_DEFAULTS.put("planetoids.planets.minSize", 4);
      CONFIG_DEFAULTS.put("planetoids.planets.maxSize", 20);
      CONFIG_DEFAULTS.put("planetoids.planets.minDistance", 10);
      CONFIG_DEFAULTS.put("planetoids.planets.minShellSize", 3);
      CONFIG_DEFAULTS.put("planetoids.planets.maxShellSize", 5);
      CONFIG_DEFAULTS.put("planetoids.planets.floorBlock", "STATIONARY_WATER");
      CONFIG_DEFAULTS.put("planetoids.planets.floorHeight", 0);
      CONFIG_DEFAULTS.put("planetoids.planets.bedrock", Boolean.valueOf(false));

      ArrayList<String> cores = new ArrayList<String>();
      ArrayList<String> shells = new ArrayList<String>();

      shells.add(Material.STONE.toString() + "-1.0");
      shells.add(Material.GRASS.toString() + "-1.0");
      shells.add(Material.LEAVES.toString() + "-0.9");
      shells.add(Material.ICE.toString() + "-0.9");
      shells.add(Material.SNOW_BLOCK.toString() + "-0.9");
      shells.add(Material.GLOWSTONE.toString() + "-0.4");
      shells.add(Material.BRICK.toString() + "-0.6");
      shells.add(Material.SANDSTONE.toString() + "-0.8");
      shells.add(Material.OBSIDIAN.toString() + "-0.5");
      shells.add(Material.MOSSY_COBBLESTONE.toString() + "-0.3");
      shells.add(Material.WOOL.toString() + "-0.4");
      shells.add(Material.GLASS.toString() + "-0.9");

      cores.add(Material.PUMPKIN.toString() + "-0.8");
      cores.add(Material.STATIONARY_LAVA.toString() + "-0.8");
      cores.add(Material.STATIONARY_WATER.toString() + "-1.0");
      cores.add(Material.COAL_ORE.toString() + "-1.0");
      cores.add(Material.IRON_ORE.toString() + "-0.8");
      cores.add(Material.DIAMOND_ORE.toString() + "-0.4");
      cores.add(Material.CLAY.toString() + "-0.3");
      cores.add(Material.LAPIS_ORE.toString() + "-0.4");
      cores.add(Material.LOG.toString() + "-1.0");
      cores.add(Material.GOLD_ORE.toString() + "-0.6");
      cores.add(Material.REDSTONE_ORE.toString() + "-0.75");
      cores.add(Material.SAND.toString() + "-1.0");
      cores.add(Material.BEDROCK.toString() + "-0.5");
      cores.add(Material.AIR.toString() + "-1.0");
      cores.add(Material.DIRT.toString() + "-1.0");

      CONFIG_DEFAULTS.put("planetoids.planets.blocks.cores", cores);
      CONFIG_DEFAULTS.put("planetoids.planets.blocks.shells", shells);
   }

   public void onDisable() {
      // TODO: Place any custom disable code here
      if (worldName != null) {
         getServer().unloadWorld(worldName, true);
      }

      // EXAMPLE: Custom code, here we just output some info so we can check all is well
      PluginDescriptionFile pdfFile = this.getDescription();
      System.out.println(pdfFile.getName() + " unloaded.");
   }

   private boolean loadSettings() {
      loadDefaults();
      File plConfigFile = new File(getDataFolder(), "settings.yml");
      if (plConfigFile.exists()) {
         planetConfig = new Configuration(plConfigFile);
         planetConfig.load();
         planetConfig.setHeader("#Planetoids configuration file");
         boolean refreshConfig = false;
         for (String prop : CONFIG_DEFAULTS.keySet()) {
            if (planetConfig.getProperty(prop) == null) {
               refreshConfig = true;
               planetConfig.setProperty(prop, CONFIG_DEFAULTS.get(prop));
            }
         }
         if (refreshConfig) {
            planetConfig.save();
         }
      } else {
         try {
            getDataFolder().mkdirs();
            plConfigFile.createNewFile();
            planetConfig = new Configuration(plConfigFile);
            planetConfig.setHeader("#Planetoids configuration file");
            for (String s : CONFIG_DEFAULTS.keySet()) {
               planetConfig.setProperty(s, CONFIG_DEFAULTS.get(s));
            }
            planetConfig.save();
         } catch (Exception ex) {
            ex.printStackTrace();
            return false;
         }
      }
      return true;
   }

   public void onEnable() {
      boolean settingsLoaded = loadSettings();

      PluginDescriptionFile pdfFile = this.getDescription();
      if (settingsLoaded) {
         worldName = planetConfig.getString("planetoids.worldname", "Planetoids");

         if (planetConfig.getBoolean("planetoids.commands.pltp", true)) {
            getCommand("pltp").setExecutor(new PGPltpCommand(this, worldName));
         }

         //Create chunk generator
         PGChunkGenerator pgGen = new PGChunkGenerator(planetConfig, this);

         // EXAMPLE: Custom code, here we just output some info so we can check all is well
         planetoids = getServer().createWorld(worldName, Environment.NORMAL, (long) planetConfig.getDouble("planetoids.seed", 0.0), pgGen);

         if (!planetConfig.getBoolean("planetoids.weather", false)) {
            planetoids.setWeatherDuration(0);
         }

         if (planetConfig.getBoolean("planetoids.disablemonsters", false)) {
            planetoids.setSpawnFlags(false, planetoids.getAllowAnimals());
         }

         if (planetConfig.getBoolean("planetoids.disableanimals", false)) {
            planetoids.setSpawnFlags(planetoids.getAllowMonsters(), false);
         }

         scheduler = getServer().getScheduler();
         PGRunnable task = new PGRunnable();
         if (planetConfig.getBoolean("planetoids.alwaysnight", true)) {
            scheduler.scheduleSyncRepeatingTask(this, task, 60L, 8399L);
         }

         System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
      } else {
         System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " unable to load!  Check settings file.");
      }
   }
}
