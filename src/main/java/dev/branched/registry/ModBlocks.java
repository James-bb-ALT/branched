package dev.branched.registry;

import dev.branched.Branched;
import dev.branched.block.StaticBranchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Blocks owned by Branched: only the static 3D branch blocks, never logs/leaves. */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Branched.MOD_ID);

    private static final Map<String, RegistryObject<StaticBranchBlock>> BY_NAME = new LinkedHashMap<>();

    // Vanilla wood families
    public static final RegistryObject<StaticBranchBlock> OAK_BRANCH = branch("oak_branch");
    public static final RegistryObject<StaticBranchBlock> SPRUCE_BRANCH = branch("spruce_branch");
    public static final RegistryObject<StaticBranchBlock> BIRCH_BRANCH = branch("birch_branch");
    public static final RegistryObject<StaticBranchBlock> JUNGLE_BRANCH = branch("jungle_branch");
    public static final RegistryObject<StaticBranchBlock> ACACIA_BRANCH = branch("acacia_branch");
    public static final RegistryObject<StaticBranchBlock> DARK_OAK_BRANCH = branch("dark_oak_branch");
    public static final RegistryObject<StaticBranchBlock> MANGROVE_BRANCH = branch("mangrove_branch");
    public static final RegistryObject<StaticBranchBlock> CHERRY_BRANCH = branch("cherry_branch");
    public static final RegistryObject<StaticBranchBlock> CRIMSON_BRANCH = branch("crimson_branch");
    public static final RegistryObject<StaticBranchBlock> WARPED_BRANCH = branch("warped_branch");
    public static final RegistryObject<StaticBranchBlock> PALE_OAK_BRANCH = branch("pale_oak_branch");
    public static final RegistryObject<StaticBranchBlock> POPLAR_BRANCH = branch("poplar_branch");

    // Regions Unexplored
    public static final RegistryObject<StaticBranchBlock> PINE_BRANCH = branch("pine_branch");
    public static final RegistryObject<StaticBranchBlock> LARCH_BRANCH = branch("larch_branch");
    public static final RegistryObject<StaticBranchBlock> MAPLE_BRANCH = branch("maple_branch");
    public static final RegistryObject<StaticBranchBlock> MAGNOLIA_BRANCH = branch("magnolia_branch");
    public static final RegistryObject<StaticBranchBlock> BLACKWOOD_BRANCH = branch("blackwood_branch");
    public static final RegistryObject<StaticBranchBlock> REDWOOD_BRANCH = branch("redwood_branch");
    public static final RegistryObject<StaticBranchBlock> KAPOK_BRANCH = branch("kapok_branch");
    public static final RegistryObject<StaticBranchBlock> EUCALYPTUS_BRANCH = branch("eucalyptus_branch");
    public static final RegistryObject<StaticBranchBlock> BAOBAB_BRANCH = branch("baobab_branch");
    public static final RegistryObject<StaticBranchBlock> CYPRESS_BRANCH = branch("cypress_branch");
    public static final RegistryObject<StaticBranchBlock> PALM_BRANCH = branch("palm_branch");
    public static final RegistryObject<StaticBranchBlock> WILLOW_BRANCH = branch("willow_branch");
    public static final RegistryObject<StaticBranchBlock> SILVER_BIRCH_BRANCH = branch("silver_birch_branch");
    public static final RegistryObject<StaticBranchBlock> ASHEN_BRANCH = branch("ashen_branch");
    public static final RegistryObject<StaticBranchBlock> DEAD_BRANCH = branch("dead_branch");
    public static final RegistryObject<StaticBranchBlock> MAUVE_BRANCH = branch("mauve_branch");
    public static final RegistryObject<StaticBranchBlock> SOCOTRA_BRANCH = branch("socotra_branch");
    public static final RegistryObject<StaticBranchBlock> JOSHUA_BRANCH = branch("joshua_branch");
    public static final RegistryObject<StaticBranchBlock> BRIMWOOD_BRANCH = branch("brimwood_branch");
    public static final RegistryObject<StaticBranchBlock> COBALT_BRANCH = branch("cobalt_branch");
    public static final RegistryObject<StaticBranchBlock> ALPHA_OAK_BRANCH = branch("alpha_oak_branch");
    public static final RegistryObject<StaticBranchBlock> SMALL_OAK_BRANCH = branch("small_oak_branch");

    // Aether & Deep Aether
    public static final RegistryObject<StaticBranchBlock> SKYROOT_BRANCH = branch("skyroot_branch");
    public static final RegistryObject<StaticBranchBlock> GOLDEN_OAK_BRANCH = branch("golden_oak_branch");
    public static final RegistryObject<StaticBranchBlock> ROSEROOT_BRANCH = branch("roseroot_branch");
    public static final RegistryObject<StaticBranchBlock> YAGROOT_BRANCH = branch("yagroot_branch");
    public static final RegistryObject<StaticBranchBlock> CRUDEROOT_BRANCH = branch("cruderoot_branch");
    public static final RegistryObject<StaticBranchBlock> CONBERRY_BRANCH = branch("conberry_branch");
    public static final RegistryObject<StaticBranchBlock> SUNROOT_BRANCH = branch("sunroot_branch");

    // Quark
    public static final RegistryObject<StaticBranchBlock> BLOSSOM_BRANCH = branch("blossom_branch");
    public static final RegistryObject<StaticBranchBlock> ANCIENT_BRANCH = branch("ancient_branch");
    public static final RegistryObject<StaticBranchBlock> AZALEA_BRANCH = branch("azalea_branch");

    // Fish of Thieves
    public static final RegistryObject<StaticBranchBlock> BANANA_BRANCH = branch("banana_branch");
    public static final RegistryObject<StaticBranchBlock> FOT_COCONUT_BRANCH = branch("fot_coconut_branch");

    // Twilight Forest
    public static final RegistryObject<StaticBranchBlock> TWILIGHT_OAK_BRANCH = branch("twilight_oak_branch");
    public static final RegistryObject<StaticBranchBlock> CANOPY_BRANCH = branch("canopy_branch");
    public static final RegistryObject<StaticBranchBlock> DARKWOOD_BRANCH = branch("darkwood_branch");
    public static final RegistryObject<StaticBranchBlock> TWILIGHT_MANGROVE_BRANCH = branch("twilight_mangrove_branch");
    public static final RegistryObject<StaticBranchBlock> MINING_BRANCH = branch("mining_branch");
    public static final RegistryObject<StaticBranchBlock> TIME_BRANCH = branch("time_branch");
    public static final RegistryObject<StaticBranchBlock> TRANSFORMATION_BRANCH = branch("transformation_branch");
    public static final RegistryObject<StaticBranchBlock> SORTING_BRANCH = branch("sorting_branch");

    // Biome Makeover
    public static final RegistryObject<StaticBranchBlock> BM_ANCIENT_OAK_BRANCH = branch("bm_ancient_oak_branch");
    public static final RegistryObject<StaticBranchBlock> BM_BLIGHTED_BALSA_BRANCH = branch("bm_blighted_balsa_branch");
    public static final RegistryObject<StaticBranchBlock> BM_SWAMP_CYPRESS_BRANCH = branch("bm_swamp_cypress_branch");
    public static final RegistryObject<StaticBranchBlock> BM_WILLOW_BRANCH = branch("bm_willow_branch");

    // Ecologics, Upgrade Aquatic, BaobabTree, Deeper & Darker, Alex's Caves
    public static final RegistryObject<StaticBranchBlock> WALNUT_BRANCH = branch("walnut_branch");
    public static final RegistryObject<StaticBranchBlock> COCONUT_BRANCH = branch("coconut_branch");
    public static final RegistryObject<StaticBranchBlock> ECOLOGICS_AZALEA_BRANCH = branch("ecologics_azalea_branch");
    public static final RegistryObject<StaticBranchBlock> RIVER_BRANCH = branch("river_branch");
    public static final RegistryObject<StaticBranchBlock> BAOBABTREE_BRANCH = branch("baobabtree_branch");
    public static final RegistryObject<StaticBranchBlock> ECHO_BRANCH = branch("echo_branch");
    public static final RegistryObject<StaticBranchBlock> PEWEN_BRANCH = branch("pewen_branch");
    public static final RegistryObject<StaticBranchBlock> THORNWOOD_BRANCH = branch("thornwood_branch");

    private ModBlocks() {
    }

    private static RegistryObject<StaticBranchBlock> branch(String name) {
        RegistryObject<StaticBranchBlock> obj = BLOCKS.register(name, () -> new StaticBranchBlock(branchProperties()));
        BY_NAME.put(name, obj);
        return obj;
    }

    public static Map<String, RegistryObject<StaticBranchBlock>> allBranches() {
        return Collections.unmodifiableMap(BY_NAME);
    }

    private static BlockBehaviour.Properties branchProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(1.5F, 3.0F)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .isViewBlocking((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false);
    }
}
