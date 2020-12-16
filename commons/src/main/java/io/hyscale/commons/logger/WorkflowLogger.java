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
package com.github.srujankujmar.commons.logger;

import com.github.srujankujmar.commons.models.Activity;
import com.github.srujankujmar.commons.models.Status;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("java:S106")
public class WorkflowLogger {

    public static final Integer LEFT_ALIGNED_PADDING = 82;

    private static final String STARS = "**************************";
    private static final String CONTINUATION_DOTS = ".";
    private static final String PADDING_DOT = ".";
    private static final String ALIGNEMENT_SPACES = "  ";
    private static final Integer PADDING = 50;
    private static final String START_BRACES = "[";
    private static final String END_BRACES = "]";
    private static final List<String> persistedActivities = new ArrayList<>();

    private static boolean disabled;

    private WorkflowLogger() {}

    public static void header(Activity activity, String... args) {
        if(isDisabled()){
            return;
        }
        System.out.println();
        System.out.println(STARS + getPaddedHeader(String.format(getActivity(activity), args)) + STARS);
        System.out.println();
    }

    public static void footer() {
        if(isDisabled()){
            return;
        }
        System.out.println();
        System.out.println(STARS + STARS + STARS + STARS);
    }

    public static void info(Activity activity, String... args) {
        logActivity(activity, LoggerTags.USER_INFO_TAG, args);
    }
    
    public static void debug(Activity activity, String... args) {
        logActivity(activity, LoggerTags.DEBUG, args);
    }

    public static void verbose(Activity activity, String... args) {
        logActivity(activity, LoggerTags.VERBOSE, args);
    }

    public static void error(Activity activity, String... args) {
        logActivity(activity, LoggerTags.ERROR, args);
    }

    public static void warn(Activity activity, String... args) {
        logActivity(activity, LoggerTags.WARN, args);
    }
    
    public static void action(Activity activity, String... args) {
        logActivity(activity, LoggerTags.ACTION, args);
    }

    public static void startActivity(Activity activity, String... args) {
        if(isDisabled()){
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(LoggerTags.ACTION.getTag()).append(ALIGNEMENT_SPACES);
        sb.append(getLeftAlignedActivity(getFormattedMessage(activity, args)));
        System.out.print(sb.toString());
    }

    public static void continueActivity() {
        if(isDisabled()){
            return;
        }
        System.out.print(CONTINUATION_DOTS);
    }

    public static void endActivity(Status status, String... args) {
        if(isDisabled()){
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(ALIGNEMENT_SPACES).append(START_BRACES);
        sb.append(String.format(status.getMessage(), args));
        sb.append(END_BRACES);
        System.out.println(sb.toString());
    }

    public static void logTable(TableFormatter tableFormatter) {
        if(isDisabled()){
            return;
        }
        System.out.print(tableFormatter.toString());
    }

    public static void logTableRow(TableFormatter tableFormatter, String[] row) {
        if(isDisabled()){
            return;
        }
        System.out.println(tableFormatter.getFormattedRow(row));
    }

    public static void logTableFields(TableFormatter tableFormatter) {
        if(isDisabled()){
            return;
        }
        System.out.println(tableFormatter.getFormattedFields());
    }

    private static String getActivityMessage(Activity activity, LoggerTags tag, String... args) {
        StringBuilder sb = new StringBuilder();
        if (tag != null) {
            sb.append(tag.getTag());
            sb.append(ALIGNEMENT_SPACES);
        }
        sb.append(getFormattedMessage(activity, args));
        return sb.toString();
    }

    private static void logActivity(Activity activity, LoggerTags tag, String... args) {
        if(isDisabled()){
            return;
        }
        System.out.println(getActivityMessage(activity, tag, args));

    }

    private static String getFormattedMessage(Activity activity, String... args) {
        if (activity == null || activity.getActivityMessage() == null) {
            return "";
        }
        return args != null && args.length != 0 ? String.format(getActivity(activity), args)
                : activity.getActivityMessage();
    }

    private static String getActivity(Activity activity) {
        return activity.getActivityMessage().replaceAll("\\{\\}", "%s");
    }

    private static String getPaddedHeader(String input) {
        int length = input.length();
        if (length > PADDING) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input);
        int pads = (PADDING - length) / 2;
        if ((PADDING - length) % 2 != 0) {
            sb.append(" ");
        }
        for (int i = 0; i < pads; i++) {
            sb.append(" ");
            sb.insert(0, ' ');
        }

        return sb.toString();
    }

    private static String getLeftAlignedActivity(String input) {
        int length = input.length();
        if (length > LEFT_ALIGNED_PADDING) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input);
        int pads = (LEFT_ALIGNED_PADDING - length);
        for (int i = 0; i < pads; i++) {
            sb.append(PADDING_DOT);
        }
        return sb.toString();
    }

    public static void startActivity(ActivityContext context, String... args) {
        if(isDisabled()){
            return;
        }
        if (context != null) {
            context.setStartTime(System.currentTimeMillis());
            StringBuilder sb = new StringBuilder();
            sb.append(LoggerTags.ACTION.getTag()).append(ALIGNEMENT_SPACES);
            sb.append(getFormattedMessage(context.getStartActivity(), args));
            System.out.print(sb.toString());
        }
    }

    public static void continueActivity(ActivityContext context) {
        if(isDisabled()){
            return;
        }
        if (context != null) {
            int remaining = context.getRemaining();
            if (context.getRemaining() > 0) {
                System.out.print(CONTINUATION_DOTS);
                context.setRemaining(--remaining);
            }
        } else {
            continueActivity();
        }
    }

    public static void endActivity(ActivityContext context, Status status, String... args) {
        if(isDisabled()){
            return;
        }
        if (context != null) {
            int remaining = context.getRemaining();
            if (remaining > 0) {
                for (int i = 0; i < remaining; i++) {
                    System.out.print(CONTINUATION_DOTS);
                }
                context.setRemaining(0);
            }
        }
        endActivity(status, args);
    }

    public static void persist(Activity activity, String... args) {
        persist(activity, LoggerTags.WARN, args);
    }
    
    public static void persist(Activity activity, LoggerTags loggerTags, String... args) {
        if (activity != null) {
            persistedActivities.add(getActivityMessage(activity, loggerTags, args));
        }
    }

    public static void logPersistedActivities() {
        if(isDisabled()){
            return;
        }
        if (!persistedActivities.isEmpty()) {
            persistedActivities.stream().filter(StringUtils::isNotBlank).forEach(System.out::println);
            System.out.println();
        }
        persistedActivities.clear();
    }

    public static boolean isDisabled() {
        return disabled;
    }

    public static void setDisabled(boolean disabled) {
        WorkflowLogger.disabled = disabled;
    }

    public static void log(String message) {
        if(isDisabled()){
            return;
        }
        System.out.println(message);
    }

}
