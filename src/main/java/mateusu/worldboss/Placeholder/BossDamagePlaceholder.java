package mateusu.worldboss.Placeholder;

import mateusu.worldboss.Main;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class BossDamagePlaceholder extends PlaceholderExpansion {

    private final Main plugin;

    private YamlConfiguration usersConfig;
    private File usersFile;

    public BossDamagePlaceholder(Main plugin) {
        this.plugin = plugin;

        this.usersFile = new File(plugin.getDataFolder(), "users.yml");
        this.usersConfig = YamlConfiguration.loadConfiguration(usersFile);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "worldboss";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Mateusuh";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        if (identifier.equals("damage")) {
            String playerUUID = player.getUniqueId().toString();
            // Retorna o valor do mapa ou 0 se não estiver presente
            double damage = plugin.playerDamageMap.getOrDefault(playerUUID, 0.0);
            return String.valueOf(damage);
        }

        return null;
    }


}
