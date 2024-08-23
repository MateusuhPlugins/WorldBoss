package mateusu.worldboss;

import mateusu.worldboss.Boss.Bosses;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class GUIManager {

    private Main plugin;
    private String title;

    public GUIManager(Main plugin) {
        this.plugin = plugin;
        this.title = plugin.getMessageInConfig("bossGUIName");
    }

    public String getTitle()
    {
        return title;
    }

    public void openBossListGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, getTitle());

        // Adiciona vidro preto nas bordas
        addBlackGlassBorders(gui);

        // Adiciona os bosses ativos no GUI
        FileConfiguration bossConfig = plugin.getBossesConfig();
        for (String key : plugin.getBosses().keySet())
        {
            ItemStack bossItem = createBossItem(key, bossConfig);
            gui.addItem(bossItem);
        }

        player.openInventory(gui);
    }

    private void addBlackGlassBorders(Inventory gui) {
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = blackGlass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            blackGlass.setItemMeta(meta);
        }

        // Adiciona vidro preto nas bordas
        for (int i = 0; i < 10; i++) {
            gui.setItem(i, blackGlass); // topo
            gui.setItem(35 + i, blackGlass); // fundo
        }
        gui.setItem(17, blackGlass); // canto esquerdo
        gui.setItem(18, blackGlass); // canto direito
        gui.setItem(26, blackGlass); // canto inferior esquerdo
        gui.setItem(27, blackGlass); // canto inferior direito
    }

    private ItemStack createBossItem(String bossName, FileConfiguration bossConfig)
    {
        String bossType = bossConfig.getString("bosses." + bossName + ".type", "UNKNOWN");

        // Concatena o tipo do boss com "_spawn_egg" e converte para uppercase para seguir o padrão do Material enum
        String materialName = bossType.toUpperCase() + "_SPAWN_EGG";

        // Tenta pegar o Material correspondente
        Material material = Material.getMaterial(materialName);

        // Se o material for nulo (não encontrado), usa um item padrão como fallback
        if (material == null) {
            material = Material.DRAGON_EGG;
        }

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

        // Cria o item representando o boss
        ItemStack bossItem = new ItemStack(material);
        ItemMeta meta = bossItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + bossName);

            String type = bossType;

            meta.setLore(Arrays.asList(
                    plugin.getMessageInConfig("bossItemType") + type,
                    plugin.getMessageInConfig("bossItemTime") + timeStart + " - " + timeEnd,
                    plugin.getMessageInConfig("bossItemStatus") + status
            ));

            bossItem.setItemMeta(meta);
        }
        return bossItem;
    }

    // Atualiza o item da GUI correspondente ao boss com o novo status
    public void updateBossStatus(String bossName)
    {
        FileConfiguration bossConfig = plugin.getBossesConfig();

        // Se o boss está "ABERTO", verifica se ele foi derrotado
        Bosses boss = plugin.getBoss(bossName);
        String status = boss.getStatus();

        if (status.equals(plugin.getMessageInConfig("bossItemStatusOpen")) && boss.isDefeated())
        {
            status = plugin.getMessageInConfig("bossItemStatusDefeated"); //Muda o status do item para DERROTADO
        }

        // Atualiza o item correspondente no GUI
        for (Player player : Bukkit.getOnlinePlayers())
        {
            InventoryView inventoryView = player.getOpenInventory();
            if (inventoryView != null && inventoryView.getTitle().equals(title))
            {
                Inventory openInventory = inventoryView.getTopInventory();

                for (ItemStack item : openInventory.getContents())
                {
                    if (item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + bossName)) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            // Obtém os horários de início e fim da configuração do boss
                            String timeStart = bossConfig.getString("bosses." + bossName + ".time.start", "00:00");
                            String timeEnd = bossConfig.getString("bosses." + bossName + ".time.end", "23:59");

                            meta.setLore(Arrays.asList(
                                    plugin.getMessageInConfig("bossItemType") + bossConfig.getString("bosses." + bossName + ".type"),
                                    plugin.getMessageInConfig("bossItemTime") + timeStart + " - " + timeEnd,
                                    plugin.getMessageInConfig("bossItemStatus") + status
                            ));
                            item.setItemMeta(meta);
                        }
                    }
                }
            }
        }
    }

}
