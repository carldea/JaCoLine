/*
 * Copyright (c) 2019 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/JaCoLine/blob/master/LICENSE
 */
package com.chrisnewland.jacoline.web.filter;

import com.chrisnewland.jacoline.core.CommandLineSwitchParser;
import com.chrisnewland.jacoline.web.service.ServiceUtil;

import org.glassfish.jersey.message.internal.MediaTypes;

import javax.validation.constraints.NotNull;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;

@Provider public class RequestFilter implements ContainerRequestFilter
{
	private static Path pathBadWords;

	public static final void initialise(Path pathBadWords)
	{
		RequestFilter.pathBadWords = pathBadWords;
	}

	@Context private Providers providers;

	private Set<String> loadBadWords() throws IOException
	{
		if (pathBadWords == null)
		{
			throw new RuntimeException("Bad words file not set. Cannot continue.");
		}

		String csv = new String(Files.readAllBytes(pathBadWords), StandardCharsets.UTF_8);

		return new HashSet<>(Arrays.asList(csv.split(",")));
	}

	@Override public void filter(ContainerRequestContext request) throws IOException
	{
		if (!request.hasEntity() || !MediaTypes.typeEqual(APPLICATION_FORM_URLENCODED_TYPE, request.getMediaType()))
		{
			return;
		}

		ByteArrayInputStream resettableIS = toResettableStream(request.getEntityStream());

		Form form = providers.getMessageBodyReader(Form.class, Form.class, new Annotation[0], APPLICATION_FORM_URLENCODED_TYPE)
							 .readFrom(Form.class, Form.class, new Annotation[0], APPLICATION_FORM_URLENCODED_TYPE, null,
									 resettableIS);

		MultivaluedMap<String, String> map = form.asMap();

		Set<String> badWords = loadBadWords();

		for (Map.Entry<String, List<String>> entry : map.entrySet())
		{
			String key = entry.getKey();

			List<String> valueList = entry.getValue();

			if (valueList.size() != 1)
			{
				abortBadParams(request);
			}
			else
			{
				String value = valueList.get(0);

				checkValue(key, value, badWords, request);
			}
		}

		resettableIS.reset();

		request.setEntityStream(resettableIS);
	}

	private void checkValue(String key, String value, Set<String> badWords, ContainerRequestContext requestContext)
	{
		switch (key)
		{
		case "jdk":
			if (!CommandLineSwitchParser.getJDKList().contains(value))
			{
				abortBadParams(requestContext);
			}
			break;
		case "os":
			if (!CommandLineSwitchParser.getOperatingSystemList().contains(value))
			{
				abortBadParams(requestContext);
			}
			break;
		case "arch":
			if (!CommandLineSwitchParser.getArchitectureList().contains(value))
			{
				abortBadParams(requestContext);
			}
			break;
		default:
			if (isBadWord(badWords, key, value))
			{
				abortBadParams(requestContext);
			}
		}
	}

	private void abortBadParams(ContainerRequestContext request)
	{
		String errorPage;

		try
		{
			errorPage = ServiceUtil.loadError();
		}
		catch (IOException ioe)
		{
			errorPage = "";
		}

		Response redirectToErrorPage = Response.status(Response.Status.BAD_REQUEST).entity(errorPage).build();

		request.abortWith(redirectToErrorPage);
	}

	private boolean isBadWord(Set<String> badWords, String paramName, String parameterValue)
	{
		if (parameterValue != null)
		{
			parameterValue = parameterValue.toLowerCase();

			for (String bad : badWords)
			{
				if (parameterValue.contains(bad))
				{
					System.out.println("Bad value for parameter '" + paramName + "' contains '" + bad + "'");

					return true;
				}
			}
		}

		return false;
	}

	@NotNull private ByteArrayInputStream toResettableStream(InputStream entityStream) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		byte[] buffer = new byte[1024];

		int len;

		while ((len = entityStream.read(buffer)) > -1)
		{
			baos.write(buffer, 0, len);
		}

		baos.flush();

		return new ByteArrayInputStream(baos.toByteArray());
	}
}