package edu.chemistry.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import edu.chemistry.EduRules;
import edu.chemistry.entity.TripodCameraEntity;
import edu.chemistry.photo.PhotoHooks;
import edu.chemistry.registry.EduEntities;
import edu.chemistry.registry.EduSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Education's camera. Use it in the air to take a picture of what you see (sneak for a close-up); use it on a block to
 * set it up on its tripod, then use the tripod camera to have it take a picture of you.
 */
public class CameraItem extends Item {
    public CameraItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        // sneaking takes a close-up instead of placing the camera
        if (player == null || player.isShiftKeyDown()) return InteractionResult.PASS;
        Level level = context.getLevel();
        if (level instanceof ServerLevel server) {
            BlockPos clicked = context.getClickedPos();
            BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(context.getClickedFace());
            TripodCameraEntity camera = new TripodCameraEntity(EduEntities.TRIPOD_CAMERA, level);
            camera.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot() + 180.0F, 0.0F);
            server.addFreshEntity(camera);
            context.getItemInHand().consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            if (!EduRules.clientEnabled) return InteractionResultHolder.fail(player.getItemInHand(hand));
            PhotoHooks.takePicture.accept(player.isShiftKeyDown(), player);
        }
        level.playSound(player, player.getX(), player.getY(), player.getZ(), EduSounds.CAMERA_TAKE_PICTURE, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
