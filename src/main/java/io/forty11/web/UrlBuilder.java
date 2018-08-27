package io.forty11.web;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.forty11.j.J;

public class UrlBuilder
{

   String       protocol = null;
   String       host     = null;
   Integer      port     = null;
   String       path     = null;

   List<NVPair> query    = new ArrayList();

   public UrlBuilder()
   {

   }

   public UrlBuilder(String url)
   {
      this(new Url(url));
   }

   public UrlBuilder(Url url)
   {
      protocol = url.getProtocol();
      host = url.getHost();
      port = url.getPort();
      path = url.getPath();
   }

   public UrlBuilder(String protocol, String host, Integer port, String path, Object... params)
   {
      this.protocol = protocol;
      this.host = host;
      this.port = port;
      this.path = path;

      List plist = null;
      if (params != null)
      {
         plist = Arrays.asList(params);
         if (plist.size() > 0 && plist.get(0) instanceof Collection)
         {
            plist = new ArrayList((Collection) plist.get(0));
         }
      }

      for (int i = 0; plist != null && i < plist.size(); i += 2)
      {
         query.add(new NVPair(plist.get(i) + "", plist.get(i + 1) == null ? null : (plist.get(i + 1) + "")));
      }
   }

   public String toString()
   {
      return toUrl().toString();
   }

   public String getHost()
   {
      return host;
   }

   public UrlBuilder withHost(String host)
   {
      host = host.replace("/", "");
      this.host = host;
      return this;
   }

   public Integer getPort()
   {
      return port;
   }

   public UrlBuilder withPort(Integer port)
   {
      this.port = port;
      return this;
   }

   public String getProtocol()
   {
      return protocol;
   }

   public UrlBuilder withProtocol(String protocol)
   {
      this.protocol = protocol;
      return this;
   }

   public String getPath()
   {
      return path;
   }

   public UrlBuilder withPath(String path)
   {
      this.path = path;
      if (!path.startsWith("/"))
         path += "/";

      return this;
   }

   public UrlBuilder addPath(String dir)
   {
      if (path == null)
         path = "/";

      if (!path.startsWith("/"))
         path = "/" + path;

      if (!path.endsWith("/"))
         path += "/";

      while (dir.startsWith("/"))
         dir = dir.substring(1);

      path += dir;

      if (!path.endsWith("/"))
         path += "/";

      return this;
   }

   /**
    * Parses queryString and adds the nvpairs to query.
    */
   public UrlBuilder withQuery(String queryString)
   {
      Map<String, String> params = Url.parseQuery(queryString);
      for (String key : params.keySet())
      {
         query.add(new NVPair(key, params.get(key)));
      }
      return this;
   }

   public UrlBuilder withParam(String name, String value)
   {
      try
      {
         query.add(new NVPair(URLEncoder.encode(name, "UTF-8"), value != null ? URLEncoder.encode(value, "UTF-8") : null));
      }
      catch (Exception ex)
      {
         J.rethrow(ex);
      }
      return this;
   }

   public Url toUrl()
   {
      String queryStr = null;
      if (query != null && query.size() > 0)
      {
         queryStr = "";
         for (int i = 0; i < query.size(); i++)
         {
            NVPair pair = query.get(i);
            if (J.empty(pair.value))
               queryStr += pair.name;
            else
               queryStr += pair.name + "=" + pair.value;

            if (i < query.size() - 1)
               queryStr += "&";
         }
      }

      Url u = new Url(this.protocol, this.host, this.port, this.path, queryStr);
      return u;

   }

   public class NVPair
   {
      String name  = null;
      String value = null;

      public NVPair(String name, String value)
      {
         super();
         this.name = name;
         this.value = value;
      }

      public String getName()
      {
         return name;
      }

      public void setName(String name)
      {
         this.name = name;
      }

      public String getValue()
      {
         return value;
      }

      public void setValue(String value)
      {
         this.value = value;
      }

   }
}
