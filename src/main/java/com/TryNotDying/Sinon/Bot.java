/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.TryNotDying.Sinon;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.TryNotDying.Sinon.audio.AloneInVoiceHandler;
import com.TryNotDying.Sinon.audio.AudioHandler;
import com.TryNotDying.Sinon.audio.NowplayingHandler;
import com.TryNotDying.Sinon.audio.PlayerManager;
import com.TryNotDying.Sinon.gui.GUI;
import com.TryNotDying.Sinon.gui.TerminalGUI; // Import TerminalGUI
import com.TryNotDying.Sinon.playlist.PlaylistLoader;
import com.TryNotDying.Sinon.settings.SettingsManager;
import java.util.Objects;
import com.TryNotDying.Sinon.utils.YoutubeOauth2TokenHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Above import dependencies
 * Below is the bot class file
 */
public class Bot
{
    private final EventWaiter waiter;
    private final ScheduledExecutorService threadpool;
    private final BotConfig config;
    private final SettingsManager settings;
    private final PlayerManager players;
    private final PlaylistLoader playlists;
    private final NowplayingHandler nowplaying;
    private final AloneInVoiceHandler aloneInVoiceHandler;
    private final YoutubeOauth2TokenHandler youTubeOauth2TokenHandler;
    private final GUI gui;
    
    private boolean shuttingDown = false;
    private JDA jda;
    private ServerSocket serverSocket; // ServerSocket to listen for connections

    public Bot(EventWaiter waiter, BotConfig config, SettingsManager settings, GUI gui)
    {
        this.waiter = waiter;
        this.config = config;
        this.settings = settings;
        this.playlists = new PlaylistLoader(config);
        this.threadpool = Executors.newSingleThreadScheduledExecutor();
        this.youTubeOauth2TokenHandler = new YoutubeOauth2TokenHandler();
        this.youTubeOauth2TokenHandler.init();
        this.players = new PlayerManager(this);
        this.players.init();
        this.nowplaying = new NowplayingHandler(this);
        this.nowplaying.init();
        this.aloneInVoiceHandler = new AloneInVoiceHandler(this);
        this.aloneInVoiceHandler.init();
        this.gui = gui;
    }
    
    public BotConfig getConfig()
    {
        return config;
    }
    
    public SettingsManager getSettingsManager()
    {
        return settings;
    }
    
    public EventWaiter getWaiter()
    {
        return waiter;
    }
    
    public ScheduledExecutorService getThreadpool()
    {
        return threadpool;
    }
    
    public PlayerManager getPlayerManager()
    {
        return players;
    }
    
    public PlaylistLoader getPlaylistLoader()
    {
        return playlists;
    }
    
    public NowplayingHandler getNowplayingHandler()
    {
        return nowplaying;
    }

    public AloneInVoiceHandler getAloneInVoiceHandler()
    {
        return aloneInVoiceHandler;
    }

    public YoutubeOauth2TokenHandler getYouTubeOauth2Handler()
    {
        return youTubeOauth2TokenHandler;
    }
    
    public JDA getJDA()
    {
        return jda;
    }
    
    public void closeAudioConnection(long guildId)
    {
        Guild guild = jda.getGuildById(guildId);
        if(guild!=null)
            threadpool.submit(() -> guild.getAudioManager().closeAudioConnection());
    }
    
    public void resetGame()
    {
        Activity game = config.getGame()==null || config.getGame().getName().equalsIgnoreCase("none") ? null : config.getGame();
        if(!Objects.equals(jda.getPresence().getActivity(), game))
            jda.getPresence().setActivity(game);
    }

    public void shutdown()
    {
        if(shuttingDown)
            return;
        shuttingDown = true;
        threadpool.shutdownNow();
        if(jda.getStatus()!=JDA.Status.SHUTTING_DOWN)
        {
            jda.getGuilds().stream().forEach(g -> 
            {
                g.getAudioManager().closeAudioConnection();
                AudioHandler ah = (AudioHandler)g.getAudioManager().getSendingHandler();
                if(ah!=null)
                {
                    ah.stopAndClear();
                    ah.getPlayer().destroy();
                }
            });
            jda.shutdown();
        }
        if(gui!=null)
            gui.dispose();
        System.exit(0);
    }

    public void setJDA(JDA jda)
    {
        this.jda = jda;
    }
    
    public void startTerminalServer() {
        try {
            serverSocket = new ServerSocket(12345); // Port number 12345
            LOG.info("Terminal server started on port 12345.");

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept incoming connections
                new Thread(new TerminalHandler(clientSocket, this)).start();
            }
        } catch (IOException e) {
            LOGGER.error("Error starting terminal server: {}", e.getMessage(), e);
        }
    }

    private class TerminalHandler implements Runnable {
        private final Socket clientSocket;
        private final Bot bot;

        public TerminalHandler(Socket clientSocket, Bot bot) {
            this.clientSocket = clientSocket;
            this.bot = bot;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    String[] parts = inputLine.trim().toLowerCase().split("\\s+"); // Split by space
                    String command = parts[0];

                    switch (command) {
                        case "start":
                            // ... handle start command ...
                            break;
                        case "stop":
                            // ... handle stop command ...
                            break;
                        case "status":
                            // ... handle status command ...
                            break;
                        case "loadconfig":
                            if (parts.length != 2) {
                                out.println("Invalid command. Usage: loadconfig <config_file_path>");
                                break;
                            }
                            String configFilePath = parts[1];
                            try {
                                bot.loadConfig(configFilePath); // Call your loadConfig method
                                out.println("Configuration reloaded successfully.");
                            } catch (BotConfigException ex) {
                                out.println("Error loading configuration: " + ex.getMessage());
                            }
                            break;
                        // ... (handle other commands)
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error handling terminal client connection: {}", e.getMessage(), e);
            }
        }
    }

    public void loadConfig(String configFilePath) throws BotConfigException {
        try {
            // Load the new config file
            Config newConfig = ConfigFactory.parseFile(new File(configFilePath));

            // Update bot settings with the new config
            // ... (Your code to load config values) ... 
            // For example:
            // botConfig.setToken(newConfig.getString("token"));
            // botConfig.setPrefix(newConfig.getString("prefix"));
            // ... (Load other settings) ...

            // Apply the new settings to your bot 
            // ... (Your code to apply settings to your bot) ...

            // Log a success message
            LOG.info("Configuration reloaded from: {}", configFilePath);
        } catch (ConfigException ex) {
            // Handle the exception
            LOG.error("Error loading configuration file: {}", ex.getMessage(), ex);
            throw new BotConfigException("Error loading configuration: " + ex.getMessage(), ex);
        }
    }
    
    public void sendMessageToTerminal(String message) {
        SwingUtilities.invokeLater(() -> TerminalGUI.outputArea.append(message + "\n")); // Use SwingUtilities for thread safety
    }
}