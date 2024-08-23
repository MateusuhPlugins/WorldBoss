package mateusu.worldboss.InGame;

import mateusu.worldboss.Boss.Bosses;
import mateusu.worldboss.GUIManager;
import mateusu.worldboss.Main;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Comandos implements CommandExecutor {
    private Main plugin;

    private FileConfiguration bossesConfig;

    private String defaultCommands;
    private String noPermissionMessage;

    public Comandos(Main plugin) {
        this.plugin = plugin;

        this.bossesConfig = plugin.getBossesConfig();
        this.defaultCommands = ChatColor.translateAlternateColorCodes('&', plugin.getMessageInConfig("commandList") + "\n&a/&cwb&a create {boss} \n/&cwb&a remove {boss} \n/&cwb&a setspawn {boss} \n/&cwb&a setentrance {boss} \n/&cwb&a sethealth {boss} {vida} \n/&cwb&a settype {boss} {tipo} \n/&cwb&a settime {boss} {horaInicial} {horaFinal} \n/&cwb&a start {boss} \n/&cwb&a stop {boss}  \n/&cwb&a list \n/&cwb&a opengui \n/&cwb&a reload");
        this.noPermissionMessage = plugin.getMessageInConfig("noPermission");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (cmd.getName().equalsIgnoreCase("wb")) {
            if (!(sender instanceof Player)) {
                if (args.length >= 1 && (args[0].equalsIgnoreCase("opengui") || args[0].equalsIgnoreCase("setspawn") || args[0].equalsIgnoreCase("setentrance"))) {
                    sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("commandOnlyInServer"));
                    return false;
                }
            }

            if (!sender.hasPermission("wb.use") || !sender.hasPermission("wb.admin")) {
                sender.sendMessage(noPermissionMessage);
                return false;
            }

            if (args.length == 1) {
                String subCommand = args[0];

                switch (subCommand.toLowerCase()) {
                    case "list":
                        if (!sender.hasPermission("wb.list") || !sender.hasPermission("wb.admin")) {
                            sender.sendMessage(noPermissionMessage);
                            return false;
                        }

                        ConfigurationSection bossesSection = bossesConfig.getConfigurationSection("bosses");

                        if (bossesSection == null || bossesSection.getKeys(false).isEmpty()) {
                            sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("invalidBossList"));
                            return true;
                        }

                        sender.sendMessage(plugin.getMessageInConfig("bossList"));
                        for (String bossName : bossesSection.getKeys(false)) {
                            sender.sendMessage(ChatColor.GREEN + "- " + ChatColor.GOLD + bossName);
                        }

                        return true;
                    case "reload":
                        if (!sender.hasPermission("wb.reload") || !sender.hasPermission("wb.admin")) {
                            sender.sendMessage(noPermissionMessage);
                            return false;
                        }

                        plugin.reloadBossesConfig();
                        plugin.reloadUsersConfig();
                        plugin.reloadMessagesConfig();
                        plugin.reloadConfig();

                        sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("configReload"));
                        return true;
                    case "opengui":
                        if (!sender.hasPermission("wb.opengui") || !sender.hasPermission("wb.admin")) {
                            sender.sendMessage(noPermissionMessage);
                            return false;
                        }

                        GUIManager guiManager = new GUIManager(plugin);
                        guiManager.openBossListGUI((Player) sender);

                        return true;
                    default:
                        sender.sendMessage(defaultCommands);
                        return false;
                }
            } else if (args.length >= 2) {
                String subCommand = args[0];
                String bossName = args[1].toLowerCase(); // Normalize bossName to lowercase

                // Normalize the boss names in the configuration for comparison
                String finalBossName = bossesConfig.getConfigurationSection("bosses").getKeys(false).stream()
                        .filter(name -> name.equalsIgnoreCase(bossName))
                        .findFirst()
                        .orElse(null);

                if (finalBossName == null && !subCommand.equalsIgnoreCase("create")) {
                    sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("bossInvalidName"));
                    return false;
                }

                switch (subCommand.toLowerCase()) {
                    case "create":
                        if (!sender.hasPermission("wb.create") || !sender.hasPermission("wb.admin")) {
                            sender.sendMessage(noPermissionMessage);
                            return false;
                        }

                        if (bossesConfig.getConfigurationSection("bosses").getKeys(false).stream()
                                .anyMatch(name -> name.equalsIgnoreCase(bossName))) {
                            sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("bossRepeatedName"));
                            return false;
                        }

                        bossesConfig.createSection("bosses." + args[1]);
                        plugin.saveBossesConfig();

                        sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("bossSuccessfullyCreated"));
                        return true;
                    case "setspawn":
                    case "setentrance":
                    case "remove":
                    case "start":
                    case "stop":
                    case "sethealth":
                    case "settype":
                        if (!sender.hasPermission("wb." + subCommand) || !sender.hasPermission("wb.admin")) {
                            sender.sendMessage(noPermissionMessage);
                            return false;
                        }

                        if (finalBossName == null) {
                            sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("bossInvalidName"));
                            return false;
                        }

                        switch (subCommand.toLowerCase()) {
                            case "setspawn":
                                Player player = (Player) sender;
                                Location location = player.getLocation();

                                bossesConfig.set("bosses." + finalBossName + ".spawn.world", location.getWorld().getName());
                                bossesConfig.set("bosses." + finalBossName + ".spawn.x", location.getX());
                                bossesConfig.set("bosses." + finalBossName + ".spawn.y", location.getY());
                                bossesConfig.set("bosses." + finalBossName + ".spawn.z", location.getZ());

                                plugin.saveBossesConfig();
                                sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("bossSpawnSet"));
                                return true;
                            case "setentrance":
                                Player playerEntrance = (Player) sender;
                                Location pLocation = playerEntrance.getLocation();

                                bossesConfig.set("bosses." + finalBossName + ".entrance.world", pLocation.getWorld().getName());
                                bossesConfig.set("bosses." + finalBossName + ".entrance.x", pLocation.getX());
                                bossesConfig.set("bosses." + finalBossName + ".entrance.y", pLocation.getY());
                                bossesConfig.set("bosses." + finalBossName + ".entrance.z", pLocation.getZ());
                                bossesConfig.set("bosses." + finalBossName + ".entrance.yaw", pLocation.getYaw());

                                plugin.saveBossesConfig();
                                sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("bossEntranceSet"));
                                return true;
                            case "remove":
                                bossesConfig.set("bosses." + finalBossName, null);
                                plugin.saveBossesConfig();

                                plugin.removeBoss(finalBossName);
                                sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("bossSuccessfullyRemoved"));
                                return true;
                            case "start":
                                if (!hasAllAttributes(finalBossName)) {
                                    sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("bossNotConfigured"));
                                    return false;
                                }

                                // Verificar o status atual do boss
                                String status = bossesConfig.getString("bosses." + finalBossName + ".status", ""); // Padrão vazio

                                if (status.equalsIgnoreCase("ACTIVE"))
                                {
                                    sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("bossAlreadyStarted"));
                                    return false;
                                }

                                // Atualizar o status do boss para ACTIVE
                                bossesConfig.set("bosses." + finalBossName + ".status", "ACTIVE");
                                bossesConfig.set("bosses." + finalBossName + ".defeated", "NO");
                                plugin.saveBossesConfig();

                                // Criar uma instância do boss e iniciar
                                Bosses boss = new Bosses(plugin, finalBossName);
                                boss.start();

                                sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("bossSuccessfullyStarted"));
                                return true;
                            case "stop":
                                // Definir o status do boss como DISABLED
                                bossesConfig.set("bosses." + finalBossName + ".status", "DISABLED");
                                plugin.saveBossesConfig();

                                // Parar o boss
                                Bosses bossToStop = plugin.getBoss(finalBossName); // Supondo que você tenha um método para recuperar o boss
                                if (bossToStop != null) {
                                    plugin.removeBoss(bossName);

                                    sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("bossSuccessfullyStopped"));
                                } else {
                                    sender.sendMessage(plugin.getMessageInConfig("bossCannotBeRemoved"));
                                }
                                return true;
                            case "sethealth":
                                if (args.length != 3) {
                                    sender.sendMessage(defaultCommands);
                                    return false;
                                }

                                try {
                                    double health = Double.parseDouble(args[2]);
                                    if (health <= 0) {
                                        String invalidHealthMessage = plugin.getMessageInConfig("bossInvalidHealth");
                                        sender.sendMessage(plugin.getMessageInConfig("prefix") + invalidHealthMessage);
                                        return false;
                                    }

                                    bossesConfig.set("bosses." + finalBossName + ".health", health);
                                    plugin.saveBossesConfig();

                                    String healthSetMessage = plugin.getMessageInConfig("bossHealthSet");
                                    sender.sendMessage(plugin.getMessageInConfig("prefix") + healthSetMessage + health);
                                } catch (NumberFormatException e) {
                                    String invalidHealthMessage = plugin.getMessageInConfig("bossInvalidHealth");
                                    sender.sendMessage(plugin.getMessageInConfig("prefix") + invalidHealthMessage);
                                }
                                return true;
                            case "settype":
                                if (args.length != 3) {
                                    sender.sendMessage(defaultCommands);
                                    return false;
                                }

                                String type = args[2];

                                if (!isValidEntityType(type)) {
                                    sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("bossInvalidType"));
                                    return false;
                                }

                                bossesConfig.set("bosses." + finalBossName + ".type", type);
                                plugin.saveBossesConfig();
                                sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("bossTypeSet") + type);
                                return true;
                        }
                        return false;
                    case "settime":
                        if (!sender.hasPermission("wb.settime") || !sender.hasPermission("wb.admin")) {
                            sender.sendMessage(noPermissionMessage);
                            return false;
                        }

                        if (finalBossName == null) {
                            sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("bossInvalidName"));
                            return false;
                        }

                        if (args.length != 4) {
                            sender.sendMessage(defaultCommands);
                            return false;
                        }

                        String startTime = args[2];
                        String endTime = args[3];

                        if (!isValidTimeFormat(startTime) || !isValidTimeFormat(endTime)) {
                            sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("wrongTimeFormatting"));
                            return false;
                        }

                        String[] startParts = startTime.split(":");
                        String[] endParts = endTime.split(":");

                        try {
                            int startHour = Integer.parseInt(startParts[0]);
                            int startMinute = Integer.parseInt(startParts[1]);
                            int endHour = Integer.parseInt(endParts[0]);
                            int endMinute = Integer.parseInt(endParts[1]);

                            if (startHour < 0 || startHour > 23 || startMinute < 0 || startMinute > 59 ||
                                    endHour < 0 || endHour > 23 || endMinute < 0 || endMinute > 59) {
                                sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("wrongTimeFormatting"));
                                return false;
                            }

                            bossesConfig.set("bosses." + finalBossName + ".time.start", startTime);
                            bossesConfig.set("bosses." + finalBossName + ".time.end", endTime);

                            plugin.saveBossesConfig();
                            sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("bossTimeSet") + startTime + " - " + endTime);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("wrongTimeFormatting"));
                        }

                        return true;
                    default:
                        sender.sendMessage(defaultCommands);
                        return false;
                }
            }
            return false;
        }
        return false;
    }

    // Método para verificar se todos os atributos necessários para invocar o boss estão definidos
    private boolean hasAllAttributes(String bossName) {
        boolean hasSpawn = bossesConfig.contains("bosses." + bossName + ".spawn");
        boolean hasEntrance = bossesConfig.contains("bosses." + bossName + ".entrance");
        boolean hasType = bossesConfig.contains("bosses." + bossName + ".type");
        boolean hasHealth = bossesConfig.contains("bosses." + bossName + ".health");
        boolean hasTime = bossesConfig.contains("bosses." + bossName + ".time");
        return hasSpawn && hasEntrance && hasType && hasHealth && hasTime;
    }

    // Método para verificar se o valor inserido para horário do boss é de horário
    private boolean isValidTimeFormat(String time)
    {
        Pattern pattern = Pattern.compile("([01][0-9]|2[0-3]):[0-5][0-9]");
        Matcher matcher = pattern.matcher(time);
        return matcher.matches();
    }

    private boolean isValidEntityType(String type) {
        try {
            // Verifica se a entidade fornecida é um tipo válido no Minecraft
            return org.bukkit.entity.EntityType.valueOf(type.toUpperCase()) != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
