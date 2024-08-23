package mateusu.worldboss;

import mateusu.worldboss.Boss.Bosses;
import mateusu.worldboss.Boss.BossesScheduler;
import mateusu.worldboss.InGame.Comandos;
import mateusu.worldboss.InGame.Eventos;
import mateusu.worldboss.Placeholder.BossDamagePlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public final class Main extends JavaPlugin {

    // Dentro da sua classe Eventos
    public final Map<String, Double> playerDamageMap = new HashMap<>();

    private FileConfiguration messagesConfig;
    private File messagesConfigFile;
    private FileConfiguration bossesConfig;
    private File bossesConfigFile;
    private FileConfiguration usersConfig;
    private File usersConfigFile;

    private Map<String, Bosses> activeBosses = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Plugin WorldBoss iniciado.");

        // Salva o config.yml padrão se ele não existir
        saveDefaultConfig();

        // Verifica se o config.yml foi criado corretamente
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Falha ao criar config.yml!");
            return;
        }

        // Garante que os valores padrão sejam aplicados caso estejam faltando
        getConfig().options().copyDefaults(true);

        // Salva qualquer nova configuração adicionada
        saveConfig();

        // Cria e carrega o arquivo bosses.yml
        File bossesFile = new File(getDataFolder(), "bosses.yml");
        if (!bossesFile.exists()) {
            saveResource("bosses.yml", false);
        }

        // Cria e carrega o arquivo users.yml
        File usersFile = new File(getDataFolder(), "users.yml");
        if (!usersFile.exists()) {
            saveResource("users.yml", false);
        }

        // Cria e carrega o arquivo messages.yml
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }

        // Carregar configurações
        reloadBossesConfig();
        reloadUsersConfig();
        reloadMessagesConfig();

        // Carrega os danos dos jogadores da configuração
        for (String key : usersConfig.getConfigurationSection("users").getKeys(false))
        {
            double damage = usersConfig.getDouble("users." + key + ".damage", 0.0);
            playerDamageMap.put(key, damage);
        }

        for (String bossName : bossesConfig.getConfigurationSection("bosses").getKeys(false)) {
            String status = bossesConfig.getString("bosses." + bossName + ".status", "");
            if (status.equalsIgnoreCase("ACTIVE")) {
                Bosses boss = createBossFromConfig(bossName);
                if (boss != null)
                {
                    addBoss(bossName, boss);
                }
            }
        }

        // Registrar comandos e eventos
        getCommand("wb").setExecutor(new Comandos(this));
        getServer().getPluginManager().registerEvents(new Eventos(this), this);

        // Registrar PlaceholderAPI, se disponível
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BossDamagePlaceholder(this).register();
        }

        // Inicia o BossesScheduler
        BossesScheduler scheduler = new BossesScheduler(this);
        scheduler.runTaskTimer(this, 0L, 20L); // Executa a cada segundo
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        for(Bosses bosses : activeBosses.values())
        {
            bosses.bossBar.removeAll();
            bosses.stop();
        }
    }

    // Método para criar um boss a partir da configuração
    private Bosses createBossFromConfig(String bossName)
    {
        return new Bosses(this, bossName);
    }

    // Método para recarregar a configuração dos bosses
    public void reloadBossesConfig() {
        if (bossesConfigFile == null) {
            bossesConfigFile = new File(getDataFolder(), "bosses.yml");
        }
        bossesConfig = YamlConfiguration.loadConfiguration(bossesConfigFile);

        // Verifica se o arquivo é lido corretamente
        InputStream defConfigStream = this.getResource("bosses.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            bossesConfig.setDefaults(defConfig);
        }
    }

    // Método para salvar a configuração dos bosses
    public void saveBossesConfig() {
        if (bossesConfig == null || bossesConfigFile == null) {
            return;
        }
        try {
            bossesConfig.save(bossesConfigFile);
        } catch (IOException e) {
            getLogger().severe("Não foi possível salvar a configuração de bosses.yml!");
            e.printStackTrace();
        }
    }

    // Método para acessar a configuração dos bosses
    public FileConfiguration getBossesConfig() {
        if (bossesConfig == null) {
            reloadBossesConfig();
        }
        return bossesConfig;
    }

    // Método para recarregar a configuração dos usuários
    public void reloadUsersConfig() {
        if (usersConfigFile == null) {
            usersConfigFile = new File(getDataFolder(), "users.yml");
        }
        usersConfig = YamlConfiguration.loadConfiguration(usersConfigFile);

        // Verifica se o arquivo é lido corretamente
        InputStream defConfigStream = this.getResource("users.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            usersConfig.setDefaults(defConfig);
        }
    }

    // Método para recarregar a configuração das mensagens
    public void reloadMessagesConfig()
    {
        if (messagesConfigFile == null)
        {
            messagesConfigFile = new File(getDataFolder(), "messages.yml");
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesConfigFile);

        InputStream defConfigStream = this.getResource("messages.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            messagesConfig.setDefaults(defConfig);
        }
    }

    // Método para salvar a configuração dos usuários
    public void saveUsersConfig()
    {
        if (usersConfig == null || usersConfigFile == null) {
            return;
        }
        try {
            usersConfig.save(usersConfigFile);
        } catch (IOException e) {
            getLogger().severe("Não foi possível salvar a configuração de users.yml!");
            e.printStackTrace();
        }
    }

    public String getMessageInConfig(String key) {
        // Certifica-se de que o arquivo de configuração de mensagens foi carregado
        if (messagesConfig == null) {
            reloadMessagesConfig();
        }

        // Busca a mensagem no arquivo de configuração
        String message = messagesConfig.getString("messages." + key);

        // Substitui os códigos de cor (ex: &6, &e) por cores reais
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // Método para acessar a configuração dos usuários
    public FileConfiguration getUsersConfig() {
        if (usersConfig == null) {
            reloadUsersConfig();
        }
        return usersConfig;
    }

    public void addBoss(String name, Bosses boss) {
        activeBosses.put(name, boss);
    }

    public Bosses getBoss(String name) {
        return activeBosses.get(name);
    }

    public void removeBoss(String name) {
        Bosses boss = activeBosses.get(name);
        boss.stop();

        activeBosses.remove(name);
    }

    public Map<String, Bosses> getBosses()
    {
        return activeBosses;
    }
}
