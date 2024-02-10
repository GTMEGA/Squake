package squeek.quakemovement.mixin.plugin;

import com.falsepattern.lib.mixin.IMixin;
import com.falsepattern.lib.mixin.IMixinPlugin;
import com.falsepattern.lib.mixin.ITargetedMod;
import lombok.Getter;
import org.apache.logging.log4j.Logger;
import squeek.quakemovement.ModInfo;

public class MixinPlugin implements IMixinPlugin {
    @Getter
    private final Logger logger = IMixinPlugin.createLogger(ModInfo.MODID);

    @Override
    public ITargetedMod[] getTargetedModEnumValues() {
        return TargetedMod.values();
    }

    @Override
    public IMixin[] getMixinEnumValues() {
        return Mixin.values();
    }
}
