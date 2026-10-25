package edu.chemistry;

import edu.chemistry.registry.EduBlocks;
import edu.chemistry.registry.EduEntities;
import edu.chemistry.registry.EduItems;
import edu.chemistry.registry.EduMenus;
import edu.chemistry.registry.EduSounds;
import edu.chemistry.registry.EduTabs;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EduMod implements ModInitializer {
    public static final String MOD_ID = "edu";
    public static final Logger LOG = LoggerFactory.getLogger("Education Chemistry");

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        EduRules.init();
        EduSounds.init();
        EduBlocks.init();
        edu.chemistry.registry.EduBlockEntities.init();
        EduItems.init();
        edu.chemistry.registry.EduVillagers.init();
        EduEntities.init();
        edu.chemistry.agent.Agents.init();
        EduMenus.init();
        EduTabs.init();
        edu.chemistry.registry.EduRecipes.init();
        LOG.info("Education Edition chemistry loaded: {} elements, {} compounds", 118, edu.chemistry.data.ChemData.COMPOUND_IDS.length);
    }
}
