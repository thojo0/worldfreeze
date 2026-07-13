package de.thojo0.worldfreeze.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import de.thojo0.worldfreeze.WorldFreeze;
import net.minecraft.server.level.ServerLevel;

@Mixin(ServerLevel.class)
public abstract class ServerWorldMixin {
    @Shadow
    private int emptyTime;

    @ModifyVariable(method = "tick", name = "bl2", at = @At("STORE"), ordinal = 1)
    public boolean tick(boolean value) {
        if (WorldFreeze.isFrozen((ServerLevel) (Object) this)) {
            this.emptyTime = 400;
            return false;
        }
        return value;
    }
}
