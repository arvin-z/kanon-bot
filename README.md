
# Kanon Bot

A Discord bot for playing music via LavaLink, built with Spring Framework and Discord4J.

> :warning: As of version `1.0.0`, Kanon Bot has transitioned from using built-in Lavaplayer to using LavaLink. Anyone upgrading from a `0.x.x` version will need to follow the steps below to set up a LavaLink server.


## How to run

1. Download the LavaLink v4 server from https://github.com/lavalink-devs/Lavalink, creating an `application.yml` based on the example version (and enable YouTube source using the instructions here: https://github.com/lavalink-devs/youtube-source).
2. Download Kanon Bot. In `src/main/resources/application.properties`, set your bot token from the Discord Developer Portal, as well as your LavaLink server's Address and Password. The address should be in the format `ws://ADDRESS:PORT`. You can also customize the command prefix and the LavaLink node name to use.
3. In the project's root directory, run `./mvnw clean install` (Linux/Mac) or `mvnw.cmd clean install` (Windows)
4. Once built, run the Jar file in the `target` folder


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

If you hare having trouble with YouTube videos (and not with other sources), YouTube is likely blocking access to your LavaLink server based on IP address. To fix this you can try several different options: you could run your LavaLink server through a residential (or another IP that isn't blacklisted) proxy. You could also try [LavaLink's IPv6 rotation feature](https://blog.arbjerg.dev/2020/3/tunnelbroker-with-lavalink), or [use PO Tokens](https://github.com/lavalink-devs/youtube-source?tab=readme-ov-file#using-a-potoken).



