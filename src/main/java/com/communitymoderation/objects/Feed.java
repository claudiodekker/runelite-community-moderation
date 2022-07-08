package com.communitymoderation.objects;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Feed
{
	public final int apiVersion;
	public final Instant expiresAt;
	public final List<String> hashes;

	public Feed()
	{
		this.apiVersion = -1;
		this.expiresAt = Instant.now().plusSeconds(120);
		this.hashes = new ArrayList<>();
	}

	public Feed(int apiVersion, Instant expiresAt, ArrayList<String> players)
	{
		this.apiVersion = apiVersion;
		this.expiresAt = expiresAt;
		this.hashes = players;
	}
}
