/**
 * 
 */
package fort_w;

/**
 * @author tc-rocket
 *
 */
public class TestWebGet
{
   //   @BeforeClass
   //   public static void onceBeforeAnyTest()
   //   {
   //   }
   //
   //   @AfterClass
   //   public static void onceAfterAllTests()
   //   {
   //      System.out.println("---------------------------------------------------------------------------------");
   //      System.out.println("If you had many failures, chances are it because you don't have ToxiProxy running");
   //      System.out.println("Most of these tests depend on ToxiProxy - https://github.com/Shopify/toxiproxy");
   //      System.out.println("It is simple to install on Mac with.. ");
   //      System.out.println("    brew tap shopify/shopify");
   //      System.out.println("    brew install toxiproxy");
   //      System.out.println("For Windows, download ToxiProxy at the following link to run the app... ");
   //      System.out.println("    https://github.com/Shopify/toxiproxy/releases/download/v2.1.2/toxiproxy-server-windows-amd64.exe");
   //   }
   //
   //   @Before
   //   public void beforeEachTest()
   //   {
   //   }
   //
   //   @Test
   //   public void testWebGetSimple1()
   //   {
   //      FutureResponse fr = Web.get("http://google.com");
   //      Response response = fr.get();
   //      response.getContent();
   //      Assert.assertEquals("Response should be a 200", 200, response.getCode());
   //   }
   //
   //   @Test
   //   public void testWebGetSimple2()
   //   {
   //      FutureResponse fr = Web.get("http://google.com");
   //      fr.onResponse(new ResponseHandler()
   //         {
   //            @Override
   //            public void onResponse(Response response) throws Exception
   //            {
   //               response.getContent();
   //               Assert.assertEquals("Response should be a 200", 200, response.getCode());
   //            }
   //         });
   //
   //      // do this just to block, so the above code is executed
   //      fr.get();
   //
   //   }
   //
   //   @Test
   //   public void testWebGetRetry() throws IOException
   //   {
   //      ToxiproxyClient client = new ToxiproxyClient();
   //
   //      // Delete all existing proxy configs
   //      List<Proxy> proxies = client.getProxies();
   //      for (Proxy p : proxies)
   //      {
   //         p.delete();
   //      }
   //
   //      // Create new Toxiproxy 
   //      final Proxy proxy = client.createProxy("webget", "localhost:21212", "s3.amazonaws.com:80");
   //
   //      // slow the connection down
   //      proxy.toxics().bandwidth("bandwidth-10", ToxicDirection.DOWNSTREAM, 100);
   //
   //      // disable the proxy so the first request fails
   //      proxy.disable();
   //
   //      // after 2 second enable the proxy so the re-try will succeed
   //      _enableProxyAfter(proxy, 2000);
   //
   //      String url = "http://localhost:21212/files.liftck.com/test/large-01.jpg";
   //
   //      FutureResponse fr = Web.get(url);
   //      Response response = fr.get();
   //
   //      Assert.assertEquals("Response should be a 200", 200, response.getCode());
   //      Assert.assertTrue("Retry count should be more than zero", fr.getTotalRetries() > 0);
   //      Assert.assertEquals("Content length should equal file length", response.getContentLength(), response.getFileLength());
   //
   //   }
   //
   //   @Test
   //   public void testWebGetRetryMoreThanOnce() throws IOException
   //   {
   //      ToxiproxyClient client = new ToxiproxyClient();
   //
   //      // Delete all existing proxy configs
   //      List<Proxy> proxies = client.getProxies();
   //      for (Proxy p : proxies)
   //      {
   //         p.delete();
   //      }
   //
   //      // Create new Toxiproxy 
   //      final Proxy proxy = client.createProxy("webget", "localhost:21212", "s3.amazonaws.com:80");
   //
   //      // slow the connection down
   //      proxy.toxics().bandwidth("bandwidth-10", ToxicDirection.DOWNSTREAM, 100);
   //
   //      // disable the proxy so the first request fails
   //      proxy.disable();
   //
   //      // after 5 seconds enable the proxy so the re-try will succeed
   //      _enableProxyAfter(proxy, 5000);
   //
   //      String url = "http://localhost:21212/files.liftck.com/test/large-01.jpg";
   //
   //      FutureResponse fr = Web.get(url);
   //      Response response = fr.get();
   //
   //      Assert.assertEquals("Response should be a 200", 200, response.getCode());
   //      Assert.assertTrue("Retry count should be more than one", fr.getTotalRetries() > 1);
   //      Assert.assertEquals("Content length should equal file length", response.getContentLength(), response.getFileLength());
   //   }
   //
   //   @Test
   //   public void testWebGetRetryOnlyOnceAndFail() throws IOException
   //   {
   //      ToxiproxyClient client = new ToxiproxyClient();
   //
   //      // Delete all existing proxy configs
   //      List<Proxy> proxies = client.getProxies();
   //      for (Proxy p : proxies)
   //      {
   //         p.delete();
   //      }
   //
   //      // Create new Toxiproxy 
   //      final Proxy proxy = client.createProxy("webget", "localhost:21212", "s3.amazonaws.com:80");
   //
   //      // slow the connection down
   //      proxy.toxics().bandwidth("bandwidth-10", ToxicDirection.DOWNSTREAM, 100);
   //
   //      // disable the proxy so the first request fails
   //      proxy.disable();
   //
   //      String url = "http://localhost:21212/files.liftck.com/test/large-01.jpg";
   //
   //      FutureResponse fr = Web.get(url, 1);
   //      Response response = fr.get();
   //
   //      Assert.assertTrue("Response should be not be a 200", response.getCode() != 200);
   //      Assert.assertTrue("Retry count should be exactly one", fr.getTotalRetries() == 1);
   //      Assert.assertNotNull("Response error should not be null", response.getError());
   //   }
   //
   //   @Test
   //   public void testWebGetResumableRetry() throws IOException
   //   {
   //      ToxiproxyClient client = new ToxiproxyClient();
   //
   //      // Delete all existing proxy configs
   //      List<Proxy> proxies = client.getProxies();
   //      for (Proxy p : proxies)
   //      {
   //         p.delete();
   //      }
   //
   //      // Create new Toxiproxy 
   //      final Proxy proxy = client.createProxy("webget", "localhost:21212", "s3.amazonaws.com:80");
   //
   //      // slow the connection down
   //      proxy.toxics().bandwidth("bandwidth-10", ToxicDirection.DOWNSTREAM, 40);
   //
   //      // after 1 second disable the proxy so we'll have a partial download
   //      _disableProxyAfter(proxy, 1000);
   //
   //      // after 5 seconds enable the proxy so the retry can resume the download
   //      _enableProxyAfter(proxy, 5000);
   //
   //      String url = "http://localhost:21212/files.liftck.com/test/large-01.jpg";
   //
   //      FutureResponse fr = Web.get(url);
   //      Response response = fr.get();
   //
   //      Assert.assertEquals("Response should be a 206", 206, response.getCode());
   //      Assert.assertEquals("Content range size should equal file length", response.getContentRangeSize(), response.getFileLength());
   //   }
   //
   //   @Test
   //   public void testWebGetResumableRetryUpDown() throws IOException
   //   {
   //      ToxiproxyClient client = new ToxiproxyClient();
   //
   //      // Delete all existing proxy configs
   //      List<Proxy> proxies = client.getProxies();
   //      for (Proxy p : proxies)
   //      {
   //         p.delete();
   //      }
   //
   //      // Create new Toxiproxy 
   //      final Proxy proxy = client.createProxy("webget", "localhost:21212", "s3.amazonaws.com:80");
   //
   //      // slow the connection down
   //      proxy.toxics().bandwidth("bandwidth-10", ToxicDirection.DOWNSTREAM, 20);
   //
   //      ProxyUpDown proxyUpDown = new ProxyUpDown(2000, 1000, proxy, true);
   //      Thread t = new Thread(proxyUpDown, "testWebGetResumableRetryUpDown-proxyUpDown");
   //      t.start();
   //
   //      String url = "http://localhost:21212/files.liftck.com/test/large-01.jpg";
   //
   //      FutureResponse fr = Web.get(url);
   //      Response response = fr.get();
   //
   //      proxyUpDown.stop();
   //
   //      Assert.assertEquals("Response should be a 206", 206, response.getCode());
   //      Assert.assertEquals("Content range size should equal file length", response.getContentRangeSize(), response.getFileLength());
   //   }
   //
   //   @Test
   //   public void testWebGetNoConnection() throws IOException
   //   {
   //      ToxiproxyClient client = new ToxiproxyClient();
   //
   //      // Delete all existing proxy configs
   //      List<Proxy> proxies = client.getProxies();
   //      for (Proxy p : proxies)
   //      {
   //         p.delete();
   //      }
   //
   //      // Create new Toxiproxy 
   //      final Proxy proxy = client.createProxy("webget", "localhost:21212", "s3.amazonaws.com:80");
   //
   //      // slow the connection down
   //      proxy.toxics().bandwidth("bandwidth-10", ToxicDirection.DOWNSTREAM, 50);
   //
   //      // disable the proxy right away and leave it disabled
   //      proxy.disable();
   //
   //      String url = "http://localhost:21212/files.liftck.com/test/large-01.jpg";
   //
   //      FutureResponse fr = Web.get(url);
   //      Response response = fr.get();
   //
   //      Assert.assertTrue("Response should be not be a 200", response.getCode() != 200);
   //      Assert.assertNotNull("Response error should not be null", response.getError());
   //
   //   }
   //
   //   @Test
   //   public void testWebGet404NoRetryReset() throws IOException
   //   {
   //
   //      String url = "http://google.com/blah/blah";
   //
   //      FutureResponse fr = Web.get(url, null, 3);
   //      Response response = fr.get();
   //
   //      System.out.println(response);
   //
   //      Assert.assertTrue("Response should be 404", response.getCode() == 404);
   //      Assert.assertTrue("Should not retry more than 3 times", fr.getTotalRetries() <= 3);
   //
   //   }
   //
   //   static void _disableProxyAfter(final Proxy proxy, long delay)
   //   {
   //      Timer timer = new Timer();
   //      timer.schedule(new TimerTask()
   //         {
   //
   //            @Override
   //            public void run()
   //            {
   //               try
   //               {
   //                  proxy.disable();
   //               }
   //               catch (IOException e)
   //               {
   //                  e.printStackTrace();
   //               }
   //            }
   //         }, delay);
   //   }
   //
   //   static void _enableProxyAfter(final Proxy proxy, long delay)
   //   {
   //      Timer timer = new Timer();
   //      timer.schedule(new TimerTask()
   //         {
   //
   //            @Override
   //            public void run()
   //            {
   //               try
   //               {
   //                  proxy.enable();
   //               }
   //               catch (IOException e)
   //               {
   //                  e.printStackTrace();
   //               }
   //            }
   //         }, delay);
   //   }
   //
   //   class ProxyUpDown implements Runnable
   //   {
   //
   //      long    upSleep;
   //      long    downSleep;
   //      boolean run   = true;
   //      Proxy   proxy;
   //      boolean debug = false;
   //
   //      public ProxyUpDown(long upSleep, long downSleep, Proxy proxy, boolean debug)
   //      {
   //         super();
   //         this.upSleep = upSleep;
   //         this.downSleep = downSleep;
   //         this.proxy = proxy;
   //         this.debug = debug;
   //      }
   //
   //      public void stop()
   //      {
   //         run = false;
   //      }
   //
   //      @Override
   //      public void run()
   //      {
   //
   //         try
   //         {
   //            while (run)
   //            {
   //               if (proxy.isEnabled())
   //               {
   //                  System.out.println("[TestWebGet] --> Proxy going down");
   //                  proxy.disable();
   //                  Thread.sleep(downSleep);
   //               }
   //               else
   //               {
   //                  System.out.println("[TestWebGet] --> Proxy going up");
   //                  proxy.enable();
   //                  Thread.sleep(upSleep);
   //               }
   //
   //            }
   //         }
   //         catch (Exception e)
   //         {
   //            e.printStackTrace();
   //         }
   //
   //      }
   //
   //   }

}
