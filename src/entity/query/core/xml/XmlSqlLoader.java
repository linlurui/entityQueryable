
/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */


package entity.query.core.xml;

import entity.query.core.ApplicationConfig;
import entity.query.core.PreparedSql;
import entity.tool.util.CollectionUtils;
import entity.tool.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight XML SQL loader that understands a subset of MyBatis dynamic SQL
 * tags without depending on the MyBatis runtime.
 */
public final class XmlSqlLoader {

    private static final Logger log = LoggerFactory.getLogger(XmlSqlLoader.class);
    private static final String DEFAULT_ROOT = "mapper";
    private static final String ROOT_KEY = "${entity.mybatis.mapperRoot}";

    private static final Map<String, XmlMappedStatement> STATEMENTS = new ConcurrentHashMap<String, XmlMappedStatement>();
    private static final Map<String, SqlNode> FRAGMENTS = new ConcurrentHashMap<String, SqlNode>();
    private static final Set<String> LOADED_RESOURCES = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static volatile boolean initialized = false;

    private XmlSqlLoader() {
    }

    public static PreparedSql getPreparedSql(String statementId, Object parameterObject) {
        if (StringUtils.isEmpty(statementId)) {
            throw new IllegalArgumentException("Statement id can not be empty");
        }
        ensureInitialized();
        XmlMappedStatement statement = STATEMENTS.get(statementId);
        if (statement == null) {
            throw new IllegalArgumentException("Statement not found: " + statementId);
        }
        return statement.build(parameterObject);
    }

    private static void ensureInitialized() {
        if (initialized) {
            return;
        }
        synchronized (STATEMENTS) {
            if (initialized) {
                return;
            }
            loadMapperXmls();
            initialized = true;
        }
    }

    private static void loadMapperXmls() {
        String rootSetting = ApplicationConfig.getInstance().get(ROOT_KEY, DEFAULT_ROOT);
        if (StringUtils.isEmpty(rootSetting)) {
            log.warn("No mapper root configured via {}", ROOT_KEY);
            return;
        }
        String[] roots = rootSetting.split("[,;]");
        for (String root : roots) {
            String trimmed = root == null ? "" : root.trim();
            if (StringUtils.isEmpty(trimmed)) {
                continue;
            }
            List<File> xmlFiles = resolveXmlFiles(trimmed);
            for (File xmlFile : xmlFiles) {
                parseMapper(xmlFile);
            }
        }
    }

    private static List<File> resolveXmlFiles(String location) {
        List<File> results = new ArrayList<File>();
        if (location.startsWith("classpath:")) {
            String relative = location.substring("classpath:".length());
            URL resource = Thread.currentThread().getContextClassLoader().getResource(relative);
            if (resource != null && "file".equalsIgnoreCase(resource.getProtocol())) {
                File root = new File(resource.getFile());
                collectXmlFiles(root, results);
            } else {
                log.warn("Unsupported mapper location '{}'. Only file protocol classpath resources are supported.", location);
            }
            return results;
        }
        File root = new File(location);
        if (!root.isAbsolute()) {
            root = new File(System.getProperty("user.dir"), location);
        }
        if (!root.exists()) {
            log.warn("Mapper location '{}' does not exist.", root.getAbsolutePath());
            return results;
        }
        if (root.isFile()) {
            if (root.getName().endsWith(".xml")) {
                results.add(root);
            }
            return results;
        }
        collectXmlFiles(root, results);
        return results;
    }

    private static void collectXmlFiles(File directory, List<File> target) {
        if (directory == null || !directory.exists()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                collectXmlFiles(file, target);
                continue;
            }
            if (file.getName().endsWith(".xml")) {
                target.add(file);
            }
        }
    }

    private static void parseMapper(File xmlFile) {
        if (xmlFile == null) {
            return;
        }
        String resource = xmlFile.getAbsolutePath();
        if (!LOADED_RESOURCES.add(resource)) {
            return;
        }
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(xmlFile);
            Document document = buildDocument(inputStream);
            if (document == null) {
                log.warn("Skip mapper {} because document is empty", resource);
                return;
            }
            Element root = document.getDocumentElement();
            if (root == null || !"mapper".equals(root.getNodeName())) {
                log.warn("Skip mapper {} because root element is not <mapper>", resource);
                return;
            }
            String namespace = root.getAttribute("namespace");
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (!(node instanceof Element)) {
                    continue;
                }
                Element element = (Element) node;
                String nodeName = element.getNodeName();
                if ("sql".equals(nodeName)) {
                    String id = element.getAttribute("id");
                    if (StringUtils.isEmpty(id)) {
                        log.warn("Skip <sql> without id in {}", resource);
                        continue;
                    }
                    SqlNode sqlNode = parseChildren(element, namespace);
                    FRAGMENTS.put(qualifyId(namespace, id), sqlNode);
                    continue;
                }
                if (isStatement(nodeName)) {
                    parseStatement(namespace, element, resource);
                }
            }
            log.info("Loaded xml mapper: {}", resource);
        } catch (Exception ex) {
            log.error("Failed to load mapper xml {}: {}", resource, ex.getMessage(), ex);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignore) {
                    log.debug("Failed to close mapper stream for {}: {}", resource, ignore.getMessage());
                }
            }
        }
    }

    private static Document buildDocument(InputStream inputStream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(inputStream);
        } catch (Exception ex) {
            log.error("Failed to parse mapper xml: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private static boolean isStatement(String nodeName) {
        if (StringUtils.isEmpty(nodeName)) {
            return false;
        }
        String lower = nodeName.toLowerCase(Locale.ENGLISH);
        return "select".equals(lower) || "update".equals(lower) || "insert".equals(lower) || "delete".equals(lower);
    }

    private static void parseStatement(String namespace, Element element, String resource) {
        String id = element.getAttribute("id");
        if (StringUtils.isEmpty(id)) {
            log.warn("Skip statement without id in {}", resource);
            return;
        }
        SqlNode rootSqlNode = parseChildren(element, namespace);
        if (rootSqlNode == null) {
            log.warn("Skip statement {} because body is empty", id);
            return;
        }
        String qualifiedId = qualifyId(namespace, id);
        STATEMENTS.put(qualifiedId, new XmlMappedStatement(qualifiedId, rootSqlNode));
    }

    private static SqlNode parseChildren(Element element, String namespace) {
        NodeList childNodes = element.getChildNodes();
        List<SqlNode> contents = new ArrayList<SqlNode>();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            SqlNode sqlNode = parseNode(child, namespace);
            if (sqlNode != null) {
                contents.add(sqlNode);
            }
        }
        if (contents.isEmpty()) {
            return null;
        }
        if (contents.size() == 1) {
            return contents.get(0);
        }
        return new MixedSqlNode(contents);
    }

    private static SqlNode parseNode(Node node, String namespace) {
        if (node == null) {
            return null;
        }
        short nodeType = node.getNodeType();
        if (nodeType == Node.TEXT_NODE || nodeType == Node.CDATA_SECTION_NODE) {
            String text = node.getNodeValue();
            if (StringUtils.isEmpty(text)) {
                return null;
            }
            return new TextSqlNode(text);
        }
        if (!(node instanceof Element)) {
            return null;
        }
        Element element = (Element) node;
        String nodeName = element.getNodeName();
        if ("if".equals(nodeName)) {
            return new IfSqlNode(element.getAttribute("test"), parseChildren(element, namespace));
        }
        if ("choose".equals(nodeName)) {
            return parseChoose(element, namespace);
        }
        if ("when".equals(nodeName)) {
            return new IfSqlNode(element.getAttribute("test"), parseChildren(element, namespace));
        }
        if ("otherwise".equals(nodeName)) {
            return parseChildren(element, namespace);
        }
        if ("trim".equals(nodeName)) {
            return new TrimSqlNode(
                    parseChildren(element, namespace),
                    element.getAttribute("prefix"),
                    element.getAttribute("suffix"),
                    element.getAttribute("prefixOverrides"),
                    element.getAttribute("suffixOverrides")
            );
        }
        if ("where".equals(nodeName)) {
            return new WhereSqlNode(parseChildren(element, namespace));
        }
        if ("set".equals(nodeName)) {
            return new SetSqlNode(parseChildren(element, namespace));
        }
        if ("foreach".equals(nodeName)) {
            return new ForEachSqlNode(
                    element.getAttribute("collection"),
                    element.getAttribute("item"),
                    element.getAttribute("index"),
                    element.getAttribute("open"),
                    element.getAttribute("close"),
                    element.getAttribute("separator"),
                    parseChildren(element, namespace)
            );
        }
        if ("include".equals(nodeName)) {
            String refId = element.getAttribute("refid");
            if (StringUtils.isEmpty(refId)) {
                log.warn("<include> missing refid");
                return null;
            }
            return new IncludeSqlNode(qualifyId(namespace, refId));
        }
        return parseChildren(element, namespace);
    }

    private static SqlNode parseChoose(Element chooseElement, String namespace) {
        NodeList childNodes = chooseElement.getChildNodes();
        List<IfSqlNode> whenNodes = new ArrayList<IfSqlNode>();
        SqlNode otherwise = null;
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (!(child instanceof Element)) {
                continue;
            }
            Element element = (Element) child;
            if ("when".equals(element.getNodeName())) {
                whenNodes.add(new IfSqlNode(element.getAttribute("test"), parseChildren(element, namespace)));
            } else if ("otherwise".equals(element.getNodeName())) {
                otherwise = parseChildren(element, namespace);
            }
        }
        return new ChooseSqlNode(whenNodes, otherwise);
    }

    private static String qualifyId(String namespace, String id) {
        if (StringUtils.isEmpty(id)) {
            return id;
        }
        if (id.contains(".") || StringUtils.isEmpty(namespace)) {
            return id;
        }
        return namespace + "." + id;
    }

    private static final class XmlMappedStatement {

        private final String id;
        private final SqlNode root;

        private XmlMappedStatement(String id, SqlNode root) {
            this.id = id;
            this.root = root;
        }

        private PreparedSql build(Object parameterObject) {
            ParameterResolver resolver = new ParameterResolver(parameterObject);
            DynamicContext context = new DynamicContext(resolver);
            root.apply(context);
            return context.toPreparedSql();
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private interface SqlNode {

        void apply(DynamicContext context);
    }

    private static final class MixedSqlNode implements SqlNode {

        private final List<SqlNode> contents;

        private MixedSqlNode(List<SqlNode> contents) {
            this.contents = contents;
        }

        @Override
        public void apply(DynamicContext context) {
            if (contents == null) {
                return;
            }
            for (SqlNode node : contents) {
                if (node != null) {
                    node.apply(context);
                }
            }
        }
    }

    private static final class TextSqlNode implements SqlNode {

        private final String text;

        private TextSqlNode(String text) {
            this.text = text == null ? "" : text;
        }

        @Override
        public void apply(DynamicContext context) {
            if (StringUtils.isEmpty(text)) {
                return;
            }
            StringBuilder sql = context.sqlBuilder();
            int index = 0;
            while (index < text.length()) {
                int start = text.indexOf("#{", index);
                if (start == -1) {
                    sql.append(text.substring(index));
                    break;
                }
                sql.append(text.substring(index, start));
                int end = findPlaceholderEnd(text, start + 2);
                if (end == -1) {
                    sql.append(text.substring(start));
                    break;
                }
                String placeholder = text.substring(start + 2, end).trim();
                sql.append("?");
                context.addParameter(context.getResolver().resolve(placeholder));
                index = end + 1;
            }
        }

        private int findPlaceholderEnd(String text, int start) {
            int braces = 1;
            for (int i = start; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '{') {
                    braces++;
                } else if (c == '}') {
                    braces--;
                    if (braces == 0) {
                        return i;
                    }
                }
            }
            return -1;
        }
    }

    private static final class IfSqlNode implements SqlNode {

        private final String test;
        private final SqlNode contents;

        private IfSqlNode(String test, SqlNode contents) {
            this.test = test;
            this.contents = contents;
        }

        @Override
        public void apply(DynamicContext context) {
            if (contents == null) {
                return;
            }
            if (ExpressionEvaluator.evaluate(test, context.getResolver())) {
                contents.apply(context);
            }
        }
    }

    private static final class ChooseSqlNode implements SqlNode {

        private final List<IfSqlNode> whenNodes;
        private final SqlNode otherwise;

        private ChooseSqlNode(List<IfSqlNode> whenNodes, SqlNode otherwise) {
            this.whenNodes = whenNodes;
            this.otherwise = otherwise;
        }

        @Override
        public void apply(DynamicContext context) {
            if (whenNodes != null) {
                for (IfSqlNode whenNode : whenNodes) {
                    if (whenNode != null && ExpressionEvaluator.evaluate(whenNode.test, context.getResolver())) {
                        whenNode.contents.apply(context);
                        return;
                    }
                }
            }
            if (otherwise != null) {
                otherwise.apply(context);
            }
        }
    }

    private static class TrimSqlNode implements SqlNode {

        private final SqlNode contents;
        private final String prefix;
        private final String suffix;
        private final String prefixOverrides;
        private final String suffixOverrides;

        private TrimSqlNode(SqlNode contents, String prefix, String suffix, String prefixOverrides, String suffixOverrides) {
            this.contents = contents;
            this.prefix = prefix;
            this.suffix = suffix;
            this.prefixOverrides = prefixOverrides;
            this.suffixOverrides = suffixOverrides;
        }

        @Override
        public void apply(DynamicContext context) {
            if (contents == null) {
                return;
            }
            DynamicContext filtered = context.newChild();
            contents.apply(filtered);
            String sql = filtered.getSql();
            if (StringUtils.isEmpty(sql)) {
                return;
            }
            String trimmed = applyOverrides(sql);
            if (StringUtils.isEmpty(trimmed)) {
                return;
            }
            if (StringUtils.isNotEmpty(prefix)) {
                context.append(prefix.trim());
                context.append(" ");
            }
            context.append(trimmed);
            if (StringUtils.isNotEmpty(suffix)) {
                context.append(" ");
                context.append(suffix.trim());
            }
            context.transferParameters(filtered);
        }

        private String applyOverrides(String sql) {
            String trimmed = sql.trim();
            if (StringUtils.isNotEmpty(prefixOverrides)) {
                for (String candidate : prefixOverrides.split("\\|")) {
                    String c = candidate.trim();
                    if (c.length() == 0) {
                        continue;
                    }
                    if (trimmed.toUpperCase(Locale.ENGLISH).startsWith(c.toUpperCase(Locale.ENGLISH))) {
                        trimmed = trimmed.substring(c.length()).trim();
                        break;
                    }
                }
            }
            if (StringUtils.isNotEmpty(suffixOverrides)) {
                for (String candidate : suffixOverrides.split("\\|")) {
                    String c = candidate.trim();
                    if (c.length() == 0) {
                        continue;
                    }
                    if (trimmed.toUpperCase(Locale.ENGLISH).endsWith(c.toUpperCase(Locale.ENGLISH))) {
                        trimmed = trimmed.substring(0, trimmed.length() - c.length()).trim();
                        break;
                    }
                }
            }
            return trimmed;
        }
    }

    private static final class WhereSqlNode extends TrimSqlNode {
        private WhereSqlNode(SqlNode contents) {
            super(contents, "WHERE", null, "AND|OR", null);
        }
    }

    private static final class SetSqlNode extends TrimSqlNode {
        private SetSqlNode(SqlNode contents) {
            super(contents, "SET", null, null, ",");
        }
    }

    private static final class ForEachSqlNode implements SqlNode {

        private final String collectionExpression;
        private final String item;
        private final String index;
        private final String open;
        private final String close;
        private final String separator;
        private final SqlNode contents;

        private ForEachSqlNode(String collectionExpression, String item, String index, String open, String close, String separator, SqlNode contents) {
            this.collectionExpression = StringUtils.isEmpty(collectionExpression) ? "list" : collectionExpression;
            this.item = StringUtils.isEmpty(item) ? "item" : item;
            this.index = StringUtils.isEmpty(index) ? "index" : index;
            this.open = open;
            this.close = close;
            this.separator = separator;
            this.contents = contents;
        }

        @Override
        public void apply(DynamicContext context) {
            Object collectionValue = context.getResolver().resolve(collectionExpression);
            IterableWrapper iterable = IterableWrapper.of(collectionValue);
            if (!iterable.hasItems()) {
                return;
            }
            if (StringUtils.isNotEmpty(open)) {
                context.append(open);
            }
            boolean first = true;
            int i = 0;
            for (Object itemValue : iterable) {
                Map<String, Object> scope = new LinkedHashMap<String, Object>();
                scope.put(item, itemValue);
                scope.put(index, i);
                context.getResolver().pushScope(scope);
                DynamicContext inner = context.newChild();
                contents.apply(inner);
                String innerSql = inner.getSql().trim();
                context.getResolver().popScope();
                if (StringUtils.isEmpty(innerSql)) {
                    i++;
                    continue;
                }
                if (!first && StringUtils.isNotEmpty(separator)) {
                    context.append(separator);
                }
                context.append(innerSql);
                context.transferParameters(inner);
                first = false;
                i++;
            }
            if (StringUtils.isNotEmpty(close)) {
                context.append(close);
            }
        }
    }

    private static final class IncludeSqlNode implements SqlNode {

        private final String refId;

        private IncludeSqlNode(String refId) {
            this.refId = refId;
        }

        @Override
        public void apply(DynamicContext context) {
            SqlNode fragment = FRAGMENTS.get(refId);
            if (fragment == null) {
                throw new IllegalArgumentException("Sql fragment not found: " + refId);
            }
            fragment.apply(context);
        }
    }

    private static final class DynamicContext {

        private final ParameterResolver resolver;
        private final StringBuilder sqlBuilder;
        private final List<Object> parameters;

        private DynamicContext(ParameterResolver resolver) {
            this(resolver, new StringBuilder(), new ArrayList<Object>());
        }

        private DynamicContext(ParameterResolver resolver, StringBuilder sqlBuilder, List<Object> parameters) {
            this.resolver = resolver;
            this.sqlBuilder = sqlBuilder;
            this.parameters = parameters;
        }

        private DynamicContext newChild() {
            return new DynamicContext(resolver, new StringBuilder(), new ArrayList<Object>());
        }

        private StringBuilder sqlBuilder() {
            return sqlBuilder;
        }

        private void append(String text) {
            if (text == null) {
                return;
            }
            sqlBuilder.append(text);
        }

        private void addParameter(Object value) {
            parameters.add(value);
        }

        private void transferParameters(DynamicContext child) {
            if (child == null) {
                return;
            }
            parameters.addAll(child.parameters);
        }

        private String getSql() {
            return sqlBuilder.toString();
        }

        private ParameterResolver getResolver() {
            return resolver;
        }

        private PreparedSql toPreparedSql() {
            return new PreparedSql(sqlBuilder.toString(), parameters);
        }
    }

    private static final class ParameterResolver {

        private final Object parameterObject;
        private final Deque<Map<String, Object>> scopes = new ArrayDeque<Map<String, Object>>();

        private ParameterResolver(Object parameterObject) {
            this.parameterObject = parameterObject;
        }

        private void pushScope(Map<String, Object> scope) {
            if (scope != null) {
                scopes.push(scope);
            }
        }

        private void popScope() {
            if (!scopes.isEmpty()) {
                scopes.pop();
            }
        }

        private Object resolve(String expression) {
            if (StringUtils.isEmpty(expression)) {
                return parameterObject;
            }
            String normalized = expression.trim();
            String[] parts = normalized.split("\\.");
            Object current = resolveFirst(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                current = navigate(current, parts[i]);
            }
            return current;
        }

        private Object resolveFirst(String name) {
            Object scoped = resolveFromScopes(name);
            if (scoped != null) {
                return scoped;
            }
            if (parameterObject instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) parameterObject;
                if (map.containsKey(name)) {
                    return map.get(name);
                }
            }
            return ReflectionPropertyAccessor.getValue(parameterObject, name);
        }

        private Object resolveFromScopes(String name) {
            if (StringUtils.isEmpty(name)) {
                return null;
            }
            String actualName = name;
            String indexExpression = null;
            if (name.contains("[") && name.endsWith("]")) {
                int bracketIndex = name.indexOf('[');
                actualName = name.substring(0, bracketIndex);
                indexExpression = name.substring(bracketIndex + 1, name.length() - 1);
            }
            for (Map<String, Object> scope : scopes) {
                if (scope.containsKey(actualName)) {
                    Object value = scope.get(actualName);
                    if (indexExpression != null) {
                        return navigate(value, indexExpression);
                    }
                    return value;
                }
            }
            return null;
        }

        private Object navigate(Object current, String property) {
            if (current == null || StringUtils.isEmpty(property)) {
                return null;
            }
            String actualProperty = property;
            String index = null;
            if (property.contains("[") && property.endsWith("]")) {
                int bracketIndex = property.indexOf('[');
                actualProperty = property.substring(0, bracketIndex);
                index = property.substring(bracketIndex + 1, property.length() - 1);
            }
            Object value = null;
            if (current instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) current;
                if (map.containsKey(actualProperty)) {
                    value = map.get(actualProperty);
                } else if (StringUtils.isEmpty(actualProperty)) {
                    value = map.get(index);
                    index = null;
                }
            } else if (StringUtils.isEmpty(actualProperty)) {
                value = current;
            } else {
                value = ReflectionPropertyAccessor.getValue(current, actualProperty);
            }
            if (index != null) {
                return navigate(value, index);
            }
            if (value == null && isInteger(actualProperty) && (current instanceof List || current.getClass().isArray())) {
                index = actualProperty;
                value = ReflectionPropertyAccessor.getValue(current, index);
            }
            return value;
        }

        private boolean isInteger(String text) {
            if (StringUtils.isEmpty(text)) {
                return false;
            }
            for (int i = 0; i < text.length(); i++) {
                if (!Character.isDigit(text.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class ReflectionPropertyAccessor {

        private static Object getValue(Object target, String property) {
            if (target == null) {
                return null;
            }
            if (StringUtils.isEmpty(property)) {
                return target;
            }
            if ("_parameter".equals(property) || "_root".equals(property)) {
                return target;
            }
            if (target instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) target;
                if (map.containsKey(property)) {
                    return map.get(property);
                }
                return null;
            }
            if (target instanceof List) {
                List<?> list = (List<?>) target;
                try {
                    int index = Integer.parseInt(property);
                    if (index >= 0 && index < list.size()) {
                        return list.get(index);
                    }
                } catch (Exception ignore) {
                }
                return null;
            }
            if (target.getClass().isArray()) {
                try {
                    int index = Integer.parseInt(property);
                    return java.lang.reflect.Array.get(target, index);
                } catch (Exception ignore) {
                    return null;
                }
            }
            try {
                return entity.tool.util.ReflectionUtils.getFieldValue(target, property);
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    private static final class IterableWrapper implements Iterable<Object> {

        private final List<Object> items;

        private IterableWrapper(List<Object> items) {
            this.items = items;
        }

        private static IterableWrapper of(Object source) {
            if (source == null) {
                return new IterableWrapper(Collections.emptyList());
            }
            if (source instanceof Iterable) {
                List<Object> copy = new ArrayList<Object>();
                for (Object item : (Iterable<?>) source) {
                    copy.add(item);
                }
                return new IterableWrapper(copy);
            }
            if (source.getClass().isArray()) {
                int length = java.lang.reflect.Array.getLength(source);
                List<Object> copy = new ArrayList<Object>(length);
                for (int i = 0; i < length; i++) {
                    copy.add(java.lang.reflect.Array.get(source, i));
                }
                return new IterableWrapper(copy);
            }
            if (source instanceof Map) {
                return new IterableWrapper(new ArrayList<Object>(((Map<?, ?>) source).values()));
            }
            return new IterableWrapper(Collections.singletonList(source));
        }

        private boolean hasItems() {
            return !items.isEmpty();
        }

        @Override
        public java.util.Iterator<Object> iterator() {
            return items.iterator();
        }
    }

    private static final class ExpressionEvaluator {

        private static boolean evaluate(String expression, ParameterResolver resolver) {
            if (StringUtils.isEmpty(expression)) {
                return false;
            }
            return evaluateOr(expression.trim(), resolver);
        }

        private static boolean evaluateOr(String expression, ParameterResolver resolver) {
            int index = findLogicalOperator(expression, "or");
            if (index >= 0) {
                String left = expression.substring(0, index);
                String right = expression.substring(index + 2);
                return evaluateOr(left, resolver) || evaluateAnd(right, resolver);
            }
            return evaluateAnd(expression, resolver);
        }

        private static boolean evaluateAnd(String expression, ParameterResolver resolver) {
            int index = findLogicalOperator(expression, "and");
            if (index >= 0) {
                String left = expression.substring(0, index);
                String right = expression.substring(index + 3);
                return evaluateAnd(left, resolver) && evaluateFactor(right, resolver);
            }
            return evaluateFactor(expression, resolver);
        }

        private static boolean evaluateFactor(String expression, ParameterResolver resolver) {
            String trimmed = trimParentheses(expression);
            if (StringUtils.isEmpty(trimmed)) {
                return false;
            }
            if (trimmed.startsWith("!")) {
                return !evaluateFactor(trimmed.substring(1), resolver);
            }
            Comparison comparison = Comparison.parse(trimmed);
            if (comparison != null) {
                Object left = resolver.resolve(comparison.left);
                Object right = comparison.parseRightValue(resolver);
                return comparison.compare(left, right);
            }
            Object value = resolver.resolve(trimmed);
            return truthy(value);
        }

        private static int findLogicalOperator(String expression, String operator) {
            String lower = expression.toLowerCase(Locale.ENGLISH);
            int depth = 0;
            for (int i = 0; i < lower.length(); i++) {
                char c = lower.charAt(i);
                if (c == '(') {
                    depth++;
                    continue;
                }
                if (c == ')') {
                    depth--;
                    continue;
                }
                if (depth == 0 && lower.startsWith(operator, i) && isBoundary(lower, i - 1) && isBoundary(lower, i + operator.length())) {
                    return i;
                }
            }
            return -1;
        }

        private static boolean isBoundary(String text, int index) {
            if (index < 0 || index >= text.length()) {
                return true;
            }
            return !Character.isLetterOrDigit(text.charAt(index));
        }

        private static String trimParentheses(String expression) {
            String trimmed = expression.trim();
            while (trimmed.startsWith("(") && trimmed.endsWith(")")) {
                String candidate = trimmed.substring(1, trimmed.length() - 1).trim();
                if (!isBalanced(candidate)) {
                    break;
                }
                trimmed = candidate;
            }
            return trimmed;
        }

        private static boolean isBalanced(String expression) {
            int depth = 0;
            for (int i = 0; i < expression.length(); i++) {
                char c = expression.charAt(i);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                    if (depth < 0) {
                        return false;
                    }
                }
            }
            return depth == 0;
        }

        private static boolean truthy(Object value) {
            if (value == null) {
                return false;
            }
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue() != 0;
            }
            if (value instanceof Collection) {
                return !CollectionUtils.isEmpty((Collection<?>) value);
            }
            if (value instanceof CharSequence) {
                return ((CharSequence) value).length() > 0;
            }
            return true;
        }

        private static final class Comparison {

            private final String left;
            private final String operator;
            private final String rightLiteral;

            private Comparison(String left, String operator, String rightLiteral) {
                this.left = left.trim();
                this.operator = operator.trim();
                this.rightLiteral = rightLiteral.trim();
            }

            private static Comparison parse(String expression) {
                String[] operators = new String[]{"!=", ">=", "<=", "==", ">", "<"};
                for (String operator : operators) {
                    int index = indexOfOperator(expression, operator);
                    if (index >= 0) {
                        String left = expression.substring(0, index);
                        String right = expression.substring(index + operator.length());
                        return new Comparison(left, operator, right);
                    }
                }
                return null;
            }

            private static int indexOfOperator(String expression, String operator) {
                int depth = 0;
                for (int i = 0; i <= expression.length() - operator.length(); i++) {
                    char c = expression.charAt(i);
                    if (c == '(') {
                        depth++;
                        continue;
                    }
                    if (c == ')') {
                        depth--;
                        continue;
                    }
                    if (depth == 0 && expression.regionMatches(true, i, operator, 0, operator.length())) {
                        return i;
                    }
                }
                return -1;
            }

            private Object parseRightValue(ParameterResolver resolver) {
                String literal = rightLiteral.trim();
                if ("null".equalsIgnoreCase(literal)) {
                    return null;
                }
                if ("true".equalsIgnoreCase(literal) || "false".equalsIgnoreCase(literal)) {
                    return Boolean.valueOf(literal);
                }
                if ((literal.startsWith("'") && literal.endsWith("'")) || (literal.startsWith("\"") && literal.endsWith("\""))) {
                    return literal.substring(1, literal.length() - 1);
                }
                try {
                    if (literal.contains(".")) {
                        return Double.parseDouble(literal);
                    }
                    return Long.parseLong(literal);
                } catch (NumberFormatException ignore) {
                }
                return resolver.resolve(literal);
            }

            private boolean compare(Object leftValue, Object rightValue) {
                if ("==".equals(operator)) {
                    if (leftValue == null) {
                        return rightValue == null;
                    }
                    return leftValue.equals(rightValue);
                }
                if ("!=".equals(operator)) {
                    if (leftValue == null) {
                        return rightValue != null;
                    }
                    return !leftValue.equals(rightValue);
                }
                if (leftValue == null || rightValue == null) {
                    return false;
                }
                if (!(leftValue instanceof Comparable<?>) || !(rightValue instanceof Comparable<?>)) {
                    return false;
                }
                int compare = compareValues((Comparable<?>) leftValue, (Comparable<?>) rightValue);
                if (">".equals(operator)) {
                    return compare > 0;
                }
                if (">=".equals(operator)) {
                    return compare >= 0;
                }
                if ("<".equals(operator)) {
                    return compare < 0;
                }
                if ("<=".equals(operator)) {
                    return compare <= 0;
                }
                return false;
            }

            private int compareValues(Comparable<?> leftValue, Comparable<?> rightValue) {
                if (leftValue.getClass().equals(rightValue.getClass())) {
                    @SuppressWarnings("unchecked")
                    Comparable<Object> leftComparable = (Comparable<Object>) leftValue;
                    return leftComparable.compareTo(rightValue);
                }
                double l = numericValue(leftValue);
                double r = numericValue(rightValue);
                return Double.compare(l, r);
            }

            private double numericValue(Object value) {
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                try {
                    return Double.parseDouble(value.toString());
                } catch (Exception ignore) {
                    return 0;
                }
            }
        }
    }
}

