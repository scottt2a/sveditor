package net.sf.sveditor.core.log;

public class LogHandle implements ILogHandle {
	private String			fName;
	private ILogListener	fListener;
	private int				fDebugLevel = 10;
	private int				fIndent;
	
	public LogHandle(String name) {
		fName = name;
	}
	
	public void init(ILogListener parent) {
		fListener = parent;
	}
	
	public String getName() {
		return fName;
	}

	public void print(int type, int level, String msg) {
	}

	public void println(int type, int level, String msg) {
		fListener.message(this, type, level, msg);
	}
	
	public void debug(String msg) {
		println(ILogListener.Type_Debug, fDebugLevel, 
				(fIndent > 0)?(indent(fIndent) + msg):msg);
	}

	public void enter(String msg) {
		debug(msg);
		fIndent++;
	}

	public void leave(String msg) {
		if (fIndent > 0) {
			fIndent--;
		}
		debug(msg);
	}

	public void error(String msg) {
		println(ILogListener.Type_Error, fDebugLevel, msg);
	}

	public void error(String msg, Exception e) {
		println(ILogListener.Type_Error, fDebugLevel, msg);
		println(ILogListener.Type_Error, fDebugLevel, e.getMessage());
		for (StackTraceElement s_e : e.getStackTrace()) {
			println(ILogListener.Type_Error, fDebugLevel, "    at " + 
					s_e.getClassName() + "." + s_e.getMethodName() + "(" +
					s_e.getFileName() + ":" + s_e.getLineNumber() + ")");
		}
	}
	
	private String indent(int ind) {
		String ret = "";
		while (ind-- > 0) {
			ret += "    ";
		}
		
		return ret;
	}

}