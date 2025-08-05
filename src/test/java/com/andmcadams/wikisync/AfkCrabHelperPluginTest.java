package com.andmcadams.wikisync;

import com.afkcrabhelper.AfkCrabHelperPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AfkCrabHelperPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(AfkCrabHelperPlugin.class);
		RuneLite.main(args);
	}
}
