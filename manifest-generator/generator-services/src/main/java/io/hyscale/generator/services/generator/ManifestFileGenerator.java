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
package com.github.srujankujmar.generator.services.generator;

import java.io.File;

import com.github.srujankujmar.commons.models.YAMLManifest;
import com.github.srujankujmar.commons.utils.NormalizationUtil;
import com.github.srujankujmar.generator.services.constants.ManifestGenConstants;
import com.github.srujankujmar.generator.services.exception.ManifestErrorCodes;
import com.github.srujankujmar.plugin.framework.models.ManifestMeta;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.io.HyscaleFilesUtil;

/**
 * Generate Manifest File with yaml
 *
 * @author tushart
 */
@Component
public class ManifestFileGenerator {

	public YAMLManifest getYamlManifest(String manifestDir, String yaml, ManifestMeta manifestMeta)
			throws HyscaleException {
		if (StringUtils.isBlank(yaml) || StringUtils.isBlank(manifestDir)) {
			throw new HyscaleException(ManifestErrorCodes.ERROR_WHILE_WRITING_MANIFEST_TO_FILE, manifestDir);
		}
		StringBuilder sb = new StringBuilder(manifestDir);
		sb.append(getManifestFileName(manifestMeta));
		sb.append(ManifestGenConstants.YAML_EXTENSION);
		File manifestFile = HyscaleFilesUtil.createFile(sb.toString(), yaml);
		YAMLManifest yamlManifest = new YAMLManifest();
		yamlManifest.setYamlManifest(manifestFile);
		return yamlManifest;
	}

	private String getManifestFileName(ManifestMeta manifestMeta) {
		StringBuilder sb = new StringBuilder();
		if (!StringUtils.isBlank(manifestMeta.getIdentifier())) {
			sb.append(NormalizationUtil.normalize(manifestMeta.getIdentifier()));
			sb.append(ManifestGenConstants.NAME_DELIMITER);
		}
		sb.append(NormalizationUtil.normalize(manifestMeta.getKind()));
		return sb.toString();
	}

}
