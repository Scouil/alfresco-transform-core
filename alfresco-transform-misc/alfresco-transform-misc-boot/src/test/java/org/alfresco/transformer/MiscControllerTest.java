/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * -
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 * -
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * -
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * -
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transformer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_HTML;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IWORK_KEYNOTE;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IWORK_NUMBERS;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_WORDPROCESSING;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_RFC822;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transformer.transformers.SelectingTransformer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@RunWith(SpringRunner.class)
@WebMvcTest(MiscController.class)
public class MiscControllerTest extends AbstractTransformerControllerTest
{
    @Autowired
    private MiscController controller;

    private final String sourceEncoding = "UTF-8";
    private final String targetEncoding = "UTF-8";
    private final String targetMimetype = MIMETYPE_TEXT_PLAIN;

    private static final String ENGINE_CONFIG_NAME = "misc_engine_config.json";

    @Before
    public void before() throws Exception
    {
        sourceMimetype = MIMETYPE_HTML;
        sourceExtension = "html";
        targetExtension = "txt";
        expectedOptions = null;
        expectedSourceSuffix = null;
        expectedSourceFileBytes = readTestFile(sourceExtension);
        expectedTargetFileBytes = readTestFile(targetExtension);
        //expectedTargetFileBytes = null;
        sourceFile = new MockMultipartFile("file", "quick." + sourceExtension, sourceMimetype,
            expectedSourceFileBytes);
    }

    @Override
    public String getEngineConfigName()
    {
        return ENGINE_CONFIG_NAME;
    }

    @Override
    protected void mockTransformCommand(String sourceExtension, String targetExtension,
        String sourceMimetype, boolean readTargetFileBytes)
    {
    }

    @Override
    protected AbstractTransformerController getController()
    {
        return controller;
    }

    @Override
    protected void updateTransformRequestWithSpecificOptions(TransformRequest transformRequest)
    {
    }

    @Override
    protected MockHttpServletRequestBuilder mockMvcRequest(String url, MockMultipartFile sourceFile,
        String... params)
    {
        final MockHttpServletRequestBuilder builder = super
            .mockMvcRequest(url, sourceFile, params)
            .param("sourceEncoding", sourceEncoding)
            .param("targetMimetype", targetMimetype)
            .param("sourceMimetype", sourceMimetype);

        // Only the 'string' transformer should have the targetEncoding.
        if (!"message/rfc822".equals(sourceMimetype) && !"text/html".equals(sourceMimetype))
        {
            builder.param("targetEncoding", targetEncoding);
        }
        return builder;
    }

    @Test
    @Override
    public void noTargetFileTest()
    {
        // Ignore the test in super class as the Misc transforms are real rather than mocked up.
        // It is the mock that returns a zero length file for other transformers, when we supply an invalid targetExtension.
    }

    /**
     * Test transforming a valid eml file to text
     */
    @Test
    public void testRFC822ToText() throws Exception
    {
        String expected = "Gym class featuring a brown fox and lazy dog";
        MvcResult result = sendRequest("eml",
            null,
            MIMETYPE_RFC822,
            "txt",
            MIMETYPE_TEXT_PLAIN,
            null,
            readTestFile("eml"));
        assertTrue("Content from eml transform didn't contain expected value. ",
            result.getResponse().getContentAsString().contains(expected));
    }

    /**
     * Test transforming a non-ascii eml file to text
     */
    @Test
    public void testNonAsciiRFC822ToText() throws Exception
    {
        String expected = "El r\u00E1pido zorro marr\u00F3n salta sobre el perro perezoso";
        MvcResult result = sendRequest("eml",
            null,
            MIMETYPE_RFC822,
            "txt",
            MIMETYPE_TEXT_PLAIN,
            null,
            readTestFile("spanish.eml"));

        String contentResult = new String(result.getResponse().getContentAsByteArray(), UTF_8);
        assertTrue("Content from eml transform didn't contain expected value. ",
            contentResult.contains(expected));
    }

    /**
     * Test transforming a valid eml with an attachment to text; attachment should be ignored
     */
    @Test
    public void testRFC822WithAttachmentToText() throws Exception
    {
        String expected = "Mail with attachment content";
        String notExpected = "File attachment content";
        MvcResult result = sendRequest("eml",
            null,
            MIMETYPE_RFC822,
            "txt",
            MIMETYPE_TEXT_PLAIN,
            null,
            readTestFile("attachment.eml"));
        assertTrue("Content from eml transform didn't contain expected value. ",
            result.getResponse().getContentAsString().contains(expected));
        assertFalse(result.getResponse().getContentAsString().contains(notExpected));
    }

    /**
     * Test transforming a valid eml with minetype multipart/alternative to text
     */
    @Test
    public void testRFC822AlternativeToText() throws Exception
    {
        String expected = "alternative plain text";
        MvcResult result = sendRequest("eml",
            null,
            MIMETYPE_RFC822,
            "txt",
            MIMETYPE_TEXT_PLAIN,
            null,
            readTestFile("alternative.eml"));
        assertTrue("Content from eml transform didn't contain expected value. ",
            result.getResponse().getContentAsString().contains(expected));
    }

    /**
     * Test transforming a valid eml with nested mimetype multipart/alternative to text
     */
    @Test
    public void testRFC822NestedAlternativeToText() throws Exception
    {
        String expected = "nested alternative plain text";
        MvcResult result = sendRequest("eml",
            null,
            MIMETYPE_RFC822,
            "txt",
            MIMETYPE_TEXT_PLAIN,
            null,
            readTestFile("nested.alternative.eml"));
        assertTrue("Content from eml transform didn't contain expected value. ",
            result.getResponse().getContentAsString().contains(expected));
    }

    /**
     * Test transforming a valid eml with a html part containing html special characters to text
     */
    @Test
    public void testHtmlSpecialCharsToText() throws Exception
    {
        String expected = "&nbsp;";
        MvcResult result = sendRequest("eml",
            null,
            MIMETYPE_RFC822,
            "txt",
            MIMETYPE_TEXT_PLAIN,
            null,
            readTestFile("htmlChars.eml"));
        assertFalse(result.getResponse().getContentAsString().contains(expected));
    }

    @Test
    public void testHTMLtoString() throws Exception
    {
        final String NEWLINE = System.getProperty("line.separator");
        final String TITLE = "Testing!";
        final String TEXT_P1 = "This is some text in English";
        final String TEXT_P2 = "This is more text in English";
        final String TEXT_P3 = "C'est en Fran\u00e7ais et Espa\u00f1ol";
        String partA = "<html><head><title>" + TITLE + "</title></head>" + NEWLINE;
        String partB = "<body><p>" + TEXT_P1 + "</p>" + NEWLINE +
                       "<p>" + TEXT_P2 + "</p>" + NEWLINE +
                       "<p>" + TEXT_P3 + "</p>" + NEWLINE;
        String partC = "</body></html>";
        final String expected = TITLE + NEWLINE + TEXT_P1 + NEWLINE + TEXT_P2 + NEWLINE + TEXT_P3 + NEWLINE;

        MvcResult result = sendRequest("html",
            "UTF-8",
            MIMETYPE_HTML,
            "txt",
            MIMETYPE_TEXT_PLAIN,
            null,
            expected.getBytes());

        String contentResult = new String(result.getResponse().getContentAsByteArray(),
            targetEncoding);
        assertTrue("The content did not include \"" + expected, contentResult.contains(expected));
    }

    @Test
    public void testStringToString() throws Exception
    {
        String expected;
        byte[] content;
        try
        {
            content = "azAz10!�$%^&*()\t\r\n".getBytes(UTF_8);
            expected = new String(content, "MacDingbat");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("Encoding not recognised", e);
        }

        MvcResult result = sendRequest("txt",
            "MacDingbat",
            MIMETYPE_TEXT_PLAIN,
            "txt",
            MIMETYPE_TEXT_PLAIN,
            "UTF-8",
            content);

        String contentResult = new String(result.getResponse().getContentAsByteArray(),
            targetEncoding);
        assertTrue("The content did not include \"" + expected, contentResult.contains(expected));
    }

    @Test
    public void testEmptyTextFileReturnsEmptyFile() throws Exception
    {
        // Use empty content to create an empty source file
        byte[] content = new byte[0];

        MvcResult result = sendRequest("txt",
            "UTF-8",
            MIMETYPE_TEXT_PLAIN,
            "txt",
            MIMETYPE_TEXT_PLAIN,
            "UTF-8",
            content);

        assertEquals("Returned content should be empty for an empty source file", 0,
            result.getResponse().getContentLength());
    }

    @Test
    public void textToPdf() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++)
        {
            sb.append(i);
            sb.append(" I must not talk in class or feed my homework to my cat.\n");
        }
        sb.append("\nBart\n");
        String expected = sb.toString();

        MvcResult result = sendRequest("txt",
            "UTF-8",
            MIMETYPE_TEXT_PLAIN,
            "pdf",
            MIMETYPE_PDF,
            null,
            expected.getBytes());

        // Read back in the PDF and check it
        PDDocument doc = PDDocument.load(result.getResponse().getContentAsByteArray());
        PDFTextStripper textStripper = new PDFTextStripper();
        StringWriter textWriter = new StringWriter();
        textStripper.writeText(doc, textWriter);
        doc.close();

        expected = clean(expected);
        String actual = clean(textWriter.toString());

        assertEquals("The content did not match.", expected, actual);
    }

    @Test
    public void testAppleIWorksPages() throws Exception
    {
        MvcResult result = sendRequest("numbers", null, MIMETYPE_IWORK_NUMBERS,
            "jpeg", MIMETYPE_IMAGE_JPEG, null, readTestFile("pages"));
        assertTrue("Expected image content but content is empty.",
            result.getResponse().getContentLengthLong() > 0L);
    }

    @Test
    public void testAppleIWorksNumbers() throws Exception
    {
        MvcResult result = sendRequest("numbers", null, MIMETYPE_IWORK_NUMBERS,
            "jpeg", MIMETYPE_IMAGE_JPEG, null, readTestFile("numbers"));
        assertTrue("Expected image content but content is empty.",
            result.getResponse().getContentLengthLong() > 0L);
    }

    @Test
    public void testAppleIWorksKey() throws Exception
    {
        MvcResult result = sendRequest("key", null, MIMETYPE_IWORK_KEYNOTE,
            "jpeg", MIMETYPE_IMAGE_JPEG, null, readTestFile("key"));
        assertTrue("Expected image content but content is empty.",
            result.getResponse().getContentLengthLong() > 0L);
    }

    //    @Test
// TODO Doesn't work with java 11, enable when fixed
    public void testOOXML() throws Exception
    {
        MvcResult result = sendRequest("docx", null, MIMETYPE_OPENXML_WORDPROCESSING,
            "jpeg", MIMETYPE_IMAGE_JPEG, null, readTestFile("docx"));
        assertTrue("Expected image content but content is empty.",
            result.getResponse().getContentLengthLong() > 0L);
    }

    private MvcResult sendRequest(String sourceExtension,
        String sourceEncoding,
        String sourceMimetype,
        String targetExtension,
        String targetMimetype,
        String targetEncoding,
        byte[] content) throws Exception
    {
        final MockMultipartFile sourceFile = new MockMultipartFile("file",
            "test_file." + sourceExtension, sourceMimetype, content);

        final MockHttpServletRequestBuilder requestBuilder = super
            .mockMvcRequest("/transform", sourceFile)
            .param("targetExtension", targetExtension)
            .param("targetMimetype", targetMimetype)
            .param("sourceMimetype", sourceMimetype);

        if (sourceEncoding != null)
        {
            requestBuilder.param("sourceEncoding", sourceEncoding);
        }
        if (targetEncoding != null)
        {
            requestBuilder.param("targetEncoding", targetEncoding);
        }

        return mockMvc.perform(requestBuilder)
                      .andExpect(status().is(OK.value()))
                      .andExpect(header().string("Content-Disposition",
                          "attachment; filename*= " +
                          (targetEncoding == null ? "UTF-8" : targetEncoding) +
                          "''test_file." + targetExtension))
                      .andReturn();
    }

    private String clean(String text)
    {
        text = text.replaceAll("\\s+\\r", "");
        text = text.replaceAll("\\s+\\n", "");
        text = text.replaceAll("\\r", "");
        text = text.replaceAll("\\n", "");
        return text;
    }
}