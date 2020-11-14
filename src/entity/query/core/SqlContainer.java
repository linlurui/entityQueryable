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

import java.util.ArrayList;

public final class SqlContainer {
	
	public SqlContainer() {
		Where = new StringBuilder();
		Join = new ArrayList<String>();
		OrderBy = new StringBuilder();
		GroupBy = new StringBuilder();
		Select = new StringBuilder();
		Union = new StringBuilder();
		From = new StringBuilder();
		On = new ArrayList<String>();
	}
	
	public StringBuilder Where;
	public ArrayList<String> Join;
	public StringBuilder OrderBy;
	public StringBuilder GroupBy;
	public StringBuilder Select;
	public StringBuilder Union;
	public StringBuilder From;
	public ArrayList<String> On;
}
