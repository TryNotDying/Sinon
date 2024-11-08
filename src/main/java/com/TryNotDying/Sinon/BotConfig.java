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

import com.TryNotDying.Sinon.entities.Prompt;
import com.TryNotDying.Sinon.utils.OtherUtil;
import com.TryNotDying.Sinon.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.typesafe.config.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Above import dependencies
 * Below is the bot configuration file system
 */
public class BotConfig {
    private final Prompt prompt;
    private final static String CONTEXT = "Config";
    private final static String START_TOKEN = "/// START OF Sinon CONFIG ///";
    private final static String END_TOKEN = "/// END OF Sinon CONFIG ///";

    private Path path = null;
    private String token, prefix, altprefix, helpWord, playlistsFolder, logLevel,
            successEmoji, warningEmoji, errorEmoji, loadingEmoji, searchingEmoji,
            evalEngine, SCApi; // Added SCApi variable
    private boolean youtubeOauth2, stayInChannel, songInGame, npImages, updatealerts, useEval, dbots;
    private long owner, maxSeconds, aloneTimeUntilStop, verifiedMemberRoleId, djRoleId, adminRoleId; // Add verifiedMemberRoleId, djRoleId, adminRoleId
    private int maxYTPlaylistPages;
    private double skipratio;
    private OnlineStatus status;
    private Activity game;
    private Config aliases, transforms;
    private boolean valid = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(BotConfig.class);

    public BotConfig(Prompt prompt) {
        this.prompt = prompt;
    }

    public void load() throws BotConfigException { // Throw custom exception
        valid = false;
        
        // read config from file
        try {
            // get the path to the config, default config.txt
            path = getConfigPath();

            // load in the config file, plus the default values
            Config config = ConfigFactory.load();

            // set values
            token = getSCApi(config); // Load token using getSCApi
            prefix = config.getString("prefix");
            altprefix = config.getString("altprefix");
            helpWord = config.getString("help");
            owner = config.getLong("owner");
            successEmoji = config.getString("success");
            warningEmoji = config.getString("warning");
            errorEmoji = config.getString("error");
            loadingEmoji = config.getString("loading");
            searchingEmoji = config.getString("searching");
            game = OtherUtil.parseGame(config.getString("game"));
            status = OtherUtil.parseStatus(config.getString("status"));
            youtubeOauth2 = config.getBoolean("youtubeoauth2");
            stayInChannel = config.getBoolean("stayinchannel");
            songInGame = config.getBoolean("songinstatus");
            npImages = config.getBoolean("npimages");
            updatealerts = config.getBoolean("updatealerts");
            logLevel = config.getString("loglevel");
            useEval = config.getBoolean("eval");
            evalEngine = config.getString("evalengine");
            maxSeconds = config.getLong("maxtime");
            maxYTPlaylistPages = config.getInt("maxytplaylistpages");
            aloneTimeUntilStop = config.getLong("alonetimeuntilstop");
            playlistsFolder = config.getString("playlistsfolder");
            aliases = config.getConfig("aliases");
            transforms = config.getConfig("transforms");
            skipratio = config.getDouble("skipratio");
            dbots = owner == 547080973498449934L;
            SCApi = config.hasPath("scapi") ? config.getString("scapi") : ""; //Safe retrieval of SCApi

            // we may need to write a new config file
            boolean write = false;
            
            // Load verified member role ID from config 
            verifiedMemberRoleId = config.hasPath("verifiedmember_roleID") 
                    ? config.getLong("verifiedmember_roleID") 
                    : 0L; // Use 0L as a default if the role ID is not found
            
            // Load DJ role ID from config 
            djRoleId = config.hasPath("dj_roleID") 
                    ? config.getLong("dj_roleID") 
                    : 0L; // Use 0L as a default if the role ID is not found
            
            // Load admin member role ID from config 
            adminRoleId = config.hasPath("admin_roleID") 
                    ? config.getLong("admin_roleID") 
                    : 0L; // Use 0L as a default if the role ID is not found

            // validate bot owner
            if (owner <= 0) {
                try {
                    owner = Long.parseLong(prompt.prompt("Owner ID was missing, or the provided owner ID is not valid."
                            + "\nPlease provide the User ID of the bot's owner."
                            + "\nInstructions for obtaining your User ID can be found here:"
                            + "\nhttps://github.com/TryNotDying/Sinon/wiki/Finding-Your-User-ID"
                            + "\nOwner User ID: "));
                } catch (NumberFormatException | NullPointerException ex) {
                    owner = 0;
                }
                if (owner <= 0) {
                    throw new BotConfigException("Invalid User ID! Exiting.\n\nConfig Location: " + path.toAbsolutePath().toString()); // Throw exception
                } else {
                    write = true;
                }
            }

            if (write)
                writeToFile();

            // if we get through the whole config, it's good to go
            valid = true;
        } catch (ConfigException ex) {
            LOGGER.error("Error loading config: {}", ex.getMessage(), ex); // Improved logging
            throw new BotConfigException("Error loading config: " + ex.getMessage(), ex); // Throw exception
        }
    }

    private String getSCApi(Config config) {
        return config.hasPath("scapi") ? config.getString("scapi") : ""; 
    }

    private void writeToFile() {
        byte[] bytes = loadDefaultConfig().replace("BOT_TOKEN_HERE", token)
                .replace("0 // OWNER ID", Long.toString(owner))
                .trim().getBytes();
        try {
            Files.write(path, bytes);
        } catch (IOException ex) {
            prompt.alert(Prompt.Level.WARNING, CONTEXT, "Failed to write new config options to config.txt: " + ex
                    + "\nPlease make sure that the files are not on your desktop or some other restricted area.\n\nConfig Location: "
                    + path.toAbsolutePath().toString(), ex);
        }
    }

    private static String loadDefaultConfig() {
        String original = OtherUtil.loadResource("/reference.conf");
        return original == null
                ? "token = BOT_TOKEN_HERE\r\nowner = 0 // OWNER ID"
                : original.substring(original.indexOf(START_TOKEN) + START_TOKEN.length(), original.indexOf(END_TOKEN)).trim();
    }

    private static Path getConfigPath() {
        Path path = OtherUtil.getPath(System.getProperty("config.file", System.getProperty("config", "config.txt")));
        if (path.toFile().exists()) {
            if (System.getProperty("config.file") == null)
                System.setProperty("config.file", System.getProperty("config", path.toAbsolutePath().toString()));
            ConfigFactory.invalidateCaches();
        }
        return path;
    }

    public static void writeDefaultConfig() {
        Prompt prompt = new Prompt(null, null, true, true);
        prompt.alert(Prompt.Level.INFO, "Sinon Config", "Generating default config file");
        Path path = BotConfig.getConfigPath();
        byte[] bytes = BotConfig.loadDefaultConfig().getBytes();
        try {
            prompt.alert(Prompt.Level.INFO, "Sinon Config", "Writing default config file to " + path.toAbsolutePath().toString());
            Files.write(path, bytes);
        } catch (Exception ex) {
            prompt.alert(Prompt.Level.ERROR, "Sinon Config", "An error occurred writing the default config file: " + ex.getMessage(), ex);
        }
    }
    
    // Define the getter, ect for Class ID's
    public long getVerifiedMemberRoleId() { // Get Verified Member Role From Config
        return verifiedMemberRoleId;
    }
    
    public long getAdminRoleId() { // Get Admin Role From Config
        return adminRoleId;
    }
    
    public long getDjRoleId() { // Get Dj Role From Config
        return djRoleId;
    }

    public long getUserId() { // Might Be Used To Retrieve User Information, Currently Not Used
        return userId;
    }
    
    public boolean isValid() {
        return valid;
    }

    public String getConfigLocation() {
        return path.toFile().getAbsolutePath();
    }

    public String getPrefix() {
        return prefix;
    }

    public String getAltPrefix() {
        return "NONE".equalsIgnoreCase(altprefix) ? null : altprefix;
    }

    public String getToken() {
        return token;
    }

    public double getSkipRatio() {
        return skipratio;
    }

    public long getOwnerId() {
        return owner;
    }

    public String getSuccess() {
        return successEmoji;
    }

    public String getWarning() {
        return warningEmoji;
    }

    public String getError() {
        return errorEmoji;
    }

    public String getLoading() {
        return loadingEmoji;
    }

    public String getSearching() {
        return searchingEmoji;
    }

    public Activity getGame() {
        return game;
    }

    public boolean isGameNone() {
        return game != null && game.getName().equalsIgnoreCase("none");
    }

    public OnlineStatus getStatus() {
        return status;
    }

    public String getHelp() {
        return helpWord;
    }

    public boolean useYoutubeOauth2() {
        return youtubeOauth2;
    }

    public boolean getStay() {
        return stayInChannel;
    }

    public boolean getSongInStatus() {
        return songInGame;
    }

    public String getPlaylistsFolder() {
        return playlistsFolder;
    }

    public boolean getDBots() {
        return dbots;
    }

    public boolean useUpdateAlerts() {
        return updatealerts;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public boolean useEval() {
        return useEval;
    }

    public String getEvalEngine() {
        return evalEngine;
    }

    public boolean useNPImages() {
        return npImages;
    }

    public String getSCApi() {
        return SCApi;
    }

    public long getMaxSeconds() {
        return maxSeconds;
    }

    public int getMaxYTPlaylistPages() {
        return maxYTPlaylistPages;
    }

    public String getMaxTime() {
        return TimeUtil.formatTime(maxSeconds * 1000);
    }

    public long getAloneTimeUntilStop() {
        return aloneTimeUntilStop;
    }

    public boolean isTooLong(AudioTrack track) {
        if (maxSeconds <= 0)
            return false;
        return Math.round(track.getDuration() / 1000.0) > maxSeconds;
    }

    public String[] getAliases(String command) {
        try {
            return aliases.getStringList(command).toArray(new String[0]);
        } catch (NullPointerException | ConfigException.Missing e) {
            return new String[0];
        }
    }

    public Config getTransforms() {
        return transforms;
    }

    // Custom Exception Class
    public static class BotConfigException extends Exception {
        public BotConfigException(String message, Throwable cause) {
            super(message, cause);
        }

        public BotConfigException(String message) {
            super(message);
        }
    }
}