package krasa.ttcmonitor.service;

import krasa.ttcmonitor.commons.LoggingRequestInterceptor;
import krasa.ttcmonitor.commons.MyException;
import krasa.ttcmonitor.controller.model.EsoItem;
import krasa.ttcmonitor.controller.model.EsoWatch;
import org.apache.commons.codec.Charsets;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.GZIPInputStream;

@Service
public class MonitoringService implements DisposableBean {
	private static final Logger LOG = LoggerFactory.getLogger(MonitoringService.class);

	private volatile int requests = 0;
	private volatile LocalTime last;
	private RestTemplate restTemplate;
	private HttpComponentsClientHttpRequestFactory requestFactory;
	private final RestTemplate torRestTemplate;

	static class MyConnectionSocketFactory extends SSLConnectionSocketFactory {

		public MyConnectionSocketFactory(final SSLContext sslContext) {
			super(sslContext);
		}

		@Override
		public Socket createSocket(final HttpContext context) throws IOException {
			InetSocketAddress socksaddr = (InetSocketAddress) context.getAttribute("socks.address");
			Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksaddr);
			return new Socket(proxy);
		}

	}

	@Autowired
	public MonitoringService(ProxyProvider proxyProvider) {
		SimpleClientHttpRequestFactory torRequestFactory = new SimpleClientHttpRequestFactory();
		torRequestFactory.setProxy(new Proxy(Proxy.Type.SOCKS, proxyProvider.getProxy()) {
			@Override
			public SocketAddress address() {
				return proxyProvider.getProxy();
			}
		});
		torRestTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(torRequestFactory));


//		try {
//			Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory>create()
//					.register("http", PlainConnectionSocketFactory.INSTANCE)
//					.register("https", new MyConnectionSocketFactory(SSLContext.getDefault()))
//					.build();
//			PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(reg);
//			CloseableHttpClient httpclient = HttpClients.custom()
//					.setConnectionManager(cm).addInterceptorFirst(new HttpRequestInterceptor() {
//						@Override
//						public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
//							InetSocketAddress socksaddr = new InetSocketAddress("localhost", 9150);
//							context.setAttribute("socks.address", socksaddr);
//						}
//					})
//					.build();
//			requestFactory = new HttpComponentsClientHttpRequestFactory(httpclient);
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
////
		requestFactory = new HttpComponentsClientHttpRequestFactory();
		restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(requestFactory));
//		torRestTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		interceptors.add(new LoggingRequestInterceptor());
		restTemplate.setInterceptors(interceptors);
		torRestTemplate.setInterceptors(interceptors);

		torRestTemplate.setMessageConverters(List.of(new StringHttpMessageConverter() {
			private Charset getContentTypeCharset(@Nullable MediaType contentType) {
				if (contentType != null && contentType.getCharset() != null) {
					return contentType.getCharset();
				} else {
					Charset charset = getDefaultCharset();
					Assert.state(charset != null, "No default charset");
					return charset;
				}
			}

			@Override
			protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage) throws IOException {
				if ("gzip".equals(inputMessage.getHeaders().getFirst("Content-Encoding"))) {
					final GZIPInputStream gis = new GZIPInputStream(inputMessage.getBody());
					Charset charset = getContentTypeCharset(inputMessage.getHeaders().getContentType());
					return StreamUtils.copyToString(gis, charset);
				}
				return super.readInternal(clazz, inputMessage);
			}
		}));
		;
	}

	public List<EsoItem> process(EsoWatch esoWatch, boolean tor) {
		LOG.info("Fetching: " + esoWatch.toString());
		String esoWatchLink = esoWatch.getLink();
//		Integer random = new Random().nextInt(100);
//		esoWatchLink = esoWatchLink.replace("PriceMax=1501", "PriceMax=" + (1500 + random));
//		esoWatchLink = esoWatchLink.replace("PriceMax=3001", "PriceMax=" + (3001 + random));

		List<EsoItem> esoItems = new ArrayList<>();
		RequestEntity.HeadersBuilder<?> headersBuilder = RequestEntity.get(URI.create(esoWatchLink));
		headersBuilder.header("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:70.0) Gecko/20100101 Firefox/70.0");
		headersBuilder.header("Cache-control", "max-age=0");
		headersBuilder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		headersBuilder.header("Accept-Encoding", "gzip, deflate, br");
		headersBuilder.header("Host", "eu.tamrieltradecentre.com");
		headersBuilder.header("Referer", "https://eu.tamrieltradecentre.com/pc/Trade/Search");
		RequestEntity<Void> build = headersBuilder.build();
		LOG.info("Request: " + esoWatchLink);
		ResponseEntity<String> url = getRestTemplate(tor).exchange(build, String.class);
//		ResponseEntity<String> url = getRestTemplate().execute(esoWatchLink, HttpMethod.GET, null, new ResponseExtractor<ResponseEntity<String>>() {
//			@Override
//			public ResponseEntity<String> extractData(ClientHttpResponse response) throws IOException {
//					try {
//						GZIPInputStream gzipInputStream = new GZIPInputStream(response.getBody());
//						String bodyHtml = IOUtils.toString(gzipInputStream, "UTF-8");
//						System.err.println(bodyHtml);
//					} catch (IOException e) {
//						throw new RuntimeException(e);
//					}
//				return null;
//			}
//		});
		LOG.info("Response: " + url.getStatusCode());

		if (url.getStatusCode() != HttpStatus.OK) {
			throw new MyException("Response: " + url.getStatusCode() + " for " + esoWatch + ": " + url.toString(), esoWatch);
		}

		String bodyHtml = url.getBody();

		String s = bodyHtml != null ? bodyHtml.toLowerCase() : "";
		if (s.contains("sorry") || s.contains("captcha")) {
			throw new RuntimeException("Captcha!! " + url.toString());
		}
		if (s.contains("not valid")) {
			throw new RuntimeException("not valid!! " + url.toString());
		}


		Map<String, String> params = urlParams(esoWatchLink);

		Document doc = Jsoup.parseBodyFragment(bodyHtml);
		Element body = doc.body();
		Elements select = body.select("tr[cursor-pointer]");
		Elements links = doc.select("tr.cursor-pointer"); // a with href
		Iterator<Element> iterator = links.iterator();
		while (iterator.hasNext()) {
			Element next = iterator.next();
			Elements td = next.select("td");
			List<Element> c = td.subList(0, td.size());

			Element nameElement = c.get(0);
			Element locationElement = c.get(2);
			Element priceElement = c.get(3);
			Element sinceElement = c.get(4);
			String traits = nameElement.selectFirst("img").attr("data-trait");

			String category1 = params.get("ItemCategory1ID") != null ? MetadataManager.categories1.get(params.get("ItemCategory1ID")).name : null;
			String category2 = params.get("ItemCategory2ID") != null ? MetadataManager.categories2.get(params.get("ItemCategory2ID")).name : null;
			String category3 = params.get("ItemCategory3ID") != null ? MetadataManager.categories3.get(params.get("ItemCategory3ID")).name : null;

			String link = "https://eu.tamrieltradecentre.com" + next.attr("data-on-click-link");
			String quality = nameElement.selectFirst("div").attr("class");
			String name = nameElement.selectFirst("div").text();
			String level = nameElement.selectFirst("div").nextElementSibling().text().replace("Level: ", "");
			String location = "-";
			if (locationElement.childNodeSize() > 1) {
				location = locationElement.child(0).text() + " --> " + locationElement.child(1).text();
			}
			String price = priceElement.text();
			String elapsed = sinceElement.attr("data-mins-elapsed");


			EsoItem esoItem = new EsoItem();
			esoItem.setTrait(traits);
			esoItem.setCategory1(category1);
			esoItem.setCategory2(category2);
			esoItem.setCategory3(category3);
			esoItem.setLink(link);
			esoItem.setQuality(quality);
			esoItem.setName(name);
			esoItem.setLocation(location);
			esoItem.setPrice(price.replace("=", "="));
			esoItem.setLevel(level);
			LocalDateTime localTime = LocalDateTime.now().minusMinutes(Long.parseLong(elapsed));
			esoItem.setCreated(localTime.format(DateTimeFormatter.ofPattern("dd.MM. kk:mm")));

			esoItem.setElapsed(elapsed);
			esoItems.add(esoItem);
		}

		requests++;
		last = LocalTime.now();
		LOG.info("Items: " + esoItems.size());
		esoWatch.setLastCheck(LocalDateTime.now());
		return esoItems;
	}

	private RestTemplate getRestTemplate(boolean tor) {
		if (tor) {
			return torRestTemplate;
		} else {
			return restTemplate;
		}
	}

	private Map<String, String> urlParams(String link) {
		List<NameValuePair> parse = URLEncodedUtils.parse(link, Charsets.UTF_8);
		Map<String, String> params = new HashMap<>();
		for (NameValuePair nameValuePair : parse) {
			params.put(nameValuePair.getName(), nameValuePair.getValue());
		}
		return params;
	}

	public int getRequests() {
		return requests;
	}

	public LocalTime getLast() {
		return last;
	}

	@Override
	public void destroy() throws Exception {
		requestFactory.destroy();
	}
}
