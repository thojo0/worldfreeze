package de.thojo0.worldfreeze.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.thojo0.worldfreeze.WorldFreeze;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerInteractionManagerMixin {
    @Shadow
    @Final
    private ServerPlayer player;

    @Inject(method = "setGameModeForPlayer", at = @At("TAIL"))
    private void setGameMode(GameType gameMode, GameType previousGameMode, CallbackInfo ci) {
        if (WorldFreeze.isFrozen(player.level())) {
            player.getAbilities().mayBuild = false;
        }
    }
}
