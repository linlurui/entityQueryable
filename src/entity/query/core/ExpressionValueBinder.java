/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */

package entity.query.core;

import entity.tool.util.ReflectionUtils;
import entity.tool.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for binding placeholder values (#{field}) inside SQL expressions
 * to the underlying entity instance so that Queryable can resolve them later.
 */
public final class ExpressionValueBinder {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("#\\{([\\w\\.]+)\\}");

    private ExpressionValueBinder() {
    }

    public static void bind(Class<?> clazz, Object entity, String expression, Object... values) {
        if (clazz == null || entity == null || StringUtils.isEmpty(expression) || values == null || values.length == 0) {
            return;
        }

        List<String> placeholders = extractPlaceholders(expression);
        if (placeholders.isEmpty()) {
            return;
        }

        List<String> targetFields = resolveTargets(placeholders, values.length);
        if (targetFields.size() != values.length) {
            throw new IllegalArgumentException(String.format(
                    "Placeholder count mismatch in expression '%s'. Expect %s value(s) but got %s.",
                    expression, targetFields.size(), values.length));
        }

        for (int i = 0; i < targetFields.size(); i++) {
            ReflectionUtils.setFieldValue(clazz, entity, targetFields.get(i), values[i]);
        }
    }

    private static List<String> extractPlaceholders(String expression) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(expression);
        List<String> placeholders = new ArrayList<String>();
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
        return placeholders;
    }

    private static List<String> resolveTargets(List<String> placeholders, int valueCount) {
        if (placeholders.size() == valueCount) {
            return placeholders;
        }

        LinkedHashSet<String> uniqueOrder = new LinkedHashSet<String>(placeholders);
        if (uniqueOrder.size() == valueCount) {
            return new ArrayList<String>(uniqueOrder);
        }

        return new ArrayList<String>();
    }
}

