package squeek.quakemovement;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;

@Mod(modid = ModInfo.MODID,
     version = ModInfo.VERSION,
     dependencies = "after:Squeedometer")
public class ModQuakeMovement {
    // The instance of your mod that Forge uses.
    @Instance(value = ModInfo.MODID)
    public static ModQuakeMovement instance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.init(event.getSuggestedConfigurationFile());
        if (event.getSide() == Side.CLIENT) {
            FMLCommonHandler.instance().bus().register(new ToggleKeyHandler());
        }
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        SpeedometerAdapter.init();
    }
}
