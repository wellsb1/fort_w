/*
 * Copyright (c) 2008 Wells Burke
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package io.forty11.web;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

import io.forty11.j.J;
import io.forty11.j.api.Lang;
import io.forty11.j.api.Streams;
import io.forty11.utils.Executor;

/**
 * 
 * @author Wells Burke
 *
 */
public class Web
{
   static final int POOL_MIN  = 2;
   static final int POOL_MAX  = 100;
   static final int QUEUE_MAX = 500;

   static Executor  pool      = null;

   public static FutureResponse get(String url)
   {
      return rest("GET", url, null, null);
   }

   public static FutureResponse get(String url, List<String> headers)
   {
      return rest("GET", url, null, headers);
   }

   public static FutureResponse put(String url, String json)
   {
      return rest("PUT", url, json, null);
   }

   public static FutureResponse put(String url, String body, List<String> headers)
   {
      return rest("PUT", url, body, headers);
   }

   public static FutureResponse post(String url, String body)
   {
      return rest("POST", url, body, null);
   }

   public static FutureResponse post(String url, String body, List<String> headers)
   {
      return rest("POST", url, body, headers);
   }

   public static FutureResponse delete(String url)
   {
      return rest("DELETE", url, null, null);
   }

   public static FutureResponse delete(String url, List<String> headers)
   {
      return rest("DELETE", url, null, headers);
   }

   public static FutureResponse rest(final String m, final String url, final String body, final List<String> headers)
   {
      final FutureResponse future = new FutureResponse()
         {
            public void run()
            {
               Response response = new Response(url);
               HttpRequestBase req = null;

               try
               {
                  int timeout = 30000;

                  HttpClient h = getHttpClient();
                  HttpResponse hr = null;

                  response.log += "\r\n--request header------";
                  response.log += "\r\n" + m + " " + url;

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

                  for (int i = 0; headers != null && i < headers.size() - 1; i += 2)
                  {
                     req.setHeader(headers.get(i), headers.get(i + 1));
                     response.log += "\r\n" + headers.get(i) + ": " + headers.get(i + 1);
                  }
                  if (body != null && req instanceof HttpEntityEnclosingRequestBase)
                  {
                     response.log += "\r\n--request body--------";
                     //response.log += "\r\n" + json;
                     ((HttpEntityEnclosingRequestBase) req).setEntity(new StringEntity(body));
                  }

                  RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).setConnectionRequestTimeout(timeout).build();
                  req.setConfig(requestConfig);

                  hr = h.execute(req);

                  HttpEntity e = hr.getEntity();

                  response.status = hr.getStatusLine().toString();
                  response.code = hr.getStatusLine().getStatusCode();

                  response.log += "\r\n--response headers -----";
                  response.log += "\r\n" + "status: " + response.status;
                  for (Header header : hr.getAllHeaders())
                  {
                     response.log += header.getName() + ": " + header.getValue();
                     response.headers.put(header.getName(), header.getValue());
                  }

                  //String rbody = J.read(e.getContent());
                  //r.body = rbody;

                  InputStream is = e.getContent();

                  Url u = new Url(url);
                  String fileName = u.getFile();
                  File tempFile = J.createTempFile(fileName);
                  try
                  {
                     Streams.pipe(is, new FileOutputStream(tempFile));
                  }
                  catch (IOException ex)
                  {
                     tempFile.delete();
                     throw ex;
                  }

                  response.setFile(tempFile);

               }
               catch (Exception ex)
               {
                  response.error = ex;
                  response.file = null;
               }
               finally
               {
                  if (req != null)
                  {
                     try
                     {
                        req.releaseConnection();
                     }
                     catch (Exception ex)
                     {
                        ex.printStackTrace();
                     }
                  }

                  setResponse(response);
               }

            }
         };

      submit(future);
      return future;

   }

   static synchronized void submit(FutureResponse future)
   {
      if (pool == null)
         pool = new Executor(POOL_MIN, POOL_MAX, QUEUE_MAX);

      pool.submit(future);
   }

   /**
    * @see http://literatejava.com/networks/ignore-ssl-certificate-errors-apache-httpclient-4-4/
    * @return
    * @throws Exception
    */
   public static synchronized HttpClient getHttpClient() throws Exception
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
      PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory> create().register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", sslSocketFactory).build());
      b.setConnectionManager(connMgr);

      // finally, build the HttpClient;
      //      -- done!
      HttpClient client = b.build();

      return client;
   }

   public static abstract class FutureResponse implements RunnableFuture<Response>
   {
      Response              response   = null;
      List<ResponseHandler> onSuccess  = new ArrayList();
      List<ResponseHandler> onFailure  = new ArrayList();
      List<ResponseHandler> onResponse = new ArrayList();

      public FutureResponse onSuccess(ResponseHandler handler)
      {
         boolean done = false;
         synchronized (this)
         {
            done = isDone();
            if (!done)
            {
               onSuccess.add(handler);
            }
         }

         if (done && isSuccess())
         {
            try
            {
               handler.onResponse(response);
            }
            catch (Throwable ex)
            {
               ex.printStackTrace();
            }
         }

         return this;
      }

      public FutureResponse onFailure(ResponseHandler handler)
      {
         boolean done = false;
         synchronized (this)
         {
            done = isDone();
            if (!done)
            {
               onFailure.add(handler);
            }
         }

         if (done && !isSuccess())
         {
            try
            {
               handler.onResponse(response);
            }
            catch (Throwable ex)
            {
               ex.printStackTrace();
            }
         }

         return this;
      }

      public FutureResponse onResponse(ResponseHandler handler)
      {
         boolean done = false;
         synchronized (this)
         {
            done = isDone();
            if (!done)
            {
               onResponse.add(handler);
            }
         }

         if (done)
         {
            try
            {
               handler.onResponse(response);
            }
            catch (Throwable ex)
            {
               ex.printStackTrace();
            }
         }

         return this;
      }

      public void setResponse(Response response)
      {
         synchronized (this)
         {
            this.response = response;

            if (isSuccess())
            {
               for (ResponseHandler h : onSuccess)
               {
                  try
                  {
                     h.onResponse(response);
                  }
                  catch (Throwable ex)
                  {
                     ex.printStackTrace();
                  }
               }
            }
            else
            {
               for (ResponseHandler h : onFailure)
               {
                  try
                  {
                     h.onResponse(response);
                  }
                  catch (Throwable ex)
                  {
                     ex.printStackTrace();
                  }
               }
            }

            for (ResponseHandler h : onResponse)
            {
               try
               {
                  h.onResponse(response);
               }
               catch (Throwable ex)
               {
                  ex.printStackTrace();
               }
            }

            notifyAll();
         }
      }

      @Override
      public boolean cancel(boolean arg0)
      {
         return false;
      }

      @Override
      public Response get()
      {
         while (response == null)
         {
            synchronized (this)
            {
               if (response == null)
               {
                  try
                  {
                     wait();
                  }
                  catch (Exception ex)
                  {

                  }
               }
            }
         }

         return response;
      }

      public boolean isSuccess()
      {
         if (response != null && response.error == null && response.code >= 200 && response.code < 300)
            return true;

         return false;
      }

      public Response get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
      {
         timeout = unit.convert(timeout, TimeUnit.MILLISECONDS);
         long start = System.currentTimeMillis();
         while (!isDone())
         {
            long now = System.currentTimeMillis();
            if (now - start >= timeout)
            {
               J.sleep(250);
            }
         }

         return response;
      }

      @Override
      public boolean isCancelled()
      {
         return false;
      }

      @Override
      public boolean isDone()
      {
         return response != null;
      }
   }

   static interface ResponseHandler
   {
      public void onResponse(Response response) throws Exception;
   }

   public static class Response
   {
      String                               url      = null;
      String                               fileName = null;
      File                                 file     = null;
      long                                 length   = -1;
      String                               type     = null;
      public int                           code     = 0;
      public String                        status   = "";
      public Exception                     error    = null;
      public String                        log      = "";

      public LinkedHashMap<String, String> headers  = new LinkedHashMap();

      Response(String url)
      {
         setUrl(url);
      }

      public boolean isSuccess()
      {
         return code >= 200 && code <= 300 && error == null;
      }

      public int getCode()
      {
         return code;
      }

      public String getStatus()
      {
         return status;
      }

      public Exception getError()
      {
         return error;
      }

      public String getLog()
      {
         return log;
      }

      public LinkedHashMap<String, String> getHeaders()
      {
         return new LinkedHashMap(headers);
      }

      public String getHeader(String header)
      {
         String value = headers.get(header);
         if (value == null)
         {
            for (String key : headers.keySet())
            {
               if (key.equalsIgnoreCase(header))
                  return headers.get(key);
            }
         }
         return value;
      }

      public InputStream getInputStream() throws IOException
      {
         if (file != null)
            return new BufferedInputStream(new FileInputStream(file));

         return null;
      }

      public String getContent()
      {
         try
         {
            if(file != null && file.length() > 0)
            {
               String string = J.read(file);
               return string;
            }
         }
         catch (Exception ex)
         {
            J.rethrow(ex);
         }
         return null;
      }

      public void setFile(File file) throws Exception
      {
         this.file = file;
      }

      public long getLength()
      {
         return length;
      }

      public void setLength(long length)
      {
         this.length = length;
      }

      public void setUrl(String url)
      {
         if (!Lang.empty(url))
         {
            url = url.trim();
            url = url.replaceAll(" ", "%20");
         }

         this.url = url;

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

      public Response onSuccess(ResponseHandler handler)
      {
         if (isSuccess())
         {
            try
            {
               handler.onResponse(this);
            }
            catch (Exception ex)
            {
               ex.printStackTrace();
            }
         }
         return this;
      }

      public Response onFailure(ResponseHandler handler)
      {
         if (!isSuccess())
         {
            try
            {
               handler.onResponse(this);
            }
            catch (Exception ex)
            {
               ex.printStackTrace();
            }
         }
         return this;
      }

      public Response onResponse(ResponseHandler handler)
      {
         try
         {
            handler.onResponse(this);
         }
         catch (Exception ex)
         {
            ex.printStackTrace();
         }
         return this;
      }

   }
}
