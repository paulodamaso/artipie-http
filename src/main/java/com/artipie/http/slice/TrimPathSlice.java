/*
 * MIT License
 *
 * Copyright (c) 2020 Artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.http.slice;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.Header;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.google.common.collect.Iterables;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.client.utils.URIBuilder;
import org.reactivestreams.Publisher;

/**
 * Slice that removes the first part from the request URI.
 * <p>
 * For example {@code GET http://www.w3.org/pub/WWW/TheProject.html HTTP/1.1}
 * would be {@code GET http://www.w3.org/WWW/TheProject.html HTTP/1.1}.<br/>
 * The full path will be available as the value of {@code X-FullPath} header.
 * </p>
 *
 * @since 0.8
 */
public final class TrimPathSlice implements Slice {

    /**
     * Delegate slice.
     */
    private final Slice slice;

    /**
     * Pattern to trim.
     */
    private final Pattern ptn;

    /**
     * Trim URI path by first hit of path param.
     * @param slice Origin slice
     * @param path Path to trim
     */
    public TrimPathSlice(final Slice slice, final String path) {
        this(
            slice,
            Pattern.compile(String.format("^/(?:%s)(\\/.*)?", TrimPathSlice.normalized(path)))
        );
    }

    /**
     * Trim URI path by pattern.
     *
     * @param slice Origin slice
     * @param ptn Path to trim
     */
    public TrimPathSlice(final Slice slice, final Pattern ptn) {
        this.slice = slice;
        this.ptn = ptn;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final RequestLineFrom rline = new RequestLineFrom(line);
        final URI uri = rline.uri();
        final String full = uri.getPath();
        final Matcher matcher = this.ptn.matcher(full);
        final Response response;
        if (matcher.matches()) {
            response = this.slice.response(
                new RequestLine(
                    rline.method().toString(),
                    new URIBuilder(uri)
                        .setPath(asPath(matcher.group(1)))
                        .toString(),
                    rline.version()
                ).toString(),
                Iterables.concat(
                    headers, Collections.singletonList(new Header("X-FullPath", full))
                ),
                body
            );
        } else {
            response = new RsWithStatus(
                new RsWithBody(
                    String.format(
                        "Request path %s was not matched to %s", full, this.ptn
                    ),
                    StandardCharsets.UTF_8
                ),
                RsStatus.INTERNAL_ERROR
            );
        }
        return response;
    }

    /**
     * Normalize path: remove whitespaces and slash chars.
     * @param path Path
     * @return Normalized path
     * @checkstyle ReturnCountCheck (10 lines)
     */
    @SuppressWarnings("PMD.OnlyOneReturn")
    private static String normalized(final String path) {
        final String clear = Objects.requireNonNull(path).trim();
        if (clear.isEmpty()) {
            return "";
        }
        if (clear.charAt(0) == '/') {
            return normalized(clear.substring(1));
        }
        if (clear.charAt(clear.length() - 1) == '/') {
            return normalized(clear.substring(0, clear.length() - 1));
        }
        return clear;
    }

    /**
     * Convert matched string to valid path.
     * @param result Result of matching
     * @return Path string
     * @checkstyle ReturnCountCheck (15 lines)
     */
    @SuppressWarnings("PMD.OnlyOneReturn")
    private static String asPath(final String result) {
        if (result == null || result.isEmpty()) {
            return "/";
        }
        if (result.charAt(0) != '/') {
            return '/' + result;
        }
        return result;
    }
}
