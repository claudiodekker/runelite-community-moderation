package com.communitymoderation;

import com.communitymoderation.objects.Player;
import com.google.inject.Injector;
import java.util.HashMap;
import java.util.LinkedList;
import lombok.Setter;
import net.runelite.client.util.Text;

public class PlayerManager
{
	protected static final int CACHE_SIZE = 300;
	protected final HashMap<String, Player> playerCache = new HashMap<>();
	protected final LinkedList<String> fifoPriorityList = new LinkedList<>();
	@Setter
	protected Injector injector;

	public Player find(String rawPlayerName)
	{
		String playerName = Text.standardize(rawPlayerName);

		// Ensure we always have the requested Player in the cache.
		if (!playerCache.containsKey(playerName))
		{
			playerCache.put(playerName, preparePlayer(playerName));
		}

		// Move the player entry to the end of the list.
		// If the player isn't in the list, remove will fail gracefully.
		fifoPriorityList.remove(playerName);
		fifoPriorityList.add(playerName);

		// Garbage-collect old players that we haven't seen in a while...
		if (fifoPriorityList.size() > CACHE_SIZE)
		{
			playerCache.remove(fifoPriorityList.removeFirst());
		}

		return playerCache.get(playerName);
	}

	protected Player preparePlayer(String playerName)
	{
		Player instance = new Player(playerName);
		injector.injectMembers(instance);

		return instance;
	}
}
