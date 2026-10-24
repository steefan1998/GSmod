package nl.gsmod.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.gsmod.GSMod;
import nl.gsmod.machine.MachineType;
import nl.gsmod.menu.MachineMenu;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, GSMod.MODID);

    public static final Supplier<MenuType<MachineMenu>> COMBUSTION_GENERATOR = machine("combustion_generator", MachineType.GENERATOR);
    public static final Supplier<MenuType<MachineMenu>> CRUSHER = machine("crusher", MachineType.CRUSHER);
    public static final Supplier<MenuType<MachineMenu>> ALLOY_SMELTER = machine("alloy_smelter", MachineType.ALLOY_SMELTER);

    public static MenuType<MachineMenu> forType(MachineType type) {
        return switch (type) {
            case GENERATOR -> COMBUSTION_GENERATOR.get();
            case CRUSHER -> CRUSHER.get();
            case ALLOY_SMELTER -> ALLOY_SMELTER.get();
        };
    }

    private static Supplier<MenuType<MachineMenu>> machine(String name, MachineType type) {
        return MENUS.register(name, () -> new MenuType<>((id, inventory) -> new MachineMenu(type, id, inventory), FeatureFlags.VANILLA_SET));
    }

    private ModMenus() {}
}
