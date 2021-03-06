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
package com.github.srujankujmar.troubleshooting.integration.actions;

import com.github.srujankujmar.troubleshooting.integration.models.AbstractedErrorMessage;
import com.github.srujankujmar.troubleshooting.integration.models.ActionNode;
import com.github.srujankujmar.troubleshooting.integration.models.DiagnosisReport;
import com.github.srujankujmar.troubleshooting.integration.models.TroubleshootingContext;
import org.springframework.stereotype.Component;

/*
 * Prepares a diagnosis report stating that no service has been
 * deployed in the cluster. Useful for cases when a wrong service
 * name in unintendedly provided.
 */
@Component
public class ServiceNotDeployedAction extends ActionNode<TroubleshootingContext> {

    @Override
    public void process(TroubleshootingContext context) {
        DiagnosisReport report = new DiagnosisReport();
        report.setReason(AbstractedErrorMessage.SERVICE_NOT_DEPLOYED.getReason());
        report.setRecommendedFix(AbstractedErrorMessage.SERVICE_NOT_DEPLOYED.getMessage());
        context.addReport(report);
    }

    @Override
    public String describe() {
        return "Service is not deployed";
    }
}
