package edu.chemistry.data;

import edu.chemistry.registry.EduBlocks;
import edu.chemistry.registry.EduItems;
import edu.chemistry.registry.EduSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/** Lab table reactions. Anything that isn't a known reaction turns into garbage with a random effect. */
public final class LabReactions {
    /** what a reaction needs: how to recognise it, and what it looks like (for recipe viewers) */
    private record Ing(Predicate<ItemStack> test, Supplier<ItemStack> shown) {
    }

    private record Part(Predicate<ItemStack> test, int count, Supplier<ItemStack> shown) {
    }

    private record Reaction(String name, List<Part> parts, Supplier<ItemStack> result, Effect effect) {
    }

    @FunctionalInterface
    private interface Effect {
        void play(ServerLevel level, BlockPos pos);
    }

    private static List<Reaction> reactions;

    private static Ing item(Supplier<Item> item) {
        return new Ing(s -> s.is(item.get()), () -> new ItemStack(item.get()));
    }

    private static Ing compound(String id) {
        return new Ing(s -> s.is(EduItems.compound(id)), () -> new ItemStack(EduItems.compound(id)));
    }

    private static Ing element(int z) {
        return new Ing(s -> s.is(EduItems.ELEMENTS[z]), () -> new ItemStack(EduItems.ELEMENTS[z]));
    }

    private static Ing awkwardPotion() {
        return new Ing(LabReactions::isAwkwardPotion, () -> PotionContents.createItemStack(Items.POTION, Potions.AWKWARD));
    }

    private static boolean isAwkwardPotion(ItemStack s) {
        if (!s.is(Items.POTION)) return false;
        PotionContents contents = s.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(Potions.AWKWARD);
    }

    private static Part part(Ing ing, int count) {
        return new Part(ing.test(), count, ing.shown());
    }

    /** A reaction as recipe viewers (JEI) show it: the ingredients, with how many of each, and what it makes. */
    public record Shown(String name, List<ItemStack> inputs, ItemStack result) {
    }

    public static List<Shown> shown() {
        List<Shown> out = new ArrayList<>();
        for (Reaction r : reactions()) {
            List<ItemStack> inputs = new ArrayList<>();
            for (Part part : r.parts()) inputs.add(part.shown().get().copyWithCount(part.count()));
            out.add(new Shown(r.name(), inputs, r.result().get()));
        }
        return out;
    }

    private static List<Reaction> reactions() {
        if (reactions != null) return reactions;
        List<Reaction> list = new ArrayList<>();
        list.add(new Reaction("bleach", List.of(part(compound("water"), 3), part(compound("sodium_hypochlorite"), 3)),
            () -> new ItemStack(EduItems.BLEACH), (l, p) -> {
                burst(l, p, ParticleTypes.FLAME, 30, 0.05);
                sound(l, p, SoundEvents.FIRE_AMBIENT, 2.0F, 0.8F);
            }));
        list.add(new Reaction("heat_block", List.of(part(element(26), 1), part(compound("water"), 1), part(item(() -> Items.CHARCOAL), 1),
            part(compound("salt"), 1)),
            () -> new ItemStack(EduBlocks.HEAT_BLOCK), (l, p) -> {
                burst(l, p, ParticleTypes.FLAME, 20, 0.08);
                sound(l, p, SoundEvents.GHAST_SHOOT, 1.0F, 0.6F);
            }));
        list.add(new Reaction("ice_bomb", List.of(part(compound("sodium_acetate"), 4)),
            () -> new ItemStack(EduItems.ICE_BOMB), (l, p) -> {
                burst(l, p, ParticleTypes.SNOWFLAKE, 30, 0.08);
                sound(l, p, SoundEvents.WITHER_BREAK_BLOCK, 0.5F, 1.2F);
            }));
        list.add(new Reaction("super_fertilizer", List.of(part(compound("ammonia"), 1), part(element(15), 1)),
            () -> new ItemStack(EduItems.SUPER_FERTILIZER), (l, p) -> {
                burst(l, p, ParticleTypes.COMPOSTER, 25, 0.05);
                sound(l, p, SoundEvents.GRAVEL_STEP, 1.0F, 0.3F);
            }));
        list.add(new Reaction("magnesium_salts", List.of(part(element(12), 1), part(compound("salt"), 1)),
            () -> new ItemStack(EduItems.compound("magnesium_salts")), (l, p) -> {
                burst(l, p, ParticleTypes.WHITE_ASH, 40, 0.05);
                sound(l, p, SoundEvents.SKELETON_STEP, 1.0F, 0.6F);
            }));
        list.add(new Reaction("elephant_toothpaste", List.of(part(compound("soap"), 1), part(compound("potassium_iodide"), 1),
            part(compound("hydrogen_peroxide"), 1)),
            () -> new ItemStack(EduItems.compound("garbage")), LabReactions::elephantToothpaste));
        // medicine: an awkward potion and one element
        Object[][] medicines = {{47, "antidote"}, {27, "elixir"}, {20, "eye_drops"}, {83, "tonic"}};
        for (Object[] m : medicines) {
            int z = (Integer) m[0];
            String id = (String) m[1];
            list.add(new Reaction(id, List.of(part(awkwardPotion(), 1), part(element(z), 1)),
                () -> new ItemStack(switch (id) {
                    case "antidote" -> EduItems.ANTIDOTE;
                    case "elixir" -> EduItems.ELIXIR;
                    case "eye_drops" -> EduItems.EYE_DROPS;
                    default -> EduItems.TONIC;
                }), (l, p) -> {
                    burst(l, p, ParticleTypes.WITCH, 20, 0.05);
                    sound(l, p, SoundEvents.BREWING_STAND_BREW, 1.0F, 1.0F);
                }));
        }
        reactions = list;
        return list;
    }

    /** Runs the reaction for the items on the table, using them up. */
    public static void react(ServerLevel level, BlockPos pos, Player player, List<ItemStack> items) {
        for (Reaction r : reactions()) {
            if (matches(r, items)) {
                for (Part part : r.parts()) {
                    int left = part.count();
                    for (ItemStack s : items) {
                        if (left <= 0) break;
                        if (!s.isEmpty() && part.test().test(s)) {
                            int n = Math.min(left, s.getCount());
                            s.shrink(n);
                            left -= n;
                        }
                    }
                }
                r.effect().play(level, pos);
                popOut(level, pos, r.result().get());
                sound(level, pos, EduSounds.LAB_PRODUCT, 1.0F, 0.6F);
                return;
            }
        }
        // not a known reaction: everything is used up and turns into garbage
        for (ItemStack s : items) s.setCount(0);
        switch (level.getRandom().nextInt(5)) {
            case 0 -> { // miscellaneous mystical
                burst(level, pos, ParticleTypes.ENCHANT, 60, 0.5);
                burst(level, pos, ParticleTypes.WITCH, 20, 0.1);
                sound(level, pos, EduSounds.LAB_FIZZ, 1.0F, 0.8F);
                sound(level, pos, SoundEvents.TNT_PRIMED, 1.0F, 0.5F);
            }
            case 1 -> { // huge explosion (harmless)
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
                sound(level, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.0F, 0.7F);
                sound(level, pos, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.9F);
            }
            case 2 -> { // fire and lava
                burst(level, pos, ParticleTypes.LAVA, 15, 0.1);
                burst(level, pos, ParticleTypes.FLAME, 30, 0.08);
                sound(level, pos, SoundEvents.FIRE_AMBIENT, 2.0F, 0.5F);
            }
            case 3 -> { // smoke
                burst(level, pos, ParticleTypes.LARGE_SMOKE, 40, 0.05);
                sound(level, pos, SoundEvents.FIRE_EXTINGUISH, 1.0F, 0.6F);
            }
            default -> { // ignition
                burst(level, pos, ParticleTypes.FLAME, 50, 0.2);
                sound(level, pos, SoundEvents.FIRECHARGE_USE, 1.0F, 0.6F);
            }
        }
        popOut(level, pos, new ItemStack(EduItems.compound("garbage")));
    }

    private static boolean matches(Reaction r, List<ItemStack> items) {
        boolean any = false;
        for (ItemStack s : items) {
            if (s.isEmpty()) continue;
            any = true;
            boolean known = false;
            for (Part part : r.parts()) {
                if (part.test().test(s)) known = true;
            }
            if (!known) return false;
        }
        if (!any) return false;
        for (Part part : r.parts()) {
            int have = 0;
            for (ItemStack s : items) {
                if (!s.isEmpty() && part.test().test(s)) have += s.getCount();
            }
            if (have < part.count()) return false;
        }
        return true;
    }

    private static void elephantToothpaste(ServerLevel level, BlockPos pos) {
        int[] colors = {0xFF4F4F, 0x4FA8FF, 0x6FFF6F, 0xFFE04F, 0xFF7FD4};
        int color = colors[level.getRandom().nextInt(colors.length)];
        for (int i = 0; i < 6; i++) {
            level.sendParticles(new DustParticleOptions(color, 2.0F), pos.getX() + 0.5, pos.getY() + 1.1 + i * 0.35, pos.getZ() + 0.5, 25,
                0.25 + i * 0.05, 0.15, 0.25 + i * 0.05, 0.0);
        }
        level.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 40, 0.4, 0.6, 0.4, 0.05);
        sound(level, pos, EduSounds.LAB_FIZZ, 1.0F, 0.6F);
        sound(level, pos, SoundEvents.BLAZE_SHOOT, 1.0F, 0.9F);
    }

    private static void burst(ServerLevel level, BlockPos pos, ParticleOptions particle, int count, double speed) {
        level.sendParticles(particle, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, count, 0.3, 0.3, 0.3, speed);
    }

    private static void sound(ServerLevel level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, volume, pitch);
    }

    private static void popOut(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, stack);
        entity.setDeltaMovement(level.getRandom().nextGaussian() * 0.02, 0.25, level.getRandom().nextGaussian() * 0.02);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    private LabReactions() {
    }
}
