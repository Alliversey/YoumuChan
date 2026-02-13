package org.allivilsey.youmuchan;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(
        id = "youmuchan",
        name = "YoumuChan",
        version = "2.0",
        authors = {"Allivilsey"}
)
public class YoumuChan {

    private final ProxyServer proxyServer;

    private InGameInfoCollector inGameInfoCollector;

    @Inject
    public YoumuChan(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }
    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        inGameInfoCollector = new InGameInfoCollector(5 * 60 * 1000);

        InGameInfoListener listener = new InGameInfoListener(inGameInfoCollector);

        proxyServer.getEventManager().register(this, listener);
    }
}
