package com.gmail.nossr50.commands.player;

import com.gmail.nossr50.config.MainConfig;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.runnables.commands.MctopCommandAsyncTask;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.StringUtils;
import com.gmail.nossr50.util.commands.CommandUtils;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.scoreboards.ScoreboardManager;
import com.google.common.collect.ImmutableList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class MctopCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        PrimarySkillType skill = null;

        switch (args.length) {
            case 0:
                display(1, skill, sender, command);
                return true;

            case 1:
                if (StringUtils.isInt(args[0])) {
                    display(Math.abs(Integer.parseInt(args[0])), skill, sender, command);
                    return true;
                }

                skill = extractSkill(sender, args[0]);

                if (skill == null) {
                    return true;
                }

                display(1, skill, sender, command);
                return true;

            case 2:
                if (CommandUtils.isInvalidInteger(sender, args[1])) {
                    return true;
                }

                skill = extractSkill(sender, args[0]);

                if (skill == null) {
                    return true;
                }

                display(Math.abs(Integer.parseInt(args[1])), skill, sender, command);
                return true;

            default:
                return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        switch (args.length) {
            case 1:
                return StringUtil.copyPartialMatches(args[0], PrimarySkillType.SKILL_NAMES, new ArrayList<>(PrimarySkillType.SKILL_NAMES.size()));
            default:
                return ImmutableList.of();
        }
    }

    private void display(int page, PrimarySkillType skill, CommandSender sender, Command command) {
        if (skill != null && !Permissions.mctop(sender, skill)) {
            sender.sendMessage(command.getPermissionMessage());
            return;
        }

        if (sender instanceof Player) {
            if (!CommandUtils.hasPlayerDataKey(sender)) {
                return;
            }

            McMMOPlayer mcMMOPlayer = UserManager.getPlayer(sender.getName());
            long cooldownMillis = Math.max(MainConfig.getInstance().getDatabasePlayerCooldown(), 1750);

            if (mcMMOPlayer.getDatabaseATS() + cooldownMillis > System.currentTimeMillis()) {
                double seconds = ((mcMMOPlayer.getDatabaseATS() + cooldownMillis) - System.currentTimeMillis()) / 1000;
                if (seconds < 1) {
                    seconds = 1;
                }

                sender.sendMessage(LocaleLoader.formatString(LocaleLoader.getString("Commands.Database.Cooldown"), seconds));
                return;
            }

            if (((Player) sender).hasMetadata(mcMMO.DATABASE_PROCESSING_COMMAND_METAKEY)) {
                sender.sendMessage(LocaleLoader.getString("Commands.Database.Processing"));
                return;
            } else {
                ((Player) sender).setMetadata(mcMMO.DATABASE_PROCESSING_COMMAND_METAKEY, new FixedMetadataValue(mcMMO.p, null));
            }

            mcMMOPlayer.actualizeDatabaseATS();
        }

        display(page, skill, sender);
    }

    private void display(int page, PrimarySkillType skill, CommandSender sender) {
        boolean useBoard = (sender instanceof Player) && (mcMMO.getScoreboardSettings().isScoreboardEnabled(ScoreboardManager.SidebarType.TOP_BOARD));
        boolean useChat = !useBoard || mcMMO.getScoreboardSettings().isScoreboardPrinting(ScoreboardManager.SidebarType.TOP_BOARD);

        new MctopCommandAsyncTask(page, skill, sender, useBoard, useChat).runTaskAsynchronously(mcMMO.p);
    }

    private PrimarySkillType extractSkill(CommandSender sender, String skillName) {
        if (CommandUtils.isInvalidSkill(sender, skillName)) {
            return null;
        }

        PrimarySkillType skill = PrimarySkillType.getSkill(skillName);

        if (CommandUtils.isChildSkill(sender, skill)) {
            return null;
        }

        return skill;
    }
}
