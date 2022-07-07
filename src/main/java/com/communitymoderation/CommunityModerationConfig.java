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
		name = "Show muted friends",
		description = "Disables community-mutes for players on your friends list.",
		position = 1,
		section = overrideMutes
	)
	default boolean showMutedFriendMessages()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMutedClanMemberMessages",
		name = "Show muted clan members",
		description = "Disables community-mutes for members of your clan chat.",
		position = 2,
		section = overrideMutes
	)
	default boolean showMutedClanMemberMessages()
	{
		return true;
	}

	@ConfigItem(
		keyName = "allowedPlayers",
		name = "Additional Players",
		description = "Disable community-mutes for the given players. (comma-separated, case insensitive)",
		position = 3,
		section = overrideMutes
	)
	default String allowedPlayers()
	{
		return "";
	}

	@ConfigItem(
		keyName = "showMutedPlayers",
		name = "Show all muted players",
		description = "Disables the muting aspect of the plugin, while keeping the reporting enabled (Useful for Player Moderators / PMods).",
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
