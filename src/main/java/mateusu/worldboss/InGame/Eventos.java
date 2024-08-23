package mateusu.worldboss.InGame;

import mateusu.worldboss.Boss.Bosses;
import mateusu.worldboss.GUIManager;
import mateusu.worldboss.Main;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Eventos implements Listener {

    private Main plugin;

    private FileConfiguration usersConfig;

    private Map<String, Double> playerDamageTemp = new HashMap<>(); // Armazena o dano total causado durante os 10 segundos
    private Map<String, Double> playerDamageTotal = new HashMap<>(); // Armazena o dano total causado ao boss
    private Map<String, BukkitRunnable> activeTasks = new HashMap<>();

    private String lastHitPlayerUUID;

    public Eventos(Main plugin) {
        this.plugin = plugin;
        this.usersConfig = plugin.getUsersConfig();
    }

    @EventHandler
    public void onBossHitDamage(EntityDamageByEntityEvent e) {
        for (Bosses boss : plugin.getBosses().values()) {
            Player p = null;

            if (e.getDamager() instanceof Player) {
                p = (Player) e.getDamager();
            } else if (e.getDamager() instanceof Projectile) {
                Projectile projectile = (Projectile) e.getDamager();
                if (projectile.getShooter() instanceof Player) {
                    p = (Player) projectile.getShooter();
                }
            }

            if (p != null) {
                if (boss.getLivingEntity() == e.getEntity()) {
                    double damage = e.getDamage();
                    String playerUUID = p.getUniqueId().toString();

                    // Atualiza o dano TEMPORÁRIO antes da morte no mapa
                    double currentTempDamage = playerDamageTemp.getOrDefault(playerUUID, 0.0);
                    double newTempDamage = currentTempDamage + damage;
                    newTempDamage = Math.round(newTempDamage * 100.0) / 100.0;
                    playerDamageTemp.put(playerUUID, newTempDamage);

                    // Atualiza o dano TOTAL antes da morte no mapa
                    double currentTotalDamage = playerDamageTotal.getOrDefault(playerUUID, 0.0);
                    double newTotalDamage = currentTotalDamage + damage;
                    newTotalDamage = Math.round(newTotalDamage * 100.0) / 100.0;
                    playerDamageTotal.put(playerUUID, newTotalDamage);

                    // Atualiza o valor na configuração
                    double currentConfigDamage = usersConfig.getDouble("users." + playerUUID + ".damage", 0.0);
                    double newConfigDamage = Math.round((currentConfigDamage + damage) * 100.0) / 100.0;
                    usersConfig.set("users." + playerUUID + ".damage", newConfigDamage);
                    usersConfig.set("users." + playerUUID + ".nick", p.getName());

                    plugin.saveUsersConfig();
                    plugin.playerDamageMap.put(p.getUniqueId().toString(), newConfigDamage); // Atualiza o valor ao vivo no placeholder

                    schedulePlayerDeathTask(p.getUniqueId().toString(), boss);

                    if (e.getDamage() >= boss.getLivingEntity().getHealth()) {
                        this.lastHitPlayerUUID = e.getDamager().getUniqueId().toString();
                    }
                }
            }
        }
    }

    public void schedulePlayerDeathTask(final String playerUUID, final Bosses boss) {
        // Se já houver uma task ativa para este jogador, cancele-a
        if (activeTasks.containsKey(playerUUID)) {
            return;
        }

        // Inicia um novo contador para 10 segundos (200 ticks)
        int[] counter = {200};

        // Cria uma nova task
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                // Verifica se o boss está morto ou não existe mais
                if (boss.getLivingEntity().isDead() || boss.getLivingEntity() == null) {
                    cancel();
                    return;
                }

                // Verifica se o contador chegou a zero
                if (counter[0] <= 0) {
                    // Verifica se o player ainda cumpre os requisitos para a task
                    if (playerDamageTemp.containsKey(playerUUID) && playerDamageTemp.get(playerUUID) > 0) {
                        Player player = Bukkit.getPlayer(UUID.fromString(playerUUID));
                        if (player != null && player.isOnline()) {
                            Location entranceLocation = boss.getEntranceLocation();
                            respawnAtEntrance(player, entranceLocation);

                            // Calcula o dano total do jogador antes de morrer
                            double damageBeforeDeath = playerDamageTemp.getOrDefault(playerUUID, 0.0);

                            // Aplica recompensas baseadas no dano
                            rewardPlayer(player, damageBeforeDeath);

                            player.sendMessage(ChatColor.RED + "Dano dado antes da sua morte: " + damageBeforeDeath);

                            // Remove o jogador do mapa de danos
                            playerDamageTemp.remove(playerUUID);

                            // Remove das tasks ativas
                            activeTasks.remove(player.getUniqueId().toString());
                        }
                    }
                    cancel(); // Cancela a task após a execução
                } else {
                    counter[0] -= 10; // Reduz o contador a cada tick
                }
            }
        };

        // Agendar a task para rodar a cada 10 ticks (meio segundo)
        task.runTaskTimer(plugin, 0L, 10L);

        // Armazena a task no mapa de tasks ativas
        activeTasks.put(playerUUID, task);
    }

    // Método para lidar com a morte do boss e limpeza de dados dos jogadores
    @EventHandler
    public void onBossDeath(EntityDeathEvent e)
    {
        for (Bosses boss : plugin.getBosses().values()) {
            if (boss.getLivingEntity() == e.getEntity()) {
                for (String playerUUID : playerDamageTemp.keySet()) {
                    Player player = Bukkit.getPlayer(UUID.fromString(playerUUID));
                    if (player != null && player.isOnline()) {
                        // Aplica recompensas baseadas no dano
                        double damageBeforeDeath = playerDamageTemp.getOrDefault(playerUUID, 0.0);
                        rewardPlayer(player, damageBeforeDeath);

                        player.sendTitle(plugin.getMessageInConfig("bossDefeatedTitle"), plugin.getMessageInConfig("bossDefeatedSubtitle"));
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);

                        // Atualiza o status do boss para "DERROTADO" na configuração
                        FileConfiguration bossConfig = plugin.getBossesConfig(); // Obtém a configuração do boss
                        bossConfig.set("bosses." + boss.getBossName() + ".defeated", "YES");
                        plugin.saveBossesConfig(); // Salva a configuração do boss
                    }
                }
                playerDamageTemp.clear();

                rewardTopPlayersAndLastHit();
            }
        }
    }

    // Método para recompensar jogadores no final da batalha
    public void rewardTopPlayersAndLastHit() {
        // Ordena jogadores por dano
        List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(playerDamageTotal.entrySet());
        sortedEntries.sort(Map.Entry.<String, Double>comparingByValue().reversed());

        // Recompensa o top 10
        for (int i = 0; i < Math.min(10, sortedEntries.size()); i++) {
            Map.Entry<String, Double> entry = sortedEntries.get(i);
            Player player = Bukkit.getPlayer(UUID.fromString(entry.getKey()));
            if (player != null) {
                String reward = plugin.getConfig().getString("rewards.top." + (i + 1));
                if (reward != null) {
                    applyRewards(player, reward);
                }
            }
        }

        // Recompensa o jogador que deu o último dano
        if (lastHitPlayerUUID != null) {
            Player player = Bukkit.getPlayer(UUID.fromString(lastHitPlayerUUID));
            if (player != null) {
                String reward = plugin.getConfig().getString("rewards.lastHit");
                if (reward != null) {
                    applyRewards(player, reward);
                }
            }
        }

        playerDamageTotal.clear();
    }

    // Recompensa o jogador baseado no config.yml
    public void rewardPlayer(Player player, double damage) {
        // Obtém a seção de recompensas da configuração
        Map<String, Object> rewardsConfig = plugin.getConfig().getConfigurationSection("rewards.damage-based-rewards").getValues(false);

        // Inicializa variáveis para armazenar o valor mais próximo e sua recompensa correspondente
        int closestThreshold = -1;
        String closestRewards = null;

        // Itera sobre cada entrada na configuração
        for (Map.Entry<String, Object> entry : rewardsConfig.entrySet()) {
            int damageThreshold = Integer.parseInt(entry.getKey());

            // Verifica se o dano é maior ou igual ao threshold atual e se ele é o maior possível abaixo do dano dado
            if (damage >= damageThreshold && damageThreshold > closestThreshold) {
                closestThreshold = damageThreshold;
                closestRewards = (String) entry.getValue();
            }
        }

        // Se caso o dano suficiente, receba recompensas
        if (closestRewards != null) {
            applyRewards(player, closestRewards);
        }
    }

    private void applyRewards(Player player, String rewards) {
        String[] rewardItems = rewards.split(", ");
        for (String reward : rewardItems) {
            if (reward.contains(" MONEY")) {
                // Enviar dinheiro ao player
                int moneyAmount = Integer.parseInt(reward.replace(" MONEY", "").trim());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "money add " + player.getName() + " " + moneyAmount);
            } else {
                // Lógica para conceder itens
                String[] itemParts = reward.split(" ");
                int amount = Integer.parseInt(itemParts[0]);
                Material material = Material.getMaterial(itemParts[1].toUpperCase());

                if (material != null) {
                    player.getInventory().addItem(new ItemStack(material, amount));
                } else {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Material inválido para o WorldBoss: " + itemParts[1]);
                }
            }
        }
    }

    // Método para lidar com respawn na entrada
    public void respawnAtEntrance(Player player, Location entranceLocation) {
        player.teleport(entranceLocation);
        player.setHealth(player.getMaxHealth()); // Restaura a vida do jogador

        player.sendTitle(plugin.getMessageInConfig("playerDiedTitle"), plugin.getMessageInConfig("playerDiedSubtitle"));
        player.playSound(player.getLocation(), Sound.ENTITY_SKELETON_DEATH, 1.0F, 1.0F);
    }

    @EventHandler
    public void bossSlimeDie(SlimeSplitEvent e) {
        for (Bosses boss : plugin.getBosses().values()) {
            if (boss.getBossType() == EntityType.SLIME) {
                if (boss.getLivingEntity() == e.getEntity()) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        GUIManager guiManager = new GUIManager(plugin);

        // Verifica se o clique ocorreu na GUI do boss
        if (e.getView().getTitle().equals(guiManager.getTitle())) {
            e.setCancelled(true);

            Player player = (Player) e.getWhoClicked();

            // Obtém o item clicado
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem != null && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasLore()) {
                List<String> lore = clickedItem.getItemMeta().getLore();
                String bossName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

                // Verifica se a lore contém a indicação de que o boss está ativo
                boolean isActive = lore.stream().anyMatch(line -> line.contains("Status:") && line.contains("ABERTO"));

                if (isActive) {
                    // Obtém a entrada do boss da configuração
                    FileConfiguration config = plugin.getBossesConfig();
                    if (config.contains("bosses." + bossName + ".spawn")) {
                        String worldName = config.getString("bosses." + bossName + ".spawn.world");
                        double x = config.getDouble("bosses." + bossName + ".entrance.x");
                        double y = config.getDouble("bosses." + bossName + ".entrance.y");
                        double z = config.getDouble("bosses." + bossName + ".entrance.z");
                        float yaw = (float) config.getDouble("bosses." + bossName + ".entrance.yaw");

                        World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            Location entranceLocation = new Location(world, x, y, z, yaw, 0);
                            player.teleport(entranceLocation);
                            player.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("entranceJoin"));

                            player.closeInventory();
                        } else {
                            Bukkit.getConsoleSender().sendMessage(plugin.getMessageInConfig("worldNotFound") + worldName);
                        }
                    }
                }
            }
        }
    }

    // Verifica e move o boss de volta para o spawn se não encontrar jogadores dentro do raio especificado
    public void trackBossMovement() {
        for (Bosses boss : plugin.getBosses().values()) {
            Location spawnLocation = boss.getBossSpawnLocation();
            double maxDistance = plugin.getConfig().getDouble("bosses." + boss.getBossName() + ".maxDistance", 50.0);
            if (spawnLocation == null) continue;

            // Verifica se há jogadores dentro do raio
            boolean playerInRange = false;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(spawnLocation.getWorld()) && player.getLocation().distance(spawnLocation) <= maxDistance) {
                    playerInRange = true;
                    break;
                }
            }

            if (!playerInRange) {
                // Move o boss de volta ao spawn
                boss.getLivingEntity().teleport(spawnLocation);
            }
        }
    }

    // Adiciona um agendador para rastrear o movimento do boss periodicamente
    public void startBossTracking() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::trackBossMovement, 0L, 200L); // Executa a cada 10 segundos (200 ticks)
    }
}
