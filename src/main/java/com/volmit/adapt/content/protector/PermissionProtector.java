package com.volmit.adapt.content.protector;

import com.volmit.adapt.api.adaptation.Adaptation;
import com.volmit.adapt.api.protection.Protector;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PermissionProtector implements Protector {

    @Override
    public boolean canBlockBreak(Player player, Location blockLocation, Adaptation<?> adaptation) {
        return player.isOp();
    }

    @Override
    public boolean canBlockPlace(Player player, Location blockLocation, Adaptation<?> adaptation) {
        return player.isOp();
    }

    @Override
    public boolean canPVP(Player player, Location entityLocation, Adaptation<?> adaptation) {
        return player.isOp();
    }

    @Override
    public boolean canPVE(Player player, Location entityLocation, Adaptation<?> adaptation) {
        return player.isOp();
    }

    @Override
    public boolean canInteract(Player player, Location targetLocation, Adaptation<?> adaptation) {
        return player.isOp();
    }

    @Override
    public boolean canAccessChest(Player player, Location chestLocation, Adaptation<?> adaptation) {
        return player.isOp();
    }

    @Override
    public String getName() {
        return "Permission";
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

}
