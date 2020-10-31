package krasa.ttcmonitor.service;

import javafx.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Arrays;
import java.util.List;

@Component
public class ProxyProvider {

	Pair<String, Integer> proxy = new Pair<>("localhost", 9150);

	public InetSocketAddress getProxy() {
		return new InetSocketAddress(proxy.getKey(), proxy.getValue());
	}

	public static void main(String[] args) {

		SimpleClientHttpRequestFactory torRequestFactory = new SimpleClientHttpRequestFactory();
		final ProxyProvider proxyProvider = new ProxyProvider();
		proxyProvider.next();
		proxyProvider.next();
		proxyProvider.next();
		;
		torRequestFactory.setProxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("88.100.162.153", 4145)));
		RestTemplate torRestTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(torRequestFactory));
		ResponseEntity<String> forEntity = torRestTemplate.getForEntity("https://www.google.com", String.class);
		System.err.println(forEntity);

	}

	int i = 0;

	public Pair<String, Integer> next() {

		List<String> strings = Arrays.asList("91.217.96.1:56636",
			"77.48.137.3:50523",
			"91.217.96.169:56636",
			"91.217.96.25:56636",
			"89.24.210.10:4145",
			"89.102.198.78:45399",
			"88.146.204.49:4153",
			"77.48.137.65:50523",
			"80.95.109.6:4145",
			"85.163.0.37:4145",
			"93.99.53.201:48577",
			"80.188.239.106:40695",
			"217.77.171.114:4145",
			"62.168.57.109:41983",
			"89.24.119.126:4145",
			"195.39.6.80:4145",
			"84.244.67.101:4145",
			"90.183.152.178:4145",
			"193.85.228.182:47747",
			"90.181.150.210:4145",
			"82.142.87.2:4145",
			"109.238.223.67:61150",
			"109.238.219.225:4153",
			"90.183.158.50:44964",
			"109.238.223.1:51372",
			"89.233.183.64:4145",
			"109.238.208.130:4153",
			"109.238.222.1:4153",
			"90.181.150.211:4145",
			"178.77.197.96:33555",
			"62.201.17.106:51178",
			"185.63.96.216:4145",
			"87.249.142.66:4145",
			"80.188.204.250:4145",
			"92.62.229.4:1080");
		String s = strings.get(i);
		String[] split = s.split(":");

		proxy = new Pair<String, Integer>(split[0], Integer.parseInt(split[1]));

		i++;
		if (i > strings.size() - 1) {
			i = 0;
		}
		return proxy;
	}
}
