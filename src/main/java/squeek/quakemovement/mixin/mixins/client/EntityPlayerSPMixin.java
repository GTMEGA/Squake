package squeek.quakemovement.mixin.mixins.client;

import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import squeek.quakemovement.ModConfig;
import squeek.quakemovement.SpeedometerAdapter;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

@Mixin(EntityPlayerSP.class)
public abstract class EntityPlayerSPMixin extends AbstractClientPlayer {
    @Unique
    private boolean didJumpThisTick = false;
    @Unique
    List<float[]> baseVelocities = new ArrayList<>();

    public EntityPlayerSPMixin(World p_i45074_1_, GameProfile p_i45074_2_) {
        super(p_i45074_1_, p_i45074_2_);
    }

    @Override
    public void moveEntityWithHeading(float sidemove, float forwardmove) {
        if (!ModConfig.ENABLED) {
            super.moveEntityWithHeading(sidemove, forwardmove);
            return;
        }

        double d0 = this.posX;
        double d1 = this.posY;
        double d2 = this.posZ;

        if (this.capabilities.isFlying && this.ridingEntity == null) {
            super.moveEntityWithHeading(sidemove, forwardmove);
        } else {
            this.quake_moveEntityWithHeading(sidemove, forwardmove);
        }

        this.addMovementStat(this.posX - d0, this.posY - d1, this.posZ - d2);
    }

    @Inject(method = "onLivingUpdate",
            at = @At("HEAD"),
            require = 1)
    private void beforeOnLivingUpdate(CallbackInfo ci) {
        this.didJumpThisTick = false;
        SpeedometerAdapter.setDidJumpThisTick(false);

        if (!baseVelocities.isEmpty()) {
            baseVelocities.clear();
        }

        SpeedometerAdapter.setIsJumping(isJumping);
    }

    @Override
    public void moveFlying(float sidemove, float forwardmove, float wishspeed) {
        if (!ModConfig.ENABLED) {
            super.moveFlying(sidemove, forwardmove, wishspeed);
            return;
        }

        if ((this.capabilities.isFlying && this.ridingEntity == null) || this.isInWater() ||
            this.handleLavaMovement() || this.isOnLadder()) {
            super.moveFlying(sidemove, forwardmove, wishspeed);
            return;
        }

        wishspeed *= 2.15f;
        float[] wishdir = getMovementDirection(sidemove, forwardmove);
        float[] wishvel = new float[]{wishdir[0] * wishspeed, wishdir[1] * wishspeed};
        baseVelocities.add(wishvel);
    }

    @Override
    public void jump() {
        super.jump();

        if (!ModConfig.ENABLED) {
            return;
        }

        // undo this dumb thing
        if (this.isSprinting()) {
            float f = this.rotationYaw * 0.017453292F;
            this.motionX += MathHelper.sin(f) * 0.2F;
            this.motionZ -= MathHelper.cos(f) * 0.2F;
        }

        quake_Jump();

        this.didJumpThisTick = true;
        SpeedometerAdapter.setDidJumpThisTick(true);
    }

    /* =================================================
     * START HELPERS
     * =================================================
     */

    public double getSpeed() {
        return MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
    }

    public float getSurfaceFriction() {
        float f2 = 1.0F;

        if (this.onGround) {
            Block ground = this.worldObj.getBlock(MathHelper.floor_double(this.posX),
                                                  MathHelper.floor_double(this.boundingBox.minY) - 1,
                                                  MathHelper.floor_double(this.posZ));

            f2 = 1.0F - ground.slipperiness;
        }

        return f2;
    }

    public float getSlipperiness() {
        float f2 = 0.91F;
        if (this.onGround) {
            f2 = 0.54600006F;
            Block ground = this.worldObj.getBlock(MathHelper.floor_double(this.posX),
                                                  MathHelper.floor_double(this.boundingBox.minY) - 1,
                                                  MathHelper.floor_double(this.posZ));

            if (ground != null) {
                f2 = ground.slipperiness * 0.91F;
            }
        }
        return f2;
    }

    public float minecraft_getMoveSpeed() {
        float f2 = this.getSlipperiness();

        float f3 = 0.16277136F / (f2 * f2 * f2);

        return this.getAIMoveSpeed() * f3;
    }

    public float[] getMovementDirection(float sidemove, float forwardmove) {
        float f3 = sidemove * sidemove + forwardmove * forwardmove;
        float[] dir = {0.0F, 0.0F};

        if (f3 >= 1.0E-4F) {
            f3 = MathHelper.sqrt_float(f3);

            if (f3 < 1.0F) {
                f3 = 1.0F;
            }

            f3 = 1.0F / f3;
            sidemove *= f3;
            forwardmove *= f3;
            float f4 = MathHelper.sin(this.rotationYaw * (float) Math.PI / 180.0F);
            float f5 = MathHelper.cos(this.rotationYaw * (float) Math.PI / 180.0F);
            dir[0] = (sidemove * f5 - forwardmove * f4);
            dir[1] = (forwardmove * f5 + sidemove * f4);
        }

        return dir;
    }

    public float quake_getMoveSpeed() {
        float baseSpeed = this.getAIMoveSpeed();
        return !this.isSneaking() ? baseSpeed * 2.15F : baseSpeed * 1.11F;
    }

    public float quake_getMaxMoveSpeed() {
        float baseSpeed = this.getAIMoveSpeed();
        return baseSpeed * 2.15F;
    }

    private void spawnBunnyhopParticles(int numParticles) {
        // taken from sprint
        int j = MathHelper.floor_double(this.posX);
        int i = MathHelper.floor_double(this.posY - 0.20000000298023224D - this.yOffset);
        int k = MathHelper.floor_double(this.posZ);
        Block ground = this.worldObj.getBlock(j, i, k);

        if (ground != null && ground.getMaterial() != Material.air) {
            for (int iParticle = 0; iParticle < numParticles; iParticle++) {
                this.worldObj.spawnParticle(
                        "blockcrack_" + Block.getIdFromBlock(ground) + "_" + this.worldObj.getBlockMetadata(j, i, k),
                        this.posX + (this.rand.nextFloat() - 0.5D) * this.width, this.boundingBox.minY + 0.1D,
                        this.posZ + (this.rand.nextFloat() - 0.5D) * this.width, -this.motionX * 4.0D, 1.5D,
                        -this.motionZ * 4.0D);
            }
        }
    }

    /* =================================================
     * END HELPERS
     * =================================================
     */

    /* =================================================
     * START MINECRAFT PHYSICS
     * =================================================
     */

    private void minecraft_ApplyGravity() {
        if (this.worldObj.isRemote && (!this.worldObj.blockExists((int) this.posX, 0, (int) this.posZ) ||
                                       !this.worldObj.getChunkFromBlockCoords((int) this.posX,
                                                                              (int) this.posZ).isChunkLoaded)) {
            if (this.posY > 0.0D) {
                this.motionY = -0.1D;
            } else {
                this.motionY = 0.0D;
            }
        } else {
            // gravity
            this.motionY -= 0.08D;
        }

        // air resistance
        this.motionY *= 0.9800000190734863D;
    }

    private void minecraft_ApplyFriction(float momentumRetention) {
        this.motionX *= momentumRetention;
        this.motionZ *= momentumRetention;
    }

    private void minecraft_ApplyLadderPhysics() {
        if (this.isOnLadder()) {
            float f5 = 0.15F;

            if (this.motionX < (-f5)) {
                this.motionX = (-f5);
            }

            if (this.motionX > f5) {
                this.motionX = f5;
            }

            if (this.motionZ < (-f5)) {
                this.motionZ = (-f5);
            }

            if (this.motionZ > f5) {
                this.motionZ = f5;
            }

            this.fallDistance = 0.0F;

            if (this.motionY < -0.15D) {
                this.motionY = -0.15D;
            }

            boolean flag = this.isSneaking();

            if (flag && this.motionY < 0.0D) {
                this.motionY = 0.0D;
            }
        }
    }

    private void minecraft_ClimbLadder() {
        if (this.isCollidedHorizontally && this.isOnLadder()) {
            this.motionY = 0.2D;
        }
    }

    private void minecraft_SwingLimbsBasedOnMovement() {
        this.prevLimbSwingAmount = this.limbSwingAmount;
        double d0 = this.posX - this.prevPosX;
        double d1 = this.posZ - this.prevPosZ;
        float f6 = MathHelper.sqrt_double(d0 * d0 + d1 * d1) * 4.0F;

        if (f6 > 1.0F) {
            f6 = 1.0F;
        }

        this.limbSwingAmount += (f6 - this.limbSwingAmount) * 0.4F;
        this.limbSwing += this.limbSwingAmount;
    }

    private void minecraft_WaterMove(float sidemove, float forwardmove) {
        double d0 = this.posY;
        this.moveFlying(sidemove, forwardmove, 0.04F);
        this.moveEntity(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.800000011920929D;
        this.motionY *= 0.800000011920929D;
        this.motionZ *= 0.800000011920929D;
        this.motionY -= 0.02D;

        if (this.isCollidedHorizontally &&
            this.isOffsetPositionInLiquid(this.motionX, this.motionY + 0.6000000238418579D - this.posY + d0,
                                          this.motionZ)) {
            this.motionY = 0.30000001192092896D;
        }
    }

    public void minecraft_moveEntityWithHeading(float sidemove, float forwardmove) {
        // take care of water and lava movement using default code
        if ((this.isInWater() && !this.capabilities.isFlying) ||
            (this.handleLavaMovement() && !this.capabilities.isFlying)) {
            super.moveEntityWithHeading(sidemove, forwardmove);
        } else {
            // get friction
            float momentumRetention = this.getSlipperiness();

            // alter motionX/motionZ based on desired movement
            this.moveFlying(sidemove, forwardmove, this.minecraft_getMoveSpeed());

            // make adjustments for ladder interaction
            minecraft_ApplyLadderPhysics();

            // do the movement
            this.moveEntity(this.motionX, this.motionY, this.motionZ);

            // climb ladder here for some reason
            minecraft_ClimbLadder();

            // gravity + friction
            minecraft_ApplyGravity();
            minecraft_ApplyFriction(momentumRetention);

            // swing them arms
            minecraft_SwingLimbsBasedOnMovement();
        }
    }

    /* =================================================
     * END MINECRAFT PHYSICS
     * =================================================
     */



    /* =================================================
     * START QUAKE PHYSICS
     * =================================================
     */

    /**
     * Moves the entity based on the specified heading.  Args: strafe, forward
     */
    public void quake_moveEntityWithHeading(float sidemove, float forwardmove) {
        // take care of ladder movement using default code
        if (this.isOnLadder()) {
            super.moveEntityWithHeading(sidemove, forwardmove);
            return;
        }
        // take care of lava movement using default code
        else if ((this.handleLavaMovement() && !this.capabilities.isFlying)) {
            super.moveEntityWithHeading(sidemove, forwardmove);
            return;
        } else if (this.isInWater() && !this.capabilities.isFlying) {
            if (ModConfig.SHARKING_ENABLED) {
                quake_WaterMove(sidemove, forwardmove);
            } else {
                super.moveEntityWithHeading(sidemove, forwardmove);
                return;
            }
        } else {
            // get all relevant movement values
            float wishspeed = (sidemove != 0.0F || forwardmove != 0.0F) ? this.quake_getMoveSpeed() : 0.0F;
            float[] wishdir = this.getMovementDirection(sidemove, forwardmove);
            boolean onGroundForReal = this.onGround && !this.isJumping;
            float momentumRetention = this.getSlipperiness();

            // ground movement
            if (onGroundForReal) {
                // apply friction before acceleration so we can accelerate back up to maxspeed afterwards
                //quake_Friction(); // buggy because material-based friction uses a totally different format
                minecraft_ApplyFriction(momentumRetention);

                double sv_accelerate = ModConfig.ACCELERATE;

                if (wishspeed != 0.0F) {
                    // alter based on the surface friction
                    sv_accelerate *= this.minecraft_getMoveSpeed() * 2.15F / wishspeed;

                    quake_Accelerate(wishspeed, wishdir[0], wishdir[1], sv_accelerate);
                }

                if (!baseVelocities.isEmpty()) {
                    float speedMod = wishspeed / quake_getMaxMoveSpeed();
                    // add in base velocities
                    for (float[] baseVel : baseVelocities) {
                        this.motionX += baseVel[0] * speedMod;
                        this.motionZ += baseVel[1] * speedMod;
                    }
                }
            }
            // air movement
            else {
                double sv_airaccelerate = ModConfig.AIR_ACCELERATE;
                quake_AirAccelerate(wishspeed, wishdir[0], wishdir[1], sv_airaccelerate);

                if (ModConfig.SHARKING_ENABLED && ModConfig.SHARKING_SURFACE_TENSION > 0.0D && this.isJumping &&
                    this.motionY < 0.0F) {
                    AxisAlignedBB axisalignedbb =
                            this.boundingBox.getOffsetBoundingBox(this.motionX, this.motionY, this.motionZ);
                    boolean isFallingIntoWater = this.worldObj.isAnyLiquid(axisalignedbb);

                    if (isFallingIntoWater) {
                        this.motionY *= ModConfig.SHARKING_SURFACE_TENSION;
                    }
                }
            }

            // apply velocity
            this.moveEntity(this.motionX, this.motionY, this.motionZ);

            // HL2 code applies half gravity before acceleration and half after acceleration, but this seems to work fine
            minecraft_ApplyGravity();
        }

        // swing them arms
        minecraft_SwingLimbsBasedOnMovement();
    }

    private void quake_Jump() {
        quake_ApplySoftCap(this.quake_getMaxMoveSpeed());

        boolean didTrimp = quake_DoTrimp();

        if (!didTrimp) {
            quake_ApplyHardCap(this.quake_getMaxMoveSpeed());
        }
    }

    private boolean quake_DoTrimp() {
        if (ModConfig.TRIMPING_ENABLED && this.isSneaking()) {
            double curspeed = this.getSpeed();
            float movespeed = this.quake_getMaxMoveSpeed();
            if (curspeed > movespeed) {
                double speedbonus = curspeed / movespeed * 0.5F;
                if (speedbonus > 1.0F) {
                    speedbonus = 1.0F;
                }

                this.motionY += speedbonus * curspeed * ModConfig.TRIMP_MULTIPLIER;

                if (ModConfig.TRIMP_MULTIPLIER > 0) {
                    float mult = 1.0f / ModConfig.TRIMP_MULTIPLIER;
                    this.motionX *= mult;
                    this.motionZ *= mult;
                }

                spawnBunnyhopParticles(30);

                return true;
            }
        }

        return false;
    }

    private void quake_ApplyWaterFriction(double friction) {
        this.motionX *= friction;
        this.motionY *= friction;
        this.motionZ *= friction;

		/*
		float speed = (float)(this.getSpeed());
		float newspeed = 0.0F;
		if (speed != 0.0F)
		{
			newspeed = speed - 0.05F * speed * friction; //* player->m_surfaceFriction;

			float mult = newspeed/speed;
			this.motionX *= mult;
			this.motionY *= mult;
			this.motionZ *= mult;
		}

		return newspeed;
		*/

		/*
		// slow in water
		this.motionX *= 0.800000011920929D;
		this.motionY *= 0.800000011920929D;
		this.motionZ *= 0.800000011920929D;
		*/
    }

    @SuppressWarnings("unused")
    private void quake_WaterAccelerate(float wishspeed, float speed, double wishX, double wishZ, double accel) {
        float addspeed = wishspeed - speed;
        if (addspeed > 0) {
            float accelspeed = (float) (accel * wishspeed * 0.05F);
            if (accelspeed > addspeed) {
                accelspeed = addspeed;
            }

            this.motionX += accelspeed * wishX;
            this.motionZ += accelspeed * wishZ;
        }
    }

    private void quake_WaterMove(float sidemove, float forwardmove) {
        double lastPosY = this.posY;

        // get all relevant movement values
        float wishspeed = (sidemove != 0.0F || forwardmove != 0.0F) ? this.quake_getMaxMoveSpeed() : 0.0F;
        float[] wishdir = this.getMovementDirection(sidemove, forwardmove);
        boolean isSharking = this.isJumping && this.isOffsetPositionInLiquid(0.0D, 1.0D, 0.0D);
        double curspeed = this.getSpeed();

        if (!isSharking || curspeed < 0.078F) {
            minecraft_WaterMove(sidemove, forwardmove);
        } else {
            if (curspeed > 0.09) {
                quake_ApplyWaterFriction(ModConfig.SHARKING_WATER_FRICTION);
            }

            if (curspeed > 0.098) {
                quake_AirAccelerate(wishspeed, wishdir[0], wishdir[1], ModConfig.ACCELERATE);
            } else {
                quake_Accelerate(.0980F, wishdir[0], wishdir[1], ModConfig.ACCELERATE);
            }

            this.moveEntity(this.motionX, this.motionY, this.motionZ);

            this.motionY = 0.0D;
        }

        // water jump
        if (this.isCollidedHorizontally &&
            this.isOffsetPositionInLiquid(this.motionX, this.motionY + 0.6000000238418579D - this.posY + lastPosY,
                                          this.motionZ)) {
            this.motionY = 0.30000001192092896D;
        }

        if (!baseVelocities.isEmpty()) {
            float speedMod = wishspeed / quake_getMaxMoveSpeed();
            // add in base velocities
            for (float[] baseVel : baseVelocities) {
                this.motionX += baseVel[0] * speedMod;
                this.motionZ += baseVel[1] * speedMod;
            }
        }
    }

    private void quake_Accelerate(float wishspeed, double wishX, double wishZ, double accel) {
        double addspeed, accelspeed, currentspeed;

        // Determine veer amount
        // this is a dot product
        currentspeed = this.motionX * wishX + this.motionZ * wishZ;

        // See how much to add
        addspeed = wishspeed - currentspeed;

        // If not adding any, done.
        if (addspeed <= 0) {
            return;
        }

        // Determine acceleration speed after acceleration
        accelspeed = accel * wishspeed / getSlipperiness() * 0.05F;

        // Cap it
        if (accelspeed > addspeed) {
            accelspeed = addspeed;
        }

        // Adjust pmove vel.
        this.motionX += accelspeed * wishX;
        this.motionZ += accelspeed * wishZ;
    }

    private void quake_AirAccelerate(float wishspeed, double wishX, double wishZ, double accel) {
        double addspeed, accelspeed, currentspeed;

        float wishspd = wishspeed;
        float maxAirAcceleration = (float) ModConfig.MAX_AIR_ACCEL_PER_TICK;

        if (wishspd > maxAirAcceleration) {
            wishspd = maxAirAcceleration;
        }

        // Determine veer amount
        // this is a dot product
        currentspeed = this.motionX * wishX + this.motionZ * wishZ;

        // See how much to add
        addspeed = wishspd - currentspeed;

        // If not adding any, done.
        if (addspeed <= 0) {
            return;
        }

        // Determine acceleration speed after acceleration
        accelspeed = accel * wishspeed * 0.05F;

        // Cap it
        if (accelspeed > addspeed) {
            accelspeed = addspeed;
        }

        // Adjust pmove vel.
        this.motionX += accelspeed * wishX;
        this.motionZ += accelspeed * wishZ;
    }

    @SuppressWarnings("unused")
    private void quake_Friction() {
        double speed, newspeed, control;
        float friction;
        float drop;

        // Calculate speed
        speed = this.getSpeed();

        // If too slow, return
        if (speed <= 0.0F) {
            return;
        }

        drop = 0.0F;

        // convars
        float sv_friction = 1.0F;
        float sv_stopspeed = 0.005F;

        float surfaceFriction = this.getSurfaceFriction();
        friction = sv_friction * surfaceFriction;

        // Bleed off some speed, but if we have less than the bleed
        //  threshold, bleed the threshold amount.
        control = (speed < sv_stopspeed) ? sv_stopspeed : speed;

        // Add the amount to the drop amount.
        drop += control * friction * 0.05F;

        // scale the velocity
        newspeed = speed - drop;
        if (newspeed < 0.0F) {
            newspeed = 0.0F;
        }

        if (newspeed != speed) {
            // Determine proportion of old speed we are using.
            newspeed /= speed;
            // Adjust velocity according to proportion.
            this.motionX *= newspeed;
            this.motionZ *= newspeed;
        }
    }

    private void quake_ApplySoftCap(float movespeed) {
        float softCapPercent = ModConfig.SOFT_CAP;
        float softCapDegen = ModConfig.SOFT_CAP_DEGEN;

        if (ModConfig.UNCAPPED_BUNNYHOP_ENABLED) {
            softCapPercent = 1.0F;
            softCapDegen = 1.0F;
        }

        float speed = (float) (this.getSpeed());
        float softCap = movespeed * softCapPercent;

        // apply soft cap first; if soft -> hard is not done, then you can continually trigger only the hard cap and stay at the hard cap
        if (speed > softCap) {
            if (softCapDegen != 1.0F) {
                float applied_cap = (speed - softCap) * softCapDegen + softCap;
                float multi = applied_cap / speed;
                this.motionX *= multi;
                this.motionZ *= multi;
            }

            spawnBunnyhopParticles(10);
        }
    }

    private void quake_ApplyHardCap(float movespeed) {
        if (ModConfig.UNCAPPED_BUNNYHOP_ENABLED) {
            return;
        }

        float hardCapPercent = ModConfig.HARD_CAP;

        float speed = (float) (this.getSpeed());
        float hardCap = movespeed * hardCapPercent;

        if (speed > hardCap && hardCap != 0.0F) {
            float multi = hardCap / speed;
            this.motionX *= multi;
            this.motionZ *= multi;

            spawnBunnyhopParticles(30);
        }
    }

    @SuppressWarnings("unused")
    private void quake_OnLivingUpdate() {
        this.didJumpThisTick = false;
    }

    /* =================================================
     * END QUAKE PHYSICS
     * =================================================
     */
}
