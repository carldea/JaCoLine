package com.chrisnewland.jacoline.commandline;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Serialiser
{
	public List<SwitchInfo> deserialise(Path pathToSerialisedSwitchInfo) throws IOException
	{
		List<String> lines = Files.readAllLines(pathToSerialisedSwitchInfo, StandardCharsets.UTF_8);

		List<SwitchInfo> result = new ArrayList<>(lines.size());

		for (String line : lines)
		{
			SwitchInfo switchInfo = SwitchInfo.deserialise(line);

			result.add(switchInfo);
		}

		return result;
	}

	public void serialise(Path pathToSerialisationFile, Collection<SwitchInfo> switchInfoSet) throws IOException
	{
		StringBuilder builder = new StringBuilder();

		for (SwitchInfo switchInfo : switchInfoSet)
		{
			builder.append(switchInfo.serialise()).append("\n");
		}

		System.out.println("Serialised to " + pathToSerialisationFile.toString());

		Files.write(pathToSerialisationFile, builder.toString().getBytes(StandardCharsets.UTF_8));
	}
}
