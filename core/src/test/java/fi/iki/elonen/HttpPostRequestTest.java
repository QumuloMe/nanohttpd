package fi.iki.elonen;

/*
 * #%L
 * NanoHttpd-Core
 * %%
 * Copyright (C) 2012 - 2015 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class HttpPostRequestTest extends HttpServerTest {
    class HttpPostRequestBuilder {
        private StringBuilder headers = new StringBuilder();
        private StringBuilder body = new StringBuilder();
        private String divider;
        private boolean isMultipartFormData;

        public HttpPostRequestBuilder() {
            isMultipartFormData = false;
            divider = UUID.randomUUID().toString();
            headers.append("POST " + HttpServerTest.URI + " HTTP/1.1\n");
        }

        private int digitsInNumber(int number) {
            return 1 + (int)Math.log10(number);
        }

        private void setMultipartFormData() {
            if (isMultipartFormData) {
                return;
            }

            isMultipartFormData = true;
            headers.append("Content-Type: " + "multipart/form-data, boundary=" + divider + "\r\n");
        }

        /**
         * @return The finished request body
         */
        @Override
        public String toString() {
            StringBuilder request = new StringBuilder(headers);
            request.append("Content-Length: ");

            // Calculate what the Content-Length value ought to be; it's the header + body + extra finishing bits
            int contentLength = request.length() + body.length() + "\r\n\r\n".length();
            if (isMultipartFormData) {
                contentLength += "\r\n----".length() + divider.length();
            }

            contentLength += digitsInNumber(contentLength + digitsInNumber(contentLength));
            request.append(contentLength);
            request.append("\r\n\r\n");
            request.append(body);

            if (isMultipartFormData) {
                request.append("--" + divider + "--\r\n");
            }

            return request.toString();
        }

        private void addDivider() {
            setMultipartFormData();
            body.append("--" + divider + "\r\n");
        }

        /**
         * Add a file to the multipart form data.
         *
         * @param fileName
         *            Name of file to be uploaded
         * @param fileContent
         *            Content of file to be uploaded
         */
        public HttpPostRequestBuilder addFile(String parameterName, String fileName, String fileContent) {
            addDivider();
            body.append("Content-Disposition: form-data; name=\"" + parameterName + "\"; filename=\"" + fileName + "\"\r\n");
            body.append("Content-Type: image/jpeg\r\n");
            body.append("\r\n");
            body.append(fileContent);
            body.append("\r\n");

            return this;
        }

        /**
         * Add a parameter to the multipart form data.
         * @param parameterName Name of the parameter to be added
         * @param content Content of the parameter to be added
         */
        public HttpPostRequestBuilder addParameter(String parameterName, String content) {
            addDivider();
            body.append("Content-Disposition: form-data; name=\"" + parameterName + "\"\r\n");
            body.append("\r\n");
            body.append(content);
            body.append("\r\n");

            return this;
        }

        /**
         * Adds some raw data to the POST request.
         * @param content The content to add.
         */
        public HttpPostRequestBuilder addRawData(String content) {
            body.append(content);
            return this;
        }
    }

    public static final String FIELD = "caption";

    public static final String VALUE = "Summer vacation";

    public static final String FIELD2 = "location";

    public static final String VALUE2 = "Grand Canyon";

    public static final String POST_RAW_CONTENT_FILE_ENTRY = "postData";

    public static final String VALUE_TEST_SIMPLE_RAW_DATA_WITH_EMPHASIS = "Test raw data & Result value";


    @Test
    public void testPostWithMultipartFormUpload() throws Exception {
        String filename = "GrandCanyon.txt";
        String fileContent = HttpPostRequestTest.VALUE;

        invokeServer(new HttpPostRequestBuilder().addFile(HttpPostRequestTest.FIELD, filename, fileContent).toString());

        assertEquals(1, this.testServer.parms.size());
        BufferedReader reader = new BufferedReader(new FileReader(this.testServer.files.get(HttpPostRequestTest.FIELD)));
        List<String> lines = readLinesFromFile(reader);
        assertLinesOfText(new String[]{
            fileContent
        }, lines);
    }

    @Test
    public void testPostWithMultipartFormUploadFilenameHasSpaces() throws Exception {
        String fileNameWithSpace = "Grand Canyon.txt";
        String fileContent = HttpPostRequestTest.VALUE;

        invokeServer(new HttpPostRequestBuilder().addFile(HttpPostRequestTest.FIELD, fileNameWithSpace, fileContent).toString());

        String fileNameAfter = new ArrayList<String>(this.testServer.parms.values()).get(0);

        assertEquals(fileNameWithSpace, fileNameAfter);
    }

    @Test
    public void testPostWithMultipleMultipartFormFields() throws Exception {
        HttpPostRequestBuilder req = new HttpPostRequestBuilder()
                .addParameter(HttpPostRequestTest.FIELD, HttpPostRequestTest.VALUE)
                .addParameter(HttpPostRequestTest.FIELD2, HttpPostRequestTest.VALUE2);
        invokeServer(req.toString());

        assertEquals(2, this.testServer.parms.size());
        assertEquals(HttpPostRequestTest.VALUE, this.testServer.parms.get(HttpPostRequestTest.FIELD));
        assertEquals(HttpPostRequestTest.VALUE2, this.testServer.parms.get(HttpPostRequestTest.FIELD2));
    }

    @Test
    public void testSimplePostWithSingleMultipartFormField() throws Exception {
        HttpPostRequestBuilder req = new HttpPostRequestBuilder()
                .addParameter(HttpPostRequestTest.FIELD, HttpPostRequestTest.VALUE);
        invokeServer(req.toString());

        assertEquals(1, this.testServer.parms.size());
        assertEquals(HttpPostRequestTest.VALUE, this.testServer.parms.get(HttpPostRequestTest.FIELD));
    }

    @Test
    public void testSimpleRawPostData() throws Exception {
        invokeServer(new HttpPostRequestBuilder().addRawData(HttpPostRequestTest.VALUE_TEST_SIMPLE_RAW_DATA_WITH_EMPHASIS).toString());
        assertEquals(0, this.testServer.parms.size());
        assertEquals(1, this.testServer.files.size());
        assertEquals(HttpPostRequestTest.VALUE_TEST_SIMPLE_RAW_DATA_WITH_EMPHASIS, this.testServer.files.get(HttpPostRequestTest.POST_RAW_CONTENT_FILE_ENTRY));
    }

    @Test
    public void testPostWithMultipartFormFieldsAndFile() throws IOException {
        HttpPostRequestBuilder req = new HttpPostRequestBuilder()
                .addFile(HttpPostRequestTest.FIELD, "GrandCanyon.txt", HttpPostRequestTest.VALUE)
                .addParameter(HttpPostRequestTest.FIELD2, HttpPostRequestTest.VALUE2);

        invokeServer(req.toString());

        assertEquals("Parameter count did not match.", 2, this.testServer.parms.size());
        assertEquals("Parameter value did not match", HttpPostRequestTest.VALUE2, this.testServer.parms.get(HttpPostRequestTest.FIELD2));
        BufferedReader reader = new BufferedReader(new FileReader(this.testServer.files.get(HttpPostRequestTest.FIELD)));
        List<String> lines = readLinesFromFile(reader);
        assertLinesOfText(new String[]{
                HttpPostRequestTest.VALUE
        }, lines);
    }

    @Test
    public void testPostWithMultipartFormUploadMultipleFiles() throws IOException {
        HttpPostRequestBuilder req = new HttpPostRequestBuilder()
                .addFile(HttpPostRequestTest.FIELD, "GrandCanyon.txt", HttpPostRequestTest.VALUE)
                .addFile(HttpPostRequestTest.FIELD2, "AnotherPhoto.txt", HttpPostRequestTest.VALUE2);

        invokeServer(req.toString());

        assertEquals("Parameter count did not match.", 2, this.testServer.parms.size());
        BufferedReader reader = new BufferedReader(new FileReader(this.testServer.files.get(HttpPostRequestTest.FIELD)));
        List<String> lines = readLinesFromFile(reader);
        assertLinesOfText(new String[]{
                HttpPostRequestTest.VALUE
        }, lines);
        String fileName2 = this.testServer.files.get(HttpPostRequestTest.FIELD2);
        int testNumber = 0;
        while (fileName2 == null && testNumber < 5) {
            testNumber++;
            fileName2 = this.testServer.files.get(HttpPostRequestTest.FIELD2 + testNumber);
        }
        reader = new BufferedReader(new FileReader(fileName2));
        lines = readLinesFromFile(reader);
        assertLinesOfText(new String[]{
                HttpPostRequestTest.VALUE2
        }, lines);

    }

    @Test
    public void testPostWithMultipartFormUploadFileWithMultilineContent() throws Exception {
        String filename = "GrandCanyon.txt";
        String lineSeparator = "\n";
        String fileContent = HttpPostRequestTest.VALUE + lineSeparator + HttpPostRequestTest.VALUE + lineSeparator + HttpPostRequestTest.VALUE;

        invokeServer(new HttpPostRequestBuilder().addFile(HttpPostRequestTest.FIELD, "GrandCanyon.txt", fileContent).toString());

        assertEquals("Parameter count did not match.", 1, this.testServer.parms.size());
        BufferedReader reader = new BufferedReader(new FileReader(this.testServer.files.get(HttpPostRequestTest.FIELD)));
        List<String> lines = readLinesFromFile(reader);
        assertLinesOfText(fileContent.split(lineSeparator), lines);
    }

}
