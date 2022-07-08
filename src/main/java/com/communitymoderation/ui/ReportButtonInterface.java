package com.communitymoderation.ui;

import com.communitymoderation.CommunityModerationPlugin;
import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ScriptEvent;
import net.runelite.api.SpriteID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;

public class ReportButtonInterface
{
	private final CommunityModerationPlugin plugin;

	private final Client client;

	private Widget reportButtonCheckbox;

	private Widget reportButtonText;

	private Widget reportRulesContainer;

	@Inject
	public ReportButtonInterface(Client client, CommunityModerationPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
	}

	public void init()
	{
		if (isHidden())
		{
			return;
		}

		reportRulesContainer = client.getWidget(553, 14);

		reportButtonCheckbox = reportRulesContainer.createChild(WidgetType.GRAPHIC);
		reportButtonCheckbox.setAction(1, "Toggle");
		reportButtonCheckbox.setHasListener(true);
		reportButtonCheckbox.setName("report to Community Mod Plugin");
		reportButtonCheckbox.setOnOpListener((JavaScriptCallback) this::toggleSendCommunityReport);
		reportButtonCheckbox.setOriginalHeight(16);
		reportButtonCheckbox.setOriginalWidth(16);
		reportButtonCheckbox.setOriginalX(214);
		reportButtonCheckbox.setOriginalY(147);
		reportButtonCheckbox.setSpriteId(SpriteID.SQUARE_CHECK_BOX_CHECKED);
		reportButtonCheckbox.revalidate();

		reportButtonText = reportRulesContainer.createChild(WidgetType.TEXT);
		reportButtonText.setAction(1, "Toggle");
		reportButtonText.setFontId(494);
		reportButtonText.setHasListener(true);
		reportButtonText.setName("report to Community Mod Plugin");
		reportButtonText.setOnOpListener((JavaScriptCallback) this::toggleSendCommunityReport);
		reportButtonText.setOriginalHeight(21);
		reportButtonText.setOriginalWidth(224);
		reportButtonText.setOriginalX(234);
		reportButtonText.setOriginalY(147);
		reportButtonText.setText("Also report to Community Mod Plugin");
		reportButtonText.setTextColor(0xFFFFFF);
		reportButtonText.setTextShadowed(true);
		reportButtonText.setYTextAlignment(WidgetTextAlignment.CENTER);
		reportButtonText.revalidate();
	}

	public void destroy()
	{
		if (reportButtonText != null)
		{
			reportButtonCheckbox.setHidden(true);
			reportButtonText.setHidden(true);
		}

		reportRulesContainer = null;
	}

	private void toggleSendCommunityReport(ScriptEvent event)
	{
		if (reportButtonCheckbox.getSpriteId() == SpriteID.SQUARE_CHECK_BOX_CHECKED)
		{
			reportButtonCheckbox.setSpriteId(SpriteID.SQUARE_CHECK_BOX);
			plugin.setSendCommunityReport(false);
			return;
		}

		reportButtonCheckbox.setSpriteId(SpriteID.SQUARE_CHECK_BOX_CHECKED);
		plugin.setSendCommunityReport(true);
	}

	public boolean isHidden()
	{
		Widget widget = client.getWidget(553, 1);

		return widget == null || widget.isHidden();
	}
}
