package com.communitymoderation;

import com.communitymoderation.objects.Report;
import com.communitymoderation.ui.ReportButtonInterface;
import com.google.inject.Provides;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import static net.runelite.api.ChatMessageType.*;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.MessageNode;
import net.runelite.api.Player;
import net.runelite.api.Renderable;
import net.runelite.api.ScriptID;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.VarClientStrChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Community Moderation"
)
public class CommunityModerationPlugin extends Plugin
{
	protected static final List<ChatMessageType> supportedChatTypes = Arrays.asList(
		PUBLICCHAT,
		PRIVATECHAT,
		FRIENDSCHAT,
		CLAN_CHAT,
		CLAN_GUEST_CHAT,
		AUTOTYPER,
		TRADEREQ,
		CHALREQ_TRADE,
		CHALREQ_FRIENDSCHAT,
		CLAN_GIM_CHAT
	);
	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private Hooks hooks;
	@Inject
	private CommunityModerationConfig config;
	@Inject
	private ReportButtonInterface reportButtonInterface;
	@Inject
	private CommunityModerationService service;
	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;
	@Setter
	private Boolean sendCommunityReport;
	private String reportedPlayer;

	@Provides
	CommunityModerationConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CommunityModerationConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		hooks.registerRenderableDrawListener(drawListener);
		clientThread.invokeLater(reportButtonInterface::init);
		log.info("Community Mod Plugin started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		hooks.unregisterRenderableDrawListener(drawListener);
		clientThread.invokeLater(reportButtonInterface::destroy);
		log.info("Community Mod Plugin stopped!");
	}

	boolean isUnmutedPlayer(String rawPlayerName)
	{
		if (config.showMutedPlayers())
		{
			return true;
		}

		final String playerName = Text.standardize(rawPlayerName);
		final Player localPlayer = client.getLocalPlayer();

		if (localPlayer != null && Text.standardize(localPlayer.getName()).equals(playerName))
		{
			return true;
		}

		if (config.showMutedFriendMessages() && client.isFriended(playerName, false))
		{
			return true;
		}

		if (config.showMutedClanMemberMessages() && (isFriendsChatMember(playerName) || isClanChatMember(playerName)))
		{
			return true;
		}

		if (Text.fromCSV(config.allowedPlayers()).stream().anyMatch(name -> Text.standardize(name).equals(playerName)))
		{
			return true;
		}

		return this.service.getFeed().players.stream().noneMatch(name -> Text.standardize(name).equals(playerName));
	}

	private boolean isClanChatMember(String name)
	{
		ClanChannel clanChannel = client.getClanChannel();
		if (clanChannel != null && clanChannel.findMember(name) != null)
		{
			return true;
		}

		clanChannel = client.getGuestClanChannel();

		return clanChannel != null && clanChannel.findMember(name) != null;
	}

	private boolean isFriendsChatMember(String name)
	{
		FriendsChatManager friendsChatManager = client.getFriendsChatManager();

		return friendsChatManager != null && friendsChatManager.findByName(name) != null;
	}

	@Subscribe(priority = -2)
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		if (!(event.getActor() instanceof Player) || isUnmutedPlayer(event.getActor().getName()))
		{
			return;
		}

		event.getActor().setOverheadText("");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"communitymod".equals(event.getGroup()))
		{
			return;
		}

		client.refreshChat();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		ChatMessageType chatMessageType = event.getType();
		if (!supportedChatTypes.contains(chatMessageType))
		{
			return;
		}

		if (isUnmutedPlayer(event.getName()))
		{
			return;
		}

		ChatLineBuffer lineBuffer = client.getChatLineMap().get(chatMessageType.getType());
		if (lineBuffer == null)
		{
			return;
		}

		lineBuffer.removeMessageNode(event.getMessageNode());
		clientThread.invokeLater(() -> client.runScript(ScriptID.SPLITPM_CHANGED));
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!"chatFilterCheck".equals(event.getEventName()))
		{
			return;
		}

		// https://github.com/runelite/runelite/blob/1cddbcef7dea161f7d85f158813ed8f873f7557d/runelite-client/src/main/scripts/ChatSplitBuilder.rs2asm#L402-L407
		int[] intStack = client.getIntStack();
		int intStackSize = client.getIntStackSize();
		final int messageId = intStack[intStackSize - 1];

		final MessageNode messageNode = client.getMessages().get(messageId);
		final ChatMessageType chatMessageType = messageNode.getType();
		final String playerName = messageNode.getName();

		if (!supportedChatTypes.contains(chatMessageType) || isUnmutedPlayer(playerName))
		{
			return;
		}

		// Block the chat message.
		intStack[intStackSize - 3] = 0;
	}

	boolean shouldDraw(Renderable renderable, boolean drawingUI)
	{
		if (!config.hideMutedPlayers())
		{
			return true;
		}

		if (!(renderable instanceof Player))
		{
			return true;
		}

		return isUnmutedPlayer(((Player) renderable).getName());
	}

	@Subscribe
	public void onVarClientStrChanged(VarClientStrChanged event)
	{
		if (event.getIndex() != 370)
		{
			return;
		}

		String offendingPlayerName = Text.standardize(client.getVarcStrValue(370));
		if (offendingPlayerName.isEmpty())
		{
			return;
		}

		reportedPlayer = offendingPlayerName;
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() != 1123)
		{
			return;
		}

		if (!this.sendCommunityReport)
		{
			reportedPlayer = null;
			return;
		}

		HashMap<Integer, MessageNode> allPlayerMessages = new HashMap<>();
		client.getChatLineMap().values().forEach(buffer -> {
			if (buffer == null)
			{
				return;
			}

			Arrays.stream(buffer.getLines().clone())
				.filter(Objects::nonNull)
				.filter(chatLine -> Text.standardize(chatLine.getName()).equals(reportedPlayer))
				.forEach(node -> allPlayerMessages.put(node.getId(), node));
		});

		List<String> mostRecentPlayerMessages = allPlayerMessages.values().stream()
			.sorted((m1, m2) -> m2.getTimestamp() - m1.getTimestamp())
			.limit(5)
			.map(messageNode -> messageNode.getTimestamp() + "| " + messageNode.getValue())
			.collect(Collectors.toList());

		this.service.submitReport(new Report(
			reportedPlayer,
			mostRecentPlayerMessages
		));
		reportedPlayer = null;
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == 553)
		{
			sendCommunityReport = true;
			reportButtonInterface.init();
		}
	}
}
