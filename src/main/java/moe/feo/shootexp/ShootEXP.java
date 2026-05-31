package moe.feo.shootexp;

import com.mojang.logging.LogUtils;
import moe.feo.shootexp.command.ShootExpCommand;
import moe.feo.shootexp.event.AttackHandler;
import moe.feo.shootexp.event.EatHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(ShootEXP.MOD_ID)
public class ShootEXP {

    public static final String MOD_ID = "shootexp";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ShootEXP(IEventBus modEventBus, ModContainer modContainer) {
        Config.load();

        NeoForge.EVENT_BUS.register(new AttackHandler());
        NeoForge.EVENT_BUS.register(new EatHandler());
        NeoForge.EVENT_BUS.register(new ShootExpCommand());
    }
}
