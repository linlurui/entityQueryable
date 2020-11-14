/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */

package entity.tool.util;

public class RegexUtils {
	
	public static final String PrefixAndSuffix = "\\[\\s*([_\\d\\w]+)\\s*\\]";
	
	public static final String PrefixAndSuffixForChar = "'[^']*\\\\[\\\\s*([_\\\\d\\\\w]+)\\\\s*\\\\][^']*'";
	
	public static final String Args = "[%\\?]*#\\{([^\\{\\}]+)\\}[%\\\\?]*";
	
	public static final String ArgsReplacement = "'*[%\\?]*#\\{([^\\{\\}]+)\\}[%\\\\?]*'*";
	
	public static final String Values = "\\('%s'\\)";
}
