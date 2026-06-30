package com.playerfinder.config;

/** A server PlayerFinder can ping (Server List Ping) to check who's online without connecting to it. */
public class FinderServer {
    /** Friendly label shown in scan results. */
    public String name;
    /** Hostname or IP as the user typed it (SRV records are resolved at ping time). */
    public String host;
    /** Port; 25565 unless the address included one / an SRV record overrides it. */
    public int port = 25565;

    public FinderServer() {}

    public FinderServer(String name, String host, int port) {
        this.name = name;
        this.host = host;
        this.port = port;
    }
}
