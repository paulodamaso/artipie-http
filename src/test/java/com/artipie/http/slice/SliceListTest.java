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

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SliceList}.
 *
 * @since 0.10
 * @todo #158:30min Implement SliceList
 *  Implement slice list, which receives a key and return a list of values
 *  found in repository bound to that key, printing result to an HTML. Then
 *  enable the test below and return coverage missed classes values to 15.
 */
public final class SliceListTest {

    /**
     * Get method.
     */
    private static final String METHOD = "GET";

    /**
     * Http.
     */
    private static final String HTTP = "HTTP/1.1";

    @Test
    @Disabled
    void returnsList() {
        final Storage storage = new InMemoryStorage();
        storage.save(
            new Key.From("com/artipie/FileOne.txt"),
            new Content.From("File One Content".getBytes(StandardCharsets.UTF_8))
        ).join();
        storage.save(
            new Key.From("com/artipie/FileTwo.txt"),
            new Content.From("File Two Content".getBytes(StandardCharsets.UTF_8))
        ).join();
        storage.save(
            new Key.From("com/artipie/FileThree.txt"),
            new Content.From("File Three Content".getBytes(StandardCharsets.UTF_8))
        ).join();
        storage.save(
            new Key.From("other", "path", "FileFour.txt"),
            new Content.From("File Four Content".getBytes(StandardCharsets.UTF_8))
        ).join();
        MatcherAssert.assertThat(
            new SliceList(
                storage
            ).response(
                new RequestLine(
                    SliceListTest.METHOD,
                    new Key.From("com", "artipie").string(),
                    SliceListTest.HTTP
                ).toString(),
                Collections.emptyList(),
                Flowable.empty()
            ),
            new ResponseMatcher(
                //@checkstyle LineLengthCheck (1 line)
                "<html><body><ul><li>FileOne.txt</li><li>FileTwo.txt</li><li>FileThree.txt</li></ul></body></html>".getBytes(StandardCharsets.UTF_8)
            )
        );
    }

    @Test
    @Disabled
    void returnsNotFound() {
        final Storage storage = new InMemoryStorage();
        storage.save(
            new Key.From("com/artipie/File404.txt"),
            new Content.From("File 404 Content".getBytes(StandardCharsets.UTF_8))
        ).join();
        MatcherAssert.assertThat(
            new SliceList(
                storage
            ).response(
                new RequestLine(
                    SliceListTest.METHOD,
                    new Key.From("any").string(),
                    SliceListTest.HTTP
                ).toString(),
                Collections.emptyList(),
                Flowable.empty()
            ),
            new RsHasStatus(
                RsStatus.NOT_FOUND
            )
        );
    }
}