package io.forty11.web.js;

import java.util.Iterator;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JS
{
   public static JSArray toJSArray(String json)
   {
      return ((JSArray) parse(json));
   }

   public static JSObject toJSObject(String json)
   {
      return ((JSObject) parse(json));
   }

   public static Object toObject(String json)
   {
      return parse(json);
   }

   /**
    * Removes control characters exception /r/n/t
    * 
    * @see https://stackoverflow.com/questions/14028716/how-to-remove-control-characters-from-java-string
    * @param string
    * @return
    */
   public static String encodeString(String string)
   {
      return string.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
   }

   static Object parse(String js)
   {
      try
      {
         ObjectMapper mapper = new ObjectMapper();
         JsonNode rootNode = mapper.readValue(js, JsonNode.class);

         Object parsed = map(rootNode);
         return parsed;
      }
      catch (Exception ex)
      {
         String msg = "Error parsing JSON:" + ex.getMessage();

         if (!(ex instanceof JsonParseException))
         {
            msg += "\r\nSource:" + js;
         }

         throw new RuntimeException("400 Bad Request: '" + js + "'");
      }
   }

   static Object map(JsonNode json)
   {
      if (json == null)
         return null;

      if (json.isNull())
         return null;

      if (json.isValueNode())
         return json.asText();//FIX

      if (json.isArray())
      {
         JSArray retVal = null;
         retVal = new JSArray();

         for (JsonNode child : json)
         {
            retVal.add(map(child));
         }

         return retVal;
      }
      else if (json.isObject())
      {
         JSObject retVal = null;
         retVal = new JSObject();

         Iterator<String> it = json.fieldNames();
         while (it.hasNext())
         {
            String field = it.next();
            JsonNode value = json.get(field);
            retVal.put(field, map(value));
         }
         return retVal;
      }

      throw new RuntimeException("unparsable json:" + json);
   }
}
