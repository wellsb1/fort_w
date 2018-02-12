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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
   static Log       log                      = LogFactory.getLog(Web.class);

   static final int POOL_MIN                 = 2;
   static final int POOL_MAX                 = 100;
   static final int QUEUE_MAX                = 500;
   static final int DEFAULT_RETRY_ATTEMPTS   = 5;
   static final int TOTAL_MAX_RETRY_ATTEMPTS = 50;

   static Executor  pool                     = null;
   static Timer     timer                    = null;

   public static FutureResponse get(String url)
   {
      return rest(new Request("GET", url));
   }

   public static FutureResponse get(String url, List<String> headers)
   {
      return rest(new Request("GET", url, null, headers));
   }

   public static FutureResponse get(String url, List<String> headers, int retryAttempts)
   {
      return rest(new Request("GET", url, null, headers, retryAttempts));
   }

   public static FutureResponse get(String url, int retryAttempts)
   {
      return rest(new Request("GET", url, null, null, retryAttempts));
   }

   public static FutureResponse put(String url, String body)
   {
      return rest(new Request("PUT", url, body, null));
   }

   public static FutureResponse put(String url, String body, List<String> headers)
   {
      return rest(new Request("PUT", url, body, headers));
   }

   public static FutureResponse post(String url, String body)
   {
      return rest(new Request("POST", url, body, null));
   }

   public static FutureResponse post(String url, String body, List<String> headers)
   {
      return rest(new Request("POST", url, body, headers));
   }

   public static FutureResponse post(String url, String body, List<String> headers, int retryAttempts)
   {
      return rest(new Request("POST", url, body, headers, retryAttempts));
   }

   public static FutureResponse delete(String url)
   {
      return rest(new Request("DELETE", url));
   }

   public static FutureResponse delete(String url, List<String> headers)
   {
      return rest(new Request("DELETE", url, null, headers));
   }

   public static FutureResponse rest(final Request request)
   {
      final FutureResponse future = new FutureResponse()
         {
            public void run()
            {
               String m = request.getMethod();
               String url = request.getUrl();
               List<String> headers = request.getHeaders();
               boolean retryable = true;

               Response response = new Response(url);
               HttpRequestBase req = null;
               File tempFile = null;

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

                     if (this.getRetryFile() != null && this.getRetryFile().length() > 0)
                     {
                        if (headers == null)
                        {
                           headers = new ArrayList<String>();
                        }

                        // Add Range header for resumable download
                        long range = this.getRetryFile().length();
                        headers.add("Range");
                        headers.add("bytes=" + range + "-");

                        debug("RANGE REQUEST HEADER ** " + range);
                     }
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
                  if (request.getBody() != null && req instanceof HttpEntityEnclosingRequestBase)
                  {
                     response.log += "\r\n--request body--------";
                     //response.log += "\r\n" + json;
                     ((HttpEntityEnclosingRequestBase) req).setEntity(new StringEntity(request.getBody()));
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
                     response.log += "\r\n" + header.getName() + ": " + header.getValue();
                     response.headers.put(header.getName(), header.getValue());
                  }

                  debug("RESPONSE CODE ** " + response.code + "   (" + response.status + ")");
                  debug("CONTENT RANGE RESPONSE HEADER ** " + response.getHeader("Content-Range"));

                  InputStream is = e.getContent();

                  // We had a successful response, so let's reset the retry count to give the best chance of success
                  if (response.code >= 200 && response.code <= 300)
                  {
                     debug("Resetting retry count");
                     this.resetRetryCount();
                  }

                  Url u = new Url(url);
                  String fileName = u.getFile();

                  // if we have a retry file and it's length matches the Content-Range header's start and the Content-Range header's unit's are bytes use the existing file
                  if (response.code == 404)
                  {
                     retryable = false; // do not allow this to retry on a 404
                     return; //will go to finally block
                  }
                  else if (this.getRetryFile() != null && this.getRetryFile().length() == response.getContentRangeStart() && "bytes".equalsIgnoreCase(response.getContentRangeUnit()))
                  {
                     tempFile = this.getRetryFile();
                     debug("## Using existing file .. " + tempFile);
                  }
                  else if (response.code == 206)
                  {
                     // status code is 206 Partial Content, but we don't want to use the existing file for some reason, so abort this and force it to fail
                     retryable = false; // do not allow this to retry
                     throw new Exception("Partial content without valid values, aborting this request");
                  }
                  else
                  {
                     tempFile = J.createTempFile(fileName);
                     tempFile.deleteOnExit();
                     debug("## Creating temp file .. " + tempFile);
                  }

                  response.setFile(tempFile);

                  // stream to the temp file with append set to true (this is crucial for resumable downloads)
                  Streams.pipe(is, new FileOutputStream(tempFile, true));

                  if (response.getContentRangeSize() > 0 && tempFile.length() > response.getContentRangeSize())
                  {
                     // Something is wrong.. The server is saying the file should be X, but the actual file is larger than X, abort this
                     retryable = false; // do not allow this to retry
                     throw new Exception("Downloaded file is larger than the server says it should be, aborting this request");
                  }

               }
               catch (Exception ex)
               {
                  response.error = ex;

                  if (isNetworkException(ex))
                  {
                     log.debug("Network exception " + ex.getClass().getName() + " - " + ex.getMessage() + " - " + url);
                  }
                  else
                  {
                     log.warn(ex.getClass().getName() + " exception in rest call to URL: " + url, ex);
                  }

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
                        log.info("Exception trying to release the request connection", ex);
                     }
                  }

                  // If this is a retryable response, submit it later
                  // Since we resetRetryCount upon any successful response, we are still guarding against a crazy large amount of retries with the TOTAL_MAX_RETRY_ATTEMPTS
                  if (retryable && this.getRetryCount() < request.getRetryAttempts() && !response.isSuccess() && this.getTotalRetries() < TOTAL_MAX_RETRY_ATTEMPTS)
                  {
                     this.incrementRetryCount();

                     long timeout = (1000 * this.getRetryCount() * this.getRetryCount()) + (int) (1000 * Math.random() * this.getRetryCount());

                     debug("retrying: " + this.getRetryCount() + " - " + timeout + " - " + url);

                     // Set this for possible resumable download on the next try
                     if (this.getRetryFile() == null && response.code == 200)
                     {
                        this.setRetryFile(response.file);
                     }

                     submitLater(this, timeout);
                     return;
                  }
                  else
                  {
                     if (!response.isSuccess())
                     {
                        response.file = null;
                        if (tempFile != null)
                        {
                           debug("Deleting temp file: " + tempFile);
                           tempFile.delete();
                        }

                        if (response.getError() != null && !(isNetworkException(response.getError())))
                        {
                           log.warn("Error in Web.rest() . " + m + " : " + url, response.getError());
                        }

                     }

                     setResponse(response);
                  }
               }
            }
         };

      submit(future);
      return future;

   }

   public static boolean isNetworkException(Exception ex)
   {
      return ex instanceof org.apache.http.conn.HttpHostConnectException //
            || ex instanceof org.apache.http.conn.ConnectTimeoutException //
            || ex instanceof java.net.ConnectException //
            || ex instanceof java.net.SocketTimeoutException;
   }

   static synchronized void submit(FutureResponse future)
   {
      if (pool == null)
         pool = new Executor(POOL_MIN, POOL_MAX, QUEUE_MAX);

      pool.submit(future);
   }

   static synchronized void submitLater(final FutureResponse future, long delay)
   {
      if (timer == null)
      {
         timer = new Timer();
      }

      timer.schedule(new TimerTask()
         {
            @Override
            public void run()
            {
               submit(future);
            }
         }, delay);

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

   private static void debug(Object obj)
   {
      if (log.isDebugEnabled())
      {
         log.debug(obj);
      }
   }

   public static class Request
   {
      String       method;
      String       url;
      String       body;
      List<String> headers;
      int          retryAttempts;

      public Request(String method, String url)
      {
         this(method, url, null, null);
      }

      public Request(String method, String url, String body, List<String> headers)
      {
         this(method, url, body, headers, DEFAULT_RETRY_ATTEMPTS);
      }

      public Request(String method, String url, String body, List<String> headers, int retryAttempts)
      {
         super();
         this.method = method;
         this.url = url;
         this.body = body;
         this.headers = headers;
         this.retryAttempts = retryAttempts;
      }

      public String getMethod()
      {
         return method;
      }

      public void setMethod(String method)
      {
         this.method = method;
      }

      public String getUrl()
      {
         return url;
      }

      public void setUrl(String url)
      {
         this.url = url;
      }

      public String getBody()
      {
         return body;
      }

      public void setBody(String body)
      {
         this.body = body;
      }

      public List<String> getHeaders()
      {
         return headers;
      }

      public void setHeaders(List<String> headers)
      {
         this.headers = headers;
      }

      public int getRetryAttempts()
      {
         return retryAttempts;
      }

      public void setRetryAttempts(int retryAttempts)
      {
         this.retryAttempts = retryAttempts;
      }

   }

   public static abstract class FutureResponse implements RunnableFuture<Response>
   {
      Response              response     = null;
      List<ResponseHandler> onSuccess    = new ArrayList();
      List<ResponseHandler> onFailure    = new ArrayList();
      List<ResponseHandler> onResponse   = new ArrayList();
      int                   retryCount   = 0;
      File                  retryFile;
      int                   totalRetries = 0;              // this number doesn't get reset and is the true measure of how many retries occured

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
               log.error("Error handling onSuccess", ex);
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
               log.error("Error handling onFailure", ex);
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
               log.error("Error handling onResponse", ex);
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
                     log.error("Error handling success callbacks in setResponse", ex);
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
                     log.error("Error handling failure callbacks in setResponse", ex);
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
                  log.error("Error handling callbacks in setResponse", ex);
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
         if (SwingUtilities.isEventDispatchThread())
         {
            String msg = "Blocking on the Swing thread. Your code is blocking the UI by calling FutureResponse.get() on the Swing event dispatch thread.  You should consider moving your call into a background thread.";
            Exception ex = new Exception();
            ex.fillInStackTrace();
            log.warn(msg, ex);
         }

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
         if (SwingUtilities.isEventDispatchThread())
         {
            String msg = "Blocking on the Swing thread. Your code is blocking the UI by calling FutureResponse.get() on the Swing event dispatch thread.  You should consider moving your call into a background thread.";
            Exception ex = new Exception();
            ex.fillInStackTrace();
            log.warn(msg, ex);
         }

         timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
         while (response == null)
         {
            synchronized (this)
            {
               if (response == null)
               {
                  wait(timeout);
                  if (response == null)
                     throw new TimeoutException(timeout + " millisecond timeout reached");
               }
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

      protected int getRetryCount()
      {
         return retryCount;
      }

      public void incrementRetryCount()
      {
         this.totalRetries++;
         this.retryCount++;
      }

      public void resetRetryCount()
      {
         this.retryCount = 0;
      }

      public int getTotalRetries()
      {
         return totalRetries;
      }

      public File getRetryFile()
      {
         return retryFile;
      }

      public void setRetryFile(File retryFile)
      {
         this.retryFile = retryFile;
      }

   }

   public static interface ResponseHandler
   {
      public void onResponse(Response response) throws Exception;
   }

   public static class Response
   {
      static Log                           logger            = LogFactory.getLog(Response.class);

      String                               url               = null;
      String                               fileName          = null;
      File                                 file              = null;
      String                               type              = null;
      public int                           code              = 0;
      public String                        status            = "";
      public Exception                     error             = null;
      public String                        log               = "";

      String                               contentRangeUnit  = null;
      long                                 contentRangeStart = -1;
      long                                 contentRangeEnd   = -1;
      long                                 contentRangeSize  = -1;

      public LinkedHashMap<String, String> headers           = new LinkedHashMap();

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
            return new BufferedInputStream(new DeleteFileOnCloseInputStream(file));

         return null;
      }

      public String getContent()
      {
         try
         {
            if (file != null && file.length() > 0)
            {
               String string = J.read(getInputStream());
               return string;
            }
         }
         catch (Exception ex)
         {
            J.rethrow(ex);
         }
         return null;
      }

      public long getFileLength()
      {
         if (file != null)
         {
            return file.length();
         }
         return -1;
      }

      public void setFile(File file) throws Exception
      {
         this.file = file;
      }

      /**
       * This is the value returned from the server via the "Content-Length" header
       * NOTE: this will not match file length, for partial downloads, consider also using ContentRangeSize
       * @return
       */
      public long getContentLength()
      {
         if (headers != null && headers.get("Content-Length") != null)
         {
            return Long.parseLong(headers.get("Content-Length"));
         }
         return 0;
      }

      /**
       * This value come from the "Content-Range" header and is the unit part
       * Content-Range: <unit> <range-start>-<range-end>/<size>
       * @return
       */
      public String getContentRangeUnit()
      {
         parseContentRange();
         return contentRangeUnit;
      }

      /**
       * This value come from the "Content-Range" header and is the first part
       * Content-Range: <unit> <range-start>-<range-end>/<size>
       * @return
       */
      public long getContentRangeStart()
      {
         parseContentRange();
         return contentRangeStart;
      }

      /**
       * This value come from the "Content-Range" header and is the middle part
       * Content-Range: <unit> <range-start>-<range-end>/<size>
       * @return
       */
      public long getContentRangeEnd()
      {
         parseContentRange();
         return contentRangeEnd;
      }

      /**
       * This value come from the "Content-Range" header and is the last part
       * Content-Range: <unit> <range-start>-<range-end>/<size>
       * @return
       */
      public long getContentRangeSize()
      {
         parseContentRange();
         return contentRangeSize;
      }

      /**
       * Parses the "Content-Range" header
       * Content-Range: <unit> <range-start>-<range-end>/<size>
       */
      private void parseContentRange()
      {
         if (contentRangeUnit == null)
         {
            String range = headers.get("Content-Range");
            if (range != null)
            {
               String[] parts = range.split(" ");
               contentRangeUnit = parts[0];
               parts = parts[1].split("/");
               contentRangeSize = Long.parseLong(parts[1]);
               parts = parts[0].split("-");
               if (parts.length == 2)
               {
                  contentRangeStart = Long.parseLong(parts[0]);
                  contentRangeEnd = Long.parseLong(parts[1]);
               }
            }
         }
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

      public String getUrl()
      {
         return url;
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
               logger.error("Error handling onSuccess", ex);
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
               logger.error("Error handling onFailure", ex);
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
            logger.error("Error handling onResponse", ex);
         }
         return this;
      }

      @Override
      public String toString()
      {
         return "Response [url=" + url + ", type=" + type + ", code=" + code + ", status=" + status + "]";
      }

   }

   /**
    * Simple FileInputStream that will attempt to delete the file onf close of the inputstream
    *
    */
   public static class DeleteFileOnCloseInputStream extends FileInputStream
   {

      private File file;

      public DeleteFileOnCloseInputStream(File file) throws FileNotFoundException
      {
         super(file);
         this.file = file;
      }

      @Override
      public void close() throws IOException
      {
         try
         {
            super.close();
         }
         finally
         {
            if (file != null)
            {
               file.delete();
               file = null;
            }
         }
      }

   }
}
