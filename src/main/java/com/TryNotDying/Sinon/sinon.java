package com.TryNotDying.Sinon;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.TryNotDying.Sinon.utils.ColorPalette; // Import ColorPalette
import com.TryNotDying.Sinon.commands.AboutCommand;
import com.TryNotDying.Sinon.commands.PingCommand;
import com.TryNotDying.Sinon.commands.admin.*;
import com.TryNotDying.Sinon.commands.dj.*;
import com.TryNotDying.Sinon.commands.general.*;
import com.TryNotDying.Sinon.commands.music.*;
import com.TryNotDying.Sinon.commands.owner.*;
import com.TryNotDying.Sinon.entities.Prompt;
import com.TryNotDying.Sinon.entities.User;
import com.TryNotDying.Sinon.gui.GUI;
import com.TryNotDying.Sinon.settings.SettingsManager;
import com.TryNotDying.Sinon.utils.OtherUtil;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.MessageEmbedBuilder; // Import MessageEmbedBuilder
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import java.awt.Color; // Import Color
import java.util.Arrays;
import javax.security.auth.login.LoginException;

/**
*
*/
public class sinon {
    public final static Logger LOG = LoggerFactory.getLogger(sinon.class);
    public final static Permission[] RECOMMENDED_PERMS = {Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_MANAGE, Permission.MESSAGE_EXT_EMOJI,
            Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.NICKNAME_CHANGE};
    public final static GatewayIntent[] INTENTS = {GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES};

    /**
     *
     */
    public static void main(String[] args) {
        if (args.length > 0)// ... (Initialization code for bot)
            switch (args[0].toLowerCase()) {
                case "generate-config":
                    BotConfig.writeDefaultConfig();
                    return;
                default:
            }
        startBot();
    }

    private static void startBot() {
        // create prompt to handle startup
        Prompt prompt = new Prompt("Sinon");

        // startup checks
        OtherUtil.checkVersion(prompt);
        OtherUtil.checkJavaVersion(prompt);

        // load config
        BotConfig config = new BotConfig(prompt);
        config.load();
        if (!config.isValid())
            return;

        GUI gui = null;

        if (!prompt.isNoGUI()) {
            try {
                gui = new GUI();
                gui.init();
            } catch (Exception e) {
                LOG.error("Could not start GUI. If you are "
                        + "running on a server or in a location where you cannot display a "
                        + "window, please run in nogui mode using the -Dnogui=true flag.", e);
            }
        }

        LOG.info("Loaded config from {}", config.getConfigLocation());

        // set log level from config - improved error handling
        Logger rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (rootLogger instanceof ch.qos.logback.classic.Logger) {
            ((ch.qos.logback.classic.Logger) rootLogger).setLevel(Level.toLevel(config.getLogLevel(), Level.INFO));
        } else {
            LOG.warn("Could not set log level. Incorrect logging implementation detected.");
        }

        // set up the listener
        EventWaiter waiter = new EventWaiter();
        SettingsManager settings = new SettingsManager();
        Bot bot = new Bot(waiter, config, settings, gui);
        if (gui != null)
            gui.setBot(bot);
        
        // attempt to log in and start
        try {
            JDA jda = JDABuilder.create(config.getToken(), Arrays.asList(INTENTS))
                    .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                    .disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.EMOTE, CacheFlag.ONLINE_STATUS)
                    .setActivity(config.isGameNone() ? null : Activity.playing("loading..."))
                    .setStatus(config.getStatus() == OnlineStatus.INVISIBLE || config.getStatus() == OnlineStatus.OFFLINE
                            ? OnlineStatus.INVISIBLE : OnlineStatus.DO_NOT_DISTURB)
                    .addEventListeners(new Listener(bot)) // Add the Listener directly to JDA
                    .setBulkDeleteSplittingEnabled(true)
                    .build();
            bot.setJDA(jda);

            // after JDA is built start Command Client
            CommandClient client = CommandClient.buildCommandClient(config, settings, this);

            // Now, call registerCommands
            client.registerCommands();

            // check if something about the current startup is not supported
            String unsupportedReason = OtherUtil.getUnsupportedBotReason(jda);
            if (unsupportedReason != null) {
                prompt.alert(Prompt.Level.ERROR, "Sinon", "Sinon cannot be run on this Discord bot: " + unsupportedReason);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                } // this is awful but until we have a better way...
                jda.shutdown();
                System.exit(1);
            }

            // other check that will just be a warning now but may be required in the future
            // check if the user has changed the prefix and provide info about the
            // message content intent
            if (!"@mention".equals(config.getPrefix())) {
                LOG.info("You currently have a custom prefix set. "
                        + "If your prefix is not working, make sure that the 'MESSAGE CONTENT INTENT' is Enabled "
                        + "on https://discord.com/developers/applications/{}" + "/bot", jda.getSelfUser().getId());
            }

            // set up the command client (after JDA is built)
            CommandClient client = CommandClient.buildCommandClient(config, settings, bot);

            // ... (rest of your code) 
        } catch (LoginException ex) {
        	prompt.alert(Prompt.Level.ERROR, "Sinon", ex + "\nPlease make sure you are " +
        	        "editing the correct config.txt file, and that you have used the "
        	        + "correct token (not the 'secret'!)\nConfig Location: " + config.getConfigLocation(), ex);
            System.exit(1);
        } catch (IllegalArgumentException ex) {
        	prompt.alert(Prompt.Level.ERROR, "Sinon", "Some aspect of the configuration is invalid: " + ex.getMessage() +
        		       "\nConfig Location: " + config.getConfigLocation(), ex); 
            System.exit(1);
        } catch (ErrorResponseException ex) {
            prompt.alert(Prompt.Level.ERROR, "Sinon", "Invalid response returned when "
                    + "attempting to connect, please make sure you're connected to the internet: {}", ex);
            System.exit(1);
        }
    }
}