package squeek.quakemovement.mixin.mixins.common;

import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import squeek.quakemovement.ModConfig;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

@Mixin(EntityPlayerMP.class)
public abstract class EntityPlayerMPMixin extends EntityPlayer {
    public EntityPlayerMPMixin(World p_i45324_1_, GameProfile p_i45324_2_) {
        super(p_i45324_1_, p_i45324_2_);
    }

    @Override
    protected void fall(float p_70069_1_) {
        boolean wasVelocityChangedBeforeFall = this.velocityChanged;

        if (ModConfig.INCREASED_FALL_DISTANCE != 0.0D) {
            fallDistance -= (float) ModConfig.INCREASED_FALL_DISTANCE;
        }
        super.fall(fallDistance);

        velocityChanged = wasVelocityChangedBeforeFall;
    }
}
