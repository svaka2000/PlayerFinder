package com.playerfinder.net;

import java.util.Hashtable;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

/** Resolves a Minecraft {@code _minecraft._tcp.<host>} SRV record (how most public servers/networks
 *  publish their real host+port) so a user can just type "play.example.net". Best-effort: if there's no
 *  SRV record or the lookup fails, callers fall back to the host and default port. */
public final class SrvResolver {
    private SrvResolver() {}

    public record Endpoint(String host, int port) {}

    /** Look up the SRV target for a host, or {@code null} if none / lookup failed. */
    public static Endpoint resolve(String host) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("java.naming.provider.url", "dns:");
            env.put("com.sun.jndi.dns.timeout.initial", "2000");
            env.put("com.sun.jndi.dns.timeout.retries", "1");
            InitialDirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes("_minecraft._tcp." + host, new String[]{"SRV"});
            Attribute srv = attrs.get("SRV");
            if (srv == null || srv.size() == 0) return null;
            // SRV record format: "priority weight port target." — take the first.
            String[] parts = ((String) srv.get(0)).split("\\s+");
            if (parts.length < 4) return null;
            int port = Integer.parseInt(parts[2]);
            String target = parts[3];
            if (target.endsWith(".")) target = target.substring(0, target.length() - 1);
            return new Endpoint(target, port);
        } catch (Throwable t) {
            return null; // no SRV record, no DNS, etc. — caller uses host:defaultPort
        }
    }
}
