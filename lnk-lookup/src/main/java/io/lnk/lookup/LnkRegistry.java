package io.lnk.lookup;

import io.lnk.api.Address;
import io.lnk.api.URI;
import io.lnk.api.registry.Registry;
import io.lnk.lookup.consul.ConsulLookupRegistry;
import io.lnk.lookup.zk.ZokLookupRegistry;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年6月1日 下午4:34:25
 */
public class LnkRegistry implements Registry {
    private static final String ZK_PROTOCOL = "zk";
    private static final String CONSUL_PROTOCOL = "consul";
    private final Registry registry;

    public LnkRegistry(final URI uri) {
        super();
        switch (uri.getProtocol()) {
            case ZK_PROTOCOL:
                this.registry = new ZokLookupRegistry(uri);
                break;
            case CONSUL_PROTOCOL:
                this.registry = new ConsulLookupRegistry(uri);
                break;
            default:
                throw new RuntimeException("can't support Registry URI : " + uri);
        }
    }

    @Override
    public Address[] lookup(String serviceGroup, String serviceId, String version, int protocol) {
        return this.registry.lookup(serviceGroup, serviceId, version, protocol);
    }

    @Override
    public void registry(String serviceGroup, String serviceId, String version, int protocol, Address addr) {
        this.registry.registry(serviceGroup, serviceId, version, protocol, addr);
    }

    @Override
    public void unregistry(String serviceGroup, String serviceId, String version, int protocol) {
        this.registry.unregistry(serviceGroup, serviceId, version, protocol);
    }
}
