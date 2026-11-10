package dev.frostclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;

/** Optional hold-to-place helper. Always delegates to the vanilla interaction manager.
 * It never creates blocks itself or sends custom placement packets. Servers retain authority. */
public final class AccuratePlacement {
    private static BlockPos lastTarget;
    private static int lastSlot=-1;
    private static int tick=0;
    private static int lastSent=-100;
    private AccuratePlacement(){}

    public static BlockPos proposedPosition(Minecraft mc){
        if(mc.player==null || mc.level==null || !(mc.hitResult instanceof BlockHitResult hit))return null;
        if(!(mc.player.getMainHandItem().getItem() instanceof BlockItem))return null;
        BlockPos clicked=hit.getBlockPos();
        BlockPos place=mc.level.getBlockState(clicked).canBeReplaced()?clicked:clicked.relative(hit.getDirection());
        if(!mc.level.getWorldBorder().isWithinBounds(place))return null;
        if(!mc.level.getBlockState(place).canBeReplaced())return null;
        if(place.distSqr(mc.player.blockPosition())>36)return null;
        return place;
    }

    public static void tick(Minecraft mc){
        tick++;
        if(!FrostClient.CONFIG.enabled("Accurate Block Placement") || mc.screen!=null || mc.gameMode==null
                || mc.player==null || !mc.options.keyUse.isDown() || mc.player.isUsingItem()){
            lastTarget=null;
            return;
        }
        BlockPos target=proposedPosition(mc);
        if(target==null){lastTarget=null;return;}
        int slot=mc.player.getInventory().getSelectedSlot();
        if(slot!=lastSlot){lastTarget=null;lastSlot=slot;}
        // Only a genuinely new adjacent placement target triggers an extra interaction.
        // This reduces gaps as the crosshair moves, without duplicate rapid-fire packets.
        if(target.equals(lastTarget) || tick-lastSent<2)return;
        lastTarget=target.immutable();
        lastSent=tick;
        BlockHitResult hit=(BlockHitResult)mc.hitResult;
        mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,hit);
    }
}
