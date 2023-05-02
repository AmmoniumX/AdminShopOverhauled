package com.vnator.adminshop.network;

import com.vnator.adminshop.AdminShop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MojangAPI {

    private static final Map<String, String> storedResults = new HashMap<>();
    public static String getUsernameByUUID(String uuid) {
        // Search in stored results
        if (storedResults.containsKey(uuid)) {
            return storedResults.get(uuid);
        }
        // Search in online players
        assert Minecraft.getInstance().level != null;
        Optional<AbstractClientPlayer> search = Minecraft.getInstance().level.players().stream().filter(player ->
            player.getStringUUID().equals(uuid)).findAny();
        if (search.isPresent()) {
            return search.get().getName().getString();
        }

        // Search in mojang API
        try {
            URL url = new URL("https://api.mojang.com/user/profile/" + uuid.replace("-", ""));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Extract the username from the response JSON
                String jsonResponse = response.toString();
                int nameStartIndex = jsonResponse.lastIndexOf("\"name\" : \"") + 10;
                int nameEndIndex = jsonResponse.lastIndexOf("\"}");
                // Save name to stored results and return
                String name = jsonResponse.substring(nameStartIndex, nameEndIndex);
                storedResults.put(uuid, name);
                return name;
            } else {
                AdminShop.LOGGER.error("Mojang API request failed: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return uuid;
    }
}
