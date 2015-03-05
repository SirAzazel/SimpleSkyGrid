package vorquel.mod.simpleskygrid.config;

import com.google.gson.stream.JsonReader;
import cpw.mods.fml.common.Loader;
import org.apache.commons.io.FileUtils;
import vorquel.mod.simpleskygrid.SimpleSkyGrid;
import vorquel.mod.simpleskygrid.config.prototype.IPrototype;
import vorquel.mod.simpleskygrid.config.prototype.PFactory;
import vorquel.mod.simpleskygrid.config.prototype.PNull;
import vorquel.mod.simpleskygrid.world.igenerated.IGeneratedObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

public class Config {

    public static HashMap<Integer, DimensionProperties> dimensionPropertiesMap = new HashMap<>();
    public static ConfigDataMap<IPrototype<IGeneratedObject>, Double> generationData = new ConfigDataMap<>();
    public static ConfigDataMap<IPrototype<IGeneratedObject>, UniqueQuantity> uniqueGenData = new ConfigDataMap<>();

    public static void loadConfigs() {
        File configHome = new File(Loader.instance().getConfigDir(), "SimpleSkyGrid");
        if(!configHome.exists() && !configHome.mkdir()) {
            SimpleSkyGrid.logger.fatal("Unable to create config directory");
            throw new RuntimeException("Unable to create config directory");
        }
        File config = new File(configHome, "SimpleSkyGrid.json");
        String configName = "SimpleSkyGrid.json";
        if(!config.exists()) {
            String configHomeDir = "/assets/simpleskygrid/config/";
            URL configURL = Config.class.getResource(configHomeDir+configName);
            try {
                FileUtils.copyURLToFile(configURL, config);
            } catch (IOException e) {
                SimpleSkyGrid.logger.fatal("Unable to copy config file: " + configName);
                SimpleSkyGrid.logger.fatal(e.getMessage());
                throw new RuntimeException("Unable to copy config file: " + configName + "\n" + e.getMessage());
            }
        }
        try {
            JsonReader jsonReader = new JsonReader(new FileReader(config));
            jsonReader.setLenient(true);
            jsonReader.beginObject();
            while(jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                switch(name) {
                    case "generation":     readGeneration(jsonReader);    break;
                    case "unique_gen":     readUniqueGen(jsonReader);     break;
                    case "loot_placement": readLootPlacement(jsonReader); break;
                    case "loot":           readLoot(jsonReader);          break;
                    default:
                        if(name.startsWith("dim"))
                            readDimension(jsonReader, name);
                        else {
                            SimpleSkyGrid.logger.warn(String.format("Unknown label %s in config file %s", name, configName));
                            jsonReader.skipValue();
                        }
                }
            }
            jsonReader.endObject();
            jsonReader.close();
        } catch (FileNotFoundException e) {
            SimpleSkyGrid.logger.fatal("Unable to load config file: " + configName);
            SimpleSkyGrid.logger.fatal(e.getMessage());
            throw new RuntimeException("Unable to load config file: " + configName + "\n" + e.getMessage());
        } catch (IOException e) {
            SimpleSkyGrid.logger.fatal("Problem reading config file: " + configName);
            SimpleSkyGrid.logger.fatal(e.getMessage());
            throw new RuntimeException("Problem reading config file: " + configName + "\n" + e.getMessage());
        }
    }

    private static void readDimension(JsonReader jsonReader, String dimName) throws IOException {
        int dim;
        try {
            dim = Integer.decode(dimName.substring(3));
        } catch(NumberFormatException e) {
            SimpleSkyGrid.logger.warn(String.format("Unknown label %s in config file", dimName));
            jsonReader.skipValue();
            return;
        }
        DimensionProperties prop = new DimensionProperties();
        jsonReader.beginObject();
        while(jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            switch(name) {
                case "height":         prop.height             = jsonReader.nextInt();    break;
                case "radius":         prop.radius             = jsonReader.nextInt();    break;
                case "spawn_height":   prop.spawnHeight        = jsonReader.nextInt();    break;
                case "generation":     prop.generationLabel    = jsonReader.nextString(); break;
                case "unique_gen":     prop.uniqueGenLabel     = jsonReader.nextString(); break;
                case "loot_placement": prop.lootPlacementLabel = jsonReader.nextString(); break;
                default:
                    SimpleSkyGrid.logger.warn(String.format("Unknown label %s in config file", name));
                    jsonReader.skipValue();
            }
        }
        jsonReader.endObject();
        if(prop.height == -1 || prop.generationLabel == null) {
            SimpleSkyGrid.logger.fatal(String.format("Underspecified dimension %d in config file", dim));
            throw new RuntimeException(String.format("Underspecified dimension %d in config file", dim));
        }
        dimensionPropertiesMap.put(dim, prop);
    }

    private static void readGeneration(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        while(jsonReader.hasNext()) {
            String label = jsonReader.nextName();
            jsonReader.beginArray();
            while(jsonReader.hasNext()) {
                jsonReader.beginObject();
                IPrototype<IGeneratedObject> prototype = PNull.generatedObject;
                double weight = 0;
                while(jsonReader.hasNext()) {
                    String innerLabel = jsonReader.nextName();
                    switch(innerLabel) {
                        case "object": prototype = PFactory.readGeneratedObject(jsonReader); break;
                        case "weight": weight    = readWeight(jsonReader);                     break;
                        default:
                            SimpleSkyGrid.logger.warn(String.format("Unknown generation label %s in config file", innerLabel));
                            jsonReader.skipValue();
                    }
                }
                if(prototype.isComplete() && weight > 0)
                    generationData.put(label, prototype, weight);
                jsonReader.endObject();
            }
            jsonReader.endArray();
        }
        jsonReader.endObject();
    }

    private static void readUniqueGen(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        while(jsonReader.hasNext()) {
            String label = jsonReader.nextName();
            jsonReader.beginArray();
            while(jsonReader.hasNext()) {
                jsonReader.beginObject();
                IPrototype<IGeneratedObject> prototype = PNull.generatedObject;
                UniqueQuantity quantity = new UniqueQuantity();
                while(jsonReader.hasNext()) {
                    String innerLabel = jsonReader.nextName();
                    switch(innerLabel) {
                        case "object":   prototype = PFactory.readGeneratedObject(jsonReader); break;
                        case "count":    quantity.countSource = PFactory.readCount(jsonReader).getObject(); break;
                        case "location": quantity.pointSource = PFactory.readPoint(jsonReader).getObject(); break;
                        default:
                            SimpleSkyGrid.logger.warn(String.format("Unknown uniqueGen label %s in config file", innerLabel));
                            jsonReader.skipValue();
                    }
                }
                if(prototype.isComplete() && quantity.isComplete())
                    uniqueGenData.put(label, prototype, quantity);
                jsonReader.endObject();
            }
            jsonReader.endArray();
        }
        jsonReader.endObject();
    }

    private static void readLootPlacement(JsonReader jsonReader) throws IOException {
        jsonReader.skipValue();
    }

    private static void readLoot(JsonReader jsonReader) throws IOException {
        jsonReader.skipValue();
    }

    private static double readWeight(JsonReader jsonReader) throws IOException {
        double weight = jsonReader.nextDouble();
        if(weight < 0) {
            SimpleSkyGrid.logger.error("Negative weight in config file");
            weight = 0;
        } else if(Double.isInfinite(weight) || Double.isNaN(weight)) {
            SimpleSkyGrid.logger.error("Crazy weight in config file");
            weight = 0;
        }
        return weight;
    }

    public static class DimensionProperties {
        public int    height             = -1;
        public int    radius             = -1;
        public int    spawnHeight        = 65;
        public String generationLabel    = null;
        public String uniqueGenLabel     = null;
        public String lootPlacementLabel = null;

        public boolean isFinite() {
            return radius != -1;
        }

        public boolean inRadius(int xChunk, int zChunk) {
            int xAbs = xChunk < 0 ? -xChunk : xChunk + 1;
            int zAbs = zChunk < 0 ? -zChunk : zChunk + 1;
            return xAbs <= radius && zAbs <= radius;
        }
    }
}
