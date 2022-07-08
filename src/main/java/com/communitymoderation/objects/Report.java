package com.communitymoderation.objects;

import java.util.List;

public class Report
{
	public final String playerName;
	public final List<String> recentMessages;

	public Report(String playerName, List<String> recentMessages)
	{
		this.playerName = playerName;
		this.recentMessages = recentMessages;
	}
}
