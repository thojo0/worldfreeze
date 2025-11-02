package de.thojo0.worldfreeze.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.thojo0.worldfreeze.WorldFreeze;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.world.GameMode;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {
    @Shadow
    @Final
    private ServerPlayerEntity player;

    @Inject(method = "setGameMode", at = @At("TAIL"))
    private void setGameMode(GameMode gameMode, GameMode previousGameMode, CallbackInfo ci) {
        if (WorldFreeze.isFrozen(player.getEntityWorld())) {
            player.getAbilities().allowModifyWorld = false;
        }
    }
}
