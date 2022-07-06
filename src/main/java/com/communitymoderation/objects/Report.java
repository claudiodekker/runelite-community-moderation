package com.communitymoderation.objects;

import java.util.List;

public class Report
{
	public String playerName;
	public List<String> recentMessages;

	public Report(String playerName, List<String> recentMessages)
	{
		this.playerName = playerName;
		this.recentMessages = recentMessages;
	}
}
