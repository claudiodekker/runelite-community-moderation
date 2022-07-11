package com.communitymoderation;

import com.communitymoderation.objects.Player;
import com.communitymoderation.ui.ReportButtonInterface;
import com.google.inject.Provides;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import static net.runelite.api.ChatMessageType.*;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.Renderable;
import net.runelite.api.ScriptID;
import net.runelite.api.Varbits;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.VarClientStrChanged;
import net.runelite.api.events.VarbitChanged;
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
	@Inject
	private PlayerManager players;

	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;
	@Setter
	private Boolean sendCommunityReport;
	private String offendingPlayerName;

	private Player localPlayer;
	private Boolean inPvp = false;

	@Provides
	protected CommunityModerationConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CommunityModerationConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		players.setInjector(injector);
		hooks.registerRenderableDrawListener(drawListener);
		clientThread.invokeLater(reportButtonInterface::init);
		clientThread.invokeLater(this::updatePvpState);
		log.info("Community Mod Plugin started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		hooks.unregisterRenderableDrawListener(drawListener);
		clientThread.invokeLater(reportButtonInterface::destroy);
		log.info("Community Mod Plugin stopped!");
	}

	protected void updatePvpState()
	{
		this.inPvp = client.getVarbitValue(Varbits.PVP_SPEC_ORB) == 1;
	}

	public boolean isUnmutedPlayer(String playerName)
	{
		net.runelite.api.Player localPlayer = client.getLocalPlayer();
		if (config.showMutedPlayers() || localPlayer == null)
		{
			return true;
		}

		final Player player = players.find(playerName);
		if (player.is(players.find(localPlayer.getName())))
		{
			return true;
		}

		if (this.inPvp)
		{
			return true;
		}

		if (!player.isMuted())
		{
			return true;
		}

		if (player.isManuallyWhitelisted())
		{
			return true;
		}

		if (config.showMutedFriendMessages() && player.isFriend())
		{
			return true;
		}

		if (config.showMutedClanMemberMessages() && (player.isFriendsChatMember() || player.isClanChatMember()))
		{
			return true;
		}

		return false;
	}

	protected boolean shouldDraw(Renderable renderable, boolean drawingUI)
	{
		if (!config.hideMutedPlayers())
		{
			return true;
		}

		if (!(renderable instanceof net.runelite.api.Player))
		{
			return true;
		}

		return isUnmutedPlayer(
			((net.runelite.api.Player) renderable).getName()
		);
	}

	@Subscribe
	protected void onChatMessage(ChatMessage event)
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
	protected void onConfigChanged(ConfigChanged event)
	{
		if ("community-moderation".equals(event.getGroup()))
		{
			client.refreshChat();
		}
	}

	@Subscribe(priority = -2)
	protected void onOverheadTextChanged(OverheadTextChanged event)
	{
		if (!(event.getActor() instanceof net.runelite.api.Player))
		{
			return;
		}

		if (isUnmutedPlayer(event.getActor().getName()))
		{
			return;
		}

		event.getActor().setOverheadText("");
	}

	@Subscribe
	protected void onScriptCallbackEvent(ScriptCallbackEvent event)
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

	@Subscribe
	protected void onScriptPreFired(ScriptPreFired event)
	{
		// 1123 = Script that gets called when a player clicks on a Report offense reason.
		if (event.getScriptId() != 1123)
		{
			return;
		}

		if (!this.sendCommunityReport)
		{
			offendingPlayerName = null;
			return;
		}

		players.find(offendingPlayerName).report();
		offendingPlayerName = null;
	}

	@Subscribe
	protected void onVarbitChanged(VarbitChanged event)
	{
		updatePvpState();
	}

	@Subscribe
	protected void onVarClientStrChanged(VarClientStrChanged event)
	{
		// VarClientStr 370 is the "Offending Player" field in the Report interface.
		if (event.getIndex() != 370)
		{
			return;
		}

		String offendingPlayerName = Text.standardize(client.getVarcStrValue(370));
		if (offendingPlayerName.isEmpty())
		{
			// We don't want to clear the offending player name when it's empty, as this also
			// happens automatically when the report is submitted, preventing us from using
			// the name in our own reports. Instead, we'll just clear it ourselves later.
			return;
		}

		this.offendingPlayerName = offendingPlayerName;
	}

	@Subscribe
	protected void onWidgetLoaded(WidgetLoaded event)
	{
		// Widget Group 553 = Report Dialog
		if (event.getGroupId() == 553)
		{
			sendCommunityReport = true;
			reportButtonInterface.init();
		}
	}
}
