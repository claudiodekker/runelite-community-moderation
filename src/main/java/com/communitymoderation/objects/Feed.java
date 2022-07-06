package com.communitymoderation.objects;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Feed
{
	public int apiVersion;
	public Instant expiresAt;
	public List<String> players;

	public Feed()
	{
		this.apiVersion = -1;
		this.expiresAt = Instant.now().plusSeconds(120);
		this.players = new ArrayList<>();
	}

	public Feed(int apiVersion, Instant expiresAt, ArrayList<String> players)
	{
		this.apiVersion = apiVersion;
		this.expiresAt = expiresAt;
		this.players = players;
	}
}
