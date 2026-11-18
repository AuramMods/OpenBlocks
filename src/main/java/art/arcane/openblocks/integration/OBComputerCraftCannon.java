package art.arcane.openblocks.integration;

import art.arcane.openblocks.block.entity.OBCannonBlockEntity;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dan200.computercraft.api.peripheral.PeripheralType;

public final class OBComputerCraftCannon implements GenericPeripheral {

    public static void register() {
        ComputerCraftAPI.registerGenericSource(new OBComputerCraftCannon());
    }

    @Override public String id() { return "open_blocks:cannon"; }
    @Override public PeripheralType getType() { return PeripheralType.ofAdditional("openblocks_cannon"); }

    @LuaFunction(mainThread = true)
    public final void setYaw(final OBCannonBlockEntity cannon, final double yaw) { cannon.setYaw(yaw); }

    @LuaFunction(mainThread = true)
    public final void setPitch(final OBCannonBlockEntity cannon, final double pitch) { cannon.setPitch(pitch); }

    @LuaFunction(mainThread = true)
    public final void setSpeed(final OBCannonBlockEntity cannon, final double speed) { cannon.setSpeed(speed); }
}
