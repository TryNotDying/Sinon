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
package com.TryNotDying.Sinon.utils;

import com.TryNotDying.Sinon.sinon;
import com.TryNotDying.Sinon.entities.Prompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory; //Import this line

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.util.Scanner;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.User;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Above import dependencies
 * Below is the update utility
 */
public class OtherUtil
{
    public final static String NEW_VERSION_AVAILABLE = "There is a new version of Sinon available!\n"
                    + "Current version: %s\n"
                    + "New Version: %s\n\n"
                    + "Please visit https://github.com/TryNotDying/Sinon/releases/latest to get the latest release.";
    private final static String WINDOWS_INVALID_PATH = "c:\\windows\\system32\\";
    private static final Logger LOGGER = LoggerFactory.getLogger(OtherUtil.class); // Initialize LOGGER here
    /**
     * gets a Path from a String
     * also fixes the windows tendency to try to start in system32
     * any time the bot tries to access this path, it will instead start in the location of the jar file
     * 
     * @param path the string path
     * @return the Path object
     */
    public static Path getPath(String path) {
        Path result = Paths.get(path);
        // special logic to prevent trying to access system32
        if (result.toAbsolutePath().toString().toLowerCase().startsWith(WINDOWS_INVALID_PATH)) {
            try {
                //This line assumes your code is in a jar file. Adjust if not the case.
                result = Paths.get(new File(OtherUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile().getPath() + File.separator + path);
            } catch (URISyntaxException | NullPointerException ignored) {
                // Handle the exception appropriately – perhaps log it or throw a custom exception.
                System.err.println("Error getting path: " + ignored.getMessage());
            }
        }
        return result;
    }
    
    /**
     * Loads a resource from the jar as a string
     * 
     * @param clazz class base object
     * @param name name of resource
     * @return string containing the contents of the resource
     */
    public static String loadResource(String resourcePath) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Scanner scanner = null; // Declare scanner outside the try block
        try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return null;
            }
            scanner = new Scanner(inputStream, StandardCharsets.UTF_8); // Create scanner inside the try block
            return scanner.useDelimiter("\\A").next(); // Read the entire stream 
        } catch (IOException e) {
            LOGGER.error("Error loading resource '{}': {}", resourcePath, e.getMessage(), e); // Use SLF4j
            return null;
        } finally {
            if (scanner != null) {
                scanner.close(); // Close the scanner in the finally block
            }
        }
    }

    public static Path getPath(String pathString) {
        return Paths.get(pathString);
    }
    
    /**
     * Loads image data from a URL
     * 
     * @param url url of image
     * @return inputstream of url
     */
    public static InputStream imageFromUrl(String url)
    {
        if(url==null)
            return null;
        try 
        {
            URL u = new URL(url);
            URLConnection urlConnection = u.openConnection();
            urlConnection.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36");
            return urlConnection.getInputStream();
        }
        catch(IOException | IllegalArgumentException ignore) {}
        return null;
    }
    
    /**
     * Parses an activity from a string
     * 
     * @param game the game, including the action such as 'playing' or 'watching'
     * @return the parsed activity
     */
    public static Activity parseGame(String game)
    {
        if(game==null || game.trim().isEmpty() || game.trim().equalsIgnoreCase("default"))
            return null;
        String lower = game.toLowerCase();
        if(lower.startsWith("playing"))
            return Activity.playing(makeNonEmpty(game.substring(7).trim()));
        if(lower.startsWith("listening to"))
            return Activity.listening(makeNonEmpty(game.substring(12).trim()));
        if(lower.startsWith("listening"))
            return Activity.listening(makeNonEmpty(game.substring(9).trim()));
        if(lower.startsWith("watching"))
            return Activity.watching(makeNonEmpty(game.substring(8).trim()));
        if(lower.startsWith("streaming"))
        {
            String[] parts = game.substring(9).trim().split("\\s+", 2);
            if(parts.length == 2)
            {
                return Activity.streaming(makeNonEmpty(parts[1]), "https://twitch.tv/"+parts[0]);
            }
        }
        return Activity.playing(game);
    }
   
    public static String makeNonEmpty(String str)
    {
        return str == null || str.isEmpty() ? "\u200B" : str;
    }
    
    public static OnlineStatus parseStatus(String status)
    {
        if(status==null || status.trim().isEmpty())
            return OnlineStatus.ONLINE;
        OnlineStatus st = OnlineStatus.fromKey(status);
        return st == null ? OnlineStatus.ONLINE : st;
    }
    
    public static void checkJavaVersion(Prompt prompt)
    {
        if(!System.getProperty("java.vm.name").contains("64"))
            prompt.alert(Prompt.Level.WARNING, "Java Version", 
                    "It appears that you may not be using a supported Java version. Please use 64-bit java.");
    }
    
    public static void checkVersion(Prompt prompt)
    {
        // Get current version number
        String version = getCurrentVersion();
        
        // Check for new version
        String latestVersion = getLatestVersion();
        
        if(latestVersion!=null && !latestVersion.equals(version))
        {
            prompt.alert(Prompt.Level.WARNING, "Sinon Version", String.format(NEW_VERSION_AVAILABLE, version, latestVersion));
        }
    }
    
    public static String getCurrentVersion()
    {
        if(sinon.class.getPackage()!=null && sinon.class.getPackage().getImplementationVersion()!=null)
            return sinon.class.getPackage().getImplementationVersion();
        else
            return "UNKNOWN";
    }
    
    public static String getLatestVersion()
    {
        try
        {
            Response response = new OkHttpClient.Builder().build()
                    .newCall(new Request.Builder().get().url("https://api.github.com/repos/TryNotDying/Sinon/releases/latest").build())
                    .execute();
            ResponseBody body = response.body();
            if(body != null)
            {
                try(Reader reader = body.charStream())
                {
                    JSONObject obj = new JSONObject(new JSONTokener(reader));
                    return obj.getString("tag_name");
                }
                finally
                {
                    response.close();
                }
            }
            else
                return null;
        }
        catch(IOException | JSONException | NullPointerException ex)
        {
            return null;
        }
    }

    /**
     * Checks if Sinon is being run on is supported & returns the reason if it is not.
     * @return A string with the reason, or null if it is supported.
     */
    public static String getUnsupportedBotReason(JDA jda) 
    {
        if (jda.getSelfUser().getFlags().contains(User.UserFlag.VERIFIED_BOT))
            return "Sinon is verified.";

        ApplicationInfo info = jda.retrieveApplicationInfo().complete();
        if (info.isBotPublic())
            return "\"Public Bot\" is enabled. Using Sinon as a public bot is not supported. Please disable it in the "
                    + "Developer Dashboard at https://discord.com/developers/applications/" + jda.getSelfUser().getId() + "/bot ."
                    + "You may also need to disable all Installation Contexts at https://discord.com/developers/applications/" 
                    + jda.getSelfUser().getId() + "/installation .";

        return null;
    }
}