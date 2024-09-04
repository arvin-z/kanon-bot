
# Kanon Bot

A Discord bot for playing music, built with Spring Boot
using Discord4J and Lavaplayer.


## How to run


1. In `src/main/resources/application.properties`, set your bot token as well as the command prefix you would like the bot to use.
2. In the project's root directory, run `mvnw clean install` (Linux/Mac) or `mvnw.cmd clean install` (Windows)
3. Once building is complete, run the Jar file in the `target` folder


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


## Troubleshooting

The most common issue is getting blocked/limited by YouTube. There are a few methods you can try to fix this:

### POToken

1. Download and run `youtube-trusted-session-generator` from here: https://github.com/iv-org/youtube-trusted-session-generator
2. Copy the outputted PO Token and Visitor Data into `application.properties`
3. Rebuild

### OAuth

1. In `application.properties`, set `kanonbot.useoauth` to `true`, while keeping `kanonbot.oauthtoken` blank.
2. Rebuild and run the Jar
3. Upon running the Jar, check the log for a message about YouTube OAuth. You will see a short code as well as a link to `google.com/device`.
4. Copy the code and go to the link, if you are asked to sign in, sign in. Then enter the code.
5. Once the 'device' is successfully added, check your log again, you will see an OAuth token generated.
6. Copy the OAuth token into 'application.properties`
7. Rebuild

### IPv6 rotation

Kanon Bot now supports Lavaplayer's IP rotation feature. This feature is recommended if you are facing rate limiting issues. To use this feature you need to own an IPv6 block and have your machine configured for it. Then you must add your IPv6 block to `application.properties`.

##### Example setup in Debian using TunnelBroker

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
8. Rebuild



