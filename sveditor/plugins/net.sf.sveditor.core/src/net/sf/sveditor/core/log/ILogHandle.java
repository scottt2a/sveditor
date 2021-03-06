/****************************************************************************
 * Copyright (c) 2008-2014 Matthew Ballance and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Ballance - initial implementation
 ****************************************************************************/


package net.sf.sveditor.core.log;

public interface ILogHandle extends ILogLevel {
	String				LOG_CAT_DEFAULT = "DEFAULT";
	String				LOG_CAT_PARSER  = "Parser";
	
	
	String getName();
	
	void init(ILogListener parent);
	
	void print(int type, int level, String msg);
	
	void println(int type, int level, String msg);
	
	boolean isEnabled();
	
	int getDebugLevel();
	
	void setDebugLevel(int level);
	
	void addLogLevelListener(ILogLevelListener l);
	
}
