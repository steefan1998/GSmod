package nl.gsmod.stages.compat;

import java.util.List;

import org.jspecify.annotations.Nullable;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import nl.gsmod.stages.GSModStages;
import nl.gsmod.stages.Stages;
import nl.gsmod.stages.client.ClientStages;

/** Hides locked items in JEI while the player has no access, and puts them back once they unlock the stage. */
@JeiPlugin
public class JeiStagesPlugin implements IModPlugin {
    private static @Nullable IJeiRuntime runtime;
    private static List<ItemStack> hidden = List.of();

    @Override
    public Identifier getPluginUid() {
        return GSModStages.id("jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        hidden = List.of();
        refresh();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        hidden = List.of();
    }

    /** Called on the client thread whenever the player's stage changes. */
    public static void refresh() {
        if (runtime == null) {
            return;
        }
        IIngredientManager ingredients = runtime.getIngredientManager();
        if (!hidden.isEmpty()) {
            ingredients.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
            hidden = List.of();
        }
        if (!ClientStages.access()) {
            List<ItemStack> locked = ingredients.getAllIngredients(VanillaTypes.ITEM_STACK).stream().filter(Stages::isLocked).map(ItemStack::copy).toList();
            if (!locked.isEmpty()) {
                ingredients.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, locked);
                hidden = locked;
            }
            GSModStages.LOGGER.debug("Hid {} locked items in JEI", locked.size());
        }
    }
}
