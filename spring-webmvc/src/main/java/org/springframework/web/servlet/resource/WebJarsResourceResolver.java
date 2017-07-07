/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.resource;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.webjars.MultipleMatchesException;
import org.webjars.WebJarAssetLocator;

import org.springframework.core.io.Resource;

/**
 * A {@code ResourceResolver} that delegates to the chain to locate a resource and then
 * attempts to find a matching versioned resource contained in a WebJar JAR file.
 *
 * <p>This allows WebJars.org users to write version agnostic paths in their templates,
 * like {@code <script src="/jquery/jquery.min.js"/>}.
 * This path will be resolved to the unique version {@code <script src="/jquery/1.2.0/jquery.min.js"/>},
 * which is a better fit for HTTP caching and version management in applications.
 *
 * <p>This also resolves resources for version agnostic HTTP requests {@code "GET /jquery/jquery.min.js"}.
 *
 * <p>This resolver requires the "org.webjars:webjars-locator" library on classpath,
 * and is automatically registered if that library is present.
 *
 * @author Brian Clozel
 * @since 4.2
 * @see org.springframework.web.servlet.config.annotation.ResourceChainRegistration
 * @see <a href="http://www.webjars.org">webjars.org</a>
 */
public class WebJarsResourceResolver extends AbstractResourceResolver {

	private final static String WEBJARS_LOCATION = "META-INF/resources/webjars/";

	private final static int WEBJARS_LOCATION_LENGTH = WEBJARS_LOCATION.length();

	private final WebJarAssetLocator webJarAssetLocator = new WebJarAssetLocator();


	@Override
	protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		Resource resolved = chain.resolveResource(request, requestPath, locations);
		if (resolved == null) {
			String webJarResourcePath = findWebJarResourcePath(requestPath);
			if (webJarResourcePath != null) {
				return chain.resolveResource(request, webJarResourcePath, locations);
			}
		}
		return resolved;
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		String path = chain.resolveUrlPath(resourceUrlPath, locations);
		if (path == null) {
			String webJarResourcePath = findWebJarResourcePath(resourceUrlPath);
			if (webJarResourcePath != null) {
				return chain.resolveUrlPath(webJarResourcePath, locations);
			}
		}
		return path;
	}

	protected String findWebJarResourcePath(String path) {
		try {
			int startOffset = (path.startsWith("/") ? 1 : 0);
			int endOffset = path.indexOf("/", 1);
			if (endOffset != -1) {
				String webjar = path.substring(startOffset, endOffset);
				String partialPath = path.substring(endOffset);
				String webJarPath = webJarAssetLocator.getFullPath(webjar, partialPath);
				return webJarPath.substring(WEBJARS_LOCATION_LENGTH);
			}
		}
		catch (MultipleMatchesException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("WebJar version conflict for \"" + path + "\"", ex);
			}
		}
		catch (IllegalArgumentException ex) {
			if (logger.isTraceEnabled()) {
				logger.trace("No WebJar resource found for \"" + path + "\"");
			}
		}
		return null;
	}

}
