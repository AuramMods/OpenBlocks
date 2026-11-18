package art.arcane.openblocks.integration;

import art.arcane.openblocks.block.entity.OBDonationStationBlockEntity;
import art.arcane.openblocks.world.OBDonationMetadata;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dan200.computercraft.api.peripheral.PeripheralType;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public final class OBComputerCraftDonations implements GenericPeripheral {

    public static void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ComputerCraftAPI.registerGenericSource(new OBComputerCraftDonations()));
    }

    @Override public String id() { return "open_blocks:donation_station"; }
    @Override public PeripheralType getType() { return PeripheralType.ofAdditional("openblocks_donation"); }

    @LuaFunction(mainThread = true)
    public final Object[] getItemAuthor(final OBDonationStationBlockEntity station) {
        final var mod = OBDonationMetadata.identify(station.items().getStackInSlot(0));
        return mod == null ? new Object[] {null, null} : new Object[] {mod.name(), mod.authors()};
    }
}
