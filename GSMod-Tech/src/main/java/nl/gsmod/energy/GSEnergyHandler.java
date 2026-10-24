package nl.gsmod.energy;

import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * {@link SimpleEnergyHandler} whose capacity can change at runtime (capacity upgrades, config)
 * and which reports changes so the owning block entity can be marked dirty.
 */
public class GSEnergyHandler extends SimpleEnergyHandler {
    private final Runnable onChanged;

    public GSEnergyHandler(int capacity, int maxInsert, int maxExtract, Runnable onChanged) {
        super(capacity, maxInsert, maxExtract);
        this.onChanged = onChanged;
    }

    public int getEnergy() {
        return energy;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = Math.max(0, capacity);
        if (energy > this.capacity) {
            set(this.capacity);
        }
    }

    public void setLimits(int maxInsert, int maxExtract) {
        this.maxInsert = Math.max(0, maxInsert);
        this.maxExtract = Math.max(0, maxExtract);
    }

    /** Internal production, ignores the insert limit. Returns the amount actually added. */
    public int generate(int amount) {
        int added = Math.max(0, Math.min(amount, capacity - energy));
        if (added > 0) {
            set(energy + added);
        }
        return added;
    }

    /** Internal consumption, ignores the extract limit. */
    public boolean consume(int amount) {
        if (amount > energy) {
            return false;
        }
        if (amount > 0) {
            set(energy - amount);
        }
        return true;
    }

    @Override
    protected void onEnergyChanged(int previousAmount) {
        onChanged.run();
    }
}
