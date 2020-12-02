/**
 * Copyright 2019 Pramati Prism, Inc.
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
package com.github.srujankujmar.deployer.services.util;

import java.util.ArrayList;
import java.util.List;

import com.github.srujankujmar.deployer.services.model.ServiceAddress;
import io.kubernetes.client.openapi.models.V1LoadBalancerIngress;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;

/**
 * Utility to process information from {@link V1Service}
 *
 */
public class K8sServiceUtil {

	public static ServiceAddress getServiceAddress(V1Service service) {
		if (service == null) {
			return null;
		}
		ServiceAddress serviceAddress = new ServiceAddress();
		V1LoadBalancerIngress loadBalancerIngress = getLoadBalancer(service);
		if (loadBalancerIngress != null) {
			String host = loadBalancerIngress.getIp() == null ? loadBalancerIngress.getHostname()
					: loadBalancerIngress.getIp();
			serviceAddress.setServiceIP(host);
		}
		List<Integer> ports = getPorts(service);
		serviceAddress.setPorts(ports);

		return serviceAddress;
	}

	public static V1LoadBalancerIngress getLoadBalancer(V1Service lbSvc) {
		V1LoadBalancerIngress loadBalancerIngress = null;
		if (lbSvc == null || lbSvc.getStatus() == null || lbSvc.getStatus().getLoadBalancer() == null) {
			return loadBalancerIngress;
		}
		List<V1LoadBalancerIngress> ingressList = lbSvc.getStatus().getLoadBalancer().getIngress();
		if (ingressList != null && !ingressList.isEmpty()) {
			loadBalancerIngress = ingressList.get(0);
		}
		return loadBalancerIngress;
	}

    public static List<Integer> getPorts(V1Service service) {
	if (service == null || service.getSpec() == null) {
	    return null;
	}
	List<V1ServicePort> v1ServicePorts = service.getSpec().getPorts();
	if (v1ServicePorts == null || v1ServicePorts.isEmpty()) {
	    return null;
	}
	List<Integer> portsList = new ArrayList<Integer>();

	v1ServicePorts.forEach(each -> {
	    if (each != null && each.getPort() != null) {
		portsList.add(each.getPort());
	    }
	});
	return portsList;
    }
}
