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


package net.sf.sveditor.core.db.index;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.StringIterableIterator;
import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBMarker;
import net.sf.sveditor.core.db.refs.ISVDBRefSearchSpec;
import net.sf.sveditor.core.db.refs.ISVDBRefVisitor;
import net.sf.sveditor.core.db.search.ISVDBFindNameMatcher;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * Implements an item iterator that operates on a list of index iterators
 * 
 * @author ballance
 *
 */
public class SVDBIndexListIterator implements ISVDBIndexIterator {
	
	private List<ISVDBIndexIterator>			fIndexIteratorList;
	
	public SVDBIndexListIterator() {
		fIndexIteratorList = new ArrayList<ISVDBIndexIterator>();
	}
	
	public void addIndexIterator(ISVDBIndexIterator it) {
		fIndexIteratorList.add(it);
	}

	public List<SVDBFilePath> getFilePath(String path) {
		List<SVDBFilePath> ret = new ArrayList<SVDBFilePath>();
		
		for (ISVDBIndexIterator it : fIndexIteratorList) {
			ret.addAll(it.getFilePath(path));
		}
		
		return ret;
	}

	public List<SVDBDeclCacheItem> findGlobalScopeDecl(
			IProgressMonitor monitor, String name, ISVDBFindNameMatcher matcher) {
		List<SVDBDeclCacheItem> ret = new ArrayList<SVDBDeclCacheItem>();
		for (ISVDBIndexIterator index_it : fIndexIteratorList) {
			List<SVDBDeclCacheItem> tmp = index_it.findGlobalScopeDecl(monitor, name, matcher);
			ret.addAll(tmp);
		}
		return ret;
	}
	
	public List<SVDBMarker> getMarkers(String path) {
		List<SVDBMarker> markers = new ArrayList<SVDBMarker>();
		
		for (ISVDBIndexIterator index_it : fIndexIteratorList) {
			List<SVDBMarker> m = index_it.getMarkers(path);
			
			if (m != null) {
				markers.addAll(m);
			}
		}
		
		return markers;
	}
	
	public void findReferences(
			IProgressMonitor 	monitor,
			ISVDBRefSearchSpec	ref_spec,
			ISVDBRefVisitor		ref_matcher) {
		
		for (ISVDBIndexIterator index_it : fIndexIteratorList) {
			index_it.findReferences(monitor, ref_spec, ref_matcher);
		}
	}
	
	public Iterable<String> getFileList(IProgressMonitor monitor) {
		StringIterableIterator ret = new StringIterableIterator();
		
		for (ISVDBIndexIterator index_it : fIndexIteratorList) {
			ret.addIterable(index_it.getFileList(new NullProgressMonitor()));
		}
		
		return ret;
	}

	public Iterable<String> getFileList(IProgressMonitor monitor, int flags) {
		StringIterableIterator ret = new StringIterableIterator();
		
		for (ISVDBIndexIterator index_it : fIndexIteratorList) {
			ret.addIterable(index_it.getFileList(new NullProgressMonitor(), flags));
		}
		
		return ret;
	}
	
	public List<SVDBIncFileInfo> findIncludeFiles(String root, int flags) {
		List<SVDBIncFileInfo> ret = new ArrayList<SVDBIncFileInfo>();
		
		for (ISVDBIndexIterator index_it : fIndexIteratorList) {
			List<SVDBIncFileInfo> result = index_it.findIncludeFiles(root, flags);
			
			for (SVDBIncFileInfo r : result) {
				if (!ret.contains(r)) {
					ret.add(r);
				}
			}
		}
		
		return ret;
	}

	public SVDBFile findFile(IProgressMonitor monitor, String path) {
		SVDBFile ret = null;
		
		synchronized (fIndexIteratorList) {
			for (ISVDBIndexIterator index_it : fIndexIteratorList) {
				ret = index_it.findFile(monitor, path);
				if (ret != null) {
					break;
				}
			}
		}
		return ret;
	}

	public SVDBFile findPreProcFile(IProgressMonitor monitor, String path) {
		SVDBFile ret = null;
		
		synchronized (fIndexIteratorList) {
			for (ISVDBIndexIterator index_it : fIndexIteratorList) {
				ret = index_it.findPreProcFile(monitor, path);
				if (ret != null) {
					break;
				}
			}
		}
		return ret;
	}
	
	public List<SVDBDeclCacheItem> findPackageDecl(IProgressMonitor monitor,
			SVDBDeclCacheItem pkg_item) {
		List<SVDBDeclCacheItem> ret = new ArrayList<SVDBDeclCacheItem>();
		for (ISVDBIndexIterator index_it : fIndexIteratorList) {
			List<SVDBDeclCacheItem> tmp = index_it.findPackageDecl(monitor, pkg_item);
			ret.addAll(tmp);
		}
		return ret;
	}

	public SVDBFile getDeclFile(IProgressMonitor monitor, SVDBDeclCacheItem item) {
		for (ISVDBIndexIterator index_it : fIndexIteratorList) {
			SVDBFile tmp = index_it.getDeclFile(monitor, item);
			if (tmp != null) {
				return tmp;
			}
		}
		return null;
	}

	public SVDBFile getDeclFilePP(IProgressMonitor monitor, SVDBDeclCacheItem item) {
		for (ISVDBIndexIterator index_it : fIndexIteratorList) {
			SVDBFile tmp = index_it.getDeclFilePP(monitor, item);
			if (tmp != null) {
				return tmp;
			}
		}
		return null;
	}

	public void execOp(
			IProgressMonitor monitor, 
			ISVDBIndexOperation op,
			boolean sync) {
		for (ISVDBIndexIterator index_it : fIndexIteratorList) {
			index_it.execOp(monitor, op, sync);
		}
	}

}
