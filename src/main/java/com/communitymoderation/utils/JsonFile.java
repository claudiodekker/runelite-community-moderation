package com.communitymoderation.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import net.runelite.client.RuneLite;
import static net.runelite.http.api.RuneLiteAPI.GSON;

public class JsonFile
{
	private static final File BASE_PATH = new File(RuneLite.RUNELITE_DIR, "community-moderation");

	protected static void ensureDirExists()
	{
		BASE_PATH.mkdirs();
	}

	protected static File getHandle(String filename) throws IOException
	{
		ensureDirExists();
		File handle = new File(BASE_PATH, filename + ".json");
		if (!handle.exists())
		{
			handle.createNewFile();
		}

		return handle;
	}

	public static <T> T read(String filename, Class<T> type) throws IOException
	{
		FileReader reader = new FileReader(getHandle(filename));
		T result = GSON.fromJson(reader, type);
		reader.close();

		return result;
	}

	public static void write(String filename, Object contents) throws IOException
	{
		FileWriter writer = new FileWriter(getHandle(filename));
		GSON.toJson(contents, writer);
		writer.close();
	}
}
