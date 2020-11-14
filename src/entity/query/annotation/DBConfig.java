/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */


package entity.query.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)

public @interface DBConfig {
	public String id() default "";
	public String server() default "";
	public String uid() default "";
	public String pwd() default "";
	public String db() default "";
	public String charset() default "";
	public String dbType() default "";
	public String driverType() default "jdbc";
	public String driver() default "";
	public String path() default "";
	public int port() default 3306;

	public int initialSize() default 5;
	public int maxActive() default 10;
	public boolean single() default true;
	public int minIdle() default 5;
	public int maxWait() default 20000;
	public boolean useUnfairLock() default true;
	public int maxOpenPreparedStatements() default -1;
	public String validationQuery() default "";
	public int validationQueryTimeout() default 20000;
	public int minEvictableIdleTimeMillis() default 0;
	public String filters() default "";
	public boolean autoReconnect() default true;
	public boolean rxjava2() default false;
}
