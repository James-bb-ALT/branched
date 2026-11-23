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
    public static final RegistryObject<StaticBranchBlock> LICOROOT_BRANCH = branch("licoroot_branch");

    // Deep Aether & Twilight Delight additions
    public static final RegistryObject<StaticBranchBlock> ROTTEN_ROSEROOT_BRANCH = branch("rotten_roseroot_branch");
    public static final RegistryObject<StaticBranchBlock> IRONWOOD_BRANCH = branch("ironwood_branch");

    // BetterEnd
    public static final RegistryObject<StaticBranchBlock> DRAGON_TREE_BRANCH = branch("dragon_tree_branch");
    public static final RegistryObject<StaticBranchBlock> HELIX_TREE_BRANCH = branch("helix_tree_branch");
    public static final RegistryObject<StaticBranchBlock> LACUGROVE_BRANCH = branch("lacugrove_branch");
    public static final RegistryObject<StaticBranchBlock> LUCERNIA_BRANCH = branch("lucernia_branch");
    public static final RegistryObject<StaticBranchBlock> PYTHADENDRON_BRANCH = branch("pythadendron_branch");
    public static final RegistryObject<StaticBranchBlock> TENANEA_BRANCH = branch("tenanea_branch");
    public static final RegistryObject<StaticBranchBlock> UMBRELLA_TREE_BRANCH = branch("umbrella_tree_branch");

    // BetterNether
    public static final RegistryObject<StaticBranchBlock> ANCHOR_TREE_BRANCH = branch("anchor_tree_branch");
    public static final RegistryObject<StaticBranchBlock> GLOOMWOOD_BRANCH = branch("gloomwood_branch");
    public static final RegistryObject<StaticBranchBlock> GLOOMWOOD_DARK_BRANCH = branch("gloomwood_dark_branch");
    public static final RegistryObject<StaticBranchBlock> NETHER_SAKURA_BRANCH = branch("nether_sakura_branch");
    public static final RegistryObject<StaticBranchBlock> RUBEUS_BRANCH = branch("rubeus_branch");
    public static final RegistryObject<StaticBranchBlock> BN_WILLOW_BRANCH = branch("bn_willow_branch");
    public static final RegistryObject<StaticBranchBlock> BN_WART_BRANCH = branch("bn_wart_branch");

    // Biomes O' Plenty
    public static final RegistryObject<StaticBranchBlock> BOP_DEAD_BRANCH = branch("bop_dead_branch");
    public static final RegistryObject<StaticBranchBlock> BOP_EMPYREAL_BRANCH = branch("bop_empyreal_branch");
    public static final RegistryObject<StaticBranchBlock> BOP_FIR_BRANCH = branch("bop_fir_branch");
    public static final RegistryObject<StaticBranchBlock> BOP_HELLBARK_BRANCH = branch("bop_hellbark_branch");
    public static final RegistryObject<StaticBranchBlock> BOP_JACARANDA_BRANCH = branch("bop_jacaranda_branch");
    public static final RegistryObject<StaticBranchBlock> BOP_MAGIC_BRANCH = branch("bop_magic_branch");
    public static final RegistryObject<StaticBranchBlock> BOP_MAHOGANY_BRANCH = branch("bop_mahogany_branch");
    public static final RegistryObject<StaticBranchBlock> BOP_MAPLE_BRANCH = branch("bop_maple_branch");
    public static final RegistryObject<StaticBranchBlock> BOP_PALM_BRANCH = branch("bop_palm_branch");
    public static final RegistryObject<StaticBranchBlock> BOP_PINE_BRANCH = branch("bop_pine_branch");
    public static final RegistryObject<StaticBranchBlock> BOP_REDWOOD_BRANCH = branch("bop_redwood_branch");
    public static final RegistryObject<StaticBranchBlock> BOP_UMBRAN_BRANCH = branch("bop_umbran_branch");
    public static final RegistryObject<StaticBranchBlock> BOP_WILLOW_BRANCH = branch("bop_willow_branch");

    // Oh The Biomes We've Gone
    public static final RegistryObject<StaticBranchBlock> BWG_ASPEN_BRANCH = branch("bwg_aspen_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_BAOBAB_BRANCH = branch("bwg_baobab_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_BLUE_ENCHANTED_BRANCH = branch("bwg_blue_enchanted_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_CIKA_BRANCH = branch("bwg_cika_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_CYPRESS_BRANCH = branch("bwg_cypress_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_EBONY_BRANCH = branch("bwg_ebony_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_FIR_BRANCH = branch("bwg_fir_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_GREEN_ENCHANTED_BRANCH = branch("bwg_green_enchanted_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_HOLLY_BRANCH = branch("bwg_holly_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_IRONWOOD_BRANCH = branch("bwg_ironwood_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_JACARANDA_BRANCH = branch("bwg_jacaranda_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_MAHOGANY_BRANCH = branch("bwg_mahogany_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_MAPLE_BRANCH = branch("bwg_maple_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_PALM_BRANCH = branch("bwg_palm_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_PALO_VERDE_BRANCH = branch("bwg_palo_verde_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_PINE_BRANCH = branch("bwg_pine_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_RAINBOW_EUCALYPTUS_BRANCH = branch("bwg_rainbow_eucalyptus_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_REDWOOD_BRANCH = branch("bwg_redwood_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_SAKURA_BRANCH = branch("bwg_sakura_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_SKYRIS_BRANCH = branch("bwg_skyris_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_WHITE_MANGROVE_BRANCH = branch("bwg_white_mangrove_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_WILLOW_BRANCH = branch("bwg_willow_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_WITCH_HAZEL_BRANCH = branch("bwg_witch_hazel_branch");
    public static final RegistryObject<StaticBranchBlock> BWG_ZELKOVA_BRANCH = branch("bwg_zelkova_branch");

    // Nature's Spirit
    public static final RegistryObject<StaticBranchBlock> NS_ASPEN_BRANCH = branch("ns_aspen_branch");
    public static final RegistryObject<StaticBranchBlock> NS_CEDAR_BRANCH = branch("ns_cedar_branch");
    public static final RegistryObject<StaticBranchBlock> NS_COCONUT_BRANCH = branch("ns_coconut_branch");
    public static final RegistryObject<StaticBranchBlock> NS_CYPRESS_BRANCH = branch("ns_cypress_branch");
    public static final RegistryObject<StaticBranchBlock> NS_FIR_BRANCH = branch("ns_fir_branch");
    public static final RegistryObject<StaticBranchBlock> NS_GHAF_BRANCH = branch("ns_ghaf_branch");
    public static final RegistryObject<StaticBranchBlock> NS_JOSHUA_BRANCH = branch("ns_joshua_branch");
    public static final RegistryObject<StaticBranchBlock> NS_LARCH_BRANCH = branch("ns_larch_branch");
    public static final RegistryObject<StaticBranchBlock> NS_MAHOGANY_BRANCH = branch("ns_mahogany_branch");
    public static final RegistryObject<StaticBranchBlock> NS_MAPLE_BRANCH = branch("ns_maple_branch");
    public static final RegistryObject<StaticBranchBlock> NS_OLIVE_BRANCH = branch("ns_olive_branch");
    public static final RegistryObject<StaticBranchBlock> NS_PALO_VERDE_BRANCH = branch("ns_palo_verde_branch");
    public static final RegistryObject<StaticBranchBlock> NS_REDWOOD_BRANCH = branch("ns_redwood_branch");
    public static final RegistryObject<StaticBranchBlock> NS_SAXAUL_BRANCH = branch("ns_saxaul_branch");
    public static final RegistryObject<StaticBranchBlock> NS_SUGI_BRANCH = branch("ns_sugi_branch");
    public static final RegistryObject<StaticBranchBlock> NS_WILLOW_BRANCH = branch("ns_willow_branch");
    public static final RegistryObject<StaticBranchBlock> NS_WISTERIA_BRANCH = branch("ns_wisteria_branch");

    // Atmospheric
    public static final RegistryObject<StaticBranchBlock> ATM_ASPEN_BRANCH = branch("atm_aspen_branch");
    public static final RegistryObject<StaticBranchBlock> ATM_CRUSTOSE_BRANCH = branch("atm_crustose_branch");
    public static final RegistryObject<StaticBranchBlock> ATM_GRIMWOOD_BRANCH = branch("atm_grimwood_branch");
    public static final RegistryObject<StaticBranchBlock> ATM_KOUSA_BRANCH = branch("atm_kousa_branch");
    public static final RegistryObject<StaticBranchBlock> ATM_LAUREL_BRANCH = branch("atm_laurel_branch");
    public static final RegistryObject<StaticBranchBlock> ATM_MORADO_BRANCH = branch("atm_morado_branch");
    public static final RegistryObject<StaticBranchBlock> ATM_ROSEWOOD_BRANCH = branch("atm_rosewood_branch");
    public static final RegistryObject<StaticBranchBlock> ATM_YUCCA_BRANCH = branch("atm_yucca_branch");

    // Environmental
    public static final RegistryObject<StaticBranchBlock> ENV_PINE_BRANCH = branch("env_pine_branch");
    public static final RegistryObject<StaticBranchBlock> ENV_PLUM_BRANCH = branch("env_plum_branch");
    public static final RegistryObject<StaticBranchBlock> ENV_WILLOW_BRANCH = branch("env_willow_branch");
    public static final RegistryObject<StaticBranchBlock> ENV_WISTERIA_BRANCH = branch("env_wisteria_branch");

    // Autumnity
    public static final RegistryObject<StaticBranchBlock> AUT_MAPLE_BRANCH = branch("aut_maple_branch");

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
