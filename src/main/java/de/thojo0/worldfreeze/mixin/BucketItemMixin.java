package de.thojo0.worldfreeze.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.thojo0.worldfreeze.WorldFreeze;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;

@Mixin(BucketItem.class)
public class BucketItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void preventBucketUseInFrozenWorld(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        // Prüfen, ob die Welt eingefroren ist
        if (WorldFreeze.isFrozen(level)) {
            WorldFreeze.sendMessage(player, WorldFreeze.WORLD_FROZEN_MSG);
            
            // Aktion direkt abbrechen und FAIL zurückgeben
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}