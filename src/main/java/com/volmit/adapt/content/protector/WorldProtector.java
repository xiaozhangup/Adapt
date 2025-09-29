package com.volmit.adapt.content.protector;

import com.volmit.adapt.api.adaptation.Adaptation;
import com.volmit.adapt.api.protection.Protector;
import me.xiaozhangup.domain.OrangDomain;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class WorldProtector implements Protector {
    @Override
    public boolean canBlockBreak(Player player, Block block, Adaptation<?> adaptation) {
        return player.isOp() || !OrangDomain.INSTANCE.getWorlds().contains(block.getWorld().getName());
    }

    @Override
    public boolean canBlockPlace(Player player, Block block, Adaptation<?> adaptation) {
        return player.isOp() || !OrangDomain.INSTANCE.getWorlds().contains(block.getWorld().getName());
    }

    @Override
    public boolean canPVP(Player player, Location entityLocation, Adaptation<?> adaptation) {
        return player.isOp() || !OrangDomain.INSTANCE.getWorlds().contains(entityLocation.getWorld().getName());
    }

    @Override
    public boolean canPVE(Player player, Location entityLocation, Adaptation<?> adaptation) {
        return player.isOp() || !OrangDomain.INSTANCE.getWorlds().contains(entityLocation.getWorld().getName());
    }

    @Override
    public boolean canInteract(Player player, Location targetLocation, Adaptation<?> adaptation) {
        return player.isOp() || !OrangDomain.INSTANCE.getWorlds().contains(targetLocation.getWorld().getName());
    }

    @Override
    public boolean canAccessChest(Player player, Location chestLocation, Adaptation<?> adaptation) {
        return player.isOp() || !OrangDomain.INSTANCE.getWorlds().contains(chestLocation.getWorld().getName());
    }

    @Override
    public String getName() {
        return "World";
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

}
