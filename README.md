
# Kanon Bot

A Discord bot for playing music, built with Spring Boot
using Discord4J and Lavaplayer.


## How to run


1. In `src/main/resources/application.properties`, set your bot token as well as the command prefix you would like the bot to use, and (optionally) your IPv6 rotation block (see section below).
2. In the project's root directory, run `mvnw clean install` (Linux/Mac) or `mvnw.cmd clean install` (Windows)
3. Once building is complete, run the Jar file in the `target` folder


## New in Version 0.5: IPv6 rotation.

Kanon Bot now supports Lavaplayer's IP rotation feature. This feature is recommended if you are facing rate limiting issues. To use this feature you need to own an IPv6 block and have your machine configured for it. Then you must add your IPv6 block to `application.properties`.

> When updating to version 0.5 from an older version, make sure your `application.properties` has the new line `kanonbot.ipv6block=`. You can keep the value blank if you don't want to use the feature. 

#### Example setup in Debian using TunnelBroker

1. On TunnelBroker's website, click Create Regular Tunnel. Enter your IPv4 address (where the bot is hosted), select the closest region, and hit submit. Once created, it's recommended that you click assign /48. (Note: Your TunnelBroker account may need to be a day or two old in order for the assign /48 option to be available.)
2. Enable IPv6 binding with `sudo /sbin/sysctl -w net.ipv6.ip_nonlocal_bind=1` and add `net.ipv6.ip_nonlocal_bind = 1` to the end of your `/etc/sysctl.conf`.
3. Create a file `/etc/network/if-up.d/he-ipv6-post-up` which contains the following, replacing `1234:1234:1234::/48` with your IP block:

    ```
    #!/bin/bash

    if [ "$IFACE" = "he-ipv6" ]; then
        ip -6 route replace local 1234:1234:1234::/48 dev lo
    fi
    ```
    Run `sudo chmod +x /etc/network/if-up.d/he-ipv6-post-up` to make it executable.
5. On your TunnelBroker tunnel details page, click Example Configurations, select Debian/Ubuntu. Copy the generated config to the end of your `/etc/network/interfaces`. Add one more property to the entry, `post-up /etc/network/if-up.d/he-ipv6-post-up`. The bottom of your `/etc/network/interfaces` will end up looking something like this:

    ```
    auto he-ipv6
    iface he-ipv6 inet6 v4tunnel
            address ...
            netmask ...
            endpoint ...
            local ...
            ttl ...
            gateway ...
            post-up /etc/network/if-up.d/he-ipv6-post-up
    ```
6. Run `sudo systemctl restart networking`. Now you should be able to use your IPv6 block: You can test it using something like `ping6 google.com`.
7. Add your IP block to `application.properties`. It should look something like `kanonbot.ipv6block=1234:1234:1234::/48`


## Commands


- play <song>
- np (now playing)
- queue
- clear
- shuffle
- remove
- skip
- jump
- back
- loop
- localloop
- pause
- unpause
- seek
- ff (fast-forward)
- rewind
- join
- dc (disconnect)
- help
- speed
- pitch

