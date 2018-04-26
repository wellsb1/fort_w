package io.forty11.web;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import io.forty11.j.J;
import io.forty11.web.Url;

public class UrlBuilder
{

   String       protocol = null;
   String       host     = null;
   String       port     = null;
   String       path     = null;

   List<NVPair> query    = new ArrayList();

   public UrlBuilder()
   {

   }

   public UrlBuilder(Url url)
   {

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

   public String getPort()
   {
      return port;
   }

   public UrlBuilder withPort(String port)
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

   public UrlBuilder withParam(String name, String value)
   {
      try
      {
         query.add(new NVPair(URLEncoder.encode(name, "UTF-8"), URLEncoder.encode(value, "UTF-8")));
      }
      catch (Exception ex)
      {
         J.rethrow(ex);
      }
      return this;
   }

   public Url toUrl()
   {
      String url = "";

      if (host != null)
      {
         url += protocol != null ? protocol : "http";
         url += "://";
         url += host;
         if (port != null)
         {
            url += ":" + port;
         }
      }

      if (path != null)
      {
         url += path;
      }

      for (int i = 0; i < query.size(); i++)
      {
         if (i == 0)
            url += "?";

         NVPair pair = query.get(i);
         url += pair.name + "=" + pair.value;

         if (i < query.size() - 1)
            url += "&";
      }

      return new Url(url);
   }

   class NVPair
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
