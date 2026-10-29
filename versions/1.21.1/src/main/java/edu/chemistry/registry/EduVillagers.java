package edu.chemistry.registry;

import com.google.common.collect.ImmutableSet;
import edu.chemistry.EduMod;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Chemist villager: a jobless villager who finds a Lab Table takes the job. Its trades (two picked at random at each
 * level, like vanilla's) are in ChemistTrades.
 */
public final class EduVillagers {
    public static final ResourceKey<PoiType> CHEMIST_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, EduMod.id("chemist"));
    public static VillagerProfession CHEMIST;

    private EduVillagers() {
    }

    /** the Lab Table as a job site */
    public static void initJobSite() {
        Set<BlockState> states = ImmutableSet.copyOf(EduBlocks.LAB_TABLE.getStateDefinition().getPossibleStates());
        Holder<PoiType> poi = Registry.registerForHolder(BuiltInRegistries.POINT_OF_INTEREST_TYPE, CHEMIST_POI, new PoiType(states, 1, 1));
        // NeoForge and Forge put a registered job site's blocks in the block state -> job site lookup themselves
        if (EduMod.loader != EduMod.Loader.FABRIC) return;
        // vanilla's registerBlockStates (opened by the access widener): puts each Lab Table state in the block state -> job site lookup
        PoiTypes.registerBlockStates(poi, states);
    }

    /** the Chemist (its job site is only named, so it doesn't matter which is registered first) */
    public static void initProfession() {
        CHEMIST = Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, EduMod.id("chemist"), new VillagerProfession("chemist",
            h -> h.is(CHEMIST_POI), h -> h.is(CHEMIST_POI), ImmutableSet.of(), ImmutableSet.of(), SoundEvents.BREWING_STAND_BREW));
        VillagerTrades.TRADES.put(CHEMIST, ChemistTrades.byLevel());
    }
}
