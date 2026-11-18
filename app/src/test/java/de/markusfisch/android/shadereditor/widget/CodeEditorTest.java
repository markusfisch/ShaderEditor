package de.markusfisch.android.shadereditor.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.shadereditor.editor.Document;
import de.markusfisch.android.shadereditor.editor.Text;

public class CodeEditorTest {
	// #122 with the new editor: pasting a ShaderToy shader converts it once,
	// and cutting the main() of the converted shader does not wrap it again.
	@Test
	public void onlyAnInsertedMainImageIsConverted() {
		String toy = "void mainImage(out vec4 c, in vec2 p) {\n" +
				"\tc = vec4(1.0);\n}\n";
		Document document = new Document(Text.EMPTY);
		List<Text> inserted = new ArrayList<>();
		document.addListener(new Document.Listener() {
			@Override
			public void onChange(Document.Change change, Text before) {
				inserted.add(change.inserted());
			}
		});

		document.replace(0, 0, Text.of(toy));
		String converted = CodeEditor.convert(document,
				document.text().toString(), inserted);
		assertNotNull(converted);
		assertEquals(converted, document.text().toString());
		assertEquals(converted.indexOf("void main()"),
				converted.lastIndexOf("void main()"));

		String cut = converted.substring(0, converted.indexOf("void main()"));
		document.replace(cut.length(), document.text().length(), Text.EMPTY);
		assertNull(CodeEditor.convert(document, document.text().toString(),
				inserted));
		assertEquals(cut, document.text().toString());
	}
}
