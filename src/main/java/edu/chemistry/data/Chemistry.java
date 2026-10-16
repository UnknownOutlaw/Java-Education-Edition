package edu.chemistry.data;

import edu.chemistry.item.CompoundItem;
import edu.chemistry.item.ElementBlockItem;
import edu.chemistry.registry.EduItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Recipe logic shared by the compound creator, element constructor and material reducer. */
public final class Chemistry {
    /** neutron counts allowed around the main isotope */
    public static final int ISOTOPE_BELOW = 2;
    public static final int ISOTOPE_ABOVE = 2;
    public static final int MAX_PROTONS = 118;
    public static final int MAX_ELECTRONS = 118;
    public static final int MAX_NEUTRONS = 180;

    public static int mainNeutrons(int z) {
        return ChemData.MASS[z] - z;
    }

    public static boolean isValidIsotope(int protons, int neutrons) {
        if (protons < 1 || protons > MAX_PROTONS) return false;
        int main = mainNeutrons(protons);
        if (protons == 1) return neutrons >= 0 && neutrons <= 2; // protium, deuterium, tritium
        return neutrons >= Math.max(0, main - ISOTOPE_BELOW) && neutrons <= main + ISOTOPE_ABOVE;
    }

    /** @return atomic number of the element these particles make, or -1 */
    public static int construct(int protons, int neutrons, int electrons) {
        if (protons < 1 || electrons != protons) return -1;
        return isValidIsotope(protons, neutrons) ? protons : -1;
    }

    public static int atomicNumber(ItemStack stack) {
        return stack.getItem() instanceof ElementBlockItem e ? e.atomicNumber() : -1;
    }

    /** Sums the elements in a grid. Returns null if anything that isn't an element is in it. */
    public static Map<Integer, Integer> countElements(List<ItemStack> stacks) {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (ItemStack s : stacks) {
            if (s.isEmpty()) continue;
            int z = atomicNumber(s);
            if (z <= 0) return null;
            counts.merge(z, s.getCount(), Integer::sum);
        }
        return counts;
    }

    private static int multiple(Map<Integer, Integer> counts, int[] recipe) {
        if (recipe.length == 0 || counts.size() != recipe.length / 2) return 0;
        int k = -1;
        for (int i = 0; i < recipe.length; i += 2) {
            Integer have = counts.get(recipe[i]);
            if (have == null || have % recipe[i + 1] != 0) return 0;
            int m = have / recipe[i + 1];
            if (k == -1) k = m;
            else if (k != m) return 0;
        }
        return Math.max(k, 0);
    }

    /** What the compound creator makes from this grid (count = how many full recipes are in it), or EMPTY. */
    public static ItemStack compoundFor(List<ItemStack> grid) {
        Map<Integer, Integer> counts = countElements(grid);
        if (counts == null || counts.isEmpty()) return ItemStack.EMPTY;
        for (int i = 0; i < ChemData.COMPOUND_IDS.length; i++) {
            int k = multiple(counts, ChemData.COMPOUND_RECIPES[i]);
            if (k > 0) return new ItemStack(EduItems.compound(ChemData.COMPOUND_IDS[i]), Math.min(k, 64));
        }
        for (int i = 0; i < ChemData.NATURAL_ITEMS.length; i++) {
            int k = multiple(counts, ChemData.NATURAL_RECIPES[i]);
            if (k > 0) {
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(ChemData.NATURAL_ITEMS[i]));
                return new ItemStack(item, Math.min(k, 64));
            }
        }
        return ItemStack.EMPTY;
    }

    /** Material reducer output for ONE of the given item, as stacks (at most 9). Empty list = can't be reduced. */
    public static List<ItemStack> reduce(ItemStack input) {
        List<ItemStack> out = new ArrayList<>();
        if (input.isEmpty()) return out;
        int[] pairs = null;
        String extra = null;
        if (input.getItem() instanceof CompoundItem compound) {
            for (int i = 0; i < ChemData.COMPOUND_IDS.length; i++) {
                if (ChemData.COMPOUND_IDS[i].equals(compound.compoundId())) pairs = ChemData.COMPOUND_MAKEUP[i];
            }
        } else {
            String id = BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
            pairs = ChemData.REDUCTIONS.get(id);
            extra = ChemData.REDUCTION_EXTRAS.get(id);
        }
        if (pairs == null || pairs.length == 0) return out;
        for (int i = 0; i < pairs.length; i += 2) {
            int z = pairs[i];
            int count = pairs[i + 1];
            while (count > 0 && out.size() < 9) {
                int n = Math.min(count, 64);
                out.add(new ItemStack(EduItems.ELEMENTS[z], n));
                count -= n;
            }
        }
        if (extra != null && out.size() < 9) {
            out.add(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(extra))));
        }
        return out;
    }

    private Chemistry() {
    }
}
