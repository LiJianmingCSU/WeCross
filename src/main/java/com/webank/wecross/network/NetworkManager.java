package com.webank.wecross.network;

import com.webank.wecross.core.PathUtils;
import com.webank.wecross.host.Peer;
import com.webank.wecross.resource.Path;
import com.webank.wecross.resource.Resource;
import com.webank.wecross.resource.request.ResourceRequest;
import com.webank.wecross.resource.response.ResourceResponse;
import com.webank.wecross.stub.StateRequest;
import com.webank.wecross.stub.StateResponse;
import com.webank.wecross.stub.Stub;
import com.webank.wecross.stub.remote.RemoteResource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkManager {

    private Map<String, Network> networks = new HashMap<>();
    private int seq = 1;
    private Logger logger = LoggerFactory.getLogger(NetworkManager.class);

    public StateResponse getState(StateRequest request) {
        StateResponse response = new StateResponse();
        response.setSeq(seq);

        return response;
    }

    public Resource getResource(Path path) throws Exception {
        Network network = getNetwork(path);

        if (network != null) {
            Stub stub = network.getStub(path);

            if (stub != null) {
                Resource resource = stub.getResource(path);

                return resource;
            }
        }

        return null;
    }

    public void addResource(Resource resource) throws Exception {
        logger.info("Add resource path:{}", resource.getPath());
        String networkName = resource.getPath().getNetwork();
        networks.putIfAbsent(networkName, new Network());
        networks.get(networkName).addResource(resource);
    }

    public void removeResource(Path path, boolean ignoreLocal) throws Exception {
        logger.info("Remove resource ignore:{} path:{}", ignoreLocal, path);
        Network network = getNetwork(path);
        network.removeResource(path, ignoreLocal);
        if (network.isEmpty()) {
            networks.remove(path.getNetwork());
        }
    }

    public void removeResource(Path path) throws Exception {
        removeResource(path, false);
    }

    public Network getNetwork(Path path) {
        return getNetwork(path.getNetwork());
    }

    public Network getNetwork(String name) {
        logger.trace("get network: {}", name);
        Network network = networks.get(name);
        return network;
    }

    public Map<String, Network> getNetworks() {
        return networks;
    }

    public void setNetworks(Map<String, Network> networks) {
        this.networks = networks;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public Set<String> getAllNetworkStubResourceName(boolean ignoreRemote) {
        Set<String> ret = new HashSet<>();

        for (Map.Entry<String, Network> entry : networks.entrySet()) {
            String networkName = PathUtils.toPureName(entry.getKey());
            Set<String> allStubResourceName = entry.getValue().getAllStubResourceName(ignoreRemote);

            for (String stubResourceName : allStubResourceName) {
                ret.add(networkName + "." + stubResourceName);
            }
        }
        return ret;
    }

    public Set<Path> getAllNetworkStubResourcePath(boolean ignoreRemote) throws Exception {
        Set<String> resourcesString = getAllNetworkStubResourceName(ignoreRemote);
        Set<Path> ret = new HashSet<>();
        for (String str : resourcesString) {
            ret.add(Path.decode(str));
        }
        return ret;
    }

    public void updateActivePeerNetwork(Set<Peer> peers) {
        Map<String, Set<Peer>> resource2Peers = new HashMap<>();
        for (Peer peer : peers) {
            for (String resource : peer.getResources()) {
                Set<Peer> theResourcePeers = resource2Peers.get(resource);
                if (theResourcePeers == null) {
                    theResourcePeers = new HashSet<>();
                }
                theResourcePeers.add(peer);
                resource2Peers.put(resource, theResourcePeers); // Replace
            }
        }

        Set<String> currentResources = getAllNetworkStubResourceName(false);

        Set<String> resources2Add = new HashSet<>(resource2Peers.keySet());
        resources2Add.removeAll(currentResources);

        Set<String> resources2Remove = new HashSet<>(currentResources);
        resources2Remove.removeAll(resource2Peers.keySet());

        Set<String> resources2Update = new HashSet<>(currentResources);
        resources2Update.removeAll(resources2Remove);

        // Delete inactive remote resources
        logger.info("Remove inactive remote resources " + resources2Remove);
        for (String resource : resources2Remove) {
            try {
                removeResource(Path.decode(resource), true);
            } catch (Exception e) {
                logger.error("Remove resource exception: resource:{}, exception:{}", resource, e);
            }
        }

        // Add new remote resources
        logger.info("Add new remote resources " + resources2Add);
        for (String resource : resources2Add) {
            try {
                Set<Peer> newPeers = resource2Peers.get(resource);
                Resource newResource = new RemoteResource(newPeers, 1);
                newResource.setPath(Path.decode(resource));
                addResource(newResource);
            } catch (Exception e) {
                logger.error("Add resource exception: resource:{}, exception:{}", resource, e);
            }
        }

        // Update peer to resources
        logger.info("Update remote resources " + resources2Update);
        for (String resource : resources2Update) {
            try {
                Set<Peer> newPeers = resource2Peers.get(resource);
                Resource resource2Update = getResource(Path.decode(resource));
                resource2Update.setPeers(newPeers);
            } catch (Exception e) {
                logger.error(
                        "Update remote resources exception: resource:{}, exception:{}",
                        resource,
                        e);
            }
        }
    }

    public List<Resource> getAllResources(boolean ignoreRemote) throws Exception {
        List<Resource> resourcesList = new ArrayList<>();

        Set<Path> pathSet = getAllNetworkStubResourcePath(ignoreRemote);
        for (Path path : pathSet) {
            Resource resource = getResource(path);
            resourcesList.add(resource);
        }

        return resourcesList;
    }

    public ResourceResponse list(ResourceRequest request) {
        ResourceResponse resourceResponse = new ResourceResponse();

        try {
            List<Resource> resources = getAllResources(request.isIgnoreRemote());
            resourceResponse.setErrorCode(0);
            resourceResponse.setErrorMessage("");
            resourceResponse.setResources(resources);
        } catch (Exception e) {
            resourceResponse.setErrorCode(1);
            resourceResponse.setErrorMessage("Unexpected error: " + e.getMessage());
        }

        return resourceResponse;
    }
}