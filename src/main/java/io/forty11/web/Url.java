/*
 * Copyright 2008-2017 Wells Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.forty11.web;

import java.io.Serializable;

public class Url implements Serializable
{

   protected String protocol = "http";
   protected String host     = null;
   protected int    port     = 0;
   protected String uri      = null;
   protected String query    = null;

   public Url(String url)
   {
      this((Url) null, url);
   }

   public Url(Url parent, String url)
   {
      parse(parent, url);
   }

   public Url(String parent, String url)
   {
      this(new Url(parent), url);
   }

   protected void parse(Url parent, String url)
   {
      if (url.indexOf(":/") > 0 && url.indexOf("://") < 0)
         url = url.replaceAll(":/", "://");

      url = url.replace("&amp;", "&");

      try
      {
         int queryIndex = url.indexOf('?');
         if (queryIndex >= 0)
         {
            query = url.substring(queryIndex + 1, url.length());
            url = url.substring(0, queryIndex);
         }

         //replace slashes after stripping off query to leave query as it was found
         url = url.replace('\\', '/');

         int potocolEnd = url.indexOf("://");
         if (potocolEnd < 0)
         {
            if (parent != null)
            {
               protocol = parent.protocol;
               host = parent.host;
               port = parent.port;
            }

            if (url.length() == 0 || url.charAt(0) == '/')
            {
               //-- absolute path form parent
               uri = url;
            }
            else
            {
               //-- path relative to parent
               if (parent != null)
               {
                  String parentUri = parent.uri;
                  if (parentUri.charAt(parentUri.length() - 1) != '/')
                  {
                     if (parentUri.lastIndexOf('/') >= 0)
                     {
                        //chop off the file to make it path
                        //realtive not file relative
                        parentUri = parentUri.substring(0, parentUri.lastIndexOf('/') + 1);
                     }
                     else
                     {
                        parentUri += '/';
                     }
                  }

                  if (url.charAt(0) == '/')
                  {
                     url = url.substring(1, url.length());
                  }

                  uri = parentUri + url;

               }
               else
               {
                  uri = url;
               }
            }
         }
         else
         {
            //-- parse a full url
            protocol = url.substring(0, url.indexOf(':'));

            int hostStart = url.indexOf('/') + 2;
            int hostEnd = url.indexOf(':', hostStart);

            //--this is probably ah file url like file://c:/
            //--so don't cound this colon
            //if(hostEnd - hostStart <= 1)
            //   hostEnd = url.indexOf(':', hostEnd + 1);
            if (hostEnd < 0 || hostEnd > url.indexOf('/', hostStart))
            {
               hostEnd = url.indexOf('/', hostStart);
            }
            if (hostEnd < 0)
            {
               url += "/";
               hostEnd = url.indexOf('/', hostStart);
            }

            host = url.substring(hostStart, hostEnd);

            String rest = url.substring(hostEnd, url.length());

            if (rest.indexOf(':') > -1)
            {
               int nextColon = rest.indexOf(':');
               int nextSlash = rest.indexOf('/');

               if (nextColon < nextSlash)
               {
                  String portString = rest.substring(nextColon + 1, nextSlash);
                  port = Integer.parseInt(portString);
                  rest = rest.substring(nextSlash, rest.length());
               }
            }

            uri = rest;
         }

         if (uri == null || uri.length() == 0)
         {
            uri = "/";
         }
         else if (uri.charAt(0) != '/')
         {
            uri = '/' + url;
         }
         while (uri.contains("//"))
         {
            uri = uri.replace("//", "/");
         }
      }
      catch (Exception ex)
      {
         System.err.println("Error parsing url \"" + url + "\"");
         ex.printStackTrace();
      }
   }

   public String toString()
   {
      String url = protocol + "://" + host;

      if (port > 0)
         url += ":" + port;

      if (uri != null)
         url += uri;

      if (query != null)
         url += "?" + query;

      return url;
   }

   public boolean equals(Object obj)
   {
      if (obj instanceof Url)
      {
         return toString().equals(((Url) obj).toString());
      }
      return false;
   }

   public String getDomain()
   {
      String domain = host;

      if (domain.lastIndexOf('.') > domain.indexOf('.'))
      {
         domain = domain.substring(domain.indexOf('.') + 1, domain.length());
      }
      return domain;
   }

   public String getHost()
   {
      return host;
   }

   public void setHost(String host)
   {
      this.host = host;
   }

   public int getPort()
   {
      if (port == 0)
         return 80;

      return port;
   }

   public void setPort(int port)
   {
      this.port = port;
   }

   public String getProtocol()
   {
      return protocol;
   }

   public void setProtocol(String protocol)
   {
      this.protocol = protocol;
   }

   public String getQuery()
   {
      return query;
   }

   public void setQuery(String query)
   {
      this.query = query;
   }

   public String getUri()
   {
      return uri;
   }

   public String getFile()
   {
      if (uri != null && !uri.endsWith("/"))
      {
         if (uri.lastIndexOf('/') > -1)
            return uri.substring(uri.lastIndexOf('/') + 1, uri.length());
         return uri;

      }

      return null;
   }

   public void setUri(String uri)
   {
      this.uri = uri;
   }

}
