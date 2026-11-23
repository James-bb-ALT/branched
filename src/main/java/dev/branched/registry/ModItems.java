package dev.branched.registry;

import dev.branched.Branched;
import dev.branched.block.StaticBranchBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Items for the static branch blocks; no saplings or fruits are registered. */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Branched.MOD_ID);

    private static final Map<String, RegistryObject<BlockItem>> BY_NAME = registerAll();

    public static final RegistryObject<BlockItem> OAK_BRANCH = BY_NAME.get("oak_branch");
    public static final RegistryObject<BlockItem> SPRUCE_BRANCH = BY_NAME.get("spruce_branch");
    public static final RegistryObject<BlockItem> ACACIA_BRANCH = BY_NAME.get("acacia_branch");

    private ModItems() {
    }

    private static Map<String, RegistryObject<BlockItem>> registerAll() {
        Map<String, RegistryObject<BlockItem>> map = new LinkedHashMap<>();
        for (Map.Entry<String, RegistryObject<StaticBranchBlock>> entry : ModBlocks.allBranches().entrySet()) {
            String name = entry.getKey();
            RegistryObject<StaticBranchBlock> block = entry.getValue();
            map.put(name, ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties())));
        }
        return Collections.unmodifiableMap(map);
    }

    public static Map<String, RegistryObject<BlockItem>> allBranchItems() {
        return BY_NAME;
    }
}
