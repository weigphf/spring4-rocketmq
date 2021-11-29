package common;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author : linguozhi@52tt.com
 * @desc : 测试基类
 * @date :  2018/2/6
 */
@RunWith(SpringJUnit4ClassRunner.class) //使用junit4进行测试
@ContextConfiguration(locations={"classpath:rootContext.xml"}) //加载配置文件
public class BaseTest {
}
