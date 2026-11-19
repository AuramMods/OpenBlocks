package art.arcane.openblocks.integration;

import art.arcane.openblocks.block.entity.OBGuideBlockEntity;
import art.arcane.openblocks.guide.OBGuideShape;
import art.arcane.openblocks.guide.OBGuideShape.Bounds;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dan200.computercraft.api.peripheral.PeripheralType;
import java.util.Map;

public final class OBComputerCraftGuide implements GenericPeripheral {

    public static void register() { ComputerCraftAPI.registerGenericSource(new OBComputerCraftGuide()); }
    @Override public String id() { return "open_blocks:guide"; }
    @Override public PeripheralType getType() { return PeripheralType.ofAdditional("openblocks_guide"); }

    @LuaFunction(mainThread = true)
    public final Map<String, Integer> getSize(final OBGuideBlockEntity guide) {
        final Bounds b = guide.bounds();
        return Map.of("negX", b.negX(), "negY", b.negY(), "negZ", b.negZ(), "posX", b.posX(), "posY", b.posY(), "posZ", b.posZ());
    }

    @LuaFunction(mainThread = true)
    public final void setSize(final OBGuideBlockEntity guide, final Map<?, ?> size) throws LuaException {
        guide.setBounds(new Bounds(readSize(size, "negX"), readSize(size, "negY"), readSize(size, "negZ"),
                readSize(size, "posX"), readSize(size, "posY"), readSize(size, "posZ")));
    }

    @LuaFunction(mainThread = true)
    public final int getColor(final OBGuideBlockEntity guide) { return guide.color(); }

    @LuaFunction(mainThread = true)
    public final void setColor(final OBGuideBlockEntity guide, final int rgb) { guide.setColor(rgb); }

    @LuaFunction(mainThread = true)
    public final int getCount(final OBGuideBlockEntity guide) { return guide.points().size(); }

    @LuaFunction(mainThread = true)
    public final String getShape(final OBGuideBlockEntity guide) { return guide.shape().id(); }

    @LuaFunction(mainThread = true)
    public final void setShape(final OBGuideBlockEntity guide, final String id) throws LuaException {
        final OBGuideShape shape = OBGuideShape.byName(id);
        if (shape == null) throw new LuaException("Unknown guide shape: " + id);
        guide.setShape(shape);
    }

    @LuaFunction(value = {"incrementMode", "cycleShape"}, mainThread = true)
    public final String incrementMode(final OBGuideBlockEntity guide) {
        guide.setShape(OBGuideShape.byIndex(guide.shape().ordinal() + 1));
        return guide.shape().id();
    }

    @LuaFunction(mainThread = true)
    public final String decrementMode(final OBGuideBlockEntity guide) {
        guide.setShape(OBGuideShape.byIndex(guide.shape().ordinal() - 1));
        return guide.shape().id();
    }

    private static int readSize(final Map<?, ?> size, final String key) throws LuaException {
        final Object raw = size.get(key);
        final int cap = OBGuideBlockEntity.sizeLimit();
        if (raw instanceof Number value && value.doubleValue() == value.intValue() && value.intValue() >= 0 && value.intValue() <= cap) {
            return value.intValue();
        }
        throw new LuaException(key + " must be an integer from 0 to " + cap);
    }
}
