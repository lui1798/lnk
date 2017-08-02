package io.lnk.lookup;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lnk.api.Address;
import io.lnk.api.registry.Registry;
import io.lnk.lookup.zookeeper.ZooKeeperService;
import io.lnk.lookup.zookeeper.notify.NotifyEvent;
import io.lnk.lookup.zookeeper.notify.NotifyHandler;
import io.lnk.lookup.zookeeper.notify.NotifyMessage;
import io.lnk.lookup.zookeeper.notify.NotifyMessage.MessageMode;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年6月19日 下午3:18:53
 */
public class ZooKeeperRegistry implements Registry {
    private static final Logger log = LoggerFactory.getLogger(ZooKeeperRegistry.class.getSimpleName());
    private final ConcurrentHashMap<String, Set<String>> registryServices;
    private final ReentrantLock lock = new ReentrantLock();
    private ZooKeeperService zooKeeperService;

    public ZooKeeperRegistry() {
        this.registryServices = new ConcurrentHashMap<String, Set<String>>();
    }

    @Override
    public Address[] lookup(String serviceId, String version, int protocol) {
        SortedSet<Address> addrList = new TreeSet<Address>();
        String path = null;
        try {
            path = Paths.Registry.createPath(serviceId, version, protocol);
            Set<String> serverList = this.getServerList(path);
            if (serverList != null && !serverList.isEmpty()) {
                for (String server : serverList) {
                    addrList.add(new Address(server));
                }
            }
        } catch (Throwable e) {
            log.error("lookup path : " + path + " Error.", e);
        }
        return addrList.toArray(new Address[addrList.size()]);
    }

    @Override
    public void registry(String serviceId, String version, int protocol, Address addr) {
        String path = null;
        try {
            path = Paths.Registry.createPath(serviceId, version, protocol);
            NotifyMessage message = new NotifyMessage();
            message.setPath(path);
            message.setData(StringUtils.EMPTY);
            message.setMessageMode(MessageMode.PERSISTENT);
            this.zooKeeperService.push(message);
            String server = addr.toString();
            Set<String> serverList = this.registryServices.get(path);
            if (serverList == null) {
                serverList = new TreeSet<String>();
            }
            serverList.add(server);
            this.registryServices.put(path, serverList);
            path += ("/" + server);
            message.setPath(path);
            message.setData(server);
            message.setMessageMode(MessageMode.EPHEMERAL);
            this.zooKeeperService.push(message);
            this.registerHandler(serviceId, version, protocol, addr);
            message = new NotifyMessage();
            message.setPath(path);
            message.setData(new Random().nextInt(10000) + "");
            message.setMessageMode(MessageMode.PERSISTENT);
            this.zooKeeperService.push(message);
            log.info("registry path : {} success.", path);
        } catch (Throwable e) {
            log.error("registry path : " + path + " Error.", e);
        }
    }

    @Override
    public void unregistry(String serviceId, String version, int protocol, Address addr) {
        String path = null;
        try {
            path = Paths.Registry.createPath(serviceId, version, protocol);
            this.zooKeeperService.unregister(path, NotifyHandler.NULL);
            String server = addr.toString();
            Set<String> serverList = this.registryServices.get(path);
            if (serverList != null) {
                serverList.remove(server);
            }
            path += ("/" + server);
            this.zooKeeperService.delete(path);
            log.warn("unregistry path : {} success.", path);
        } catch (Throwable e) {
            log.error("unregistry path : " + path + " Error.", e);
        }
    }

    private Set<String> getServerList(String path) {
        Set<String> serverList = this.registryServices.get(path);
        if (serverList == null) {
            lock.lock();
            try {
                serverList = this.registryServices.get(path);
                if (serverList == null) {
                    log.warn("get serverList path : {}", path);
                    List<String> savedServers = this.zooKeeperService.getChildren(path);
                    if (savedServers == null || savedServers.isEmpty()) {
                        log.info("get serverList path : {} serverList is empty.", path);
                        return serverList;
                    }
                    log.info("get serverList path : {} serverList : {}.", path, savedServers);
                    serverList = this.registryServices.get(path);
                    if (serverList == null) {
                        serverList = new TreeSet<String>();
                    } else {
                        serverList.clear();
                    }
                    serverList.addAll(savedServers);
                    this.registryServices.put(path, serverList);
                    log.info("get serverList path : {}.", path);
                }
            } catch (Exception e) {
                log.error("get serverList path : " + path + " Error.", e);
            } finally {
                lock.unlock();
            }
        }
        return serverList;
    }

    private void registerHandler(String serviceId, String version, int protocol, Address addr) {
        String path = null;
        try {
            path = Paths.Registry.createPath(serviceId, version, protocol);
            String server = addr.toString();
            Set<String> serverList = this.registryServices.get(path);
            if (serverList == null) {
                serverList = new TreeSet<String>();
            }
            serverList.add(server);
            this.registryServices.put(path, serverList);
            this.zooKeeperService.registerHandler(path, new NotifyHandler() {
                public boolean receiveChildNotify() {
                    return true;
                }

                public void handleNotify(NotifyEvent notifyEvent, NotifyMessage message) throws Throwable {
                    String path = message.getPath();
                    if (StringUtils.contains(path, Paths.Registry.PROVIDERS) == false) {
                        return;
                    }
                    int serverIndex = StringUtils.indexOf(path, Paths.Registry.PROVIDERS);
                    path = StringUtils.substring(path, 0, serverIndex + Paths.Registry.PROVIDERS.length());
                    List<String> serverList = zooKeeperService.getChildren(path);
                    if (serverList == null || serverList.isEmpty()) {
                        registryServices.remove(path);
                        return;
                    }
                    Set<String> servers = registryServices.get(path);
                    if (servers == null) {
                        servers = new TreeSet<String>();
                    } else {
                        servers.clear();
                    }
                    servers.addAll(serverList);
                    registryServices.put(path, servers);
                }
            });
        } catch (Throwable e) {
            log.error("registerHandler path : " + path + " Error.", e);
        }
    }

    public void setZooKeeperService(ZooKeeperService zooKeeperService) {
        this.zooKeeperService = zooKeeperService;
    }
}
