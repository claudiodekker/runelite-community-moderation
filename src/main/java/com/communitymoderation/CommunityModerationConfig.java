package com.communitymoderation;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("community-moderation")
public interface CommunityModerationConfig extends Config
{
	@ConfigSection(
		name = "Override Muted Players",
		description = "Controls to what level you want to see community-muted players.",
		position = 0
	)
	String overrideMutes = "overrideMutes";

	@ConfigItem(
		keyName = "showMutedFriendMessages",
		name = "Allow muted friends",
		description = "Ignores community-mutes for players on your friends list.",
		position = 1,
		section = overrideMutes
	)
	default boolean showMutedFriendMessages()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMutedClanMemberMessages",
		name = "Allow muted clan members",
		description = "Ignores community-mutes for members of your clan chat.",
		position = 2,
		section = overrideMutes
	)
	default boolean showMutedClanMemberMessages()
	{
		return true;
	}

	@ConfigItem(
		keyName = "allowedPlayers",
		name = "Manually allowed players",
		description = "Allows you to manually ignore community-mutes. (comma-separated, case insensitive)",
		position = 3,
		section = overrideMutes
	)
	default String allowedPlayers()
	{
		return "";
	}

	@ConfigItem(
		keyName = "showMutedPlayers",
		name = "Show community-muted players",
		description = "Disables the muting aspect of the plugin, while keeping the reporting enabled. Intended for Player Moderators (PMods) who take their job seriously.",
		position = 4,
		section = overrideMutes
	)
	default boolean showMutedPlayers()
	{
		return false;
	}

	@ConfigSection(
		name = "Entity Hider",
		description = "Controls settings related to community-muted player visibility.",
		position = 5
	)
	String entityHider = "entityHider";

	@ConfigItem(
		keyName = "hideMutedPlayers",
		name = "Hide muted players",
		description = "Configures whether or not community-muted players are hidden.",
		position = 6,
		section = entityHider
	)
	default boolean hideMutedPlayers()
	{
		return false;
	}
}
