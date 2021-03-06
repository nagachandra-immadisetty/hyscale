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

import com.github.srujankujmar.troubleshooting.integration.models.*;
import org.springframework.stereotype.Component;

@Component
public class ContactClusterAdministratorAction extends ActionNode<TroubleshootingContext> {

    @Override
    public void process(TroubleshootingContext context) {
        DiagnosisReport report = new DiagnosisReport();
        report.setReason(AbstractedErrorMessage.CONTACT_CLUSTER_ADMINISTRATOR.getReason());
        report.setRecommendedFix(AbstractedErrorMessage.CONTACT_CLUSTER_ADMINISTRATOR.getMessage());
        context.addReport(report);
    }

    @Override
    public String describe() {
        return "Please contact cluster administrator";
    }

}
