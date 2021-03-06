package net.sf.sveditor.ui.editor.actions;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import net.sf.sveditor.core.scanner.SVCharacter;
import net.sf.sveditor.ui.editor.SVDocumentPartitions;
import net.sf.sveditor.ui.editor.SVEditor;
import net.sf.sveditor.ui.scanutils.SVDocumentTextScanner;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.TextEditorAction;

public class GotoMatchingBracketAction extends TextEditorAction {
	private SVEditor				fEditor;
	private Map<String, String>		fBeginCharMap;
	private Map<String, String>		fEndCharMap;
	
	public GotoMatchingBracketAction(ResourceBundle bundle, String prefix, SVEditor editor) {
		super(bundle, prefix, editor);
		fEditor = editor;
		fBeginCharMap = new HashMap<String, String>();
		fBeginCharMap.put("(", ")");
		fBeginCharMap.put("{", "}");
		fBeginCharMap.put("[", "]");
		fBeginCharMap.put("begin", "end");
		
		fEndCharMap = new HashMap<String, String>();
		fEndCharMap.put(")", "(");
		fEndCharMap.put("}", "{");
		fEndCharMap.put("]", "[");
		fBeginCharMap.put("end", "begin");
	}

	@Override
	public void run() {
		ISourceViewer sv = fEditor.sourceViewer();
		IDocument doc = sv.getDocument();
		ITextSelection tsel = (ITextSelection)fEditor.getSite().getSelectionProvider().getSelection();
		
		int offset = tsel.getOffset();
		
		String st = null, en=null;
		boolean begin = false;
		boolean valid = false;
		
		try {
			int ch = doc.getChar(offset);
			String st_c = "" + ch;
		
			if (fBeginCharMap.containsKey(st_c)) {
				begin = true;
				st = st_c;
				en = fBeginCharMap.get(st_c);
				offset++;
				valid = true;
			} else if (fEndCharMap.containsValue(st_c)) {
				begin = false;
				st = st_c;
				en = fEndCharMap.get(st_c);
				valid = true;
				offset--;
			} else {
				// Scan the characters around the carat
				int st_off = offset;
				int en_off = offset;
				
				
				do {
					ch = doc.getChar(st_off);
				
					if (!SVCharacter.isSVIdentifierPart(ch)) {
						break;
					}
					st_off--;
				} while (st_off >= 0);
				
				if (st_off < 0) {
					st_off = 0;
				} else if (st_off < offset) {
					st_off++;
				}

				do {
					ch = doc.getChar(en_off);
					
					if (!SVCharacter.isSVIdentifierPart(ch)) {
						break;
					}
					en_off++;
				} while (en_off < doc.getLength());
				
				if (en_off > offset) {
					en_off--;
				}
				
				if (en_off > st_off) {
					String str = doc.get(st_off, (en_off-st_off+1));
				
					if (fBeginCharMap.containsKey(str)) {
						st = str;
						en = fBeginCharMap.get(str);
						offset=en_off+1;
						begin = true;
						valid = true;
					} else if (fEndCharMap.containsKey(str)) {
						st = str;
						en = fEndCharMap.get(str);
						offset=st_off-1;
						begin = false;
						valid = true;
					}
				}
			}

			if (valid) {
				SVDocumentTextScanner scanner = new SVDocumentTextScanner(doc, SVDocumentPartitions.SV_PARTITIONING, 
						new String[] {
						SVDocumentPartitions.SV_MULTILINE_COMMENT,
						SVDocumentPartitions.SV_SINGLELINE_COMMENT,
						SVDocumentPartitions.SV_STRING},
						"", offset, begin, true);

				int n_st=1, n_en=0;

				do {
					if ((ch = scanner.get_ch()) == -1) {
						break;
					}

					String t;
					if (ch == '\"') {
						t = scanner.readString(ch);
					} else if ((t = scanner.readIdentifier(ch)) == null) {
						scanner.get_ch();
						t = "" + (char)ch;
					}

					if (t.equals(st)) {
						n_st++;
					} else if (t.equals(en)) {
						n_en++;
					}
				} while (n_st != n_en);

				if (n_st == n_en) {
					int pos = (int)scanner.getPos();

					if (!begin) {
						pos++;
					}

					sv.setSelectedRange(pos, 0);
				}
			}
		} catch (BadLocationException e) { }
	}

}
