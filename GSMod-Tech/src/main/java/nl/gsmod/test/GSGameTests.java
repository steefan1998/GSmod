package nl.gsmod.test;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.gsmod.GSMod;
import nl.gsmod.block.EnergyCellBlockEntity;
import nl.gsmod.machine.AlloySmelterBlockEntity;
import nl.gsmod.machine.CrusherBlockEntity;
import nl.gsmod.machine.GeneratorBlockEntity;
import nl.gsmod.machine.MachineBlock;
import nl.gsmod.registry.ModBlocks;
import nl.gsmod.registry.ModItems;

/** Run with {@code ./gradlew runGameTestServer}, or {@code /test runall gsmod} in a dev world. */
public final class GSGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, GSMod.MODID);
    private static final BlockPos A = new BlockPos(1, 1, 1);
    private static final BlockPos B = new BlockPos(2, 1, 1);

    static {
        FUNCTIONS.register("crusher_doubles_ore", () -> GSGameTests::crusherDoublesOre);
        FUNCTIONS.register("alloy_smelter_makes_steel", () -> GSGameTests::alloySmelterMakesSteel);
        FUNCTIONS.register("generator_charges_cell", () -> GSGameTests::generatorChargesCell);
        FUNCTIONS.register("cell_powers_crusher", () -> GSGameTests::cellPowersCrusher);
        FUNCTIONS.register("speed_upgrade_shortens_time", () -> GSGameTests::speedUpgradeShortensTime);
        FUNCTIONS.register("crusher_needs_energy", () -> GSGameTests::crusherNeedsEnergy);
    }

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(GSGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(GSMod.id("default"));
        for (var entry : FUNCTIONS.getEntries()) {
            Identifier id = entry.getId();
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(env, Identifier.withDefaultNamespace("empty"), 400, 0, true);
            event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id), data));
        }
    }

    private static void crusherDoublesOre(GameTestHelper helper) {
        helper.setBlock(A, ModBlocks.CRUSHER.get());
        CrusherBlockEntity crusher = helper.getBlockEntity(A, CrusherBlockEntity.class);
        crusher.getEnergy().generate(10_000);
        crusher.setItem(0, new ItemStack(ModItems.GALVANITE_ORE.get()));
        helper.succeedWhen(() -> {
            ItemStack out = crusher.getItem(1);
            helper.assertTrue(out.is(ModItems.GALVANITE_DUST.get()) && out.getCount() >= 2, "expected >= 2 galvanite dust, got " + out);
            helper.assertTrue(crusher.getItem(0).isEmpty(), "input not consumed");
        });
    }

    private static void alloySmelterMakesSteel(GameTestHelper helper) {
        helper.setBlock(A, ModBlocks.ALLOY_SMELTER.get());
        AlloySmelterBlockEntity smelter = helper.getBlockEntity(A, AlloySmelterBlockEntity.class);
        smelter.getEnergy().generate(15_000);
        // Reversed slot order on purpose: recipes match in either order.
        smelter.setItem(0, new ItemStack(Items.COAL, 2));
        smelter.setItem(1, new ItemStack(Items.IRON_INGOT, 1));
        helper.succeedWhen(() -> {
            helper.assertTrue(smelter.getItem(2).is(ModItems.STEEL_INGOT.get()), "no steel produced, got " + smelter.getItem(2));
            helper.assertTrue(smelter.getItem(0).isEmpty() && smelter.getItem(1).isEmpty(), "inputs not consumed");
        });
    }

    private static void generatorChargesCell(GameTestHelper helper) {
        helper.setBlock(A, ModBlocks.COMBUSTION_GENERATOR.get());
        helper.setBlock(B, ModBlocks.ENERGY_CELL.get());
        GeneratorBlockEntity generator = helper.getBlockEntity(A, GeneratorBlockEntity.class);
        generator.setItem(0, new ItemStack(Items.COAL));
        EnergyCellBlockEntity cell = helper.getBlockEntity(B, EnergyCellBlockEntity.class);
        helper.succeedWhen(() -> helper.assertTrue(cell.getEnergy().getEnergy() >= 2_000,
                "cell only has " + cell.getEnergy().getEnergy() + " FE"));
    }

    private static void cellPowersCrusher(GameTestHelper helper) {
        helper.setBlock(A, ModBlocks.ENERGY_CELL.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(B, ModBlocks.CRUSHER.get());
        EnergyCellBlockEntity cell = helper.getBlockEntity(A, EnergyCellBlockEntity.class);
        cell.getEnergy().generate(100_000);
        CrusherBlockEntity crusher = helper.getBlockEntity(B, CrusherBlockEntity.class);
        crusher.setItem(0, new ItemStack(Items.COBBLESTONE));
        helper.succeedWhen(() -> helper.assertTrue(crusher.getItem(1).is(Items.GRAVEL), "crusher produced " + crusher.getItem(1)));
    }

    private static void speedUpgradeShortensTime(GameTestHelper helper) {
        helper.setBlock(A, ModBlocks.CRUSHER.get());
        CrusherBlockEntity crusher = helper.getBlockEntity(A, CrusherBlockEntity.class);
        crusher.getEnergy().generate(20_000);
        crusher.setItem(0, new ItemStack(ModItems.GALVANITE_ORE.get(), 4));
        crusher.setItem(2, new ItemStack(ModItems.SPEED_UPGRADE.get(), 4));
        // 200 ticks base / (1 + 4 * 0.5) = 67 ticks per ore.
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(crusher.getItem(1).getCount() >= 2, "upgraded crusher too slow: " + crusher.getItem(1));
            helper.succeed();
        });
    }

    private static void crusherNeedsEnergy(GameTestHelper helper) {
        helper.setBlock(A, ModBlocks.CRUSHER.get());
        CrusherBlockEntity crusher = helper.getBlockEntity(A, CrusherBlockEntity.class);
        crusher.setItem(0, new ItemStack(ModItems.GALVANITE_ORE.get()));
        helper.runAfterDelay(250, () -> {
            helper.assertTrue(crusher.getItem(1).isEmpty(), "crusher worked without power");
            helper.succeed();
        });
    }

    private GSGameTests() {}
}
