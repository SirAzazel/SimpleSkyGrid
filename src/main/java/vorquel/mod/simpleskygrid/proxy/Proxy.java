package vorquel.mod.simpleskygrid.proxy;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.common.DimensionManager;
import vorquel.mod.simpleskygrid.config.CommandReloadConfigs;
import vorquel.mod.simpleskygrid.config.Config;
import vorquel.mod.simpleskygrid.event.SpawnFixer;
import vorquel.mod.simpleskygrid.helper.Log;
import vorquel.mod.simpleskygrid.helper.Ref;
import vorquel.mod.simpleskygrid.world.provider.ClassLoaderWorldProvider;
import vorquel.mod.simpleskygrid.world.provider.WorldProviderSurfaceAlt;

import java.util.Hashtable;

public class Proxy {

    public void preInit(FMLPreInitializationEvent event) {
        Log.setLogger(event.getModLog());
        Config.loadConfigs();
    }

    public void init() {
        FMLCommonHandler.instance().bus().register(new SpawnFixer());
    }

    public void postInit() {
        Ref.postInit();
        createWorldProviders();
    }

    private void createWorldProviders() {
        int currentId = Integer.MAX_VALUE;
        ClassLoaderWorldProvider classLoader = ClassLoaderWorldProvider.that;
        Hashtable<Class<? extends WorldProvider>, Integer> ourProviderIds = new Hashtable<>();
        Hashtable<Integer, Integer> dimensions = ReflectionHelper.getPrivateValue(DimensionManager.class, null, "dimensions");
        Hashtable<Integer, Class<? extends WorldProvider>> providers = ReflectionHelper.getPrivateValue(DimensionManager.class, null, "providers");
        Hashtable<Integer, Boolean> spawnSettings = ReflectionHelper.getPrivateValue(DimensionManager.class, null, "spawnSettings");
        for(int dim : Config.dimensionPropertiesMap.keySet()) {
            Class<? extends WorldProvider> superClass = WorldProviderSurfaceAlt.class;
            boolean keepLoaded = false;
            int newId;
            if(DimensionManager.isDimensionRegistered(dim)) {
                int id = dimensions.get(dim);
                superClass = providers.get(id);
                keepLoaded = spawnSettings.get(id);
                DimensionManager.unregisterDimension(dim);
            }
            if(classLoader.hasProxy(superClass))
                newId = ourProviderIds.get(superClass);
            else {
                Class<? extends WorldProvider> proxyClass = classLoader.getWorldProviderProxy(superClass);
                //noinspection StatementWithEmptyBody
                while(!DimensionManager.registerProviderType(++currentId, proxyClass, keepLoaded));
                ourProviderIds.put(superClass, currentId);
                newId = currentId;
            }
            DimensionManager.registerDimension(dim, newId);
        }
    }

    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandReloadConfigs());
    }
}
