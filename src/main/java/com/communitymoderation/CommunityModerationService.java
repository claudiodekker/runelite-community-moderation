package com.communitymoderation;

import com.communitymoderation.objects.Feed;
import com.communitymoderation.objects.Report;
import com.communitymoderation.utils.JsonFile;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.util.OSType;
import static net.runelite.http.api.RuneLiteAPI.GSON;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
public class CommunityModerationService
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final HttpUrl BASE_URL = HttpUrl.parse("https://runelite-community-moderation.dekker.io/api/v1");
	private static final String CACHE_FILENAME = "feed";
	private final Client client;
	private final OkHttpClient http;
	private long lastAccount;
	private String machineUUID;
	private Feed currentFeed;

	@Inject
	public CommunityModerationService(OkHttpClient http, Client client)
	{
		this.client = client;
		this.http = http.newBuilder()
			.pingInterval(0, TimeUnit.SECONDS)
			.connectTimeout(30, TimeUnit.SECONDS)
			.readTimeout(30, TimeUnit.SECONDS)
			.addNetworkInterceptor(chain -> chain.proceed(chain.request()
				.newBuilder()
				.header("RuneLite-MachineID", getMachineUUID())
				.build()
			))
			.build();
	}


	private String getMachineUUID()
	{
		long accountHash = client.getAccountHash();

		if (lastAccount != accountHash)
		{
			lastAccount = accountHash;
			machineUUID = calculateMachineUUID(accountHash);
		}

		return machineUUID;
	}

	private String calculateMachineUUID(long accountHash)
	{
		try
		{
			Hasher hasher = Hashing.sha256().newHasher();
			Runtime runtime = Runtime.getRuntime();

			hasher.putByte((byte) OSType.getOSType().ordinal());
			hasher.putByte((byte) runtime.availableProcessors());
			hasher.putUnencodedChars(System.getProperty("os.arch", ""));
			hasher.putUnencodedChars(System.getProperty("os.version", ""));
			hasher.putUnencodedChars(System.getProperty("user.name", ""));

			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (networkInterfaces.hasMoreElements())
			{
				NetworkInterface networkInterface = networkInterfaces.nextElement();
				byte[] hardwareAddress = networkInterface.getHardwareAddress();
				if (hardwareAddress != null)
				{
					hasher.putBytes(hardwareAddress);
				}
			}
			hasher.putLong(accountHash);
			return hasher.hash().toString();
		}
		catch (Exception ex)
		{
			return "";
		}
	}

	private Feed fetchFeed()
	{
		final HttpUrl url = BASE_URL.newBuilder().addPathSegment("feed").build();

		Request request = new Request.Builder()
			.header("RuneLite-MachineID", getMachineUUID())
			.url(url)
			.build();

		try (Response response = http.newCall(request).execute())
		{
			if (response.code() != 200)
			{
				log.debug("Could not load muted player feed due to server error: " + response.code());
				return null;
			}

			InputStream in = response.body().byteStream();

			return GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Feed.class);
		}
		catch (Exception e)
		{
			log.debug("Could not load muted player feed!.", e);
		}

		return null;
	}

	public void submitReport(Report report)
	{
		final HttpUrl url = BASE_URL.newBuilder().addPathSegment("report").build();

		Request request = new Request.Builder()
			.header("RuneLite-MachineID", getMachineUUID())
			.post(RequestBody.create(JSON, GSON.toJson(report)))
			.url(url)
			.build();

		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Unable to submit report.", e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				if (response.code() == 200)
				{
					log.debug("Report submitted.");
				}
				else
				{
					log.debug("Report submission attempt failed with code: " + response.code());
				}
				response.close();
			}
		});
	}

	public Feed getFeed()
	{
		if (currentFeed != null && currentFeed.expiresAt != null && Instant.now().isBefore(currentFeed.expiresAt))
		{
			return currentFeed;
		}

		try
		{
			Feed cachedFeed = JsonFile.read(CACHE_FILENAME, Feed.class);
			if (cachedFeed != null && cachedFeed.expiresAt != null && Instant.now().isBefore(cachedFeed.expiresAt))
			{
				currentFeed = cachedFeed;

				return currentFeed;
			}

			Feed freshFeed = this.fetchFeed();
			if (freshFeed != null && freshFeed.expiresAt != null && Instant.now().isBefore(freshFeed.expiresAt))
			{
				JsonFile.write(CACHE_FILENAME, freshFeed);
				currentFeed = freshFeed;

				return currentFeed;
			}
		}
		catch (Exception e)
		{
			log.debug("Failed loading muted players.", e);
		}

		currentFeed = new Feed();

		return currentFeed;
	}
}
