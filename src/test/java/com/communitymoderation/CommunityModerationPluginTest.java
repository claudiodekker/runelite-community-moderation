package com.communitymoderation;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CommunityModerationPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CommunityModerationPlugin.class);
		RuneLite.main(args);
	}
}