package com.volmit.adapt.content.protector;

import com.volmit.adapt.api.adaptation.Adaptation;
import com.volmit.adapt.api.protection.Protector;
import me.xiaozhangup.domain.poly.Poly;
import me.xiaozhangup.domain.utils.LemonUtilsKt;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class OrangDomainProtector implements Protector {

    @Override
    public boolean canBlockBreak(Player player, Block block, Adaptation<?> adaptation) {
        Poly poly = LemonUtilsKt.getPoly(block.getLocation());
        if (poly == null)
            return true;
        return poly.hasPermission("build", null, false);
    }

    @Override
    public boolean canBlockPlace(Player player, Block block, Adaptation<?> adaptation) {
        Poly poly = LemonUtilsKt.getPoly(block.getLocation());
        if (poly == null)
            return true;
        return poly.hasPermission("build", null, false);
    }

    @Override
    public boolean canPVP(Player player, Location entityLocation, Adaptation<?> adaptation) {
        Poly poly = LemonUtilsKt.getPoly(entityLocation);
        if (poly == null)
            return true;
        return poly.hasPermission("pvp", null, false);
    }

    @Override
    public boolean canPVE(Player player, Location entityLocation, Adaptation<?> adaptation) {
        Poly poly = LemonUtilsKt.getPoly(entityLocation);
        if (poly == null)
            return true;
        return poly.hasPermission("pvp", null, false);
    }

    @Override
    public boolean canInteract(Player player, Location targetLocation, Adaptation<?> adaptation) {
        Poly poly = LemonUtilsKt.getPoly(targetLocation);
        if (poly == null)
            return true;
        return poly.hasPermission("interact", null, false);
    }

    @Override
    public boolean canAccessChest(Player player, Location chestLocation, Adaptation<?> adaptation) {
        Poly poly = LemonUtilsKt.getPoly(chestLocation);
        if (poly == null)
            return true;
        return poly.hasPermission("interact", null, false);
    }

    @Override
    public String getName() {
        return "OrangDomain";
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

}
