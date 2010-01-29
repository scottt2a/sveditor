package net.sf.sveditor.core.indent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import net.sf.sveditor.core.scanutils.IRandomAccessTextScanner;

public class SVIndentScanner implements ISVIndentScanner {
	private IRandomAccessTextScanner	fScanner;
	private int							fStartOffset;
	private int							fEndOffset;
	private StringBuilder				fResult;
	private Stack<Integer>				fTabStack;
	private int							fUngetCh;
	private int							fLastCh[] = {-1, -1};
	
	private boolean						fStartLine;
	private String						fLeadingWS;
	private SVIndentToken				fCurrent;
	
	private static Set<String>			fScopeKeywords;
	private static Set<String>			fQualifiers;
	private StringBuilder				fTmp;
	private static Set<String>			fOperators;
	private static final String			fOperatorList[] = {
		// unary operators
		"+", "-", "!", "~", "&", "|", "~|", "^", "~^", "^~",
		
		// binary operators
		"+", "-", "*", "/", "%", "==", "!=", "===", "!==", 
		"==?", "!=?", "&&", "||", "**", "<", "<=", ">", ">=", 
		"&", "|", ">>", "<<", ">>>", "<<<", 
		
		// inc/dec operators
		"++", "--",
		
		// Assignment operators
		"=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=",
		"<<=", ">>=", "<<<=", ">>>=",
		
		// unary_module_path_operator
		"!", "~", "&", "~&", "|", "~|", "^", "~^", "^~",
		
		// binary module-path operator
		"==", "!=", "&&", "||", "&", "|", "^", "^~", "~^",
		
		":", "::"
	};

	
	static {
		fScopeKeywords = new HashSet<String>();
		fScopeKeywords.add("class");
		fScopeKeywords.add("function");
		fScopeKeywords.add("task");
		fScopeKeywords.add("covergroup");
		
		fQualifiers = new HashSet<String>();
		fQualifiers.add("virtual");
		fQualifiers.add("static");
		fQualifiers.add("public");
		fQualifiers.add("local");
		fQualifiers.add("protected");
		
		fOperators = new HashSet<String>();
		for (String op : fOperatorList) {
			if (!fOperators.contains(op)) {
				fOperators.add(op);
			}
		}
	}
	
	
	public SVIndentScanner(ISVIndenter indenter) {
		fResult  = new StringBuilder();
		fTabStack = new Stack<Integer>();
		fTmp = new StringBuilder();
	}
	
	public SVIndentToken next() {
		boolean start_line;
		int pos = 0;
		
		// Scans forward to the next statement and returns the indent level
		SVIndentToken token = null;
		start_line = fStartLine;
		fStartLine = false;

		if (fLeadingWS == null) {
			pos = (int)fScanner.getPos();
			fLeadingWS = getIndent();
		}
		int c = get_ch();

		if (c == '\n') {
			token = new SVIndentToken(SVIndentTokenType.BlankLine, fLeadingWS);
			// Next line will be start-line
			fStartLine = true;
			token.setIsEndLine(true);
		} else if (c == '/') {
			// Likely a comment
			int c2 = get_ch();
			if (c2 == '/') {
				// Read single-line comment
				token = read_single_line_comment(fLeadingWS);
				token.setIsEndLine(true);
			} else if (c2 == '*') {
				token = read_multi_line_comment(fLeadingWS);
			}
		} else if (c == '`' || Character.isJavaIdentifierStart(c)) {
			boolean is_macro = (c == '`');

			if (is_macro) {
				c = get_ch();
			}
			String id = readIdentifier(c);

			if (is_macro) {
				id = '`' + id;
			}

			token = new SVIndentToken(SVIndentTokenType.Identifier, fLeadingWS, id);
		} else if (c == '(') {
			// read an expression
			token = read_expression(fLeadingWS);
		} else if (c == ';') {
			token = new SVIndentToken(SVIndentTokenType.Operator, fLeadingWS, ";");
		} else if (Character.isDigit(c) || c == '\'') {
			fTmp.setLength(0);

			if (c == '\'') {
				int c2 = get_ch();
				if ((c2 >= 'a' && c2 <= 'o') ||
						(c2 >= 'A' && c2 <= 'O')) {
					// probably a number
					fTmp.append((char)c);
					fTmp.append((char)c2);
				} else {
					// probably part of a cast
					unget_ch(c2);
					token = new SVIndentToken(SVIndentTokenType.Operator, 
							fLeadingWS, "'");
				}
			} else {
				fTmp.append((char)c);
			}

			if (token == null) {
				while ((c = get_ch()) != -1 && 
						(c == '_' || Character.isDigit(c) ||
								(c >= 'a' && c <= 'f') ||
								(c >= 'A' && c <= 'F'))) {
					fTmp.append((char)c);
				}

				unget_ch(c);

				token = new SVIndentToken(SVIndentTokenType.Number,
						fLeadingWS, fTmp.toString());
			}
		} else if (c == -1) {
			token = null;
		} else {
			fTmp.setLength(0);
			fTmp.append((char)c);

			while (fOperators.contains(fTmp.toString())) {
				if ((c = get_ch()) == -1) {
					break;
				}
				fTmp.append((char)c);
			}

			if (!fOperators.contains(fTmp.toString())) {
				if (fTmp.length() > 1) {
					fTmp.setLength(fTmp.length()-1);
					unget_ch(c);
				} else {
					unget_ch(c);
				}
			}

			if (fOperators.contains(fTmp.toString())) {
				token = new SVIndentToken(SVIndentTokenType.Operator,
						fLeadingWS, fTmp.toString());
			} else {
				token = null;
				System.out.println("[NOTE] Unknown character \"" + (char)c + "\"");
			}
		}
			
		fLeadingWS = null;

		if (token != null) {
			token.setPos(pos);

			c = get_ch();

			if (c == '\n') {
				token.setIsEndLine(true);
				fStartLine = true;
			} else {
				fTmp.setLength(0);
				unget_ch(c);

				pos = (int)fScanner.getPos();
				while ((c = get_ch()) != -1 && 
						Character.isWhitespace(c) && c != '\n') {
					fTmp.append((char)c);
				}
				if (c == '\n') {
					token.setIsEndLine(true);
					token.setTrailingWS(fTmp.toString());
					fStartLine = true;
				} else {
					fLeadingWS = fTmp.toString();
					unget_ch(c);
				}
			}
			token.setIsStartLine(start_line);
		}
		
		fCurrent = token;
		
		return token;
	}
	
	public SVIndentToken current() {
		return fCurrent;
	}
	
	public String indent(
			IRandomAccessTextScanner 	scanner, 
			int 						start_offset, 
			int 						end_offset) {
		fResult.setLength(0);
		
		fScanner		= scanner;
		fStartOffset	= start_offset;
		fEndOffset		= end_offset;
		fUngetCh		= -1;
		fLastCh[0] = -1;
		fLastCh[1] = '\n';
		
		fTabStack.clear();
		fTabStack.push(0);
		
		indent();
		// Standalone comment
		

		return fResult.toString();
	}
	
	public String indent(IRandomAccessTextScanner scanner) {
		return indent(scanner, -1, -1);
	}
	
	private void indent() {
	}
	
	private SVIndentToken read_expression(String leading_ws) {
		SVIndentExprToken ret = new SVIndentExprToken(leading_ws);
		int n_left = 1, n_right = 0, c;
		boolean read_leading_ws = false;
		fTmp.setLength(0);
		
		fTmp.append('(');
		
		while ((c = get_ch()) != -1 && n_left != n_right) {
			if (read_leading_ws) {
				if (Character.isWhitespace(c)) {
					leading_ws += (char)c;
				} else {
					read_leading_ws = false;
					unget_ch(c);
				}
			} else {
				if (c == '(') {
					n_left++;
				} else if (c == ')') {
					n_right++;
				}
				
				fTmp.append((char)c);
				if (c == '\n') {
					SVIndentToken tok = new SVIndentToken(
							SVIndentTokenType.Expression, leading_ws,
							fTmp.toString());
					tok.setIsEndLine(true);
					ret.addExprElem(tok);
					fTmp.setLength(0);
					read_leading_ws = true;
					leading_ws = "";
				}
			}
		}
		
		unget_ch(c);
		
		if (fTmp.length() > 0) {
			SVIndentToken tok = new SVIndentToken(
					SVIndentTokenType.Expression, leading_ws,
					fTmp.toString());
			ret.addExprElem(tok);
		}

		// Return null if the braces didn't match
		return (n_left == n_right)?ret:null;
	}
	
	
	private SVIndentToken read_single_line_comment(String leading_ws) {
		int c;
		
		fTmp.setLength(0);
		fTmp.append("//");
		
		while ((c = get_ch()) != -1 && c != '\n') {
			fTmp.append((char)c);
		}
		
		unget_ch(c);
		
		return new SVIndentToken(SVIndentTokenType.SingleLineComment, 
				leading_ws, fTmp.toString());
	}

	private SVMultiLineIndentToken read_multi_line_comment(String leading_ws) {
		SVMultiLineIndentToken ret = new SVMultiLineIndentToken(leading_ws); 
		int comment[] = {-1, -1}, c;
		boolean read_newline = false;
		fTmp.setLength(0);
		fTmp.append("/*");

		
		while ((c = get_ch()) != -1) {
			if (read_newline) {
				if (Character.isWhitespace(c) && c != '\n') {
					fTmp.append((char)c);
				} else {
					leading_ws = fTmp.toString();
					fTmp.setLength(0);
					read_newline = false;
					unget_ch(c);
				}
			} else {
				if (c == '\n') {
					SVIndentToken tok = new SVIndentToken(
							SVIndentTokenType.MultiLineComment, leading_ws, 
							fTmp.toString());
					tok.setIsEndLine(true);
					
					read_newline = true;
					ret.addCommentLines(tok);
					fTmp.setLength(0);
					leading_ws = "";
				} else {
					comment[0] = comment[1];
					comment[1] = c;
					fTmp.append((char)c);
					if (comment[0] == '*' && comment[1] == '/') {
						break;
					}
				}
			}
		}
		
		if (fTmp.length() > 0) {
			ret.addCommentLines(new SVIndentToken(
					SVIndentTokenType.MultiLineComment, leading_ws,
					fTmp.toString()));
		}
		
		if (c == -1) {
			return null;
		} else {
			return ret;
		}
	}
	
	private String readIdentifier(int c) {
		fTmp.setLength(0);
		
		fTmp.append((char)c);
		while ((c = get_ch()) != -1 && Character.isJavaIdentifierPart(c)) {
			fTmp.append((char)c);
		}
		unget_ch(c);
		
		return fTmp.toString();
	}
	
	private boolean isQualifier(String key) {
		return fQualifiers.contains(key);
	}
	
	private void inc_indent() {
		fTabStack.push(fTabStack.peek()+1);
	}
	
	private void dec_indent() {
		fTabStack.pop();
	}
	
	private int get_indent() {
		return fTabStack.peek();
	}
	
	/**
	 * Get the indent of the current line. 
	 * 
	 * @return
	 */
	private String getIndent() {
		int c;
		fTmp.setLength(0);
		
		while ((c = get_ch()) != -1 && 
				Character.isWhitespace(c) && c != '\n') {
			fTmp.append((char)c);
		}
		
		if (c != -1) {
			unget_ch(c);
		}
		
		return fTmp.toString();
	}
	
	private int get_ch() {
		int c = -1;
		
		if (fUngetCh != -1) {
			c = fUngetCh;
			fUngetCh = -1;
		} else {
			c = fScanner.get_ch();
			
			fLastCh[0] = fLastCh[1];
			fLastCh[1] = c;
		}
		
		return c;
	}
	
	private int get_lastch() {
		return fLastCh[0];
	}
	
	private void unget_ch(int ch) {
		fUngetCh = ch;
	}
	
	private void put_ch(int c) {
		fResult.append((char)c);
	}
	
	private void put_str(String str) {
		for (int i=0; i<str.length(); i++) {
			put_ch(str.charAt(i));
		}
	}
	
	private boolean is_enabled(int pos) {
		return ((pos >= fStartOffset || fStartOffset == -1) &&
				(pos <= fEndOffset || fEndOffset == -1));
	}
	
}