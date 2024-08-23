package mateusu.worldboss.Boss;

import mateusu.worldboss.GUIManager;
import mateusu.worldboss.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class BossesScheduler extends BukkitRunnable {
    private final Main plugin;

    public BossesScheduler(Main plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Atualiza o ITEM do GUI do Boss, e o remove caso FECHADO.
        for (String bossName : plugin.getBosses().keySet()) {
            String timeStart = plugin.getBossesConfig().getString("bosses." + bossName + ".time.start", "00:00");
            String timeEnd = plugin.getBossesConfig().getString("bosses." + bossName + ".time.end", "23:59");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime startTime = LocalTime.parse(timeStart, formatter);
            LocalTime endTime = LocalTime.parse(timeEnd, formatter);
            LocalTime currentTime = LocalTime.now(ZoneId.of("America/Sao_Paulo"));

            boolean isOpen = !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
            String status = isOpen ? plugin.getMessageInConfig("bossItemStatusOpen") : plugin.getMessageInConfig("bossItemStatusClosed");

            Bosses boss = plugin.getBoss(bossName);
            if (status.equalsIgnoreCase(plugin.getMessageInConfig("bossItemStatusOpen")) && boss.isDefeated()) //Muda o status do item para DERROTADO
            {
                status = plugin.getMessageInConfig("bossItemStatusDefeated");
            }

            // Atualiza o status na GUI
            GUIManager guiManager = new GUIManager(plugin);
            guiManager.updateBossStatus(bossName);

            // Caso o boss esteja "FECHADO", executa uma ação específica
            if (status.equals(plugin.getMessageInConfig("bossItemStatusClosed"))) {
                if (boss.isDefeated() || (boss.getLivingEntity() != null && !boss.getLivingEntity().isDead()))
                {
                    executeClosedAction(boss);
                }
            }
        }

        // Spawna o BOSS caso seja a hora (esteja ABERTO e não tenha sido DERROTADO)
        for (String bossName : plugin.getBosses().keySet())
        {
            Bosses boss = plugin.getBoss(bossName);

            if (boss != null && (boss.getLivingEntity() == null || boss.getLivingEntity().isDead())
                    && boss.getStatus().equalsIgnoreCase(plugin.getMessageInConfig("bossItemStatusOpen")) && !boss.isDefeated())
            {
                // Verifica se a mensagem global está habilitada
                boolean isGlobalWarningEnabled = plugin.getConfig().getBoolean("configs.global-message-warning.enabled", false);

                if (isGlobalWarningEnabled)
                {
                    Bukkit.broadcastMessage(plugin.getMessageInConfig("globalBossSpawnWarning"));
                }

                boss.spawn();
            }
        }
    }

    // Método para executar ações específicas quando o boss estiver "FECHADO"
    private void executeClosedAction(Bosses boss)
    {
        if (!boss.isDefeated())
        {
            Bukkit.broadcastMessage(plugin.getMessageInConfig("prefix") + plugin.getMessageInConfig("bossNotDefeated"));
        }

        boss.stop();

        // Atualiza o status do boss para "NÃO DERROTADO" na configuração
        FileConfiguration bossConfig = plugin.getBossesConfig(); // Obtém a configuração do boss
        bossConfig.set("bosses." + boss.getBossName() + ".defeated", "NO");
        plugin.saveBossesConfig(); // Salva a configuração do boss
    }
}
