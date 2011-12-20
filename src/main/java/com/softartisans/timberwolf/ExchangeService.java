package com.softartisans.timberwolf;

import com.cloudera.alfredo.client.AuthenticatedURL;
import com.cloudera.alfredo.client.AuthenticationException;

import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.apache.xmlbeans.XmlException;

import org.xmlsoap.schemas.soap.envelope.EnvelopeDocument;
import org.xmlsoap.schemas.soap.envelope.EnvelopeType;

/**
 * ExchangeService handles packing xmlbeans objects into a SOAP envelope,
 * sending them off to the Exchange server and then returning the xmlbeans
 * objects that come back.
 */
public class ExchangeService
{
    private static final String DECLARATION =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    private static final String SOAP_ENCODING = "UTF-8";

    private String endpoint;
    private HttpUrlConnectionFactory connectionFactory;

    public ExchangeService(String url, HttpUrlConnectionFactory factory)
    {
        endpoint = url;
        connectionFactory = factory;
    }

    /** 
     * Creates a new ExchangeService that talks to the given Exchange server.
     */    
    public ExchangeService(String url)
    {
        this(url, new AlfredoHttpUrlConnectionFactory());
    }    

    /** Sends a SOAP envelope request and returns the response. */
    private EnvelopeDocument sendRequest(EnvelopeDocument envelope)
        throws UnsupportedEncodingException, IOException, XmlException,
               AuthenticationException
    {
        String request = DECLARATION + envelope.xmlText();
        // TODO: log request.

        HttpURLConnection conn = connectionFactory.newInstance(endpoint, 
            request.getBytes(SOAP_ENCODING));
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
        {
            EnvelopeDocument response = EnvelopeDocument.Factory.parse(
                conn.getInputStream());
            return response;
        }
        else
        {
            // TODO: log error
            // And return something better.
            return EnvelopeDocument.Factory.newInstance();
        }
    }

    /** Returns the results of a find item request. */
    public FindItemResponseType findItem(FindItemType findItem)
        throws UnsupportedEncodingException, IOException, XmlException,
               AuthenticationException
    {
        EnvelopeDocument request = EnvelopeDocument.Factory.newInstance();
        EnvelopeType envelope = request.addNewEnvelope();
        envelope.addNewBody().setFindItem(findItem);

        EnvelopeDocument response = sendRequest(request);
        return response.getEnvelope().getBody().getFindItemResponse();
    }

    /** Returns the results of a get item request. */
    public GetItemResponseType getItem(GetItemType getItem)
        throws UnsupportedEncodingException, IOException, XmlException,
               AuthenticationException
    {
        EnvelopeDocument request = EnvelopeDocument.Factory.newInstance();
        EnvelopeType envelope = request.addNewEnvelope();
        envelope.addNewBody().setGetItem(getItem);

        EnvelopeDocument response = sendRequest(request);
        return response.getEnvelope().getBody().getGetItemResponse();
    }
}
