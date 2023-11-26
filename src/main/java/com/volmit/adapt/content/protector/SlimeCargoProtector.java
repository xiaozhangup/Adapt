package com.volmit.adapt.content.protector;

import com.volmit.adapt.api.adaptation.Adaptation;
import com.volmit.adapt.api.protection.Protector;
import me.xiaozhangup.slimecargo.listeners.protect.utils.ActionType;
import me.xiaozhangup.slimecargo.listeners.protect.utils.CheckUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

public class SlimeCargoProtector implements Protector {

    @Override
    public boolean canBlockBreak(Player player, Location blockLocation, Adaptation<?> adaptation) {
        return CheckUtils.INSTANCE.hasPermission(player, blockLocation, ActionType.BREAK);
    }

    @Override
    public boolean canBlockPlace(Player player, Location blockLocation, Adaptation<?> adaptation) {
        return CheckUtils.INSTANCE.hasPermission(player, blockLocation, ActionType.PLACE);
    }

    @Override
    public boolean canPVP(Player player, Location entityLocation, Adaptation<?> adaptation) {
        return CheckUtils.INSTANCE.hasPermission(player, entityLocation, ActionType.KILL);
    }

    @Override
    public boolean canPVE(Player player, Location entityLocation, Adaptation<?> adaptation) {
        return CheckUtils.INSTANCE.hasPermission(player, entityLocation, ActionType.KILL);
    }

    @Override
    public boolean canInteract(Player player, Location targetLocation, Adaptation<?> adaptation) {
        return CheckUtils.INSTANCE.hasPermission(player, targetLocation, ActionType.INTERACT);
    }

    @Override
    public boolean canAccessChest(Player player, Location chestLocation, Adaptation<?> adaptation) {
        return CheckUtils.INSTANCE.hasPermission(player, chestLocation, ActionType.OPEN);
    }

    @Override
    public String getName() {
        return "SlimeCargo";
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

}
