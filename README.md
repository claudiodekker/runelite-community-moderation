# Community Moderation

Community-driven player moderation for RuneLite.

This plugin automatically mutes frequently and recently reported players.

## How does it work?

When a player makes an in-game abuse report with this plugin installed (using the in-game report button),
the report will also be forwarded to our third-party community-moderation service.

Once a report reaches this service, it will be cross-referenced with other reports, and based on a number of factors
such as the freshness of a report and the frequency, a prioritized list gets generated.

The plugin will then periodically ask the service for a fresh copy of this list, which it will then use
to hide chat messages, trade requests, and even the player itself from your game (if you wish to do so).

The result is an experience comparable to a player moderator reporting and muting a player in-game!

## Why is this better than normal reports and mutes? What problem does this really solve?

It isn't. Instead, you should look at it as an addition; a second line of defense.

In the base game, the moment you report a player, that player is added to your account's ignore list, but will still be
able to continue their possibly abusive behaviour towards others until that report is reviewed by RuneScape's team.

These reports don't just include player-made reports though, but also reports that the system automatically flags for
review. As you can imagine, this means that this team has to review hundreds if not thousands of reports every day.

Now, a solution has long existed to this problem, in the form of Player Moderators: A group of regular players that
have proven themselves to be trustworthy when it comes to submitting accurate abuse reports, that were granted the
power to instantly short-term mute players for everyone else in the game as well while their report was reviewed.

This has solved the problem for most kinds of abusive behaviour, but hasn't for one: _bots_. Being the endless 
cat-and-mouse game that it is, bot-makers have answered with brute force: If it takes a few hours for a report to get
reviewed and the account banned, then that's still a few hours. 

This is essentially what you see today. When one bot is up and running, another one that's intended to replace it is
already being prepared, so that once the first bot is banned, the second is ready to go. With this whole process
automated, it appears to players as if these bots never get banned.

This is then also where this plugin intends to fill the gap: If it only takes a few hours for a player to get banned or
muted in-game for everyone, we can guesstimate whether a report is likely valid, and instantly mute that player for
a short while. Then, once we lift our mute, the game takes over and keep the player muted if the report was accurate.

The result is a beautiful summer day with a nice light breeze for anyone that uses this plugin.
