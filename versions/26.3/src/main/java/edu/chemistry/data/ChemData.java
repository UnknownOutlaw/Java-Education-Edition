package edu.chemistry.data;

import java.util.HashMap;
import java.util.Map;

/** Generated from the Minecraft Education chemistry pack. Do not edit by hand. */
public final class ChemData {
    /** index = atomic number; 0 = unknown element */
    public static final String[] SYMBOLS = {"?", "H", "He", "Li", "Be", "B", "C", "N", "O", "F", "Ne", "Na", "Mg", "Al", "Si", "P", "S", "Cl", "Ar", "K", "Ca", "Sc", "Ti", "V", "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn", "Ga", "Ge", "As", "Se", "Br", "Kr", "Rb", "Sr", "Y", "Zr", "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd", "In", "Sn", "Sb", "Te", "I", "Xe", "Cs", "Ba", "La", "Ce", "Pr", "Nd", "Pm", "Sm", "Eu", "Gd", "Tb", "Dy", "Ho", "Er", "Tm", "Yb", "Lu", "Hf", "Ta", "W", "Re", "Os", "Ir", "Pt", "Au", "Hg", "Tl", "Pb", "Bi", "Po", "At", "Rn", "Fr", "Ra", "Ac", "Th", "Pa", "U", "Np", "Pu", "Am", "Cm", "Bk", "Cf", "Es", "Fm", "Md", "No", "Lr", "Rf", "Db", "Sg", "Bh", "Hs", "Mt", "Ds", "Rg", "Cn", "Nh", "Fl", "Mc", "Lv", "Ts", "Og"};
    public static final String[] NAMES = {"Unknown Element", "Hydrogen", "Helium", "Lithium", "Beryllium", "Boron", "Carbon", "Nitrogen", "Oxygen", "Fluorine", "Neon", "Sodium", "Magnesium", "Aluminum", "Silicon", "Phosphorus", "Sulfur", "Chlorine", "Argon", "Potassium", "Calcium", "Scandium", "Titanium", "Vanadium", "Chromium", "Manganese", "Iron", "Cobalt", "Nickel", "Copper", "Zinc", "Gallium", "Germanium", "Arsenic", "Selenium", "Bromine", "Krypton", "Rubidium", "Strontium", "Yttrium", "Zirconium", "Niobium", "Molybdenum", "Technetium", "Ruthenium", "Rhodium", "Palladium", "Silver", "Cadmium", "Indium", "Tin", "Antimony", "Tellurium", "Iodine", "Xenon", "Cesium", "Barium", "Lanthanum", "Cerium", "Praseodymium", "Neodymium", "Promethium", "Samarium", "Europium", "Gadolinium", "Terbium", "Dysprosium", "Holmium", "Erbium", "Thulium", "Ytterbium", "Lutetium", "Hafnium", "Tantalum", "Tungsten", "Rhenium", "Osmium", "Iridium", "Platinum", "Gold", "Mercury", "Thallium", "Lead", "Bismuth", "Polonium", "Astatine", "Radon", "Francium", "Radium", "Actinium", "Thorium", "Protactinium", "Uranium", "Neptunium", "Plutonium", "Americium", "Curium", "Berkelium", "Californium", "Einsteinium", "Fermium", "Mendelevium", "Nobelium", "Lawrencium", "Rutherfordium", "Dubnium", "Seaborgium", "Bohrium", "Hassium", "Meitnerium", "Darmstadtium", "Roentgenium", "Copernicium", "Nihonium", "Flerovium", "Moscovium", "Livermorium", "Tennessine", "Oganesson"};
    /** mass number of the main isotope */
    public static final int[] MASS = {0, 1, 4, 7, 9, 11, 12, 14, 16, 19, 20, 23, 24, 27, 28, 31, 32, 35, 40, 39, 40, 45, 48, 51, 52, 55, 56, 59, 58, 63, 64, 69, 74, 75, 80, 79, 84, 85, 88, 89, 90, 93, 98, 98, 102, 103, 106, 107, 114, 115, 120, 121, 130, 127, 132, 133, 138, 139, 140, 141, 142, 145, 152, 153, 158, 159, 164, 165, 166, 169, 174, 175, 180, 181, 184, 187, 192, 193, 195, 197, 202, 205, 208, 209, 209, 210, 222, 223, 226, 227, 232, 231, 238, 237, 244, 243, 247, 247, 251, 252, 257, 258, 259, 266, 267, 268, 269, 270, 269, 278, 281, 282, 285, 286, 289, 290, 293, 294, 294};

    public static final String[] COMPOUND_IDS = {"aluminum_oxide", "ammonia", "barium_sulfate", "benzene", "boron_trioxide", "calcium_bromide", "calcium_chloride", "cerium_chloride", "crude_oil", "glue", "hydrogen_peroxide", "hypochlorite", "iron_sulfide", "latex", "lithium_hydride", "luminol", "lye", "magnesium_nitrate", "magnesium_oxide", "polyethylene", "potassium_iodide", "soap", "sodium_acetate", "sodium_fluoride", "sodium_hydride", "sodium_hypochlorite", "sodium_oxide", "sulfate", "water", "salt", "mercuric_chloride", "potassium_chloride", "tungsten_chloride", "magnesium_salts", "garbage"};
    public static final String[] COMPOUND_FORMULAS = {"Al₂O₃", "NH₃", "BaSO₄", "C₆H₆", "B₂O₃", "CaBr₂", "CaCl₂", "CeCl₃", "C₉H₂₀", "C₅H₅NO₂", "H₂O₂", "ClO", "FeS", "C₅H₈", "LiH", "C₈H₇N₃O₂", "NaOH", "Mg(NO₃)₂", "MgO", "(C₂H₄)₅", "KI", "C₁₈H₃₅NaO₂", "C₂H₃NaO₂", "NaF", "NaH", "NaClO", "Na₂O", "SO₄", "H₂O", "NaCl", "HgCl₂", "KCl", "WCl₆", "MgSO₄", ""};
    /** compound creator recipes as (atomic number, count) pairs; empty = not made in the compound creator */
    public static final int[][] COMPOUND_RECIPES = {
        {13, 2, 8, 3},  // aluminum_oxide
        {7, 1, 1, 3},  // ammonia
        {56, 1, 16, 1, 8, 4},  // barium_sulfate
        {6, 6, 1, 6},  // benzene
        {5, 2, 8, 3},  // boron_trioxide
        {20, 1, 35, 2},  // calcium_bromide
        {20, 1, 17, 2},  // calcium_chloride
        {58, 1, 17, 3},  // cerium_chloride
        {6, 9, 1, 20},  // crude_oil
        {6, 5, 1, 5, 7, 1, 8, 2},  // glue
        {1, 2, 8, 2},  // hydrogen_peroxide
        {17, 1, 8, 1},  // hypochlorite
        {26, 1, 16, 1},  // iron_sulfide
        {6, 5, 1, 8},  // latex
        {3, 1, 1, 1},  // lithium_hydride
        {6, 8, 1, 7, 7, 3, 8, 2},  // luminol
        {11, 1, 8, 1, 1, 1},  // lye
        {12, 1, 7, 2, 8, 6},  // magnesium_nitrate
        {12, 1, 8, 1},  // magnesium_oxide
        {6, 10, 1, 20},  // polyethylene
        {19, 1, 53, 1},  // potassium_iodide
        {6, 18, 1, 35, 11, 1, 8, 2},  // soap
        {6, 2, 1, 3, 11, 1, 8, 2},  // sodium_acetate
        {11, 1, 9, 1},  // sodium_fluoride
        {11, 1, 1, 1},  // sodium_hydride
        {11, 1, 17, 1, 8, 1},  // sodium_hypochlorite
        {11, 2, 8, 1},  // sodium_oxide
        {16, 1, 8, 4},  // sulfate
        {1, 2, 8, 1},  // water
        {11, 1, 17, 1},  // salt
        {80, 1, 17, 2},  // mercuric_chloride
        {19, 1, 17, 1},  // potassium_chloride
        {74, 1, 17, 6},  // tungsten_chloride
        {},  // magnesium_salts
        {},  // garbage
    };
    /** element makeup used by the material reducer (includes lab-table products) */
    public static final int[][] COMPOUND_MAKEUP = {
        {13, 2, 8, 3},  // aluminum_oxide
        {7, 1, 1, 3},  // ammonia
        {56, 1, 16, 1, 8, 4},  // barium_sulfate
        {6, 6, 1, 6},  // benzene
        {5, 2, 8, 3},  // boron_trioxide
        {20, 1, 35, 2},  // calcium_bromide
        {20, 1, 17, 2},  // calcium_chloride
        {58, 1, 17, 3},  // cerium_chloride
        {6, 9, 1, 20},  // crude_oil
        {6, 5, 1, 5, 7, 1, 8, 2},  // glue
        {1, 2, 8, 2},  // hydrogen_peroxide
        {17, 1, 8, 1},  // hypochlorite
        {26, 1, 16, 1},  // iron_sulfide
        {6, 5, 1, 8},  // latex
        {3, 1, 1, 1},  // lithium_hydride
        {6, 8, 1, 7, 7, 3, 8, 2},  // luminol
        {11, 1, 8, 1, 1, 1},  // lye
        {12, 1, 7, 2, 8, 6},  // magnesium_nitrate
        {12, 1, 8, 1},  // magnesium_oxide
        {6, 10, 1, 20},  // polyethylene
        {19, 1, 53, 1},  // potassium_iodide
        {6, 18, 1, 35, 11, 1, 8, 2},  // soap
        {6, 2, 1, 3, 11, 1, 8, 2},  // sodium_acetate
        {11, 1, 9, 1},  // sodium_fluoride
        {11, 1, 1, 1},  // sodium_hydride
        {11, 1, 17, 1, 8, 1},  // sodium_hypochlorite
        {11, 2, 8, 1},  // sodium_oxide
        {16, 1, 8, 4},  // sulfate
        {1, 2, 8, 1},  // water
        {11, 1, 17, 1},  // salt
        {80, 1, 17, 2},  // mercuric_chloride
        {19, 1, 17, 1},  // potassium_chloride
        {74, 1, 17, 6},  // tungsten_chloride
        {12, 1, 16, 1, 8, 4},  // magnesium_salts
        {},  // garbage
    };
    /** vanilla items that the compound creator can make */
    public static final String[] NATURAL_ITEMS = {"minecraft:charcoal", "minecraft:ink_sac", "minecraft:sugar"};
    public static final int[][] NATURAL_RECIPES = {{6, 7, 1, 4, 8, 1}, {26, 1, 16, 1, 8, 4}, {6, 6, 1, 12, 8, 6}};

    /** material reducer table: item id -> (atomic number, count) pairs */
    public static final Map<String, int[]> REDUCTIONS = new HashMap<>();
    /** extra item returned by the reducer (e.g. the empty bucket) */
    public static final Map<String, String> REDUCTION_EXTRAS = new HashMap<>();

    static {
        REDUCTIONS.put("minecraft:acacia_log", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:acacia_planks", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:acacia_slab", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:acacia_stairs", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:acacia_wood", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:andesite", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:andesite_slab", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:andesite_stairs", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:andesite_wall", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:birch_log", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:birch_planks", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:birch_slab", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:birch_stairs", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:birch_wood", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:black_glazed_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:black_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:blue_glazed_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:blue_ice", new int[] {1, 67, 8, 33});
        REDUCTIONS.put("minecraft:blue_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:brown_glazed_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:brown_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:charcoal", new int[] {6, 7, 1, 4, 8, 1});
        REDUCTIONS.put("minecraft:chiseled_quartz_block", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:chiseled_red_sandstone", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:chiseled_sandstone", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:clay", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:clay_ball", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:coal", new int[] {1, 6, 6, 50, 8, 43, 7, 1});
        REDUCTIONS.put("minecraft:coal_block", new int[] {1, 6, 6, 50, 8, 43, 7, 1});
        REDUCTIONS.put("minecraft:coal_ore", new int[] {1, 6, 6, 50, 8, 43, 7, 1});
        REDUCTIONS.put("minecraft:cobblestone", new int[] {8, 47, 14, 28, 13, 8, 26, 5, 20, 4, 11, 3, 19, 3, 12, 2});
        REDUCTIONS.put("minecraft:cobblestone_slab", new int[] {8, 47, 14, 28, 13, 8, 26, 5, 20, 4, 11, 3, 19, 3, 12, 2});
        REDUCTIONS.put("minecraft:cobblestone_stairs", new int[] {8, 47, 14, 28, 13, 8, 26, 5, 20, 4, 11, 3, 19, 3, 12, 2});
        REDUCTIONS.put("minecraft:cobblestone_wall", new int[] {8, 47, 14, 28, 13, 8, 26, 5, 20, 4, 11, 3, 19, 3, 12, 2});
        REDUCTIONS.put("minecraft:cut_red_sandstone", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:cut_red_sandstone_slab", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:cut_sandstone", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:cut_sandstone_slab", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:cyan_glazed_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:cyan_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:dark_oak_log", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:dark_oak_planks", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:dark_oak_slab", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:dark_oak_stairs", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:dark_oak_wood", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:diamond", new int[] {6, 100});
        REDUCTIONS.put("minecraft:diamond_block", new int[] {6, 100});
        REDUCTIONS.put("minecraft:diamond_ore", new int[] {6, 64, 14, 12, 8, 24});
        REDUCTIONS.put("minecraft:diorite", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:diorite_slab", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:diorite_stairs", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:diorite_wall", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:dirt", new int[] {14, 21, 20, 8, 15, 3, 7, 3, 26, 13, 12, 6, 30, 2, 29, 2, 8, 42});
        REDUCTIONS.put("minecraft:emerald", new int[] {13, 15, 4, 25, 14, 20, 8, 40});
        REDUCTIONS.put("minecraft:emerald_block", new int[] {13, 15, 4, 25, 14, 20, 8, 40});
        REDUCTIONS.put("minecraft:emerald_ore", new int[] {13, 15, 4, 25, 14, 20, 8, 40});
        REDUCTIONS.put("minecraft:end_stone", new int[] {14, 59, 6, 13, 0, 28});
        REDUCTIONS.put("minecraft:glowstone", new int[] {18, 20, 5, 20, 36, 20, 10, 20, 0, 20});
        REDUCTIONS.put("minecraft:glowstone_dust", new int[] {18, 20, 5, 20, 36, 20, 10, 20, 0, 20});
        REDUCTIONS.put("minecraft:gold_block", new int[] {79, 100});
        REDUCTIONS.put("minecraft:gold_ingot", new int[] {79, 100});
        REDUCTIONS.put("minecraft:gold_nugget", new int[] {79, 100});
        REDUCTIONS.put("minecraft:gold_ore", new int[] {79, 64, 14, 12, 8, 24});
        REDUCTIONS.put("minecraft:granite", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:granite_slab", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:granite_stairs", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:granite_wall", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:grass_block", new int[] {6, 15, 8, 70, 7, 8, 15, 7});
        REDUCTIONS.put("minecraft:gravel", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:gray_glazed_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:gray_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:green_glazed_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:green_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:ice", new int[] {1, 67, 8, 33});
        REDUCTIONS.put("minecraft:ink_sac", new int[] {26, 1, 16, 1, 8, 4});
        REDUCTIONS.put("minecraft:iron_block", new int[] {26, 100});
        REDUCTIONS.put("minecraft:iron_ingot", new int[] {26, 100});
        REDUCTIONS.put("minecraft:iron_nugget", new int[] {26, 100});
        REDUCTIONS.put("minecraft:iron_ore", new int[] {26, 64, 14, 12, 8, 24});
        REDUCTIONS.put("minecraft:jungle_log", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:jungle_planks", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:jungle_slab", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:jungle_stairs", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:jungle_wood", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:lapis_block", new int[] {11, 13, 16, 8, 13, 13, 8, 53, 14, 13});
        REDUCTIONS.put("minecraft:lapis_lazuli", new int[] {11, 13, 16, 8, 13, 13, 8, 53, 14, 13});
        REDUCTIONS.put("minecraft:lapis_ore", new int[] {11, 13, 16, 8, 13, 13, 8, 53, 14, 13});
        REDUCTIONS.put("minecraft:lava_bucket", new int[] {8, 47, 14, 28, 13, 8, 26, 5, 20, 4, 11, 3, 19, 3, 12, 2});
        REDUCTION_EXTRAS.put("minecraft:lava_bucket", "minecraft:bucket");
        REDUCTIONS.put("minecraft:light_blue_glazed_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:light_blue_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:light_gray_glazed_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:light_gray_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:lime_glazed_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:lime_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:magenta_glazed_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:magenta_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:magma_block", new int[] {8, 47, 14, 28, 13, 8, 26, 5, 20, 4, 11, 3, 19, 3, 12, 2});
        REDUCTIONS.put("minecraft:mossy_cobblestone", new int[] {8, 45, 14, 28, 13, 8, 26, 5, 20, 4, 11, 3, 19, 3, 12, 2, 6, 2});
        REDUCTIONS.put("minecraft:mossy_cobblestone_slab", new int[] {8, 45, 14, 28, 13, 8, 26, 5, 20, 4, 11, 3, 19, 3, 12, 2, 6, 2});
        REDUCTIONS.put("minecraft:mossy_cobblestone_stairs", new int[] {8, 45, 14, 28, 13, 8, 26, 5, 20, 4, 11, 3, 19, 3, 12, 2, 6, 2});
        REDUCTIONS.put("minecraft:mossy_cobblestone_wall", new int[] {8, 45, 14, 28, 13, 8, 26, 5, 20, 4, 11, 3, 19, 3, 12, 2, 6, 2});
        REDUCTIONS.put("minecraft:mycelium", new int[] {6, 15, 8, 70, 7, 8, 15, 7});
        REDUCTIONS.put("minecraft:nether_quartz_ore", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:netherrack", new int[] {14, 64, 8, 18, 80, 15, 0, 3});
        REDUCTIONS.put("minecraft:oak_log", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:oak_planks", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:oak_slab", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:oak_stairs", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:oak_wood", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:obsidian", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:orange_glazed_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:orange_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:packed_ice", new int[] {1, 67, 8, 33});
        REDUCTIONS.put("minecraft:pink_glazed_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:pink_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:polished_andesite", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:polished_andesite_slab", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:polished_andesite_stairs", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:polished_diorite", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:polished_diorite_slab", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:polished_diorite_stairs", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:polished_granite", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:polished_granite_slab", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:polished_granite_stairs", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:purple_glazed_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:purple_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:quartz", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:quartz_block", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:quartz_bricks", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:quartz_pillar", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:quartz_slab", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:quartz_stairs", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:red_glazed_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:red_sand", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:red_sandstone", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:red_sandstone_slab", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:red_sandstone_stairs", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:red_sandstone_wall", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:red_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:redstone", new int[] {6, 31, 92, 31, 0, 38});
        REDUCTIONS.put("minecraft:redstone_block", new int[] {6, 31, 92, 31, 0, 38});
        REDUCTIONS.put("minecraft:redstone_ore", new int[] {6, 31, 92, 31, 0, 38});
        REDUCTIONS.put("minecraft:sand", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:sandstone", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:sandstone_slab", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:sandstone_stairs", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:sandstone_wall", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:smooth_quartz", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:smooth_quartz_slab", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:smooth_quartz_stairs", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:smooth_red_sandstone", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:smooth_red_sandstone_slab", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:smooth_red_sandstone_stairs", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:smooth_sandstone", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:smooth_sandstone_slab", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:smooth_sandstone_stairs", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:smooth_stone", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:smooth_stone_slab", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:snow", new int[] {1, 67, 8, 33});
        REDUCTIONS.put("minecraft:snow_block", new int[] {1, 67, 8, 33});
        REDUCTIONS.put("minecraft:snowball", new int[] {1, 67, 8, 33});
        REDUCTIONS.put("minecraft:soul_sand", new int[] {14, 37, 8, 3, 0, 60});
        REDUCTIONS.put("minecraft:spruce_log", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:spruce_planks", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:spruce_slab", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:spruce_stairs", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:spruce_wood", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:stone", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:stone_slab", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:stone_stairs", new int[] {14, 33, 8, 67});
        REDUCTIONS.put("minecraft:stripped_acacia_log", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:stripped_acacia_wood", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:stripped_birch_log", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:stripped_birch_wood", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:stripped_dark_oak_log", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:stripped_dark_oak_wood", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:stripped_jungle_log", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:stripped_jungle_wood", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:stripped_oak_log", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:stripped_oak_wood", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:stripped_spruce_log", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:stripped_spruce_wood", new int[] {6, 49, 1, 6, 8, 44, 7, 1});
        REDUCTIONS.put("minecraft:sugar", new int[] {6, 6, 1, 12, 8, 6});
        REDUCTIONS.put("minecraft:terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:water_bucket", new int[] {1, 67, 8, 33});
        REDUCTION_EXTRAS.put("minecraft:water_bucket", "minecraft:bucket");
        REDUCTIONS.put("minecraft:white_glazed_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:white_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:yellow_glazed_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
        REDUCTIONS.put("minecraft:yellow_terracotta", new int[] {14, 77, 13, 17, 26, 3, 12, 1, 20, 1, 8, 1});
    }

    private ChemData() {
    }
}
