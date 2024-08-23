package mateusu.worldboss.Boss;

import mateusu.worldboss.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Bosses {

    private Main plugin;
    private String bossName;

    private LivingEntity boss;
    private Set<UUID> playersInRange = new HashSet<>(); // Para controlar jogadores próximos

    private Location spawnLocation;
    private double maxDistance = 20.0; // Distância máxima que o boss pode se afastar do spawn

    public BossBar bossBar;

    public Bosses(Main plugin, String bossName) {
        this.plugin = plugin;
        this.bossName = bossName;

        this.bossBar = Bukkit.createBossBar(
                ChatColor.RED + bossName,
                BarColor.RED,
                BarStyle.SOLID);

        // Define o local de spawn
        this.spawnLocation = getBossSpawnLocation();
    }

    private FileConfiguration getBossesConfig()
    {
        return plugin.getBossesConfig();
    }

    public void start()
    {
        // Adicionar o boss ao mapa de bosses ativos
        plugin.addBoss(bossName, this);
    }

    public LivingEntity getLivingEntity() {
        return boss;
    }

    public String getBossName() {
        return bossName;
    }

    public boolean isDefeated() {
        // Define o caminho para a configuração do boss
        String path = "bosses." + bossName + ".defeated";

        // Obtém o valor de "defeated" da configuração
        String defeated = getBossesConfig().getString(path);

        // Verifica se o valor é "YES", ignorando o case (maiusculas/minúsculas)
        return defeated.equalsIgnoreCase("YES");
    }

    public EntityType getBossType() {
        return boss.getType();
    }

    public Location getBossSpawnLocation() {
        // Define o caminho para a configuração do boss
        String path = "bosses." + bossName + ".spawn";

        // Obtém os valores de coordenadas e mundo da configuração
        String worldName = getBossesConfig().getString(path + ".world");
        double x = getBossesConfig().getDouble(path + ".x");
        double y = getBossesConfig().getDouble(path + ".y");
        double z = getBossesConfig().getDouble(path + ".z");

        // Verifica se o mundo é válido
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) {
            Bukkit.getConsoleSender().sendMessage(plugin.getMessageInConfig("worldNotFound") + worldName);
            return null;
        }

        // Cria e retorna o objeto Location
        return new Location(world, x, y, z);
    }

    public Location getEntranceLocation() {
        // Define o caminho para a configuração da entrada do boss
        String path = "bosses." + bossName + ".entrance";

        // Obtém os valores de coordenadas e mundo da configuração
        String worldName = getBossesConfig().getString(path + ".world");
        double x = getBossesConfig().getDouble(path + ".x");
        double y = getBossesConfig().getDouble(path + ".y");
        double z = getBossesConfig().getDouble(path + ".z");
        float yaw = (float) plugin.getConfig().getDouble("bosses." + bossName + ".entrance.yaw"); // Use o valor de yaw

        // Verifica se o mundo é válido
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) {
            Bukkit.getConsoleSender().sendMessage(plugin.getMessageInConfig("worldNotFound") + worldName);
            return null;
        }

        // Cria e retorna o objeto Location
        return new Location(world, x, y, z, yaw, 0);
    }

    public String getStatus() {
        FileConfiguration bossConfig = plugin.getBossesConfig();

        // Obtém o horário atual no fuso horário de Brasília
        LocalTime currentTime = LocalTime.now(ZoneId.of("America/Sao_Paulo"));

        // Obtém os horários de início e fim da configuração do boss
        String timeStart = bossConfig.getString("bosses." + bossName + ".time.start", "00:00");
        String timeEnd = bossConfig.getString("bosses." + bossName + ".time.end", "23:59");

        // Converte os horários de string para LocalTime
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime startTime = LocalTime.parse(timeStart, formatter);
        LocalTime endTime = LocalTime.parse(timeEnd, formatter);

        // Determina se o boss está "ABERTO" ou "FECHADO"
        boolean isOpen = !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
        String status = isOpen ? plugin.getMessageInConfig("bossItemStatusOpen") : plugin.getMessageInConfig("bossItemStatusClosed");

        return status;
    }

    public void spawn()
    {
        // Carrega as propriedades do boss da configuração
        String path = "bosses." + bossName;
        String typeString = getBossesConfig().getString(path + ".type");
        double health = getBossesConfig().getDouble(path + ".health");
        Location location = getBossSpawnLocation();

        if (typeString == null || location == null) {
            Bukkit.getConsoleSender().sendMessage(bossName + "- " + plugin.getMessageInConfig("bossConfigurationIncomplete"));
            return;
        }

        // Converte o tipo do mob para EntityType
        EntityType type;
        try {
            type = EntityType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            Bukkit.getConsoleSender().sendMessage(bossName + "- " + plugin.getMessageInConfig("bossInvalidType"));
            return;
        }

        // Spawna a entidade no local especificado
        boss = (LivingEntity) location.getWorld().spawnEntity(location, type);

        if(type == EntityType.SLIME)
        {
            Slime slime = (Slime) boss;
            slime.setSize(3);
        }

        boss.setCustomName(ChatColor.RED + bossName); // Define o nome customizado do boss
        boss.setCustomNameVisible(true); // Torna o nome visível
        boss.setMaxHealth(health); // Define a vida máxima do boss
        boss.setHealth(health); // Define a vida do boss

        boss.getWorld().playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 20.0F, 1.0F);

        startTrackingPlayers();

        Bukkit.getConsoleSender().sendMessage(bossName + "- " + plugin.getMessageInConfig("bossSuccessfullySpawned"));
    }

    public void stop() {
        if (boss != null && !boss.isDead())
        {
            boss.remove();
            Bukkit.getConsoleSender().sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("bossSuccessfullyRemoved"));
        }
        else
        {
            Bukkit.getConsoleSender().sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("bossCannotBeRemoved"));
        }
    }

    private void startTrackingPlayers() {
        bossBar.setVisible(true); // Torna a BossBar visível

        new BukkitRunnable() {
            double progress = 1.0;

            @Override
            public void run() {
                if (boss == null || boss.isDead()) {
                    // Se o boss está morto, encerra o runnable
                    if (!bossBar.getPlayers().isEmpty())
                        bossBar.removeAll();

                    playersInRange.clear();
                    cancel();
                    return;
                }

                boolean playerInRange = false;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().equals(boss.getWorld())) {
                        double distance = player.getLocation().distance(boss.getLocation());
                        if (distance <= 20) { // Distância para mostrar a BossBar (20 blocos)
                            playerInRange = true;
                            if (!playersInRange.contains(player.getUniqueId())) {
                                playersInRange.add(player.getUniqueId());
                                bossBar.addPlayer(player);
                            }
                        } else {
                            if (playersInRange.contains(player.getUniqueId())) {
                                playersInRange.remove(player.getUniqueId());
                                bossBar.removePlayer(player);
                            }
                        }
                    }
                }

                // Se não houver jogadores na área de alcance
                if (!playerInRange) {
                    moveBossBackToSpawn();
                }

                progress = boss.getHealth() / boss.getMaxHealth();
                if (progress >= 0) {
                    bossBar.setProgress(progress);
                } else {
                    bossBar.setProgress(0);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Verifica a cada segundo
    }

    private void moveBossBackToSpawn() {
        if (boss == null || boss.isDead() || spawnLocation == null) return;

        Location currentLocation = boss.getLocation();
        double distanceToSpawn = currentLocation.distance(spawnLocation);

        if (distanceToSpawn > maxDistance) {
            // Move o boss de volta ao spawn
            boss.teleport(spawnLocation);
        }
    }
}
