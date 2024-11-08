package com.TryNotDying.Sinon;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.command.SlashCommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.TryNotDying.Sinon.commands.AboutCommand;
import com.TryNotDying.Sinon.commands.PingCommand;
import com.TryNotDying.Sinon.commands.admin.*;
import com.TryNotDying.Sinon.commands.dj.*;
import com.TryNotDying.Sinon.commands.general.*;
import com.TryNotDying.Sinon.commands.music.*;
import com.TryNotDying.Sinon.commands.owner.*;
import com.TryNotDying.Sinon.settings.SettingsManager;
import com.TryNotDying.Sinon.utils.ColorPalette;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import java.awt.Color;
import java.util.Arrays;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.Command.Type;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import javax.annotation.Nonnull;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class CommandClient extends ListenerAdapter {
    private final JDA jda;
    private final Bot bot;

    public CommandClient(Bot bot) {
        this.jda = bot.getJDA();
        this.bot = bot;
    }

    public void registerCommands() {
        // Create a SlashCommandClientBuilder
        SlashCommandClientBuilder cb = new SlashCommandClientBuilder()
                .setOwnerId(Long.toString(bot.getConfig().getOwnerId()))
                .setEmojis(bot.getConfig().getSuccess(), bot.getConfig().getWarning(), bot.getConfig().getError())
                .setHelpWord(bot.getConfig().getHelp())
                .setLinkedCacheSize(200)
                .setGuildSettingsManager(bot.getSettingsManager());

        // Define commands using CommandData
        Command[] commands = {
            new CommandData("about", "Get information about this bot.", Type.CHAT_INPUT),
            new CommandData("ping", "Ping the bot to check its latency.", Type.CHAT_INPUT),
            new CommandData("play", "Plays a song.", Type.CHAT_INPUT)
                    .addOption(OptionType.STRING, "song", "Song name or URL", true),
            new CommandData("queue", "Show the current queue.", Type.CHAT_INPUT),
            new CommandData("skip", "Skip the current song.", Type.CHAT_INPUT),
            new CommandData("search", "Search for a song.", Type.CHAT_INPUT)
                    .addOption(OptionType.STRING, "song", "Song name or URL", true),
            new CommandData("scsearch", "Search for a song on SoundCloud.", Type.CHAT_INPUT)
                    .addOption(OptionType.STRING, "song", "Song name or URL", true),
            new CommandData("playlists", "Manage playlists.", Type.CHAT_INPUT)
                    .addOption(OptionType.STRING, "playlist", "Playlist name", true)
                    .addOptions(
                            new OptionData(OptionType.STRING, "action", "Action to perform", true)
                                    .addChoices(
                                            new Choice("play", "play"),
                                            new Choice("remove", "remove")
                                    )
                    ),
            new CommandData("shuffle", "Shuffle the queue.", Type.CHAT_INPUT),
            new CommandData("seek", "Seek to a specific position in the song.", Type.CHAT_INPUT)
                    .addOption(OptionType.INTEGER, "position", "Position to seek", true),
            new CommandData("lyrics", "Show lyrics for the current song.", Type.CHAT_INPUT)
                    .addOption(OptionType.STRING, "song", "Song name", true),
            new CommandData("nowplaying", "Show the currently playing song.", Type.CHAT_INPUT),
            new CommandData("forceremove", "Force remove a song from the queue.", Type.CHAT_INPUT)
                    .addOption(OptionType.USER, "user", "User to remove", true),
            new CommandData("forceskip", "Force skip the current song.", Type.CHAT_INPUT),
            new CommandData("movetrack", "Move a song in the queue.", Type.CHAT_INPUT)
                    .addOption(OptionType.INTEGER, "from", "The original position", true)
                    .addOption(OptionType.INTEGER, "to", "The new position", true),
            new CommandData("pause", "Pause the music.", Type.CHAT_INPUT),
            new CommandData("playnext", "Play the next song in the queue.", Type.CHAT_INPUT)
                    .addOption(OptionType.STRING, "song", "Song name or URL", true),
            new CommandData("repeat", "Repeat the current song.", Type.CHAT_INPUT)
                    .addOption(OptionType.STRING, "mode", "Repeat mode (off, all, single)", false),
            new CommandData("skipto", "Skip to a specific song in the queue.", Type.CHAT_INPUT)
                    .addOption(OptionType.INTEGER, "position", "Position to skip to", true),
            new CommandData("stop", "Stop the music.", Type.CHAT_INPUT),
            new CommandData("volume", "Set the volume.", Type.CHAT_INPUT)
                    .addOption(OptionType.INTEGER, "volume", "Volume level (5-150)", false),
            new CommandData("queuetype", "Set the queue type.", Type.CHAT_INPUT)
                    .addOption(OptionType.STRING, "type", "Queue type", false),
            new CommandData("skipratio", "Set the skip ratio.", Type.CHAT_INPUT)
                    .addOption(OptionType.INTEGER, "percentage", "Skip percentage (0 - 100)", true),
            new CommandData("setdj", "Set the DJ role.", Type.CHAT_INPUT)
                    .addOption(OptionType.USER, "user", "User to set DJ role for", true),
            new CommandData("settc", "Set the text channel to stay in.", Type.CHAT_INPUT)
                    .addOption(OptionType.CHANNEL, "channel", "Text channel", true),
            new CommandData("setvc", "Set the voice channel to stay in.", Type.CHAT_INPUT)
                    .addOption(OptionType.CHANNEL, "channel", "Voice channel", true),
            new CommandData("autoplaylist", "Set an auto-playlist.", Type.CHAT_INPUT)
                    .addOption(OptionType.STRING, "playlist", "Playlist name (or NONE to clear)", true),
            new CommandData("debug", "Run a debug command.", Type.CHAT_INPUT),
            new CommandData("playlist", "Manage playlists.", Type.CHAT_INPUT)
                    .addOption(OptionType.STRING, "action", "Action to perform", true)
                    .addOptions(
                            new OptionData(OptionType.STRING, "playlist", "Playlist name", true)
                                    .addChoices(
                                            new Choice("play", "play"),
                                            new Choice("remove", "remove")
                                    )
                    ),
            new CommandData("setavatar", "Set the bot's avatar.", Type.CHAT_INPUT)
                    .addOption(OptionType.STRING, "url", "URL of the image to set as avatar", true),
            new CommandData("setgame", "Set the bot's game.", Type.CHAT_INPUT)
                    .addOption(OptionType.STRING, "action", "Action to perform", true)
                    .addOption(OptionType.STRING, "text", "Game name or Stream information", true),
            new CommandData("setname", "Set the bot's name.", Type.CHAT_INPUT)
                    .addOption(OptionType.STRING, "name", "New name for the bot", true),
            new CommandData("setstatus", "Set the bot's status.", Type.CHAT_INPUT)
                    .addOption(OptionType.STRING, "status", "New status for the bot", true),
            new CommandData("shutdown", "Shut down the bot.", Type.CHAT_INPUT),
            new CommandData("eval", "Evaluates nashorn code.", Type.CHAT_INPUT)
                    .addOption(OptionType.STRING, "code", "Code to evaluate", true)

        };
        // Add the commands to the builder
        cb.addCommands(commands);

        // enable eval if applicable
        if (bot.getConfig().useEval()) {
            cb.addCommand(new EvalCmd(bot));
        }

        // set status if set in config
        if (bot.getConfig().getStatus() != OnlineStatus.UNKNOWN) {
            cb.setStatus(bot.getConfig().getStatus());
        }

        // set game
        if (bot.getConfig().getGame() == null) {
            cb.useDefaultGame();
        } else if (bot.getConfig().isGameNone()) {
            cb.setActivity(null);
        } else {
            cb.setActivity(bot.getConfig().getGame());
        }

        return cb.build();
    }

    // Implement the onSlashCommandEvent
    @Override
    public void onSlashCommandEvent(@Nonnull SlashCommandEvent event) {
        String commandName = event.getName();

        // Centralized role checks
        if (checkAdminPermission(event)) {
            switch (commandName) {
                case "queuetype":
                    // ... Implement your logic for the queuetype command ...
                    break;
                case "setskip":
                    // ... Implement your logic for the setskip command ...
                    break;
                case "settc":
                    // ... Implement your logic for the settc command ...
                    break;
                case "setvc":
                    // ... Implement your logic for the setvc command ...
                    break;
                case "setdj":
                    // ... Implement your logic for the setdj command ...
                    break;
                default:
                    // Handle cases for other commands if needed
                    break;
            }
        } else if (checkDJPermission(event)) {
            switch (commandName) {
                case "forceremove":
                    // ... Implement your logic for the forceremove command ...
                    break;
                case "forceskip":
                    // ... Implement your logic for the forceskip command ...
                    break;
                case "movetrack":
                    // ... Implement your logic for the movetrack command ...
                    break;
                case "playnext":
                    // ... Implement your logic for the playnext command ...
                    break;
                case "repeat":
                    // ... Implement your logic for the repeat command ...
                    break;
                case "skipto":
                    // ... Implement your logic for the skipto command ...
                    break;
                case "volume":
                    // ... Implement your logic for the volume command ...
                    break;
                case "pause":
                    // ... Implement your logic for the pause command ...
                    break;
                case "stop":
                    // ... Implement your logic for the stop command ...
                    break;
                default:
                    // Handle cases for other commands if needed
                    break;
            }
        } else {
            switch (commandName) {
                case "play":
                    // ... Implement your logic for the play command ... 
                    break;
                case "queue":
                    // ... Implement your logic for the queue command ...
                    break;
                case "skip":
                    // ... Implement your logic for the skip command ...
                    break;
                case "search":
                    // ... Implement your logic for the search command ... 
                    break;
                case "scsearch":
                    // ... Implement your logic for the scsearch command ...
                    break;
                case "playlists":
                    // ... Implement your logic for the playlists command ... 
                    break;
                case "shuffle":
                    // ... Implement your logic for the shuffle command ... 
                    break;
                case "seek":
                    // ... Implement your logic for the seek command ... 
                    break;
                case "lyrics":
                    // ... Implement your logic for the lyrics command ...
                    break;
                case "nowplaying":
                    // ... Implement your logic for the nowplaying command ...
                    break;
                default:
                    // Handle cases for other commands if needed 
                    break;
            }
        }
    }

    // Centralized role check for admin commands
    private boolean checkAdminPermission(SlashCommandEvent event) {
        Member member = event.getMember();
        if (member == null) {
            return false;
        }
        List<Role> roles = member.getRoles();
        for (Role role : roles) {
            if (role.getIdLong() == bot.getConfig().getAdminRoleId()) {
                return true;
            }
        }
        return false;
    }

    // Centralized role check for DJ commands
    private boolean checkDJPermission(SlashCommandEvent event) {
        Member member = event.getMember();
        if (member == null) {
            return false;
        }
        List<Role> roles = member.getRoles();
        for (Role role : roles) {
            if (role.getIdLong() == bot.getConfig().getDjRoleId()) {
                return true;
            }
        }
        return false;
    }
}