package com.nick;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryManager;

@Component
public class AppSimpleEurekaService {
	private static final DynamicPropertyFactory configInstance = com.netflix.config.DynamicPropertyFactory
			.getInstance();

	private static final Logger LOGGER = LoggerFactory
			.getLogger(AppSimpleEurekaService.class);

	private void dumpConfigs() {

		String propsfile = configInstance.getStringProperty(
				"eureka.client.props", "unknown").get();

		LOGGER.info("props file name: " + propsfile);

		String microServiceName = configInstance.getStringProperty(
				"eureka.name", "unknown").get();

		LOGGER.info("micro service name: " + microServiceName);

		String microServiceVipAddress = configInstance.getStringProperty(
				"eureka.vipAddress", "unknown").get();

		LOGGER.info("micro service vip address: " + microServiceVipAddress);

		String microServicePort = configInstance.getStringProperty(
				"eureka.port", "unknown").get();

		LOGGER.info("micro service port: " + microServicePort);

		String eurekaServerUrl = configInstance.getStringProperty(
				"eureka.serviceUrl.default", "unknown").get();

		LOGGER.info("eureka server url: " + eurekaServerUrl);

	}

	@PostConstruct
	public void registerWithEureka() {

		dumpConfigs();

		int sleepSeconds = 10; // Application initialization and running
								// simulation time

		// Register with Eureka
		DiscoveryManager.getInstance().initComponent(
				new MyDataCenterInstanceConfig(),
				new DefaultEurekaClientConfig());

		// A good practice is to register as STARTING and only change status to
		// UP
		// after the service is ready to receive traffic
		LOGGER.info("Registering service simple to eureka with STARTING status");

		ApplicationInfoManager.getInstance().setInstanceStatus(
				InstanceStatus.STARTING);

		LOGGER.info("Simulating service initialization by sleeping for "
				+ sleepSeconds + " seconds...");
		try {
			Thread.sleep(sleepSeconds * 1000);
		} catch (InterruptedException e) {
			// Nothing
		}

		// Now we change our status to UP
		LOGGER.info("Done sleeping, now changing status to UP");
		ApplicationInfoManager.getInstance().setInstanceStatus(
				InstanceStatus.UP);

		String vipAddress = configInstance.getStringProperty(
				"eureka.vipAddress", "simple.mydomain.net").get();

		InstanceInfo nextServerInfo = null;
		while (nextServerInfo == null) {
			try {
				nextServerInfo = DiscoveryManager.getInstance()
						.getDiscoveryClient()
						.getNextServerFromEureka(vipAddress, false);
			} catch (Throwable e) {
				LOGGER.error("Waiting for service to register with eureka..");

				try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
					LOGGER.error(e1.getMessage(), e1);
				}

			}
		}

		LOGGER.info("Service started and ready to process requests..");

	}

	@PreDestroy
	public void unRegisterWithEureka() {
		// Un register from eureka.
		DiscoveryManager.getInstance().shutdownComponent();
	}

}
