/*
 * Copyright 2008-2017 Wells Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.forty11.web;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.forty11.j.J;
import io.forty11.j.api.ApiMethod;
import io.forty11.j.api.Files;
import io.forty11.j.api.Lang;
import io.forty11.j.api.Streams;
import io.forty11.j.api.Strings;

/**
 * 
 * @author Wells Burke
 *
 */
public class Web
{
   static Map<String, String> mimeTypes = new LinkedHashMap();

   static
   {
      mimeTypes.put("atom", "application/atom+xml");
      mimeTypes.put("gif", "image/gif");
      mimeTypes.put("png", "image/png");
      mimeTypes.put("jpg", "image/jpeg");
      mimeTypes.put("jpeg", "image/jpeg");
      mimeTypes.put("json", "application/json");
      mimeTypes.put("rss", "application/rss+xml");
   }

   public static class Response
   {
      public int                           code    = 0;
      public String                        status  = "";
      public String                        body    = null;
      public Exception                     error   = null;
      public String                        log     = "";

      public LinkedHashMap<String, String> headers = new LinkedHashMap();
   }

   public static Response restGet(String url, String... reqHeaders) throws Exception
   {
      return rest("GET", url, null, reqHeaders);
   }

   public static Response restPut(String url, String json, String... reqHeaders) throws Exception
   {
      return rest("PUT", url, json, reqHeaders);
   }

   public static Response restPost(String url, String json, String... reqHeaders) throws Exception
   {
      return rest("POST", url, json, reqHeaders);
   }

   public static Response restDelete(String url, String... reqHeaders) throws Exception
   {
      return rest("DELETE", url, null, reqHeaders);
   }

   public static Response rest(String m, String url, String json, String... headers) throws Exception
   {
      //System.out.println("REST:" + m + " " + url);

      int timeout = 30000;
      Response r = new Response();

      HttpClient h = getHttpClient();
      HttpResponse hr = null;

      r.log += "\r\n--request header------";
      r.log += "\r\n" + m + " " + url;

      HttpRequestBase req = null;

      if ("post".equalsIgnoreCase(m))
      {
         req = new HttpPost(url);
      }
      if ("put".equalsIgnoreCase(m))
      {
         req = new HttpPut(url);
      }
      else if ("get".equalsIgnoreCase(m))
      {
         req = new HttpGet(url);
      }
      else if ("delete".equalsIgnoreCase(m))
      {
         req = new HttpDelete(url);
      }

      for (int i = 0; headers != null && i < headers.length - 1; i += 2)
      {
         req.setHeader(headers[i], headers[i + 1]);
         r.log += "\r\n" + headers[i] + ": " + headers[i + 1];
      }
      if (json != null && req instanceof HttpEntityEnclosingRequestBase)
      {
         r.log += "\r\n--request body--------";
         r.log += "\r\n" + json;
         ((HttpEntityEnclosingRequestBase) req).setEntity(new StringEntity(json));
      }

      RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout)
                                                 .setConnectionRequestTimeout(timeout).build();
      req.setConfig(requestConfig);

      hr = h.execute(req);

      HttpEntity e = hr.getEntity();

      r.status = hr.getStatusLine().toString();
      r.code = hr.getStatusLine().getStatusCode();

      r.log += "\r\n--response headers -----";
      r.log += "\r\n" + "status: " + r.status;
      for (Header header : hr.getAllHeaders())
      {
         r.log += header.getName() + ": " + header.getValue();
         r.headers.put(header.getName(), header.getValue());
      }

      String rbody = J.read(e.getContent());
      r.body = rbody;

      r.log += "\r\n--response body------";
      r.log += "\r\n" + rbody;

      return r;

   }

   public static String getMimeType(String fileName)
   {
      String ext = Files.getFileExtension(fileName);

      if (!Lang.empty(ext))
      {
         return mimeTypes.get(ext.toLowerCase());
      }
      return null;
   }

   public static String getFileNameMimeTypeFileExtension(String fileName)
   {
      String ext = Files.getFileExtension(fileName);
      if (ext != null)
      {
         ext = ext.toLowerCase();
         if (mimeTypes.containsKey(ext))
         {
            return ext;
         }
      }
      return null;
   }

   public static List<String> getMimeTypeFileExtensions(String mimeType)
   {
      List<String> exts = new ArrayList();
      for (String ext : mimeTypes.keySet())
      {
         String type = mimeTypes.get(ext);
         if (Strings.wildcardMatch(mimeType, type))
         {
            exts.add(ext);
         }
      }
      return exts;
   }

   /**
    * @see http://literatejava.com/networks/ignore-ssl-certificate-errors-apache-httpclient-4-4/
    * @return
    * @throws Exception
    */
   public static HttpClient getHttpClient() throws Exception
   {
      HttpClientBuilder b = HttpClientBuilder.create();

      // setup a Trust Strategy that allows all certificates.
      //
      SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy()
         {
            public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
            {
               return true;
            }
         }).build();
      b.setSslcontext(sslContext);

      // don't check Hostnames, either.
      //      -- use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
      HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

      // here's the special part:
      //      -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
      //      -- and create a Registry, to register it.
      //
      SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
      //Registry<ConnectionSocketFactory> socketFactoryRegistry = ;

      // now, we create connection-manager using our Registry.
      //      -- allows multi-threaded use
      PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
                                                                                                         .register("http",
                                                                                                               PlainConnectionSocketFactory.getSocketFactory())
                                                                                                         .register("https",
                                                                                                               sslSocketFactory)
                                                                                                         .build());
      b.setConnectionManager(connMgr);

      // finally, build the HttpClient;
      //      -- done!
      HttpClient client = b.build();

      return client;
   }

   @ApiMethod
   public static WebFile wget(String url, long maxLength) throws IOException
   {
      WebFile file = new WebFile();
      file.setUrl(url);
      return wget(file, maxLength);
   }

   @ApiMethod
   public static WebFile wget(WebFile file, final long maxLength) throws IOException
   {
      try
      {
         String url = file.getUrl();

         url = url.trim();
         url = url.replaceAll(" ", "%20");

         HttpClient httpClient = getHttpClient();

         final HttpGet get = new HttpGet(url);
         //get.setFollowRedirects(true);

         HttpResponse response = httpClient.execute(get);
         HttpEntity entity = response.getEntity();
         if (entity != null)
         {
            long len = entity.getContentLength();
            InputStream inputStream = entity.getContent();
            URL uri = new URL(get.getURI().toString());

            file.url = uri.toString();
            file.fileName = uri.getFile();

            file.length = len;
            if (maxLength > 0 && file.length > maxLength)
               throw new IOException("file exceeds maximum length");

            if (entity.getContentType() != null)
            {
               String mt = entity.getContentType().getValue();
               if (mt.indexOf(':') > 0)
                  mt = mt.substring(mt.indexOf(':') + 1, mt.length()).trim();

               if (mt.indexOf(';') > 0)
                  mt = mt.substring(0, mt.indexOf(';'));

               file.type = mt;
            }

            file.inputStream = new FilterInputStream(inputStream)
               {
                  long    total  = 0;
                  boolean closed = false;

                  protected void check() throws IOException
                  {
                     if (maxLength > 0 && (total > maxLength))
                     {
                        close();
                        throw new IOException("exceeded maxLength: " + total + " > " + maxLength);
                     }
                  }

                  @Override
                  public int read() throws IOException
                  {
                     total += 1;
                     check();
                     return super.read();
                  }

                  @Override
                  public int read(byte[] b, int off, int len) throws IOException
                  {
                     int read = super.read(b, off, len);
                     total += read;
                     check();

                     return read;
                  }

                  @Override
                  public void close() throws IOException
                  {
                     try
                     {
                        closed = true;
                        get.releaseConnection();
                     }
                     catch (Exception ex)
                     {

                     }
                     super.close();
                  }

                  @Override
                  protected void finalize() throws Throwable
                  {
                     if (!closed)
                        close();

                     super.finalize();
                  }
               };

         }
         else
         {
            return null;
         }

         return file;
      }
      catch (IOException ex)
      {
         throw ex;
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         throw new IOException(t.getMessage(), t);
      }
   }

   public static class WebFile
   {
      String      url         = null;
      String      fileName    = null;
      long        length      = -1;
      String      type        = "unknown/unknown";

      InputStream inputStream = null;
      File        tempFile    = null;

      Document    document    = null;
      Element     element     = null;
      String      comment     = null;
      String      title       = null;

      public WebFile()
      {

      }

      public WebFile(String url)
      {
         setUrl(url);
      }

      public WebFile(File tempFile)
      {
         setTempFile(tempFile);
      }

      public boolean isDownloaded()
      {
         return tempFile != null || inputStream != null;
      }

      public String getUrl()
      {
         return url;
      }

      public void setUrl(String url)
      {
         if (!Lang.empty(url))
         {
            url = url.trim();
            url = url.replaceAll(" ", "%20");
         }

         this.url = url;
         if (Lang.empty(type) || "unknown/unknown".equals(type))
         {
            type = getMimeType(url);
         }

         if (Lang.empty(fileName))
         {
            try
            {
               fileName = new URL(url).getFile();
               if (Lang.empty(fileName))
                  fileName = null;
            }
            catch (Exception ex)
            {

            }
         }
      }

      public String getFileName()
      {
         return fileName;
      }

      public void setFileName(String fileName)
      {
         this.fileName = fileName;
      }

      public long getLength()
      {
         return length;
      }

      public void setLength(long length)
      {
         this.length = length;
      }

      public String getType()
      {
         return type;
      }

      public void setType(String type)
      {
         this.type = type;
      }

      public Document getDocument()
      {
         return document;
      }

      public void setDocument(Document document)
      {
         this.document = document;
      }

      public Element getElement()
      {
         return element;
      }

      public void setElement(Element element)
      {
         this.element = element;
      }

      public String getComment()
      {
         return comment;
      }

      public void setComment(String comment)
      {
         this.comment = comment;
      }

      public String getTitle()
      {
         return title;
      }

      public void setTitle(String title)
      {
         this.title = title;
      }

      public InputStream getInputStream() throws IOException
      {
         if (tempFile != null)
            return new FileInputStream(tempFile);

         if (inputStream == null)
            wget(this, -1);

         InputStream temp = inputStream;
         inputStream = null;

         return temp;
      }

      public void setInputStream(InputStream inputStream)
      {
         this.inputStream = inputStream;
      }

      public String getContent() throws Exception
      {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         Streams.pipe(getInputStream(), baos);
         return new String(baos.toByteArray());
      }

      public File getTempFile() throws Exception
      {
         if (tempFile == null)
         {
            InputStream is = getInputStream();
            tempFile = Files.createTempFile(getFileName());//File.createTempFile("download", ".tmp");
            try
            {
               Streams.pipe(is, new FileOutputStream(tempFile));
            }
            catch (IOException ex)
            {
               tempFile.delete();
               throw ex;
            }

         }

         setTempFile(tempFile);
         return tempFile;
      }

      public void setTempFile(File tempFile)
      {
         if (length < 0)
            setLength(tempFile.length());

         this.tempFile = tempFile;
      }
   }
}
