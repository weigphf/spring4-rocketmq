package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConfigUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);

    public static String getPropVal2String(String fileName, String key) {
        try {
            return java.util.ResourceBundle.getBundle(fileName).getString(key);
        } catch (Exception ex) {
            logger.error("读取配置错误", ex);
        }

        return null;
    }

    public static Integer getPropVal2Integer(String fileName, String key) {
        try {
            return Integer.parseInt(java.util.ResourceBundle.getBundle(fileName).getString(key));
        } catch (Exception ex) {
            logger.error("读取配置错误", ex);
        }

        return null;
    }
}
