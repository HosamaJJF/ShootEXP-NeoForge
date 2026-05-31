package moe.feo.shootexp.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import moe.feo.shootexp.Config;
import moe.feo.shootexp.item.ExpItem;
import moe.feo.shootexp.mechanic.PlayerStatus;
import moe.feo.shootexp.mechanic.PlayerStatusManager;
import moe.feo.shootexp.util.ShootExpUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.function.Predicate;

public class ShootExpCommand {

    private static final Predicate<CommandSourceStack> REQUIRE_OP = s -> {
        if (s.permissions() instanceof LevelBasedPermissionSet levelSet) {
            return levelSet.level().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS);
        }
        return false;
    };

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        var root = Commands.literal("shootexp");

        // /shootexp help
        root.then(Commands.literal("help").executes(this::help));

        // /shootexp status [player]
        root.then(Commands.literal("status")
                .executes(ctx -> status(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(REQUIRE_OP)
                        .executes(ctx -> status(ctx, EntityArgument.getPlayer(ctx, "player")))));

        // /shootexp item <owner> <recipient> <amount>
        root.then(Commands.literal("item")
                .requires(REQUIRE_OP)
                .then(Commands.argument("owner", EntityArgument.player())
                        .then(Commands.argument("recipient", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(this::giveItem)))));

        // /shootexp restore all <player>
        root.then(Commands.literal("restore")
                .requires(REQUIRE_OP)
                .then(Commands.literal("all")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(this::restoreAll)))
                .then(Commands.literal("times")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                        .executes(this::restoreTimes))))
                .then(Commands.literal("stock")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(this::restoreStock)))));

        // /shootexp set <player> <times> <stock>
        root.then(Commands.literal("set")
                .requires(REQUIRE_OP)
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("times", IntegerArgumentType.integer(0))
                                .then(Commands.argument("stock", IntegerArgumentType.integer(0))
                                        .executes(this::set)))));

        // /shootexp reload
        root.then(Commands.literal("reload")
                .requires(REQUIRE_OP)
                .executes(this::reload));

        dispatcher.register(root);
    }

    private int help(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        source.sendSystemMessage(ShootExpUtil.formatComponent(ShootExpUtil.lang("shootexp.help.header")));
        for (String key : new String[]{
                "shootexp.help.help", "shootexp.help.status", "shootexp.help.item",
                "shootexp.help.restore", "shootexp.help.restore_times",
                "shootexp.help.restore_stock", "shootexp.help.set", "shootexp.help.reload"}) {
            source.sendSystemMessage(ShootExpUtil.formatComponent(ShootExpUtil.lang(key)));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int status(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        PlayerStatus status = PlayerStatusManager.get(target.getUUID());
        String msg = ShootExpUtil.lang("shootexp.message.status")
                .replace("%PLAYER%", target.getName().getString())
                .replace("%TIMES%", String.valueOf(status.getTimesOfShoot()))
                .replace("%STOCK%", String.valueOf(status.getStock()));
        ctx.getSource().sendSystemMessage(ShootExpUtil.formatComponent(msg));
        return Command.SINGLE_SUCCESS;
    }

    private int giveItem(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer owner = EntityArgument.getPlayer(ctx, "owner");
        ServerPlayer recipient = EntityArgument.getPlayer(ctx, "recipient");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");

        var item = ExpItem.create(owner.getName().getString(), recipient.getName().getString(), amount);
        if (!ctx.getSource().getPlayerOrException().getInventory().add(item)) {
            ctx.getSource().getPlayerOrException().drop(item, false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int restoreAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        PlayerStatusManager.get(target.getUUID()).restoreAll();
        String msg = ShootExpUtil.lang("shootexp.message.restore_all")
                .replace("%PLAYER%", target.getName().getString());
        ctx.getSource().sendSystemMessage(ShootExpUtil.formatComponent(msg));
        return Command.SINGLE_SUCCESS;
    }

    private int restoreTimes(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int count = IntegerArgumentType.getInteger(ctx, "count");
        PlayerStatusManager.get(target.getUUID()).addShootTimes(-count);
        String msg = ShootExpUtil.lang("shootexp.message.restore_times")
                .replace("%PLAYER%", target.getName().getString())
                .replace("%AMOUNT%", String.valueOf(count));
        ctx.getSource().sendSystemMessage(ShootExpUtil.formatComponent(msg));
        return Command.SINGLE_SUCCESS;
    }

    private int restoreStock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        PlayerStatusManager.get(target.getUUID()).addStock(amount);
        String msg = ShootExpUtil.lang("shootexp.message.restore_stock")
                .replace("%PLAYER%", target.getName().getString())
                .replace("%AMOUNT%", String.valueOf(amount));
        ctx.getSource().sendSystemMessage(ShootExpUtil.formatComponent(msg));
        return Command.SINGLE_SUCCESS;
    }

    private int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int times = IntegerArgumentType.getInteger(ctx, "times");
        int stock = IntegerArgumentType.getInteger(ctx, "stock");
        PlayerStatus status = PlayerStatusManager.get(target.getUUID());
        status.setTimesOfShoot(times);
        status.setStock(stock);
        String msg = ShootExpUtil.lang("shootexp.message.set")
                .replace("%PLAYER%", target.getName().getString())
                .replace("%TIMES%", String.valueOf(times))
                .replace("%STOCK%", String.valueOf(stock));
        ctx.getSource().sendSystemMessage(ShootExpUtil.formatComponent(msg));
        return Command.SINGLE_SUCCESS;
    }

    private int reload(CommandContext<CommandSourceStack> ctx) {
        ShootExpUtil.clearLangCache();
        Config.load();
        ctx.getSource().sendSystemMessage(ShootExpUtil.formatComponent(
                ShootExpUtil.lang("shootexp.message.reload")));
        return Command.SINGLE_SUCCESS;
    }
}
