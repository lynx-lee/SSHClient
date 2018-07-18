package net.lynxlee.toolsstudio.ssh.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.air.svnSyncTools.conf.config.ConfigHelper;

public class Util {

	public static final String SUCCESS_FLAG = "successfully";

	/**
	 * log4j日志记录器
	 */
//	private static Log log = LogFactory.getLog(Util.class);

	/**
	 * 判断字符是否加密
	 * 
	 * @param value
	 * @return
	 */
	public static boolean isAESValue(String value) {
		if (-1 == value.indexOf("{AES}")) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * @param value
	 * @return
	 */
	public static String removeAESValue(String value) {
		String str = "{AES}";
		int i = str.length();
		int j = value.indexOf("{AES}");
		return value.substring(j + i, value.length());
	}

	/**
	 * @param value
	 * @return
	 */
	public static String addAESValue(String value) {
		String str = "{AES}";
		StringBuffer sb = new StringBuffer();
		sb.append(str);
		sb.append(value);
		return sb.toString();
	}

	/**
	 * 
	 * @param str
	 * @return
	 */
	public static String getVariableValue(String str) {
		String label_1 = "${";
		String label_2 = "}";
		String _key, _value;
		int start = -1;
		int end = -1;
		int bstart = -1;
		int bend = -1;

		while (true) {
			start = str.indexOf(label_1);
			end = str.indexOf(label_2, start) + 1;
			if (start == bstart && end == bend) {
				break;
			}
			bstart = start;
			bend = end;
			if (start > -1 && end > -1) {
				_key = str.substring(start, end);
				_value = ConfigHelper.getInstance().getPropertie(_key.substring(label_1.length(), _key.length() - 1));
				if (null != _value && !"".equals(_value)) {
					str = str.replace(_key, _value);
				}
			} else {
				break;
			}
		}
		return str;
	}

	/**
	 * 
	 * @param shell
	 * @param value
	 * @return
	 */
	public static String makeShell(String shell, String... value) {
		String label_1 = "#{";
		String label_2 = "}";
		String _tmp = null;
		int count = 0;
		if (null != value && value.length > 0) {
			while (true) {
				_tmp = label_1 + (count + 1) + label_2;
				if (shell.contains(_tmp)) {
					shell = shell.replace(_tmp, null == value[count] && "".equals(value[count]) ? _tmp : value[count]);
				} else {
					break;
				}
				count++;
			}
		}
		return shell;
	}

	/**
	 * 
	 * @param str
	 * @param delim
	 * @return
	 */
	public static String[] toStringArray(String str, String delim) {

		StringTokenizer stk = new StringTokenizer(str, delim);
		String[] strs = new String[stk.countTokens()];
		int i = 0;
		while (stk.hasMoreTokens()) {
			strs[i] = stk.nextToken();
			i++;
		}
		return strs;
	}

	public static String getTableName(String str) {
		String[] strs = str.split("[ ]");
		ArrayList<String> array = new ArrayList<String>();
		for (int i = 0; i < strs.length; i++) {
			if (!strs[i].trim().equals("")) {
				array.add(strs[i].trim());
			}
		}
		return array.get(2).substring(1, array.get(2).length() - 1);
	}

	public static boolean checkIP(String ip) {
		// "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
		// +"(00?\\d|1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
		// +"(00?\\d|1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
		// +"(00?\\d|1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$"
		Pattern pattern = Pattern.compile(
				"\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b");
		Matcher matcher = pattern.matcher(ip); // 以验证127.400.600.2为例
		return matcher.matches();
	}

	public static boolean checkNumber(String number) {
		Pattern pattern = Pattern.compile("^[0-9]+$");
		Matcher matcher = pattern.matcher(number); // 以验证数字
		return matcher.matches();
	}

	public static String urlEncode(String url) {
		String[] pattern = { "%", "+", " ", "?", "#", "&", "=" };
		try {
			for (String x : pattern) {
				if (url.contains(x)) {
					url = url.replaceAll(x, URLEncoder.encode(x, "UTF-8"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return url;
	}

	public static String toCharset(String str, String charset) throws UnsupportedEncodingException {
		if (str.equals(new String(str.getBytes("UTF-8"), "UTF-8"))) {
			return new String(str.getBytes("UTF-8"), charset);
		} else if (str.equals(new String(str.getBytes("US-ASCII"), "US-ASCII"))) {
			return new String(str.getBytes("US-ASCII"), charset);
		} else if (str.equals(new String(str.getBytes("ISO-8859-1"), "ISO-8859-1"))) {
			return new String(str.getBytes("ISO-8859-1"), charset);
		} else if (str.equals(new String(str.getBytes("UTF-16BE"), "UTF-16BE"))) {
			return new String(str.getBytes("UTF-16BE"), charset);
		} else if (str.equals(new String(str.getBytes("UTF-16LE"), "UTF-16LE"))) {
			return new String(str.getBytes("UTF-16LE"), charset);
		} else if (str.equals(new String(str.getBytes("UTF-16"), "UTF-16"))) {
			return new String(str.getBytes("UTF-16"), charset);
		} else if (str.equals(new String(str.getBytes("GBK"), "GBK"))) {
			return new String(str.getBytes("GBK"), charset);
		} else if (str.equals(new String(str.getBytes("GB2132"), "GB2132"))) {
			return new String(str.getBytes("GB2132"), charset);
		} else if (str.equals(new String(str.getBytes("GB18030"), "GB18030"))) {
			return new String(str.getBytes("GB18030"), charset);
		}
		return str;
	}

	/**
	 * Unicode转 汉字字符串
	 * 
	 * @param str
	 * 
	 * @return
	 */
	public static String unicodeToString(String str) {
		Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
		Matcher matcher = pattern.matcher(str);
		char ch;
		while (matcher.find()) {
			// group 6728
			String group = matcher.group(2);
			// ch:'木' 26408
			ch = (char) Integer.parseInt(group, 16);
			// group1 \u6728
			String group1 = matcher.group(1);
			str = str.replace(group1, ch + "");
		}
		return str;
	}

	/**
	 * Unicode转 汉字字符串
	 * 
	 * @param str
	 * 
	 * @return
	 */
	public static String urlDecodeToString(String str, String charset) {
		Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
		Matcher matcher = pattern.matcher(str);
		char ch;
		while (matcher.find()) {
			// group 6728
			String group = matcher.group(2);
			// ch:'木' 26408
			ch = (char) Integer.parseInt(group, 16);
			// group1 \u6728
			String group1 = matcher.group(1);
			str = str.replace(group1, ch + "");
		}
		return str;
	}

	public static String setTimeUnit(double time, int i) {
		final String[] unit = { "纳秒", "微秒", "毫秒", "秒", "分钟", "小时" };
		double _time = 0;
		switch (i) {
		case 0:
			_time = time / 1000;
			if (_time < 1) {
				return String.format("%.2f", time) + unit[i];
			}
			return _time < 1000 ? String.format("%.2f", _time) + unit[i + 1] : setTimeUnit(_time, i + 1);
		case 1:
			_time = time / 1000;
			return _time < 1000 ? String.format("%.2f", _time) + unit[i + 1] : setTimeUnit(_time, i + 1);
		case 2:
			return time / 1000 < 60 ? String.format("%.2f", time / 1000) + unit[i + 1]
					: setTimeUnit(time / 1000, i + 1);
		case 3:
			return time / 60 < 60 ? String.format("%.2f", time / 60) + unit[i + 1] : setTimeUnit(time / 60, i + 1);
		case 4:
			return time / 60 < 60 ? String.format("%.2f", time / 60) + unit[i + 1] : setTimeUnit(time / 60, i + 1);
		case 5:
			return time + unit[i];
		}
		return null;
	}

	public static void main(String[] args) {
		System.out.println(makeShell(
				"cd; cd air_tmp; mkdir .#{1}; scp -rCl 20000 ${ssh.server2.user}@${ssh.server2.host}:#{2} .#{3}",
				"/svn/repositories/ModelBank_Galaxy/conf/", "/svn/repositories/ModelBank_Galaxy/conf/authz",
				"/svn/repositories/ModelBank_Galaxy/conf/"));
	}
	// String xx =
	// "CREATE TABLE \"TBL_SERVERS\" (\"ID\" INTEGER PRIMARY KEY
	// AUTOINCREMENT NOT NULL , \"SERVER_NAME\" VARCHAR NOT NULL ,
	// \"SERVER_HOST\" VARCHAR, \"SERVER_PORT\" VARCHAR, \"SERVER_USER\"
	// VARCHAR, \"SERVER_PASSWORD\" VARCHAR, \"SERVER_STATUS\" INTEGER)";
	//
	// System.out.println(getTableName(xx));

	// String qq =
	// "service=hostset&hostset_status=delhost&serverName=server1&xx=%22aa%22&data%5B0%5D%5BServerName%5D=server1&data%5B0%5D%5BIPAddress%5D=192.168.0.1&data%5B0%5D%5BPort%5D=22&data%5B0%5D%5BUser%5D=root&data%5B1%5D%5BServerName%5D=server2&data%5B1%5D%5BIPAddress%5D=192.168.0.2&data%5B1%5D%5BPort%5D=22&data%5B1%5D%5BUser%5D=root&data%5B2%5D%5BServerName%5D=server3&data%5B2%5D%5BIPAddress%5D=192.168.0.3&data%5B2%5D%5BPort%5D=22&data%5B2%5D%5BUser%5D=root";
	// try {
	// System.out.println(URLDecoder.decode(qq, "utf-8"));
	// } catch (UnsupportedEncodingException e) {
	// e.printStackTrace();
	// }
	// try {
	// System.out.println(getConfigPath());
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
}