/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.examples.google.computeengine;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jclouds.compute.config.ComputeServiceProperties.POLL_INITIAL_PERIOD;
import static org.jclouds.compute.config.ComputeServiceProperties.POLL_MAX_PERIOD;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.inject.Module;

public class CreateServerErrorExample implements Closeable {

	private static final String PROVIDER = "google-compute-engine";
	private static final String ZONE = "europe-west1-b";
	private static final String PROFILE = "f1-micro";
	// private static final String IMAGE = "debian-7-wheezy";
	private static final String IMAGE = "centos-7";
	private static final String NAME = "jclouds-example";
	private static final String POLL_PERIOD_TWENTY_SECONDS = String.valueOf(SECONDS.toMillis(20));

	private final ComputeService computeService;

	public static void main(final String[] args) {
		String serviceAccountEmailAddress = args[0];
		String serviceAccountKey = null;
		try {
			serviceAccountKey = Files.toString(new File(args[1]), Charset.defaultCharset());
		} catch (IOException e) {
			System.err.println("Cannot open service account private key PEM file: " + args[1] + "\n" + e.getMessage());
			System.exit(1);
		}

		CreateServerErrorExample createServer = new CreateServerErrorExample(serviceAccountEmailAddress,
				serviceAccountKey);

		try {
			createServer.createServer();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				createServer.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	public CreateServerErrorExample(final String serviceAccountEmailAddress, final String serviceAccountKey) {
		Properties overrides = new Properties();
		overrides.setProperty(POLL_INITIAL_PERIOD, POLL_PERIOD_TWENTY_SECONDS);
		overrides.setProperty(POLL_MAX_PERIOD, POLL_PERIOD_TWENTY_SECONDS);

		Iterable<Module> modules = ImmutableSet.<Module> of(new SshjSshClientModule());

		ComputeServiceContext context = ContextBuilder.newBuilder(PROVIDER)
				.credentials(serviceAccountEmailAddress, serviceAccountKey)
				.modules(modules)
				.overrides(overrides)
				.buildView(ComputeServiceContext.class);
		computeService = context.getComputeService();
	}

	private void createServer()
			throws RunNodesException, TimeoutException, IOException {

		Template template = computeService.templateBuilder()
				.locationId(ZONE)
				.hardwareId(getHardware().getId())
				.imageId(getImage().getId())
				.build();

		Set<? extends NodeMetadata> nodes = computeService.createNodesInGroup(NAME, 1, template);

		NodeMetadata nodeMetadata = nodes.iterator().next();
		String publicAddress = nodeMetadata.getPublicAddresses().iterator().next();

		System.out.format("  %s%n", nodeMetadata);
		System.out.format("  Instance %s started with IP %s%n", nodeMetadata.getName(), publicAddress);
		System.out.format("  Username %s", nodeMetadata.getCredentials().identity);
		System.out.format("  Key %s ", nodeMetadata.getCredentials().credential);
	}

	private Hardware getHardware() {
		for (Hardware profile : computeService.listHardwareProfiles()) {
			if (ZONE.equals(profile.getLocation().getId()) && PROFILE.equals(profile.getName())) {
				return profile;
			}
		}
		return null;
	}

	private Image getImage() {
		for (Image image : computeService.listImages()) {
			if (image.getName().startsWith(IMAGE)) {
				return image;
			}
		}
		return null;
	}

	@Override
	public final void close() throws IOException {
		Closeables.close(computeService.getContext(), true);
	}
}
