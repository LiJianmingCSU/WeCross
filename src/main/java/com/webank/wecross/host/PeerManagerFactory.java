package com.webank.wecross.host;

import com.webank.wecross.p2p.P2PMessageEngine;
import com.webank.wecross.p2p.P2PMessageEngineFactory;
import java.util.Map;
import javax.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PeerManagerFactory {
    private P2PMessageEngine p2pEngine;

    @Resource(name = "initPeers")
    private Map<String, Peer> peers; // url -> peer

    @Resource(name = "newSyncPeerMessageHandler")
    private SyncPeerMessageHandler messageHandler;

    private long peerActiveTimeout = 170000; // 17s

    @Bean
    public PeerManager newPeerManager() {
        PeerManager manager = new PeerManager();
        manager.setP2pEngine(getP2pEngine());
        manager.setPeers(peers);
        messageHandler.setPeerManager(manager);
        manager.setMessageHandler(messageHandler);
        manager.setPeerActiveTimeout(peerActiveTimeout);
        return manager;
    }

    public void setMessageHandler(SyncPeerMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    public void setPeers(Map<String, Peer> peers) {
        this.peers = peers;
    }

    public P2PMessageEngine getP2pEngine() {
        if (p2pEngine == null) {
            p2pEngine = P2PMessageEngineFactory.getEngineInstance();
        }
        return p2pEngine;
    }

    public void setP2pEngine(P2PMessageEngine p2pEngine) {
        this.p2pEngine = p2pEngine;
    }
}