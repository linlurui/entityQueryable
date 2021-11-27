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


import entity.tool.util.JsonUtils;
import entity.tool.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.net.URL;
import java.util.*;


public class ApplicationConfig  {

    private static final Logger log = LoggerFactory.getLogger(ApplicationConfig.class);

    private final static Map<String, String> configMap = new HashMap<String, String>();

    private static Map<String, Object> map;

    private static ApplicationConfig instance;

    public static ApplicationConfig getInstance() {

        if(instance != null) {
            return instance;
        }
        synchronized (configMap) {
            return new ApplicationConfig();
        }
    }

    private ApplicationConfig() {
        try {
            init();
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void init() throws FileNotFoundException {

        if(configMap.size() > 0) {
            return;
        }

        synchronized (configMap) {
            Yaml yml = new Yaml();

            InputStream configStream = getApplicationConfigStream();

            map = yml.loadAs(configStream, HashMap.class);

            InputStream configStream2 = getApplicationConfigByResource();

            if(configStream2 != null) {
                Map map2 = yml.loadAs(configStream2, HashMap.class);
                fillKeyValue("", map2);
            }
            fillKeyValue("", map);
        }
    }

    private Map fillPropertys(Map srcProperties, Map targetProperties) {
        for(Object key : srcProperties.keySet()) {
            if(!targetProperties.containsKey(key)) {
                targetProperties.put(key, srcProperties.get(key));
            }
        }

        return targetProperties;
    }

    private InputStream getApplicationConfigStream() throws FileNotFoundException {
        String property = System.getProperty("catalina.home");
        String path = property+ File.separator + "conf" + File.separator+"application.yml";
        File file = new File(path);
        if(file.exists()) {
            return new FileInputStream(file);
        }

        file = new File(property+ File.separator + "conf" + File.separator+"application.yaml");

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + File.separator+"conf"+File.separator+"application.yml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + File.separator+"conf"+File.separator+"application.yaml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + File.separator+"config"+File.separator+"application.yml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + File.separator+"config"+File.separator+"application.yaml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + File.separator+"application.yaml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + File.separator+"application.yml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + File.separator+"resources"+File.separator+"application.yml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + File.separator+"resources"+File.separator+"application.yaml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + File.separator+"src"+File.separator+"main"+File.separator+"resources"+File.separator+"application.yml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + File.separator+"src"+File.separator+"main"+File.separator+"resources"+File.separator+"application.yaml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }

        try {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream("application.yml");
        }
        catch (Exception e) {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream("application.yaml");
        }
    }

    public InputStream getApplicationConfigByResource() throws FileNotFoundException {
        URL url = this.getClass().getClassLoader().getResource("application.yml");
        if(url == null) {
            return null;
        }
        String path = url.getPath();
        File file = new File(path);
        if(file.exists()) {
            return new FileInputStream(file);
        }
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.yml");
        if(inputStream != null) {
            return inputStream;
        }

        url = this.getClass().getClassLoader().getResource("application.yaml");
        if(url == null) {
            return null;
        }
        path = url.getPath();
        file = new File(path);
        if(file.exists()) {
            return new FileInputStream(file);
        }
        inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.yaml");
        if(inputStream != null) {
            return inputStream;
        }

        return null;
    }

    private synchronized void fillKeyValue(String key, Map<String, Object> map) {

        for(Map.Entry<String, Object> entry : map.entrySet()) {

            String currentKey = String.format("%s%s", StringUtils.isEmpty(key) ? key : key + ".", entry.getKey());
            if(entry.getValue() instanceof Map) {
                fillKeyValue(currentKey, (Map<String, Object>) entry.getValue());
            }

            else {
                configMap.put(String.format("${%s}", currentKey), entry.getValue().toString());
            }
        }
    }

    public void set(String key, String value) {
        configMap.put(String.format("${%s}", key), value);
        List<String> keys = StringUtils.splitString2List(key, "\\.");
        Map tempMap = map;
        for(int i=0; i<keys.size(); i++) {
            if(i==keys.size()-1) {
                tempMap.put(keys.get(i), value);
            }
            else if(!tempMap.containsKey(keys.get(i))) {
                Map newMap = new HashMap();
                tempMap.put(keys.get(i), newMap);
                tempMap = newMap;
            }
        }
    }

    public String get(String key) {

        List<String> keys = StringUtils.splitString2List(key, ":");
        if(keys.size() == 2) {
            return get(keys.get(0) + "}", keys.get(1).substring(0, keys.get(1).length() - 1));
        }

        return get(key, key);
    }

    public <T> T get(String key, T defaultValue) {
        if(configMap == null || configMap.size() < 1) {
            return defaultValue;
        }

        if(configMap.containsKey(key) && StringUtils.isNotEmpty(configMap.get(key))) {
            return (T) StringUtils.cast(defaultValue.getClass(), configMap.get(key).toString());
        }

        return defaultValue;
    }

    public <T> T get(String key, Class<T> clazz) throws IOException {
        if(configMap == null || configMap.size() < 1) {
            return null;
        }

        if(clazz != null && clazz.equals(configMap.get(key).getClass())) {
            return (T) configMap.get(key);
        }

        return JsonUtils.convert(configMap.get(key), clazz);
    }

    public Map<String, Object> getMap(String key) {
        return getMap(key, map);
    }

    public Map<String, Object> getMap(String key, Map<String, Object> data) {
        if(data == null || data.size() < 1) {
            return null;
        }

        List<String> keys = StringUtils.splitString2List(key, "\\.");
        if(keys.size() > 1) {
            if(data.containsKey(keys.get(0)) && data.get(keys.get(0)) != null && data.get(keys.get(0)) instanceof Map) {
                data = (Map<String, Object>) data.get(keys.get(0));
                return getMap(StringUtils.join(".", keys.subList(1, keys.size())), data);
            }
        }

        else if(keys.size() == 1){
            if(data.containsKey(key) && data.get(key) != null && data.get(key) instanceof Map) {
                return (Map<String, Object>) data.get(key);
            }
        }

        return data;
    }

    public List getList(String key) throws IOException {

        Map result = getMap(key);
        if(result instanceof List) {
            return (List) result;
        }

        List keyList = StringUtils.splitString2List(key, "\\.");
        if(result instanceof Map) {
            if(keyList.size() > 0 && result.containsKey(keyList.get(keyList.size() - 1)) &&
                    result.get(keyList.get(keyList.size() - 1)) instanceof List) {
                
            }
        }

        return JsonUtils.convert(result.get(keyList.get(keyList.size() - 1)), ArrayList.class);
    }
}
