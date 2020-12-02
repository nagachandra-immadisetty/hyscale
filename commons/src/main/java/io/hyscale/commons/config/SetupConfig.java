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
package com.github.srujankujmar.commons.config;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class SetupConfig {

    public static final String HYSCALE_CTL_HOME = "HYSCALECTL_HOME";
    public static final String USER_HOME_DIR = System.getProperty("user.home");
    public static final String CURRENT_WORKING_DIR = System.getProperty("user.dir");
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    public static final String INSTALLATION_DIR = USER_HOME_DIR;
    public static final String HYSCALECTL_HOME_DIR = System.getenv(HYSCALE_CTL_HOME);
    public static final String KUBECONF_PATH_PROP = "HYSCALECTL_KUBECONF";
    public static final String KUBECONF_PATH_DIR = System.getenv(KUBECONF_PATH_PROP);
    public static final String DOCKERCONF_PATH_PROP = "HYSCALECTL_DOCKERCONF";
    public static final String DOCKERCONF_PATH_DIR = System.getenv(DOCKERCONF_PATH_PROP);


    private static final String generatedFilesDir = "generated-files";
    private static final String logDir = "logs";
    private static final String appsDirectory = "apps";
    private static final String hyscale = "hyscale";

    private static final ThreadLocal<String> absolutePathTL = new ThreadLocal<String>();

    public static void setAbsolutePath(String path) {
        if (StringUtils.isNotBlank(path)) {
            if (absolutePathTL.get() == null) {
                absolutePathTL.set(path);
            }
        }
    }

    public static String getToolLogDir() {
        return INSTALLATION_DIR + FILE_SEPARATOR + hyscale + FILE_SEPARATOR + logDir;
    }

    private static String getAbsolutePath() {
        if (absolutePathTL.get() != null) {
            return absolutePathTL.get() + FILE_SEPARATOR;
        }
        return absolutePathTL.get();
    }

    public static void clearAbsolutePath(){
        if(absolutePathTL!=null){
            absolutePathTL.remove();
        }
    }

    /*
     * 1. When the path is absolute return the absolute path itself 2. When the path
     * is not absolute and use service spec absolute path , the source path is
     * expected to be relative to the service spec 3. When the path is not absolute
     * and service spec absolute path does not exist, the source path is expected to
     * be relative to the current working directory
     */

    public static String getAbsolutePath(String source) {
        if (StringUtils.isBlank(source)) {
            return CURRENT_WORKING_DIR;
        }
        File sourceFile = new File(source);
        if (sourceFile.isAbsolute()) {
            return source;
        } else if (StringUtils.isNotBlank(getAbsolutePath())) {
            return getAbsolutePath() + source;
        }
        return CURRENT_WORKING_DIR + FILE_SEPARATOR + source;
    }

    public String getInstallationDir() {
        return INSTALLATION_DIR + FILE_SEPARATOR;
    }

    public String getAppsDir() {
        return getInstallationDir() + hyscale + FILE_SEPARATOR + appsDirectory + FILE_SEPARATOR;
    }

    public String getServiceDir(String appName, String serviceName) {
        StringBuilder sb = new StringBuilder();
        sb.append(getAppsDir()).append(appName).append(FILE_SEPARATOR).append(serviceName).append(FILE_SEPARATOR);
        return sb.toString();
    }

    public String getGeneratedFilesDir(String appName, String serviceName) {
        return getServiceDir(appName, serviceName) + generatedFilesDir + FILE_SEPARATOR;
    }

    public String getLogsDir(String appName, String serviceName) {
        return getServiceDir(appName, serviceName) + logDir + FILE_SEPARATOR;
    }

    public static String getMountPathOf(String dir) {
        /*if (StringUtils.isNotBlank(dir) && StringUtils.isNotBlank(HYSCALECTL_HOME_DIR)) {
            String hyscaleCtlHomeDir = HYSCALECTL_HOME_DIR;
            if (!hyscaleCtlHomeDir.endsWith(FILE_SEPARATOR)) {
                hyscaleCtlHomeDir += FILE_SEPARATOR;
            }
            String userHomeDir = USER_HOME_DIR;
            if (!userHomeDir.endsWith(FILE_SEPARATOR)) {
                userHomeDir += FILE_SEPARATOR;
            }
            if (dir.contains(userHomeDir)) {
                return dir.replace(userHomeDir, hyscaleCtlHomeDir);
            }
        }
        return dir;*/
        return getMountPathOf(dir, HYSCALECTL_HOME_DIR);

    }

    public static String getMountPathOfKubeConf(String dir) {
        return StringUtils.isNotBlank(KUBECONF_PATH_DIR) ? KUBECONF_PATH_DIR : dir;
    }

    public static String getMountOfDockerConf(String dir) {
        return StringUtils.isNotBlank(DOCKERCONF_PATH_DIR) ? DOCKERCONF_PATH_DIR : dir;
    }

    private static String getMountPathOf(String dir, String source) {
        if (StringUtils.isNotBlank(dir) && StringUtils.isNotBlank(source)) {
            String sourceDir = source;
            if (!sourceDir.endsWith(FILE_SEPARATOR)) {
                sourceDir += FILE_SEPARATOR;
            }
            String userHomeDir = USER_HOME_DIR;
            if (!userHomeDir.endsWith(FILE_SEPARATOR)) {
                userHomeDir += FILE_SEPARATOR;
            }
            if (dir.contains(userHomeDir)) {
                return dir.replace(userHomeDir, sourceDir);
            }
        }
        return dir;
    }
}
