package de.tentact.privateserver.provider.service;
/*  Created in the IntelliJ IDEA.
    Copyright(c) 2020
    Created by 0utplay | Aldin Sijamhodzic
    Datum: 05.08.2020
    Uhrzeit: 12:51
*/

import com.github.juliarn.npc.NPC;
import com.github.juliarn.npc.NPCPool;
import com.github.juliarn.npc.modifier.MetadataModifier;
import com.github.juliarn.npc.profile.Profile;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.tentact.languageapi.LanguageAPI;
import de.tentact.languageapi.player.LanguagePlayer;
import de.tentact.privateserver.PrivateServer;
import de.tentact.privateserver.i18n.I18N;
import de.tentact.privateserver.provider.config.Configuration;
import de.tentact.privateserver.provider.config.NPCServerItemProperty;
import de.tentact.privateserver.provider.config.NPCSetting;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class PrivateServerUtil {

    private final PrivateServer privateServer;
    private final Configuration configuration;
    private final CloudNetDriver cloudNetDriver = CloudNetDriver.getInstance();
    private final IPlayerManager iPlayerManager;
    private final NPCPool npcPool;
    private int npcId;

    public PrivateServerUtil(PrivateServer privateServer) {
        this.privateServer = privateServer;
        this.configuration = this.privateServer.getConfiguration();
        this.iPlayerManager = this.cloudNetDriver.getServicesRegistry().getFirstService(IPlayerManager.class);
        this.npcPool = new NPCPool(privateServer);
        this.spawnNPC();
    }

    /**
     * Creates a new PrivateServer based on the given arguments
     *
     * @param serverOwner        the servers owner
     * @param serviceTemplate    the template to start the server with
     * @param serverItemProperty a {@link NPCServerItemProperty} to get settings from
     * @return if the service is created and started or not
     */
    public boolean startPrivateServer(UUID serverOwner, ServiceTemplate serviceTemplate, NPCServerItemProperty serverItemProperty) {
        ServiceTask serviceTask = this.configuration.getPrivateServerConfig().getPrivateServerTask();

        if (serviceTask == null) {
            return false;
        }
        JsonDocument document = new JsonDocument("serverowner", serverOwner)
                .append("templatePrefix", serviceTemplate.getPrefix())
                .append("templateName", serviceTemplate.getName())
                .append("autoStopOnOwnerLeave", serverItemProperty.isAutoStopOnOwnerLeave())
                .append("autoStopOnEmpty", serverItemProperty.isAutoStopIfEmpty());

        ServiceInfoSnapshot serviceInfoSnapshot = ServiceConfiguration
                .builder(serviceTask)
                .addTemplates(serviceTemplate)
                .properties(document)
                .build()
                .createNewService();
        if (serviceInfoSnapshot == null) {
            return false;
        }
        serviceInfoSnapshot.provider().startAsync();
        return true;
    }

    /**
     * This effectively runs startPrivateServer, but does some more checks and responses to the player
     *
     * @param player   the player to create a server for
     * @param template the template to create a server with
     */
    public void createPrivateServer(Player player, String template) {
        LanguagePlayer languagePlayer = LanguageAPI.getInstance().getPlayerExecutor().getLanguagePlayer(player.getUniqueId());
        if (languagePlayer == null) {
            return;
        }
        if (this.hasPrivateServer(player.getUniqueId())) {
            languagePlayer.sendMessage(I18N.PLAYER_ALREADY_HAS_PSERVER.get());
            return;
        }

        if (!template.matches("([A-Za-z0-9]+\\/[A-Za-z0-9]+)")) {
            languagePlayer.sendMessage(I18N.WRONG_TEMPLATE_FORMAT.get());
            return;
        }

        String templatePrefix = template.split("/")[0];
        String templateName = template.split("/")[1];
        NPCServerItemProperty serverItem = this.configuration.getPrivateServerConfig().getStartItems()
                .stream()
                .filter(npcServerItemProperty -> npcServerItemProperty.getTemplateToStart().equalsIgnoreCase(template)).findFirst().orElse(null);

        if (serverItem == null) {
            languagePlayer.sendMessage(I18N.TEMPLATE_NOT_FOUND.get()
                    .replace("%TEMPLATE%", template)
                    .replace("%TEMPLATE_NAME%", templateName)
                    .replace("%TEMPLATE_PREFIX%", templatePrefix));
            return;
        }

        if (!player.hasPermission(serverItem.getStartPermission())) {
            languagePlayer.sendMessage(I18N.NO_TEMPLATE_START_PERMISSION.get()
                    .replace("%TEMPLATE%", template)
                    .replace("%TEMPLATE_NAME%", templateName)
                    .replace("%TEMPLATE_PREFIX%", templatePrefix));
            return;
        }

        ServiceTemplate serviceTemplate = this.cloudNetDriver.getLocalTemplateStorageTemplates()
                .stream()
                .filter(sTemplate -> sTemplate.getPrefix().equalsIgnoreCase(templatePrefix) && sTemplate.getName().equalsIgnoreCase(templateName)).findFirst().orElse(null);

        if (serviceTemplate == null) {
            languagePlayer.sendMessage(I18N.TEMPLATE_NOT_FOUND.get()
                    .replace("%TEMPLATE%", template)
                    .replace("%TEMPLATE_NAME%", templateName)
                    .replace("%TEMPLATE_PREFIX%", templatePrefix));
            return;
        }

        boolean started = this.privateServer.getPrivateServerUtil().startPrivateServer(languagePlayer.getUniqueId(), serviceTemplate, serverItem);
        if (started) {
            languagePlayer.sendMessage(I18N.STARTING_PSERVER.get()
                    .replace("%TEMPLATE%", template)
                    .replace("%TEMPLATE_NAME%", templateName)
                    .replace("%TEMPLATE_PREFIX%", templatePrefix));
            Bukkit.getScheduler().runTaskLater(this.privateServer, () -> languagePlayer.sendMessage(I18N.TELEPORT_AFTER_START.get()), 20 * 2);
            return;
        }
        languagePlayer.sendMessage(I18N.STARTING_PSERVER_ERROR.get());
    }

    /**
     * @param serverOwner the uuid to search for
     * @return if the serverOwner has an PrivateServer
     */
    public boolean hasPrivateServer(UUID serverOwner) {
        for (ServiceInfoSnapshot cloudService : this.cloudNetDriver.getCloudServiceProvider().getCloudServices(this.configuration.getPrivateServerConfig().getPrivateServerTaskName())) {
            if (cloudService.getProperties().contains("serverowner") && cloudService.getProperties().get("serverowner", UUID.class).equals(serverOwner)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Send an owner to his PrivateServer
     *
     * @param serverOwner the owner to send
     */
    public void sendOwner(UUID serverOwner) {
        if (!this.hasPrivateServer(serverOwner)) {
            return;
        }
        this.getServiceInfoSnapshot(serverOwner).ifPresent(serviceInfoSnapshot ->
                this.iPlayerManager.getPlayerExecutor(serverOwner).connect(serviceInfoSnapshot.getName()));
    }

    public Optional<ServiceInfoSnapshot> getServiceInfoSnapshot(UUID serverOwner) {
        for (ServiceInfoSnapshot cloudService : this.cloudNetDriver.getCloudServiceProvider().getCloudServices(this.configuration.getPrivateServerConfig().getPrivateServerTaskName())) {
            if (cloudService.getProperties().contains("serverowner") && cloudService.getProperties().get("serverowner", UUID.class).equals(serverOwner)) {
                return Optional.of(cloudService);
            }
        }
        return Optional.empty();
    }

    public <T> T getProperty(ServiceInfoSnapshot serviceInfoSnapshot, String name, Class<T> clazz) {
        return serviceInfoSnapshot.getProperties().get(name, clazz);
    }

    public String getPropertyAsString(ServiceInfoSnapshot serviceInfoSnapshot, String property) {
        return this.getProperty(serviceInfoSnapshot, property, String.class);
    }

    //Spawn a NPC at the location given in the configuration
    public void spawnNPC() {
        Bukkit.broadcastMessage("SPAWN");
        if (this.configuration.getPrivateServerConfig().getNPCSettings().getNPCLocation() != null) {
            NPCSetting settings = this.configuration.getPrivateServerConfig().getNPCSettings();
            Profile profile = new Profile(UUID.randomUUID(), settings.getNPCName(),
                    Collections.singletonList(new Profile.Property("textures",
                            settings.getSkinValue(), null)));
            //profile = new Profile(settings.getNPCName());
           // profile.complete();

            npcId = new NPC.Builder(profile)
                    .imitatePlayer(settings.isImitatePlayer())
                    .lookAtPlayer(settings.isLookAtPlayer())
                    .location(settings.getNPCLocation().getLocation())
                    .build(npcPool).getEntityId();
            Bukkit.broadcastMessage(settings.getNPCLocation().getLocation().getWorld().getName());
            NPC npc = this.npcPool.getNPC(this.npcId);
            if(npc != null) {
                npc.metadata().queue(MetadataModifier.EntityMetadata.SKIN_LAYERS, true).send();
            }

            Bukkit.broadcastMessage("SPAWN2");
        }
    }

    public NPC getNPC() {
        return this.npcPool.getNPC(this.npcId);
    }

    public void removeNPC() {
        this.npcPool.removeNPC(this.npcId);
    }

    public NPCPool getNPCPool() {
        return npcPool;
    }

}
