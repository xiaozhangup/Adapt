package com.volmit.adapt.content.protector;

import com.volmit.adapt.api.adaptation.Adaptation;
import com.volmit.adapt.api.protection.Protector;
import me.xiaozhangup.slimecargo.SlimeCargoNext;
import me.xiaozhangup.slimecargo.objects.enums.ProtectType;
import me.xiaozhangup.slimecargo.protect.SlimeProtect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SlimeCargoProtector implements Protector {
    private final SlimeProtect protect = SlimeCargoNext.slimeProtect;

    @Override
    public boolean canBlockBreak(Player player, Location blockLocation, Adaptation<?> adaptation) {
        return protect.hasPermission(ProtectType.BREAK, player, blockLocation, null, null, true) != SlimeProtect.Result.DENY;
    }

    @Override
    public boolean canBlockPlace(Player player, Location blockLocation, Adaptation<?> adaptation) {
        return protect.hasPermission(ProtectType.BUILD, player, blockLocation, null, null, true) != SlimeProtect.Result.DENY;
    }

    @Override
    public boolean canPVP(Player player, Location entityLocation, Adaptation<?> adaptation) {
        return protect.hasPermission(ProtectType.ATTACK, player, entityLocation, null, null, true) != SlimeProtect.Result.DENY;
    }

    @Override
    public boolean canPVE(Player player, Location entityLocation, Adaptation<?> adaptation) {
        return protect.hasPermission(ProtectType.ATTACK, player, entityLocation, null, null, true) != SlimeProtect.Result.DENY;
    }

    @Override
    public boolean canInteract(Player player, Location targetLocation, Adaptation<?> adaptation) {
        return protect.hasPermission(ProtectType.INTERACT_BLOCK, player, targetLocation, null, null, true) != SlimeProtect.Result.DENY;
    }

    @Override
    public boolean canAccessChest(Player player, Location chestLocation, Adaptation<?> adaptation) {
        return protect.hasPermission(ProtectType.CONTAINER, player, chestLocation, null, null, true) != SlimeProtect.Result.DENY;
    }

    @Override
    public String getName() {
        return "SlimeCargo";
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

}
