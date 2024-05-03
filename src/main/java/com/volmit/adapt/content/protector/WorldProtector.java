package com.volmit.adapt.content.protector;

import com.volmit.adapt.api.adaptation.Adaptation;
import com.volmit.adapt.api.protection.Protector;
import me.xiaozhangup.domain.OrangDomain;
import me.xiaozhangup.domain.common.poly.BarrierPoly;
import me.xiaozhangup.domain.utils.LemonUtilsKt;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class WorldProtector implements Protector {
    private final List<String> worlds = OrangDomain.INSTANCE.getWorlds();

    @Override
    public boolean canBlockBreak(Player player, Location blockLocation, Adaptation<?> adaptation) {
        return player.isOp() || !worlds.contains(blockLocation.getWorld().getName());
    }

    @Override
    public boolean canBlockPlace(Player player, Location blockLocation, Adaptation<?> adaptation) {
        return player.isOp() || !worlds.contains(blockLocation.getWorld().getName());
    }

    @Override
    public boolean canPVP(Player player, Location entityLocation, Adaptation<?> adaptation) {
        return player.isOp() || !worlds.contains(entityLocation.getWorld().getName());
    }

    @Override
    public boolean canPVE(Player player, Location entityLocation, Adaptation<?> adaptation) {
        return player.isOp() || !worlds.contains(entityLocation.getWorld().getName());
    }

    @Override
    public boolean canInteract(Player player, Location targetLocation, Adaptation<?> adaptation) {
        return player.isOp() || !worlds.contains(targetLocation.getWorld().getName());
    }

    @Override
    public boolean canAccessChest(Player player, Location chestLocation, Adaptation<?> adaptation) {
        return player.isOp() || !worlds.contains(chestLocation.getWorld().getName());
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
