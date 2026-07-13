package de.thojo0.worldfreeze.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.thojo0.worldfreeze.WorldFreeze;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;

@Mixin(BlockItem.class)
public class BlockItemMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void preventPlacementInFrozenWorld(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();

        // Prüfen, ob die Welt eingefroren ist
        if (WorldFreeze.isFrozen(level)) {
            Player player = context.getPlayer();
            if (player != null) {
                WorldFreeze.sendMessage(player, WorldFreeze.WORLD_FROZEN_MSG);
            }
            // Bricht das Platzieren ab und signalisiert dem Spiel, dass die Aktion fehlgeschlagen ist
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}