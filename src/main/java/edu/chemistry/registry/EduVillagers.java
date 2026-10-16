package edu.chemistry.registry;

import com.google.common.collect.ImmutableSet;
import edu.chemistry.EduMod;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.block.state.BlockState;

/** The Chemist: a villager profession whose job site is the Lab Table. Its trades are data files (trade_set/chemist). */
public final class EduVillagers {
    public static final ResourceKey<PoiType> CHEMIST_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, EduMod.id("chemist"));
    public static final ResourceKey<VillagerProfession> CHEMIST = ResourceKey.create(Registries.VILLAGER_PROFESSION, EduMod.id("chemist"));

    private EduVillagers() {
    }

    public static void init() {
        Set<BlockState> states = ImmutableSet.copyOf(EduBlocks.LAB_TABLE.getStateDefinition().getPossibleStates());
        Holder<PoiType> poi = Registry.registerForHolder(BuiltInRegistries.POINT_OF_INTEREST_TYPE, CHEMIST_POI, new PoiType(states, 1, 1));
        // vanilla's private registerBlockStates: puts each Lab Table state in the block state -> job site lookup
        try {
            java.lang.reflect.Method m = PoiTypes.class.getDeclaredMethod("registerBlockStates", Holder.class, Set.class);
            m.setAccessible(true);
            m.invoke(null, poi, states);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("couldn't make the Lab Table a job site", e);
        }

        Int2ObjectMap<ResourceKey<TradeSet>> trades = new Int2ObjectOpenHashMap<>();
        for (int level = 1; level <= 5; level++) {
            trades.put(level, ResourceKey.create(Registries.TRADE_SET, EduMod.id("chemist/level_" + level)));
        }
        Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, CHEMIST, new VillagerProfession(
            Component.translatable("entity.edu.villager.chemist"),
            h -> h.is(CHEMIST_POI), h -> h.is(CHEMIST_POI),
            ImmutableSet.of(), ImmutableSet.of(), SoundEvents.BREWING_STAND_BREW, trades));
    }
}
