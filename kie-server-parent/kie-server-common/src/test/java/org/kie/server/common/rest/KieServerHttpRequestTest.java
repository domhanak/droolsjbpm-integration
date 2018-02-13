/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
/*
 * Copyright (c) 2014 Kevin Sawicki <kevinsawicki@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package org.kie.server.common.rest;



import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static javax.ws.rs.core.HttpHeaders.IF_NONE_MATCH;
import static javax.ws.rs.core.HttpHeaders.USER_AGENT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.assertj.core.api.Assertions.fail;
import static org.kie.server.common.rest.KieServerHttpRequest.CHARSET_UTF8;
import static org.kie.server.common.rest.KieServerHttpRequest.appendQueryParameters;
import static org.kie.server.common.rest.KieServerHttpRequest.deleteRequest;
import static org.kie.server.common.rest.KieServerHttpRequest.getRequest;
import static org.kie.server.common.rest.KieServerHttpRequest.newRequest;
import static org.kie.server.common.rest.KieServerHttpRequest.postRequest;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.B64Code;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit tests of {@link KieServerHttpRequest}
 */
public class KieServerHttpRequestTest extends ServerTestCase {

    private static String url;

    private static RequestHandler handler;

    /**
     * Set up server
     *
     * @throws Exception
     */
    @BeforeClass
    public static void startServer() throws Exception {
        url = setUp(new RequestHandler() {

            @Override
            public void handle( String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response )
                    throws IOException, ServletException {
                if( handler != null )
                    handler.handle(target, baseRequest, request, response);
            }

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                if( handler != null )
                    handler.handle(request, response);
            }
        });
    }

    /**
     * Clear handler
     */
    @After
    public void clearHandler() {
        handler = null;
    }

    /**
     * Create request with malformed URL
     */
    @Test(expected = KieServerHttpRequestException.class)
    public void malformedStringUrlTest() {
        KieServerHttpRequest.newRequest("\\m/");
    }

    /**
     * Create request with malformed URL
     */
    @Test
    public void malformedStringUrlCauseTest() {
        try {
            KieServerHttpRequest.newRequest("\\m/");
            fail("Exception not thrown");
        } catch( KieServerHttpRequestException e ) {
            assertThat(e.getCause()).isNotNull();
        }
    }

    /**
     * Set request buffer size to negative value
     */
    @Test(expected = IllegalArgumentException.class)
    public void negativeBufferSize() {
        KieServerHttpRequest.newRequest("http://localhost").bufferSize(-1);
    }

    /**
     * Make a GET request with an empty body response
     *
     * @throws Exception
     */
    @Test
    public void getEmptyTest() throws Exception {
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                response.setStatus(HTTP_OK);
            }
        };
        
        KieServerHttpRequest request = getRequest(new URL(url));
        assertThat(request.getConnection()).isNotNull();
        assertThat(request.timeout(30000).getConnection().getReadTimeout()).isEqualTo(30000);
        assertThat(request.bufferSize(2500).bufferSize()).isEqualTo(2500);
        assertThat(request.ignoreCloseExceptions(false).ignoreCloseExceptions()).isFalse();
        int code = request.get().response().code();
        assertThat(code).isEqualTo(200);
        assertThat(method.get()).isEqualTo("GET");
        assertThat(request.response().message()).isEqualTo("OK");
        assertThat(code).isEqualTo(HTTP_OK);
        assertThat(request.response().body()).isEqualTo("");
        assertThat(request.toString()).isNotNull();
        assertThat(request.toString().length() == 0).isFalse();
        assertThat(request.disconnect()).isEqualTo(request);
        assertThat(request.response().contentLength() == 0).isTrue();
        assertThat(url).isEqualTo(request.getUrl().toString());
        assertThat(request.getMethod()).isEqualTo("GET");
    }

    /**
     * Make a GET request with an empty body response
     *
     * @throws Exception
     */
    @Test
    public void getUrlEmpty() throws Exception {
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = getRequest(new URL(url));
        assertThat(request.getConnection()).isNotNull();
        int code = request.response().code();
        assertThat(code).isEqualTo(200);
        assertThat(method.get()).isEqualTo("GET");
        assertThat(request.response().message()).isEqualTo("OK");
        assertThat(code).isEqualTo(HTTP_OK);
        assertThat(request.response().body()).isEqualTo("");
    }

    /**
     * Make a GET request with an empty body response
     *
     * @throws Exception
     */
    @Test
    public void getNoContent() throws Exception {
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                response.setStatus(HTTP_NO_CONTENT);
            }
        };
        KieServerHttpRequest request = getRequest(new URL(url));
        assertThat(request.getConnection()).isNotNull();
        int code = request.response().code();
        assertThat(code).isEqualTo(HTTP_NO_CONTENT);
        assertThat(method.get()).isEqualTo("GET");
        assertThat(request.response().message()).isEqualTo("No Content");
        assertThat(code).isEqualTo(HTTP_NO_CONTENT);
        assertThat(request.response().body()).isEqualTo("");
    }

    /**
     * Make a GET request with a URL that needs encoding
     *
     * @throws Exception
     */
    @Test
    public void getUrlEncodedWithSpace() throws Exception {
        String unencoded = "/a resource";
        final AtomicReference<String> path = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                path.set(request.getPathInfo());
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url + unencoded);
        assertThat(request.get().response().code()).isEqualTo(200);
        assertThat(path.get()).isEqualTo(unencoded);
    }

    /**
     * Make a GET request with a URL that needs encoding
     *
     * @throws Exception
     */
    @Test
    public void getUrlEncodedWithUnicode() throws Exception {
        String unencoded = "/\u00DF";
        final AtomicReference<String> path = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                path.set(request.getPathInfo());
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url + unencoded);
        assertThat(request.get().response().code()).isEqualTo(200);
        assertThat(path.get()).isEqualTo(unencoded);
    }

    /**
     * Make a GET request with a URL that needs encoding
     *
     * @throws Exception
     */
    @Test
    public void getUrlEncodedWithPercent() throws Exception {
        String unencoded = "/%";
        final AtomicReference<String> path = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                path.set(request.getPathInfo());
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url + unencoded);
        assertThat(request.get().response().code()).isEqualTo(200);
        assertThat(path.get()).isEqualTo(unencoded);
    }

    /**
     * Make a DELETE request with an empty body response
     *
     * @throws Exception
     */
    @Test
    public void deleteEmpty() throws Exception {
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = deleteRequest(new URL(url));
        assertThat(request.getConnection()).isNotNull();
        assertThat(request.delete().response().code()).isEqualTo(200);
        assertThat(method.get()).isEqualTo("DELETE");
        assertThat(request.response().body()).isEqualTo("");
        assertThat(request.getMethod()).isEqualTo("DELETE");
    }

    /**
     * Make a DELETE request with an empty body response
     *
     * @throws Exception
     */
    @Test
    public void deleteUrlEmpty() throws Exception {
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = deleteRequest(new URL(url));
        assertThat(request.getConnection()).isNotNull();
        assertThat(request.response().code()).isEqualTo(200);
        assertThat(method.get()).isEqualTo("DELETE");
        assertThat(request.response().body()).isEqualTo("");
    }

    /**
     * Make a POST request with an empty request body
     *
     * @throws Exception
     */
    @Test
    public void postEmpty() throws Exception {
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                response.setStatus(HTTP_CREATED);
            }
        };
        KieServerHttpRequest request = newRequest(url);
        assertThat(request.post().response().code()).isEqualTo(HTTP_CREATED);
        assertThat(method.get()).isEqualTo("POST");
    }

    /**
     * Make a POST request with an empty request body
     *
     * @throws Exception
     */
    @Test
    public void postUrlEmpty() throws Exception {
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                response.setStatus(HTTP_CREATED);
            }
        };
        KieServerHttpRequest request = getRequest(new URL(url));
        assertThat(request.post().response().code()).isEqualTo(HTTP_CREATED);
        assertThat(method.get()).isEqualTo("POST");
    }

    /**
     * Make a POST request with a non-empty request body
     *
     * @throws Exception
     */
    @Test
    public void postNonEmptyString() throws Exception {
        final AtomicReference<String> body = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                body.set(new String(read()));
                response.setStatus(HTTP_OK);
            }
        };
        int code = postRequest(new URL(url)).body("hello").response().code();
        assertThat(code).isEqualTo(HTTP_OK);
        assertThat(body.get()).isEqualTo("hello");
    }

    /**
     * Make a post with an explicit set of the content length
     *
     * @throws Exception
     */
    @Test
    public void postWithLength() throws Exception {
        final AtomicReference<String> body = new AtomicReference<String>();
        final AtomicReference<Integer> length = new AtomicReference<Integer>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                body.set(new String(read()));
                length.set(request.getContentLength());
                response.setStatus(HTTP_OK);
            }
        };
        String data = "hello";
        int sent = data.getBytes().length;
        int code = newRequest(new URL(url)).body(data).post().response().code();
        assertThat(code).isEqualTo(HTTP_OK);
        assertThat(length.get().intValue()).isEqualTo(sent);
        assertThat(body.get()).isEqualTo(data);
    }

    /**
     * Make a post of form data
     *
     * @throws Exception
     */
    @Test
    public void postForm() throws Exception {
        final AtomicReference<String> body = new AtomicReference<String>();
        final AtomicReference<String> contentType = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                body.set(new String(read()));
                contentType.set(request.getContentType());
                response.setStatus(HTTP_OK);
            }
        };
        Map<String, String> data = new LinkedHashMap<String, String>();
        data.put("name", "user");
        data.put("number", "100");
        int code = postRequest(new URL(url)).form(data).form("zip", "12345").response().code();
        assertThat(code).isEqualTo(HTTP_OK);
        assertThat(body.get()).isEqualTo("name=user&number=100&zip=12345");
        assertThat(contentType.get()).isEqualTo("application/x-www-form-urlencoded; charset=UTF-8");
    }

    /**
     * Make a post of form data
     *
     * @throws Exception
     */
    @Test
    public void postFormWithNoCharset() throws Exception {
        final AtomicReference<String> body = new AtomicReference<String>();
        final AtomicReference<String> contentType = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                body.set(new String(read()));
                contentType.set(request.getContentType());
                response.setStatus(HTTP_OK);
            }
        };
        Map<String, String> data = new LinkedHashMap<String, String>();
        data.put("name", "user");
        data.put("number", "100");
        int code = postRequest(new URL(url)).form(data, null).form("zip", "12345").response().code();
        assertThat(code).isEqualTo(HTTP_OK);
        assertThat(body.get()).isEqualTo("name=user&number=100&zip=12345");
        assertThat(contentType.get()).isEqualTo("application/x-www-form-urlencoded");
    }

    /**
     * Make a post with an empty form data map
     *
     * @throws Exception
     */
    @Test
    public void postEmptyForm() throws Exception {
        final AtomicReference<String> body = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                body.set(new String(read()));
                response.setStatus(HTTP_OK);
            }
        };
        int code = postRequest(new URL(url)).form(new HashMap<String, String>()).response().code();
        assertThat(code).isEqualTo(HTTP_OK);
        assertThat(body.get()).isEqualTo("");
    }

    /**
     * Make a GET request for a non-empty response body
     *
     * @throws Exception
     */
    @Test
    public void getNonEmptyString() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                write("hello");
            }
        };
        KieServerHttpRequest request = newRequest(url);
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(request.response().body()).isEqualTo("hello");
        assertThat(request.response().contentLength()).isEqualTo("hello".getBytes().length);
        assertThat(request.response().contentLength() == 0).isFalse();
    }

    /**
     * Make a GET request with a response that includes a charset parameter
     *
     * @throws Exception
     */
    @Test
    public void getWithResponseCharset() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                response.setContentType("text/html; charset=UTF-8");
            }
        };
        KieServerHttpRequest request = newRequest(url);
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(request.response().charset()).isEqualTo(CHARSET_UTF8);
    }

    /**
     * Make a GET request with a response that includes a charset parameter
     *
     * @throws Exception
     */
    @Test
    public void getWithResponseCharsetAsSecondParam() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                response.setContentType("text/html; param1=val1; charset=UTF-8");
            }
        };
        KieServerHttpRequest request = newRequest(url);
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(request.response().charset()).isEqualTo(CHARSET_UTF8);
    }

    /**
     * Make a GET request with basic authentication specified
     *
     * @throws Exception
     */
    @Test
    public void basicAuthentication() throws Exception {
        final AtomicReference<String> user = new AtomicReference<String>();
        final AtomicReference<String> password = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                String auth = request.getHeader("Authorization");
                auth = auth.substring(auth.indexOf(' ') + 1);
                auth = B64Code.decode(auth, CHARSET_UTF8);
                int colon = auth.indexOf(':');
                user.set(auth.substring(0, colon));
                password.set(auth.substring(colon + 1));
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).basicAuthorization("user", "p4ssw0rd");
        assertThat(request.post().response().code()).isEqualTo(200);
        assertThat(user.get()).isEqualTo("user");
        assertThat(password.get()).isEqualTo("p4ssw0rd");
    }

    /**
     * Make a GET request with basic proxy authentication specified
     *
     * @throws Exception
     */
    @Test
    @Ignore // add proxy functionality..?
    public void basicProxyAuthentication() throws Exception {
        final AtomicBoolean finalHostReached = new AtomicBoolean(false);
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                finalHostReached.set(true);
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).useProxy("localhost", proxyPort).proxyBasic("user", "p4ssw0rd");
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(proxyUser.get()).isEqualTo("user");
        assertThat(proxyPassword.get()).isEqualTo("p4ssw0rd");
        assertThat(finalHostReached.get()).isEqualTo(true);
        assertThat(proxyHitCount.get()).isEqualTo(1);
    }

    /**
     * Make a GET and get response body as byte array
     *
     * @throws Exception
     */
    @Test
    public void getBytes() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                write("hello");
            }
        };
        KieServerHttpRequest request = newRequest(url);
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(Arrays.equals("hello".getBytes(), request.response().bytes())).isTrue();
    }

    /**
     * Make a GET request that returns an error string
     *
     * @throws Exception
     */
    @Test
    public void getError() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                write("error");
            }
        };
        KieServerHttpRequest request = newRequest(url);
        assertThat(request.get().response().code()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
        assertThat(request.response().body()).isEqualTo("error");
    }

    /**
     * Make a GET request that returns an empty error string
     *
     * @throws Exception
     */
    @Test
    public void noError() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url);
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(request.response().body()).isEqualTo("");
    }

    /**
     * Verify 'Content-Encoding' header
     *
     * @throws Exception
     */
    @Test
    public void contentEncodingHeader() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                response.setHeader("Content-Encoding", "gzip");
            }
        };
        assertThat(newRequest(url).get().response().contentEncoding()).isEqualTo("gzip");
    }

    /**
     * Verify 'Content-Type' header
     *
     * @throws Exception
     */
    @Test
    public void contentTypeHeader() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                response.setHeader("Content-Type", "text/html");
            }
        };
        assertThat(newRequest(url).get().response().contentType()).isEqualTo("text/html");
    }

    /**
     * Verify 'Content-Type' header
     *
     * @throws Exception
     */
    @Test
    public void requestContentTypeTest() throws Exception {
        final AtomicReference<String> contentType = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                contentType.set(request.getContentType());
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).contentType("text/html", "UTF-8");
        assertThat(request.post().response().code()).isEqualTo(HTTP_OK);
        assertThat(contentType.get()).isEqualTo("text/html; charset=UTF-8");
    }

    /**
     * Verify 'Content-Type' header
     *
     * @throws Exception
     */
    @Test
    public void requestContentTypeNullCharsetTest() throws Exception {
        final AtomicReference<String> contentType = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                contentType.set(request.getContentType());
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).contentType("text/html", null);
        assertThat(request.post().response().code()).isEqualTo(HTTP_OK);
        assertThat(contentType.get()).isEqualTo("text/html");
    }

    /**
     * Verify 'Content-Type' header
     *
     * @throws Exception
     */
    @Test
    public void requestContentTypeEmptyCharsetTest() throws Exception {
        final AtomicReference<String> contentType = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                contentType.set(request.getContentType());
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).contentType("text/html", "");
        assertThat(request.post().response().code()).isEqualTo(HTTP_OK);
        assertThat(contentType.get()).isEqualTo("text/html");
    }

    /**
     * Verify setting headers
     *
     * @throws Exception
     */
    @Test
    public void headers() throws Exception {
        final AtomicReference<String> h1 = new AtomicReference<String>();
        final AtomicReference<String> h2 = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                h1.set(request.getHeader("h1"));
                h2.set(request.getHeader("h2"));
            }
        };
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("h1", "v1");
        headers.put("h2", "v2");
        KieServerHttpRequest request = newRequest(url).headers(headers);
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(h1.get()).isEqualTo("v1");
        assertThat(h2.get()).isEqualTo("v2");
    }

    /**
     * Verify setting headers
     *
     * @throws Exception
     */
    @Test
    public void emptyHeaders() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).headers(Collections.<String, String> emptyMap());
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
    }

    /**
     * Verify getting all headers
     *
     * @throws Exception
     */
    @Test
    public void getAllHeaders() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                response.setHeader("a", "a");
                response.setHeader("b", "b");
                response.addHeader("a", "another");
            }
        };
        Map<String, List<String>> headers = newRequest(url).get().response().headers();
        assertThat(6).isEqualTo(headers.size());
        assertThat(2).isEqualTo(headers.get("a").size());
        assertThat(headers.get("b").get(0).equals("b")).isTrue();
    }

    /**
     * Verify setting number header
     *
     * @throws Exception
     */
    @Test
    public void numberHeader() throws Exception {
        final AtomicReference<String> h1 = new AtomicReference<String>();
        final AtomicReference<String> h2 = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                h1.set(request.getHeader("h1"));
                h2.set(request.getHeader("h2"));
            }
        };
        KieServerHttpRequest request = newRequest(url).header("h1", 5).header("h2", (Number) null);
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(h1.get()).isEqualTo("5");
        assertThat(h2.get()).isEqualTo(null);
    }

    /**
     * Verify 'User-Agent' request header
     *
     * @throws Exception
     */
    @Test
    public void userAgentHeader() throws Exception {
        final AtomicReference<String> header = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                header.set(request.getHeader("User-Agent"));
            }
        };
        KieServerHttpRequest request = newRequest(url).header(USER_AGENT, "browser 1.0");
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(header.get()).isEqualTo("browser 1.0");
    }

    /**
     * Verify 'Accept' request header
     *
     * @throws Exception
     */
    @Test
    public void acceptHeader() throws Exception {
        final AtomicReference<String> header = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                header.set(request.getHeader("Accept"));
            }
        };
        KieServerHttpRequest request = newRequest(url).accept("application/json");
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(header.get()).isEqualTo("application/json");
    }

    /**
     * Verify 'Accept' request header when calling {@link KieServerHttpRequest#acceptJson()}
     *
     * @throws Exception
     */
    @Test
    public void acceptJson() throws Exception {
        final AtomicReference<String> header = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                header.set(request.getHeader("Accept"));
            }
        };
        KieServerHttpRequest request = newRequest(url).accept(APPLICATION_JSON);
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(header.get()).isEqualTo("application/json");
    }

    /**
     * Verify 'If-None-Match' request header
     *
     * @throws Exception
     */
    @Test
    public void ifNoneMatchHeader() throws Exception {
        final AtomicReference<String> header = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                header.set(request.getHeader("If-None-Match"));
            }
        };
        KieServerHttpRequest request = newRequest(url).header(IF_NONE_MATCH, "eid");
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(header.get()).isEqualTo("eid");
    }

    /**
     * Verify 'Accept-Charset' request header
     *
     * @throws Exception
     */
    @Test
    public void acceptCharsetHeader() throws Exception {
        final AtomicReference<String> header = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                header.set(request.getHeader("Accept-Charset"));
            }
        };
        KieServerHttpRequest request = newRequest(url).acceptCharset(CHARSET_UTF8);
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(header.get()).isEqualTo(CHARSET_UTF8);
    }

    /**
     * Verify 'Accept-Encoding' request header
     *
     * @throws Exception
     */
    @Test
    public void acceptEncodingHeader() throws Exception {
        final AtomicReference<String> header = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                header.set(request.getHeader("Accept-Encoding"));
            }
        };
        KieServerHttpRequest request = newRequest(url).acceptEncoding("compress");
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(header.get()).isEqualTo("compress");
    }

    /**
     * Verify certificate and host helpers on HTTPS connection
     *
     * @throws Exception
     */
    @Test
    public void httpsTrust() throws Exception {
        assertThat(getRequest("https://localhost").trustAllCerts().trustAllHosts()).isNotNull();
    }

    /**
     * Verify certificate and host helpers ignore non-HTTPS connection
     *
     * @throws Exception
     */
    @Test
    public void httpTrust() throws Exception {
        assertThat(getRequest("http://localhost").trustAllCerts().trustAllHosts()).isNotNull();
    }

    /**
     * Verify hostname verifier is set and accepts all
     */
    @Test
    public void verifierAccepts() {
        KieServerHttpRequest request = getRequest("https://localhost");
        HttpsURLConnection connection = (HttpsURLConnection) request.getConnection();
        request.trustAllHosts();
        assertThat(connection.getHostnameVerifier()).isNotNull();
        assertThat(connection.getHostnameVerifier().verify(null, null)).isTrue();
    }

    /**
     * Verify single hostname verifier is created across all calls
     */
    @Test
    public void singleVerifier() {
        KieServerHttpRequest request1 = getRequest("https://localhost").trustAllHosts();
        KieServerHttpRequest request2 = getRequest("https://localhost").trustAllHosts();
        assertThat(((HttpsURLConnection) request1.getConnection()).getHostnameVerifier()).isNotNull();
        assertThat(((HttpsURLConnection) request2.getConnection()).getHostnameVerifier()).isNotNull();
        assertEquals(((HttpsURLConnection) request1.getConnection()).getHostnameVerifier(),
                ((HttpsURLConnection) request2.getConnection()).getHostnameVerifier());
    }

    /**
     * Verify single SSL socket factory is created across all calls
     */
    @Test
    public void singleSslSocketFactory() {
        KieServerHttpRequest request1 = getRequest("https://localhost").trustAllCerts();
        KieServerHttpRequest request2 = getRequest("https://localhost").trustAllCerts();
        assertThat(((HttpsURLConnection) request1.getConnection()).getSSLSocketFactory()).isNotNull();
        assertThat(((HttpsURLConnection) request2.getConnection()).getSSLSocketFactory()).isNotNull();
        assertEquals(((HttpsURLConnection) request1.getConnection()).getSSLSocketFactory(),
                ((HttpsURLConnection) request2.getConnection()).getSSLSocketFactory());
    }

    /**
     * Make a GET request that should be compressed
     *
     * @throws Exception
     */
    @Test
    public void getGzipped() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                if( !"gzip".equals(request.getHeader("Accept-Encoding")) )
                    return;

                response.setHeader("Content-Encoding", "gzip");
                GZIPOutputStream output;
                try {
                    output = new GZIPOutputStream(response.getOutputStream());
                } catch( IOException e ) {
                    throw new RuntimeException(e);
                }
                try {
                    output.write("hello compressed".getBytes(CHARSET_UTF8));
                } catch( IOException e ) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        output.close();
                    } catch( IOException ignored ) {
                        // Ignored
                    }
                }
            }
        };
        KieServerHttpRequest request = getRequest(url).acceptEncoding("gzip").setUncompress(true);
        assertThat(request.response().code()).isEqualTo(HTTP_OK);
        assertThat(request.response().body()).isEqualTo("hello compressed");
    }

    /**
     * Make a GET request that should be compressed but isn't
     *
     * @throws Exception
     */
    @Test
    public void getNonGzippedWithUncompressEnabled() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                if( !"gzip".equals(request.getHeader("Accept-Encoding")) )
                    return;

                write("hello not compressed");
            }
        };
        KieServerHttpRequest request = getRequest(url).acceptEncoding("gzip").setUncompress(true);
        assertThat(request.response().code()).isEqualTo(HTTP_OK);
        assertThat(request.response().body()).isEqualTo("hello not compressed");
    }

    /**
     * Get header with multiple response values
     *
     * @throws Exception
     */
    @Test
    public void getHeaders() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                response.addHeader("a", "1");
                response.addHeader("a", "2");
            }
        };
        KieServerHttpRequest request = getRequest(url);
        assertThat(request.response().code()).isEqualTo(HTTP_OK);
        String[] values = request.response().headers("a");
        assertThat(values).isNotNull();
        assertThat(values.length).isEqualTo(2);
        assertThat(Arrays.asList(values).contains("1")).isTrue();
        assertThat(Arrays.asList(values).contains("2")).isTrue();
    }

    /**
     * Get header values when not set in response
     *
     * @throws Exception
     */
    @Test
    public void getEmptyHeaders() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = getRequest(url);
        assertThat(request.response().code()).isEqualTo(HTTP_OK);
        String[] values = request.response().headers("a");
        assertThat(values).isNotNull();
        assertThat(values.length).isEqualTo(0);
    }

    /**
     * Get header parameter value
     *
     * @throws Exception
     */
    @Test
    public void getSingleParameterTest() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                response.setHeader("a", "b;c=d");
            }
        };
        KieServerHttpRequest request = getRequest(url);
        assertThat(request.response().code()).isEqualTo(HTTP_OK);
        assertThat("c")).as("d").isEqualTo(request.response().headerParameter("a");
    }

    /**
     * Get header parameter value
     *
     * @throws Exception
     */
    @Test
    public void getMultipleParametersTest() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                response.setHeader("a", "b;c=d;e=f");
            }
        };
        KieServerHttpRequest request = getRequest(url);
        assertThat(request.response().code()).isEqualTo(HTTP_OK);
        assertThat("c")).as("d").isEqualTo(request.response().headerParameter("a");
        assertThat("e")).as("f").isEqualTo(request.response().headerParameter("a");
    }

    /**
     * Get header parameter value
     *
     * @throws Exception
     */
    @Test
    public void getSingleParameterQuotedTest() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                response.setHeader("a", "b;c=\"d\"");
            }
        };
        KieServerHttpRequest request = getRequest(url);
        assertThat(request.response().code()).isEqualTo(HTTP_OK);
        assertThat("c")).as("d").isEqualTo(request.response().headerParameter("a");
    }

    /**
     * Get header parameter value
     *
     * @throws Exception
     */
    @Test
    public void getMultipleParametersQuotedTest() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                response.setHeader("a", "b;c=\"d\";e=\"f\"");
            }
        };
        KieServerHttpRequest request = getRequest(url);
        assertThat(request.response().code()).isEqualTo(HTTP_OK);
        assertThat("c")).as("d").isEqualTo(request.response().headerParameter("a");
        assertThat("e")).as("f").isEqualTo(request.response().headerParameter("a");
    }

    /**
     * Get header parameter value
     *
     * @throws Exception
     */
    @Test
    public void getMissingParameter() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                response.setHeader("a", "b;c=d");
            }
        };
        KieServerHttpRequest request = newRequest(url);
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(request.response().headerParameter("a", "e")).isNull();
    }

    /**
     * Get header parameter value
     *
     * @throws Exception
     */
    @Test
    public void getParameterFromMissingHeaderTest() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                response.setHeader("a", "b;c=d");
            }
        };
        KieServerHttpRequest request = newRequest(url);
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(request.response().headerParameter("b", "c")).isNull();
        assertThat(request.response().headerParameters("b").isEmpty()).isTrue();
    }

    /**
     * Get header parameter value
     *
     * @throws Exception
     */
    @Test
    public void getEmptyParameterTest() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                response.setHeader("a", "b;c=");
            }
        };
        KieServerHttpRequest request = newRequest(url);
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(request.response().headerParameter("a", "c")).isNull();
        assertThat(request.response().headerParameters("a").isEmpty()).isTrue();
    }

    /**
     * Get header parameter value
     *
     * @throws Exception
     */
    @Test
    public void getEmptyParameters() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                response.setHeader("a", "b;");
            }
        };
        KieServerHttpRequest request = newRequest(url);
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(request.response().headerParameter("a", "c")).isNull();
        assertThat(request.response().headerParameters("a").isEmpty()).isTrue();
    }

    /**
     * Get header parameter values
     *
     * @throws Exception
     */
    @Test
    public void getParameters() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                response.setHeader("a", "value;b=c;d=e");
            }
        };
        KieServerHttpRequest request = newRequest(url);
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        Map<String, String> params = request.response().headerParameters("a");
        assertThat(params).isNotNull();
        assertThat(params).hasSize(2);
        assertThat(params.get("b")).isEqualTo("c");
        assertThat(params.get("d")).isEqualTo("e");
    }

    /**
     * Get header parameter values
     *
     * @throws Exception
     */
    @Test
    public void getQuotedParameters() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                response.setHeader("a", "value;b=\"c\";d=\"e\"");
            }
        };
        KieServerHttpRequest request = newRequest(url);
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        Map<String, String> params = request.response().headerParameters("a");
        assertThat(params).isNotNull();
        assertThat(params).hasSize(2);
        assertThat(params.get("b")).isEqualTo("c");
        assertThat(params.get("d")).isEqualTo("e");
    }

    /**
     * Get header parameter values
     *
     * @throws Exception
     */
    @Test
    public void getMixQuotedParameters() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_OK);
                response.setHeader("a", "value; b=c; d=\"e\"");
            }
        };
        KieServerHttpRequest request = newRequest(url);
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        Map<String, String> params = request.response().headerParameters("a");
        assertThat(params).isNotNull();
        assertThat(params).hasSize(2);
        assertThat(params.get("b")).isEqualTo("c");
        assertThat(params.get("d")).isEqualTo("e");
    }

    /**
     * Verify sending form data as a sequence of {@link Entry} objects
     *
     * @throws Exception
     */
    @Test
    public void postFormAsEntries() throws Exception {
        final AtomicReference<String> body = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                body.set(new String(read()));
                response.setStatus(HTTP_OK);
            }
        };
        Map<String, String> data = new LinkedHashMap<String, String>();
        data.put("name", "user");
        data.put("number", "100");
        KieServerHttpRequest request = newRequest(url).form(data);
        int code = request.post().response().code();
        assertThat(code).isEqualTo(HTTP_OK);
        assertThat(body.get()).isEqualTo("name=user&number=100");
    }

    /**
     * Verify sending form data where entry value is null
     *
     * @throws Exception
     */
    @Test
    public void postFormEntryWithNullValue() throws Exception {
        final AtomicReference<String> body = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                body.set(new String(read()));
                response.setStatus(HTTP_OK);
            }
        };
        Map<String, String> data = new LinkedHashMap<String, String>();
        data.put("name", null);
        KieServerHttpRequest request = newRequest(url).form(data);
        int code = request.post().response().code();
        assertThat(code).isEqualTo(HTTP_OK);
        assertThat(body.get()).isEqualTo("name=");
    }

    /**
     * Verify POST with query parameters
     *
     * @throws Exception
     */
    @Test
    public void postWithMappedQueryParams() throws Exception {
        Map<String, String> inputParams = new HashMap<String, String>();
        inputParams.put("name", "user");
        inputParams.put("number", "100");
        final Map<String, String> outputParams = new HashMap<String, String>();
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                outputParams.put("name", request.getParameter("name"));
                outputParams.put("number", request.getParameter("number"));
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).query(inputParams);
        assertThat(request.post().response().code()).isEqualTo(HTTP_OK);
        assertThat(method.get()).isEqualTo("POST");
        assertThat(outputParams.get("name")).isEqualTo("user");
        assertThat(outputParams.get("number")).isEqualTo("100");
    }

    /**
     * Verify POST with query parameters
     *
     * @throws Exception
     */
    @Test
    public void postWithVaragsQueryParams() throws Exception {
        final Map<String, String> outputParams = new HashMap<String, String>();
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                outputParams.put("name", request.getParameter("name"));
                outputParams.put("number", request.getParameter("number"));
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).query("name", "user").query("number", "100");
        assertThat(request.post().response().code()).isEqualTo(HTTP_OK);
        assertThat(method.get()).isEqualTo("POST");
        assertThat(outputParams.get("name")).isEqualTo("user");
        assertThat(outputParams.get("number")).isEqualTo("100");
    }

    /**
     * Verify POST with escaped query parameters
     *
     * @throws Exception
     */
    @Test
    public void postWithEscapedMappedQueryParams() throws Exception {
        Map<String, String> inputParams = new HashMap<String, String>();
        inputParams.put("name", "us er");
        inputParams.put("number", "100");
        final Map<String, String> outputParams = new HashMap<String, String>();
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                outputParams.put("name", request.getParameter("name"));
                outputParams.put("number", request.getParameter("number"));
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).query(inputParams);
        assertThat(request.post().response().code()).isEqualTo(HTTP_OK);
        assertThat(method.get()).isEqualTo("POST");
        assertThat(outputParams.get("name")).isEqualTo("us er");
        assertThat(outputParams.get("number")).isEqualTo("100");
    }

    /**
     * Verify POST with escaped query parameters
     *
     * @throws Exception
     */
    @Test
    public void postWithEscapedVarargsQueryParams() throws Exception {
        final Map<String, String> outputParams = new HashMap<String, String>();
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                outputParams.put("name", request.getParameter("name"));
                outputParams.put("number", request.getParameter("number"));
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).query("name", "us er").query("number", "100");
        assertThat(request.post().response().code()).isEqualTo(HTTP_OK);
        assertThat(method.get()).isEqualTo("POST");
        assertThat(outputParams.get("name")).isEqualTo("us er");
        assertThat(outputParams.get("number")).isEqualTo("100");
    }

    /**
     * Verify POST with numeric query parameters
     *
     * @throws Exception
     */
    @Test
    public void postWithNumericQueryParams() throws Exception {
        Map<Object, Object> inputParams = new HashMap<Object, Object>();
        inputParams.put(1, 2);
        inputParams.put(3, 4);
        final Map<String, String> outputParams = new HashMap<String, String>();
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                outputParams.put("1", request.getParameter("1"));
                outputParams.put("3", request.getParameter("3"));
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).query(inputParams);
        assertThat(request.post().response().code()).isEqualTo(HTTP_OK);
        assertThat(method.get()).isEqualTo("POST");
        assertThat(outputParams.get("1")).isEqualTo("2");
        assertThat(outputParams.get("3")).isEqualTo("4");
    }

    /**
     * Verify GET with query parameters
     *
     * @throws Exception
     */
    @Test
    public void getWithMappedQueryParams() throws Exception {
        Map<String, String> inputParams = new HashMap<String, String>();
        inputParams.put("name", "user");
        inputParams.put("number", "100");
        final Map<String, String> outputParams = new HashMap<String, String>();
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                outputParams.put("name", request.getParameter("name"));
                outputParams.put("number", request.getParameter("number"));
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).query(inputParams);
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(method.get()).isEqualTo("GET");
        assertThat(outputParams.get("name")).isEqualTo("user");
        assertThat(outputParams.get("number")).isEqualTo("100");
    }

    /**
     * Verify GET with query parameters
     *
     * @throws Exception
     */
    @Test
    public void getWithVarargsQueryParams() throws Exception {
        final Map<String, String> outputParams = new HashMap<String, String>();
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                outputParams.put("name", request.getParameter("name"));
                outputParams.put("number", request.getParameter("number"));
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).query("name", "user").query("number", "100");
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(method.get()).isEqualTo("GET");
        assertThat(outputParams.get("name")).isEqualTo("user");
        assertThat(outputParams.get("number")).isEqualTo("100");
    }

    /**
     * Verify GET with escaped query parameters
     *
     * @throws Exception
     */
    @Test
    public void getWithEscapedMappedQueryParams() throws Exception {
        Map<String, String> inputParams = new HashMap<String, String>();
        inputParams.put("name", "us er");
        inputParams.put("number", "100");
        final Map<String, String> outputParams = new HashMap<String, String>();
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                outputParams.put("name", request.getParameter("name"));
                outputParams.put("number", request.getParameter("number"));
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).query(inputParams);
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(method.get()).isEqualTo("GET");
        assertThat(outputParams.get("name")).isEqualTo("us er");
        assertThat(outputParams.get("number")).isEqualTo("100");
    }

    /**
     * Verify GET with escaped query parameters
     *
     * @throws Exception
     */
    @Test
    public void getWithEscapedVarargsQueryParams() throws Exception {
        final Map<String, String> outputParams = new HashMap<String, String>();
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                outputParams.put("name", request.getParameter("name"));
                outputParams.put("number", request.getParameter("number"));
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).query("name", "us er").query("number", "100");
        assertThat(request.get().response().code()).isEqualTo(HTTP_OK);
        assertThat(method.get()).isEqualTo("GET");
        assertThat(outputParams.get("name")).isEqualTo("us er");
        assertThat(outputParams.get("number")).isEqualTo("100");
    }

    /**
     * Verify DELETE with query parameters
     *
     * @throws Exception
     */
    @Test
    public void deleteWithMappedQueryParams() throws Exception {
        Map<String, String> inputParams = new HashMap<String, String>();
        inputParams.put("name", "user");
        inputParams.put("number", "100");
        final Map<String, String> outputParams = new HashMap<String, String>();
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                outputParams.put("name", request.getParameter("name"));
                outputParams.put("number", request.getParameter("number"));
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).query(inputParams);
        assertThat(request.delete().response().code()).isEqualTo(HTTP_OK);
        assertThat(method.get()).isEqualTo("DELETE");
        assertThat(outputParams.get("name")).isEqualTo("user");
        assertThat(outputParams.get("number")).isEqualTo("100");
    }

    /**
     * Verify DELETE with query parameters
     *
     * @throws Exception
     */
    @Test
    public void deleteWithVarargsQueryParams() throws Exception {
        final Map<String, String> outputParams = new HashMap<String, String>();
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                outputParams.put("name", request.getParameter("name"));
                outputParams.put("number", request.getParameter("number"));
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).query("name", "user").query("number", "100");
        assertThat(request.delete().response().code()).isEqualTo(HTTP_OK);
        assertThat(method.get()).isEqualTo("DELETE");
        assertThat(outputParams.get("name")).isEqualTo("user");
        assertThat(outputParams.get("number")).isEqualTo("100");
    }

    /**
     * Verify DELETE with escaped query parameters
     *
     * @throws Exception
     */
    @Test
    public void deleteWithEscapedMappedQueryParams() throws Exception {
        Map<String, String> inputParams = new HashMap<String, String>();
        inputParams.put("name", "us er");
        inputParams.put("number", "100");
        final Map<String, String> outputParams = new HashMap<String, String>();
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                outputParams.put("name", request.getParameter("name"));
                outputParams.put("number", request.getParameter("number"));
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).query(inputParams);
        assertThat(request.delete().response().code()).isEqualTo(HTTP_OK);
        assertThat(method.get()).isEqualTo("DELETE");
        assertThat(outputParams.get("name")).isEqualTo("us er");
        assertThat(outputParams.get("number")).isEqualTo("100");
    }

    /**
     * Verify DELETE with escaped query parameters
     *
     * @throws Exception
     */
    @Test
    public void deleteWithEscapedVarargsQueryParams() throws Exception {
        final Map<String, String> outputParams = new HashMap<String, String>();
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                outputParams.put("name", request.getParameter("name"));
                outputParams.put("number", request.getParameter("number"));
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).query("name", "us er").query("number", "100");
        assertThat(request.delete().response().code()).isEqualTo(HTTP_OK);
        assertThat(method.get()).isEqualTo("DELETE");
        assertThat(outputParams.get("name")).isEqualTo("us er");
        assertThat(outputParams.get("number")).isEqualTo("100");
    }

    /**
     * Verify POST with query parameters
     *
     * @throws Exception
     */
    @Test
    public void postWithVarargsQueryParams() throws Exception {
        final Map<String, String> outputParams = new HashMap<String, String>();
        final AtomicReference<String> method = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                method.set(request.getMethod());
                outputParams.put("name", request.getParameter("name"));
                outputParams.put("number", request.getParameter("number"));
                response.setStatus(HTTP_OK);
            }
        };
        KieServerHttpRequest request = newRequest(url).query("name", "user").query("number", "100");
        assertThat(request.post().response().code()).isEqualTo(HTTP_OK);
        assertThat(method.get()).isEqualTo("POST");
        assertThat(outputParams.get("name")).isEqualTo("user");
        assertThat(outputParams.get("number")).isEqualTo("100");
    }

    /**
     * Append with base URL with no path
     *
     * @throws Exception
     */
    @Test
    public void appendMappedQueryParamsWithNoPath() throws Exception {
        assertThat(Collections.singletonMap("a").as("http://test.com/?a=b").isCloseTo(appendQueryParameters("http://test.com", within("b"))));
    }

    /**
     * Append with base URL with no path
     *
     * @throws Exception
     */
    @Test
    public void appendVarargsQueryParmasWithNoPath() throws Exception {
        assertThat("a").as("http://test.com/?a=b").isCloseTo(appendQueryParameters("http://test.com", within("b")));
    }

    /**
     * Append with base URL with path
     *
     * @throws Exception
     */
    @Test
    public void appendMappedQueryParamsWithPath() throws Exception {
        assertEquals("http://test.com/segment1?a=b",
                appendQueryParameters("http://test.com/segment1", Collections.singletonMap("a", "b")));
        assertThat(Collections.singletonMap("a").as("http://test.com/?a=b").isCloseTo(appendQueryParameters("http://test.com/", within("b"))));
    }

    /**
     * Append with base URL with path
     *
     * @throws Exception
     */
    @Test
    public void appendVarargsQueryParamsWithPath() throws Exception {
        assertThat("a").as("http://test.com/segment1?a=b").isCloseTo(appendQueryParameters("http://test.com/segment1", within("b")));
        assertThat("a").as("http://test.com/?a=b").isCloseTo(appendQueryParameters("http://test.com/", within("b")));
    }

    /**
     * Append multiple params
     *
     * @throws Exception
     */
    @Test
    public void appendMultipleMappedQueryParams() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("a", "b");
        params.put("c", "d");
        assertThat(params)).as("http://test.com/1?a=b&c=d").isEqualTo(appendQueryParameters("http://test.com/1");
    }

    /**
     * Append multiple params
     *
     * @throws Exception
     */
    @Test
    public void appendMultipleVarargsQueryParams() throws Exception {
        assertThat("c").as("http://test.com/1?a=b&c=d", appendQueryParameters("http://test.com/1", "a").isCloseTo("b", within("d")));
    }

    /**
     * Append null params
     *
     * @throws Exception
     */
    @Test
    public void appendNullMappedQueryParams() throws Exception {
        assertThat((Map<?).as("http://test.com/1").isCloseTo(appendQueryParameters("http://test.com/1", within(?>) null)));
    }

    /**
     * Append null params
     *
     * @throws Exception
     */
    @Test
    public void appendNullVaragsQueryParams() throws Exception {
        assertThat((Object[]) null)).as("http://test.com/1").isEqualTo(appendQueryParameters("http://test.com/1");
    }

    /**
     * Append empty params
     *
     * @throws Exception
     */
    @Test
    public void appendEmptyMappedQueryParams() throws Exception {
        assertThat(Collections.<String).as("http://test.com/1").isCloseTo(appendQueryParameters("http://test.com/1", within(String> emptyMap())));
    }

    /**
     * Append empty params
     *
     * @throws Exception
     */
    @Test
    public void appendEmptyVarargsQueryParams() throws Exception {
        assertThat(new Object[0])).as("http://test.com/1").isEqualTo(appendQueryParameters("http://test.com/1");
    }

    /**
     * Append params with null values
     *
     * @throws Exception
     */
    @Test
    public void appendWithNullMappedQueryParamValues() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("a", null);
        params.put("b", null);
        assertThat(params)).as("http://test.com/1?a=&b=").isEqualTo(appendQueryParameters("http://test.com/1");
    }

    /**
     * Append params with null values
     *
     * @throws Exception
     */
    @Test
    public void appendWithNullVaragsQueryParamValues() throws Exception {
        assertThat("b").as("http://test.com/1?a=&b=", appendQueryParameters("http://test.com/1", "a").isCloseTo(null, within(null)));
    }

    /**
     * Try to append with wrong number of arguments
     */
    @Test(expected = IllegalArgumentException.class)
    public void appendOddNumberOfParams() {
        appendQueryParameters("http://test.com", "1");
    }

    /**
     * Append with base URL already containing a '?'
     */
    @Test
    public void appendMappedQueryParamsWithExistingQueryStart() {
        assertThat(Collections.singletonMap("a").as("http://test.com/1?a=b").isCloseTo(appendQueryParameters("http://test.com/1?", within("b"))));
    }

    /**
     * Append with base URL already containing a '?'
     */
    @Test
    public void appendVarargsQueryParamsWithExistingQueryStart() {
        assertThat("a").as("http://test.com/1?a=b").isCloseTo(appendQueryParameters("http://test.com/1?", within("b")));
    }

    /**
     * Append with base URL already containing a '?'
     */
    @Test
    public void appendMappedQueryParamsWithExistingParams() {
        assertEquals("http://test.com/1?a=b&c=d",
                appendQueryParameters("http://test.com/1?a=b", Collections.singletonMap("c", "d")));
        assertEquals("http://test.com/1?a=b&c=d",
                appendQueryParameters("http://test.com/1?a=b&", Collections.singletonMap("c", "d")));

    }

    /**
     * Append with base URL already containing a '?'
     */
    @Test
    public void appendWithVarargsQueryParamsWithExistingParams() {
        assertThat("c").as("http://test.com/1?a=b&c=d").isCloseTo(appendQueryParameters("http://test.com/1?a=b", within("d")));
        assertThat("c").as("http://test.com/1?a=b&c=d").isCloseTo(appendQueryParameters("http://test.com/1?a=b&", within("d")));
    }

    /**
     * Get a 400
     *
     * @throws Exception
     */
    @Test
    public void badRequestCode() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_BAD_REQUEST);
            }
        };
        KieServerHttpRequest request = newRequest(url);
        assertThat(request).isNotNull();
        assertThat(request.get().response().code()).isEqualTo(HTTP_BAD_REQUEST);
    }

    /**
     * Verify data is sent when receiving response without first calling {@link KieServerHttpRequest#response().Code()}
     *
     * @throws Exception
     */
    @Test
    public void sendReceiveWithoutcode() throws Exception {
        final AtomicReference<String> body = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                body.set(new String(read()));
                try {
                    response.getWriter().write("world");
                } catch( IOException ignored ) {
                    // Ignored
                }
                response.setStatus(HTTP_OK);
            }
        };

        KieServerHttpRequest request = newRequest(new URL(url)).ignoreCloseExceptions(false);
        assertThat(request.body("hello").post().response().body()).isEqualTo("world");
        assertThat(body.get()).isEqualTo("hello");
    }

    /**
     * Verify data is send when receiving response headers without first calling {@link KieServerHttpRequest#response().code()}
     *
     * @throws Exception
     */
    @Test
    public void sendHeadersWithoutCode() throws Exception {
        final AtomicReference<String> body = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                body.set(new String(read()));
                response.setHeader("h1", "v1");
                response.setHeader("h2", "v2");
                response.setStatus(HTTP_OK);
            }
        };

        KieServerHttpRequest request = newRequest(new URL(url)).ignoreCloseExceptions(false);
        Map<String, List<String>> headers = request.body("hello").post().response().headers();
        assertThat(headers.get("h1").get(0)).isEqualTo("v1");
        assertThat(headers.get("h2").get(0)).isEqualTo("v2");
        assertThat(body.get()).isEqualTo("hello");
    }

    /**
     * Verify data is send when receiving response integer header without first
     * calling {@link KieServerHttpRequest#response().Code()}
     *
     * @throws Exception
     */
    @Test
    public void sendIntHeaderWithoutCode() throws Exception {
        final AtomicReference<String> body = new AtomicReference<String>();
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                body.set(new String(read()));
                response.setIntHeader("Width", 9876);
                response.setStatus(HTTP_OK);
            }
        };

        KieServerHttpRequest request = newRequest(new URL(url)).ignoreCloseExceptions(false);
        assertThat(request.body("hello").post().response().intHeader("Width")).isEqualTo(9876);
        assertThat(body.get()).isEqualTo("hello");
    }

    /**
     * Verify reading response body for empty 200
     *
     * @throws Exception
     */
    @Test
    public void streamOfEmptyOkResponse() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(200);
            }
        };
        assertThat(newRequest(url).get().response().body()).isEqualTo("");
    }

    /**
     * Verify reading response body for empty 400
     *
     * @throws Exception
     */
    @Test
    public void bodyOfEmptyErrorResponse() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_BAD_REQUEST);
            }
        };
        assertThat(newRequest(url).get().response().body()).isEqualTo("");
    }

    /**
     * Verify reading response body for non-empty 400
     *
     * @throws Exception
     */
    @Test
    public void bodyOfNonEmptyErrorResponse() throws Exception {
        handler = new RequestHandler() {

            @Override
            public void handle( Request request, HttpServletResponse response ) {
                response.setStatus(HTTP_BAD_REQUEST);
                try {
                    response.getWriter().write("error");
                } catch( IOException ignored ) {
                    // Ignored
                }
            }
        };
        assertThat(newRequest(url).get().response().body()).isEqualTo("error");
    }

}
