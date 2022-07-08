package com.communitymoderation.objects;

import com.communitymoderation.CommunityModerationConfig;
import com.communitymoderation.CommunityModerationService;
import com.communitymoderation.PlayerManager;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.MessageNode;
import net.runelite.api.clan.ClanChannel;
import net.runelite.client.util.Text;

public class Player
{
	@Inject
	protected Client client;
	@Inject
	protected CommunityModerationConfig config;
	@Inject
	protected CommunityModerationService service;
	@Inject
	protected PlayerManager players;
	@Getter
	protected final String name;
	private String hash;

	public Player(String playerName)
	{
		this.name = playerName;
	}

	public String getHash()
	{
		if (this.hash != null)
		{
			return this.hash;
		}

		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.reset();
			digest.update(this.name.getBytes(StandardCharsets.UTF_8));

			return this.hash = String.format("%040x", new BigInteger(1, digest.digest()));
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new RuntimeException(e);
		}
	}

	public List<String> getRecentMessages()
	{
		HashMap<Integer, MessageNode> allPlayerMessages = new HashMap<>();
		client.getChatLineMap().values().forEach(buffer -> {
			if (buffer == null)
			{
				return;
			}

			Arrays.stream(buffer.getLines().clone())
				.filter(Objects::nonNull)
				.filter(chatLine -> Text.standardize(chatLine.getName()).equals(this.name))
				.forEach(node -> allPlayerMessages.put(node.getId(), node));
		});

		return allPlayerMessages.values().stream()
			.sorted((m1, m2) -> m2.getTimestamp() - m1.getTimestamp())
			.limit(5)
			.map(messageNode -> messageNode.getTimestamp() + "| " + messageNode.getValue())
			.collect(Collectors.toList());
	}

	public boolean is(Player player)
	{
		return player.getHash().equals(this.getHash());
	}

	public boolean isManuallyWhitelisted()
	{
		return Text.fromCSV(config.allowedPlayers())
			.stream()
			.anyMatch(name -> players.find(name).is(this));
	}

	public boolean isMuted()
	{
		return this.service.getFeed().hashes.contains(this.getHash());
	}

	public boolean isFriend()
	{
		return client.isFriended(this.name, false);
	}

	public boolean isFriendsChatMember()
	{
		FriendsChatManager friendsChatManager = client.getFriendsChatManager();

		return friendsChatManager != null && friendsChatManager.findByName(this.name) != null;
	}

	public boolean isClanChatMember()
	{
		ClanChannel clanChannel = client.getClanChannel();
		if (clanChannel != null && clanChannel.findMember(this.name) != null)
		{
			return true;
		}

		clanChannel = client.getGuestClanChannel();

		return clanChannel != null && clanChannel.findMember(this.name) != null;
	}

	public void report()
	{
		this.service.submitReport(new Report(
			this.getName(),
			this.getRecentMessages()
		));
	}
}
