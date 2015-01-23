package vorquel.mod.simpleskygrid;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.common.DimensionManager;
import org.apache.logging.log4j.Logger;
import vorquel.mod.simpleskygrid.event.SpawnFixer;
import vorquel.mod.simpleskygrid.helper.Config;
import vorquel.mod.simpleskygrid.helper.Ref;
import vorquel.mod.simpleskygrid.world.ClassLoaderWorldProvider;
import vorquel.mod.simpleskygrid.world.WorldProviderSurfaceAlt;

import java.util.Hashtable;

@Mod(modid = "SimpleSkyGrid", name = "Simple Sky Grid", version = "@MOD_VERSION@")
public class SimpleSkyGrid {

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        Config.init(event.getSuggestedConfigurationFile());
        Ref.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(new SpawnFixer());
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        Ref.postInit();
        createWorldProviders();
    }

    private void createWorldProviders() {
        int currentId = Integer.MAX_VALUE;
        ClassLoaderWorldProvider classLoader = ClassLoaderWorldProvider.that;
        Hashtable<Class<? extends WorldProvider>, Integer> ourProviderIds = new Hashtable<Class<? extends WorldProvider>, Integer>();
        Hashtable<Integer, Integer> dimensions = ReflectionHelper.getPrivateValue(DimensionManager.class, null, "dimensions");
        Hashtable<Integer, Class<? extends WorldProvider>> providers = ReflectionHelper.getPrivateValue(DimensionManager.class, null, "providers");
        Hashtable<Integer, Boolean> spawnSettings = ReflectionHelper.getPrivateValue(DimensionManager.class, null, "spawnSettings");
        for(int dim : Config.getDimensions()) {
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
                while(!DimensionManager.registerProviderType(++currentId, proxyClass, keepLoaded));
                ourProviderIds.put(superClass, currentId);
                newId = currentId;
            }
            try {
                Object object = providers.get(newId).newInstance();
                System.out.println("Provider " + newId + " is Provider? " + (object instanceof Object));
            } catch(Exception e) {
                System.out.println("Provider " + newId + " is not an Object?!");
            }
            DimensionManager.registerDimension(dim, newId);
        }
    }
}
